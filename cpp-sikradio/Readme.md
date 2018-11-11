# Sikradio
In this project I implemented simple internet radio. It is written in modern c++ (c++17).

### Description
There are two programs `sikradio-receiver` and `sikradio-sender`. Sender reads bytes in loop from stdin until EOF. Then sends bytes as fast as possible to given multicast address. Receiver automatically discovers senders and receives bytes to buffer in memory. When buffer is 3/4 full starts writing bytes to stdout as fast as possible. If receiver would write byte that is not in buffer yet, it restarts playing (flushes buffer and waits until buffer is again 3/4 full). Receiver automatically sends requests for retransmission bytes/packets that are probably lost.

### Design
To be added.

### Build
Requirements:

* linux
* boost library (tested on =1.65.0)
* gcc (tested on =7.3.0)
* cmake (tested on =3.8)
* make (tested on =4.2.1)

Just go into source directory nad run:
```
make
```
It will create two executables `sikradio-receiver` and `sikradio-sender`.

### Run
You need compiled executables `sikradio-receiver`, `sikradio-sender` and a `*.mp3` or `*.wav` file with your favourite song. Moreover you need to have [sox](http://sox.sourceforge.net) installed on your system. For convenient usage I created scripts `send.sh`, `recv.sh` to run radio.

**Before running radio please double check that you allow incoming udp packets in your firewall!** For testing purposes I recommend to temporarily turn off firewall completly.

Make sure that scripts have executable permission:
```
chmod +x ./send.sh
chmod +x ./recv.sh
```

Run sender:
```
./send.sh ./sikradio-sender ./my_favourite_song.mp3
```

Run receiver:
```
./recv.sh ./sikradio-receiver
```

After few seconds you should hear the song.

The best effect is when you run sender and receiver on two different computers connected to the same network (aka LAN).

### Receiver UI
`sikradio-receiver` has simple text user interface which allows you to change current playing station. You can connect to it with `telnet`. If you are using `recv.sh` then on the same computer run:
```
telnet localhost 13456
```

You will see list of discovered radio stations. You can change current playing station with arrows up and down.

It is also possible to connect to UI from many computers at the same time!
