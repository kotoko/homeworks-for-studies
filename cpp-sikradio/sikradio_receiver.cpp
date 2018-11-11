#include <algorithm>
#include <arpa/inet.h>
#include <atomic>
#include <boost/functional/hash.hpp>
#include <boost/optional.hpp>
#include <boost/program_options.hpp>
#include <boost/thread.hpp>
#include <cassert>
#include <cctype>
#include <chrono>
#include <cstddef>
#include <deque>
#include <fcntl.h>
#include <iostream>
#include <map>
#include <netdb.h>
#include <netinet/in.h>
#include <poll.h>
#include <sstream>
#include <string>
#include <sys/types.h>
#include <sys/socket.h>
#include <time.h>
#include <unordered_map>
#include <vector>

#include "default_values.hpp"
#include "file_descriptor.hpp"
#include "memory_chunk.hpp"
#include "optimizations.hpp"
#include "spinlock_mutex.hpp"
#include "telnet.hpp"
#include "utility.hpp"


using std::atomic;
using std::atomic_bool;
using std::atomic_int;
using std::atomic_llong;
using std::cerr;
using std::cout;
using std::deque;
using std::endl;
using std::exception;
using std::function;
using std::make_shared;
using std::map;
using std::max;
using std::min;
using std::move;
using std::multimap;
using std::nullopt;
using std::optional;
using std::shared_ptr;
using std::sort;
using std::string;
using std::stringstream;
using std::swap;
using std::tolower;
using std::unordered_map;
using std::vector;
using telnet::DO;
using telnet::DONT;
using telnet::ECHO;
using telnet::IAC;
using telnet::LINEMODE;
using telnet::SGA;
using telnet::WILL;
using telnet::WONT;
namespace chrono = std::chrono;
namespace po = boost::program_options;

namespace
{
#ifndef NDEBUG
	constexpr bool DEB = true;
#else
	constexpr bool DEB = false;
#endif

	struct MyException : public exception
	{
	private:
		const char *message_;

	public:
		MyException(const char* message)
				: message_(message)
		{}
		const char* what() const throw()
		{
			return message_;
		}
	};

	struct ParsedStation
	{
		string mcast;
		string name;
		unsigned int data_port;
	};

	struct Station
	{
		string mcast;
		in_addr_t address;
		string name;
		size_t id;  // = hash of ParsedStation
		unsigned int data_port;
	};

	enum UIStatus { SendingUI, SendingCMD };
	enum UIMode { Text, Telnet };
	enum AudioStatus { Idle, Downloading, Playing };

	struct UIClient
	{
		shared_ptr<FileDescriptor> fd;
		UIStatus status;
		UIMode mode;
		unsigned char last_bytes[3];
		size_t pos;
		char* msg;
		size_t msg_len;
	};

	string DISCOVER_ADDR;
	unsigned int CTRL_PORT;
	unsigned int UI_PORT;
	unsigned long BSIZE;  // Bytes.
	unsigned long RTIME;  // Miliseconds.
	optional<string> NAZWA;

	atomic_int rc;  // Return code.
	atomic_bool terminate;  // Is it time to stop?

	deque<shared_ptr<Station>> stations_list;
	atomic<size_t> selected_st;  // Id of station. 0 means none.
	atomic_llong stations_ui_counter;  // Used to detect changes (added/deleted station).
	SpinlockMutex mut_stations;  // For: stations_list.
	SpinlockMutex mut_stations_helper;  // For: stations_list.
}


size_t hash_value(ParsedStation const& station)
{
	// source: https://stackoverflow.com/a/3612016
	size_t seed = 0;
	boost::hash_combine(seed, station.mcast);
	boost::hash_combine(seed, station.data_port);
	boost::hash_combine(seed, station.name);
	return seed == 0 ? 1 : seed;
}

auto get_time()
{
	return chrono::system_clock::now();
}

int open_socket_udp(unsigned long port = 0)
{
	int sock;
	sockaddr_in address;

	address.sin_family = AF_INET;  // IPv4.
	address.sin_addr.s_addr = htonl(INADDR_ANY);  // Any my address IP.
	address.sin_port = htons((uint16_t) port);  // My port. Default: any port.

	// Open socket.
	sock = socket(PF_INET, SOCK_DGRAM, 0);
	if(sock < 0) {
		throw MyException("open_socket_udp(): Error in socket()");
	}

	// Bind the socket to a concrete address.
	if(bind(sock, (struct sockaddr *) &address, (socklen_t) sizeof(address)) < 0) {
		close(sock);
		throw MyException("open_socket_udp(): Error in bind()");
	}

	return sock;
}

int open_socket_tcp(unsigned long port = 0)
{
	int sock;
	sockaddr_in address;

	address.sin_family = AF_INET;  // IPv4.
	address.sin_addr.s_addr = htonl(INADDR_ANY);  // Any my address IP.
	address.sin_port = htons((uint16_t) port);  // My port. Default: any port.

	// Open socket.
	sock = socket(PF_INET, SOCK_STREAM, 0);
	if(sock < 0) {
		throw MyException("open_socket_udp(): Error in socket()");
	}

	// Bind the socket to a concrete address.
	if(bind(sock, (struct sockaddr *) &address, (socklen_t) sizeof(address)) < 0) {
		close(sock);
		throw MyException("open_socket_udp(): Error in bind()");
	}

	return sock;
}

void set_listen_tcp(int sock)
{
	constexpr int CONNECTIONS_QUEUE = 5;

	if(listen(sock, CONNECTIONS_QUEUE) == -1) {
		throw MyException("set_listen_tcp(): Error in listen()");
	}
}

void set_nonblock(int sock)
{
	if(fcntl(sock, F_SETFL, O_NONBLOCK) < 0) {
		throw MyException("set_nonblock(): Error in fcntl()");
	}
}

void join_broadcast(int sock)
{
	int enable = 1;

	if(setsockopt(sock, SOL_SOCKET, SO_BROADCAST, &enable, sizeof(enable)) < 0) {
		throw MyException("join_broadcast(): Error in setsockopt()");
	}
}

void join_multicast(int sock, const string &mcast)
{
	struct ip_mreq mreq;
	mreq.imr_multiaddr.s_addr = inet_addr(mcast.c_str());
	mreq.imr_interface.s_addr = htonl(INADDR_ANY);

	if(setsockopt(sock, IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq)) < 0) {
		throw MyException("join_multicast(): Error in setsockopt()");
	}
}

void set_timeout(int sock, unsigned int sec, unsigned int nano_sec = 0)
{
	struct timeval tv;
	tv.tv_sec = sec;
	tv.tv_usec = nano_sec;

	if(setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, (const char*)&tv, sizeof tv) < 0) {
		throw MyException("set_timeout(): Error in setsockopt()");
	}
}

void require_correct_port(unsigned int p)
{
	if(!is_correct_port(p)) {
		throw MyException("Given port is not in range 1-65535!");
	}
}

void require_correct_ipv4_address(const string& s)
{
	if(!is_correct_ipv4(s)) {
		throw MyException("Given ip address is not a valid ipv4 address!");
	}
}

optional<ParsedStation> parse_reply(MemoryChunk<char>& packet, size_t len)
{
	thread_local static const string msg_borewicz = "BOREWICZ_HERE ";
	ParsedStation station = ParsedStation();

	assert(packet.length() >= 97);

	// Wrong length.
	if(len < 26 || len > 97) {
		if(DEB) {
			if(strncmp("ZERO_SEVEN_COME_IN\n", packet.ptr(), 19) != 0) {
				cerr << "parse_reply(): wrong length " << len << "!" << endl;
			}
		}
		return nullopt;
	}

	// Wrong message.
	if(strncmp(msg_borewicz.c_str(), packet.ptr(), msg_borewicz.length()) != 0) {
		if(DEB) {cerr << "parse_reply(): wrong message!" << endl;}
		return nullopt;
	}

	// Parse MCAST_ADDR.
	// Wrong first byte.
	if(packet.ptr()[14] < '0' || packet.ptr()[14] > '9') {
		if(DEB) {cerr << "parse_reply(): wrong first byte of IP!" << endl;}
		return nullopt;
	}

	char ip[15] = "";
	size_t i;
	size_t offset = 14;

	// Find length.
	for(i = 0; i < 15 && (
			(packet.ptr()[offset + i] >= '0' && packet.ptr()[offset + i] <= '9')
			|| packet.ptr()[offset + i] == '.'
			);
	    ++i) {}

	// Wrong character after IP.
    if(packet.ptr()[offset + i] != ' ') {
	    if(DEB) {cerr << "parse_reply(): wrong character in/after IP!" << endl;}
		return nullopt;
	}

	for(size_t j = 0; j < i; ++j) {
		ip[j] = packet.ptr()[offset + j];
	}
	station.mcast.assign(ip, i);

	offset += i + 1;

	if(!is_correct_ipv4(station.mcast)) {
		if(DEB) {
			cerr << "parse_reply(): IP is not correct ipv4! ('"
			     << station.mcast << "')" << endl;
		}
		return nullopt;
	}

	if(!is_correct_multicast(station.mcast)) {
		if(DEB) {
			cerr << "parse_reply(): IP is not in multicast range! ('"
			     << station.mcast <<"')" << endl;
		}
		return nullopt;
	}

	// Parse DATA_PORT.
	// Find length.
	for(i = 0; i < 5 && packet.ptr()[offset + i] >= '0'
			&& packet.ptr()[offset + i] <= '9'; ++i) {}

	if(packet.ptr()[offset + i] != ' ') {
		if(DEB) {cerr << "parse_reply(): wrong character in/after DATA_PORT!" << endl;}
		return nullopt;
	}

	station.data_port = 0;
	for(size_t j = 0; j < i; ++j) {
		station.data_port = station.data_port * 10 + (packet.ptr()[offset + j] - '0');
	}
	if(packet.ptr()[offset + i] != ' ') {
		if(DEB) {cerr << "parse_reply(): wrong character after DATA_PORT!" << endl;}
		return nullopt;
	}

	offset += i + 1;

	if(!is_correct_port(station.data_port)) {
		if(DEB) {
			cerr << "parse_reply(): port is out of range! ("
			     << station.data_port << ")" << endl;
		}
		return nullopt;
	}

	// Parse NAME
	char name[64] = "";

	// Find length
	for(i = 0; i < 64 && packet.ptr()[offset + i] >= 32; ++i) {}
	if(packet.ptr()[offset + i] != '\n') {
		if(DEB) {
			cerr << "parse_reply(): name contains forbidden "
			        "characters or missing \\n!" << endl;
		}
		return nullopt;
	}

	for(size_t j = 0; j < i; ++j) {
		name[j] = packet.ptr()[offset + j];
	}

	station.name.assign(name, i);
	if(DEB) {
		cerr << "parse_reply(): correct REPLY {"
		     << station.mcast << ", " << station.data_port
		     << ", '" << station.name << "'}" << endl;
	}

	return station;
}

string generate_menu_telnet(
		deque<shared_ptr<Station>>& stations,
		size_t selection = 0)
{
	thread_local static stringstream stream;
	thread_local static const string header =
		"------------------------------------------------------------------------\n\r"
		"  SIK Radio\n\r"
		"------------------------------------------------------------------------\n\r";
	thread_local static const string footer =
		"------------------------------------------------------------------------\n\r";
	thread_local static const string pointer =      "  > ";
	thread_local static const string no_pointer =   "    ";

	// Clear stream.
	stream.str(string());

	// Generate menu.
	stream << header;
	for(const auto& station : stations) {
		if((*station).id == selection) {
			stream << pointer;
		} else {
			stream << no_pointer;
		}
		stream << (*station).name << "\n\r";
	}
	stream << footer;

	return stream.str();
}

string generate_menu_text(deque<shared_ptr<Station>>& stations, size_t selection = 0)
{
	thread_local static stringstream stream;
	thread_local static const string header =
			"------------------------------------------------------------------------\n"
			"  SIK Radio\n"
			"------------------------------------------------------------------------\n";
	thread_local static const string footer =
			"------------------------------------------------------------------------\n";
	thread_local static const string pointer =      "  > ";
	thread_local static const string no_pointer =   "    ";

	// Clear stream.
	stream.str(string());

	// Generate menu.
	stream << header;
	for(const auto& station : stations) {
		if((*station).id == selection) {
			stream << pointer;
		} else {
			stream << no_pointer;
		}
		stream << (*station).name << "\n";
	}
	stream << footer;

	return stream.str();
}

void close_ui_connection(
		vector<UIClient>& connections,
		vector<pollfd>& poll_array,
		size_t i)
{
	if(DEB) {cerr << "close_ui_connection(): bye bye client!" << endl;}

	assert(!connections.empty());
	assert(i < connections.size());

	swap(connections[i], connections[connections.size() - 1]);
	connections.pop_back();

	swap(poll_array[i + 1], poll_array[poll_array.size() - 1]);
	poll_array.pop_back();
}

void arrow_up_ui(deque<shared_ptr<Station>>& stations_copy)
{
	const size_t selected = selected_st;
	ssize_t selected_index = -1;

	for(size_t i = 0; i < stations_copy.size(); ++i) {
		if((*stations_copy[i]).id == selected) {
			selected_index = i;
			break;
		}
	}

	if(selected_index == -1) {
		if(!stations_copy.empty()) {
			selected_st = (*stations_copy[stations_copy.size() - 1]).id;
		}
	} else {
		if(selected_index > 0) {
			selected_st = (*stations_copy[selected_index - 1]).id;
		}
	}
}

void arrow_down_ui(deque<shared_ptr<Station>>& stations_copy)
{
	const size_t selected = selected_st;
	ssize_t selected_index = -1;

	for(size_t i = 0; i < stations_copy.size(); ++i) {
		if((*stations_copy[i]).id == selected) {
			selected_index = i;
			break;
		}
	}

	if(selected_index == -1) {
		if(!stations_copy.empty()) {
			selected_st = (*stations_copy[stations_copy.size() - 1]).id;
		}
	} else {
		if((size_t)selected_index + 1 < stations_copy.size()) {
			selected_st = (*stations_copy[selected_index + 1]).id;
		}
	}
}

void add_byte_retr(
	unordered_map<unsigned long long, chrono::time_point<chrono::system_clock>>& retr_bytes,
	map<chrono::time_point<chrono::system_clock>, unsigned long long>& sorted_retr_bytes,
	unsigned long long byte_nr,
	chrono::time_point<chrono::system_clock>& now)
{
	thread_local static constexpr auto EPSILON = chrono::nanoseconds(1);

	if(retr_bytes.find(byte_nr) == retr_bytes.end()) {
		while(sorted_retr_bytes.find(now) != sorted_retr_bytes.end()) {
			now += EPSILON;
		}

		retr_bytes[byte_nr] = now;
		sorted_retr_bytes[now] = byte_nr;
	}
}

string generate_rexmit(
		unordered_map<unsigned long long, chrono::time_point<chrono::system_clock>>& retr_bytes,
		map<chrono::time_point<chrono::system_clock>, unsigned long long>& sorted_retr_bytes,
		chrono::time_point<chrono::system_clock>& now)
{
	thread_local static const auto rtime = chrono::milliseconds(RTIME);
	thread_local static constexpr auto EPSILON = chrono::nanoseconds(1);
	thread_local static stringstream stream;
	thread_local static const string prefix = "LOUDER_PLEASE ";
	thread_local static const string suffix = "\n";
	bool first;
	unsigned long long byte;
    chrono::time_point<chrono::system_clock> tmp_time;

	assert(!retr_bytes.empty());
	assert(!sorted_retr_bytes.empty());

	// Clear stream.
	stream.str("");

	stream << prefix;

	first = true;
	while(sorted_retr_bytes.begin()->first < now) {
		tmp_time = sorted_retr_bytes.begin()->first;
		byte = sorted_retr_bytes.begin()->second;

		// Add byte to string.
		if(!first) {
			stream << ",";
		}
		stream << byte;

		// Delete old time.
		sorted_retr_bytes.erase(tmp_time);
		retr_bytes.erase(byte);

		// Calculate next time.
		while(tmp_time <= now) {
			tmp_time += rtime;
		}
		while(sorted_retr_bytes.find(tmp_time) != sorted_retr_bytes.end()) {
			tmp_time += EPSILON;
		}

		// Save new time.
		sorted_retr_bytes[tmp_time] = byte;
		retr_bytes[byte] = tmp_time;

		first = false;
	}

	stream << suffix;

	return stream.str();
}

void worker_recv_ctrl_loop(shared_ptr<FileDescriptor> sock_ctrl)
{
	constexpr auto DURATION_EXPIRE = chrono::seconds(20);
	constexpr auto EPSILON = chrono::milliseconds(1);
	constexpr auto UI_COUNTER_MOD = 9000000000000000000LL;
	constexpr size_t MAX_PACKET_CTRL_LEN = 100;  // Bytes.

	MemoryChunk<char> packet(MAX_PACKET_CTRL_LEN);
	socklen_t rcva_len;
	ssize_t len;
	sockaddr_in packet_address;
	auto exp_time = get_time();
	auto now = get_time();
	auto old_time = get_time();
	optional<ParsedStation> parsed_st;
	// expire_times: id -> time.
	unordered_map<size_t, chrono::time_point<chrono::system_clock>> expire_times;
	// sorted_expire_times: time -> id (avoid conflicts).
	map<chrono::time_point<chrono::system_clock>, size_t> sorted_expire_times;
	auto it = sorted_expire_times.begin();
	SpinlockMutexGuardB guard_stations(mut_stations, SpinlockMutexGuard::Action::None);
	SpinlockMutexGuardA guard_stations_helper(mut_stations_helper, SpinlockMutexGuard::Action::None);
	size_t st_id;
	Station st;
	shared_ptr<Station> st_ptr;
	bool removed;
	long long counter;

	// Join to broadcast if needed.
	if(is_correct_broadcast(DISCOVER_ADDR)) {
		join_broadcast(*sock_ctrl);
	}

	// Join to multicast if needed.
	if(is_correct_multicast(DISCOVER_ADDR)) {
		join_multicast(*sock_ctrl, DISCOVER_ADDR);
	}

	// Set timeout on socket.
	set_timeout(*sock_ctrl, 2);

	while(true) {
		assert(expire_times.size() == sorted_expire_times.size());

		// 0. Check if it's time to stop.
		if(unlikely(terminate == true)) {
			return;
		}

		// 1. Forget old stations.
		{
			removed = false;

			now = get_time();
			it = sorted_expire_times.begin();

			guard_stations_helper.lock();  // (Let UI wait a little).
			while(it != sorted_expire_times.end() && it->first < now) {
				removed = true;
				st_id = it->second;

				guard_stations.lock();
				// Move station to the end of list.
				for(size_t i = 0; i + 1 < stations_list.size(); ++i) {
					if((*stations_list[i]).id == st_id) {
						swap(stations_list[i], stations_list[stations_list.size() - 1]);
						break;
					}
				}

				// Remove from the end of list.
				stations_list.pop_back();
				guard_stations.unlock();

				// Remove expire time.
				sorted_expire_times.erase(expire_times[st_id]);
				expire_times.erase(st_id);

				// Get next candidate.
				it = sorted_expire_times.begin();
			}
			guard_stations_helper.unlock();

			// Update ui counter.
			if(removed) {
				counter = stations_ui_counter;
				counter = (counter + 1) % UI_COUNTER_MOD;
				stations_ui_counter = counter;
			}
		}

		// 2. Receive packet.
		rcva_len = (socklen_t) sizeof(packet_address);
		len = recvfrom(*sock_ctrl, (void*)packet.ptr(), packet.size(), 0,
		               (sockaddr *) &packet_address, &rcva_len);

		if(unlikely(len < 0)) {
			if(errno != EAGAIN && errno != EINTR) {
				throw MyException("worker_recv_ctrl_loop(): Error in recvfrom()");
			}
			continue;
		} else {
			if(DEB) {
				if(strncmp("ZERO_SEVEN_COME_IN\n", packet.ptr(), 19) != 0) {
					cerr << "worker_recv_ctrl_loop(): got packet: "
					     << string(packet.ptr(), (size_t) max(len - 1, (ssize_t) 0))
					     << endl;
				}
			}
		}

		// 3. Parse packet.
		parsed_st = parse_reply(packet, (size_t) len);

		// 4. Update stats & list.
		if(parsed_st) {
			// Calculate hash.
			st_id = hash_value(*parsed_st);

			// Save current time.
			now = get_time();

			// Missing from current list.
			if(expire_times.find(st_id) == expire_times.end()) {
				// Create struct.
				st.id = st_id;
				st.name = (*parsed_st).name;
				st.mcast = (*parsed_st).mcast;
				st.data_port = (*parsed_st).data_port;
				st.address = packet_address.sin_addr.s_addr;

				st_ptr = make_shared<Station>(st);

				// Add to global list.
				guard_stations_helper.lock();
				guard_stations.lock();

				stations_list.push_back(st_ptr);

				guard_stations.unlock();
				guard_stations_helper.unlock();

				// Add info about time.
				// This time is for sure NOT in sorted_expire_times.
				exp_time = now - DURATION_EXPIRE - DURATION_EXPIRE;
				expire_times[st_id] = exp_time;
				sorted_expire_times[exp_time] = st_id;

				// Select this station if it matches the "-n" argument.
				if(NAZWA && (*NAZWA) == st.name) {
					selected_st = st_id;
				}

				// Update UI counter.
				counter = stations_ui_counter;
				counter = (counter + 1) % UI_COUNTER_MOD;
				stations_ui_counter = counter;
			}

			// Calculate new expiration time.
			exp_time = now;
			exp_time += DURATION_EXPIRE;

			// Update expiration time.
			old_time = expire_times[st_id];
			sorted_expire_times.erase(old_time);

			// Avoid conflicts.
			while(sorted_expire_times.find(exp_time) != sorted_expire_times.end()) {
				exp_time += EPSILON;
			}

			// Save expiration time.
			expire_times[st_id] = exp_time;  // overwrite existing value
			sorted_expire_times[exp_time] = st_id;  // add new value
		}
	}
}

void worker_send_ctrl_loop(shared_ptr<FileDescriptor> sock_ctrl)
{
	const char* lookup = "ZERO_SEVEN_COME_IN\n";

	// Join to broadcast if needed.
	if(is_correct_broadcast(DISCOVER_ADDR)) {
		join_broadcast(*sock_ctrl);
	}

	// Set destination address structure.
	ssize_t snd_len;
	sockaddr_in mcast_address;
	memset(&mcast_address, 0, sizeof(mcast_address));
	mcast_address.sin_family = AF_INET;  // IPv4.
	mcast_address.sin_addr.s_addr = inet_addr(DISCOVER_ADDR.c_str());
	mcast_address.sin_port = htons((uint16_t)CTRL_PORT);

	const size_t net_len = strlen(lookup) * sizeof(char);

	// Set default wait time.
	timespec t, t_remain, t_default;
	t_default.tv_nsec = 0;
	t_default.tv_sec = 5;

	while(true) {
		// 0. Check if it's time to stop.
		if(unlikely(terminate == true)) {
			return;
		}

		// 1. Wait.
		t = t_default;
		while(true) {
			rc = nanosleep(&t, &t_remain);
			if(rc < 0) {
				if(errno == EINTR) {
					t = t_remain;
					continue;
				}
				throw MyException("worker_send_ctrl_loop(): Error in nanosleep()");
			}
			break;
		}

		// 2. Send LOOKUP.
		while(true) {
			// Send packet.
			snd_len = sendto(*sock_ctrl, (void*)lookup, net_len, 0,
			                 (struct sockaddr*) &mcast_address, sizeof(mcast_address));

			if(unlikely(snd_len < 0)) {
				if(errno == EAGAIN || errno == EINTR) {
					continue;
				}
			}
			break;
		}

		// Check if successful.
		if(unlikely(snd_len < 0)) {
			cerr << "Error during sending LOOKUP packet! Continuing..." << endl;
		} else {
			if(DEB) {cerr << "worker_send_ctrl_loop(): sent LOOKUP " << endl;}
		}
	}
}

void worker_ui_loop(shared_ptr<FileDescriptor> sock_ui)
{
	constexpr int TIMEOUT = 300;  // Milliseconds.
	SpinlockMutexGuardB guard_stations_helper(mut_stations_helper, SpinlockMutexGuard::Action::None);
	SpinlockMutexGuardB guard_stations(mut_stations, SpinlockMutexGuard::Action::None);
	long long counter;
	deque<shared_ptr<Station>> stations_copy;
	string msg_text_clear;
	string msg_telnet_clear;
	string msg_telnet_start;
	string menu_text;
	string menu_telnet;
	vector<UIClient> connections;  // Connections with clients.
	vector<pollfd> poll_array;  // Array passed to poll().
	pollfd pollfd_tmp;
	UIClient client_tmp;
	int ret;
	int sock;
	size_t menu_selection;
	char byte;
	ssize_t code;

	// Initialise telnet commands.
	{
		// 27 = <ESC>
		// "[2J" = Erase Screen
		// "[{ROW};{COLUMN}H" = Cursor Home
		const char telnet_clear[] = {27, '[', '2', 'J', 27, '[', '1', ';', '1', 'H'};
		msg_telnet_clear = telnet_clear;

		// Empty line(s).
		const char text_clear[] = {'\n'};
		msg_text_clear = text_clear;

		// Commands:
		// 1. Suppress GO AHEAD messages.
		// 2. Switch client to 'character at the time' mode.
		// 3. Hide typed letters on client's side.
		// 4. End of line.
		const unsigned char telnet_start[] = {IAC, WILL, SGA, IAC, WONT, LINEMODE,
										      IAC, WILL, ECHO, '\n'};
		msg_telnet_start = (const char*)telnet_start;
	}

	// Set nonblocking mode on socket.
	set_nonblock(*sock_ui);

	// Listen for new connections.
	set_listen_tcp(*sock_ui);

	// Add listening socket to poll array.
	{
		pollfd_tmp.fd = *sock_ui;
		pollfd_tmp.events = POLLIN;
		pollfd_tmp.revents = 0;

		poll_array.push_back(pollfd_tmp);
	}

	// Initialise counter.
	counter = -1;

	// Initialise station selection.
	menu_selection = 0;

	while(true) {
		assert(connections.size() + 1 == poll_array.size());

		// Check if it's time to stop.
		if(unlikely(terminate == true)) {
			return;
		}

		// Update cached list of stations.
		if(counter != stations_ui_counter || menu_selection != selected_st) {
			// Station was added/deleted.
			if(counter != stations_ui_counter) {
				guard_stations_helper.lock();
				guard_stations.lock();

				stations_copy = stations_list;
				counter = stations_ui_counter;

				guard_stations.unlock();
				guard_stations_helper.unlock();

				// Sort cached list of stations.
				sort(begin(stations_copy), end(stations_copy),
					[](const auto& i, const auto& j) {
						// 1. Name, case insensitive compare.
						for(size_t c = 0; c < (*i).name.size() && c < (*j).name.size(); ++c) {
							if(tolower((*i).name[c]) != tolower((*j).name[c])) {
								return tolower((*i).name[c]) < tolower((*j).name[c]);
							}
						}
						if((*i).name.size() != (*j).name.size()) {
							return (*i).name.size() < (*j).name.size();
						}

						// 2. Name, case sensitive compare.
						for(size_t c = 0; c < (*i).name.size() && c < (*j).name.size(); ++c) {
							if((*i).name[c] != (*j).name[c]) {
								return (*i).name[c] < (*j).name[c];
							}
						}

						// 3. Data port.
						if((*i).data_port != (*j).data_port) {
							return (*i).data_port < (*j).data_port;
						}

						// 4. Multicast address.
						return (*i).mcast < (*j).mcast;
					});
			}

			// Get selected station.
			menu_selection = selected_st;

			// Generate string with menu.
			menu_text = generate_menu_text(stations_copy, menu_selection);
			menu_telnet = generate_menu_telnet(stations_copy, menu_selection);

			if(DEB) {cerr << menu_text;}

			// Make sure to update all clients.
			for(size_t i = 0; i < connections.size(); ++i) {
				poll_array[i + 1].events |= POLLOUT;

				if(connections[i].status == UIStatus::SendingUI) {
					connections[i].status = UIStatus::SendingCMD;
					connections[i].pos = 0;

					if(connections[i].mode == UIMode::Telnet) {
						connections[i].msg = (char*)msg_telnet_clear.c_str();
						connections[i].msg_len = msg_telnet_clear.size();
					} else if(connections[i].mode == UIMode::Text) {
						connections[i].msg = (char*)msg_text_clear.c_str();
						connections[i].msg_len = msg_text_clear.size();
					}
				} else if(connections[i].status == UIStatus::SendingCMD) {
					// Ignore.
				}
			}
		}

		// Poll.
		ret = poll(poll_array.data(), (nfds_t)poll_array.size(), TIMEOUT);

		// Timeout or interruption.
		if(ret == 0 || (ret < 0 && errno == EINTR)) {
			continue;
		}

		// Error.
		if(unlikely(ret < 0)) {
			if(errno == EINVAL) {
				cerr << "Exceeded the RLIMIT_NOFILE value! "
				        "Closing random UI connection..." << endl;

				if(!connections.empty()) {
					close_ui_connection(connections, poll_array, connections.size() - 1);
				}
				continue;
			}
			throw MyException("worker_ui_loop(): Error in poll()");
		}

		// New client.
		if(poll_array[0].revents & (POLLIN | POLLERR)) {
			sock = accept(*sock_ui, (struct sockaddr*)nullptr, (socklen_t*)nullptr);

			// Error.
			if(unlikely(sock < 0)) {
				if(errno != EAGAIN && errno != EWOULDBLOCK && errno != EINTR) {
					cerr << "Error during accepting new UI connection! "
					        "Continuing..." << endl;
				} else {
					if(DEB) {cerr << "worker_ui_loop(): accept() timeout" << endl;}
				}
			}
			// Success.
			else {
				if(DEB) {cerr << "worker_ui_loop(): NEW CLIENT!" << endl;}

				client_tmp.fd = make_shared<FileDescriptor>(sock);
				client_tmp.status = UIStatus::SendingCMD;
				client_tmp.msg = msg_telnet_start.data();
				client_tmp.msg_len = msg_telnet_start.size();
				client_tmp.pos = 0;
				client_tmp.last_bytes[0] = 0;
				client_tmp.last_bytes[1] = 0;
				client_tmp.last_bytes[2] = 0;
				client_tmp.mode = UIMode::Text;

				connections.push_back(client_tmp);

				// Release file descriptor from temporary struct.
				client_tmp.fd = nullptr;

				pollfd_tmp.fd = sock;
				pollfd_tmp.events = POLLIN | POLLOUT;
				pollfd_tmp.revents = 0;

				poll_array.push_back(pollfd_tmp);

				// Set nonblocking mode.
				try {
					set_nonblock(sock);
				} catch(MyException& e) {
					cerr << "Error during setting NONBLOCK on new UI connection! "
					        "Continuing..." << endl;
					close_ui_connection(connections, poll_array, connections.size() - 1);
				}
			}
		}

		// Get input from clients.
		for(size_t i = 0; i < connections.size(); ++i) {
			if(poll_array[i + 1].revents & (POLLIN | POLLERR)) {
				// Read 1 byte.
				code = read(poll_array[i + 1].fd, &byte, 1);

				// Success.
				if(likely(code > 0)) {
					connections[i].last_bytes[2] = connections[i].last_bytes[1];
					connections[i].last_bytes[1] = connections[i].last_bytes[0];
					connections[i].last_bytes[0] = (unsigned char) byte;

					// Try to interpret last 3 bytes.
					// Telnet command.
					if(connections[i].last_bytes[2] == IAC
						&& (
							connections[i].last_bytes[1] == DO
							|| connections[i].last_bytes[1] == DONT
							|| connections[i].last_bytes[1] == WILL
							|| connections[i].last_bytes[1] == WONT))
					{
						// Switch to telnet mode.
						if(connections[i].mode == UIMode::Text) {
							connections[i].mode = UIMode::Telnet;

							// Redraw menu in telnet mode.
							if(connections[i].status == UIStatus::SendingUI) {
								connections[i].status = UIStatus::SendingCMD;
								connections[i].msg = (char*)msg_telnet_clear.c_str();
								connections[i].msg_len = msg_telnet_clear.size();
								connections[i].pos = 0;
								poll_array[i + 1].events |= POLLOUT;
							}
						}

						// Client does not support important telnet options for us.
						if(connections[i].last_bytes[1] == DONT
						   && connections[i].last_bytes[0] == SGA)
						{
							cerr << "Client does not support (telnet) "
							        "'suppress go ahead'! "
									"Closing connection and continuing..." << endl;
							close_ui_connection(connections, poll_array, i);
							--i;
						} else if(connections[i].last_bytes[1] == DONT
						          && connections[i].last_bytes[0] == ECHO)
						{
							cerr << "Client does not support (telnet) 'echo'! "
									"Closing connection and continuing..." << endl;
							close_ui_connection(connections, poll_array, i);
							--i;
						} else {
							// Clear telnet command.
							connections[i].last_bytes[2] = 0;
							connections[i].last_bytes[1] = 0;
							connections[i].last_bytes[0] = 0;
						}
					}
					// Arrow UP (27, 91, 65).
					else if(connections[i].last_bytes[2] == 27
					        && connections[i].last_bytes[1] == 91
					        && connections[i].last_bytes[0] == 65)
					{
						// Change current station.
						arrow_up_ui(stations_copy);

						// Clear command.
						connections[i].last_bytes[2] = 0;
						connections[i].last_bytes[1] = 0;
						connections[i].last_bytes[0] = 0;
					}
					// Arrow DOWN (27, 91, 66).
					else if(connections[i].last_bytes[2] == 27
					        && connections[i].last_bytes[1] == 91
					        && connections[i].last_bytes[0] == 66)
					{
						// Change current station.
						arrow_down_ui(stations_copy);

						// Clear command.
						connections[i].last_bytes[2] = 0;
						connections[i].last_bytes[1] = 0;
						connections[i].last_bytes[0] = 0;
					}
				}
				// EOF.
				else if (code == 0) {
					close_ui_connection(connections, poll_array, i);
					--i;
				}
				// Error.
				else {
					if(errno != EAGAIN && errno != EINTR) {
						cerr << "Error during read() from client! "
						        "Closing connection and continuing..." << endl;
						close_ui_connection(connections, poll_array, i);
						--i;
					}
				}
			}
		}

		// Send output to clients.
		for(size_t i = 0; i < connections.size(); ++i) {
			if(poll_array[i + 1].revents & (POLLOUT)) {
				// Check if there is something to write.
				if(likely(connections[i].pos < connections[i].msg_len)) {
					// Send message.
					code = write(poll_array[i + 1].fd,
							connections[i].msg + connections[i].pos,
							connections[i].msg_len - connections[i].pos);

					// Success.
					if(likely(code >= 0)) {
						connections[i].pos += code;

						// Full message sent.
						if(connections[i].pos == connections[i].msg_len) {
							if(connections[i].status == UIStatus::SendingCMD) {
								connections[i].status = UIStatus::SendingUI;
								connections[i].pos = 0;

								if(connections[i].mode == UIMode::Text) {
									connections[i].msg = (char*) menu_text.c_str();
									connections[i].msg_len = menu_text.size();
								} else if(connections[i].mode == UIMode::Telnet) {
									connections[i].msg = (char*) menu_telnet.c_str();
									connections[i].msg_len = menu_telnet.size();
								}
							} else if(connections[i].status == UIStatus::SendingUI) {
								poll_array[i + 1].events &= ~(POLLOUT);
							}
						}
					}
					// Error.
					else {
						if(errno != EAGAIN && errno != EINTR) {
							cerr << "Error during write() to client! "
							        "Closing connection and continuing..." << endl;
							close_ui_connection(connections, poll_array, i);
							--i;
						}
					}
				} else {
					assert(false);
				}
			}
		}
	}
}

void worker_audio_loop(shared_ptr<FileDescriptor> sock_retr)
{
	constexpr auto MAX_AUDIO_PACKET_LEN = 65536;
	constexpr auto COUNTER_TERMINATE = 120;
	constexpr auto SLEEP_EMPTY_LIST = 1;  // Seconds.
	constexpr auto SLEEP_MCAST_ERROR = 1;  // Seconds.
	constexpr auto SLEEP_OPEN_PORT_ERROR = 1;  // Seconds.
	constexpr auto POLL_TIMEOUT = 700;  // Milliseconds.
	constexpr auto CHUNK_STDOUT = 64;  // Bytes.

	MemoryChunk<char> audio_buff(BSIZE);
	MemoryChunk<unsigned long long> audio_index(BSIZE);
	MemoryChunk<char> audio_packet(MAX_AUDIO_PACKET_LEN);
	AudioStatus status;
	int counter_terminate;
	long long counter_ui;
	bool forgot;
	SpinlockMutexGuardA guard_stations(mut_stations, SpinlockMutexGuard::Action::None);
	size_t selection;
	shared_ptr<Station> current_st(nullptr);
	shared_ptr<FileDescriptor> sock_audio(nullptr);
	timespec t, t_remain;
	optional<unsigned long long> byte0;
	optional<unsigned long long> session_id;
	uint64_t net_byte_nr;
	uint64_t net_session_id;
	unsigned long long next_byte_to_print;
	unsigned long long tmp_byte_nr;
	unsigned long long tmp_session_id;
	pollfd poll_array[3];
	int ret;
	size_t tmp_psize;
	optional<size_t> psize;
	ssize_t code;
	size_t offset;
	bool retransmission;
	size_t n;
	bool correct_bytes;
	// retr_bytes: byte_nr -> time.
	unordered_map<unsigned long long, chrono::time_point<chrono::system_clock>> retr_bytes;
	auto retr_bytes_it = retr_bytes.begin();
	// sorted_retr_bytes: time -> byte_nr (avoid conflicts).
	map<chrono::time_point<chrono::system_clock>, unsigned long long> sorted_retr_bytes;
	auto now = get_time();
	auto tmp_now = now;
	string msg_rexmit;
	sockaddr_in station_address;
	long timeout;
	auto tmp_time = now - now;

	// Initialise status.
	status = AudioStatus::Idle;

	// Set nonblocking mode.
	set_nonblock(*sock_retr);
	set_nonblock(STDOUT_FILENO);

	// Initialise counters.
	counter_ui = -1;
	counter_terminate = 0;

	// Set current playing station to none.
	selection = 0;

	// Initialise poll array.
	poll_array[0].fd = -1;  // sock_audio.
	poll_array[0].events = 0;
	poll_array[1].fd = -1;  // sock_retr.
	poll_array[1].events = 0;
	poll_array[2].fd = -1;  // stdout.
	poll_array[2].events = 0;

	// Initialise time.
	now = get_time();

	while(true) {
		// (Maybe) Check if it's time to terminate.
		if(counter_terminate > COUNTER_TERMINATE) {
			if(unlikely(terminate == true)) {
				return;
			}
			counter_terminate = 0;
		}

		// Update counter.
		++counter_terminate;

		// Station was added/removed. Check if current station is forgotten.
		if(counter_ui != stations_ui_counter) {
			counter_ui = stations_ui_counter;

			guard_stations.lock();
			if(unlikely(stations_list.empty())) {
				forgot = true;
			} else {
				// Found it.
				if(likely((*stations_list[0]).id == selection)) {
					forgot = false;
				}
				// Maybe it's somewhere in the list.
				else {
					forgot = true;
					for(size_t i = 0; i < stations_list.size(); ++i) {
						// Found it.
						if((*stations_list[i]).id == selection) {
							forgot = false;

							// Move to the beginning.
							swap(stations_list[0], stations_list[i]);
						}
					}
				}
			}
			guard_stations.unlock();

			// Select random station.
			if(unlikely(forgot)) {
				guard_stations.lock();
				if(!stations_list.empty()) {
					selected_st = (*stations_list[0]).id;
				} else {
					selected_st = 0;
				}
				guard_stations.unlock();
			}
		}

		// Get info about station.
		if(unlikely(selection != selected_st || selected_st == 0)) {
			// Cleanup of old station.
			status = AudioStatus::Idle;
			current_st.reset();
			sock_audio.reset();
			selection = 0;

			// Get info about station.
			guard_stations.lock();
			if(!stations_list.empty()) {
				for(size_t i = 0; i < stations_list.size(); ++i) {
					if((*stations_list[i]).id == selected_st) {
						selection = (*stations_list[i]).id;
						current_st = stations_list[i];
					}
				}
			}
			guard_stations.unlock();

			// List with stations is empty.
			if(selection == 0) {
				t.tv_sec = SLEEP_EMPTY_LIST;
				t.tv_nsec = 0;

				// Wait a little.
				while(true) {
					rc = nanosleep(&t, &t_remain);
					if(rc < 0) {
						if(errno == EINTR) {
							t = t_remain;
							continue;
						}
						throw MyException("worker_audio_loop(): Error in nanosleep()");
					}
					break;
				}

				// Repeat process.
				continue;
			}
		}

		// Prepare for receiving audio.
		if(unlikely(status == AudioStatus::Idle)) {
			// Open socket.
			try {
				sock_audio.reset();
				sock_audio = make_shared<FileDescriptor>(
						open_socket_udp((*current_st).data_port));
			} catch(exception &e) {
				cerr << "Error during opening audio socket! (port "
				     << (*current_st).data_port << ") Continuing..." << endl;
				sock_audio.reset();

				// Wait a little.
				t.tv_sec = SLEEP_OPEN_PORT_ERROR;
				t.tv_nsec = 0;
				while(true) {
					rc = nanosleep(&t, &t_remain);
					if(rc < 0) {
						if(errno == EINTR) {
							t = t_remain;
							continue;
						}
						throw MyException("worker_audio_loop(): Error in nanosleep()");
					}
					break;
				}

				// Repeat process.
				continue;
			}

			// Join multicast.
			try {
				join_multicast(*sock_audio, (*current_st).mcast);
			} catch(exception &e) {
				cerr << "Error during joining to MCAST_ADDR! ("
				     << (*current_st).mcast << ") Continuing..."<< endl;
				sock_audio.reset();

				// Wait a little.
				t.tv_sec = SLEEP_MCAST_ERROR;
				t.tv_nsec = 0;
				while(true) {
					rc = nanosleep(&t, &t_remain);
					if(rc < 0) {
						if(errno == EINTR) {
							t = t_remain;
							continue;
						}
						throw MyException("worker_audio_loop(): Error in nanosleep()");
					}
					break;
				}

				// Repeat process.
				continue;
			}

			// Set up address for retransmission.
			station_address.sin_family = AF_INET;
			station_address.sin_addr.s_addr = (*current_st).address;
			station_address.sin_port = htons((uint16_t) CTRL_PORT);

			// Clear buffer.
			audio_buff.zero();
			audio_index.zero();

			// Clear retransmissions.
			retr_bytes.clear();
			sorted_retr_bytes.clear();

			// Clear session info.
			byte0 = nullopt;
			session_id = nullopt;
			psize = nullopt;

			// Set up poll array.
			poll_array[0].fd = *sock_audio;
			poll_array[0].events = POLLIN;
			poll_array[1].fd = -1;
			poll_array[1].events = 0;
			poll_array[2].fd = -1;
			poll_array[2].events = 0;

			// Change status to downloading.
			status = AudioStatus::Downloading;
		}

		assert(sock_audio);
		assert(poll_array[0].fd != -1
		       || poll_array[1].fd != -1
		       || poll_array[2].fd != -1);

		// Maybe we can send retransmission.
		if(!sorted_retr_bytes.empty()) {
			if(sorted_retr_bytes.begin()->first < now) {
				poll_array[1].fd = *sock_retr;
				poll_array[1].events = POLLOUT;

				timeout = POLL_TIMEOUT;
			} else {
				poll_array[1].fd = -1;
				poll_array[1].events = 0;

				// Set shorter timeout to add sock_retr to poll_array.
				tmp_time = (sorted_retr_bytes.begin()->first) - now;
				timeout = (chrono::duration_cast<chrono::milliseconds>(tmp_time)).count() + 1;

				assert(timeout >= 0);
			}
		} else {
			poll_array[1].fd = -1;
			poll_array[1].events = 0;

			timeout = POLL_TIMEOUT;
		}

		// Poll.
		ret = poll(poll_array, sizeof(poll_array) / sizeof(poll_array[0]), (int)timeout);

		// Timeout or interruption.
		if(ret == 0 || (ret < 0 && errno == EINTR)) {
			continue;
		}

		// Error.
		if(unlikely(ret < 0)) {
			throw MyException("worker_audio_loop(): Error in poll()");
		}

		// Save current time.
		now = get_time();

		// Check audio socket.
		if(poll_array[0].revents & (POLLIN | POLLERR)) {
			code = recv(*sock_audio, audio_packet.ptr(), audio_packet.size(), 0);

			// Error.
			if(unlikely(code < 0)) {
				if(errno != EAGAIN && errno != EINTR) {
					throw("worker_audio_loop(): Error in recv()");
				}
			}
			// Success. Check minimum size and parse audio packet.
			else if(likely((size_t)code >= sizeof(net_session_id) + sizeof(net_byte_nr))) {
				// Retransmission is allowed.
				retransmission = true;

				if(DEB) {
					if((size_t)code >= audio_packet.size()) {
						cerr << "worker_audio_loop(): received audio packet may "
						        "be truncated!" << endl;
					}
				}

				offset = 0;

				// Copy session id.
				memcpy(&net_session_id, audio_packet.ptr() + offset, sizeof(net_session_id));
				offset += sizeof(net_session_id);
				tmp_session_id = be64toh(net_session_id);

				// Copy byte number.
				memcpy(&net_byte_nr, audio_packet.ptr() + offset, sizeof(net_byte_nr));
				offset += sizeof(net_byte_nr);
				tmp_byte_nr = be64toh(net_byte_nr);

				// Calculate psize.
				tmp_psize = ((size_t)code) - sizeof(net_session_id) - sizeof(net_byte_nr);

				// Set byte0.
				if(unlikely(!byte0)) {
					byte0 = tmp_byte_nr;

					if(DEB) {cerr << "worker_audio_loop(): byte0 = " << *byte0 << endl;}

					// Save which byte print to stdout.
					next_byte_to_print = tmp_byte_nr;

					// Turn off retransmissions on first audio packet.
					retransmission = false;
				}

				// Set psize.
				if(unlikely(!psize)) {
					psize = tmp_psize;
					if(DEB) {cerr << "worker_audio_loop(): psize = " << *psize << endl;}
				} else {
					if(DEB) {
						if(*psize != tmp_psize) {
							cerr << "worker_audio_loop(): inconsistent PSIZE "
							        "in audio packet" << endl;
						}
					}
				}

				// Set session id.
				if(unlikely(!session_id)) {
					session_id = tmp_session_id;
					if(DEB) {cerr << "worker_audio_loop(): session_id = " << *session_id << endl;}
				}
				// Restart playing.
				else if(tmp_session_id > *session_id) {
					status = AudioStatus::Idle;
				}

				// Check if psize is not too big.
				if(tmp_psize > (size_t)BSIZE) {
					cerr << "PSIZE is bigger than BSIZE! Truncating audio packet and continuing...";
					tmp_psize = (size_t)BSIZE;
				}

				// Copy audio.
				if(tmp_session_id == *session_id && tmp_byte_nr >= next_byte_to_print) {
					// First part
					const unsigned long long first = min(
							(unsigned long long)tmp_psize,
					        (unsigned long long)BSIZE - (tmp_byte_nr % BSIZE));

					// Copy audio
					memcpy(audio_buff.ptr() + (tmp_byte_nr % BSIZE),
					       audio_packet.ptr() + offset,
					       first);

					// Set byte numbers
					for(size_t j = 0; j < first; ++j) {
						audio_index.ptr()[(tmp_byte_nr % BSIZE) + j] = tmp_byte_nr + j;
					}

					// Second part
					const unsigned long long second = min(
							(unsigned long long)(tmp_psize - first),
							(unsigned long long)BSIZE);

					if(first < tmp_psize) {
						// Copy audio
						memcpy(audio_buff.ptr(),
						       audio_packet.ptr() + offset + first,
						       second);

						// Set byte numbers
						for(size_t j = 0; j < second; ++j) {
							audio_index.ptr()[j] = tmp_byte_nr + first + j;
						}
					}

					// Check if we can start playing.
					if(status == AudioStatus::Downloading) {
						if((tmp_byte_nr + tmp_psize) >= *byte0 + (BSIZE / 4) * 3) {
							if(DEB) {cerr << "worker_audio_loop(): START STDOUT" << endl;}

							status = AudioStatus::Playing;
							poll_array[2].fd = STDOUT_FILENO;
							poll_array[2].events = POLLOUT;
							poll_array[2].revents = 0;
						}
					}

					// Delete this byte from retransmission queue.
					if(likely(retransmission)) {
						retr_bytes_it = retr_bytes.find(tmp_byte_nr);

						if(retr_bytes_it != retr_bytes.end()) {
							sorted_retr_bytes.erase(retr_bytes_it->second);
							retr_bytes.erase(retr_bytes_it);
						}
					}

					// Add bytes to retransmission queue.
					if(likely(retransmission)) {
						tmp_now = now;

						if(likely(tmp_byte_nr >= *psize)) {
							tmp_byte_nr = tmp_byte_nr - *psize;
						}

						while(audio_index.ptr()[tmp_byte_nr % BSIZE] != tmp_byte_nr
						      && tmp_byte_nr >= next_byte_to_print)
						{
							// Add to list.
							add_byte_retr(retr_bytes,
							              sorted_retr_bytes,
							              tmp_byte_nr,
							              tmp_now);

							tmp_byte_nr = tmp_byte_nr - *psize;
						}
					}
				}
			}
		}

		// Check retransmission socket.
		if(poll_array[1].revents & (POLLOUT | POLLERR)) {
			if(likely(!sorted_retr_bytes.empty())) {
				msg_rexmit = generate_rexmit(retr_bytes, sorted_retr_bytes, now);

				if(DEB) {
					if(msg_rexmit.size() >= 10000) {
						cerr << "worker_audio_loop(): debug info: REXMIT is long ("
						     << msg_rexmit.size() << " bytes)" << endl;
					}
				}

				code = sendto(*sock_retr, msg_rexmit.c_str(), msg_rexmit.size(), 0,
				                 (struct sockaddr*)&station_address, sizeof(station_address));

				if(DEB) {cerr << "worker_audio_loop(): sent " << msg_rexmit;}

				if(unlikely((size_t)code != msg_rexmit.size())) {
					cerr << "Error during sending REXMIT packet!"
					        " Continuing..." << endl;
				}
			}
		}

		// Check stdout socket.
		if(poll_array[2].revents & (POLLOUT | POLLERR)) {
			offset = next_byte_to_print % BSIZE;
			n = min((size_t)CHUNK_STDOUT, BSIZE - offset);

			// Check if correct next bytes.
			correct_bytes = true;
			for(size_t j = 0; j < n; ++j) {
				if(audio_index.ptr()[offset + j] != next_byte_to_print + j) {
					correct_bytes = false;
					if(DEB) {
						cerr << "worker_audio_loop(): wrong / missing byte "
						     << next_byte_to_print + j
						     << " in buffer for stdout" << endl;
					}
					break;
				}
			}

			if(correct_bytes) {
				code = write(STDOUT_FILENO, audio_buff.ptr() + offset, n);

				// Error.
				if(unlikely(code < 0)) {
					if(errno != EAGAIN && errno != EINTR) {
						throw MyException("worker_audio_loop(): Error in write() to stdout");
					}
				}
				// Success.
				else {
					next_byte_to_print += code;
				}
			} else {
				// Restart playing.
				status = AudioStatus::Idle;
			}
		}
	}
}

void worker_bootstrap(function<void()> f)
{
	try {
		f();
	} catch(exception &e) {
		rc = 1;
		cerr << e.what() << endl;
	} catch(...) {
		rc = 1;
		cerr << "Exception of unknown type!" << endl;
	}

	terminate = true;

	return;
}

int main(int argc, char** argv)
{
	shared_ptr<FileDescriptor> sock_ctrl(nullptr);
	shared_ptr<FileDescriptor> sock_ui(nullptr);
	shared_ptr<FileDescriptor> sock_retr(nullptr);

	// Parse arguments.
	try {
		// Optional arguments.
		string nazwa;
		po::options_description desc_opt("Optional arguments");
		desc_opt.add_options()
				("help,h", "show help message")
				(",d", po::value<string>(&DISCOVER_ADDR)
				        ->default_value(sikradio::RECEIVER_DISCOVER_ADDR)
						->value_name("IPV4"), "DISCOVER_ADDR")
				(",C", po::value<unsigned int>(&CTRL_PORT)
				        ->default_value(sikradio::RECEIVER_CTRL_PORT)
						->value_name("PORT"), "CTRL_PORT")
				(",U", po::value<unsigned int>(&UI_PORT)
				        ->default_value(sikradio::RECEIVER_UI_PORT)
						->value_name("PORT"), "UI_PORT")
				(",b", po::value<unsigned long>(&BSIZE)
				        ->default_value(sikradio::RECEIVER_BSIZE)
						->value_name("SIZE"), "BSIZE (bytes)")
				(",R", po::value<unsigned long>(&RTIME)
				        ->default_value(sikradio::RECEIVER_RTIME)
						->value_name("TIME"), "RTIME (milliseconds)")
				(",n", po::value<string>(&nazwa)
				        ->value_name("NAME"), "NAZWA")
				;

		// Optional arguments.
		po::options_description all("sikradio-receiver");
		all.add(desc_opt);

		// Parse arguments.
		po::variables_map vm;
		po::store(po::parse_command_line(argc, argv, all), vm);

		// Show help.
		if(vm.count("help")) {
			cout << all << endl;
			return 0;
		}

		// Raise errors if any.
		po::notify(vm);

		// Save argument NAZWA.
		if(vm.count("-n")) {
			NAZWA = nazwa;
		}

		if(DEB) {
			cerr << "Parsed arguments:" << endl
			     << "  DISCOVER_ADDR: " << DISCOVER_ADDR << endl
			     << "  CTRL_PORT: " << CTRL_PORT << endl
			     << "  UI_PORT: " << UI_PORT << endl
			     << "  BSIZE: " << BSIZE << endl
			     << "  RTIME: " << RTIME << endl
			     << "  NAZWA: " << (NAZWA ? (*NAZWA) : string("✘")) << endl;
		}

		// Check correctness of ip address.
		require_correct_ipv4_address(DISCOVER_ADDR);

		// Check correctness of ports.
		require_correct_port(CTRL_PORT);
		require_correct_port(UI_PORT);

		// Avoid problems during calculations with BSIZE.
		if(BSIZE < 2) {
			throw MyException("BSIZE must be >=2!");
		}

		// This case will cause problem in bind().
		if(CTRL_PORT == UI_PORT) {
			cerr << "Note: UI_PORT = CTRL_PORT may cause problems!" << endl;
		}

	} catch(exception &e) {
		cerr << e.what() << endl;
		return 1;
	} catch(...) {
		cerr << "Exception of unknown type!" << endl;
		return 1;
	}

	// Do basic initialisation.
	try {
		// Open sockets
		// Socket is used to accept connections to UI.
		sock_ui = make_shared<FileDescriptor>(open_socket_tcp(UI_PORT));
		// Socket is used to receive REPLY packets and send LOOKUP packets.
		sock_ctrl = make_shared<FileDescriptor>(open_socket_udp());
		// Socket is used to send REXMIT packets.
		sock_retr = make_shared<FileDescriptor>(open_socket_udp());

		// Set default return code.
		rc = 0;

		// Set to not end threads now.
		terminate = false;

		// Select none station.
		selected_st = 0;

		// Set ui_counter.
		stations_ui_counter = 0;

	} catch(exception &e) {
		cerr << e.what() << endl;
		return 1;
	} catch(...) {
		cerr << "Exception of unknown type!" << endl;
		return 1;
	}

	// Run threads.
	{
		boost::thread_group tg;

		if(DEB) {cerr << "Starting threads" << endl;}

		tg.create_thread(boost::bind(&worker_bootstrap,
		                             [&sock_ctrl](){worker_recv_ctrl_loop(sock_ctrl);}));
		tg.create_thread(boost::bind(&worker_bootstrap,
		                             [&sock_ctrl](){worker_send_ctrl_loop(sock_ctrl);}));
		tg.create_thread(boost::bind(&worker_bootstrap,
		                             [&sock_ui](){worker_ui_loop(sock_ui);}));
		tg.create_thread(boost::bind(&worker_bootstrap,
		                             [&sock_retr](){worker_audio_loop(sock_retr);}));

		tg.join_all();
	}

	if(DEB) {cerr << "Finished?" << endl;}

	return rc;
}
