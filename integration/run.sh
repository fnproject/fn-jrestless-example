#!/usr/bin/env bash

. "$(dirname "$0")"/lib.sh

set -ex

SUFFIX=$(git rev-parse HEAD)



# Stand up the functions runner
docker pull fnproject/functions
FUNCTIONS_CONTAINER_ID=$(docker run -d -p 8080 --name functions-$SUFFIX -v /var/run/docker.sock:/var/run/docker.sock fnproject/functions)
defer docker rm -f $FUNCTIONS_CONTAINER_ID


FUNCTIONS_HOST=$(
   docker inspect \
          --type container \
          -f '{{range index .NetworkSettings.Ports "8080/tcp"}}{{.HostIp}}{{end}}' \
          $FUNCTIONS_CONTAINER_ID
   )

FUNCTIONS_PORT=$(
   docker inspect \
          --type container \
          -f '{{range index .NetworkSettings.Ports "8080/tcp"}}{{.HostPort}}{{end}}' \
          $FUNCTIONS_CONTAINER_ID
   )

export API_URL="http://$FUNCTIONS_HOST:$FUNCTIONS_PORT"

echo "Using API_URL: $API_URL"

export no_proxy=$no_proxy,$FUNCTIONS_HOST
export NO_PROXY=$no_proxy


wait_for_http "$API_URL"

# Set locations of test resources
export TEST_DIR="$(realpath "$(dirname "$0")"/../blogging-example )"
TEST_SQL_SETUP_DIR="$TEST_DIR/src/test/resources"

echo "test scripts live in $TEST_SQL_SETUP_DIR"



export MYSQL_USER=bloguser

# Stand up MySQL in a container of its own
MYSQL_CONTAINER_ID=$(
    docker run -d --name mysql-$SUFFIX -v $TEST_SQL_SETUP_DIR:/docker-entrypoint-initdb.d:ro \
                -e MYSQL_PASSWORD=SgRoV3s \
                -e MYSQL_USER=$MYSQL_USER \
                -e MYSQL_DATABASE=POSTS \
                -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
                mysql
    )
defer docker rm -f $MYSQL_CONTAINER_ID

export MYSQL_HOST=$(
   docker inspect \
          --type container \
          -f '{{range .NetworkSettings.Networks}}{{ .IPAddress }}{{end}}' \
          $MYSQL_CONTAINER_ID
   )
defer docker logs $MYSQL_CONTAINER_ID

wait_for_docker_log $MYSQL_CONTAINER_ID "running /docker-entrypoint-initdb.d/dbZZZsetup-over.sql"

echo "Mysql listening on docker network at address $MYSQL_HOST"

# Run the main integration test

"$(dirname "$0")"/smoke-test.sh

docker ps -a | head


set +x
echo Success!
