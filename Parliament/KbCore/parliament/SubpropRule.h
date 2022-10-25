// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_SUBPROPRULE_H_INCLUDED)
#define PARLIAMENT_SUBPROPRULE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/RuleEngine.h"

namespace bbn::parliament
{

class SubpropRule : public Rule
{
public:
	SubpropRule(KbInstance* pKB, RuleEngine* pRE);
	void applyRuleHead(BindingList &bindingList) override;
	bool mustRunAtStartup() const override
		{ return true; }

private:
	static void installSubPropOfSelfRule(KbInstance* pKB, RuleEngine* pRE, ResourceId clsRsrcId);
};

class SubpropHelperRule : public StandardRule
{
public:
	SubpropHelperRule(KbInstance* pKB, RuleEngine* pRE, ResourceId subPropRsrcId,
		ResourceId superPropRsrcId);
	void applyRuleHead(BindingList &bindingList) override;

private:
	ResourceId m_subPropRsrcId;
	ResourceId m_superPropRsrcId;
};

}	// namespace end

#endif // !PARLIAMENT_SUBPROPRULE_H_INCLUDED
