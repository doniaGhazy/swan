#!/usr/bin/env bash

source ../../tester.bash
TAINT_SPEC=DEFAULT
# Use VTA because UCG fails (TODO)
DRIVER_OPTIONS="--cg-algo vtafp"
test_directory
