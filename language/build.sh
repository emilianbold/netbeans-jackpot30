#!/bin/bash
(cd ide; ant "$@" clean && ant "$@" nbms && ant "$@" test) || exit 1

