// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/test/unit_test.hpp>
#include <boost/test/data/monomorphic.hpp>
#include <boost/test/data/test_case.hpp>
#include <boost/format.hpp>
#include <ostream>
#include <string>
#include <stdexcept>
#include <db.h>
#include "parliament/StringToId.h"
#include "parliament/ArrayLength.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/Exceptions.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"
#include "TestUtils.h"

namespace bdata = ::boost::unit_test::data;

using namespace ::bbn::parliament;
using ::boost::format;
using ::std::char_traits;
using ::std::exception;
using ::std::ostream;

struct ParseTestCase
{
	uint32		m_cacheGBytes;
	uint32		m_cacheBytes;
	uint32		m_numCacheSegments;
	const char*	m_pOptions;
};

static const RsrcString		k_testKey1		= convertToRsrcChar("Hello World!");
static const RsrcString		k_testKey2		= convertToRsrcChar("Goodbye World!");
static const ResourceId		k_testValue1	= 37;
static const ResourceId		k_testValue2	= 43;
static const ParseTestCase	k_parseSuccessTests[]	=
	{
		{ 0, 32 * 1024 * 1024, 1, "32m,1" },
		{ 0, 32 * 1024 * 1024, 1, " 32 M , 1 " },
		{ 1, 476 * 1024 * 1024, 4, "1500m,4" },
		{ 2, 0, 16, "2g,16" },
		{ 2, 0, 16, "2G,16" },
		{ 2, 254490624, 0, "2345678k,0" },
		{ 3, 318526464, 128, " \t 3456789 \t K \t , \t 128 \t " },
	};
static const char*const		k_parseFailureTests[]	=
{
	"32m 1",
	"32,1",
	"32.5m,1",
	"32t,2",
	"32k",
	"23456789010123456789K, 32",
	"32m,5234567890",
};
static const TChar k_fName[] = _T("tempFile.db");
static const char k_options[] = "32m,1";


// Required to make BOOST_DATA_TEST_CASE happy:
static ostream& operator<<(ostream& os, const ParseTestCase& tc)
{
	os << tc.m_pOptions << ::std::endl;
	return os;
}

static void reportBdbError(int err, const char* pFileName, int lineNum)
{
	if (err != 0)
	{
		throw Exception(
			format("Berkeley DB error in file \"%1%\" at line number %2%:  %3% (%4%)")
			% pFileName % lineNum % db_strerror(err) % err);
	}
}

BOOST_AUTO_TEST_SUITE(StringToIdTestSuite)

BOOST_DATA_TEST_CASE(
	testStringToIdOptionParserOnGoodInput,
	bdata::make(k_parseSuccessTests),
	tc)
{
	BOOST_CHECK_NO_THROW(BerkeleyDbEnvOptions{tc.m_pOptions});
	auto opt = BerkeleyDbEnvOptions{tc.m_pOptions};

	BOOST_CHECK_EQUAL(tc.m_cacheGBytes, opt.m_cacheGBytes);
	BOOST_CHECK_EQUAL(tc.m_cacheBytes, opt.m_cacheBytes);
	BOOST_CHECK_EQUAL(tc.m_numCacheSegments, opt.m_numCacheSegments);
}

BOOST_DATA_TEST_CASE(
	testStringToIdOptionParserOnBadInput,
	bdata::make(k_parseFailureTests),
	pTC)
{
	BOOST_CHECK_THROW(BerkeleyDbEnvOptions{pTC}, Exception);
}

BOOST_AUTO_TEST_CASE(testStringToIdCorrectness)
{
	FileDeleter deleter(k_fName);

	{
		StringToId s2i(k_fName, k_options, false);
		BOOST_CHECK_EQUAL(k_nullRsrcId, s2i.find(k_testKey1));
		BOOST_CHECK_EQUAL(false, s2i.isMember(k_testKey1));

		BOOST_CHECK_NO_THROW(s2i.insert(k_testKey1, k_testValue1));
		BOOST_CHECK_EQUAL(k_testValue1, s2i.find(k_testKey1));
		BOOST_CHECK_EQUAL(true, s2i.isMember(k_testKey1));

		BOOST_CHECK_NO_THROW(s2i.sync());
		BOOST_CHECK_NO_THROW(s2i.compact());
	}

	{
		StringToId s2i(k_fName, k_options, true);
		BOOST_CHECK_EQUAL(k_testValue1, s2i.find(k_testKey1));
		BOOST_CHECK_EQUAL(true, s2i.isMember(k_testKey1));

		BOOST_CHECK_EQUAL(k_nullRsrcId, s2i.find(k_testKey2));
		BOOST_CHECK_EQUAL(false, s2i.isMember(k_testKey2));
	}

	DB* pDB = nullptr;
	{
		reportBdbError(
			db_create(&pDB, nullptr, 0),
			__FILE__, __LINE__);
		reportBdbError(
			pDB->open(pDB, nullptr, convertTCharToUtf8(k_fName).c_str(), nullptr, DB_UNKNOWN, DB_RDONLY | DB_THREAD, 0),
			__FILE__, __LINE__);

		DBC* pCursor = 0;
		reportBdbError(
			pDB->cursor(pDB, nullptr, &pCursor, 0),
			__FILE__, __LINE__);

		size_t numRecords = 0;
		for (;;)
		{
			RsrcChar keyBuffer[2 * sizeof(k_testKey1) + 6];
			DBT key;
			memset(&key, 0, sizeof(key));
			key.flags = DB_DBT_USERMEM;
			key.data = keyBuffer;
			key.ulen = sizeof(keyBuffer);

			ResourceId id;
			DBT value;
			memset(&value, 0, sizeof(value));
			value.flags = DB_DBT_USERMEM;
			value.data = &id;
			value.ulen = sizeof(id);

			int errCode = pCursor->c_get(pCursor, &key, &value, DB_NEXT);
			if (errCode == DB_NOTFOUND)
			{
				break;
			}
			else
			{
				reportBdbError(errCode, __FILE__, __LINE__);
			}

			++numRecords;

			BOOST_CHECK_EQUAL(0u, key.size % sizeof(RsrcChar));
			BOOST_CHECK_EQUAL(k_testKey1.length(), key.size / sizeof(RsrcChar));
			BOOST_CHECK(char_traits<RsrcChar>::compare(k_testKey1.c_str(), keyBuffer, k_testKey1.length()) == 0);

			BOOST_CHECK_EQUAL(sizeof(k_testValue1), static_cast<size_t>(value.size));
			BOOST_CHECK_EQUAL(k_testValue1, id);
		}

		if (pCursor != nullptr)
		{
			pCursor->c_close(pCursor);
		}

		if (pDB != nullptr)
		{
			pDB->close(pDB, 0);
		}

		BOOST_CHECK_EQUAL(1u, numRecords);
	}
}

BOOST_AUTO_TEST_CASE(testStrToIdEntryIterator)
{
	FileDeleter deleter(k_fName);
	StringToId s2i(k_fName, k_options, false);
	for (auto it = cBegin(s2i); it != cEnd(s2i); ++it)
	{
		BOOST_ERROR("Iteration over an empty StringToId yielded an entry");
	}

	BOOST_CHECK_NO_THROW(s2i.insert(k_testKey1, k_testValue1));
	BOOST_CHECK_NO_THROW(s2i.insert(k_testKey2, k_testValue2));
	BOOST_CHECK_EQUAL(k_testValue1, s2i.find(k_testKey1));
	BOOST_CHECK_EQUAL(k_testValue2, s2i.find(k_testKey2));

	size_t i = 0;
	for (auto it = cBegin(s2i); it != cEnd(s2i); ++it, ++i)
	{
		if (i == 0)
		{
			// Key 2 comes first because it's first in alphabetical order.  Also, we
			// convert to UTF-8 for the comparison because of the lack of streaming
			// operators for UTF-16.  (BOOST_CHECK_EQUAL requires streaming operators
			// internally to report errors.)
			BOOST_CHECK_EQUAL(convertFromRsrcChar(k_testKey2), convertFromRsrcChar(it->first));
			BOOST_CHECK_EQUAL(k_testValue2, it->second);
		}
		else if (i == 1)
		{
			BOOST_CHECK_EQUAL(convertFromRsrcChar(k_testKey1), convertFromRsrcChar(it->first));
			BOOST_CHECK_EQUAL(k_testValue1, it->second);
		}
		else
		{
			BOOST_ERROR("Iteration over a StringToId yielded too many entries");
		}
	}
	BOOST_CHECK_MESSAGE(i == 2,
		format("Iteration over a StringToId yielded too few entries (i == %1%)") % i);

	auto end1(cEnd(s2i));
	auto it1(cBegin(s2i));
	++it1;
	BOOST_CHECK_EQUAL(convertFromRsrcChar(k_testKey1), convertFromRsrcChar(it1->first));
	BOOST_CHECK_EQUAL(k_testValue1, it1->second);
	auto it2(it1++);
	BOOST_CHECK_EQUAL(convertFromRsrcChar(k_testKey1), convertFromRsrcChar(it2->first));
	BOOST_CHECK_EQUAL(k_testValue1, it2->second);
	BOOST_CHECK(s2i.end() == it1);
	it1 = it2;
	BOOST_CHECK_EQUAL(convertFromRsrcChar(k_testKey1), convertFromRsrcChar(it1->first));
	BOOST_CHECK_EQUAL(k_testValue1, it1->second);
	it1 = end1;
	BOOST_CHECK(s2i.end() == it1);
}

BOOST_AUTO_TEST_SUITE_END()
