// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_KBRSRC_H_INCLUDED)
#define PARLIAMENT_KBRSRC_H_INCLUDED

#include "parliament/Types.h"
#include "parliament/Util.h"

namespace bbn::parliament
{

struct KbRsrc
{
	static constexpr size_t anonUriOffset() noexcept
		{ return ::std::numeric_limits<size_t>::max(); }

	StatementId	m_subjectFirst;
	StatementId	m_predicateFirst;
	StatementId	m_objectFirst;
	size_t			m_subjectCount;
	size_t			m_predicateCount;
	size_t			m_objectCount;
	size_t			m_uriOffset;
	Flags			m_flags;

	// This init() method should be a default ctor, but because we do pointer
	// arithemtic on KbRsrc (in particular, using offsetof), we need KbRsrc to
	// be a POD (Plain Old Data) which precludes declaring a ctor.
	void init() noexcept
	{
		m_subjectFirst = k_nullStmtId;
		m_predicateFirst = k_nullStmtId;
		m_objectFirst = k_nullStmtId;
		m_subjectCount = 0;
		m_predicateCount = 0;
		m_objectCount = 0;
		m_uriOffset = anonUriOffset();
		m_flags = asBitMask(ResourceFlags::k_rsrcFlagValid);
	}
	bool testFlag(ResourceFlags flag) const noexcept
		{ return !!(m_flags & asBitMask(flag)); }
	void setFlag(ResourceFlags flag, bool value) noexcept
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
		{ return testFlag(ResourceFlags::k_rsrcFlagValid); }
	bool isLiteral() const noexcept
		{ return testFlag(ResourceFlags::k_rsrcFlagLiteral); }
	bool isAnonymous() const noexcept
		{ return testFlag(ResourceFlags::k_rsrcFlagAnonymous); }
	bool isStatementTag() const noexcept
		{ return testFlag(ResourceFlags::k_rsrcFlagStatementTag); }
};

}	// namespace end

#endif // !PARLIAMENT_KBRSRC_H_INCLUDED
