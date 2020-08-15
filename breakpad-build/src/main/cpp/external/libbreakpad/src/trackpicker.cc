

#include <dlfcn.h>
#include <malloc.h>
#include <stdlib.h>
#include <unistd.h>
#include "trackpicker.h"
#include "../../../mylog.h"
#include "errno.h"
#define __USE_GNU
#include <ucontext.h>

#include <cxxabi.h>
#ifdef __cplusplus
extern "C" {
#endif

#ifdef __ANDROID__
#define USE_UNWIND
#define USE_CORKSCREW
#define USE_LIBUNWIND
#endif

/* 纯C环境下，定义宏NO_CPP_DEMANGLE */
//#if (!defined(__cplusplus)) && (!defined(NO_CPP_DEMANGLE))
//# define NO_CPP_DEMANGLE
//#endif

// NO_CPP_DEMANGLE  代表非纯C 环境
#ifndef NO_CPP_DEMANGLE
# ifdef __cplusplus
	using __cxxabiv1::__cxa_demangle;
# endif
#endif

char* LINESEPERATOR = "\t\n";
int buffersize = 3000;//crash信息缓存buffer的大小
int THREAD_NAME_LENGTH =500; //线程名的最大字符长度

/* Alternative stack size. */
#define SIG_STACK_BUFFER_SIZE (SIGSTKSZ)


#ifdef USE_LIBUNWIND
void* uframes[BACKTRACE_FRAMES_MAX];
extern int coffeecatch_is_dll(const char *name);
extern void format_pc_address(char *buffer, size_t buffer_size, uintptr_t pc);
#endif


#ifdef USE_CORKSCREW

backtrace_frame_t crash_frames[BACKTRACE_FRAMES_MAX];
size_t coffeecatch_backtrace_signal(siginfo_t* si, void* sc,
                                    backtrace_frame_t* frames,
                                    size_t ignore_depth,
                                    size_t max_depth);
#endif
extern size_t getTheCrashFrames(siginfo_t* si, void* sc,
                                backtrace_frame_t* frames,
                                size_t ignore_depth,
                                size_t max_depth);
extern uintptr_t coffeecatch_get_pc_from_ucontext(const ucontext_t *uc);

typedef struct t_print_fun {
    char *buffer;
    size_t buffer_size;
} t_print_fun;


/**
 * 获取进程名字
 * @param pid 进程ID
 * @return
 */
char * getProcessNameById(pid_t pid){
    int BUF_SIZE = 200;
    char proc_pid_path[BUF_SIZE];
    char buf[BUF_SIZE];
    sprintf(proc_pid_path, "/proc/%d/status", pid);
    FILE* fp = fopen(proc_pid_path, "r");

    if(NULL != fp){
        if(fgets(buf,BUF_SIZE-1,fp) == NULL){
            fclose(fp);
            return "null";
        }
        fclose(fp);

        int index= -1;

        int str_len = strlen(buf);
        for(int i = 0;i< str_len;i++){
            if(buf[i]==':' ){
                index = i+1;
                break;
            }
        }
        char * name ;
        if(index < str_len){
            name =buf+index;
        } else{
            name = "null";
        }
        int tmpindex = strlen(name);
        while (name[tmpindex-1]=='\n'){
            ALOGD("getProcessNameById: tmpindex:%d",tmpindex);
            name[tmpindex-1]='\0';
            tmpindex = strlen(name);

        }
        ALOGD("getProcessNameById:%d,processName:%s",pid,name);

        return name;
    }

    return "null";

}

/**
 * 获取线程名字
 * @param tid 线程ID
 * @return
 */
char* getThreadNameById(pid_t tid) {
    if (tid <= 1) {
        return NULL;
    }
    char* path = (char *) calloc(1, 80);
    char* line = (char *) calloc(1, THREAD_NAME_LENGTH);

    snprintf(path, PATH_MAX, "proc/%d/comm", tid);
    FILE* commFile = NULL;
    if (commFile = fopen(path, "r")) {
        fgets(line, THREAD_NAME_LENGTH, commFile);
        fclose(commFile);
    }
    free(path);
    if (line) {
        int length = strlen(line);
        if (line[length - 1] == '\n') {
            line[length - 1] = '\0';
        }
    }
    return line;
}

/**
 * 获取singal 的描述符
 * @param sig
 * @param code
 * @return
 */
const char * getCrashSingalNoDes(int sig, int code){

    switch(sig) {
        case SIGILL:
            return "SIGILL";
        case SIGFPE:
            return "SIGFPE";
            break;
        case SIGSEGV:
            return "SIGSEGV";
            break;
        case SIGBUS:
            return "SIGBUS";
            break;
        case SIGTRAP:
            return "SIGTRAP";
            break;
        case SIGCHLD:
            return "SIGCHLD";
            break;
        case SIGPOLL:
            return "SIGPOLL";
        case SIGABRT:
            return "SIGABRT";
        case SIGALRM:
            return "SIGALRM";
        case SIGCONT:
            return "SIGCONT";
        case SIGHUP:
            return "SIGHUP";
        case SIGINT:
            return "SIGINT";
        case SIGKILL:
            return "SIGKILL";
        case SIGPIPE:
            return "SIGPIPE";
        case SIGQUIT:
            return "SIGQUIT";
        case SIGSTOP:
            return "SIGSTOP";
        case SIGTERM:
            return "SIGTERM";
        case SIGTSTP:
            return "SIGTSTP";
        case SIGTTIN:
            return "SIGTTIN";
        case SIGTTOU:
            return "SIGTTOU";
        case SIGUSR1:
            return "SIGUSR1";
        case SIGUSR2:
            return "SIGUSR2";
        case SIGPROF:
            return "SIGPROF";
        case SIGSYS:
            return "SIGSYS";
        case SIGVTALRM:
            return "SIGVTALRM";
        case SIGURG:
            return "SIGURG";
        case SIGXCPU:
            return "SIGXCPU";
        case SIGXFSZ:
            return "SIGXFSZ";
        default:
            return "unkown";
            break;
    }
}

/**
 * 获取signal code 的描述符
 * @param sig
 * @param code
 * @return
 */
const char * getCrashSingalCodeDes(int sig, int code){

    switch(sig) {
        case SIGILL:
            switch(code) {
                case ILL_ILLOPC:
                    return "IILL_ILLOPC";
                case ILL_ILLOPN:
                    return "ILL_ILLOPN";
                case ILL_ILLADR:
                    return "ILL_ILLADR";
                case ILL_ILLTRP:
                    return "ILL_ILLTRP";
                case ILL_PRVOPC:
                    return "ILL_PRVOPC";
                case ILL_PRVREG:
                    return "ILL_PRVREG";
                case ILL_COPROC:
                    return "ILL_COPROC";
                case ILL_BADSTK:
                    return "ILL_BADSTK";
                default:
                    return "ILL_BADSTK";
            }
            break;
        case SIGFPE:
            switch(code) {
                case FPE_INTDIV:
                    return "FPE_INTDIV";
                case FPE_INTOVF:
                    return "IFPE_INTOVF";
                case FPE_FLTDIV:
                    return "FPE_FLTDIV";
                case FPE_FLTOVF:
                    return "FPE_FLTOVF";
                case FPE_FLTUND:
                    return "FPE_FLTUND";
                case FPE_FLTRES:
                    return "FPE_FLTRES";
                case FPE_FLTINV:
                    return "FPE_FLTINV";
                case FPE_FLTSUB:
                    return "FPE_FLTSUB";
                default:
                    return "Floating-point";
            }
            break;
        case SIGSEGV:
            switch(code) {
                case SEGV_MAPERR:
                    return "SEGV_MAPERR";
                case SEGV_ACCERR:
                    return "SEGV_ACCERR";
                default:
                    return "Segmentation violation";
            }
            break;
        case SIGBUS:
            switch(code) {
                case BUS_ADRALN:
                    return "BUS_ADRALN";
                case BUS_ADRERR:
                    return "BUS_ADRALN";
                case BUS_OBJERR:
                    return "BUS_OBJERR";
                default:
                    return "Bus error";
            }
            break;
        case SIGTRAP:
            switch(code) {
                case TRAP_BRKPT:
                    return "TRAP_BRKPT";
                case TRAP_TRACE:
                    return "TRAP_TRACE";
                default:
                    return "Trap";
            }
            break;
        case SIGCHLD:
            switch(code) {
                case CLD_EXITED:
                    return "CLD_EXITED";
                case CLD_KILLED:
                    return "CLD_KILLED";
                case CLD_DUMPED:
                    return "CLD_DUMPED";
                case CLD_TRAPPED:
                    return "CLD_TRAPPED";
                case CLD_STOPPED:
                    return "CLD_STOPPED";
                case CLD_CONTINUED:
                    return "CLD_CONTINUED";
                default:
                    return "Child";
            }
            break;
        case SIGPOLL:
            switch(code) {
                case POLL_IN:
                    return "POLL_IN";
                case POLL_OUT:
                    return "POLL_OUT";
                case POLL_MSG:
                    return "POLL_MSG";
                case POLL_ERR:
                    return "POLL_ERR";
                case POLL_PRI:
                    return "POLL_PRI";
                case POLL_HUP:
                    return "POLL_HUP";
                default:
                    return "Pool";
            }
            break;
        case SIGABRT:
            return "SIGABRT";
        case SIGALRM:
            return "SIGALRM";
        case SIGCONT:
            return "SIGCONT";
        case SIGHUP:
            return "SIGHUP";
        case SIGINT:
            return "SIGINT";
        case SIGKILL:
            return "SIGKILL";
        case SIGPIPE:
            return "SIGPIPE";
        case SIGQUIT:
            return "SIGQUIT";
        case SIGSTOP:
            return "SIGSTOP";
        case SIGTERM:
            return "SIGTERM";
        case SIGTSTP:
            return "SIGTSTP";
        case SIGTTIN:
            return "SIGTTIN";
        case SIGTTOU:
            return "SIGTTOU";
        case SIGUSR1:
            return "SIGUSR1";
        case SIGUSR2:
            return "SIGUSR2";
        case SIGPROF:
            return "SIGPROF";
        case SIGSYS:
            return "SIGSYS";
        case SIGVTALRM:
            return "SIGVTALRM";
        case SIGURG:
            return "SIGURG";
        case SIGXCPU:
            return "SIGXCPU";
        case SIGXFSZ:
            return "SIGXFSZ";
        default:
            switch(code) {
                case SI_USER:
                    return "SI_USER";
                case SI_QUEUE:
                    return "SI_QUEUE";
                case SI_TIMER:
                    return "SI_TIMER";
                case SI_ASYNCIO:
                    return "SI_ASYNCIO";
                case SI_MESGQ:
                    return
                            "SI_MESGQ";
                default:
                    return "Unknown signal";
            }
            break;
    }
}

/**
 * 获取crash原因描述 (结合signal number 和signal code)
 * @param sig sinal number
 * @param code singal code
 * @return
 */
const char* getCrashSingalDes(int sig, int code) {
    switch(sig) {
        case SIGILL:
            switch(code) {
                case ILL_ILLOPC:
                    return "Illegal opcode";
                case ILL_ILLOPN:
                    return "Illegal operand";
                case ILL_ILLADR:
                    return "Illegal addressing mode";
                case ILL_ILLTRP:
                    return "Illegal trap";
                case ILL_PRVOPC:
                    return "Privileged opcode";
                case ILL_PRVREG:
                    return "Privileged register";
                case ILL_COPROC:
                    return "Coprocessor error";
                case ILL_BADSTK:
                    return "Internal stack error";
                default:
                    return "Illegal operation";
            }
            break;
        case SIGFPE:
            switch(code) {
                case FPE_INTDIV:
                    return "Integer divide by zero";
                case FPE_INTOVF:
                    return "Integer overflow";
                case FPE_FLTDIV:
                    return "Floating-point divide by zero";
                case FPE_FLTOVF:
                    return "Floating-point overflow";
                case FPE_FLTUND:
                    return "Floating-point underflow";
                case FPE_FLTRES:
                    return "Floating-point inexact result";
                case FPE_FLTINV:
                    return "Invalid floating-point operation";
                case FPE_FLTSUB:
                    return "Subscript out of range";
                default:
                    return "Floating-point";
            }
            break;
        case SIGSEGV:
            switch(code) {
                case SEGV_MAPERR:
                    return "Address not mapped to object";
                case SEGV_ACCERR:
                    return "Invalid permissions for mapped object";
                default:
                    return "Segmentation violation";
            }
            break;
        case SIGBUS:
            switch(code) {
                case BUS_ADRALN:
                    return "Invalid address alignment";
                case BUS_ADRERR:
                    return "Nonexistent physical address";
                case BUS_OBJERR:
                    return "Object-specific hardware error";
                default:
                    return "Bus error";
            }
            break;
        case SIGTRAP:
            switch(code) {
                case TRAP_BRKPT:
                    return "Process breakpoint";
                case TRAP_TRACE:
                    return "Process trace trap";
                default:
                    return "Trap";
            }
            break;
        case SIGCHLD:
            switch(code) {
                case CLD_EXITED:
                    return "Child has exited";
                case CLD_KILLED:
                    return "Child has terminated abnormally and did not create a core file";
                case CLD_DUMPED:
                    return "Child has terminated abnormally and created a core file";
                case CLD_TRAPPED:
                    return "Traced child has trapped";
                case CLD_STOPPED:
                    return "Child has stopped";
                case CLD_CONTINUED:
                    return "Stopped child has continued";
                default:
                    return "Child";
            }
            break;
        case SIGPOLL:
            switch(code) {
                case POLL_IN:
                    return "Data input available";
                case POLL_OUT:
                    return "Output buffers available";
                case POLL_MSG:
                    return "Input message available";
                case POLL_ERR:
                    return "I/O error";
                case POLL_PRI:
                    return "High priority input available";
                case POLL_HUP:
                    return "Device disconnected";
                default:
                    return "Pool";
            }
            break;
        case SIGABRT:
            return "Process abort signal";
        case SIGALRM:
            return "Alarm clock";
        case SIGCONT:
            return "Continue executing, if stopped";
        case SIGHUP:
            return "Hangup";
        case SIGINT:
            return "Terminal interrupt signal";
        case SIGKILL:
            return "Kill";
        case SIGPIPE:
            return "Write on a pipe with no one to read it";
        case SIGQUIT:
            return "Terminal quit signal";
        case SIGSTOP:
            return "Stop executing";
        case SIGTERM:
            return "Termination signal";
        case SIGTSTP:
            return "Terminal stop signal";
        case SIGTTIN:
            return "Background process attempting read";
        case SIGTTOU:
            return "Background process attempting write";
        case SIGUSR1:
            return "User-defined signal 1";
        case SIGUSR2:
            return "User-defined signal 2";
        case SIGPROF:
            return "Profiling timer expired";
        case SIGSYS:
            return "Bad system call";
        case SIGVTALRM:
            return "Virtual timer expired";
        case SIGURG:
            return "High bandwidth data is available at a socket";
        case SIGXCPU:
            return "CPU time limit exceeded";
        case SIGXFSZ:
            return "File size limit exceeded";
        default:
            switch(code) {
                case SI_USER:
                    return "Signal sent by kill()";
                case SI_QUEUE:
                    return "Signal sent by the sigqueue()";
                case SI_TIMER:
                    return "Signal generated by expiration of a timer set by timer_settime()";
                case SI_ASYNCIO:
                    return "Signal generated by completion of an asynchronous I/O request";
                case SI_MESGQ:
                    return
                            "Signal generated by arrival of a message on an empty message queue";
                default:
                    return "Unknown signal";
            }
            break;
    }
}


/* 判断是不是so库*/
int coffeecatch_is_dll(const char *name) {
    size_t i;
    for(i = 0; name[i] != '\0'; i++) {
        if (name[i + 0] == '.' &&
            name[i + 1] == 's' &&
            name[i + 2] == 'o' &&
            ( name[i + 3] == '\0' || name[i + 3] == '.') ) {
            return 1;
        }
    }
    return 0;
}

/**
 * 输出PC addres,对应的可读格式化信息
 * @param pc
 * @param fun
 * @param arg
 */
void format_pc_address_cb(uintptr_t pc,
                          void (*fun)(void *arg, const char *module,
                                      uintptr_t addr,
                                      const char *function,
                                      uintptr_t offset), void *arg) {
    if (pc != 0) {
        Dl_info info;
        void * const addr = (void*) pc;
        /* dladdr() returns 0 on error, and nonzero on success. */
        if (dladdr(addr, &info) != 0 && info.dli_fname != NULL) {
            const uintptr_t near = (uintptr_t) info.dli_saddr;
            const uintptr_t offs = pc - near;
            const uintptr_t addr_rel = pc - (uintptr_t) info.dli_fbase;
            /* We need the absolute address for the main module (?).
               TODO FIXME to be investigated. */
            const uintptr_t addr_to_use = coffeecatch_is_dll(info.dli_fname)
                                          ? addr_rel : pc;
            //ALOGD("format_pc_address_cb 1 :%llx,fname:%s,sname:%s",pc,info.dli_fname,info.dli_sname);
            fun(arg, info.dli_fname, addr_to_use, info.dli_sname, offs);
        } else {
            //ALOGD("format_pc_address_cb 2");
            fun(arg, NULL, pc, NULL, 0);
        }
    }
}


/**
 * 打印格式化信息
 * @param arg
 * @param module
 * @param uaddr
 * @param function
 * @param offset
 */
void print_fun(void *arg, const char *module, uintptr_t uaddr,
               const char *function, uintptr_t offset) {
    t_print_fun *const t = (t_print_fun*) arg;
    char *const buffer = t->buffer;
    const size_t buffer_size = t->buffer_size;

    const void*const addr = (void*) uaddr;
    int ret = 0;


    if (module == NULL) {
        ret = snprintf(buffer, buffer_size, "[at %p]%s", addr,LINESEPERATOR);
    } else if (function != NULL) {


#if (!defined NO_CPP_DEMANGLE) //demangle: 将mangled的函数名(带乱码) 转换成源文件中的函数函数名
        ALOGD("NO_CPP_DEMANGLE:%s",function);
        int status;
        char *tmp = __cxxabiv1::__cxa_demangle(function, NULL, 0, &status);
        if (status == 0 && tmp) {
            function = tmp;
        }
#endif

        ret = snprintf(buffer, buffer_size, "[at %s:%p (%s+0x%x)]%s", module, addr,
                       function, (int) offset,LINESEPERATOR);
    } else {
        ret =snprintf(buffer, buffer_size, "[at %s:%p]%s", module, addr,LINESEPERATOR);
    }
}

/* 输出一个PC address,对应的格式化的信息 */
void format_pc_address(char *buffer, size_t buffer_size, uintptr_t pc) {
    t_print_fun t;
    t.buffer = buffer;
    t.buffer_size = buffer_size;
    format_pc_address_cb(pc, print_fun, &t);
}


/* Use libunwind to get a backtrace inside a signal handler.
   Will only return a non-zero code on Android >= 5 (with libunwind.so
   being shipped) */


void printStackWithBuffer(int frames_size,backtrace_frame_t frames[],int skipframe, char *buffer,int buffer_size) {
    // 第七次就打印不出来了
    int buffer_offset = 0;
    int buffer_leftsize = buffer_size - buffer_offset;
    for (int i = skipframe; i < frames_size; i++) {
        const uintptr_t pc = frames[i].absolute_pc;
        format_pc_address(&buffer[buffer_offset], buffer_leftsize, pc);
        buffer_offset = strlen(buffer);
        buffer_leftsize = buffer_size - buffer_offset;
    }
}
void printStack(int frames_size,void * frames[]){
    // 第七次就打印不出来了
    int stack_buffer_size = SIG_STACK_BUFFER_SIZE;
    char *buffer = (char *)malloc(stack_buffer_size);
    int buffer_offset = 0;
    int buffer_size = stack_buffer_size;
    int buffer_leftsize = buffer_size - buffer_offset;
    for(int i = 0 ; i < frames_size ; i++) {
        const uintptr_t pc = (uintptr_t )frames[i];
        format_pc_address(&buffer[buffer_offset], buffer_leftsize, pc);
        buffer_offset = strlen(buffer);
        buffer_leftsize = buffer_size - buffer_offset;
    }

    ALOGD("printStack:\n%s",buffer);


 }



size_t getTheCrashFrames(siginfo_t* si, void* sc,
                       backtrace_frame_t* frames,
                       size_t ignore_depth,
                       size_t max_depth){
    size_t  framesize;
#ifdef USE_CORKSCREW
    framesize = coffeecatch_backtrace_signal(si,sc,frames,ignore_depth,max_depth);
    ALOGD("getTheCrashFrames USE_CORKSCREW:%d",framesize);
#endif

#ifdef USE_LIBUNWIND
    ALOGD("getTheCrashFrames before USE_LIBUNWIND:%d",framesize);
    if(framesize == 0){
        void* uframes[BACKTRACE_FRAMES_MAX] = {0};
        framesize = coffeecatch_unwind_signal(uframes,BACKTRACE_FRAMES_MAX);
        for(int i = 0;i<framesize;i++){
            frames[i].absolute_pc = (uintptr_t) uframes[i];
        }
    }
    ALOGD("getTheCrashFrames after USE_LIBUNWIND:%d",framesize);

#endif
    return framesize;
 }

/**
 * 获取crash的关键信息
 * @param siginfo
 * @param tid
 * @param context
 * @return
 */
char * getCrashKeyInfo(siginfo_t siginfo,pid_t tid,ucontext_t context){
    char * infobuffer =( char * ) malloc(buffersize);

    int buffer_offset = 0;
    int buffer_leftsize = buffersize;
    //线程信息
    pid_t pid = getpid();
    char * threadName = getThreadNameById(tid);
    char * processName = getProcessNameById(pid);
    ALOGD("getProcessNameById-processName:%s",processName);
    char * code = ( char *)siginfo.si_code;



#if  defined(__arm__64) //64位 arm

    ALOGD("feifei ======== defined arm_64:");

    //线程信息
    snprintf(infobuffer,buffersize,"threadinfo || pid:%d,processName:%s,tid:%d,threadName:%s,fault addr:%#018lx%s",getpid(),processName,gettid(),threadName,context.uc_mcontext.fault_address,LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;


    //singal信息
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"singalinfo || singal :%d (%s) code:%d desc:%s %s",siginfo.si_signo,
             getCrashSingalCodeDes(siginfo.si_signo, siginfo.si_code),siginfo.si_code,getCrashSingalDes(siginfo.si_signo,siginfo.si_code),LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;

    //regs 寄存器信息
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"Reg[0]:%#018llx,Reg[1]:%#018llx,Reg[2]:%#018llx,Reg[3]:%#018llx %s",context.uc_mcontext.regs[0],context.uc_mcontext.regs[1],context.uc_mcontext.regs[2],context.uc_mcontext.regs[3],LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"Reg[4]:%#018llx,Reg[5]:%#018llx,Reg[6]:%#018llx,Reg[7]:%#018llx %s",context.uc_mcontext.regs[4],context.uc_mcontext.regs[5],context.uc_mcontext.regs[6],context.uc_mcontext.regs[7],LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"Reg[8]:%#018llx,Reg[9]:%#018llx,Reg[10]:%#018llx,Reg[11]:%#018llx %s",context.uc_mcontext.regs[8],context.uc_mcontext.regs[9],context.uc_mcontext.regs[10],context.uc_mcontext.regs[11],LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"Reg[12]:%#018llx,Reg[13]:%#018llx,Reg[14]:%#018llx,Reg[15]:%#018llx %s",context.uc_mcontext.regs[12],context.uc_mcontext.regs[13],context.uc_mcontext.regs[14],context.uc_mcontext.regs[15],LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"Reg[16]:%#018llx,Reg[17]:%#018llx,Reg[18]:%#018llx,Reg[19]:%#018llx %s",context.uc_mcontext.regs[16],context.uc_mcontext.regs[17],context.uc_mcontext.regs[18],context.uc_mcontext.regs[19],LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"Reg[20]:%#018llx,Reg[21]:%#018llx,Reg[22]:%#018llx,Reg[23]:%#018llx %s",context.uc_mcontext.regs[20],context.uc_mcontext.regs[21],context.uc_mcontext.regs[22],context.uc_mcontext.regs[23],LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"Reg[24]:%#018llx,Reg[25]:%#018llx,Reg[26]:%#018llx,Reg[27]:%#018llx %s",context.uc_mcontext.regs[24],context.uc_mcontext.regs[25],context.uc_mcontext.regs[26],context.uc_mcontext.regs[27],LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"Reg[28]:%#018llx,Reg[29]:%#018llx,Reg[30]:%#018llx,Reg[31]:%#018llx %s",context.uc_mcontext.regs[28],context.uc_mcontext.regs[29],context.uc_mcontext.regs[30],context.uc_mcontext.regs[31],LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;


    //奔溃点 信息
    ALOGD("crash point");
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"crash point || pc:%#018llx",context.uc_mcontext.pc);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;

    format_pc_address(&infobuffer[buffer_offset], buffer_leftsize, context.uc_mcontext.pc);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;

#elif defined(__arm__)  //32位 arm

   ALOGD("feifei ======== defined arm:");

    //线程信息
    snprintf(infobuffer,buffersize,"threadinfo || pid:%d,processName:%s,tid:%d,threadName:%s,fault addr:%#018lx%s",getpid(),processName,gettid(),threadName,context.uc_mcontext.fault_address,LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;

    //singal信息
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"singalinfo || singal :%d (%s) code:%d desc:%s %s",siginfo.si_signo,
             getCrashSingalCodeDes(siginfo.si_signo, siginfo.si_code),siginfo.si_code,getCrashSingalDes(siginfo.si_signo,siginfo.si_code),LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;

    //寄存器 信息
    ALOGD("regs");
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"REGS ip 0x%3x,sp 0x%3x,lr 0x%3x, pc 0x%3x\n",context.uc_mcontext.arm_ip,context.uc_mcontext.arm_sp,context.uc_mcontext.arm_lr,context.uc_mcontext.arm_pc);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;

    //奔溃点信息
    ALOGD("crash point");
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"crash point || pc 0x%3x",context.uc_mcontext.arm_pc);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;

    format_pc_address(&infobuffer[buffer_offset], buffer_leftsize, context.uc_mcontext.arm_pc);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;


    //ALOGD("the arm_fp 0x%3x\n",context.uc_mcontext.arm_fp);
    //ALOGD("the arm_ip 0x%3x\n",context.uc_mcontext.arm_ip);
    //ALOGD("the arm_sp 0x%3x\n",context.uc_mcontext.arm_sp);
    //ALOGD("the arm_lr 0x%3x\n",context.uc_mcontext.arm_lr);
    //ALOGD("the arm_pc 0x%3x\n",context.uc_mcontext.arm_pc);
    //ALOGD("the arm_cpsr 0x%3x\n",context.uc_mcontext.arm_cpsr);
    //ALOGD("the falut_address 0x%3x\n",context.uc_mcontext.fault_address);

#elif defined(__x86_64__)
    ALOGD("feifei ======== defined __x86_64__:");
   //线程信息
    snprintf(infobuffer,buffersize,"threadinfo || pid:%d,processName:%s,tid:%d,threadName:%s %s",getpid(),processName,gettid(),threadName,LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;

    //singal信息
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"singalinfo || singal :%d (%s) code:%d desc:%s %s",siginfo.si_signo,
             getCrashSingalCodeDes(siginfo.si_signo, siginfo.si_code),siginfo.si_code,getCrashSingalDes(siginfo.si_signo,siginfo.si_code),LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;
    //寄存器 信息
    ALOGD("regs");
    for (i = 0; i < NGREG; i++) {
         snprintf(&infobuffer[buffer_offset],buffer_leftsize,"reg[%02d]: 0x%016lx %s", i, uc->uc_mcontext.gregs[i],LINESEPERATOR);
		 buffer_offset = strlen(infobuffer);
         buffer_leftsize=buffersize-buffer_offset;
         ALOGD("reg[%02d]: 0x%016lx", i, uc->uc_mcontext.gregs[i]);
    }

    //奔溃点信息
    ALOGD("crash point");
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"crash point || pc %016lx",uc->uc_mcontext.gregs[REG_RIP]);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;

    format_pc_address(&infobuffer[buffer_offset], buffer_leftsize, uc->uc_mcontext.gregs[REG_RIP]);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;

#endif

    //backstace
    ALOGD("backtrace:");
    snprintf(&infobuffer[buffer_offset],buffer_leftsize,"backtrace :%s",LINESEPERATOR);
    buffer_offset = strlen(infobuffer);
    buffer_leftsize=buffersize-buffer_offset;


    ALOGD("uframes:");
    size_t  framesize =getTheCrashFrames(&siginfo,&context,crash_frames,0,BACKTRACE_FRAMES_MAX);
    printStackWithBuffer(framesize,crash_frames,9,&infobuffer[buffer_offset],buffer_leftsize);
    ALOGD("getCrashKeyInfo.size:%d",strlen(infobuffer));

    return infobuffer;
}




// ---------------- USE_CORKSCREW
#ifdef USE_CORKSCREW
size_t coffeecatch_backtrace_signal(siginfo_t* si, void* sc,
                                    backtrace_frame_t* frames,
                                    size_t ignore_depth,
                                    size_t max_depth) {
    void *const libcorkscrew = dlopen("libcorkscrew.so", RTLD_LAZY | RTLD_LOCAL);
    if (libcorkscrew != NULL) {
        t_unwind_backtrace_signal_arch unwind_backtrace_signal_arch
                = (t_unwind_backtrace_signal_arch)
                        dlsym(libcorkscrew, "unwind_backtrace_signal_arch");
        t_acquire_my_map_info_list acquire_my_map_info_list
                = (t_acquire_my_map_info_list)
                        dlsym(libcorkscrew, "acquire_my_map_info_list");
        t_release_my_map_info_list release_my_map_info_list
                = (t_release_my_map_info_list)
                        dlsym(libcorkscrew, "release_my_map_info_list");
        if (unwind_backtrace_signal_arch != NULL
            && acquire_my_map_info_list != NULL
            && release_my_map_info_list != NULL) {
            map_info_t*const info = acquire_my_map_info_list();
            const ssize_t size =
                    unwind_backtrace_signal_arch(si, sc, info, frames, ignore_depth,
                                                 max_depth);
            release_my_map_info_list(info);
            return size >= 0 ? size : 0;
        } else {
            ALOGD("symbols not found in libcorkscrew.so\n");
        }
        dlclose(libcorkscrew);
    } else {
        ALOGD("libcorkscrew.so could not be loaded\n");
    }
    return 0;
}



int coffeecatch_backtrace_symbols(const backtrace_frame_t* backtrace,
                                  size_t frames,
                                  void (*fun)(void *arg,
                                              const backtrace_symbol_t *sym),
                                  void *arg) {
    int success = 0;
    void *const libcorkscrew = dlopen("libcorkscrew.so", RTLD_LAZY | RTLD_LOCAL);
    if (libcorkscrew != NULL) {
        t_get_backtrace_symbols get_backtrace_symbols
                = (t_get_backtrace_symbols)
                        dlsym(libcorkscrew, "get_backtrace_symbols");
        t_free_backtrace_symbols free_backtrace_symbols
                = (t_free_backtrace_symbols)
                        dlsym(libcorkscrew, "free_backtrace_symbols");
        if (get_backtrace_symbols != NULL
            && free_backtrace_symbols != NULL) {
            backtrace_symbol_t symbols[BACKTRACE_FRAMES_MAX];
            size_t i;
            if (frames > BACKTRACE_FRAMES_MAX) {
                frames = BACKTRACE_FRAMES_MAX;
            }
            get_backtrace_symbols(backtrace, frames, symbols);
            for(i = 0; i < frames; i++) {
                fun(arg, &symbols[i]);
            }
            free_backtrace_symbols(symbols, frames);
            success = 1;
        } else {
            ALOGD("symbols not found in libcorkscrew.so\n");
        }
        dlclose(libcorkscrew);
    } else {
        ALOGD("libcorkscrew.so could not be loaded\n");
    }
    return success;
}

typedef struct t_coffeecatch_backtrace_symbols_fun {
    void (*fun)(void *arg, const char *module, uintptr_t addr,
                const char *function, uintptr_t offset);
    void *arg;
} t_coffeecatch_backtrace_symbols_fun;

static void coffeecatch_backtrace_symbols_fun(void *arg, const backtrace_symbol_t *sym) {
    t_coffeecatch_backtrace_symbols_fun *const bt =
            (t_coffeecatch_backtrace_symbols_fun*) arg;
    const char *symbol = sym->demangled_name != NULL
                         ? sym->demangled_name : sym->symbol_name;
    const uintptr_t rel = sym->relative_pc - sym->relative_symbol_addr;
    bt->fun(bt->arg, sym->map_name, sym->relative_pc, symbol, rel);
}
#endif


#ifdef USE_LIBUNWIND
typedef int (*t_backtrace)
        (void **buffer, int size);

ssize_t coffeecatch_unwind_signal(void** frames,size_t max_depth) {
    ALOGD("coffeecatch_unwind_signal dopen");
    void *libunwind = dlopen("libunwind.so", RTLD_LAZY | RTLD_LOCAL);
    ALOGD("coffeecatch_unwind_signal dopen success");
    if (libunwind != NULL) {

        t_backtrace backtrace =
                (t_backtrace)dlsym(libunwind, "unw_backtrace");
        if (backtrace != NULL) {
            ALOGD("before backtrace");
            int nb = backtrace(frames, max_depth);
            ALOGD("after backtrace,nb:%d",nb);
            if (nb > 0) {
            }
            return nb;
        } else {
            ALOGD("symbols not found in libunwind.so\n");
        }
        dlclose(libunwind);
    } else {
        ALOGD("libunwind.so could not be loaded,error:%s",dlerror());
    }
    return -1;
}
#endif


///**
// * Get the program counter, given a pointer to a ucontext_t context.
// **/
// uintptr_t coffeecatch_get_pc_from_ucontext(const ucontext_t *uc) {
//#if (defined(__arm__))
//    return uc->uc_mcontext.arm_pc;
//#elif defined(__aarch64__)
//    return uc->uc_mcontext.pc;
//#elif (defined(__x86_64__))
//    return uc->uc_mcontext.gregs[REG_RIP];
//#elif (defined(__i386))
//  return uc->uc_mcontext.gregs[REG_EIP];
//#elif (defined (__ppc__)) || (defined (__powerpc__))
//  return uc->uc_mcontext.regs->nip;
//#elif (defined(__hppa__))
//  return uc->uc_mcontext.sc_iaoq[0] & ~0x3UL;
//#elif (defined(__sparc__) && defined (__arch64__))
//  return uc->uc_mcontext.mc_gregs[MC_PC];
//#elif (defined(__sparc__) && !defined (__arch64__))
//  return uc->uc_mcontext.gregs[REG_PC];
//#elif (defined(__mips__))
//  return uc->uc_mcontext.gregs[31];
//#else
//#error "Architecture is unknown, please report me!"
//#endif
}

