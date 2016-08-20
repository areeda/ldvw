#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Generalized plotting program for ldvw, use --help option for details"""
__author__ = 'joseph areeda'
__version__ = '0.0.0'

import os.path
import os
# make sure weh have a home value in our environment
try:
    home = os.environ['HOME']
except:
    os.environ['HOME'] = ''
if len(os.environ['HOME']) == 0:
    os.environ['HOME'] = '/usr/local/ldvw/.config/'


import astropy
from astropy.time import Time
import matplotlib as mpl
import matplotlib.pyplot as plt
from matplotlib import gridspec
import numpy as np
from numpy import genfromtxt
import plotArgs
import sys
import re
import pylab as pyl

argList = plotArgs.getargs(sys.argv[1:])  # parse command line arguments
if argList.test:
    print 'Dump arguments, parsing, and exit requested.'
    print 'sys.argv:'
    print('    %s') % sys.argv
    print 'argList:'
    print ('   %s') % argList
    sys.exit(3)

mpl.rc('font', family='sans-serif')
mpl.rcParams['font.size'] = '16.0'
mpl.rcParams['font.weight'] = 'book'
mpl.rcParams['legend.loc'] = 'best'

# determine image dimensions (geometry)
width = 12
height = 6
dpi = 100.0

if argList.geometry is not None:
    m = re.search(r'(\d+)x(\d+)', argList.geometry)
    if m is not None:
        width = int(m.group(1)) / dpi
        height = int(m.group(2)) / dpi

mpl.rcParams['figure.figsize'] = [width, height]
mpl.rcParams['figure.dpi'] = dpi

# NB: make all mpl parameter changes before creating figure
fig = plt.figure()
#gs1 = gridspec.GridSpec(1, 1)
#gs1.update(left=0.05, right=0.48, top=1.5, wspace=0.05)


rect = fig.patch
rect.set_facecolor('w')

if argList.interactive:
    plt.interactive(True)

#ax = plt.subplot(gs1[0, 0])
ax = fig.add_subplot(1, 1, 1)

if argList.nogrid:
    ax.grid(False)
else:
    ax.grid(True)

if argList.logx:
    ax.set_xscale('log')

if argList.logy:
    ax.set_yscale('log')


nfiles=argList.infile.__len__()

if argList.legend is not None:
    nlegends=argList.legend.__len__()
else:
    nlegends=0

# scaling for x and y axis
floatMax = sys.float_info.max
minmax=[floatMax, -floatMax,floatMax, -floatMax]

for fn in range(0, nfiles):
    fnl = argList.infile[fn]
    filename = fnl[0]     #<--weird but we get a list of lists back
    if argList.verbose > 0:
        print "Adding: " + filename
    pdata = genfromtxt(filename, delimiter=',')
    leg = str(fn)
    if fn < nlegends:
       leg = argList.legend[fn][0]
    ax.plot(pdata[:, 0], pdata[:, 1],label=leg,linewidth=1.5)
    
    # if they specify x-axis limits we have to recalculate y limits
    y0 = 0
    y1 = len(pdata[:, 0]) - 1

    if argList.xmin:
        minmax[0] = float(argList.xmin)
        y0 = (np.abs(pdata[:,0]-minmax[0])).argmin()
    else:
        minmax[0] = np.min([minmax[0],np.min(pdata[:, 0])])

    if argList.xmax:
        minmax[1] = float(argList.xmax)
        y1 = (np.abs(pdata[:,0]-minmax[1])).argmin()
    else:
        minmax[1] = np.max([minmax[1],np.max(pdata[:, 0])])

    if argList.ymin:
        minmax[2] = float(argList.ymin)
    else:
        minmax[2] = np.min([minmax[2],np.min(pdata[y0:y1, 1])])

    if argList.ymax:
        minmax[3] = float(argList.ymax)
    else:
        minmax[3] = np.max([minmax[3],np.max(pdata[y0:y1, 1])])
    #--- end of per input file loop

ax.axes.axis(minmax)
if nlegends > 0:
   pyl.legend(framealpha=0.5,fontsize='x-small')

# if they didn't give us a title make one up
clist = []
minGps = None
maxGps = None
plottype = ''
title = ''
if argList.title is None:
    for fnl in argList.infile:
        filename = fnl[0]
        base = os.path.basename(filename)
        f = re.search(r'(Spectrum_)?(.*)_(\d+)_(\d+)\.csv', base)

        if f is not None:
            plottype = f.group(1)[0:len(f.group(1))-1]
            if f.group(2) not in clist:
                clist.append(f.group(2))

            stGps = int(f.group(3))
            dur = int(f.group(4))

            if minGps is None:
                minGps = stGps
            else:
                minGps = min(stGps, minGps)

            if maxGps is None:
                maxGps = stGps + dur
            else:
                maxGps = max(maxGps, stGps + dur)

        title = plottype
        if len(clist) > 0:
            title += " " + clist[0]
            for c in clist[1:]:
                title += ", " + c

        if minGps:
            t = Time(minGps, format='gps')
            title += "\n%s - %10d (%ds)" % (t.iso, minGps, maxGps-minGps)
else:
    title = ''
    for t in argList.title:
        if t != '':
            title += "\n"
        title += t

plt.title(r'%s' % title, fontsize=12)

if argList.suptitle is not None:
    plt.suptitle(r'%s' %argList.suptitle,fontsize=18)

if argList.ylabel is None:
    if plottype == "Spectrum":
        plt.ylabel(r'ASD $\left( \frac{\mathrm{Counts}}{\sqrt{\mathrm{Hz}}}\right)$')
else:
    plt.ylabel(r'%s' % argList.ylabel)

if argList.xlabel is None:
    if plottype == "Spectrum":
        plt.xlabel('Frequency (Hz)')
else:
    plt.xlabel(r'%s'%argList.xlabel)

if not argList.nogrid:
    plt.grid(b=True, which='major', color='k', linestyle='solid')
    plt.grid(b=True, which='minor', color='0.06', linestyle='dotted')

#gs1.tight_layout(pad=1.4, w_pad=4.5, h_pad=3.0)

plt.tight_layout(pad=1.4, w_pad=4.5, h_pad=3.0)
plt.subplots_adjust(top=0.85)

if argList.out is not None:
    plt.savefig(argList.out, facecolor=fig.get_facecolor(), edgecolor='none')

if argList.interactive:
    plt.show()

