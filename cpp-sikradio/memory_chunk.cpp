#include "memory_chunk.hpp"


namespace
{
#ifndef NDEBUG
	constexpr bool DEB = true;
#else
	constexpr bool DEB = false;
#endif
}

const char* MemoryChunkException::what() const throw()
{
	return message_;
}

MemoryChunkException::MemoryChunkException(
		const char* message = "MemoryChunk error!")
		: message_(message)
{}
