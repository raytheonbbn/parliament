// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_VARRECORDTABLE_H_INCLUDED)
#define PARLIAMENT_VARRECORDTABLE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/FixRecordTable.h"

#include <boost/filesystem/path.hpp>

PARLIAMENT_NAMESPACE_BEGIN

class VarRecordTable
{
public:
	VarRecordTable(const ::boost::filesystem::path& filePath, bool readOnly,
			size_t capacity, double growthFactor)
		: m_fixRecTbl(filePath, readOnly, capacity, growthFactor) {}
	VarRecordTable(const VarRecordTable&) = delete;
	VarRecordTable& operator=(const VarRecordTable&) = delete;
	VarRecordTable(VarRecordTable&&) = delete;
	VarRecordTable& operator=(VarRecordTable&&) = delete;
	~VarRecordTable() = default;

	size_t size() const
		{ return m_fixRecTbl.recordCount(); }
	size_t maxSize() const
		{ return m_fixRecTbl.maxSize(); }
	size_t capacity() const
		{ return m_fixRecTbl.capacity(); }
	bool isEmpty() const
		{ return m_fixRecTbl.recordCount() == 0; }
	const RsrcChar* getRecordAt(size_t recordOffset) const
		{ return &m_fixRecTbl.getRecordAt(recordOffset); }
	size_t pushBack(const RsrcChar* pStr, size_t strLen);
	size_t pushBack(const RsrcChar* pStr);
	void sync()
		{ m_fixRecTbl.sync(); }
	void releaseExcessCapacity()
		{ m_fixRecTbl.releaseExcessCapacity(); }

private:
	FixRecordTable<RsrcChar> m_fixRecTbl;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_VARRECORDTABLE_H_INCLUDED
