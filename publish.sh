#!/usr/bin/env bash

set -e

sbt +clean
sbt test
sbt +reactifyJVM/publishSigned +reactifyJS/publishSigned +reactifyNative/publishSigned sonatypeRelease