// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/Util.h"
#include "parliament/Exceptions.h"
#include "parliament/Types.h"
#include "parliament/Version.h"
#include "parliament/Windows.h"

#include <cstring>
#include <locale>
#include <sstream>
#include <cstdlib>
#include <string>

#include <boost/algorithm/string/predicate.hpp>

#if !defined(PARLIAMENT_WINDOWS) && !defined(PARLIAMENT_SOLARIS)
#	include <sys/time.h>
#endif

namespace pmnt = ::bbn::parliament;

using ::boost::algorithm::ends_with;
using ::boost::algorithm::starts_with;

using ::std::char_traits;
using ::std::ctype;
using ::std::locale;
using ::std::ostringstream;
using ::std::string;
using ::std::use_facet;



// ========================================================
//
// Global functions
//
// ========================================================

string pmnt::getKbVersion()
{
	return PARLIAMENT_VERSION_STRING;
}

pmnt::TString pmnt::tGetEnvVar(const TChar* pVarName)
{
	const TChar* pEnvVarValue =
#if defined(PARLIAMENT_WINDOWS) && defined(UNICODE)
		::_wgetenv(pVarName);
#else
		::getenv(pVarName);
#endif
	return (pEnvVarValue == nullptr) ? TString() : TString(pEnvVarValue);
}



// ========================================================
//
// HiResTimer class
//
// ========================================================

#if defined(PARLIAMENT_WINDOWS)
static int initPerfCounterFrequency();

static bool g_usePerfCounter = false;
static pmnt::uint64 g_perfCounterFrequency = 0;
static int g_forceInit = initPerfCounterFrequency();

static int initPerfCounterFrequency()
{
	g_usePerfCounter = !!::QueryPerformanceFrequency(
		reinterpret_cast<LARGE_INTEGER*>(&g_perfCounterFrequency));
	return 1;
}
#endif

pmnt::HiResTimer::HiResTime pmnt::HiResTimer::getHiResTime()
{
#if defined(PARLIAMENT_WINDOWS)
	if (g_usePerfCounter)
	{
		uint64 perfCount;
		if (!::QueryPerformanceCounter(reinterpret_cast<LARGE_INTEGER*>(&perfCount)))
		{
			SysErrCode errCode = Exception::getSysErrCode();
			string errMsg = Exception::getSysErrMsg(errCode);
			ostringstream s;
			s << "The Windows API QueryPerformanceCounter failed:  " << errMsg << " (" << errCode << ")";
			throw Exception(s.str());
		}
		return perfCount;
	}
	else
	{
		FILETIME t;
		GetSystemTimeAsFileTime(&t);
		return * reinterpret_cast<HiResTime*>(&t);
	}
#elif defined(PARLIAMENT_SOLARIS)
	return gethrtime();
#else
	struct timeval t;
	if (gettimeofday(&t, nullptr) != 0)
	{
		SysErrCode errCode = Exception::getSysErrCode();
		string errMsg = Exception::getSysErrMsg(errCode);
		ostringstream s;
		s << "The POSIX API gettimeofday failed:  " << errMsg << " (" << errCode << ")";
		throw Exception(s.str());
	}
	return ((uint64) t.tv_sec * 1000000) + (uint64) t.tv_usec;
#endif
}

pmnt::uint64 pmnt::HiResTimer::getUnitsPerSec()
{
#if defined(PARLIAMENT_WINDOWS)
	return g_usePerfCounter ? g_perfCounterFrequency : 10 * 1000 * 1000;
#elif defined(PARLIAMENT_SOLARIS)
	return 1000 * 1000 * 1000;
#else
	return 1 * 1000 * 1000;
#endif
}
