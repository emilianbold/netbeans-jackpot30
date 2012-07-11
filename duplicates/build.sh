#!/bin/bash
(cd ide; ant "$@" clean && ant "$@" nbms && ant "$@" test) || exit 1
(cd server/indexer; ant "$@" clean && ant "$@" build && cp -r build/cluster build/indexer; cp -r ../../ide/build/cluster/* build/indexer/; cp -r ../../../remoting/ide/build/cluster/* build/indexer/; cd build; zip -r ../../../../remoting/build/duplicates-indexer.zip indexer/) || exit 1
(cd server/web/duplicates.web.api; ant "$@" clean && ant "$@") || exit 1

