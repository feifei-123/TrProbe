// Copyright (c) 2010 Google Inc.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// The ExceptionHandler object installs signal handlers for a number of
// signals. We rely on the signal handler running on the thread which crashed
// in order to identify it. This is true of the synchronous signals (SEGV etc),
// but not true of ABRT. Thus, if you send ABRT to yourself in a program which
// uses ExceptionHandler, you need to use tgkill to direct it to the current
// thread.
//
// The signal flow looks like this:
//
//   SignalHandler (uses a global stack of ExceptionHandler objects to find
//        |         one to handle the signal. If the first rejects it, try
//        |         the second etc...)
//        V
//   HandleSignal ----------------------------| (clones a new process which
//        |                                   |  shares an address space with
//   (wait for cloned                         |  the crashed process. This
//     process)                               |  allows us to ptrace the crashed
//        |                                   |  process)
//        V                                   V
//   (set signal handler to             ThreadEntry (static function to bounce
//    SIG_DFL and rethrow,                    |      back into the object)
//    killing the crashed                     |
//    process)                                V
//                                          DoDump  (writes minidump)
//                                            |
//                                            V
//                                         sys_exit
//

// This code is a little fragmented. Different functions of the ExceptionHandler
// class run in a number of different contexts. Some of them run in a normal
// context and are easy to code, others run in a compromised context and the
// restrictions at the top of minidump_writer.cc apply: no libc and use the
// alternative malloc. Each function should have comment above it detailing the
// context which it runs in.

#include "client/linux/handler/exception_handler.h"

#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <sched.h>
#include <signal.h>
#include <stdio.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <sys/wait.h>
#include <unistd.h>

#include <sys/signal.h>
#include <sys/ucontext.h>
#include <sys/user.h>
#include <ucontext.h>

#include <algorithm>
#include <utility>
#include <vector>
#include <android/log.h>
#include <dlfcn.h>

#include "common/basictypes.h"
#include "common/linux/linux_libc_support.h"
#include "common/memory_allocator.h"
#include "client/linux/log/log.h"
#include "client/linux/microdump_writer/microdump_writer.h"
#include "client/linux/minidump_writer/linux_dumper.h"
#include "client/linux/minidump_writer/minidump_writer.h"
#include "common/linux/eintr_wrapper.h"
#include "third_party/lss/linux_syscall_support.h"
#include "../../../../../../mylog.h"
#include "trackpicker.h"
#if defined(__ANDROID__)

#include "linux/sched.h"

#endif

#ifndef PR_SET_PTRACER
#define PR_SET_PTRACER 0x59616d61
#endif


namespace google_breakpad {

    namespace {
// The list of signals which we consider to be crashes. The default action for
// all these signals must be Core (see man 7 signal) because we rethrow the
// signal after handling it and expect that it'll be fatal.

        void* uframes[BACKTRACE_FRAMES_MAX] = {0};

        //--- feifei 支持的信号量
        const int kExceptionSignals[] = {
                SIGSEGV, SIGABRT, SIGFPE, SIGILL, SIGBUS, SIGTRAP
        };
        //--- 信号量个数
        const int kNumHandledSignals =
                sizeof(kExceptionSignals) / sizeof(kExceptionSignals[0]);

        //--- 每个信号的原有信号处理函数
        struct sigaction old_handlers[kNumHandledSignals];

        //--- 信号是否安装
        bool handlers_installed = false;

// InstallAlternateStackLocked will store the newly installed stack in new_stack
// and (if it exists) the previously installed stack in old_stack.
        stack_t old_stack;
        stack_t new_stack;
        bool stack_installed = false;

// Create an alternative stack to run the signal handlers on. This is done since
// the signal might have been caused by a stack overflow.
// Runs before crashing: normal context.
        void InstallAlternateStackLocked() {
            ALOGD("InstallAlternateStackLocked stack_installed:%d",stack_installed);
            if (stack_installed)
                return;

            memset(&old_stack, 0, sizeof(old_stack));
            memset(&new_stack, 0, sizeof(new_stack));

            // SIGSTKSZ may be too small to prevent the signal handlers from overrunning
            // the alternative stack. Ensure that the size of the alternative stack is
            // large enough.
            static const unsigned kSigStackSize = std::max(16384, SIGSTKSZ);

            // Only set an alternative stack if there isn't already one, or if the current
            // one is too small.
            //为singal 处理函数设置堆栈
            ALOGD("sys_sigaltstack(NULL, &old_stack) == -1:%d",sys_sigaltstack(NULL, &old_stack) == -1);
            ALOGD("!old_stack.ss_sp:%d",!old_stack.ss_sp);
            ALOGD("old_stack.ss_size < kSigStackSize:%d",old_stack.ss_size < kSigStackSize);

            //todo feifei 这里去强制 设置备用栈，可以解决发生crash时,unwind 失败(会二次引发singal 11 SIGSEG 错误)的问题
            if (sys_sigaltstack(NULL, &old_stack) == -1 || !old_stack.ss_sp ||
                old_stack.ss_size < kSigStackSize) {
                new_stack.ss_sp = calloc(1, kSigStackSize);
                new_stack.ss_size = kSigStackSize;
                new_stack.ss_flags = 0;
                ALOGD("InstallAlternateStackLocked:设置备用栈");

                if (sys_sigaltstack(&new_stack, NULL) == -1) {
                    free(new_stack.ss_sp);
                    return;
                }
                stack_installed = true;
            }

            ALOGD("InstallAlternateStackLocked finish~");
        }

// Runs before crashing: normal context.
        void RestoreAlternateStackLocked() {

            ALOGD("RestoreAlternateStackLocked:恢复备用栈");
            if (!stack_installed)
                return;

            stack_t current_stack;
            if (sys_sigaltstack(NULL, &current_stack) == -1)
                return;

            //之前安装过 singlestack,则需要将信号处理函数恢复.并释放newstack
            // Only restore the old_stack if the current alternative stack is the one
            // installed by the call to InstallAlternateStackLocked.
            if (current_stack.ss_sp == new_stack.ss_sp) {
                if (old_stack.ss_sp) {
                    if (sys_sigaltstack(&old_stack, NULL) == -1)
                        return;
                } else {
                    stack_t disable_stack;
                    disable_stack.ss_flags = SS_DISABLE;
                    if (sys_sigaltstack(&disable_stack, NULL) == -1)
                        return;
                }
            }

            free(new_stack.ss_sp);
            stack_installed = false;
        }

        void InstallDefaultHandler(int sig) {
#if defined(__ANDROID__)

            // Android L+ expose signal and sigaction symbols that override the system
            // ones. There is a bug in these functions where a request to set the handler
            // to SIG_DFL is ignored. In that case, an infinite loop is entered as the
            // signal is repeatedly sent to breakpad's signal handler.
            // To work around this, directly call the system's sigaction.
            struct kernel_sigaction sa;
            memset(&sa, 0, sizeof(sa));
            sys_sigemptyset(&sa.sa_mask);
            sa.sa_handler_ = SIG_DFL;
            sa.sa_flags = SA_RESTART;
            sys_rt_sigaction(sig, &sa, NULL, sizeof(kernel_sigset_t));
#else
            signal(sig, SIG_DFL);
#endif
        }

// The global exception handler stack. This is needed because there may exist
// multiple ExceptionHandler instances in a process. Each will have itself
// registered in this stack.
        std::vector<ExceptionHandler *> *g_handler_stack_ = NULL;
        pthread_mutex_t g_handler_stack_mutex_ = PTHREAD_MUTEX_INITIALIZER;

// sizeof(CrashContext) can be too big w.r.t the size of alternatate stack
// for SignalHandler(). Keep the crash context as a .bss field. Exception
// handlers are serialized by the |g_handler_stack_mutex_| and at most one at a
// time can use |g_crash_context_|.
        ExceptionHandler::CrashContext g_crash_context_;

        FirstChanceHandler g_first_chance_handler_ = nullptr;
    }  // namespace

// Runs before crashing: normal context.
    ExceptionHandler::ExceptionHandler(const MinidumpDescriptor &descriptor,
                                       FilterCallback filter,
                                       MinidumpCallback callback,
                                       void *callback_context,
                                       bool install_handler,
                                       const int server_fd)
            : filter_(filter),
              callback_(callback),
              callback_context_(callback_context),
              minidump_descriptor_(descriptor),
              crash_handler_(NULL) {
        if (server_fd >= 0)
            crash_generation_client_.reset(CrashGenerationClient::TryCreate(server_fd));

        if (!IsOutOfProcess() && !minidump_descriptor_.IsFD() &&
            !minidump_descriptor_.IsMicrodumpOnConsole())
            minidump_descriptor_.UpdatePath();

#if defined(__ANDROID__)
        if (minidump_descriptor_.IsMicrodumpOnConsole())
            logger::initializeCrashLogWriter();
#endif

        //请求互斥锁
        pthread_mutex_lock(&g_handler_stack_mutex_);

        // Pre-fault the crash context struct. This is to avoid failing due to OOM
        // if handling an exception when the process ran out of virtual memory.
        memset(&g_crash_context_, 0, sizeof(g_crash_context_));

        if (!g_handler_stack_)
            g_handler_stack_ = new std::vector<ExceptionHandler *>;

        ALOGD("install_handler == true");
        if (install_handler) {
            InstallAlternateStackLocked();
            InstallHandlersLocked();
        }
        g_handler_stack_->push_back(this);
        pthread_mutex_unlock(&g_handler_stack_mutex_);
    }

// Runs before crashing: normal context.
    ExceptionHandler::~ExceptionHandler() {
        pthread_mutex_lock(&g_handler_stack_mutex_);
        std::vector<ExceptionHandler *>::iterator handler =
                std::find(g_handler_stack_->begin(), g_handler_stack_->end(), this);
        g_handler_stack_->erase(handler);
        if (g_handler_stack_->empty()) {
            delete g_handler_stack_;
            g_handler_stack_ = NULL;
            RestoreAlternateStackLocked();
            RestoreHandlersLocked();
        }
        pthread_mutex_unlock(&g_handler_stack_mutex_);
    }

// Runs before crashing: normal context.
// static

    // Save the old signal handlers and install new ones. 保存就的信号处理函数，并安装新的信号处理函数
    bool ExceptionHandler::InstallHandlersLocked() {
        if (handlers_installed)
            return false;


        /**
       * int sigaction(int signum, const struct sigaction *act,
       *            struct sigaction *oldact);
       *           signum参数指出要捕获的信号类型，act参数指定新的信号处理方式，oldact参数输出先前信号的处理方式（如果不为NULL的话）。
       */


        // Fail if unable to store all the old handlers.  保存旧的信号处理函数 到kExceptionSignals[]数组
        for (int i = 0; i < kNumHandledSignals; ++i) {
            if (sigaction(kExceptionSignals[i], NULL, &old_handlers[i]) == -1)
                return false;
        }

        struct sigaction sa;
        memset(&sa, 0, sizeof(sa)); //清空sa 结构体
        sigemptyset(&sa.sa_mask); //清空sa_mask

        // Mask all exception signals when we're handling one of them.
        for (int i = 0; i < kNumHandledSignals; ++i)
            sigaddset(&sa.sa_mask, kExceptionSignals[i]);

        //指定信号处理函数 ---- 敲重点
        sa.sa_sigaction = SignalHandler;
        sa.sa_flags = SA_ONSTACK | SA_SIGINFO;

        for (int i = 0; i < kNumHandledSignals; ++i) {
            if (sigaction(kExceptionSignals[i], &sa, NULL) == -1) { //注册新的信号处理函数
                // At this point it is impractical to back out changes, and so failure to
                // install a signal is intentionally ignored.

            }
        }
        handlers_installed = true;
        return true;
    }

// This function runs in a compromised context: see the top of the file.
// Runs on the crashing thread.
// static
    void ExceptionHandler::RestoreHandlersLocked() {
        if (!handlers_installed)
            return;

        for (int i = 0; i < kNumHandledSignals; ++i) {
            if (sigaction(kExceptionSignals[i], &old_handlers[i], NULL) == -1) {
                InstallDefaultHandler(kExceptionSignals[i]);
            }
        }
        handlers_installed = false;
    }

// void ExceptionHandler::set_crash_handler(HandlerCallback callback) {
//   crash_handler_ = callback;
// }

// This function runs in a compromised context: see the top of the file.
// Runs on the crashing thread.
// static

    // SingalHandler  自定义的信号处理函数
     void ExceptionHandler::SignalHandler(int sig, siginfo_t *info, void *uc) {

        ALOGD("==========SignalHandler==========sinal no:%d,signal code:%d,process:%d,thread:%d",info->si_signo,info->si_code,getpid(),gettid());


        // Give the first chance handler a chance to recover from this signal
        //
        // This is primarily used by V8. V8 uses guard regions to guarantee memory
        // safety in WebAssembly. This means some signals might be expected if they
        // originate from Wasm code while accessing the guard region. We give V8 the
        // chance to handle and recover from these signals first.
        if (g_first_chance_handler_ != nullptr &&
            g_first_chance_handler_(sig, info, uc)) {
            return;
        }

        // All the exception signals are blocked at this point.  抢夺互斥锁
        pthread_mutex_lock(&g_handler_stack_mutex_);

        // Sometimes, Breakpad runs inside a process where some other buggy code
        // saves and restores signal handlers temporarily with 'signal'
        // instead of 'sigaction'. This loses the SA_SIGINFO flag associated
        // with this function. As a consequence, the values of 'info' and 'uc'
        // become totally bogus, generally inducing a crash. //bogus - 虚假的
        //
        // The following code tries to detect this case. When it does, it
        // resets the signal handlers with sigaction + SA_SIGINFO and returns.
        // This forces the signal to be thrown again, but this time the kernel
        // will call the function with the right arguments.

        //如果进程中存在其他的调试代码临时存储了信号导致状态异常,此处尝试恢复信号sg_flags并 重新抛出信号。
        struct sigaction cur_handler;
        if (sigaction(sig, NULL, &cur_handler) == 0 &&
            cur_handler.sa_sigaction == SignalHandler &&
            (cur_handler.sa_flags & SA_SIGINFO) == 0) {
            // Reset signal handler with the right flags.
            sigemptyset(&cur_handler.sa_mask);
            sigaddset(&cur_handler.sa_mask, sig);

            cur_handler.sa_sigaction = SignalHandler;
            cur_handler.sa_flags = SA_ONSTACK | SA_SIGINFO;

            if (sigaction(sig, &cur_handler, NULL) == -1) {
                // When resetting the handler fails, try to reset the
                // default one to avoid an infinite loop here.

                InstallDefaultHandler(sig);
            }
            pthread_mutex_unlock(&g_handler_stack_mutex_);
            return;
        }



        bool handled = false;
        for (int i = g_handler_stack_->size() - 1; !handled && i >= 0; --i) {
            ALOGD("begin HandleSignal:%d",i);
            handled = (*g_handler_stack_)[i]->HandleSignal(sig, info, uc);
        }


        // Upon returning from this signal handler, sig will become unmasked and then
        // it will be retriggered. If one of the ExceptionHandlers handled it
        // successfully, restore the default handler. Otherwise, restore the
        // previously installed handler. Then, when the signal is retriggered, it will
        // be delivered to the appropriate handler.

        ALOGD("begin HandleSignal:handled:%d",handled);
        if (handled) {
            //
            InstallDefaultHandler(sig);//恢复系统默认的信号处理函数
        } else {
            //恢复旧的信号处理函数
            RestoreHandlersLocked();
        }

        pthread_mutex_unlock(&g_handler_stack_mutex_);

        // info->si_code <= 0 iff SI_FROMUSER (SI_FROMKERNEL otherwise).
        if (info->si_code <= 0 || sig == SIGABRT) {
            // This signal was triggered by somebody sending us the signal with kill().
            // In order to retrigger it, we have to queue a new signal by calling
            // kill() ourselves.  The special case (si_pid == 0 && sig == SIGABRT) is
            // due to the kernel sending a SIGABRT from a user request via SysRQ.
            if (sys_tgkill(getpid(), syscall(__NR_gettid), sig) < 0) {
                // If we failed to kill ourselves (e.g. because a sandbox disallows us
                // to do so), we instead resort to terminating our process. This will
                // result in an incorrect exit code.
                _exit(1);
            }
        } else {
            // This was a synchronous signal triggered by a hard fault (e.g. SIGSEGV).
            // No need to reissue the signal. It will automatically trigger again,
            // when we return from the signal handler.
        }
    }

    //ThreadArgument 是一个自定义的结构体
    struct ThreadArgument {
        pid_t pid;  // the crashing process
        const MinidumpDescriptor *minidump_descriptor;
        ExceptionHandler *handler;
        const void *context;  // a CrashContext structure
        size_t context_size;
    };

// This is the entry function for the cloned process. We are in a compromised
// context here: see the top of the file.
// static
// Clone 子进程的入口函数 ThreadEntry 其实就是一个自定义的普通函数 int(void*)
    int ExceptionHandler::ThreadEntry(void *arg) {

        ALOGD("ThreadEntry run in child process, processId:%d",getpid());
        const ThreadArgument *thread_arg = reinterpret_cast<ThreadArgument *>(arg);

        //获取crash信息和堆栈信息
        CrashContext *context = (CrashContext *)thread_arg->context;
        char *keyInfo =  getCrashKeyInfo(context->siginfo,context->tid,context->context);

        // Close the write end of the pipe. This allows us to fail if the parent dies
        // while waiting for the continue signal.

        thread_arg->handler->sendKeyInfoBack(keyInfo);
        free(keyInfo);
        sys_close(thread_arg->handler->fdes[1]);//关闭fdes[1]

        // Block here until the crashing process unblocks us when
        // we're allowed to use ptrace
        thread_arg->handler->WaitForContinueSignal(); //读fdes[0]
        sys_close(thread_arg->handler->fdes[0]);

        return thread_arg->handler->DoDump(thread_arg->pid, thread_arg->context,
                                           thread_arg->context_size) == false;
    }

// This function runs in a compromised context: see the top of the file.
// Runs on the crashing thread.

    //---- 正常处理某个信号的操作
    //---- 返回true 代表信号量被消费，false表示信号量未被消息
    bool ExceptionHandler::HandleSignal(int /*sig*/, siginfo_t *info, void *uc) {
        if (filter_ && !filter_(callback_context_))
            return false;

        // Allow ourselves to be dumped if the signal is trusted.
        bool signal_trusted = info->si_code > 0;
        bool signal_pid_trusted = info->si_code == SI_USER ||
                                  info->si_code == SI_TKILL;
        if (signal_trusted || (signal_pid_trusted && info->si_pid == getpid())) {
            sys_prctl(PR_SET_DUMPABLE, 1, 0, 0, 0);
            ALOGD("sys_prctl(PR_SET_DUMPABLE)");
        }

        // Fill in all the holes in the struct to make Valgrind happy.
        memset(&g_crash_context_, 0, sizeof(g_crash_context_));
        memcpy(&g_crash_context_.siginfo, info, sizeof(siginfo_t));
        memcpy(&g_crash_context_.context, uc, sizeof(ucontext_t));
#if defined(__aarch64__)
        ucontext_t* uc_ptr = (ucontext_t*)uc;
        struct fpsimd_context* fp_ptr =
            (struct fpsimd_context*)&uc_ptr->uc_mcontext.__reserved;
        if (fp_ptr->head.magic == FPSIMD_MAGIC) {
          memcpy(&g_crash_context_.float_state, fp_ptr,
                 sizeof(g_crash_context_.float_state));
        }
#elif !defined(__ARM_EABI__) && !defined(__mips__)
        // FP state is not part of user ABI on ARM Linux.
        // In case of MIPS Linux FP state is already part of ucontext_t
        // and 'float_state' is not a member of CrashContext.
        ucontext_t* uc_ptr = (ucontext_t*)uc;
        if (uc_ptr->uc_mcontext.fpregs) {
          memcpy(&g_crash_context_.float_state, uc_ptr->uc_mcontext.fpregs,
                 sizeof(g_crash_context_.float_state));
        }
#endif
        g_crash_context_.tid = syscall(__NR_gettid);
        if (crash_handler_ != NULL) {
            if (crash_handler_(&g_crash_context_, sizeof(g_crash_context_),
                               callback_context_)) {
                return true;
            }
        }

        return GenerateDump(&g_crash_context_);
    }

// This is a public interface to HandleSignal that allows the client to
// generate a crash dump. This function may run in a compromised context.
    bool ExceptionHandler::SimulateSignalDelivery(int sig) {
        siginfo_t siginfo = {};
        // Mimic a trusted signal to allow tracing the process (see
        // ExceptionHandler::HandleSignal().
        siginfo.si_code = SI_USER;
        siginfo.si_pid = getpid();
        ucontext_t context;
        getcontext(&context);
        return HandleSignal(sig, &siginfo, &context);
    }

// This function may run in a compromised context: see the top of the file.


    typedef int (*t_backtrace)
            (void **buffer, int size);
    //生成miniDump的方法
    bool ExceptionHandler::GenerateDump(CrashContext *context) {
        if (IsOutOfProcess())
            return crash_generation_client_->RequestDump(context, sizeof(*context));

        // Allocating too much stack isn't a problem, and better to err on the side
        // of caution than smash it into random locations.
        static const unsigned kChildStackSize = 16000*100; //子进程申请的堆栈大小
        PageAllocator allocator;
        uint8_t *stack = reinterpret_cast<uint8_t *>(allocator.Alloc(kChildStackSize));
        if (!stack)
            return false;
        // clone() needs the top-most address. (scrub just to be safe)
        stack += kChildStackSize;
        my_memset(stack - 16, 0, 16);

        ThreadArgument thread_arg;
        thread_arg.handler = this;
        thread_arg.minidump_descriptor = &minidump_descriptor_;
        thread_arg.pid = getpid();
        thread_arg.context = context;
        thread_arg.context_size = sizeof(*context);

        // We need to explicitly enable ptrace of parent processes on some
        // kernels, but we need to know the PID of the cloned process before we
        // can do this. Create a pipe here which we can use to block the
        // cloned process after creating it, until we have explicitly enabled ptrace

        //(1)创建管道
        if (sys_pipe(fdes) == -1) {//创建管道
            // Creating the pipe failed. We'll log an error but carry on anyway,
            // as we'll probably still get a useful crash report. All that will happen
            // is the write() and read() calls will fail with EBADF
            static const char no_pipe_msg[] = "ExceptionHandler::GenerateDump "
                                              "sys_pipe failed:";
            logger::write(no_pipe_msg, sizeof(no_pipe_msg) - 1);
            logger::write(strerror(errno), strlen(strerror(errno)));
            logger::write("\n", 1);

            // Ensure fdes[0] and fdes[1] are invalid file descriptors.
            fdes[0] = fdes[1] = -1;
        }


        //(2) Clone()出子进程
        const pid_t child = sys_clone(
                ThreadEntry, stack, CLONE_FS | CLONE_UNTRACED, &thread_arg, NULL, NULL,
                NULL);
        if (child == -1) {
            sys_close(fdes[0]);
            sys_close(fdes[1]);
            return false;
        }

        //从子进程读取 提取的crash信息
        char * crashKeyinfo = readKeyInfoInSubProcess();
        // Close the read end of the pipe.
        sys_close(fdes[0]);//关掉"读"管道文件

        // Allow the child to ptrace us
        sys_prctl(PR_SET_PTRACER, child, 0, 0, 0);//允许子进程ptrace 父进程
        SendContinueSignalToChild();
        int status = 0;
        const int r = HANDLE_EINTR(sys_waitpid(child, &status, __WALL)); //sys_waitpid 等待子进程child结束

        sys_close(fdes[1]);

        if (r == -1) {
            static const char msg[] = "ExceptionHandler::GenerateDump waitpid failed:";
            logger::write(msg, sizeof(msg) - 1);
            logger::write(strerror(errno), strlen(strerror(errno)));
            logger::write("\n", 1);
        }


        bool success = r != -1 && WIFEXITED(status) && WEXITSTATUS(status) == 0;
        ALOGD("WIFEXITED(status):%d,WEXITSTATUS(status):%d,r:%d,success:%d",WIFEXITED(status),WEXITSTATUS(status),r,success);
        // ---- 对外暴露的 DumpCallback 回调


        if (callback_)
            success = callback_(minidump_descriptor_, callback_context_, success,crashKeyinfo);
        //sucess = true 代表该crash已经被正确处理,不需再传递给默认的信号处理器
        free(crashKeyinfo);
        return success;
    }

// This function runs in a compromised context: see the top of the file.
    void ExceptionHandler::SendContinueSignalToChild() {
        static const char okToContinueMessage = 'a';
        int r;
        r = HANDLE_EINTR(sys_write(fdes[1], &okToContinueMessage, sizeof(char)));
        if (r == -1) {
            static const char msg[] = "ExceptionHandler::SendContinueSignalToChild "
                                      "sys_write failed:";
            logger::write(msg, sizeof(msg) - 1);
            logger::write(strerror(errno), strlen(strerror(errno)));
            logger::write("\n", 1);
        }
    }

// This function runs in a compromised context: see the top of the file.
// Runs on the cloned process.
    void ExceptionHandler::WaitForContinueSignal() {
        int r;
        char receivedMessage;
        r = HANDLE_EINTR(sys_read(fdes[0], &receivedMessage, sizeof(char)));
        if (r == -1) {
            static const char msg[] = "ExceptionHandler::WaitForContinueSignal "
                                      "sys_read failed:";
            logger::write(msg, sizeof(msg) - 1);
            logger::write(strerror(errno), strlen(strerror(errno)));
            logger::write("\n", 1);
        }
    }


    void ExceptionHandler::sendKeyInfoBack(char * msg){
        int r = HANDLE_EINTR(sys_write(fdes[1], msg, strlen(msg)));
        if (r == -1) {
            static const char msg[] = "ExceptionHandler::sendKeyInfoBack "
                                      "sys_read failed:";
            logger::write(msg, sizeof(msg) - 1);
            logger::write(strerror(errno), strlen(strerror(errno)));
            logger::write("\n", 1);
        }
    }

    int crashInfoBuffeserSize = 3000;
    char* ExceptionHandler::readKeyInfoInSubProcess(){
        char * infobuffer =(char*)malloc(crashInfoBuffeserSize);
        int r = HANDLE_EINTR(sys_read(fdes[0], infobuffer, crashInfoBuffeserSize));
//        ALOGD("readKeyInfoInSubProcess:%s",infobuffer);
        if (r == -1) {
            static const char msg[] = "ExceptionHandler::readKeyInfoInSubProcess "
                                      "sys_read failed:";
            logger::write(msg, sizeof(msg) - 1);
            logger::write(strerror(errno), strlen(strerror(errno)));
            logger::write("\n", 1);
        }
        return infobuffer;
    }


// This function runs in a compromised context: see the top of the file.
// Runs on the cloned process.
    bool ExceptionHandler::DoDump(pid_t crashing_process, const void *context,
                                  size_t context_size) {
        const bool may_skip_dump =
                minidump_descriptor_.skip_dump_if_principal_mapping_not_referenced();
        const uintptr_t principal_mapping_address =
                minidump_descriptor_.address_within_principal_mapping();
        const bool sanitize_stacks = minidump_descriptor_.sanitize_stacks();
        if (minidump_descriptor_.IsMicrodumpOnConsole()) {
            return google_breakpad::WriteMicrodump(
                    crashing_process,
                    context,
                    context_size,
                    mapping_list_,
                    may_skip_dump,
                    principal_mapping_address,
                    sanitize_stacks,
                    *minidump_descriptor_.microdump_extra_info());
        }
        if (minidump_descriptor_.IsFD()) {
            return google_breakpad::WriteMinidump(minidump_descriptor_.fd(),
                                                  minidump_descriptor_.size_limit(),
                                                  crashing_process,
                                                  context,
                                                  context_size,
                                                  mapping_list_,
                                                  app_memory_list_,
                                                  may_skip_dump,
                                                  principal_mapping_address,
                                                  sanitize_stacks);
        }
        return google_breakpad::WriteMinidump(minidump_descriptor_.path(),
                                              minidump_descriptor_.size_limit(),
                                              crashing_process,
                                              context,
                                              context_size,
                                              mapping_list_,
                                              app_memory_list_,
                                              may_skip_dump,
                                              principal_mapping_address,
                                              sanitize_stacks);
    }

// static
    bool ExceptionHandler::WriteMinidump(const string &dump_path,
                                         MinidumpCallback callback,
                                         void *callback_context) {
        MinidumpDescriptor descriptor(dump_path);
        ExceptionHandler eh(descriptor, NULL, callback, callback_context, false, -1);
        return eh.WriteMinidump();
    }

// In order to making using EBP to calculate the desired value for ESP
// a valid operation, ensure that this function is compiled with a
// frame pointer using the following attribute. This attribute
// is supported on GCC but not on clang.
#if defined(__i386__) && defined(__GNUC__) && !defined(__clang__)
    __attribute__((optimize("no-omit-frame-pointer")))
#endif

    bool ExceptionHandler::WriteMinidump() {
        if (!IsOutOfProcess() && !minidump_descriptor_.IsFD() &&
            !minidump_descriptor_.IsMicrodumpOnConsole()) {
            // Update the path of the minidump so that this can be called multiple times
            // and new files are created for each minidump.  This is done before the
            // generation happens, as clients may want to access the MinidumpDescriptor
            // after this call to find the exact path to the minidump file.
            minidump_descriptor_.UpdatePath();
        } else if (minidump_descriptor_.IsFD()) {
            // Reposition the FD to its beginning and resize it to get rid of the
            // previous minidump info.
            lseek(minidump_descriptor_.fd(), 0, SEEK_SET);
            ignore_result(ftruncate(minidump_descriptor_.fd(), 0));
        }

        // Allow this process to be dumped.
        sys_prctl(PR_SET_DUMPABLE, 1, 0, 0, 0);

        CrashContext context;
        int getcontext_result = getcontext(&context.context);
        if (getcontext_result)
            return false;

#if defined(__i386__)
        // In CPUFillFromUContext in minidumpwriter.cc the stack pointer is retrieved
        // from REG_UESP instead of from REG_ESP. REG_UESP is the user stack pointer
        // and it only makes sense when running in kernel mode with a different stack
        // pointer. When WriteMiniDump is called during normal processing REG_UESP is
        // zero which leads to bad minidump files.
        if (!context.context.uc_mcontext.gregs[REG_UESP]) {
          // If REG_UESP is set to REG_ESP then that includes the stack space for the
          // CrashContext object in this function, which is about 128 KB. Since the
          // Linux dumper only records 32 KB of stack this would mean that nothing
          // useful would be recorded. A better option is to set REG_UESP to REG_EBP,
          // perhaps with a small negative offset in case there is any code that
          // objects to them being equal.
          context.context.uc_mcontext.gregs[REG_UESP] =
            context.context.uc_mcontext.gregs[REG_EBP] - 16;
          // The stack saving is based off of REG_ESP so it must be set to match the
          // new REG_UESP.
          context.context.uc_mcontext.gregs[REG_ESP] =
            context.context.uc_mcontext.gregs[REG_UESP];
        }
#endif

#if !defined(__ARM_EABI__) && !defined(__aarch64__) && !defined(__mips__)
        // FPU state is not part of ARM EABI ucontext_t.
        memcpy(&context.float_state, context.context.uc_mcontext.fpregs,
               sizeof(context.float_state));
#endif
        context.tid = sys_gettid();

        // Add an exception stream to the minidump for better reporting.
        memset(&context.siginfo, 0, sizeof(context.siginfo));
        context.siginfo.si_signo = MD_EXCEPTION_CODE_LIN_DUMP_REQUESTED;
#if defined(__i386__)
        context.siginfo.si_addr =
            reinterpret_cast<void*>(context.context.uc_mcontext.gregs[REG_EIP]);
#elif defined(__x86_64__)
        context.siginfo.si_addr =
            reinterpret_cast<void*>(context.context.uc_mcontext.gregs[REG_RIP]);
#elif defined(__arm__)
        context.siginfo.si_addr =
                reinterpret_cast<void *>(context.context.uc_mcontext.arm_pc);
#elif defined(__aarch64__)
        context.siginfo.si_addr =
            reinterpret_cast<void*>(context.context.uc_mcontext.pc);
#elif defined(__mips__)
        context.siginfo.si_addr =
            reinterpret_cast<void*>(context.context.uc_mcontext.pc);
#else
#error "This code has not been ported to your platform yet."
#endif

        return GenerateDump(&context);
    }

    void ExceptionHandler::AddMappingInfo(const string &name,
                                          const uint8_t identifier[sizeof(MDGUID)],
                                          uintptr_t start_address,
                                          size_t mapping_size,
                                          size_t file_offset) {
        MappingInfo info;
        info.start_addr = start_address;
        info.size = mapping_size;
        info.offset = file_offset;
        strncpy(info.name, name.c_str(), sizeof(info.name) - 1);
        info.name[sizeof(info.name) - 1] = '\0';

        MappingEntry mapping;
        mapping.first = info;
        memcpy(mapping.second, identifier, sizeof(MDGUID));
        mapping_list_.push_back(mapping);
    }

    void ExceptionHandler::RegisterAppMemory(void *ptr, size_t length) {
        AppMemoryList::iterator iter =
                std::find(app_memory_list_.begin(), app_memory_list_.end(), ptr);
        if (iter != app_memory_list_.end()) {
            // Don't allow registering the same pointer twice.
            return;
        }

        AppMemory app_memory;
        app_memory.ptr = ptr;
        app_memory.length = length;
        app_memory_list_.push_back(app_memory);
    }

    void ExceptionHandler::UnregisterAppMemory(void *ptr) {
        AppMemoryList::iterator iter =
                std::find(app_memory_list_.begin(), app_memory_list_.end(), ptr);
        if (iter != app_memory_list_.end()) {
            app_memory_list_.erase(iter);
        }
    }

// static
    bool ExceptionHandler::WriteMinidumpForChild(pid_t child,
                                                 pid_t child_blamed_thread,
                                                 const string &dump_path,
                                                 MinidumpCallback callback,
                                                 void *callback_context) {
        // This function is not run in a compromised context.
        MinidumpDescriptor descriptor(dump_path);
        descriptor.UpdatePath();
        if (!google_breakpad::WriteMinidump(descriptor.path(),
                                            child,
                                            child_blamed_thread))
            return false;

        return callback ? callback(descriptor, callback_context, true,"111111") : true;
    }

    void SetFirstChanceExceptionHandler(FirstChanceHandler callback) {
        g_first_chance_handler_ = callback;
    }

}  // namespace google_breakpad
