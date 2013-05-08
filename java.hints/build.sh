#!/bin/bash
ant "$@" clean && ant "$@" nbms && ant "$@" test || exit 1

