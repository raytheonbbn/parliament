// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

//TODO: Change this code to use a BDB Environment, and set the DB_DIRECT_DB flag on it

#include "parliament/StringToId.h"
#include "parliament/Exceptions.h"
#include "parliament/Log.h"
#include "parliament/RegEx.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"

#include <algorithm>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <db.h>

#if defined(USE_IN_MEMORY_LOOKUP_TABLE)
#include <boost/filesystem/fstream.hpp>
#endif

namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::boost::bad_lexical_cast;
using ::boost::format;
using ::boost::lexical_cast;

using ::std::bad_alloc;
using ::std::char_traits;
using ::std::copy;
using ::std::make_pair;
using ::std::string;

#if defined(USE_IN_MEMORY_LOOKUP_TABLE)
using ::std::getline;
using ::std::ios_base;
using ::std::make_pair;

using RsrcIFStream = bfs::basic_ifstream<pmnt::RsrcChar>;
using RsrcOFStream = bfs::basic_ofstream<pmnt::RsrcChar>;
#endif

static const char k_optionRegExStr[] = "^[ \t]*([0-9]+)[ \t]*([kKmMgG])[ \t]*,[ \t]*([0-9]+)[ \t]*$";

static auto g_log(pmnt::Log::getSource("StringToId"));

static void initInDbt(DBT& dbt, const void* pData, size_t dataLen)
{
	memset(&dbt, 0, sizeof(dbt));
	dbt.flags = DB_DBT_USERMEM;
	dbt.data = const_cast<void*>(pData);
	dbt.ulen = dbt.size = static_cast<pmnt::uint32>(dataLen);
}

static void initOutDbt(DBT& dbt, void* pBuffer, size_t bufferLen)
{
	memset(&dbt, 0, sizeof(dbt));
	dbt.flags = DB_DBT_USERMEM;
	dbt.data = pBuffer;
	dbt.ulen = static_cast<pmnt::uint32>(bufferLen);
}

//======================================================

pmnt::StrToIdEntryIterator::StrToIdEntryIterator() :
	m_pDB(nullptr),
	m_buffer(),
	m_curVal(nullValue()),
	m_pCursor(nullptr)
{
}

pmnt::StrToIdEntryIterator::StrToIdEntryIterator(DB* pDB) :
	m_pDB(pDB),
	m_buffer(k_initialBufferSize),
	m_curVal(make_pair(&(m_buffer[0]), k_nullRsrcId)),
	m_pCursor(createCursor(m_pDB))
{
	advanceCursor();
}

pmnt::StrToIdEntryIterator::StrToIdEntryIterator(const StrToIdEntryIterator& rhs) :
	m_pDB(rhs.m_pDB),
	m_buffer(rhs.m_buffer),
	m_curVal(m_pDB == nullptr ? nullValue() : make_pair(&(m_buffer[0]), rhs.m_curVal.second)),
	m_pCursor(m_pDB == nullptr ? nullptr : createCursor(m_pDB))
{
	if (m_pDB != nullptr)
	{
		setCursorPosition();
	}
}

pmnt::StrToIdEntryIterator& pmnt::StrToIdEntryIterator::operator=(
	const StrToIdEntryIterator& rhs)
{
	if (this != &rhs)
	{
		m_pDB = rhs.m_pDB;
		if (m_pDB == nullptr)
		{
			if (!m_buffer.empty())
			{
				m_buffer[0] = 0;
			}
			m_curVal = nullValue();
			closeCursor();
		}
		else
		{
			if (rhs.m_buffer.size() > m_buffer.size())
			{
				m_buffer.resize(rhs.m_buffer.size());
			}
			copy(begin(rhs.m_buffer), end(rhs.m_buffer), begin(m_buffer));
			m_curVal = make_pair(&(m_buffer[0]), rhs.m_curVal.second);
			if (m_pCursor == nullptr)
			{
				m_pCursor = createCursor(m_pDB);
			}

			setCursorPosition();
		}
	}
	return *this;
}

pmnt::StrToIdEntryIterator::~StrToIdEntryIterator()
{
	closeCursor();
}

// Preincrement:
pmnt::StrToIdEntryIterator& pmnt::StrToIdEntryIterator::operator++()
{
	advanceCursor();
	return *this;
}

// Postincrement:
pmnt::StrToIdEntryIterator pmnt::StrToIdEntryIterator::operator++(int)
{
	StrToIdEntryIterator copy(*this);
	advanceCursor();
	return copy;
}

bool pmnt::StrToIdEntryIterator::operator==(const StrToIdEntryIterator& rhs) const
{
	bool result = false;
	if (m_pDB == nullptr && rhs.m_pDB == nullptr)
	{
		result = true;
	}
	else if (m_pDB == rhs.m_pDB)
	{
		size_t lhsLen = char_traits<Buffer::value_type>::length(m_curVal.first);
		size_t rhsLen = char_traits<Buffer::value_type>::length(rhs.m_curVal.first);
		if (lhsLen == rhsLen && char_traits<Buffer::value_type>::compare(
			m_curVal.first, rhs.m_curVal.first, lhsLen) == 0)
		{
			result = true;
		}
	}
	return result;
}

DBC* pmnt::StrToIdEntryIterator::createCursor(DB* pDB)
{
	DBC* pCursor = nullptr;
	int err = pDB->cursor(pDB, nullptr, &pCursor, 0);
	if (err != 0)
	{
		throw Exception(format("Unable to create Berkeley DB cursor:  %1% (%2%)")
			% db_strerror(err) % err);
	}
	return pCursor;
}

void pmnt::StrToIdEntryIterator::closeCursor()
{
	if (m_pCursor != nullptr)
	{
		int err = m_pCursor->c_close(m_pCursor);
		m_pCursor = nullptr;
		if (err != 0)
		{
			PMNT_LOG(g_log, LogLevel::error)
				<< format("Unable to close Berkeley DB cursor:  %1% (%2%)")
					% db_strerror(err) % err;
		}
	}
}

// Intended to be called only from the copy ctor and the assignment operator
void pmnt::StrToIdEntryIterator::setCursorPosition()
{
	DBT key, val;
	initInDbt(key, m_curVal.first,
		char_traits<Buffer::value_type>::length(m_curVal.first) * sizeof(Buffer::value_type));
	initOutDbt(val, &m_curVal.second, sizeof(m_curVal.second));

	int err = m_pCursor->c_get(m_pCursor, &key, &val, DB_SET);
	if (err == 0)
	{
		// Do nothing
	}
	else if (err == DB_NOTFOUND)
	{
		// Convert this to an "end" iterator:
		closeCursor();
		m_pDB = nullptr;
	}
	else
	{
		closeCursor();
		m_pDB = nullptr;
		throw Exception(format("Unable to advance Berkeley DB cursor:  %1% (%2%)")
			% db_strerror(err) % err);
	}
}

//TODO: Replace "#if" with "if constexpr" (C++17) when all compilers support it:
void pmnt::StrToIdEntryIterator::checkBufferSizeDivisibleByCharSize(uint32 bufferSize)
{
#if defined(PARLIAMENT_RSRC_AS_UTF16)
	//if constexpr (sizeof(Buffer::value_type) > 1)
	//{
		if (bufferSize % sizeof(Buffer::value_type) != 0)
		{
			throw Exception(
				format("Berkeley DB buffer size %1% is not divisible by character size %2%")
				% bufferSize % sizeof(Buffer::value_type));
		}
	//}
#endif
}

// Intended to be called only from ctor and operator++
void pmnt::StrToIdEntryIterator::advanceCursor()
{
	while (m_pDB != nullptr)
	{
		DBT key, val;
		initOutDbt(key, m_curVal.first, (m_buffer.size() - 1) * sizeof(Buffer::value_type));
		initOutDbt(val, &m_curVal.second, sizeof(m_curVal.second));

		int err = m_pCursor->c_get(m_pCursor, &key, &val, DB_NEXT);
		if (err == 0)
		{
			checkBufferSizeDivisibleByCharSize(key.size);
			m_buffer[key.size / sizeof(Buffer::value_type)] = 0;
			break;
		}
		else if (err == DB_BUFFER_SMALL)
		{
			checkBufferSizeDivisibleByCharSize(key.size);
			m_buffer.resize(key.size / sizeof(Buffer::value_type) + 1);
		}
		else if (err == DB_NOTFOUND)
		{
			// Convert this to an "end" iterator:
			closeCursor();
			m_pDB = nullptr;
		}
		else
		{
			closeCursor();
			m_pDB = nullptr;
			throw Exception(format("Unable to advance Berkeley DB cursor:  %1% (%2%)")
				% db_strerror(err) % err);
		}
	}
}

// ======================================================================

pmnt::StringToId::StringToId(const bfs::path& filePath, const string& optionStr, bool readOnly) :
	m_filePath(filePath),
	m_optionStr(optionStr),
	m_readOnly(readOnly),
#if defined(USE_IN_MEMORY_LOOKUP_TABLE)
	m_db()
#else
	m_pDB(nullptr)
#endif
{
#if defined(USE_IN_MEMORY_LOOKUP_TABLE)
	RsrcIFStream strm(m_filePath, ios_base::in);
	while (true)
	{
		RsrcString rawRsrc, rsrc, rawId;
		getline(strm, rawRsrc);
		getline(strm, rawId);
		if (!strm && rawId.length() <= 0)
		{
			break;
		}

		bool lastCharWasBackslash = false;
		for (auto it = cBegin(rawRsrc); it != cEnd(rawRsrc); ++it)
		{
			RsrcChar ch = *it;
			if (lastCharWasBackslash)
			{
				switch (ch)
				{
				case 'n':
					rsrc += '\n';
					break;
				case 'r':
					rsrc += '\r';
					break;
				case '\\':
					rsrc += '\\';
					break;
				default:
					rsrc += '\\';
					rsrc += ch;
					break;
				}
				lastCharWasBackslash = false;
			}
			else if (ch == '\\')
			{
				lastCharWasBackslash = true;
			}
			else
			{
				rsrc += ch;
			}
		}

		size_t id = lexical_cast<size_t>(rawId);

		m_db.insert(make_pair(rsrc, id));
	}
#else
	auto opt = parseOptionString(m_optionStr);

	int err = db_create(&m_pDB, nullptr, 0);
	if (err != 0)
	{
		throw Exception(format("Unable to create Berkeley DB instance:  %1% (%2%)")
			% db_strerror(err) % err);
	}

	m_pDB->set_errfile(m_pDB, stderr);
	m_pDB->set_errpfx(m_pDB, "Berkeley DB");

	err = m_pDB->set_cachesize(m_pDB, opt.m_cacheGBytes, opt.m_cacheBytes, opt.m_numCacheSegments);
	if (err != 0)
	{
		m_pDB->close(m_pDB, 0);
		m_pDB = nullptr;
		throw Exception(format("Unable to set Berkeley DB cache size:  %1% (%2%)")
			% db_strerror(err) % err);
	}

	uint32 flags = DB_THREAD | (m_readOnly ? DB_RDONLY : DB_CREATE);
#if defined(PARLIAMENT_WINDOWS)
	err = m_pDB->open(m_pDB, nullptr, pathAsUtf8(m_filePath).c_str(), nullptr, DB_BTREE, flags, 0);
#else
	err = m_pDB->open(m_pDB, nullptr, m_filePath.c_str(), nullptr, DB_BTREE, flags, 0);
#endif
	if (err != 0)
	{
		m_pDB->close(m_pDB, 0);
		m_pDB = nullptr;
		throw Exception(format("Unable to open Berkeley DB:  %1% (%2%)")
			% db_strerror(err) % err);
	}
#endif
}

pmnt::StringToId::Options pmnt::StringToId::parseOptionString(const string& optionStr)
{
	RegEx rex = compileRegEx(k_optionRegExStr);
	SMatch captures;
	if (regExMatch(optionStr, captures, rex))
	{
		try
		{
			uint64 totalCacheSize = lexical_cast<uint64>(captures[1].str());
			char unit = captures[2].str()[0];
			switch (unit)
			{
			case 'k':
			case 'K':
				totalCacheSize *= 1024ul;
				break;
			case 'm':
			case 'M':
				totalCacheSize *= 1024ul * 1024ul;
				break;
			case 'g':
			case 'G':
				totalCacheSize *= 1024ul * 1024ul * 1024ul;
				break;
			}
			return Options{
				static_cast<uint32>(totalCacheSize / (1024ul * 1024ul * 1024ul)),	// cacheGBytes
				static_cast<uint32>(totalCacheSize % (1024ul * 1024ul * 1024ul)),	// cacheBytes
				lexical_cast<uint32>(captures[3].str())};									// numCacheSegments
		}
		catch (const bad_lexical_cast& ex)
		{
			throw Exception(format("Unable to parse bdbCacheSize option string \"%1%\" "
				"due to a numeric conversion error:  %2%") % optionStr % ex.what());
		}
	}
	else
	{
		throw Exception(format("Unable to parse bdbCacheSize option string \"%1%\" "
			"due to a syntax error") % optionStr);
	}
}

pmnt::StringToId::~StringToId()
{
#if defined(USE_IN_MEMORY_LOOKUP_TABLE)
	HiResTimer timer;
	if (!m_readOnly)
	{
		RsrcOFStream strm(m_filePath, ios_base::out | ios_base::trunc);
		for (const auto& entry : m_db)
		{
			for (auto ch : entry.first)
			{
				switch (ch)
				{
				case '\n':
					strm << "\\n";
					break;
				case '\r':
					strm << "\\r";
					break;
				case '\\':
					strm << "\\\\";
					break;
				default:
					strm << ch;
					break;
				}
			}
			strm << ::std::endl << entry.second << ::std::endl;
		}
	}
	timer.stop();
	PMNT_LOG(g_log, LogLevel::debug) << format("Time to write string-to-id map = %1% seconds")
		% timer.getSec();
#else
	if (m_pDB != nullptr)
	{
		int err = m_pDB->close(m_pDB, 0);
		m_pDB = nullptr;
		if (err != 0)
		{
			PMNT_LOG(g_log, LogLevel::error) << format("Unable to close Berkeley DB:  %1% (%2%)")
				% db_strerror(err) % err;
		}
	}
#endif
}

void pmnt::StringToId::sync()
{
#if !defined(USE_IN_MEMORY_LOOKUP_TABLE)
	if (!m_readOnly)
	{
		int err = m_pDB->sync(m_pDB, 0);
		if (err != 0)
		{
			throw Exception(format("Unable to sync Berkeley DB:  %1% (%2%)")
				% db_strerror(err) % err);
		}
	}
#endif
}

void pmnt::StringToId::compact()
{
	checkWritable();

#if !defined(USE_IN_MEMORY_LOOKUP_TABLE)
	// We compact 3 times to avoid leaving any empty pages in the middle of the file.
	// Compacting repeatedly tends to return these empty pages to the file system.
	for (int i = 0; i < 3; ++i)
	{
		DB_COMPACT compactData;
		memset(&compactData, 0, sizeof(compactData));
		int err = m_pDB->compact(m_pDB, nullptr, nullptr, nullptr, &compactData,
			DB_FREE_SPACE, nullptr);
		if (err != 0)
		{
			throw Exception(format("Unable to compact Berkeley DB:  %1% (%2%)")
				% db_strerror(err) % err);
		}
	}
#endif
}

size_t pmnt::StringToId::find(const RsrcChar* pKey, size_t keyLen) const
{
	if (pKey == nullptr)
	{
		throw Exception("StringToId::find called with a null key");
	}

	size_t result = k_nullRsrcId;

#if defined(USE_IN_MEMORY_LOOKUP_TABLE)
	RsrcString key(pKey, pKey + keyLen);
	auto it = m_db.find(key);
	if (it != m_db.end())
	{
		result = it->second;
	}
#else
	DBT key, val;
	initInDbt(key, pKey, keyLen * sizeof(*pKey));
	initOutDbt(val, &result, sizeof(result));

	int err = m_pDB->get(m_pDB, nullptr, &key, &val, 0);
	if (err != 0 && err != DB_NOTFOUND)
	{
		throw Exception(format("Unable to find key in Berkeley DB:  %1% (%2%)")
			% db_strerror(err) % err);
	}
#endif
	return result;
}

void pmnt::StringToId::insert(const RsrcChar* pKey, size_t keyLen, size_t value)
{
	checkWritable();

	if (pKey == nullptr)
	{
		throw Exception("StringToId::insert called with a null key");
	}
	if (keyLen <= 0)
	{
		throw Exception("StringToId::insert called with zero-length key");
	}
	if (value == k_nullRsrcId)
	{
		throw Exception("StringToId::insert called with a null value");
	}

#if defined(USE_IN_MEMORY_LOOKUP_TABLE)
	RsrcString key(pKey, pKey + keyLen);
	m_db.insert(make_pair(key, value));
#else
	DBT key, val;
	initInDbt(key, pKey, keyLen * sizeof(*pKey));
	initInDbt(val, &value, sizeof(value));

	int err = m_pDB->put(m_pDB, nullptr, &key, &val, DB_NOOVERWRITE);
	if (err != 0 && err != DB_KEYEXIST)
	{
		throw Exception(format("Unable to insert key in Berkeley DB:  %1% (%2%)")
			% db_strerror(err) % err);
	}
#endif
}

void pmnt::StringToId::checkWritable() const
{
	if (m_readOnly)
	{
		throw Exception(
			"Write operations are prohibited on read-only StringToId instances");
	}
}
