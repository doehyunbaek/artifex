#!/bin/bash
RESULTDIR_AdHocCHA=/evaluation/results/immutability/AdHocCHA
RESULTDIR_CHA=/evaluation/results/immutability/CHA
RESULTDIR_RTA=/evaluation/results/immutability/RTA
RESULTDIR_XTA=/evaluation/results/immutability/XTA

LEVEL=1
TIMES=1
THREADS=8

MODE=$1 

cd /opal
git checkout master
git reset --hard 584fe0394ae5b93d3a3d192cb5e24ebb41968417
sbt -Dsbt.rootdir=true "project Tools; runMain org.opalj.support.info.Immutability_Adhoc -JDK -analysis Fields -threads $THREADS -level $LEVEL -resultFolder $RESULTDIR_AdHocCHA -times $TIMES -adHocCHA -analysisName JDKAdHocCHA $MODE"
sbt -Dsbt.rootdir=true "project Tools; runMain org.opalj.support.info.Immutability_Adhoc -JDK -analysis Fields -threads $THREADS -level $LEVEL -callGraph CHA -resultFolder $RESULTDIR_CHA -times $TIMES -analysisName JDKCHA $MODE"
sbt -Dsbt.rootdir=true "project Tools; runMain org.opalj.support.info.Immutability_Adhoc -JDK -analysis Fields -threads $THREADS -level $LEVEL -callGraph RTA -resultFolder $RESULTDIR_RTA -times $TIMES -analysisName JDKRTA $MODE"
sbt -Dsbt.rootdir=true "project Tools; runMain org.opalj.support.info.Immutability_Adhoc -JDK -analysis Fields -threads $THREADS -level $LEVEL -callGraph XTA -resultFolder $RESULTDIR_XTA -times $TIMES -analysisName JDKXTA $MODE"
git checkout develop
git reset --hard 95eafb2fc2f0ae72895d9dd75e26a514663595e3
