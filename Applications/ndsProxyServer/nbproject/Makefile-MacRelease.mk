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
CND_PLATFORM=GNU-Linux-x86
CND_DLIB_EXT=so
CND_CONF=MacRelease
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/1140831991/nds.o \
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
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=-L/usr/lib/sasl2 -L/usr/lib/x86_64-linux-gnu -lndsclient -lboost_regex -lpthread -lrt -lsasl2 -lstdc++

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/ndsproxyserver

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/ndsproxyserver: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.cc} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/ndsproxyserver ${OBJECTFILES} ${LDLIBSOPTIONS}

${OBJECTDIR}/_ext/1140831991/nds.o: /home/joe/NetBeansProjects/ldvw/Applications/ndsProxyServer/nds.i 
	${MKDIR} -p ${OBJECTDIR}/_ext/1140831991
	${RM} "$@.d"
	$(COMPILE.c) -O2 -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1140831991/nds.o /home/joe/NetBeansProjects/ldvw/Applications/ndsProxyServer/nds.i

${OBJECTDIR}/Channels.o: Channels.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/Channels.o Channels.cpp

${OBJECTDIR}/NDSConnection.o: NDSConnection.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/NDSConnection.o NDSConnection.cpp

${OBJECTDIR}/NDSData.o: NDSData.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/NDSData.o NDSData.cpp

${OBJECTDIR}/NDSException.o: NDSException.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/NDSException.o NDSException.cpp

${OBJECTDIR}/ProxyServer.o: ProxyServer.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/ProxyServer.o ProxyServer.cpp

${OBJECTDIR}/ServerThread.o: ServerThread.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/ServerThread.o ServerThread.cpp

${OBJECTDIR}/Utils.o: Utils.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/Utils.o Utils.cpp

${OBJECTDIR}/main.o: main.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -I/usr/include/nds2-client -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/main.o main.cpp

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
