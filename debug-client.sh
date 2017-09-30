#!/bin/sh
java -d64 -Xmx1200M -Xdebug -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Xrunjdwp:transport=dt_socket,address=7724,server=y,suspend=n -jar client/build/libs/chunkstories.jar --dir="."
