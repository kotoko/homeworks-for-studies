#ifndef ZADANIE2_DEFAULT_VALUES_HPP
#define ZADANIE2_DEFAULT_VALUES_HPP

#include "indeks.hpp"

namespace sikradio
{
	constexpr unsigned int RECEIVER_CTRL_PORT = 30000 + (INDEKS % 10000);
	constexpr unsigned int RECEIVER_UI_PORT = 10000 + (INDEKS % 10000);
	constexpr unsigned long RECEIVER_BSIZE = 64 * 1024;
	constexpr unsigned long RECEIVER_RTIME = 250;
	const char* RECEIVER_DISCOVER_ADDR = "255.255.255.255";

	constexpr unsigned int SENDER_DATA_PORT = 20000 + (INDEKS % 10000);
	constexpr unsigned int SENDER_CTRL_PORT = 30000 + (INDEKS % 10000);
	constexpr unsigned long SENDER_PSIZE = 512;
	constexpr unsigned long SENDER_FSIZE = 128 * 1024;
	constexpr unsigned long SENDER_RTIME = 250;
	const char* SENDER_NAZWA = "Nienazwany Nadajnik";
}

#endif //ZADANIE2_DEFAULT_VALUES_HPP
