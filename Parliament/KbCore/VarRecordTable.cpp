// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/VarRecordTable.h"
#include "parliament/Exceptions.h"

#include <vector>

namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::std::char_traits;
using ::std::string;
using ::std::vector;

static const pmnt::RsrcChar k_zero = 0;

size_t pmnt::VarRecordTable::pushBack(const RsrcChar* pStr, size_t strLen)
{
	if (pStr == nullptr)
	{
		throw Exception("Error:  Null string pointer passed to VarRecordTable::pushBack()");
	}

	// Add the character sequence to the file:
	const RsrcChar* pRecord = m_fixRecTbl.pushBack(pStr, strLen);

	// Compute the offset of the record sequence (it is important to do this
	// before the next statement, because the next statement could cause a file
	// reallocation, which would change the base address of the file mapping):
	size_t result = pRecord - &m_fixRecTbl.getRecordAt(0);

	// Add a terminating null:
	m_fixRecTbl.pushBack(&k_zero, 1);
	return result;
}

size_t pmnt::VarRecordTable::pushBack(const RsrcChar* pStr)
{
	if (pStr == nullptr)
	{
		throw Exception("Error:  Null string pointer passed to VarRecordTable::pushBack()");
	}

	size_t numCharsToPush = char_traits<RsrcChar>::length(pStr) + 1;
	const RsrcChar* pRecord = m_fixRecTbl.pushBack(pStr, numCharsToPush);
	return pRecord - &m_fixRecTbl.getRecordAt(0);
}
