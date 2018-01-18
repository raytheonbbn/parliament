// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <algorithm>
#include <boost/filesystem/path.hpp>
#include <boost/test/unit_test.hpp>
#include <boost/test/data/test_case.hpp>
#include <fstream>
#include <iterator>
#include <map>
#include <ostream>
#include <string>
#include <system_error>
#include "parliament/Config.h"
#include "parliament/Exceptions.h"
#include "parliament/Windows.h"
#include "parliament/ArrayLength.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/UnicodeIterator.h"
#include "TestUtils.h"

namespace bdata = ::boost::unit_test::data;
namespace bfs = ::boost::filesystem;

using namespace ::bbn::parliament;
using ::std::endl;
using ::std::equal;
using ::std::ofstream;
using ::std::ostream;
using ::std::string;

// =========================================================================

static bfs::path getConfigFilePath(const TChar* pFileName)
{
#if defined(PARLIAMENT_WINDOWS)
	TChar buffer[1024];
	DWORD errCode = ::GetModuleFileName(0, buffer, static_cast<DWORD>(arrayLen(buffer)));
	if (errCode <= 0 || errCode >= arrayLen(buffer))
	{
		throw ::std::system_error(::GetLastError(), ::std::system_category(),
			"GetModuleFileName failed in unit test");
	}

	auto modulePath = bfs::path{buffer};
	return modulePath.parent_path() / pFileName;
#else
	return pFileName;
#endif
}

BOOST_AUTO_TEST_SUITE(ConfigTestSuite)

struct GetConfigFilePathTestCase
{
	const TChar*		m_pEnvVar;
	const bfs::path&	m_expectedResult;
};

static ostream& operator<<(ostream& os, const GetConfigFilePathTestCase& tc)
{
	os << "Env var '" << convertTCharToUtf8(tc.m_pEnvVar) << "'";
	return os;
}

static const GetConfigFilePathTestCase k_getConfigFilePathTestCases[] =
	{
#if defined(PARLIAMENT_WINDOWS)
		{ _T("PARLIAMENT_CONFIG_PATH=C:\\Config.txt"),	bfs::path{_T("C:\\Config.txt")} },
#else
		{ _T("PARLIAMENT_CONFIG_PATH=/config.txt"),		bfs::path{_T("/config.txt")} },
#endif
		{ _T("PARLIAMENT_CONFIG_PATH="),						getConfigFilePath(_T("ParliamentConfig.txt")) },
	};

BOOST_DATA_TEST_CASE(
	testConfigGetConfigFilePath,
	bdata::make(k_getConfigFilePathTestCases),
	tc)
{
	setEnvVar(tc.m_pEnvVar);
	BOOST_CHECK_EQUAL(tc.m_expectedResult, Config::testGetConfigFilePath());
}

// =========================================================================

struct IsBlankOrCommentLineTestCase
{
	const char*	m_pInputStr;
	bool			m_expectedResult;
};

static ostream& operator<<(ostream& os, const IsBlankOrCommentLineTestCase& tc)
{
	os << "Input string '" << tc.m_pInputStr << "'";
	return os;
}

static const IsBlankOrCommentLineTestCase k_isBlankOrCommentLineTestCases[] =
	{
		{ "# blah blah blah",	true },
		{ "   \t# blah blah blah",	true },
		{ "   \t blah blah blah",	false },
		{ "   \t \r\n   \t ",	true },
	};

BOOST_DATA_TEST_CASE(
	testConfigIsBlankOrCommentLine,
	bdata::make(k_isBlankOrCommentLineTestCases),
	tc)
{
	BOOST_CHECK(Config::testIsBlankOrCommentLine(tc.m_pInputStr) == tc.m_expectedResult);
}

// =========================================================================

struct ParseUnsignedTestCase
{
	const char*	m_pInputStr;
	bool			m_shouldThrow;
	size_t		m_expectedResult;
};

static ostream& operator<<(ostream& os, const ParseUnsignedTestCase& tc)
{
	os << "Input string '" << tc.m_pInputStr << "'";
	return os;
}

static const ParseUnsignedTestCase k_parseUnsignedTestCases[] =
	{
		{ "a 37",	true,		0u },
		{ "37 a",	true,		0u },
		{ "-37",		true,		0u },
		{ " 37 ",	false,	37u },
	};

BOOST_DATA_TEST_CASE(
	testConfigParseUnsigned,
	bdata::make(k_parseUnsignedTestCases),
	tc)
{
	if (tc.m_shouldThrow)
	{
		BOOST_CHECK_THROW(Config::testParseUnsigned(tc.m_pInputStr, 1), Exception);
	}
	else
	{
		BOOST_CHECK_EQUAL(tc.m_expectedResult, Config::testParseUnsigned(tc.m_pInputStr, 1));
	}
}

// =========================================================================

struct ParseDoubleTestCase
{
	const char*	m_pInputStr;
	bool			m_shouldThrow;
	double		m_expectedResult;
};

static ostream& operator<<(ostream& os, const ParseDoubleTestCase& tc)
{
	os << "Input string '" << tc.m_pInputStr << "'";
	return os;
}

static const ParseDoubleTestCase k_parseDoubleTestCases[] =
	{
		{ "a 37.9",	true,		0.0 },
		{ "37.9 a",	true,		0.0 },
		{ "-37.9",	false,	-37.9 },
		{ " 37.9 ",	false,	37.9 },
	};

BOOST_DATA_TEST_CASE(
	testConfigParseDouble,
	bdata::make(k_parseDoubleTestCases),
	tc)
{
	if (tc.m_shouldThrow)
	{
		BOOST_CHECK_THROW(Config::testParseDouble(tc.m_pInputStr, 1), Exception);
	}
	else
	{
		BOOST_CHECK_EQUAL(tc.m_expectedResult, Config::testParseDouble(tc.m_pInputStr, 1));
	}
}

// =========================================================================

struct ParseBoolTestCase
{
	const char*	m_pInputStr;
	bool			m_shouldThrow;
	bool			m_expectedResult;
};

static ostream& operator<<(ostream& os, const ParseBoolTestCase& tc)
{
	os << "Input string '" << tc.m_pInputStr << "'";
	return os;
}

static const ParseBoolTestCase k_parseBoolTestCases[] =
	{
		{ "true",			false,	true },
		{ "trUe",			false,	true },
		{ "TRUE",			false,	true },
		{ "t",				false,	true },
		{ "T",				false,	true },
		{ "yes",				false,	true },
		{ "YES",				false,	true },
		{ "y",				false,	true },
		{ "Y",				false,	true },
		{ "on",				false,	true },
		{ "1",				false,	true },
		{ " false",			false,	false },
		{ "FALSE ",			false,	false },
		{ " \tfaLSe ",		false,	false },
		{ "f",				false,	false },
		{ "F",				false,	false },
		{ "no",				false,	false },
		{ "n",				false,	false },
		{ "off",				false,	false },
		{ "0",				false,	false },
		{ "affirmative",	true,		false },
		{ "",					true,		false },
	};

BOOST_DATA_TEST_CASE(
	testConfigParseBool,
	bdata::make(k_parseBoolTestCases),
	tc)
{
	if (tc.m_shouldThrow)
	{
		BOOST_CHECK_THROW(Config::testParseBool(tc.m_pInputStr, 1), Exception);
	}
	else
	{
		BOOST_CHECK_EQUAL(tc.m_expectedResult, Config::testParseBool(tc.m_pInputStr, 1));
	}
}

// =========================================================================

BOOST_AUTO_TEST_CASE(testConfigDefaultCtor)
{
	Config c;

	BOOST_CHECK_EQUAL(true, c.logToConsole());
	BOOST_CHECK_EQUAL(false, c.logConsoleAsynchronous());
	BOOST_CHECK_EQUAL(true, c.logConsoleAutoFlush());
	BOOST_CHECK_EQUAL(true, c.logToFile());
	BOOST_CHECK_EQUAL(string("log/ParliamentNative%3N_%Y-%m-%d_%H-%M-%S.log"), c.logFilePath());
	BOOST_CHECK_EQUAL(false, c.logFileAsynchronous());
	BOOST_CHECK_EQUAL(true, c.logFileAutoFlush());
	BOOST_CHECK_EQUAL(10u * 1024u * 1024u, c.logFileRotationSize());
	BOOST_CHECK_EQUAL(150u * 1024u * 1024u, c.logFileMaxAccumSize());
	BOOST_CHECK_EQUAL(100u * 1024u * 1024u, c.logFileMinFreeSpace());
	BOOST_CHECK_EQUAL(string("02:00:00"), c.logFileRotationTimePoint());
	BOOST_CHECK_EQUAL(string("INFO"), c.logLevel());
	BOOST_CHECK(c.logChannelLevels().empty());

	BOOST_CHECK_EQUAL(bfs::path(_T(".")), c.kbDirectoryPath());
	BOOST_CHECK_EQUAL(string("statements.mem"), c.stmtFileName());
	BOOST_CHECK_EQUAL(string("resources.mem"), c.rsrcFileName());
	BOOST_CHECK_EQUAL(string("uris.mem"), c.uriTableFileName());
	BOOST_CHECK_EQUAL(string("u2i.db"), c.uriToIntFileName());

	BOOST_CHECK_EQUAL(false, c.readOnly());
	BOOST_CHECK_EQUAL(15000u, c.fileSyncTimerDelay());
	BOOST_CHECK_EQUAL(100000u, c.initialRsrcCapacity());
	BOOST_CHECK_EQUAL(64u, c.avgRsrcLen());
	BOOST_CHECK_EQUAL(1.25, c.rsrcGrowthFactor());
	BOOST_CHECK_EQUAL(500000u, c.initialStmtCapacity());
	BOOST_CHECK_EQUAL(1.25, c.stmtGrowthFactor());
	BOOST_CHECK_EQUAL(string("32m,1"), c.bdbCacheSize());

	BOOST_CHECK_EQUAL(true, c.normalizeTypedStringLiterals());

	BOOST_CHECK_EQUAL(false, c.runAllRulesAtStartup());
	BOOST_CHECK_EQUAL(false, c.enableSWRLRuleEngine());

	BOOST_CHECK_EQUAL(true, c.isSubclassRuleOn());
	BOOST_CHECK_EQUAL(true, c.isSubpropertyRuleOn());
	BOOST_CHECK_EQUAL(true, c.isDomainRuleOn());
	BOOST_CHECK_EQUAL(true, c.isRangeRuleOn());
	BOOST_CHECK_EQUAL(true, c.isEquivalentClassRuleOn());
	BOOST_CHECK_EQUAL(true, c.isEquivalentPropRuleOn());
	BOOST_CHECK_EQUAL(true, c.isInverseOfRuleOn());
	BOOST_CHECK_EQUAL(true, c.isSymmetricPropRuleOn());
	BOOST_CHECK_EQUAL(false, c.isFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(false, c.isInvFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(true, c.isTransitivePropRuleOn());

	BOOST_CHECK_EQUAL(false, c.inferRdfsClass());
	BOOST_CHECK_EQUAL(false, c.inferOwlClass());
	BOOST_CHECK_EQUAL(false, c.inferRdfsResource());
	BOOST_CHECK_EQUAL(false, c.inferOwlThing());
}

// =========================================================================

BOOST_AUTO_TEST_CASE(testConfigReadFromFile)
{
	static const TChar k_fName[] = _T("tempConfigFileForConfigTest.txt");

	Config c;

	TString envVar(_T("PARLIAMENT_CONFIG_PATH="));
	envVar += k_fName;
	setEnvVar(envVar.c_str());

	{
		FileDeleter deleter(k_fName);

		ofstream s(k_fName);
		s << "# Parameters file for the Parliament core DLL" << endl;
		s << " \t # Parameters file for the Parliament core DLL" << endl;
		s << endl;
		s << "logToConsole             = yes" << endl;
		s << "logConsoleAsynchronous   = no" << endl;
		s << "logConsoleAutoFlush      = yes" << endl;
		s << "logToFile                = yes" << endl;
		s << "logFilePath              = log/ParliamentNative%3N_%Y-%m-%d_%H-%M-%S.log" << endl;
		s << "logFileAsynchronous      = no" << endl;
		s << "logFileAutoFlush         = yes" << endl;
		s << "logFileRotationSize      = 10485760" << endl;
		s << "logFileMaxAccumSize      = 157286400" << endl;
		s << "logFileMinFreeSpace      = 104857600" << endl;
		s << "logFileRotationTimePoint = 02:00:00" << endl;
		s << endl;
		s << "logLevel                 = INFO" << endl;
		s << "logChannelLevel          = KbInstance=ALL" << endl;
		s << "logChannelLevel          = StringToId=ALL" << endl;
		s << endl;
		s << "kbDirectoryPath=./subdir" << endl;
		s << "stmtFileName         =statements" << endl;
		s << "rsrcFileName      \t = resources" << endl;
		s << "uriTableFileName     = foo" << endl;
		s << "uriToIntFileName     = bar" << endl;
		s << endl;
		s << "  readOnly           = yes" << endl;
		s << "  fileSyncTimerDelay = 10000" << endl;
		s << "initialRsrcCapacity  = 100" << endl;
		s << " \t avgRsrcLen       = 100" << endl;
		s << "rsrcGrowthFactor     = 2 " << endl;
		s << "initialStmtCapacity  = 500 \t " << endl;
		s << "stmtGrowthFactor     = 2" << endl;
		s << "bdbCacheSize         = 512m,2" << endl;
		s << "normalizeTypedStringLiterals = no" << endl;
		s << endl;
		s << "runAllRulesAtStartup = yes" << endl;
		s << "enableSWRLRuleEngine = off" << endl;
		s << endl;
		s << "SubclassRule           = on" << endl;
		s << "SubpropertyRule        = on" << endl;
		s << "DomainRule             = on" << endl;
		s << "RangeRule              = on" << endl;
		s << "EquivalentClassRule    = on" << endl;
		s << "EquivalentPropRule     = on" << endl;
		s << "InverseOfRule          = on" << endl;
		s << "SymmetricPropRule      = on" << endl;
		s << "FunctionalPropRule     = off" << endl;
		s << "InvFunctionalPropRule  = on" << endl;
		s << "TransitivePropRule     = on" << endl;
		s << endl;
		s << "inferRdfsClass       = yes" << endl;
		s << "inferOwlClass        = no" << endl;
		s << "inferRdfsResource    = yes" << endl;
		s << "inferOwlThing        = no" << endl;
		s.close();

		BOOST_REQUIRE_NO_THROW(c = Config::readFromFile());

		BOOST_CHECK_EQUAL(true, c.logToConsole());
		BOOST_CHECK_EQUAL(false, c.logConsoleAsynchronous());
		BOOST_CHECK_EQUAL(true, c.logConsoleAutoFlush());
		BOOST_CHECK_EQUAL(true, c.logToFile());
		BOOST_CHECK_EQUAL(string("log/ParliamentNative%3N_%Y-%m-%d_%H-%M-%S.log"), c.logFilePath());
		BOOST_CHECK_EQUAL(false, c.logFileAsynchronous());
		BOOST_CHECK_EQUAL(true, c.logFileAutoFlush());
		BOOST_CHECK_EQUAL(10u * 1024u * 1024u, c.logFileRotationSize());
		BOOST_CHECK_EQUAL(150u * 1024u * 1024u, c.logFileMaxAccumSize());
		BOOST_CHECK_EQUAL(100u * 1024u * 1024u, c.logFileMinFreeSpace());
		BOOST_CHECK_EQUAL(string("02:00:00"), c.logFileRotationTimePoint());
		BOOST_CHECK_EQUAL(string("INFO"), c.logLevel());

		::std::map<string, string> expected;
		expected["KbInstance"] = "ALL";
		expected["StringToId"] = "ALL";
		BOOST_CHECK(equal(begin(expected), end(expected), begin(c.logChannelLevels())));

		BOOST_CHECK_EQUAL(string("./subdir"), c.kbDirectoryPath());
		BOOST_CHECK_EQUAL(string("statements"), c.stmtFileName());
		BOOST_CHECK_EQUAL(string("resources"), c.rsrcFileName());
		BOOST_CHECK_EQUAL(string("foo"), c.uriTableFileName());
		BOOST_CHECK_EQUAL(string("bar"), c.uriToIntFileName());

		BOOST_CHECK_EQUAL(true, c.readOnly());
		BOOST_CHECK_EQUAL(10000u, c.fileSyncTimerDelay());
		BOOST_CHECK_EQUAL(100u, c.initialRsrcCapacity());
		BOOST_CHECK_EQUAL(100u, c.avgRsrcLen());
		BOOST_CHECK_EQUAL(2.0, c.rsrcGrowthFactor());
		BOOST_CHECK_EQUAL(500u, c.initialStmtCapacity());
		BOOST_CHECK_EQUAL(2.0, c.stmtGrowthFactor());
		BOOST_CHECK_EQUAL(string("512m,2"), c.bdbCacheSize());
		BOOST_CHECK_EQUAL(false, c.normalizeTypedStringLiterals());

		BOOST_CHECK_EQUAL(true, c.runAllRulesAtStartup());
		BOOST_CHECK_EQUAL(false, c.enableSWRLRuleEngine());

		BOOST_CHECK_EQUAL(true, c.isSubclassRuleOn());
		BOOST_CHECK_EQUAL(true, c.isSubpropertyRuleOn());
		BOOST_CHECK_EQUAL(true, c.isDomainRuleOn());
		BOOST_CHECK_EQUAL(true, c.isRangeRuleOn());
		BOOST_CHECK_EQUAL(true, c.isEquivalentClassRuleOn());
		BOOST_CHECK_EQUAL(true, c.isEquivalentPropRuleOn());
		BOOST_CHECK_EQUAL(true, c.isInverseOfRuleOn());
		BOOST_CHECK_EQUAL(true, c.isSymmetricPropRuleOn());
		BOOST_CHECK_EQUAL(false, c.isFunctionalPropRuleOn());
		BOOST_CHECK_EQUAL(true, c.isInvFunctionalPropRuleOn());
		BOOST_CHECK_EQUAL(true, c.isTransitivePropRuleOn());

		BOOST_CHECK_EQUAL(true, c.inferRdfsClass());
		BOOST_CHECK_EQUAL(false, c.inferOwlClass());
		BOOST_CHECK_EQUAL(true, c.inferRdfsResource());
		BOOST_CHECK_EQUAL(false, c.inferOwlThing());
	}

	{
		FileDeleter deleter(k_fName);

		ofstream s(k_fName);
		s << "# Parameters file for the Parliament core DLL" << endl;
		s << endl;
		s << "logToConsole             = yes" << endl;
		s << "logConsoleAsynchronous   = no" << endl;
		s << "logConsoleAutoFlush      = yes" << endl;
		s << "logToFile                = yes" << endl;
		s << "logFilePath              = log/ParliamentNative%3N_%Y-%m-%d_%H-%M-%S.log" << endl;
		s << "logFileAsynchronous      = no" << endl;
		s << "logFileAutoFlush         = yes" << endl;
		s << "logFileRotationSize      = 10485760" << endl;
		s << "logFileMaxAccumSize      = 157286400" << endl;
		s << "logFileMinFreeSpace      = 104857600" << endl;
		s << "logFileRotationTimePoint = 02:00:00" << endl;
		s << endl;
		s << "logLevel                 = INFO" << endl;
		s << "logChannelLevel          = KbInstance=ALL" << endl;
		s << "logChannelLevel          = StringToId=ALL" << endl;
		s << endl;
		s << "kbDirectoryPath      = ./subdir" << endl;
		s << "stmtFileNameMessedUp = statements" << endl;	// this key is bad
		s << "rsrcFileName         = resources" << endl;
		s << "uriTableFileName     = foo" << endl;
		s << "uriToIntFileName     = bar" << endl;
		s << endl;
		s << "readOnly             = yes" << endl;
		s << "fileSyncTimerDelay   = 10000" << endl;
		s << "initialRsrcCapacity  = 100" << endl;
		s << "avgRsrcLen           = 100" << endl;
		s << "rsrcGrowthFactor     = 2" << endl;
		s << "initialStmtCapacity  = 500" << endl;
		s << "stmtGrowthFactor     = 2" << endl;
		s << "bdbCacheSize         = 512m,2" << endl;
		s << "normalizeTypedStringLiterals = no" << endl;
		s << endl;
		s << "runAllRulesAtStartup = no" << endl;
		s << "enableSWRLRuleEngine = no" << endl;
		s << endl;
		s << "SubclassRule           = on" << endl;
		s << "SubpropertyRule        = on" << endl;
		s << "DomainRule             = on" << endl;
		s << "RangeRule              = on" << endl;
		s << "EquivalentClassRule    = on" << endl;
		s << "EquivalentPropRule     = on" << endl;
		s << "InverseOfRule          = on" << endl;
		s << "SymmetricPropRule      = on" << endl;
		s << "FunctionalPropRule     = on" << endl;
		s << "InvFunctionalPropRule  = on" << endl;
		s << "TransitivePropRule     = on" << endl;
		s << endl;
		s << "inferRdfsClass       = yes" << endl;
		s << "inferOwlClass        = yes" << endl;
		s << "inferRdfsResource    = yes" << endl;
		s << "inferOwlThing        = yes" << endl;
		s.close();

		BOOST_CHECK_THROW(c = Config::readFromFile(), Exception);
	}

	setEnvVar(_T("PARLIAMENT_CONFIG_PATH="));

	BOOST_REQUIRE_NO_THROW(c = Config::readFromFile());
	Config defaults;

	BOOST_CHECK_EQUAL(defaults.logToConsole(), c.logToConsole());
	BOOST_CHECK_EQUAL(defaults.logConsoleAsynchronous(), c.logConsoleAsynchronous());
	BOOST_CHECK_EQUAL(defaults.logConsoleAutoFlush(), c.logConsoleAutoFlush());
	BOOST_CHECK_EQUAL(defaults.logToFile(), c.logToFile());
	BOOST_CHECK_EQUAL(defaults.logFilePath(), c.logFilePath());
	BOOST_CHECK_EQUAL(defaults.logFileAsynchronous(), c.logFileAsynchronous());
	BOOST_CHECK_EQUAL(defaults.logFileAutoFlush(), c.logFileAutoFlush());
	BOOST_CHECK_EQUAL(defaults.logFileRotationSize(), c.logFileRotationSize());
	BOOST_CHECK_EQUAL(defaults.logFileMaxAccumSize(), c.logFileMaxAccumSize());
	BOOST_CHECK_EQUAL(defaults.logFileMinFreeSpace(), c.logFileMinFreeSpace());
	BOOST_CHECK_EQUAL(defaults.logFileRotationTimePoint(), c.logFileRotationTimePoint());
	BOOST_CHECK_EQUAL(defaults.logLevel(), c.logLevel());
	BOOST_CHECK(equal(begin(defaults.logChannelLevels()), end(defaults.logChannelLevels()), begin(c.logChannelLevels())));

	BOOST_CHECK_EQUAL(defaults.kbDirectoryPath(), c.kbDirectoryPath());
	BOOST_CHECK_EQUAL(defaults.stmtFileName(), c.stmtFileName());
	BOOST_CHECK_EQUAL(defaults.rsrcFileName(), c.rsrcFileName());
	BOOST_CHECK_EQUAL(defaults.uriTableFileName(), c.uriTableFileName());
	BOOST_CHECK_EQUAL(defaults.uriToIntFileName(), c.uriToIntFileName());

	BOOST_CHECK_EQUAL(defaults.readOnly(), c.readOnly());
	BOOST_CHECK_EQUAL(defaults.fileSyncTimerDelay(), c.fileSyncTimerDelay());
	BOOST_CHECK_EQUAL(defaults.initialRsrcCapacity(), c.initialRsrcCapacity());
	BOOST_CHECK_EQUAL(defaults.avgRsrcLen(), c.avgRsrcLen());
	BOOST_CHECK_EQUAL(defaults.rsrcGrowthFactor(), c.rsrcGrowthFactor());
	BOOST_CHECK_EQUAL(defaults.initialStmtCapacity(), c.initialStmtCapacity());
	BOOST_CHECK_EQUAL(defaults.stmtGrowthFactor(), c.stmtGrowthFactor());
	BOOST_CHECK_EQUAL(defaults.bdbCacheSize(), c.bdbCacheSize());
	BOOST_CHECK_EQUAL(defaults.normalizeTypedStringLiterals(), c.normalizeTypedStringLiterals());

	BOOST_CHECK_EQUAL(defaults.runAllRulesAtStartup(), c.runAllRulesAtStartup());
	BOOST_CHECK_EQUAL(defaults.enableSWRLRuleEngine(), c.enableSWRLRuleEngine());

	BOOST_CHECK_EQUAL(defaults.isSubclassRuleOn(), c.isSubclassRuleOn());
	BOOST_CHECK_EQUAL(defaults.isSubpropertyRuleOn(), c.isSubpropertyRuleOn());
	BOOST_CHECK_EQUAL(defaults.isDomainRuleOn(), c.isDomainRuleOn());
	BOOST_CHECK_EQUAL(defaults.isRangeRuleOn(), c.isRangeRuleOn());
	BOOST_CHECK_EQUAL(defaults.isEquivalentClassRuleOn(), c.isEquivalentClassRuleOn());
	BOOST_CHECK_EQUAL(defaults.isEquivalentPropRuleOn(), c.isEquivalentPropRuleOn());
	BOOST_CHECK_EQUAL(defaults.isInverseOfRuleOn(), c.isInverseOfRuleOn());
	BOOST_CHECK_EQUAL(defaults.isSymmetricPropRuleOn(), c.isSymmetricPropRuleOn());
	BOOST_CHECK_EQUAL(defaults.isFunctionalPropRuleOn(), c.isFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(defaults.isInvFunctionalPropRuleOn(), c.isInvFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(defaults.isTransitivePropRuleOn(), c.isTransitivePropRuleOn());

	BOOST_CHECK_EQUAL(defaults.inferRdfsClass(), c.inferRdfsClass());
	BOOST_CHECK_EQUAL(defaults.inferOwlClass(), c.inferOwlClass());
	BOOST_CHECK_EQUAL(defaults.inferRdfsResource(), c.inferRdfsResource());
	BOOST_CHECK_EQUAL(defaults.inferOwlThing(), c.inferOwlThing());
}

BOOST_AUTO_TEST_SUITE_END()
