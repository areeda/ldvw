#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""Command line coherence using GWpy, see --help for details
"""

__author__ = 'joseph areeda'
__version__ = '0.0.0'

# import all stdlib packages first
import os
import sys
import re
import argparse

# if we're launched with minimum or no environment variables make some guesses
if len(os.getenv('HOME', '')) == 0:
    os.environ['HOME'] = '/tmp/'
# if launched from a terminal with no display
if len(os.getenv('DISPLAY', '')) == 0:
    import matplotlib
    matplotlib.use('Agg')

# import all third-party packages in sets (in vague order of dependence)
import numpy
from astropy.time import Time
from matplotlib import (rcParams, pyplot)
from gwpy.timeseries import TimeSeries
from gwpy.plotter.tex import label_to_latex

# -----------------------------------------------------------------------------
# parse command line arguments

parser = argparse.ArgumentParser(description='Plot LIGO time series using NDS2',
                                 prefix_chars='-')
parser.add_argument('-g', '--geometry', default='1200x600',
                    help='size of resulting image WxH, default: %(default)s')
parser.add_argument('--interactive', action='store_true',
                    help='when running from ipython allows experimentation')
parser.add_argument('--logf', action='store_true',
                    help='make frequency axis logarithmic')
parser.add_argument('--logy', action='store_true',
                    help='make y-axis logarithmic')
parser.add_argument('--nogrid', action='store_true',
                    help='do not display grid lines')
parser.add_argument('--title', action='append' , help='One or more title lines')
parser.add_argument('--suptitle',
                    help='1st title line (larger than the others)')
parser.add_argument('--xlabel', help='x axis text')
parser.add_argument('--ylabel', help='y axis text')
parser.add_argument('--ymin', help='fix min value for yaxis defaults to min of data')
parser.add_argument('--fmin', help='min value for frequency axis')
parser.add_argument('--ymax', help='max value for y-axis default to max of data')
parser.add_argument('--fmax', help='max value for frequency axis')
parser.add_argument('--out', help='output filename, type=ext (png, pdf, jpg)')
parser.add_argument('-v', '--verbose', action='count', dest='verbose',
                    default=0)
parser.add_argument('--test', action='store_true',
                    help='print arguments and parse results, then exit with '
                         'error')
parser.add_argument('-V', '--version', action='version',
                    version='%(prog)s %(__version__)')

# list of channel names
parser.add_argument('--chan',  nargs='+', action='append', required=True,
                    help='Two or more channel names, first one is compared to all the others')
parser.add_argument('--start', nargs='+', action='append', required=True,
                    help='Starting GPS times(required)')
parser.add_argument('--duration', default=10, help='Duration (seconds) [10]')
parser.add_argument('--fftlen', help='length of fft in seconds for coh calculation [duration]')
parser.add_argument('--ovlap', help='Overlap as fraction [0-1)')
argList = parser.parse_args()

if argList.test or argList.verbose > 0:
    print('Dump arguments, parsing, and exit requested.')
    print('sys.argv:')
    print('    %s' % sys.argv)
    print('argList:')
    print('   %s' % argList)
if argList.test:
    sys.exit(3)

# -----------------------------------------------------------------------------
# Configure plot

# set rcParams
rcParams.update({
    'figure.dpi': 100.,
    'font.family': 'sans-serif',
    'font.size': 16.,
    'font.weight': 'book',
    'legend.loc': 'best',
    'lines.linewidth': 1.5,
})

# determine image dimensions (geometry)
width=1200
height=768
if (argList.geometry):
    try:
        width, height = map(float, argList.geometry.split('x', 1))
        height = max(height, 500)
    except (TypeError, ValueError) as e:
        e.args = ('Cannot parse --geometry as WxH, e.g. 1200x600',)
        raise

dpi = rcParams['figure.dpi']
xinch=width / dpi
yinch=height / dpi
rcParams['figure.figsize'] = (xinch, yinch)

# retrieve channel data from NDS as a TimeSeries
chanList=[]
if len(argList.chan) > 1:
    for chan in argList.chan:
        chanList.append(chan[0])

startArg = argList.start
while type(startArg) is list:
    startArg = startArg[0]

start=int(startArg)
dur=int(argList.duration)
verb = argList.verbose > 0
timeSeries=[]
minMax=[]

if len(chanList) > 1 and start and dur:
    for chan in chanList:
        if verb:
            print ('Fetching %s %d, %d' % (chan,start,dur))
        data = TimeSeries.fetch(chan,start,start+dur,verbose=verb)
        if data.min() == data.max():
            print ('Data from %s has a constant value of %f.  Coherence can not be calculated.' % (chan, data.min()))
        else:
            timeSeries.append(data)
else:
    raise ValueError('Less than 2 channels, no start time, or no duration specified')

if len(timeSeries) < 2:
    raise ValueError('Less than 2 channels, no start time, or no duration specified')

fftlen = dur / 4
if argList.fftlen :
    fftlen = float(argList.fftlen)
ovlap = 0.5
if argList.ovlap:
    ovlap = float(argList.ovlap)

# calculate and plot the first pair, note the first channel is the reference channel
coh= timeSeries[0].coherence(timeSeries[1],fftlength=fftlen, overlap=ovlap*fftlen)

plot = coh.plot()

# if we have more time series calculate and add to the first plot
if len(timeSeries) > 2:
    for idx in range(2, len(timeSeries)):
        cohb = timeSeries[0].coherence(timeSeries[idx],fftlength=fftlen, overlap=ovlap*fftlen)
        plot.add_spectrum(cohb)

ax = plot.gca()

if argList.logy:
    ax.set_yscale('log')
else:
    ax.set_yscale('linear')

if argList.logf:
    ax.set_xscale('log')
else:
    ax.set_xscale('linear')

# scale the axes
ymin=0
ymax=1
if argList.ymin:
    ymin=argList.ymin
if argList.ymax:
    ymax=argList.ymax
ax.set_ylim(ymin,ymax)

fmin=numpy.min(coh.frequencies.data)
fmax=numpy.max(coh.frequencies.data)
if argList.fmin:
    fmin=float(argList.fmin)
if argList.fmax:
    fmax=float(argList.fmax)
ax.set_xlim(fmin,fmax)

ax.legend(prop={'size':10})
# since we only plot one at a time right now, remove the legend
#ax.legend_.remove()
#ax.legend().set_visible(False)

# add titles
title = ''
if argList.title:
    for t in argList.title:
        if len(title) > 0:
            title += "\n"
        title += t
# info on the processing
fs = data.channel.sample_rate.value
startGPS =Time(start, format='gps')
timeStr = "%s - %10d (%ds)" % (startGPS.iso, start, dur)

spec= r'%s, Fs=%d Hz, secpfft=%.1f, overlap=%.2f' % (timeStr, fs, fftlen, ovlap)
if len(title) > 0:
    title += "\n"
title += spec
plot.set_title(title, fontsize=12)

if argList.xlabel:
    xlabel=argList.xlabel
else:
    xlabel='Frequency (Hz)'
plot.xlabel=xlabel

if argList.ylabel:
    ylabel=argList.ylabel
else:
    ylabel=r'Coherence'
    if argList.logy:
        ylabel += ' log$_{10}$';

plot.ylabel=ylabel

if not argList.nogrid:
    ax.grid(b=True, which='major', color='k', linestyle='solid')
    ax.grid(b=True, which='minor', color='0.06', linestyle='dotted')

# info on the channel
if argList.suptitle:
    supTitle = argList.suptitle
else:
    supTitle = coh.name
supTitle = label_to_latex(supTitle)
#supTitle = r'$\mathrm{' + supTitle + r'}$'
plot.suptitle(supTitle,fontsize=14)

# save the figure. Note type depends on extension of output filename (png, jpg, pdf)
plot.savefig(argList.out, edgecolor='white',figsize=[xinch,yinch] , dpi=dpi)

if verb:
    print("Image written to %s" % argList.out)