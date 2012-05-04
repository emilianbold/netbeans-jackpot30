DIR=`dirname $0`
java -Djava.index.useMemCache=false -Xbootclasspath/p:$DIR/web/lib/nb-javac-api.jar:$DIR/web/lib/nb-javac-impl.jar -jar $DIR/web/web.main.jar "$@"
