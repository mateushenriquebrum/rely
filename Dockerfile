FROM ubuntu:latest
LABEL authors="mateus"

ENTRYPOINT ["top", "-b"]