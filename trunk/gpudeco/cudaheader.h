#pragma once
#define SETSIZE 100000
#define ensemble 1
#define deadlineapp 0
//#define types 4

#define TYPES 4
#define VSIZE 100 //num of tasks
#define ESIZE 233 

typedef struct {
	int taskno;
	float fvalue;
	bool childcolor[TYPES];
	int configurations[VSIZE];//size = # of tasks
}CONFIGSTACK;

typedef struct {
	int *vertex;
	int *type;
	int *edge;
	float extTime[TYPES];
}DAG_CUDA;

extern "C" void Astar_CUDA (int randomsize, int *vertex, int *edge, int v_size, int e_size,int flag,int types, 
				 float *probestTime[VSIZE],float OnDemandLag, const float *priceOnDemand, float globalBestCost, 
				 float deadline,float meet_dl,CONFIGSTACK feasible);
