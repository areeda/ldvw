__author__ = 'joseph areeda'
__version__ = '0.0.0'

import argparse

def getargs(args):
    parser = argparse.ArgumentParser(description='General X-Y plot routine', prefix_chars='-')
    
    parser.add_argument('-g', '--geometry', 
        help='size of resulting image WxH eg 1200x600')
    
    parser.add_argument('--interactive', action='store_true' )
    parser.add_argument('--logy', action='store_true' )
    parser.add_argument('--logx', action='store_true') 
    parser.add_argument('--nogrid', action='store_true') 
    parser.add_argument('--title', action='append' , help='One or more title lines')

    parser.add_argument('--xlabel', help='X-axis label')
    parser.add_argument('--ylabel', help='Y-axis label')
    parser.add_argument('--xmin', help='min value for X-axis')
    parser.add_argument('--ymin', help='min value for Y-axis')
    parser.add_argument('--xmax', help='max value for X-axis')
    parser.add_argument('--ymax', help='max value for Y-axis')

    parser.add_argument('--out', help='output filename, type=ext (png, pdf, jpg)')

    parser.add_argument('-v', '--verbose', action='count',dest='verbose', default=0)
    parser.add_argument('--test', action='store_true', help='print arguments and parse results, then exit with error')

    parser.add_argument('-V', '--version', action='version', 
                        version='%(prog)s %(__version__)')
    
    # positional arguments are data files for ovlaid or subplots
    parser.add_argument('--infile', nargs='+', action='append',
                   help='path to data files 2xN array of doubles')

    result = parser.parse_args(args)
    
    return result

