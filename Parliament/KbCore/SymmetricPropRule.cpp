// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/SymmetricPropRule.h"
#include "parliament/UriLib.h"

#include <memory>

namespace pmnt = ::bbn::parliament;

pmnt::SymmetricPropRule::SymmetricPropRule(KbInstance* pKB, RuleEngine* pRE) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleSymmetricProp.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfType.id()),
		RuleAtomSlot::createForRsrc(uriLib().m_owlSymmetricProp.id())));
}

void pmnt::SymmetricPropRule::applyRuleHead(BindingList &bindingList)
{
	ResourceId propRsrcId = bindingList[0].getBinding();

	auto pNewRule = ::std::make_shared<StandardRule>(m_pKB, m_pRE, getRsrcId());
	pNewRule->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(propRsrcId),
		RuleAtomSlot::createForVar(1)));
	pNewRule->headPushBack(RuleAtom(RuleAtomSlot::createForVar(1),
		RuleAtomSlot::createForRsrc(propRsrcId),
		RuleAtomSlot::createForVar(0)));
	m_pRE->addRule(pNewRule);
}
