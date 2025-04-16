#!/bin/bash

run(){
	ADAPTER=$1
	ALGO=$2
	INPUT_DIR=/evaluation/testcases
	OUTPUT_DIR=/evaluation/fingerprints
	sbt -java-home /opt/jdk8u342-b07/jre -J-Xmx400G "; project jcg_evaluation; runMain FingerprintExtractor -i $INPUT_DIR -o $OUTPUT_DIR --adapter $ADAPTER --algorithm-prefix $ALGO"
}

cd /JCG/JCG
#OPAL
run OPAL CHA
run OPAL RTA
run OPAL XTA
run OPAL 0-CFA
run OPAL 1-1-CFA
#
#SOOT
run Soot CHA
run Soot RTA
run Soot SPARK
#
#WALA
run Wala CHA
run Wala RTA
run Wala 0-CFA
#
exit
