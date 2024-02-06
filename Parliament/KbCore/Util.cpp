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
#include "parliament/UnicodeIterator.h"
#include "parliament/Version.h"
#include "parliament/Windows.h"

#include <memory>
#include <string>
#include <vector>

#if !defined(PARLIAMENT_WINDOWS)
#	if defined(PARLIAMENT_LINUX) && !defined(_GNU_SOURCE)
#		define _GNU_SOURCE
#	endif
#	include <dlfcn.h>
#endif

#if defined(PARLIAMENT_MACOS)
#	define _DARWIN_BETTER_REALPATH
#endif
#include <stdlib.h>

#if !defined(PARLIAMENT_WINDOWS) && !defined(PARLIAMENT_SOLARIS)
#	include <sys/time.h>
#endif

namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::string;
using ::std::unique_ptr;

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
						% convertTCharToUtf8(pVarName) % numChars % errCode);
				PMNT_LOG(g_log, log::Level::error) << errMsg;
				throw Exception(errMsg);
			}
		}
		else if (numChars < size(buffer))
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

bfs::path pmnt::getCurrentDllFilePath()
{
#if defined(PARLIAMENT_WINDOWS)
	HMODULE hModule = 0;
	if (!::GetModuleHandleEx(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS,
		reinterpret_cast<LPCTSTR>(getCurrentDllFilePath), &hModule))
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to retrieve the module handle:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}

	for (DWORD bufferLen = MAX_PATH;; bufferLen += MAX_PATH)
	{
		::std::vector<TChar> buffer(bufferLen, '\0');
		DWORD retVal = ::GetModuleFileName(hModule, &buffer[0], bufferLen);
		if (retVal == 0)
		{
			SysErrCode errCode = Exception::getSysErrCode();
			throw Exception(format("Unable to retrieve the module file name:  %1% (%2%)")
				% Exception::getSysErrMsg(errCode) % errCode);
		}
		else if (retVal < bufferLen)
		{
			return &buffer[0];
			break;
		}
	}
#else
	::Dl_info info;
	if (::dladdr(reinterpret_cast<const void*>(getCurrentDllFilePath), &info) == 0)
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to retrieve the shared library file name:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}

	auto deleter = [](char* p){ ::free(p); };
	char* pRawPtr = ::realpath(info.dli_fname, nullptr);
	if (pRawPtr == nullptr)
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Error calling realpath:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}
	unique_ptr<char, decltype(deleter)> pPath(pRawPtr, deleter);
	return pPath.get();
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
