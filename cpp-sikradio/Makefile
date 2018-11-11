SHELL := /bin/bash

release:
	mkdir bin/; \
	cd bin/; \
	cmake -DCMAKE_BUILD_TYPE=Release ..; \
	make; \
	cp -vf sikradio-receiver sikradio-sender ../; \
	cd ..;

debug:
	mkdir bin_deb/; \
	cd bin_deb/; \
	cmake -DCMAKE_BUILD_TYPE=Debug ..; \
	make; \
	cp -vf sikradio-receiver sikradio-sender ../; \
	cd ..;

clean:
	rm -fr bin/ bin_deb/ sikradio-receiver sikradio-sender;

.ONESHELL: 
