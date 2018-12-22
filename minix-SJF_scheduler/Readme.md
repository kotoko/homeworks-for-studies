# Shortest Job First scheduler
In this project I implemented shortest job first scheduler for minix. It is written in C. Code is in the format of a patch to be applied onto the minix source code.

## Description
SJF is a (well-known scheduling policy)[https://en.wikipedia.org/wiki/Shortest_job_next]. This is preemptive version of SJF. Queue `SJF_Q = 8` in minix has different behaviuor then the rest. By default programs are **not** in SJF queue and can not get there unless they make explicit syscall.

## Build
Requirements:

* minix (tested on ~=3.3.0)

Run your copy of minix.

Copy `sjf.patch` to the directory `/` on minix.

Apply patch to the minix source code:

```
cd /
patch -p1 < sjf.patch
```

Then build minix's source code:

```
cp -v /usr/src/minix/include/minix/com.h /usr/include/minix/com.h
cp -v /usr/src/minix/include/minix/callnr.h /usr/include/minix/callnr.h
cp -v /usr/src/include/unistd.h /usr/include/unistd.h
cp -v /usr/src/minix/include/minix/syslib.h /usr/include/minix/syslib.h
cp -v /usr/src/minix/include/minix/ipc.h /usr/include/minix/ipc.h
cp -v /usr/src/minix/include/minix/config.h /usr/include/minix/config.h

cd /usr/src/minix/lib/libsys
make && make install
cd /usr/src/minix/servers/pm/
make && make install
cd /usr/src/minix/servers/sched/
make && make install
cd /usr/src/lib/libc
make && make install
cd /usr/src/minix/kernel
make && make install

cd /usr/src/releasetools
make do-hdboot
```

Finally reboot minix:

```
/sbin/reboot
```

## Usage

Program written in C should include library `unistd.h`.

If program wants to change its own scheduler policy, it can call function `int setsjf(int expected_time)`. `expected_time` must be `0 <= expected_time <= MAX_SJFPRIO=100`.

* If `expected_time` is equall to `0`, that means that process wants to switch to default minix's scheduling policy.
* If `expected_time` is not equall to `0`, that means that process wants to switch to SJF schedulinug policy and `expected_time` is an expected time of execution.
