// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_KBSTMT_H_INCLUDED)
#define PARLIAMENT_KBSTMT_H_INCLUDED

#include "parliament/Types.h"
#include "parliament/Util.h"

PARLIAMENT_NAMESPACE_BEGIN

struct KbStmt
{
	ResourceId	m_subjectId;
	ResourceId	m_predicateId;
	ResourceId	m_objectId;
	ResourceId	m_statementTag;
	StatementId	m_subjectNext;
	StatementId	m_predicateNext;
	StatementId	m_objectNext;
	Flags			m_flags;

	// This init() method should be a default ctor, but because we do pointer
	// arithemtic on KbStmt (in particular, using offsetof), we need KbStmt to
	// be a POD (Plain Old Data) which precludes declaring a ctor.
	void init() noexcept
	{
		m_subjectId = k_nullRsrcId;
		m_predicateId = k_nullRsrcId;
		m_objectId = k_nullRsrcId;
		m_statementTag = k_nullRsrcId;
		m_subjectNext = k_nullStmtId;
		m_predicateNext = k_nullStmtId;
		m_objectNext = k_nullStmtId;
		m_flags = 0;
	}
	bool testFlag(StatementFlags flag) const noexcept
		{ return !!(m_flags & asBitMask(flag)); }
	void setFlag(StatementFlags flag, bool value) noexcept
		{
			if (value)
			{
				m_flags |= asBitMask(flag);
			}
			else
			{
				m_flags &= ~asBitMask(flag);
			}
		}
	bool isValid() const noexcept
		{ return testFlag(StatementFlags::k_stmtFlagValid); }
	bool isDeleted() const noexcept
		{ return testFlag(StatementFlags::k_stmtFlagDeleted); }
	bool isInferred() const noexcept
		{ return testFlag(StatementFlags::k_stmtFlagInferred); }
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_KBSTMT_H_INCLUDED
