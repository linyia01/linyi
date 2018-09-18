#include "stdafx.h"
#include "wav.h"
#include "mfcc.h"
#include "dtw.h"
#include "train.h"
#include <string>
#include <io.h>
#include <iostream>
using namespace std;
int _tmain(int argc, _TCHAR* argv[]){
	WAV_STRUCT wavFileEg;
	wavStructInit(&wavFileEg);
	//F:/au_mp3/16k/src_sn/106580_75191-lq_25.wav
	//F:/au_mp3/16k/src_ot/vioce_env_765_denoise.wav
	string writepath="F:/au_mp3/16k/mfcc_ot/";
	string srcPath="F:/au_mp3/16k/src_ot/";

	long hFile = 0; 
	struct _finddata_t fileinfo;
	int count=0;
	if((hFile = _findfirst((srcPath+"/*").c_str(),&fileinfo)) !=  -1){
		do{
			if(!(fileinfo.attrib &  _A_SUBDIR)){
				try{
					count++;
					cout<<count<<" "+srcPath+fileinfo.name<<endl;
					double * originData = wavFile_execute((srcPath+fileinfo.name).c_str(), &wavFileEg);
					if(originData==NULL ){
						continue;
					}
					MFCC_Matrix mfcc_std, mfcc_diff_1, mfcc_diff_2;
					mfcc_std.frameNum=0;
					mfcc_std.dimension=0;
					mfcc_execute(&mfcc_std, originData, wavFileEg.numSamples, wavFileEg.wavHeader.fmt.sampleRate, 24, 128, 512, 24);
					if(mfcc_std.frameNum==0){
						continue;
					}
					FILE * fp = fopen((writepath+fileinfo.name+".mfcc").c_str(), "wb");
					float x=mfcc_std.frameNum;
					float y=mfcc_std.dimension;
					fwrite(&x, sizeof(float), 1, fp);
					fwrite(&y, sizeof(float), 1, fp);
					for(int ix = 0; ix < mfcc_std.frameNum; ++ix){
						for(int jy = 0; jy < mfcc_std.dimension; ++jy){
							float tem=mfcc_std.coeff[ix][jy];
							fwrite(&tem, sizeof(float), 1, fp);
							//printf("%.3f\t",mfcc_std.coeff[i][j]);
						}
						//printf("\n");
					}
					freeMatrix(mfcc_std.coeff);
					fclose(fp);
					free(originData);
					originData=NULL;
				}catch(const char* msg){
					cerr << msg << endl;
				}
			}
		}while(_findnext(hFile, &fileinfo)  == 0);
		_findclose(hFile);  
	}
	printf("training successful\n");
	return 0;
	 

	 
	//double * originData = wavFile_execute("F:/au_mp3/16k/src_sn/106580_75191-lq_25.wav", &wavFileEg);
	
	/*FILE * fp = fopen((writepath+filename+"mfcc").c_str(), "wb");
    if(!fp){
        printf("error: write search path failed\n");
        return FALSE;
    }
    fwrite(&(mfcc_std.dimension), sizeof(int), 1, fp);
    fwrite(&mfcc_std.frameNum, sizeof(int), 1, fp);
    for(int i = 0; i < mfcc_std.frameNum; ++i){
        for(int j = 0; j < mfcc_std.dimension; ++j){
            fwrite(&(mfcc_std.coeff[i][j]), sizeof(double), 1, fp);
			printf("%.3f\t",mfcc_std.coeff[i][j]);
        }
		printf("\n");
    }
	fclose(fp);
	mfcc_diff_execute(&mfcc_std, &mfcc_diff_1);
	mfcc_diff_execute(&mfcc_diff_1, &mfcc_diff_2);
	Boolean sign = update(mfcc_std, mfcc_diff_1, mfcc_diff_2, "");*/
	
}
