#!/bin/bash

workflow=montage100
deadline=75000
hitrate=0.92
lambda=0.1
numjobs=1
randomsize=1000
algorithm=Autoscaling
usecase=deadline
budget=10
algorithm2=ours

#rocks run host compute-0-1 "cd /users/staff/amelie/amelie/pdccsvn/amelie/prologworkflow/papercode ./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm $usecase $budget" > /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm-$usecase-$budget.txt&
./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm2 $usecase $budget > /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm2-$usecase-$budget.txt&
#rocks run host compute-0-3 "cd /users/staff/amelie/amelie/pdccsvn/amelie/prologworkflow/papercode ./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm2 $usecase $budget" > /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm2-$usecase-$budget.txt&

