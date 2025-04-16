#!/bin/bash

for project in jasml javacc jext proguard sablecc
do
	# OPAL
	/runner/runJCG.sh $project OPAL CHA 3
	/runner/runJCG.sh $project OPAL RTA 3
	/runner/runJCG.sh $project OPAL 0-CFA 3

	# Soot
	/runner/runJCG.sh $project Soot CHA 3
	/runner/runJCG.sh $project Soot RTA 3
	/runner/runJCG.sh $project Soot SPARK 3

	# WALA
	/runner/runJCG.sh $project WALA CHA 3
	/runner/runJCG.sh $project WALA RTA 3
	/runner/runJCG.sh $project WALA 0-CFA 3
done

exit
