#include "spinlock_mutex.hpp"

// SpinlockMutex
SpinlockMutex::SpinlockMutex()
		: turn_(73), waiting_A_(false), waiting_B_(false)
{}

void SpinlockMutex::lock_A()
{
	waiting_A_ = true;

	// std::atomic already does memory barrier here.
	// See: https://stackoverflow.com/q/40320254

	turn_ = 1;
	while(waiting_B_ == true && turn_ == 1) {}
}

void SpinlockMutex::lock_B()
{
	waiting_B_ = true;
	turn_ = 0;
	while(waiting_A_ == true && turn_ == 0) {}
}

bool SpinlockMutex::try_lock_A()
{
	waiting_A_ = true;
	turn_ = 1;

	if(waiting_B_ == true && turn_ == 1) {
		waiting_A_ = false;
		return false;
	}
	else {
		return true;
	}
}

bool SpinlockMutex::try_lock_B()
{
	waiting_B_ = true;
	turn_ = 0;

	if(waiting_A_ == true && turn_ == 0) {
		waiting_B_ = false;
		return false;
	} else {
		return true;
	}
}

void SpinlockMutex::unlock_A()
{
	waiting_A_ = false;
}

void SpinlockMutex::unlock_B()
{
	waiting_B_ = false;
}


// SpinlockMutexGuard
SpinlockMutexGuard::SpinlockMutexGuard(SpinlockMutex& mut)
		: mut_(mut), locked_(false)
{}

SpinlockMutexGuard::operator bool() const noexcept
{
	return locked_;
}

bool SpinlockMutexGuard::owns_lock() const noexcept
{
	return locked_;
}


// SpinlockMutexGuardA
SpinlockMutexGuardA::SpinlockMutexGuardA(
		SpinlockMutex& mut,
		SpinlockMutexGuard::Action action)
		: SpinlockMutexGuard(mut)
{
	switch(action) {
		case TryLock:
			try_lock();
			break;

		case Lock:
			lock();
			break;

		default:
			break;
	}
}

void SpinlockMutexGuardA::lock()
{
	mut_.lock_A();
	locked_ = true;
}

bool SpinlockMutexGuardA::try_lock()
{
	if(mut_.try_lock_A()) {
		locked_ = true;
		return true;
	} else {
		return false;
	}
}

void SpinlockMutexGuardA::unlock()
{
	mut_.unlock_A();
	locked_ = false;
}

SpinlockMutexGuardA::~SpinlockMutexGuardA()
{
	if(locked_) {
		unlock();
	}
}


// SpinlockMutexGuardB
SpinlockMutexGuardB::SpinlockMutexGuardB(
		SpinlockMutex& mut,
		SpinlockMutexGuard::Action action)
		: SpinlockMutexGuard(mut)
{
	switch(action) {
		case TryLock:
			try_lock();
			break;

		case Lock:
			lock();
			break;

		default:
			break;
	}
}

void SpinlockMutexGuardB::lock()
{
	mut_.lock_B();
	locked_ = true;
}

bool SpinlockMutexGuardB::try_lock()
{
	if(mut_.try_lock_B()) {
		locked_ = true;
		return true;
	} else {
		return false;
	}
}

void SpinlockMutexGuardB::unlock()
{
	mut_.unlock_B();
	locked_ = false;
}

SpinlockMutexGuardB::~SpinlockMutexGuardB()
{
	if(locked_) {
		unlock();
	}
}
