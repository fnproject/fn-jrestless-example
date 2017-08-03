#!/bin/bash
fn routes config set $1 $2 "DB_URL" "jdbc:mysql://172.17.0.1/POSTS"
fn routes config set $1 $2 "DB_PASSWORD" "SgRoV3s"
fn routes config set $1 $2 "DB_USER" "root"
fn routes config set $1 $2 "DB_DRIVER" "com.mysql.cj.jdbc.Driver"
