// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/test/unit_test.hpp>
#include <algorithm>
#include <iterator>
#include <string>
#include "parliament/Exceptions.h"
#include "parliament/Util.h"
#include "parliament/ArrayLength.h"
#if defined(PARLIAMENT_WINDOWS)
#	include "parliament/Windows.h"
#else
#	include <unistd.h>
#endif

using namespace ::bbn::parliament;
using ::std::count;
using ::std::string;

BOOST_AUTO_TEST_SUITE(UtilTestSuite)

BOOST_AUTO_TEST_CASE(testArrayLen)
{
	static const uint8 k_testData[] = { 0x01, 0x02, 0x03 };

	BOOST_CHECK_EQUAL(
		sizeof(k_testData) / sizeof(k_testData[0]),
		arrayLen(k_testData));
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

BOOST_AUTO_TEST_SUITE_END()
