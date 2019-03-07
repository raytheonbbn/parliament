// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

// Uncomment this if using the static Boost.Test library that has been built with
// BOOST_TEST_ALTERNATIVE_INIT_API defined, or if using the dynamic library:
//#define BOOST_TEST_ALTERNATIVE_INIT_API

#include <boost/test/unit_test.hpp>
#include "parliament/Log.h"

using namespace ::bbn::parliament;
using ::boost::unit_test::framework::master_test_suite;

static auto g_log = log::getSource("TestMain");

#if defined(BOOST_TEST_ALTERNATIVE_INIT_API) || defined(BOOST_ALL_DYN_LINK) || defined(BOOST_TEST_DYN_LINK)
bool init_unit_test()
#else
::boost::unit_test::test_suite* init_unit_test_suite(int /* argc */, char**const /* argv */)
#endif
{
	master_test_suite().p_name.value = "Parliament Master Test Suite";
	PMNT_LOG(g_log, log::Level::info) << "Test suite:  " << master_test_suite().p_name.value;

#if defined(BOOST_TEST_ALTERNATIVE_INIT_API) || defined(BOOST_ALL_DYN_LINK) || defined(BOOST_TEST_DYN_LINK)
	return true;
#else
	return nullptr;
#endif
}
