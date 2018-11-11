cmake_minimum_required(VERSION 3.8)
project(zadanie2)

set(CMAKE_CXX_STANDARD 17)

add_compile_options(
        "-Wall"
        "-Wextra"
        "-Wcast-align"
        "-Wswitch-default"
        "-Wredundant-decls"
        "-flto"
        "$<$<CONFIG:DEBUG>:-Wpadded>"
        "$<$<CONFIG:DEBUG>:-g>"
        "$<$<CONFIG:DEBUG>:-O0>"
        "$<$<CONFIG:RELEASE>:-DNDEBUG>"
        "$<$<CONFIG:RELEASE>:-DOPTIMISE_BRANCHES>"
        "$<$<CONFIG:RELEASE>:-Werror>"
        "$<$<CONFIG:RELEASE>:-fno-stack-protector>"
        "$<$<CONFIG:RELEASE>:-fno-asynchronous-unwind-tables>"
        "$<$<CONFIG:RELEASE>:-O2>"
)

find_package( Boost REQUIRED COMPONENTS program_options thread )
include_directories( ${Boost_INCLUDE_DIRS} )


add_executable(
        sikradio-receiver

        default_values.hpp
        file_descriptor.cpp
        file_descriptor.hpp
        indeks.hpp
        memory_chunk.cpp
        memory_chunk.hpp
        optimizations.hpp
        sikradio_receiver.cpp
        spinlock_mutex.cpp
        spinlock_mutex.hpp
        telnet.hpp
        utility.cpp
        utility.hpp
)

add_executable(
        sikradio-sender

        default_values.hpp
        file_descriptor.cpp
        file_descriptor.hpp
        indeks.hpp
        memory_chunk.cpp
        memory_chunk.hpp
        optimizations.hpp
        sikradio_sender.cpp
        spinlock_mutex.cpp
        spinlock_mutex.hpp
        utility.cpp
        utility.hpp
)

target_link_libraries(sikradio-receiver ${Boost_LIBRARIES})
target_link_libraries(sikradio-sender ${Boost_LIBRARIES})
