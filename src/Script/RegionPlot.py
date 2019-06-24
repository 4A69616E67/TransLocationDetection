# -*- coding: utf-8 -*-

"""
Created on Mon Nov 12 00:33:20 2018

@author: Hao-jiang
"""

import numpy as np
import matplotlib

matplotlib.use('Agg')
import matplotlib.pyplot as plt
import argparse
import sys
from matplotlib.colors import LinearSegmentedColormap


def getargs():
    parser = argparse.ArgumentParser(description="Heat Map Plot.")
    parser.add_argument('-q', '--quantile', help='quantile for Max value of Heat Map.')
    parser.add_argument('-i', '--input', help='matrix file.')
    parser.add_argument('-l', '--list', help='list you want to draw.')
    parser.add_argument('-t', '--type', help='[point] or [region].')
    parser.add_argument('-o', '--out', help='Out put file')
    parser.add_argument('-r', '--resolution', help='resolution of HiC Data. Unit:bp')
    parser.add_argument('-c', '--chr', nargs='+', default=None, help="Chromosome1:start Chromosome2:start")
    args = parser.parse_args()

    return args


class Region:
    def __init__(self, n, x, y):
        self.name = n
        self.start = x
        self.end = y


class PlotRegion:
    def __init__(self, rn, rx, ry):
        self.name = rn
        self.X_region = rx
        self.Y_region = ry


def proper_u(pos):
    """
    Express a genomic position in a proper unit (KB, MB, or both).
    """

    i_part = int(pos) // 1000000  # Integer Part
    d_part = (int(pos) % 1000000) // 1000  # Decimal Part
    if (i_part > 0) and (d_part > 0):
        return ''.join([str(i_part), 'M', str(d_part), 'K'])
    elif i_part == 0:
        return ''.join([str(d_part), 'K'])
    else:
        return ''.join([str(i_part), 'M'])


args = getargs()
matrix_file = args.input
plot_file = args.list
outfile = args.out
Type = args.type
resolution = np.int32(args.resolution)
temp_chr = args.chr[0].split(":")
Chr1 = Region(temp_chr[0], np.int32(temp_chr[1]), np.int32(temp_chr[1]))
temp_chr = args.chr[1].split(":")
Chr2 = Region(temp_chr[0], np.int32(temp_chr[1]), np.int32(temp_chr[1]))
# matrix_file = "K562-rep1.r94.50.0k-P0.5k.2d.matrix"
# plot_file = "K562-rep1.chr6-chr16.0.1M.HisD.point"
# outfile = "test.pdf"
# Type = ""
# resolution = 5000
# Chr1 = Region("chr16", 78090763, 0)
# Chr2 = Region("chr6", 38013645, 0)
Matrix = np.transpose(np.loadtxt(matrix_file))
Dpi = 300
nonzero = Matrix[np.nonzero(Matrix)]
vmax = np.percentile(nonzero, 99)
my_cmap = LinearSegmentedColormap.from_list('interaction', ['#FFFFFF', '#CD0000'])
fig, ax = plt.subplots(1, figsize=(16, np.double(Matrix.shape[0]) / Matrix.shape[1] * 14))
sc = ax.imshow(Matrix, cmap=my_cmap, aspect='auto', interpolation='none',
               extent=(0, Matrix.shape[1], 0, Matrix.shape[0]), vmax=vmax, origin='lower')

xticks = list(np.linspace(0, Matrix.shape[1], 11).astype(int))
xpos = [(int(t) * resolution + int(Chr1.start)) for t in xticks]
xlabels = [proper_u(p) for p in xpos]
ax.set_xticks(xticks)
ax.set_xticklabels(xlabels, size=6)

yticks = list(np.linspace(0, Matrix.shape[0], 11).astype(int))
ypos = [(int(t) * resolution + int(Chr2.start)) for t in yticks]
ylabels = [proper_u(p) for p in ypos]
ax.set_yticks(yticks)
ax.set_yticklabels(ylabels, size=6)

ax.set_xlabel(Chr1.name, labelpad=15, style='italic')
ax.set_ylabel(Chr2.name, labelpad=15, style='italic')
fig.colorbar(sc)

if Type == 'point':
    f = open(plot_file, 'r')
    for line in f:
        lines = line.split()
        if lines[1] == Chr1.name and lines[4] == Chr2.name:
            point = Region(lines[0], (np.int32(lines[2]) + np.int32(lines[3])) / 2,
                           (np.int32(lines[5]) + np.int32(lines[6])) / 2)
        elif lines[4] == Chr1.name and lines[1] == Chr2.name:
            point = Region(lines[0], (np.int32(lines[5]) + np.int32(lines[6])) / 2,
                           (np.int32(lines[2]) + np.int32(lines[3])) / 2)
        else:
            continue
        ax.plot([np.floor((point.start - Chr1.start) / resolution), np.floor((point.start - Chr1.start) / resolution)],
                [0, Matrix.shape[0] - 1], markersize=0, linewidth=1, color='green')
        ax.plot([0, Matrix.shape[1] - 1],
                [np.floor((point.end - Chr2.start) / resolution), np.floor((point.end - Chr2.start) / resolution)],
                markersize=0, linewidth=1, color='green')
        ax.plot(np.floor((point.start - Chr1.start) / resolution), np.floor((point.end - Chr2.start) / resolution),
                'ob', markersize=3, alpha=0.7)
elif Type == 'region':
    f = open(plot_file, 'r')
    for line in f:
        lines = line.split()
        if lines[1] == Chr1.name and lines[4] == Chr2.name:
            region = PlotRegion(lines[0], Region(lines[1], np.int32(lines[2]), np.int32(lines[3])),
                                Region(lines[4], np.int32(lines[5]), np.int32(lines[6])))
        elif lines[4] == Chr1.name and lines[1] == Chr2.name:
            region = PlotRegion(lines[0], Region(lines[4], np.int32(lines[5]), np.int32(lines[6])),
                                Region(lines[1], np.int32(lines[2]), np.int32(lines[3])))
        else:
            continue
        x_start = np.floor((region.X_region.start - Chr1.start) / resolution)
        x_end = np.floor((region.X_region.end - Chr1.start) / resolution)
        y_start = np.floor((region.Y_region.start - Chr2.start) / resolution)
        y_end = np.floor((region.Y_region.end - Chr2.start) / resolution)
        ax.plot([x_start, x_start], [y_start, y_end], markersize=0, linewidth=1, color='black')
        ax.plot([x_end, x_end], [y_start, y_end], markersize=0, linewidth=1, color='black')
        ax.plot([x_start, x_end], [y_start, y_start], markersize=0, linewidth=1, color='black')
        ax.plot([x_start, x_end], [y_end, y_end], markersize=0, linewidth=1, color='black')
        ax.text(np.mean([x_start, x_end]), np.mean([y_start, y_end]), region.name, horizontalalignment='center',
                fontsize=2, verticalalignment='center')

fig.savefig(outfile, dpi=Dpi, transparent=False)
