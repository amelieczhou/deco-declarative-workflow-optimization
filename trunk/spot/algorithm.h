#include "instance.h"
#include "datacenter.h"

class Prolog{
public:
	std::vector<DAG*> dags;

	void Simulate();
	Prolog(){}
	Prolog(std::vector<DAG*>& input) {dags=input;}
};

class Autoscaling{
	public:
	std::vector<DAG*> dags;

	void Initialize();
	void Simulate(bool a);//if true, autoscaling simulation; otherwise simulation for our algorithm
	Autoscaling(){}
	Autoscaling(std::vector<DAG*>& input) {dags=input;}
};

class EnsembleSPSS{
	public:
	std::vector<DAG*> dags;
	int* plan;
	
	void planEnsemble(int* plan);
	float planWorkflow(DAG dag);
	void Simulate(bool a);//if true, spss simulation; otherwise simulation for our algorithm
	EnsembleSPSS(){}
	EnsembleSPSS(std::vector<DAG*>& input) {dags=input;}
};

class COLOG{
	public:
	DataCenter** dcs;
	int* plan;

	void Initialize();
	void PlanMaking();
	void Simulate(bool a);//if true, colog simulation; otherwise simulation for our algorithm
	COLOG(){}
	COLOG(DataCenter**& input) {dcs=input;}
};