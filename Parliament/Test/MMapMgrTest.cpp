// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/test/unit_test.hpp>
#include <string>
#include <vector>
#include "parliament/MMapMgr.h"
#include "parliament/ArrayLength.h"
#include "parliament/CharacterLiteral.h"
#include "TestUtils.h"

using namespace ::bbn::parliament;
using ::std::string;
using ::std::vector;

static const uint8 k_testData1[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
static const uint8 k_testData2[] = { 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10 };
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
	0x10, 0x00, 0x00, 0x00,	// record count
#if defined(PARLIAMENT_64BITS)
	0x00, 0x00, 0x00, 0x00,	// extra bytes of 64-bit record count
#endif
	0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,	// data
	0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10	// data
};
static const TChar k_fName[] = _T("tempFile");

struct TblAlignmentGuide1
{
	MMapMgr::TblHeader	m_header;
	uint8						m_recordArray[arrayLen(k_testData1)];
};

struct TblAlignmentGuide2
{
	MMapMgr::TblHeader	m_header;
	uint8						m_recordArray[arrayLen(k_testData1) + arrayLen(k_testData2)];
};

BOOST_AUTO_TEST_SUITE(MMapMgrTestSuite)

BOOST_AUTO_TEST_CASE(testMMapMgrTblHeader)
{
	string filePath("xyzzy");

	MMapMgr::TblHeader th;
	MMapMgr::TblHeader::initHeader(&th);
	BOOST_CHECK_NO_THROW(th.checkCompatibility(filePath));
	BOOST_CHECK_EQUAL(th.m_recordCount, 0u);

	MMapMgr::TblHeader::initHeader(&th);
	th.m_magic[1] = 'Z';
	BOOST_CHECK_THROW(th.checkCompatibility(filePath), Exception);

	MMapMgr::TblHeader::initHeader(&th);
	++th.m_majorVersion;
	BOOST_CHECK_THROW(th.checkCompatibility(filePath), Exception);

	MMapMgr::TblHeader::initHeader(&th);
	++th.m_minorVersion;
	BOOST_CHECK_THROW(th.checkCompatibility(filePath), Exception);

	MMapMgr::TblHeader::initHeader(&th);
	th.m_byteOrderMark = 0x04030201;
	BOOST_CHECK_THROW(th.checkCompatibility(filePath), Exception);
}

BOOST_AUTO_TEST_CASE(testMMapMgrGrowRecordCount)
{
	BOOST_CHECK(MMapMgr::growRecordCount(10, 2.0) >= 10000);			// Must grow to at least 10000 records
	BOOST_CHECK(MMapMgr::growRecordCount(20000, 0.01) >= 21000);	// Must grow by at least 1000 records
	BOOST_CHECK(MMapMgr::growRecordCount(20000, 1.5) >= 30000);		// Must grow by at least a factor of growthFactor
}

BOOST_AUTO_TEST_CASE(testMMapMgr)
{
	FileDeleter deleter(k_fName);
	//deleter.disableDeleteOnDestruct();
	{
		MMapMgr mmap(k_fName, false, sizeof(TblAlignmentGuide1));

		BOOST_CHECK_NO_THROW(memcpy(
			reinterpret_cast<TblAlignmentGuide1*>(mmap.baseAddr())->m_recordArray,
			k_testData1, sizeof(k_testData1)));
		BOOST_CHECK_NO_THROW(mmap.header().m_recordCount += sizeof(k_testData1));

		BOOST_CHECK_NO_THROW(mmap.reallocate(sizeof(TblAlignmentGuide2)));
		BOOST_CHECK_EQUAL(static_cast<FileHandle::FileSize>(sizeof(TblAlignmentGuide2)), mmap.fileSize());

		BOOST_CHECK_NO_THROW(memcpy(
			reinterpret_cast<TblAlignmentGuide2*>(mmap.baseAddr())->m_recordArray + sizeof(k_testData1),
			k_testData2, sizeof(k_testData2)));
		BOOST_CHECK_NO_THROW(mmap.header().m_recordCount += sizeof(k_testData2));

		BOOST_CHECK_NO_THROW(mmap.syncAsynchronously());
		BOOST_CHECK_NO_THROW(mmap.syncSynchronously());
	} // force the file mapping to be closed before we read it

	vector<uint8> fileContent;
	readFileContents(k_fName, fileContent);
	BOOST_CHECK_EQUAL(fileContent.size(), arrayLen(k_expectedResult));
	BOOST_CHECK(memcmp(&(fileContent[0]), k_expectedResult, sizeof(k_expectedResult)) == 0);
}

BOOST_AUTO_TEST_SUITE_END()
