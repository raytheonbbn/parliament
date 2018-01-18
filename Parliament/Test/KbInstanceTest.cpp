// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/test/unit_test.hpp>
#include <cstdio>
#include <exception>
#include <limits>
#include <set>
#include <string>
#include "parliament/KbInstance.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/Config.h"
#include "parliament/StmtIterator.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/UriLib.h"
#include "parliament/Util.h"
#include "TestUtils.h"

using namespace ::bbn::parliament;
using ::std::exception;
using ::std::numeric_limits;
using ::std::string;

using RsrcList = ::std::set<ResourceId>;

static const TChar		k_dirName[]	= _T(".");
static const RsrcString	k_humanUri	= convertToRsrcChar("http://example.org/#Human");
static const RsrcString	k_dogUri		= convertToRsrcChar("http://example.org/#Dog");
static const RsrcString	k_catUri		= convertToRsrcChar("http://example.org/#Cat");
static const RsrcString	k_mammalUri	= convertToRsrcChar("http://example.org/#Mammal");
static const RsrcString	k_animalUri	= convertToRsrcChar("http://example.org/#Animal");
static const RsrcString	k_dickUri	= convertToRsrcChar("http://example.org/#Dick");
static const RsrcString	k_janeUri	= convertToRsrcChar("http://example.org/#Jane");
static const RsrcString	k_spotUri	= convertToRsrcChar("http://example.org/#Spot");
static const RsrcString	k_puffUri	= convertToRsrcChar("http://example.org/#Puff");
static const RsrcString	k_plainText	= convertToRsrcChar("\"Mike Dean\"");
static const RsrcString	k_typedText	= convertToRsrcChar("\"Mike Dean\"^^http://www.w3.org/2001/XMLSchema#string");
static const RsrcString	k_langText1	= convertToRsrcChar("\"Mike Dean\"@en-us");
static const RsrcString	k_langText2	= convertToRsrcChar("\"Mike Dean\"@en-US");

static Config createTestConfig(bool withInference)
{
	Config config;
	config.kbDirectoryPath(k_dirName);

	if (!withInference)
	{
		config.disableAllRules();
	}
	return config;
}

static void findInstances(KbInstance& kb, ResourceId classRsrcId,
	const RsrcList& expectedResults)
{
	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();

	RsrcList actualResults;
	StmtIterator end = kb.end();
	for (StmtIterator it = kb.find(k_nullRsrcId, rdfTypeRsrcId, classRsrcId);
		it != end; ++it)
	{
		actualResults.insert(it.statement().getSubjectId());
	}

	BOOST_CHECK_EQUAL(actualResults.size(), expectedResults.size());
	for (auto it1 = cBegin(actualResults), it2 = cBegin(expectedResults);
		it1 != cEnd(actualResults) && it2 != cEnd(expectedResults);
		++it1, ++it2)
	{
		BOOST_CHECK_EQUAL(*it1, *it2);
	}
}

static void checkStmtAndRsrcCounts(const KbInstance& kb)
{
	BOOST_CHECK(kb.rsrcCount() >= 12u);
	size_t stmtCount = kb.stmtCount();
	size_t totalStmts;
	size_t numDelStmts;
	size_t numInferredStmts;
	size_t numDelAndInferredStmts;
	size_t numHidden;
	size_t numVirtual;
	kb.countStmts(totalStmts, numDelStmts, numInferredStmts, numDelAndInferredStmts, numHidden, numVirtual);
	BOOST_CHECK_EQUAL(stmtCount, totalStmts);
	BOOST_CHECK_EQUAL(totalStmts, 20u);
	BOOST_CHECK_EQUAL(numDelStmts, 1u);
	BOOST_CHECK_EQUAL(numInferredStmts, 11u);
	BOOST_CHECK_EQUAL(numDelAndInferredStmts, 0u);
}

static void runCreateTest(Config& config)
{
	config.readOnly(false);
	KbInstance kb(config);

	ResourceId rdfsSubClassOfRsrcId	= kb.uriLib().m_rdfsSubClassOf.id();
	ResourceId rdfTypeRsrcId			= kb.uriLib().m_rdfType.id();
	ResourceId humanRsrcId				= kb.uriToRsrcId(k_humanUri, false, true);
	ResourceId dogRsrcId					= kb.uriToRsrcId(k_dogUri, false, true);
	ResourceId catRsrcId					= kb.uriToRsrcId(k_catUri, false, true);
	ResourceId mammalRsrcId				= kb.uriToRsrcId(k_mammalUri, false, true);
	ResourceId animalRsrcId				= kb.uriToRsrcId(k_animalUri, false, true);
	ResourceId dickRsrcId				= kb.uriToRsrcId(k_dickUri, false, true);
	ResourceId janeRsrcId				= kb.uriToRsrcId(k_janeUri, false, true);
	ResourceId spotRsrcId				= kb.uriToRsrcId(k_spotUri, false, true);
	ResourceId puffRsrcId				= kb.uriToRsrcId(k_puffUri, false, true);

	// Set up some classes:
	kb.addStmt(mammalRsrcId, rdfsSubClassOfRsrcId, animalRsrcId, false);
	kb.addStmt(humanRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId, false);
	kb.addStmt(dogRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId, false);
	kb.addStmt(catRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId, false);

	// Create some instances:
	StatementId origStmtId = kb.addStmt(dickRsrcId, rdfTypeRsrcId, humanRsrcId, false);
	kb.addStmt(janeRsrcId, rdfTypeRsrcId, humanRsrcId, false);
	kb.addStmt(spotRsrcId, rdfTypeRsrcId, dogRsrcId, false);
	StatementId stmtIdToDelAndRecreate = kb.addStmt(
		puffRsrcId, rdfTypeRsrcId, catRsrcId, false);

	// Delete and re-create an instance:
	RsrcList emptySet;
	RsrcList setOfPuff;
	setOfPuff.insert(puffRsrcId);
	findInstances(kb, catRsrcId, setOfPuff);
	kb.deleteStmt(puffRsrcId, rdfTypeRsrcId, catRsrcId);
	findInstances(kb, catRsrcId, emptySet);
	StatementId recreatedStmtId = kb.addStmt(
		puffRsrcId, rdfTypeRsrcId, catRsrcId, false);
	BOOST_CHECK_EQUAL(recreatedStmtId, stmtIdToDelAndRecreate);
	findInstances(kb, catRsrcId, setOfPuff);

	// Create and delete an instance:
	/* StatementId stmtIdToDel = */ kb.addStmt(puffRsrcId, rdfTypeRsrcId, humanRsrcId, false);
	kb.deleteStmt(puffRsrcId, rdfTypeRsrcId, humanRsrcId);

	// Create a duplicate:
	size_t stmtCountBefore = kb.stmtCount();
	StatementId newStmtId = kb.addStmt(dickRsrcId, rdfTypeRsrcId, humanRsrcId, false);
	BOOST_CHECK_EQUAL(newStmtId, origStmtId);
	BOOST_CHECK_EQUAL(stmtCountBefore, kb.stmtCount());

	RsrcList peopleSet;
	peopleSet.insert(dickRsrcId);
	peopleSet.insert(janeRsrcId);
	findInstances(kb, humanRsrcId, peopleSet);

	checkStmtAndRsrcCounts(kb);
}

static void runOpenTest(Config& config)
{
	config.readOnly(true);
	KbInstance kb(config);

	ResourceId humanRsrcId	= kb.uriToRsrcId(k_humanUri, false, false);
	ResourceId dickRsrcId	= kb.uriToRsrcId(k_dickUri, false, false);
	ResourceId janeRsrcId	= kb.uriToRsrcId(k_janeUri, false, false);

	RsrcList peopleSet;
	peopleSet.insert(dickRsrcId);
	peopleSet.insert(janeRsrcId);
	findInstances(kb, humanRsrcId, peopleSet);

	checkStmtAndRsrcCounts(kb);
}

static void runRsrcMethodTests(bool normalizeTypedStringLiterals)
{
	Config config = createTestConfig(false);
	config.normalizeTypedStringLiterals(normalizeTypedStringLiterals);
	KbDeleter deleter(config);
	KbInstance kb(config);
	RsrcString rdfTypeUri = kb.uriLib().m_rdfType.str();

	BOOST_CHECK_EQUAL(kb.rsrcCount(), 0ul);
	BOOST_CHECK_EQUAL(kb.averageRsrcLength(), 0.0);

	ResourceId rdfType = kb.uriToRsrcId(rdfTypeUri, false, false);
	BOOST_CHECK_EQUAL(rdfType, k_nullRsrcId);
	rdfType = kb.uriToRsrcId(rdfTypeUri.c_str(), rdfTypeUri.length(), false, true);
	BOOST_CHECK_NE(rdfType, k_nullRsrcId);
	ResourceId rdfType2 = kb.uriToRsrcId(rdfTypeUri.c_str(), false, false);
	BOOST_CHECK_EQUAL(rdfType, rdfType2);
	rdfType2 = kb.uriToRsrcId(rdfTypeUri, false, true);
	BOOST_CHECK_EQUAL(rdfType, rdfType2);
	const RsrcChar* pRsrc = kb.rsrcIdToUri(rdfType2);
	BOOST_CHECK(rdfTypeUri == pRsrc);

	BOOST_CHECK_EQUAL(kb.rsrcCount(), 1ul);
	BOOST_CHECK_EQUAL(kb.averageRsrcLength(), rdfTypeUri.length());

	ResourceId anonId = kb.createAnonymousRsrc();
	BOOST_CHECK_NE(anonId, k_nullRsrcId);
	BOOST_CHECK_NE(anonId, rdfType);
	pRsrc = kb.rsrcIdToUri(anonId);
	BOOST_CHECK(nullptr == pRsrc);

	BOOST_CHECK_EQUAL(kb.rsrcCount(), 2ul);
	BOOST_CHECK_EQUAL(kb.averageRsrcLength(), rdfTypeUri.length() / 2.0);

	BOOST_CHECK(kb.isRsrcValid(rdfType));
	BOOST_CHECK(!kb.isRsrcAnonymous(rdfType));
	BOOST_CHECK(kb.isRsrcValid(anonId));
	BOOST_CHECK(kb.isRsrcAnonymous(anonId));

	// Check normalization of string-typed literals:
	ResourceId plainLit = kb.uriToRsrcId(k_plainText, true, true);
	ResourceId typedLit = kb.uriToRsrcId(k_typedText, true, true);
	BOOST_CHECK_NE(plainLit, k_nullRsrcId);
	BOOST_CHECK(kb.isRsrcValid(plainLit));
	BOOST_CHECK(!kb.isRsrcAnonymous(plainLit));
	BOOST_CHECK_NE(typedLit, k_nullRsrcId);
	BOOST_CHECK(kb.isRsrcValid(typedLit));
	BOOST_CHECK(!kb.isRsrcAnonymous(typedLit));
	pRsrc = kb.rsrcIdToUri(plainLit);
	BOOST_CHECK(k_plainText == pRsrc);
	pRsrc = kb.rsrcIdToUri(typedLit);
	if (kb.config().normalizeTypedStringLiterals())
	{
		BOOST_CHECK_EQUAL(plainLit, typedLit);
		BOOST_CHECK_EQUAL(kb.rsrcCount(), 3ul);
		BOOST_CHECK_EQUAL(kb.averageRsrcLength(),
			(rdfTypeUri.length() + k_plainText.length()) / 3.0);
		BOOST_CHECK(k_plainText == pRsrc);
	}
	else
	{
		BOOST_CHECK_NE(plainLit, typedLit);
		BOOST_CHECK_EQUAL(kb.rsrcCount(), 4ul);
		BOOST_CHECK_EQUAL(kb.averageRsrcLength(),
			(rdfTypeUri.length() + k_plainText.length() + k_typedText.length()) / 4.0);
		BOOST_CHECK(k_typedText == pRsrc);
	}

	// Check normalization of language-tagged literals:
	ResourceId langLit1 = kb.uriToRsrcId(k_langText1, true, true);
	ResourceId langLit2 = kb.uriToRsrcId(k_langText2, true, true);
	BOOST_CHECK_NE(langLit1, k_nullRsrcId);
	BOOST_CHECK(kb.isRsrcValid(langLit1));
	BOOST_CHECK(!kb.isRsrcAnonymous(langLit1));
	BOOST_CHECK_NE(langLit2, k_nullRsrcId);
	BOOST_CHECK(kb.isRsrcValid(langLit2));
	BOOST_CHECK(!kb.isRsrcAnonymous(langLit2));
	pRsrc = kb.rsrcIdToUri(langLit1);
	BOOST_CHECK(k_langText1 == pRsrc);
	pRsrc = kb.rsrcIdToUri(langLit2);
	if (kb.config().normalizeTypedStringLiterals())
	{
		BOOST_CHECK_EQUAL(langLit1, langLit2);
		BOOST_CHECK_EQUAL(kb.rsrcCount(), 4ul);
		BOOST_CHECK_EQUAL(kb.averageRsrcLength(),
			(rdfTypeUri.length() + k_plainText.length() + k_langText1.length()) / 4.0);
		BOOST_CHECK(k_langText1 == pRsrc);
	}
	else
	{
		BOOST_CHECK_NE(langLit1, langLit2);
		BOOST_CHECK_EQUAL(kb.rsrcCount(), 6ul);
		BOOST_CHECK_EQUAL(kb.averageRsrcLength(),
			(rdfTypeUri.length() + k_plainText.length() + k_typedText.length()
				+ k_langText1.length() + k_langText2.length()) / 6.0);
		BOOST_CHECK(k_langText2 == pRsrc);
	}
}

BOOST_AUTO_TEST_SUITE(KbInstanceTestSuite)

BOOST_AUTO_TEST_CASE(testByQuickOverview)
{
	Config config = createTestConfig(true);
	KbDeleter deleter(config);

	runCreateTest(config);
	runOpenTest(config);
}

BOOST_AUTO_TEST_CASE(testKbDeletion)
{
	Config config = createTestConfig(false);
}

BOOST_AUTO_TEST_CASE(testDisposition)
{
	Config config = createTestConfig(false);
}

BOOST_AUTO_TEST_CASE(testRsrcMethods)
{
	runRsrcMethodTests(false);
	runRsrcMethodTests(true);
}

BOOST_AUTO_TEST_CASE(testDumpKbAsNTriples)
{
	runRsrcMethodTests(false);
	runRsrcMethodTests(true);
}

BOOST_AUTO_TEST_SUITE_END()
