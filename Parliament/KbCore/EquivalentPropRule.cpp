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
	bodyPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(uriLib().m_owlEquivalentProp.id()),
		RulePosition::makeVariablePos(1)));

	headPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(uriLib().m_rdfsSubPropertyOf.id()),
		RulePosition::makeVariablePos(1)));
	headPushBack(RuleAtom(RulePosition::makeVariablePos(1),
		RulePosition::makeRsrcPos(uriLib().m_rdfsSubPropertyOf.id()),
		RulePosition::makeVariablePos(0)));
}
