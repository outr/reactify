#!/usr/bin/env bash

sbt clean +reactifyJVM/publishSigned +reactifyJS/publishSigned ++2.11.12 reactifyNative/publishSigned sonatypeRelease