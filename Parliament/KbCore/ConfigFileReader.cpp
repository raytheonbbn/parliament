// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2022, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/ConfigFileReader.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/Exceptions.h"
#include "parliament/Util.h"

#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/filesystem/operations.hpp>
#include <boost/format.hpp>
#include <regex>
#include <string>

namespace ba = ::boost::algorithm;
namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::begin;
using ::std::end;
using ::std::getline;
using ::std::ifstream;
using ::std::make_pair;
using ::std::pair;
using ::std::string;
using TRegex = ::std::basic_regex<pmnt::TChar>;
using TSMatch = ::std::match_results<pmnt::TString::const_iterator>;

static constexpr const char*const k_trueBoolValues[] = { "true", "t", "yes", "y", "on", "1" };
static constexpr const char*const k_falseBoolValues[] = { "false", "f", "no", "n", "off", "0" };
static constexpr pmnt::TChar k_pmntDirRegExStr[] = _T(
	"^parliament-[0-9]+\\.[0-9]+\\.[0-9]+-((win)|(mac)|(ubuntu[0-9]+)|(rhel[0-9]+))(-d)?-((32)|(64))$");

static constexpr pmnt::TChar k_kbEnvVarName[] = _T("PARLIAMENT_KB_CONFIG_PATH");
static constexpr pmnt::TChar k_defaultKbConfigFileName[] = _T("ParliamentKbConfig.txt");

static constexpr pmnt::TChar k_logEnvVarName[] = _T("PARLIAMENT_LOG_CONFIG_PATH");
static constexpr pmnt::TChar k_defaultLogConfigFileName[] = _T("ParliamentLogConfig.txt");


void pmnt::ConfigFileReader::readFile(ConfigKind configKind, LineHandler lineHandler)
{
	auto in = openFile(configKind);
	for(uint32 lineNum = 1;; ++lineNum)
	{
		::std::string line;
		::std::getline(in, line);
		if (!in)
		{
			break;
		}
		else if (!isBlankOrCommentLine(line))
		{
			auto [key, value] = getKeyValueFromLine(line, lineNum);
			lineHandler(key, value, lineNum);
		}
	}
}

ifstream pmnt::ConfigFileReader::openFile(ConfigKind configKind)
{
	auto [ pEnvVarName, pDefaultConfigFileName ] = getEnvVarAndFileName(configKind);
	auto configPath = getConfigFilePath(pEnvVarName, pDefaultConfigFileName);
	if (!exists(configPath))
	{
		throw Exception(format("\"%1%\" does not exist") % configPath.generic_string());
	}
	else if (!is_regular_file(configPath))
	{
		throw Exception(format("\"%1%\" is not a regular file") % configPath.generic_string());
	}

	ifstream in(configPath.c_str(), ::std::ios::in);
	if (!in || !in.is_open())
	{
		throw Exception(format("Unable to open \"%1%\"") % configPath.generic_string());
	}

	return in;
}

pair<const pmnt::TChar*, const pmnt::TChar*> pmnt::ConfigFileReader::getEnvVarAndFileName(
	ConfigKind configKind)
{
	switch (configKind) {
	case ConfigKind::k_kb:
		return make_pair(k_kbEnvVarName, k_defaultKbConfigFileName);
	case ConfigKind::k_log:
		return make_pair(k_logEnvVarName, k_defaultLogConfigFileName);
	default:
		throw Exception(format("Unknown ConfigKind %1%") % static_cast<int>(configKind));
	}
}

bfs::path pmnt::ConfigFileReader::getConfigFilePath(const TChar* pEnvVarName,
	const TChar* pDefaultConfigFileName)
{
	auto envVarValue = tGetEnvVar(pEnvVarName);
	auto configFile = bfs::path(envVarValue);
	if (!envVarValue.empty() && exists(configFile) && is_regular_file(configFile))
	{
		return configFile;
	}

	auto configDir = getCurrentDllFilePath().parent_path();
	configFile = configDir / pDefaultConfigFileName;
	if (exists(configFile) && is_regular_file(configFile))
	{
		return configFile;
	}

	if (configDir.filename().native() == TString(_T("bin")))
	{
		configDir = configDir.parent_path();
		auto rex = TRegex{k_pmntDirRegExStr};
		TSMatch captures;
		if (regex_match(configDir.filename().native(), captures, rex))
		{
			auto testConfigDir = configDir.parent_path();
			if (configDir.filename().native() == TString(_T("target")))
			{
				testConfigDir /= _T("test-bin");
				auto testConfigFile = testConfigDir / pDefaultConfigFileName;
				if (exists(testConfigFile) && is_regular_file(testConfigFile))
				{
					return testConfigFile;
				}
			}
		}

		configFile = configDir / pDefaultConfigFileName;
		if (exists(configFile) && is_regular_file(configFile))
		{
			return configFile;
		}
	}

	return pDefaultConfigFileName;
}

bool pmnt::ConfigFileReader::isBlankOrCommentLine(const string& line)
{
	size_t firstNonBlankPos = line.find_first_not_of(" \t\r\n");
	return firstNonBlankPos == string::npos || line[firstNonBlankPos] == '#';
}

pair<string, string> pmnt::ConfigFileReader::getKeyValueFromLine(
	const string& line, uint32 lineNum)
{
	string::size_type equalsPos = line.find_first_of('=');
	if (equalsPos == string::npos)
	{
		throw Exception(format("Missing '=' on line %1%") % lineNum);
	}
	else
	{
		return make_pair(
			ba::trim_copy(line.substr(0, equalsPos)),
			(equalsPos == line.length())
				? string{}
				: ba::trim_copy(line.substr(equalsPos + 1)));
	}
}

size_t pmnt::ConfigFileReader::parseUnsigned(const string& str, uint32 lineNum)
{
	string str2 = ba::trim_copy(str);
	char* pEnd;
	size_t result = strtoul(str2.c_str(), &pEnd, 10);
	if (str2.length() == 0 || str2[0] == '-' || *pEnd != '\0')
	{
		throw Exception(format("Ill-formed integer '%1%' on line %2%") % str2 % lineNum);
	}
	return result;
}

double pmnt::ConfigFileReader::parseDouble(const string& str, uint32 lineNum)
{
	string str2 = ba::trim_copy(str);
	char* pEnd;
	double result = strtod(str2.c_str(), &pEnd);
	if (str2.length() == 0 || *pEnd != '\0')
	{
		throw Exception(format("Ill-formed number '%1%' on line %2%") % str2 % lineNum);
	}
	return result;
}

template<typename T, ::std::size_t N>
static bool doesAnyMatch(const string& exemplar, T(&matchList)[N])
{
	return ::std::any_of(begin(matchList), end(matchList),
		[&exemplar](const char*const pStr) { return ba::iequals(exemplar, pStr); });
}

bool pmnt::ConfigFileReader::parseBool(const string& str, uint32 lineNum)
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
		throw Exception(format("Ill-formed Boolean value '%1%' on line %2%") % str % lineNum);
	}
}
