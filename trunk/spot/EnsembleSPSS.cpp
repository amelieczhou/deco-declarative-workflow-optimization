#include "algorithm.h"
//#include "omp.h"
#include <time.h>
#include <ctime>
#include <fstream>
#include <sstream>

extern float budget;
float EnsembleSPSS::planWorkflow(DAG dag){
	//int* plancopy = plan;
	float moneycost = 0;
	float t = 0;
	bool condition = false;
	std::vector<VM*> VMTP[types];
	std::pair<vertex_iter, vertex_iter> vp;
	vp = vertices(*dag.g);
	for(; vp.first!=vp.second; ++vp.first) {//edge weight for communication cost		
		Vertex v1 = *vp.first;
		(*dag.g)[v1].prefer_type = 3;//xlarge
	}
//	dag.deadline_assign(); //add redundant tasks?
	//estimate current cost of the plan
	vp=vertices(*dag.g);

	int readys = 0;
	int numtasks = 0;
	if(dag.type == montage){
		readys = 4;
		numtasks = 20;
	}else if(dag.type == montage100){
		readys = 16;
		numtasks = 100;
	}else if(dag.type == montage1000){
		readys = 166;
		numtasks = 1000;
	}else if(dag.type == ligo){
		readys = 9;
		numtasks = 40;
	}else if(dag.type == ligo100){
		readys = 23;
		numtasks = 100;
	}else if (dag.type == ligo1000){
		readys = 229;
		numtasks = 1000;
	}else if(dag.type == epigenome){
		readys = 1;
		numtasks = 20;
	}else if(dag.type == epi100){
		readys = 1;
		numtasks = 100;
	}else if(dag.type == epi1000){
		readys = 7;
		numtasks = 997;
	}else{
		printf("what is the dag type?");
		exit(1);
	}
						
	for(int j=0; j<readys; j++) {
		(*dag.g)[j].status = ready;
		(*dag.g)[j].readyCountdown = -1;
		(*dag.g)[j].restTime = 0;				
	}
	for(int j=readys; j<numtasks; j++){
		(*dag.g)[j].status = not_ready;
		(*dag.g)[j].readyCountdown = -1;
		(*dag.g)[j].restTime = 0;				
	}
						
	//if(dag.type == montage){
	//	for(int j=1; j<5; j++) {
	//		(*dag.g)[j].status = ready;
	//		(*dag.g)[j].readyCountdown = -1;
	//		(*dag.g)[j].restTime = 0;				
	//	}
	//	for(int j=5; j<21; j++){
	//		(*dag.g)[j].status = not_ready;
	//		(*dag.g)[j].readyCountdown = -1;
	//		(*dag.g)[j].restTime = 0;	
	//	}
	//	(*dag.g)[0].status = (*dag.g)[21].status = finished;
	//}else if(dag.type == ligo){
	//	for(int j=1; j<10; j++) {
	//		(*dag.g)[j].status = ready;
	//		(*dag.g)[j].readyCountdown = -1;
	//		(*dag.g)[j].restTime = 0;				
	//	}
	//	for(int j=10; j<41; j++){
	//		(*dag.g)[j].status = not_ready;
	//		(*dag.g)[j].readyCountdown = -1;
	//		(*dag.g)[j].restTime = 0;	
	//	}
	//	(*dag.g)[0].status = (*dag.g)[41].status = finished;
	//}else if(dag.type == epigenome){				
	//	(*dag.g)[1].status = ready;
	//	(*dag.g)[1].readyCountdown = -1;
	//	(*dag.g)[1].restTime = 0;	
	//	for(int j=2; j<21; j++){
	//		(*dag.g)[j].status = not_ready;
	//		(*dag.g)[j].readyCountdown = -1;
	//		(*dag.g)[j].restTime = 0;	
	//	}
	//	(*dag.g)[0].status = (*dag.g)[21].status = finished;
	//}else{
	//	printf("what is the dag type?");
	//	exit(1);
	//}
	do{
		std::vector<taskVertex*> ready_task;
		vp = vertices(*dag.g);
		for(int i=0; i < (*vp.second - *vp.first ); i++)
		{
			bool tag = true;
			//get parent vertices
			in_edge_iterator in_i, in_end;
			edge_descriptor e;
			boost::tie(in_i, in_end) = in_edges(i, *dag.g);
			if(in_i == in_end) tag = false;
			else{
				for (; in_i != in_end; ++in_i) 
				{
					e = *in_i;
					Vertex src = source(e, *dag.g);					
					if((*dag.g)[src].status != finished)	{
						tag = false;
						//break;
					}
				}
			}
			if((*dag.g)[i].status == ready || (tag && (*dag.g)[i].status != scheduled && (*dag.g)[i].status != finished)){
				ready_task.push_back(&(*dag.g)[i]);							
			}
		}
			
		std::sort(ready_task.begin(),ready_task.end(), myfunction);
		for(int i=0; i<ready_task.size(); i++)//earliest deadline first
		{
			taskVertex* curr_task=ready_task[i];
			if(curr_task->readyCountdown == -1)//
			{
						
				int _config = curr_task->prefer_type;
				bool find = false;
				//check VM/SpotVM list for available machine
				int size = VMTP[_config].size();
				for(int j=0; j<size; j++)
				{
					if(VMTP[_config][j]->tk == NULL)
					{
						find = true;
						VMTP[_config][j]->tk = curr_task;
						break;
					}
				}
				if(find) {
					curr_task->status = scheduled;
					curr_task->tasktime = t;
					curr_task->restTime =  curr_task->estTime[curr_task->prefer_type] ;
				}
				else 			
				{
					curr_task->readyCountdown = OnDemandLag;
					curr_task->tasktime = t;
				}
			}
			else if(curr_task->readyCountdown == 0)
			{
				curr_task->status = scheduled;
				curr_task->restTime = curr_task->estTime[curr_task->prefer_type] ;

				VM* vm = new VM; 
				vm->life_time = OnDemandLag;
				vm->tk = curr_task;
				vm->type = curr_task->prefer_type;
				VMTP[curr_task->prefer_type].push_back(vm);
						
			}			
		}
		//delete VMs without task
		for(int i=0; i<types; i++)//////////
		{
			int size1 = VMTP[i].size();					
			for(int j=0; j<size1; j++)	{
				if(VMTP[i][j]->tk == NULL)	{
					double runtime = VMTP[i][j]->life_time;
					moneycost += (priceOnDemand[i] * ceil(runtime/3600.0));

					VM* vm = VMTP[i][j];
					delete vm;
					VMTP[i].erase(VMTP[i].begin()+j);
					j--;
					size1--;
				}
			}
		}
		//step 2
		std::vector<taskVertex*> scheduled_task;
		vp = vertices((*dag.g));
		for(int i=0; i<(*vp.second - *vp.first ); i++)
			if((*dag.g)[i].status == scheduled)
				scheduled_task.push_back(&(*dag.g)[i]);
				
				
		for(int i=0; i<scheduled_task.size(); i++)
		{
			scheduled_task[i]->restTime -= 1;////////////////////////////
			if(scheduled_task[i]->restTime <= 0) 
			{
				scheduled_task[i]->status = finished;
				scheduled_task[i]->end_time = t;
				scheduled_task[i]->tasktime = t - scheduled_task[i]->tasktime;
				scheduled_task[i]->cost = scheduled_task[i]->tasktime * priceOnDemand[scheduled_task[i]->prefer_type] /3600.0;
				//make the vm.task = NULL
				for(int j=0; j<VMTP[scheduled_task[i]->prefer_type].size(); j++)
					if(VMTP[scheduled_task[i]->prefer_type][j]->tk == scheduled_task[i])
					{
						VMTP[scheduled_task[i]->prefer_type][j]->tk = NULL;
						break;
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
			if(ready_task[i]->readyCountdown > 0)
				ready_task[i]->readyCountdown -= 1;
		t += 1;

		condition = false;
		int unfinishednum = 0;
		vp = vertices((*dag.g));
		for(int i=0; i < (*vp.second - *vp.first ); i++){
			if((*dag.g)[i].status!= finished)
			{
				condition = true;
				unfinishednum += 1;
			}					
		}
												
	}while(condition);//there is a task not finished

	for(int i=0; i<types; i++)
	{
		int size1 = VMTP[i].size();						
		for(int j=0; j<size1; j++)
		{
			double runtime = VMTP[i][j]->life_time;
			moneycost += (priceOnDemand[i] * ceil(runtime/3600.0));
		}
	}
	return moneycost;
}


void EnsembleSPSS::planEnsemble(int* plan){
	for(int i=0; i<dags.size(); i++){
		plan[i] = 0;//none of the workflows is executed.
	}
	float currentcost = 0;
	for(int i=0; i<dags.size(); i++){
		float cur=planWorkflow(*dags[i])+currentcost;
		if( cur < budget){
			plan[i] = 1;
			currentcost =cur; 
		}else break;
	}
}

void EnsembleSPSS::Simulate(bool spss){
	std::pair<vertex_iter, vertex_iter> vp;
	if(spss){
		plan = (int*)malloc(dags.size()*sizeof(int));
		for(int i=0; i<dags.size(); i++){
			plan[i] = 0;//none of the workflows is executed.
		}
		//prepare for runtime
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

		property_map<Graph, edge_weight_t>::type weightmap;
		for(int i=0; i<dags.size(); i++){
			weightmap = get(edge_weight, *dags[i]->g);
			vp = vertices(*dags[i]->g);
			int quantile = dags[i]->meet_dl * randomsize;//0.5;
			for(; vp.first != vp.second; vp.first++){
				//dag.g[*vp.first].probestTime = new float[types][randomsize];
				for(int t=0; t<types; t++){
					for(int j=0; j<randomsize; j++){
						(*dags[i]->g)[*vp.first].netUp[t*randomsize+j] = (*dags[i]->g)[*vp.first].trans_data * random_network_up[t*randomsize+j] / 8000;
						(*dags[i]->g)[*vp.first].netDown[t*randomsize+j] = (*dags[i]->g)[*vp.first].rec_data * random_network_down[t*randomsize+j] / 8000;
						(*dags[i]->g)[*vp.first].randomIO[t*randomsize+j] = (*dags[i]->g)[*vp.first].read_data / random_random_io[t*randomsize+j];
						(*dags[i]->g)[*vp.first].seqIO[t*randomsize+j] = (*dags[i]->g)[*vp.first].seq_data / random_sequential_io[t*randomsize+j];
						(*dags[i]->g)[*vp.first].probestTime[t*randomsize+j] = (*dags[i]->g)[*vp.first].cpuTime[t] + (*dags[i]->g)[*vp.first].netUp[t*randomsize+j]
							+ (*dags[i]->g)[*vp.first].netDown[t*randomsize+j] + (*dags[i]->g)[*vp.first].randomIO[t*randomsize+j] + (*dags[i]->g)[*vp.first].seqIO[t*randomsize+j];
					}
					//calculate the estimate time as the expected value of the proestTime
					std::sort((*dags[i]->g)[*vp.first].probestTime+t*randomsize,(*dags[i]->g)[*vp.first].probestTime+(t+1)*randomsize-1);
					(*dags[i]->g)[*vp.first].estTime[t] = (*dags[i]->g)[*vp.first].probestTime[t*randomsize+quantile];
					printf("task: %d, type: %d, time: %f\n",*vp.first,t,(*dags[i]->g)[*vp.first].estTime[t]);
				}
			}	
		}
	 

		free(random_sequential_io);
		free(random_random_io);
		free(random_network_up);
		free(random_network_down);

		//initialization, do deadline assignment and instance config
		time_t start,end;
		time(&start);
		planEnsemble(plan);
		time(&end);
		printf("optimization overhead of static is %.4f\n",difftime(end,start));
	}
	

	float* violation = (float*)malloc(sizeof(float)*dags.size());
	float* ave_time = (float*)malloc(sizeof(float)*dags.size());
	for(int i=0; i<dags.size(); i++){
		violation[i] = 0.0;	
		ave_time[i] = 0.0;
	}
	float ave_cost = 0;
	float ioseq[types],iorand[types],net_up[types],net_down[types];
//	omp_set_num_threads(1);
//	float viol_private[1];
//	float cost_private[1];
//	for(int i=0; i<1; i++) viol_private[i]=cost_private[i]=0;
	
	
	std::clock_t starttime = std::clock();
//	#pragma omp parallel
	{
//		#pragma omp for
		for(int monte=0; monte < randomsize; monte++)
		{
			/*for(int i=0; i<types; i++){
				ioseq[i] = random_sequential_io[randomsize*i+monte];
				iorand[i] = random_random_io[randomsize*i+monte];
				net_up[i] = random_network_up[randomsize*i+monte];
				net_down[i] = random_network_down[randomsize*i+monte];
			}*/		
	
			std::vector<DAG*> jobs;
			for(int i=0; i<dags.size(); i++){
				DAG* newdag = new DAG(*dags[i]);
				vp = vertices(*newdag->g);
				for(int j=0; j<(*vp.second - *vp.first); j++){
					(*newdag->g)[j].prefer_type = 3;
					(*newdag->g)[j].assigned_type = 3;
				}
				jobs.push_back(newdag);
			}
			std::vector<VM*> VMTP[types];
			int need_VM[types]={0,0,0,0};

			//EDF scheduling
			double t = 0;
			bool condition = false;
			double moneycost = 0.0;

			for(int i=0; i<dags.size(); i++){
				if(plan[i]==1){
					int readys = 0;
					int numtasks = 0;
					if(dags[i]->type == montage){
						readys = 4;
						numtasks = 20;
					}else if(dags[i]->type == montage100){
						readys = 16;
						numtasks = 100;
					}else if(dags[i]->type == montage1000){
						readys = 166;
						numtasks = 1000;
					}else if(dags[i]->type == ligo){
						readys = 9;
						numtasks = 40;
					}else if(dags[i]->type == ligo100){
						readys = 23;
						numtasks = 100;
					}else if (dags[i]->type == ligo1000){
						readys = 229;
						numtasks = 1000;
					}else if(dags[i]->type == epigenome){
						readys = 1;
						numtasks = 20;
					}else if(dags[i]->type == epi100){
						readys = 1;
						numtasks = 100;
					}else if(dags[i]->type == epi1000){
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
					if(plan[ji]==1){
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
				}


				//std::sort(ready_task.begin(),ready_task.end(), myfunction);
				for(int i=0; i<ready_task.size(); i++)//earliest deadline first
				{
					taskVertex* curr_task=ready_task[i];
					if(curr_task->readyCountdown == -1)//
					{
						
						int _config = curr_task->prefer_type;
						bool find = false;
						//check VM/SpotVM list for available machine
						int size = VMTP[_config].size();
						for(int j=0; j<size; j++)
						{
							if(VMTP[_config][j]->tk == NULL)
							{
								find = true;
								VMTP[_config][j]->tk = curr_task;
								break;
							}
						}
						if(find) {
							curr_task->status = scheduled;
							curr_task->tasktime = t;
							curr_task->restTime =  curr_task->probestTime[curr_task->prefer_type*randomsize+monte] ;
						}
						else 			
						{
							curr_task->readyCountdown = OnDemandLag;
							curr_task->tasktime = t;
						}
					}
					else if(curr_task->readyCountdown == 0)
					{
						curr_task->status = scheduled;
						curr_task->restTime = curr_task->probestTime[curr_task->prefer_type*randomsize+monte] ;

						VM* vm = new VM; 
						vm->life_time = OnDemandLag;
						vm->tk = curr_task;
						vm->type = curr_task->prefer_type;
						VMTP[curr_task->prefer_type].push_back(vm);
						
					}			
				}
				//delete VMs without task
				for(int i=0; i<types; i++)//////////
				{
					int size1 = VMTP[i].size();
					
					for(int j=0; j<size1; j++)
					{
						if(VMTP[i][j]->tk == NULL)
						{
							double runtime = VMTP[i][j]->life_time;
							moneycost += (priceOnDemand[i] * ceil(runtime/3600.0));

							VM* vm = VMTP[i][j];
							delete vm;
							VMTP[i].erase(VMTP[i].begin()+j);
							j--;
							size1--;
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
					if(scheduled_task[i]->restTime <= 0) 
					{
						scheduled_task[i]->status = finished;
						scheduled_task[i]->end_time = t;
						scheduled_task[i]->tasktime = t - scheduled_task[i]->tasktime;
						scheduled_task[i]->cost = scheduled_task[i]->tasktime * priceOnDemand[scheduled_task[i]->prefer_type] /3600.0;
						//make the vm.task = NULL
						for(int j=0; j<VMTP[scheduled_task[i]->prefer_type].size(); j++)
							if(VMTP[scheduled_task[i]->prefer_type][j]->tk == scheduled_task[i])
							{
								VMTP[scheduled_task[i]->prefer_type][j]->tk = NULL;
								break;
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
					if(ready_task[i]->readyCountdown > 0)
						ready_task[i]->readyCountdown -= 1;
				t += 1;

				condition = false;
				int unfinishednum = 0;
				for(int ji=0; ji<jobs.size(); ji++){
					vp = vertices((*jobs[ji]->g));
					for(int i=0; i < (*vp.second - *vp.first ); i++){
						if((*jobs[ji]->g)[i].status!= finished && plan[ji]==1)
						{
							condition = true;
							unfinishednum += 1;
						}					
					}
				}								
			}while(condition);//there is a task not finished

			for(int i=0; i<types; i++)
			{
				int size1 = VMTP[i].size();						
				for(int j=0; j<size1; j++)
				{
					double runtime = VMTP[i][j]->life_time;
					moneycost += (priceOnDemand[i] * ceil(runtime/3600.0));
				}
			}
			printf("Money Cost: %.4f, Time: %.2f\n", moneycost, t);
			//int id = omp_get_thread_num();
			//printf("thread id is %d\n",id);
			//float ave_time = 0.0;
			for(int i=0; i<jobs.size(); i++){
				vp = vertices((*jobs[i]->g));
				float executiontime = (*jobs[i]->g)[*(vp.second-2)].end_time - jobs[i]->arrival_time;
				if(executiontime > jobs[i]->deadline &&  plan[i]==1) {
					violation[i] += 1.0;
				}		
				ave_time[i] += executiontime;
			}	
			ave_cost += moneycost;
			//printf("average execution time of workflows is %f\n",ave_time/jobs.size());
			for(int i=0; i<jobs.size(); i++){
				delete jobs[i];
			}
			jobs.clear();
		}//monte		
	}//parallel
	vp = vertices(*dags[0]->g);
	for(; vp.first!=vp.second; vp.first++){
		free((*dags[0]->g)[*vp.first].netDown);
		free((*dags[0]->g)[*vp.first].netUp);
		free((*dags[0]->g)[*vp.first].probestTime);
		free((*dags[0]->g)[*vp.first].randomIO);
		free((*dags[0]->g)[*vp.first].seqIO);
		free((*dags[0]->g)[*vp.first].cumulativeTime);
		//free((*dags[0]->g)[*vp.first].randspot);
	}
	/*for(int i=0; i<1; i++) {//24
		violation += viol_private[i];
		ave_cost += cost_private[i];
	}*/
	int validjobs = 0;
        for(int i=0; i<dags.size(); i++){
                if(plan[i]==1){
                        validjobs ++;
			printf("deadline meeting rate of job %d is %.4f\n",i,1.0-violation[i]/randomsize);
			printf("average execution time of job %d is %.4f\n", i, ave_time[i]/randomsize);
		}
	}
        //violation /= (float)randomsize*validjobs;
        ave_cost /= (float)randomsize*validjobs;
	printf(" average cost of all jobs is %f\n",ave_cost);
	std::clock_t endtime = std::clock();
	std::clock_t timeelapsed = (float)(endtime - starttime) / (float)CLOCKS_PER_SEC;
	printf("time elapsed for SC11 algorithm is: %.4f\n", timeelapsed);
	float score = 0;
	for(int i=0; i<dags.size(); i++){
		if(plan[i] ==1)
		score += 1.0/std::pow(2.0,dags[i]->priority);
	}
	printf("obtained score of the ensemble is %f\n",score);
	return;
}
