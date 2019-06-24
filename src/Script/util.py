# -*- coding: utf-8 -*-
"""
Created on Thu May 24 12:01:36 2018

@author: snowf
"""

import random

import cv2
import numpy as np
# import matplotlib.pyplot as plt
from scipy import stats


class Chr:
    def __init__(self, name, startsite):
        self.Name = name
        self.Start = startsite


class HeatMap:
    def __init__(self, name, matrix, image, Fractile):
        self.Name = name
        self.Matrix = matrix
        self.Image = image
        self.Frac = Fractile
        # self.Mean = np.mean(matrix[np.nonzero(matrix)])
        # self.Var = np.var(matrix[np.nonzero(matrix)])

    def draw(self):
        vmax = np.percentile(self.Matrix[np.nonzero(self.Matrix)], self.Frac)
        image = self.Matrix
        image[image > vmax] = vmax
        self.Image = np.zeros((np.size(image, 0), np.size(image, 1), 3), np.uint8)
        for i in range(self.Image.shape[0]):
            for j in range(self.Image.shape[1]):
                self.Image[i, j, 0] = 255 * (1 - float(image[i, j] / vmax))
                self.Image[i, j, 1] = 255 * (1 - float(image[i, j] / vmax))
                self.Image[i, j, 2] = 255


# 计算四个象限的总交互数
def QuadrantCount(Matrix, MiddleSite, Size):
    MiddleSite = np.uint32(MiddleSite)
    Quadrant = [1.0, 1.0, 1.0, 1.0]
    if Size == "Auto":
        Size = min([MiddleSite[0], MiddleSite[1], np.size(Matrix, 0) - 1 - MiddleSite[0],
                    np.size(Matrix, 1) - 1 - MiddleSite[1]])
    if Size <= 0:
        return Quadrant
    Size = np.uint32(Size)
    Quadrant[0] = sum(sum(Matrix[(MiddleSite[0] - Size):MiddleSite[0], (MiddleSite[1] + 1):(MiddleSite[1] + 1 + Size)]))
    Quadrant[1] = sum(sum(Matrix[(MiddleSite[0] - Size):MiddleSite[0], (MiddleSite[1] - Size):MiddleSite[1]]))
    Quadrant[2] = sum(sum(Matrix[(MiddleSite[0] + 1):(MiddleSite[0] + 1 + Size), (MiddleSite[1] - Size):MiddleSite[1]]))
    Quadrant[3] = sum(
        sum(Matrix[(MiddleSite[0] + 1):(MiddleSite[0] + 1 + Size), (MiddleSite[1] + 1):(MiddleSite[1] + 1 + Size)]))
    return Quadrant


def CauCq(List, Index):
    return [stats.chisquare([List[Index] + 1, List[(Index - 1) % 4] + 1])[1],
            stats.chisquare([List[Index] + 1, List[(Index + 1) % 4] + 1])[1]]


# 计算卡方


def QuickCauCq(Matrix, MiddleSite, Size):
    List = QuadrantCount(Matrix, MiddleSite, Size)
    Index = List.index(max(List))
    return Index, [stats.chisquare([List[Index] + 1, List[(Index - 1) % 4] + 1])[1],
                   stats.chisquare([List[Index] + 1, List[(Index + 1) % 4] + 1])[1]]


def GetIndex(I, W):
    return [int(I / W), I % W]


# 随机识别


def RandomDetect(Matrix, Num, Region):
    H = np.size(Matrix, 0)
    W = np.size(Matrix, 1)
    MinPvalue = [1, 1]
    MinPoint = [0, 0]
    MinIndex = -1
    for i in range(Num):
        x = random.randint(int(Region[0]), int(Region[2]))
        y = random.randint(int(Region[1]), int(Region[3]))
        MaxIndex, Pvalue = QuickCauCq(
            Matrix, [x, y], min([x, y, H - x - 1, W - y - 1]))
        if sum(Pvalue) < sum(MinPvalue):
            MinPvalue = Pvalue
            MinPoint = [x, y]
            MinIndex = MaxIndex
    return MinPoint, MinPvalue, MinIndex


# 角点修正


def PointCorrection(Matrix, Point, Size, PointNum):
    H = np.size(Matrix, 0)
    W = np.size(Matrix, 1)
    Size = int(Size)
    Region = [max(0, Point[0] - Size), max(0, Point[1] - Size),
              min(Point[0] + 1 + Size, H), min(Point[1] + 1 + Size, W)]
    MinPoint = Point
    MinIndex, MinPvalue = QuickCauCq(Matrix, Point, min(
        [Point[0], Point[1], H - Point[0] - 1, W - Point[1] - 1]))
    point, pvalue, index = RandomDetect(Matrix, PointNum, Region)
    while sum(pvalue) < sum(MinPvalue) and index == MinIndex:
        MinPvalue = pvalue
        MinPoint = point
        Region = [max(0, MinPoint[0] - Size), max(0, MinPoint[1] - Size),
                  min(MinPoint[0] + 1 + Size, H), min(MinPoint[1] + 1 + Size, W)]
        point, pvalue, index = RandomDetect(Matrix, PointNum, Region)
    return MinPoint, MinPvalue, MinIndex


# 粒子群算法


class PSO:
    def __init__(self, Matrix, PopulationSize, Maxstep):
        self.Matrix = Matrix
        self.W = 0.4  # 惯性权重
        self.C1 = self.C2 = 2
        self.PopulationSize = PopulationSize  # 粒子群数量
        self.dim = 2  # 搜索空间的维度
        self.MaxStep = Maxstep  # 迭代次数
        self.X = np.random.uniform([0, 0], [np.size(Matrix, 0), np.size(
            Matrix, 1)], (self.PopulationSize, self.dim))  # 初始化粒子群位置
        self.V = np.random.rand(self.PopulationSize, self.dim)  # 初始化粒子群速度
        self.P = self.X  # 个体的最佳位置
        self.F = list()  # 个体最优适应度
        for x in self.X:
            fitness = self.CalculateFitness(x)
            self.F.append(fitness)
        self.Fg = min(self.F)  # 全局最优适应度
        self.Pg = self.X[self.F.index(self.Fg), :]  # 全局最优位置

    def Evolve(self):
        #        heatmap=HeatMap("Test",self.Matrix,None,98)
        #        heatmap.draw()
        for step in range(self.MaxStep):
            #            ColorImage=heatmap.Image.copy()
            r1 = np.random.rand(self.PopulationSize, self.dim)
            r2 = np.random.rand(self.PopulationSize, self.dim)
            self.V = self.W * self.V + self.C1 * r1 * \
                     (self.P - self.X) + self.C2 * r2 * (self.Pg - self.X)
            self.X = self.V + self.X
            fitness = list()
            for x in self.X:
                fitness.append(self.CalculateFitness(x))
            for i in range(len(fitness)):
                if fitness[i] < self.F[i]:
                    self.F[i] = fitness[i]
                    self.P[i] = self.X[i]
            #                cv2.circle(ColorImage,(int(self.X[i][1]),int(self.X[i][0])),2,(255,0,0),-1)#绘制角点
            #            print min(self.F)
            if self.Fg > min(self.F):
                # 全局最优适应度
                self.Fg = min(self.F)
                self.Pg = self.X[self.F.index(self.Fg), :]  # 全局最优位置

    #            cv2.circle(ColorImage,(int(self.Pg[1]),int(self.Pg[0])),2,(0,255,0),-1)#绘制角点
    #            cv2.imwrite("test.TmD.Color.png",ColorImage)
    #            time.sleep(2)

    def CalculateFitness(self, X):
        X = np.int0(X)
        Fitness = sum(QuickCauCq(self.Matrix, X, min([X[0], X[1], np.size(
            self.Matrix, 0) - X[0] - 1, np.size(self.Matrix, 1) - X[1] - 1]))[1])
        if np.isnan(Fitness):
            Fitness = 1
        return Fitness

# Temp=np.zeros((10,10),np.int8)
# for i in range(10):
#    for j in range(i):
#        Temp[i,j]=np.random.randint(0,2)
# Temp=Temp+Temp.T
