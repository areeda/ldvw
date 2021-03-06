#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=GNU-MacOSX
CND_DLIB_EXT=dylib
CND_CONF=Debug
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/Channels.o \
	${OBJECTDIR}/NDSConnection.o \
	${OBJECTDIR}/NDSData.o \
	${OBJECTDIR}/NDSException.o \
	${OBJECTDIR}/ProxyServer.o \
	${OBJECTDIR}/ServerThread.o \
	${OBJECTDIR}/Utils.o \
	${OBJECTDIR}/main.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=-O0
CXXFLAGS=-O0

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=-L/usr/lib64 -L/usr/lib/sasl2 -L/usr/lib/x86_64-linux-gnu -lboost_regex -lpthread -lrt /usr/lib64/libndsclient.a -lsasl2 -lstdc++

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/ndsproxyserver

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/ndsproxyserver: /usr/lib64/libndsclient.a

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/ndsproxyserver: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.cc} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/ndsproxyserver ${OBJECTFILES} ${LDLIBSOPTIONS}

${OBJECTDIR}/Channels.o: nbproject/Makefile-${CND_CONF}.mk Channels.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -g -Wall -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/Channels.o Channels.cpp

${OBJECTDIR}/NDSConnection.o: nbproject/Makefile-${CND_CONF}.mk NDSConnection.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -g -Wall -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/NDSConnection.o NDSConnection.cpp

${OBJECTDIR}/NDSData.o: nbproject/Makefile-${CND_CONF}.mk NDSData.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -g -Wall -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/NDSData.o NDSData.cpp

${OBJECTDIR}/NDSException.o: nbproject/Makefile-${CND_CONF}.mk NDSException.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -g -Wall -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/NDSException.o NDSException.cpp

${OBJECTDIR}/ProxyServer.o: nbproject/Makefile-${CND_CONF}.mk ProxyServer.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -g -Wall -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/ProxyServer.o ProxyServer.cpp

${OBJECTDIR}/ServerThread.o: nbproject/Makefile-${CND_CONF}.mk ServerThread.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -g -Wall -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/ServerThread.o ServerThread.cpp

${OBJECTDIR}/Utils.o: nbproject/Makefile-${CND_CONF}.mk Utils.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -g -Wall -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/Utils.o Utils.cpp

${OBJECTDIR}/main.o: nbproject/Makefile-${CND_CONF}.mk main.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -g -Wall -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/main.o main.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/ndsproxyserver

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
