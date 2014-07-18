workflow=montage
deadline=75000
hitrate=0.96
lambda=0.1
numjobs=1
randomsize=1000
algorithm=spss
usecase=ensemble
budget=10
algorithm2=ours

for pattern in constant uniformsorted uniformunsorted paretosorted paretounsorted
do

if [[ $pattern == constant ]]; then
for deadline in 5000 15000 
do
for budget in 10 150 300 450 #10 60 110 160 210 260 310 360 410 460
do
echo $pattern
echo $deadline
echo $budget
rocks run host compute-0-0 "cd /users/staff/amelie/amelie/pdccsvn/amelie/prologworkflow/papercode ./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm $usecase $budget $pattern" > /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm-$usecase-$budget-$pattern.txt&
#./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm2 $usecase $budget $pattern> /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm2-$usecase-$budget-$pattern.txt&
rocks run host compute-0-1 "cd /users/staff/amelie/amelie/pdccsvn/amelie/prologworkflow/papercode ./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm2 $usecase $budget $pattern" > /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm2-$usecase-$budget-$pattern.txt&

done
done
fi

if  [[ $pattern == uniformsorted || $pattern == uniformunsorted ]]; then
for deadline in 5000 15000
do
for budget in 10 2000 4000 6000 8000 #10 900 1800 2700 3600 4500 5400 6300 7200 8000 
do
echo $pattern
echo $deadline
echo $budget
rocks run host compute-0-3 "cd /users/staff/amelie/amelie/pdccsvn/amelie/prologworkflow/papercode ./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm $usecase $budget $pattern" > /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm-$usecase-$budget-$pattern.txt&
#./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm2 $usecase $budget $pattern> /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm2-$usecase-$budget-$pattern.txt&
rocks run host compute-0-4 "cd /users/staff/amelie/amelie/pdccsvn/amelie/prologworkflow/papercode ./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm2 $usecase $budget $pattern" > /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm2-$usecase-$budget-$pattern.txt&

done
done
fi

if  [[ $pattern == paretosorted || $pattern == paretounsorted ]]; then
for deadline in 5000 15000
do
for budget in 10 1200 2400 3600 #10 400 800 1200 1600 2000 2400 2800 3200 3600
do
echo $pattern
echo $deadline
echo $budget
rocks run host compute-0-5 "cd /users/staff/amelie/amelie/pdccsvn/amelie/prologworkflow/papercode ./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm $usecase $budget $pattern" > /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm-$usecase-$budget-$pattern.txt&
#./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm2 $usecase $budget $pattern> /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm2-$usecase-$budget-$pattern.txt&
rocks run host compute-0-6 "cd /users/staff/amelie/amelie/pdccsvn/amelie/prologworkflow/papercode ./PrologInter $workflow $deadline $hitrate $lambda $numjobs $randomsize $algorithm2 $usecase $budget $pattern" > /data/amelie/$workflow-$deadline-$hitrate-$lambda-$numjobs-$randomsize-$algorithm2-$usecase-$budget-$pattern.txt&

done
done
fi

done
