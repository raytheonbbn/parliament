// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/SubpropRule.h"
#include "parliament/UriLib.h"

#include <memory>

namespace pmnt = ::bbn::parliament;

pmnt::SubpropRule::SubpropRule(KbInstance* pKB, RuleEngine* pRE) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleSubproperty.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsSubPropertyOf.id()),
		RuleAtomSlot::createForVar(1)));

	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlAnnotationProperty.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlAsymmetricProperty.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlDatatypeProp.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlDeprecatedProperty.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlFuncProp.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlInvFuncProp.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlIrreflexiveProperty.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlObjectProp.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlReflexiveProperty.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlSymmetricProp.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_owlTransitiveProp.id());
	installSubPropOfSelfRule(pKB, pRE, uriLib().m_rdfProperty.id());
}

void pmnt::SubpropRule::installSubPropOfSelfRule(KbInstance* pKB, RuleEngine* pRE, ResourceId clsRsrcId)
{
	auto pRule = ::std::make_shared<StandardRule>(pKB, pRE, pRE->uriLib().m_ruleSelfSubprop.id());
	pRule->bodyPushBack(RuleAtom(
		RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(pRE->uriLib().m_rdfType.id()),
		RuleAtomSlot::createForRsrc(clsRsrcId)));
	pRule->headPushBack(RuleAtom(
		RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(pRE->uriLib().m_rdfsSubPropertyOf.id()),
		RuleAtomSlot::createForVar(0)));
	pRE->addRule(pRule);
}

void pmnt::SubpropRule::applyRuleHead(BindingList &variableBindings)
{
	ResourceId subPropRsrcId = variableBindings[0].getBinding();
	ResourceId superPropRsrcId = variableBindings[1].getBinding();

	if (subPropRsrcId != superPropRsrcId)	// avoid recursion
	{
		m_pRE->addRule(::std::make_shared<SubpropHelperRule>(m_pKB, m_pRE, subPropRsrcId, superPropRsrcId));

		auto pNewRule = ::std::make_shared<StandardRule>(m_pKB, m_pRE, getRsrcId());
		pNewRule->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(subPropRsrcId),
			RuleAtomSlot::createForVar(1)));
		pNewRule->headPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(superPropRsrcId),
			RuleAtomSlot::createForVar(1)));
		m_pRE->addRule(pNewRule);
	}
}

pmnt::SubpropHelperRule::SubpropHelperRule(KbInstance* pKB, RuleEngine* pRE,
		ResourceId subPropRsrcId, ResourceId superPropRsrcId) :
	StandardRule(pKB, pRE, pRE->uriLib().m_ruleSubproperty.id()),
	m_subPropRsrcId(subPropRsrcId),
	m_superPropRsrcId(superPropRsrcId)
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsSubPropertyOf.id()),
		RuleAtomSlot::createForRsrc(m_subPropRsrcId)));
	headPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsSubPropertyOf.id()),
		RuleAtomSlot::createForRsrc(m_superPropRsrcId)));
}

void pmnt::SubpropHelperRule::applyRuleHead(BindingList &variableBindings)
{
	ResourceId propRsrcId = variableBindings[0].getBinding();
	if (propRsrcId != m_superPropRsrcId)
	{
		StandardRule::applyRuleHead(variableBindings);
	}
}
