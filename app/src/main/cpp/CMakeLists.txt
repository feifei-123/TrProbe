# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.


#建一个静态或者动态库，并提供其关联的源文件路径.
#开发者可以定义多个库，CMake会自动去构建它们。
#Gradle可以自动将它们打包进APK中。
add_library( # Sets the name of the library.
        tr_native_lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s). 多个cpp文件分多行来列出
        native-lib.cpp
        dynamic_register_jni_demo.cpp)



#找到一个系统中预编译的库，并作为一个变量保存起来。由于CMake在搜索路径是会包含系统库，并且CMake 在搜索库路径时 会包含
#系统库，并且Cmake 会去检查它自己之前编译过的库的名字，所以开发者需要保证开发者自行添加的库的名字的唯一性
find_library( # Sets the name of the path variable.
        log-lib #设置路径变量的名称 - 为log起的别名

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log) #指定NDK库的名子，这样CMake就可以找到这个库

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

#指定CMake链接 特定so库 到 目标库中
target_link_libraries( # Specifies the target library.
        tr_native_lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})