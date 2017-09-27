#!/bin/bash
: ${MYSQL_HOST:=172.17.0.1}
: ${MYSQL_USER:=root}
fn apps config set $1 "DB_URL" "jdbc:mysql://${MYSQL_HOST}/POSTS"
fn apps config set $1 "DB_PASSWORD" "SgRoV3s"
fn apps config set $1 "DB_USER" ${MYSQL_USER}
fn apps config set $1 "DB_DRIVER" "com.mysql.cj.jdbc.Driver"


