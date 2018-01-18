// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/StmtIterator.h"
#include "parliament/Exceptions.h"
#include "parliament/KbInstance.h"
#include "parliament/UriLib.h"

#include <algorithm>
#include <limits>

namespace pmnt = ::bbn::parliament;

using ::std::min;
using ::std::numeric_limits;

pmnt::StmtIterator::StmtIterator(const KbInstance* pKB,
		ResourceId subjectId, ResourceId predicateId,
		ResourceId objectId, StmtIteratorFlags flags) :
	m_pKb(pKB),
	m_subjectId(subjectId),
	m_predicateId(predicateId),
	m_objectId(objectId),
	m_flags(flags),
	m_stmtId(k_nullStmtId),
	m_pStmtAdvanceFxn(0),
	m_reificationEmit(0),
	m_reificationEmitMode4SubMode(0),
	m_reificationIter(m_pKb),
	m_statement(m_pKb, k_nullStmtId)
{
	if (!isZero(m_flags & StmtIteratorFlags::k_skipLiteral)
		&& !isZero(m_flags & StmtIteratorFlags::k_skipNonLiteral))
	{
		throw Exception("Iterator flags must not include both k_skipLiteral "
			"and k_skipNonLiteral");
	}

	size_t subCount = (m_subjectId == k_nullRsrcId)
		? numeric_limits<size_t>::max()
		: m_pKb->subjectCount(m_subjectId);
	size_t predCount = (m_predicateId == k_nullRsrcId)
		? numeric_limits<size_t>::max()
		: m_pKb->predicateCount(m_predicateId);
	size_t objCount = (m_objectId == k_nullRsrcId)
		? numeric_limits<size_t>::max()
		: m_pKb->objectCount(m_objectId);

	size_t minCounter = min( subCount , min(predCount, objCount));

	if (minCounter == numeric_limits<size_t>::max())
	{
		m_pStmtAdvanceFxn = &StmtIterator::advanceByNone;
		m_stmtId = (m_pKb->stmtCount() > 0) ? 0 : k_nullStmtId;
		advanceByNoneInternal();
	}
	else if (minCounter == subCount)
	{
		m_pStmtAdvanceFxn = &StmtIterator::advanceBySubject;
		m_stmtId = m_pKb->firstSubject(m_subjectId, delStmtsAction());
		advanceBySubjectInternal();
	}
	else if (minCounter == predCount)
	{
		m_pStmtAdvanceFxn = &StmtIterator::advanceByPredicate;
		m_stmtId = m_pKb->firstPredicate(m_predicateId, delStmtsAction());
		advanceByPredicateInternal();
	}
	else
	{
		m_pStmtAdvanceFxn = &StmtIterator::advanceByObject;
		m_stmtId = m_pKb->firstObject(m_objectId, delStmtsAction());
		advanceByObjectInternal();
	}
}

void pmnt::StmtIterator::advanceByNone()
{
	if (m_stmtId != k_nullStmtId && ++m_stmtId >= m_pKb->stmtCount())
	{
		m_stmtId = k_nullStmtId;
	}
	advanceByNoneInternal();
}

void pmnt::StmtIterator::advanceByNoneInternal()
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
			prepareBasicStatement();
			return;
		}
		if (++m_stmtId >= m_pKb->stmtCount())
		{
			m_stmtId = k_nullStmtId;
		}
	}
	prepareForReificationTriples();
}


void pmnt::StmtIterator::advanceBySubject()
{
	if (m_stmtId != k_nullStmtId)
	{
		m_stmtId = m_pKb->nextSubject(m_stmtId, delStmtsAction());
	}
	advanceBySubjectInternal();
}

void pmnt::StmtIterator::advanceBySubjectInternal()
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
			prepareBasicStatement();
			return;
		}
	}
	prepareForReificationTriples();
}

void pmnt::StmtIterator::advanceByPredicate()
{
	if (m_stmtId != k_nullStmtId)
	{
		m_stmtId = m_pKb->nextPredicate(m_stmtId, delStmtsAction());
	}
	advanceByPredicateInternal();
}

void pmnt::StmtIterator::advanceByPredicateInternal()
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
			prepareBasicStatement();
			return;
		}
	}
	prepareForReificationTriples();
}

void pmnt::StmtIterator::advanceByObject()
{
	if (m_stmtId != k_nullStmtId)
	{
		m_stmtId = m_pKb->nextObject(m_stmtId, delStmtsAction());
	}
	advanceByObjectInternal();
}

void pmnt::StmtIterator::advanceByObjectInternal()
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
			prepareBasicStatement();
			return;
		}
	}
	prepareForReificationTriples();
}

void pmnt::StmtIterator::prepareForReificationTriples()
{
	/*
	Exclude bound preds != r:s, r:p, or r:o

	is subj bound?
		find statment name
		is statement name?
			emit 1-4 stmts, depending on predicate, bound object (reificationIterator (x, ?, ?, ?)), emit appropriate stmts
		else
			end
	is object bound?
		is pred bound?
			emit all stmt names with object in appropriate position (reificationIterator (_, ?, ?, ?)), emit one stmt
		is rdf:type rdf:statement?
			rI(_, ?, ?, ?)
		else
			emit all stmt names with object in any position. rI(_, x, _, _), rI(_, _, x, _), rI(_, _, _, x) emit one stmt per
	Is pred bound to r:s, r:p, or r:o?
		emit all stmt, position combinations rI(_, _, _, _), emit one stmt per
	is pred bound to rdf:type?
		emit all stmt names rI(_, _, _, _), emit one stmt per
	else
		emit all stmt/position combinations for all 3 positions rI(_, _, _, _), four stmts per
	*/

	if (isZero(m_flags & StmtIteratorFlags::k_skipVirtual))
	{
		const ResourceId rdfSubject = m_pKb->uriLib().m_rdfSubject.id();
		const ResourceId rdfPredicate = m_pKb->uriLib().m_rdfPredicate.id();
		const ResourceId rdfObject = m_pKb->uriLib().m_rdfObject.id();
		const ResourceId rdfStatement = m_pKb->uriLib().m_rdfStatement.id();
		const ResourceId rdfType = m_pKb->uriLib().m_rdfType.id();

		if (m_predicateId != k_nullRsrcId)
		{
			if (m_predicateId == rdfSubject)
			{
				m_reificationEmit = 0;
				m_reificationIter = ReificationIterator(m_pKb,
					m_subjectId, m_objectId, k_nullRsrcId, k_nullRsrcId);
			}
			else if(m_predicateId == rdfPredicate)
			{
				m_reificationEmit = 1;
				m_reificationIter = ReificationIterator(m_pKb,
					m_subjectId, k_nullRsrcId, m_objectId, k_nullRsrcId);
			}
			else if(m_predicateId == rdfObject)
			{
				m_reificationEmit = 2;
				m_reificationIter = ReificationIterator(m_pKb,
					m_subjectId, k_nullRsrcId, k_nullRsrcId, m_objectId);
			}
			else if (m_predicateId == rdfType
				&& (m_objectId == k_nullRsrcId || m_objectId == rdfStatement))
			{
				m_reificationEmit = 3;
				m_reificationIter = ReificationIterator(m_pKb,
					m_subjectId, k_nullRsrcId, k_nullRsrcId, k_nullRsrcId);
			}
			else
			{
				//Not a reification statement
				m_stmtId = k_nullStmtId;
				return;
			}
		}
		else if (m_objectId != k_nullRsrcId)
		{
			if (m_objectId == rdfStatement)
			{
				m_reificationEmit = 3;
				m_reificationIter = ReificationIterator(m_pKb,
					m_subjectId, k_nullRsrcId, k_nullRsrcId, k_nullRsrcId);
			}
			else
			{
				//FIRST OF SERIES OF 3 POSITIONS
				m_reificationEmit = 5;
				m_reificationIter = ReificationIterator(m_pKb,
					m_subjectId, m_objectId, k_nullRsrcId, k_nullRsrcId);
			}
		}
		else
		{
			m_reificationEmit = 4;
			m_reificationEmitMode4SubMode = 0;
			m_reificationIter = ReificationIterator(m_pKb,
				m_subjectId, k_nullRsrcId, k_nullRsrcId, k_nullRsrcId);
		}
		m_stmtId = 3;
		//advance by reification
		m_pStmtAdvanceFxn = &StmtIterator::advanceByReification;
		advanceByReificationInternal();
	}
}

void pmnt::StmtIterator::advanceByReification()
{
	if (m_reificationEmit != 4 || m_reificationEmitMode4SubMode == 3)
	{
		++m_reificationIter;
		m_reificationEmitMode4SubMode = 0;
		advanceByReificationInternal();
	}
	else
	{
		++m_reificationEmitMode4SubMode;
		prepareReificationStatement(m_reificationEmitMode4SubMode);
	}
}

void pmnt::StmtIterator::advanceByReificationInternal()
{
	if (m_reificationIter.isEnd())
	{
		//check for other iterators?
		if (m_reificationEmit == 5)
		{
			m_reificationIter = ReificationIterator(m_pKb,
				m_subjectId, k_nullRsrcId, m_objectId, k_nullRsrcId);
			++m_reificationEmit;
			advanceByReificationInternal();
		}
		else if (m_reificationEmit == 6)
		{
			m_reificationIter = ReificationIterator(m_pKb,
				m_subjectId, k_nullRsrcId, k_nullRsrcId, m_objectId);
			++m_reificationEmit;
			advanceByReificationInternal();
		}
		else
		{
			m_stmtId=k_nullStmtId;
		}
	}
	else
	{
		prepareReificationStatement((m_reificationEmit == 4)
			? m_reificationEmitMode4SubMode
			: m_reificationEmit);
	}
}

void pmnt::StmtIterator::prepareBasicStatement()
{
	m_statement = Statement(m_pKb, m_stmtId);
}

void pmnt::StmtIterator::prepareReificationStatement(int mode)
{
	ResourceId stmtName = m_reificationIter->first;
	StatementId stmtId = m_reificationIter->second;
	m_statement = Statement(m_pKb, stmtId, stmtName,
		static_cast<VirtualStmtMode>(mode % 5));
}

bool pmnt::operator==(const StmtIterator& lhs, const StmtIterator& rhs)
{
	if (lhs.m_pKb != rhs.m_pKb)
	{
		throw Exception("The two iterators are not comparable "
			"because they refer to different KB's");
	}
	return lhs.m_stmtId == rhs.m_stmtId;
}
