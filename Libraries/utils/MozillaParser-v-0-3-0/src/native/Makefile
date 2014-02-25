DEST = ../../bin
OS := $(shell uname -s)

# a space character
nullstring :=
space := $(nullstring) # a space

GCC_CFLAGS = -fPIC -fno-rtti -fno-exceptions -fno-common -fshort-wchar  -Wall -Wconversion -Wpointer-arith -Wcast-align -Woverloaded-virtual -Wsynth -Wno-ctor-dtor-privacy -Wno-non-virtual-dtor -Wno-long-long

ifneq "" "$(findstring CYGWIN,$(OS))$(findstring MINGW,$(OS))"
    CC                  = cl
    CXX                 = cl
    LD                  = '$(SDKDIR)/VC/bin/link'
    MT                  = mt
    JLIBEXT             = .dll
    LIBEXT              = .dll
    JLIBPREFIX          = 
    LIBPREFIX           = 
    LIBPATH             = PATH
    PATH_SEPARATOR      = ;
    PATH_DELIMITER      = \\
    OBJEXT              = .obj
    SHAREDFLAG          = -dll
    EXTRA_CFLAGS       += -MD -DWINDOWS=1 -nologo -I./win32
    OPTFLAGS            = -O2
    INCLUDEFLAG         = -FI
    LIBDIRFLAG          = -libpath:
    LIBFLAG             = -defaultlib:
    OUTFLAG             = -out:
    LDFLAGS            += -nologo -manifest
    USE_IPC             = 0
    WINDOWS             = 1
else
    ifeq "$(OS)" "Darwin"
        JLIBEXT         = .jnilib
        LIBEXT          = .dylib
        LIBPATH         = DYLD_LIBRARY_PATH
        SHAREDFLAG      = -dynamiclib
        EXTRA_CFLAGS   += -arch x86_64 -arch i386
        EXTRA_LDFLAGS  += -arch x86_64 -arch i386
    else
        JLIBEXT         = .so
        LIBEXT          = .so
        LIBPATH         = LD_LIBRARY_PATH
        SHAREDFLAG      = -shared
    endif

    LD                  = $(CXX)
    JLIBPREFIX          = lib
    LIBPREFIX           = lib
    PATH_SEPARATOR      = :
    PATH_DELIMITER      = /
    OBJEXT              = .o
    INCLUDEFLAG         = -include
    LIBDIRFLAG          = -L
    LIBFLAG             = -l
    OUTFLAG             = -o$(space)
#   OPTFLAGS            = -g -O0
    OPTFLAGS            = -O3
    EXTRA_CFLAGS       += $(GCC_CFLAGS)
    USE_IPC             = 1
    WINDOWS             = 0
endif

SOURCES   = $(wildcard *.cpp)
OBJS      = $(SOURCES:.cpp=$(OBJEXT))
CHILD_BIN = parserWorker

MOZLIBS   = $(LIBDIRFLAG)$(MOZ_HOME)/lib $(LIBDIRFLAG)$(MOZ_HOME)/bin $(LIBFLAG)xpcom $(LIBFLAG)xpcom_core $(LIBFLAG)plds4 $(LIBFLAG)plc4 $(LIBFLAG)nspr4

ifeq "$(USE_IPC)" "1"
    BINOBJS     = $(SOURCES:.cpp=$(OBJEXT)) main.o
    LIBOBJS     = MozillaParser$(OBJEXT) IPC$(OBJEXT) ProcessDescriptor$(OBJEXT) ProcessPool$(OBJEXT) Semaphore$(OBJEXT) Lock$(OBJEXT) util$(OBJEXT)
    LDFLAGS_LIB = $(LDFLAGS) $(EXTRA_LDFLAGS)
    LDFLAGS_BIN = $(LDFLAGS) $(MOZLIBS)
else
    BINOBJS     = 
    LIBOBJS     = $(filter-out main$(OBJEXT),$(OBJS))
    LDFLAGS_LIB = $(LDFLAGS) $(MOZLIBS)
    LDFLAGS_BIN = $(LDFLAGS)
endif

ifeq "$(WINDOWS)" "1"
# Convert POSIX path to Windows path
MOZ_BIN  := '$(subst \,\\,$(shell cd $(MOZ_HOME)/bin && cmd /c cd))'
else
MOZ_BIN   = $(MOZ_HOME)/bin
endif

INCPATH  = $(JDK_HOME)/include $(JDK_HOME)/include/linux $(foreach moz,$(MOZ_HOME),$(shell find $(MOZ_HOME) -type d ! -name CVS)) $(MOZ_HOME)/../parser/htmlparser/src

MYFLAGS := $(foreach d,$(INCPATH),-I$(d)) $(INCLUDEFLAG) $(MOZ_HOME)/include/mozilla-config.h \
           $(OPTFLAGS) \
           -DMOZILLA_INTERNAL_API \
           -DLIB_PREFIX=$(LIBPREFIX) \
           -DLIB_SUFFIX=$(LIBEXT) \
           -DLIB_PATH=$(LIBPATH) \
           -DPATH_SEPARATOR='$(PATH_SEPARATOR)' \
           -DPATH_DELIMITER='$(PATH_DELIMITER)' \
           $(EXTRA_CFLAGS)

ifeq "$(USE_IPC)" "1"
CHILD_BIN_TARGET = $(DEST)/$(CHILD_BIN)
MYFLAGS += -DMOZILLAPARSER_USE_IPC \
           -DCHILD_BIN=$(CHILD_BIN)
endif

CFLAGS   += $(MYFLAGS)
CXXFLAGS += $(MYFLAGS)

LIBTOOL  = libtool

all: start $(CHILD_BIN_TARGET) $(DEST)/$(JLIBPREFIX)MozillaParser$(JLIBEXT) finish

check_vars: check_vars1 check_vars2 check_vars3
check_vars1:
check_vars2:
check_vars3:

ifeq "$(JDK_HOME)" ""
check_vars1: ; $(error The JDK_HOME environment variable is undefined!  Please point this variable to your JDK installation)
endif

ifeq "$(MOZ_HOME)" ""
check_vars2: ; $(error The MOZ_HOME environment variable is undefined!  Please point this variable to the 'dist' subdirectory of the Mozilla codebase)
endif

start: check_vars
	@echo "Building MozillaParser library using:"
	@echo "    JDK_HOME: $(JDK_HOME)"
	@echo "    MOZ_HOME: $(MOZ_HOME)"
	@echo "------------------------------------------------------------------------"

finish:
	@echo ""
	@echo "Finished."

clean:
	rm -f *$(OBJEXT) $(JLIBPREFIX)MozillaParser$(JLIBEXT) $(CHILD_BIN)

$(JLIBPREFIX)MozillaParser$(JLIBEXT): $(LIBOBJS)
	@mkdir -p $(dir $@)
	@echo "  [LD] Linking to $@..."
	@$(LD) $(LDFLAGS_LIB) $(SHAREDFLAG) $(OUTFLAG)$@ $^
ifeq "$(WINDOWS)" "1"
	@echo "  [MT] Embedding manifest information in $@..."
	@$(MT) -nologo -manifest '$@.manifest' -outputresource:'$@;#2'
endif

$(CHILD_BIN): $(BINOBJS)
	@mkdir -p $(dir $@)
	@echo "  [LD] Linking to $@..."
	@$(CXX) -o $@ $^ $(LDFLAGS_BIN)

$(DEST)/%: %
	@mkdir -p $(DEST)
	@echo "  [CP] Copying $< to $@..."
	@cp -p $< $@

%$(OBJEXT): %.cpp
	@echo "  [CC] Compiling $< to $@..."
	@$(CXX) -c $(OUTFLAG)$@ $(CPPFLAGS) $(CXXFLAGS) $^

.PHONY: start finish check_vars clean
