// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#include <boost/lexical_cast.hpp>
#include <boost/test/unit_test.hpp>
#include <string>
#include <vector>

#include "parliament/RegEx.h"
#include "parliament/Util.h"
#include "parliament/Version.h"

using namespace ::bbn::parliament;
using ::boost::lexical_cast;
using ::std::equal;
using ::std::string;
using ::std::vector;

static const char k_verNumRegEx[] = "^([0-9]+)\\.([0-9]+)\\.([0-9]+)\\.([0-9]+)$";

BOOST_AUTO_TEST_SUITE(VersionTestSuite)

BOOST_AUTO_TEST_CASE(testVersionNumber)
{
	auto verArray = vector<int>{ PARLIAMENT_VERSION_NUMERIC };

	BOOST_CHECK_EQUAL(4u, verArray.size());
	BOOST_CHECK_EQUAL(0, verArray.back());

	auto verStr = string{ PARLIAMENT_VERSION_STRING };

	RegEx rex = compileRegEx(k_verNumRegEx);
	SMatch captures;
	BOOST_CHECK(regExMatch(verStr, captures, rex));

	// We start at i == 1 because i == 0 is the whole-pattern match.
	// Also, i is declared an int because although size() returns
	// size_t, operator[] takes an int parameter.
	auto parsedVersions = vector<int>{};
	for (int i = 1; static_cast<SMatch::size_type>(i) < captures.size(); ++i)
	{
		parsedVersions.push_back(lexical_cast<int>(captures[i].str()));
	}

	BOOST_CHECK_EQUAL(verArray.size(), parsedVersions.size());
	BOOST_CHECK(equal(cBegin(verArray), cEnd(verArray), cBegin(parsedVersions)));
}

BOOST_AUTO_TEST_SUITE_END()
