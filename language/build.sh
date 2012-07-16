#!/bin/bash
(cd ide; ant "$@" clean && ant "$@" nbms && ant "$@" test) || exit 1
(cd server/web/language.web.api; ant "$@" clean && ant "$@") || exit 1

