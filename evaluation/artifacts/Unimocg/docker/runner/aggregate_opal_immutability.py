import os
from os import listdir
from os import walk
import sys
import re
import statistics
def f(path):
    for algorithm in os.scandir(path):
        for result in os.scandir(algorithm):
            File = open(result.path, 'r')
            Line = File.readline()
            i = 0
            while Line:
                Line = File.readline()
                if "Mutable Fields: " in Line:
                    print("----------------------------------------------------------------")
                    print("algorithm: " + algorithm.name)
                    print("")
                    while i < 6:
                        print(Line)
                        Line = File.readline()
                        i = i + 1
                        if i >= 4 :
                            break
                    print("----------------------------------------------------------------")
f("/evaluation/results/immutability")
