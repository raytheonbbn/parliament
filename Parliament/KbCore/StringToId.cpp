// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2026, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/StringToId.h"
#include "parliament/Exceptions.h"
#include "parliament/Log.h"
#include "parliament/UnicodeIterator.h"

#include <boost/filesystem.hpp>
#include <boost/format.hpp>
#include <rocksdb/c.h>
#include <string>
#include <string_view>

namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::string;
using ::std::string_view;
using ::std::unique_ptr;

static auto g_log(pmnt::log::getSource("StringToId"));

static auto buildErrorMsg(string_view msg, char* pRocksDbError) -> string
{
	return str(format("%1%: %2%") % msg % (pRocksDbError == nullptr ? "" : pRocksDbError));
}

static auto throwOnError(string_view msg, char* pRocksDbError) -> void
{
	if (pRocksDbError != nullptr)
	{
		throw pmnt::Exception(buildErrorMsg(msg, pRocksDbError));
	}
}

// ======================================================================

template<typename OptType, OptType* (*createFxn)(), void (*deleteFxn)(OptType* pOptions)>
class Options {
public:
	Options() : m_pOptions{createFxn()} {}
	Options(const Options&) = delete;
	auto operator=(const Options&) -> Options& = delete;
	Options(Options&&) noexcept = default;
	auto operator=(Options&&) noexcept -> Options& = default;
	~Options() = default;

	OptType* get() const { return m_pOptions.get(); }

private:
	struct Deleter
	{
		void operator()(OptType* p) const noexcept
		{
			if (p)
			{
				deleteFxn(p);
			}
		}
	};

	unique_ptr<OptType, Deleter> m_pOptions;
};

using DbOptions = Options<rocksdb_options_t,
	&rocksdb_options_create, &rocksdb_options_destroy>;
using WaitForCompactOptions = Options<rocksdb_wait_for_compact_options_t,
	&rocksdb_wait_for_compact_options_create, &rocksdb_wait_for_compact_options_destroy>;
using CompactOptions = Options<rocksdb_compactoptions_t,
	&rocksdb_compactoptions_create, &rocksdb_compactoptions_destroy>;
using ReadOptions = Options<rocksdb_readoptions_t,
	&rocksdb_readoptions_create, &rocksdb_readoptions_destroy>;
using WriteOptions = Options<rocksdb_writeoptions_t,
	&rocksdb_writeoptions_create, &rocksdb_writeoptions_destroy>;

// ======================================================================

pmnt::StrToIdEntryIterator::StrToIdEntryIterator() :
	m_pDb{nullptr},
	m_curVal{nullValue()},
	m_pIterator{nullptr, &rocksdb_iter_destroy}
{
}

pmnt::StrToIdEntryIterator::StrToIdEntryIterator(RocksDBPtr::pointer pDb) :
	m_pDb{pDb},
	m_curVal{nullValue()},
	m_pIterator{createIterator(m_pDb), &rocksdb_iter_destroy}
{
	rocksdb_iter_seek_to_first(m_pIterator.get());
	if (!!rocksdb_iter_valid(m_pIterator.get()))
	{
		setCurrentValue();
	}
	else
	{
		advanceToEnd();
	}
}

pmnt::StrToIdEntryIterator::StrToIdEntryIterator(const StrToIdEntryIterator& rhs) :
	m_pDb{rhs.m_pDb},
	m_curVal{rhs.m_curVal},
	m_pIterator{createIterator(m_pDb), &rocksdb_iter_destroy}
{
	if (m_pDb != nullptr)
	{
		setIteratorPosition();
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
	swap(m_pDb, other.m_pDb);
	swap(m_curVal, other.m_curVal);
	swap(m_pIterator, other.m_pIterator);
}

auto pmnt::StrToIdEntryIterator::operator==(const StrToIdEntryIterator& rhs) const -> bool
{
	if (m_pDb == nullptr && rhs.m_pDb == nullptr)
	{
		return true;
	}
	else if (m_pDb == rhs.m_pDb && m_curVal.first == rhs.m_curVal.first)
	{
		return true;
	}
	return false;
}

auto pmnt::StrToIdEntryIterator::createIterator(RocksDBPtr::pointer pDb) -> RocksDBIterPtr::pointer
{
	if (pDb == nullptr)
	{
		return nullptr;
	}
	else
	{
		ReadOptions options;
		return rocksdb_create_iterator(pDb, options.get());
	}
}

// Intended to be called only from the copy ctor and the assignment operator
auto pmnt::StrToIdEntryIterator::setIteratorPosition() -> void
{
	rocksdb_iter_seek(m_pIterator.get(), m_curVal.first.data(), m_curVal.first.size());
	if (!!rocksdb_iter_valid(m_pIterator.get()))
	{
		setCurrentValue();
	}
	else
	{
		advanceToEnd();
	}
}

auto pmnt::StrToIdEntryIterator::checkKeySizeDivisibleByCharSize(size_t keySize) -> void
{
	if constexpr (sizeof(value_type::first_type::value_type) > 1)
	{
		if (keySize % sizeof(value_type::first_type::value_type) != 0)
		{
			throw Exception(format(
				"RocksDB key size %1% is not divisible by character size %2%")
				% keySize % sizeof(value_type::first_type::value_type));
		}
	}
}

auto pmnt::StrToIdEntryIterator::checkValueSize(size_t valueSize) -> void
{
	if (valueSize != sizeof(value_type::second_type))
	{
		throw Exception(format(
			"RocksDB value size %1% is not the same as the ResourceId size %2%")
			% valueSize % sizeof(value_type::second_type));
	}
}

// Intended to be called only from ctor and operator++
auto pmnt::StrToIdEntryIterator::advanceIterator() -> void
{
	if (m_pDb != nullptr)
	{
		rocksdb_iter_next(m_pIterator.get());
		if (!!rocksdb_iter_valid(m_pIterator.get()))
		{
			setCurrentValue();
		}
		else
		{
			advanceToEnd();
		}
	}
}

// Converts this to an "end" iterator
auto pmnt::StrToIdEntryIterator::advanceToEnd() -> void
{
	m_pIterator.reset(nullptr);
	m_curVal = nullValue();
	m_pDb = nullptr;
}

auto pmnt::StrToIdEntryIterator::setCurrentValue() -> void
{
	size_t keyLength = 0;
	size_t valueLength = 0;
	auto pKey = rocksdb_iter_key(m_pIterator.get(), &keyLength);
	auto pValue = rocksdb_iter_value(m_pIterator.get(), &valueLength);

	checkKeySizeDivisibleByCharSize(keyLength);
	checkValueSize(valueLength);
	m_curVal.first = value_type::first_type{
		reinterpret_cast<const RsrcChar*>(pKey),
		keyLength / sizeof(value_type::first_type::value_type)};
	m_curVal.second = *reinterpret_cast<ResourceId*>(const_cast<char*>(pValue));
}

// ======================================================================

pmnt::StringToId::StringToId(const KbConfig& config) :
	m_config{config},
	m_pDB{createDb(config), &rocksdb_close}
{
}

auto pmnt::StringToId::createDb(const KbConfig& config) -> RocksDBPtr::pointer
{
	auto rocksDbPath = config.uriToIntFilePath();
	if (is_regular_file(rocksDbPath))
	{
		throw pmnt::Exception(format(
			"Parliament's URI-to-Int table '%1%' exists, but is a regular file rather "
			"than a directory. Is this a Parliament instance from an older version of "
			"Parliament? If so, see Parliament User Guide, Section 2.2.2, 'Upgrading "
			"an Existing Installation,' for the data migration procedure.")
			% rocksDbPath.generic_string());
	}
	else if (!exists(rocksDbPath))
	{
		create_directories(rocksDbPath);
	}

	DbOptions options;
	rocksdb_options_set_create_if_missing(options.get(), true);
	char* pError = nullptr;
	auto pDB = rocksdb_open(options.get(), pathAsUtf8(rocksDbPath).c_str(), &pError);
	throwOnError("Unable to open RocksDB database", pError);
	return pDB;
}

pmnt::StringToId::~StringToId()
{
	char* pError = nullptr;
	rocksdb_flush_wal(m_pDB.get(), true, &pError);
	if (pError != nullptr)
	{
		PMNT_LOG(g_log, log::Level::warn)
			<< buildErrorMsg("Unable to sync RocksDB database on close", pError);
	}

	WaitForCompactOptions options;
	rocksdb_wait_for_compact_options_set_close_db(options.get(), true);
	rocksdb_wait_for_compact_options_set_flush(options.get(), true);
	rocksdb_wait_for_compact_options_set_timeout(options.get(), 3 * 1000 * 1000);	// 3 sec in microsec
	rocksdb_wait_for_compact(m_pDB.get(), options.get(), &pError);
	if (pError != nullptr)
	{
		PMNT_LOG(g_log, log::Level::warn)
			<< buildErrorMsg("Unable to close RocksDB database", pError);
	}
}

pmnt::StringToId::StringToId(StringToId&&) noexcept = default;
auto pmnt::StringToId::operator=(StringToId&&) noexcept -> StringToId& = default;

auto pmnt::StringToId::swap(StringToId& other) noexcept -> void
{
	using ::std::swap;
	swap(m_config, other.m_config);
	swap(m_pDB, other.m_pDB);
}

auto pmnt::StringToId::sync() -> void
{
	if (!m_config.readOnly())
	{
		char* pError = nullptr;
		rocksdb_flush_wal(m_pDB.get(), true, &pError);
		throwOnError("Unable to sync RocksDB", pError);
	}
}

auto pmnt::StringToId::compact() -> void
{
	if (!m_config.readOnly())
	{
		CompactOptions options;
		rocksdb_compactoptions_set_change_level(options.get(), true);
		rocksdb_compact_range_opt(m_pDB.get(), options.get(), nullptr, 0, nullptr, 0);
	}
}

auto pmnt::StringToId::find(RsrcStringView key) const -> ResourceId
{
	ResourceId result = k_nullRsrcId;

	ReadOptions options;
	char* pError = nullptr;
	size_t valueLength = 0;

	// rocksdb_get returns NULL if not found or a malloc()ed array otherwise.
	// Stores the length of the array in valueLength.
	unique_ptr<char, decltype(&free)> pResult{
		rocksdb_get(m_pDB.get(), options.get(), reinterpret_cast<const char*>(key.data()),
		key.size() * sizeof(RsrcStringView::value_type), &valueLength, &pError),
		&free};
	throwOnError("RocksDB lookup failure", pError);
	if (pResult.get() != nullptr)
	{
		if (valueLength != sizeof(result))
		{
			throw Exception(format(
				"Expected RocksDB data value size of %1%, but found %2% instead")
				% sizeof(result) % valueLength);
		}
		result = *reinterpret_cast<ResourceId*>(
			const_cast<char*>(pResult.get()));
	}
	return result;
}

auto pmnt::StringToId::insert(RsrcStringView key, ResourceId value) -> ResourceId
{
	checkWritable();
	if (key.length() <= 0)
	{
		throw Exception("StringToId::insert called with zero-length key");
	}
	if (value == k_nullRsrcId)
	{
		throw Exception("StringToId::insert called with a null value");
	}

	WriteOptions options;
	//rocksdb_writeoptions_set_sync(options.get(), true);
	char* pError = nullptr;
	rocksdb_put(m_pDB.get(), options.get(),
		reinterpret_cast<const char*>(key.data()), key.size() * sizeof(RsrcStringView::value_type),
		const_cast<const char*>(reinterpret_cast<char*>(&value)), sizeof(value),
		&pError);
	//if (status.IsOkOverwritten())
	//{
	//	PMNT_LOG(g_log, log::Level::warn) << str(format(
	//		"Overwrote the value of StringToId key '%1%' with %2%")
	//		% convertFromRsrcChar(key) % value);
	//}
	throwOnError("Unable to insert key in RocksDB", pError);
	return value;
}

auto pmnt::StringToId::checkWritable() const -> void
{
	if (m_config.readOnly())
	{
		throw Exception(
			"Write operations are prohibited on read-only StringToId instances");
	}
}
