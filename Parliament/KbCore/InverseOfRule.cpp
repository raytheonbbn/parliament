// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/InverseOfRule.h"
#include "parliament/UriLib.h"

#include <memory>

namespace pmnt = ::bbn::parliament;

pmnt::InverseOfRule::InverseOfRule(KbInstance* pKB, RuleEngine* pRE) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleInverseProp.id())
{
	bodyPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(uriLib().m_owlInverseOf.id()),
		RulePosition::makeVariablePos(1)));
}

void pmnt::InverseOfRule::applyRuleHead(BindingList &bindingList)
{
	ResourceId p1RsrcId = bindingList[0].m_rsrcId;
	ResourceId p2RsrcId = bindingList[1].m_rsrcId;

	if (p1RsrcId == p2RsrcId)
	{
		m_pKB->addStmt(p1RsrcId, uriLib().m_rdfType.id(),
			uriLib().m_owlSymmetricProp.id(), true);
	}
	else
	{
		m_pKB->addStmt(p2RsrcId, uriLib().m_owlInverseOf.id(), p1RsrcId, true);

		auto pNewRule = ::std::make_shared<StandardRule>(m_pKB, m_pRE, getRsrcId());
		pNewRule->bodyPushBack(RuleAtom(
			RulePosition::makeVariablePos(0),
			RulePosition::makeRsrcPos(p1RsrcId),
			RulePosition::makeVariablePos(1)));
		pNewRule->headPushBack(RuleAtom(
			RulePosition::makeVariablePos(1),
			RulePosition::makeRsrcPos(p2RsrcId),
			RulePosition::makeVariablePos(0)));
		m_pRE->addRule(pNewRule);
	}
}
