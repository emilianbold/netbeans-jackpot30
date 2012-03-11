#!/bin/bash
(cd common; ant "$@" clean && ant "$@" nbms) || exit 1
(cd ide; ant "$@" clean && ant "$@" nbms) || exit 1
rm -rf build
mkdir -p build/indexing-backend
(cd server/indexer; ant "$@" clean && ant "$@" build-zip && unzip -d ../../build/indexing-backend dist/indexer.zip) || exit 1
mkdir -p build/indexing-backend/web
(cd server/web/web.main; ant clean && ant jar && cp -r dist/* ../../../build/indexing-backend/web) || exit 1

cp server/scripts/* build/indexing-backend

chmod u+x build/temp-indexing-backend/index
chmod u+x build/temp-indexing-backend/web

(cd build; zip -r indexing-backend.zip indexing-backend) || exit 1
(cd build; zip -r indexing-backend-shortened.zip `find indexing-backend -type f | grep -v indexing-backend/indexer/enterprise/ | grep -v indexing-backend/indexer/apisupport/  | grep -v indexing-backend/indexer/cnd/   | grep -v indexing-backend/indexer/dlight/   | grep -v indexing-backend/indexer/harness/   | grep -v indexing-backend/indexer/ide/   | grep -v indexing-backend/indexer/java   | grep -v indexing-backend/indexer/nb/   | grep -v indexing-backend/indexer/platform/   | grep -v indexing-backend/indexer/profiler/   | grep -v indexing-backend/indexer/websvccommon/`) || exit 1

(cd server/hudson; mvn -Dmaven.test.skip=true clean package && cp target/*.hpi ../../build) || exit

mkdir -p ide/local/release/index-server
(cd server/web/web.main; cp -r dist/* ../../../ide/local/release/index-server) || exit 1
