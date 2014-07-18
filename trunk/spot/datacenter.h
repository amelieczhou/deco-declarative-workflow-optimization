#include <vector>
#include "astar.h"

#pragma once
const int numDCs = 5;

class DataCenter{
public:
	int DCid;
	int capacity;//maximum number of VMs
	int region;
	int zone;
	std::vector<int> connected;//ids of connected datacenters
	std::vector<DAG*> workloads;//workflows requests
	std::vector<DAG*> tempworkload;

	int numcurrentVM;//current number of vms for service
	float opCost;
	float commCost[numDCs];//the communication cost from the current datacenter to others

	DataCenter(){}
	DataCenter(int id,int cap,float op, int r, int z){DCid=id;capacity=cap;opCost=op;region=r; zone=z;}

	bool evalConstraint();
	float evalOpCost();
};

float followsunEvalcost(DataCenter** dcs,configstack* config);
void followsunEvalconstraint(DataCenter** dcs,configstack* config, bool* result);
void followsun_initialFeasible(DataCenter**, configstack*,configstack*);

int position(int a, int b);
void initialization(DataCenter** dcs);