#!/bin/sh

#corbusier setting
#export EXPERIMENT_HOME=/lfs1/work/stampede/futuregrid/montage
#export PEGASUS_INSTALL=/lfs1/software/install/pegasus/pegasus-4.0.0cvs
#export PEGASUS_CONDOR_POOL=/lfs1/software/install/pegasus/pegasus-4.0.0cvs

#obelix settings
#export PEGASUS_INSTALL=/data/scratch/vahi/software/install/pegasus/pegasus-4.0.0cvs/
#export EXPERIMENT_HOME=/data/scratch/vahi/work/stampede/futuregrid/montage/
#export PEGASUS_CONDOR_POOL=/ccg/software/pegasus/dev/trunk

#futuregrid
#export PEGASUS_INSTALL=/usr/bin/..
export EXPERIMENT_HOME=/home/zhouchi/experiment
export PEGASUS_CONDOR_POOL=/usr/bin/..
export PEGASUS_HOME=/home/zhouchi/pegasus-source-4.3.2


#export MONTAGE_HOME=$EXPERIMENT_HOME/Montage_v3.3_patched_2
export PATH=$PEGASUS_HOME/bin:$PATH
export CLASSPATH=$CLASSPATH:/home/zhouchi/pegasus-source-4.3.2/dist/pegasus-4.3.2/share/pegasus/java/*
#export JAVA_HOME=/

