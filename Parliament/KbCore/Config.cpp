// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/Config.h"
#include "parliament/Exceptions.h"
#include "parliament/Log.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"
#include "parliament/Windows.h"

#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/filesystem/fstream.hpp>
#include <boost/filesystem/operations.hpp>
#include <boost/format.hpp>
#include <string>
#include <vector>

namespace ba = ::boost::algorithm;
namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::begin;
using ::std::end;
using ::std::getline;
using ::std::string;

static constexpr const char*const k_trueBoolValues[] = { "true", "t", "yes", "y", "on", "1" };
static constexpr const char*const k_falseBoolValues[] = { "false", "f", "no", "n", "off", "0" };

static auto g_log(pmnt::log::getSource("Config"));



// =========================================================================
//
// Helper functions to smooth differences between Windows and POSIX
//
// =========================================================================

#if defined(PARLIAMENT_WINDOWS)

PARLIAMENT_NAMESPACE_BEGIN

static HMODULE getCurrentModuleHandle()
{
	HMODULE hModule = 0;
	if (!::GetModuleHandleEx(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS,
		reinterpret_cast<LPCTSTR>(getCurrentModuleHandle), &hModule))
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to retrieve the module handle:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}
	return hModule;
}

static bfs::path getModulePathName(HMODULE hModule)
{
	for (DWORD bufferLen = 256;; bufferLen += 256)
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
}

PARLIAMENT_NAMESPACE_END

#endif

bfs::path pmnt::Config::getConfigFilePath(const TChar* pEnvVarName, const TChar* pDefaultConfigFileName)
{
	auto envVarValue = tGetEnvVar(pEnvVarName);
	if (!envVarValue.empty())
	{
		return envVarValue;
	}
	else
	{
#if defined(PARLIAMENT_WINDOWS)
		HMODULE hModule = getCurrentModuleHandle();
		auto configPath = getModulePathName(hModule).parent_path();
		configPath /= pDefaultConfigFileName;
		if (exists(configPath) && is_regular_file(configPath))
		{
			return configPath;
		}
		else
		{
			PMNT_LOG(g_log, log::Level::debug) << "Computed config file path '"
				<< pathAsUtf8(configPath) << "' does not exist or is not a regular file."
				"  Defaulting to the current directory.";
		}
#endif

		return pDefaultConfigFileName;
	}
}



// =========================================================================
//
// Config implementation
//
// =========================================================================

pmnt::Config::Config() = default;
pmnt::Config::Config(const Config&) = default;
pmnt::Config& pmnt::Config::operator=(const Config&) = default;
pmnt::Config::Config(Config&&) = default;
pmnt::Config& pmnt::Config::operator=(Config&&) = default;
pmnt::Config::~Config() = default;

void pmnt::Config::readFromFile()
{
	auto configPath = getConfigFilePath(getEnvVarName(), getDefaultConfigFileName());
	if (!exists(configPath))
	{
		throw Exception(format("The configuration file \"%1%\" does not exist")
			% configPath.generic_string());
	}
	else if (!is_regular_file(configPath))
	{
		throw Exception(format("The configuration file \"%1%\" is not a regular file")
			% configPath.generic_string());
	}

	bfs::ifstream in(configPath, ::std::ios::in);
	if (!in || !in.is_open())
	{
		throw Exception(format("Unable to open the configuration file \"%1%\"")
			% configPath.generic_string());
	}

	for(uint32 lineNum = 1;; ++lineNum)
	{
		string line;
		getline(in, line);
		if (!in)
		{
			break;
		}
		else if (!isBlankOrCommentLine(line))
		{
			string key, value;
			splitAtFirstEquals(line, lineNum, key, value);

			const auto& ceMap = getConfigEntryMap();
			auto it = ceMap.find(key);
			if (it == end(ceMap))
			{
				throw Exception(format(
					"Illegal configuration file syntax:  unrecognized key '%1%' on line %2% of file '%3%' in class '%4%'; map has size %5%")
					% key % lineNum % absolute(configPath).generic_string() % typeid(*this).name() % ceMap.size());
			}
			else
			{
				it->second(value, lineNum, *this);
			}
		}
	}
}

bool pmnt::Config::isBlankOrCommentLine(const string& line)
{
	size_t firstNonBlankPos = line.find_first_not_of(" \t\r\n");
	return firstNonBlankPos == string::npos || line[firstNonBlankPos] == '#';
}

void pmnt::Config::splitAtFirstEquals(/* in */ const string& input,
	/* in */ uint32 lineNum, /* out */ string& key, /* out */ string& value)
{
	string::size_type equalsPos = input.find_first_of('=');
	if (equalsPos == string::npos)
	{
		throw Exception(format("Illegal configuration file syntax:  missing '=' on line %1%")
			% lineNum);
	}
	else
	{
		key = ba::trim_copy(input.substr(0, equalsPos));
		value = (equalsPos == input.length())
			? string()
			: ba::trim_copy(input.substr(equalsPos + 1));
	}
}

size_t pmnt::Config::parseUnsigned(const string& str, uint32 lineNum)
{
	string str2 = ba::trim_copy(str);
	char* pEnd;
	size_t result = strtoul(str2.c_str(), &pEnd, 10);
	if (str2.length() == 0 || str2[0] == '-' || *pEnd != '\0')
	{
		throw Exception(format(
			"Illegal configuration file syntax:  ill-formed integer '%1%' on line %2%")
			% str2 % lineNum);
	}
	return result;
}

double pmnt::Config::parseDouble(const string& str, uint32 lineNum)
{
	string str2 = ba::trim_copy(str);
	char* pEnd;
	double result = strtod(str2.c_str(), &pEnd);
	if (str2.length() == 0 || *pEnd != '\0')
	{
		throw Exception(format(
			"Illegal configuration file syntax:  ill-formed floating point number '%1%' on line %2%")
			% str2 % lineNum);
	}
	return result;
}

template<typename T, ::std::size_t N>
static bool doesAnyMatch(const string& exemplar, T(&matchList)[N])
{
	return ::std::any_of(begin(matchList), end(matchList),
		[&exemplar](const char*const pStr) { return ba::iequals(exemplar, pStr); });
}

bool pmnt::Config::parseBool(const string& str, uint32 lineNum)
{
	string s2 = ba::trim_copy(str);
	if (doesAnyMatch(s2, k_trueBoolValues))
	{
		return true;
	}
	else if (doesAnyMatch(s2, k_falseBoolValues))
	{
		return false;
	}
	else
	{
		throw Exception(format(
			"Illegal configuration file syntax:  ill-formed Boolean value '%1%' on line %2%")
			% str % lineNum);
	}
}
