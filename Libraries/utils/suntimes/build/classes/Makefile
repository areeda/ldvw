LIBDIR=/usr/lib

all:
	javac com/web_tomorrow/utils/javapopt/*.java

doc:
	javadoc -d docs -overview popt_overview.html -author -use -windowtitle "Java Popt documentation" -header "<b>Java Popt documentation</b>" -footer "(c)2001 Kevin Boone/Web-Tomorrow" com.web_tomorrow.utils.javapopt 

jar:
	jar cvf javapopt.jar *

install:
	cp -p javapopt.jar $(LIBDIR)
