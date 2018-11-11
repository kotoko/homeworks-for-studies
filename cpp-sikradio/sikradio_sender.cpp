#include <arpa/inet.h>
#include <atomic>
#include <boost/optional.hpp>
#include <boost/program_options.hpp>
#include <boost/thread.hpp>
#include <cassert>
#include <cstddef>
#include <deque>
#include <exception>
#include <iostream>
#include <netdb.h>
#include <netinet/in.h>
#include <queue>
#include <string>
#include <sys/types.h>
#include <sys/socket.h>
#include <unordered_map>

#include "default_values.hpp"
#include "file_descriptor.hpp"
#include "memory_chunk.hpp"
#include "optimizations.hpp"
#include "spinlock_mutex.hpp"
#include "utility.hpp"


using boost::optional;
using std::atomic_bool;
using std::atomic_int;
using std::cerr;
using std::cout;
using std::deque;
using std::endl;
using std::exception;
using std::function;
using std::memcpy;
using std::min;
using std::move;
using std::queue;
using std::shared_ptr;
using std::size_t;
using std::string;
using std::strncmp;
using std::to_string;
using std::unordered_map;
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

	struct Rexmit
	{
		sockaddr_in client;
		queue<unsigned long long> first_bytes;
	};

	string MCAST_ADDR;
	unsigned int CTRL_PORT;
	unsigned int DATA_PORT;
	unsigned long PSIZE;  // Bytes.
	unsigned long FSIZE;  // Bytes.
	unsigned long RTIME;  // Milliseconds.
	string NAZWA;

	unsigned long long session_id;

	atomic_int rc;  // Return code.
	atomic_bool terminate;  // Is it time to stop?

	queue<Rexmit> rexmit_requests;
	SpinlockMutex mut_rexmit;  // For: rexmit_requests.

	MemoryChunk<char> audio_buffer;
	MemoryChunk<unsigned long long> audio_index;
	SpinlockMutex mut_audio;  // For: audio_buffer, audio_index.

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
		throw MyException("open_socket_udp(): Error in socket()!");
	}

	// Bind the socket to a concrete address.
	if(bind(sock, (struct sockaddr *) &address, (socklen_t) sizeof(address)) < 0) {
		close(sock);
		throw MyException("open_socket_udp(): Error in bind()!");
	}

	return sock;
}

void join_broadcast(int sock)
{
	int enable = 1;

	if(setsockopt(sock, SOL_SOCKET, SO_BROADCAST, &enable, sizeof(enable)) < 0) {
		throw MyException("join_broadcast(): Error in setsockopt()");
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

void require_correct_multicast(const string& s)
{
	if(!is_correct_ipv4(s)) {
		throw MyException("Given ip address is not a valid ipv4 address!");
	}

	if(!is_correct_multicast(s)) {
		throw MyException("Given ip address is not in multicast range!");
	}
}

void require_correct_port(unsigned int p)
{
	if(!is_correct_port(p)) {
		throw MyException("Given port is not in range 1-65535!");
	}
}

void require_correct_name(const string& s)
{
	if(s.length() > 64) {
		throw MyException("Too long station name!");
	}

	for(const auto& c : s) {
		if((int)c < 32) {
			throw MyException("Illegal character(s) in station name!");
		}
	}
}

void worker_audio_loop(shared_ptr<FileDescriptor> sock_audio)
{
	bool eof = false;
	MemoryChunk<char> net_bytes(PSIZE + 2 * (sizeof(uint64_t) / sizeof(char)));
	MemoryChunk<char> buff(PSIZE);
	size_t offset;
	ssize_t read_len;
	ssize_t snd_len;
	unsigned long long next_byte = 0;
	const uint64_t net_session_id = htobe64(session_id);
	sockaddr_in snd_address;
	int counter = 0;
	constexpr int COUNTER_LIMIT = 1000;

	// Set address structure.
	memset(&snd_address, 0, sizeof(snd_address));
	snd_address.sin_family = AF_INET;
	snd_address.sin_addr.s_addr = inet_addr(MCAST_ADDR.c_str());
	snd_address.sin_port = htons((uint16_t)DATA_PORT);

	// Get mutex guard.
	SpinlockMutexGuardA guard_audio(mut_audio, SpinlockMutexGuard::Action::None);

	while(likely(!eof)) {
		// (Maybe) Check if it's time to terminate.
		if(counter > COUNTER_LIMIT) {
			if(unlikely(terminate == true)) {
				return;
			}
			counter = 0;
		}

		// Reset offset.
		offset = 0;

		// Read one chunk of data.
		while(offset < PSIZE) {
			read_len = read(STDIN_FILENO, buff.ptr() + offset, PSIZE - offset);

			if(unlikely(read_len < 0)) {
				if(errno == EAGAIN || errno == EINTR) {
					continue;
				}
				throw MyException("worker_audio_loop(): Error in read()");
			}

			// EOF.
			if(unlikely(read_len == 0)) {
				eof = true;
				break;
			}

			// Read something successfully.
			offset += read_len;
		}

		// Send data if the buffer is full.
		if(likely(offset == PSIZE)) {
			const uint64_t net_next_byte = htobe64(next_byte);

			// Copy session id.
			memcpy((void*)net_bytes.ptr(), (void*)&net_session_id, sizeof(net_session_id));

			// Copy first byte number.
			memcpy((void*)(net_bytes.ptr() + sizeof(net_session_id)),
			       (void*)&net_next_byte, sizeof(net_next_byte));

			// Copy audio data.
			memcpy((void*)(net_bytes.ptr() + sizeof(net_session_id) + sizeof(net_next_byte)),
			       (void*)buff.ptr(), buff.size());

			// Send packet.
			while(true) {
				snd_len = sendto(*sock_audio, (void*)net_bytes.ptr(), net_bytes.size(), 0,
				                 (struct sockaddr*)&snd_address, sizeof(snd_address));

				if(unlikely(snd_len < 0)) {
					if(errno == EAGAIN || errno == EINTR) {
						continue;
					}
				}
				break;
			}

			// Check if successful.
			if(unlikely(snd_len < 0)) {
				cerr << "Error during sending audio packet! Continuing..." << endl;
			}

			// Cache audio data.
			guard_audio.lock();

			const unsigned long long first = min(
					(unsigned long long)PSIZE,
					(unsigned long long)FSIZE - (next_byte % FSIZE)
			);

			// First part
			memcpy(audio_buffer.ptr() + (next_byte % FSIZE), buff.ptr(), first);
			memset_u_long_long(audio_index.ptr() + (next_byte % FSIZE), next_byte, first);

			// Second part
			if(first < PSIZE) {
				memcpy(audio_buffer.ptr(), buff.ptr() + first, PSIZE - first);
				memset_u_long_long(audio_index.ptr(), next_byte, PSIZE - first);
			}

			guard_audio.unlock();

			// Update next byte counter.
			next_byte += PSIZE;
		}

		// Update counter.
		++counter;
	}
}

void handle_ctrl_request(
		shared_ptr<FileDescriptor>& sock_ctrl,
		MemoryChunk<char>& packet,
		size_t packet_size,
		sockaddr_in &orig_address)
{
	thread_local static MemoryChunk<char> tmp(200);
	thread_local static size_t msg_len = 0;
	thread_local static const string msg_zero_seven = "ZERO_SEVEN_COME_IN";
	thread_local static const string msg_louder = "LOUDER_PLEASE ";

	// Generate REPLY message.
	if(unlikely(msg_len == 0)) {
		const string msg = "BOREWICZ_HERE " + MCAST_ADDR + " " + to_string(DATA_PORT)
		                       + " " + NAZWA + "\n";
		if(tmp.length() < msg.size()) {
			throw MyException("handle_ctrl_request(): Too small hardcoded buffer!");
		}

		memcpy((void*)tmp.ptr(), (void*)msg.c_str(), msg.size());

		msg_len = msg.size();
	}

	sockaddr_in client_address = orig_address;

	// LOOKUP message (ZERO_SEVEN_COME_IN).
	if(packet_size >= 18
	   && strncmp(msg_zero_seven.c_str(), packet.ptr(), msg_zero_seven.length()) == 0)
	{
		if(DEB) {cerr << "handle_ctrl_request(): ZERO_SEVEN_COME_IN" << endl;}

		// Send message.
		ssize_t snd_len;
		while(true) {
			snd_len = sendto(*sock_ctrl, (void*)tmp.ptr(), msg_len, 0,
			                 (struct sockaddr *)&client_address, sizeof(client_address));

			if(unlikely(snd_len < 0)) {
				if(errno == EAGAIN || errno == EINTR) {
					continue;
				}
			}
			break;
		}

		if(DEB) {cerr << "handle_ctrl_request(): sent REPLY" << endl;}

		// Check if successful.
		if(unlikely((size_t)snd_len != msg_len)) {
			cerr << "Error during sending REPLY message (BOREWICZ_HERE...)! "
			        "Continuing..." << endl;
		}

		return;
	}
	// REXMIT message (LOUDER_PLEASE).
	else if(packet_size >= 15
	        && strncmp(msg_louder.c_str(), packet.ptr(), msg_louder.length()) == 0)
	{
		// Shortest message: LOUDER_PLEASE + _ + [0-9]

		unsigned long long byte_nr;
		size_t i;
		Rexmit r;

		r.client = orig_address;

		i = 14;

		// Ignore totally invalid request.
		if(packet.ptr()[i] < '0' || packet.ptr()[i] > '9') {
			return;
		}

		if(DEB) {cerr << "handle_ctrl_request(): valid LOUDER_PLEASE" << endl;}

		while(i < packet_size) {
			// Parse number.
			if(packet.ptr()[i] < '0' || packet.ptr()[i] > '9') {
				break;
			}
			errno = 0;
			byte_nr = strtoull(packet.ptr() + i, NULL, 10);

			// Number is not valid.
			if(errno == ERANGE) {
				break;
			}

			// Save number.
			r.first_bytes.push(byte_nr);

			// Go to next token to parse.
			while(i < packet_size && packet.ptr()[i] >= '0' && packet.ptr()[i] <= '9') {
				++i;
			}

			// Check if we want to search for next number.
			if(i < packet_size && packet.ptr()[i] == ',') {
				++i;
				continue;
			}

			break;
		}

		// Get mutex guard.
		SpinlockMutexGuardA guard_rexmit(mut_rexmit, SpinlockMutexGuard::Action::None);

		// Copy from temporary variable to global list.
		guard_rexmit.lock();
		rexmit_requests.push(move(r));
		guard_rexmit.unlock();

		return;
	}
}

void worker_ctrl_loop(shared_ptr<FileDescriptor> sock_ctrl)
{
	constexpr auto MAX_PACKET_CTRL_LEN = 65536;  // Bytes.

	sockaddr_in client_address;
	socklen_t rcva_len;
	ssize_t size;

	// Set timeout on socket.
	set_timeout(*sock_ctrl, 2);

	MemoryChunk<char> packet_ctrl(MAX_PACKET_CTRL_LEN);
	bool timeout = false;
	int counter = 0;
	constexpr int COUNTER_LIMIT = 10;  // Number of packets without checking.

	while(true) {
		// (Maybe) Check if it's time to terminate.
		if(counter > COUNTER_LIMIT || timeout) {
			if(unlikely(terminate == true)) {
				return;
			}
			counter = 0;
		}

		// Read packet.
		rcva_len = (socklen_t) sizeof(client_address);
		size = recvfrom(*sock_ctrl, (void*)packet_ctrl.ptr(), packet_ctrl.size(), 0,
		               (struct sockaddr*)&client_address, &rcva_len);

		timeout = false;
		++counter;

		// Check if successful.
		if(unlikely(size < 0)) {
			if(errno == EAGAIN || errno == EINTR) {
				if(errno == EAGAIN) {
					timeout = true;
				}
				continue;
			}

			throw MyException("worker_ctrl_loop(): Error in recvfrom()");
		}

		// Do something.
		handle_ctrl_request(sock_ctrl, packet_ctrl, (size_t)size, client_address);
	}
}

void worker_retr_loop(shared_ptr<FileDescriptor> sock_retr)
{
	// How many bytes send from one packet request in one loop iteration?
	constexpr int LIMIT_PER_REQUEST = 25;
	timespec t, t_remain, t_default;
	int rc;
	SpinlockMutexGuardB guard_audio(mut_audio, SpinlockMutexGuard::Action::None);
	SpinlockMutexGuardB guard_rexmit(mut_rexmit, SpinlockMutexGuard::Action::None);
	queue<Rexmit> q;
	unordered_map<unsigned long long, bool> sent_bytes;
	MemoryChunk<unsigned long long> audio_index_copy(FSIZE);
	MemoryChunk<char> audio_buffer_copy(FSIZE);
	MemoryChunk<char> net_bytes(PSIZE + 2 * (sizeof(uint64_t) / sizeof(char)));
	Rexmit r;
	bool all;
	int i;
	unsigned long long byte_nr;
	const uint64_t net_session_id = htobe64(session_id);
	ssize_t snd_len;
	sockaddr_in snd_address;

	// Convert milliseconds to nanoseconds + seconds.
	t_default.tv_nsec = ((RTIME % 1000) * 1000000) % 1000000000;
	t_default.tv_sec = RTIME / 1000;

	// Set address structure.
	memset(&snd_address, 0, sizeof(snd_address));
	snd_address.sin_family=AF_INET;
	snd_address.sin_addr.s_addr = inet_addr(MCAST_ADDR.c_str());
	snd_address.sin_port = htons((uint16_t)DATA_PORT);

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
				if(errno == EINTR){
					t = t_remain;
					continue;
				}
				throw MyException("worker_retr_loop(): Error in nanosleep()");
			}

			break;
		}

		// 2. Retransmit
		// Get queue of requests
		guard_rexmit.lock();
		q.swap(rexmit_requests);
		guard_rexmit.unlock();

		// Reset cache.
		sent_bytes.clear();

		// Check if there is something to do.
		if(!q.empty()) {
			// Get copy of the current audio buffer.
			guard_audio.lock();
			audio_buffer_copy.copy(audio_buffer);
			audio_index_copy.copy(audio_index);
			guard_audio.unlock();

			while(!q.empty()) {
				// Get requests list.
				r = move(q.front());
				q.pop();

				all = true;
				i = 0;

				while(!r.first_bytes.empty()) {
					if(i == LIMIT_PER_REQUEST) {
						all = false;
						break;
					}

					// Get single request.
					byte_nr = r.first_bytes.front();
					r.first_bytes.pop();

					// Send package.
					if(audio_index_copy.ptr()[byte_nr % FSIZE] == byte_nr
					   && sent_bytes.find(byte_nr) == sent_bytes.end()) {
						const uint64_t net_byte_nr = htobe64(byte_nr);
						const unsigned long long first = (unsigned long long) min(
								(unsigned long long)PSIZE,
								(unsigned long long)FSIZE - (byte_nr % (unsigned long long)FSIZE)
						);

						// Copy session id.
						memcpy((void*)net_bytes.ptr(),
						       (void*)&net_session_id,
						       sizeof(net_session_id));

						// Copy first byte number.
						memcpy((void*)(net_bytes.ptr() + sizeof(net_session_id)),
						       (void*)&net_byte_nr,
						       sizeof(net_byte_nr));

						// Copy audio data.
						// First part.
						memcpy((void*)(net_bytes.ptr() + sizeof(net_session_id) + sizeof(net_byte_nr)),
						       (void*)(audio_buffer_copy.ptr() + (byte_nr % FSIZE)),
						       first);

						// Second part.
						if(first < PSIZE) {
							memcpy((void*)(net_bytes.ptr() + sizeof(net_session_id) + sizeof(net_byte_nr) + first),
							       (void*)(audio_buffer_copy.ptr() + first),
							       PSIZE - first);
						}

						// Send packet.
						while(true) {
							snd_len = sendto(*sock_retr, (void*)net_bytes.ptr(), net_bytes.size(), 0,
							                 (struct sockaddr*) &snd_address, sizeof(snd_address));

							if(unlikely(snd_len < 0)) {
								if(errno == EAGAIN || errno == EINTR) {
									continue;
								}
							}
							break;
						}

						// Check if successful.
						if(unlikely(snd_len < 0)) {
							cerr << "Error during sending retransmission audio packet! "
							        "Continuing..." << endl;
						}
						else {
							sent_bytes[byte_nr] = true;
						}

						if(DEB) {
							cerr << "worker_retr_loop(): sent RETRANSMISSION: "
							     << byte_nr << endl;
						}
					} else {
						if(DEB) {
							if(audio_index_copy.ptr()[byte_nr % FSIZE] != byte_nr) {
								cerr << "worker_retr_loop(): ignoring RETRANSMISSION: "
								     << byte_nr << endl;
							}
						}
					}

					// Update counter.
					++i;
				}

				// Add to the end of queue if something left.
				if(!all) {
					q.push(move(r));
				}
			}
		}

		assert(q.empty());
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
	shared_ptr<FileDescriptor> sock_audio(nullptr);
	shared_ptr<FileDescriptor> sock_retr(nullptr);
	shared_ptr<FileDescriptor> sock_ctrl(nullptr);

	// Parse arguments.
	try {
		// Required arguments.
		po::options_description desc_req("Required arguments");
		desc_req.add_options()
			(",a", po::value<string>(&MCAST_ADDR)->required()
					->value_name("IPV4"), "MCAST_ADDR")
			;

		// Optional arguments.
		po::options_description desc_opt("Optional arguments");
		desc_opt.add_options()
			("help,h", "show help message")
			(",P", po::value<unsigned int>(&DATA_PORT)
			        ->default_value(sikradio::SENDER_DATA_PORT)
					->value_name("PORT"), "DATA_PORT")
			(",C", po::value<unsigned int>(&CTRL_PORT)
			        ->default_value(sikradio::SENDER_CTRL_PORT)
					->value_name("PORT"), "CTRL_PORT")
			(",p", po::value<unsigned long>(&PSIZE)
			        ->default_value(sikradio::SENDER_PSIZE)
					->value_name("SIZE"), "PSIZE (bytes)")
			(",f", po::value<unsigned long>(&FSIZE)
			        ->default_value(sikradio::SENDER_FSIZE)
					->value_name("SIZE"), "FSIZE (bytes)")
			(",R", po::value<unsigned long>(&RTIME)
			        ->default_value(sikradio::SENDER_RTIME)
					->value_name("TIME"), "RTIME (milliseconds)")
			(",n", po::value<string>(&NAZWA)
			        ->default_value(sikradio::SENDER_NAZWA)
					->value_name("NAME"), "NAZWA")
			;

		// Required + optional arguments.
		po::options_description all("sikradio-sender");
		all.add(desc_req).add(desc_opt);

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

		if(DEB) {
			cerr << "Parsed arguments:" << endl
			     << "  MCAST_ADDR: " << MCAST_ADDR << endl
			     << "  CTRL_PORT: " << CTRL_PORT << endl
			     << "  DATA_PORT: " << DATA_PORT << endl
			     << "  PSIZE: " << PSIZE << endl
			     << "  FSIZE: " << FSIZE << endl
			     << "  RTIME: " << RTIME << endl
			     << "  NAZWA: " << NAZWA << endl;
		}

		// Make sure that the audio packet contains audio byte(s).
		if(PSIZE < 1) {
			throw MyException("PSIZE must be >=1!");
		}

		// Make sure that the buffer is at least the size of audio packet.
		if(FSIZE < PSIZE) {
			throw MyException("FSIZE must be >= PSIZE!");
		}

		// Check correctness of ip address.
		require_correct_multicast(MCAST_ADDR);

		// Check correctness of ports.
		require_correct_port(CTRL_PORT);
		require_correct_port(DATA_PORT);

		// Check correctness of station name.
		require_correct_name(NAZWA);

		// This case will cause problem in bind().
		if(CTRL_PORT == DATA_PORT) {
			cerr << "Note: DATA_PORT = CTRL_PORT may cause problems!" << endl;
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
		// Open sockets.
		// Socket is used to send and receive ctrl packets.
		sock_ctrl = std::make_shared<FileDescriptor>(open_socket_udp(CTRL_PORT));
		// Socket is used to send audio packets.
		sock_audio = std::make_shared<FileDescriptor>(open_socket_udp());
		// Socket is used to send retransmission (audio) packets.
		sock_retr = std::make_shared<FileDescriptor>(open_socket_udp());

		// Set session id.
		session_id = get_seconds_since_epoch();

		if(DEB) {cerr << "session_id: " << session_id << endl;}

		// Initialise audio buffer.
		MemoryChunk<unsigned long long> buff_index(FSIZE);
		MemoryChunk<char> buff_audio(FSIZE);
		audio_index.swap(buff_index);
		audio_buffer.swap(buff_audio);

		// Set default return code.
		rc = 0;

		// Set to not end threads now.
		terminate = false;

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
		                             [&sock_audio](){worker_audio_loop(sock_audio);}));
		tg.create_thread(boost::bind(&worker_bootstrap,
		                             [&sock_ctrl](){worker_ctrl_loop(sock_ctrl);}));
		tg.create_thread(boost::bind(&worker_bootstrap,
		                             [&sock_retr](){worker_retr_loop(sock_retr);}));

		tg.join_all();
	}

	if(DEB) {cerr << "Done!" << endl;}

	return rc;
}
