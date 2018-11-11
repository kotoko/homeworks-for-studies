global _start

section .rodata

SYS_READ equ 0
SYS_OPEN equ 2
SYS_CLOSE equ 3
SYS_EXIT equ 60
CHUNK_SIZE equ 16384                   ; how many bytes read from file in one syscall
                                       ; best value should be one of: 4096, 8192, 16384

section .bss

fd: resq 1                             ; file descriptor
file_open: resq 1                      ; bool: 1 - open, 0 - closed
chunk: resb CHUNK_SIZE                 ; bytes read from file


section .text

_start:
    cmp qword [rsp], 2                 ; check number of parameters
    jne _exit_failure                  ; argc != 2

    mov rax, SYS_OPEN                  ; open file
    mov rdi, [rsp + 16]                ; file name (argv[1])
    mov rsi, 0                         ; read only mode
    syscall

    cmp qword rax, 0                   ; check if opened successfully?
    jl _exit_failure                   ; failed to open file

    mov qword [file_open], 1           ; save information that file is open
    mov [fd], rax                      ; rax has file descriptor

                                       ; Variables in part1:
                                       ; cl = chunk[i] (single number from file)
    xor rbx, rbx                       ; first counter of numbers
    xor r12, r12                       ; second counter of numbers
    xor r13, r13                       ; third counter of numbers
    xor r14, r14                       ; fourth counter of numbers
    xor r9, r9                         ; i
    xor r11, r11                       ; buffer length

; read first permutation
_part1_loop:
    cmp r11, r9                        ; check if already read all buffered characters
    jne _part1_skip_reading            ; no need to refill buffer

    call _read_chunk                   ; refill buffer

    test rax, rax                      ; check if read >0 bytes?
    jz _exit_failure                   ; read EOF/0 bytes

    xor r9, r9                         ; i = 0
    mov r11, rax                       ; save buffer length to r11

_part1_skip_reading:
    mov cl, [chunk + r9]               ; cl = chunk[i]

    test cl, cl                        ; check if chunk[i] == 0?
    jz _part1_loop_end                 ; chunk[i] == 0 -> break loop

    call _update_counters              ; update counters for chunk[i]

_part1_counter_end:
    inc r9                             ; i++
    jmp _part1_loop

; successfully read first permutation
_part1_loop_end:
    inc r9                             ; i++

    ; counters from part1 will be now: r8, r10, rsi, rdi
    mov r8, rbx
    mov r10, r12
    mov rsi, r13
    mov rdi, r14

    ; counters used for next permutation
    xor rbx, rbx
    xor r12, r12
    xor r13, r13
    xor r14, r14

_part2_loop:
    cmp r11, r9                        ; check if already read all buffered characters
    jne _part2_skip_reading            ; no need to refill buffer

    ; save temporary registers
    push r8
    push r10
    push rsi
    push rdi

    call _read_chunk                   ; refill buffer

    ; restore temporary registers
    pop rdi
    pop rsi
    pop r10
    pop r8

    test rax, rax                      ; check if read >0 bytes?
    jz _part2_loop_end                 ; read EOF/0 bytes

    xor r9, r9                         ; i = 0
    mov r11, rax                       ; save buffer length to r11

_part2_skip_reading:
    mov cl, [chunk + r9]               ; cl = chunk[i]

    test cl, cl                        ; check if chunk[i] == 0?
    jz _part2_analyze                  ; analyze permutation

    call _update_counters              ; update counters for chunk[i]

    inc r9                             ; i++
    jmp _part2_loop

; check that all current counters are equal to counters from part1
_part2_analyze:
    xor rbx, r8
    jnz _exit_failure

    xor r12, r10
    jnz _exit_failure

    xor r13, rsi
    jnz _exit_failure

    xor r14, rdi
    jnz _exit_failure

    inc r9                             ; i++
    jmp _part2_loop

_part2_loop_end:
    ; check that all counters are zeros - no half-open permutation
    test rbx, rbx
    jnz _exit_failure

    test r12, r12
    jnz _exit_failure

    test r13, r13
    jnz _exit_failure

    test r14, r14
    jnz _exit_failure

    ; close file
    mov rax, SYS_CLOSE
    mov rdi, [fd]
    syscall

    mov qword [file_open], 0           ; save information that file is closed

    ; exit successfully
    mov rax, SYS_EXIT
    mov rdi, 0
    syscall

; subroutine that updates counters for given number
; parameters:
;   cl - number,
;   rbx, r12, r13, r14 - counters
; modifies registers cl and rax for internal use
_update_counters:
    mov rax, 1                         ; temporary variable used to calculate 2**(cl % 64)

    ; determine which counter to use and set cl = cl % 64
    cmp cl, 64
    jb _update_first_counter           ; [0, 64)

    sub cl, 64
    cmp cl, 64
    jb _update_second_counter          ; [64, 128)

    sub cl, 64
    cmp cl, 64
    jb _update_third_counter           ; [128, 192)

    sub cl, 64
    jmp _update_fourth_counter         ; [192, 256)
_update_first_counter:
    shl rax, cl                        ; calculate 2**(cl % 64)

    test rax, rbx
    jnz _exit_failure                  ; this number appears second time in permutation

    xor rbx, rax                       ; set adequate bit to 1
    ret
_update_second_counter:
    shl rax, cl                        ; calculate 2**(cl % 64)

    test rax, r12
    jnz _exit_failure                  ; this number appears second time in permutation

    xor r12, rax                       ; set adequate bit to 1
    ret
_update_third_counter:
    shl rax, cl                        ; calculate 2**(cl % 64)

    test rax, r13
    jnz _exit_failure                  ; this number appears second time in permutation

    xor r13, rax                       ; set adequate bit to 1
    ret
_update_fourth_counter:
    shl rax, cl                        ; calculate 2**(cl % 64)

    test rax, r14
    jnz _exit_failure                  ; this number appears second time in permutation

    xor r14, rax                       ; set adequate bit to 1
    ret

; exit with code 1
_exit_failure:
    ; check if file is open?
    mov qword rax, [file_open]
    test rax, rax
    jz _exit_failure_end               ; file is closed

    ; close file
    mov rax, SYS_CLOSE
    mov rdi, [fd]
    syscall

    mov qword [file_open], 0           ; save information that file is closed

_exit_failure_end:
    mov rax, SYS_EXIT
    mov rdi, 1
    syscall

; subroutine that reads next chunk from file
; saves number of read bytes (0 = EOF) in rax
_read_chunk:
    mov rax, SYS_READ
    mov rdi, [fd]
    mov rsi, chunk
    mov rdx, CHUNK_SIZE
    syscall

    cmp qword rax, 0                   ; check return value
    jl _exit_failure                   ; problem while reading from file

    ret
