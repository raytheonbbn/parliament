// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/EquivalentPropRule.h"
#include "parliament/UriLib.h"

namespace pmnt = ::bbn::parliament;

pmnt::EquivalentPropRule::EquivalentPropRule(KbInstance* pKB, RuleEngine* pRE) :
	StandardRule(pKB, pRE, pRE->uriLib().m_ruleEquivalentProp.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_owlEquivalentProp.id()),
		RuleAtomSlot::createForVar(1)));

	headPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsSubPropertyOf.id()),
		RuleAtomSlot::createForVar(1)));
	headPushBack(RuleAtom(RuleAtomSlot::createForVar(1),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsSubPropertyOf.id()),
		RuleAtomSlot::createForVar(0)));
}
