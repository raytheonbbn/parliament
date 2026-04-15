// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/test/unit_test.hpp>
#include <boost/test/data/monomorphic.hpp>
#include <boost/test/data/test_case.hpp>
#include <boost/filesystem.hpp>
#include <boost/format.hpp>
#include <iterator>
#include <ostream>
#include <rocksdb/db.h>
#include <string>
#include <string_view>
#include <stdexcept>
#include <vector>
#include "parliament/StringToId.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/Exceptions.h"
#include "parliament/KbConfig.h"
#include "parliament/UnicodeIterator.h"
#include "TestUtils.h"

namespace bdata = ::boost::unit_test::data;

using namespace ::bbn::parliament;
using ::boost::format;
using ::std::cbegin;
using ::std::cend;
using ::std::char_traits;
using ::std::exception;
using ::std::ostream;
using ::std::pair;
using ::std::string;
using ::std::string_view;
using ::std::unique_ptr;
using ::std::vector;

static const RsrcString			k_testKey1		= convertToRsrcChar("Hello World!");
static const RsrcString			k_testKey2		= convertToRsrcChar("Goodbye World!");
static constexpr ResourceId	k_testValue1	= 37;
static constexpr ResourceId	k_testValue2	= 43;
static constexpr string_view	k_fName			= "tempFile.db";

static void reportRocksDBError(rocksdb::Status& status, const char* pFileName, int lineNum)
{
	if (!status.ok())
	{
		throw Exception(
			format("RocksDB error, file \"%1%\" at line %2%: %3% (%4%/%5%)")
			% pFileName % lineNum % status.ToString() % status.code() % status.subcode());
	}
}
#define CHECK_ERR(err) reportRocksDBError((status), __FILE__, __LINE__);

BOOST_AUTO_TEST_SUITE(StringToIdTestSuite)

BOOST_AUTO_TEST_CASE(testStringToIdCorrectness)
{
	KbConfig kbConfig;
	kbConfig.readFromFile();
	kbConfig.uriToIntFileName(k_fName);
	kbConfig.readOnly(false);
	KbDeleter deleter{kbConfig, true};

	{
		StringToId s2i(kbConfig);
		BOOST_CHECK_EQUAL(k_nullRsrcId, s2i.find(k_testKey1));
		BOOST_CHECK_EQUAL(false, s2i.isMember(k_testKey1));

		BOOST_CHECK_NO_THROW(s2i.insert(k_testKey1, k_testValue1));
		BOOST_CHECK_EQUAL(k_testValue1, s2i.find(k_testKey1));
		BOOST_CHECK_EQUAL(true, s2i.isMember(k_testKey1));

		// Insert again to ensure insertion is idempotent:
		BOOST_CHECK_NO_THROW(s2i.insert(k_testKey1, k_testValue1));
		BOOST_CHECK_EQUAL(k_testValue1, s2i.find(k_testKey1));
		BOOST_CHECK_EQUAL(true, s2i.isMember(k_testKey1));

		BOOST_CHECK_NO_THROW(s2i.sync());
		BOOST_CHECK_NO_THROW(s2i.compact());
	}

	{
		StringToId s2i(kbConfig);
		BOOST_CHECK_EQUAL(k_testValue1, s2i.find(k_testKey1));
		BOOST_CHECK_EQUAL(true, s2i.isMember(k_testKey1));

		BOOST_CHECK_EQUAL(k_nullRsrcId, s2i.find(k_testKey2));
		BOOST_CHECK_EQUAL(false, s2i.isMember(k_testKey2));
	}

	{
		unique_ptr<rocksdb::DB> pDB;
		rocksdb::Options options;
		options.create_if_missing = true;
		auto status = rocksdb::DB::Open(options,
			pathAsUtf8(kbConfig.uriToIntFilePath()).c_str(), &pDB); CHECK_ERR(status);

		vector<pair<RsrcString, ResourceId>> contents;
		rocksdb::Iterator* pIter = pDB->NewIterator(rocksdb::ReadOptions());
		for (pIter->SeekToFirst(); pIter->Valid(); pIter->Next()) {
			BOOST_CHECK_EQUAL(0u, pIter->key().size() % sizeof(RsrcChar));
			BOOST_CHECK_EQUAL(sizeof(ResourceId), pIter->value().size());
			auto rsrcId = *reinterpret_cast<ResourceId*>(
				const_cast<char*>(pIter->value().data()));
			contents.push_back(make_pair(pIter->key().ToString(), rsrcId));
		}
		delete pIter;
		CHECK_ERR(status); // Check for any errors found during the scan

		BOOST_CHECK_EQUAL(1u, contents.size());
		BOOST_CHECK_EQUAL(k_testKey1, contents[0].first);
		BOOST_CHECK_EQUAL(k_testValue1, contents[0].second);
	}
}

BOOST_AUTO_TEST_CASE(testStrToIdEntryIterator)
{
	KbConfig kbConfig;
	kbConfig.readFromFile();
	kbConfig.uriToIntFileName(k_fName);
	kbConfig.readOnly(false);
	KbDeleter deleter{kbConfig, true};

	StringToId s2i(kbConfig);
	for (auto it = cbegin(s2i); it != cend(s2i); ++it)
	{
		BOOST_ERROR("Iteration over an empty StringToId yielded an entry");
	}

	BOOST_CHECK_NO_THROW(s2i.insert(k_testKey1, k_testValue1));
	BOOST_CHECK_NO_THROW(s2i.insert(k_testKey2, k_testValue2));
	BOOST_CHECK_EQUAL(k_testValue1, s2i.find(k_testKey1));
	BOOST_CHECK_EQUAL(k_testValue2, s2i.find(k_testKey2));

	size_t i = 0;
	for (auto it = cbegin(s2i); it != cend(s2i); ++it, ++i)
	{
		if (i == 0)
		{
			// Key 2 comes first because it's first in alphabetical order. Also, we
			// convert to UTF-8 for the comparison because of the lack of streaming
			// operators for UTF-16. (BOOST_CHECK_EQUAL requires streaming operators
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

	auto end1(cend(s2i));
	auto it1(cbegin(s2i));
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
