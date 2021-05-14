// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/Util.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/Exceptions.h"
#include "parliament/Log.h"
#include "parliament/Types.h"
#include "parliament/Version.h"
#include "parliament/Windows.h"

#include <cstdlib>
#include <string>
#include <vector>

#if !defined(PARLIAMENT_WINDOWS) && !defined(PARLIAMENT_SOLARIS)
#	include <sys/time.h>
#endif

namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::string;

static auto g_log(pmnt::log::getSource("Util"));



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
#if defined(PARLIAMENT_WINDOWS)
#if defined(PARLIAMENT_UNIT_TEST)
	constexpr size_t k_bufferIncrement = 8;	// Force a retry with enlarged buffer at test time
#else
	constexpr size_t k_bufferIncrement = 512;
#endif
	for (DWORD bufferSize = k_bufferIncrement;; bufferSize += k_bufferIncrement)
	{
		::std::vector<TChar> buffer(bufferSize, _T('\0'));

		DWORD numChars = ::GetEnvironmentVariable(pVarName, &(buffer[0]), bufferSize);
		auto errCode = Exception::getSysErrCode();
		if (numChars == 0)
		{
			if (errCode == ERROR_ENVVAR_NOT_FOUND)
			{
				return TString();
			}
			else
			{
				auto errMsg = str(format{
					"GetEnvironmentVariable error: var = '%1%', numChars = %2%, error code = %3%"}
						% pVarName % numChars % errCode);
				PMNT_LOG(g_log, log::Level::error) << errMsg;
				throw Exception(errMsg);
			}
		}
		else if (numChars < buffer.size())
		{
			return TString(&(buffer[0]));
		}
		else
		{
			// The buffer is too small -- loop around and try again with a bigger buffer
		}
	}
#else
	const TChar* pEnvVarValue = ::getenv(pVarName);
	return (pEnvVarValue == nullptr) ? TString() : TString(pEnvVarValue);
#endif
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
			auto errCode = Exception::getSysErrCode();
			auto errMsg = str(format{"QueryPerformanceCounter error: %1% (%2%)"}
					% Exception::getSysErrMsg(errCode) % errCode);
			PMNT_LOG(g_log, log::Level::error) << errMsg;
			throw Exception(errMsg);
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
		auto errCode = Exception::getSysErrCode();
		auto errMsg = str(format{"gettimeofday error: %1% (%2%)"}
				% Exception::getSysErrMsg(errCode) % errCode);
		PMNT_LOG(g_log, log::Level::error) << errMsg;
		throw Exception(errMsg);
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
