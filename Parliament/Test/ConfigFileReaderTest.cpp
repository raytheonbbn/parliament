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
#include <ostream>
#include "parliament/ConfigFileReader.h"
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
using ::std::ostream;

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

static constexpr TStringView k_testEnvVarName{_T("TEST_CONFIG_PATH")};
static constexpr TStringView k_testConfigFileName{_T("test-config.txt")};

BOOST_AUTO_TEST_CASE(getConfigFilePathTest)
{
	{	// Check the fallback, where the config file is in the current working directory:
		EnvVarReset envVarReset(k_testEnvVarName, _T(""));
		BOOST_CHECK_EQUAL(bfs::path{k_testConfigFileName},
			ConfigFileReader::testGetConfigFilePath(k_testEnvVarName, k_testConfigFileName));
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

BOOST_AUTO_TEST_SUITE_END()
