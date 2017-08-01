#!/bin/bash -ex

TEST_SQL_SETUP_DIR=$(pwd)/src/test/resources

# Stand up MySQL in a container of its own
docker rm -f mysql-jaxrs || true

echo "Starting MySql"

MYSQL_CONTAINER_ID=$(
    docker run -d --name mysql-jaxrs \
                -v $TEST_SQL_SETUP_DIR:/docker-entrypoint-initdb.d:ro \
                -e MYSQL_PASSWORD=SgRoV3s \
                -e MYSQL_USER=jaxrs \
                -e MYSQL_DATABASE=POSTS \
                -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
                -p 3306:3306 \
                mysql
    )
