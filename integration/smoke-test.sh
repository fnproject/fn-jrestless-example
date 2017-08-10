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

fn apps config set myapp "DB_URL" "jdbc:mysql://${MYSQL_HOST}/POSTS"
fn apps config set myapp "DB_PASSWORD" "SgRoV3s"
fn apps config set myapp "DB_USER" ${MYSQL_USER}
fn apps config set myapp "DB_DRIVER" "com.mysql.cj.jdbc.Driver"

fn routes inspect myapp /route/blogs

fn build


curl -v -H 'Accept: application/json' -X GET "$API_URL/r/myapp/route/blogs"

set +x
fn calls list myapp | while read k v
do
  echo "$k $v"
  if [[ "$k" = "ID:" ]]; then id="$v"; fi
  if [[ -z "$k" ]]; then
    echo '[[['
    fn logs get myapp "$id"
    echo ']]]'
    echo
  fi
done



diff -u <(echo -n '[{"date":"Friday","author":"Rae","title":"Testing","body":"Data to retrieve"}]') \
        <(curl -H 'Accept: application/json' -X GET "$API_URL/r/myapp/route/blogs")

diff -u <(echo -n "NewTest added") \
        <(curl -H "Content-Type: application/json" -X POST -d '{"title": "NewTest", "date": "blank", "author": "blank", "body": "blank"}' "$API_URL/r/myapp/route/add")


echo "--->--- SUCCESS! --->---"
