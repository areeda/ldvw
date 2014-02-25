rm -v bin/*

MOZILLA_SRC_HOME=/home/ohad/mozilla/
JDK_HOME=/usr/lib/j2sdk1.5-sun/

c++ -o bin/MozillaParser.so -shared  -I$MOZILLA_SRC_HOME/dist/include/system_wrappers -DMOZILLA_INTERNAL_API -DOSTYPE=\"Linux2.6.9-42.0.3\" -DOSARCH=\"Linux\" -DBUILD_ID=0000000000 -I$MOZILLA_SRC_HOME/parser/htmlparser/src -I$MOZILLA_SRC_HOME/parser/htmlparser/public -I$MOZILLA_SRC_HOME/dist/include/unicharutil -I$MOZILLA_SRC_HOME/dist/include/uconv/ -I$MOZILLA_SRC_HOME/dist/include/util  -I$JDK_HOME/include/ -I$JDK_HOME/include/linux -I$MOZILLA_SRC_HOME/dist/include/xpcom -I$MOZILLA_SRC_HOME/dist/include/string -I$MOZILLA_SRC_HOME/dist/include/necko -I$MOZILLA_SRC_HOME/dist/include/content -I$MOZILLA_SRC_HOME/dist/include -I$MOZILLA_SRC_HOME/dist/include -I$MOZILLA_SRC_HOME/dist/include/nspr    -I/usr/X11R6/include   -fPIC  -I/usr/X11R6/include -I/usr/X11R6/include -DMOZILLA_CLIENT  -L$MOZILLA_SRC_HOME/dist/bin/ -lxpcom -L$MOZILLA_SRC_HOME/dist/bin/components/ -lhtmlpars -include $MOZILLA_SRC_HOME/mozilla-config.h src/JavaContentSink.cpp src/MozillaParser.cpp

