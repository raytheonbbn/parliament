// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/test/unit_test.hpp>
#include <boost/test/data/monomorphic.hpp>
#include <boost/test/data/test_case.hpp>
#include <limits>
#include <string>
#include "parliament/VarRecordTable.h"
#include "parliament/ArrayLength.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/UnicodeIterator.h"
#include "TestUtils.h"

namespace bdata = ::boost::unit_test::data;

using namespace ::bbn::parliament;
using ::std::char_traits;
using ::std::max;
using ::std::numeric_limits;
using ::std::size;
using ::std::string;
using ::std::vector;

// Used to declare char constants that adjust their width based on PARLIAMENT_RSRC_AS_UTF16:
#if defined(PARLIAMENT_RSRC_AS_UTF16)
#	define C(x) x,'\0',
#else
#	define C(x) x,
#endif

static constexpr TChar k_fName[] = _T("tempFile.mem");

static const RsrcString	k_testData1 = convertToRsrcChar("Hello World!");
static const RsrcString	k_testData2 = convertToRsrcChar("Goodbye World!");

static constexpr uint8 k_expectedResult[] =
{
	'P', 'a', 'r', 'l', 'i', 'a', 'm', 'e', 'n', 't', '\0', '\0',	// magic file format id
	0x04, 0x00,					// major version
	0x01, 0x00,					// minor version
	0x04, 0x03, 0x02, 0x01,	// BOM
#if defined(PARLIAMENT_64BITS)
#	if defined(PARLIAMENT_RSRC_AS_UTF16)
	0x05, 0x00, 0x00, 0x00,	// flags
#	else
	0x01, 0x00, 0x00, 0x00,	// flags
#	endif
#elif defined(PARLIAMENT_32BITS)
#	if defined(PARLIAMENT_RSRC_AS_UTF16)
	0x04, 0x00, 0x00, 0x00,	// flags
#	else
	0x00, 0x00, 0x00, 0x00,	// flags
#	endif
#else
#	error Parliament currently supports only 32-bit and 64-bit hardware architectures
#endif
	0x1c, 0x00, 0x00, 0x00,	// record count
#if defined(PARLIAMENT_64BITS)
	0x00, 0x00, 0x00, 0x00,	// extra bytes of 64-bit record count
#endif
	// Data:
	C('H')C('e')C('l')C('l')C('o')C(' ')C('W')C('o')C('r')C('l')C('d')C('!')C('\0')
	C('G')C('o')C('o')C('d')C('b')C('y')C('e')C(' ')C('W')C('o')C('r')C('l')C('d')C('!')C('\0')
};

static constexpr size_t k_growthIncrements[] = { 30u, 0u };
static constexpr double k_growthFactors[] = { 0.0, 2.0 };

BOOST_AUTO_TEST_SUITE(VarRecordTableTestSuite)

BOOST_DATA_TEST_CASE(
	testVarRecordTable,
	bdata::make(k_growthIncrements) ^ bdata::make(k_growthFactors),
	growthIncrement, growthFactor)
{
	size_t firstRecordOffset = 0;
	size_t secondRecordOffset = 0;

	FileDeleter deleter(k_fName);

	{
		VarRecordTable vrt(k_fName, false, k_testData1.length() + 1, growthIncrement, growthFactor);
		BOOST_CHECK(vrt.isEmpty());
		const size_t maxSizeT = numeric_limits<size_t>::max();
		BOOST_CHECK((maxSizeT - sizeof(MMapMgr::TblHeader)) / sizeof(RsrcChar) <= vrt.maxSize());
		BOOST_CHECK(vrt.maxSize() <= maxSizeT);
		BOOST_CHECK_EQUAL(0u, size(vrt));
		BOOST_CHECK_EQUAL(k_testData1.length() + 1, vrt.capacity());

		BOOST_CHECK_NO_THROW(firstRecordOffset = vrt.pushBack(k_testData1.c_str()));
		BOOST_CHECK_EQUAL(0u, firstRecordOffset);
		BOOST_CHECK(!vrt.isEmpty());
		BOOST_CHECK_EQUAL(k_testData1.length() + 1, size(vrt));
		BOOST_CHECK_EQUAL(k_testData1.length() + 1, vrt.capacity());

		BOOST_CHECK_NO_THROW(secondRecordOffset = vrt.pushBack(k_testData2.c_str()));
		BOOST_CHECK_EQUAL(k_testData1.length() + 1, secondRecordOffset);
		BOOST_CHECK_EQUAL(k_testData1.length() + k_testData2.length() + 2, size(vrt));
		BOOST_CHECK(vrt.capacity() >= k_testData1.length() + k_testData2.length() + 2);

		BOOST_CHECK_NO_THROW(vrt.sync());
	}

	{
		VarRecordTable vrt(k_fName, true, 4, growthIncrement, growthFactor);
		BOOST_CHECK(!vrt.isEmpty());
		BOOST_CHECK_EQUAL(k_testData1.length() + k_testData2.length() + 2, size(vrt));
		BOOST_CHECK(vrt.capacity() >= k_testData1.length() + k_testData2.length() + 2);

		const RsrcChar* pFirstRecord = vrt.getRecordAt(firstRecordOffset);
		BOOST_CHECK_EQUAL(k_testData1.length(), char_traits<RsrcChar>::length(pFirstRecord));
		BOOST_CHECK(k_testData1 == pFirstRecord);

		const RsrcChar* pSecondRecord = vrt.getRecordAt(secondRecordOffset);
		BOOST_CHECK_EQUAL(k_testData2.length(), char_traits<RsrcChar>::length(pSecondRecord));
		BOOST_CHECK(k_testData2 == pSecondRecord);

		BOOST_CHECK_THROW(vrt.getRecordAt(secondRecordOffset + k_testData2.length() + 1), Exception);
	}

	vector<uint8> fileContent;
	readFileContents(k_fName, fileContent);
	BOOST_CHECK(size(fileContent) >= arrayLen(k_expectedResult));
	BOOST_CHECK(memcmp(&(fileContent[0]), k_expectedResult, sizeof(k_expectedResult)) == 0);
}

// This test case reproduces bug #1381, "Bad URI offset in resource table
// (resources.mem)".  See the following Bugzilla entry for more information:
// http://loki.bbn.com/bugzilla/show_bug.cgi?id=1381
BOOST_AUTO_TEST_CASE(testVarRecordTableBug1381)
{
	// In order to raise the probability that the OS will re-map the file at a
	// different base address when the file grows, we start with a small file
	// but use a very large growth factor:
	static const size_t k_initialSize = 1024;
	static const size_t k_growthIncrement = 0;
#if defined(PARLIAMENT_64BITS)
	static const double k_growthFactor = 1024.0 * 1024.0;
#else
	static const double k_growthFactor = 64.0 * 1024.0;
#endif

	// Create a new VarRecordTable:
	FileDeleter deleter(k_fName);
	VarRecordTable vrt(k_fName, false, k_initialSize, k_growthIncrement, k_growthFactor);

	// Insert a tiny string that fits within the file's capacity so that
	// the file will not grow:
	RsrcString testData1(1, 'i');
	size_t offset1 = vrt.pushBack(testData1.c_str(), testData1.length());
	const RsrcChar* pBase = vrt.getRecordAt(0);
	BOOST_CHECK_EQUAL(0u, offset1);

	// Now insert a second string whose length takes it right up to the end
	// of the file's capacity, so that the terminating null (but only the
	// terminating null) will force the file to grow.  Because the growth
	// factor is very large, we can expect that the file will be re-mapped at
	// a different base address, which is what triggers the bug.
	RsrcString testData2(k_initialSize - testData1.length() - 1, 'i');
	size_t offset2 = vrt.pushBack(testData2.c_str(), testData2.length());
	BOOST_CHECK_EQUAL(offset1 + testData1.length() + 1, offset2);

	// Check that the file actually did get re-mapped at a different base
	// address.  Because it is possible that the OS will re-map at the same
	// address, this check is only at the warning level.
	const RsrcChar* pNewBase = vrt.getRecordAt(0);
	BOOST_WARN_MESSAGE(pNewBase != pBase, "The OS did not re-map the file "
		"at a new base address as expected -- for more information, see the "
		"comments in the code for the test case testVarRecordTableBug1381 "
		"(located in the file " __FILE__ ") and the Bugzilla entry, "
		"\"http://loki.bbn.com/bugzilla/show_bug.cgi?id=1381\"");
}

BOOST_AUTO_TEST_SUITE_END()
