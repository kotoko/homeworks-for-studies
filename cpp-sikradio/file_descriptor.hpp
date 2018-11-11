#ifndef ZADANIE2_FILEDESCRIPTOR_HPP
#define ZADANIE2_FILEDESCRIPTOR_HPP


/* Wrapper for file descriptor (RAII). In constructor
 * shall pass already opened fd. In destructor class
 * call close() on fd.
 *
 * Recommended usage with shared_ptr().
 */
class FileDescriptor
{
private:
	int fd_;

public:
	FileDescriptor(int fd);
	FileDescriptor(const FileDescriptor& other) = delete;
	FileDescriptor(FileDescriptor&& other) = delete;
	~FileDescriptor() noexcept;
	FileDescriptor& operator=(const FileDescriptor& other) = delete;
	FileDescriptor& operator=(FileDescriptor&& other) = delete;
	operator int() const noexcept;
	explicit operator bool() const noexcept;

private:
	void swap(FileDescriptor& other) noexcept;
};


#endif //ZADANIE2_FILEDESCRIPTOR_HPP
