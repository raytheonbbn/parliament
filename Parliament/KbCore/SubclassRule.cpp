// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/SubclassRule.h"
#include "parliament/KbConfig.h"
#include "parliament/UriLib.h"

#include <memory>

namespace pmnt = ::bbn::parliament;

pmnt::SubclassRule::SubclassRule(KbInstance* pKB, RuleEngine* pRE) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleSubclass.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsSubClassOf.id()),
		RuleAtomSlot::createForVar(1)));
}

void pmnt::SubclassRule::applyRuleHead(BindingList& variableBindings)
{
	ResourceId subClsRsrcId = variableBindings[0].getBinding();
	ResourceId superClsRsrcId = variableBindings[1].getBinding();

	if (subClsRsrcId != superClsRsrcId)	// avoid recursion
	{
		m_pRE->addRule(::std::make_shared<SubclassHelperRule>(m_pKB, m_pRE, subClsRsrcId, superClsRsrcId));

		if (m_pKB->config().inferRdfsClass())
		{
			m_pKB->addStmt(subClsRsrcId, uriLib().m_rdfType.id(),
				uriLib().m_rdfsClass.id(), true);
			m_pKB->addStmt(superClsRsrcId, uriLib().m_rdfType.id(),
				uriLib().m_rdfsClass.id(), true);
		}
		if (m_pKB->config().inferOwlClass())
		{
			m_pKB->addStmt(subClsRsrcId, uriLib().m_rdfType.id(),
				uriLib().m_owlClass.id(), true);
			m_pKB->addStmt(superClsRsrcId, uriLib().m_rdfType.id(),
				uriLib().m_owlClass.id(), true);
		}
		if (m_pKB->config().inferRdfsResource())
		{
			m_pKB->addStmt(subClsRsrcId, uriLib().m_rdfsSubClassOf.id(),
				uriLib().m_rdfsResource.id(), true);
			if (superClsRsrcId != uriLib().m_rdfsResource.id()
				&& superClsRsrcId != uriLib().m_owlThing.id())
			{
				m_pKB->addStmt(superClsRsrcId, uriLib().m_rdfsSubClassOf.id(),
					uriLib().m_rdfsResource.id(), true);
			}
		}
		if (m_pKB->config().inferOwlThing())
		{
			m_pKB->addStmt(subClsRsrcId, uriLib().m_rdfsSubClassOf.id(),
				uriLib().m_owlThing.id(), true);
			if (superClsRsrcId != uriLib().m_rdfsResource.id()
				&& superClsRsrcId != uriLib().m_owlThing.id())
			{
				m_pKB->addStmt(superClsRsrcId, uriLib().m_rdfsSubClassOf.id(),
					uriLib().m_owlThing.id(), true);
			}
		}

		auto pNewRule = ::std::make_shared<StandardRule>(m_pKB, m_pRE, getRsrcId());
		pNewRule->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(uriLib().m_rdfType.id()),
			RuleAtomSlot::createForRsrc(subClsRsrcId)));
		pNewRule->headPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(uriLib().m_rdfType.id()),
			RuleAtomSlot::createForRsrc(superClsRsrcId)));
		m_pRE->addRule(pNewRule);
	}
}

pmnt::SubclassHelperRule::SubclassHelperRule(KbInstance* pKB, RuleEngine* pRE,
		ResourceId subClsRsrcId, ResourceId superClsRsrcId) :
	StandardRule(pKB, pRE, pRE->uriLib().m_ruleSubclass.id()),
	m_subClsRsrcId(subClsRsrcId),
	m_superClsRsrcId(superClsRsrcId)
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsSubClassOf.id()),
		RuleAtomSlot::createForRsrc(m_subClsRsrcId)));
	headPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsSubClassOf.id()),
		RuleAtomSlot::createForRsrc(m_superClsRsrcId)));
}

void pmnt::SubclassHelperRule::applyRuleHead(BindingList& variableBindings)
{
	ResourceId clsRsrcId = variableBindings[0].getBinding();
	if (clsRsrcId != m_superClsRsrcId)
	{
		StandardRule::applyRuleHead(variableBindings);
	}
}
