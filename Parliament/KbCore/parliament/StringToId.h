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
#include <string>
#include <utility>
#include <vector>

// Un-comment this to switch to an all in-memory implementation.  This is
// useful only for diagnostic purposes, for experiments that test the disk-
// related behavior of the memory-mapped files in isolation from any disk-
// related effects of BDB.
//#define USE_IN_MEMORY_LOOKUP_TABLE

#if defined(USE_IN_MEMORY_LOOKUP_TABLE)
#	include <unordered_map>
#else
// Forward declarations of types defined by Berkeley DB:
struct __db;
using DB = struct __db;
struct __dbc;
using DBC = struct __dbc;
#endif

PARLIAMENT_NAMESPACE_BEGIN

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
	StrToIdEntryIterator(DB* pDB);	// Creates a "begin" iterator

	StrToIdEntryIterator(const StrToIdEntryIterator& rhs);
	StrToIdEntryIterator& operator=(const StrToIdEntryIterator& rhs);

	PARLIAMENT_EXPORT ~StrToIdEntryIterator();

	PARLIAMENT_EXPORT StrToIdEntryIterator& operator++();		// preincrement -- efficient
	PARLIAMENT_EXPORT StrToIdEntryIterator operator++(int);	// postincrement -- expensive

	PARLIAMENT_EXPORT reference operator*() const
		{ return m_curVal; }
	PARLIAMENT_EXPORT pointer operator->() const
		{ return &m_curVal; }

	PARLIAMENT_EXPORT bool operator==(const StrToIdEntryIterator& rhs) const;
	PARLIAMENT_EXPORT bool operator!=(const StrToIdEntryIterator& rhs) const
		{ return !(*this == rhs); }

private:
	using Buffer = ::std::vector<RsrcChar>;

	static value_type nullValue()
		{ return ::std::make_pair(static_cast<value_type::first_type>(nullptr), k_nullRsrcId); }
	static DBC* createCursor(DB* pDB);
	static inline void checkBufferSizeDivisibleByCharSize(uint32 bufferSize);

	void closeCursor();
	void setCursorPosition();
	void advanceCursor();

	static const size_t	k_initialBufferSize = 128;

	DB*						m_pDB;		// Owned elsewhere -- do not clean this up on destruction!
	Buffer					m_buffer;
	value_type				m_curVal;
	DBC*						m_pCursor;
};

class StringToId
{
public:
	using const_iterator = StrToIdEntryIterator;

	struct Options
	{
		Options() : m_cacheGBytes(0), m_cacheBytes(0), m_numCacheSegments(0) {}
		Options(uint32 cacheGBytes, uint32 cacheBytes, uint32 numCacheSegments) :
			m_cacheGBytes(cacheGBytes),
			m_cacheBytes(cacheBytes),
			m_numCacheSegments(numCacheSegments)
		{}

		uint32 m_cacheGBytes;
		uint32 m_cacheBytes;
		uint32 m_numCacheSegments;
	};

	StringToId(const ::boost::filesystem::path& filePath, const ::std::string& optionStr, bool readOnly);
	StringToId(const StringToId&) = delete;
	StringToId& operator=(const StringToId&) = delete;
	StringToId(StringToId&&) = delete;
	StringToId& operator=(StringToId&&) = delete;
	~StringToId();

	void sync();
	void compact();

	bool isMember(const RsrcChar* pKey, size_t keyLen) const
		{ return find(pKey, keyLen) != k_nullRsrcId; }

	bool isMember(const RsrcChar* pKey) const
		{ return find(pKey) != k_nullRsrcId; }

	bool isMember(const RsrcString& key) const
		{ return find(key) != k_nullRsrcId; }

	size_t find(const RsrcChar* pKey, size_t keyLen) const;

	size_t find(const RsrcChar* pKey) const
		{ return find(pKey, ::std::char_traits<RsrcChar>::length(pKey)); }

	size_t find(const RsrcString& key) const
		{ return find(key.c_str(), key.length()); }

	const_iterator begin() const
		{ return StrToIdEntryIterator(m_pDB); }
	const_iterator cbegin() const
		{ return StrToIdEntryIterator(m_pDB); }
	const_iterator end() const
		{ return StrToIdEntryIterator(); }
	const_iterator cend() const
		{ return StrToIdEntryIterator(); }

	void insert(const RsrcChar* pKey, size_t keyLen, size_t value);

	void insert(const RsrcChar* pKey, size_t value)
		{ insert(pKey, ::std::char_traits<RsrcChar>::length(pKey), value); }

	void insert(const RsrcString& key, size_t value)
		{ insert(key.c_str(), key.length(), value); }

#if defined(PARLIAMENT_UNIT_TEST)
	static Options testParseOptionString(const ::std::string& optionStr)
		{ return parseOptionString(optionStr); }
#endif

private:
#if defined(USE_IN_MEMORY_LOOKUP_TABLE)
	using StringToIntMap = ::std::unordered_map<RsrcString, size_t>;
#endif

	static Options parseOptionString(const ::std::string& optionStr);
	void checkWritable() const;

	::boost::filesystem::path	m_filePath;
	::std::string					m_optionStr;
	bool								m_readOnly;
#if defined(USE_IN_MEMORY_LOOKUP_TABLE)
	StringToIntMap					m_db;
#else
	DB*								m_pDB;
#endif
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_STRINGTOID_H_INCLUDED
