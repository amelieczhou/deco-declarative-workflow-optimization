#include "instance.h"

#pragma once

enum App_type{
	deadlineapp=0,
	ensemble=1,
	followsun=2
};

class configstack{
	//this struct is used to store the searched configuration list in the stack, and maintain their parent and child info
public:
	std::vector<int> configurations;
	//configstack* parent; //parent on the search path
	//configstack* child; //child on the search path
	int taskno; //the task modified to form this configuration 
	//int nextsearchedtype; //the type searched for the next task
	bool* childcolor; //mark which child has been searched
	float fvalue; //used for astar to sort the openset

	configstack(){taskno=0; fvalue=0; childcolor=new bool[types]; childcolor[0]=true; for(int i=1; i<types; i++) childcolor[i]=false;}
	configstack(const configstack& a) {configurations=a.configurations; taskno=a.taskno; fvalue=a.fvalue; childcolor=new bool[types]; for(int i=0; i<types; i++) childcolor[i]=a.childcolor[i];}
	inline bool operator==(configstack* a) {return this->configurations == a->configurations;}
};

class SearchPrune{
public:
	//DAG dag;
	std::vector<DAG*> dags;//for online simulation
	App_type flag;
	configstack foundsolution;
	
	//use the aStar to search
	//use the expected cost to prune

	void initialFeasible(configstack*,configstack*);//find initial feasible solution
	float solutionEvalcost(configstack*, DAG*);//return the evaluated cost and constrain satisfication
	float solutionEvalcost_o(configstack*);//return the evaluated cost and constrain satisfication
	bool solutionEvalconstraint(configstack*, DAG*);
	bool solutionEvalconstraint_o(configstack*);
	void OfflineAstar();
	void Simulate();
};


bool configsortfunction(configstack* a, configstack* b);
bool ompconfigsortfunction(configstack a, configstack b);
//float estimateCost(const DAG& dag, int start, bool estimate); //estimate or calculate the total cost of dag, starting from the start task
//void estimateTime(DAG& dag, float* estTime); //estimate the total execution time of dag, used in offline search
void conv(float* array1, float* array2, float* result, int length1, int length2);
void calmaxdistr(float* array1, float* array2, float* result, int length1, int length2);
float deadlineEvalcost(DAG* dag);
bool deadlineEvalconstraint(DAG*);
float ensembleEvalcost(std::vector<DAG*> dags,configstack* config);
bool ensembleEvalconstraint(std::vector<DAG*> ,configstack* );
void deadline_initialFeasible(configstack*, DAG*,configstack* );
void ensemble_initialFeasible(configstack*, std::vector<DAG*>,configstack*);
