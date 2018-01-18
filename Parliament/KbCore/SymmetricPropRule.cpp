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
	bodyPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(uriLib().m_rdfType.id()),
		RulePosition::makeRsrcPos(uriLib().m_owlSymmetricProp.id())));
}

void pmnt::SymmetricPropRule::applyRuleHead(BindingList &bindingList)
{
	ResourceId propRsrcId = bindingList[0].m_rsrcId;

	auto pNewRule = ::std::make_shared<StandardRule>(m_pKB, m_pRE, getRsrcId());
	pNewRule->bodyPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(propRsrcId),
		RulePosition::makeVariablePos(1)));
	pNewRule->headPushBack(RuleAtom(RulePosition::makeVariablePos(1),
		RulePosition::makeRsrcPos(propRsrcId),
		RulePosition::makeVariablePos(0)));
	m_pRE->addRule(pNewRule);
}
