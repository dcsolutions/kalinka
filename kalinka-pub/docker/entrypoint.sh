#!/bin/bash
set -e
set -x

cmd=(java)

cmd=("${cmd[@]}" -jar kalinka-pub.jar)

exec "${cmd[@]}"


