# encoding:utf-8

from scipy.stats.stats import pearsonr,spearmanr
import numpy as np

nor_data = np.loadtxt("D:/experiment/code/K562-rep2.chr2.normalize.2d.matrix")
corr_data = np.zeros(nor_data.shape)
corr_data = np.float64(corr_data)
for i in range(nor_data.shape[0]):
    for j in range(i,nor_data.shape[1]):        
        corr_data[i,j] = pearsonr(nor_data[i],nor_data[j])[0]
        corr_data[j,i] = corr_data[i,j]
corr_data = np.nan_to_num(corr_data)
cov_data = np.cov(corr_data)
eig_value = np.linalg.eig(cov_data)
pca_value = eig_value[1][:,0]
pca_value = np.double(pca_value)
f = open("../K562-rep2.chr3.inter.pca.txt",'w')
f.write("index\tpca_1\n")
for i in range(len(pca_value)):
    f.write(str(float(i)/10)+"\t"+str(pca_value[i])+"\n")
f.close()