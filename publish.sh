#!/usr/bin/env bash

set -e

sbt +clean
sbt test
sbt +reactifyJVM/publishSigned +reactifyJS/publishSigned ++2.11.12 reactifyNative/publishSigned sonatypeRelease