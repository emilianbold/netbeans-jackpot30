#!/bin/bash
ant "$@" clean && ant "$@" build && ant "$@" test && (cd compiler; ant "$@" create-standalone-compiler && build/test/scripted/run )  && (cd tool; ant "$@" create-standalone-tool && build/test/scripted/run ) || exit 1
MAVEN_REPO=`pwd`/build/.m2
mkdir -p "$MAVEN_REPO"
mvn $MAVEN_EXTRA_ARGS install:install-file -Dfile=tool/build/jackpot/jackpot.jar -DgroupId=org.netbeans.modules.jackpot30 -DartifactId=tool -Dversion=7.2-SNAPSHOT -Dpackaging=jar -DgeneratePom=true "-DlocalRepositoryPath=$MAVEN_REPO"
(cd maven; mvn $MAVEN_EXTRA_ARGS -Dmaven.executable=`which mvn` -DaltDeploymentRepository=temp::default::file://"$MAVEN_REPO" deploy)
(cd build; zip -r .m2.zip .m2)
