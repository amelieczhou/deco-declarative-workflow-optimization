//ver version from Amelie
#include <stdio.h>
#include "astar.h"
#include "algorithm.h"
#include <ctime>
#include <cmath>
#include <utility>
#include <algorithm>
#include <fstream>
#include <sstream>
#include <stack>
#include <omp.h>
#define GPU
#define OMP
//#define CPU
//CUDA
#include "cudaheader.h"

using namespace boost;

void SearchPrune::OfflineAstar(){

	if(flag == followsun){
		printf("cannot use astar search for followsun application\n");
		exit(1);
	}

	std::vector<configstack*> Openset;
	std::vector<configstack*> Closeset;
	std::vector<configstack> solutions;	

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
	float *probestTime_device[VSIZE];
	int xx=0;
	std::pair<vertex_iter, vertex_iter> vp;
	for(int i=0; i<dags.size(); i++){
		vp = vertices((*dags[i]->g));

		int quantile = dags[i]->meet_dl * randomsize;
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
					//(*dags[i]->g)[*vp.first].probestTime[t*randomsize+j] /= 60.0;
				}
				//calculate the estimate time as the expected value of the proestTime
				std::sort((*dags[i]->g)[*vp.first].probestTime+t*randomsize,(*dags[i]->g)[*vp.first].probestTime+(t+1)*randomsize-1);
				(*dags[i]->g)[*vp.first].estTime[t] = (*dags[i]->g)[*vp.first].probestTime[t*randomsize+quantile];
				printf("task: %d, type: %d, time: %f\n",*vp.first,t,(*dags[i]->g)[*vp.first].estTime[t]);
			}
			(*dags[i]->g)[*vp.first].assigned_type = 0;//initially all assigned to small
		if (i==0)
				probestTime_device[xx++]=(*dags[i]->g)[*vp.first].probestTime;
		}
	}


	configstack* initialstate = new configstack();
	configstack* feasible = new configstack();
	//float* exeTime = (float*)malloc(randomsize*sizeof(float));
	//first step: search for a feasible solution and use it as lower bound
	initialFeasible(initialstate,feasible);
	solutions.push_back(*feasible);
	Openset.push_back(initialstate);
	//Openset.push_back(feasible);
	for(int i=0; i<feasible->configurations.size(); i++)
		printf("%d, ",feasible->configurations[i]);
	float globalBestCost = feasible->fvalue;
	int numoftasks = feasible->configurations.size();
//prepare data for CUDA Astar
        int *v_h, *e_h;
        float *estTime_h;
        v_h=(int *)malloc(sizeof(int)*VSIZE);
        e_h=(int *)malloc(2*sizeof(int)*ESIZE);
        estTime_h=(float *)malloc(types * sizeof(float)*VSIZE);

        typedef property_map<Graph, vertex_index_t>::type IndexMap;
        IndexMap index = get(vertex_index, *dags[0]->g);
        vp = vertices((*dags[0]->g));

        int i=0;
        for (;vp.first != vp.second; ++vp.first)
        {
          v_h[i++]=index[*vp.first];
        }
        std::cout << std::endl;
        graph_traits<Graph>::edge_iterator ei, ei_end;
        i=0;
        for (boost::tie(ei, ei_end) = boost::edges(*dags[0]->g); ei != ei_end; ++ei)
        {
                e_h[i++]=index[boost::source(*ei, *dags[0]->g)];
                e_h[i++]=index[boost::target(*ei, *dags[0]->g)];
        }
        std::cout << std::endl;

        vp = vertices((*dags[0]->g));
        i=0;
        for(vp=vertices((*dags[0]->g)); vp.first!=vp.second; vp.first++){
                for (int j=0;j<types;j++)
                        estTime_h[i++]  = (*dags[0]->g)[*vp.first].estTime[j];
        }

        //call CUDA Astar
        CONFIGSTACK init_cuda;
        init_cuda.taskno=initialstate->taskno;
        init_cuda.fvalue=initialstate->fvalue;
        for (int i=0;i<TYPES;++i)
                init_cuda.childcolor[i]=initialstate->childcolor[i];
        for (int i=0;i<VSIZE;++i)
                init_cuda.configurations[i]=initialstate->configurations[i];
                //Xuntao CUDA
        int flag=0;
                for (int i=0;i<numoftasks;++i)
                {
                        printf ("%d ", feasible->configurations[i]);
                }
                //printf(constraint ? "true" : "false");
                printf ("%f \n", feasible->fvalue);
        std::cout<<"A* Search GPU started"<<std::endl;
        clock_t t2,t3;
	t2=clock();
        Astar_CUDA (randomsize, v_h, e_h, (*dags[0]->g).m_vertices.size(), (*dags[0]->g).m_edges.size(), flag,  types,
                probestTime_device, OnDemandLag, priceOnDemand,  globalBestCost, dags[0]->deadline, dags[0]->meet_dl, init_cuda);
        t3=clock();
	
	int searchcount = 0;
	double t0,t1;
	t0=omp_get_wtime();//clock();
#ifdef OMP
	std::cout<<"A* Search CPU-OMP started"<<std::endl;
	int nthreads=6;
	int limit=0;
	configstack node[6];
	std::vector<configstack*> localOpenset[6];
while(!(Openset.empty() || searchcount > 10000)){
		//no need to sort, only find the smallest one is enough
		limit=0;
		for (int i=0;i<nthreads;++i)
		{
			if (i<Openset.size())
			{
				std::nth_element(Openset.begin(),Openset.end(),Openset.end(),configsortfunction);//sort from large to small,in order to reduce the erase complexity
				node[i]=*Openset.back();
				Openset.erase(Openset.end()-1);
				limit++;
			}
		}
		#pragma omp parallel default(shared) shared (Openset, Closeset, solutions, node, limit) num_threads(nthreads)
		{
			int tid=omp_get_thread_num();
			if (tid<limit)
			{
				DAG* ldag = new DAG(*dags[0]);
				configstack* headnode = &(node[tid]);//back has the smallest fvalue

		//check if satisfy deadline 
		if(flag==deadlineapp)
			for(int i=0; i<numoftasks; i++)
				(*ldag->g)[i].assigned_type = headnode->configurations[i]; 
		bool constraint = solutionEvalconstraint(headnode, ldag);		
        //if satisfy users' requirement, select as the lower bound
		if(constraint){			 
			if(headnode->fvalue < globalBestCost){
				globalBestCost = headnode->fvalue;				
				#pragma omp critical(solution)
				{
					solutions.push_back(*headnode);
				}
				//remove headnode from openset and add it to closedset
				//printf("search prune find a solution with ratio: %f\n",ratio);
			}
        	}
		#pragma omp critical(closeset)
		{
			Closeset.push_back(headnode);
		}
		//for each neighboring node, how to include transformation!
		int nexttask = headnode->taskno + 1;
		if(nexttask < numoftasks){	
			int bound = types;
			int start = headnode->configurations[nexttask]+1;
			if(flag==ensemble) 	{
				bound = 2;
				start = 0;//only 0 or 1
			}
				
			for(int t=start; t<bound; t++){
				configstack* state = new configstack();
				state->taskno = nexttask;
				state->configurations = headnode->configurations;
				state->configurations[nexttask] = t;
				//is it a feasible solution?
				//dag.g[nexttask].assigned_type = t;		
				if(flag==deadlineapp){
					for(int ii=0; ii<numoftasks; ii++)
					{
						(*ldag->g)[ii].assigned_type = state->configurations[ii];
					
					}
				}
				float currentcost = solutionEvalcost(state, ldag);
				bool closeset_in;
				//#pragma omp critical(closeset)
				{
					closeset_in=std::find(Closeset.begin(),Closeset.end(),state) != Closeset.end();
				}
				if(currentcost >= globalBestCost || closeset_in){//std::binary_search(Closeset.begin(),Closeset.end(),state)){
					//just ignore this configuration
				}else{
					std::vector<configstack*>::iterator iter;
					//#pragma omp critical(openset)
					{
						iter = std::find(Openset.begin(),Openset.end(),state);
					}
					if(iter == Openset.end()){//state is not in Openset
					//bool found = std::binary_search(Openset.begin(),Openset.end(),state)
					//if(!found){
						state->fvalue = currentcost;
						//#pragma omp critical(openset)
						{
						//	Openset.push_back(state);
							localOpenset[tid].push_back(state);
						}
					}else{
						//state is already in Openset
						printf("when would this happen?\n");
					}
				}				
			}//for t
			
		}
		}//limit	
		}//parallel
		//push openset here
			
		for (int i=0;i<nthreads;++i)
		{
			while(localOpenset[i].size()>0)
			{
				Openset.push_back(localOpenset[i].back());
				localOpenset[i].pop_back();
			}
		}
		searchcount+=limit;
	}//while
#endif
#ifdef CPU
printf ("single core\n");
while(!(Openset.empty() || searchcount > 10000)){
		//no need to sort, only find the smallest one is enough
		std::nth_element(Openset.begin(),Openset.end(),Openset.end(),configsortfunction);//sort from large to small,in order to reduce the erase complexity
		configstack* headnode = Openset.back();//back has the smallest fvalue

		//check if satisfy deadline 
		if(flag==deadlineapp)
			for(int i=0; i<numoftasks; i++)
				(*dags[0]->g)[i].assigned_type = headnode->configurations[i]; 
		bool constraint = solutionEvalconstraint_o(headnode);		
        //if satisfy users' requirement, select as the lower bound
		if(constraint){			 
			if(headnode->fvalue < globalBestCost){
				globalBestCost = headnode->fvalue;				
				solutions.push_back(*headnode);
				//remove headnode from openset and add it to closedset
				//printf("search prune find a solution with ratio: %f\n",ratio);
			}
        }
		Openset.erase(Openset.end()-1);
		Closeset.push_back(headnode);

		//for each neighboring node, how to include transformation!
		int nexttask = headnode->taskno + 1;
		//if(nexttask < numoftasks){	
			int bound = types;
			int start = headnode->configurations[nexttask]+1;
			if(flag==ensemble) 	{
				bound = 2;
				start = 0;//only 0 or 1
			}
				
			for(int t=start; t<bound; t++){
				configstack* state = new configstack();
				state->taskno = nexttask;
				state->configurations = headnode->configurations;
				state->configurations[nexttask] = t;
				//is it a feasible solution?
				//dag.g[nexttask].assigned_type = t;		
				if(flag==deadlineapp){
					for(int ii=0; ii<numoftasks; ii++)
						(*dags[0]->g)[ii].assigned_type = state->configurations[ii];
				}
				float currentcost = solutionEvalcost_o(state);
				if(currentcost >= globalBestCost || std::find(Closeset.begin(),Closeset.end(),state) != Closeset.end()){//std::binary_search(Closeset.begin(),Closeset.end(),state)){
					//just ignore this configuration
				}else{
					std::vector<configstack*>::iterator iter = std::find(Openset.begin(),Openset.end(),state);
					if(iter == Openset.end()){//state is not in Openset
					//bool found = std::binary_search(Openset.begin(),Openset.end(),state)
					//if(!found){
						state->fvalue = currentcost;
						Openset.push_back(state);
					}else{
						//state is already in Openset
						printf("when would this happen?\n");
					}
				}				
			}//for t
			
		searchcount ++;
	}
#endif
	t1=omp_get_wtime();//clock();
	printf ("CPU time: %f", (double)(t1-t0));
	printf ("GPU time: %f", (double)(t3-t2)/CLOCKS_PER_SEC);
	
	foundsolution = solutions.back();
	//calculate the cumulative time distribution of the dag
	//float* cumulative=(float*)malloc(randomsize*sizeof(float));
	//solutionEvalconstraint(&solutions.back(),cumulative);
	//dags[0].cumulativetime = cumulative;

	//free(cumulative);
	free(random_sequential_io);
	free(random_random_io);
	free(random_network_up);
	free(random_network_down);
}
//for single core
float SearchPrune::solutionEvalcost_o(configstack* config)
{
	if(flag==deadlineapp)//deadline use case
		return deadlineEvalcost(dags[0]);
	else if(flag==ensemble){//ensemble use case
		std::vector<DAG*> pdags;
		for(int i=0; i<dags.size(); i++)
			pdags.push_back(dags[i]);
		return ensembleEvalcost(pdags,config);
	}
	//else if(flag==followsun)//follow the sun use case
	//	return followsunEvalcost();
	else {
		printf("solutionEval error\n");
		exit(1);
	}
}
bool SearchPrune::solutionEvalconstraint_o(configstack* config)
{
	if(flag==deadlineapp)//deadline use case
		return deadlineEvalconstraint(dags[0]);
	else if(flag==ensemble){//ensemble use case
		std::vector<DAG*> pdags;
		for(int i=0; i<dags.size(); i++)
			pdags.push_back(dags[i]);
		return ensembleEvalconstraint(pdags,config);
	}
	//else if(flag==followsun)//follow the sun use case
	//	return followsunEvalconstraint();
	else {
		printf("solutionEval error\n");
		exit(1);
	}
}
//for single core, above
void SearchPrune::Simulate(){
	//execute according to the foundsolution
	if(flag == deadlineapp){
		//deadline application simulation
		std::pair<vertex_iter,vertex_iter> vp;
		for(vp=vertices(*dags[0]->g); vp.first!=vp.second; vp.first++)
			(*dags[0]->g)[*vp.first].assigned_type = foundsolution.configurations[*vp.first];
		Autoscaling* autoptimizer = new Autoscaling();
		autoptimizer->dags.push_back(dags[0]);
		autoptimizer->Simulate(false);

	}else if(flag == ensemble){
		EnsembleSPSS* ensembleopt = new EnsembleSPSS();		
		ensembleopt->plan = (int*)malloc(dags.size()*sizeof(int));
		for(int i=0; i<dags.size(); i++){
			ensembleopt->plan[i] = foundsolution.configurations[i];
			ensembleopt->dags.push_back( dags[i]);
		}
		ensembleopt->Simulate(false);
	
	}else if(flag == followsun){
	
	}else{
		printf("what's the application type for simulation?\n");
	}
}
bool configsortfunction(configstack* a, configstack* b)
{
	return (a->fvalue > b->fvalue); //sort from large to small
}
bool ompconfigsortfunction(configstack a, configstack b)
{
	return (a.fvalue > b.fvalue); //sort from large to small
}

void conv(float* array1, float* array2, float* result, int length1, int length2){
	int resultlength = length1 + length2 - 1;
	for(int index=0; index < resultlength; index++){
		float tmp = 0.0;
		for(int k=0; k<length1; k++){
			if(index >= k && (index-k)<length2)
				tmp += array1[k]*array2[index-k];
		}
		result[index]=tmp;
	}
}
void calmaxdistr(float* array1, float* array2, float* result, int length1, int length2){
	int length = length1>length2?length1:length2;
	for(int i=0; i<length; i++)
		result[i] = 0.0;
	for(int i=0; i<length1; i++){
		for(int j=0; j<length2; j++){
			if(array1[i]!=0)
				printf("");
			if(i>=j){
				result[i] += array1[i]*array2[j]; 
			}else{//i<j
				result[j] += array1[i]*array2[j];
			}
		}
	}
}

float SearchPrune::solutionEvalcost(configstack* config, DAG *ldag)
{
	if(flag==deadlineapp)//deadline use case
		return deadlineEvalcost(ldag);
	else if(flag==ensemble){//ensemble use case
		std::vector<DAG*> pdags;
		for(int i=0; i<dags.size(); i++)
			pdags.push_back(dags[i]);
		return ensembleEvalcost(pdags,config);
	}
	//else if(flag==followsun)//follow the sun use case
	//	return followsunEvalcost();
	else {
		printf("solutionEval error\n");
		exit(1);
	}
}
bool SearchPrune::solutionEvalconstraint(configstack* config, DAG *ldag)
{
	if(flag==deadlineapp)//deadline use case
		return deadlineEvalconstraint(ldag);
	else if(flag==ensemble){//ensemble use case
		std::vector<DAG*> pdags;
		for(int i=0; i<dags.size(); i++)
			pdags.push_back(dags[i]);
		return ensembleEvalconstraint(pdags,config);
	}
	//else if(flag==followsun)//follow the sun use case
	//	return followsunEvalconstraint();
	else {
		printf("solutionEval error\n");
		exit(1);
	}
}
void SearchPrune::initialFeasible(configstack* config,configstack* feasible){
	if(flag==deadlineapp)
		return deadline_initialFeasible(config, dags[0],feasible);//only one dag in dags
	else if(flag==ensemble){
		std::vector<DAG*> pdags;
		for(int i=0; i<dags.size(); i++)
			pdags.push_back(dags[i]);
		return ensemble_initialFeasible(config,pdags,feasible);
	}
	//else if(flag==followsun)
	//	return followsun_initialFeasible(config,feasible);
	else{
		printf("initialFeasible error\n");
		exit(1);
	}
}
