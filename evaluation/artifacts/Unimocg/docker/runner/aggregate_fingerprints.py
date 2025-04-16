import csv
import sys

def aggregate(filename):
    tsv_file = open(filename)
    read_tsv = csv.reader(tsv_file, delimiter="\t")

    keys = {
        'Non-virtual Calls':{'NVC'},
        'Virtual Calls':{'VC'},
        'Types':{'TC'},
        'Static Initializer':{'SI'},
        'Java 8 Interfaces':{'J8DIM', 'J8SIM'},
        'Unsafe':{'Unsafe'},
        'Class.forName':{'CFNE'},
        'Signature Polymorphic Methods':{'SPM'},
        'Java 9+':{'Java 9 Modules', 'J10SIM'},
        'Non-Java':{'NJ'},
        'MethodHandle':{'TMR'},
        'Invokedynamic':{'Lambda', 'MR'},
        'Reflection':{'TR', 'LRR', 'CSR'},
        'JVM Calls':{'JVMC'},
        'Serialization':{'Ser', 'ExtSer', 'SerLam'},
        'Library Analysis':{'LIB'},
        'Class Loading':{'CL'},
        'DynamicProxy':{'DP'}
    }

    result = {}
    totals = {}
    f = []
    for  row in read_tsv:
        f.append(row)

    for key in keys:
        result[key] = 0
        totals[key] = 0
        for v in keys[key]:
            for row in f:
                if row[0].startswith(v) and (row[0][len(v):] + "0").isdigit():
                    totals[key] += 1
                    if row[1]=='S' or row[1]=='I':
                        result[key] += 1
    print("===================================================================")
    print(filename)
    print("")
    sum = 0
    total = 0
    for key in result:
        sum += result[key]
        total += totals[key]
        print(f"{key}: {result[key]}/{totals[key]}")
    print("")
    print(f"Sum (out of {total}): {sum} ({100*sum/total})")
    print("-------------------------------------------------------------------")

files = sys.argv
del files[0]
for f in files:
  aggregate(f)
