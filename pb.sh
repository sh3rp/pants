#!/bin/sh
protoc --java_out=src/main/java src/main/protobuf/PantsProtocol.proto
