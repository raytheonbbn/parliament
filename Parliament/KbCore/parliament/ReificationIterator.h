// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_REIFICATIONITERATOR_H_INCLUDED)
#define PARLIAMENT_REIFICATIONITERATOR_H_INCLUDED

#include "parliament/Types.h"
#include "parliament/PhysicalStmtIterator.h"

#include <iterator>

PARLIAMENT_NAMESPACE_BEGIN

class KbInstance;

class ReificationIterator
{
public:
	using iterator_category = ::std::forward_iterator_tag;
	using value_type = ::std::pair<ResourceId,StatementId>;
	using difference_type = ptrdiff_t;
	using pointer = const value_type*;
	using reference = const value_type&;

	// These two ctors are intended to be called only from KbInstance's
	// begin(), end(), and find() methods.  The first ctor produces an
	// "end" iterator, and the second one creates a "begin".
	ReificationIterator(const KbInstance* pKB) :
			m_pKb(pKB),
			m_stmtName(k_nullRsrcId),
			m_subjectId(k_nullRsrcId),
			m_predicateId(k_nullRsrcId),
			m_objectId(k_nullRsrcId),
			m_pStmtAdvanceFxn(nullptr),
			m_nameIter(pKB),
			m_stmtIter(pKB),
			m_currentPair(k_nullRsrcId, k_nullStmtId)
		{}

	ReificationIterator(const KbInstance* pKB, ResourceId statementName,
		ResourceId subjectId, ResourceId predicateId, ResourceId objectId);

	ReificationIterator(const ReificationIterator&) = default;
	ReificationIterator& operator=(const ReificationIterator&) = default;

	PARLIAMENT_EXPORT ~ReificationIterator()
		{}

	PARLIAMENT_EXPORT ReificationIterator& operator++()	// preincrement
		{
			(this->*m_pStmtAdvanceFxn)();
			return *this;
		}
	PARLIAMENT_EXPORT ReificationIterator operator++(int)	// postincrement
		{
			ReificationIterator result = *this;
			(this->*m_pStmtAdvanceFxn)();
			return result;
		}

	PARLIAMENT_EXPORT ResourceId subjectId() const;

	PARLIAMENT_EXPORT ResourceId predicateId() const;

	PARLIAMENT_EXPORT ResourceId objectId() const;

	PARLIAMENT_EXPORT bool isLiteral() const;

	PARLIAMENT_EXPORT reference operator*() const
		{ return m_currentPair; }
	PARLIAMENT_EXPORT pointer operator->() const
		{ return &m_currentPair; }

	// This method is intended only for use by the ReificationIterator JNI interface.
	bool isEnd() const
		{ return m_currentPair.first == k_nullRsrcId && m_currentPair.second == k_nullStmtId; }

	friend PARLIAMENT_EXPORT bool operator==(const ReificationIterator& lhs, const ReificationIterator& rhs);
	friend PARLIAMENT_EXPORT bool operator!=(const ReificationIterator& lhs, const ReificationIterator& rhs);

private:
	friend class KbInstance;

	using StmtAdvanceFxn = void (ReificationIterator::*)();

	void advanceByStatementName();
	void advanceByStatement();
	void createNameIteratorFromStatementIterator();
	bool checkCurrentAndSet();
	void advanceByStatementNameInternal();

	const KbInstance*		m_pKb;
	ResourceId				m_stmtName;
	ResourceId				m_subjectId;
	ResourceId				m_predicateId;
	ResourceId				m_objectId;
	StmtAdvanceFxn			m_pStmtAdvanceFxn;
	PhysicalStmtIterator	m_nameIter;
	PhysicalStmtIterator	m_stmtIter;
	value_type				m_currentPair;
};

PARLIAMENT_EXPORT bool operator==(const ReificationIterator& lhs, const ReificationIterator& rhs);
PARLIAMENT_EXPORT inline bool operator!=(const ReificationIterator& lhs, const ReificationIterator& rhs)
	{ return !(lhs == rhs); }

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_REIFICATIONITERATOR_H_INCLUDED
