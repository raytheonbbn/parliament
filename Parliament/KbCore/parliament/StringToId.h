// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2026, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_STRINGTOID_H_INCLUDED)
#define PARLIAMENT_STRINGTOID_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"
#include "parliament/KbConfig.h"

namespace rocksdb {
class DB;
class Iterator;
}

namespace bbn::parliament {

using RocksDBPtr = ::std::unique_ptr<::rocksdb::DB>;

class StrToIdEntryIterator
{
public:
	using iterator_category = ::std::forward_iterator_tag;
	using value_type = ::std::pair<RsrcStringView, ResourceId>;
	using difference_type = ptrdiff_t;
	using pointer = const value_type*;
	using reference = const value_type&;

	// Conceptually private: These two ctors are intended to be called only
	// from the begin() and end() methods on StringToId
	StrToIdEntryIterator();									// Creates an "end" iterator
	StrToIdEntryIterator(RocksDBPtr::pointer pDb);	// Creates a "begin" iterator

	StrToIdEntryIterator(const StrToIdEntryIterator&);
	auto operator=(const StrToIdEntryIterator&) -> StrToIdEntryIterator&;
	StrToIdEntryIterator(StrToIdEntryIterator&&) noexcept;
	auto operator=(StrToIdEntryIterator&&) noexcept -> StrToIdEntryIterator&;
	PARLIAMENT_EXPORT ~StrToIdEntryIterator();

	auto swap(StrToIdEntryIterator& other) noexcept -> void;

	PARLIAMENT_EXPORT auto operator++() -> StrToIdEntryIterator&	// preincrement
		{
			advanceIterator();
			return *this;
		}
	PARLIAMENT_EXPORT auto operator++(int) -> StrToIdEntryIterator	// postincrement
		{
			auto copy = StrToIdEntryIterator{*this};
			advanceIterator();
			return copy;
		}

	PARLIAMENT_EXPORT auto operator*() const -> reference
		{ return m_curVal; }
	PARLIAMENT_EXPORT auto operator->() const -> pointer
		{ return &m_curVal; }

	PARLIAMENT_EXPORT auto operator==(const StrToIdEntryIterator& rhs) const -> bool;
	PARLIAMENT_EXPORT auto operator!=(const StrToIdEntryIterator& rhs) const -> bool
		{ return !(*this == rhs); }

private:
	using RocksDBIterPtr = ::std::unique_ptr<::rocksdb::Iterator>;

	static auto nullValue() -> value_type
		{ return ::std::make_pair(value_type::first_type{}, k_nullRsrcId); }
	static auto createIterator(RocksDBPtr::pointer pDb) -> RocksDBIterPtr::pointer;
	static inline auto checkKeySizeDivisibleByCharSize(uint32 keySize) -> void;
	static inline auto checkValueSize(uint32 valueSize) -> void;

	auto setIteratorPosition() -> void;
	auto advanceIterator() -> void;
	auto advanceToEnd() -> void;
	auto setCurrentValue() -> void;

	RocksDBPtr::pointer	m_pDb;		// Owned elsewhere -- do not clean up on destruction!
	value_type				m_curVal;
	RocksDBIterPtr			m_pIterator;
};

class StringToId
{
public:
	using const_iterator = StrToIdEntryIterator;
	using Path = ::boost::filesystem::path;

	StringToId(const KbConfig& config);
	StringToId(const StringToId&) = delete;
	auto operator=(const StringToId&) -> StringToId& = delete;
	StringToId(StringToId&&) noexcept;
	auto operator=(StringToId&&) noexcept -> StringToId&;
	~StringToId();

	auto swap(StringToId& other) noexcept -> void;

	auto sync() -> void;
	auto compact() -> void;

	auto isMember(RsrcStringView key) const -> bool
		{ return find(key) != k_nullRsrcId; }
	auto find(RsrcStringView key) const -> ResourceId;

	auto insert(RsrcStringView key, ResourceId value) -> ResourceId;

	auto begin() const -> const_iterator
		{ return StrToIdEntryIterator{m_pDB.get()}; }
	auto cbegin() const -> const_iterator
		{ return StrToIdEntryIterator{m_pDB.get()}; }
	auto end() const -> const_iterator
		{ return StrToIdEntryIterator{}; }
	auto cend() const -> const_iterator
		{ return StrToIdEntryIterator{}; }

private:
	static auto createDb(const KbConfig& config) -> RocksDBPtr::pointer;
	auto checkWritable() const -> void;

	KbConfig		m_config;
	RocksDBPtr	m_pDB;
};

// See Effective C++, 3rd Edition, Item 25:
inline void swap(StrToIdEntryIterator& lhs, StrToIdEntryIterator& rhs) noexcept
{
	lhs.swap(rhs);
}

// See Effective C++, 3rd Edition, Item 25:
inline void swap(StringToId& lhs, StringToId& rhs) noexcept
{
	lhs.swap(rhs);
}

} // namespace end

// See Effective C++, 3rd Edition, Item 25:
namespace std {
	template<>
	inline void swap<::bbn::parliament::StrToIdEntryIterator>(
		::bbn::parliament::StrToIdEntryIterator& lhs,
		::bbn::parliament::StrToIdEntryIterator& rhs) noexcept
	{
		lhs.swap(rhs);
	}
}

// See Effective C++, 3rd Edition, Item 25:
namespace std {
	template<>
	inline void swap<::bbn::parliament::StringToId>(
		::bbn::parliament::StringToId& lhs,
		::bbn::parliament::StringToId& rhs) noexcept
	{
		lhs.swap(rhs);
	}
}

#endif // !PARLIAMENT_STRINGTOID_H_INCLUDED
