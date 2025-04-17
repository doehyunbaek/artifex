FROM ubuntu:20.04
ENV USER_NAME unimocg-evaluation
ENV USER_DIR /home/${USER_NAME}
##
ENTRYPOINT [ "/bin/bash" ]
##
# INSTALL PRELIMINARY TOOLS
RUN \
apt-get update && \
apt-get -y install apt-transport-https ca-certificates software-properties-common && \
apt-get update && \
apt-get -y install apt-utils && \
apt-get update && \
apt-get -y install git \
curl \
less \
nano \
vim \
zip \
zlib1g-dev \
wget \
python3.5 \
unzip \
aptitude \
automake \
autoconf \
bison \
build-essential \
clang \
doxygen \
flex \
graphviz \
libffi-dev \
        libncurses5-dev \
        libopenmpi-dev \
        libsqlite3-dev \
        libtool \
        lsb-release \
        mcpp \
        nano \
        openmpi-bin \
        python3-matplotlib \
        python3-numpy \
        python3-pandas \
        zip \
        zlib1g-dev
##
# Install OpenJDK 8 version 342
RUN \ 
cd / && \
mkdir /JDK && \
cd JDK && \
apt-get -y autoremove openjdk* && \ 
apt-get -y purge openjdk* && \
wget https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u342-b07/OpenJDK8U-jdk_x64_linux_hotspot_8u342b07.tar.gz && \
tar xvf /JDK/OpenJDK8U-jdk_x64_linux_hotspot_8u342b07.tar.gz -C /opt && \
cd / && \
update-alternatives --install /usr/bin/java java /opt/jdk8u342-b07/bin/java 1000 && \
update-alternatives --install /usr/bin/javac javac /opt/jdk8u342-b07/bin/javac 1000 && \
update-alternatives --install /usr/bin/jar jar /opt/jdk8u342-b07/bin/jar 1000        
#
#
RUN \
cd / && \
mkdir evaluation
COPY evaluation /evaluation
## Copy Runner
RUN mkdir runner
COPY runner /runner
COPY corpora /corpora
RUN cd /corpora/files && \
git clone https://bitbucket.org/jensdietrich/xcorpus.git
RUN for d in /corpora/files/xcorpus/data/qualitas_corpus_20130901/* ; do unzip $d/project/bin.zip -d $d/project/bin; done
##
## INSTALL SBT
COPY container-files /
##
RUN \
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add && \
apt-get update && \
apt-get install sbt
#
RUN \
cd / && \
mkdir /evaluation/results && \
mkdir /evaluation/results/immutability && \
mkdir /evaluation/results/immutability/AdHocCHA && \
mkdir /evaluation/results/immutability/CHA && \
mkdir /evaluation/results/immutability/RTA && \
mkdir /evaluation/results/immutability/XTA && \
mkdir /evaluation/testcases && \
mkdir /evaluation/fingerprints && \
mkdir /evaluation/results/xcorpus
##
## INSTALL OPAL
ARG OPAL_BRANCH="develop"
ARG OPAL_COMMIT_ID="95eafb2fc2f0ae72895d9dd75e26a514663595e3"
##
RUN cd / && \
git clone https://github.com/stg-tud/opal.git && \
cd opal && \
git checkout ${OPAL_BRANCH} && \
git reset --hard ${OPAL_COMMIT_ID} && \
sbt publishLocal
#
## INSTALL JCG 
ARG JCG_BRANCH="master"
ARG JCG_COMMIT_ID="325b1b475b292858ade9f3e0d68fb940c5a47e6d"
RUN cd / && \ 
mkdir JCG && \
cd /JCG && \
git clone https://github.com/opalj/JCG.git && \
cd /JCG/JCG && \
git checkout ${JCG_BRANCH} && \
git reset --hard ${JCG_COMMIT_ID} && \
sbt compile && \
sbt "project jcg_testcases ; runMain TestCaseExtractor --rsrcDir jcg_testcases/src/main/resources"
RUN cp -r /JCG/JCG/testcaseJars/* /evaluation/testcases
RUN cp -r /JCG/JCG/infrastructure_incompatible_testcases/compiled_jars_and_configs/* /evaluation/testcases
COPY /jre.conf /JCG/JCG/jre.conf
##
### preparing immutability analysis
COPY immutability/Immutability_Adhoc.scala /opal/DEVELOPING_OPAL/tools/src/main/scala/org/opalj/support/info/Immutability_Adhoc.scala
COPY immutability/AdHocCHAFieldImmutabilityAnalysis.scala /opal/OPAL/tac/src/main/scala/org/opalj/tac/fpcf/analyses/AdHocCHAFieldImmutabilityAnalysis.scala
### 
##
###
RUN chmod +x /runner/createFingerprint.sh
RUN chmod +x /runner/createFingerprintsAll.sh
RUN chmod +x /runner/*.sh
RUN chmod +x /runner/runJCG.sh
RUN chmod +x /runner/runJCGAll.sh
RUN chmod +x /runner/opalImmutability.sh
RUN chmod +x /evaluation/opal/scripts/OPALImmutability.sh
##
