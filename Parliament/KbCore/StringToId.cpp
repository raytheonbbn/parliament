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
#include <rocksdb/db.h>
#include <string>
#include <string_view>

namespace pmnt = ::bbn::parliament;
namespace rdb = ::rocksdb;

using ::boost::format;
using ::std::string;
using ::std::string_view;

static auto g_log(pmnt::log::getSource("StringToId"));

static auto buildErrorMsg(const rdb::Status& status, string_view msg) -> string
{
	return str(format("%1%: %2% (%3%/%4%)")
		% msg % status.ToString()
		% static_cast<size_t>(status.code())
		% static_cast<size_t>(status.subcode()));
}

static auto throwOnError(const rdb::Status& status, string_view msg) -> void
{
	if (!status.ok())
	{
		throw pmnt::Exception(buildErrorMsg(status, msg));
	}
}

// ======================================================================

pmnt::StrToIdEntryIterator::StrToIdEntryIterator() :
	m_pDb{nullptr},
	m_curVal{nullValue()},
	m_pIterator{}
{
}

pmnt::StrToIdEntryIterator::StrToIdEntryIterator(RocksDBPtr::pointer pDb) :
	m_pDb{pDb},
	m_curVal{nullValue()},
	m_pIterator{createIterator(m_pDb)}
{
	m_pIterator->SeekToFirst();
	if (m_pIterator->Valid())
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
	m_pIterator{(m_pDb != nullptr) ? createIterator(m_pDb) : nullptr}
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
	rdb::ReadOptions options;
	return pDb->NewIterator(options);
}

// Intended to be called only from the copy ctor and the assignment operator
auto pmnt::StrToIdEntryIterator::setIteratorPosition() -> void
{
	m_pIterator->Seek(m_curVal.first);
	if (m_pIterator->Valid())
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
		m_pIterator->Next();
		if (m_pIterator->Valid())
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
	checkKeySizeDivisibleByCharSize(m_pIterator->key().size());
	checkValueSize(m_pIterator->value().size());
	m_curVal.first = value_type::first_type{
		reinterpret_cast<const RsrcChar*>(m_pIterator->key().data()),
		m_pIterator->key().size() / sizeof(value_type::first_type::value_type)};
	m_curVal.second = *reinterpret_cast<ResourceId*>(const_cast<char*>(
		m_pIterator->value().data()));
}

// ======================================================================

pmnt::StringToId::StringToId(const KbConfig& config) :
	m_config{config},
	m_pDB{createDb(config)}
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
			"Parliament? If so, see Section 2.2.2, 'Upgrading an Existing Installation' "
			"in the Parliament User Guide for the data migration procedure.")
			% rocksDbPath.generic_string());
	}
	else if (!exists(rocksDbPath))
	{
		create_directories(rocksDbPath);
	}

	auto pDB = static_cast<RocksDBPtr::pointer>(nullptr);
	rdb::Options options;
	options.create_if_missing = true;
	auto status = rdb::DB::Open(options, pathAsUtf8(rocksDbPath).c_str(), &pDB);
	throwOnError(status, "Unable to open RocksDB database");
	return pDB;
}

pmnt::StringToId::~StringToId()
{
	auto syncStatus = m_pDB->SyncWAL();
	if (!syncStatus.ok())
	{
		PMNT_LOG(g_log, log::Level::warn)
			<< buildErrorMsg(syncStatus, "Unable to sync RocksDB database on close");
	}

	rdb::WaitForCompactOptions options;
	options.close_db = true;
	options.flush = true;
	options.timeout = ::std::chrono::seconds{3};
	auto waitStatus = m_pDB->WaitForCompact(options);
	if (!waitStatus.ok())
	{
		PMNT_LOG(g_log, log::Level::warn)
			<< buildErrorMsg(waitStatus, "Unable to close RocksDB database");
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
		auto status = m_pDB->SyncWAL();
		throwOnError(status, "Unable to sync RocksDB");
	}
}

auto pmnt::StringToId::compact() -> void
{
	if (!m_config.readOnly())
	{
		rdb::CompactRangeOptions options;
		options.change_level = true;
		auto status = m_pDB->CompactRange(options, nullptr, nullptr);
		throwOnError(status, "Unable to compact RocksDB");
	}
}

auto pmnt::StringToId::find(RsrcStringView key) const -> ResourceId
{
	ResourceId result = k_nullRsrcId;

	rdb::ReadOptions options;
	auto colFamily = m_pDB->DefaultColumnFamily();
	auto keySlice = rdb::Slice{reinterpret_cast<const char*>(key.data()),
		key.size() * sizeof(RsrcStringView::value_type)};
	rdb::PinnableSlice resultSlice;
	auto status = m_pDB->Get(options, colFamily, keySlice, &resultSlice);
	if (status.ok())
	{
		if (resultSlice.size() != sizeof(result))
		{
			throw Exception(format(
				"Expected RocksDB data value size of %1%, but found %2% instead")
				% sizeof(result) % resultSlice.size());
		}
		result = *reinterpret_cast<ResourceId*>(
			const_cast<char*>(resultSlice.data()));
	}
	else if (!status.IsNotFound())
	{
		throwOnError(status, "Unable to find key in RocksDB");
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

	rdb::WriteOptions options;
	//options.sync = true;
	auto keySlice = rdb::Slice{reinterpret_cast<const char*>(key.data()),
		key.size() * sizeof(RsrcStringView::value_type)};
	auto valueSlice = rdb::Slice{const_cast<const char*>(
		reinterpret_cast<char*>(&value)), sizeof(value)};
	auto status = m_pDB->Put(options, keySlice, valueSlice);
	if (status.IsOkOverwritten())
	{
		PMNT_LOG(g_log, log::Level::warn) << str(format(
			"Overwrote the value of StringToId key '%1%' with %2%")
			% convertFromRsrcChar(key) % value);
	}
	throwOnError(status, "Unable to insert key in RocksDB");
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
