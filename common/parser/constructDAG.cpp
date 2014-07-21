#include "stdafx.h"
#include "instance.h"
#include <stdio.h>
#include <fstream>

/**
	@author: Amelie
	Construct dags from the DAX files.
*/
void constructDAG(DAG* dag, DAG_type dagtype, char* autoscaling, int priority){
	dag->priority = priority;
	if(dagtype == montage || dagtype ==montage100 || dagtype==montage1000){		
		//generate a montage DAG
		//unit time is second
		float tProjectPP[types] = {540,264,150,90};//{100,100,100,1};//{8,4,2,1};
		float tDiffFit[types] = {1200,600,339,200};//{100,100,100,1};//{8,4,2,1};
		float tConcatFit[types] = {2268,1122,636,372};//{100,100,100,1};//{8,4,2,1};
		float tBgModel[types] = {17244,8200,4633,2725};//{100,100,100,1};//{8,4,2,1};
		float tBackground[types] = {40,20,11,7};//{100,100,100,1};//{8,4,2,1};
		float tImgTbl[types] = {56,30,17,10};//{100,100,100,1};//{8,4,2,1};
		float tAdd[types] = {1080,179,101,59};//{100,100,100,1};//{8,4,2,1};
		float tShrink[types] = {324,150,85,50};//{100,100,100,1};//{8,4,2,1};
		float tJPEG[types] = {186,112,63,37};//{100,100,100,1};//{8,4,2,1};

		int indexs[9];
		int numtask=0;
		if(dagtype == montage){
			dag->type = montage;
			numtask = 20;
			indexs[0]=0; indexs[1]=4; indexs[2]=10; 
			indexs[3]=11; indexs[4]=12; indexs[5]=16; 
			indexs[6]=17; indexs[7]=18; indexs[8]=19;
		}else if(dagtype == montage100){
			dag->type = montage100;
			numtask = 100;
			indexs[0]=0; indexs[1]=16; indexs[2]=78;//0-1 projectpp,1-2difffit, 2-3 concatfit,3-4 bgmodel, 4-5 background,5-6 imgtbl,
			indexs[3]=79; indexs[4]=80; indexs[5]=96;
			indexs[6]=97; indexs[7]=98; indexs[8]=99;
		}else if(dagtype == montage1000){
			dag->type = montage1000;
			numtask = 1000;
			indexs[0]=0; indexs[1]=166; indexs[2]=828;
			indexs[3]=829; indexs[4]=830; indexs[5]=996;
			indexs[6]=997; indexs[7]=998; indexs[8]=999;
		}
				
		if(strcmp(autoscaling,"Autoscaling") == 0 ){
			//for(int i=0; i<9; i++)
			//	indexs[i] += 1;
			//add dummy entry and exit
			taskVertex* tk = new taskVertex();
			tk->name = 0; 
			tk->estTime = new float[types];
			for(int i=0; i<types; i++){
				tk->cpuTime[i] = 0.0;
			}
			tk->rec_data = tk->read_data= tk->seq_data  = tk->trans_data = 0;
			add_vertex(*tk,*dag->g);
		}
		int x=0, y=0;//debug
		for(int i=0; i<numtask; i++){
			taskVertex* tk = new taskVertex();
			tk->name = i;
			if(strcmp(autoscaling,"Autoscaling") == 0 )
				tk->name += 1;

			for(int k=0; k<types; k++)
				tk->estTime[k] = tk->actTime[k] = 0;
			if(i>=indexs[0] && i<indexs[1])	{
				for(int j=0; j<types; j++)
					tk->cpuTime[j] = tProjectPP[j];
				tk->read_data = 0; //random io in seeks
				tk->trans_data = 2070; //network transfer in MB
				tk->seq_data = 1050; //sequential io in MB
				tk->rec_data = 0; //network download data in MB
			}else if(i>=indexs[1] && i<indexs[2]){
				for(int j=0; j<types; j++)
					tk->cpuTime[j] = tDiffFit[j];
				tk->read_data = 0; //random io in seeks
				tk->trans_data = 86.6; //network transfer in MB
				tk->seq_data = 2100; //sequential io in MB
				tk->rec_data = 2100; //network download data in MB
			}else if(i >= indexs[2] && i<indexs[3]) {
				for(int j=0; j<types; j++)
					tk->cpuTime[j] = tConcatFit[j];
				tk->read_data = 0; //random io in seeks
				tk->trans_data = 1; //network transfer in MB
				tk->seq_data = 519.6; //sequential io in MB
				tk->rec_data = 519.6; //network download data in MB
			}else if(i >= indexs[3] && i<indexs[4]) {
				for(int j=0; j<types; j++)
					tk->cpuTime[j] = tBgModel[j];
				tk->read_data = 0; //random io in seeks
				tk->trans_data = 0.1; //network transfer in MB
				tk->seq_data = 1; //sequential io in MB
				tk->rec_data = 1; //network download data in MB
			}else if(i>=indexs[4] && i<indexs[5]){
				for(int j=0; j<types; j++)
					tk->cpuTime[j] = tBackground[j];
				tk->read_data = 0; //random io in seeks
				tk->trans_data = 2070; //network transfer in MB
				tk->seq_data = 2070; //sequential io in MB
				tk->rec_data = 2070; //network download data in MB
			}else if(i >= indexs[5] && i<indexs[6]){
				for(int j=0; j<types; j++)
					tk->cpuTime[j] = tImgTbl[j];
				tk->read_data = 0; //random io in seeks
				tk->trans_data = 0; //network transfer in MB
				tk->seq_data = 8280; //sequential io in MB
				tk->rec_data = 8280; //network download data in MB
			}else if(i == indexs[6]){
				for(int j=0; j<types; j++)
					tk->cpuTime[j] = tAdd[j];
				tk->read_data = 0; //random io in seeks
				tk->trans_data = 4950; //network transfer in MB
				tk->seq_data = 8280; //sequential io in MB
				tk->rec_data = 8280; //network download data in MB
			}else if(i == indexs[7]){
				for(int j=0; j<types; j++)
					tk->cpuTime[j] = tShrink[j];
				tk->read_data = 0; //random io in seeks
				tk->trans_data = 154.4; //network transfer in MB
				tk->seq_data = 2470; //sequential io in MB
				tk->rec_data = 2470; //network download data in MB
			}else if(i == indexs[8]) {
				for(int j=0; j<types; j++)
					tk->cpuTime[j] = tJPEG[j];
				tk->read_data = 0; //random io in seeks
				tk->trans_data = 6.8; //network transfer in MB
				tk->seq_data = 154.4; //sequential io in MB
				tk->rec_data = 154.4; //network download data in MB
			}else {
				printf("there is a task not considered in montage.\n");
				exit(1);
			}
			//for debuging, dont forget to comment it!!!!!!!!!!
			//tk.read_data = tk.trans_data = tk.seq_data = 0;
			add_vertex(*tk, *dag->g);
			x = (int)dag->g->m_vertices.size();
			y = (int)dag->g->m_vertices.capacity();
			/*if(x>100)
				printf("what's wrong?\n");*/
		}
		if(strcmp(autoscaling,"Autoscaling") == 0 ){
			taskVertex* tk = new taskVertex();
			tk->name = numtask+1; 
			tk->estTime = new float[types];
			for(int i=0; i<types; i++){
				tk->cpuTime[i] = 0.0;
			}
			tk->rec_data = tk->read_data= tk->seq_data  = tk->trans_data = 0;
			add_vertex(*tk,*dag->g);
		}
		
		//add edges to the graph
		if(dagtype == montage){
			int l1[4] = {0,1,2,3};
			int l2[6] = {4,5,6,7,8,9};
			int l3[4] = {12,13,14,15};
			if(strcmp(autoscaling,"Autoscaling") == 0 ){
				add_edge(1,5,*dag->g);
				add_edge(2,5,*dag->g);
				add_edge(1,6,*dag->g);
				add_edge(2,6,*dag->g);
				add_edge(2,7,*dag->g);
				add_edge(3,7,*dag->g);
				add_edge(3,8,*dag->g);
				add_edge(4,8,*dag->g);
				add_edge(2,9,*dag->g);
				add_edge(4,9,*dag->g);
				add_edge(4,10,*dag->g);
				for(int i=0; i<6; i++){
					add_edge(l2[i]+1,11,*dag->g);
				}
				add_edge(11,12,*dag->g);
				for(int i=0; i<4;i++){
					add_edge(0,l1[i]+1,*dag->g);
					add_edge(i+1,l3[i]+1,*dag->g);
					add_edge(11+1,l3[i]+1,*dag->g);
					add_edge(l3[i]+1,17,*dag->g);
				}
				add_edge(19,20,*dag->g);
				add_edge(17,18,*dag->g);
				add_edge(18,19,*dag->g);
				add_edge(20,21,*dag->g);
			}else{
				add_edge(0,4,*dag->g);
				add_edge(1,4,*dag->g);
				add_edge(0,5,*dag->g);
				add_edge(1,5,*dag->g);
				add_edge(1,6,*dag->g);
				add_edge(2,6,*dag->g);
				add_edge(2,7,*dag->g);
				add_edge(3,7,*dag->g);
				add_edge(1,8,*dag->g);
				add_edge(3,8,*dag->g);
				add_edge(3,9,*dag->g);
				for(int i=0; i<6; i++){
					add_edge(l2[i],10,*dag->g);
				}
				add_edge(10,11,*dag->g);
				for(int i=0; i<4;i++){
					add_edge(i,l3[i],*dag->g);
					add_edge(11,l3[i],*dag->g);
					add_edge(l3[i],16,*dag->g);
				}
				add_edge(16,17,*dag->g);
				add_edge(17,18,*dag->g);
				add_edge(18,19,*dag->g);
			}
		}else if(dagtype == montage100 || dagtype == montage1000){
			char* filename;
			if(dagtype == montage100)
				filename = "Montage_100.xml";	
			else
				filename = "Montage_1000.xml";
			//add virtual task edges
			if(strcmp(autoscaling,"Autoscaling") == 0){
				for(int i=0; i<indexs[1]; i++)
					add_edge(0,i+1,*dag->g);
				add_edge(numtask,numtask+1,*dag->g);
			}

			FILE* rFile;
			rFile = fopen(filename,"r");
			if(rFile == NULL){
				printf("cannot open file %s\n",filename);
				exit(1);
			}
			std::ifstream fin(filename);
			char str[14400];
			char *ptr, *ptr2;
			while(fin.getline(str,14400)){//
				ptr = str;
				char firstword[9];
				for(int i=0; i<8; i++){
					firstword[i] = str[i];
				}
				firstword[8] = '\0';
				if(strcmp(firstword,"  <child")==0){
					//start to record the edges
					ptr2 = strchr(ptr,'0');
					while(*ptr2=='0'){
						ptr2++;
					}
					ptr = ptr2;
					while(*ptr!='"'){
						ptr++;
					}
					char buf[4];
					memset(buf,0,4);
					strncpy(buf,ptr2,ptr-ptr2);
					int child = atoi(buf); 
					//read all parents	
					fin.getline(str,14400);
					for(int i=0; i<8; i++){
						firstword[i] = str[i];
					}
					while(strcmp(firstword,"  </chil")!=0){				
						char buff[4];
						memset(buff,0,4);
						ptr = str;
						ptr2=strchr(ptr,'0');
						while(*ptr2=='0'){
							ptr2++;
						}
						ptr = ptr2;
						while(*ptr!='"'){
							ptr++;
						}				
						strncpy(buff,ptr2,ptr-ptr2);
						int parent = atoi(buff); 
						//insert edge						
						if(strcmp(autoscaling,"Autoscaling") == 0){
							add_edge(parent+1,child+1,*dag->g);
						}else{
							add_edge(parent,child,*dag->g);
						}
						//update
						fin.getline(str,14400);
						for(int i=0; i<8; i++){
							firstword[i] = str[i];
						}
					}
				}
			}
		}
	}else if(dagtype == ligo || dagtype==ligo100 || dagtype ==ligo1000){		
		//unit time in seconds
		float TmpltBank[types] = {5296,2913,1602,881};
		float Inspiral[types] = {31092,17100,9405,5173};
		float Thinca[types] = {153,84,47,26};
		float TrigBank[types] = {13,7,4,2};

		int indexs[8];
		int numtask = 0;
		if(dagtype == ligo){
			numtask = 40;
			dag->type = ligo;
			indexs[0]=0; indexs[1]=9; indexs[2]=18; indexs[3]=29;//0-1 tmpltbank, 1-2 and 3-4 inspiral, 2 4 5 6 thinca, 7-3 trigbank
			indexs[4]=38; indexs[5]=19; indexs[6]=39; indexs[7]=20;
		}else if(dagtype == ligo100){
			numtask = 100;
			dag->type = ligo100;
			indexs[0]=0; indexs[1]=23; indexs[2]=46; indexs[3]=49;
			indexs[4]=73; indexs[5]=97; indexs[6]=100; indexs[7]=0;
		}else if(dagtype == ligo1000){
			numtask = 1000;
			dag->type = ligo1000;
			indexs[0]=0; indexs[1]=229; indexs[2]=458; indexs[3]=49;//0-1 tmpltbank, 1-2 and 4-5 inspiral, 2-3 and 5-6 thinca, 3-4trigbank
			indexs[4]=729; indexs[5]=980; indexs[6]=1000; indexs[7]=0;
			
		}		
		if(strcmp(autoscaling,"Autoscaling") == 0 ){
			taskVertex* tk = new taskVertex();
			tk->name = 0; 
			tk->estTime = new float[types];
			for(int i=0; i<types; i++){
				tk->cpuTime[i] = 0.0;
			}
			tk->rec_data = tk->read_data= tk->seq_data  = tk->trans_data = 0;
			add_vertex(*tk,*dag->g);
		}
		for(int i=0; i<numtask; i++) //40tasks in total
		{
			taskVertex tk;
			tk.name = i;
			if(strcmp(autoscaling,"Autoscaling") == 0 )
				tk.name += 1;
			for(int k=0; k<types; k++)
				tk.estTime[k] = tk.actTime[k] = 0;
			if(dagtype==ligo){
				if(i>=indexs[0] && i<indexs[1]){
					//TmpltBank
					for(int j=0; j<types; j++)
						tk.cpuTime[j] = TmpltBank[j];
					tk.read_data = 0; //random io in seeks
					tk.trans_data = 0.13; //network transfer in MB
					tk.seq_data = 1780.8; //sequential io in MB
					tk.rec_data = 0; //network download data in MB
				}else if((i>=indexs[1] && i<indexs[2])||(i>=indexs[3] && i<indexs[4])){
					//Inspiral
					for(int j=0; j<types; j++)
						tk.cpuTime[j] = Inspiral[j];
					tk.read_data = 0; //random io in seeks
					tk.trans_data = 1; //network transfer in MB
					tk.seq_data = 11002; //sequential io in MB
					tk.rec_data = 11002; //network download data in MB
				}else if(i==indexs[2] || i==indexs[5] || i==indexs[4] || i==indexs[6]){
					//Thinca
					for(int j=0; j<types; j++)
						tk.cpuTime[j] = Thinca[j];
					tk.read_data = 0; //random io in seeks
					tk.trans_data = 0.7; //network transfer in MB
					tk.seq_data = 38.4; //sequential io in MB
					tk.rec_data = 38.4; //network download data in MB
				}else if(i>=indexs[7] && i<indexs[3]){
					//TrigBank
					for(int j=0; j<types; j++)
						tk.cpuTime[j] = TrigBank[j];
					tk.read_data = 0; //random io in seeks
					tk.trans_data = 0; //network transfer in MB
					tk.seq_data = 0.7; //sequential io in MB
					tk.rec_data = 0.7; //network download data in MB
				}else {
					printf("there's a task not considered in Ligo.\n");
					exit(1);
				}
			}else{
				if(i>=indexs[0] && i<indexs[1]){
					//TmpltBank
					for(int j=0; j<types; j++)
						tk.cpuTime[j] = TmpltBank[j];
					tk.read_data = 0; //random io in seeks
					tk.trans_data = 0.13; //network transfer in MB
					tk.seq_data = 1780.8; //sequential io in MB
					tk.rec_data = 0; //network download data in MB
				}else if((i>=indexs[1] && i<indexs[2])||(i>=indexs[4] && i<indexs[5])){
					//Inspiral
					for(int j=0; j<types; j++)
						tk.cpuTime[j] = Inspiral[j];
					tk.read_data = 0; //random io in seeks
					tk.trans_data = 1; //network transfer in MB
					tk.seq_data = 11002; //sequential io in MB
					tk.rec_data = 11002; //network download data in MB
				}else if((i>=indexs[2] && i<indexs[3])||(i>=indexs[5] && i<indexs[6])){
					//Thinca
					for(int j=0; j<types; j++)
						tk.cpuTime[j] = Thinca[j];
					tk.read_data = 0; //random io in seeks
					tk.trans_data = 0.7; //network transfer in MB
					tk.seq_data = 38.4; //sequential io in MB
					tk.rec_data = 38.4; //network download data in MB
				}else if(i>=indexs[3] && i<indexs[4]){
					//TrigBank
					for(int j=0; j<types; j++)
						tk.cpuTime[j] = TrigBank[j];
					tk.read_data = 0; //random io in seeks
					tk.trans_data = 0; //network transfer in MB
					tk.seq_data = 0.7; //sequential io in MB
					tk.rec_data = 0.7; //network download data in MB
				}else {
					printf("there's a task not considered in Ligo.\n");
					exit(1);
				}
			}
			add_vertex(tk, *dag->g);
		}
		if(strcmp(autoscaling,"Autoscaling") == 0 ){
			taskVertex* tk = new taskVertex();
			tk->name = numtask+1; 
			tk->estTime = new float[types];
			for(int i=0; i<types; i++){
				tk->cpuTime[i] = 0.0;
			}
			tk->rec_data = tk->read_data= tk->seq_data  = tk->trans_data = 0;
			add_vertex(*tk,*dag->g);
		}
		//add edges
		if(dagtype==ligo){
			int l1[9] = {0,1,2,3,4,5,6,7,8};
			int l2[5] = {9,10,11,12,13};
			int l3[4] = {14,15,16,17};
			if(strcmp(autoscaling,"Autoscaling") == 0 ){
				for(int i=0; i<9; i++) {
					add_edge(0,l1[i]+1,*dag->g);
					add_edge(l1[i]+1,l1[i]+10,*dag->g);
					add_edge(l1[i]+21,l1[i]+30,*dag->g);
				}
				for(int i=0; i<5; i++) {
					add_edge(l2[i]+1,19,*dag->g);
					add_edge(19,l2[i]+12,*dag->g);
					add_edge(l2[i]+21,39,*dag->g);
				}
				for(int i=0; i<4; i++){
					add_edge(l3[i]+1,20,*dag->g);
					add_edge(20,l3[i]+12,*dag->g);
					add_edge(l3[i]+21,40,*dag->g);
				}
				add_edge(39,41,*dag->g);
				add_edge(40,41,*dag->g);
			}else{
				for(int i=0; i<9; i++) {
					add_edge(l1[i],l1[i]+9,*dag->g);
					add_edge(l1[i]+20,l1[i]+29,*dag->g);
				}
				for(int i=0; i<5; i++) {
					add_edge(l2[i],18,*dag->g);
					add_edge(18,l2[i]+11,*dag->g);
					add_edge(l2[i]+20,38,*dag->g);
				}
				for(int i=0; i<4; i++){
					add_edge(l3[i],19,*dag->g);
					add_edge(19,l3[i]+11,*dag->g);
					add_edge(l3[i]+20,39,*dag->g);
				}		
			}
		}else if(dagtype ==ligo100 || dagtype ==ligo1000){
			char* filename;
			if(dagtype == ligo100)
				filename = "Inspiral_100.xml";	
			else
				filename = "Inspiral_1000.xml";
			//add virtual task edges
			if(strcmp(autoscaling,"Autoscaling") == 0){
				for(int i=0; i<indexs[1]; i++)
					add_edge(0,i+1,*dag->g);
				for(int i=indexs[5]; i<indexs[6]; i++)
					add_edge(i+1,numtask+1,*dag->g);
			}

			FILE* rFile;
			rFile = fopen(filename,"r");
			if(rFile == NULL){
				printf("cannot open file %s\n",filename);
				exit(1);
			}
			std::ifstream fin(filename);
			char str[14400];
			char *ptr, *ptr2;
			while(fin.getline(str,14400)){//
				ptr = str;
				char firstword[9];
				for(int i=0; i<8; i++){
					firstword[i] = str[i];
				}
				firstword[8] = '\0';
				if(strcmp(firstword,"  <child")==0){
					//start to record the edges
					ptr2 = strchr(ptr,'0');
					while(*ptr2=='0'){
						ptr2++;
					}
					ptr = ptr2;
					while(*ptr!='"'){
						ptr++;
					}
					char buf[4];
					memset(buf,0,4);
					strncpy(buf,ptr2,ptr-ptr2);
					int child = atoi(buf); 
					//read all parents	
					fin.getline(str,14400);
					for(int i=0; i<8; i++){
						firstword[i] = str[i];
					}
					while(strcmp(firstword,"  </chil")!=0){				
				
						char buff[4];
						memset(buff,0,4);
						ptr = str;
						ptr2=strchr(ptr,'0');
						while(*ptr2=='0'){
							ptr2++;
						}
						ptr = ptr2;
						while(*ptr!='"'){
							ptr++;
						}				
						strncpy(buff,ptr2,ptr-ptr2);
						int parent = atoi(buff);  
						//insert edge						
						if(strcmp(autoscaling,"Autoscaling") == 0){
							
							add_edge(parent+1,child+1,*dag->g);
						}else{
							add_edge(parent,child,*dag->g);
						}
						//update
						fin.getline(str,14400);
						for(int i=0; i<8; i++){
							firstword[i] = str[i];
						}
					}
				}
			}
		}
	}else if(dagtype == epigenome||dagtype==epi100||dagtype==epi1000){	

		float fastQSplit[types] = {791,434,238,133};
		float filterConstams[types] = {261,144,79,43};
		float sol2sanger[types] = {53,29,16,9};
		float fastq2bfq[types] = {149,82,45,25};
		float map[types] = {21355,11745,6460,3553};
		float mapMerge[types] = {291,160,88,48};
		float maqIndex[types] = {145,80,44,24};
		float pileup[types] = {185,102,56,31};

		int indexs[8];
		int numtask = 0;
		if(dagtype ==epigenome){
			dag->type = epigenome;
			numtask = 20;
			indexs[0]=0; indexs[1]=1; indexs[2]=5; 
			indexs[3]=9; indexs[4]=13; indexs[5]=17; 
			indexs[6]=18; indexs[7]=19;
		}else if(dagtype == epi100){
			dag->type = epi100;
			numtask = 100;
			indexs[0]=0; indexs[1]=1; indexs[2]=25; 
			indexs[3]=49; indexs[4]=73; indexs[5]=97; 
			indexs[6]=98; indexs[7]=99;
		}else if(dagtype == epi1000){
			dag->type = epi1000;
			numtask = 997;
			indexs[0]=0; indexs[1]=7; indexs[2]=252; 
			indexs[3]=497; indexs[4]=742; indexs[5]=987; 
			indexs[6]=995; indexs[7]=996;
		}
		
		if(strcmp(autoscaling,"Autoscaling") == 0 ){
			taskVertex* tk = new taskVertex();
			tk->name = 0; 
			tk->estTime = new float[types];
			for(int i=0; i<types; i++){
				tk->cpuTime[i] = 0.0;
			}
			tk->rec_data = tk->read_data= tk->seq_data  = tk->trans_data = 0;
			add_vertex(*tk,*dag->g);
		}
		for(int i=0; i<numtask; i++){ //20 tasks in total
			taskVertex tk;
			tk.name = i;
			if(strcmp(autoscaling,"Autoscaling") == 0 )
				tk.name += 1;
			for(int k=0; k<types; k++)
				tk.estTime[k] = tk.actTime[k] = 0;

			if(i>=indexs[0]&&i<indexs[1]){
				//fastQSplit
				for(int j=0; j<types; j++)
					tk.cpuTime[j] = fastQSplit[j];
				tk.read_data = 0; //random io in seeks
				tk.trans_data = 1696; //network transfer in MB
				tk.seq_data = 1696; //sequential io in MB
				tk.rec_data = 0; //network download data in MB
			}else if(i>=indexs[1] && i<indexs[2]){
				//filterConstams
				for(int j=0; j<types; j++)
					tk.cpuTime[j] = filterConstams[j];
				tk.read_data = 0; //random io in seeks
				tk.trans_data = 424; //network transfer in MB
				tk.seq_data = 424; //sequential io in MB
				tk.rec_data = 424; //network download data in MB
			}else if(i>=indexs[2] && i<indexs[3]){
				//sol2sanger
				for(int j=0; j<types; j++)
					tk.cpuTime[j] = sol2sanger[j];
				tk.read_data = 0; //random io in seeks
				tk.trans_data = 323; //network transfer in MB
				tk.seq_data = 420.8; //sequential io in MB
				tk.rec_data = 420.8; //network download data in MB
			}else if(i>=indexs[3] && i<indexs[4]){
				//fastq2bfq
				for(int j=0; j<types; j++)
					tk.cpuTime[j] = fastq2bfq[j];
				tk.read_data = 0; //random io in seeks
				tk.trans_data = 71; //network transfer in MB
				tk.seq_data = 323; //sequential io in MB
				tk.rec_data = 323; //network download data in MB
			}else if(i>=indexs[4] && i<indexs[5]){
				//map
				for(int j=0; j<types; j++)
					tk.cpuTime[j] = map[j];
				tk.read_data = 0; //random io in seeks
				tk.trans_data = 28.8; //network transfer in MB
				tk.seq_data = 4440; //sequential io in MB
				tk.rec_data = 4440; //network download data in MB
			}else if(i>=indexs[5] && i<indexs[6]){
				//mapMerge
				for(int j=0; j<types; j++)
					tk.cpuTime[j] = mapMerge[j];
				tk.read_data = 0; //random io in seeks
				tk.trans_data = 213.68; //network transfer in MB
				tk.seq_data = 221.44; //sequential io in MB
				tk.rec_data = 221.44; //network download data in MB
			}else if(i==indexs[6]){
				//maqIndex
				for(int j=0; j<types; j++)
					tk.cpuTime[j] = maqIndex[j];
				tk.read_data = 0; //random io in seeks
				tk.trans_data = 107.53; //network transfer in MB
				tk.seq_data = 214.1; //sequential io in MB
				tk.rec_data = 214.1; //network download data in MB
			}else if(i==indexs[7]){
				//pileup
				for(int j=0; j<types; j++)
					tk.cpuTime[j] = pileup[j];
				tk.read_data = 0; //random io in seeks
				tk.trans_data = 84; //network transfer in MB
				tk.seq_data = 151.82; //sequential io in MB
				tk.rec_data = 151.82; //network download data in MB
			}else{
				printf("there is a task not considered in epigenome.\n");
				exit(1);
			}
			add_vertex(tk, *dag->g);
		}
		if(strcmp(autoscaling,"Autoscaling") == 0 ){
			taskVertex* tk = new taskVertex();
			tk->name = numtask+1; 
			tk->estTime = new float[types];
			for(int i=0; i<types; i++){
				tk->cpuTime[i] = 0.0;
			}
			tk->rec_data = tk->read_data= tk->seq_data  = tk->trans_data = 0;
			add_vertex(*tk,*dag->g);
		}

		if(dagtype == epigenome){
			if(strcmp(autoscaling,"Autoscaling") == 0 ){
				//add edges
				for(int i=0; i<4; i++){
					add_edge(1,i+2,*dag->g);
					add_edge(i+2,i+6,*dag->g);
					add_edge(i+6,i+10,*dag->g);
					add_edge(i+10,i+14,*dag->g);
					add_edge(i+14,18,*dag->g);
				}
				add_edge(19,20,*dag->g);
				add_edge(18,19,*dag->g);
				add_edge(0,1,*dag->g);
				add_edge(20,21,*dag->g);
			}else{
				//add edges
				for(int i=0; i<4; i++){
					add_edge(0,i+1,*dag->g);
					add_edge(i+1,i+5,*dag->g);
					add_edge(i+5,i+9,*dag->g);
					add_edge(i+9,i+13,*dag->g);
					add_edge(i+13,17,*dag->g);
				}
				add_edge(17,18,*dag->g);
				add_edge(18,19,*dag->g);
			}
		}else if(dagtype ==epi100 || dagtype==epi1000){
			if(strcmp(autoscaling,"Autoscaling") == 0 ){
				for(int i=0; i<indexs[1]; i++)
					add_edge(0,i+1,*dag->g);
				add_edge(numtask,numtask+1,*dag->g);
			}
			char* filename;
			if(dagtype == epi100)
				filename = "Epigenomics_100.xml";	
			else
				filename = "Epigenomics_997.xml";

			FILE* rFile;
			rFile = fopen(filename,"r");
			if(rFile == NULL){
				printf("cannot open file %s\n",filename);
				exit(1);
			}
			std::ifstream fin(filename);
			char str[14400];
			char *ptr, *ptr2;
			while(fin.getline(str,14400)){//
				ptr = str;
				char firstword[9];
				for(int i=0; i<8; i++){
					firstword[i] = str[i];
				}
				firstword[8] = '\0';
				if(strcmp(firstword,"  <child")==0){
					//start to record the edges
					ptr2 = strchr(ptr,'0');
					while(*ptr2=='0'){
						ptr2++;
					}
					ptr = ptr2;
					while(*ptr!='"'){
						ptr++;
					}
					char buf[4];
					memset(buf,0,4);
					strncpy(buf,ptr2,ptr-ptr2);
					int child = atoi(buf); 
					//read all parents	
					fin.getline(str,14400);
					for(int i=0; i<8; i++){
						firstword[i] = str[i];
					}
					while(strcmp(firstword,"  </chil")!=0){				
				
						char buff[4];
						memset(buff,0,4);
						ptr = str;
						ptr2=strchr(ptr,'0');
						while(*ptr2=='0'){
							ptr2++;
						}
						ptr = ptr2;
						while(*ptr!='"'){
							ptr++;
						}				
						strncpy(buff,ptr2,ptr-ptr2);
						int parent = atoi(buff); 
						//insert edge						
						if(strcmp(autoscaling,"Autoscaling") == 0){
							
							add_edge(parent+1,child+1,*dag->g);
						}else{
							add_edge(parent,child,*dag->g);
						}
						//update
						fin.getline(str,14400);
						for(int i=0; i<8; i++){
							firstword[i] = str[i];
						}
					}
				}
			}
		}
	}else if(dagtype==pipeline){
		if(strcmp(autoscaling,"Autoscaling")==0){
			taskVertex* tk = new taskVertex();
			tk->name = 0; 
			tk->estTime = new float[types];
			for(int i=0; i<types; i++){
				tk->cpuTime[i] = 0.0;
			}
			tk->rec_data = tk->read_data= tk->seq_data  = tk->trans_data = 0;
			add_vertex(*tk,*dag->g);
		}

		for(int i=0; i<2; i++){
			taskVertex tk;
			tk.name = i;
			if(strcmp(autoscaling,"Autoscaling") == 0 ){
				tk.name+=1;
			}
			for(int k=0; k<types; k++)
				tk.estTime[k] = tk.actTime[k] = 0;
			float pileup[types] = {185,102,56,31};
			for(int j=0; j<types; j++)
				tk.cpuTime[j] = pileup[j];
			tk.read_data = 0; //random io in seeks
			tk.trans_data = 84; //network transfer in MB
			tk.seq_data = 151.82; //sequential io in MB
			tk.rec_data = 151.82; //network download data in MB

			add_vertex(tk,*dag->g);
		}
		add_edge(0,1,*dag->g);
		if(strcmp(autoscaling,"Autoscaling") == 0){
			taskVertex* tk = new taskVertex();
			tk->name = 3; 
			tk->estTime = new float[types];
			for(int i=0; i<types; i++){
				tk->cpuTime[i] = 0.0;
			}
			tk->rec_data = tk->read_data= tk->seq_data  = tk->trans_data = 0;
			add_vertex(*tk,*dag->g);
			add_edge(1,2,*dag->g);
			add_edge(2,3,*dag->g);
		}			
	}
	else{
		printf("constructDAG only for dag type of montage, ligo and epigenome\n");
	}
	return;
}
