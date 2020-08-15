/* CoffeeCatch, a tiny native signal handler/catcher for JNI code.
 * (especially for Android/Dalvik)
 *
 * Copyright (c) 2013, Xavier Roche (http://www.httrack.com/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef TRACKPICKER_H
#define TRACKPICKER_H

#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <sys/types.h>
#include <jni.h>
#include <assert.h>
#include "trackpicker.h"
#include <signal.h>


#ifdef __ANDROID__
#define USE_UNWIND
#define USE_CORKSCREW
#define USE_LIBUNWIND
#endif



#ifdef USE_UNWIND
/* Number of backtraces to get. */
#define BACKTRACE_FRAMES_MAX 32
#endif


#ifdef USE_CORKSCREW

typedef struct map_info_t map_info_t;
/* Extracted from Android's include/corkscrew/backtrace.h */
typedef struct {
    uintptr_t absolute_pc;
    uintptr_t stack_top;
    size_t stack_size;
} backtrace_frame_t;
typedef struct {
    uintptr_t relative_pc;
    uintptr_t relative_symbol_addr;
    char* map_name;
    char* symbol_name;
    char* demangled_name;
} backtrace_symbol_t;
/* Extracted from Android's libcorkscrew/arch-arm/backtrace-arm.c */
typedef ssize_t (*t_unwind_backtrace_signal_arch)
        (siginfo_t* si, void* sc, const map_info_t* lst, backtrace_frame_t* bt,
         size_t ignore_depth, size_t max_depth);
typedef map_info_t* (*t_acquire_my_map_info_list)();
typedef void (*t_release_my_map_info_list)(map_info_t* milist);
typedef void (*t_get_backtrace_symbols)(const backtrace_frame_t* backtrace,
                                        size_t frames,
                                        backtrace_symbol_t* symbols);
typedef void (*t_free_backtrace_symbols)(backtrace_symbol_t* symbols,
                                         size_t frames);
#endif

/* Thread-specific crash handler structure. */
typedef struct native_code_handler_struct {

    /* Alternate stack. */
    char *stack_buffer;
    size_t stack_buffer_size;


    /* Signal code and info. */
    int code;
    siginfo_t si;
    ucontext_t uc;

    /* Uwind context. */
#if (defined(USE_CORKSCREW))
    backtrace_frame_t frames[BACKTRACE_FRAMES_MAX];
#elif (defined(USE_UNWIND))
    uintptr_t frames[BACKTRACE_FRAMES_MAX];
#endif
#ifdef USE_LIBUNWIND
    void* uframes[BACKTRACE_FRAMES_MAX];
#endif
    size_t frames_size;

} native_code_handler_struct;



#ifdef __cplusplus
extern "C" {
#endif


extern char * getCrashKeyInfo(siginfo_t siginfo,pid_t tid,ucontext_t context);
extern void getCrashFrames(native_code_handler_struct * t);
extern const char * getCrashSingalNoDes(int sig, int code);
extern const char * getCrashSingalCodeDes(int sig, int code);
extern const char* getCrashSingalDes(int sig, int code);

extern ssize_t coffeecatch_unwind_signal(void** frames,size_t max_depth);
void printStack(int frames_size,void * frames[]);

//extern void test2(ucontext_t context);
//extern void assembleThreadInfo(siginfo_t siginfo,pid_t tid,ucontext_t context,char * infobuffer,int buffersize);
#ifdef __cplusplus
}
#endif

#endif
