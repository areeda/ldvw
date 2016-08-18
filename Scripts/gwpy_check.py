#!/usr/bin/env python
__author__ = 'joseph areeda'
__email__ = 'joseph.areeda@ligo.org'
__version__ = '0.0.1'

from distutils.version import LooseVersion, StrictVersion
import sys
import os
import re

html = False
args = sys.argv

if len(args) > 1 and args[1] == 'html':
    html = True

required_versions = {
        'astropy': '0.4.0',
        'argparse': '1.1',
        'dateutil': '2.2',
        'gwpy': '0.1a10',
        'matplotlib': '1.3.0',
        'nds2-client': '0.11.0',
        'numpy': '1.7.0',
        'python': '2.6',
        'scipy': '0.11.0'
    }
found_versions = {}

# packages we can import and examine __version__
packages=[ 'numpy', 'argparse', 'dateutil', 'matplotlib',
           'scipy', 'astropy', 'gwpy', ]

missing = []

tbl_fmt = '| %11s | %8s | %13s | %15s |'
tbl_delim = '|-------------|----------|---------------|-----------------|'
is_odd = True

def add_titles():
    """ Add table titles
    :return:none
    """
    global html, tbl_fmt, tbl_delim

    if html:
        print("<table>")
        print('    <thead>')
        print('        <th>Module</th>')
        print('        <th>Required</th>')
        print('        <th>Installed</th>')
        print('        <th>Status</th>')
        print('    </thead>')
        print('    <tbody>')
    else:
        print(tbl_delim)
        print(tbl_fmt %
              ('Module', 'Required', 'Installed', 'Status'))
        print tbl_delim
    return

def add_row(pkg, req, ver, status):
    """ Add a row to the package satus table
    :param pkg: package name
    :param req: required version
    :param ver: installed version
    :param status: status
    :return: None
    """
    global html, tbl_fmt, tbl_delim, is_odd
    if html:
        if is_odd:
            print('    <tr class="odd">')
        else:
            print('    <tr class="even">')
        is_odd = not is_odd

        print('        <td>' + pkg + '</td>')
        print('        <td>' + req + '</td>')
        print('        <td>' + ver + '</td>')
        print('        <td>' + status + '</td>')
        print('    </tr>')
    else:
        print(tbl_fmt % (pkg, req, ver, status))
    return

def close_table():
    if html:
        print '    </tbody>'
        print '</table>'
    else:
        print(tbl_delim)
        print()
    return

def which(program):
    """ simulate linux which command
    :param program: name or full path to search for
    :return:None or path to program
    """

    def is_exe(fpath):
        return os.path.isfile(fpath) and os.access(fpath, os.X_OK)

    ret_path = None

    fpath, fname = os.path.split(program)
    if fpath:
        if is_exe(program):
            ret_path = program
    else:
        for path in os.environ["PATH"].split(os.pathsep):
            path = path.strip('"')
            exe_file = os.path.join(path, program)
            if is_exe(exe_file):
                ret_path = exe_file
    return ret_path

# ================= MAIN ==========================
for pkg in packages:

    try :
        x = __import__ (pkg)
    except:
        missing.append(pkg)
        continue
    if hasattr(x, '__version__'):
        found_versions[pkg] = x.__version__
    else:
        found_versions[pkg] = 'unknown'

# handle the weird ones
svi = sys.version_info
py_ver = '%d.%d.%d' % (svi[0], svi[1], svi[2])
found_versions['python'] = py_ver

try:
    from glue import git_version
    glue_version = git_version.tag.replace('-', '.').split('.', 2)[-1]
    found_versions['glue'] = glue_version
except:
    missing.append('glue')

nds_path = which('nds_query')
if nds_path:
    stream = os.popen(nds_path + ' --version')
    version_line = stream.readline()
    stream.close()

    m=re.search('([0-9]+.[0-9]+.[0-9]+.*)\n',version_line)

    if m:
        nds_version = m.group(1)
        found_versions['nds2-client'] = nds_version
    else:
        missing.append('nds2-client')
else:
    missing.append('nds2-client')

add_titles()

for pkg, req in required_versions.iteritems():
    status = 'Missing'
    ver = 'N/A'
    if pkg in found_versions:
        ver = found_versions[pkg]
        status = 'update needed'
        if LooseVersion(ver) >= LooseVersion(req):
            status = 'OK'
    add_row(pkg, req, ver, status)
close_table()
