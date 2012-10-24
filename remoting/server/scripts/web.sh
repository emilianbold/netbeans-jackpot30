#!/bin/bash
#XXX: unix only
DIR=`dirname $0`
classpath="$DIR/web/web.main.jar"
for jar in $DIR/web/lib/*.jar; do
    classpath="$classpath:$jar"
done
java $JACKPOT_WEB_OPTS -Djava.index.useMemCache=false -Xbootclasspath/p:$DIR/web/lib/nb-javac-api.jar:$DIR/web/lib/nb-javac-impl.jar -classpath "$classpath" web.main.WebMain "$@"
