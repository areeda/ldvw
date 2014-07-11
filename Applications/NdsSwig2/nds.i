/*
 * Copyright (C) 2014 Joseph Areeda <joseph.areeda at ligo.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * A set of low level interfaces to the NDS2 libraries for Java, Python, Matlab and Octave
 * The goal is to expose all useful (from the application) functions in the ND2 library
 */

%module nds

/* Tell swig to include the header file in the C wrapper that it is creating,
 * but don't process the declarations in it yet.
 */
%{
/*
 * FIXME: The Octave headers define these macros. This is probably a bug in
 * Octave---it needs these internally, but it should not expose them in its API.
 * This project, like all Autotools-based projects, also defines these macros,
 * which causes a preprocessor warning.
 */
#undef PACKAGE_NAME
#undef PACKAGE_STRING
#undef PACKAGE_TARNAME
#undef PACKAGE_VERSION

/* Includes for our C++ wrapper functions*/
#include <NdsException.h>
#include <TimeInterval.h>
%}

/* Includes for our C++ wrapper functions*/
%include <NdsException.h>
%include <TimeInterval.h>
        
        
/* Some built-in swig interface files that we will need. */
%include <exception.i>
%include <constraints.i>
%include "std_string.i"
        
/* Target language specific includes*/

#ifdef SWIGJAVA
%typemap(javapackage) nds;
#elif defined(SWIGPYTHON)
/* %include "nds_python.i" */
#elif defined(SWIGOCTAVE)
/* %include "nds_octave.i" */
#endif

/* Exported Classes, these make up the API in each language */
/*
class NdsException
{
  public:
    NDSException();
    NDSException(int rc);
    NDSException(int rc, const char* msg);
    NDSException(const NDSException& orig);
    virtual ~NDSException() throw();

    const char* what() const throw();
};

class TimeInterval
{
  public:
	TimeInterval();
    TimeInterval(string epoch) ;
    TimeInterval(long start, long stop);
    TimeInterval(const TimeInterval& orig);
    virtual ~TimeInterval();
    long getDuraton();
    long getStartGps();
    long getStopGps();
    string getEpochName();
};
*/
