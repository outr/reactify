#!/usr/bin/env bash

set -e

sbt +clean
sbt test
sbt +publishSigned
sbt sonatypeBundleRelease