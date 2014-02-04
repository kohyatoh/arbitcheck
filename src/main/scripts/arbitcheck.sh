#!/bin/sh

# project home directory
BASEPATH=`dirname $0`/../../../

# parse arguments
while [ $# -gt 0 ]
do
  case "$1" in
  (-cp) USER_CLASSPATH=$2; shift;;
  (*)
    if [ "$PROPERTY" = "" ]; then
      PROPERTY=$1
    else
      OPTS="$OPTS $1"
    fi;;
  esac
  shift
done

if [ "$PROPERTY" = "" -o "$USER_CLASSPATH" = "" ]; then
  echo "usage: $0 -cp your_class_path property"
  exit 1
fi

# run arbitcheck
bp=$BASEPATH
java -cp $USER_CLASSPATH:$bp/dist/arbitcheck.jar:$bp/lib/plume.jar arbitcheck.Main $PROPERTY $USER_CLASSPATH $OPTS
