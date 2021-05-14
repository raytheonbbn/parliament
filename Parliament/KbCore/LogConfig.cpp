// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2019, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/LogConfig.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"

namespace pmnt = ::bbn::parliament;

using ::std::string;

static constexpr pmnt::TChar k_envVarName[] = _T("PARLIAMENT_LOG_CONFIG_PATH");
static constexpr pmnt::TChar k_logPathBaseVarName[] = _T("PARLIAMENT_LOG_PATH_BASE");
static constexpr pmnt::TChar k_defaultConfigFileName[] = _T("ParliamentLogConfig.txt");

pmnt::LogConfig::LogConfig() :
	m_ceMap(),
	m_logToConsole(false),
	m_logConsoleAsynchronous(false),
	m_logConsoleAutoFlush(true),
	m_logToFile(true),
	m_logFilePath(convertUtf8ToLogPath("log/ParliamentNative%3N_%Y-%m-%d_%H-%M-%S.log")),
	m_logFileAsynchronous(false),
	m_logFileAutoFlush(true),
	m_logFileRotationSize(10u * 1024u * 1024u),
	m_logFileMaxAccumSize(150u * 1024u * 1024u),
	m_logFileMinFreeSpace(100u * 1024u * 1024u),
	m_logFileRotationTimePoint("02:00:00"),	// 2 AM
	m_logLevel("INFO"),
	m_logChannelLevel()
{
	m_ceMap["logToConsole"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logToConsole = parseBool(value, lineNum); };
	m_ceMap["logConsoleAsynchronous"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logConsoleAsynchronous = parseBool(value, lineNum); };
	m_ceMap["logConsoleAutoFlush"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logConsoleAutoFlush = parseBool(value, lineNum); };
	m_ceMap["logToFile"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logToFile = parseBool(value, lineNum); };
	m_ceMap["logFilePath"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logFilePath = convertUtf8ToLogPath(value); };
	m_ceMap["logFileAsynchronous"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileAsynchronous = parseBool(value, lineNum); };
	m_ceMap["logFileAutoFlush"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileAutoFlush = parseBool(value, lineNum); };
	m_ceMap["logFileRotationSize"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileRotationSize = parseUnsigned(value, lineNum); };
	m_ceMap["logFileMaxAccumSize"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileMaxAccumSize = parseUnsigned(value, lineNum); };
	m_ceMap["logFileMinFreeSpace"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileMinFreeSpace = parseUnsigned(value, lineNum); };
	m_ceMap["logFileRotationTimePoint"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileRotationTimePoint = value; };
	m_ceMap["logLevel"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{ c.m_logLevel = value; };
	m_ceMap["logChannelLevel"] = [](const string& value, uint32 lineNum, LogConfig& c)
		{
			string channel, logLevel;
			splitAtFirstEquals(value, lineNum, channel, logLevel);
			c.addLogChannelLevel(channel, logLevel);
		};
}

const pmnt::TChar* pmnt::LogConfig::getEnvVarName() const
{
	return k_envVarName;
}

const pmnt::TChar* pmnt::LogConfig::getDefaultConfigFileName() const
{
	return k_defaultConfigFileName;
}

const pmnt::Config::ConfigEntryMap& pmnt::LogConfig::getConfigEntryMap() const
{
	// This cast is gross, but the only difference between the types is
	// the type of the config parameter passed to the handler function,
	// and that is covariant, so it should be okay.
	const ConfigEntryMap& ref = m_ceMap;
	return reinterpret_cast<const Config::ConfigEntryMap&>(ref);
}

pmnt::Config::Path pmnt::LogConfig::convertUtf8ToLogPath(const string& value)
{
	auto result = convertUtf8ToPath(value);
	if (result.is_relative())
	{
		pmnt::TString logPathBase = pmnt::tGetEnvVar(k_logPathBaseVarName);
		if (!logPathBase.empty())
		{
			auto base = convertUtf8ToPath(convertTCharToUtf8(logPathBase));
			return base /= result;
		}
	}
	return result;
}
