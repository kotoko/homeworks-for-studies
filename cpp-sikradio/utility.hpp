#ifndef ZADANIE2_UTILITY_HPP
#define ZADANIE2_UTILITY_HPP

#include <chrono>
#include <string>

void memset_u_long_long(void* buffer, unsigned long long value, size_t count);

bool is_correct_ipv4(const std::string& s);

bool is_correct_multicast(const std::string& s);

bool is_correct_broadcast(const std::string& s);

bool is_correct_port(unsigned int port);

decltype(std::chrono::seconds().count()) get_seconds_since_epoch();

#endif //ZADANIE2_UTILITY_HPP
