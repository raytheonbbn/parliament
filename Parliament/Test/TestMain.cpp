// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

// Uncomment this if using the static Boost.Test library that has been built with
// BOOST_TEST_ALTERNATIVE_INIT_API defined, or if using the dynamic library:
//#define BOOST_TEST_ALTERNATIVE_INIT_API

#include <boost/test/unit_test.hpp>
#include "parliament/Config.h"
#include "parliament/Log.h"

#include <exception>

using namespace ::bbn::parliament;
using ::boost::unit_test::framework::master_test_suite;

static auto g_log = Log::getSource("TestMain");

#if defined(BOOST_TEST_ALTERNATIVE_INIT_API) || defined(BOOST_ALL_DYN_LINK) || defined(BOOST_TEST_DYN_LINK)
bool init_unit_test()
#else
::boost::unit_test::test_suite* init_unit_test_suite(int /* argc */, char**const /* argv */)
#endif
{
	try
	{
		// Since logging can be initialized only once per process, these lines
		// ensure that logging is initialized from a known config, rather than
		// from an artificial configuration generated for the benefit of a test.

		// This is plenty good enough most of the time:
		auto config = Config();

		// Use this instead if you really need to bew config-file driven:
		//auto config{Config::readFromFile()};

		config.logToConsole(false);
		//config.logLevel("debug");
		//config.addLogChannelLevel("LiteralUtils", "debug");
		Log::init(config);

		master_test_suite().p_name.value = "Parliament Master Test Suite";
	}
	catch (const ::std::exception& ex)
	{
		try
		{
			// If we used Config::readFromFile() above and it failed, then we must init
			// the log with a known-good configuration before we can log the error:
			Log::init(Config());
		}
		catch (const ::std::exception& ex2)
		{
			BOOST_TEST_MESSAGE("Exception while initializing logging:  " << ex2.what());
		}

		BOOST_TEST_MESSAGE("Exception:  " << ex.what());
		PMNT_LOG(g_log, LogLevel::error) << "Exception:  " << ex.what();
	}
#if defined(BOOST_TEST_ALTERNATIVE_INIT_API) || defined(BOOST_ALL_DYN_LINK) || defined(BOOST_TEST_DYN_LINK)
	return true;
#else
	return nullptr;
#endif
}
