#!/bin/bash

cd ../pegasus-source-4.3.2
ant
cd ../experiment
./plan_dax.sh pipe.dax
