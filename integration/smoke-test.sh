#!/bin/bash

set -ex

# Called by the integration framework.

# NO_PROXY, API_URL and MYSQL_HOST and TEST_DIR should be configured here

cd "$TEST_DIR"

fn apps create myapp
fn routes create myapp /route/blogs
fn routes create myapp /route/html
fn routes create myapp /route/add

# Configure our routes

./configure.sh myapp /route/blogs
./configure.sh myapp /route/html
./configure.sh myapp /route/add

fn build


curl -v -H 'Accept: application/json' -X GET "$API_URL/r/myapp/route/blogs"