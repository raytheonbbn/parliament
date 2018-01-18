// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_STMTITERATOR_H_INCLUDED)
#define PARLIAMENT_STMTITERATOR_H_INCLUDED

#include "parliament/Types.h"
#include "parliament/KbStmt.h"
#include "parliament/Statement.h"
#include "parliament/ReificationIterator.h"

#include <iterator>

PARLIAMENT_NAMESPACE_BEGIN

class KbInstance;

class StmtIterator
{
public:
	using iterator_category = ::std::forward_iterator_tag;
	using value_type = KbStmt;
	using difference_type = ptrdiff_t;
	using pointer = KbStmt*;
	using reference = KbStmt&;

	// Conceptually private:
	// These two ctors are intended to be called only from KbInstance's
	// begin(), end(), and find() methods.  The first ctor produces an
	// "end" iterator, and the second one creates a "begin".
	StmtIterator(const KbInstance* pKB) :
			m_pKb(pKB),
			m_subjectId(k_nullRsrcId),
			m_predicateId(k_nullRsrcId),
			m_objectId(k_nullRsrcId),
			m_flags(StmtIteratorFlags::k_skipNone),
			m_stmtId(k_nullStmtId),
			m_pStmtAdvanceFxn(0),
			m_reificationEmit(0),
			m_reificationEmitMode4SubMode(0),
			m_reificationIter(m_pKb),
			m_statement(m_pKb, k_nullStmtId)
		{}
	StmtIterator(const KbInstance* pKB,
		ResourceId subjectId, ResourceId predicateId,
		ResourceId objectId, StmtIteratorFlags flags = StmtIteratorFlags::k_skipDeleted);

	StmtIterator(const StmtIterator&) = default;
	StmtIterator& operator=(const StmtIterator&) = default;

	PARLIAMENT_EXPORT ~StmtIterator()
		{}

	PARLIAMENT_EXPORT StmtIterator& operator++()	// preincrement
		{
			(this->*m_pStmtAdvanceFxn)();
			return *this;
		}
	PARLIAMENT_EXPORT StmtIterator operator++(int)	// postincrement
		{
			StmtIterator result = *this;
			(this->*m_pStmtAdvanceFxn)();
			return result;
		}

	PARLIAMENT_EXPORT Statement statement() const
		{ return m_statement; }
	PARLIAMENT_EXPORT const Statement& statementRef() const
		{ return m_statement; }

#if 0
	// In order for StmtIterator to behave like a true STL iterator class,
	// it needs the following two methods.  However, it is preferred
	// that clients use statement ids rather than KbStmt pointers
	// to refer to statements, so they are elided.  To restore these
	// methods, remove the #if and add a friend declaration for
	// StmtIterator to the KbInstance class.
	PARLIAMENT_EXPORT reference operator*() const
		{ return *(m_pKb->stmtIdToStmt(m_stmtId)); }
	PARLIAMENT_EXPORT pointer operator->() const
		{ return (m_stmtId == k_nullStmtId) ? 0 : m_pKb->stmtIdToStmt(m_stmtId); }
#endif

	// Conceptually private:
	// This method is intended only for use by the StmtIterator JNI interface.
	bool isEnd() const
		{ return m_stmtId == k_nullStmtId; }

	bool includeDeletedStmts() const
		{ return isZero(m_flags & StmtIteratorFlags::k_skipDeleted); }
	bool includeInferredStmts() const
		{ return isZero(m_flags & StmtIteratorFlags::k_skipInferred); }
	bool includeLiteralStmts() const
		{ return isZero(m_flags & StmtIteratorFlags::k_skipLiteral); }
	bool includeNonLiteralStmts() const
		{ return isZero(m_flags & StmtIteratorFlags::k_skipNonLiteral); }
	bool includeHiddenStmts() const
		{ return !isZero(m_flags & StmtIteratorFlags::k_showHidden); }
	DeletedStmtsAction delStmtsAction() const
		{
			return includeDeletedStmts()
				? DeletedStmtsAction::include
				: DeletedStmtsAction::exclude;
		}

	friend PARLIAMENT_EXPORT bool operator==(const StmtIterator& lhs, const StmtIterator& rhs);
	friend PARLIAMENT_EXPORT bool operator!=(const StmtIterator& lhs, const StmtIterator& rhs);

private:
	friend class KbInstance;

	using StmtAdvanceFxn = void (StmtIterator::*)();

	void advanceByNone();
	void advanceByNoneInternal();
	void advanceBySubject();
	void advanceBySubjectInternal();
	void advanceByPredicate();
	void advanceByPredicateInternal();
	void advanceByObject();
	void advanceByObjectInternal();
	void advanceByReification();
	void advanceByReificationInternal();
	void prepareForReificationTriples();
	void prepareBasicStatement();
	void prepareReificationStatement(int mode);

	const KbInstance*		m_pKb;
	ResourceId				m_subjectId;
	ResourceId				m_predicateId;
	ResourceId				m_objectId;
	StmtIteratorFlags		m_flags;
	StatementId				m_stmtId;
	StmtAdvanceFxn			m_pStmtAdvanceFxn;
	int						m_reificationEmit;
	int						m_reificationEmitMode4SubMode;
	ReificationIterator	m_reificationIter;
	Statement				m_statement;
};

PARLIAMENT_EXPORT bool operator==(const StmtIterator& lhs, const StmtIterator& rhs);
PARLIAMENT_EXPORT inline bool operator!=(const StmtIterator& lhs, const StmtIterator& rhs)
	{ return !(lhs == rhs); }

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_STMTITERATOR_H_INCLUDED
