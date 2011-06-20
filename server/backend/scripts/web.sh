DIR=`dirname $0`
java -Xbootclasspath/p:$DIR/web/lib/javac-api-nb-7.0-b07.jar:$DIR/web/lib/javac-impl-nb-7.0-b07.jar -jar $DIR/web/web.main.jar "$@"
