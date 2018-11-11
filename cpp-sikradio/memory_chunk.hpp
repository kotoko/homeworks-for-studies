#ifndef ZADANIE2_MEMORYGUARD_H
#define ZADANIE2_MEMORYGUARD_H

#include <cstddef>
#include <cstring>
#include <exception>

#include "optimizations.hpp"


struct MemoryChunkException : public std::exception
{
private:
	const char *message_;

public:
	MemoryChunkException(const char* message);
	const char* what() const throw();
};


/*
 * Wrapper for new & delete in RAII style. Class allows
 * you to allocate some memory block. It give you pointer
 * to memory.
 */
template<typename T>
class MemoryChunk
{
private:
	T* memory_;
	std::size_t n_;

public:
	MemoryChunk(std::size_t n = 0);
	MemoryChunk(const MemoryChunk<T>& other) = delete;
	MemoryChunk(MemoryChunk<T>&& other) noexcept;
	MemoryChunk<T>& operator=(const MemoryChunk<T>& other) = delete;
	MemoryChunk<T>& operator=(MemoryChunk<T>&& other) noexcept;
	~MemoryChunk() noexcept;
	T* ptr() const noexcept;
	std::size_t size() const noexcept;  // Size in bytes.
	std::size_t length() const noexcept;  // Number of elements in array.
	void swap(MemoryChunk<T>& other) noexcept;
	void copy(const MemoryChunk<T>& other);
	void zero();
	explicit operator bool() const noexcept;
};


#ifndef NDEBUG
#define DEB true
#else
#define DEB false
#endif

template<typename T>
MemoryChunk<T>::MemoryChunk(std::size_t n) : n_(n)
{
	if(unlikely(n < 0)) {
		throw MemoryChunkException("Illegal size of memory to allocate!");
	}

	memory_ = new T [n];
	zero();
}

template<typename T>
MemoryChunk<T>::~MemoryChunk() noexcept
{
	delete[] memory_;
}

template<typename T>
T* MemoryChunk<T>::ptr() const noexcept
{
	return memory_;
}

template<typename T>
std::size_t MemoryChunk<T>::size() const noexcept
{
	return n_ * sizeof(T);
}

template<typename T>
std::size_t MemoryChunk<T>::length() const noexcept
{
	return n_;
}

template<typename T>
MemoryChunk<T>::MemoryChunk(MemoryChunk<T>&& other) noexcept
{
	n_ = 0;
	memory_ = nullptr;

	(*this).swap(other);
}

template<typename T>
void MemoryChunk<T>::swap(MemoryChunk<T>& other) noexcept
{
	std::size_t tmp_n;
	T *tmp_memory;

	tmp_n = other.n_;
	tmp_memory = other.memory_;

	other.n_ = n_;
	other.memory_ = memory_;

	n_ = tmp_n;
	memory_ = tmp_memory;
}

template<typename T>
MemoryChunk<T>& MemoryChunk<T>::operator=(MemoryChunk<T>&& other) noexcept
        {
	(*this).swap(other);

	return *this;
}

template<typename T>
void MemoryChunk<T>::copy(const MemoryChunk<T>& other)
{
	if(unlikely(n_ != other.n_)) {
		throw MemoryChunkException("Memory sizes are not equal!");
	}

	if(likely(this != &other)) {
		std::memcpy(memory_, other.memory_, sizeof(T) * n_);
	}
}

template<typename T>
void MemoryChunk<T>::zero()
{
	std::memset(memory_, 0, sizeof(T) * n_);
}

template<typename T>
MemoryChunk<T>::operator bool() const noexcept
{
	return n_ != 0;
}

#undef DEB
#endif //ZADANIE2_MEMORYGUARD_H
