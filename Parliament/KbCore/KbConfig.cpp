// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2019, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/KbConfig.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/ConfigFileReader.h"
#include "parliament/Exceptions.h"
#include "parliament/Log.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"

#include <boost/algorithm/string/predicate.hpp>
#include <boost/filesystem/operations.hpp>
#include <boost/format.hpp>
#include <boost/thread/once.hpp>

namespace pmnt = ::bbn::parliament;
namespace ba = ::boost::algorithm;

using ::boost::format;
using ::std::string;

static ::boost::once_flag g_onceInitFlag = BOOST_ONCE_INIT;
static auto g_log = pmnt::log::getSource("KbConfig");
static constexpr pmnt::TChar k_bdbCacheSizeVarName[] = _T("PARLIAMENT_BDB_CACHE_SIZE");
pmnt::KbConfig::ConfigEntryMap pmnt::KbConfig::g_ceMap;
bool pmnt::KbConfig::g_isConfigEntryMapInitialized = pmnt::KbConfig::initConfigEntryMap();

bool pmnt::KbConfig::initConfigEntryMap()
{
	call_once(g_onceInitFlag, &unsynchronizedInitConfigEntryMap);
	return true;
}

void pmnt::KbConfig::unsynchronizedInitConfigEntryMap()
{
	g_ceMap["kbDirectoryPath"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.kbDirectoryPath(convertUtf8ToPath(value)); };
	g_ceMap["stmtFileName"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_stmtFileName = value; };
	g_ceMap["rsrcFileName"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_rsrcFileName = value; };
	g_ceMap["uriTableFileName"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_uriTableFileName = value; };
	g_ceMap["uriToIntFileName"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_uriToIntFileName = value; };
	g_ceMap["stmtToIdFileName"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{
			PMNT_LOG(g_log, log::Level::warn)
				<< "The 'stmtToIdFileName' configuration option is ignored "
					"and can be deleted from your configuration file";
		};
	g_ceMap["readOnly"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_readOnly = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["fileSyncTimerDelay"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_fileSyncTimerDelay = ConfigFileReader::parseUnsigned(value, lineNum); };
	g_ceMap["keepDupStmtIdx"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{
			PMNT_LOG(g_log, log::Level::warn)
				<< "The 'keepDupStmtIdx' configuration option is ignored "
					"and can be deleted from your configuration file";
		};
	g_ceMap["initialRsrcCapacity"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_initialRsrcCapacity = ConfigFileReader::parseUnsigned(value, lineNum); };
	g_ceMap["avgRsrcLen"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_avgRsrcLen = ConfigFileReader::parseUnsigned(value, lineNum); };
	g_ceMap["rsrcGrowthIncrement"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_rsrcGrowthIncrement = ConfigFileReader::parseUnsigned(value, lineNum); };
	g_ceMap["rsrcGrowthFactor"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_rsrcGrowthFactor = ConfigFileReader::parseDouble(value, lineNum); };
	g_ceMap["initialStmtCapacity"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_initialStmtCapacity = ConfigFileReader::parseUnsigned(value, lineNum); };
	g_ceMap["stmtGrowthIncrement"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_stmtGrowthIncrement = ConfigFileReader::parseUnsigned(value, lineNum); };
	g_ceMap["stmtGrowthFactor"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_stmtGrowthFactor = ConfigFileReader::parseDouble(value, lineNum); };
	g_ceMap["bdbCacheSize"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{
			auto envVarValue = convertTCharToUtf8(tGetEnvVar(k_bdbCacheSizeVarName));
			if (!envVarValue.empty())
			{
				c.m_bdbCacheSize = envVarValue;
				PMNT_LOG(g_log, log::Level::info)
					<< "The '" << convertTCharToUtf8(k_bdbCacheSizeVarName) << "' environment"
						" variable overrode the corresponding value in the configuration file";
			}
			else
			{
				c.m_bdbCacheSize = value;
			}
		};
	g_ceMap["normalizeTypedStringLiterals"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_normalizeTypedStringLiterals = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["TimeoutDuration"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_timeoutDuration = ConfigFileReader::parseUnsigned(value, lineNum); };
	g_ceMap["TimeoutUnit"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.timeoutUnit(value); };
	g_ceMap["runAllRulesAtStartup"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_runAllRulesAtStartup = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["enableSWRLRuleEngine"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_enableSWRLRuleEngine = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["SubclassRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isSubclassRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["SubpropertyRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isSubpropertyRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["DomainRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isDomainRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["RangeRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isRangeRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["EquivalentClassRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isEquivalentClassRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["EquivalentPropRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isEquivalentPropRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["InverseOfRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isInverseOfRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["SymmetricPropRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isSymmetricPropRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["FunctionalPropRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isFunctionalPropRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["InvFunctionalPropRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isInvFunctionalPropRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["TransitivePropRule"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_isTransitivePropRuleOn = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["inferRdfsClass"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_inferRdfsClass = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["inferOwlClass"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_inferOwlClass = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["inferRdfsResource"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_inferRdfsResource = ConfigFileReader::parseBool(value, lineNum); };
	g_ceMap["inferOwlThing"] = [](const string& value, uint32 lineNum, KbConfig& c)
		{ c.m_inferOwlThing = ConfigFileReader::parseBool(value, lineNum); };
}

pmnt::KbConfig::KbConfig() :
	m_kbDirectoryPath(convertUtf8ToPath("kb-data")),
	m_stmtFileName("statements.mem"),
	m_rsrcFileName("resources.mem"),
	m_uriTableFileName("uris.mem"),
	m_uriToIntFileName("u2i.db"),
	m_readOnly(false),
	m_fileSyncTimerDelay(15000),
	m_initialRsrcCapacity(300000),
	m_avgRsrcLen(100),
	m_rsrcGrowthIncrement(600000),
	m_rsrcGrowthFactor(0),
	m_initialStmtCapacity(500000),
	m_stmtGrowthIncrement(1000000),
	m_stmtGrowthFactor(0),
	m_bdbCacheSize("512m,1"),
	m_normalizeTypedStringLiterals(true),
	m_timeoutDuration(5),
	m_timeoutUnit(TimeUnit::k_min),
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
	m_inferOwlThing(false)
{
}

pmnt::KbConfig::KbConfig(const KbConfig&) = default;
pmnt::KbConfig& pmnt::KbConfig::operator=(const KbConfig&) = default;
pmnt::KbConfig::KbConfig(KbConfig&&) = default;
pmnt::KbConfig& pmnt::KbConfig::operator=(KbConfig&&) = default;
pmnt::KbConfig::~KbConfig() = default;

void pmnt::KbConfig::readFromFile()
{
	ConfigFileReader::readFile(ConfigKind::k_kb,
		[pKbConfig = this](const string& key, const string& value, uint32 lineNum)
		{
			auto it = pKbConfig->g_ceMap.find(key);
			if (it == end(pKbConfig->g_ceMap))
			{
				throw Exception(format(
					"Illegal configuration file syntax: Unrecognized key '%1%' on line %2% in class '%3%'")
					% key % lineNum % typeid(*pKbConfig).name());
			}
			else
			{
				it->second(value, lineNum, *pKbConfig);
			}
		});
}

void pmnt::KbConfig::kbDirectoryPath(const Path& newValue)
{
	if (exists(newValue) && !is_directory(newValue))
	{
		throw Exception(format(
			"New kbDirectoryPath setting is not a directory:  '%2%'")
			% newValue);
	}
	m_kbDirectoryPath = newValue;
}

pmnt::KbConfig::Path pmnt::KbConfig::stmtFilePath() const
{
	return m_kbDirectoryPath / convertUtf8ToPath(m_stmtFileName);
}

pmnt::KbConfig::Path pmnt::KbConfig::rsrcFilePath() const
{
	return m_kbDirectoryPath / convertUtf8ToPath(m_rsrcFileName);
}

pmnt::KbConfig::Path pmnt::KbConfig::uriTableFilePath() const
{
	return m_kbDirectoryPath / convertUtf8ToPath(m_uriTableFileName);
}

pmnt::KbConfig::Path pmnt::KbConfig::uriToIntFilePath() const
{
	return m_kbDirectoryPath / convertUtf8ToPath(m_uriToIntFileName);
}

string pmnt::KbConfig::javaTimeoutUnit() const
{
	switch (m_timeoutUnit)
	{
	case TimeUnit::k_nanoSec:
		return "NANOSECONDS";
	case TimeUnit::k_microSec:
		return "MICROSECONDS";
	case TimeUnit::k_milliSec:
		return "MILLISECONDS";
	case TimeUnit::k_sec:
		return "SECONDS";
	case TimeUnit::k_min:
		return "MINUTES";
	case TimeUnit::k_hour:
		return "HOURS";
	case TimeUnit::k_day:
		return "DAYS";
	default:
		using ULT = typename ::std::underlying_type<TimeUnit>::type;
		throw Exception(format("Unrecognized time unit: '%1%'") % static_cast<ULT>(m_timeoutUnit));
	}
}

void pmnt::KbConfig::timeoutUnit(const string& newValue) {
	if (ba::iequals(newValue, "NANOSECONDS"))
	{
		m_timeoutUnit = TimeUnit::k_nanoSec;
	}
	else if (ba::iequals(newValue, "MICROSECONDS"))
	{
		m_timeoutUnit = TimeUnit::k_microSec;
	}
	else if (ba::iequals(newValue, "MILLISECONDS"))
	{
		m_timeoutUnit = TimeUnit::k_milliSec;
	}
	else if (ba::iequals(newValue, "SECONDS"))
	{
		m_timeoutUnit = TimeUnit::k_sec;
	}
	else if (ba::iequals(newValue, "MINUTES"))
	{
		m_timeoutUnit = TimeUnit::k_min;
	}
	else if (ba::iequals(newValue, "HOURS"))
	{
		m_timeoutUnit = TimeUnit::k_hour;
	}
	else if (ba::iequals(newValue, "DAYS"))
	{
		m_timeoutUnit = TimeUnit::k_day;
	}
	else
	{
		throw Exception(format("Invalid time unit: '%1%'") % newValue);
	}
}

void pmnt::KbConfig::setStatusOfAllRules(bool enableAllRules)
{
	m_enableSWRLRuleEngine			= enableAllRules;

	m_isSubclassRuleOn				= enableAllRules;
	m_isSubpropertyRuleOn			= enableAllRules;
	m_isDomainRuleOn					= enableAllRules;
	m_isRangeRuleOn					= enableAllRules;
	m_isEquivalentClassRuleOn		= enableAllRules;
	m_isEquivalentPropRuleOn		= enableAllRules;
	m_isInverseOfRuleOn				= enableAllRules;
	m_isSymmetricPropRuleOn			= enableAllRules;
	m_isFunctionalPropRuleOn		= enableAllRules;
	m_isInvFunctionalPropRuleOn	= enableAllRules;
	m_isTransitivePropRuleOn		= enableAllRules;

	m_inferRdfsClass					= enableAllRules;
	m_inferOwlClass					= enableAllRules;
	m_inferRdfsResource				= enableAllRules;
	m_inferOwlThing					= enableAllRules;
}

const pmnt::KbConfig& pmnt::KbConfig::ensureKbDirExists() const
{
	if (!exists(m_kbDirectoryPath))
	{
		create_directory(m_kbDirectoryPath);
		PMNT_LOG(g_log, log::Level::debug) << "Created KB directory '"
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
