# Makefile for SunTimes and supporting classes. You will probably need to
# modify this to suit your system. Note that the SunTimes test client requires
# JavaPopt but the SunTimes class itself does not 

LIBDIR=/usr/lib
BINDIR=/usr/bin
CP=cp
JAR=jar
WARDIR=/home/kevin/source/tomcat/jakarta-tomcat-3.2.1/webapps

all:
	javac -d WEB-INF/classes com/web_tomorrow/utils/suntimes/*.java com/web_tomorrow/utils/datetime/*.java com/web_tomorrow/suntimes_web/*.java

doc:
	javadoc -d docs -overview suntimes_overview.html -author -use -windowtitle "SunTimes documentation" -header "<b>SunTimes documentation</b>" -footer "(c)2001 Kevin Boone/Web-Tomorrow" com.web_tomorrow.utils.suntimes com.web_tomorrow.utils.datetime

war: 
	$(JAR) cvf suntimes.war WEB-INF *.jsp *.gif 

install:
	$(CP) suntimes.war $(WARDIR)

testinstall:
	$(CP) -vpr  *.jsp *.gif $(WARDIR)/suntimes

dist:
	tar cvfz suntimes_web-1.0-bin.tar.gz *.war *.html

