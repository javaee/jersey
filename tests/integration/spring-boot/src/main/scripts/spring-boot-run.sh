#!/bin/bash

start() {
    java -jar "$1" > /dev/null 2>&1
    PID=$!
    echo $PID > spring-boot.pid
}

stop() {
    kill -9 `cat spring-boot.pid` 
    rm spring-boot.pid
}

case "$1" in
    start)
        start "$2"
        ;;
    stop)
        stop
        ;;
esac
