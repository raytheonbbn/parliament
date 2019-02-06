// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

//TODO: [Java] Fix Parliament SAIL so that KbUri and friends only translate the rsrcId to a string if necessary (problem: KbValue needs the string to handle a change in KbInstance references)
//TODO: Add capability to dump KB as N-Triples and to load a new KB from an N-Triple file
//TODO: Minimize includes throughout C++ code
//TODO: Allow new statement handlers written in Java
//TODO: Add code to arrange owl:sameAs (and similar) sub-graphs so as to have a star topology so that query processing is minimized.
//TODO: char --> TChar, so that Windows version uses wide character API
//TODO: Add iterators over the resources (e.g., all resources that are used as subjects, or all resources matching a pattern)
//TODO: Re-think statement deletion semantics
//TODO: Re-order write operations so as to minimize the bad consequences of process termination (i.e., file corruption)
//TODO: Add support for XSD types on literals
//TODO: Support ACID transactions
//TODO: Implement a DB_ENV->memp_trickle background thread
//TODO: Experiment to see if file fragmentation is causing bad BDB performance
//TODO: Store resource strings in less space

#include "parliament/KbInstanceImpl.h"
#include "parliament/Exceptions.h"
#include "parliament/FileHandle.h"
#include "parliament/LiteralUtils.h"
#include "parliament/Statement.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"

#include "parliament/DomainRule.h"
#include "parliament/EquivalentClassRule.h"
#include "parliament/EquivalentPropRule.h"
#include "parliament/FuncPropRule.h"
#include "parliament/InverseOfRule.h"
#include "parliament/InvFuncPropRule.h"
#include "parliament/RangeRule.h"
#include "parliament/SubclassRule.h"
#include "parliament/SubpropRule.h"
#include "parliament/SWRLTriggerRule.h"
#include "parliament/SymmetricPropRule.h"
#include "parliament/TransitivePropRule.h"

#include <boost/filesystem/operations.hpp>
#include <boost/format.hpp>

#include <iomanip>
#include <ostream>
#include <sstream>
#include <utility>
#include <vector>

// Uncomment for debugging only:
//#include <iostream>

namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::char_traits;
using ::std::dec;
using ::std::endl;
using ::std::hex;
using ::std::istream;
using ::std::make_pair;
using ::std::make_shared;
using ::std::nothrow_t;
using ::std::ostream;
using ::std::ostringstream;
using ::std::pair;
using ::std::remove;
using ::std::setfill;
using ::std::setw;
using ::std::string;
using ::std::tie;

pmnt::KbInstance::KbInstance(const Config& config) :
	m_pi(makeUnique<Impl>(config, this))
{
	PMNT_LOG(m_pi->m_log, LogLevel::info) << "Initializing KbInstance";

	if (!m_pi->m_config.readOnly())
	{
		// Note:  Normally, we need to be careful not to add the same rule twice.
		// However, since this ctor is the only place we add the rules, it is
		// safe to ignore this check.
		if (m_pi->m_config.isSubclassRuleOn())
		{
			m_pi->m_re.addRule(make_shared<SubclassRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.isSubpropertyRuleOn())
		{
			m_pi->m_re.addRule(make_shared<SubpropRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.isDomainRuleOn())
		{
			m_pi->m_re.addRule(make_shared<DomainRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.isRangeRuleOn())
		{
			m_pi->m_re.addRule(make_shared<RangeRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.isEquivalentClassRuleOn())
		{
			m_pi->m_re.addRule(make_shared<EquivalentClassRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.isEquivalentPropRuleOn())
		{
			m_pi->m_re.addRule(make_shared<EquivalentPropRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.isInverseOfRuleOn())
		{
			m_pi->m_re.addRule(make_shared<InverseOfRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.isSymmetricPropRuleOn())
		{
			m_pi->m_re.addRule(make_shared<SymmetricPropRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.isFunctionalPropRuleOn())
		{
			m_pi->m_re.addRule(make_shared<FuncPropRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.isInvFunctionalPropRuleOn())
		{
			m_pi->m_re.addRule(make_shared<InvFuncPropRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.isTransitivePropRuleOn())
		{
			m_pi->m_re.addRule(make_shared<TransitivePropRule>(this, &(m_pi->m_re)));
		}
		if (m_pi->m_config.enableSWRLRuleEngine())
		{
			m_pi->m_re.addRule(make_shared<SWRLTriggerRule>(this, &(m_pi->m_re)));
		}

		m_pi->m_re.setEndOfStartupTime();	// Do not add any rules after this point

		PMNT_LOG(m_pi->m_log, LogLevel::info) << "Finished initializing rule engine";
	}
}

pmnt::KbDisposition pmnt::KbInstance::determineDisposition(
	const Config& config, bool throwIfIndeterminate)
{
	bool uriTblExists		= is_regular_file(config.uriTableFilePath());
	bool uriToIntExists	= is_regular_file(config.uriToIntFilePath());
	bool stmtTblExists	= is_regular_file(config.stmtFilePath());
	bool rsrcTblExists	= is_regular_file(config.rsrcFilePath());

	if (!uriTblExists && !uriToIntExists && !stmtTblExists && !rsrcTblExists)
	{
		return KbDisposition::k_kbDoesNotExist;
	}
	else if (uriTblExists && uriToIntExists && stmtTblExists && rsrcTblExists)
	{
		return KbDisposition::k_kbExists;
	}
	else if (uriTblExists && !uriToIntExists && stmtTblExists && rsrcTblExists)
	{
		return KbDisposition::k_kbExistsWithoutUriToInt;
	}
	else if (throwIfIndeterminate)
	{
		throw Exception("KB is in an indeterminate state:  Some files exist, while others are missing.");
	}
	else
	{
		return KbDisposition::k_indeterminateKbState;
	}
}

pmnt::KbInstance::~KbInstance()
{
	m_pi->m_stmtHndlrList.clear();	// Just in case...
	PMNT_LOG(m_pi->m_log, LogLevel::info) << "KbInstance destructed";
}

const pmnt::Config& pmnt::KbInstance::config() const
{
	return m_pi->m_config;
}

void pmnt::KbInstance::sync()
{
	if (!m_pi->m_config.readOnly())
	{
		m_pi->m_uriTbl.sync();
		m_pi->m_uriToRsrcId.sync();
		m_pi->m_stmtTbl.sync();
		m_pi->m_rsrcTbl.sync();
	}
}

void pmnt::KbInstance::getExcessCapacity(/* out */ double& pctUnusedUriCapacity,
	/* out */ double& pctUnusedRsrcCapacity, /* out */ double& pctUnusedStmtCapacity) const
{
	pctUnusedUriCapacity = 0.0;
	pctUnusedRsrcCapacity = 0.0;
	pctUnusedStmtCapacity = 0.0;

	pctUnusedUriCapacity = computeExcessCapacity(m_pi->m_uriTbl.capacity(), m_pi->m_uriTbl.size());
	pctUnusedRsrcCapacity = computeExcessCapacity(m_pi->m_rsrcTbl.capacity(), m_pi->m_rsrcTbl.recordCount());
	pctUnusedStmtCapacity = computeExcessCapacity(m_pi->m_stmtTbl.capacity(), m_pi->m_stmtTbl.recordCount());
}

double pmnt::KbInstance::computeExcessCapacity(size_t capacity, size_t recCount)
{
	return (capacity == 0)
		? 0
		: static_cast<double>(capacity - recCount) / static_cast<double>(capacity);
}

void pmnt::KbInstance::releaseExcessCapacity()
{
	m_pi->m_uriTbl.releaseExcessCapacity();
	m_pi->m_uriToRsrcId.compact();
	m_pi->m_stmtTbl.releaseExcessCapacity();
	m_pi->m_rsrcTbl.releaseExcessCapacity();
}

pmnt::ResourceId pmnt::KbInstance::uriToRsrcId(
	const RsrcChar* pUri, size_t uriLen, bool isLiteral, bool createIfMissing)
{
	if (pUri == nullptr)
	{
		throw Exception("Error:  Null URI passed to KbInstance::uriToRsrcId()");
	}

	RsrcString normalizedLiteral;
	if (isLiteral && m_pi->m_config.normalizeTypedStringLiterals())
	{
		PMNT_LOG(m_pi->m_log, LogLevel::debug) << format{"Normalizing '%1%'"}
			% convertFromRsrcChar(RsrcString(pUri, pUri + uriLen));
		RsrcString lexicalForm;
		RsrcString datatypeUri;
		RsrcString langTag;
		::std::tie(lexicalForm, datatypeUri, langTag) = LiteralUtils::parseLiteral(pUri, pUri + uriLen);
		PMNT_LOG(m_pi->m_log, LogLevel::debug) << format{"     parse: lex form = '%1%', dtype = '%2%', lang = '%3%'"}
			% convertFromRsrcChar(lexicalForm) % convertFromRsrcChar(datatypeUri) % convertFromRsrcChar(langTag);
		if (datatypeUri == LiteralUtils::k_plainLiteralDatatype)
		{
			normalizedLiteral = LiteralUtils::composePlainLiteral(lexicalForm);
			pUri = normalizedLiteral.c_str();
			uriLen = normalizedLiteral.length();
			PMNT_LOG(m_pi->m_log, LogLevel::debug) << format{"         as '%1%'"}
				% convertFromRsrcChar(normalizedLiteral);
		}
		else if (!langTag.empty())
		{
			normalizedLiteral = LiteralUtils::composeLangLiteral(lexicalForm, langTag);
			pUri = normalizedLiteral.c_str();
			uriLen = normalizedLiteral.length();
			PMNT_LOG(m_pi->m_log, LogLevel::debug) << format{"         as '%1%'"}
				% convertFromRsrcChar(normalizedLiteral);
		}
	}

	ResourceId result = m_pi->m_uriToRsrcId.find(pUri, uriLen);
	if (createIfMissing && result == k_nullRsrcId)
	{
		ensureNotReadOnly("KbInstance::uriToRsrcId");

		const ResourceId rsrcId = (ResourceId) m_pi->m_rsrcTbl.recordCount();

		// Initialize a KbRsrc
		KbRsrc rsrc;
		rsrc.init();
		rsrc.setFlag(ResourceFlags::k_rsrcFlagLiteral, isLiteral);

		// Insert URI in m_pi->m_uriTbl and link it to the KbRsrc
		rsrc.m_uriOffset = m_pi->m_uriTbl.pushBack(pUri, uriLen);

		// Insert the KbRsrc in the file
		m_pi->m_rsrcTbl.pushBack(rsrc);

		// Map the uri to the resource id
		m_pi->m_uriToRsrcId.insert(pUri, uriLen, rsrcId);

		result = rsrcId;
	}
	return result;
}

pmnt::ResourceId pmnt::KbInstance::uriToRsrcId(
	const RsrcChar* pUri, bool isLiteral, bool createIfMissing)
{
	if (pUri == nullptr)
	{
		throw Exception("Error:  Null URI passed to KbInstance::uriToRsrcId()");
	}
	return uriToRsrcId(pUri, char_traits<RsrcChar>::length(pUri), isLiteral, createIfMissing);
}

const pmnt::RsrcChar* pmnt::KbInstance::rsrcIdToUri(ResourceId rsrcId) const
{
	const RsrcChar* pResult = nullptr;

	KbRsrc& rsrc = m_pi->m_rsrcTbl.getRecordAt(rsrcId);

	if (!rsrc.isAnonymous() && !rsrc.testFlag(ResourceFlags::k_rsrcFlagStatementTag))
	{
		pResult = m_pi->m_uriTbl.getRecordAt(rsrc.m_uriOffset);
	}

	return pResult;
}

// Allocate a fresh Id for an anonymous resource
pmnt::ResourceId pmnt::KbInstance::createAnonymousRsrc()
{
	ensureNotReadOnly("KbInstance::createAnonymousRsrc");

	ResourceId rsrcId = m_pi->m_rsrcTbl.recordCount();
	KbRsrc rsrc;
	rsrc.init();
	rsrc.m_flags |= asBitMask(ResourceFlags::k_rsrcFlagAnonymous);
	m_pi->m_rsrcTbl.pushBack(rsrc);
	return rsrcId;
}

pmnt::Statement pmnt::KbInstance::findStatement(
	ResourceId subjectId, ResourceId predicateId, ResourceId objectId) const
{
	StmtIterator it = find(subjectId, predicateId, objectId, StmtIteratorFlags::k_showHidden);
	return it.isEnd()
		? Statement(this, k_nullStmtId)
		: it.statement();
}

pmnt::StatementId pmnt::KbInstance::findStatementId(
	ResourceId subjectId, ResourceId predicateId, ResourceId objectId) const
{
	StmtIterator it = find(subjectId, predicateId, objectId, StmtIteratorFlags::k_showHidden);
	return it.isEnd()
		? k_nullStmtId
		: it.statementRef().getStatementId();
}

size_t pmnt::KbInstance::rsrcCount() const
{
	return m_pi->m_rsrcTbl.recordCount();
}

size_t pmnt::KbInstance::stmtCount() const
{
	return m_pi->m_stmtTbl.recordCount();
}

double pmnt::KbInstance::averageRsrcLength() const
{
	size_t accumulatedLength = 0;
	ResourceId numRsrcs = rsrcCount();
	for (ResourceId rsrcId = 0; rsrcId < numRsrcs; ++rsrcId)
	{
		if (isRsrcValid(rsrcId) && !isRsrcAnonymous(rsrcId))
		{
			const RsrcChar* pRsrcStr = rsrcIdToUri(rsrcId);
			size_t len = char_traits<RsrcChar>::length(pRsrcStr);
			accumulatedLength += len;
		}
	}
	return (numRsrcs == 0)
		? 0.0
		: (double) accumulatedLength / (double) numRsrcs;
}

void pmnt::KbInstance::countStmts(/* out */ size_t& total, /* out */ size_t& numDel,
	/* out */ size_t& numInferred, /* out */ size_t& numDelAndInferred,
	/* out */ size_t& numHidden, /* out */ size_t& numVirtual) const
{
	total = 0;
	numDel = 0;
	numInferred = 0;
	numDelAndInferred = 0;
	numVirtual = 0;
	numHidden = 0;

	StmtIterator endIter = end();
	StmtIterator iter = find(k_nullRsrcId, k_nullRsrcId, k_nullRsrcId, StmtIteratorFlags::k_showHidden);
	for (; iter != endIter; ++iter)
	{
		bool isDel = iter.statement().isDeleted();
		bool isInf = iter.statement().isInferred();
		bool isVirtual = iter.statement().isVirtual();
		bool isHidden = iter.statement().isHidden();

		++total;
		if (isDel)
		{
			++numDel;
		}
		if (isInf)
		{
			++numInferred;
		}
		if (isDel && isInf)
		{
			++numDelAndInferred;
		}
		if (isVirtual)
		{
			++numVirtual;
			--total;
		}
		if (isHidden)
		{
			++numHidden;
		}
	}

	if (total != stmtCount())
	{
		throw Exception("Brute-force statement count does not "
			"match statement count stored in the file header");
	}
}

// Add a new statement to the kb.  If this call to addStmt is recursive (because a
// previous call caused an inference), then return k_nullStmtId.  Also, if the
// statement is part of a reification and is therefore virtual, return k_nullStmtId.
// Otherwise, return the new statement ID.
//
// Assumes that some external caller is ensuring serialization between threads,
// which is okay because that is the blanket assumption across all KbInstance
// methods that write.
pmnt::StatementId pmnt::KbInstance::addStmt(ResourceId subjectId,
	ResourceId predicateId, ResourceId objectId, bool isInferred)
{
	ensureNotReadOnly("KbInstance::addStmt");

	if (m_pi->m_addStmtStack.empty())
	{
		// This is a top-level call to addStmt, so do the actual add and remember the
		// statement ID to return later.  Also, add a dummy record to the stack so we
		// know that we're recursing on the next addStmt call.
		m_pi->m_addStmtStack.emplace_back(k_nullRsrcId, k_nullRsrcId, k_nullRsrcId, false);
		StatementId stmtId = addStmtInternal(subjectId, predicateId, objectId, isInferred);

		// In the midst of the addStmtInternal call above, we may have accumulated new
		// inferences to add in m_pi->m_addStmtStack, so we add those now.  Note that
		// the mode of iterating over the collection is a little odd because the call
		// to addStmtInternal in the body of the loop may cause recursive calls to addStmt
		// that add new entries to the collection, so we can't use iterators.
		while (!m_pi->m_addStmtStack.empty())
		{
			StmtToAdd stmtToAdd{m_pi->m_addStmtStack.back()};
			m_pi->m_addStmtStack.pop_back();
			if (stmtToAdd.m_subjId != k_nullRsrcId)
			{
				addStmtInternal(stmtToAdd.m_subjId, stmtToAdd.m_predId, stmtToAdd.m_objId,
					stmtToAdd.m_isInferred);
			}
		}

		// Now return the statement ID from above:
		return stmtId;
	}
	else
	{
		// We are in the midst of a recursion, so don't actually add the new statement,
		// just remember that we need to add it later and return.  (This breaks the
		// recursion so we don't overflow the stack.)
		m_pi->m_addStmtStack.emplace_back(subjectId, predicateId, objectId, isInferred);
		return k_nullStmtId;
	}
}

// Add a new statement to the kb.  If the statement is part of a reification
// and thus is virtual, return k_nullStmtId.  Else return new statement id.
pmnt::StatementId pmnt::KbInstance::addStmtInternal(ResourceId subjectId,
	ResourceId predicateId, ResourceId objectId, bool isInferred)
{
	// Test to see if this statement is part of a reification
	if (predicateId == uriLib().m_rdfSubject.id()
		|| predicateId == uriLib().m_rdfPredicate.id()
		|| predicateId == uriLib().m_rdfObject.id()
		|| (predicateId == uriLib().m_rdfType.id() && objectId == uriLib().m_rdfStatement.id()))
	{
		handleReificationAdd(subjectId, predicateId, objectId);
		return k_nullStmtId;
	}
	else
	{
		Statement s = findStatement(subjectId, predicateId, objectId);
		StatementId result = s.getStatementId();
		if (result != k_nullStmtId)
		{
			KbStmt* pStmt = stmtIdToStmt(result);
			pStmt->setFlag(StatementFlags::k_stmtFlagValid, true);		// ensure it's marked valid
			bool wasDeleted = pStmt->isDeleted();
			pStmt->setFlag(StatementFlags::k_stmtFlagDeleted, false);	// ensure it's not deleted

			// Note that the literal and inferred flags and the source are left
			// unchanged intentionally, even if the caller passes different values.

			if (wasDeleted)
			{
				for (auto pStmtHndlr : m_pi->m_stmtHndlrList)
				{
					pStmtHndlr->onNewStmt(this, Statement(this, result));
				}
			}
		}
		else if (!s.isVirtual())
		{
			// Physically add the statement in the statement table:
			result = addStmtCore(subjectId, predicateId, objectId, false, false, isInferred);

			// Fire the trigger
			for (auto pStmtHndlr : m_pi->m_stmtHndlrList)
			{
				pStmtHndlr->onNewStmt(this, Statement(this, result));
			}
		}
		return result;
	}
}

// This is the core of the addStmt implementation, where that physical modifications
// are made to the statment table itself.  Do not call this directly unless you are
// very clear about what you are doing!
pmnt::StatementId pmnt::KbInstance::addStmtCore(ResourceId subjectId, ResourceId predicateId,
	ResourceId objectId, bool isHidden, bool isDeleted, bool isInferred)
{
	// Hidden flag takes precedence over the other two:
	if (isHidden)
	{
		isDeleted = false;
		isInferred = false;
	}

	// Get the Id of statement to be added
	auto nextStmtID = static_cast<StatementId>(stmtCount());

	KbStmt stmt;
	stmt.init();
	stmt.setFlag(StatementFlags::k_stmtFlagValid, true);		// ensure it's marked valid
	stmt.setFlag(StatementFlags::k_stmtFlagDeleted, isDeleted);
	stmt.setFlag(StatementFlags::k_stmtFlagInferred, isInferred);
	stmt.setFlag(StatementFlags::k_stmtFlagHidden, isHidden);

	// Set up stmt subject, predicate, and object:
	stmt.m_subjectId		= subjectId;
	stmt.m_predicateId	= predicateId;
	stmt.m_objectId		= objectId;

	// Link stmt into subject linked list
	KbRsrc& subRsrc = m_pi->m_rsrcTbl.getRecordAt(subjectId);
	stmt.m_subjectNext = subRsrc.m_subjectFirst;
	subRsrc.m_subjectFirst = nextStmtID;
	++subRsrc.m_subjectCount;

	// Link stmt into predicate linked list
	KbRsrc& predRsrc = m_pi->m_rsrcTbl.getRecordAt(predicateId);
	stmt.m_predicateNext = predRsrc.m_predicateFirst;
	predRsrc.m_predicateFirst = nextStmtID;
	++predRsrc.m_predicateCount;

	// Link stmt into object linked list
	KbRsrc& objRsrc = m_pi->m_rsrcTbl.getRecordAt(objectId);
	stmt.m_objectNext = objRsrc.m_objectFirst;
	objRsrc.m_objectFirst = nextStmtID;
	++objRsrc.m_objectCount;

	// Store the stmt
	m_pi->m_stmtTbl.pushBack(stmt);

	return nextStmtID;
}

void pmnt::KbInstance::deleteStmt(ResourceId subjectId, ResourceId predicateId,
	ResourceId objectId)
{
	ensureNotReadOnly("KbInstance::deleteStmt");
	// Test to see if this statement is part of a reification
	if (predicateId == uriLib().m_rdfSubject.id()
		|| predicateId == uriLib().m_rdfPredicate.id()
		|| predicateId == uriLib().m_rdfObject.id()
		|| (predicateId == uriLib().m_rdfType.id() && objectId == uriLib().m_rdfStatement.id()))
	{
		handleReificationDelete(subjectId, predicateId, objectId);
	}
	else
	{
		StatementId stmtId = findStatementId(subjectId, predicateId, objectId);
		if (stmtId != k_nullStmtId)
		{
			stmtIdToStmt(stmtId)->setFlag(StatementFlags::k_stmtFlagDeleted, true);
		}
	}
}

void pmnt::KbInstance::handleReificationDelete(ResourceId subjectId,
	ResourceId predicateId, ResourceId objectId)
{
	ResourceId itName = subjectId;
	ResourceId itSubj = k_nullRsrcId;
	ResourceId itPred = k_nullRsrcId;
	ResourceId itObj = k_nullRsrcId;

	if (predicateId == uriLib().m_rdfSubject.id())
	{
		itSubj = objectId;
	}
	else if (predicateId == uriLib().m_rdfPredicate.id())
	{
		itPred = objectId;
	}
	else if (predicateId == uriLib().m_rdfObject.id())
	{
		itObj = objectId;
	}
	else
	{
		// Do nothing, <name> rdf:type rdf:Statement
		return;
	}

	ReificationIterator it(this, itName, itSubj, itPred, itObj);
	if (!it.isEnd())
	{
		deleteReification(it->first, it.subjectId(), it.predicateId(), it.objectId());
	}
}

void pmnt::KbInstance::handleReificationAdd(ResourceId subjectId,
	ResourceId predicateId, ResourceId objectId)
{
	auto it = m_pi->m_reificationMap.find(subjectId);
	if (it == cEnd(m_pi->m_reificationMap))
	{
		it = m_pi->m_reificationMap.insert(make_pair(subjectId, PartialReification())).first;
	}

	PartialReification& pr = it->second;
	if (predicateId == uriLib().m_rdfSubject.id())
	{
		pr.m_subId = objectId;
	}
	else if (predicateId == uriLib().m_rdfPredicate.id())
	{
		pr.m_predId = objectId;
	}
	else if (predicateId == uriLib().m_rdfObject.id())
	{
		pr.m_objId = objectId;
	}
	else
	{
		// Do nothing:  <name> rdf:type rdf:Statement
	}

	if (pr.m_subId != k_nullRsrcId && pr.m_predId != k_nullRsrcId && pr.m_objId != k_nullRsrcId)
	{
		// This reification is complete, so assert it:
		ResourceId stmtName;
		StatementId stmtId;
		tie(stmtName, stmtId) = addReification(subjectId, pr.m_subId, pr.m_predId, pr.m_objId);
		m_pi->m_reificationMap.erase(it);

		// Fire the triggers:
		for (auto pStmtHndlr : m_pi->m_stmtHndlrList)
		{
			pStmtHndlr->onNewStmt(this, Statement(this, stmtId, stmtName, VirtualStmtMode::k_subject));
			pStmtHndlr->onNewStmt(this, Statement(this, stmtId, stmtName, VirtualStmtMode::k_predicate));
			pStmtHndlr->onNewStmt(this, Statement(this, stmtId, stmtName, VirtualStmtMode::k_object));
			pStmtHndlr->onNewStmt(this, Statement(this, stmtId, stmtName, VirtualStmtMode::k_type));
		}
	}
}

pair<pmnt::ResourceId, pmnt::StatementId> pmnt::KbInstance::addReification(
	ResourceId stmtName, ResourceId subjectId, ResourceId predicateId, ResourceId objectId)
{
	ensureNotReadOnly("KbInstance::addReification");

	StatementId stmtId = findStatementId(subjectId, predicateId, objectId);
	if (stmtId == k_nullStmtId)
	{
		stmtId = addStmtCore(subjectId, predicateId, objectId, false, true, false);
	}
	KbStmt* pStmt = stmtIdToStmt(stmtId);
	ResourceId stmtTag = pStmt->m_statementTag;
	if (stmtTag == k_nullRsrcId)
	{
		// Get the next resource ID:
		stmtTag = static_cast<ResourceId>(m_pi->m_rsrcTbl.recordCount());

		// Initialize the KbRsrc
		KbRsrc rsrc;
		rsrc.init();
		rsrc.m_uriOffset = stmtId;
		rsrc.setFlag(ResourceFlags::k_rsrcFlagStatementTag, true);

		// Insert the KbRsrc in the file
		m_pi->m_rsrcTbl.pushBack(rsrc);
		pStmt->m_statementTag = stmtTag;
	}

	// Create the hidden link statement
	ResourceId hasName = m_pi->m_uriLib.m_statementHasName.id();
	if (findStatementId(stmtTag, hasName, stmtName) == k_nullStmtId)
	{
		addStmtCore(stmtTag, hasName, stmtName, true, false, false);
	}

	return make_pair(stmtName, stmtId);
}

void pmnt::KbInstance::deleteReification(ResourceId stmtName, ResourceId subjectId,
		ResourceId predicateId, ResourceId objectId)
{
	ensureNotReadOnly("KbInstance::deleteReification");
	StatementId stmtId = findStatementId(subjectId, predicateId, objectId);
	if (stmtId != k_nullStmtId){
		KbStmt* pStmt = stmtIdToStmt(stmtId);
		ResourceId stmtTag = pStmt->m_statementTag;
		deleteStmt(stmtTag, m_pi->m_uriLib.m_statementHasName.id(),stmtName);
	}
}

pmnt::KbRsrc* pmnt::KbInstance::rsrcIdToRsrc(ResourceId rsrcId) const
{
	return &m_pi->m_rsrcTbl.getRecordAt(rsrcId);
}

pmnt::KbStmt* pmnt::KbInstance::stmtIdToStmt(StatementId stmtId) const
{
	return &m_pi->m_stmtTbl.getRecordAt(stmtId);
}

pmnt::KbStmt* pmnt::KbInstance::stmtIdToStmt(StatementId stmtId,
	const nothrow_t& /* nothrow */) const
{
	return (stmtId == k_nullStmtId || stmtId >= (StatementId) stmtCount())
		? nullptr
		: stmtIdToStmt(stmtId);
}

pmnt::ResourceId pmnt::KbInstance::subject(StatementId stmtId) const
{
	return stmtIdToStmt(stmtId)->m_subjectId;
}

pmnt::ResourceId pmnt::KbInstance::predicate(StatementId stmtId) const
{
	return stmtIdToStmt(stmtId)->m_predicateId;
}

pmnt::ResourceId pmnt::KbInstance::object(StatementId stmtId) const
{
	return stmtIdToStmt(stmtId)->m_objectId;
}

pmnt::ResourceId pmnt::KbInstance::statementTag(StatementId stmtId) const
{
	return stmtIdToStmt(stmtId)->m_statementTag;
}

pmnt::StmtIterator pmnt::KbInstance::begin(StmtIteratorFlags flags) const
{
	return StmtIterator(this, k_nullRsrcId, k_nullRsrcId, k_nullRsrcId, flags);
}

pmnt::StmtIterator pmnt::KbInstance::end() const
{
	return StmtIterator(this);
}

pmnt::StmtIterator pmnt::KbInstance::find(
	ResourceId subjectId, ResourceId predicateId,
	ResourceId objectId, StmtIteratorFlags flags) const
{
	if (m_pi->testAndClearRunAddNewRulesFlag() && m_pi->m_config.enableSWRLRuleEngine())
	{
		m_pi->m_re.addNewRules();
	}

	return StmtIterator(this, subjectId, predicateId, objectId, flags);
}

pmnt::PhysicalStmtIterator pmnt::KbInstance::beginPhysical(
	StmtIteratorFlags flags) const
{
	return PhysicalStmtIterator(this, k_nullRsrcId, k_nullRsrcId, k_nullRsrcId, flags);
}

pmnt::PhysicalStmtIterator pmnt::KbInstance::endPhysical() const
{
	return PhysicalStmtIterator(this);
}

pmnt::PhysicalStmtIterator pmnt::KbInstance::findPhysical(
	ResourceId subjectId, ResourceId predicateId,
	ResourceId objectId, StmtIteratorFlags flags) const
{
	return PhysicalStmtIterator(this, subjectId, predicateId, objectId, flags);
}

pmnt::ReificationIterator pmnt::KbInstance::findReifications(
	ResourceId stmtName, ResourceId subjectId, ResourceId predicateId,
	ResourceId objectId) const
{
	return ReificationIterator(this, stmtName, subjectId, predicateId, objectId);
}

void pmnt::KbInstance::setRunAddNewRulesFlag()
{
	m_pi->setRunAddNewRulesFlag();
}

// A convenience method for the SWRL rules engine:
pmnt::ResourceId pmnt::KbInstance::findAndGetObjectId(ResourceId subjId,
	ResourceId predId, StmtIteratorFlags flags /* = StmtIteratorFlags::k_skipDeleted */) const
{
	StmtIterator iter = find(subjId, predId, k_nullRsrcId, flags);
	return iter.isEnd()
		? k_nullRsrcId
		: iter.statement().getObjectId();
}

pmnt::StatementId pmnt::KbInstance::getStatementIdForStatementTag(
	ResourceId resourceId) const
{
	KbRsrc& rsrc = m_pi->m_rsrcTbl.getRecordAt(resourceId);
	return rsrc.m_uriOffset;
}

size_t pmnt::KbInstance::subjectCount(ResourceId subjectId) const
{
	return rsrcIdToRsrc(subjectId)->m_subjectCount;
}

size_t pmnt::KbInstance::predicateCount(ResourceId predicateId) const
{
	return rsrcIdToRsrc(predicateId)->m_predicateCount;
}

size_t pmnt::KbInstance::objectCount(ResourceId objectId) const
{
	return rsrcIdToRsrc(objectId)->m_objectCount;
}

pmnt::StatementId pmnt::KbInstance::firstSubject(ResourceId subjectId,
	DeletedStmtsAction delStmtsAction) const
{
	StatementId result = k_nullStmtId;

	if (delStmtsAction == DeletedStmtsAction::include)
	{
		result = rsrcIdToRsrc(subjectId)->m_subjectFirst;
	}
	else
	{
		KbStmt* pStmt = nullptr;
		for (result = rsrcIdToRsrc(subjectId)->m_subjectFirst; result != k_nullStmtId; result = pStmt->m_subjectNext)
		{
			pStmt = stmtIdToStmt(result);
			if (!pStmt->isDeleted())
			{
				break;
			}
		}
	}
	return result;
}

pmnt::StatementId pmnt::KbInstance::firstPredicate(ResourceId predicateId,
	DeletedStmtsAction delStmtsAction) const
{
	StatementId result = k_nullStmtId;

	if (delStmtsAction == DeletedStmtsAction::include)
	{
		result = rsrcIdToRsrc(predicateId)->m_predicateFirst;
	}
	else
	{
		KbStmt* pStmt = nullptr;
		for (result = rsrcIdToRsrc(predicateId)->m_predicateFirst; result != k_nullStmtId; result = pStmt->m_predicateNext)
		{
			pStmt = stmtIdToStmt(result);
			if (!pStmt->isDeleted())
			{
				break;
			}
		}
	}
	return result;
}

pmnt::StatementId pmnt::KbInstance::firstObject(ResourceId objectId,
	DeletedStmtsAction delStmtsAction) const
{
	StatementId result = k_nullStmtId;

	if (delStmtsAction == DeletedStmtsAction::include)
	{
		result = rsrcIdToRsrc(objectId)->m_objectFirst;
	}
	else
	{
		KbStmt* pStmt = nullptr;
		for (result = rsrcIdToRsrc(objectId)->m_objectFirst; result != k_nullStmtId; result = pStmt->m_objectNext)
		{
			pStmt = stmtIdToStmt(result);
			if (!pStmt->isDeleted())
			{
				break;
			}
		}
	}
	return result;
}


pmnt::StatementId pmnt::KbInstance::nextSubject(StatementId stmtId,
	DeletedStmtsAction delStmtsAction) const
{
	StatementId result = k_nullStmtId;
	if (delStmtsAction == DeletedStmtsAction::include)
	{
		result = stmtIdToStmt(stmtId)->m_subjectNext;
	}
	else
	{
		for (KbStmt* pStmt = stmtIdToStmt(stmtId);;)
		{
			result = pStmt->m_subjectNext;
			if (result == k_nullStmtId)
			{
				break;
			}
			pStmt = stmtIdToStmt(result);
			if (!pStmt->isDeleted())
			{
				break;
			}
		}
	}
	return result;
}

pmnt::StatementId pmnt::KbInstance::nextPredicate(StatementId stmtId,
	DeletedStmtsAction delStmtsAction) const
{
	StatementId result = k_nullStmtId;
	if (delStmtsAction == DeletedStmtsAction::include)
	{
		result = stmtIdToStmt(stmtId)->m_predicateNext;
	}
	else
	{
		for (KbStmt* pStmt = stmtIdToStmt(stmtId);;)
		{
			result = pStmt->m_predicateNext;
			if (result == k_nullStmtId)
			{
				break;
			}
			pStmt = stmtIdToStmt(result);
			if (!pStmt->isDeleted())
			{
				break;
			}
		}
	}
	return result;
}

pmnt::StatementId pmnt::KbInstance::nextObject(StatementId stmtId,
	DeletedStmtsAction delStmtsAction) const
{
	StatementId result = k_nullStmtId;
	if (delStmtsAction == DeletedStmtsAction::include)
	{
		result = stmtIdToStmt(stmtId)->m_objectNext;
	}
	else
	{
		for (KbStmt* pStmt = stmtIdToStmt(stmtId);;)
		{
			result = pStmt->m_objectNext;
			if (result == k_nullStmtId)
			{
				break;
			}
			pStmt = stmtIdToStmt(result);
			if (!pStmt->isDeleted())
			{
				break;
			}
		}
	}
	return result;
}

bool pmnt::KbInstance::isStmtDeleted(StatementId stmtId) const
{
	return stmtIdToStmt(stmtId)->testFlag(StatementFlags::k_stmtFlagDeleted);
}

bool pmnt::KbInstance::isStmtInferred(StatementId stmtId) const
{
	return stmtIdToStmt(stmtId)->testFlag(StatementFlags::k_stmtFlagInferred);
}

bool pmnt::KbInstance::isStmtHidden(StatementId stmtId) const
{
	return stmtIdToStmt(stmtId)->testFlag(StatementFlags::k_stmtFlagHidden);
}

bool pmnt::KbInstance::isStmtValid(StatementId stmtId) const
{
	return stmtIdToStmt(stmtId)->testFlag(StatementFlags::k_stmtFlagValid);
}

bool pmnt::KbInstance::isRsrcLiteral(ResourceId rsrcId) const
{
	return rsrcIdToRsrc(rsrcId)->testFlag(ResourceFlags::k_rsrcFlagLiteral);
}

bool pmnt::KbInstance::isRsrcAnonymous(ResourceId rsrcId) const
{
	return rsrcIdToRsrc(rsrcId)->testFlag(ResourceFlags::k_rsrcFlagAnonymous);
}

bool pmnt::KbInstance::isRsrcValid(ResourceId rsrcId) const
{
	return rsrcIdToRsrc(rsrcId)->testFlag(ResourceFlags::k_rsrcFlagValid);
}

void pmnt::KbInstance::addNewStmtHndlr(NewStmtHandler* pHandler)
{
	auto end = cEnd(m_pi->m_stmtHndlrList);
	auto it = ::std::find(cBegin(m_pi->m_stmtHndlrList), end, pHandler);
	if (it == end)
	{
		m_pi->m_stmtHndlrList.push_back(pHandler);
	}
}

void pmnt::KbInstance::removeNewStmtHndlr(NewStmtHandler* pHandler)
{
	if (m_pi)	// Since this is likely to be called at shutdown
	{
		auto end = ::std::end(m_pi->m_stmtHndlrList);
		auto newEnd = remove(::std::begin(m_pi->m_stmtHndlrList), end, pHandler);
		m_pi->m_stmtHndlrList.erase(newEnd, end);
	}
}

void pmnt::KbInstance::dumpKbAsNTriples(ostream& strm,
	InferredStmtsAction infStmtsAction, DeletedStmtsAction delStmtsAction,
	EncodingCharSet charSet) const
{
	auto endIter = end();
	for (auto it = find(k_nullRsrcId, k_nullRsrcId, k_nullRsrcId,
		StmtIteratorFlags::k_skipNone); it != endIter; ++it)
	{
		auto stmt = it.statement();
		auto isDel = stmt.isDeleted();
		auto isInf = stmt.isInferred();

		if ((!isDel && !isInf)
			|| (isDel && delStmtsAction == DeletedStmtsAction::include)
			|| (isInf && infStmtsAction == InferredStmtsAction::include))
		{
			encodeRsrc(strm, stmt.getSubjectId(), charSet, EncodingType::IRI);
			strm << ' ';
			encodeRsrc(strm, stmt.getPredicateId(), charSet, EncodingType::IRI);
			strm << ' ';
			auto encType = isRsrcLiteral(stmt.getObjectId())
				? EncodingType::LITERAL : EncodingType::IRI;
			encodeRsrc(strm, stmt.getObjectId(), charSet, encType);
			strm << " ." << endl;
		}
	}
}

void pmnt::KbInstance::encodeRsrc(ostream& strm, ResourceId rsrcId,
	EncodingCharSet charSet, EncodingType encType) const
{
	if (rsrcId != k_nullRsrcId)
	{
		if (encType == EncodingType::IRI)
		{
			if (isRsrcAnonymous(rsrcId))
			{
				strm << "_:bn" << hex << setfill('0') << setw(8) << rsrcId
					<< setfill(' ') << dec;
			}
			else
			{
				auto pRsrc = rsrcIdToUri(rsrcId);
				strm << '<';
				encodeUnicodeString(strm, pRsrc, charSet, encType);
				strm << '>';
			}
		}
		else
		{
			auto pRsrc = rsrcIdToUri(rsrcId);
			RsrcString lexicalForm;
			RsrcString datatypeUri;
			RsrcString langTag;
			::std::tie(lexicalForm, datatypeUri, langTag) = LiteralUtils::parseLiteral(pRsrc);
			if (datatypeUri.empty() && langTag.empty())
			{
				strm << '"';
				encodeUnicodeString(strm, lexicalForm.c_str(), charSet, encType);
				strm << '"';
			}
			else if (langTag.empty())
			{
				strm << '"';
				encodeUnicodeString(strm, lexicalForm.c_str(), charSet, encType);
				strm << "\"^^<";
				encodeUnicodeString(strm, datatypeUri.c_str(), charSet, encType);
				strm << '>';
			}
			else
			{
				strm << '"';
				encodeUnicodeString(strm, lexicalForm.c_str(), charSet, encType);
				strm << "\"@";
				encodeUnicodeString(strm, langTag.c_str(), charSet, encType);
			}
		}
	}
}

void pmnt::KbInstance::encodeUnicodeString(ostream& strm, const RsrcChar* pRsrc,
	EncodingCharSet charSet, EncodingType encType)
{
	using UnicodeIter = UnicodeIterator<RsrcChar, const RsrcChar*>;

	static const char k_hexDigits[] =
	{
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};

	auto pEnd = pRsrc + char_traits<RsrcChar>::length(pRsrc);
	auto end = UnicodeIter::end(pEnd);
	for (auto it = UnicodeIter::begin(pRsrc, pEnd); it != end; ++it)
	{
		uint32 ch = *it;

		if (ch == 0x0a)
		{
			strm << "\\n";
		}
		else if (ch == 0x0d)
		{
			strm << "\\r";
		}
		else if (ch == 0x22)
		{
			strm << "\\\"";
		}
		else if (ch == 0x5c)
		{
			strm << "\\\\";
		}
		else if (0x20 <= ch && ch <= 0x7e)
		{
			strm << static_cast<char>(static_cast<uint8>(ch));
		}
		else if (charSet == EncodingCharSet::utf8)
		{
			strm << convertUtf32ToUtf8(&ch, &ch + 1);
		}
		else if (ch == 0x09)
		{
			strm << "\\t";
		}
		else if (ch == 0x08)
		{
			strm << "\\b";
		}
		else if (ch == 0x0c)
		{
			strm << "\\f";
		}
		else if (ch <= 0xffff)
		{
			strm << "\\u";
			for (int shift = 12; shift >= 0; shift -= 4)
			{
				strm << k_hexDigits[(ch >> shift) & 0xf];
			}
		}
		else
		{
			strm << "\\U";
			for (int shift = 28; shift >= 0; shift -= 4)
			{
				strm << k_hexDigits[(ch >> shift) & 0xf];
			}
		}
	}
}

void pmnt::KbInstance::loadKbFromNTriples(istream& /* strm */,
	bool /* fileHasSrc */, InferredStmtsAction /* infStmtsAction */,
	DeletedStmtsAction /* delStmtsAction */) const
{
}

void pmnt::KbInstance::printStatements(ostream& s, bool includeNextStmts, bool verboseNextStmts) const
{
	StatementId numStmts = stmtCount();
	for (StatementId stmtId = 0; stmtId < numStmts; ++stmtId)
	{
		const KbStmt& stmt = m_pi->m_stmtTbl.getRecordAt(stmtId);
		if (stmt.isValid())
		{
			printStmt(stmt, stmtId, s, includeNextStmts, verboseNextStmts);
		}
	}
	s << endl;
}

void pmnt::KbInstance::printResources(ostream& s, bool includeFirstStmts, bool verboseFirstStmts) const
{
	ResourceId numRsrcs = rsrcCount();
	for (ResourceId rsrcId = 0; rsrcId < numRsrcs; ++rsrcId)
	{
		const KbRsrc& rsrc = m_pi->m_rsrcTbl.getRecordAt(rsrcId);
		if (rsrc.isValid())
		{
			printRsrc(rsrc, rsrcId, s, includeFirstStmts, verboseFirstStmts);
		}
	}

	s << endl;
}

void pmnt::KbInstance::printStmt(const KbStmt& stmt, StatementId stmtId,
	ostream& s, bool includeNextStmts, bool verboseNextStmts) const
{
	s << endl << "statement[" << stmtId << "]:  flags: 0x" << hex << setfill('0')
		<< setw(8) << stmt.m_flags << setfill(' ') << dec << endl;

	printTriple(stmt, 1, s);

	if (includeNextStmts)
	{
		printStmtIdField("subjectNext", stmt.m_subjectNext, s, verboseNextStmts);
		printStmtIdField("predicateNext", stmt.m_predicateNext, s, verboseNextStmts);
		printStmtIdField("objectNext", stmt.m_objectNext, s, verboseNextStmts);
	}
}

void pmnt::KbInstance::printRsrc(const KbRsrc& rsrc, ResourceId rsrcId,
	ostream& s, bool includeFirstStmts, bool verboseFirstStmts) const
{
	size_t totalUses = rsrc.m_subjectCount + rsrc.m_predicateCount + rsrc.m_objectCount;

	s << endl << "resource[" << rsrcId << "]:  "
		<< formatRsrcUri(rsrcId, false) << endl;
	s << "  flags: 0x" << hex << setfill('0') << setw(8) << rsrc.m_flags
		<< setfill(' ') << dec << ", # uses: " << totalUses;
	if (totalUses > 0)
	{
		s << " [subj: " << rsrc.m_subjectCount
			<< ", pred: " << rsrc.m_predicateCount
			<< ", obj: " << rsrc.m_objectCount << "]";
	}
	s << endl;

	if (includeFirstStmts)
	{
		printStmtIdField("subjectFirst", rsrc.m_subjectFirst, s, verboseFirstStmts);
		printStmtIdField("predicateFirst", rsrc.m_predicateFirst, s, verboseFirstStmts);
		printStmtIdField("objectFirst", rsrc.m_objectFirst, s, verboseFirstStmts);
	}
}

void pmnt::KbInstance::printStmtIdField(const char* pFieldName,
	StatementId stmtId, ostream& s, bool includeStmtTriple) const
{
	if (stmtId != k_nullStmtId)
	{
		s << "  " << pFieldName << ":  [sid: " << stmtId << "]" << endl;
		if (includeStmtTriple)
		{
			KbStmt* pStmt = stmtIdToStmt(stmtId);
			printTriple(*pStmt, 2, s);
		}
	}
}

void pmnt::KbInstance::printTriple(const KbStmt& stmt, uint32 indentLevel, ostream& s) const
{
	string indent(2 * indentLevel, ' ');

	s << indent << "subj: " << formatRsrcUri(stmt.m_subjectId) << endl;
	s << indent << "pred: " << formatRsrcUri(stmt.m_predicateId) << endl;
	s << indent << "obj:  " << formatRsrcUri(stmt.m_objectId) << endl;
}

string pmnt::KbInstance::formatRsrcUri(ResourceId rsrcId, bool includeRsrcId) const
{
	string result("<<null>>");

	if (rsrcId != k_nullRsrcId)
	{
		const RsrcChar* pRsrc = rsrcIdToUri(rsrcId);
		if (pRsrc == nullptr)
		{
			result = "<<anonymous>>";
		}
		else if (*pRsrc == 0)
		{
			result = "<<blank>>";
		}
		else
		{
#if defined(PARLIAMENT_RSRC_AS_UTF16)
			result = convertToUtf8(pRsrc);
#else
			result = pRsrc;
#endif
		}
	}

	if (includeRsrcId)
	{
		ostringstream ss;
		ss << result << " [" << rsrcId << "]";
		return ss.str();
	}
	else
	{
		return result;
	}
}

void pmnt::KbInstance::deleteKb(const Config& cfg, const bfs::path& dir)
{
	remove(dir / cfg.uriTableFileName());
	remove(dir / cfg.uriToIntFileName());
	remove(dir / cfg.stmtFileName());
	remove(dir / cfg.rsrcFileName());
}

void pmnt::KbInstance::deleteKb(const Config& cfg)
{
	remove(cfg.uriTableFilePath());
	remove(cfg.uriToIntFilePath());
	remove(cfg.stmtFilePath());
	remove(cfg.rsrcFilePath());
}

void pmnt::KbInstance::deleteKb(const bfs::path& dir)
{
	deleteKb(Config::readFromFile(), dir);
}

void pmnt::KbInstance::deleteKb()
{
	deleteKb(Config::readFromFile());
}

size_t pmnt::KbInstance::ruleCount() const
{
	return m_pi->m_re.ruleCount();
}

void pmnt::KbInstance::printRules(ostream& s) const
{
	m_pi->m_re.printRules(s);
}

void pmnt::KbInstance::printRuleTriggers(ostream& s) const
{
	m_pi->m_re.printTriggers(s);
}

void pmnt::KbInstance::ensureNotReadOnly(const char* pCallingMethodName) const
{
	if (m_pi->m_config.readOnly())
	{
		throw Exception(format("Attempt to invoke a KB-changing method "
			"when the KB was opened in read-only mode (from method %1%)")
			% pCallingMethodName);
	}
}

const pmnt::UriLib& pmnt::KbInstance::uriLib() const
{
	return m_pi->m_uriLib;
}

#if defined(PARLIAMENT_UNIT_TEST)
pmnt::RuleEngine& pmnt::KbInstance::ruleEngine()
{
	return m_pi->m_re;
}
#endif
