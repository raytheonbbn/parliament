// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

//TODO: Change this code to use a BDB Environment, and set the DB_DIRECT_DB flag on it

#include "parliament/StringToId.h"
#include "parliament/Exceptions.h"
#include "parliament/Log.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"

#include <algorithm>
#include <boost/filesystem.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <db.h>
#include <mutex>
#include <regex>

namespace pmnt = ::bbn::parliament;

using Mutex = ::std::recursive_mutex;
using Lock = ::std::lock_guard<Mutex>;

using ::boost::format;
using ::boost::lexical_cast;

using ::std::char_traits;
using ::std::copy;
using ::std::make_pair;
using ::std::regex;
using ::std::string;
using ::std::string_view;
using svmatch = ::std::match_results<::std::string_view::const_iterator>;

static constexpr char k_bdbErrorPrefix[] = "Berkeley DB";
static constexpr char k_optionRegExStr[] = "^[ \t]*([0-9]+)[ \t]*([kKmMgG])[ \t]*,[ \t]*([0-9]+)[ \t]*$";

static auto g_environmentMutex = Mutex{};
static auto g_log(pmnt::log::getSource("StringToId"));

template <typename DataElement>
static auto initInputDbt(DBT& dbt, const DataElement* pData, size_t numElements) -> void
{
	memset(&dbt, 0, sizeof(dbt));
	dbt.flags = DB_DBT_USERMEM;
	dbt.data = reinterpret_cast<void*>(const_cast<DataElement*>(pData));
	dbt.ulen = dbt.size = static_cast<pmnt::uint32>(numElements * sizeof(*pData));
}

template <typename DataElement>
static auto initOutputDbt(DBT& dbt, DataElement* pBuffer, size_t numElements) -> void
{
	memset(&dbt, 0, sizeof(dbt));
	dbt.flags = DB_DBT_USERMEM;
	dbt.data = pBuffer;
	dbt.ulen = static_cast<pmnt::uint32>(numElements * sizeof(*pBuffer));
}

// ======================================================================

pmnt::BerkeleyDbEnvOptions::BerkeleyDbEnvOptions(string_view optionStr) :
	m_cacheGBytes(0),
	m_cacheBytes(0),
	m_numCacheSegments(0)
{
	auto rex = regex{k_optionRegExStr};
	svmatch captures;
	if (regex_match(begin(optionStr), end(optionStr), captures, rex))
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
			m_cacheGBytes = static_cast<uint32>(totalCacheSize / (1024ul * 1024ul * 1024ul));
			m_cacheBytes = static_cast<uint32>(totalCacheSize % (1024ul * 1024ul * 1024ul));
			m_numCacheSegments = lexical_cast<uint32>(captures[3].str());
		}
		catch (const ::boost::bad_lexical_cast& ex)
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

// ======================================================================

static auto freeDbEnv(__db_env* pDbEnv) noexcept -> void
{
	if (pDbEnv)
	{
		auto err = pDbEnv->close(pDbEnv, 0);
		if (err != 0)
		{
			PMNT_LOG(g_log, pmnt::log::Level::error)
				<< format("Unable to close Berkeley DB environment:  %1% (%2%)")
					% db_strerror(err) % err;
		}
	}
}

auto pmnt::BDbEnvManager::getEnv(const Path& filePath, const string& optionStr) -> BDbEnvPtr
{
	// This lock guards against the situation in which no environment exists and two
	// threads request one at the same time.  Without the lock, both threads could try
	// to create the environment independently, resulting in two environments.
	auto exclusiveLock = Lock{g_environmentMutex};

	auto pEnv = m_pDbEnv.lock();
	if (!pEnv)
	{
		m_homeDir = deriveHomeDir(filePath);
		m_options = BerkeleyDbEnvOptions{optionStr};
		pEnv = BDbEnvPtr{createDbEnv(), &freeDbEnv};

		pEnv->set_errfile(pEnv.get(), stderr);
		pEnv->set_errpfx(pEnv.get(), k_bdbErrorPrefix);

		if (auto err = pEnv->set_cachesize(pEnv.get(), m_options.m_cacheGBytes,
				m_options.m_cacheBytes, m_options.m_numCacheSegments);
			err != 0)
		{
			throw Exception(format("Unable to set Berkeley DB cache size:  %1% (%2%)")
				% db_strerror(err) % err);
		}

		if (auto err = pEnv->open(pEnv.get(),
#if defined(PARLIAMENT_WINDOWS)
				pathAsUtf8(m_homeDir).c_str(),
#else
				m_homeDir.c_str(),
#endif
				DB_INIT_MPOOL | DB_PRIVATE | DB_CREATE | DB_THREAD, 0);
			err != 0)
		{
			throw Exception(format("Unable to open Berkeley DB:  %1% (%2%)")
				% db_strerror(err) % err);
		}

		m_pDbEnv = pEnv;
	}
	return pEnv;
}

auto pmnt::BDbEnvManager::deriveHomeDir(const Path& filePath) -> Path
{
	auto homeDir = absolute(filePath).parent_path();
	if (exists(homeDir) && is_directory(homeDir))
	{
		return homeDir;
	}
	else if (!exists(homeDir))
	{
		create_directories(homeDir);
		return homeDir;
	}
	else
	{
		throw Exception(format("Unable to create Berkeley DB environment because '%1%' is not a directory")
			% homeDir.generic_string());
	}
}

auto pmnt::BDbEnvManager::createDbEnv() -> __db_env*
{
	auto pDbEnv = static_cast<__db_env*>(nullptr);
	auto err = db_env_create(&pDbEnv, 0);
	if (err != 0)
	{
		throw Exception(format("Unable to create Berkeley DB environment:  %1% (%2%)")
			% db_strerror(err) % err);
	}
	return pDbEnv;
}

pmnt::BDbEnvManager::~BDbEnvManager() = default;

// ======================================================================

static auto freeDbCursor(__dbc* pCursor) noexcept -> void
{
	if (pCursor)
	{
		auto err = pCursor->c_close(pCursor);
		if (err != 0)
		{
			PMNT_LOG(g_log, pmnt::log::Level::error)
				<< format("Unable to close Berkeley DB cursor:  %1% (%2%)")
					% db_strerror(err) % err;
		}
	}
}

pmnt::StrToIdEntryIterator::StrToIdEntryIterator() :
	m_pDB{nullptr},
	m_buffer{},
	m_curVal{nullValue()},
	m_pCursor{nullptr, &freeDbCursor}
{
}

pmnt::StrToIdEntryIterator::StrToIdEntryIterator(__db* pDB) :
	m_pDB{pDB},
	m_buffer(k_initialBufferSize),
	m_curVal{make_pair(&(m_buffer[0]), k_nullRsrcId)},
	m_pCursor{createCursor(m_pDB), &freeDbCursor}
{
	advanceCursor();
}

pmnt::StrToIdEntryIterator::StrToIdEntryIterator(const StrToIdEntryIterator& rhs) :
	m_pDB{rhs.m_pDB},
	m_buffer(rhs.m_buffer),
	m_curVal{m_pDB == nullptr ? nullValue() : make_pair(&(m_buffer[0]), rhs.m_curVal.second)},
	m_pCursor{(m_pDB == nullptr ? nullptr : createCursor(m_pDB)), &freeDbCursor}
{
	if (m_pDB != nullptr)
	{
		setCursorPosition();
	}
}

auto pmnt::StrToIdEntryIterator::operator=(const StrToIdEntryIterator& rhs) -> StrToIdEntryIterator&
{
	StrToIdEntryIterator temp(rhs);
	swap(temp);
	return *this;
}

pmnt::StrToIdEntryIterator::StrToIdEntryIterator(StrToIdEntryIterator&&) noexcept = default;
auto pmnt::StrToIdEntryIterator::operator=(StrToIdEntryIterator&&) noexcept -> StrToIdEntryIterator& = default;
pmnt::StrToIdEntryIterator::~StrToIdEntryIterator() = default;

auto pmnt::StrToIdEntryIterator::swap(StrToIdEntryIterator& other) noexcept -> void
{
	using ::std::swap;
	swap(m_pDB, other.m_pDB);
	swap(m_buffer, other.m_buffer);
	swap(m_curVal, other.m_curVal);
	swap(m_pCursor, other.m_pCursor);
}

auto pmnt::StrToIdEntryIterator::operator==(const StrToIdEntryIterator& rhs) const -> bool
{
	if (m_pDB == nullptr && rhs.m_pDB == nullptr)
	{
		return true;
	}
	else if (m_pDB == rhs.m_pDB)
	{
		auto lhsLen = char_traits<Buffer::value_type>::length(m_curVal.first);
		auto rhsLen = char_traits<Buffer::value_type>::length(rhs.m_curVal.first);
		if (lhsLen == rhsLen && char_traits<Buffer::value_type>::compare(
			m_curVal.first, rhs.m_curVal.first, lhsLen) == 0)
		{
			return true;
		}
	}
	return false;
}

auto pmnt::StrToIdEntryIterator::createCursor(__db* pDB) -> __dbc*
{
	auto pCursor = static_cast<__dbc*>(nullptr);
	auto err = pDB->cursor(pDB, nullptr, &pCursor, 0);
	if (err != 0)
	{
		throw Exception(format("Unable to create Berkeley DB cursor:  %1% (%2%)")
			% db_strerror(err) % err);
	}
	return pCursor;
}

// Intended to be called only from the copy ctor and the assignment operator
auto pmnt::StrToIdEntryIterator::setCursorPosition() -> void
{
	auto key = DBT{};
	auto val = DBT{};
	initInputDbt(key, m_curVal.first, char_traits<Buffer::value_type>::length(m_curVal.first));
	initOutputDbt(val, &m_curVal.second, 1);

	auto err = m_pCursor->c_get(m_pCursor.get(), &key, &val, DB_SET);
	if (err == 0)
	{
		// Do nothing
	}
	else if (err == DB_NOTFOUND)
	{
		advanceToEnd();
	}
	else
	{
		advanceToEnd();
		throw Exception(format("Unable to advance Berkeley DB cursor:  %1% (%2%)")
			% db_strerror(err) % err);
	}
}

auto pmnt::StrToIdEntryIterator::checkBufferSizeDivisibleByCharSize(uint32 bufferSize) -> void
{
	if constexpr (sizeof(Buffer::value_type) > 1)
	{
		if (bufferSize % sizeof(Buffer::value_type) != 0)
		{
			throw Exception(
				format("Berkeley DB buffer size %1% is not divisible by character size %2%")
				% bufferSize % sizeof(Buffer::value_type));
		}
	}
}

// Intended to be called only from ctor and operator++
auto pmnt::StrToIdEntryIterator::advanceCursor() -> void
{
	while (m_pDB != nullptr)
	{
		auto key = DBT{};
		auto val = DBT{};
		initOutputDbt(key, m_curVal.first, size(m_buffer) - 1);
		initOutputDbt(val, &m_curVal.second, 1);

		auto err = m_pCursor->c_get(m_pCursor.get(), &key, &val, DB_NEXT);
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
			advanceToEnd();
		}
		else
		{
			advanceToEnd();
			throw Exception(format("Unable to advance Berkeley DB cursor:  %1% (%2%)")
				% db_strerror(err) % err);
		}
	}
}

// Converts this to an "end" iterator
auto pmnt::StrToIdEntryIterator::advanceToEnd() -> void
{
	closeCursor();
	m_pDB = nullptr;
}

// ======================================================================

static auto freeDb(__db* pDb) noexcept -> void
{
	if (pDb)
	{
		auto err = pDb->close(pDb, 0);
		if (err != 0)
		{
			PMNT_LOG(g_log, pmnt::log::Level::error) << format("Unable to close Berkeley DB:  %1% (%2%)")
				% db_strerror(err) % err;
		}
	}
}

pmnt::StringToId::StringToId(const Path& filePath, const string& optionStr, bool readOnly) :
	m_filePath{absolute(filePath)},
	m_readOnly{readOnly},
	m_pDbEnv{BDbEnvManager::getInstance().getEnv(filePath, optionStr)},
	m_pDB{createDb(m_pDbEnv), &freeDb}
{
	uint32 flags = DB_THREAD | (m_readOnly ? DB_RDONLY : DB_CREATE);
	auto err2{m_pDB->open(m_pDB.get(), nullptr,
#if defined(PARLIAMENT_WINDOWS)
		pathAsUtf8(m_filePath).c_str(),
#else
		m_filePath.c_str(),
#endif
		nullptr, DB_BTREE, flags, 0)};
	if (err2 != 0)
	{
		throw Exception(format("Unable to open Berkeley DB:  %1% (%2%)")
			% db_strerror(err2) % err2);
	}
}

auto pmnt::StringToId::createDb(BDbEnvPtr pDbEnv) -> __db*
{
	auto pDb = static_cast<__db*>(nullptr);
	auto err = db_create(&pDb, pDbEnv.get(), 0);
	if (err != 0)
	{
		throw Exception(format("Unable to create Berkeley DB instance:  %1% (%2%)")
			% db_strerror(err) % err);
	}
	return pDb;
}

pmnt::StringToId::StringToId(StringToId&&) noexcept = default;
auto pmnt::StringToId::operator=(StringToId&&) noexcept -> StringToId& = default;
pmnt::StringToId::~StringToId() = default;

auto pmnt::StringToId::swap(StringToId& other) noexcept -> void
{
	using ::std::swap;
	swap(m_filePath, other.m_filePath);
	swap(m_readOnly, other.m_readOnly);
	swap(m_pDB, other.m_pDB);
}

auto pmnt::StringToId::sync() -> void
{
	if (!m_readOnly)
	{
		auto err = m_pDB->sync(m_pDB.get(), 0);
		if (err != 0)
		{
			throw Exception(format("Unable to sync Berkeley DB:  %1% (%2%)")
				% db_strerror(err) % err);
		}
	}
}

auto pmnt::StringToId::compact() -> void
{
	checkWritable();

	// We compact 3 times to avoid leaving any empty pages in the middle of the file.
	// Compacting repeatedly tends to return these empty pages to the file system.
	for (int i = 0; i < 3; ++i)
	{
		DB_COMPACT compactData;
		memset(&compactData, 0, sizeof(compactData));
		auto err = m_pDB->compact(m_pDB.get(), nullptr, nullptr, nullptr, &compactData,
			DB_FREE_SPACE, nullptr);
		if (err != 0)
		{
			throw Exception(format("Unable to compact Berkeley DB:  %1% (%2%)")
				% db_strerror(err) % err);
		}
	}
}

auto pmnt::StringToId::find(const RsrcChar* pKey, size_t keyLen) const -> ResourceId
{
	if (pKey == nullptr)
	{
		throw Exception("StringToId::find called with a null key");
	}

	ResourceId result = k_nullRsrcId;

	auto key = DBT{};
	auto val = DBT{};
	initInputDbt(key, pKey, keyLen);
	initOutputDbt(val, &result, 1);

	auto err = m_pDB->get(m_pDB.get(), nullptr, &key, &val, 0);
	if (err != 0 && err != DB_NOTFOUND)
	{
		throw Exception(format("Unable to find key in Berkeley DB:  %1% (%2%)")
			% db_strerror(err) % err);
	}
	return result;
}

auto pmnt::StringToId::insert(const RsrcChar* pKey, size_t keyLen, ResourceId value) -> void
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

	auto key = DBT{};
	auto val = DBT{};
	initInputDbt(key, pKey, keyLen);
	initInputDbt(val, &value, 1);

	auto err = m_pDB->put(m_pDB.get(), nullptr, &key, &val, DB_NOOVERWRITE);
	if (err != 0 && err != DB_KEYEXIST)
	{
		throw Exception(format("Unable to insert key in Berkeley DB:  %1% (%2%)")
			% db_strerror(err) % err);
	}
}

auto pmnt::StringToId::checkWritable() const -> void
{
	if (m_readOnly)
	{
		throw Exception(
			"Write operations are prohibited on read-only StringToId instances");
	}
}
