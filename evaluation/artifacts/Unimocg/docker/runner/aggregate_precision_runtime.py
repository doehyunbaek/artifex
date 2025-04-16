import os
import csv
import statistics
import subprocess
import re
result = {}
def f(path):
    for number in os.scandir(path):
        if number.is_dir():
            for library in os.scandir(number.path):
                if library.is_dir():
                    for framework in os.scandir(library.path):
                        if framework.is_dir():
                            for algorithm in os.scandir(framework.path):
                                if algorithm.is_dir():
                                    for entry in os.scandir(algorithm.path):
                                        try:
                                            path = algorithm.path+"/timings.txt"
                                            time = float(open(path, "r").read().split(" ")[0])
                                        except:
                                            time=0
                                        if not framework.name in result:
                                            result[framework.name] = {}
                                        if not library.name in result[framework.name]:
                                            result[framework.name][library.name] = {}
                                        if not algorithm.name in result[framework.name][library.name]:
                                            result[framework.name][library.name][algorithm.name] = {}
                                        if time > 0:
                                            result[framework.name][library.name][algorithm.name][number.name] = {}
                                            result[framework.name][library.name][algorithm.name][number.name]["time"] = time
                                        rm = "error"
                                        if str(number.name)==str(1):
                                            p = algorithm.path + '/cg.json"'
                                            command = 'cd /JCG/JCG; timeout --foreground 90m sbt -java-home /opt/jdk8u342-b07/jre -J-Xmx400G "; project jcg_evaluation; runMain CallGraphSize ' + algorithm.path+'/cg.json"'
                                            output = subprocess.getoutput(command)
                                            pattern = r"- ([0-9]+) reachable methods -"
                                            reachable_methods = re.findall(pattern, str(output))
                                            result[framework.name][library.name][algorithm.name][number.name]["rm"]=reachable_methods[0]

f("/evaluation/results/xcorpus")
with open('xcorpus_results.tsv', 'wt') as out_file:
    tsv_writer = csv.writer(out_file, delimiter='\t')
    tsv_writer.writerow(['framework', 'library', 'algorithm', 'rm', 'first', 'second', 'third', 'time median'])

    for fw in result.keys():
        for lib in result[fw].keys():
            for algo in result[fw][lib].keys():
                times = list(map(lambda x: x["time"], result[fw][lib][algo].values()))
                if len(times)>0:
                    time_median = statistics.median(times)

                else:
                    times = ['error', 'error', 'error']
                    time_median = "error"
                try:
                    rm_output = result[fw][lib][algo]['1']["rm"]
                except:
                    rm_output= "error"
                tsv_writer.writerow([fw,lib,algo,rm_output, *times, time_median])

print("result: ")
print(result)
