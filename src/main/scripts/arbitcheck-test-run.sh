#!/bin/sh

# project home directory
BASEPATH=`dirname $0`/../../../

# parse arguments
DIR=gentests
while [ $# -gt 0 ]
do
    case "$1" in
    (-cp) USER_CLASSPATH=$2; shift;;
    (*) DIR=$1;;
    esac
    shift
done

if [ "$USER_CLASSPATH" = "" ]; then
  echo "usage: $0 -cp your_class_path [generated_test_dir]"
  exit 1
fi

# find tests
bp=$BASEPATH
pw=`pwd`
cd $DIR
suites=`find . -name *Test\.class`
cd $pw

# exec tests
for suiteclass in $suites; do
  suite=`echo $suiteclass | sed -e "s/^\.\/\(.*\)\.class\$/\\1/" | sed -e "s/\//./g"`
  java -cp $DIR:$USER_CLASSPATH:$bp/dist/arbitcheck.jar:$bp/lib/plume.jar:$bp/lib/junit-4.11.jar org.junit.runner.JUnitCore $suite
done
