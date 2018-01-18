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
	bodyPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(uriLib().m_rdfsDomain.id()),
		RulePosition::makeVariablePos(1)));
}

void pmnt::DomainRule::applyRuleHead(BindingList &variableBindings)
{
	ResourceId propId = variableBindings[0].m_rsrcId;
	ResourceId domainId = variableBindings[1].m_rsrcId;

	auto pNewRule = ::std::make_shared<StandardRule>(m_pKB, m_pRE, getRsrcId());
	pNewRule->bodyPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(propId),
		RulePosition::makeVariablePos(1)));
	pNewRule->headPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(uriLib().m_rdfType.id()),
		RulePosition::makeRsrcPos(domainId)));
	m_pRE->addRule(pNewRule);
}
