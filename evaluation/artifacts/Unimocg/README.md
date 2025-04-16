# Unimocg: Modular Call-Graph Algorithms for Consistent Handling of Language Features - Companion Artifact

This is the artifact of the paper:
_Unimocg: Modular Call-Graph Algorithms for Consistent Handling of Language Features_ published at ISSTA 2024.
Our paper proposes a modular architecture for call-graph construction that decouples the computation of type information from resolving calls.

The evaluation of our paper shows how Unimocg enables a framework of call-graph algorithms with different precision, soundness, and scalability trade-offs from reusable modules.
The ten algorithms Unimocg currently supports show consistent soundness without sacrificing precision or performance.
The evaluation also shows how an immutability analysis is improved by using Unimocg.
This artifact contains everything required to reproduce the results of the evaluation.

## Getting started
For a first run, make sure Docker is installed and the current user is added to the docker group (`sudo usermod -aG docker $USER`).   

### Creating the container
`sudo ./createContainer.sh`

### Starting the Container
`sudo docker run -it unimocgimage`

### Starting the Evaluation

#### Generating the soundness fingerprints for OPAL's RTA (cf. Table 2):   
Generate the soundness fingerprints with the following commands (Caution: may take over 30 minutes depending on computational resources):    
`cd /JCG/JCG`   
`/runner/createFingerprint.sh OPAL RTA`    
Then aggregate them with following command, which prints the information as given in Table 2:    
`python3 /runner/aggregate_fingerprints.py /evaluation/fingerprints/OPAL-RTA.profile` 

#### Running the analyses for evaluating the precision and performance of OPAL's RTA (cf. Table 3):   
Run OPAL's RTA analysis once on the jasml project:    
`cd /JCG/JCG`    
`/runner/runJCG.sh jasml OPAL RTA 1`  
The following commands collect the results and prints them:    
`cd /evaluation/results`   
`python3 /runner/aggregate_precision_runtime.py`   

#### Starting the immutability analyses like it (cf. Table 4):    
Run OPAL's immutability analysis on the JDK:    
`/runner/opalImmutability.sh`    
The following commands collect the results and prints them:  
`python3 /runner/aggregate_opal_immutability.py`

## Detailed Description
For the complete evaluation 400 GB of RAM are required and runtime may, depending on computational resources, exceed 5 days.

### Prerequisites
- Docker must be installed
- At least 400 GB RAM
- Create the docker group:   
  `sudo groupadd docker`
- Add the current user to the docker group:  
  `sudo usermod -aG docker $USER`

### Creating the container
`sudo ./createContainer.sh`    
This command creates the docker container, downloading all necessary dependencies in the process.

### Starting the Container
`sudo docker run -it unimocgimage` 
This command starts the docker container and opens a terminal in which all commands listed below can be executed.

### Starting the Evaluation

#### Generating the fingerprints (cf. Table 1/2):              
`/runner/createFingerprintsAll.sh`  
The result of running this command are files `/evaluation/fingerprints/<Tool>-<Algorithm>.profile` which indicate for each test case whether it has been passed soundly (S), failed because of unsoundness (U) or an error (E), was sound but imprecise (I), or did not complete within the set timeout (T).

The test cases match to the categories as follows (automatic aggregration see next step):
| Category | Testcases | 
|----------|-----------|
| Non-virtual Calls | NVC |
| Virtual Calls | VC |
| Types | TC |
| Static Initializer | SI |
| Java 8 Interfaces | J8DIM , J8SIM |
| Unsafe | Unsafe |
| Class.forName | CFNE |
| Signature Polymorphic Methods | SPM |
| Java 9+ | Java 9 Modules, J10SIM |
| Non-Java | NJ |
| MethodHandle | TMR |
| Invokedynamics | Lambda , MR |
| Reflection | TR, LRR, CSR |
| JVM Calls | JVMC |
| Serialization | Ser, ExtSer, SerLam |
| Library Analysis | LIB |
| Class Loading | CL |
| DynamicProxy | DP |

#### Aggregate the fingerprint results over all adapters and algorithms:    
`python3 /runner/aggregate_fingerprints.py /evaluation/fingerprints/*.profile`  
This command aggregates the files from the previous step and prints the number of soundly passed test cases for each category of the table, the number of test cases per category, the overall sums of test cases and soundly passed test cases and the percentage of soundly passed test cases.

#### Run the anaylses for evaluating the precision and performance of different call-graph algorithms (cf. Table 3):   
`cd /JCG/JCG`  
`/runner/runJCGAll.sh`    
This command runs all algorithms from all frameworks three times on the projects jasml, javacc, jext, proguard, and sablecc.
It produces two output files in the `/evaluation/results/xcorpus/<run-number>/<Project>/<Tool>/<Algorithm>` directories: `cg.json` and `timings.txt`.
`cg.json` contains the respective call graph, `timings.txt` the runtime for the call-graph analysis.

#### Aggregate the figures:    
`cd /evaluation/results`     
`python3 /runner/aggregate_precision_runtime.py`    
This command aggregates the files from the previous step, computes the median runtime, prints the results and stores them to `/evaluation/results/xcorpus_results.tsv`.

#### Starting the immutability analyses (cf. Table 4):        
`/runner/opalImmutability.sh`  
This command runs OPAL's immutability analysis on the Open JDK 1.8.0_342-b07 using an ad-hoc CHA as well as OPAL's immutability analysis with CHA, RTA, and XTA call graphs.
The result is written to files `/evaluation/results/immutability/<Algorithm>`  
Each file lists the field immutability results per field and a summary at the end that enumerates the number of all mutable, non-transtively, dependently, and transitively immutable fields.     

#### Starting the overview script of the immutability analyses:    
`python3 /runner/aggregate_opal_immutability.py`    
This command aggregates the files from the previous step and prints a summary (Table 4).

### Results
The directory `summaries` of the artifact contains the following summaries of the results that are created from the aggregate scripts:  
* Table 1 & 2 of the paper are taken from `summary_results_fingerprint.txt`. The file contains the output of `aggregate_fingerprints.py`.
* Table 3 is the paper are taken from `xcorpus_results.tsv`
* Table 4 of the paper is taken from `immutability_results.txt`. The file contains the results output of `aggregate_opal_immutability.py`.

The directory `evaluation/fingerprints` contains the raw fingerprint result files for Table 1 and Table 2.

The directories `evaluation/results/xcorpus/<run-number>/<Project>/<Tool>/<Algorithm>` contain the timing files for Table 3. The call graph files were omitted because of their size but can be generated as described above.

The directories `evaluation/results/immutability/<Algorithm>` contain the raw result files of the immutability analysis from Section 4.4 grouped by call-graph algorithm used.

## Reusing the artifact

The commands above compute call graphs, their sizes, and soundness fingerprints for the static analysis frameworks and algorithms considered in the paper.
The JCG tool—which we used to produce these results and which is part of the Docker container—supports further call-graph algorithms in these frameworks and another framework, Doop.
One can thus use this artifact to compute similar results for these further algorithms and framework by adapting the respective scripts used in the commands above.
The included JCG can also be used to provide further data and comparisons of computed call graphs, using various classes in its `Evaluation` subproject.

JCG is extensible with adapters for further static analysis frameworks, such adapters need to implement the `JCGTestAdapter` interface and be made known to JCG by adding them to the `ALL_ADAPTERS` list in the `ConfigParser` object.
Further test cases for soundness fingerprints can be added to JCG by adding them to Markdown files in `jcg_testcases/src/main/resources` and then executing JCG's `TestCaseExtractor`.
