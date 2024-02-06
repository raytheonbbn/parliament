// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <algorithm>
#include <array>
#include <boost/filesystem/operations.hpp>
#include <boost/filesystem/path.hpp>
#include <boost/test/unit_test.hpp>
#include <boost/test/data/test_case.hpp>
#include <fstream>
#include <iterator>
#include <map>
#include <ostream>
#include <string>
#include <system_error>
#include "parliament/ConfigFileReader.h"
#include "parliament/Exceptions.h"
#include "parliament/Windows.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/UnicodeIterator.h"
#include "TestUtils.h"

#if !defined(PARLIAMENT_WINDOWS)
#	include <dlfcn.h>
#endif

namespace bdata = ::boost::unit_test::data;
namespace bfs = ::boost::filesystem;

using namespace ::bbn::parliament;
using ::std::array;
using ::std::endl;
using ::std::equal;
using ::std::ofstream;
using ::std::ostream;
using ::std::string;

// =========================================================================

static bfs::path getRunningDllFilePath()
{
#if defined(PARLIAMENT_WINDOWS)
	array<TChar, 8192> buffer;
	DWORD errCode = ::GetModuleFileName(0, data(buffer), static_cast<DWORD>(size(buffer)));
	if (errCode <= 0 || errCode >= size(buffer))
	{
		throw ::std::system_error(::GetLastError(), ::std::system_category(),
			"GetModuleFileName failed in unit test");
	}
	return data(buffer);
#else
	::Dl_info info;
	if (::dladdr(reinterpret_cast<const void*>(getRunningDllFilePath), &info) == 0)
	{
		throw ::std::system_error(errno, ::std::system_category(),
			"dladdr failed in unit test");
	}
	return info.dli_fname;
#endif
}

BOOST_AUTO_TEST_SUITE(ConfigFileReaderTestSuite)

static constexpr TChar k_testEnvVarName[] = _T("TEST_CONFIG_PATH");
static constexpr TChar k_testConfigFileName[] = _T("test-config.txt");

BOOST_AUTO_TEST_CASE(getConfigFilePathTest)
{
	{	// Check the fallback, where the config file is in the current working directory:
		EnvVarReset envVarReset(k_testEnvVarName, _T(""));
		BOOST_CHECK_EQUAL(convertTCharToUtf8(k_testConfigFileName),
			convertTCharToUtf8(ConfigFileReader::testGetConfigFilePath(
				k_testEnvVarName, k_testConfigFileName).native()));
	}
	{	// Check that setting the env var works:
		auto envVarValue = bfs::current_path();
		envVarValue /= _T("..");
		envVarValue /= _T("KbCore");
		envVarValue /= k_testConfigFileName;

		FileDeleter deleter(envVarValue);
		touchFile(envVarValue);

		envVarValue = canonical(envVarValue);
		EnvVarReset envVarReset(k_testEnvVarName, envVarValue.native());

		BOOST_CHECK_EQUAL(envVarValue, canonical(
			ConfigFileReader::testGetConfigFilePath(k_testEnvVarName, k_testConfigFileName)));
	}
	{	// Check that loading from the same directory as the DLL works:
		auto configPath = getRunningDllFilePath().parent_path();
		configPath /= k_testConfigFileName;

		FileDeleter deleter(configPath);
		touchFile(configPath);

		configPath = canonical(configPath);

		BOOST_CHECK_EQUAL(configPath, canonical(
			ConfigFileReader::testGetConfigFilePath(k_testEnvVarName, k_testConfigFileName)));
	}
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
	isBlankOrCommentLineTest,
	bdata::make(k_isBlankOrCommentLineTestCases),
	tc)
{
	BOOST_CHECK(ConfigFileReader::testIsBlankOrCommentLine(tc.m_pInputStr) == tc.m_expectedResult);
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
	parseUnsignedTest,
	bdata::make(k_parseUnsignedTestCases),
	tc)
{
	if (tc.m_shouldThrow)
	{
		BOOST_CHECK_THROW(ConfigFileReader::parseUnsigned(tc.m_pInputStr, 1), Exception);
	}
	else
	{
		BOOST_CHECK_EQUAL(tc.m_expectedResult, ConfigFileReader::parseUnsigned(tc.m_pInputStr, 1));
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
	parseDoubleTest,
	bdata::make(k_parseDoubleTestCases),
	tc)
{
	if (tc.m_shouldThrow)
	{
		BOOST_CHECK_THROW(ConfigFileReader::parseDouble(tc.m_pInputStr, 1), Exception);
	}
	else
	{
		BOOST_CHECK_EQUAL(tc.m_expectedResult, ConfigFileReader::parseDouble(tc.m_pInputStr, 1));
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
	parseBoolTest,
	bdata::make(k_parseBoolTestCases),
	tc)
{
	if (tc.m_shouldThrow)
	{
		BOOST_CHECK_THROW(ConfigFileReader::parseBool(tc.m_pInputStr, 1), Exception);
	}
	else
	{
		BOOST_CHECK_EQUAL(tc.m_expectedResult, ConfigFileReader::parseBool(tc.m_pInputStr, 1));
	}
}

BOOST_AUTO_TEST_SUITE_END()
