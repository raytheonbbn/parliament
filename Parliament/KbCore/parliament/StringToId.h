// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_STRINGTOID_H_INCLUDED)
#define PARLIAMENT_STRINGTOID_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <boost/filesystem/path.hpp>
#include <iterator>
#include <memory>
#include <string>
#include <utility>
#include <vector>

// Forward declarations of types defined by Berkeley DB:
struct __db_env;
struct __db;
struct __dbc;

namespace bbn::parliament {

using BDbEnvPtr = ::std::shared_ptr<__db_env>;

struct BerkeleyDbEnvOptions
{
	BerkeleyDbEnvOptions() :
		m_cacheGBytes(0),
		m_cacheBytes(0),
		m_numCacheSegments(0)
	{}
	BerkeleyDbEnvOptions(const ::std::string& optionStr);

	uint32 m_cacheGBytes;
	uint32 m_cacheBytes;
	uint32 m_numCacheSegments;
};

class BDbEnvManager
{
public:
	using Path = ::boost::filesystem::path;
	using WeakBDbEnvPtr = ::std::weak_ptr<__db_env>;

	// Under C++11, this instance is instantiated on first use and guaranteed to be
	// destroyed.  In addition, these operations are guaranteed to be thread-safe.
	static BDbEnvManager& getInstance()
	{
		static BDbEnvManager instance;
		return instance;
	}

	BDbEnvManager(const BDbEnvManager&) = delete;
	auto operator=(const BDbEnvManager&) -> BDbEnvManager& = delete;
	BDbEnvManager(BDbEnvManager&&) = delete;
	auto operator=(BDbEnvManager&&) -> BDbEnvManager& = delete;
	~BDbEnvManager();

	// Intended for the StringToId class below:
	auto getEnv(const Path& filePath, const ::std::string& optionStr) -> BDbEnvPtr;

private:
	BDbEnvManager() :
		m_homeDir(),
		m_options(),
		m_pDbEnv()
	{}

	static auto deriveHomeDir(const Path& filePath) -> Path;
	static auto createDbEnv() -> __db_env*;

	Path						m_homeDir;
	BerkeleyDbEnvOptions	m_options;
	WeakBDbEnvPtr			m_pDbEnv;
};

class StrToIdEntryIterator
{
public:
	using iterator_category = ::std::forward_iterator_tag;
	using value_type = ::std::pair<RsrcChar*, ResourceId>;
	using difference_type = ptrdiff_t;
	using pointer = const value_type*;
	using reference = const value_type&;

	// Conceptually private:  These two ctors are intended to be called only
	// from the begin() and end() methods on StringToId
	StrToIdEntryIterator();				// Creates an "end" iterator
	StrToIdEntryIterator(__db* pDB);	// Creates a "begin" iterator

	StrToIdEntryIterator(const StrToIdEntryIterator&);
	auto operator=(const StrToIdEntryIterator&) -> StrToIdEntryIterator&;
	StrToIdEntryIterator(StrToIdEntryIterator&&) noexcept;
	auto operator=(StrToIdEntryIterator&&) noexcept -> StrToIdEntryIterator&;
	PARLIAMENT_EXPORT ~StrToIdEntryIterator();

	auto swap(StrToIdEntryIterator& other) noexcept -> void;

	PARLIAMENT_EXPORT auto operator++() -> StrToIdEntryIterator&	// preincrement
		{
			advanceCursor();
			return *this;
		}
	PARLIAMENT_EXPORT auto operator++(int) -> StrToIdEntryIterator	// postincrement
		{
			auto copy = StrToIdEntryIterator{*this};
			advanceCursor();
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
	using Buffer = ::std::vector<RsrcChar>;
	using DbcDeleterFunction = void (*)(__dbc* pCursor) noexcept;
	using DbcPtr = ::std::unique_ptr<__dbc, DbcDeleterFunction>;

	static auto nullValue() -> value_type
		{ return ::std::make_pair(static_cast<value_type::first_type>(nullptr), k_nullRsrcId); }
	static auto createCursor(__db* pDB) -> __dbc*;
	static inline auto checkBufferSizeDivisibleByCharSize(uint32 bufferSize) -> void;

	auto closeCursor() -> void
		{ m_pCursor.reset(nullptr); }
	auto setCursorPosition() -> void;
	auto advanceCursor() -> void;
	auto advanceToEnd() -> void;

	static const size_t	k_initialBufferSize = 128;

	__db*			m_pDB;		// Owned elsewhere -- do not clean up on destruction!
	Buffer		m_buffer;
	value_type	m_curVal;
	DbcPtr		m_pCursor;
};

class StringToId
{
public:
	using const_iterator = StrToIdEntryIterator;
	using Path = ::boost::filesystem::path;

	StringToId(const Path& filePath, const ::std::string& optionStr, bool readOnly);
	StringToId(const StringToId&) = delete;
	auto operator=(const StringToId&) -> StringToId& = delete;
	StringToId(StringToId&&) noexcept;
	auto operator=(StringToId&&) noexcept -> StringToId&;
	~StringToId();

	auto swap(StringToId& other) noexcept -> void;

	auto sync() -> void;
	auto compact() -> void;

	auto isMember(const RsrcChar* pKey, size_t keyLen) const -> bool
		{ return find(pKey, keyLen) != k_nullRsrcId; }

	auto isMember(const RsrcChar* pKey) const -> bool
		{ return find(pKey) != k_nullRsrcId; }

	auto isMember(const RsrcString& key) const -> bool
		{ return find(key) != k_nullRsrcId; }

	auto find(const RsrcChar* pKey, size_t keyLen) const -> ResourceId;

	auto find(const RsrcChar* pKey) const -> ResourceId
		{ return find(pKey, ::std::char_traits<RsrcChar>::length(pKey)); }

	auto find(const RsrcString& key) const -> ResourceId
		{ return find(key.c_str(), key.length()); }

	auto begin() const -> const_iterator
		{ return StrToIdEntryIterator(m_pDB.get()); }
	auto cbegin() const -> const_iterator
		{ return StrToIdEntryIterator(m_pDB.get()); }
	auto end() const -> const_iterator
		{ return StrToIdEntryIterator(); }
	auto cend() const -> const_iterator
		{ return StrToIdEntryIterator(); }

	auto insert(const RsrcChar* pKey, size_t keyLen, ResourceId value) -> void;

	auto insert(const RsrcChar* pKey, ResourceId value) -> void
		{ insert(pKey, ::std::char_traits<RsrcChar>::length(pKey), value); }

	auto insert(const RsrcString& key, ResourceId value) -> void
		{ insert(key.c_str(), key.length(), value); }

private:
	using DbDeleterFunction = void (*)(__db* pDb) noexcept;
	using DbPtr = ::std::unique_ptr<__db, DbDeleterFunction>;

	static auto createDb(BDbEnvPtr pDbEnv) -> __db*;
	auto checkWritable() const -> void;

	Path			m_filePath;
	bool			m_readOnly;
	BDbEnvPtr	m_pDbEnv;
	DbPtr			m_pDB;
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
