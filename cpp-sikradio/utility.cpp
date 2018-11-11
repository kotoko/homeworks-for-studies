#include <cstddef>
#include <regex>

#include "utility.hpp"

using std::regex;
using std::size_t;
using std::string;
namespace chrono = std::chrono;

void memset_u_long_long(
		void* buffer,
		unsigned long long value,
		size_t count)
{
	// Inspired by: https://stackoverflow.com/a/7893471
	const size_t m = count / sizeof(unsigned long long);
	unsigned long long *p = (unsigned long long*)buffer;

	for(size_t i = 0; i < m; ++i, ++p) {
		*p = value;
	}

	return;
}

bool is_correct_ipv4(const string& s)
{
	// https://stackoverflow.com/a/14453696
	const regex ipv4("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"
	                 "(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3})$");
	return regex_match(s, ipv4);
}

bool is_correct_multicast(const string& s)
{
	// https://stackoverflow.com/a/13145552
	const regex multicast(
			"^2(?:2[4-9]|3\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d?|0)){3}$");
	return regex_match(s, multicast);
}

bool is_correct_broadcast(const string& s)
{
	const regex broadcast("^255.255.255.255$");
	return regex_match(s, broadcast);
}

bool is_correct_port(unsigned int port)
{
	return port <= 65535 && port >= 1;
}

decltype(chrono::seconds().count()) get_seconds_since_epoch()
{
	// source: https://stackoverflow.com/a/14032877
	// get the current time
	const auto now = chrono::system_clock::now();

	// transform the time into a duration since the epoch
	const auto epoch = now.time_since_epoch();

	// cast the duration into seconds
	const auto seconds = chrono::duration_cast<chrono::seconds>(epoch);

	// return the number of seconds
	return seconds.count();
}