#!/bin/bash
set -e
set -x

cmd=(java)

cmd=("${cmd[@]}" -jar kalinka-sub.jar)

exec "${cmd[@]}"


