#!/usr/bin/env bash

ENV="prod"
cd `dirname $0`
APPLICATION="my-crawler"
SERVER_PORT=9206
LOGS_DIR="/tmp/logs/${APPLICATION}"
mkdir -p ${LOGS_DIR}
HOST_NAME=`hostname`
SPRING_PROFILE=" -Dspring.profiles.active=${ENV} "
JAVA_MEM_OPTS=" -server -Xms2000m -Xmx2000m -Xmn500m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:+ParallelRefProcEnabled -XX:-UseBiasedLocking "
JAVA_GC_LOG_OPTS=" -XX:+PrintGCDetails -Xloggc:$LOGS_DIR/gc_log.`date +%m%d%H%M` -XX:+PrintGCDateStamps "
JAVA_ERR_DUMP_OPTS=" -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$LOGS_DIR/oom_err_heap_dump.`date +%m%d%H%M` "

JARFile="${APPLICATION}.jar"
PIDFile="${APPLICATION}.pid"
STDOUT_FILE="${LOGS_DIR}/stdout.log"
function check_if_pid_file_exists {
    if [ ! -f ${PIDFile} ]
    then
        echo "PID file not found: $PIDFile"
        exit 1
    fi
}
function check_if_process_is_running {
    local pid=$1
    if ps -p ${pid} > /dev/null
    then
        return 0
    else
        return 1
    fi
}
function print_process {
    echo $(<"${PIDFile}")
}
case "$1" in
    status)
        if [ ! -f ${PIDFile} ]
        then
            echo "PID file not found: $PIDFile"
            exit 0
        fi
        if check_if_process_is_running $(print_process)
        then
            echo $(print_process)" is running"
        else
            echo "Process not running: $(print_process)"
        fi
        ;;
    stop)
        check_if_pid_file_exists
        pid=$(print_process)
        if ! check_if_process_is_running $pid
        then
            echo "Process $pid already stopped"
            exit 0
        fi
        kill -TERM $pid
        printf "Waiting for process to stop"
        NOT_KILLED=1
        for i in {1..20}; do
            if check_if_process_is_running $pid
            then
                printf ". "
                sleep 1
            else
                echo "killed"
                NOT_KILLED=0
                break
            fi
        done
        echo
        if [ ${NOT_KILLED} = 1 ]
        then
            echo "Cannot kill process $(print_process)"
            exit 1
        fi
        echo "Process stopped"
        ;;
    start)
        if [ -f ${PIDFile} ] && check_if_process_is_running $(print_process)
        then
            echo "Process $(print_process) already running"
            exit 1
        fi
        nohup java ${SPRING_PROFILE} ${JAVA_MEM_OPTS} ${JAVA_GC_LOG_OPTS} ${JAVA_ERR_DUMP_OPTS}  -jar ${JARFile} > $STDOUT_FILE 2>&1 &
        printf "Wait process to start"
        for i in {1..60}; do
            if [ ! -f ${PIDFile} ] 
            then
                printf ". "
                sleep 1
            else
                if check_if_process_is_running $(print_process)
                then
                    echo "\nProcess started\n"
                    break
                else
                    printf ". "
                    sleep 1
                fi
            fi
        done
        ;;
    restart)
        $0 stop
        if [ $? = 1 ]
        then
            exit 1
        fi
        $0 start
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
esac
exit 0
