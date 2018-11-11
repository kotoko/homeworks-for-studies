// Adopted from: https://github.com/klange/nyancat/blob/master/src/telnet.h
#ifndef TELNET_HPP
#define TELNET_HPP

namespace telnet
{
	constexpr unsigned char IAC = 255;
	constexpr unsigned char DONT = 254;
	constexpr unsigned char DO = 253;
	constexpr unsigned char WONT = 252;
	constexpr unsigned char WILL = 251;

	constexpr unsigned char SE = 240;  // Subnegotiation End
	constexpr unsigned char NOP = 241;  // No Operation
	constexpr unsigned char DM = 242;  // Data Mark
	constexpr unsigned char BRK = 243;  // Break
	constexpr unsigned char IP = 244;  // Interrupt process
	constexpr unsigned char AO = 245;  // Abort output
	constexpr unsigned char AYT = 246;  // Are You There
	constexpr unsigned char EC = 247;  // Erase Character
	constexpr unsigned char EL = 248;  // Erase Line
	constexpr unsigned char GA = 249;  // Go Ahead
	constexpr unsigned char SB = 250;  // Subnegotiation Begin

	constexpr unsigned char BINARY = 0; // 8-bit data path
	constexpr unsigned char ECHO = 1; // echo
	constexpr unsigned char RCP = 2; // prepare to reconnect
	constexpr unsigned char SGA = 3; // suppress go ahead
	constexpr unsigned char NAMS = 4; // approximate message size
	constexpr unsigned char STATUS = 5; // give status
	constexpr unsigned char TM = 6; // timing mark
	constexpr unsigned char RCTE = 7; // remote controlled transmission and echo
	constexpr unsigned char NAOL = 8; // negotiate about output line width
	constexpr unsigned char NAOP = 9; // negotiate about output page size
	constexpr unsigned char NAOCRD = 10; // negotiate about CR disposition
	constexpr unsigned char NAOHTS = 11; // negotiate about horizontal tabstops
	constexpr unsigned char NAOHTD = 12; // negotiate about horizontal tab disposition
	constexpr unsigned char NAOFFD = 13; // negotiate about formfeed disposition
	constexpr unsigned char NAOVTS = 14; // negotiate about vertical tab stops
	constexpr unsigned char NAOVTD = 15; // negotiate about vertical tab disposition
	constexpr unsigned char NAOLFD = 16; // negotiate about output LF disposition
	constexpr unsigned char XASCII = 17; // extended ascii character set
	constexpr unsigned char LOGOUT = 18; // force logout
	constexpr unsigned char BM = 19; // byte macro
	constexpr unsigned char DET = 20; // data entry terminal
	constexpr unsigned char SUPDUP = 21; // supdup protocol
	constexpr unsigned char SUPDUPOUTPUT = 22; // supdup output
	constexpr unsigned char SNDLOC = 23; // send location
	constexpr unsigned char TTYPE = 24; // terminal type
	constexpr unsigned char EOR = 25; // end or record
	constexpr unsigned char TUID = 26; // TACACS user identification
	constexpr unsigned char OUTMRK = 27; // output marking
	constexpr unsigned char TTYLOC = 28; // terminal location number
	constexpr unsigned char VT3270REGIME = 29; // 3270 regime
	constexpr unsigned char X3PAD = 30; // X.3 PAD
	constexpr unsigned char NAWS = 31; // window size
	constexpr unsigned char TSPEED = 32; // terminal speed
	constexpr unsigned char LFLOW = 33; // remote flow control
	constexpr unsigned char LINEMODE = 34; // Linemode option
	constexpr unsigned char XDISPLOC = 35; // X Display Location
	constexpr unsigned char OLD_ENVIRON = 36; // Old - Environment variables
	constexpr unsigned char AUTHENTICATION = 37; // Authenticate
	constexpr unsigned char ENCRYPT = 38; // Encryption option
	constexpr unsigned char NEW_ENVIRON = 39; // New - Environment variables
	constexpr unsigned char TN3270E = 40; // TN3270E
	constexpr unsigned char XAUTH = 41; // XAUTH
	constexpr unsigned char CHARSET = 42; // CHARSET
	constexpr unsigned char RSP = 43; // Telnet Remote Serial Port
	constexpr unsigned char COM_PORT_OPTION = 44; // Com Port Control Option
	constexpr unsigned char SUPPRESS_LOCAL_ECHO = 45; // Telnet Suppress Local Echo
	constexpr unsigned char TLS = 46; // Telnet Start TLS
	constexpr unsigned char KERMIT = 47; // KERMIT
	constexpr unsigned char SEND_URL = 48; // SEND-URL
	constexpr unsigned char FORWARD_X = 49; // FORWARD_X
	constexpr unsigned char PRAGMA_LOGON = 138; // TELOPT PRAGMA LOGON
	constexpr unsigned char SSPI_LOGON = 139; // TELOPT SSPI LOGON
	constexpr unsigned char PRAGMA_HEARTBEAT = 140; // TELOPT PRAGMA HEARTBEAT
	constexpr unsigned char EXOPL = 255; // Extended-Options-List
	constexpr unsigned char NOOPT = 0;

	constexpr unsigned char IS = 0;
	constexpr unsigned char SEND = 1;
}

#endif
