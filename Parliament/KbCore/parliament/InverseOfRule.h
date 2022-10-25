// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_INVERSEOFRULE_H_INCLUDED)
#define PARLIAMENT_INVERSEOFRULE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/RuleEngine.h"

namespace bbn::parliament
{

class InverseOfRule : public Rule
{
public:
	InverseOfRule(KbInstance* pKB, RuleEngine* pRE);
	void applyRuleHead(BindingList &bindingList) override;
	bool mustRunAtStartup() const override
		{ return true; }
};

}	// namespace end

#endif // !PARLIAMENT_INVERSEOFRULE_H_INCLUDED
