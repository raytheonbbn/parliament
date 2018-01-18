// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_INVFUNCPROPRULE_H_INCLUDED)
#define PARLIAMENT_INVFUNCPROPRULE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/RuleEngine.h"

PARLIAMENT_NAMESPACE_BEGIN

class InvFuncPropRule : public Rule
{
public:
	InvFuncPropRule(KbInstance* pKB, RuleEngine* pRE);
	void applyRuleHead(BindingList &bindingList) override;
	bool mustRunAtStartup() const override
		{ return true; }
};

class InvFuncPropHelperRule : public Rule
{
public:
	InvFuncPropHelperRule(KbInstance* pKB, RuleEngine* pRE, ResourceId invFuncPropId);
	void applyRuleHead(BindingList &bindingList) override;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_INVFUNCPROPRULE_H_INCLUDED
