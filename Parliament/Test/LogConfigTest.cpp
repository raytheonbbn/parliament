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
#include "parliament/LogConfig.h"
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

static constexpr TChar k_envVar[] = _T("PARLIAMENT_LOG_CONFIG_PATH");
static constexpr TChar k_fName[] = _T("tempFileForLogConfigTest.txt");

BOOST_AUTO_TEST_SUITE(LogConfigTestSuite)

BOOST_AUTO_TEST_CASE(testConfigDefaultCtor)
{
	LogConfig c;

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
}

BOOST_AUTO_TEST_CASE(testConfigDefaultFileContainsDefaults)
{
	LogConfig defaults;
	LogConfig c;
	BOOST_REQUIRE_NO_THROW(c.readFromFile());

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
	BOOST_CHECK_EQUAL(defaults.logChannelLevels().size(), c.logChannelLevels().size());
	BOOST_CHECK(equal(begin(defaults.logChannelLevels()), end(defaults.logChannelLevels()), begin(c.logChannelLevels())));
}

static auto k_validTestConfig = u8R"~~~(
# Parameters file for the Parliament core DLL
 	 # Parameters file for the Parliament core DLL

logToConsole             = yes
logConsoleAsynchronous   = no
logConsoleAutoFlush      = yes
logToFile                = yes
logFilePath              = log/ParliamentNative%3N_%Y-%m-%d_%H-%M-%S.log
logFileAsynchronous      = no
logFileAutoFlush         = yes
logFileRotationSize      = 10485760
logFileMaxAccumSize      = 157286400
logFileMinFreeSpace      = 104857600
logFileRotationTimePoint = 02:00:00

logLevel                 = INFO
logChannelLevel          = KbInstance=ALL
logChannelLevel          = StringToId=ALL
)~~~";

BOOST_AUTO_TEST_CASE(testConfigReadFromFile)
{
	LogConfig c;

	EnvVarReset envVarReset(k_envVar, k_fName);
	FileDeleter filedeleter(k_fName);

	{
		ofstream s(k_fName);
		s << k_validTestConfig;
	}

	BOOST_REQUIRE_NO_THROW(c.readFromFile());

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
	BOOST_CHECK_EQUAL(expected.size(), c.logChannelLevels().size());
	BOOST_CHECK(equal(begin(expected), end(expected), begin(c.logChannelLevels())));
}

// Note that the key 'logToFileMessedUp' is bad:
static auto k_invalidTestConfig = u8R"~~~(
# Parameters file for the Parliament core DLL

logToConsole             = yes
logConsoleAsynchronous   = no
logConsoleAutoFlush      = yes
logToFileMessedUp        = yes
logFilePath              = log/ParliamentNative%3N_%Y-%m-%d_%H-%M-%S.log
logFileAsynchronous      = no
logFileAutoFlush         = yes
logFileRotationSize      = 10485760
logFileMaxAccumSize      = 157286400
logFileMinFreeSpace      = 104857600
logFileRotationTimePoint = 02:00:00

logLevel                 = INFO
logChannelLevel          = KbInstance=ALL
logChannelLevel          = StringToId=ALL
)~~~";

BOOST_AUTO_TEST_CASE(testConfigReadFromBadFile)
{
	LogConfig c;

	EnvVarReset envVarReset(k_envVar, k_fName);
	FileDeleter filedeleter(k_fName);

	{
		ofstream s(k_fName);
		s << k_invalidTestConfig;
	}

	BOOST_CHECK_THROW(c.readFromFile(), Exception);
}

BOOST_AUTO_TEST_SUITE_END()
