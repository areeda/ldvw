#!/usr/bin/env python
__author__ = 'joseph areeda'
__email__ = 'joseph.areeda@ligo.org'
__version__ = '0.0.0'

from distutils.version import LooseVersion, StrictVersion

packages=[ \
        ['numpy', '1.5.0' ],
        ['argparse', '1.2.1'],
        ['glue', '0.0.0'],
        ['dateutil', '0.0.0'],
        ['matplotlib', '1.3.0'],
        ['scipy', '0.14.0'],
        ['astropy', '0.3.0'],
        ['nds2', '0.0.0'], 
        ['gwpy', '0.1a10']
    ]

for pkg in packages:
    print("checking package: %20s min version: %-8s" % ( pkg[0], pkg[1])),
    try :
        x = __import__ (pkg[0])
    except:
        print("    not available.")
        continue
    if hasattr(x, '__version__') and x.__version__:
        print ("installed: %s" % x.__version__),
        if LooseVersion(x.__version__) < LooseVersion(pkg[1]):
            print (" UPDATE REQUIRED")
        else:
            print ("OK")
    else:
        print("version unknown")



