// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/FuncPropRule.h"
#include "parliament/StmtIterator.h"
#include "parliament/UriLib.h"

#include <memory>

namespace pmnt = ::bbn::parliament;

pmnt::FuncPropRule::FuncPropRule(KbInstance* pKB, RuleEngine* pRE) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleFuncProp.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfType.id()),
		RuleAtomSlot::createForRsrc(uriLib().m_owlFuncProp.id())));
}

void pmnt::FuncPropRule::applyRuleHead(BindingList &bindingList)
{
	ResourceId funcPropId = bindingList[0].getBinding();
	m_pRE->addRule(::std::make_shared<FuncPropHelperRule>(m_pKB, m_pRE, funcPropId));
}

pmnt::FuncPropHelperRule::FuncPropHelperRule(KbInstance* pKB, RuleEngine* pRE,
		ResourceId funcPropId) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleFuncProp.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(funcPropId),
		RuleAtomSlot::createForVar(1)));
}

void pmnt::FuncPropHelperRule::applyRuleHead(BindingList &bindingList)
{
	ResourceId objId = bindingList[1].getBinding();
	if (!m_pKB->isRsrcLiteral(objId))
	{
		ResourceId subjId = bindingList[0].getBinding();
		ResourceId predId = getBody().back().m_predSlot.getRsrcId();

		StatementId newStmtId = m_pKB->find(subjId, predId, objId).statement().getStatementId();

		StmtIterator it = m_pKB->find(subjId, predId, k_nullRsrcId);
		StmtIterator end = m_pKB->end();
		for (; it != end; ++it)
		{
			if (!it.statement().isLiteral()
				&& (it.statement().isVirtual() || it.statement().getStatementId() != newStmtId)
				&& it.statement().getObjectId() != objId)
			{
				m_pKB->addStmt(objId, uriLib().m_owlSameAs.id(),
					it.statement().getObjectId(), true);
				m_pKB->addStmt(it.statement().getObjectId(),
					uriLib().m_owlSameAs.id(), objId, true);

				// In the presence of proper same-as inference (or backwards
				// chaining), this break statement avoids inserting unnecessary
				// same-as statements.  Since we do not have such reasoning yet,
				// we comment this out to create a fully connected same-as graph.
				// (This also makes the unit test simpler.)
				//break;		// only link to first instance
			}
		}
	}
}
