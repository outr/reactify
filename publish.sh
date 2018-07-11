#!/usr/bin/env bash

sbt +clean +compile
sbt +test
sbt +reactifyJVM/publishSigned +reactifyJS/publishSigned ++2.11.12 reactifyNative/publishSigned sonatypeRelease