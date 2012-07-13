#!/bin/bash
cp -r ../duplicates/server/indexer/build/cluster/* build/indexing-backend/indexer/indexer
cp -r ../duplicates/ide/build/cluster/* build/indexing-backend/indexer/indexer
cp -r ../remoting/ide/build/cluster/* build/indexing-backend/indexer/indexer
cp -r ../language/ide/build/cluster/* build/indexing-backend/indexer/indexer
cp -r ../duplicates/server/web/duplicates.web.api/dist/*.jar build/indexing-backend/web/lib

(cd build; zip -r indexing-backend-feature-packed.zip indexing-backend) || exit 1
(cd build; zip -r indexing-backend-feature-packed-shortened.zip `find indexing-backend -type f | grep -v indexing-backend/indexer/enterprise/ | grep -v indexing-backend/indexer/apisupport/  | grep -v indexing-backend/indexer/cnd/   | grep -v indexing-backend/indexer/dlight/   | grep -v indexing-backend/indexer/harness/   | grep -v indexing-backend/indexer/ide/   | grep -v indexing-backend/indexer/java   | grep -v indexing-backend/indexer/nb/   | grep -v indexing-backend/indexer/platform/   | grep -v indexing-backend/indexer/profiler/   | grep -v indexing-backend/indexer/websvccommon/`) || exit 1
