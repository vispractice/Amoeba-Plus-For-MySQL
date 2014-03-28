#!/bin/sh

sh stop.sh
if [ $? != 0 ];
  then
    exit $?;
fi
sh startup.sh
