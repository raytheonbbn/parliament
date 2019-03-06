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

static constexpr TChar k_defaultConfigFileName[] = _T("ParliamentKbConfig.txt");
static const GetConfigFilePathTestCase k_getConfigFilePathTestCases[] =
	{
#if defined(PARLIAMENT_WINDOWS)
		{ _T("PARLIAMENT_KB_CONFIG_PATH=C:\\Config.txt"),	bfs::path{_T("C:\\Config.txt")} },
#else
		{ _T("PARLIAMENT_KB_CONFIG_PATH=/config.txt"),		bfs::path{_T("/config.txt")} },
#endif
		{ _T("PARLIAMENT_KB_CONFIG_PATH="),						getConfigFilePath(k_defaultConfigFileName) },
	};

BOOST_DATA_TEST_CASE(
	testConfigGetConfigFilePath,
	bdata::make(k_getConfigFilePathTestCases),
	tc)
{
	TString envVar{tc.m_pEnvVar};
	TString::size_type i = envVar.find(_T('='));
	BOOST_CHECK(i != TString::npos);
	TString envVarName{tc.m_pEnvVar, 0, i};
	TString envVarValue{tc.m_pEnvVar, i + 1, envVar.size()};

	EnvVarReset envVarReset(envVarName, envVarValue);

	BOOST_CHECK_EQUAL(tc.m_expectedResult,
		Config::testGetConfigFilePath(envVarName.c_str(), k_defaultConfigFileName));
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

BOOST_AUTO_TEST_SUITE_END()
