// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_STATEMENT_H_INCLUDED)
#define PARLIAMENT_STATEMENT_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

PARLIAMENT_NAMESPACE_BEGIN

class KbInstance;

enum class VirtualStmtMode
{
	k_subject,
	k_predicate,
	k_object,
	k_type
};

class Statement
{
public:
	Statement(const KbInstance* pKb, StatementId stmtId) :
			m_pKb(pKb),
			m_stmtId(stmtId),
			m_stmtName(k_nullRsrcId),
			m_virtualMode(VirtualStmtMode::k_subject)
		{}
	Statement(const KbInstance* pKb, StatementId stmtId,
		ResourceId stmtName, VirtualStmtMode virtualMode) :
			m_pKb(pKb),
			m_stmtId(stmtId),
			m_stmtName(stmtName),
			m_virtualMode(virtualMode)
		{}

	Statement(const Statement&) = default;
	Statement& operator=(const Statement&) = default;
	Statement(Statement&&) = default;
	Statement& operator=(Statement&&) = default;
	~Statement() = default;

	PARLIAMENT_EXPORT ResourceId getSubjectId() const;
	PARLIAMENT_EXPORT ResourceId getPredicateId() const;
	PARLIAMENT_EXPORT ResourceId getObjectId() const;
	StatementId getStatementId() const
		{ return (m_stmtName == k_nullRsrcId) ? m_stmtId : k_nullStmtId; }
	PARLIAMENT_EXPORT bool isDeleted() const;
	PARLIAMENT_EXPORT bool isInferred() const;
	bool isVirtual() const
		{ return m_stmtName != k_nullRsrcId; }
	PARLIAMENT_EXPORT bool isLiteral() const;
	PARLIAMENT_EXPORT bool isHidden() const;

private:
	const KbInstance*	m_pKb;
	StatementId			m_stmtId;
	ResourceId			m_stmtName;
	VirtualStmtMode	m_virtualMode;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_STATEMENT_H_INCLUDED
