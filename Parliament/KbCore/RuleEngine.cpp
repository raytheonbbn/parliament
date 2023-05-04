// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

//TODO: Ensure that iterators are not invalidated within their iteration loops
//TODO: Eliminate fixed-size RuleVariableBinding arrays (and possibly eliminate RuleVariableBinding entirely)
//TODO: Allow rule callbacks implemented in Java
//TODO: Finish implementing builtin atoms
//TODO: Add an option to create rules without running them (except for rule-creating rules)
//TODO: Add an option to the admin client to run all rules to ensure entailments are complete

#include "parliament/RuleEngine.h"
#include "parliament/KbConfig.h"
#include "parliament/Exceptions.h"
#include "parliament/LiteralUtils.h"
#include "parliament/Log.h"
#include "parliament/StmtIterator.h"
#include "parliament/SWRLRuleBuilder.h"
#include "parliament/UnicodeIterator.h"

#include <algorithm>
#include <boost/algorithm/string.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <iterator>
#include <memory>
#include <ostream>
#include <typeinfo>
#include <utility>

namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::boost::lexical_cast;
using ::boost::numeric_cast;
using ::std::begin;
using ::std::distance;
using ::std::end;
using ::std::endl;
using ::std::make_pair;
using ::std::make_shared;
using ::std::make_unique;
using ::std::move;
using ::std::ostream;
using ::std::shared_ptr;
using ::std::string;

static auto g_log = pmnt::log::getSource("RuleEngine");

// ==========   RuleAtomSlot implementation   ==========

void pmnt::RuleAtomSlot::print(ostream& s, const KbInstance* pKB) const
{
	if (m_isVariable)
	{
		s << "?" << m_variableIndex;
	}
	else
	{
		s << pKB->rsrcIdToUri(m_rsrcId);
	}
}

// ==========   RuleAtom implementation   ==========

void pmnt::RuleAtom::print(ostream& s, const KbInstance* pKB) const
{
	s << "  ";
	m_subjSlot.print(s, pKB);
	s << " ";
	m_predSlot.print(s, pKB);
	s << " ";
	m_objSlot.print(s, pKB);
	s << endl;
}

// ==========   SWRLBuiltinRuleAtom implementation   ==========

bool pmnt::SWRLBuiltinRuleAtom::evaluate(KbInstance* pKB, BindingList& bindingList) const
{
	const auto argCountLimits = getArgCountLimits();
	if (size(getAtomSlotList()) < argCountLimits.m_min)
	{
		PMNT_LOG(g_log, log::Level::warn) << format(
			"SWRL built-in '%1%' requires at least %2% arguments, but has only %3%")
			% convertFromRsrcChar(m_id) % argCountLimits.m_min % size(getAtomSlotList());
		return false;
	}
	else if (argCountLimits.m_max != ArgCountLimits::k_unbounded
		&& size(getAtomSlotList()) > argCountLimits.m_max)
	{
		PMNT_LOG(g_log, log::Level::warn) << format(
			"SWRL built-in '%1%' takes at most %2% arguments, but has %3%")
			% convertFromRsrcChar(m_id) % argCountLimits.m_max % size(getAtomSlotList());
		return false;
	}

	try
	{
		return evalImpl(pKB, bindingList);
	}
	catch (const ::std::exception& ex)
	{
		PMNT_LOG(g_log, log::Level::warn) << format(
			"Exception while processing SWRL built-in '%1%':  '%1%' (%2%)")
			% convertFromRsrcChar(m_id) % ex.what() % typeid(ex).name();
		return false;
	}
}

bool pmnt::SWRLBuiltinRuleAtom::checkResult(const RuleAtomSlot& resultSlot,
	double resultVal, KbInstance* pKB, BindingList& bindingList) const
{
	if (resultSlot.isVariable())
	{
		ResourceId resultId = getRsrcIdForValue(resultVal, pKB);
		return resultSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		double compareVal = getDoubleFromLiteralStr(pKB->rsrcIdToUri(resultSlot.getRsrcId()));
		return equivalent(compareVal, resultVal);
	}
}

pmnt::ResourceId pmnt::SWRLBuiltinRuleAtom::getRsrcIdForValue(const double value, KbInstance* pKB) const
{
	//TODO: Is xsd:double the correct datatype here?
	auto literal = convertToRsrcChar(str(
		format("\"%1%\"^^http://www.w3.org/2001/XMLSchema#double") % value));
	return pKB->uriToRsrcId(literal, true, true);
}

double pmnt::SWRLBuiltinRuleAtom::getDoubleFromAtomSlot(
	const RuleAtomSlot& atomSlot, KbInstance* pKB, const BindingList& bindingList) const
{
	ResourceId rsrcId;
	if (atomSlot.isVariable())
	{
		// don't work with body evaluations in which inputs are not bound previously
		if (!bindingList[atomSlot.getVarIndexId()].isBound())
		{
			throw Exception("body evaluations must have inputs bound previously");
		}

		rsrcId = bindingList[atomSlot.getVarIndexId()].getBinding();
	}
	else
	{
		rsrcId = atomSlot.getRsrcId();
	}

	const RsrcChar* firstUri = pKB->rsrcIdToUri(rsrcId);
	return getDoubleFromLiteralStr(firstUri);
}

pmnt::RsrcString pmnt::SWRLBuiltinRuleAtom::getLiteralStrFromAtomSlot(
	const RuleAtomSlot& atomSlot, KbInstance* pKB, const BindingList& bindingList) const
{
	ResourceId rsrcId;
	if (!atomSlot.isVariable())
	{
		rsrcId = atomSlot.getRsrcId();
	}
	else if (bindingList[atomSlot.getVarIndexId()].isBound())
	{
		rsrcId = bindingList[atomSlot.getVarIndexId()].getBinding();
	}
	else
	{
		// Don't work with body evaluations in which inputs are not bound previously:
		throw Exception("body evaluations must have inputs bound previously");
	}
	return getLexicalFormFromLiteralStr(pKB->rsrcIdToUri(rsrcId));
}

pmnt::RsrcString pmnt::SWRLBuiltinRuleAtom::getLexicalFormFromLiteralStr(
	const RsrcChar* pLiteral) const
{
	return ::std::get<0>(LiteralUtils::parseLiteral(pLiteral));
}

double pmnt::SWRLBuiltinRuleAtom::getDoubleFromLiteralStr(const RsrcChar* pLiteral) const
{
	try
	{
		auto [lexicalForm, datatypeUri, lang] = LiteralUtils::parseLiteral(pLiteral);
		if (datatypeUri.empty())
		{
			return LiteralUtils::convertToDouble(lexicalForm, lang.empty()
				? LiteralUtils::k_plainLiteralDatatype
				: LiteralUtils::k_langLiteralDatatype);
		}
		else
		{
			return LiteralUtils::convertToDouble(lexicalForm, datatypeUri);
		}
	}
	catch (const ::boost::bad_lexical_cast& ex)
	{
		auto fmt = format("Unable to convert SWRL built-in argument '%1%' to double:  %2%")
			% pLiteral % ex.what();
		PMNT_LOG(g_log, log::Level::error) << fmt;
		throw Exception(fmt);
	}
}

void pmnt::SWRLBuiltinRuleAtom::print(ostream& s, const KbInstance* pKB) const
{
	// TODO: Implement
}

// ==========   Rule implementation   ==========

void pmnt::Rule::print(ostream& s) const
{
	s << "rule " << m_pKB->rsrcIdToUri(getRsrcId()) << endl;
	for (const auto& atom : getBody())
	{
		atom.print(s, m_pKB);
	}
	s << " => " << endl;
	printHead(s);
}

void pmnt::Rule::printHead(ostream& s) const
{
	s << "  Rule head is custom-defined." << endl;
}

// ==========   StandardRule implementation   ==========

//TODO: for head vars, distinguish between literals and URIs (probably using info from the body vars)
void pmnt::StandardRule::applyRuleHead(BindingList& variableBindings)
{
	PMNT_LOG(g_log, log::Level::debug) << "applyRuleHead(): applying rule";
	for (const auto& atom : m_head)
	{
		ResourceId subjectId = atom.m_subjSlot.isVariable()
			? variableBindings[atom.m_subjSlot.getVarIndexId()].getBinding()
			: atom.m_subjSlot.getRsrcId();
		ResourceId predicateId = atom.m_predSlot.isVariable()
			? variableBindings[atom.m_predSlot.getVarIndexId()].getBinding()
			: atom.m_predSlot.getRsrcId();
		ResourceId objectId = atom.m_objSlot.isVariable()
			? variableBindings[atom.m_objSlot.getVarIndexId()].getBinding()
			: atom.m_objSlot.getRsrcId();

		PMNT_LOG(g_log, log::Level::debug) << format("applyRuleHead(): adding a new statement: %1% %2% %3%")
			% subjectId % predicateId % objectId;

		if (subjectId != k_nullRsrcId && predicateId != k_nullRsrcId && objectId != k_nullRsrcId)
		{
			m_pKB->addStmt(subjectId, predicateId, objectId, true);
		}
	}
}

void pmnt::StandardRule::printHead(ostream& s) const
{
	for (const auto& atom : m_head)
	{
		atom.print(s, m_pKB);
	}
}

// ==========   RuleTrigger implementation   ==========

void pmnt::RuleTrigger::print(ostream& s, const char* pTriggerType,
	const RuleList& ruleList, const KbInstance* pKB) const
{
	s << pTriggerType << " trigger:  " << pKB->rsrcIdToUri(m_rsrcId) << endl;
	getRule(ruleList)->print(s);
	s << "atom";
	getAtom(ruleList).print(s, pKB);
}

// ==========   FwdChainNodeFwdChainNode implementation   ==========

pmnt::AtomIndex pmnt::FwdChainNode::chooseMatchAtomIndex() const
{
	// For now, just process the atoms in order:
	auto firstFalse = ::std::find(cbegin(m_atomMatchList), cend(m_atomMatchList), false);
	return (firstFalse == cend(m_atomMatchList))
		? k_noMatchedAtom
		: ::std::distance(cbegin(m_atomMatchList), firstFalse);
}

// ==========   RuleEngine implementation   ==========

pmnt::RuleEngine::RuleEngine(KbInstance* pKB) :
	m_pKB(pKB),
	m_ruleList(),
	m_subjTriggerMap(),
	m_predTriggerMap(),
	m_objTriggerMap(),
	m_SWRLBuiltinTriggerMap(),
	m_newStmtHandlerInstalled(false),
	m_startupTimeIsOver(false)
{
}

pmnt::RuleEngine::~RuleEngine()
{
	m_pKB->removeNewStmtHndlr(this);
}

void pmnt::RuleEngine::onNewStmt(KbInstance* pKB, const Statement& stmt)
{
	if (pKB != m_pKB)
	{
		throw Exception("Unknown KbInstance in new statement handler");
	}
	else
	{
		checkTriggers(m_subjTriggerMap, stmt.getSubjectId(), stmt);
		checkTriggers(m_predTriggerMap, stmt.getPredicateId(), stmt);
		checkTriggers(m_objTriggerMap, stmt.getObjectId(), stmt);

		// And then check against all builtins:
		checkBuiltinTriggers(stmt.getSubjectId(), stmt);
		checkBuiltinTriggers(stmt.getPredicateId(), stmt);
		checkBuiltinTriggers(stmt.getObjectId(), stmt);
	}
}

void pmnt::RuleEngine::setTriggers(RuleIndex ruleIdx)
{
	if (ruleIdx < 0 || static_cast<size_t>(ruleIdx) >= ruleCount())
	{
		throw Exception("Illegal argument in RuleEngine::setTriggers()");
	}

	PMNT_LOG(g_log, log::Level::debug) << format("Setting triggers for %1% atoms")
		% size(m_ruleList[ruleIdx]->getBody());

	auto beginIt = cbegin(m_ruleList[ruleIdx]->getBody());
	auto endIt = cend(m_ruleList[ruleIdx]->getBody());
	for (auto atomIt = beginIt; atomIt != endIt; ++atomIt)
	{
		// TODO: is this still correct?
		AtomIndex atomIdx = distance(beginIt, atomIt);

		if (!atomIt->m_subjSlot.isVariable())
		{
			setTrigger(m_subjTriggerMap, atomIt->m_subjSlot.getRsrcId(), ruleIdx, atomIdx);
		}

		if (!atomIt->m_predSlot.isVariable()
			&& ((atomIt->m_predSlot.getRsrcId() != uriLib().m_rdfType.id()
				&& atomIt->m_predSlot.getRsrcId() != uriLib().m_rdfsSubClassOf.id())
				|| (atomIt->m_subjSlot.isVariable() && atomIt->m_objSlot.isVariable()))) // only constant
		{
			setTrigger(m_predTriggerMap, atomIt->m_predSlot.getRsrcId(), ruleIdx, atomIdx);
		}

		if (!atomIt->m_objSlot.isVariable())
		{
			setTrigger(m_objTriggerMap, atomIt->m_objSlot.getRsrcId(), ruleIdx, atomIdx);
		}
	}

	auto builtinBegin = cbegin(m_ruleList[ruleIdx]->getBodyBuiltIns());
	auto builtinEnd = cend(m_ruleList[ruleIdx]->getBodyBuiltIns());
	for (auto builtinAtomIt = builtinBegin; builtinAtomIt != builtinEnd; ++builtinAtomIt)
	{
		// TODO: is this correct ?
		auto atomIdx = distance(builtinBegin, builtinAtomIt);

		PMNT_LOG(g_log, log::Level::debug) << "setting triggers for builtin";

		for (const auto& atomSlot : (*builtinAtomIt)->getAtomSlotList())
		{
			setTrigger(m_SWRLBuiltinTriggerMap, atomSlot.getRsrcId(), ruleIdx, atomIdx);
		}
	}
}

void pmnt::RuleEngine::setTrigger(RuleTriggerMap& triggerMap,
	ResourceId rsrcId, RuleIndex ruleIdx, AtomIndex atomIdx)
{
	triggerMap.insert(make_pair(rsrcId, RuleTrigger(rsrcId, ruleIdx, atomIdx)));
}

// indicate whether statement matches the atom and bindings
bool pmnt::RuleEngine::checkStatementAddBinding(const RuleAtom& atom,
	const Statement& stmt, BindingList& bindingList)
{
	PMNT_LOG(g_log, log::Level::debug) << "RuleEngine::checkStatementAddBinding() -- standard atom check...";
	return atom.m_subjSlot.checkSlotAddBinding(stmt.getSubjectId(), bindingList)
			&& atom.m_predSlot.checkSlotAddBinding(stmt.getPredicateId(), bindingList)
			&& atom.m_objSlot.checkSlotAddBinding(stmt.getObjectId(), bindingList);
}

// TODO: eliminate?
bool pmnt::RuleEngine::checkStatementAddBinding(const SWRLBuiltinRuleAtom& atom,
	const Statement& stmt, BindingList& bindingList)
{
	PMNT_LOG(g_log, log::Level::debug) << "RuleEngine::checkStatementAddBinding() -- builtin atom check...";

	// TODO: statement?

	return atom.evaluate(m_pKB, bindingList);
}

//TODO: Should this do something?
void pmnt::RuleEngine::checkBuiltinTriggers(ResourceId rsrcId, const Statement& stmt)
{
//	pair<ConstRuleTriggerIter, ConstRuleTriggerIter> range = m_SWRLBuiltinTriggerMap.equal_range(rsrcId);
//
//	for (; range.first != range.second; ++range.first)
//	{
//		// could later queue for subsequent fire
//		const RuleTrigger& trigger = range.first->second;
//
//		auto pFCNode = make_unique<FwdChainNode>(m_ruleList[trigger.m_ruleIdx]);
//		SWRLBuiltinRuleAtom* atom = pFCNode->getBodyBuiltIns()[trigger.m_atomIdx];
//
//		if (checkStatementAddBinding(*atom, stmt, pFCNode->getBindingList()))
//		{
//			PMNT_LOG(g_log, log::Level::debug) << "checkStatementAddBinding successful";
//			pFCNode->getMatchList()[trigger.m_atomIdx] = true;
//			traverseFwdChainTree(move(pFCNode));
//		}
//		else
//		{
//			PMNT_LOG(g_log, log::Level::debug) << "checkStatementAddBinding NOT successful";
//		}
//	}
}

void pmnt::RuleEngine::checkTriggers(const RuleTriggerMap& triggerMap,
	ResourceId rsrcId, const Statement& stmt)
{
	using TriggerList = ::std::vector<RuleTrigger>;

	auto [rangeBegin, rangeEnd] = triggerMap.equal_range(rsrcId);

	// Make a copy, because the loop body below can change the content of triggerMap:
	TriggerList matchedTriggers;
	for (auto it = rangeBegin; it != rangeEnd; ++it)
	{
		matchedTriggers.push_back(it->second);
	}

	for (const auto& trigger : matchedTriggers)
	{
		auto pFCNode = make_unique<FwdChainNode>(m_ruleList[trigger.m_ruleIdx]);
		const RuleAtom& atom = m_ruleList[trigger.m_ruleIdx]->getBody()[trigger.m_atomIdx];
		if (checkStatementAddBinding(atom, stmt, pFCNode->getBindingList()))
		{
			PMNT_LOG(g_log, log::Level::debug) << "checkStatementAddBinding successful";
			pFCNode->getMatchList()[trigger.m_atomIdx] = true;
			traverseFwdChainTree(move(pFCNode));
		}
		else
		{
			PMNT_LOG(g_log, log::Level::debug) << "checkStatementAddBinding NOT successful";
		}
	}
}

void pmnt::RuleEngine::addRule(shared_ptr<Rule> pNewRule)
{
	if (!m_newStmtHandlerInstalled)
	{
		m_pKB->addNewStmtHndlr(this);
		m_newStmtHandlerInstalled = true;
	}

	RuleIndex ruleIdx = ruleCount();
	m_ruleList.push_back(pNewRule);
	setTriggers(ruleIdx);

	if (m_startupTimeIsOver || pNewRule->mustRunAtStartup() || m_pKB->config().runAllRulesAtStartup())
	{
		//create fcNode having no bindings and no matched atoms
		auto fcNode = make_unique<FwdChainNode>(pNewRule);
		traverseFwdChainTree(move(fcNode));
	}
}

void pmnt::RuleEngine::printRules(ostream& s) const
{
	for (const auto& pRule : m_ruleList)
	{
		pRule->print(s);
	}
}

void pmnt::RuleEngine::printTriggers(ostream& s) const
{
	printTriggerMap(s, m_subjTriggerMap, "Subject");
	printTriggerMap(s, m_predTriggerMap, "Predicate");
	printTriggerMap(s, m_objTriggerMap, "Object");

	// TODO: expand for builtins?
}

void pmnt::RuleEngine::printTriggerMap(ostream& s,
	const RuleTriggerMap& triggerMap, const char* pTriggerType) const
{
	for (const auto& entry : triggerMap)
	{
		entry.second.print(s, pTriggerType, m_ruleList, m_pKB);
	}
}

void pmnt::RuleEngine::expandFwdChainNode(FwdChainNode& fcNode)
{
	PMNT_LOG(g_log, log::Level::debug) << "expandFwdChainNode";

	//next line is the query optimization code
	const AtomIndex nextAtomIdx = fcNode.chooseMatchAtomIndex();
	if (nextAtomIdx == FwdChainNode::k_noMatchedAtom)
	{
		// look at builtins, then apply head
		PMNT_LOG(g_log, log::Level::debug) << "expandFwdChainNode -- no matched atom yet, checking builtins now...";

		for (const auto& bodyBuiltin : fcNode.getBodyBuiltIns())
		{
			if (!bodyBuiltin->evaluate(m_pKB, fcNode.getBindingList()))
			{
				//TODO: correct?
				return;
			}
		}

		PMNT_LOG(g_log, log::Level::debug) << "expandFwdChainNode -- applying rule head";
		fcNode.applyRuleHead(fcNode.getBindingList());
		return;
	}

	const RuleAtom& atom = fcNode.getBody()[nextAtomIdx];

	//TODO: need to use the bindings here
	const ResourceId subjectId = atom.m_subjSlot.isVariable()
		? k_nullRsrcId
		: atom.m_subjSlot.getRsrcId();
	const ResourceId predicateId = atom.m_predSlot.isVariable()
		? k_nullRsrcId
		: atom.m_predSlot.getRsrcId();
	const ResourceId objectId = atom.m_objSlot.isVariable()
		? k_nullRsrcId
		: atom.m_objSlot.getRsrcId();

	PMNT_LOG(g_log, log::Level::debug) << format("expandFwdChainNode -- looking for: %1% %2% %3%")
		% subjectId % predicateId % objectId;

	for (StmtIterator iter = m_pKB->find(subjectId, predicateId, objectId); iter != m_pKB->end(); ++iter)
	{
		PMNT_LOG(g_log, log::Level::debug) << "expandFwdChainNode -- inside iterator loop";

		auto pFCNode = make_unique<FwdChainNode>(fcNode);
		pFCNode->getMatchList()[nextAtomIdx] = true;
		if (checkStatementAddBinding(atom, iter.statement(), pFCNode->getBindingList()))
		{
			m_fwdChainList.push_back(move(pFCNode));
		}
	}

	PMNT_LOG(g_log, log::Level::debug) << "expandFwdChainNode -- method end";
}

void pmnt::RuleEngine::traverseFwdChainTree(FwdChainNodePtr pRootFCNode)
{
	//setup fcNodeList
	m_fwdChainList.push_back(move(pRootFCNode));

	PMNT_LOG(g_log, log::Level::debug) << "traverseFwdChainNode";

	//main fcNode loop
	while (!m_fwdChainList.empty()) {
		auto pFCNode = move(m_fwdChainList.back());
		m_fwdChainList.pop_back();
		expandFwdChainNode(*pFCNode);
	}
}

//returns true iff rule head or body successfully constructed
//TODO: ensure that SWRL built-ins, which have unbound vars, can be read by this method
bool pmnt::RuleEngine::constructRuleHeadOrBody(StandardRule* pNewRule,
	ResourceId headOrBodyId, bool isHead, VarMap& varMap)
{
	ResourceId listId = headOrBodyId;
	bool listCompleted = false;
	unsigned int atomNumber = 0;

	SWRLRuleBuilder builder(m_pKB);

	while (!listCompleted) {
		//get list first, last
		ResourceId firstId = m_pKB->findAndGetObjectId(listId, uriLib().m_rdfListFirst.id());
		ResourceId restId = m_pKB->findAndGetObjectId(listId, uriLib().m_rdfListRest.id());
		if (firstId == k_nullRsrcId || restId == k_nullRsrcId)
		{
			PMNT_LOG(g_log, log::Level::error) << format("Invalid RDF list -- need both first and "
				"rest arguments:  Unprocessed rule id %1% contains badly formatted atom number %2%")
				% m_pKB->rsrcIdToUri(pNewRule->getRsrcId()) % atomNumber;
			return false;
		}

		try
		{
			builder.addAtomToRule(firstId, pNewRule, varMap, isHead);
		}
		catch (const Exception& ex)
		{
			PMNT_LOG(g_log, log::Level::error) << format(
				"Error:  Unprocessed rule id %1% contains badly formatted atom number %2%:  %3%")
				% m_pKB->rsrcIdToUri(pNewRule->getRsrcId()) % atomNumber % ex.what();
			return false;
		}

		//iterate
		listId = restId;
		if (listId == uriLib().m_rdfListNil.id())
		{
			listCompleted = true;
		}
		++atomNumber;
	}
	return true;
}

void pmnt::RuleEngine::addNewRules()
{
	StmtIterator iter = m_pKB->find(k_nullRsrcId, uriLib().m_rdfType.id(), uriLib().m_swrlImp.id());
	StmtIterator end = m_pKB->end();
	for (; iter != end; ++iter)
	{
		PMNT_LOG(g_log, log::Level::debug) << "RuleEngine::addNewRules() -- found a rule";

		ResourceId ruleId = m_pKB->subject(iter.statement().getStatementId());
		//ensure we have not processed this rule before
		if (m_loadedRules.count(ruleId) > 0)
		{
			continue;
		}
		auto pRule = make_shared<StandardRule>(m_pKB, this, ruleId);
		//get rule variables and set up a map
		VarMap varMap;
		//get rule head ID
		ResourceId headId = m_pKB->findAndGetObjectId(ruleId, uriLib().m_swrlHead.id());
		if (headId == k_nullRsrcId)
		{
			PMNT_LOG(g_log, log::Level::error) << format("Error:  Unprocessed rule id %1% contains no rule head")
				% m_pKB->rsrcIdToUri(ruleId);
			continue;
		}

		//get rule body ID
		ResourceId bodyId = m_pKB->findAndGetObjectId(ruleId, uriLib().m_swrlBody.id());
		if (bodyId == k_nullRsrcId)
		{
			PMNT_LOG(g_log, log::Level::error) << format("Error:  Unprocessed rule id %1% contains no rule body")
				% m_pKB->rsrcIdToUri(ruleId);
			continue;
		}

		// NOTE: must do body before head

		//construct rule body
		if (!constructRuleHeadOrBody(pRule.get(), bodyId, false, varMap))
		{
			PMNT_LOG(g_log, log::Level::debug) << "RuleEngine::addNewRules() -- could not construct body";
			continue;
		}

		//construct rule head
		if (!constructRuleHeadOrBody(pRule.get(), headId, true, varMap))
		{
			PMNT_LOG(g_log, log::Level::debug) << "RuleEngine::addNewRules() -- could not construct head";
			continue;
		}

		addRule(pRule);
		m_loadedRules.insert(ruleId);

		PMNT_LOG(g_log, log::Level::debug) << "RuleEngine::addNewRules() -- rule added!";
	}
}
