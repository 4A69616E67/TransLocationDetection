# -*- coding: utf-8 -*-
"""
@author: snowf
install opencv by "pip install opencv-python"
"""

import cv2 
import numpy as np
#import matplotlib.pyplot as plt
import argparse
from scipy import stats
import util

#class Chr:
#    def __init__(self,name,startsite):
#        self.Name=name
#        self.Start=startsite
#
#class HeatMap:
#    def __init__(self,name,matrix,image,Fractile):
#        self.Name=name
#        self.Matrix=matrix
#        self.Image=image
#        self.Frac=Fractile
#        self.Mean=np.mean(matrix[np.nonzero(matrix)])
#        self.Var=np.var(matrix[np.nonzero(matrix)])
#    
#    def draw(self):
#        vmax=np.percentile(self.Matrix[np.nonzero(self.Matrix)],self.Frac)
##        vmax=np.max(self.Matrix)
#        image=self.Matrix
#        image[image > vmax]=vmax
#        self.Image=np.zeros((np.size(image,0),np.size(image,1),3),np.uint8)
#        for i in range(self.Image.shape[0]):
#            for j in range(self.Image.shape[1]):
#                self.Image[i,j,0]=255*(1-float(image[i,j]/vmax))
#                self.Image[i,j,1]=255*(1-float(image[i,j]/vmax))
#                self.Image[i,j,2]=255                
#    
def GetIndex(I,W):
    return [I/W,I%W]

#def QuadrantCount(Matrix,MiddleSite,Size):
#    H=np.size(Matrix,0)
#    W=np.size(Matrix,1)
#    MiddleSite=np.uint32(MiddleSite)
#    Quadrant=np.ones((4,1),np.float32)
#    if Size<=0:
#        return Quadrant
#    Size=np.uint32(Size)
#    for i in range(MiddleSite[0]-Size,MiddleSite[0]):
#        for j in range(MiddleSite[1]+1,MiddleSite[1]+1+Size):
#            if i>=0 and i<H and j>=0 and j<W:
#                Quadrant[0]+=Matrix[i,j]               
#    for i in range(MiddleSite[0]-Size,MiddleSite[0]):
#        for j in range(MiddleSite[1]-Size,MiddleSite[1]):
#            if i>=0 and i<H and j>=0 and j<W:
#                Quadrant[1]+=Matrix[i,j]
#    for i in range(MiddleSite[0]+1,MiddleSite[0]+1+Size):
#        for j in range(MiddleSite[1]-Size,MiddleSite[1]):
#            if i>=0 and i<H and j>=0 and j<W:
#                Quadrant[2]+=Matrix[i,j]
#    for i in range(MiddleSite[0]+1,MiddleSite[0]+1+Size):
#        for j in range(MiddleSite[1]+1,MiddleSite[1]+1+Size):
#            if i>=0 and i<H and j>=0 and j<W:
#                Quadrant[3]+=Matrix[i,j]
#    return Quadrant

def GetArgs():
    parser = argparse.ArgumentParser(description="Harris Corner Detect.")
    parser.add_argument('-q','--quantile',help = 'quantile for Max value of Heat Map.',default="99")
    parser.add_argument('-i','--input',help = 'input file.')
    parser.add_argument('-p','--prefix',help = 'Out put prefix')
    parser.add_argument('-r','--res',help = 'resolution of HiC Data. Unit:bp')
    parser.add_argument('-c','--chr',default = None,help = "Chromosome1,Chromosome2")
    args = parser.parse_args()

    return args


args = GetArgs()
#矩阵文件
MatrixFile=args.input
#MatrixFile="chr3-chr10-500k.2d.matrix"
#分辨率
Auantile=int(args.quantile)
Resolution=int(args.res)
Prefix=args.prefix
chr1=util.Chr(args.chr.split(",")[0].split(":")[0],int(args.chr.split(",")[0].split(":")[1]))#染色体类
chr2=util.Chr(args.chr.split(",")[1].split(":")[0],int(args.chr.split(",")[1].split(":")[1]))
matrix=np.loadtxt(MatrixFile)#获取矩阵数据
OrigHeatmap=util.HeatMap("Orig",matrix,None,99)#Heatmap类
OrigHeatmap.draw()#绘制图像
GrayImg=cv2.cvtColor(OrigHeatmap.Image,cv2.COLOR_BGR2GRAY)#获得灰度图
ColorImg=OrigHeatmap.Image#获得彩图
#filteredimg=cv2.GaussianBlur(filteredimg,(3,3),0)#高斯滤波
BlockSize=3
dst=cv2.cornerHarris(GrayImg,BlockSize,3,0.04)#Harris角点检测
SortIndex=np.argsort(-dst.ravel())#从大到小排序
N=50
Top50Index=np.zeros((N,2),np.uint32)#前50的索引
#取前50个点并打印
t50 =open(Prefix+".HisD.t"+str(N)+".point","w+")
for i in range(N):
    Top50Index[i]=GetIndex(SortIndex[i],dst.shape[1])
    t50.write("P"+str(i)+"\t"+str(Top50Index[i,0])+","+str(Top50Index[i,1])+"\t"+chr1.Name+"\t"+str(chr1.Start+Resolution*Top50Index[i,0])+"\t"+chr2.Name+"\t"+str(chr2.Start+Resolution*Top50Index[i,1])+"\t"+str(dst[Top50Index[i,0],Top50Index[i,1]])+"\n")
t50.close()
N=100
RealIndex=np.zeros((N,2),np.float32)#前10的索引
#取前100个点
for i in range(N):
    RealIndex[i]=GetIndex(SortIndex[i],dst.shape[1])

#合并掉距离相近的点
for i in list(reversed(range(N))):
    for j in list(reversed(range(i))):
        if np.abs(RealIndex[i,0]-RealIndex[j,0])+np.abs(RealIndex[i,1]-RealIndex[j,1])<=4:
            RealIndex[i]=[-1,-1]
            break
RealIndex=np.uint32(RealIndex[RealIndex[:,0]!=-1])#更新
#将潜在断点的周围分成4个区域并统计四个区域的交互总数，再做一个卡方检验来判断断点的可信度
Pwrite=open(Prefix+".HisD.point","w+")
Bias=np.int16([[1,-1],[1,1],[-1,1],[-1,-1]])*(BlockSize/2-1)#每个象限的偏移
Pvalue=[0.0,0.0]
#修正每个点的坐标
for i in range(np.size(RealIndex,0)):
    Value=util.QuadrantCount(OrigHeatmap.Matrix,RealIndex[i],4)#计算象限值
    MaxV=Value.max()#取最大值
    MaxIndex=np.argwhere(Value==MaxV)[0,0]#找到最大值对应的象限
    PList=np.zeros((4,2),np.uint32)#构建偏移点
    PList[0]=RealIndex[i]
    PList[1]=RealIndex[i]+[0,Bias[MaxIndex][1]]
    PList[2]=RealIndex[i]+[Bias[MaxIndex][0],0]
    PList[3]=RealIndex[i]+[Bias[MaxIndex][0],Bias[MaxIndex][1]]
    #值初始化
    MinIndex=-1
    MinCq=[1.0,1.0]
    MinP=np.zeros((1,2),np.uint32)
    #求每个偏移点的卡方值
    for P in PList:
        Value=util.QuadrantCount(OrigHeatmap.Matrix,P,4)
        MaxIndex=np.argwhere(Value==Value.max())[0,0]#取最大值的索引
        #求与相邻两个象限的卡方值
        Pvalue[0]=stats.chisquare([Value.max(),Value[(MaxIndex-1)%4]])[1]
        Pvalue[1]=stats.chisquare([Value.max(),Value[(MaxIndex+1)%4]])[1]
        #取最小值的点作为修正点
        if sum(Pvalue)<sum(MinCq):
            MinCq=Pvalue
            MinP=P
            MinIndex=MaxIndex
    #打印潜在的断点
    if sum(MinCq)<0.02 and MinP[0]>=0 and MinP[0]<ColorImg.shape[0] and MinP[1]>=0 and MinP[1]<ColorImg.shape[1]:
        Pwrite.write("P"+str(i)+"\t"+str(MinP[0])+","+str(MinP[1])+"\t"+chr1.Name+"\t"+str(chr1.Start+Resolution*MinP[0])+"\t"+chr2.Name+"\t"+str(chr2.Start+Resolution*MinP[1])+"\t"+str(dst[MinP[0],MinP[1]])+"\t"+str(MinCq[0])+"\t"+str(MinCq[1])+"\t"+str(MinIndex+1)+"\n")
        #将修正点绘制在热图上
        ColorImg[MinP[0],MinP[1]]=[255,0,0]
Pwrite.close()
cv2.imwrite(Prefix+".HisD.Color.png",ColorImg)#输出彩色图像
#cv2.imwrite(Prefix+".HisD.gray.png",GrayImg)#输出灰度图像

