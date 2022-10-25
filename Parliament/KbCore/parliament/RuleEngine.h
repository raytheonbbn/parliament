// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_RULES_H_INCLUDED)
#define PARLIAMENT_RULES_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"
#include "parliament/NewStmtHandler.h"
#include "parliament/KbInstance.h"
#include "parliament/UriLib.h"
#include "parliament/Statement.h"

#include <cmath>
#include <iosfwd>
#include <limits>
#include <memory>
#include <set>
#include <stdexcept>
#include <unordered_map>
#include <vector>

namespace bbn::parliament
{

class RuleEngine;

class RuleVariableBinding
{
public:
	RuleVariableBinding() : m_rsrcId(k_nullRsrcId) {}
	void bind(ResourceId rsrcId)
		{ m_rsrcId = rsrcId; }
	bool isBound() const
		{ return m_rsrcId != k_nullRsrcId; }
	ResourceId getBinding() const
		{ return m_rsrcId; }

private:
	ResourceId m_rsrcId;
};

using BoolList = ::std::vector<bool>;
using BindingList = ::std::vector<RuleVariableBinding>;

class RuleAtomSlot
{
public:
	static RuleAtomSlot createForVar(uint32 variableIndex)
		{
			RuleAtomSlot newSlot;
			newSlot.m_isVariable = true;
			newSlot.m_variableIndex = variableIndex;
			return newSlot;
		}
	static RuleAtomSlot createForRsrc(ResourceId rsrcId)
		{
			RuleAtomSlot newSlot;
			newSlot.m_isVariable = false;
			newSlot.m_rsrcId = rsrcId;
			return newSlot;
		}
	bool isVariable() const
		{ return m_isVariable; }
	ResourceId getRsrcId() const
		{
			if (m_isVariable)
			{
				throw ::std::logic_error("Attempt to retrieve resource ID from a variable slot");
			}
			return m_rsrcId;
		}
	uint32 getVarIndexId() const
		{
			if (!m_isVariable)
			{
				throw ::std::logic_error("Attempt to retrieve variable index from a resource slot");
			}
			return m_variableIndex;
		}
	bool checkSlotAddBinding(ResourceId rsrcId, BindingList& bindingList) const
		{
			if (!m_isVariable)
			{
				return m_rsrcId == rsrcId;
			}
			else if (bindingList[m_variableIndex].isBound())
			{
				return bindingList[m_variableIndex].getBinding() == rsrcId;
			}
			else
			{
				bindingList[m_variableIndex].bind(rsrcId);
				return true;
			}
		}
	void print(::std::ostream& s, const KbInstance* pKB) const;

private:
	RuleAtomSlot()
		: m_isVariable(false), m_rsrcId(k_nullRsrcId) {}

	bool				m_isVariable;	// Determines which union element is currently in use
	union
	{
		uint32		m_variableIndex;
		ResourceId	m_rsrcId;
	};
};

using RuleAtomSlotList = ::std::vector<RuleAtomSlot>;

struct RuleAtom
{
	RuleAtomSlot	m_subjSlot;
	RuleAtomSlot	m_predSlot;
	RuleAtomSlot	m_objSlot;

	RuleAtom(const RuleAtomSlot& subjSlot, const RuleAtomSlot& predSlot,
			const RuleAtomSlot& objSlot)
		: m_subjSlot(subjSlot), m_predSlot(predSlot), m_objSlot(objSlot) {}
	void print(::std::ostream& s, const KbInstance* pKB) const;
};

struct ArgCountLimits
{
	static constexpr size_t	k_unbounded = ::std::numeric_limits<size_t>::max();

	size_t m_min;
	size_t m_max;
};

//TODO: This is lame.  We really need a unifying approach, but this will do for now.
class SWRLBuiltinRuleAtom
{
public:
	SWRLBuiltinRuleAtom(const RsrcString& id) : m_id(id), m_slots() {}

	virtual ~SWRLBuiltinRuleAtom() {}

	//TODO: provide implementations
	virtual void print(::std::ostream& s, const KbInstance* pKB) const;

	bool evaluate(KbInstance* pKB, BindingList& bindingList) const;

	const RuleAtomSlotList& getAtomSlotList() const
		{ return m_slots; }
	void appendAtomSlot(const RuleAtomSlot& atomSlot)
		{ m_slots.push_back(atomSlot); }

protected:
	double getDoubleFromAtomSlot(const RuleAtomSlot& atomSlot,
		KbInstance* pKB, const BindingList& bindingList) const;
	RsrcString getLiteralStrFromAtomSlot(const RuleAtomSlot& atomSlot,
		KbInstance* pKB, const BindingList& bindingList) const;
	ResourceId getRsrcIdForValue(const double value, KbInstance* pKB) const;
	bool checkResult(const RuleAtomSlot& resultSlot, double resultVal, KbInstance* pKB,
		BindingList& bindingList) const;

	bool equivalent(double one, double two) const
	{
		return ::std::fabs(one - two) < ::std::numeric_limits<double>::epsilon();
	}

	static constexpr size_t	k_unboundedNumArgs = ::std::numeric_limits<size_t>::max();

private:
	double getDoubleFromLiteralStr(const RsrcChar* uri) const;
	RsrcString getLexicalFormFromLiteralStr(const RsrcChar* uri) const;

	virtual ArgCountLimits getArgCountLimits() const = 0;
	virtual bool evalImpl(KbInstance* pKB, BindingList& bindingList) const = 0;

	RsrcString			m_id;
	RuleAtomSlotList	m_slots;
};

using AtomList = ::std::vector<RuleAtom>;
using AtomIndex = AtomList::difference_type;

using SWRLBuiltinAtomPtr = ::std::unique_ptr<SWRLBuiltinRuleAtom>;
using SWRLBuiltinAtomList = ::std::vector<SWRLBuiltinAtomPtr>;

//TODO: FuncPropRule, InvFuncPropRule, TransitivePropRule helper rules could
// optionally be consolidated.  Currently when they fire they create a helper rule,
// and when it fires it creates a StandardRule.  We could change this so that a
// StandardRule is created instead of a helper rule.  However, the matching of the
// StandardRule would be less efficient than the current arrangement.

class Rule
{
public:
	Rule(KbInstance* pKB, RuleEngine* pRE, ResourceId rsrcId)
		: m_pKB(pKB), m_pRE(pRE), m_rsrcId(rsrcId), m_body() {}
	Rule(const Rule&) = delete;
	Rule& operator=(const Rule&) = delete;
	Rule(Rule&&) = delete;
	Rule& operator=(Rule&&) = delete;
	virtual ~Rule() = default;

	ResourceId getRsrcId() const
		{ return m_rsrcId; }
	const AtomList& getBody() const
		{ return m_body; }
	void bodyPushBack(const RuleAtom& newBodyAtom)
		{ m_body.push_back(newBodyAtom); }

	const SWRLBuiltinAtomList& getBodyBuiltIns() const
		{ return m_bodyBuiltIns; }
	void bodyBuiltInsPushBack(SWRLBuiltinAtomPtr newBodyBuiltinAtom)
		{ m_bodyBuiltIns.push_back(::std::move(newBodyBuiltinAtom)); }

	virtual void applyRuleHead(BindingList& variableBindings) = 0;

	virtual bool mustRunAtStartup() const
		{ return false; }
	virtual void print(::std::ostream& s) const;

protected:
	virtual void printHead(::std::ostream& s) const;
	const UriLib& uriLib() const;

	KbInstance*				m_pKB;
	RuleEngine*				m_pRE;
	ResourceId				m_rsrcId;
	AtomList					m_body;
	SWRLBuiltinAtomList	m_bodyBuiltIns;
};

class StandardRule : public Rule
{
public:
	StandardRule(KbInstance* pKB, RuleEngine* pRE, ResourceId rsrcId)
		: Rule(pKB, pRE, rsrcId), m_head() {}

	const AtomList& getHead() const
		{ return m_head; }
	void headPushBack(const RuleAtom& newHeadAtom)
		{ m_head.push_back(newHeadAtom); }

	const SWRLBuiltinAtomList& getHeadBuiltIns() const
		{ return m_headBuiltIns; }
	void headBuiltInsPushBack(SWRLBuiltinAtomPtr newHeadBuiltinAtom)
		{ m_headBuiltIns.push_back(::std::move(newHeadBuiltinAtom)); }

	void addAtom(const RuleAtom& newAtom, bool addToHead)
		{ (addToHead ? m_head : m_body).push_back(newAtom); }
	void addBuiltInAtom(SWRLBuiltinAtomPtr pNewAtom, bool addToHead)
		{ (addToHead ? m_headBuiltIns : m_bodyBuiltIns).push_back(::std::move(pNewAtom)); }

	void applyRuleHead(BindingList& variableBindings) override;

protected:
	void printHead(::std::ostream& s) const override;

private:
	AtomList					m_head;
	SWRLBuiltinAtomList	m_headBuiltIns;
};

using RuleList = ::std::vector<::std::shared_ptr<Rule>>;
using RuleIndex = RuleList::difference_type;

struct RuleTrigger
{
	ResourceId	m_rsrcId;	// this is also the RuleTriggerMap key
	RuleIndex	m_ruleIdx;	// index of a rule within RuleEngine::m_ruleList
	AtomIndex	m_atomIdx;	// index of an atom within getRule(...).m_body

	RuleTrigger(ResourceId rsrcId, RuleIndex ruleIdx, AtomIndex atomIdx)
		: m_rsrcId(rsrcId), m_ruleIdx(ruleIdx), m_atomIdx(atomIdx) {}
	RuleList::const_reference getRule(const RuleList& ruleList) const
		{ return ruleList[m_ruleIdx]; }
	const RuleAtom& getAtom(const RuleList& ruleList) const
		{ return getRule(ruleList)->getBody()[m_atomIdx]; }
	void print(::std::ostream& s, const char* pTriggerType,
		const RuleList& ruleList, const KbInstance* pKB) const;
};

class FwdChainNode
{
public:
	static constexpr AtomIndex k_noMatchedAtom = ::std::numeric_limits<AtomIndex>::min();

	FwdChainNode(::std::shared_ptr<Rule> pRule) :
		m_pRule(pRule),
		m_atomMatchList(m_pRule->getBody().size(), false),
		m_bindingList(k_maxNumVariables)
	{}

	FwdChainNode(const FwdChainNode&) = default;
	FwdChainNode& operator=(const FwdChainNode&) = default;
	FwdChainNode(FwdChainNode&&) = default;
	FwdChainNode& operator=(FwdChainNode&&) = default;
	~FwdChainNode() = default;

	const SWRLBuiltinAtomList& getBodyBuiltIns() const
		{ return m_pRule->getBodyBuiltIns(); }
	const AtomList& getBody() const
		{ return m_pRule->getBody(); }
	void applyRuleHead(BindingList& variableBindings)
		{ m_pRule->applyRuleHead(variableBindings); }
	const BoolList& getMatchList() const
		{ return m_atomMatchList; }
	BoolList& getMatchList()
		{ return m_atomMatchList; }
	const BindingList& getBindingList() const
		{ return m_bindingList; }
	BindingList& getBindingList()
		{ return m_bindingList; }

	// if return is > 0, stores the index of a matched Atom within rule.body OR
	// if returns k_noMatchedAtom, indicates that no Atom within rule.body is matched
	AtomIndex chooseMatchAtomIndex() const;

private:
	static constexpr size_t k_maxNumVariables = 10;

	::std::shared_ptr<Rule>	m_pRule;
	BoolList						m_atomMatchList;
	BindingList					m_bindingList;
};

using VarMap = ::std::unordered_map<ResourceId, uint32>;

class RuleEngine : public NewStmtHandler
{
public:
	RuleEngine(KbInstance* pKB);
	RuleEngine(const RuleEngine&) = delete;
	RuleEngine& operator=(const RuleEngine&) = delete;
	RuleEngine(RuleEngine&&) = delete;
	RuleEngine& operator=(RuleEngine&&) = delete;
	~RuleEngine() override;

	void addRule(::std::shared_ptr<Rule> pNewRule);
	void setEndOfStartupTime()
		{ m_startupTimeIsOver = true; }
	const UriLib& uriLib() const
		{ return m_pKB->uriLib(); }
	size_t ruleCount() const
		{ return m_ruleList.size(); }
	void printRules(::std::ostream& s) const;
	void printTriggers(::std::ostream& s) const;

	void onNewStmt(KbInstance* pKB, const Statement& stmt) override;

	void addNewRules();

private:
	using RuleTriggerMap = ::std::unordered_multimap<ResourceId, RuleTrigger>;
	using FwdChainNodePtr = ::std::unique_ptr<FwdChainNode>;
	using FwdChainList = ::std::vector<FwdChainNodePtr>;
	using RsrcIdSet = ::std::set<ResourceId>;

	void applyRuleToExistingStatements(RuleIndex ruleIdx);
	void setTriggers(RuleIndex ruleIdx);
	static void setTrigger(RuleTriggerMap& triggerMap, ResourceId rsrcId,
		RuleIndex ruleIdx, AtomIndex atomIdx);

	bool checkStatementAddBinding(const RuleAtom& atom, const Statement& stmt, BindingList& bindingList);
	bool checkStatementAddBinding(const SWRLBuiltinRuleAtom& atom, const Statement& stmt, BindingList& bindingList);

	void checkTriggers(const RuleTriggerMap& triggerMap, ResourceId rsrcId,
		const Statement& stmt);
	void checkBuiltinTriggers(ResourceId rsrcId, const Statement& stmt);
	void printTriggerMap(::std::ostream& s, const RuleTriggerMap& triggerMap,
		const char* pTriggerType) const;

	void expandFwdChainNode(FwdChainNode& fcNode);
	void traverseFwdChainTree(FwdChainNodePtr pRootFCNode);
	bool constructRuleHeadOrBody(StandardRule* pNewRule, ResourceId headOrBodyId,
		bool isHead, VarMap& vars);

	KbInstance*				m_pKB;
	RuleList					m_ruleList;
	RuleTriggerMap			m_subjTriggerMap;
	RuleTriggerMap			m_predTriggerMap;
	RuleTriggerMap			m_objTriggerMap;
	RuleTriggerMap			m_SWRLBuiltinTriggerMap;
	bool						m_newStmtHandlerInstalled;
	bool						m_startupTimeIsOver;
	FwdChainList			m_fwdChainList;
	RsrcIdSet				m_loadedRules;
};

inline const UriLib& Rule::uriLib() const
{
	return m_pRE->uriLib();
}

}	// namespace end

#endif // !PARLIAMENT_RULES_H_INCLUDED
