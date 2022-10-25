// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_FIXRECORDTABLE_H_INCLUDED)
#define PARLIAMENT_FIXRECORDTABLE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"
#include "parliament/Exceptions.h"
#include "parliament/MMapMgr.h"
#include "parliament/UnicodeIterator.h"

#include <algorithm>
#include <boost/filesystem/path.hpp>
#include <boost/format.hpp>
#include <cmath>
#include <limits>
#include <type_traits>

namespace bbn::parliament
{

template<typename RT>
class FixRecordTable
{
public:
	using RecordType = RT;

	FixRecordTable(const ::boost::filesystem::path& filePath, bool readOnly,
			size_t initRecCount, size_t growthIncrement, double growthFactor) :
		m_mMap(filePath, readOnly, computeFileSize(initRecCount)),
		m_growthIncrement(growthIncrement),
		m_growthFactor(growthFactor)
		{
			if (m_growthIncrement < 1 && m_growthFactor <= 1.0)
			{
				throw Exception(::boost::format(
					"FixRecordTable:  Either the growth increment must be positive or "
					"the growth factor must be greater than 1.0 for KB file \"%1%\"")
					% pathAsUtf8(m_mMap.filePath()));
			}
		}
	FixRecordTable(const FixRecordTable&) = delete;
	FixRecordTable& operator=(const FixRecordTable&) = delete;
	FixRecordTable(FixRecordTable&&) = delete;
	FixRecordTable& operator=(FixRecordTable&&) = delete;
	~FixRecordTable() = default;

	RT& getRecordAt(size_t recIndex) const
		{
			if (recIndex >= recordCount())
			{
				throw Exception(::boost::format("FixRecordTable index 0x%|1$016x| out of "
					"range for KB file \"%2%\"") % recIndex % pathAsUtf8(m_mMap.filePath()));
			}
			return uncheckedGetRecordAt(recIndex);
		}
	size_t recordCount() const
		{ return m_mMap.header().m_recordCount; }
	bool isEmpty() const
		{ return recordCount() == 0; }
	size_t capacity() const
		{ return static_cast<size_t>((m_mMap.fileSize() - firstRecOffset()) / recSize()); }
	size_t maxSize() const
		{ return (::std::numeric_limits<size_t>::max() - firstRecOffset()) / recSize(); }
	RT* pushBack(const RT& newRecordValue)
		{ return pushBack(&newRecordValue, 1); }
	RT* pushBack(const RT* pFirstNewRecordValue, size_t numNewRecords)
		{
			const size_t oldRecCount = recordCount();
			RT* pResult = &uncheckedGetRecordAt(oldRecCount);

			if (oldRecCount + numNewRecords > capacity())
			{
				size_t newRecCount = oldRecCount + numNewRecords;
				if (m_growthFactor > 1.0)
				{
					using ULongLong = ::std::make_unsigned<long long>::type;
					newRecCount = static_cast<size_t>(
						static_cast<ULongLong>(
						::std::llrint(m_growthFactor * newRecCount)));
				}
				if (m_growthIncrement > 0)
				{
					newRecCount += m_growthIncrement;
				}
				if (newRecCount <= oldRecCount + numNewRecords)
				{
					throw Exception(::boost::format(
						"FixRecordTable:  Growth increment of %1% and growth factor of "
						"%2% failed to grow KB file \"%3%\"")
						% m_growthIncrement % m_growthFactor % pathAsUtf8(m_mMap.filePath()));
				}
				m_mMap.reallocate(computeFileSize(newRecCount));
				pResult = &uncheckedGetRecordAt(oldRecCount);
			}

			// copy new records into the file
			::std::copy(pFirstNewRecordValue, pFirstNewRecordValue + numNewRecords, pResult);

			// update the record count
			m_mMap.header().m_recordCount = oldRecCount + numNewRecords;
			return pResult;
		}
	void popBack(size_t numRecordsToPop = 1)
		{
			const size_t oldRecCount = recordCount();
			numRecordsToPop = ::std::min(numRecordsToPop, oldRecCount);
			m_mMap.header().m_recordCount = oldRecCount - numRecordsToPop;
		}
	void sync()
		{ m_mMap.sync(); }
	void releaseExcessCapacity()
		{
			if (capacity() > recordCount())
			{
				m_mMap.reallocate(computeFileSize(recordCount()));
			}
		}

#if defined(PARLIAMENT_UNIT_TEST)
	static size_t testFirstRecOffset() { return firstRecOffset(); }
	static size_t testRecSize() { return recSize(); }
#endif

private:
	// By using this struct to determine the memory layout, we let
	// the compiler figure out all of the alignment issues for us.
	template<typename RecType>
	struct TblAlignmentGuide
	{
		MMapMgr::TblHeader		m_header;
		RecType					m_recordArray[2];
	};

	static size_t firstRecOffset()
		{ return offsetof(TblAlignmentGuide<RT>, m_recordArray[0]); }

	// This is the number of bytes between the start of one record
	// and the start of the next.  Note that this is not necessarily
	// the same as sizeof(RT), since the compiler may insert padding
	// between array elements to guarantee proper alignment.
	static size_t recSize()
		{ return offsetof(TblAlignmentGuide<RT>, m_recordArray[1]) - firstRecOffset(); }

	// To compute the file size corresponding to a given record
	// count, we add the header size to the record size times the
	// number of records.  However, we use firstRecOffset() and
	// recSize() rather than sizeof(MMapMgr::TblHeader) and sizeof(RT)
	// to account for padding bytes that the compiler may insert
	// between struct and array elements to guarantee proper alignment.
	static FileHandle::FileSize computeFileSize(size_t recordCount)
		{ return firstRecOffset() + recSize() * recordCount; }

	RT& uncheckedGetRecordAt(size_t recIndex) const
		{
			return static_cast<TblAlignmentGuide<RT>*>(
				static_cast<void*>(m_mMap.baseAddr()))->m_recordArray[recIndex];
		}

	MMapMgr			m_mMap;
	const size_t	m_growthIncrement;
	const double	m_growthFactor;
};

}	// namespace end

#endif // !PARLIAMENT_FIXRECORDTABLE_H_INCLUDED
