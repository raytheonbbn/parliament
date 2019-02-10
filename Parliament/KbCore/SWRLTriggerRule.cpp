// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/SWRLTriggerRule.h"
#include "parliament/UriLib.h"

namespace pmnt = ::bbn::parliament;

pmnt::SWRLTriggerRule::SWRLTriggerRule(KbInstance* pKB, RuleEngine* pRE) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleSWRLTrigger.id())
{
	// We trigger off statements declaring an entity as a SWRL implication.
	// We assume here that an entire SWRL rule will be inserted in a single
	// operation, and we further rely on the single-writer locking to ensure
	// that no one is reading in the midst of this insert.
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfType.id()),
		RuleAtomSlot::createForRsrc(uriLib().m_swrlImp.id())));
}

void pmnt::SWRLTriggerRule::applyRuleHead(BindingList &variableBindings)
{
	m_pKB->setRunAddNewRulesFlag();
}
