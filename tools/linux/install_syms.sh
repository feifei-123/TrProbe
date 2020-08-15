#! /bin/bash

if [ $# -lt 1 ]; then
  echo "Usage: $0 /path/of/so_file [/path/of/symbol/]"
  exit 0
fi

so_path=$1
symbol_path="symbol"
if [ $# -eq 2 ]; then
  symbol_path=$2
fi

echo "start install Symbol:$so_path, symbol:$symbol_path"
so_name=$(basename $1)

#0
if [ ! -d $symbol_path ];then
  echo "create dir: ${symbol_path}"
  mkdir ${symbol_path}
fi

#1
sub_dir=${symbol_path}/${so_name}
if [ ! -d $sub_dir ];then
  echo "create dir: ${sub_dir}"
  mkdir ${sub_dir}
fi

#2
echo "dump_syms $so_path > ${so_name}.sym"
./dump_syms $so_path > ${so_name}.sym

#3
uid=`head -n 1 ${so_name}.sym | awk '{print $4}'`
echo $uid
sub_dir=${sub_dir}/${uid}
if [ ! -d $sub_dir ];then
  echo "create dir: ${sub_dir}"
  mkdir ${sub_dir}
fi

#4
mv ${so_name}.sym ${symbol_path}/${so_name}/${uid}
echo "symbol installed!"
