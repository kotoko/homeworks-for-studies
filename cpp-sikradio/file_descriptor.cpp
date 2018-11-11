#include <unistd.h>

#include "file_descriptor.hpp"


namespace
{
#ifndef NDEBUG
	constexpr bool DEB = true;
#else
	constexpr bool DEB = false;
#endif
}

FileDescriptor::FileDescriptor(int fd) : fd_(fd)
{}

void FileDescriptor::swap(FileDescriptor& other) noexcept
{
	auto tmp_fd = other.fd_;
	other.fd_ = fd_;
	fd_ = tmp_fd;
}

FileDescriptor::~FileDescriptor() noexcept
{
	if(*this) {
		close(fd_);
	}
}

FileDescriptor::operator int() const noexcept
{
	return fd_;
}

FileDescriptor::operator bool() const noexcept
{
	return fd_ != -1;
}

