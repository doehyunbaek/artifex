#!/bin/bash
ADAPTER=$1
ALGO=$2
INPUT_DIR=/evaluation/testcases
OUTPUT_DIR=/evaluation/fingerprints
sbt -java-home /opt/jdk8u342-b07/jre -J-Xmx400G "; project jcg_evaluation; runMain FingerprintExtractor -i $INPUT_DIR -o $OUTPUT_DIR --adapter $ADAPTER --algorithm-prefix $ALGO"
