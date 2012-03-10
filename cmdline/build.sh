#!/bin/bash
ant "$@" clean && ant "$@" build && ant "$@" test && (cd compiler; ant "$@" create-standalone-compiler && build/test/scripted/run )  && (cd tool; ant "$@" create-standalone-tool && build/test/scripted/run ) || exit 1

