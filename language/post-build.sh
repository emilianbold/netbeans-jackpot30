#!/bin/bash
(cd server/web/language.web.api; ant "$@" clean && ant "$@") || exit 1
