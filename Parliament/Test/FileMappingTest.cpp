// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/test/unit_test.hpp>
#include "parliament/FileHandle.h"
#include "parliament/FileMapping.h"
#include "parliament/CharacterLiteral.h"
#include "TestUtils.h"

using namespace ::bbn::parliament;
using ::std::vector;

BOOST_AUTO_TEST_SUITE(FileMappingTestSuite)

BOOST_AUTO_TEST_CASE(testFileMapping)
{
	static const TChar k_fName[] = _T("tempFile");
	static const char k_testString1[] = "The quick brown fox";
	static const char k_testString2[] = "A red-nosed unicorn jumps over the lazy dog.";

	FileDeleter deleter(k_fName);

	{
		FileHandle hFile(k_fName, false, strlen(k_testString1) + 1);

		FileMapping fileMapping(hFile);
		strcpy(reinterpret_cast<char*>(fileMapping.getBaseAddr()), k_testString1);
		BOOST_CHECK(strcmp(reinterpret_cast<char*>(fileMapping.getBaseAddr()), k_testString1) == 0);
		BOOST_CHECK_NO_THROW(fileMapping.sync());
		fileMapping.close();

		hFile.truncate(strlen(k_testString2) + 1);

		fileMapping.reopen(hFile);
		strcpy(reinterpret_cast<char*>(fileMapping.getBaseAddr()), k_testString2);
		BOOST_CHECK(strcmp(reinterpret_cast<char*>(fileMapping.getBaseAddr()), k_testString2) == 0);
	}

	vector<uint8> fileContent;
	readFileContents(k_fName, fileContent);
	BOOST_CHECK_EQUAL(size(fileContent), strlen(k_testString2) + 1);
	BOOST_CHECK(memcmp(&(fileContent[0]), k_testString2, size(fileContent)) == 0);
}

BOOST_AUTO_TEST_SUITE_END()
