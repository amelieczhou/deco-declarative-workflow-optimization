#include "cudaheader.h"
#include <stdio.h>
#include "cuda.h"
#include "cuda_runtime.h"
#include "device_launch_parameters.h"
#include <cstdlib>
#define PSIZE 32 
#define checkCudaErrors(err)  __checkCudaErrors (err, __FILE__, __LINE__)
inline void __checkCudaErrors(cudaError err, const char *file, const short line )
{
    if(cudaSuccess != err)
    {
        fprintf(stderr, "%s(%i) : CUDA Runtime API error %d: %s.\n",file, line, (short)err, cudaGetErrorString( err ) );
        exit(-1);        
    }
}
__device__ void push(CONFIGSTACK *set, int *tail, CONFIGSTACK *node)
{
	int pos;
	pos=atomicCAS(tail, SETSIZE-1, 0);
	if (*tail==pos)
		pos=atomicAdd(tail,1);
	set[pos]=*node;
}
__device__ CONFIGSTACK  pop (CONFIGSTACK *set, int *head, int *tail)
{
	int pos;
	if (*head!=*tail)
	{
		pos=atomicCAS(head, SETSIZE-1, 0);
		if (*head==pos)
			pos=atomicAdd(head, 1);
		return set[pos];//always pop the 0 + offset element
	}
}
__device__ bool find(CONFIGSTACK *set, CONFIGSTACK *state, int *head, int *tail)
{
	for (int i=*head;i!=*tail;)
	{
		int j;
		bool flag=true;
		for (j=0;j<VSIZE;++j)
		{
			if (set[i].configurations[j]!=state->configurations[j])
			{
				flag=false;
				break;
			}
		}
		if (flag)
			return true;
		if (i!=SETSIZE-1)
			i++;
		else
			i=0;
	}
	return false;
}
__global__ void evaluate (//Lock lock_o, Lock lock_c, Lock lock_s
						CONFIGSTACK *buffer, 
						DAG_CUDA dag, CONFIGSTACK *openSet, int *osHead, int *osTail, CONFIGSTACK *closeSet, int *csHead, int* csTail, CONFIGSTACK *solutionSet, int *solutionSize, 
						int flag, int types, float *probestTime, float OnDemandLag, 
						float deadline, float meet_dl, int randomsize, float *priceOnDemand, float globalBestCost, int *gv, int *inEdgeCount, int *in_edge_own)
{
	int bid=blockIdx.x;
	int tid=threadIdx.x;
	//int *inEdgeCount, *in_edge_own;
	//inEdgeCount=(int*)malloc(sizeof(int)*VSIZE);
	//in_edge_own=(int*)malloc(sizeof(int)*ESIZE*VSIZE);

	//__shared__ int inEdgeCount[VSIZE];
	//__shared__ int in_edge_own[ESIZE][VSIZE];//worst case
	__shared__ int totalcost;
	__shared__ int count;
	__shared__ CONFIGSTACK currentNode;
	__shared__ CONFIGSTACK  child;
	
	if (tid==0)
	{
		currentNode=buffer[bid];
		count=0;
	}	//fetch the node from buffer
	/*
	if (tid<VSIZE)
	{
            inEdgeCount[tid]=0;
            for (int i=1;i<2*ESIZE;i+=2)
            {
                    if (dag.edge[i]==tid)
                    {
                            in_edge_own[inEdgeCount[tid]+ESIZE*tid]=dag.edge[i-1];
                            inEdgeCount[tid]++;
                    }
            }
    	}
	__syncthreads();
	*/
	int len=0;
	int lc=0;
	float cumulativeTime[VSIZE];//accuracy
	while (len+tid<randomsize)
	{
		for (int i=0;i<VSIZE;++i)
		{
			cumulativeTime[i]=0;
		}
		for (int i=0;i<VSIZE;++i)
		{
			int config=currentNode.configurations[i];
			float tmpTime=0;
			if (inEdgeCount[i]==0)
			{
				cumulativeTime[i]=probestTime[i*randomsize*TYPES+config*randomsize+len+tid] + OnDemandLag;
			}
			else
			{
				for (int j=0;j<inEdgeCount[i];++j)
				{
					tmpTime= tmpTime>cumulativeTime[in_edge_own[j+ESIZE*i]]?tmpTime:cumulativeTime[in_edge_own[j+ESIZE*i]];
				}
				cumulativeTime[i]=tmpTime+probestTime[i*randomsize*TYPES + currentNode.configurations[i] * randomsize + len + tid] + OnDemandLag;
			}
		}		
		
		if(cumulativeTime[VSIZE-1]<=deadline)
		{
			lc++;
		}
		
		len+=blockDim.x;
	}
	atomicAdd(&count, lc);
	__syncthreads();
	if (tid==0)
	{
		if ((float)count / (float)randomsize >= meet_dl)
		{
		int fv=(int)(currentNode.fvalue*1000.0);
		if (fv<=atomicMin(gv, fv))
		{
			push(solutionSet, solutionSize, &currentNode);
		}
		}
	}
	if (tid==32)
	{
		push(closeSet, csTail, &currentNode);
		child=currentNode;
		child.taskno=currentNode.taskno + 1;
	}
	
	__syncthreads();	
	
	if (child.taskno < VSIZE)
	{
		for (int t=currentNode.configurations[child.taskno]+1;t<types;t++)
		{
			if (tid ==0)
			{
				child.configurations[child.taskno]=t;
			}
			__syncthreads();
			if (flag == deadlineapp)
			{
					if (tid==0)
						totalcost=0;
					for (int i=0;i<VSIZE;++i)
					{
						float taskcost=0;
						int len=0;
						while(len+tid<randomsize)//for (int j=0;j<randomsize;++j)
						{
							taskcost+=ceilf((probestTime[i*randomsize*TYPES+child.configurations[i]*randomsize+len+tid]+OnDemandLag)/3600.0)*priceOnDemand[child.configurations[i]];//edit
							len+=blockDim.x;
						}
						taskcost*=1000.0;
						atomicAdd(&totalcost, (int)taskcost);
					}
					__syncthreads();
					if (tid==0)
					{
						totalcost = (int)(1.0 * totalcost / randomsize);
						if (!( totalcost >= *gv || find(closeSet, &child, csHead, csTail))){
							child.fvalue=totalcost*1.0/1000;
							push(openSet, osTail, &child);
						}
					}
			}
		}
	}

}
__device__ bool equal(CONFIGSTACK a, CONFIGSTACK b)
{
	if (a.fvalue!=b.fvalue)
		return false;
	for (int i=0;i<VSIZE;++i)
		if (a.configurations[i]!=b.configurations[i])
			return false;
	return true;
}
#define buffer_size_max 16
__global__ void top_k(int *buffer_size, CONFIGSTACK *buffer, CONFIGSTACK *openSet, int *osHead, int *osTail)
{
	//to edit
	int bid=blockIdx.x;
	int tid=threadIdx.x;
	int sizeOpenSet=*osTail>=*osHead?(*osTail-*osHead):(SETSIZE-*osHead+*osTail);
	extern __shared__ float pos[];
	if (sizeOpenSet<buffer_size_max)
	{
		//if (tid < buffer_size_max)
		if (bid==0 && tid==0)
		{
			for (int i=0;i<sizeOpenSet;++i)
				buffer[i]=pop(openSet, osHead, osTail);
			*buffer_size=sizeOpenSet;
		}
	}
	else //if (sizeOpenSet<2*buffer_size_max)
	{
		pos[tid*2]=*osHead+bid*sizeOpenSet/gridDim.x+tid;
		pos[tid*2+1]=openSet[(int)pos[tid*2]].fvalue;
		int len=blockDim.x;
		while (len+pos[tid*2]<*osHead+(bid+1)*sizeOpenSet/gridDim.x)
		{
			if (openSet[len+(int)pos[tid*2]].fvalue<pos[tid*2+1])
			{
				pos[tid*2+1]=openSet[len+(int)pos[tid*2]].fvalue;
				pos[tid*2]+=len;
			}
			len+=blockDim.x;
		}
		len=blockDim.x/2;
		__syncthreads();
		while (len>=1)
		{
			if (tid<len)
			{
				if (pos[(tid+len)*2+1]<pos[tid*2+1])
				{
					pos[tid*2+1]=pos[(tid+len)*2+1];
					pos[tid*2]=pos[(tid+len)*2];
				}
			}
			len/=2;
			__syncthreads();
		}
		if (tid==0)
		{	
		buffer[bid]=openSet[(int)pos[0]];
		CONFIGSTACK temp;
		temp=pop(openSet, osHead, osTail);
		if (!equal(temp, buffer[bid]))
			openSet[(int)pos[0]]=temp;
		*buffer_size=buffer_size_max;
		}
	}
}
extern "C" void Astar_CUDA (int randomsize, int *vertex, int *edge, int v_size, int e_size,int flag,int types, 
				 float *probestTime[VSIZE],float OnDemandLag, const float *priceOnDemand, float globalBestCost, float deadline,float meet_dl,CONFIGSTACK initstate)
{
	checkCudaErrors(cudaSetDevice(0));
	//Lock lock_o, lock_c, lock_s, 
	printf ("CUDA function started\n");
	cudaEvent_t start, stop;
	cudaEventCreate(&start);
	cudaEventCreate(&stop);	
	cudaEventRecord(start, 0);
	CONFIGSTACK *openSet, *closeSet;
	int *osTail, *osHead, *csTail, *csHead, *d_osTail, *d_osHead;

	checkCudaErrors(cudaHostAlloc((void**)&osTail,sizeof(int),cudaHostAllocMapped));
	checkCudaErrors(cudaHostGetDevicePointer((void**)&d_osTail,(void*)osTail,0));
	checkCudaErrors(cudaHostAlloc((void**)&osHead,sizeof(int),cudaHostAllocMapped));
	checkCudaErrors(cudaHostGetDevicePointer((void**)&d_osHead,(void*)osHead,0));

	//cudaMalloc((void **)&osTail, sizeof(int)*1);
	//cudaMalloc((void **)&osHead, sizeof(int)*1);
	cudaMalloc((void **)&csTail, sizeof(int));
	cudaMalloc((void **)&csHead, sizeof(int));
	cudaMalloc((void **)&openSet, sizeof(CONFIGSTACK)*SETSIZE);
	cudaMalloc((void **)&closeSet, sizeof(CONFIGSTACK)*SETSIZE);
	cudaMemcpy(openSet, &initstate, sizeof(CONFIGSTACK), cudaMemcpyHostToDevice);
	*osTail=1;
	*osHead=0;
	
	//declare the solution set
	int *solutionSize, solutionResultSize=0;
	CONFIGSTACK *solutionSet, *solutionResult;
	solutionResult=(CONFIGSTACK *)malloc(sizeof(CONFIGSTACK)*100);
	cudaMalloc((void **)&solutionSize, sizeof(int));
	cudaMalloc((void **)&solutionSet, sizeof(CONFIGSTACK)*100);

	//prepare the dag
	DAG_CUDA dag_d;
	cudaMalloc((void **)&(dag_d.vertex), sizeof(int)*v_size);
	cudaMalloc((void **)&(dag_d.type), sizeof(int)*v_size);
	cudaMalloc((void **)&(dag_d.edge), sizeof(int)*2*ESIZE);

	float *priceOnDemand_d;
	cudaMalloc((void **)&priceOnDemand_d, sizeof(float)*TYPES);
	cudaMemcpy(priceOnDemand_d, priceOnDemand, sizeof(float)*TYPES, cudaMemcpyHostToDevice);

	//prepare the *probestTime
	float *probestTime_a;
	checkCudaErrors(cudaMalloc((void**)&probestTime_a, sizeof(float)*randomsize*TYPES*VSIZE));
	for (int i=0;i<VSIZE;++i)
	{
		//checkCudaErrors(
		cudaMemcpy(&probestTime_a[i*randomsize*TYPES], probestTime[i], sizeof(float)*randomsize*TYPES,cudaMemcpyHostToDevice);
	}

	cudaMemcpy(dag_d.vertex, vertex, sizeof(int)*v_size, cudaMemcpyHostToDevice);
	cudaMemcpy(dag_d.edge, edge, 2*sizeof(int)*ESIZE, cudaMemcpyHostToDevice);

	int *buffer_size, *d_buffer_size;

	checkCudaErrors(cudaHostAlloc((void**)&buffer_size,sizeof(int),cudaHostAllocMapped));
	checkCudaErrors(cudaHostGetDevicePointer((void**)&d_buffer_size,(void*)buffer_size,0));

	CONFIGSTACK *buffer;//, *buffer_child;
	cudaMalloc((void **)&buffer, sizeof(CONFIGSTACK)*buffer_size_max);
	//buffer_child=(CONFIGSTACK *)malloc(sizeof(CONFIGSTACK)*buffer_size_max*4);

	int zero=0;
	cudaMemcpy(csHead, &zero, sizeof(int), cudaMemcpyHostToDevice);
	cudaMemcpy(csTail, &zero, sizeof(int), cudaMemcpyHostToDevice);
	cudaMemcpy(solutionSize, &zero, sizeof(int), cudaMemcpyHostToDevice);

	int *gv, gv_h=(int)(globalBestCost*1000.0);
	cudaMalloc((void **)&gv, sizeof(int));
	cudaMemcpy(gv, &gv_h, sizeof(int), cudaMemcpyHostToDevice);
	
	int searchcount=0;
	*buffer_size=1;
	
	int *d_inEdgeCount, *d_in_edge_own;
	int inEdgeCount[VSIZE], in_edge_own[VSIZE*ESIZE];
        for (int i=0;i<VSIZE;++i)
        {
            inEdgeCount[i]=0;
            for (int j=1;j<2*ESIZE;j+=2)
            {
                    if (edge[j]==i)
                    {
                            in_edge_own[inEdgeCount[i]+ESIZE*i]=edge[j-1];
                            inEdgeCount[i]++;
                    }
            }
        }
	cudaMalloc((void**)&d_inEdgeCount, sizeof(int)*VSIZE);
	cudaMalloc((void**)&d_in_edge_own, sizeof(int)*VSIZE*ESIZE);
	cudaMemcpy(d_inEdgeCount, inEdgeCount, sizeof(int)*VSIZE, cudaMemcpyHostToDevice);
	cudaMemcpy(d_in_edge_own, in_edge_own, sizeof(int)*VSIZE*ESIZE, cudaMemcpyHostToDevice);

	do
	{
		int sizeOpenSet=*osTail>=*osHead?(*osTail-*osHead):(SETSIZE-*osHead+*osTail);
		int length=(int)sizeOpenSet/buffer_size_max;
		length=length>1?length:1;
		length=length<32?length:32;
		top_k<<< buffer_size_max, length, 2*length>>>(d_buffer_size, buffer, openSet, d_osHead, d_osTail);
		cudaDeviceSynchronize();
		
		evaluate<<<*buffer_size, 128>>>(buffer, dag_d, openSet, d_osHead, d_osTail, closeSet, csHead, csTail, solutionSet, solutionSize, 
				flag, types, probestTime_a, OnDemandLag, deadline, meet_dl, randomsize, priceOnDemand_d, globalBestCost, gv, d_inEdgeCount, d_in_edge_own);
		cudaDeviceSynchronize();
		
		
		searchcount+=*buffer_size;
	}while(!(*osHead == *osTail || searchcount>10000));
		//update<<<>>>(buffer_child, );
	
	cudaMemcpy(solutionResult, solutionSet, sizeof(CONFIGSTACK)*100, cudaMemcpyDeviceToHost);
	cudaMemcpy(&solutionResultSize, solutionSize, sizeof(int), cudaMemcpyDeviceToHost);
	cudaEventRecord(stop, 0);
	cudaEventSynchronize(stop);
	cudaMemcpy(&gv_h, gv, sizeof(int), cudaMemcpyDeviceToHost);
	printf ("CUDA function stoped after %d rounds with global best %f\n", searchcount, gv_h/1000.0);
	float elapsedTime;
	cudaEventElapsedTime(&elapsedTime, start, stop);
	printf ("Elapsed Time %f ms. Solutions found: %d\n", elapsedTime, solutionResultSize);
	printf ("OpenSet: %d - %d\n",*osHead, *osTail );
	for (int i=0;i<solutionResultSize;++i)
	{
		for (int j=0;j<VSIZE;++j)
			printf ("%d ", solutionResult[i].configurations[j]);
		printf ("%f\n", solutionResult[i].fvalue);
	}

}

