// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/DomainRule.h"
#include "parliament/StmtIterator.h"
#include "parliament/UriLib.h"

#include <memory>

namespace pmnt = ::bbn::parliament;

pmnt::DomainRule::DomainRule(KbInstance* pKB, RuleEngine* pRE) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleDomain.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsDomain.id()),
		RuleAtomSlot::createForVar(1)));
}

void pmnt::DomainRule::applyRuleHead(BindingList &variableBindings)
{
	ResourceId propId = variableBindings[0].getBinding();
	ResourceId domainId = variableBindings[1].getBinding();

	auto pNewRule = ::std::make_shared<StandardRule>(m_pKB, m_pRE, getRsrcId());
	pNewRule->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(propId),
		RuleAtomSlot::createForVar(1)));
	pNewRule->headPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfType.id()),
		RuleAtomSlot::createForRsrc(domainId)));
	m_pRE->addRule(pNewRule);
}
