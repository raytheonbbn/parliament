// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/test/unit_test.hpp>
#include <string>
#include "parliament/Exceptions.h"
#if defined(PARLIAMENT_WINDOWS)
#	include "parliament/Windows.h"
#else
#	include <errno.h>
#endif

using namespace ::bbn::parliament;
using ::std::string;

BOOST_AUTO_TEST_SUITE(ExceptionsTestSuite)

BOOST_AUTO_TEST_CASE(testGetSysErrMsg)
{
	static const SysErrCode k_fileNotFound =
#if defined(PARLIAMENT_WINDOWS)
		ERROR_FILE_NOT_FOUND;
#else
		ENOENT;
#endif

	static const char k_expectedErrMsg[] =
#if defined(PARLIAMENT_WINDOWS)
		"The system cannot find the file specified.";
#elif defined(PARLIAMENT_MACOS) || defined(PARLIAMENT_LINUX)
		"No such file or directory";
#else
#	error ExceptionsTest::testGetSysErrMsg has not been implemented for this platform.
#endif

	string errMsg = Exception::getSysErrMsg(k_fileNotFound);
	BOOST_CHECK_EQUAL(errMsg, k_expectedErrMsg);
}

BOOST_AUTO_TEST_SUITE_END()
