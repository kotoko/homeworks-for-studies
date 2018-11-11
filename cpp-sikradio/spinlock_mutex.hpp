#ifndef ZADANIE2_SPINLOCKMUTEX_HPP
#define ZADANIE2_SPINLOCKMUTEX_HPP

#include <atomic>


/* Class implements simple mechanism for synchronization
 * 2 threads. It uses Peterson's algorithm (spinlocks).
 */
class SpinlockMutex
{
private:
	// A = 0, B = 1
	std::atomic<int> turn_;
	std::atomic<bool> waiting_A_;
	std::atomic<bool> waiting_B_;

public:
	SpinlockMutex();
	SpinlockMutex(const SpinlockMutex& other) = delete;
	SpinlockMutex(SpinlockMutex&& other) = delete;
	SpinlockMutex& operator=(const SpinlockMutex& other) = delete;
	SpinlockMutex& operator=(SpinlockMutex&& other) = delete;

	void lock_A();
	void lock_B();
	bool try_lock_A();
	bool try_lock_B();
	void unlock_A();
	void unlock_B();
};


/* Wrapper for SpinlockMutex which uses RAII to going into
 * critical section.
 */
class SpinlockMutexGuard
{
public:
	enum Action {None, Lock, TryLock};

protected:
	SpinlockMutex& mut_;
	bool locked_;

protected:
	explicit SpinlockMutexGuard(SpinlockMutex& mut);
public:
	explicit operator bool() const noexcept;
	virtual void lock() = 0;
	virtual bool try_lock() = 0;
	virtual void unlock() = 0;
	bool owns_lock() const noexcept;
};


/* Wrapper for thread A.
 */
class SpinlockMutexGuardA : public SpinlockMutexGuard
{
public:
	SpinlockMutexGuardA(SpinlockMutex& mut, Action action = Lock);
	virtual ~SpinlockMutexGuardA();
	void lock() override;
	bool try_lock() override;
	void unlock() override;
};

/* Wrapper for thread B.
 */
class SpinlockMutexGuardB : public SpinlockMutexGuard
{
public:
	SpinlockMutexGuardB(SpinlockMutex& mut, Action action = Lock);
	virtual ~SpinlockMutexGuardB();
	void lock() override;
	bool try_lock() override;
	void unlock() override;
};

#endif //ZADANIE2_SPINLOCKMUTEX_HPP
