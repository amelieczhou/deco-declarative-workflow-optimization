#include "algorithm.h"
//#include "omp.h"
#include <ctime>
#include <time.h>

void COLOG::PlanMaking(){
	for(int i=0; i<numDCs; i++)
		for(int j=0; j<dcs[i]->workloads.size(); j++){
			DAG* dag = new DAG(*dcs[i]->workloads[j]);
			dcs[i]->tempworkload.push_back(dag);
		}

	//heuristic: for each overflowed dc, choose the cheapest one from the rest to migrate
	std::vector<DataCenter*> overflowdcs;
	std::vector<DataCenter*> underflowdcs;
	for(int i=0; i<numDCs; i++){
		if(dcs[i]->evalConstraint())
			overflowdcs.push_back(dcs[i]);
		else underflowdcs.push_back(dcs[i]);
	}
	//choose from underflowdcs the cheapest one and migrate one workflow to it
	//then check whether the migration cause overflow of the migrated datacenter
	//if yes, choose the next cheapest until done
	//else check the overflow datacenter if it's overflow anymore
	//if yes, continue the migration process
	//otherwise go to the next overflow datacenter
	for(int i=0; i<overflowdcs.size(); i++){
		bool continuemigrate = false;
		do{
			float cheapest = 10000;
			int migto = -1;
			for(int j=0; j<underflowdcs.size(); j++){//choose one cheapest
				underflowdcs[j]->tempworkload.push_back(overflowdcs[i]->tempworkload.back());
				overflowdcs[i]->tempworkload.pop_back();
				if(!overflowdcs[j]->evalConstraint()){
					float curcost = 0;
					curcost += underflowdcs[j]->evalOpCost() + overflowdcs[i]->evalOpCost();
					//communication cost
					for(int k=0; k<overflowdcs[i]->tempworkload.size(); k++){
						curcost += (*overflowdcs[i]->tempworkload[k]->g)[0].seq_data*overflowdcs[i]->commCost[j];
					}
					if(curcost < cheapest){
						cheapest = curcost;
						migto = j;
					}
					overflowdcs[i]->tempworkload.push_back(underflowdcs[j]->tempworkload.back());
					underflowdcs[j]->tempworkload.pop_back();
				}
			}
			if(migto != -1){
				//migrate overflowdcs[i] workflow to overflowdcs[migto]
				int from = overflowdcs[i]->DCid;
				int to = underflowdcs[migto]->DCid;
				if(from>to)
					plan[position(from,to)] -= 1;
				else plan[position(from,to)] += 1;
				dcs[to]->workloads.push_back(dcs[from]->workloads.back());
				dcs[from]->workloads.pop_back();
				//update tempworkload
				dcs[from]->tempworkload.push_back(dcs[to]->tempworkload.back());
				dcs[to]->tempworkload.pop_back();
			}else{
				printf("cannot find a dc to migrate \n");
				exit(1);
			}
			//after migrate one workflow, check whether overflow or not
			continuemigrate = overflowdcs[i]->evalConstraint();
		}while(continuemigrate);
	}
}

void COLOG::Initialize(){
	plan = (int*)malloc(sizeof(int)*(numDCs*(numDCs-1)/2));//in total numDCs*(numDCs-1)/2
	for(int i=0; i<numDCs*(numDCs-1)/2; i++)
		plan[i] = 0;
	initialization(dcs);
	return;
}
void COLOG::Simulate(bool colog){
	//initialization(dcs);	
	//use tempworkload to make decision	
	if(colog)
		PlanMaking();

	//start simulation process
	//first calculate the network transfer cost
	float commcost=0;	

	for(int i=0; i<numDCs; i++){
		for(int j=i+1; j<numDCs; j++){
			int trans = plan[position(i,j)];
			float inpriceperM =0;
			float outpriceperM =0;
			if(trans!=0){
				//do transfer, only charges the data outof EC2				
				if(dcs[i]->region==dcs[j]->region&&dcs[i]->zone==dcs[j]->zone){
					inpriceperM = priceNetwork[0] /1000;//1000M=1G
					outpriceperM = priceNetwork[3] /1000;//1000M=1G
				}else if(dcs[i]->region==dcs[j]->region&&dcs[i]->zone!=dcs[j]->zone){
					inpriceperM = priceNetwork[1] /1000;//1000M=1G
					outpriceperM = priceNetwork[4] /1000;//1000M=1G
				}else if(dcs[i]->region!=dcs[j]->region){
					inpriceperM = priceNetwork[2] /1000;//1000M=1G
					outpriceperM = priceNetwork[5] /1000;//1000M=1G
				}
			
				int firstleveltasks;
				if(dcs[i]->workloads[0]->type == montage)
					firstleveltasks = 4;
				else if(dcs[i]->workloads[0]->type == montage100)
					firstleveltasks = 16;
				else if(dcs[i]->workloads[0]->type == montage1000)
					firstleveltasks = 166;
				else {
					printf("calculation of transfer data size is only designed for montage\n");
					exit(1);
				}

				commcost += std::abs(trans)*firstleveltasks*((*dcs[i]->workloads[0]->g)[0].read_data+
					(*dcs[i]->workloads[0]->g)[0].rec_data+(*dcs[i]->workloads[0]->g)[0].trans_data
					+(*dcs[i]->workloads[0]->g)[0].seq_data)*(inpriceperM+outpriceperM);
			}
		}
	}
	//migration cost, crossnetwork is higher than within the same region
	//how to define??
	
	//2nd step: simulate the operation cost after migration
	for(int i=0; i<numDCs; i++){
		for(int j=i+1; j<numDCs; j++){
			int mig = plan[position(i,j)];
			if(mig>0){//migration from i to j
				for(int t=0; t<mig; t++){
					dcs[j]->workloads.push_back(dcs[i]->workloads.back());
					dcs[i]->workloads.pop_back();
				}
			}else if(mig<0){
				for(int t=0; t>mig; t--){
					dcs[i]->workloads.push_back(dcs[j]->workloads.back());
					dcs[j]->workloads.pop_back();
				}			
			}
		}
	}
	double operacost = 0;
	//start the simulation
//	omp_set_num_threads(24);
	for(int dciter=0; dciter<numDCs; dciter++){
//		float viol_private[24];
//		float cost_private[24];
//		for(int i=0; i<24; i++) viol_private[i]=cost_private[i]=0;

		float* violation = (float*)malloc(sizeof(float)*dcs[dciter]->workloads.size());
		float* ave_time = (float*)malloc(sizeof(float)*dcs[dciter]->workloads.size());
	        for(int i=0; i<dcs[dciter]->workloads.size(); i++){
                	violation[i] = 0.0;
        	        ave_time[i] = 0.0;
	        }

		float  ave_cost = 0;
		std::clock_t starttime = std::clock();
		std::pair<vertex_iter,vertex_iter> vp;

//		#pragma omp parallel
		{
//			#pragma omp for			
			for(int globaliter=0; globaliter<randomsize; globaliter++){
				std::vector<DAG*> jobs;
				for(int i=0; i<dcs[dciter]->workloads.size(); i++){
					DAG* newdag = new DAG(*dcs[dciter]->workloads[i]);
					jobs.push_back(newdag);
				}
				std::vector<VM*> VMTP;
				for(int i=0; i<jobs.size(); i++)
					for(vp=vertices(*jobs[i]->g);vp.first!=vp.second;vp.first++)
						(*jobs[i]->g)[*vp.first].assigned_type = 2;//large

				//EDF scheduling
				double t = 0;
				bool condition = false;
				double moneycost = 0.0;

				for(int i=0; i<jobs.size(); i++){					
					int readys = 0;
					int numtasks = 0;
					if(jobs[0]->type == montage){
						readys = 4;
						numtasks = 20;
					}else if(jobs[0]->type == montage100){
						readys = 16;
						numtasks = 100;
					}else if(jobs[0]->type == montage1000){
						readys = 166;
						numtasks = 1000;
					}else if(jobs[0]->type == ligo){
						readys = 9;
						numtasks = 40;
					}else if(jobs[0]->type == ligo100){
						readys = 23;
						numtasks = 100;
					}else if (jobs[0]->type == ligo1000){
						readys = 229;
						numtasks = 1000;
					}else if(jobs[0]->type == epigenome){
						readys = 1;
						numtasks = 20;
					}else if(jobs[0]->type == epi100){
						readys = 1;
						numtasks = 100;
					}else if(jobs[0]->type == epi1000){
						readys = 7;
						numtasks = 997;
					}else{
						printf("what is the dag type?");
						exit(1);
					}
						
					for(int j=0; j<readys; j++) {
						(*jobs[i]->g)[j].status = ready;
						(*jobs[i]->g)[j].readyCountdown = -1;
						(*jobs[i]->g)[j].restTime = 0;				
					}
					for(int j=readys; j<numtasks; j++){
						(*jobs[i]->g)[j].status = not_ready;
						(*jobs[i]->g)[j].readyCountdown = -1;
						(*jobs[i]->g)[j].restTime = 0;				
					}					
				}

				do{	
					
					//step 1
					std::vector<taskVertex*> ready_task;
					for(int ji=0; ji<jobs.size(); ji++){
						vp = vertices(*jobs[ji]->g);
						for(int i=0; i < (*vp.second - *vp.first ); i++)
						{
							bool tag = true;
							//get parent vertices
							in_edge_iterator in_i, in_end;
							edge_descriptor e;
							boost::tie(in_i, in_end) = in_edges(i, *jobs[ji]->g);
							if(in_i == in_end) tag = false;
							else{
								for (; in_i != in_end; ++in_i) 
								{
									e = *in_i;
									Vertex src = source(e, *jobs[ji]->g);					
									if((*jobs[ji]->g)[src].status != finished)
									{
										tag = false;
										//break;
									}
								}
							}
							if((*jobs[ji]->g)[i].status == ready || (tag && (*jobs[ji]->g)[i].status != scheduled && (*jobs[ji]->g)[i].status != finished)){
								ready_task.push_back(&(*jobs[ji]->g)[i]);							
							}
						}
					}


					//std::sort(ready_task.begin(),ready_task.end(), myfunction);
					for(int i=0; i<ready_task.size(); i++)//earliest deadline first
					{
						taskVertex* curr_task=ready_task[i];
						if(curr_task->readyCountdown == -1)//
						{
							bool find = false;
							//check VM/SpotVM list for available machine
							int size = VMTP.size();
							for(int j=0; j<size; j++)
							{
								if(VMTP[j]->tk == NULL)
								{
									find = true;
									VMTP[j]->tk = curr_task;
									break;
								}
							}
							if(find) {
								curr_task->status = scheduled;
								curr_task->tasktime = t;
								curr_task->restTime =  curr_task->probestTime[curr_task->assigned_type*randomsize+globaliter] ;
							}
							else 			
							{
								curr_task->readyCountdown = OnDemandLag;
								curr_task->tasktime = t;
							}
						}
						else if(curr_task->readyCountdown == 0)
						{
							if(VMTP.size()<dcs[dciter]->capacity){
								curr_task->status = scheduled;
								curr_task->restTime = curr_task->probestTime[curr_task->assigned_type*randomsize+globaliter] ;

								VM* vm = new VM; 
								vm->life_time = OnDemandLag;
								vm->tk = curr_task;
								vm->type = curr_task->assigned_type;
								VMTP.push_back(vm);
							}else{
								curr_task->readyCountdown = OnDemandLag;
							}					
						}			
					}
					//delete VMs without task
			
					int size1 = VMTP.size();					
					for(int j=0; j<size1; j++)
					{
						if(VMTP[j]->tk == NULL)
						{
							double runtime = VMTP[j]->life_time;
							moneycost += (dcs[dciter]->opCost* ceil(runtime/3600.0));

							VM* vm = VMTP[j];
							delete vm;
							VMTP.erase(VMTP.begin()+j);
							j--;
							size1--;
						}
					}
				
					//step 2
					std::vector<taskVertex*> scheduled_task;
					for(int ji=0; ji<jobs.size(); ji++){
						vp = vertices((*jobs[ji]->g));
						for(int i=0; i<(*vp.second - *vp.first ); i++)
							if((*jobs[ji]->g)[i].status == scheduled)
								scheduled_task.push_back(&(*jobs[ji]->g)[i]);
					}
					/*if(scheduled_task.size()==0)
						for(vp=vertices((*jobs[0]->g));vp.first!=vp.second; vp.first++)
							printf("%d\n",(*jobs[0]->g)[*vp.first].status);*/
					for(int i=0; i<scheduled_task.size(); i++)
					{
						scheduled_task[i]->restTime -= 1;////////////////////////////
						if(scheduled_task[i]->restTime <= 0) 
						{
							scheduled_task[i]->status = finished;
							scheduled_task[i]->end_time = t;
							scheduled_task[i]->tasktime = t - scheduled_task[i]->tasktime;
							//scheduled_task[i]->cost = scheduled_task[i]->tasktime * priceOnDemand[scheduled_task[i]->assigned_type] /3600.0;
							//make the vm.task = NULL
							for(int j=0; j<VMTP.size(); j++)
								if(VMTP[j]->tk == scheduled_task[i])
								{
									VMTP[j]->tk = NULL;
									break;
								}
						}
					}				
					//step 3		
					for(int j=0; j<VMTP.size(); j++)	{
						VMTP[j]->life_time += 1;				
					}
				
					for(int i=0; i<ready_task.size(); i++)//////////////////////////////////if >0
						if(ready_task[i]->readyCountdown > 0)
							ready_task[i]->readyCountdown -= 1;
					t += 1;

					condition = false;
					int unfinishednum = 0;
					for(int ji=0; ji<jobs.size(); ji++){
						vp = vertices((*jobs[ji]->g));
						for(int i=0; i < (*vp.second - *vp.first ); i++){
							if((*jobs[ji]->g)[i].status!= finished)
							{
								condition = true;
								unfinishednum += 1;
							}					
						}
					}								
				}while(condition);//there is a task not finished

				int size1 = VMTP.size();						
				for(int j=0; j<size1; j++)
				{
					double runtime = VMTP[j]->life_time;
					moneycost += (dcs[dciter]->opCost * ceil(runtime/3600.0));
				}
			
				printf("Datacenter: %d, Money Cost: %.4f, Time: %.2f\n",dciter, moneycost, t);
//				int id = omp_get_thread_num();
//				printf("thread id is %d\n",id);
//				float ave_time = 0.0;
				for(int i=0; i<jobs.size(); i++){
					vp = vertices((*jobs[i]->g));
					float executiontime = (*jobs[i]->g)[*(vp.second-2)].end_time - jobs[i]->arrival_time;
					if(executiontime > jobs[i]->deadline) {
						violation[i] += 1.0;
					}		
					ave_time[i] += executiontime;
				}
				ave_cost += moneycost;	
				//cost_private[id] += moneycost;
				//printf("average execution time of workflows is %f\n",ave_time/jobs.size());
				for(int i=0; i<jobs.size(); i++){
					delete jobs[i];
				}
				jobs.clear();
			}
			/*
			for(int i=0; i<24; i++) {
				violation += viol_private[i];
				ave_cost += cost_private[i];
			}*/
		}//parallel
		operacost += ave_cost;
		for(int i=0; i<dcs[dciter]->workloads.size(); i++){
			printf("datacenter %d job %d, deadline meeting rate is %.4f, average execution time is %.4f\n",dciter,i,1.0-violation[i]/randomsize, ave_time[i]/randomsize);
		}
			//violation /= (float)randomsize*dcs[dciter]->workloads.size();
			//ave_cost /= (float)randomsize*dcs[dciter]->workloads.size();
			//printf("deadline meeting rate is %f, average cost is %f\n",1.0-violation,ave_cost);
			std::clock_t endtime = std::clock();
			std::clock_t timeelapsed = (float)(endtime - starttime) / (float)CLOCKS_PER_SEC;
			printf("time elapsed for SC11 algorithm is: %.4f\n", timeelapsed);
		
	}//dcs
	float overallcost = commcost + operacost;
	printf("the overallcost is %.4f\n",overallcost);
	return;
}
