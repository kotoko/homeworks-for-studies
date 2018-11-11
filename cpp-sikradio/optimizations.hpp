#ifndef ZADANIE2_OPTIMIZATIONS_H
#define ZADANIE2_OPTIMIZATIONS_H

#ifdef OPTIMISE_BRANCHES
#define likely(x)     __builtin_expect(static_cast<bool>(x), 1)
#define unlikely(x)   __builtin_expect(static_cast<bool>(x), 0)
#else
#define likely(x)     (x)
#define unlikely(x)   (x)
#endif

#endif //ZADANIE2_OPTIMIZATIONS_H
