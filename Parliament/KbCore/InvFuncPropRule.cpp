// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/InvFuncPropRule.h"
#include "parliament/StmtIterator.h"
#include "parliament/UriLib.h"

#include <memory>

namespace pmnt = ::bbn::parliament;

pmnt::InvFuncPropRule::InvFuncPropRule(KbInstance* pKB, RuleEngine* pRE) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleInvFuncProp.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfType.id()),
		RuleAtomSlot::createForRsrc(uriLib().m_owlInvFuncProp.id())));
}

void pmnt::InvFuncPropRule::applyRuleHead(BindingList &bindingList)
{
	ResourceId invFuncPropId = bindingList[0].getBinding();
	m_pRE->addRule(::std::make_shared<InvFuncPropHelperRule>(m_pKB, m_pRE, invFuncPropId));
}

pmnt::InvFuncPropHelperRule::InvFuncPropHelperRule(KbInstance* pKB, RuleEngine* pRE, ResourceId invFuncPropId) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleInvFuncProp.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(invFuncPropId),
		RuleAtomSlot::createForVar(1)));
}

void pmnt::InvFuncPropHelperRule::applyRuleHead(BindingList &bindingList)
{
	ResourceId subjId= bindingList[0].getBinding();
	ResourceId predId = getBody().back().m_predSlot.getRsrcId();
	ResourceId objId = bindingList[1].getBinding();

	StatementId newStmtId = m_pKB->find(subjId, predId, objId).statement().getStatementId();

	StmtIterator it = m_pKB->find(k_nullRsrcId, predId, objId);
	StmtIterator end = m_pKB->end();
	for (; it != end; ++it)
	{
		if ((it.statement().isVirtual() || it.statement().getStatementId() != newStmtId)
			&& it.statement().getSubjectId() != subjId)
		{
			m_pKB->addStmt(subjId, uriLib().m_owlSameAs.id(),
				it.statement().getSubjectId(), true);
			m_pKB->addStmt(it.statement().getSubjectId(),
				uriLib().m_owlSameAs.id(), subjId, true);

			// In the presence of proper same-as inference (or backwards
			// chaining), this break statement avoids inserting unnecessary
			// same-as statements.  Since we do not have such reasoning yet,
			// we comment this out to create a fully connected same-as graph.
			// (This also makes the unit test simpler.)
			//break;		// only link to first instance
		}
	}
}
