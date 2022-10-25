// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_KBINSTANCEIMPL_H_INCLUDED)
#define PARLIAMENT_KBINSTANCEIMPL_H_INCLUDED

// This file should be included ONLY from KbInstance.cpp and KbValidation.cpp!

#include "parliament/KbConfig.h"
#include "parliament/FixRecordTable.h"
#include "parliament/KbRsrc.h"
#include "parliament/KbStmt.h"
#include "parliament/Log.h"
#include "parliament/NewStmtHandler.h"
#include "parliament/RuleEngine.h"
#include "parliament/StringToId.h"
#include "parliament/UriLib.h"
#include "parliament/VarRecordTable.h"

#include <atomic>
#include <unordered_map>
#include <vector>

namespace bbn::parliament
{

struct PartialReification
{
	PartialReification() :
		m_subId(k_nullRsrcId),
		m_predId(k_nullRsrcId),
		m_objId(k_nullRsrcId)
	{}

	ResourceId	m_subId;
	ResourceId	m_predId;
	ResourceId	m_objId;
};

struct StmtToAdd {
	StmtToAdd(ResourceId subjId, ResourceId predId, ResourceId objId, bool isInferred) :
		m_subjId(subjId),
		m_predId(predId),
		m_objId(objId),
		m_isInferred(isInferred)
	{}

	ResourceId m_subjId;
	ResourceId m_predId;
	ResourceId m_objId;
	bool m_isInferred;
};

class KbInstance;

struct KbInstance::Impl
{
	using AtomicBool = ::std::atomic_flag;
	using StmtHandlerList = ::std::vector<NewStmtHandler*>;
	using ReificationMap = ::std::unordered_map<ResourceId, PartialReification>;
	using RsrcTable = FixRecordTable<KbRsrc>;
	using StmtTable = FixRecordTable<KbStmt>;
	using AddStmtStack = ::std::vector<StmtToAdd>;

	Impl(const KbConfig& config, KbInstance* pKB) :
		m_config(config.ensureKbDirExists()),
		m_dontNeedToRunAddNewRules(),
		m_stmtHndlrList(),
		m_reificationMap(),
		m_disposition(determineDisposition(m_config, true)),
		m_uriTbl(m_config.uriTableFilePath(), m_config.readOnly(),
			m_config.initialRsrcCapacity() * m_config.avgRsrcLen(),
			m_config.rsrcGrowthIncrement() * m_config.avgRsrcLen(), m_config.rsrcGrowthFactor()),
		m_uriToRsrcId(m_config.uriToIntFilePath(), m_config.bdbCacheSize(), m_config.readOnly()),
		m_rsrcTbl(m_config.rsrcFilePath(), m_config.readOnly(), m_config.initialRsrcCapacity(),
			m_config.rsrcGrowthIncrement(), m_config.rsrcGrowthFactor()),
		m_stmtTbl(m_config.stmtFilePath(), m_config.readOnly(), m_config.initialStmtCapacity(),
			m_config.stmtGrowthIncrement(), m_config.stmtGrowthFactor()),
		m_uriLib(pKB),
		m_addStmtStack(),
		m_re(pKB)
	{
		testAndClearRunAddNewRulesFlag();
	}

	Impl(const Impl&) = delete;
	Impl& operator=(const Impl&) = delete;
	Impl(Impl&&) = delete;
	Impl& operator=(Impl&&) = delete;

	~Impl()
	{
		m_stmtHndlrList.clear();	// To be absolutely sure...
	}

	// m_dontNeedToRunAddNewRules is named in the negative because the semantics of atomic_flag
	// only work in one direction.  Use the methods to avoid the confusing double negative.
	void setRunAddNewRulesFlag()
	{
		m_dontNeedToRunAddNewRules.clear();
	}
	bool testAndClearRunAddNewRulesFlag()
	{
		return !m_dontNeedToRunAddNewRules.test_and_set();
	}

	const KbConfig			m_config;			// Configuration parameters passed at initialization
	mutable AtomicBool	m_dontNeedToRunAddNewRules;	// Whether to run addNewRules prior to a find()

	StmtHandlerList		m_stmtHndlrList;	// List of callbacks called when new statments are added
	ReificationMap			m_reificationMap;	// Stores partial reifications
	const KbDisposition	m_disposition;		// Must come before any of the fields that represent files

	VarRecordTable			m_uriTbl;			// Variable-length uri records
	StringToId				m_uriToRsrcId;		// Mapping between URI's and ResourceId's
	RsrcTable				m_rsrcTbl;			// Stores info about each resource (e.g., first use, validity)
	StmtTable				m_stmtTbl;			// Stores triples (subjectId, predicateId, objectId)

	UriLib					m_uriLib;
	AddStmtStack			m_addStmtStack;
	RuleEngine				m_re;					// Implements SWRL-style rule inferencing
};

}	// namespace end

#endif // !PARLIAMENT_KBINSTANCEIMPL_H_INCLUDED
