#!/usr/bin/python

import time
import nds2
import sys

servers=['nds.ligo-la.caltech.edu','nds.ligo-wa.caltech.edu',
         'nds.ligo.caltech.edu', 'nds2.ligo.caltech.edu',
         'ldas-pcdev1.ligo-wa.caltech.edu','ldas-pcdev1.ligo-la.caltech.edu',
         'nds40.ligo.caltech.edu']

for srv in servers :
    try :    
        strt=time.time()
        l=nds2.connection(srv)
        conTime=time.time() - strt
        print ('Connection: %.03f sec - %s' % (conTime, srv))
        
        strt=time.time()
        t=l.find_channels('L1:IMC-F_OUT_DQ')
        fndTime=time.time() - strt
        print ('    FindChannels: %.03f sec' % fndTime)
    except :
        e = sys.exc_info()[0]
        print ('==== Error getting channel list from %s' % srv)
