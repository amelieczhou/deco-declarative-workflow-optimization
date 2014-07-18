//for the workflow ensemble problem, the objective is to maximize the score
//the constraints are budget and deadline
#include "astar.h"
#include <stack>

extern float budget;

float ensembleEvalcost(std::vector<DAG*> dags, configstack* config){

	//a solution is a (0,1) choice for each workflow
	float score = 0;
	std::pair<vertex_iter,vertex_iter> vp;
	for(int id=0; id<dags.size(); id++){
		if(config->configurations[id]==1){//this workflow will be executed
			score += 1.0/pow(2,dags[id]->priority);
		}
	}

	return 1.0/score;
}
bool ensembleEvalconstraint(std::vector<DAG*> dags, configstack* config){
	//a solution is a (0,1) choice for each workflow
	float totalcost = 0.0;
	float onehour = 3600.0; //seconds
	std::pair<vertex_iter,vertex_iter> vp;
	for(int id=0; id<dags.size(); id++){
		if(config->configurations[id]==1){//this workflow will be executed
			vp = vertices((*dags[id]->g));
			for(; vp.first!=vp.second; vp.first++){
				float taskcost = 0;
				int config = (*dags[id]->g)[*vp.first].assigned_type;
				for(int i=0; i<randomsize; i++)
					taskcost += (((*dags[id]->g)[*vp.first].probestTime[config*randomsize+i]+OnDemandLag)/onehour)*priceOnDemand[config];//ceil?
				totalcost += taskcost / randomsize;
			}
		}
	}

	if(totalcost>budget){
		return false;
	}else{
		/*
		float* exeTime = (float*)malloc(randomsize*sizeof(float));
		//run this function after each task is assigned with a instance type
		//in this way, calculate as distribution
		int cumulativesize = 4000;
		for(int id=0; id<dags.size();id++){
			vp = vertices((*dags[id]->g));
			for(; vp.first!=vp.second; vp.first++){
				(*dags[id]->g)[*vp.first].tag = false; //not yet calculate its cumulative time
				for(int i=0; i<cumulativesize; i++)
					(*dags[id]->g)[*vp.first].cumulativeTime[i] = 0;
			}
			vp = vertices((*dags[id]->g));
			float *tmpTime = (float*)malloc(cumulativesize*sizeof(float));
			for(; vp.first!=vp.second; vp.first++){
				for(int i=0; i<cumulativesize; i++)
					tmpTime[i] = 0;
				//find all its parents
				int config = (*dags[id]->g)[*vp.first].assigned_type;
				in_edge_iterator in_i, in_end;
				edge_descriptor e;
				boost::tie(in_i, in_end) = in_edges(*vp.first, (*dags[id]->g));
				if(in_i == in_end) {
					for(int i=0; i<cumulativesize; i++)
						(*dags[id]->g)[*vp.first].cumulativeTime[i] = (*dags[id]->g)[*vp.first].probestTime[config*randomsize+i]+OnDemandLag;
					(*dags[id]->g)[*vp.first].tag = true;
				}
				else{
					for (; in_i != in_end; ++in_i) {
						e = *in_i;
						Vertex src = source(e, (*dags[id]->g));					
						if(!(*dags[id]->g)[src].tag) { //this task hasn't been assigned cumulative time
							printf("????\n");
							break;
						}else{
							for(int i=0; i<randomsize; i++){
								tmpTime[i]= tmpTime[i]>(*dags[id]->g)[src].cumulativeTime[i]?tmpTime[i]:(*dags[id]->g)[src].cumulativeTime[i];
							}
						}
					}
				}
				for(int i=0; i<randomsize; i++){
					(*dags[id]->g)[*vp.first].cumulativeTime[i] = tmpTime[i] + (*dags[id]->g)[*vp.first].probestTime[config*randomsize+i] + OnDemandLag;
				}
				(*dags[id]->g)[*vp.first].tag = true;
			}
			//if(dag.type == montage)
				for(int i=0; i<randomsize; i++){
					exeTime[i] = (*dags[id]->g)[*(vp.second-1)].cumulativeTime[i];////////////////////////////
			}
			free(tmpTime);
			int count = 0;
			for(int i=0; i<randomsize; i++){
				if(exeTime[i]<=dags[id]->deadline)
					count ++;
			}
			float ratio = (float)count / (float)randomsize;
			//if satisfy users' requirement, select as the lower bound
			if(ratio < dags[id]->meet_dl)	{
				free(exeTime);
				return false;
			}
		}
		free(exeTime);
		return true;*/

		///////////////////////////monte carlo method/////////////////////////////////
		
		for(int id=0; id<dags.size();id++){
			int count = 0;
			for(int iter=0; iter<randomsize; iter++){
				float exeTime = 0;
				std::pair<vertex_iter,vertex_iter> vp;	
				//run this function after each task is assigned with a instance type
				//in this way, calculate as distribution
				vp = vertices((*dags[id]->g));
				for(; vp.first!=vp.second; vp.first++)
					(*dags[id]->g)[*vp.first].cumulativeTime[0] = 0;

				vp = vertices((*dags[id]->g));
				for(; vp.first!=vp.second; vp.first++){	
					float tmpTime = 0;
					//find all its parents
					int config = (*dags[id]->g)[*vp.first].assigned_type;
					in_edge_iterator in_i, in_end;
					edge_descriptor e;
					boost::tie(in_i, in_end) = in_edges(*vp.first, (*dags[id]->g));
					if(in_i == in_end) {
						(*dags[id]->g)[*vp.first].cumulativeTime[0] = (*dags[id]->g)[*vp.first].probestTime[config*randomsize+iter]+OnDemandLag;
						(*dags[id]->g)[*vp.first].tag = true;
					}
					else{
						for (; in_i != in_end; ++in_i) {
							e = *in_i;
							Vertex src = source(e, (*dags[id]->g));					
							if(!(*dags[id]->g)[src].tag) { //this task hasn't been assigned cumulative time
								printf("????\n");
								break;
							}else{
								tmpTime= tmpTime>(*dags[id]->g)[src].cumulativeTime[0]?tmpTime:(*dags[id]->g)[src].cumulativeTime[0];					
							}
						}
					}
					(*dags[id]->g)[*vp.first].cumulativeTime[0] = tmpTime + (*dags[id]->g)[*vp.first].probestTime[config*randomsize+iter] + OnDemandLag;		
					(*dags[id]->g)[*vp.first].tag = true;
				}
				exeTime = (*dags[id]->g)[*(vp.second-1)].cumulativeTime[0];
				if(exeTime <= dags[id]->deadline)
					count++;
			}
			float ratio = (float)count / (float)randomsize;
			if(ratio < dags[id]->meet_dl)
				return false;
		}		
		//if satisfy users' requirement, select as the lower bound		
		return true;
	}
}
void ensemble_initialFeasible(configstack* initialstate, std::vector<DAG*> dags,configstack* feasible){
	
	std::stack<configstack*> DAGstack;

	for(int i=0; i<dags.size(); i++)//initial solution
		initialstate->configurations.push_back(1);
	std::pair<vertex_iter,vertex_iter> vp;
	//profile section, find the cheapest initialstate according to the execution time of tasks on different instance type
	for(int i=0; i<dags.size(); i++){
		for(vp=vertices(*dags[i]->g); vp.first!=vp.second; vp.first++){
			(*dags[i]->g)[*vp.first].assigned_type = 3;
		}
	}
	initialstate->taskno = -1;
	float globalBest = 100000;
	DAGstack.push(initialstate);
	bool continuesearch = true;
	
	int numoftasks = dags.size();//actually the number of workflows   
	//find a lower bound first
	do{
		bool evalresult = ensembleEvalconstraint(dags,DAGstack.top());
		//if satisfy users' requirement, select as the lower bound
		if(evalresult){
			globalBest = ensembleEvalcost(dags,DAGstack.top());
			DAGstack.top()->fvalue = globalBest;
			continuesearch = false; //out of the while loop
			//printf("initial ratio in search prune: %f\n",ratio);
		}else{
			//Closeset.push_back(DAGstack.top());
			int nexttask = DAGstack.top()->taskno + 1;
			if(nexttask < numoftasks){
				configstack* state = new configstack();
				state->configurations=DAGstack.top()->configurations;
				state->taskno = nexttask;
				for(int t=0; t<2; t++){  //choose from 0 or 1, that is to execute or not
					if(!DAGstack.top()->childcolor[2-t]){ //type t has not been visited
						state->configurations[nexttask] = t;
						DAGstack.top()->childcolor[2-t] = true;
						DAGstack.push(state);
						break;                                          
					}
					if(t == 1){ //all types have been visited
						DAGstack.pop();
						//????
					}
				}
				
			}else{ //nexttask >= numoftasks                         
				DAGstack.pop();
					//?????
			}
		}
		if(DAGstack.empty()) {
			printf("cannot find one valid solution\n");
			exit(1);
		}
	}while(continuesearch);
	*feasible = *DAGstack.top();	
	while(!DAGstack.empty())//clear
		DAGstack.pop();
	

	return;
}
