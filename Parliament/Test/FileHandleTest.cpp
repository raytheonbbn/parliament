// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <algorithm>
#include <array>
#include <boost/filesystem/operations.hpp>
#include <boost/filesystem/path.hpp>
#include <boost/test/unit_test.hpp>
#include <iterator>
#include <vector>
#include "parliament/FileHandle.h"
#include "parliament/CharacterLiteral.h"
#include "TestUtils.h"

namespace bfs = ::boost::filesystem;

using namespace ::bbn::parliament;
using ::std::array;
using ::std::copy;
using ::std::vector;

BOOST_AUTO_TEST_SUITE(FileHandleTestSuite)

BOOST_AUTO_TEST_CASE(testFileCreate)
{
	static const TChar k_fName[] = _T("tempFile");

	FileDeleter deleter(k_fName);

	BOOST_CHECK(!exists(bfs::path(k_fName)));
	FileHandle hFile(k_fName, true);
	BOOST_CHECK(hFile.getInternalFilehandle() != FileHandle::k_nullFileHandle);
	BOOST_CHECK(hFile.getFilePath() == k_fName);
	BOOST_CHECK(hFile.isReadOnly());
	BOOST_CHECK(hFile.wasCreatedOnOpen());

	// Check that the file is really read-only:
	static const uint8 byteToWrite = 0;
	BOOST_CHECK_THROW(hFile.write(&byteToWrite, 1), Exception);

	BOOST_CHECK_NO_THROW(hFile.close());
	BOOST_CHECK(exists(bfs::path(k_fName)));
	BOOST_CHECK_NO_THROW(remove(bfs::path(k_fName)));
	BOOST_CHECK(!exists(bfs::path(k_fName)));
}

BOOST_AUTO_TEST_CASE(testFileOpen)
{
	static const TChar k_fName[] = _T("tempFile");

	FileDeleter deleter(k_fName);

	vector<uint8> fileContent;
	writeBytesToFile(k_fName, fileContent);

	FileHandle hFile(k_fName, false);
	BOOST_CHECK(hFile.getInternalFilehandle() != FileHandle::k_nullFileHandle);
	BOOST_CHECK(hFile.getFilePath() == k_fName);
	BOOST_CHECK(!hFile.isReadOnly());
	BOOST_CHECK(!hFile.wasCreatedOnOpen());

	static const char k_buffer1[] = "Hello ";
	static const char k_buffer2[] = "hello";
	static const char k_buffer3[] = "World!";
	static const char k_result[] = "Hello World!";
	BOOST_CHECK_NO_THROW(hFile.write(reinterpret_cast<const uint8*>(k_buffer1), static_cast<uint32>(strlen(k_buffer1))));
	BOOST_CHECK_NO_THROW(hFile.write(reinterpret_cast<const uint8*>(k_buffer2), static_cast<uint32>(strlen(k_buffer2))));
	BOOST_CHECK(hFile.seek(strlen(k_buffer1), SeekMethod::k_start) == strlen(k_buffer1));
	BOOST_CHECK_NO_THROW(hFile.write(reinterpret_cast<const uint8*>(k_buffer3), static_cast<uint32>(strlen(k_buffer3))));

	BOOST_CHECK_NO_THROW(hFile.sync());
	BOOST_CHECK(hFile.getFileSize() == strlen(k_result));
	BOOST_CHECK_NO_THROW(hFile.close());

	readFileContents(k_fName, fileContent);

	BOOST_CHECK_EQUAL(size(fileContent), strlen(k_result));
	BOOST_CHECK_EQUAL(size(fileContent), file_size(bfs::path(k_fName)));
	BOOST_CHECK(memcmp(&(fileContent[0]), k_result, size(fileContent)) == 0);
}

BOOST_AUTO_TEST_CASE(testFileTruncate)
{
	static const TChar k_fName[] = _T("tempFile");

	FileDeleter deleter(k_fName);

	{
		FileHandle hFile(k_fName, false);
		BOOST_CHECK(hFile.getInternalFilehandle() != FileHandle::k_nullFileHandle);
		BOOST_CHECK(hFile.getFilePath() == k_fName);
		BOOST_CHECK(!hFile.isReadOnly());
		BOOST_CHECK(hFile.wasCreatedOnOpen());
		BOOST_CHECK(hFile.getFileSize() == 0);
	}

	BOOST_CHECK_EQUAL(file_size(bfs::path(k_fName)), 0u);

	{
		FileHandle hFile(k_fName, false);
		BOOST_CHECK(hFile.getInternalFilehandle() != FileHandle::k_nullFileHandle);
		BOOST_CHECK(hFile.getFilePath() == k_fName);
		BOOST_CHECK(!hFile.isReadOnly());
		BOOST_CHECK(!hFile.wasCreatedOnOpen());
		BOOST_CHECK_NO_THROW(hFile.truncate(8192));
		BOOST_CHECK(hFile.getFileSize() == 8192);
	}

	BOOST_CHECK_EQUAL(file_size(bfs::path(k_fName)), 8192u);

	{
		FileHandle hFile(k_fName, false);
		BOOST_CHECK(hFile.getInternalFilehandle() != FileHandle::k_nullFileHandle);
		BOOST_CHECK(hFile.getFilePath() == k_fName);
		BOOST_CHECK(!hFile.isReadOnly());
		BOOST_CHECK(!hFile.wasCreatedOnOpen());
		BOOST_CHECK_NO_THROW(hFile.truncate(1024));
		BOOST_CHECK(hFile.getFileSize() == 1024);
	}

	BOOST_CHECK_EQUAL(file_size(bfs::path(k_fName)), 1024u);
}

static constexpr TChar k_fName[] = _T("tempFile");
static constexpr array<uint8, 40> k_first40bytes{
	'0', '1', '2', '3', '4', '5', '6', '7',
	'0', '1', '2', '3', '4', '5', '6', '7',
	'0', '1', '2', '3', '4', '5', '6', '7',
	'0', '1', '2', '3', '4', '5', '6', '7',
	'0', '1', '2', '3', '4', '5', '6', '7' };
static constexpr array<uint8, 8> k_last8bytes{
	'0', '1', '2', '3', '4', '5', '6', '7' };

BOOST_AUTO_TEST_CASE(testGrowingFile)
{
	FileDeleter deleter(k_fName);

	{
		FileHandle hFile(k_fName, false);
		BOOST_CHECK(hFile.getInternalFilehandle() != FileHandle::k_nullFileHandle);
		BOOST_CHECK(hFile.getFilePath() == k_fName);
		BOOST_CHECK(!hFile.isReadOnly());
		BOOST_CHECK(hFile.wasCreatedOnOpen());
		BOOST_CHECK(hFile.getFileSize() == 0);

		auto bytesWritten = hFile.write(data(k_first40bytes), size(k_first40bytes));
		BOOST_CHECK_EQUAL(bytesWritten, size(k_first40bytes));
		BOOST_CHECK(hFile.getFileSize() == size(k_first40bytes));
	}

	BOOST_CHECK_EQUAL(file_size(bfs::path(k_fName)), size(k_first40bytes));
	vector<uint8> fileContent;
	readFileContents(k_fName, fileContent);
	BOOST_CHECK_EQUAL(size(k_first40bytes), size(fileContent));
	BOOST_CHECK(memcmp(&(fileContent[0]), data(k_first40bytes), size(k_first40bytes)) == 0);

	{
		FileHandle hFile(k_fName, false);
		BOOST_CHECK(hFile.getInternalFilehandle() != FileHandle::k_nullFileHandle);
		BOOST_CHECK(hFile.getFilePath() == k_fName);
		BOOST_CHECK(!hFile.isReadOnly());
		BOOST_CHECK(!hFile.wasCreatedOnOpen());
		BOOST_CHECK_NO_THROW(hFile.truncate(size(k_first40bytes) + size(k_last8bytes)));
		BOOST_CHECK(hFile.getFileSize() == size(k_first40bytes) + size(k_last8bytes));
	}

	BOOST_CHECK_EQUAL(file_size(bfs::path(k_fName)), size(k_first40bytes) + size(k_last8bytes));
	readFileContents(k_fName, fileContent);
	BOOST_CHECK_EQUAL(size(k_first40bytes) + size(k_last8bytes), size(fileContent));
	vector<uint8> expectedFileContent(size(k_first40bytes) + size(k_last8bytes), 0);
	copy(begin(k_first40bytes), end(k_first40bytes), begin(expectedFileContent));
	BOOST_CHECK(memcmp(&(fileContent[0]), &(expectedFileContent[0]), size(k_first40bytes) + size(k_last8bytes)) == 0);

	{
		FileHandle hFile(k_fName, false);
		BOOST_CHECK(hFile.getInternalFilehandle() != FileHandle::k_nullFileHandle);
		BOOST_CHECK(hFile.getFilePath() == k_fName);
		BOOST_CHECK(!hFile.isReadOnly());
		BOOST_CHECK(!hFile.wasCreatedOnOpen());

		FileHandle::FileSize filePos = hFile.seek(size(k_first40bytes), SeekMethod::k_start);
		BOOST_CHECK_EQUAL(filePos, size(k_first40bytes));
		auto bytesWritten = hFile.write(data(k_last8bytes), size(k_last8bytes));
		BOOST_CHECK_EQUAL(bytesWritten, size(k_last8bytes));
		BOOST_CHECK(hFile.getFileSize() == size(k_first40bytes) + size(k_last8bytes));
	}

	BOOST_CHECK_EQUAL(file_size(bfs::path(k_fName)), size(k_first40bytes) + size(k_last8bytes));
	readFileContents(k_fName, fileContent);
	BOOST_CHECK_EQUAL(size(k_first40bytes) + size(k_last8bytes), size(fileContent));
	copy(begin(k_last8bytes), end(k_last8bytes), begin(expectedFileContent) + size(k_first40bytes));
	BOOST_CHECK(memcmp(&(fileContent[0]), &(expectedFileContent[0]), size(k_first40bytes) + size(k_last8bytes)) == 0);
}

BOOST_AUTO_TEST_SUITE_END()
