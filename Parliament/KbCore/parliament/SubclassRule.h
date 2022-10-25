// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_SUBCLASSRULE_H_INCLUDED)
#define PARLIAMENT_SUBCLASSRULE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/RuleEngine.h"

namespace bbn::parliament
{

class SubclassRule : public Rule
{
public:
	SubclassRule(KbInstance* pKB, RuleEngine* pRE);
	void applyRuleHead(BindingList &bindingList) override;
	bool mustRunAtStartup() const override
		{ return true; }

private:
	static void installSubClassOfSelfRule(KbInstance* pKB, RuleEngine* pRE, ResourceId clsRsrcId);
};

class SubclassHelperRule : public StandardRule
{
public:
	SubclassHelperRule(KbInstance* pKB, RuleEngine* pRE, ResourceId subClsRsrcId,
		ResourceId superClsRsrcId);
	void applyRuleHead(BindingList &bindingList) override;

private:
	ResourceId m_subClsRsrcId;
	ResourceId m_superClsRsrcId;
};

}	// namespace end

#endif // !PARLIAMENT_SUBCLASSRULE_H_INCLUDED
