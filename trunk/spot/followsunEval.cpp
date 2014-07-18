//n datacenters, each has capacity constraint
//workflow requirements at each datacenter
//a solution is a migration decision, indicating how many workflows to transfer from a to b
#include "datacenter.h"
#include <stdio.h>

float followsunEvalcost(DataCenter** dcs, configstack* config){
	float totalcost=0;//opcost+commcost
	for(int i=0; i<numDCs; i++)
		totalcost += dcs[i]->evalOpCost();
	
	//network transfer cost
	for(int i=0; i<numDCs; i++){
		for(int j=i+1; j<numDCs; j++){
			int trans = config->configurations[position(i,j)];
			float inpriceperM;
			float outpriceperM;
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

			totalcost += std::abs(trans)*firstleveltasks*((*dcs[i]->workloads[0]->g)[0].read_data+
				(*dcs[i]->workloads[0]->g)[0].rec_data+(*dcs[i]->workloads[0]->g)[0].trans_data
				+(*dcs[i]->workloads[0]->g)[0].seq_data)*(inpriceperM+outpriceperM);
			}
		}
	}
	//migration cost, crossnetwork is higher than within the same region
	//how to define??
	return totalcost;
}
void followsunEvalconstraint(DataCenter** dcs,configstack* config, bool* result){
			
	//from x to id or id to x
	for(int i=0; i<numDCs; i++){
		for(int j=i+1; j<numDCs; j++){
			int mig = config->configurations[position(i,j)];
			if(mig<0){
				for(int k=0; k<-mig; k++){
					dcs[i]->tempworkload.push_back(dcs[j]->tempworkload.back());
					dcs[j]->tempworkload.pop_back();
				}			
			}else{
				for(int k=0; k<mig; k++){
					dcs[j]->tempworkload.push_back(dcs[i]->tempworkload.back());
					dcs[i]->tempworkload.pop_back();
				}
			}
		}
	}
	for(int i=0; i<numDCs; i++){
		if(dcs[i]->tempworkload.empty()) result[i]=false;
		else	result[i]=dcs[i]->evalConstraint();
	}

	return;
}

void followsun_initialFeasible(DataCenter** dcs,configstack* initialstate,configstack* feasible)
{
	initialization(dcs);
	for(int i=0; i<numDCs; i++)
		for(int j=0; j<dcs[i]->workloads.size(); j++){
			DAG* dag = new DAG(*dcs[i]->workloads[j]);
			dcs[i]->tempworkload.push_back(dag);
		}
	//std::vector<std::pair<int,int> > keytodcs;
	for(int i=0; i<numDCs; i++)	{
		for(int j=i+1; j<numDCs; j++){
			initialstate->configurations.push_back(0);//transfer from i to j, tran(i,j)=-tran(j,i)
			//keytodcs.push_back(std::pair<int,int>(i,j));
		}
	}
	
	bool overflowdcs[numDCs];	//true is overflow
	std::vector<configstack*> intermediatesolutions;	
	std::vector<configstack*> closedsolutions;
	intermediatesolutions.push_back(initialstate);	
	float globalBestCost = 10000;
	bool continuesearch = true;
	int count = 0;
	do{
		followsunEvalconstraint(dcs,intermediatesolutions.front(),overflowdcs);	
		bool overflow = false;
		for(int i=0; i<numDCs; i++)
			if(overflowdcs[i]){
				overflow = true;
				break;
			}
		if(!overflow){//qos requirements are satisfied, now compare the cost
			float totalcost = followsunEvalcost(dcs,intermediatesolutions.front());
			if(globalBestCost>totalcost)
				globalBestCost = totalcost;
			intermediatesolutions.front()->fvalue = totalcost;
			feasible = intermediatesolutions.front();
		}else{//qos requirements not satisfied, need to migrate workflows to dcs which have idle resources
			std::vector<int> froms;
			std::vector<int> tos;
			for(int i=0; i<numDCs; i++){
				if(overflowdcs[i])
					froms.push_back(i);
				else tos.push_back(i);
			}
			for(int i=0; i<froms.size();i++){ //BFS
				int from = froms[i];
				for(int j=0; j<tos.size(); j++){
					int to = tos[j];
					configstack* state = new configstack();
					state->configurations = intermediatesolutions.front()->configurations;
				//	dcs[to]->tempworkload=dcs[to]->workloads;				
					if(from>to){//-1
						state->configurations[position(from,to)] -= 1 ;
						dcs[to]->tempworkload.pop_back();
					}
					else{
						state->configurations[position(from,to)] += 1 ;
						dcs[to]->tempworkload.push_back(dcs[from]->workloads.back());
					}
					//check if migrate to "to" violates the qos of "to"				
					//if(dcs[to].evalConstraint()){
					//	//if so, this path cannot go down
					//	//simply abandon this one
					//}else{
					//	//otherwise, push this state to the intermediatesolutions for further search
					//	intermediatesolutions.push_back(state);
					//}	
					intermediatesolutions.push_back(state);
				}
			}
		}
		if(intermediatesolutions.empty()) {
			//printf("cannot find one valid solution\n");
			//exit(1);
			continuesearch = false;
		}
		count++;
		
	}while(count<1000||continuesearch);
	

	return;
}

//find out the position of the from,to pair in configstack
int position(int from, int to){
	if(from>to){
		int temp = from;
		from = to;
		to = temp;
	}
	if(from == 0)
		return to-1;
	else{
		return (2*numDCs-from-1)*from/2+(to-from-1);
	}		
}

void initialization(DataCenter** dcs){
	//for the performance of each instance type
	float* random_sequential_io = (float*)malloc(types*randomsize*sizeof(float));
	float* random_random_io = (float*)malloc(types*randomsize*sizeof(float));
	float* random_network_up = (float*)malloc(types*randomsize*sizeof(float));
	float* random_network_down = (float*)malloc(types*randomsize*sizeof(float));
	float* random_tmp = (float*)malloc(types*10000*sizeof(float));
	//read from file	
	FILE* rFile;
	char str[1024];
	char buf[256];
	char *ptr, *ptr2;
	rFile = fopen("randio.csv","r");
	if(rFile == NULL){
		printf("cannot open randio.csv\n");
		exit(1);
	}
	for(int i=0; i<types*10000; i++){
		if(fgets(str,1024,rFile)!=NULL)
			random_tmp[i] = atof(str);
	}	
	for(int i=0; i<types; i++){
		for(int j=0; j<randomsize; j++){
			random_random_io[i*randomsize+j] = random_tmp[i*10000+j];//10000 is fixed
		}
	}
	rFile = fopen("seqio.csv","r");
	if(rFile == NULL){
		printf("cannot open seqio.csv\n");
		exit(1);
	}
	for(int i=0; i<types*10000; i++){
		if(fgets(str,1024,rFile)!=NULL)
			random_tmp[i] = atof(str);
	}	
	for(int i=0; i<types; i++){
		for(int j=0; j<randomsize; j++){
			random_sequential_io[i*randomsize+j] = random_tmp[i*10000+j];//10000 is fixed
		}
	}
	rFile = fopen("netup.csv","r");
	if(rFile == NULL){
		printf("cannot open netup.csv\n");
		exit(1);
	}
	for(int i=0; i<types*10000; i++){
		if(fgets(str,1024,rFile)!=NULL)
			random_tmp[i] = atof(str);
	}	
	for(int i=0; i<types; i++){
		for(int j=0; j<randomsize; j++){
			random_network_up[i*randomsize+j] = random_tmp[i*10000+j];//10000 is fixed
		}
	}
	rFile = fopen("netdown.csv","r");
	if(rFile == NULL){
		printf("cannot open netdown.csv\n");
		exit(1);
	}
	for(int i=0; i<types*10000; i++){
		if(fgets(str,1024,rFile)!=NULL)
			random_tmp[i] = atof(str);
	}	
	for(int i=0; i<types; i++){
		for(int j=0; j<randomsize; j++){
			random_network_down[i*randomsize+j] = random_tmp[i*10000+j];//10000 is fixed
		}
	}
	free(random_tmp);
	std::pair<vertex_iter, vertex_iter> vp;
	
	for(int i=0; i<numDCs; i++){
		for (int jwl=0; jwl<dcs[i]->workloads.size(); jwl++){
			vp = vertices(*dcs[i]->workloads[jwl]->g);

			int quantile = dcs[i]->workloads[jwl]->meet_dl * randomsize;
			for(; vp.first != vp.second; vp.first++){
				//dag.g[*vp.first].probestTime = new float[types][randomsize];
				for(int t=0; t<types; t++){
					for(int j=0; j<randomsize; j++){
						(*dcs[i]->workloads[jwl]->g)[*vp.first].netUp[t*randomsize+j] = (*dcs[i]->workloads[jwl]->g)[*vp.first].trans_data * random_network_up[t*randomsize+j] / 8000;
						(*dcs[i]->workloads[jwl]->g)[*vp.first].netDown[t*randomsize+j] = (*dcs[i]->workloads[jwl]->g)[*vp.first].rec_data * random_network_down[t*randomsize+j] / 8000;
						(*dcs[i]->workloads[jwl]->g)[*vp.first].randomIO[t*randomsize+j] = (*dcs[i]->workloads[jwl]->g)[*vp.first].read_data / random_random_io[t*randomsize+j];
						(*dcs[i]->workloads[jwl]->g)[*vp.first].seqIO[t*randomsize+j] = (*dcs[i]->workloads[jwl]->g)[*vp.first].seq_data / random_sequential_io[t*randomsize+j];
						(*dcs[i]->workloads[jwl]->g)[*vp.first].probestTime[t*randomsize+j] = (*dcs[i]->workloads[jwl]->g)[*vp.first].cpuTime[t] + (*dcs[i]->workloads[jwl]->g)[*vp.first].netUp[t*randomsize+j]
							+ (*dcs[i]->workloads[jwl]->g)[*vp.first].netDown[t*randomsize+j] + (*dcs[i]->workloads[jwl]->g)[*vp.first].randomIO[t*randomsize+j] + (*dcs[i]->workloads[jwl]->g)[*vp.first].seqIO[t*randomsize+j];
//						(*dcs[i]->workloads[jwl]->g)[*vp.first].probestTime[t*randomsize+j] /= 60.0;
					}
					//calculate the estimate time as the expected value of the proestTime
					std::sort((*dcs[i]->workloads[jwl]->g)[*vp.first].probestTime+t*randomsize,(*dcs[i]->workloads[jwl]->g)[*vp.first].probestTime+(t+1)*randomsize-1);
					(*dcs[i]->workloads[jwl]->g)[*vp.first].estTime[t] = (*dcs[i]->workloads[jwl]->g)[*vp.first].probestTime[t*randomsize+quantile];
					printf("task: %d, type: %d, time: %f\n",*vp.first,t,(*dcs[i]->workloads[jwl]->g)[*vp.first].estTime[t]);
				}
				(*dcs[i]->workloads[jwl]->g)[*vp.first].assigned_type = 2;//initially all assigned to large
			}
		}
	}	
}

bool DataCenter::evalConstraint(){//true is overflow
	//a pool of VMs

	std::pair<vertex_iter,vertex_iter> vp;
	int* violation = (int*)malloc(tempworkload.size()*sizeof(int));
	for(int i=0; i<tempworkload.size(); i++)
		violation[i] = 0;
	//run randomsize times is too time consuming, so reduce to once?
	for(int globaliter=0; globaliter<1; globaliter++){
		std::vector<DAG*> jobs;
		for(int i=0; i<tempworkload.size(); i++){
			DAG* newdag = new DAG(*tempworkload[i]);
			vp = vertices(*newdag->g);
			for(int j=0; j<(*vp.second - *vp.first); j++)
				(*newdag->g)[j].assigned_type =2;
			jobs.push_back(newdag);
			std::vector<DAG*> jobs;
		for(int i=0; i<tempworkload.size(); i++){
			DAG* newdag = new DAG(*tempworkload[i]);
			vp = vertices(*newdag->g);
			for(int j=0; j<(*vp.second - *vp.first); j++)
				(*newdag->g)[j].assigned_type =3;
			jobs.push_back(newdag);
		}}
		std::vector<VM*> VMTP[types];
		float t = 0;
		bool condition = false;
		float moneycost = 0;

		float r[4];
		int totalondemand = 0;
		for(int i=0; i< tempworkload.size(); i++){//evaluate use tempworkload
			vp = vertices(*tempworkload[i]->g);
			for(; vp.first!=vp.second; vp.first++){					
				int readys = 0;
				int numtasks = 0;
				if(tempworkload[i]->type == montage){
					readys = 4;
					numtasks = 20;
				}else if(tempworkload[i]->type == montage100){
					readys = 16;
					numtasks = 100;
				}else if(tempworkload[i]->type == montage1000){
					readys = 166;
					numtasks = 1000;
				}else if(tempworkload[i]->type == ligo){
					readys = 9;
					numtasks = 40;
				}else if(tempworkload[i]->type == ligo100){
					readys = 23;
					numtasks = 100;
				}else if (tempworkload[i]->type == ligo1000){
					readys = 229;
					numtasks = 1000;
				}else if(tempworkload[i]->type == epigenome){
					readys = 1;
					numtasks = 20;
				}else if(tempworkload[i]->type == epi100){
					readys = 1;
					numtasks = 100;
				}else if(tempworkload[i]->type == epi1000){
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
		}
		do{
		
			//step 1
			std::vector<taskVertex*> ready_task;
			for(int ji=0; ji<jobs.size(); ji++){
				vp = vertices((*jobs[ji]->g));
				for(int i=0; i < (*vp.second - *vp.first ); i++)
				{
					bool tag = true;
					//get parent vertices
					in_edge_iterator in_i, in_end;
					edge_descriptor e;
					boost::tie(in_i, in_end) = in_edges(i, (*jobs[ji]->g));
					if(in_i == in_end)
						tag = false;
					else {
						for (; in_i != in_end; ++in_i) 
						{
							e = *in_i;
							Vertex src = source(e, (*jobs[ji]->g));					
							if((*jobs[ji]->g)[src].status != finished)
							{
								tag = false;
								break;
							}
						}
					}
					if((*jobs[ji]->g)[i].status == ready || (tag && (*jobs[ji]->g)[i].status != scheduled && (*jobs[ji]->g)[i].status != finished)){
						(*jobs[ji]->g)[i].status = ready;
						ready_task.push_back(&(*jobs[ji]->g)[i]);							
					}
				}
			}			
			//sort according to the subdeadline, earliest deadline first
			//std::sort(ready_task.begin(),ready_task.end(), myfunction);

			for(int i=0; i<ready_task.size(); i++)//no preference on who gets resources first
			{
				taskVertex* curr_task=ready_task[i];
				if(curr_task->readyCountdown == -1)//
				{
					int _config = curr_task->assigned_type;
					bool find = false;
					//check VM/SpotVM list for available machine
					int size = VMTP[_config].size();
					int vmindex = 0;
					for(int j=0; j<size; j++)
					{
						if(VMTP[_config][j]->tk == NULL)
						{
							find = true;
							vmindex = j;
							VMTP[_config][j]->tk = curr_task;
							break;
						}
					}
					if(find) {
						curr_task->status = scheduled;
						curr_task->taskstart = t;
						//curr_task->restTime = curr_task->estTime[_config];//>probestTime[_config*randomsize+globaliter];
						curr_task->restTime = curr_task->probestTime[_config*randomsize+globaliter];

						/*if(VMTP[_config][vmindex]->has_data == curr_task->name)
							curr_task->restTime = curr_task->cpuTime[_config] +  curr_task->netUp[_config*randomsize+globaliter];
						else VMTP[_config][vmindex]->has_data = curr_task->name;*/
					}
					else 			
					{
						curr_task->readyCountdown = OnDemandLag;
						curr_task->taskstart = t;
					}
				}
				else if(curr_task->readyCountdown == 0)
				{
					if(VMTP[curr_task->assigned_type].size()<capacity){//capacity allows to start a new VM
						curr_task->status = scheduled;
						//curr_task->restTime = curr_task->estTime[curr_task->assigned_type];//>probestTime[curr_task->assigned_type*randomsize+globaliter];
						curr_task->restTime = curr_task->probestTime[curr_task->assigned_type*randomsize+globaliter];

						VM* vm = new VM; 
						vm->life_time = OnDemandLag;
						vm->tk = curr_task;
						vm->type = curr_task->assigned_type;
						VMTP[curr_task->assigned_type].push_back(vm);	
					}else curr_task->readyCountdown == OnDemandLag;
				}
			}			
							
			//delete VMs without task
			int delondemand = 0;
			for(int i=0; i<types; i++)
			{
				int size1 = VMTP[i].size();
				totalondemand += size1;
					
				for(int j=0; j<size1; j++)
				{
					float runtime = VMTP[i][j]->life_time;
					if(VMTP[i][j]->tk == NULL&& ((int)ceil(runtime)%60==0))
					{
														
						moneycost += priceOnDemand[i]*ceil(runtime/60.0);
						VM* vm = VMTP[i][j];
						delete vm;
						VMTP[i].erase(VMTP[i].begin()+j);
						j--;
						size1 --;
						delondemand += 1;
					}
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
				
			for(int i=0; i<scheduled_task.size(); i++)
			{
				scheduled_task[i]->restTime -= 1;////////////////////////////
				if(scheduled_task[i]->restTime <= 0) {
					scheduled_task[i]->status = finished;
					scheduled_task[i]->tasktime += t - scheduled_task[i]->taskstart;
					scheduled_task[i]->end_time = t;
					//make the vm.task = NULL
					int index = scheduled_task[i]->assigned_type;
					for(int j=0; j<VMTP[index].size(); j++)
						if(VMTP[index][j]->tk == scheduled_task[i])
						{
							VMTP[index][j]->tk = NULL;
							break;
						}						
				}	
			}
			//check whether constraint is violated
			for(int i=0; i<jobs.size();i++){
				vp = vertices(*jobs[i]->g);
				if(t>jobs[i]->deadline && (*jobs[i]->g)[*vp.second-1].status != finished){
					//clear content					
					violation[i] += 1;//do violate
					if((float)violation[i]/randomsize > (1-jobs[i]->meet_dl)){
						for(int j=0; j<types;j++)
							for(int k=0; k<VMTP[j].size(); k++)
								delete VMTP[j][k];
						return true;
					}
				}
			}
			
			//step 3
			for(int i=0; i<types; i++)
			{
				int size1 = VMTP[i].size();			
					
				for(int j=0; j<size1; j++)
				{
					VMTP[i][j]->life_time += 1;				
				}
			}
			for(int i=0; i<ready_task.size(); i++)//////////////////////////////////if >0
				if(ready_task[i]->readyCountdown > 1e-12)
					ready_task[i]->readyCountdown -= 1;
			t += 1;

			condition = false;
			int unfinishednum = 0;
			for(int ji=0; ji<jobs.size(); ji++){
				vp = vertices((*jobs[ji]->g));
				for(int i=0; i < (*vp.second - *vp.first ); i++){
					if((*jobs[ji]->g)[i].status!= finished){
						condition = true;
						unfinishednum += 1;
					}					
				}
			}								
		}while(condition);//there is a task not finished
		for(int j=0; j<types;j++)
			for(int k=0; k<VMTP[j].size(); k++)
				delete VMTP[j][k];
	}	
	return false;
}
float DataCenter::evalOpCost(){
	float opcost = 0.0;
	float onehour = 3600.0; //seconds
	//round up to 1 hour, use transformations to fully utilize partial hours
	std::pair<vertex_iter,vertex_iter> vp;
	for(int d=0;d<tempworkload.size();d++){
		vp =vertices((*tempworkload[d]->g));
		for(; vp.first!=vp.second; vp.first++){
			float taskcost = 0;
			int config = (*tempworkload[d]->g)[*vp.first].assigned_type;
			for(int i=0; i<randomsize; i++)
				taskcost += (((*tempworkload[d]->g)[*vp.first].probestTime[config*randomsize+i]+OnDemandLag)/onehour)*opCost;//ceil?
			opcost += taskcost / randomsize;
		}
	}
	
	return opcost;
}
