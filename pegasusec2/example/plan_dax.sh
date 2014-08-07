#!/bin/bash

if [ $# -ne 1 ]; then
	echo "Usage: $0 DAXFILE"
	exit 1
fi

DAXFILE=$1

# This command tells Pegasus to plan the workflow contained in 
# "diamond.dax" using the config file "pegasusrc". The planned
# workflow will be stored in a relative directory named "submit".
# The execution site is "PegasusVM" and the output site is "local".
# --force tells Pegasus not to prune anything from the workflow, and
# --nocleanup tells Pegasus not to generate cleanup jobs.
pegasus-plan --conf pegasusrc -d $DAXFILE --dir submit \
	--force --sites PegasusVM1,PegasusVM2  -o local --nocleanup -vvv 
