#!/bin/sh
docker image rm -f unimocgimage
docker build --no-cache -f docker/Dockerfile docker -t unimocgimage
