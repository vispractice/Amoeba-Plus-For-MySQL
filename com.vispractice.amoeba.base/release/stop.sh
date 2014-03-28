#!/bin/sh

if [ ! -f cc.pid ];
  then
    echo 'not found pid file.';
    exit 1;
fi

pid=`cat cc.pid`

if (kill -0 $pid 2>/dev/null)
  then
    echo -n "Shutting down #$pid "
    kill $pid 1>/dev/null
else
    echo "process #$pid is not running!"
    rm -rf cc.pid
    exit 0
fi

process="$pid"

while test ! -z $process ;do
  sleep 1
  process=`ps -ef | grep -v grep | grep -v "stop.sh" | grep -w $pid | sed -n  '1P' | awk '{print $2}'`
  echo -n '.'
done

rm -rf cc.pid

echo '[ok]'
