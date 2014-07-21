#include "stdafx.h"
#include "Interpret.h"
//
//bool Unify(Term* x, Term* y){
//	if(x->tag==0&&y->tag==0){
//		if(x->name != y->name)
//			return false;
//		else{
//			int size = x->fpar.size();
//			int size1 = y->fpar.size();
//			if(size != size1)
//				return false;
//			for(int i=0; i<size; i++){
//				if(x->fpar[i] != y->fpar[i])
//					return false;
//			}
//		}
//		return true;		
//	}
//	else if(x->tag==0&&y->tag==1){
//		if(x->name != y->name)
//			return false;
//		else{
//			int size = x->fpar.size();
//			int size1 = y->cpar.size();
//			if(size != size1)
//				return false;
//			for(int i=0; i<size; i++){
//				char* buf= (char*)malloc (sizeof(char)*10) ;
//				_itoa_s(x->fpar[i],buf,10,10);
//				//int size = sizeof(y->cpar[i]);
//				y->cpar[i] = buf;
//				//free(buf);
//			}
//		}
//		return true;
//	}
//	else if(x->tag ==1&&y->tag==0){
//		if(x->name != y->name)
//			return false;
//		else{
//			int size = x->cpar.size();
//			int size1 = y->fpar.size();
//			if(size != size1)
//				return false;
//			for(int i=0; i<size; i++){
//				_itoa_s(y->fpar[i],x->cpar[i],10,10);
//			}
//		}
//		return true;
//	}else if(x->tag==1&&y->tag==1){
//		if(x->name != y->name)
//			return false;
//		else{
//			int size = x->cpar.size();
//			int size1 = y->cpar.size();
//			if(size != size1)
//				return false;
//			for(int i=0; i<size; i++){
//				x->cpar[i] = y->cpar[i];
//			}
//		}
//		return true;
//	}
//	return false;
//}
//
//
//void PrologEval(std::vector<RuleList> rules,RuleList query){
//	int rsize = rules.size();
//	if(!query.empty()){
//		for(int i=0; i<rsize; i++){
//			if(Unify(rules[i].front(),query.front())){
//				RuleList newquery;
//				RuleList temprule = rules[i];
//				temprule.pop_front();
//				RuleList tempquery = query;
//				tempquery.pop_front();
//				while(!temprule.empty()){
//					newquery.push_back(temprule.front());
//					temprule.pop_front();
//				}
//				while(!tempquery.empty()){
//					newquery.push_back(tempquery.front());
//					tempquery.pop_front();
//				}
//				PrologEval(rules,newquery);
//			}else{
//				
//			}
//		}
//	}
//
//}
