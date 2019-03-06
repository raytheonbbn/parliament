// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_KBINSTANCE_H_INCLUDED)
#define PARLIAMENT_KBINSTANCE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/PhysicalStmtIterator.h"
#include "parliament/ReificationIterator.h"
#include "parliament/StmtIterator.h"
#include "parliament/Types.h"

#include <boost/filesystem/path.hpp>
#include <memory>
#include <string>

PARLIAMENT_NAMESPACE_BEGIN

class KbConfig;
struct KbRsrc;
struct KbStmt;
class NewStmtHandler;
class Statement;
class UriLib;

#if defined(PARLIAMENT_UNIT_TEST)
class RuleEngine;
#endif

// The concrete representation of a Parliament triple store.
class KbInstance
{
public:
	PARLIAMENT_EXPORT KbInstance(const KbConfig& config);
	KbInstance(const KbInstance&) = delete;
	KbInstance& operator=(const KbInstance&) = delete;
	KbInstance(KbInstance&&) = delete;
	KbInstance& operator=(KbInstance&&) = delete;
	PARLIAMENT_EXPORT ~KbInstance();

	PARLIAMENT_EXPORT const KbConfig& config() const;
	PARLIAMENT_EXPORT void sync();
	PARLIAMENT_EXPORT void getExcessCapacity(/* out */ double& pctUnusedUriCapacity,
		/* out */ double& pctUnusedRsrcCapacity, /* out */ double& pctUnusedStmtCapacity) const;
	PARLIAMENT_EXPORT void releaseExcessCapacity();

	PARLIAMENT_EXPORT ResourceId uriToRsrcId(const RsrcChar* pUri, size_t uriLen, bool isLiteral, bool createIfMissing);
	PARLIAMENT_EXPORT ResourceId uriToRsrcId(const RsrcChar* pUri, bool isLiteral, bool createIfMissing);
	ResourceId uriToRsrcId(const RsrcString& uri, bool isLiteral, bool createIfMissing)
		{ return uriToRsrcId(uri.c_str(), uri.length(), isLiteral, createIfMissing); }
	PARLIAMENT_EXPORT const RsrcChar* rsrcIdToUri(ResourceId rsrcId) const;
	PARLIAMENT_EXPORT ResourceId createAnonymousRsrc();

	PARLIAMENT_EXPORT size_t rsrcCount() const;
	PARLIAMENT_EXPORT double averageRsrcLength() const;
	PARLIAMENT_EXPORT size_t stmtCount() const;
	PARLIAMENT_EXPORT void countStmts(/* out */ size_t& total, /* out */ size_t& numDel,
		/* out */ size_t& numInferred, /* out */ size_t& numDelAndInferred,
		/* out */ size_t& numHidden, /* out */ size_t& numVirtual) const;

	PARLIAMENT_EXPORT StatementId addStmt(ResourceId subjectId,
		ResourceId predicateId, ResourceId objectId, bool isInferred);
	PARLIAMENT_EXPORT void deleteStmt(ResourceId subjectId, ResourceId predicateId,
		ResourceId objectId);

	PARLIAMENT_EXPORT ::std::pair<ResourceId, StatementId> addReification(
		ResourceId stmtName, ResourceId subjectId, ResourceId predicateId,
		ResourceId objectId);
	PARLIAMENT_EXPORT void deleteReification(ResourceId stmtName, ResourceId subjectId,
			ResourceId predicateId, ResourceId objectId);

	PARLIAMENT_EXPORT ResourceId subject(StatementId stmtId) const;
	PARLIAMENT_EXPORT ResourceId predicate(StatementId stmtId) const;
	PARLIAMENT_EXPORT ResourceId object(StatementId stmtId) const;
	PARLIAMENT_EXPORT ResourceId statementTag(StatementId stmtId) const;

	PARLIAMENT_EXPORT StmtIterator begin(StmtIteratorFlags flags = StmtIteratorFlags::k_skipDeleted) const;
	PARLIAMENT_EXPORT StmtIterator end() const;
	PARLIAMENT_EXPORT StmtIterator find(ResourceId subjectId, ResourceId predicateId,
		ResourceId objectId, StmtIteratorFlags flags = StmtIteratorFlags::k_skipDeleted) const;

	PhysicalStmtIterator beginPhysical(StmtIteratorFlags flags = StmtIteratorFlags::k_skipDeleted) const;
	PhysicalStmtIterator endPhysical() const;
	PhysicalStmtIterator findPhysical(ResourceId subjectId, ResourceId predicateId,
		ResourceId objectId, StmtIteratorFlags flags = StmtIteratorFlags::k_skipDeleted) const;

	PARLIAMENT_EXPORT ReificationIterator findReifications(ResourceId stmtName,
		ResourceId subjectId, ResourceId predicateId,
		ResourceId objectId) const;

	PARLIAMENT_EXPORT void setRunAddNewRulesFlag();

	// A convenience method for the SWRL rules engine:
	ResourceId findAndGetObjectId(ResourceId subjId, ResourceId predId,
		StmtIteratorFlags flags = StmtIteratorFlags::k_skipDeleted) const;

	PARLIAMENT_EXPORT size_t subjectCount(ResourceId subjectId) const;
	PARLIAMENT_EXPORT size_t predicateCount(ResourceId predicateId) const;
	PARLIAMENT_EXPORT size_t objectCount(ResourceId objectId) const;

	PARLIAMENT_EXPORT StatementId firstSubject(ResourceId subjectId,
		DeletedStmtsAction delStmtsAction = DeletedStmtsAction::exclude) const;
	PARLIAMENT_EXPORT StatementId firstPredicate(ResourceId predicateId,
		DeletedStmtsAction delStmtsAction = DeletedStmtsAction::exclude) const;
	PARLIAMENT_EXPORT StatementId firstObject(ResourceId objectId,
		DeletedStmtsAction delStmtsAction = DeletedStmtsAction::exclude) const;

	PARLIAMENT_EXPORT StatementId nextSubject(StatementId stmtId,
		DeletedStmtsAction delStmtsAction = DeletedStmtsAction::exclude) const;
	PARLIAMENT_EXPORT StatementId nextPredicate(StatementId stmtId,
		DeletedStmtsAction delStmtsAction = DeletedStmtsAction::exclude) const;
	PARLIAMENT_EXPORT StatementId nextObject(StatementId stmtId,
		DeletedStmtsAction delStmtsAction = DeletedStmtsAction::exclude) const;

	PARLIAMENT_EXPORT bool isStmtDeleted(StatementId stmtId) const;
	PARLIAMENT_EXPORT bool isStmtInferred(StatementId stmtId) const;
	PARLIAMENT_EXPORT bool isStmtHidden(StatementId stmtId) const;
	PARLIAMENT_EXPORT bool isStmtValid(StatementId stmtId) const;

	PARLIAMENT_EXPORT bool isRsrcLiteral(ResourceId rsrcId) const;
	PARLIAMENT_EXPORT bool isRsrcAnonymous(ResourceId rsrcId) const;
	PARLIAMENT_EXPORT bool isRsrcValid(ResourceId rsrcId) const;

	PARLIAMENT_EXPORT void addNewStmtHndlr(NewStmtHandler* pHandler);
	PARLIAMENT_EXPORT void removeNewStmtHndlr(NewStmtHandler* pHandler);

	PARLIAMENT_EXPORT void dumpKbAsNTriples(::std::ostream& strm,
		InferredStmtsAction infStmtsAction = InferredStmtsAction::exclude,
		DeletedStmtsAction delStmtsAction = DeletedStmtsAction::exclude,
		EncodingCharSet charSet = EncodingCharSet::utf8) const;
	PARLIAMENT_EXPORT void loadKbFromNTriples(::std::istream& strm,
		bool fileHasSrc = false,
		InferredStmtsAction infStmtsAction = InferredStmtsAction::exclude,
		DeletedStmtsAction delStmtsAction = DeletedStmtsAction::exclude) const;

	PARLIAMENT_EXPORT bool validate(::std::ostream& s) const;
	PARLIAMENT_EXPORT bool validateStrToIdMapping(::std::ostream& s) const;
	PARLIAMENT_EXPORT bool validateUriTblAgainstRsrcTbl(::std::ostream& s) const;

	PARLIAMENT_EXPORT void printStatements(::std::ostream& s,
		bool includeNextStmts = true, bool verboseNextStmts = true) const;
	PARLIAMENT_EXPORT void printResources(::std::ostream& s,
		bool includeFirstStmts = true, bool verboseFirstStmts = true) const;

	PARLIAMENT_EXPORT static KbDisposition determineDisposition(
		const KbConfig& config, bool throwIfIndeterminate = false);
	PARLIAMENT_EXPORT static void deleteKb(const KbConfig& cfg, const ::boost::filesystem::path& dir);
	PARLIAMENT_EXPORT static void deleteKb(const KbConfig& cfg);
	PARLIAMENT_EXPORT static void deleteKb(const ::boost::filesystem::path& dir);
	PARLIAMENT_EXPORT static void deleteKb();

	PARLIAMENT_EXPORT size_t ruleCount() const;
	PARLIAMENT_EXPORT void printRules(::std::ostream& s) const;
	PARLIAMENT_EXPORT void printRuleTriggers(::std::ostream& s) const;

	PARLIAMENT_EXPORT const UriLib& uriLib() const;

	PARLIAMENT_EXPORT StatementId getStatementIdForStatementTag(ResourceId resourceId) const;

#if defined(PARLIAMENT_UNIT_TEST)
	RuleEngine& ruleEngine();
#endif

private:
	enum class EncodingType { LITERAL, IRI };

	static double computeExcessCapacity(size_t capacity, size_t recCount);
	ResourceId createStmtTagRsrc(StatementId reifiedStmtId);
	KbRsrc* rsrcIdToRsrc(ResourceId rsrcId) const;
	KbStmt* stmtIdToStmt(StatementId stmtId) const;
	KbStmt* stmtIdToStmt(StatementId stmtId, const ::std::nothrow_t& nothrow) const;
	Statement findStatement(ResourceId subjectId, ResourceId predicateId, ResourceId objectId) const;
	StatementId findStatementId(ResourceId subjectId, ResourceId predicateId, ResourceId objectId) const;
	StatementId addStmtInternal(ResourceId subjectId, ResourceId predicateId, ResourceId objectId, bool isInferred);
	StatementId addStmtCore(ResourceId subjectId, ResourceId predicateId,
		ResourceId objectId, bool isHidden, bool isDeleted, bool isInferred);
	void printStmt(const KbStmt& stmt, StatementId stmtId, ::std::ostream& s, bool includeNextStmts, bool verboseNextStmts) const;
	void printRsrc(const KbRsrc& rsrc, ResourceId rsrcId, ::std::ostream& s, bool includeFirstStmts, bool verboseFirstStmts) const;
	void printStmtIdField(const char* pFieldName, StatementId stmtId, ::std::ostream& s, bool includeStmtTriple) const;
	void printTriple(const KbStmt& stmt, uint32 indentLevel, ::std::ostream& s) const;
	void handleReificationAdd(ResourceId subjectId, ResourceId predicateId, ResourceId objectId);
	void handleReificationDelete(ResourceId subjectId, ResourceId predicateId, ResourceId objectId);
	::std::string formatRsrcUri(ResourceId rsrcId, bool includeRsrcId = true) const;
	void ensureNotReadOnly(const char* pCallingMethodName) const;
	bool isStartOfRsrcStr(size_t rsrcOffset) const;

	void encodeRsrc(::std::ostream& strm, ResourceId rsrcId,
		EncodingCharSet charSet, EncodingType encType) const;
	static void encodeUnicodeString(::std::ostream& strm, const RsrcChar* pRsrc,
		EncodingCharSet charSet, EncodingType encType);

	struct Impl;
	::std::unique_ptr<Impl> m_pi;	// pointer to implementation
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_KBINSTANCE_H_INCLUDED
