// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/Config.h"
#include "parliament/ArrayLength.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/Exceptions.h"
#include "parliament/Log.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"
#include "parliament/Windows.h"

#include <boost/algorithm/string/case_conv.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/filesystem/fstream.hpp>
#include <boost/filesystem/operations.hpp>
#include <boost/format.hpp>
#include <algorithm>
#include <cstring>
#include <fstream>
#include <string>
#include <vector>

namespace ba = ::boost::algorithm;
namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::begin;
using ::std::end;
using ::std::getline;
using ::std::ifstream;
using ::std::ios;
using ::std::string;
using ::std::vector;

static const char*const k_trueBoolValues[] = { "true", "t", "yes", "y", "on", "1" };
static const char*const k_falseBoolValues[] = { "false", "f", "no", "n", "off", "0" };
const pmnt::TChar pmnt::Config::k_defaultConfigFileName[] = _T("ParliamentConfig.txt");

static auto g_log = pmnt::Log::getSource("Config");



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
		vector<TChar> buffer(bufferLen, '\0');
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

bfs::path pmnt::Config::getConfigFilePath()
{
	auto envVarValue = tGetEnvVar(_T("PARLIAMENT_CONFIG_PATH"));
	if (!envVarValue.empty())
	{
		return envVarValue;
	}
	else
	{
#if defined(PARLIAMENT_WINDOWS)
		HMODULE hModule = getCurrentModuleHandle();
		auto configPath = getModulePathName(hModule).parent_path();
		configPath /= k_defaultConfigFileName;
		if (exists(configPath) && is_regular_file(configPath))
		{
			return configPath;
		}
		else
		{
			PMNT_LOG(g_log, LogLevel::info) << "Computed config file path '"
				<< pathAsUtf8(configPath) << "' does not exist or is not a regular file."
				"  Defaulting to the current directory.";
		}
#endif

		return k_defaultConfigFileName;
	}
}



// =========================================================================
//
// Config implementation
//
// =========================================================================

pmnt::Config::Config() :
	m_logToConsole(true),
	m_logConsoleAsynchronous(false),
	m_logConsoleAutoFlush(true),
	m_logToFile(true),
	m_logFilePath(convertUtf8ToPath("log/ParliamentNative%3N_%Y-%m-%d_%H-%M-%S.log")),
	m_logFileAsynchronous(false),
	m_logFileAutoFlush(true),
	m_logFileRotationSize(10u * 1024u * 1024u),
	m_logFileMaxAccumSize(150u * 1024u * 1024u),
	m_logFileMinFreeSpace(100u * 1024u * 1024u),
	m_logFileRotationTimePoint("02:00:00"),	// 2 AM
	m_logLevel("INFO"),
	m_logChannelLevel(),
	m_kbDirectoryPath(convertUtf8ToPath(".")),
	m_stmtFileName("statements.mem"),
	m_rsrcFileName("resources.mem"),
	m_uriTableFileName("uris.mem"),
	m_uriToIntFileName("u2i.db"),
	m_readOnly(false),
	m_fileSyncTimerDelay(15000),
	m_initialRsrcCapacity(100000),
	m_avgRsrcLen(64),
	m_rsrcGrowthFactor(1.25),
	m_initialStmtCapacity(500000),
	m_stmtGrowthFactor(1.25),
	m_bdbCacheSize("32m,1"),
	m_normalizeTypedStringLiterals(true),
	m_runAllRulesAtStartup(false),
	m_enableSWRLRuleEngine(false),
	m_isSubclassRuleOn(true),
	m_isSubpropertyRuleOn(true),
	m_isDomainRuleOn(true),
	m_isRangeRuleOn(true),
	m_isEquivalentClassRuleOn(true),
	m_isEquivalentPropRuleOn(true),
	m_isInverseOfRuleOn(true),
	m_isSymmetricPropRuleOn(true),
	m_isFunctionalPropRuleOn(false),
	m_isInvFunctionalPropRuleOn(false),
	m_isTransitivePropRuleOn(true),
	m_inferRdfsClass(false),
	m_inferOwlClass(false),
	m_inferRdfsResource(false),
	m_inferOwlThing(false),
	m_timeoutDuration(5),
	m_timeoutUnit("MINUTES")
{
}

bfs::path pmnt::Config::stmtFilePath() const
{
	return m_kbDirectoryPath / convertUtf8ToPath(m_stmtFileName);
}

bfs::path pmnt::Config::rsrcFilePath() const
{
	return m_kbDirectoryPath / convertUtf8ToPath(m_rsrcFileName);
}

bfs::path pmnt::Config::uriTableFilePath() const
{
	return m_kbDirectoryPath / convertUtf8ToPath(m_uriTableFileName);
}

bfs::path pmnt::Config::uriToIntFilePath() const
{
	return m_kbDirectoryPath / convertUtf8ToPath(m_uriToIntFileName);
}

pmnt::Config pmnt::Config::readFromFile()
{
	Config result;	// Initialize with default values

	auto configPath = getConfigFilePath();
	if (!exists(configPath))
	{
		throw Exception(format("The configuration file \"%1%\" does not exist")
			% configPath);
	}
	else if (!is_regular_file(configPath))
	{
		throw Exception(format("The configuration file \"%1%\" is not a regular file")
			% configPath);
	}

	bfs::ifstream in(configPath, ios::in);
	if (!in || !in.is_open())
	{
		throw Exception(format("Unable to open the configuration file \"%1%\"")
			% configPath);
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
			parseKeyValuePair(key, value, lineNum, result);
		}
	}
	return result;
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

void pmnt::Config::parseKeyValuePair(const string& key,
	const string& value, uint32 lineNum, /* in-out */ Config& cp)
{
	if (ba::iequals(key, "logToConsole"))
	{
		cp.m_logToConsole = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "logConsoleAsynchronous"))
	{
		cp.m_logConsoleAsynchronous = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "logConsoleAutoFlush"))
	{
		cp.m_logConsoleAutoFlush = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "logToFile"))
	{
		cp.m_logToFile = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "logFilePath"))
	{
		cp.m_logFilePath = convertUtf8ToPath(value);
	}
	else if (ba::iequals(key, "logFileAsynchronous"))
	{
		cp.m_logFileAsynchronous = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "logFileAutoFlush"))
	{
		cp.m_logFileAutoFlush = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "logFileRotationSize"))
	{
		cp.m_logFileRotationSize = parseUnsigned(value, lineNum);
	}
	else if (ba::iequals(key, "logFileMaxAccumSize"))
	{
		cp.m_logFileMaxAccumSize = parseUnsigned(value, lineNum);
	}
	else if (ba::iequals(key, "logFileMinFreeSpace"))
	{
		cp.m_logFileMinFreeSpace = parseUnsigned(value, lineNum);
	}
	else if (ba::iequals(key, "logFileRotationTimePoint"))
	{
		cp.m_logFileRotationTimePoint = value;
	}
	else if (ba::iequals(key, "logLevel"))
	{
		cp.m_logLevel = value;
	}
	else if (ba::iequals(key, "logChannelLevel"))
	{
		string channel, logLevel;
		splitAtFirstEquals(value, lineNum, channel, logLevel);
		cp.addLogChannelLevel(channel, logLevel);
	}
	else if (ba::iequals(key, "kbDirectoryPath"))
	{
		cp.m_kbDirectoryPath = convertUtf8ToPath(value);
		if (exists(cp.m_kbDirectoryPath) && !is_directory(cp.m_kbDirectoryPath))
		{
			throw Exception(format(
				"%1% entry on configuration file line %2% is not a directory:  '%3%'")
				% key % lineNum % value);
		}
	}
	else if (ba::iequals(key, "stmtFileName"))
	{
		cp.m_stmtFileName = value;
	}
	else if (ba::iequals(key, "rsrcFileName"))
	{
		cp.m_rsrcFileName = value;
	}
	else if (ba::iequals(key, "uriTableFileName"))
	{
		cp.m_uriTableFileName = value;
	}
	else if (ba::iequals(key, "uriToIntFileName"))
	{
		cp.m_uriToIntFileName = value;
	}
	else if (ba::iequals(key, "stmtToIdFileName"))
	{
		PMNT_LOG(g_log, LogLevel::warn)
			<< "The 'stmtToIdFileName' configuration option is ignored "
				"and can be deleted from your configuration file";
	}
	else if (ba::iequals(key, "readOnly"))
	{
		cp.m_readOnly = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "fileSyncTimerDelay"))
	{
		cp.m_fileSyncTimerDelay = parseUnsigned(value, lineNum);
	}
	else if (ba::iequals(key, "keepDupStmtIdx"))
	{
		PMNT_LOG(g_log, LogLevel::warn)
			<< "The 'keepDupStmtIdx' configuration option is ignored "
				"and can be deleted from your configuration file";
	}
	else if (ba::iequals(key, "initialRsrcCapacity"))
	{
		cp.m_initialRsrcCapacity = parseUnsigned(value, lineNum);
	}
	else if (ba::iequals(key, "avgRsrcLen"))
	{
		cp.m_avgRsrcLen = parseUnsigned(value, lineNum);
	}
	else if (ba::iequals(key, "rsrcGrowthFactor"))
	{
		cp.m_rsrcGrowthFactor = parseDouble(value, lineNum);
	}
	else if (ba::iequals(key, "initialStmtCapacity"))
	{
		cp.m_initialStmtCapacity = parseUnsigned(value, lineNum);
	}
	else if (ba::iequals(key, "stmtGrowthFactor"))
	{
		cp.m_stmtGrowthFactor = parseDouble(value, lineNum);
	}
	else if (ba::iequals(key, "bdbCacheSize"))
	{
		cp.m_bdbCacheSize = value;
	}
	else if (ba::iequals(key, "normalizeTypedStringLiterals"))
	{
		cp.m_normalizeTypedStringLiterals = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "runAllRulesAtStartup"))
	{
		cp.m_runAllRulesAtStartup = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "enableSWRLRuleEngine"))
	{
		cp.m_enableSWRLRuleEngine = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "SubclassRule"))
	{
		cp.m_isSubclassRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "SubpropertyRule"))
	{
		cp.m_isSubpropertyRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "DomainRule"))
	{
		cp.m_isDomainRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "RangeRule"))
	{
		cp.m_isRangeRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "EquivalentClassRule"))
	{
		cp.m_isEquivalentClassRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "EquivalentPropRule"))
	{
		cp.m_isEquivalentPropRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "InverseOfRule"))
	{
		cp.m_isInverseOfRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "SymmetricPropRule"))
	{
		cp.m_isSymmetricPropRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "FunctionalPropRule"))
	{
		cp.m_isFunctionalPropRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "InvFunctionalPropRule"))
	{
		cp.m_isInvFunctionalPropRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "TransitivePropRule"))
	{
		cp.m_isTransitivePropRuleOn = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "inferRdfsClass"))
	{
		cp.m_inferRdfsClass = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "inferOwlClass"))
	{
		cp.m_inferOwlClass = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "inferRdfsResource"))
	{
		cp.m_inferRdfsResource = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "inferOwlThing"))
	{
		cp.m_inferOwlThing = parseBool(value, lineNum);
	}
	else if (ba::iequals(key, "TimeoutDuration"))
	{
		cp.m_timeoutDuration = parseUnsigned(value, lineNum);
	}
	else if (ba::iequals(key, "TimeoutUnit"))
	{
		if (!validateTimeUnit(value)) {
			throw Exception(
				format("Illegal configuration file syntax: invalid '%1%' value '%2%' on line %3%")
					% key
					% value
					% lineNum
			);
		}
		cp.m_timeoutUnit = value;
	}
	else
	{
		throw Exception(format(
			"Illegal configuration file syntax:  unrecognized key '%1%' on line %2%")
			% key % lineNum);
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

void pmnt::Config::disableAllRules()
{
	m_enableSWRLRuleEngine			= false;

	m_isSubclassRuleOn				= false;
	m_isSubpropertyRuleOn			= false;
	m_isDomainRuleOn					= false;
	m_isRangeRuleOn					= false;
	m_isEquivalentClassRuleOn		= false;
	m_isEquivalentPropRuleOn		= false;
	m_isInverseOfRuleOn				= false;
	m_isSymmetricPropRuleOn			= false;
	m_isFunctionalPropRuleOn		= false;
	m_isInvFunctionalPropRuleOn	= false;
	m_isTransitivePropRuleOn		= false;

	m_inferRdfsClass					= false;
	m_inferOwlClass					= false;
	m_inferRdfsResource				= false;
	m_inferOwlThing					= false;
}

const pmnt::Config& pmnt::Config::ensureKbDirExists() const
{
	if (!exists(m_kbDirectoryPath))
	{
		create_directory(m_kbDirectoryPath);
		PMNT_LOG(g_log, LogLevel::debug) << "Created KB directory '"
			<< pathAsUtf8(m_kbDirectoryPath) << "'.";
	}
	else if (!is_directory(m_kbDirectoryPath))
	{
		throw Exception(format(
			"Configuration entry kbDirectoryPath is not a directory:  '%1%'")
			% m_kbDirectoryPath.generic_string());
	}
	return *this;
}

bool pmnt::Config::validateTimeUnit(const string& s) {
	return ba::equals(s, "MILLISECONDS") || ba::equals(s, "SECONDS") || ba::equals(s, "MINUTES");
}

void pmnt::Config::timeoutUnit(const string& newValue) {
	if (!validateTimeUnit(newValue)) {
		throw Exception(format("Invalid time unit: '%1%'") % newValue);
	}
	m_timeoutUnit = newValue;
}
