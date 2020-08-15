
## 一、linux 目录 为linux平台编译出来的工具，需要在linux环境使用，具体方法如下:
#### 1、 dump_syms 提取特定so库的符号信息

以libbreakpad-core.so为例：
```
./dump_syms libbreakpad-core.so > libbreakpad-core.so.sym
```
#### 2、根据1中生成的libbreakpad-core.so.sym生成特定的目录结构：

```
├── symbol
│   └── libbreakpad-core.so
│       └── 57399AA1EE2607A34686D5DED7D43C310
│           └── libbreakpad-core.so.sym
```

命令如下:

```
head -n1 libbreakpad-core.so.sym 
MODULE Linux arm64 57399AA1EE2607A34686D5DED7D43C310 libbreakpad-core.so

mkdir -p ./symbol/libbreakpad-core.so/57399AA1EE2607A34686D5DED7D43C310

mv libbreakpad-core.so.sym ./symbol/libbreakpad-core.so/57399AA1EE2607A34686D5DED7D43C310/


```

也可以直接使用install_syms.sh 脚本生成.sym和特殊的目录格式:

```
./insall_syms.sh ../libbreakpad.so

```

#### 3、调用minidump_stackwalk命令，将dmp文件和sym文件合成可读的crashinfo.txt

```
./minidump_stackwalk 8439c979-cecf-41a6-25eaf89c-dbf9c03b.dmp ./symbol > crashinfo.txt

```

crashinfo.txt 部分内容如下:
```
Operating system: Android
                  0.0.0 Linux 4.4.83 #2 SMP PREEMPT Sun Jan 12 10:48:20 CST 2020 aarch64
CPU: arm64
     4 CPUs

GPU: UNKNOWN

Crash reason:  SIGSEGV /SEGV_MAPERR
Crash address: 0x0
Process uptime: not available

Thread 32 (crashed)
 0  libbreakpad-core.so!testCrash1() + 0x14
     x0 = 0x0000000000000027    x1 = 0x00000078e60f5f30
     x2 = 0x0000000000000005    x3 = 0x0000000000000003
     x4 = 0x0000000040100401    x5 = 0xa800000040404000
     x6 = 0x0000000000000000    x7 = 0x7f7f7f7f7f7f7f7f
     x8 = 0x0000000000000000    x9 = 0x0000000000000001
    x10 = 0x00000078e60f60c0   x11 = 0x0000000000000018
    x12 = 0x000000000000000b   x13 = 0xffffffffffffffff
    x14 = 0xff00000000000000   x15 = 0xffffffffffffffff
    x16 = 0x00000078e82b7608   x17 = 0x00000078e823b454
    x18 = 0x0000000000000008   x19 = 0x00000078e7e2fe00
    x20 = 0x0000007902ae7f20   x21 = 0x00000078e7e2fe00
    x22 = 0x00000078e60f693c   x23 = 0x00000078e8f1bc31
    x24 = 0x0000000000000000   x25 = 0x00000078e60f7588
    x26 = 0x00000078e7e2fea0   x27 = 0x0000000000000000
    x28 = 0x0000000000000000    fp = 0x00000078e60f6650
     lr = 0x00000078e823b4c8    sp = 0x00000078e60f6620
     pc = 0x00000078e823b468
    Found by: given as instruction pointer in context
 1  libbreakpad-core.so!testCoffeCacher() + 0x4c
    x19 = 0x00000078e7e2fe00   x20 = 0x0000007902ae7f20
    x21 = 0x00000078e7e2fe00   x22 = 0x00000078e60f693c
    x23 = 0x00000078e8f1bc31   x24 = 0x0000000000000000
    x25 = 0x00000078e60f7588   x26 = 0x00000078e7e2fea0
    x27 = 0x0000000000000000   x28 = 0x0000000000000000
     fp = 0x00000078e60f6650    sp = 0x00000078e60f6630
     pc = 0x00000078e823b4c8
    Found by: call frame info
 2  libbreakpad-core.so!call_dangerous_function() + 0x14
    x19 = 0x00000078e7e2fe00   x20 = 0x0000007902ae7f20
    x21 = 0x00000078e7e2fe00   x22 = 0x00000078e60f693c
    x23 = 0x00000078e8f1bc31   x24 = 0x0000000000000000
    x25 = 0x00000078e60f7588   x26 = 0x00000078e7e2fea0
    x27 = 0x0000000000000000   x28 = 0x0000000000000000
     fp = 0x00000078e60f6670    sp = 0x00000078e60f6660
     pc = 0x00000078e823b508
    Found by: call frame info
 3  libbreakpad-core.so!Java_com_sogou_translate_jni_BreakPadCore_go2crash + 0x14
    x19 = 0x00000078e7e2fe00   x20 = 0x0000007902ae7f20
    x21 = 0x00000078e7e2fe00   x22 = 0x00000078e60f693c
    x23 = 0x00000078e8f1bc31   x24 = 0x0000000000000000
    x25 = 0x00000078e60f7588   x26 = 0x00000078e7e2fea0
    x27 = 0x0000000000000000   x28 = 0x0000000000000000
     fp = 0x00000078e60f6690    sp = 0x00000078e60f6680
     pc = 0x00000078e823b534
    Found by: call frame info
 4  base.odex + 0x111c0
    x19 = 0x00000078e7e2fe00   x20 = 0x0000007902ae7f20
    x21 = 0x00000078e7e2fe00   x22 = 0x00000078e60f693c
    x23 = 0x00000078e8f1bc31   x24 = 0x0000000000000000
    x25 = 0x00000078e60f7588   x26 = 0x00000078e7e2fea0
    x27 = 0x0000000000000000   x28 = 0x0000000000000000
     fp = 0x00000078e60f6768    sp = 0x00000078e60f66a0
     pc = 0x00000078e8e261c4
    Found by: call frame info

```



## 二、mac 目录 为mac平台编译出来的工具，需要在mac环境使用，具体方法如下:



#### 1、minidump_stackwalk 直接将.dmp 文件解析成可读的信息，但是缺少堆栈的解析(奔溃点的函数符号和行号)


Android studo安装目录自带了一份minidump_stackwalk自带的minidump_stackwalk可以在mac上直接运行 解析dmp文件

查找minidump_stackwalk位置
```
find / -name minidump_stackwalk
```
我mac上的目录为

```
/Applications/Android Studio.app/Contents/bin/lldb/bin/minidump_stackwalk
```

也可以直接使用“四”步中mac 环境编译生成的 minidump_stackwalk,位置如下:

```
breakpad/src/src/processor/minidump_stackwalk
```

minidump_stackwalk 解析minidump文件
```
/Users/feifei/Library/Android/sdk/lldb/3.1/bin/minidump_stackwalk 094495a4-c61d-473e-8505f986-72daf425.dmp > crash.info
```
crash.info 内容:
```
Operating system: Android
                  0.0.0 Linux 4.4.83 #2 SMP PREEMPT Sun Jan 12 10:48:20 CST 2020 aarch64
CPU: arm64
     4 CPUs

GPU: UNKNOWN

Crash reason:  SIGSEGV /SEGV_MAPERR
Crash address: 0x0
Process uptime: not available

Thread 32 (crashed)
 0  libbreakpad-core.so + 0x28468
     x0 = 0x0000000000000027    x1 = 0x00000078e6b82f30
     x2 = 0x0000000000000005    x3 = 0x0000000000000003
     x4 = 0x0000000040100401    x5 = 0xa800000040404000
     x6 = 0x0000000000000000    x7 = 0x7f7f7f7f7f7f7f7f
     x8 = 0x0000000000000000    x9 = 0x0000000000000001
    x10 = 0x00000078e6b830c0   x11 = 0x0000000000000018
    x12 = 0x000000000000000b   x13 = 0xffffffffffffffff
    x14 = 0xff00000000000000   x15 = 0xffffffffffffffff
    x16 = 0x00000078e86ec608   x17 = 0x00000078e8670454
    x18 = 0x00000078e6b8166c   x19 = 0x00000078fa1e8800
    x20 = 0x0000007902ae7f20   x21 = 0x00000078fa1e8800
    x22 = 0x00000078e6b8393c   x23 = 0x00000078e9018c31
    x24 = 0x0000000000000000   x25 = 0x00000078e6b84588
    x26 = 0x00000078fa1e88a0   x27 = 0x0000000000000000
    x28 = 0x0000000000000000    fp = 0x00000078e6b83650
     lr = 0x00000078e86704c8    sp = 0x00000078e6b83620
     pc = 0x00000078e8670468
    Found by: given as instruction pointer in context
 1  libbreakpad-core.so + 0x284c4
     fp = 0x00000078e6b83670    lr = 0x00000078e8670508
     sp = 0x00000078e6b83660    pc = 0x00000078e86704c8
    Found by: previous frame's frame pointer
 2  libbreakpad-core.so + 0x28504
     fp = 0x00000078e6b83690    lr = 0x00000078e8670534
     sp = 0x00000078e6b83680    pc = 0x00000078e8670508
    Found by: previous frame's frame pointer
 3  libbreakpad-core.so + 0x28530
```

由此可知程序 奔溃在了libbreakpad-core.so 的相对偏移位置 0x28468的地址

调用的关系如下：

```
libbreakpad-core.so + 0x28468
libbreakpad-core.so + 0x284c4
libbreakpad-core.so + 0x28504
libbreakpad-core.so + 0x28530
```

#### 2、利用addr2line 根据发生crash的so文件，以及偏移地址，得出产生carsh的方法、行数和调用堆栈关系。

aarch64-linux-android-addr2line 工具也是在android sdk 安装目录下自带的，可以自行查找。


```
feifeideMacBook-Pro:~ feifei$ find / -name aarch64-linux-android-addr2line
```
我mac上aarch64-linux-android-addr2line的目录为

```
/Users/feifei/Library/Android/sdk/ndk-bundle/toolchains/aarch64-linux-android-4.9/prebuilt/darwin-x86_64/bin/aarch64-linux-android-addr2line
```


针对上面得出的调用堆栈，解析libbreakpad-core.so 的 0x28468、0x284c4、0x28504、0x28530分别对应哪个函数符号

```
libbreakpad-core.so + 0x28468
libbreakpad-core.so + 0x284c4
libbreakpad-core.so + 0x28504
libbreakpad-core.so + 0x28530
```

```
feifeideMacBook-Pro:try feifei$ /Users/feifei/Library/Android/sdk/ndk-bundle/toolchains/aarch64-linux-android-4.9/prebuilt/darwin-x86_64/bin/aarch64-linux-android-addr2line -f -C -e libbreakpad-core.so 0x28468 0x284c4 0x28504 0x28530
testCrash1()
??:?
testCoffeCacher()
??:?
call_dangerous_function()
??:?
Java_com_sogou_translate_jni_BreakPadCore_go2crash
??:?
```

可以得出实际的方法调用栈为:

```
testCrash1()
testCoffeCacher()
call_dangerous_function()
Java_com_sogou_translate_jni_BreakPadCore_go2crash
```

arm-linux-androideabi-addr2line  使用方法介绍:
```
arm-linux-androideabi-addr2line -C -f -e ${SOPATH} ${Address}
-C -f  			//打印错误行数所在的函数名称
  -e    	   		//打印错误地址的对应路径及行数
  ${SOPATH}  		//so库路径 
  ${Address}		//需要转换的堆栈错误信息地址，可以添加多个，但是中间要用空格隔开
```

