#!/bin/bash

export NACOS_ENDPOINT=
export NACOS_NAMESPACE=

export NACOS_DATAID=ollama-agent
export METER_DATAID=prometheus
export METER_GROUP=
export REDIS_DATAID=redis
export REDIS_GROUP=

export SLS_ENDPOINT=cn-beijing-intranet.log.aliyuncs.com
export SLS_PROJECT=
export SLS_LOGSTORE=
export SLS_TOPIC=ollama-agent-dev

export CPB_GROUP=DEFAULT_GROUP
export CPB_DATAID=sls-access.conf

export JVM_MEM=1024M
export JVM_DIRECT_MEM=128M
export JVM_PID_FILE=ollama-agent.pid
export JVM_BOOT_JAR=ollama-agent-1.0-SNAPSHOT.jar
export JVM_SPRING_PROFILE=dev