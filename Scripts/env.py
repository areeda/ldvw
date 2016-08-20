#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

env = os.environ
for e in env:
    print e, ' = ', env[e],'<br>'

