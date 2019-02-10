// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/RangeRule.h"
#include "parliament/LiteralUtils.h"

namespace pmnt = ::bbn::parliament;

pmnt::RangeRule::RangeRule(KbInstance* pKB, RuleEngine* pRE) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleRange.id())
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(uriLib().m_rdfsRange.id()),
		RuleAtomSlot::createForVar(1)));
}

void pmnt::RangeRule::applyRuleHead(BindingList &bindingList)
{
	ResourceId propId = bindingList[0].getBinding();
	ResourceId rangeId = bindingList[1].getBinding();
	RsrcString range = m_pKB->rsrcIdToUri(rangeId);

	// Here we make a good-faith effort to weed out range declarations for
	// datatype properties.  Since this rule may fire before the whole
	// ontology is loaded, we may fail to catch some, but they won't
	// actually cause a problem because RangeHelperRule tests for literals.
	bool isKnownLiteralType = LiteralUtils::isKnownRdfDatatype(range);
	bool isDatatypeProp = (m_pKB->end()
		!= m_pKB->find(propId, uriLib().m_rdfType.id(), uriLib().m_owlDatatypeProp.id()));
	bool isDatatype = (m_pKB->end()
		!= m_pKB->find(rangeId, uriLib().m_rdfType.id(), uriLib().m_rdfsDatatype.id()));
	if (!isKnownLiteralType && !isDatatypeProp && !isDatatype)
	{
		m_pRE->addRule(::std::make_shared<RangeHelperRule>(m_pKB, m_pRE, propId, rangeId));
	}
}

pmnt::RangeHelperRule::RangeHelperRule(KbInstance* pKB, RuleEngine* pRE,
		ResourceId propId, ResourceId rangeId) :
	Rule(pKB, pRE, pRE->uriLib().m_ruleRange.id()),
	m_propId(propId),
	m_rangeId(rangeId)
{
	bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
		RuleAtomSlot::createForRsrc(m_propId),
		RuleAtomSlot::createForVar(1)));
}

void pmnt::RangeHelperRule::applyRuleHead(BindingList &variableBindings)
{
	ResourceId objId = variableBindings[1].getBinding();
	if (!m_pKB->isRsrcLiteral(objId))
	{
		ResourceId rdfTypeId = uriLib().m_rdfType.id();
		m_pKB->addStmt(objId, rdfTypeId, m_rangeId, true);
	}
}
