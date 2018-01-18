// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

// Note:  Some of these methods may seem like prime candidates for inlining.
// However, this requires #including KbInstance.h in Statement.h, creating
// a circular dependency.

#include "parliament/Statement.h"
#include "parliament/Exceptions.h"
#include "parliament/KbInstance.h"
#include "parliament/UriLib.h"

namespace pmnt = ::bbn::parliament;

pmnt::ResourceId pmnt::Statement::getSubjectId() const
{
	return (m_stmtName != k_nullRsrcId)
		? m_stmtName
		: m_pKb->subject(m_stmtId);
}

pmnt::ResourceId pmnt::Statement::getPredicateId() const
{
	if (m_stmtName == k_nullRsrcId)
	{
		return m_pKb->predicate(m_stmtId);
	}
	else
	{
		switch (m_virtualMode)
		{
		case VirtualStmtMode::k_subject:
			return m_pKb->uriLib().m_rdfSubject.id();
		case VirtualStmtMode::k_predicate:
			return m_pKb->uriLib().m_rdfPredicate.id();
		case VirtualStmtMode::k_object:
			return m_pKb->uriLib().m_rdfObject.id();
		case VirtualStmtMode::k_type:
			return m_pKb->uriLib().m_rdfType.id();
		default:
			throw Exception("Statement created with invalid virtual mode");
		}
	}
}

pmnt::ResourceId pmnt::Statement::getObjectId() const
{
	if (m_stmtName == k_nullRsrcId)
	{
		return m_pKb->object(m_stmtId);
	}
	else
	{
		switch (m_virtualMode)
		{
		case VirtualStmtMode::k_subject:
			return m_pKb->subject(m_stmtId);
		case VirtualStmtMode::k_predicate:
			return m_pKb->predicate(m_stmtId);
		case VirtualStmtMode::k_object:
			return m_pKb->object(m_stmtId);
		case VirtualStmtMode::k_type:
			return m_pKb->uriLib().m_rdfStatement.id();
		default:
			throw Exception("Statement created with invalid virtual mode");
		}
	}
}

bool pmnt::Statement::isDeleted() const
{
	return m_stmtName == k_nullRsrcId && m_pKb->isStmtDeleted(m_stmtId);
}

bool pmnt::Statement::isInferred() const
{
	return m_stmtName == k_nullRsrcId && m_pKb->isStmtInferred(m_stmtId);
}

bool pmnt::Statement::isLiteral() const
{
	return (m_stmtName == k_nullRsrcId || m_virtualMode == VirtualStmtMode::k_object)
		&& m_pKb->isRsrcLiteral(m_pKb->object(m_stmtId));
}

bool pmnt::Statement::isHidden() const
{
	return m_stmtName == k_nullRsrcId && m_pKb->isStmtHidden(m_stmtId);
}
