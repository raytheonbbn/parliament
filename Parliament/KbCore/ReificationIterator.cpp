// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/ReificationIterator.h"
#include "parliament/Exceptions.h"
#include "parliament/KbInstance.h"
#include "parliament/UriLib.h"

namespace pmnt = ::bbn::parliament;

pmnt::ReificationIterator::ReificationIterator(const KbInstance* pKB,
		ResourceId stmtName, ResourceId subjectId, ResourceId predicateId,
		ResourceId objectId) :
	m_pKb(pKB),
	m_stmtName(stmtName),
	m_subjectId(subjectId),
	m_predicateId(predicateId),
	m_objectId(objectId),
	m_pStmtAdvanceFxn(nullptr),
	m_nameIter(pKB),
	m_stmtIter(pKB),
	m_currentPair(k_nullRsrcId, k_nullStmtId)
{
	if (m_stmtName != k_nullRsrcId)
	{
		m_pStmtAdvanceFxn = &ReificationIterator::advanceByStatementName;
		const StmtIteratorFlags flags = StmtIteratorFlags::k_skipDeleted
			| StmtIteratorFlags::k_skipVirtual | StmtIteratorFlags::k_showHidden;
		m_nameIter = m_pKb->findPhysical(k_nullRsrcId,
			m_pKb->uriLib().m_statementHasName.id(), m_stmtName, flags);
		advanceByStatementNameInternal();
	}
	else
	{
		m_pStmtAdvanceFxn = &ReificationIterator::advanceByStatement;
		// Don't skip deleted here, need to be able to see reifications of
		// non-existent statements:
		m_stmtIter = m_pKb->findPhysical(m_subjectId, m_predicateId, m_objectId,
			StmtIteratorFlags::k_skipVirtual);
		advanceByStatement();
	}
}

void pmnt::ReificationIterator::advanceByStatementName()
{
	++m_nameIter;
	advanceByStatementNameInternal();
}

void pmnt::ReificationIterator::advanceByStatementNameInternal()
{
	while (!m_nameIter.isEnd())
	{
		ResourceId stmtTag = m_pKb->subject(m_nameIter.stmtId());
		StatementId stmtId = m_pKb->getStatementIdForStatementTag(stmtTag);
		if ( (m_subjectId == k_nullRsrcId || m_subjectId == m_pKb->subject(stmtId))
			&& (m_predicateId == k_nullRsrcId || m_predicateId == m_pKb->predicate(stmtId))
			&& (m_objectId == k_nullRsrcId || m_objectId == m_pKb->object(stmtId)))
		{
			m_currentPair.first = m_pKb->object(m_nameIter.stmtId());
			m_currentPair.second = stmtId;
			return;
		}
		++m_nameIter;
	}

	m_currentPair.first = k_nullRsrcId;
	m_currentPair.second = k_nullStmtId;
}

void pmnt::ReificationIterator::advanceByStatement()
{
	if (m_nameIter.isEnd() && !m_stmtIter.isEnd())
	{
		createNameIteratorFromStatementIterator();
		if (checkCurrentAndSet())
		{
			return;
		}
	}
	while(!m_stmtIter.isEnd())
	{
		while(!m_nameIter.isEnd())
		{
			++m_nameIter;
			if (checkCurrentAndSet())
			{
				return;
			}
		}
		++m_stmtIter;
		if (!m_stmtIter.isEnd())
		{
			createNameIteratorFromStatementIterator();
			if (checkCurrentAndSet())
			{
				return;
			}
		}
	}
	m_currentPair.first = k_nullRsrcId;
	m_currentPair.second = k_nullStmtId;
}

void pmnt::ReificationIterator::createNameIteratorFromStatementIterator()
{
	ResourceId statementTag = m_pKb->statementTag(m_stmtIter.stmtId());
	if (statementTag == k_nullRsrcId)
	{
		// This ensures that no names will be found
		statementTag = m_pKb->uriLib().m_statementHasName.id();
	}
	m_nameIter = m_pKb->findPhysical(statementTag,
		m_pKb->uriLib().m_statementHasName.id(), k_nullRsrcId,
		StmtIteratorFlags::k_skipDeleted | StmtIteratorFlags::k_skipVirtual | StmtIteratorFlags::k_showHidden);
}

bool pmnt::ReificationIterator::checkCurrentAndSet()
{
	if (!m_nameIter.isEnd() && !m_pKb->isStmtDeleted(m_nameIter.stmtId()))
	{
		m_currentPair.first = m_pKb->object(m_nameIter.stmtId());
		m_currentPair.second = m_stmtIter.stmtId();
		return true;
	}
	return false;
}

pmnt::ResourceId pmnt::ReificationIterator::subjectId() const
{
	return m_pKb->subject(m_currentPair.second);
}

pmnt::ResourceId pmnt::ReificationIterator::predicateId() const
{
	return m_pKb->predicate(m_currentPair.second);
}

pmnt::ResourceId pmnt::ReificationIterator::objectId() const
{
	return m_pKb->object(m_currentPair.second);
}

bool pmnt::ReificationIterator::isLiteral() const
{
	return m_pKb->isRsrcLiteral(m_pKb->object(m_currentPair.second));
}

bool pmnt::operator==(const ReificationIterator& lhs, const ReificationIterator& rhs)
{
	if (lhs.m_pKb != rhs.m_pKb)
	{
		throw Exception("The two iterators are not comparable "
			"because they refer to different KB's");
	}
	return lhs.m_nameIter == rhs.m_nameIter
		&& lhs.m_stmtIter == rhs.m_stmtIter;
}
