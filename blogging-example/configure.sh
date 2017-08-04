#!/bin/bash
: ${MYSQL_HOST:=172.17.0.1}
: ${MYSQL_USER:=root}
fn routes config set $1 $2 "DB_URL" "jdbc:mysql://${MYSQL_HOST}/POSTS"
fn routes config set $1 $2 "DB_PASSWORD" "SgRoV3s"
fn routes config set $1 $2 "DB_USER" ${MYSQL_USER}
fn routes config set $1 $2 "DB_DRIVER" "com.mysql.cj.jdbc.Driver"


