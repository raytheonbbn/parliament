// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_RANGERULE_H_INCLUDED)
#define PARLIAMENT_RANGERULE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/RuleEngine.h"

namespace bbn::parliament
{

class RangeRule : public Rule
{
public:
	RangeRule(KbInstance* pKB, RuleEngine* pRE);
	void applyRuleHead(BindingList &bindingList) override;
	bool mustRunAtStartup() const override
		{ return true; }
};

class RangeHelperRule : public Rule
{
public:
	RangeHelperRule(KbInstance* pKB, RuleEngine* pRE, ResourceId propId, ResourceId rangeId);
	void applyRuleHead(BindingList &bindingList) override;

private:
	ResourceId m_propId;
	ResourceId m_rangeId;
};

}	// namespace end

#endif // !PARLIAMENT_RANGERULE_H_INCLUDED
