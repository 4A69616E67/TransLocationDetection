# -*- coding: utf-8 -*-
"""
@author: snowf
install opencv by "pip install opencv-python"
"""

import cv2
import numpy as np
# import matplotlib.pyplot as plt
import argparse
from scipy import stats
import util


def GetIndex(I, W):
    return [int(I / W), I % W]


def GetArgs():
    parser = argparse.ArgumentParser(description="Harris Corner Detect.")
    parser.add_argument('-q', '--quantile',
                        help='quantile for Max value of Heat Map.', default=99)
    parser.add_argument('-i', '--input', nargs='+',
                        help='Matrix file.(Matrix file and Normalize matrix file if --intra set)')
    parser.add_argument('-p', '--prefix', help='Out put prefix')
    parser.add_argument('-r', '--res', help='resolution of HiC Data. Unit:bp')
    parser.add_argument('-c', '--chr', nargs='+', default=None,
                        help="Chromosome1:start:end Chromosome2:start:end")
    parser.add_argument('--intra', action='store_true',
                        default=False, help="intraction detect if set")
    parser.add_argument('--top', default=20, help="Top point would you want")
    args = parser.parse_args()

    return args


args = GetArgs()
# 矩阵文件
MatrixFile = args.input[0]
# MatrixFile="chr6-chr16.2d.matrix"
Auantile = int(args.quantile)
# 分辨率
Resolution = int(args.res)
MaxPvalue = 0.05
TopNum = int(args.top)
Prefix = args.prefix
chr1 = util.Chr(args.chr[0].split(":")[0], int(
    args.chr[0].split(":")[1]))  # 染色体类
chr2 = util.Chr(args.chr[1].split(":")[0], int(args.chr[1].split(":")[1]))
matrix = np.loadtxt(MatrixFile)  # 获取矩阵数据
OrigHeatmap = util.HeatMap("Orig", matrix, None, Auantile)  # Heatmap类
OrigHeatmap.draw()  # 绘制图像
GrayImg = cv2.cvtColor(OrigHeatmap.Image, cv2.COLOR_BGR2GRAY)  # 获得灰度图
ColorImg = OrigHeatmap.Image.copy()  # 获得彩图
# GrayImg=cv2.GaussianBlur(GrayImg,(3,3),0)#高斯滤波
# BlockSize=3
H = np.size(OrigHeatmap.Matrix, 0)
W = np.size(OrigHeatmap.Matrix, 1)
if H < 5 or W < 5:
    Pwrite = open(Prefix + ".HisD.point", "w+")
    Pwrite.close()
    exit()

TopPoint = np.zeros((TopNum + 4, 2), np.uint32)  # 前TopNum的点
TopPvalue = np.zeros((TopNum + 4, 2), np.float32)  # 前TopNum的P值
TopIndex = np.zeros(TopNum + 4, np.uint16)  # 前TopNum的象限值
topwrite = open(Prefix + ".HisD.t" + str(TopNum) + ".point", "w+")
Corners = cv2.goodFeaturesToTrack(GrayImg, TopNum, 0.01, 3)  # shi-Tomasi角点检测
# 取前TopNum个点并打印
CornersNum = len(Corners)
for i in range(min([TopNum, CornersNum])):
    TopPoint[i] = Corners[i].ravel()
    TopIndex[i], TopPvalue[i] = util.QuickCauCq(OrigHeatmap.Matrix, [TopPoint[i, 1], TopPoint[i, 0]], min(
        [TopPoint[i, 0], TopPoint[i, 1], H - TopPoint[i, 1] - 1, W - TopPoint[i, 0] - 1, 5]))
    topwrite.write("P" + str(i) + "\t" + str(TopPoint[i, 1]) + "," + str(TopPoint[i, 0]) + "\t" + chr1.Name + "\t" + str(chr1.Start + Resolution * TopPoint[i, 1]) + "\t" + chr2.Name + "\t" + str(
        chr2.Start + Resolution * TopPoint[i, 0]) + "\t" + str(TopPvalue[i, 0]) + "\t" + str(TopPvalue[i, 1]) + "\t" + str(TopIndex[i] + 1) + "\n")
topwrite.close()
Pwrite = open(Prefix + ".HisD.point", "w+")
Count = 0
for i in range(min([TopNum, CornersNum])):
    if sum(TopPvalue[i]) < MaxPvalue:
        Count += 1
        extend_length = 5
        if Resolution < 10000:
            extend_length = 30
        Pwrite.write("P" + str(i) + "\t" + str(TopPoint[i, 1]) + "," + str(TopPoint[i, 0]) + "\t" + chr1.Name + "\t" + str(chr1.Start + Resolution * (TopPoint[i, 1]-extend_length)) + "\t" +str(chr1.Start + Resolution * (TopPoint[i, 1]+extend_length)) + "\t" +
                     chr2.Name + "\t" + str(chr2.Start + Resolution * (TopPoint[i, 0]-extend_length)) + "\t"+ str(chr2.Start + Resolution * (TopPoint[i, 0]+extend_length)) + "\t" + str(TopPvalue[i, 0]) + "\t" + str(TopPvalue[i, 1]) + "\t" + str(TopIndex[i] + 1) + "\n")
        ColorImg[TopPoint[i, 1], TopPoint[i, 0]] = [255, 0, 0]

if Count == 0:
    TopPoint[0, 0] = int(W / 3)
    TopPoint[0, 1] = int(H / 3)
    TopPoint[1, 0] = int(2 * W / 3)
    TopPoint[1, 1] = int(H / 3)
    TopPoint[2, 0] = int(W / 3)
    TopPoint[2, 1] = int(2 * H / 3)
    TopPoint[3, 0] = int(2 * W / 3)
    TopPoint[3, 1] = int(2 * H / 3)
    for i in range(4):
        TopIndex[i], TopPvalue[i] = util.QuickCauCq(OrigHeatmap.Matrix, [TopPoint[i, 1], TopPoint[i, 0]], min(
            [TopPoint[i, 0], TopPoint[i, 1], H - TopPoint[i, 1] - 1, W - TopPoint[i, 0] - 1, 20]))
        if sum(TopPvalue[i]) < MaxPvalue:
            Pwrite.write("P" + str(i) + "\t" + str(TopPoint[i, 1]) + "," + str(TopPoint[i, 0]) + "\t" + chr1.Name + "\t" + str(chr1.Start + Resolution * (TopPoint[i, 1] - 10)) + "\t" + str(chr1.Start + Resolution * (TopPoint[i, 1] + 10)) + "\t" +
                         chr2.Name + "\t" + str(chr2.Start + Resolution * (TopPoint[i, 0] - 10)) + "\t" + str(chr2.Start + Resolution * (TopPoint[i, 0] + 10)) + "\t" + str(TopPvalue[i, 0]) + "\t" + str(TopPvalue[i, 1]) + "\t" + str(TopIndex[i] + 1) + "\n")
            ColorImg[TopPoint[i, 1], TopPoint[i, 0]] = [255, 0, 0]
Pwrite.close()
cv2.imwrite(Prefix + ".HisD.Color.png", ColorImg)  # 输出彩色图像
cv2.imwrite(Prefix + ".HisD.gray.png", GrayImg)  # 输出灰度图像
