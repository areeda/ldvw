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
CND_CONF=Release
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/736410056/Channel.o \
	${OBJECTDIR}/_ext/736410056/NdsException.o \
	${OBJECTDIR}/_ext/736410056/TimeInterval.o


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
LDLIBSOPTIONS=

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libndsswig2.a

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libndsswig2.a: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libndsswig2.a
	${AR} -rv ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libndsswig2.a ${OBJECTFILES} 
	$(RANLIB) ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libndsswig2.a

${OBJECTDIR}/_ext/736410056/Channel.o: /home/joe/NetBeansProjects/ldvw/Applications/NdsSwig2/Channel.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/736410056
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/736410056/Channel.o /home/joe/NetBeansProjects/ldvw/Applications/NdsSwig2/Channel.cpp

${OBJECTDIR}/_ext/736410056/NdsException.o: /home/joe/NetBeansProjects/ldvw/Applications/NdsSwig2/NdsException.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/736410056
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/736410056/NdsException.o /home/joe/NetBeansProjects/ldvw/Applications/NdsSwig2/NdsException.cpp

${OBJECTDIR}/_ext/736410056/TimeInterval.o: /home/joe/NetBeansProjects/ldvw/Applications/NdsSwig2/TimeInterval.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/736410056
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/736410056/TimeInterval.o /home/joe/NetBeansProjects/ldvw/Applications/NdsSwig2/TimeInterval.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libndsswig2.a

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
