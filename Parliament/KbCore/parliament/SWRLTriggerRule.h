// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_SWRLTRIGGERRULE_H_INCLUDED)
#define PARLIAMENT_SWRLTRIGGERRULE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/RuleEngine.h"

PARLIAMENT_NAMESPACE_BEGIN

class SWRLTriggerRule : public Rule
{
public:
	SWRLTriggerRule(KbInstance* pKB, RuleEngine* pRE);

	void applyRuleHead(BindingList &variableBindings) override;

	bool mustRunAtStartup() const override
		{ return true; }
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_SWRLTRIGGERRULE_H_INCLUDED
