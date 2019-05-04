// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <boost/format.hpp>
#include <boost/test/unit_test.hpp>
#include <boost/test/data/monomorphic.hpp>
#include <boost/test/data/test_case.hpp>
#include <cstdio>
#include <exception>
#include <limits>
#include <set>
#include <sstream>
#include <string>
#include "parliament/ArrayLength.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/KbConfig.h"
#include "parliament/KbInstance.h"
#include "parliament/StmtIterator.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/UriLib.h"
#include "parliament/Util.h"
#include "TestUtils.h"

namespace bdata = ::boost::unit_test::data;

using namespace ::bbn::parliament;
using ::boost::format;
using ::std::exception;
using ::std::numeric_limits;
using ::std::string;

using RsrcList = ::std::set<ResourceId>;

static const TChar		k_dirName[]	= _T("test-kb-data");
static const RsrcString	k_humanUri	= convertToRsrcChar("http://example.org/#Human");
static const RsrcString	k_dogUri		= convertToRsrcChar("http://example.org/#Dog");
static const RsrcString	k_catUri		= convertToRsrcChar("http://example.org/#Cat");
static const RsrcString	k_mammalUri	= convertToRsrcChar("http://example.org/#Mammal");
static const RsrcString	k_animalUri	= convertToRsrcChar("http://example.org/#Animal");
static const RsrcString	k_dickUri	= convertToRsrcChar("http://example.org/#Dick");
static const RsrcString	k_janeUri	= convertToRsrcChar("http://example.org/#Jane");
static const RsrcString	k_spotUri	= convertToRsrcChar("http://example.org/#Spot");
static const RsrcString	k_puffUri	= convertToRsrcChar("http://example.org/#Puff");
static const RsrcString	k_mikeUri	= convertToRsrcChar("http://example.org/#Mike");
static const RsrcString	k_cNameUri	= convertToRsrcChar("http://example.org/#canonicalName");
static const RsrcString	k_plainText	= convertToRsrcChar("\"Mike Dean\"");
static const RsrcString	k_typedText	= convertToRsrcChar("\"Mike Dean\"^^http://www.w3.org/2001/XMLSchema#string");
static const RsrcString	k_langText1	= convertToRsrcChar("\"Mike Dean\"@en-us");
static const RsrcString	k_langText2	= convertToRsrcChar("\"Mike Dean\"@en-US");
static const RsrcString	k_integer	= convertToRsrcChar("\"1\"^^http://www.w3.org/2001/XMLSchema#nonNegativeInteger");

static const char*const k_expectedDumpLines[] =
{
	"<http://example.org/#Mike> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/#Human> .",
	"<http://example.org/#Mike> <http://www.w3.org/2000/01/rdf-schema#label> \"Mike Dean\" .",
	"<http://example.org/#Mike> <http://www.w3.org/2000/01/rdf-schema#label> \"Mike Dean\"@en-us .",
	"<http://example.org/#Human> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> .",
	"<http://example.org/#Human> <http://www.w3.org/2000/01/rdf-schema#subClassOf> _:bn%|1$08x| .",
	"_:bn%|1$08x| <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Restriction> .",
	"_:bn%|1$08x| <http://www.w3.org/2002/07/owl#onProperty> <http://example.org/#canonicalName> .",
	"_:bn%|1$08x| <http://www.w3.org/2002/07/owl#maxCardinality> \"1\"^^<http://www.w3.org/2001/XMLSchema#nonNegativeInteger> .",
};

static KbConfig createTestConfig(bool withInference)
{
	KbConfig config;
	config.kbDirectoryPath(k_dirName);

	if (!withInference)
	{
		config.disableAllRules();
	}
	return config;
}

template<typename T>
static void checkSetsEqual(const ::std::set<T>& expectedSet, const ::std::set<T>& actualSet)
{
	BOOST_CHECK_EQUAL(expectedSet.size(), actualSet.size());
	for (auto it1 = cBegin(expectedSet), it2 = cBegin(actualSet);
		it1 != cEnd(expectedSet) && it2 != cEnd(actualSet);
		++it1, ++it2)
	{
		BOOST_CHECK_EQUAL(*it1, *it2);
	}
}

static RsrcList findMatchingSubjects(KbInstance& kb, ResourceId predicateId, ResourceId objectId)
{
	RsrcList results;
	auto end = kb.end();
	for (auto it = kb.find(k_nullRsrcId, predicateId, objectId); it != end; ++it)
	{
		results.insert(it.statement().getSubjectId());
	}
	return results;
}

static RsrcList findMatchingObjects(KbInstance& kb, ResourceId subjectId, ResourceId predicateId)
{
	RsrcList results;
	auto end = kb.end();
	for (auto it = kb.find(subjectId, predicateId, k_nullRsrcId); it != end; ++it)
	{
		results.insert(it.statement().getObjectId());
	}
	return results;
}

static void findInstances(KbInstance& kb, ResourceId classRsrcId,
	const RsrcList& expectedResults)
{
	auto actualResults{findMatchingSubjects(kb, kb.uriLib().m_rdfType.id(), classRsrcId)};
	checkSetsEqual(expectedResults, actualResults);
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

static void runCreateTest(KbConfig& config)
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

static void runOpenTest(KbConfig& config)
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

BOOST_AUTO_TEST_SUITE(KbInstanceTestSuite)

BOOST_AUTO_TEST_CASE(testByQuickOverview)
{
	KbConfig config = createTestConfig(true);
	KbDeleter deleter(config, true);

	runCreateTest(config);
	runOpenTest(config);
}

BOOST_AUTO_TEST_CASE(testKbDeletion)
{
	KbConfig config = createTestConfig(false);
}

BOOST_AUTO_TEST_CASE(testDisposition)
{
	KbConfig config = createTestConfig(false);
}

BOOST_DATA_TEST_CASE(
	testRsrcMethods,
	bdata::make({ true, false }),
	normalizeTypedStringLiterals)
{
	KbConfig config = createTestConfig(false);
	config.normalizeTypedStringLiterals(normalizeTypedStringLiterals);
	KbDeleter deleter(config, true);
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

BOOST_AUTO_TEST_CASE(testDumpKbAsNTriples)
{
	auto config = createTestConfig(true);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	auto rdfTypeRsrcId			= kb.uriLib().m_rdfType.id();
	auto rdfsLabelRsrcId		= kb.uriLib().m_rdfsLabel.id();
	auto rdfsSubClassOfRsrcId	= kb.uriLib().m_rdfsSubClassOf.id();
	auto owlClassRsrcId			= kb.uriLib().m_owlClass.id();
	auto owlRestrictionRsrcId	= kb.uriLib().m_owlRestriction.id();
	auto owlOnPropRsrcId		= kb.uriLib().m_owlOnProp.id();
	auto owlMaxCardRsrcId		= kb.uriLib().m_owlMaxCard.id();

	auto humanRsrcId				= kb.uriToRsrcId(k_humanUri, false, true);
	auto restrictionRsrcId		= kb.createAnonymousRsrc();
	auto cNameRsrcId				= kb.uriToRsrcId(k_cNameUri, false, true);
	auto mikeRsrcId				= kb.uriToRsrcId(k_mikeUri, false, true);

	auto plainLit					= kb.uriToRsrcId(k_plainText, true, true);
	auto typedLit					= kb.uriToRsrcId(k_typedText, true, true);
	auto langLit					= kb.uriToRsrcId(k_langText1, true, true);
	auto intLit					= kb.uriToRsrcId(k_integer, true, true);

	kb.addStmt(mikeRsrcId, rdfTypeRsrcId, humanRsrcId, false);
	kb.addStmt(mikeRsrcId, rdfsLabelRsrcId, plainLit, false);
	kb.addStmt(mikeRsrcId, rdfsLabelRsrcId, typedLit, false);
	kb.addStmt(mikeRsrcId, rdfsLabelRsrcId, langLit, false);

	kb.addStmt(humanRsrcId, rdfTypeRsrcId, owlClassRsrcId, false);
	kb.addStmt(humanRsrcId, rdfsSubClassOfRsrcId, restrictionRsrcId, false);
	kb.addStmt(restrictionRsrcId, rdfTypeRsrcId, owlRestrictionRsrcId, false);
	kb.addStmt(restrictionRsrcId, owlOnPropRsrcId, cNameRsrcId, false);
	kb.addStmt(restrictionRsrcId, owlMaxCardRsrcId, intLit, false);

	::std::ostringstream out;
	kb.dumpKbAsNTriples(out, InferredStmtsAction::exclude, DeletedStmtsAction::exclude, EncodingCharSet::utf8);
	out.flush();
	auto dump = out.str();

	BOOST_TEST_MESSAGE(
		"======= Output from dumpKbAsNTriples: =======\r\n"
		<< dump <<
		"=============================================");

	::std::set<string> actualLineSet;
	for (::std::istringstream in(dump); !in.eof();)
	{
		if (!in)
		{
			throw ::std::runtime_error("IO error reading from data file");
		}
		else
		{
			string line;
			::std::getline(in, line);
			if (!line.empty())
			{
				actualLineSet.insert(line);
			}
		}
	}

	::std::set<string> expectedLineSet;
	for (auto i = 0u; i < arrayLen(k_expectedDumpLines); ++i)
	{
		string expectedDumpLine{k_expectedDumpLines[i]};
		format expectedDumpLineFmt{expectedDumpLine};
		if (expectedDumpLine.find("%|1$") != string::npos)
		{
			expectedDumpLineFmt % restrictionRsrcId;
		}
		expectedLineSet.insert(str(expectedDumpLineFmt));
	}

	checkSetsEqual(expectedLineSet, actualLineSet);
}

BOOST_AUTO_TEST_CASE(testReservedPredicates)
{
	KbConfig config = createTestConfig(true);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	ResourceId rdfTypeRsrcId					= kb.uriLib().m_rdfType.id();
	ResourceId owlClass							= kb.uriLib().m_owlClass.id();
	ResourceId rdfsSubClassOfRsrcId			= kb.uriLib().m_rdfsSubClassOf.id();
	ResourceId parDirectSubClassOfRsrcId	= kb.uriLib().m_parDirectSubClassOf.id();
	ResourceId parDirectTypeRsrcId			= kb.uriLib().m_parDirectType.id();
	ResourceId humanRsrcId						= kb.uriToRsrcId(k_humanUri, false, true);
	ResourceId mammalRsrcId						= kb.uriToRsrcId(k_mammalUri, false, true);
	ResourceId animalRsrcId						= kb.uriToRsrcId(k_animalUri, false, true);
	ResourceId janeRsrcId						= kb.uriToRsrcId(k_janeUri, false, true);

	// Check that the reserved predicates cannot be inserted:
	BOOST_CHECK_THROW(
		kb.addStmt(parDirectSubClassOfRsrcId, rdfTypeRsrcId, animalRsrcId, false),
		Exception);
	BOOST_CHECK_THROW(
		kb.addStmt(mammalRsrcId, parDirectSubClassOfRsrcId, animalRsrcId, false),
		Exception);
	BOOST_CHECK_THROW(
		kb.addStmt(janeRsrcId, rdfTypeRsrcId, parDirectSubClassOfRsrcId, false),
		Exception);
	BOOST_CHECK_THROW(
		kb.addStmt(parDirectTypeRsrcId, rdfTypeRsrcId, animalRsrcId, false),
		Exception);
	BOOST_CHECK_THROW(
		kb.addStmt(janeRsrcId, parDirectTypeRsrcId, animalRsrcId, false),
		Exception);
	BOOST_CHECK_THROW(
		kb.addStmt(janeRsrcId, rdfTypeRsrcId, parDirectTypeRsrcId, false),
		Exception);

	// Set up some classes and an instance:
	kb.addStmt(humanRsrcId, rdfTypeRsrcId, owlClass, false);
	kb.addStmt(mammalRsrcId, rdfTypeRsrcId, owlClass, false);
	kb.addStmt(animalRsrcId, rdfTypeRsrcId, owlClass, false);
	kb.addStmt(mammalRsrcId, rdfsSubClassOfRsrcId, animalRsrcId, false);
	kb.addStmt(humanRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId, false);
	kb.addStmt(janeRsrcId, rdfTypeRsrcId, humanRsrcId, false);

	// Check reserved predicates:
	auto actualJanesTypes = findMatchingObjects(kb, janeRsrcId, rdfTypeRsrcId);
	auto actualJanesDirectTypes = findMatchingObjects(kb, janeRsrcId, parDirectTypeRsrcId);
	auto actualHumanSuperClasses = findMatchingObjects(kb, humanRsrcId, rdfsSubClassOfRsrcId);
	auto actualDirectHumanSuperClasses = findMatchingObjects(kb, humanRsrcId, parDirectSubClassOfRsrcId);

	RsrcList expectedJanesTypes;
	expectedJanesTypes.insert(humanRsrcId);
	expectedJanesTypes.insert(mammalRsrcId);
	expectedJanesTypes.insert(animalRsrcId);
	checkSetsEqual(expectedJanesTypes, actualJanesTypes);

	RsrcList expectedJanesDirectTypes;
	expectedJanesDirectTypes.insert(humanRsrcId);
	checkSetsEqual(expectedJanesDirectTypes, actualJanesDirectTypes);

	RsrcList expectedHumanSuperClasses;
	expectedHumanSuperClasses.insert(humanRsrcId);
	expectedHumanSuperClasses.insert(mammalRsrcId);
	expectedHumanSuperClasses.insert(animalRsrcId);
	checkSetsEqual(expectedHumanSuperClasses, actualHumanSuperClasses);

	RsrcList expectedDirectHumanSuperClasses;
	expectedDirectHumanSuperClasses.insert(mammalRsrcId);
	checkSetsEqual(expectedDirectHumanSuperClasses, actualDirectHumanSuperClasses);

	// Check statement counts:
	BOOST_CHECK_EQUAL(0u, kb.subjectCount(parDirectSubClassOfRsrcId));
	BOOST_CHECK_EQUAL(kb.predicateCount(rdfsSubClassOfRsrcId),
		kb.predicateCount(parDirectSubClassOfRsrcId));
	BOOST_CHECK_EQUAL(0u, kb.objectCount(parDirectSubClassOfRsrcId));
	BOOST_CHECK_EQUAL(0u, kb.subjectCount(parDirectTypeRsrcId));
	BOOST_CHECK_EQUAL(kb.predicateCount(rdfTypeRsrcId),
		kb.predicateCount(parDirectTypeRsrcId));
	BOOST_CHECK_EQUAL(0u, kb.objectCount(parDirectTypeRsrcId));
}

BOOST_AUTO_TEST_SUITE_END()
