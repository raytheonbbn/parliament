// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2019, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/LogConfig.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/ConfigFileReader.h"
#include "parliament/Exceptions.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"

#include <boost/filesystem/operations.hpp>

namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::string;
using ::std::string_view;

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
	m_ceMap["logToConsole"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logToConsole = ConfigFileReader::parseBool(value, lineNum); };
	m_ceMap["logConsoleAsynchronous"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logConsoleAsynchronous = ConfigFileReader::parseBool(value, lineNum); };
	m_ceMap["logConsoleAutoFlush"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logConsoleAutoFlush = ConfigFileReader::parseBool(value, lineNum); };
	m_ceMap["logToFile"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logToFile = ConfigFileReader::parseBool(value, lineNum); };
	m_ceMap["logFilePath"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logFilePath = convertUtf8ToLogPath(value); };
	m_ceMap["logFileAsynchronous"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileAsynchronous = ConfigFileReader::parseBool(value, lineNum); };
	m_ceMap["logFileAutoFlush"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileAutoFlush = ConfigFileReader::parseBool(value, lineNum); };
	m_ceMap["logFileRotationSize"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileRotationSize = ConfigFileReader::parseUnsigned(value, lineNum); };
	m_ceMap["logFileMaxAccumSize"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileMaxAccumSize = ConfigFileReader::parseUnsigned(value, lineNum); };
	m_ceMap["logFileMinFreeSpace"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileMinFreeSpace = ConfigFileReader::parseUnsigned(value, lineNum); };
	m_ceMap["logFileRotationTimePoint"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logFileRotationTimePoint = value; };
	m_ceMap["logLevel"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{ c.m_logLevel = value; };
	m_ceMap["logChannelLevel"] = [](string_view value, uint32 lineNum, LogConfig& c)
		{
			auto [ channel, logLevel ] = ConfigFileReader::getKeyValueFromLine(value, lineNum);
			c.addLogChannelLevel(channel, logLevel);
		};
}

pmnt::LogConfig::LogConfig(const LogConfig&) = default;
pmnt::LogConfig& pmnt::LogConfig::operator=(const LogConfig&) = default;
pmnt::LogConfig::LogConfig(LogConfig&&) = default;
pmnt::LogConfig& pmnt::LogConfig::operator=(LogConfig&&) = default;
pmnt::LogConfig::~LogConfig() = default;

void pmnt::LogConfig::readFromFile()
{
	ConfigFileReader::readFile(ConfigKind::k_log,
		[pLogConfig = this](string_view key, string_view value, uint32 lineNum)
		{
			auto it = pLogConfig->m_ceMap.find(string{key});
			if (it == end(pLogConfig->m_ceMap))
			{
				throw Exception(format(
					"Illegal configuration file syntax: Unrecognized key '%1%' on line %2% in class '%3%'")
					% key % lineNum % typeid(*pLogConfig).name());
			}
			else
			{
				it->second(value, lineNum, *pLogConfig);
			}
		});
}

pmnt::LogConfig::Path pmnt::LogConfig::convertUtf8ToLogPath(string_view value)
{
	auto result = convertUtf8ToPath(value);
	if (result.is_absolute())
	{
		return result;
	}

	Path logBasePath;
	ConfigFileReader::readFile(ConfigKind::k_kb,
		[pKbDirPath = &logBasePath](string_view key, string_view value, uint32 lineNum)
		{
			if (key == "kbDirectoryPath")
			{
				pKbDirPath->assign(convertUtf8ToPath(value));
			}
		});
	return absolute(logBasePath / result);
}
