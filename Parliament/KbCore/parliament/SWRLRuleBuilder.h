// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_SWRLATOMBUILDER_H_INCLUDED)
#define PARLIAMENT_SWRLATOMBUILDER_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/RuleEngine.h"

#include <memory>
#include <vector>

PARLIAMENT_NAMESPACE_BEGIN

class KbInstance;
class UriLib;

class SWRLRuleBuilder
{
public:
	SWRLRuleBuilder(const KbInstance* pKB) : m_pKB(pKB) {}
	SWRLRuleBuilder(const SWRLRuleBuilder&) = delete;
	SWRLRuleBuilder& operator=(const SWRLRuleBuilder&) = delete;
	SWRLRuleBuilder(SWRLRuleBuilder&&) = delete;
	SWRLRuleBuilder& operator=(SWRLRuleBuilder&&) = delete;
	~SWRLRuleBuilder() = default;

	void addAtomToRule(ResourceId atomRsrcId, StandardRule* pRule, VarMap& varMap, bool isRuleHead);

private:
	SWRLBuiltinAtomPtr buildBuiltinAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead);
	RuleAtom buildClassAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead);
	RuleAtom buildDataRangeAtom(ResourceId atomRsrcId, const VarMap& varMap, bool isRuleHead);
	RuleAtom buildDatavaluedPropertyAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead);
	RuleAtom buildDifferentIndividualsAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead);
	RuleAtom buildIndividualPropertyAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead);
	RuleAtom buildSameIndividualAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead);

	bool checkAndIndexAtomArg(bool isRuleHead, ResourceId argumentId, VarMap& varMap);
	void checkAndIndexArgList(SWRLBuiltinRuleAtom* pBuiltinAtom, bool isRuleHead, ResourceId argumentId, VarMap& varMap);

	RsrcString safeRsrcIdToUri(ResourceId rsrcId) const;
	const UriLib& uriLib() const;

	const KbInstance*	m_pKB;
};

PARLIAMENT_NAMESPACE_END

#endif
