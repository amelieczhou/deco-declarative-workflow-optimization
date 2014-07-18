// PrologInterpreter.cpp : Defines the entry point for the console application.
//
#include <stdio.h>
#include <stdlib.h>
#include <vector>
#include <iostream>
#include <ctime>
#include <boost/graph/topological_sort.hpp>
#include "algorithm.h"

	//Term t(1.0);
	
	//std::vector<RuleList> Rules;
	//the facts
	//std::vector<float> args;
	//float tasks[]={-1,-2,1,2,3};
	//for(int i=0; i<5; i++){
	//	args.clear();
	//	args.push_back(tasks[i]);
	//	Constant* fact = new Constant("task",args);
	//	RuleList rule; rule.push_back(fact);
	//	Rules.push_back(rule);
	//}
	//float edges[]={1,2,1,3,-1,1,2,-2,3,-2};
	//for(int i=0; i<5; i++){
	//	args.clear();
	//	args.push_back(edges[i]);
	//	args.push_back(edges[2*i+1]);
	//	Constant* fact = new Constant("edge",args);
	//	RuleList rule; rule.push_back(fact);
	//	Rules.push_back(rule);
	//}
	//float exetimes[]={1,1,50.0,1,2,90.0,2,1,40.0,2,2,70.0,3,1,30.0,3,2,55.0};
	//for(int i=0; i<8; i++){
	//	args.clear();
	//	args.push_back(exetimes[i]);
	//	args.push_back(exetimes[3*i+1]);
	//	args.push_back(exetimes[3*i+2]);
	//	Constant* fact = new Constant("exetime",args);
	//	RuleList rule; rule.push_back(fact);
	//	Rules.push_back(rule);
	//}
	//float price[]={1,1.0,2,0.5};
	//for(int i=0; i<2; i++){
	//	args.clear();
	//	args.push_back(price[i]);
	//	args.push_back(price[i*2+1]);
	//	Constant* fact = new Constant("price",args);
	//	RuleList rule; rule.push_back(fact);
	//	Rules.push_back(rule);
	//}
	////store rules as lists
	//std::vector<char*> pars(4);
	//pars[0]="X";pars[1]="Y";pars[2]="Y";pars[3]="C";
	//Variable* rule1head = new Variable("path",pars);
	//RuleList rule1;
	//rule1.push_back(rule1head);
	//pars.clear();
	//pars.push_back("X"); pars.push_back("Y");
	//Variable* rule11 = new Variable("edge",pars);
	//rule1.push_back(rule11);
	//pars.clear();pars.push_back("X");pars.push_back("Vmid");pars.push_back("T");
	//Variable* rule12 = new Variable("exetime",pars);
	//rule1.push_back(rule12);
	//pars.clear();pars.push_back("C");pars.push_back("T");
	//Variable* rule13 = new Variable("is",pars);
	//rule1.push_back(rule13);
	////rule1.push_back(NULL);
	//Rules.push_back(rule1);
	//
	//RuleList rule2;
	//pars.clear(); pars.push_back("X");pars.push_back("Y");pars.push_back("Z");pars.push_back("C");
	//Variable* rule2head = new Variable("path",pars);
	//rule2.push_back(rule2head);
	//pars.clear(); pars.push_back("X");pars.push_back("Z");
	//Variable* rule21 = new Variable("edge",pars);
	//rule2.push_back(rule21);
	//pars.clear();pars.push_back("Z");pars.push_back("Y");
	//Variable* rule22 = new Variable("\==",pars);
	//rule2.push_back(rule22);
	//pars.clear();pars.push_back("Z");pars.push_back("Y");pars.push_back("Z2");pars.push_back("C1");
	//Variable* rule23 = new Variable("path",pars);
	//rule2.push_back(rule23);
	//pars.clear();pars.push_back("X");pars.push_back("Vmid");pars.push_back("T");
	//Variable* rule24 = new Variable("exetime",pars);
	//rule2.push_back(rule24);
	//pars.clear(); pars.push_back("C");pars.push_back("+");pars.push_back("T");pars.push_back("C1");
	//Variable* rule25 = new Variable("is",pars);
	//rule2.push_back(rule25);
	////rule2.push_back(NULL);
	//Rules.push_back(rule2);

	//RuleList rule3;
	//pars.clear();pars.push_back("Tid");pars.push_back("Vmid");pars.push_back("C");
	//Variable* rule3head = new Variable("cost",pars);
	//rule3.push_back(rule3head);
	//pars.clear();pars.push_back("Vmid");pars.push_back("Up");
	//Variable* rule31 = new Variable("price",pars);
	//rule3.push_back(rule31);
	//pars.clear();pars.push_back("Tid");pars.push_back("Vmid");pars.push_back("T");
	//Variable* rule32 = new Variable("exetime",pars);
	//rule3.push_back(rule32);
	//pars.clear();pars.push_back("C");pars.push_back("*");pars.push_back("T");pars.push_back("/");pars.push_back("Up");pars.push_back("60");
	//Variable* rule33 = new Variable("is",pars);
	//rule3.push_back(rule33);
	////rule3.push_back(NULL);
	//Rules.push_back(rule3);

	//RuleList rule4;
	//pars.clear(); pars.push_back("Cost");pars.push_back("Bag");
	//Variable* rule4head = new Variable("totalcost",pars);
	//rule4.push_back(rule4head);
	//pars.clear();

	//pars.clear();pars.push_back("X");pars.push_back("Y");
	//Variable* Query = new Variable("totalcost",pars);

	//pars.clear();pars.push_back("X");pars.push_back("Y");pars.push_back("C");
	//Variable* q=new Variable("cost",pars);
	//RuleList query; query.push_back(q);
	//PrologEval(Rules,query);
	//


// Deadline_assign.cpp : Defines the entry point for the console application.
// Input: binary tree type DAG
//compare to previous version: add network and I/O operation time to all algorithms
//change estTime in order to make the instance configuration adapt to performance variance
//after discussion on 6th, Oct add runtime deadline refinement



using namespace boost;
//I/O performance distribution, seek/sec
float OnDemandLag = 1;//seconds//10;//0.5;
bool NoSpotVM = true;
float Times[4][types] = {{120,65,38,24},{90,50,30,20},{60,35,23,17},{30,20,15,13}};
float lambda;
int num_jobs;
int randomsize;
float budget;


//argvs: workflowtype,deadline,deadline meet rate,lambda,number of jobs,randomsize,autoscaling,use case,budget
int main(int argc, char* argv[])
{	
	
	srand( (unsigned)time( NULL ) );
	float deadline = atof(argv[2]);
	float meet_dl = atof(argv[3]); //the deadline meet rate submitted by the user
	lambda = atof(argv[4]);
	num_jobs = atoi(argv[5]);
	randomsize = atoi(argv[6]);
	budget = atoi(argv[9]);

	////I/O speed, seeks/sec
	//float ioseq[4], iorand[4];
	////network speed, sec/8M
	//float net_up[4], net_down[4];


	if(strcmp(argv[8],"deadline")==0){
		//input workflows
		//Graph dag;
		DAG* dag = new DAG(deadline,meet_dl);
		dag->g = new Graph();
		if(strcmp(argv[1],"montage") == 0)	{
			constructDAG(dag,montage,argv[7],0);		
		}else if(strcmp(argv[1],"ligo") == 0){
			constructDAG(dag,ligo,argv[7],0);
		}else if(strcmp(argv[1],"epigenome") == 0){
			constructDAG(dag,epigenome,argv[7],0);
		}else if(strcmp(argv[1],"test") == 0){
			//a pipeline with two tasks
			constructDAG(dag,pipeline,argv[7],0);
		}else if(strcmp(argv[1],"montage100") == 0){
			constructDAG(dag,montage100,argv[7],0);
		}else if(strcmp(argv[1],"montage1000") == 0){
			constructDAG(dag,montage1000,argv[7],0);
		}else if(strcmp(argv[1],"ligo100") == 0){
			constructDAG(dag,ligo100,argv[7],0);
		}else if(strcmp(argv[1],"ligo1000") == 0){
			constructDAG(dag,ligo1000,argv[7],0);
		}else if(strcmp(argv[1],"epi100") == 0){
			constructDAG(dag,epi100,argv[7],0);
		}else if(strcmp(argv[1],"epi1000") == 0){
			constructDAG(dag,epi1000,argv[7],0);
		}else {
			printf("Please choose a workflow type from: montage, ligo or epigenome.\n");
			exit(1);
		}
		if(strcmp(argv[7],"Autoscaling")==0){
			Autoscaling* autoOptimizer = new Autoscaling();
			autoOptimizer->dags.push_back(dag);
			autoOptimizer->Simulate(true);
		}else {
			//offline optimization
			SearchPrune* optimizer = new SearchPrune();
			optimizer->flag = deadlineapp;
			optimizer->dags.push_back(dag);
			std::clock_t starttime = std::clock();
			optimizer->OfflineAstar();
			std::clock_t endtime = std::clock();
			float timeelapsed = (float)(endtime - starttime) / (float)CLOCKS_PER_SEC;
			printf("time for Astar search is: %.4f\n", timeelapsed);
			optimizer->Simulate();
		}
	}else if(strcmp(argv[8],"ensemble")==0){		
		std::vector<DAG*> jobs;
		if(strcmp(argv[10],"constant")==0){
			//constant, all have the same number of tasks
			if(strcmp(argv[1],"montage") == 0||strcmp(argv[1],"ligo") == 0||strcmp(argv[1],"epigenome") == 0){
				//construct 30 dags, 10 of one of the three types each
				for(int i=0; i<10; i++){
					DAG* dag = new DAG(deadline,meet_dl);
					dag->g = new Graph(); 
					constructDAG(dag,montage,argv[7],3);
					jobs.push_back(dag);
					DAG* dag1 = new DAG(deadline,meet_dl);
					dag1->g = new Graph(); 
					constructDAG(dag1,ligo,argv[7],3);
					jobs.push_back(dag1);
					DAG* dag2 = new DAG(deadline,meet_dl);
					dag2->g = new Graph(); 
					constructDAG(dag2,epigenome,argv[7],3);
					jobs.push_back(dag2);
				}
			}else if(strcmp(argv[1],"montage100") == 0||strcmp(argv[1],"ligo100") == 0||strcmp(argv[1],"epi100") == 0){
				for(int i=0; i<10; i++){
					DAG* dag = new DAG(deadline,meet_dl);
					dag->g = new Graph(); 
					constructDAG(dag,montage100,argv[7],2);
					jobs.push_back(dag);
					DAG* dag1 = new DAG(deadline,meet_dl);
					dag1->g = new Graph(); 
					constructDAG(dag1,ligo100,argv[7],2);
					jobs.push_back(dag1);
					DAG* dag2 = new DAG(deadline,meet_dl);
					dag2->g = new Graph(); 
					constructDAG(dag2,epi100,argv[7],2);
					jobs.push_back(dag2);
				}
			}else if(strcmp(argv[1],"montage1000") == 0||strcmp(argv[1],"ligo1000") == 0||strcmp(argv[1],"epi1000") == 0){
				for(int i=0; i<10; i++){
					DAG* dag = new DAG(deadline,meet_dl);
					dag->g = new Graph(); 
					constructDAG(dag,montage1000,argv[7],1);
					jobs.push_back(dag);
					DAG* dag1 = new DAG(deadline,meet_dl);
					dag1->g = new Graph(); 
					constructDAG(dag1,ligo1000,argv[7],1);
					jobs.push_back(dag1);
					DAG* dag2 = new DAG(deadline,meet_dl);
					dag2->g = new Graph(); 
					constructDAG(dag2,epi1000,argv[7],1);
					jobs.push_back(dag2);
				}
			}
		}else if(strcmp(argv[10],"uniformsorted")==0){
			//uniform: 3 sizes each 12 workflows, 4 for each workflow type
			//sorted, larger size with larger priority
			for(int i=0; i<4; i++){
				//first, size=20				
				DAG* dag = new DAG(deadline,meet_dl);
				dag->g = new Graph(); 
				constructDAG(dag,montage1000,argv[7],1);
				jobs.push_back(dag);
				DAG* dag1 = new DAG(deadline,meet_dl);
				dag1->g = new Graph(); 
				constructDAG(dag1,ligo1000,argv[7],1);
				jobs.push_back(dag1);
				DAG* dag2 = new DAG(deadline,meet_dl);
				dag2->g = new Graph(); 
				constructDAG(dag2,epi1000,argv[7],1);
				jobs.push_back(dag2);
				DAG* dag3 = new DAG(deadline,meet_dl);
				dag3->g = new Graph(); 
				constructDAG(dag3,montage100,argv[7],2);
				jobs.push_back(dag3);
				DAG* dag4 = new DAG(deadline,meet_dl);
				dag4->g = new Graph(); 
				constructDAG(dag4,ligo100,argv[7],2);
				jobs.push_back(dag4);
				DAG* dag5 = new DAG(deadline,meet_dl);
				dag5->g = new Graph(); 
				constructDAG(dag5,epi100,argv[7],2);
				jobs.push_back(dag5);
				DAG* dag6 = new DAG(deadline,meet_dl);
				dag6->g = new Graph(); 
				constructDAG(dag6,montage,argv[7],3);
				jobs.push_back(dag6);
				DAG* dag7 = new DAG(deadline,meet_dl);
				dag7->g = new Graph(); 
				constructDAG(dag7,ligo,argv[7],3);
				jobs.push_back(dag7);
				DAG* dag8 = new DAG(deadline,meet_dl);
				dag8->g = new Graph(); 
				constructDAG(dag8,epigenome,argv[7],3);
				jobs.push_back(dag8);
			}
		}else if(strcmp(argv[10],"uniformunsorted")==0){
			for(int i=0; i<4; i++){
				//first, size=20				
				DAG* dag = new DAG(deadline,meet_dl);
				dag->g = new Graph(); 
				constructDAG(dag,montage1000,argv[7],2);
				jobs.push_back(dag);
				DAG* dag1 = new DAG(deadline,meet_dl);
				dag1->g = new Graph(); 
				constructDAG(dag1,ligo1000,argv[7],1);
				jobs.push_back(dag1);
				DAG* dag2 = new DAG(deadline,meet_dl);
				dag2->g = new Graph(); 
				constructDAG(dag2,epi1000,argv[7],3);
				jobs.push_back(dag2);
				DAG* dag3 = new DAG(deadline,meet_dl);
				dag3->g = new Graph(); 
				constructDAG(dag3,montage100,argv[7],1);
				jobs.push_back(dag3);
				DAG* dag4 = new DAG(deadline,meet_dl);
				dag4->g = new Graph(); 
				constructDAG(dag4,ligo100,argv[7],2);
				jobs.push_back(dag4);
				DAG* dag5 = new DAG(deadline,meet_dl);
				dag5->g = new Graph(); 
				constructDAG(dag5,epi100,argv[7],3);
				jobs.push_back(dag5);
				DAG* dag6 = new DAG(deadline,meet_dl);
				dag6->g = new Graph(); 
				constructDAG(dag6,montage,argv[7],3);
				jobs.push_back(dag6);
				DAG* dag7 = new DAG(deadline,meet_dl);
				dag7->g = new Graph(); 
				constructDAG(dag7,ligo,argv[7],2);
				jobs.push_back(dag7);
				DAG* dag8 = new DAG(deadline,meet_dl);
				dag8->g = new Graph(); 
				constructDAG(dag8,epigenome,argv[7],1);
				jobs.push_back(dag8);
			}
		}else if(strcmp(argv[10],"paretosorted")==0){
			//pareto ensembles: small number of large workflow and large number of small workflows
			//21 small, 9 medium, 3 large
			for(int i=0; i<7; i++){//size=20
				DAG* dag6 = new DAG(deadline,meet_dl);
				dag6->g = new Graph(); 
				constructDAG(dag6,montage,argv[7],3);
				jobs.push_back(dag6);
				DAG* dag7 = new DAG(deadline,meet_dl);
				dag7->g = new Graph(); 
				constructDAG(dag7,ligo,argv[7],3);
				jobs.push_back(dag7);
				DAG* dag8 = new DAG(deadline,meet_dl);
				dag8->g = new Graph(); 
				constructDAG(dag8,epigenome,argv[7],3);
				jobs.push_back(dag8);
			}
			for(int i=0; i<3; i++){//size = 100
				DAG* dag3 = new DAG(deadline,meet_dl);
				dag3->g = new Graph(); 
				constructDAG(dag3,montage100,argv[7],2);
				jobs.push_back(dag3);
				DAG* dag4 = new DAG(deadline,meet_dl);
				dag4->g = new Graph(); 
				constructDAG(dag4,ligo100,argv[7],2);
				jobs.push_back(dag4);
				DAG* dag5 = new DAG(deadline,meet_dl);
				dag5->g = new Graph(); 
				constructDAG(dag5,epi100,argv[7],2);
				jobs.push_back(dag5);
			}
			//size = 1000
			DAG* dag = new DAG(deadline,meet_dl);
			dag->g = new Graph(); 
			constructDAG(dag,montage1000,argv[7],1);
			jobs.push_back(dag);
			DAG* dag1 = new DAG(deadline,meet_dl);
			dag1->g = new Graph(); 
			constructDAG(dag1,ligo1000,argv[7],1);
			jobs.push_back(dag1);
			DAG* dag2 = new DAG(deadline,meet_dl);
			dag2->g = new Graph(); 
			constructDAG(dag2,epi1000,argv[7],1);
			jobs.push_back(dag2);
		}else if(strcmp(argv[10],"paretounsorted")==0){
			for(int i=0; i<7; i++){//size=20
				DAG* dag6 = new DAG(deadline,meet_dl);
				dag6->g = new Graph(); 
				constructDAG(dag6,montage,argv[7],3);
				jobs.push_back(dag6);
				DAG* dag7 = new DAG(deadline,meet_dl);
				dag7->g = new Graph(); 
				constructDAG(dag7,ligo,argv[7],2);
				jobs.push_back(dag7);
				DAG* dag8 = new DAG(deadline,meet_dl);
				dag8->g = new Graph(); 
				constructDAG(dag8,epigenome,argv[7],1);
				jobs.push_back(dag8);
			}
			for(int i=0; i<3; i++){//size = 100
				DAG* dag3 = new DAG(deadline,meet_dl);
				dag3->g = new Graph(); 
				constructDAG(dag3,montage100,argv[7],1);
				jobs.push_back(dag3);
				DAG* dag4 = new DAG(deadline,meet_dl);
				dag4->g = new Graph(); 
				constructDAG(dag4,ligo100,argv[7],2);
				jobs.push_back(dag4);
				DAG* dag5 = new DAG(deadline,meet_dl);
				dag5->g = new Graph(); 
				constructDAG(dag5,epi100,argv[7],3);
				jobs.push_back(dag5);
			}
			//size = 1000
			DAG* dag = new DAG(deadline,meet_dl);
			dag->g = new Graph(); 
			constructDAG(dag,montage1000,argv[7],2);
			jobs.push_back(dag);
			DAG* dag1 = new DAG(deadline,meet_dl);
			dag1->g = new Graph(); 
			constructDAG(dag1,ligo1000,argv[7],1);
			jobs.push_back(dag1);
			DAG* dag2 = new DAG(deadline,meet_dl);
			dag2->g = new Graph(); 
			constructDAG(dag2,epi1000,argv[7],3);
			jobs.push_back(dag2);
		}else printf("select one ensemble pattern\n");

		if(strcmp(argv[7],"spss")==0){//compared algorithm
			EnsembleSPSS* spssOptimizer = new EnsembleSPSS();
			for(int i=0; i<jobs.size(); i++)
				spssOptimizer->dags.push_back(jobs[i]);			
			spssOptimizer->Simulate(true);
		}else{
			SearchPrune* optimizer = new SearchPrune();
			optimizer->flag = ensemble;
			for(int i=0; i<jobs.size(); i++)
				optimizer->dags.push_back(jobs[i]);//put the dags into ensemble
			//order the dags in the ensemble by their priority, smaller first
			std::sort(optimizer->dags.begin(),optimizer->dags.end(),dagfunction);
		
			std::clock_t starttime = std::clock();
			optimizer->OfflineAstar();
			std::clock_t endtime = std::clock();
			float timeelapsed = (float)(endtime - starttime) / (float)CLOCKS_PER_SEC;
			printf("time for Astar search is: %.4f\n", timeelapsed);
			optimizer->Simulate();
		}
	}else if(strcmp(argv[8],"followsun")==0){
		//give the datacenters
		
		DataCenter* dcs[numDCs];
		for(int i=0; i<numDCs; i++){
			if(i<3)
				dcs[i] = new DataCenter(i,100,0.175,1,i);//us east region, use the cost of Amazon EC2 different zones
			else dcs[i] = new DataCenter(i,100,0.233,2,i);//singapore region
			for(int j=0; j<numDCs; j++){
				if(j==i)
					dcs[i]->commCost[j]=0;
				else 	dcs[i]->commCost[j]=0.1;//unit is dollar per GB
			}
			//for each dc, the number and size of workflows is random generated
			//number of workflow: between 10 to 50
			int numworkflows = rand() % 40 + 10;
			int select = rand() % 3;	
			for(int j=0; j<numworkflows; j++){
				//randomly select the size of workflow				
				DAG* dag = new DAG(deadline,meet_dl);
				dag->g = new Graph;
				if(select == 0){
					constructDAG(dag,montage,argv[7],0);
				}else if(select == 1){
					constructDAG(dag,montage100,argv[7],0);
				}else if(select == 2){
					constructDAG(dag,montage1000,argv[7],0);
				}
				dcs[i]->workloads.push_back(dag);
			}
		}

		if(strcmp(argv[7],"colog")==0){
			COLOG* cologoptimizer = new COLOG();
			cologoptimizer->dcs = dcs;
			cologoptimizer->Initialize();
			cologoptimizer->Simulate(true);
		}else{
			//general search other than Astar
			std::clock_t starttime = std::clock();
			configstack* initialstate=new configstack();
			configstack* feasible = new configstack();
			followsun_initialFeasible(dcs,initialstate,feasible);
			std::clock_t endtime = std::clock();
			float timeelapsed = (float)(endtime - starttime) / (float)CLOCKS_PER_SEC;
			printf("time for Astar search is: %.4f\n", timeelapsed);
			
			COLOG* cologoptimizer = new COLOG();
			cologoptimizer->dcs = dcs;
			cologoptimizer->plan = (int*)malloc(sizeof(int)*numDCs*(numDCs-1)/2);
			for(int i=0; i<numDCs*(numDCs-1)/2; i++)
				cologoptimizer->plan[i] = feasible->configurations[i];
			cologoptimizer->Simulate(false);
		}

	}else{
		printf("select one use case!\n");
	}
		

	return 0;
}

