#!/bin/bash
for immutability in oldImmutability newImmutability
do
  for algorithm in AdHocCHA CHA RTA XTA
  do
    rm -rf /evaluation/results/$immutability/$algorithm/*
  done
done
