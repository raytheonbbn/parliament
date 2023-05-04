// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#include <boost/lexical_cast.hpp>
#include <boost/test/unit_test.hpp>
#include <iterator>
#include <regex>
#include <string>
#include <vector>

#include "parliament/Types.h"
#include "parliament/Version.h"

using namespace ::bbn::parliament;
using ::boost::lexical_cast;
using ::std::equal;
using ::std::regex;
using ::std::smatch;
using ::std::string;
using ::std::vector;

static const char k_verNumRegEx[] = "^([0-9]+)\\.([0-9]+)\\.([0-9]+)\\.([0-9]+)$";

BOOST_AUTO_TEST_SUITE(VersionTestSuite)

BOOST_AUTO_TEST_CASE(testVersionNumber)
{
	auto verArray = vector<int>{PARLIAMENT_VERSION_NUMERIC};

	BOOST_CHECK_EQUAL(4u, size(verArray));
	BOOST_CHECK_EQUAL(0, verArray.back());

	auto verStr = string{PARLIAMENT_VERSION_STRING};

	auto rex = regex{k_verNumRegEx};
	smatch captures;
	BOOST_CHECK(regex_match(verStr, captures, rex));

	// We start at i == 1 because i == 0 is the whole-pattern match.
	auto parsedVersions = vector<int>{};
	for (size_t i = 1; i < size(captures); ++i)
	{
		parsedVersions.push_back(lexical_cast<int>(captures[i].str()));
	}

	BOOST_CHECK_EQUAL(size(verArray), size(parsedVersions));
	BOOST_CHECK(equal(cbegin(verArray), cend(verArray), cbegin(parsedVersions)));
}

BOOST_AUTO_TEST_SUITE_END()
