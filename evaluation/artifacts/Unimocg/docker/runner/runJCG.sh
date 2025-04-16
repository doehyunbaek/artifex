#!/bin/bash

PROJECT=$1
TOOL=$2
ALGORITHM=$3
RUNS=$4
	
for ((i=1; i<=$RUNS; i++))
do
	INPUT_DIR=/corpora/files/project-specific/xcorpus-performance/$PROJECT
	OUTPUT_DIR=/evaluation/results/xcorpus/$i
	timeout --foreground 90m sbt -java-home /opt/jdk8u342-b07/jre -J-Xmx400G "; project jcg_evaluation; runMain Evaluation --input $INPUT_DIR --output $OUTPUT_DIR --adapter $TOOL --algorithm-prefix $ALGORITHM"
done
