// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/filesystem.hpp>
#include <boost/range/iterator_range.hpp>
#include <boost/range/numeric.hpp>
#include <boost/test/unit_test.hpp>
#include <algorithm>
#include <iterator>
#include <string>
#include <string_view>
#include "parliament/Exceptions.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/Util.h"
#include "parliament/ArrayLength.h"
#include "parliament/UnicodeIterator.h"
#if defined(PARLIAMENT_WINDOWS)
#	include "parliament/Windows.h"
#else
#	include <unistd.h>
#endif

using namespace ::bbn::parliament;
using ::boost::filesystem::current_path;
using ::boost::filesystem::path;
using ::boost::make_iterator_range;
using ::std::count;
using ::std::string;
using ::std::string_view;

BOOST_AUTO_TEST_SUITE(UtilTestSuite)

static constexpr uint8 k_arrayLenTestData[] = { 0x01, 0x02, 0x03 };

BOOST_AUTO_TEST_CASE(testArrayLen)
{
	BOOST_CHECK_EQUAL(3u, arrayLen(k_arrayLenTestData));
}

static constexpr string_view k_levelStrings[] =
{
	"TRACE",
	"DEBUG",
	"INFO",
	"WARN",
	"ERROR"
};
static constexpr char k_levelList[] = "TRACE, DEBUG, INFO, WARN, ERROR";

BOOST_AUTO_TEST_CASE(testStringJoinOp)
{
	auto levelList = accumulate(make_iterator_range(k_levelStrings), string(), StringJoinOp(", "));
	BOOST_CHECK_EQUAL(k_levelList, levelList);
}

BOOST_AUTO_TEST_CASE(testHiResTimer)
{
	HiResTimer timer;
#if defined(PARLIAMENT_WINDOWS)
	::Sleep(1000);
#else
	::sleep(1);
#endif
	timer.stop();

	BOOST_CHECK(0.9 <= timer.getSec() && timer.getSec() <= 1.1);
	BOOST_CHECK(900.0 <= timer.getMilliSec() && timer.getMilliSec() <= 1100.0);
	BOOST_CHECK(900000.0 <= timer.getMicroSec() && timer.getMicroSec() <= 1100000.0);
}

BOOST_AUTO_TEST_CASE(testVersion)
{
	string ver = getKbVersion();

	BOOST_CHECK(ver.length() > 0);
	BOOST_CHECK(ver.find_first_not_of(".0123456789") == string::npos);
	BOOST_CHECK(count(begin(ver), end(ver), '.') == 3);
}

BOOST_AUTO_TEST_CASE(testGetEnv)
{
#if defined(PARLIAMENT_WINDOWS)
	auto varValue = tGetEnvVar(_T("ALLUSERSPROFILE"));
	BOOST_CHECK_EQUAL(convertTCharToUtf8(_T("C:\\ProgramData")),
		convertTCharToUtf8(varValue));
#else
	auto varValue = tGetEnvVar(_T("PWD"));
	BOOST_CHECK(!varValue.empty());
	auto varValuePath = convertUtf8ToPath(convertTCharToUtf8(varValue));
	BOOST_CHECK_EQUAL(current_path(), varValuePath);
#endif
}

BOOST_AUTO_TEST_SUITE_END()
