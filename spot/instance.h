#include <stdio.h>
#include <vector>
#include <queue>
#include <boost/graph/adjacency_list.hpp>
#include <boost/graph/graph_traits.hpp>
#include <boost/random/gamma_distribution.hpp>
#include <boost/random/normal_distribution.hpp>
#include <boost/graph/astar_search.hpp>
using namespace boost;

#pragma once
#ifndef INSTANCECONFIG_H_
#define INSTANCECONFIG_H_
//const int depth = 5; //only odd nums
//const int width = 10;//3;
//const float deadline = 1800;
const int types = 4;//types for spotvm and vm
const float priceOnDemand[] = {0.095, 0.19, 0.38, 0.76};
const float priceNetwork[]= {0.01,0.01,0,0.01,0.01,0.02};//in: same zone, same region, diff region; out: same zone, same region, diff region; per GB
extern int randomsize;//the size of random number generated
extern float OnDemandLag;//0.5;
extern float Times[4][types];
extern float lambda;
extern int num_jobs;
//extern int num_monte;
//const int ConfigSlot = 3;
#endif

enum Integer_vm
{
	not_ready = 0,
	ready = 1,
	scheduled = 2,
	finished = 3
};
enum DAG_type
{
	montage = 0,
	ligo = 1,
	epigenome = 2,
	pipeline = 3,
	montage100 = 4,
	montage1000 = 5,
	ligo100 = 6,
	ligo1000 = 7,
	epi100 = 8,
	epi1000 = 9
};


class taskVertex
{
public:
    int name;
	int type;
    bool mark;
    float start_time;
    float end_time;
    float dl;
    float readyCountdown;
    float* estTime; //expected time, used for deadline assign and configuration
	float* cpuTime;
	float* actTime; //actual time, vary with cloud dynamics
    float restTime;
    Integer_vm status;
    int config;
	float cost; //the monetary cost spent on this task
	float tasktime; //the execution time spent on this task
	float taskstart;
	int assigned_type; 

	//for autoscaling deadline assign
	float sub_deadline;
	float EST;
	float LFT;
	int prefer_type;
		
	int* configList;
	int vmID;
//	int* LV; //load vector
	float read_data; //I/O data in #seeks
	float seq_data; //sequential I/O data in MBytes
	float trans_data; //network data in MBytes
	float rec_data; //network data in MBytes

	float* randomIO; //types*randomsize
	float* seqIO;
	float* netUp;
	float* netDown;
	float* probestTime;
	float* cumulativeTime; //randomsize
	bool tag;


	void instance_config();

	taskVertex() {
		printf("constructor of task\n");
		estTime = (float*)malloc(types*sizeof(float));
		cpuTime = (float*)malloc(types*sizeof(float));
		config = 0; mark = false; assigned_type = 0; restTime = rec_data = 0.0;  prefer_type = 0.0; 
		readyCountdown = -1; status = not_ready; EST = LFT=0; sub_deadline=0;
        actTime = (float*)malloc(types*sizeof(float));
		netDown = (float*)malloc(types*randomsize*sizeof(float));
		netUp = (float*)malloc(types*randomsize*sizeof(float));
		probestTime = (float*)malloc(types*randomsize*sizeof(float));
		randomIO = (float*)malloc(types*randomsize*sizeof(float));
		seqIO = (float*)malloc(types*randomsize*sizeof(float));
		if(randomsize>4000)
			cumulativeTime = (float*)malloc(randomsize*sizeof(float));
		else
		cumulativeTime = (float*)malloc(4000*sizeof(float));
	}
};

class VM{
public:
	taskVertex* tk;
	int type;
	float life_time;
	float turn_on; //turn on time

	int has_data;//has data of task has_data on local disk
};

typedef adjacency_list<boost::vecS, boost::vecS, bidirectionalS, taskVertex, property<edge_weight_t, float> > Graph;
typedef graph_traits<Graph>::vertex_descriptor Vertex;
typedef graph_traits<Graph>::vertex_iterator vertex_iter;
typedef graph_traits<Graph>::edge_iterator edge_iter;
typedef graph_traits<Graph>::out_edge_iterator out_edge_iterator;
typedef graph_traits<Graph>::in_edge_iterator in_edge_iterator;
typedef graph_traits<Graph>::edge_descriptor edge_descriptor;
typedef graph_traits<Graph>::adjacency_iterator adja_iterator;

class DAG
{
public:
	Graph* g;
	float deadline;
	float meet_dl;
	DAG_type type;
	float arrival_time;
	float* cumulativetime;
	int priority;//for ensemble use case

	DAG() {};
	//~DAG() {printf("destroyed DAG\n");}
	DAG(float d,float d1){deadline = d; meet_dl = d1;}
	void reset();
	DAG(Graph* dag){g = dag;}
	DAG(const DAG& dag) {
		g=new Graph();//dont know the constructor of Graph, so it may cause memory leak!!!!
		Graph* graph = dag.g;
		//g->m_edges = graph->m_edges;
		//g->m_property = graph->m_property;
		std::pair<vertex_iter, vertex_iter> vp = vertices(*graph);
		for(; vp.first != vp.second; vp.first++){			
			taskVertex v = (*graph)[*vp.first];
			add_vertex(v,*g);			
		}
		vp = vertices(*g);
		for(; vp.first!=vp.second; vp.first++){
			in_edge_iterator in_i, in_end;
			edge_descriptor e;
			for (boost::tie(in_i, in_end) = in_edges(*vp.first, *graph); in_i != in_end; ++in_i){
				e = *in_i;
				int src = source(e, *graph);		
				add_edge(src,*vp.first,*g);
				if((*g).m_vertices.size()>*vp.first+1)
					printf("");
			}
		}
		deadline=dag.deadline;
		meet_dl=dag.meet_dl;
		type=dag.type;
		arrival_time=dag.arrival_time;
	}
	std::vector<int> find_CP();
	void initDLAssign();
	void deadline_assign();
};

bool function(float bid_price, float compare_price);
bool dagfunction(DAG* dag1, DAG* dag2);
float rn_01();
int rn_integers(int a, int b);
bool myfunction(taskVertex* a, taskVertex* b);
void constructDAG(DAG* dag, DAG_type dagtype, char* autoscaling, int priority);

//FOR DEADLINE ASSIGN
//double MET(taskVertex& tk);
double MTT(taskVertex* tk1, taskVertex* tk2);
double EST(taskVertex& tk, DAG job);
double LFT(taskVertex& tk, DAG job);
void AssignParents(taskVertex* tk, DAG* job);
Vertex CriticalParent(taskVertex* tk, DAG* job);
void AssignPath(std::deque<Vertex> PCP,DAG* job);
bool has_unassigned_parent(taskVertex* tk, DAG* job);
bool has_unassigned_child(taskVertex* tk, DAG* job);
void update_EST(taskVertex* tk, DAG* job);
void update_LFT(taskVertex* tk, DAG* job);
