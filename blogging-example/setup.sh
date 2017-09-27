#!/bin/bash
set -ex

# Set up mysql container
export FN_REGISTRY=blogdemo

export MYSQL_USER=bloguser

TEST_SQL_SETUP_DIR=$(pwd)/src/test/resources

# Stand up MySQL in a container of its own
docker rm -f mysql-blogexample || true

echo Starting mysql

MYSQL_CONTAINER_ID=$(
    docker run -d --name mysql-blogexample -v $TEST_SQL_SETUP_DIR:/docker-entrypoint-initdb.d:ro \
                -e MYSQL_PASSWORD=SgRoV3s \
                -e MYSQL_USER=$MYSQL_USER \
                -e MYSQL_DATABASE=POSTS \
                -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
                mysql
    )

export MYSQL_HOST=$(
   docker inspect \
          --type container \
          -f '{{range .NetworkSettings.Networks}}{{ .IPAddress }}{{end}}' \
          $MYSQL_CONTAINER_ID
   )

fn apps d blogexample || true


fn deploy --app blogexample --local


fn apps config set blogexample "DB_URL" "jdbc:mysql://${MYSQL_HOST}/POSTS"
fn apps config set blogexample "DB_PASSWORD" "SgRoV3s"
fn apps config set blogexample "DB_USER" ${MYSQL_USER}
fn apps config set blogexample "DB_DRIVER" "com.mysql.cj.jdbc.Driver"

fn routes create blogexample /route/html
fn routes create blogexample /route/add

fn apps inspect blogexample
fn routes l blogexample
