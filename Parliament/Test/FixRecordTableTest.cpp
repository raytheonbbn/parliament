// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/test/unit_test.hpp>
#include <limits>
#include <string>
#include "parliament/FixRecordTable.h"
#include "parliament/ArrayLength.h"
#include "parliament/CharacterLiteral.h"
#include "TestUtils.h"
#include "parliament/Log.h"
#include "parliament/KbRsrc.h"
#include "parliament/KbStmt.h"

using namespace ::bbn::parliament;
using ::std::numeric_limits;
using ::std::string;
using ::std::vector;

struct TestRecord
{
	uint8		m_field0;
	uint16	m_field1;
};

static const TestRecord k_testData1[] =
	{
		{ 0x01, 0x0101 },
		{ 0x02, 0x0202 },
		{ 0x03, 0x0303 },
		{ 0x04, 0x0404 }
	};
static const TestRecord k_testData2[] =
	{
		{ 0x05, 0x0505 },
		{ 0x06, 0x0606 },
		{ 0x07, 0x0707 },
		{ 0x08, 0x0808 }
	};
static const uint8 k_expectedResult[] =
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
		0x07, 0x00, 0x00, 0x00,	// record count
#if defined(PARLIAMENT_64BITS)
		0x00, 0x00, 0x00, 0x00,	// extra bytes of 64-bit record count
#endif
		0x01, 0x00, 0x01, 0x01,	// data
		0x02, 0x00, 0x02, 0x02,	// data
		0x03, 0x00, 0x03, 0x03,	// data
		0x04, 0x00, 0x04, 0x04,	// data
		0x05, 0x00, 0x05, 0x05,	// data
		0x06, 0x00, 0x06, 0x06,	// data
		0x07, 0x00, 0x07, 0x07	// data
	};
static const TChar k_fName[] = _T("tempFile");

static Log::Source g_log(Log::getSource("FixRecordTableTest"));

BOOST_AUTO_TEST_SUITE(FixRecordTableTestSuite)

// The constants k_firstRecOffset and k_recSize are declared as constexpr.
// Clang and GCC produce identical results if these are merely declared const,
// but Microsoft's compiler evaluates them as zero in this case.  However, all
// three compilers produce correct results with constexpr.  This test ensures
// we don't have a regression.
BOOST_AUTO_TEST_CASE(testFixRecordSizesAndOffsets)
{
	PMNT_LOG(g_log, LogLevel::info) << "FixRecordTable<KbStmt>::k_firstRecOffset = "
		<< FixRecordTable<KbStmt>::testFirstRecOffset();
	PMNT_LOG(g_log, LogLevel::info) << "FixRecordTable<KbStmt>::k_recSize        = "
		<< FixRecordTable<KbStmt>::testRecSize();
	PMNT_LOG(g_log, LogLevel::info) << "FixRecordTable<KbRsrc>::k_firstRecOffset = "
		<< FixRecordTable<KbRsrc>::testFirstRecOffset();
	PMNT_LOG(g_log, LogLevel::info) << "FixRecordTable<KbRsrc>::k_recSize        = "
		<< FixRecordTable<KbRsrc>::testRecSize();

	BOOST_CHECK(0ul != FixRecordTable<KbStmt>::testFirstRecOffset());
	BOOST_CHECK(0ul != FixRecordTable<KbStmt>::testRecSize());
	BOOST_CHECK(0ul != FixRecordTable<KbRsrc>::testFirstRecOffset());
	BOOST_CHECK(0ul != FixRecordTable<KbRsrc>::testRecSize());
}

BOOST_AUTO_TEST_CASE(testFixRecordTable)
{
	FileDeleter deleter(k_fName);

	{
		FixRecordTable<TestRecord> frt(k_fName, false, 4, 2.0);
		BOOST_CHECK(frt.isEmpty());
		const size_t maxSizeT = numeric_limits<size_t>::max();
		BOOST_CHECK((maxSizeT - sizeof(MMapMgr::TblHeader)) / sizeof(TestRecord) <= frt.maxSize());
		BOOST_CHECK(frt.maxSize() <= maxSizeT / sizeof(TestRecord));
		BOOST_CHECK_EQUAL(0u, frt.recordCount());
		BOOST_CHECK_EQUAL(4u, frt.capacity());

		for (size_t i = 0; i < arrayLen(k_testData1); ++i)
		{
			BOOST_CHECK_NO_THROW(frt.pushBack(k_testData1[i]));
			BOOST_CHECK(!frt.isEmpty());
			BOOST_CHECK_EQUAL(i + 1, frt.recordCount());
			BOOST_CHECK_EQUAL(4u, frt.capacity());
		}

		BOOST_CHECK_NO_THROW(frt.pushBack(k_testData2, arrayLen(k_testData2)));
		BOOST_CHECK_EQUAL(arrayLen(k_testData1) + arrayLen(k_testData2), frt.recordCount());
		const size_t finalCapacity = frt.capacity();
		BOOST_CHECK(finalCapacity >= arrayLen(k_testData1) + arrayLen(k_testData2));

		BOOST_CHECK_NO_THROW(frt.popBack());
		BOOST_CHECK_EQUAL(arrayLen(k_testData1) + arrayLen(k_testData2) - 1, frt.recordCount());
		BOOST_CHECK_EQUAL(finalCapacity, frt.capacity());

		BOOST_CHECK_NO_THROW(frt.releaseExcessCapacity());
		BOOST_CHECK_EQUAL(arrayLen(k_testData1) + arrayLen(k_testData2) - 1, frt.recordCount());
		BOOST_CHECK_EQUAL(frt.recordCount(), frt.capacity());

		BOOST_CHECK_NO_THROW(frt.syncAsynchronously());
		BOOST_CHECK_NO_THROW(frt.syncSynchronously());
	}

	{
		FixRecordTable<TestRecord> frt(k_fName, true, 4, 2.0);
		BOOST_CHECK(!frt.isEmpty());
		BOOST_CHECK_EQUAL(arrayLen(k_testData1) + arrayLen(k_testData2) - 1, frt.recordCount());
		BOOST_CHECK_EQUAL(frt.recordCount(), frt.capacity());

		for (size_t i = 0; i < arrayLen(k_testData1); ++i)
		{
			TestRecord& current = frt.getRecordAt(i);
			BOOST_CHECK_EQUAL(k_testData1[i].m_field0, current.m_field0);
			BOOST_CHECK_EQUAL(k_testData1[i].m_field1, current.m_field1);
		}

		for (size_t i = 0; i < arrayLen(k_testData2) - 1; ++i)
		{
			TestRecord& current = frt.getRecordAt(arrayLen(k_testData1) + i);
			BOOST_CHECK_EQUAL(k_testData2[i].m_field0, current.m_field0);
			BOOST_CHECK_EQUAL(k_testData2[i].m_field1, current.m_field1);
		}

		BOOST_CHECK_THROW(frt.getRecordAt(arrayLen(k_testData1) + arrayLen(k_testData2) - 1), Exception);
	}

	vector<uint8> fileContent;
	readFileContents(k_fName, fileContent);
	BOOST_CHECK_EQUAL(arrayLen(k_expectedResult), fileContent.size());
	BOOST_CHECK(memcmp(&(fileContent[0]), k_expectedResult, sizeof(k_expectedResult)) == 0);
}

BOOST_AUTO_TEST_SUITE_END()
