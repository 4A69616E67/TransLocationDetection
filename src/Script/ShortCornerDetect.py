# -*- coding: utf-8 -*-
"""
Created on Sun May 20 16:12:00 2018

@author: snowf

install opencv by "pip install opencv-python"
install scipy by "pip install scipy"
"""

import argparse

import cv2
import numpy as np
from scipy import stats

import util


def GetArgs():
    parser = argparse.ArgumentParser(description="Harris Corner Detect.")
    parser.add_argument('-q', '--quadrant',
                        help='quadrant value include [1,2,3,4]', default=None)
    parser.add_argument('-i', '--input', help='input file.')
    parser.add_argument('-p', '--prefix', help='Out put prefix')
    parser.add_argument('-r', '--res', help='resolution of HiC Data. Unit:bp')
    parser.add_argument('-c', '--chr', default=None, nargs='+',
                        help="Chromosome1:position Chromosome2:position")
    parser.add_argument('-pv', '--pvalue', default=0.0001, help="Max p value")
    args = parser.parse_args()

    return args


args = GetArgs()
Resolution = int(args.res)
Prefix = args.prefix
MaxPvalue = float(args.pvalue)
Quadrant = int(args.quadrant)
chr1 = util.Chr(args.chr[0].split(":")[0], int(args.chr[0].split(":")[1]))  # 染色体类
chr2 = util.Chr(args.chr[1].split(":")[0], int(args.chr[1].split(":")[1]))
# 矩阵文件
MatrixFile = args.input
# MatrixFile = "K562-rep1.chr13-chr22.0.1M-80.50M,23.20M.2k.2d.matrix"
matrix = np.loadtxt(MatrixFile)  # 获取矩阵数据
H = np.size(matrix, 0)  # row number
W = np.size(matrix, 1)  # col number
extend_length = 10
base_num = 3
col_sum = matrix.sum(axis=0)  # 每列的和
col_var_value = np.zeros(len(col_sum))
row_sum = matrix.sum(axis=1)  # 每行的和
row_var_value = np.zeros(len(row_sum))
for i in range(extend_length, len(row_sum) - extend_length):
    left = float(sum(row_sum[max(0, i - extend_length):i]))
    right = float(sum(row_sum[i:min(len(row_sum), i + extend_length)]))
    row_var_value[i] = (left - right) / (left + base_num) / (right + base_num) * (row_sum[i]+1)

for j in range(extend_length, len(col_sum) - extend_length):
    left = float(sum(col_sum[max(0, j - extend_length):j]))
    right = float(sum(col_sum[j:min(len(col_sum), j + extend_length)]))
    col_var_value[j] = (left - right) / (left + base_num) / (right + base_num) * (col_sum[j]+1)

if Quadrant == 1:
    row_max = row_var_value.max()
    row_max_index = row_var_value.argmax()
    col_max = col_var_value.min()
    col_max_index = col_var_value.argmin()
elif Quadrant == 2:
    row_max = row_var_value.max()
    row_max_index = row_var_value.argmax()
    col_max = col_var_value.max()
    col_max_index = col_var_value.argmax()
elif Quadrant == 3:
    row_max = row_var_value.min()
    row_max_index = row_var_value.argmin()
    col_max = col_var_value.max()
    col_max_index = col_var_value.argmax()
elif Quadrant == 4:
    row_max = row_var_value.min()
    row_max_index = row_var_value.argmin()
    col_max = col_var_value.min()
    col_max_index = col_var_value.argmin()
else:
    row_var_value = np.abs(row_var_value)
    col_var_value = np.abs(col_var_value)
    row_max = row_var_value.max()
    row_max_index = row_var_value.argmax()
    col_max = col_var_value.max()
    col_max_index = col_var_value.argmax()

H = np.size(matrix, 0)
W = np.size(matrix, 1)

f = open(Prefix + ".breakpoint", "w+")
row_position = chr1.Start + row_max_index * Resolution
col_position = chr2.Start + col_max_index * Resolution
f.write("P0" + "\t" + str(row_max_index + 1) + "," + str(col_max_index + 1) + "\t" + chr1.Name + "\t" +
        str(row_position) + "\t" + str(row_position + Resolution) + "\t" + chr2.Name + "\t" +
        str(col_position) + "\t" + str(col_position + Resolution) + "\t" + str(float(Resolution) / 1000) + "k\n")
f.close()
# ---------------------------------------------------------------------------------------------------------------------
