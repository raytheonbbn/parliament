// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/TransitivePropRule.h"
#include "parliament/StmtIterator.h"
#include "parliament/UriLib.h"

#include <memory>

namespace pmnt = ::bbn::parliament;

pmnt::TransitivePropRule::TransitivePropRule(KbInstance* pKB, RuleEngine* pRE) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleTransitiveProp.id())
{
	bodyPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(uriLib().m_rdfType.id()),
		RulePosition::makeRsrcPos(uriLib().m_owlTransitiveProp.id())));
}

void pmnt::TransitivePropRule::applyRuleHead(BindingList &bindingList)
{
	ResourceId transPropId = bindingList[0].m_rsrcId;
	m_pRE->addRule(::std::make_shared<TransitivePropHelperRule>(m_pKB, m_pRE, transPropId));
}

pmnt::TransitivePropHelperRule::TransitivePropHelperRule(KbInstance* pKB, RuleEngine* pRE, ResourceId transPropId) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleTransitiveProp.id())
{
	bodyPushBack(RuleAtom(RulePosition::makeVariablePos(0),
		RulePosition::makeRsrcPos(transPropId),
		RulePosition::makeVariablePos(1)));
}

void pmnt::TransitivePropHelperRule::applyRuleHead(BindingList &bindingList)
{
	ResourceId subjectId = bindingList[0].m_rsrcId;
	ResourceId objectId = bindingList[1].m_rsrcId;

	if (subjectId != objectId) // avoid recursion
	{
		ResourceId transPropId = getBody().back().m_predPos.m_rsrcId;

		StmtIterator end = m_pKB->end();
		for (StmtIterator iter = m_pKB->find(k_nullRsrcId, transPropId, subjectId);
			iter != end; ++iter)
		{
			if (iter.statement().getSubjectId() != subjectId)
			{
				m_pKB->addStmt(iter.statement().getSubjectId(), transPropId, objectId, true);
			}
		}
		for (StmtIterator iter = m_pKB->find(objectId, transPropId, k_nullRsrcId);
			iter != end; ++iter)
		{
			if (iter.statement().getObjectId() != objectId)
			{
				m_pKB->addStmt(subjectId, transPropId, iter.statement().getObjectId(), true);
			}
		}
	}
}
