#!/usr/bin/env python
# -*- coding: utf-8 -*-
__author__ = 'joseph areeda'
__version__ = '0.0.0'

from astropy.time import Time
import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np
from numpy import genfromtxt
import plotArgs
import sys
import re
import os.path

argList=plotArgs.getargs(sys.argv[1:])  # parse command line arguments
if argList.test :
    print('Dump arguments, parsing, and exit requested.')
    print('sys.argv:')
    print('    %s') % sys.argv
    print('argList:')
    print ('   %s') % argList
    sys.exit(3)

mpl.rc('font', family='sans-serif')
mpl.rcParams['font.size'] = '16.0'
mpl.rcParams['font.weight'] = 'book'

# determine image dimensions (geometry)
width=12
height=6
dpi=100.0

if argList.geometry is not None:
    m=re.search('(\d+)x(\d+)',argList.geometry)
    if m is not None:
        width = int(m.group(1)) / dpi
        height = int(m.group(2)) / dpi

mpl.rcParams['figure.figsize']=[width,height]
mpl.rcParams['figure.dpi']=dpi

# NB: make all mpl parameter changes before creating figure
fig=plt.figure()

rect=fig.patch
rect.set_facecolor('w')

if argList.interactive:
    plt.interactive(True)

ax=fig.add_subplot(1,1,1)

if argList.nogrid :
    ax.grid(False)
else:
    ax.grid(True)

if argList.logx :
    ax.set_xscale('log')

if argList.logy :
    ax.set_yscale('log')

for fnl in argList.infile :
    filename=fnl[0]     #<--weird but we get a list of lists back
    if argList.verbose > 0 :
        print("Adding: " + filename)
    pdata=genfromtxt(filename, delimiter=',')
    ax.plot(pdata[:,0],pdata[:,1])

# default range for plot is full range, but user can override any or all values
minmax = [np.min(pdata[:,0]), np.max(pdata[:,0]), np.min(pdata[:,1]), np.max(pdata[:,1])]
if argList.xmin is not None:
    minmax[0]=float(argList.xmin)

if argList.xmax is not None:
    minmax[1] = float(argList.xmax)

if argList.ymin is not None:
    minmax[2] = float(argList.ymin)

if argList.ymax is not None:
    minmax[3] = float(argList.ymax)

ax.axes.axis(minmax)

# if they didn't give us a title make one up
clist=[]
minGps=None
maxGps=None
plottype=''
title=''
if argList.title is None:
    for fnl in argList.infile :
        filename=fnl[0]
        base=os.path.basename(filename)
        f=re.search('(Spectrum_)?(.*)_(\d+)_(\d+)\.csv',base)
        
        if f is not None:
            plottype=f.group(1)[0:len(f.group(1))-1]
            if f.group(2) not in clist:
                clist.append(f.group(2))

            stGps=int(f.group(3))
            dur=int(f.group(4))
            
            if minGps is None:
                minGps=stGps
            else:
                minGps = min(stGps, minGps)

            if maxGps is None:
                maxGps = stGps + dur
            else:
                maxGps = max( maxGps, stGps + dur)

        title = plottype
        if len(clist) > 0:
            title += " " + clist[0]
            for c in clist[1:] :
                title += ", " + c

        if minGps is not None:
            t=Time(minGps,format='gps')
            title += "\n%s - %10d (%ds)" % (t.iso, minGps, maxGps-minGps)
else:
    title=''
    for t in argList.title :
        if t != '':
            title += "\n"
        title += t

plt.title(title)

if argList.ylabel is None:
    if plottype == "Spectrum":
        plt.ylabel(r'ASD $\left( \frac{\mathrm{Counts}}{\sqrt{\mathrm{Hz}}}\right)$')
else:
    plt.ylabel(r'%s'%argList.ylabel)

if argList.xlabel is None:
    if plottype == "Spectrum":
        plt.xlabel('Frequency (Hz)')
else:
    plt.xlabel(r'%s'%argList.xlabel)

if not argList.nogrid:
    plt.grid(b=True, which='major', color='k', linestyle='solid')
    plt.grid(b=True, which='minor', color='0.06', linestyle='dotted')

plt.tight_layout(pad=1.4, w_pad=4.5, h_pad=3.0)

if argList.interactive:
    plt.show()

if argList.out is not None:
    plt.savefig(argList.out,facecolor=fig.get_facecolor(), edgecolor='none')

