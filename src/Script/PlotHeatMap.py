# -*- coding: utf-8 -*-
"""
Created on Mon Apr 16 10:08:37 2018

@author: han-luo
"""

from __future__ import division
import numpy as np
import matplotlib
matplotlib.use('Agg')
#from matplotlib.backends.backend_pdf import PdfPages
import matplotlib.pyplot as plt
import argparse
import sys
from matplotlib.colors import LinearSegmentedColormap


def getargs():

    parser = argparse.ArgumentParser(description="Heat Map Plot.")
    parser.add_argument('-m', '--mode', choices=['A', 'B'],
                        help="""Mode for data type.
                              A means Raw Matrix.
                              B means Ternary sparse matrix.""")

    parser.add_argument('-q', '--quantile',
                        help='quantile for Max value of Heat Map.')
    parser.add_argument('-i', '--input', help='input file.')
    parser.add_argument('-o', '--out', help='Out put file')
    parser.add_argument('-r', '--resolution',
                        help='resolution of HiC Data. Unit:bp')
    parser.add_argument('-p', '--position', default=None,
                        help="""Positon for each chromosome start.
                                  if intra-chromosome interaction.
                                  eg : chr1:250000:chr1:250000
                                  or chr1:50000000:chr2:35000000
                                  Unit : bp""")
    parser.add_argument('-t', '--type', choices=['wholeGenome', 'localGenome'],
                        default='wholeGenome',
                        help=""" whether to draw wholeGenome.""")

    parser.add_argument('-c', '--config', default=None,
                        help=""" chromosome Size config file.
                                When the type is wholeGenome,config file is necessary
                                """)

    args = parser.parse_args()

    return args


def LoadData(fil, filetype):
    if filetype == 'A':
        Matrix = np.loadtxt(fil)
        return Matrix
    elif filetype == 'B':
        data_type = np.dtype({'names': ['bin1', 'bin2', 'IF'],
                              'formats': [np.int, np.int, np.float]})
        data = np.loadtxt(fil, dtype=data_type, usecols=[0, 1, 2])
        Matrix = getmatrix(data, 0, max(
            data['bin1'].max(), data['bin2'].max()))
        return Matrix
    else:
        return 0


def getmatrix(inter, l_bin, r_bin):
    inter_matrix = np.zeros((r_bin - l_bin, r_bin - l_bin), dtype=float)
    # Extract the regional data
    mask = (inter['bin1'] >= l_bin) & (inter['bin1'] < r_bin) & \
           (inter['bin2'] >= l_bin) & (inter['bin2'] < r_bin)
    inter_extract = inter[mask]

    # Fill the matrix:
    for i in inter_extract:
        # Off-diagnoal parts
        if i['bin1'] != i['bin2']:
            inter_matrix[i['bin1'] - l_bin][i['bin2'] - l_bin] += i['IF']
            inter_matrix[i['bin2'] - l_bin][i['bin1'] - l_bin] += i['IF']
        else:
            # Diagonal part
            inter_matrix[i['bin1'] - l_bin][i['bin2'] - l_bin] += i['IF']

    return inter_matrix


def properU(pos):
    """
    Express a genomic position in a proper unit (KB, MB, or both).

    """
    i_part = int(pos) // 1000000  # Integer Part
    d_part = (int(pos) % 1000000) // 1000  # Decimal Part

    if (i_part > 0) and (d_part > 0):
        return ''.join([str(i_part), 'M', str(d_part), 'K'])
    elif (i_part == 0):
        return ''.join([str(d_part), 'K'])
    else:
        return ''.join([str(i_part), 'M'])


def LoadConfig(config):
    """
    Load the Genome Size file.

    """
    dtype = np.dtype({'names': ['chr', 'start', 'end'],
                      'formats': ['S16', np.int, np.int]})

    data = np.loadtxt(config, dtype=dtype, usecols=[0, 1, 2])

    return data


if __name__ == '__main__':
    args = getargs()
    infile = args.input
    outfile = args.out
    resolution = int(args.resolution)
    mode = args.mode
    dtype = args.type
    Q = args.quantile
    if dtype == 'localGenome':
        if args.position == None:
            print("Positon parameter is necessary")
            sys.exit(1)
        else:
            Pos = (args.position).split(':')
    else:
        if args.config == None:
            print("BinSize file of wholeGenome is necessary")
            sys.exit(1)
        else:
            config = LoadConfig(args.config)

    Matrix = LoadData(infile, mode)
    nonzero = Matrix[np.nonzero(Matrix)]
    vmax = np.percentile(nonzero, Q)
    dtype = args.type

    Size = (16, (Matrix.shape[0] / Matrix.shape[1]) * 14)

    # Draw
    my_cmap = LinearSegmentedColormap.from_list(
        'interaction', ['#FFFFFF', '#CD0000'])
    fig, ax = plt.subplots(1, figsize=Size)
    sc = ax.imshow(Matrix, cmap=my_cmap, aspect='auto', interpolation='none',
                   extent=(0, Matrix.shape[1], 0, Matrix.shape[0]),
                   vmax=vmax, origin='lower')
    if dtype == 'localGenome':
        xticks = list(np.linspace(0, Matrix.shape[1], 5).astype(int))
        xpos = [(int(t) * resolution + int(Pos[1])) for t in xticks]
        xlabels = [properU(p) for p in xpos]
        ax.set_xticks(xticks)
        ax.set_xticklabels(xlabels, size=8)

        yticks = list(np.linspace(0, Matrix.shape[0], 5).astype(int))
        ypos = [(int(t) * resolution + int(Pos[3])) for t in yticks]
        ylabels = [properU(p) for p in ypos]
        ax.set_yticks(yticks)
        ax.set_yticklabels(ylabels, size=8)

        ax.set_xlabel(Pos[0], labelpad=5, style='italic')
        ax.set_ylabel(Pos[2], labelpad=5, style='italic')

    else:
        for i in config['end']:
            ax.axvline(i, ax.get_ylim()[0], ax.get_ylim()[1], linestyle='--',
                       color='black', linewidth=0.5)
            ax.axhline(i, ax.get_xlim()[0], ax.get_xlim()[1], linestyle='--',
                       color='black', linewidth=0.5)
        ticks = []
        ticklabels = []
        ax.tick_params(axis='both', bottom=False, top=False, left=False,
                       right=False, labelbottom=True, labeltop=False,
                       labelleft=True, labelright=False)
        for i in config:
            ticks.append((i['start'] + i['end']) // 2)
            ticklabels.append(i['chr'].lstrip('chr'))
        ax.set_xticks(ticks)
        ax.set_xticklabels(ticklabels, size=8)
        ax.set_yticks(ticks)
        ax.set_yticklabels(ticklabels, size=8)

    fig.colorbar(sc)
    Dpi = np.ceil(Matrix.shape[0] / Size[0] / 100) * 100
    if Dpi > 300:
        Dpi = 300
    fig.savefig(outfile, dpi=Dpi, transparent=False)
