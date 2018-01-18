// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/PhysicalStmtIterator.h"
#include "parliament/Exceptions.h"
#include "parliament/KbInstance.h"

#include <algorithm>
#include <limits>

namespace pmnt = ::bbn::parliament;

using ::std::min;
using ::std::numeric_limits;

pmnt::PhysicalStmtIterator::PhysicalStmtIterator(const KbInstance* pKB,
		ResourceId subjectId, ResourceId predicateId, ResourceId objectId,
		StmtIteratorFlags flags) :
	m_pKb(pKB),
	m_subjectId(subjectId),
	m_predicateId(predicateId),
	m_objectId(objectId),
	m_flags(flags),
	m_stmtId(k_nullStmtId),
	m_pStmtAdvanceFxn(0)
{
#if defined(PARLIAMENT_WINDOWS)
#	pragma warning(push)
#	pragma warning(disable : 4800) // forcing value to bool 'true' or 'false' (performance warning)
#endif
	if (static_cast<bool>(m_flags & StmtIteratorFlags::k_skipLiteral)
		&& static_cast<bool>(m_flags & StmtIteratorFlags::k_skipNonLiteral))
	{
		throw Exception("Iterator flags must not include both k_skipLiteral "
			"and k_skipNonLiteral");
	}
#if defined(PARLIAMENT_WINDOWS)
#	pragma warning(pop)
#endif

	size_t subCount = (m_subjectId == k_nullRsrcId)
		? numeric_limits<size_t>::max()
		: m_pKb->subjectCount(m_subjectId);
	size_t predCount = (m_predicateId == k_nullRsrcId)
		? numeric_limits<size_t>::max()
		: m_pKb->predicateCount(m_predicateId);
	size_t objCount = (m_objectId == k_nullRsrcId)
		? numeric_limits<size_t>::max()
		: m_pKb->objectCount(m_objectId);

	size_t minCounter = min(subCount, min(predCount, objCount));

	if (minCounter == numeric_limits<size_t>::max())
	{
		m_pStmtAdvanceFxn = &PhysicalStmtIterator::advanceByNone;
		m_stmtId = (m_pKb->stmtCount() > 0) ? 0 : k_nullStmtId;
		advanceByNoneInternal();
	}
	else if (minCounter == subCount)
	{
		m_pStmtAdvanceFxn = &PhysicalStmtIterator::advanceBySubject;
		m_stmtId = m_pKb->firstSubject(m_subjectId, delStmtsAction());
		advanceBySubjectInternal();
	}
	else if (minCounter == predCount)
	{
		m_pStmtAdvanceFxn = &PhysicalStmtIterator::advanceByPredicate;
		m_stmtId = m_pKb->firstPredicate(m_predicateId, delStmtsAction());
		advanceByPredicateInternal();
	}
	else
	{
		m_pStmtAdvanceFxn = &PhysicalStmtIterator::advanceByObject;
		m_stmtId = m_pKb->firstObject(m_objectId, delStmtsAction());
		advanceByObjectInternal();
	}
}

void pmnt::PhysicalStmtIterator::advanceByNone()
{
	if (m_stmtId != k_nullStmtId && ++m_stmtId >= m_pKb->stmtCount())
	{
		m_stmtId = k_nullStmtId;
	}
	advanceByNoneInternal();
}

void pmnt::PhysicalStmtIterator::advanceByNoneInternal()
{
	while (m_stmtId != k_nullStmtId)
	{
		bool isObjLiteral = m_pKb->isRsrcLiteral(m_pKb->object(m_stmtId));
		if ((includeDeletedStmts() || !m_pKb->isStmtDeleted(m_stmtId))
			&& (includeInferredStmts() || !m_pKb->isStmtInferred(m_stmtId))
			&& (includeLiteralStmts() || !isObjLiteral)
			&& (includeNonLiteralStmts() || isObjLiteral)
			&& (includeHiddenStmts() || !m_pKb->isStmtHidden(m_stmtId)))
		{
			break;
		}
		if (++m_stmtId >= m_pKb->stmtCount())
		{
			m_stmtId = k_nullStmtId;
		}
	}
}

void pmnt::PhysicalStmtIterator::advanceBySubject()
{
	if (m_stmtId != k_nullStmtId)
	{
		m_stmtId = m_pKb->nextSubject(m_stmtId, delStmtsAction());
	}
	advanceBySubjectInternal();
}

void pmnt::PhysicalStmtIterator::advanceBySubjectInternal()
{
	for (; m_stmtId != k_nullStmtId;
		m_stmtId = m_pKb->nextSubject(m_stmtId, delStmtsAction()))
	{
		bool isObjLiteral = m_pKb->isRsrcLiteral(m_pKb->object(m_stmtId));
		if ((m_predicateId == k_nullRsrcId || m_pKb->predicate(m_stmtId) == m_predicateId)
			&& (m_objectId == k_nullRsrcId || m_pKb->object(m_stmtId) == m_objectId)
			&& (includeInferredStmts() || !m_pKb->isStmtInferred(m_stmtId))
			&& (includeLiteralStmts() || !isObjLiteral)
			&& (includeNonLiteralStmts() || isObjLiteral)
			&& (includeHiddenStmts() || !m_pKb->isStmtHidden(m_stmtId)))
		{
			break;
		}
	}
}

void pmnt::PhysicalStmtIterator::advanceByPredicate()
{
	if (m_stmtId != k_nullStmtId)
	{
		m_stmtId = m_pKb->nextPredicate(m_stmtId, delStmtsAction());
	}
	advanceByPredicateInternal();
}

void pmnt::PhysicalStmtIterator::advanceByPredicateInternal()
{
	for (; m_stmtId != k_nullStmtId;
		m_stmtId = m_pKb->nextPredicate(m_stmtId, delStmtsAction()))
	{
		bool isObjLiteral = m_pKb->isRsrcLiteral(m_pKb->object(m_stmtId));
		if ((m_subjectId == k_nullRsrcId || m_pKb->subject(m_stmtId) == m_subjectId)
			&& (m_objectId == k_nullRsrcId || m_pKb->object(m_stmtId) == m_objectId)
			&& (includeInferredStmts() || !m_pKb->isStmtInferred(m_stmtId))
			&& (includeLiteralStmts() || !isObjLiteral)
			&& (includeNonLiteralStmts() || isObjLiteral)
			&& (includeHiddenStmts() || !m_pKb->isStmtHidden(m_stmtId)))
		{
			break;
		}
	}
}

void pmnt::PhysicalStmtIterator::advanceByObject()
{
	if (m_stmtId != k_nullStmtId)
	{
		m_stmtId = m_pKb->nextObject(m_stmtId, delStmtsAction());
	}
	advanceByObjectInternal();
}

void pmnt::PhysicalStmtIterator::advanceByObjectInternal()
{
	for (; m_stmtId != k_nullStmtId;
		m_stmtId = m_pKb->nextObject(m_stmtId, delStmtsAction()))
	{
		bool isObjLiteral = m_pKb->isRsrcLiteral(m_pKb->object(m_stmtId));
		if ((m_subjectId == k_nullRsrcId || m_pKb->subject(m_stmtId) == m_subjectId)
			&& (m_predicateId == k_nullRsrcId || m_pKb->predicate(m_stmtId) == m_predicateId)
			&& (includeInferredStmts() || !m_pKb->isStmtInferred(m_stmtId))
			&& (includeLiteralStmts() || !isObjLiteral)
			&& (includeNonLiteralStmts() || isObjLiteral)
			&& (includeHiddenStmts() || !m_pKb->isStmtHidden(m_stmtId)))
		{
			break;
		}
	}
}

bool pmnt::operator==(const PhysicalStmtIterator& lhs, const PhysicalStmtIterator& rhs)
{
	if (lhs.m_pKb != rhs.m_pKb)
	{
		throw Exception("The two iterators are not comparable "
			"because they refer to different KB's");
	}
	return lhs.m_stmtId == rhs.m_stmtId;
}
