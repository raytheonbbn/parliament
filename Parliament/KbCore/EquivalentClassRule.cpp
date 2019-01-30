// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/EquivalentClassRule.h"
#include "parliament/UriLib.h"

namespace pmnt = ::bbn::parliament;

pmnt::EquivalentClassRule::EquivalentClassRule(KbInstance* pKB, RuleEngine* pRE) :
	StandardRule(pKB, pRE, pRE->uriLib().m_ruleEquivalentClass.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_owlEquivalentClass.id()),
		RuleAtomSlot::createForVar(1)));

	headPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsSubClassOf.id()),
		RuleAtomSlot::createForVar(1)));
	headPushBack(RuleAtom(RuleAtomSlot::createForVar(1),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsSubClassOf.id()),
		RuleAtomSlot::createForVar(0)));
}
