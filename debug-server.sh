#!/bin/sh
mkdir test-server
cd test-server
java -Xmx2G -Xdebug -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Xrunjdwp:transport=dt_socket,address=7724,server=y,suspend=n -jar ../server/build/libs/server.jar --core=../../chunkstories-core/res/
cd ..
