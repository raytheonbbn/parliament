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
	bodyPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(uriLib().m_owlEquivalentClass.id()),
		RulePosition::makeVariablePos(1)));

	headPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(uriLib().m_rdfsSubClassOf.id()),
		RulePosition::makeVariablePos(1)));
	headPushBack(RuleAtom(RulePosition::makeVariablePos(1),
		RulePosition::makeRsrcPos(uriLib().m_rdfsSubClassOf.id()),
		RulePosition::makeVariablePos(0)));
}
