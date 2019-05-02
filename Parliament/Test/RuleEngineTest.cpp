// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

// TODO: Test SWRLTrigger Rule

#include <boost/test/unit_test.hpp>
#include <boost/test/data/monomorphic.hpp>
#include <boost/test/data/test_case.hpp>
#include <ostream>
#include "parliament/RuleEngine.h"
#include "parliament/KbConfig.h"
#include "parliament/KbInstance.h"
#include "parliament/StmtIterator.h"
#include "parliament/SubclassRule.h"
#include "parliament/SubpropRule.h"
#include "parliament/DomainRule.h"
#include "parliament/RangeRule.h"
#include "parliament/EquivalentClassRule.h"
#include "parliament/EquivalentPropRule.h"
#include "parliament/InverseOfRule.h"
#include "parliament/Log.h"
#include "parliament/SymmetricPropRule.h"
#include "parliament/FuncPropRule.h"
#include "parliament/InvFuncPropRule.h"
#include "parliament/TransitivePropRule.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/CharacterLiteral.h"
#include "TestUtils.h"

namespace bdata = ::boost::unit_test::data;

using namespace ::bbn::parliament;
using ::std::make_shared;
using ::std::ostream;

enum class RuleInitPoint
	{ k_beginning, k_middle, k_middle2, k_middle3, k_middle4, k_middle5, k_end };
static const char*const k_initPointLbls[] =
	{ "beginning", "middle", "middle2", "middle3", "middle4", "middle5", "end" };

struct RuleTestParams
{
	RuleInitPoint	m_initPoint;
	bool				m_assertInReverse;
};

// Required for BOOST_DATA_TEST_CASE:
static ostream& operator<<(ostream& os, const RuleTestParams& rtp)
{
	os << "Init point '" << k_initPointLbls[static_cast<int>(rtp.m_initPoint)]
		<< "', asserted in " << (rtp.m_assertInReverse ? "reverse" : "order");
	return os;
}

enum class RuleEnablement
	{ k_allDisabled, k_default, k_allEnabled };
static const char*const k_ruleEnablementLbls[] =
	{ "No", "Default", "All" };
struct RuleConfigTestParams
{
	RuleEnablement	m_ruleEnablement;
	size_t			m_expectedNumStmts;
};
static const RuleConfigTestParams k_ruleConfigTestParams[] =
{
	{ RuleEnablement::k_allDisabled, 3u },
	{ RuleEnablement::k_default, 6u },
	{ RuleEnablement::k_allEnabled, 29u },
};

// Required for BOOST_DATA_TEST_CASE:
static ostream& operator<<(ostream& os, const RuleConfigTestParams& rtci)
{
	os << k_ruleEnablementLbls[static_cast<int>(rtci.m_ruleEnablement)]
		<< " rules enabled, expecting "  << rtci.m_expectedNumStmts << " statements";
	return os;
}

static const TChar		k_dirName[] = _T("test-kb-data");

static const RsrcString	k_personRsrc				= convertToRsrcChar("http://example.org/#Person");
static const RsrcString	k_humanRsrc					= convertToRsrcChar("http://example.org/#Human");
static const RsrcString	k_dogRsrc					= convertToRsrcChar("http://example.org/#Dog");
static const RsrcString	k_mammalRsrc				= convertToRsrcChar("http://example.org/#Mammal");
static const RsrcString	k_animalRsrc				= convertToRsrcChar("http://example.org/#Animal");
static const RsrcString	k_redWineRsrc				= convertToRsrcChar("http://example.org/#RedWine");

static const RsrcString	k_hasColorRsrc				= convertToRsrcChar("http://example.org/#hasColor");
static const RsrcString	k_hasFurTypeRsrc			= convertToRsrcChar("http://example.org/#hasFurType");
static const RsrcString	k_testPropRsrc				= convertToRsrcChar("http://example.org/#testProp");
static const RsrcString	k_hasChildRsrc				= convertToRsrcChar("http://example.org/#hasChild");
static const RsrcString	k_hasKidRsrc				= convertToRsrcChar("http://example.org/#hasKid");
static const RsrcString	k_hasParentRsrc			= convertToRsrcChar("http://example.org/#hasParent");
static const RsrcString	k_hasSiblingRsrc			= convertToRsrcChar("http://example.org/#hasSibling");
static const RsrcString	k_hasBirthMotherRsrc		= convertToRsrcChar("http://example.org/#hasBirthMother");
static const RsrcString	k_hasSSNRsrc				= convertToRsrcChar("http://example.org/#hasSSN");
static const RsrcString	k_isContainedInRsrc		= convertToRsrcChar("http://example.org/#isContainedIn");

static const RsrcString	k_fidoRsrc					= convertToRsrcChar("http://example.org/#fido");
static const RsrcString	k_queenElizabethRsrc		= convertToRsrcChar("http://example.org/#queenElizabeth");
static const RsrcString	k_queenElizabethLitRsrc	= convertToRsrcChar("\"queenElizabeth\"");
static const RsrcString	k_princeCharlesRsrc		= convertToRsrcChar("http://example.org/#princeCharles");
static const RsrcString	k_princeWilliamRsrc		= convertToRsrcChar("http://example.org/#princeWilliam");
static const RsrcString	k_princeHarryRsrc			= convertToRsrcChar("http://example.org/#princeHarry");
static const RsrcString	k_someKidRsrc				= convertToRsrcChar("http://example.org/#someKid");
static const RsrcString	k_thisWomanRsrc			= convertToRsrcChar("http://example.org/#thisWoman");
static const RsrcString	k_thatWomanRsrc			= convertToRsrcChar("http://example.org/#thatWoman");
static const RsrcString	k_thisWomanLitRsrc		= convertToRsrcChar("\"thisWoman\"");
static const RsrcString	k_thatWomanLitRsrc		= convertToRsrcChar("\"thatWoman\"");
static const RsrcString	k_thisPersonRsrc			= convertToRsrcChar("http://example.org/#thisPerson");
static const RsrcString	k_thatPersonRsrc			= convertToRsrcChar("http://example.org/#thatPerson");
static const RsrcString	k_rosslynRsrc				= convertToRsrcChar("http://example.org/#rosslyn");
static const RsrcString	k_arlingtonRsrc			= convertToRsrcChar("http://example.org/#arlington");
static const RsrcString	k_virginiaRsrc				= convertToRsrcChar("http://example.org/#virginia");
static const RsrcString	k_usaRsrc					= convertToRsrcChar("http://example.org/#usa");
static const RsrcString	k_northAmericaRsrc		= convertToRsrcChar("http://example.org/#northAmerica");
static const RsrcString	k_mondaviPrivateReserveCabernetSauvignonRsrc
																= convertToRsrcChar("http://example.org/#mondaviPrivateReserveCabernetSauvignon");
static const RsrcString	k_blackLitRsrc				= convertToRsrcChar("\"black\"");
static const RsrcString	k_redLitRsrc				= convertToRsrcChar("\"red\"");
static const RsrcString	k_ssnRsrc					= convertToRsrcChar("http://example.org/#999-99-9999");
static const RsrcString	k_ssnLitRsrc				= convertToRsrcChar("\"999-99-9999\"");

static const RuleTestParams k_subclassRuleTestParams[] =
	{
		{ RuleInitPoint::k_beginning, false },
		{ RuleInitPoint::k_middle, false },
		{ RuleInitPoint::k_end, false }
	};

static const RuleTestParams k_inverseOfRuleTestParams[] =
	{
		{ RuleInitPoint::k_beginning, false },
		{ RuleInitPoint::k_end, false },
		{ RuleInitPoint::k_beginning, true },
		{ RuleInitPoint::k_end, true }
	};

static const RuleTestParams k_multiAtomBodyRuleTestParams[] =
	{
		{ RuleInitPoint::k_beginning, false },
		{ RuleInitPoint::k_middle, false },
		{ RuleInitPoint::k_middle2, false },
		{ RuleInitPoint::k_middle3, false },
		{ RuleInitPoint::k_middle4, false },
		{ RuleInitPoint::k_middle5, false }
	};

static const RuleTestParams k_otherRuleTestParams[] =
	{
		{ RuleInitPoint::k_beginning, false },
		{ RuleInitPoint::k_end, false }
	};

static auto g_log{log::getSource("RuleEngineTest")};

static KbConfig createTestConfig(RuleEnablement ruleEnablement)
{
	KbConfig config;
	config.kbDirectoryPath(k_dirName);
	if (ruleEnablement == RuleEnablement::k_allDisabled)
	{
		config.disableAllRules();
	}
	else if (ruleEnablement == RuleEnablement::k_allEnabled)
	{
		config.enableAllRules();
	}
	return config;
}

BOOST_AUTO_TEST_SUITE(RuleEngineTestSuite)

static bool isEntailed(const KbInstance& kb, ResourceId subjectId, ResourceId predicateId, ResourceId objectId)
{
	StmtIterator iter = kb.find(subjectId, predicateId, objectId);
	return iter != kb.end();
}

static void assertEntailments(KbInstance& kb, bool includeFinalEntailments,
	ResourceId rdfsSubClassOfRsrcId, ResourceId rdfTypeRsrcId, ResourceId dogRsrcId,
	ResourceId mammalRsrcId, ResourceId animalRsrcId, ResourceId fidoRsrcId)
{
	ResourceId rdfsClassRsrcId = kb.uriLib().m_rdfsClass.id();
	ResourceId owlClassRsrcId = kb.uriLib().m_owlClass.id();
	ResourceId rdfsResourceRsrcId = kb.uriLib().m_rdfsResource.id();
	ResourceId owlThingRsrcId = kb.uriLib().m_owlThing.id();

	BOOST_REQUIRE(k_nullRsrcId != rdfsClassRsrcId);
	BOOST_REQUIRE(k_nullRsrcId != owlClassRsrcId);
	BOOST_REQUIRE(k_nullRsrcId != rdfsResourceRsrcId);
	BOOST_REQUIRE(k_nullRsrcId != owlThingRsrcId);

	BOOST_CHECK(isEntailed(kb, owlThingRsrcId, rdfTypeRsrcId, rdfsClassRsrcId));
	BOOST_CHECK(isEntailed(kb, owlThingRsrcId, rdfTypeRsrcId, owlClassRsrcId));
	BOOST_CHECK(isEntailed(kb, owlThingRsrcId, rdfsSubClassOfRsrcId, owlThingRsrcId));
	BOOST_CHECK(isEntailed(kb, rdfsResourceRsrcId, rdfTypeRsrcId, rdfsClassRsrcId));
	BOOST_CHECK(isEntailed(kb, rdfsResourceRsrcId, rdfTypeRsrcId, owlClassRsrcId));
	BOOST_CHECK(isEntailed(kb, rdfsResourceRsrcId, rdfsSubClassOfRsrcId, rdfsResourceRsrcId));

	BOOST_CHECK(isEntailed(kb, dogRsrcId, rdfTypeRsrcId, rdfsClassRsrcId));
	BOOST_CHECK(isEntailed(kb, dogRsrcId, rdfTypeRsrcId, owlClassRsrcId));
	BOOST_CHECK(isEntailed(kb, dogRsrcId, rdfsSubClassOfRsrcId, dogRsrcId));
	BOOST_CHECK(isEntailed(kb, dogRsrcId, rdfsSubClassOfRsrcId, rdfsResourceRsrcId));
	BOOST_CHECK(isEntailed(kb, dogRsrcId, rdfsSubClassOfRsrcId, owlThingRsrcId));

	BOOST_CHECK(isEntailed(kb, mammalRsrcId, rdfTypeRsrcId, rdfsClassRsrcId));
	BOOST_CHECK(isEntailed(kb, mammalRsrcId, rdfTypeRsrcId, owlClassRsrcId));
	BOOST_CHECK(isEntailed(kb, mammalRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId));
	BOOST_CHECK(isEntailed(kb, mammalRsrcId, rdfsSubClassOfRsrcId, rdfsResourceRsrcId));
	BOOST_CHECK(isEntailed(kb, mammalRsrcId, rdfsSubClassOfRsrcId, owlThingRsrcId));

	BOOST_CHECK(isEntailed(kb, animalRsrcId, rdfTypeRsrcId, rdfsClassRsrcId));
	BOOST_CHECK(isEntailed(kb, animalRsrcId, rdfTypeRsrcId, owlClassRsrcId));
	BOOST_CHECK(isEntailed(kb, animalRsrcId, rdfsSubClassOfRsrcId, animalRsrcId));
	BOOST_CHECK(isEntailed(kb, animalRsrcId, rdfsSubClassOfRsrcId, rdfsResourceRsrcId));
	BOOST_CHECK(isEntailed(kb, animalRsrcId, rdfsSubClassOfRsrcId, owlThingRsrcId));

	BOOST_CHECK(isEntailed(kb, dogRsrcId, rdfsSubClassOfRsrcId, animalRsrcId));

	// Sub-class of self:
	BOOST_CHECK(isEntailed(kb, dogRsrcId, rdfsSubClassOfRsrcId, dogRsrcId));
	BOOST_CHECK(isEntailed(kb, mammalRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId));
	BOOST_CHECK(isEntailed(kb, animalRsrcId, rdfsSubClassOfRsrcId, animalRsrcId));

	if (includeFinalEntailments)
	{
		BOOST_CHECK(isEntailed(kb, fidoRsrcId, rdfTypeRsrcId, rdfsResourceRsrcId));
		BOOST_CHECK(isEntailed(kb, fidoRsrcId, rdfTypeRsrcId, owlThingRsrcId));
		BOOST_CHECK(isEntailed(kb, fidoRsrcId, rdfTypeRsrcId, mammalRsrcId));
		BOOST_CHECK(isEntailed(kb, fidoRsrcId, rdfTypeRsrcId, animalRsrcId));

		BOOST_CHECK_EQUAL(29u, kb.stmtCount());
	}
	else
	{
		BOOST_CHECK_EQUAL(24u, kb.stmtCount());
	}
}

BOOST_DATA_TEST_CASE(
	testSubclassRules,
	bdata::make(k_subclassRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	config.inferRdfsClass(true);
	config.inferOwlClass(true);
	config.inferRdfsResource(true);
	config.inferOwlThing(true);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<SubclassRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId rdfsSubClassOfRsrcId	= kb.uriLib().m_rdfsSubClassOf.id();
	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId dogRsrcId = kb.uriToRsrcId(k_dogRsrc, false, true);
	ResourceId mammalRsrcId = kb.uriToRsrcId(k_mammalRsrc, false, true);
	ResourceId animalRsrcId = kb.uriToRsrcId(k_animalRsrc, false, true);
	ResourceId fidoRsrcId = kb.uriToRsrcId(k_fidoRsrc, false, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(mammalRsrcId, rdfsSubClassOfRsrcId, animalRsrcId, false);
	kb.addStmt(dogRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_middle)
	{
		kb.ruleEngine().addRule(make_shared<SubclassRule>(&kb, &(kb.ruleEngine())));
		assertEntailments(kb, false, rdfsSubClassOfRsrcId, rdfTypeRsrcId,
			dogRsrcId, mammalRsrcId, animalRsrcId, fidoRsrcId);
	}

	kb.addStmt(fidoRsrcId, rdfTypeRsrcId, dogRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<SubclassRule>(&kb, &(kb.ruleEngine())));
	}

	assertEntailments(kb, true, rdfsSubClassOfRsrcId, rdfTypeRsrcId,
			dogRsrcId, mammalRsrcId, animalRsrcId, fidoRsrcId);
}

BOOST_DATA_TEST_CASE(
	testSubpropertyRules,
	bdata::make(k_otherRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<SubpropRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId datatypePropRsrcId = kb.uriLib().m_owlDatatypeProp.id();
	ResourceId rdfsSubPropOfRsrcId = kb.uriLib().m_rdfsSubPropertyOf.id();
	ResourceId fidoRsrcId = kb.uriToRsrcId(k_fidoRsrc, false, true);
	ResourceId hasColorRsrcId = kb.uriToRsrcId(k_hasColorRsrc, false, true);
	ResourceId hasFurTypeRsrcId = kb.uriToRsrcId(k_hasFurTypeRsrc, false, true);
	ResourceId testPropRsrcId = kb.uriToRsrcId(k_testPropRsrc, false, true);
	ResourceId blackRsrcId = kb.uriToRsrcId(k_blackLitRsrc, true, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(hasColorRsrcId, rdfTypeRsrcId, datatypePropRsrcId, false);
	kb.addStmt(hasFurTypeRsrcId, rdfTypeRsrcId, datatypePropRsrcId, false);
	kb.addStmt(testPropRsrcId, rdfTypeRsrcId, datatypePropRsrcId, false);
	kb.addStmt(hasColorRsrcId, rdfsSubPropOfRsrcId, hasFurTypeRsrcId, false);
	kb.addStmt(hasFurTypeRsrcId, rdfsSubPropOfRsrcId, testPropRsrcId, false);
	kb.addStmt(fidoRsrcId, hasColorRsrcId, blackRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<SubpropRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, hasColorRsrcId, rdfsSubPropOfRsrcId, testPropRsrcId));
	BOOST_CHECK(isEntailed(kb, fidoRsrcId, hasFurTypeRsrcId, blackRsrcId));
	BOOST_CHECK(isEntailed(kb, fidoRsrcId, testPropRsrcId, blackRsrcId));

	// Sub-prop of self:
	BOOST_CHECK(isEntailed(kb, hasColorRsrcId, rdfsSubPropOfRsrcId, hasColorRsrcId));
	BOOST_CHECK(isEntailed(kb, hasFurTypeRsrcId, rdfsSubPropOfRsrcId, hasFurTypeRsrcId));
	BOOST_CHECK(isEntailed(kb, testPropRsrcId, rdfsSubPropOfRsrcId, testPropRsrcId));

	BOOST_CHECK_EQUAL(12u, kb.stmtCount());
}

BOOST_DATA_TEST_CASE(
	testInverseOfRules,
	bdata::make(k_inverseOfRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<InverseOfRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId owlInverseOfRsrcId = kb.uriLib().m_owlInverseOf.id();
	ResourceId queenElizabethRsrcId = kb.uriToRsrcId(k_queenElizabethRsrc, false, true);
	ResourceId princeCharlesRsrcId = kb.uriToRsrcId(k_princeCharlesRsrc, false, true);
	ResourceId hasChildRsrcId = kb.uriToRsrcId(k_hasChildRsrc, false, true);
	ResourceId hasParentRsrcId = kb.uriToRsrcId(k_hasParentRsrc, false, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(hasChildRsrcId, owlInverseOfRsrcId, hasParentRsrcId, false);
	if (rtp.m_assertInReverse)
	{
		kb.addStmt(princeCharlesRsrcId, hasParentRsrcId, queenElizabethRsrcId, false);
	}
	else
	{
		kb.addStmt(queenElizabethRsrcId, hasChildRsrcId, princeCharlesRsrcId, false);
	}

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<InverseOfRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, hasParentRsrcId, owlInverseOfRsrcId, hasChildRsrcId));
	BOOST_CHECK(isEntailed(kb, queenElizabethRsrcId, hasChildRsrcId, princeCharlesRsrcId));
	BOOST_CHECK(isEntailed(kb, princeCharlesRsrcId, hasParentRsrcId, queenElizabethRsrcId));

	BOOST_CHECK_EQUAL(4u, kb.stmtCount());
}

BOOST_DATA_TEST_CASE(
	testSymmetricPropRules,
	bdata::make(k_otherRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<SymmetricPropRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId owlSymmetricPropRsrcId = kb.uriLib().m_owlSymmetricProp.id();
	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId hasSiblingRsrcId = kb.uriToRsrcId(k_hasSiblingRsrc, false, true);
	ResourceId princeWilliamRsrcId = kb.uriToRsrcId(k_princeWilliamRsrc, false, true);
	ResourceId princeHarryRsrcId = kb.uriToRsrcId(k_princeHarryRsrc, false, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(hasSiblingRsrcId, rdfTypeRsrcId, owlSymmetricPropRsrcId, false);
	kb.addStmt(princeWilliamRsrcId, hasSiblingRsrcId, princeHarryRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<SymmetricPropRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, princeHarryRsrcId, hasSiblingRsrcId, princeWilliamRsrcId));

	BOOST_CHECK_EQUAL(3u, kb.stmtCount());
}

BOOST_DATA_TEST_CASE(
	testFuncPropRules,
	bdata::make(k_otherRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<FuncPropRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId owlSameAsRsrcId = kb.uriLib().m_owlSameAs.id();
	ResourceId owlFuncPropRsrcId = kb.uriLib().m_owlFuncProp.id();
	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId hasBirthMotherRsrcId = kb.uriToRsrcId(k_hasBirthMotherRsrc, false, true);
	ResourceId someKidRsrcId = kb.uriToRsrcId(k_someKidRsrc, false, true);
	ResourceId thisWomanRsrcId = kb.uriToRsrcId(k_thisWomanRsrc, false, true);
	ResourceId thatWomanRsrcId = kb.uriToRsrcId(k_thatWomanRsrc, false, true);
	ResourceId thisWomanLitRsrcId = kb.uriToRsrcId(k_thisWomanLitRsrc, true, true);
	ResourceId thatWomanLitRsrcId = kb.uriToRsrcId(k_thatWomanLitRsrc, true, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(hasBirthMotherRsrcId, rdfTypeRsrcId, owlFuncPropRsrcId, false);
	kb.addStmt(someKidRsrcId, hasBirthMotherRsrcId, thisWomanRsrcId, false);
	kb.addStmt(someKidRsrcId, hasBirthMotherRsrcId, thatWomanRsrcId, false);
	kb.addStmt(someKidRsrcId, hasBirthMotherRsrcId, thisWomanLitRsrcId, false);
	kb.addStmt(someKidRsrcId, hasBirthMotherRsrcId, thatWomanLitRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<FuncPropRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, thisWomanRsrcId, owlSameAsRsrcId, thatWomanRsrcId));
	BOOST_CHECK(isEntailed(kb, thatWomanRsrcId, owlSameAsRsrcId, thisWomanRsrcId));

	BOOST_CHECK(!isEntailed(kb, thisWomanRsrcId, owlSameAsRsrcId, thatWomanLitRsrcId));
	BOOST_CHECK(!isEntailed(kb, thatWomanLitRsrcId, owlSameAsRsrcId, thisWomanRsrcId));

	BOOST_CHECK(!isEntailed(kb, thisWomanRsrcId, owlSameAsRsrcId, thisWomanLitRsrcId));
	BOOST_CHECK(!isEntailed(kb, thisWomanLitRsrcId, owlSameAsRsrcId, thisWomanRsrcId));

	BOOST_CHECK(!isEntailed(kb, thisWomanLitRsrcId, owlSameAsRsrcId, thatWomanRsrcId));
	BOOST_CHECK(!isEntailed(kb, thatWomanRsrcId, owlSameAsRsrcId, thisWomanLitRsrcId));

	BOOST_CHECK(!isEntailed(kb, thatWomanLitRsrcId, owlSameAsRsrcId, thatWomanRsrcId));
	BOOST_CHECK(!isEntailed(kb, thatWomanRsrcId, owlSameAsRsrcId, thatWomanLitRsrcId));

	BOOST_CHECK(!isEntailed(kb, thisWomanLitRsrcId, owlSameAsRsrcId, thatWomanLitRsrcId));
	BOOST_CHECK(!isEntailed(kb, thatWomanLitRsrcId, owlSameAsRsrcId, thisWomanLitRsrcId));

	BOOST_CHECK(!isEntailed(kb, thisWomanRsrcId, owlSameAsRsrcId, thisWomanRsrcId));
	BOOST_CHECK(!isEntailed(kb, thatWomanRsrcId, owlSameAsRsrcId, thatWomanRsrcId));
	BOOST_CHECK(!isEntailed(kb, thisWomanLitRsrcId, owlSameAsRsrcId, thisWomanLitRsrcId));
	BOOST_CHECK(!isEntailed(kb, thatWomanLitRsrcId, owlSameAsRsrcId, thatWomanLitRsrcId));

	BOOST_CHECK_EQUAL(7u, kb.stmtCount());
}

BOOST_DATA_TEST_CASE(
	testInvFuncPropRules,
	bdata::make(k_otherRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<InvFuncPropRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId owlInvFuncPropRsrcId = kb.uriLib().m_owlInvFuncProp.id();
	ResourceId owlSameAsRsrcId = kb.uriLib().m_owlSameAs.id();
	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId hasSSNRsrcId = kb.uriToRsrcId(k_hasSSNRsrc, false, true);
	ResourceId thisPersonRsrcId = kb.uriToRsrcId(k_thisPersonRsrc, false, true);
	ResourceId thatPersonRsrcId = kb.uriToRsrcId(k_thatPersonRsrc, false, true);
	ResourceId ssnRsrcId = kb.uriToRsrcId(k_ssnLitRsrc, true, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(hasSSNRsrcId, rdfTypeRsrcId, owlInvFuncPropRsrcId, false);
	kb.addStmt(thisPersonRsrcId, hasSSNRsrcId, ssnRsrcId, false);
	kb.addStmt(thatPersonRsrcId, hasSSNRsrcId, ssnRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<InvFuncPropRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, thisPersonRsrcId, owlSameAsRsrcId, thatPersonRsrcId));
	BOOST_CHECK(isEntailed(kb, thatPersonRsrcId, owlSameAsRsrcId, thisPersonRsrcId));

	BOOST_CHECK(!isEntailed(kb, thisPersonRsrcId, owlSameAsRsrcId, thisPersonRsrcId));
	BOOST_CHECK(!isEntailed(kb, thatPersonRsrcId, owlSameAsRsrcId, thatPersonRsrcId));

	BOOST_CHECK_EQUAL(5u, kb.stmtCount());
}

BOOST_DATA_TEST_CASE(
	testTransitivePropRules,
	bdata::make(k_otherRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<TransitivePropRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId owlTransitivePropRsrcId = kb.uriLib().m_owlTransitiveProp.id();
	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId isContainedInRsrcId = kb.uriToRsrcId(k_isContainedInRsrc, false, true);
	ResourceId rosslynRsrcId = kb.uriToRsrcId(k_rosslynRsrc, false, true);
	ResourceId arlingtonRsrcId = kb.uriToRsrcId(k_arlingtonRsrc, false, true);
	ResourceId virginiaRsrcId = kb.uriToRsrcId(k_virginiaRsrc, false, true);
	ResourceId usaRsrcId = kb.uriToRsrcId(k_usaRsrc, false, true);
	ResourceId northAmericaRsrcId = kb.uriToRsrcId(k_northAmericaRsrc, false, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(isContainedInRsrcId, rdfTypeRsrcId, owlTransitivePropRsrcId, false);

	// These stmts are added in this odd order on purpose, since it exposed bug #2232
	kb.addStmt(rosslynRsrcId, isContainedInRsrcId, arlingtonRsrcId, false);
	kb.addStmt(usaRsrcId, isContainedInRsrcId, northAmericaRsrcId, false);
	kb.addStmt(arlingtonRsrcId, isContainedInRsrcId, virginiaRsrcId, false);
	kb.addStmt(virginiaRsrcId, isContainedInRsrcId, usaRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<TransitivePropRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, rosslynRsrcId, isContainedInRsrcId, virginiaRsrcId));
	BOOST_CHECK(isEntailed(kb, rosslynRsrcId, isContainedInRsrcId, usaRsrcId));
	BOOST_CHECK(isEntailed(kb, rosslynRsrcId, isContainedInRsrcId, northAmericaRsrcId));
	BOOST_CHECK(isEntailed(kb, arlingtonRsrcId, isContainedInRsrcId, usaRsrcId));
	BOOST_CHECK(isEntailed(kb, arlingtonRsrcId, isContainedInRsrcId, northAmericaRsrcId));
	BOOST_CHECK(isEntailed(kb, virginiaRsrcId, isContainedInRsrcId, northAmericaRsrcId));

	BOOST_CHECK_EQUAL(11u, kb.stmtCount());
}

BOOST_DATA_TEST_CASE(
	testDomainRules,
	bdata::make(k_otherRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<DomainRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId rdfsDomainRsrcId = kb.uriLib().m_rdfsDomain.id();
	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId dogRsrcId = kb.uriToRsrcId(k_dogRsrc, false, true);
	ResourceId hasColorRsrcId = kb.uriToRsrcId(k_hasColorRsrc, false, true);
	ResourceId fidoRsrcId = kb.uriToRsrcId(k_fidoRsrc, false, true);
	ResourceId blackRsrcId = kb.uriToRsrcId(k_blackLitRsrc, true, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(hasColorRsrcId, rdfsDomainRsrcId, dogRsrcId, false);
	kb.addStmt(fidoRsrcId, hasColorRsrcId, blackRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<DomainRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, fidoRsrcId, rdfTypeRsrcId, dogRsrcId));

	BOOST_CHECK_EQUAL(3u, kb.stmtCount());
}

BOOST_DATA_TEST_CASE(
	testRangeRules,
	bdata::make(k_otherRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<RangeRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId rdfsRangeRsrcId = kb.uriLib().m_rdfsRange.id();
	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId xsdStringRsrcId = kb.uriLib().m_xsdString.id();
	ResourceId personRsrcId = kb.uriToRsrcId(k_personRsrc, false, true);
	ResourceId hasParentRsrcId = kb.uriToRsrcId(k_hasParentRsrc, false, true);
	ResourceId hasSSNRsrcId = kb.uriToRsrcId(k_hasSSNRsrc, false, true);
	ResourceId queenLizLitRsrcId = kb.uriToRsrcId(k_queenElizabethLitRsrc, true, true);
	ResourceId princeCharlesRsrcId = kb.uriToRsrcId(k_princeCharlesRsrc, false, true);
	ResourceId princeWilliamRsrcId = kb.uriToRsrcId(k_princeWilliamRsrc, false, true);
	ResourceId ssnRsrcId = kb.uriToRsrcId(k_ssnRsrc, false, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(hasParentRsrcId, rdfsRangeRsrcId, personRsrcId, false);
	kb.addStmt(hasSSNRsrcId, rdfsRangeRsrcId, xsdStringRsrcId, false);
	kb.addStmt(princeWilliamRsrcId, hasParentRsrcId, princeCharlesRsrcId, false);
	kb.addStmt(princeCharlesRsrcId, hasParentRsrcId, queenLizLitRsrcId, false);
	kb.addStmt(princeCharlesRsrcId, hasSSNRsrcId, ssnRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<RangeRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, princeCharlesRsrcId, rdfTypeRsrcId, personRsrcId));
	BOOST_CHECK(!isEntailed(kb, queenLizLitRsrcId, rdfTypeRsrcId, personRsrcId));
	BOOST_CHECK(!isEntailed(kb, ssnRsrcId, rdfTypeRsrcId, xsdStringRsrcId));

	BOOST_CHECK_EQUAL(6u, kb.stmtCount());
}

BOOST_DATA_TEST_CASE(
	testEquivalentClassRules,
	bdata::make(k_otherRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<SubclassRule>(&kb, &(kb.ruleEngine())));
		kb.ruleEngine().addRule(make_shared<EquivalentClassRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId owlEquivalentClassRsrcId = kb.uriLib().m_owlEquivalentClass.id();
	ResourceId rdfsSubClassOfRsrcId = kb.uriLib().m_rdfsSubClassOf.id();
	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId personRsrcId = kb.uriToRsrcId(k_personRsrc, false, true);
	ResourceId humanRsrcId = kb.uriToRsrcId(k_humanRsrc, false, true);
	ResourceId princeCharlesRsrcId = kb.uriToRsrcId(k_princeCharlesRsrc, false, true);
	ResourceId princeWilliamRsrcId = kb.uriToRsrcId(k_princeWilliamRsrc, false, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(humanRsrcId, owlEquivalentClassRsrcId, personRsrcId, false);
	kb.addStmt(princeCharlesRsrcId, rdfTypeRsrcId, humanRsrcId, false);
	kb.addStmt(princeWilliamRsrcId, rdfTypeRsrcId, personRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<EquivalentClassRule>(&kb, &(kb.ruleEngine())));
		kb.ruleEngine().addRule(make_shared<SubclassRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, humanRsrcId, rdfsSubClassOfRsrcId, personRsrcId));
	BOOST_CHECK(isEntailed(kb, personRsrcId, rdfsSubClassOfRsrcId, humanRsrcId));
	BOOST_CHECK(isEntailed(kb, princeCharlesRsrcId, rdfTypeRsrcId, personRsrcId));
	BOOST_CHECK(isEntailed(kb, princeWilliamRsrcId, rdfTypeRsrcId, humanRsrcId));

	BOOST_CHECK_EQUAL(7u, kb.stmtCount());
}

BOOST_DATA_TEST_CASE(
	testEquivalentPropRules,
	bdata::make(k_otherRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<SubpropRule>(&kb, &(kb.ruleEngine())));
		kb.ruleEngine().addRule(make_shared<EquivalentPropRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId owlEquivalentPropRsrcId = kb.uriLib().m_owlEquivalentProp.id();
	ResourceId rdfsSubPropOfRsrcId = kb.uriLib().m_rdfsSubPropertyOf.id();
	ResourceId hasChildRsrcId = kb.uriToRsrcId(k_hasChildRsrc, false, true);
	ResourceId hasKidRsrcId = kb.uriToRsrcId(k_hasKidRsrc, false, true);
	ResourceId queenElizabethRsrcId = kb.uriToRsrcId(k_queenElizabethRsrc, false, true);
	ResourceId princeCharlesRsrcId = kb.uriToRsrcId(k_princeCharlesRsrc, false, true);
	ResourceId princeWilliamRsrcId = kb.uriToRsrcId(k_princeWilliamRsrc, false, true);

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	kb.addStmt(hasKidRsrcId, owlEquivalentPropRsrcId, hasChildRsrcId, false);
	kb.addStmt(queenElizabethRsrcId, hasKidRsrcId, princeCharlesRsrcId, false);
	kb.addStmt(princeCharlesRsrcId, hasChildRsrcId, princeWilliamRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<EquivalentPropRule>(&kb, &(kb.ruleEngine())));
		kb.ruleEngine().addRule(make_shared<SubpropRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, hasKidRsrcId, rdfsSubPropOfRsrcId, hasChildRsrcId));
	BOOST_CHECK(isEntailed(kb, hasChildRsrcId, rdfsSubPropOfRsrcId, hasKidRsrcId));
	BOOST_CHECK(isEntailed(kb, queenElizabethRsrcId, hasChildRsrcId, princeCharlesRsrcId));
	BOOST_CHECK(isEntailed(kb, princeCharlesRsrcId, hasKidRsrcId, princeWilliamRsrcId));

	BOOST_CHECK_EQUAL(7u, kb.stmtCount());
}

#if 0
struct Triple
{
	ResourceId m_subj;
	ResourceId m_pred;
	ResourceId m_obbj;
};

static void testHasValueRulesForOnePermutation(const RuleTestParams& rtp, int permutation[5])
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(make_shared<HasValueRule>(&kb, &(kb.ruleEngine())));
	}

	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId rdfsSubClassOfRsrcId	= kb.uriLib().m_rdfsSubClassOf.id();
	ResourceId owlOnPropRsrcId = kb.uriLib().m_owlOnProp.id();
	ResourceId owlRestrictionRsrcId = kb.uriLib().m_owlRestriction.id();
	ResourceId owlHasValueRsrcId = kb.uriLib().m_owlHasValue.id();
	ResourceId exampleRestrictionRsrcId = kb.createAnonymousRsrc();
	ResourceId redWineRsrcId = kb.uriToRsrcId(k_redWineRsrc, false, true);
	ResourceId hasColorRsrcId = kb.uriToRsrcId(k_hasColorRsrc, false, true);
	ResourceId mondaviPrivateReserveCabernetSauvignonRsrcId = kb.uriToRsrcId(k_mondaviPrivateReserveCabernetSauvignonRsrc, false, true);
	ResourceId redLitRsrcId = kb.uriToRsrcId(k_redLitRsrc, true, true);

	Triple triples[] =
	{
		{ redWineRsrcId, rdfsSubClassOfRsrcId, exampleRestrictionRsrcId },
		{ exampleRestrictionRsrcId, rdfTypeRsrcId, owlRestrictionRsrcId },
		{ exampleRestrictionRsrcId, owlOnPropRsrcId, hasColorRsrcId },
		{ exampleRestrictionRsrcId, owlHasValueRsrcId, redLitRsrcId },
		{ mondaviPrivateReserveCabernetSauvignonRsrcId, rdfTypeRsrcId, redWineRsrcId },
	};

	BOOST_CHECK_EQUAL(0u, kb.stmtCount());

	for (int i = 0; i < arrayLen(permutation); ++i)
	{
		kb.addStmt(
			triples[permutation[i]].m_subj,
			triples[permutation[i]].m_pred,
			triples[permutation[i]].m_obbj,
			false);
	}

	if (rtp.m_initPoint == RuleInitPoint::k_end)
	{
		kb.ruleEngine().addRule(make_shared<HasValueRule>(&kb, &(kb.ruleEngine())));
	}

	// Test entailments
	BOOST_CHECK(isEntailed(kb, mondaviPrivateReserveCabernetSauvignonRsrcId, hasColorRsrcId, redLitRsrcId));

	BOOST_CHECK_EQUAL(7u, kb.stmtCount());
}

BOOST_DATA_TEST_CASE(
	testHasValueRules,
	bdata::make(k_otherRuleTestParams),
	rtp)
{
	int permutation[5];
	for (int i = 0; i < arrayLen(permutation); ++i)
	{
		permutation[i] = i;
	}
	for (;;)
	{
		testHasValueRulesForOnePermutation(rtp, permutation);
		if (rtp.m_initPoint != RuleInitPoint::k_beginning
			|| !::std::next_permutation(permutation, permutation + arrayLen(permutation)))
		{
			break;
		}
	}
}
#endif

BOOST_DATA_TEST_CASE(
	testMultiAtomBodyRules,
	bdata::make(k_multiAtomBodyRuleTestParams),
	rtp)
{
	auto config = createTestConfig(RuleEnablement::k_allDisabled);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	//the following might be improper rdf?  but for testing purposes it ought not to matter
	ResourceId trueRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#true"), false, true);

	ResourceId rdfsSubClassOfRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://www.w3.org/2000/01/rdf-schema#subClassOf"), false, true);
	ResourceId dogRsrcId = kb.uriToRsrcId(k_dogRsrc, false, true);
	ResourceId mammalRsrcId = kb.uriToRsrcId(k_mammalRsrc, false, true);
	ResourceId animalRsrcId = kb.uriToRsrcId(k_animalRsrc, false, true);
	ResourceId fidoRsrcId = kb.uriToRsrcId(k_fidoRsrc, false, true);
	ResourceId isAliveRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#isAlive"), false, true);
	ResourceId stateOfHealthRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#stateOfHealth"), false, true);
	ResourceId healthyRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#healthy"), false, true);
	ResourceId coveredWithRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#coveredWith"), false, true);
	ResourceId furRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#fur"), false, true);

	ResourceId rdfTypeRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), false, true);
	ResourceId thisPersonRsrcId = kb.uriToRsrcId(k_thisPersonRsrc, false, true);
	ResourceId thatPersonRsrcId = kb.uriToRsrcId(k_thatPersonRsrc, false, true);
	ResourceId hasSSNRsrcId = kb.uriToRsrcId(k_hasSSNRsrc, false, true);
	ResourceId ssnRsrcId = kb.uriToRsrcId(k_ssnLitRsrc, true, true);
	ResourceId personRsrcId = kb.uriToRsrcId(k_personRsrc, false, true);

	ResourceId hasAddressRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#hasAddress"), false, true);
	ResourceId hasZipCodeRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#hasZipCode"), false, true);
	ResourceId hasProfileRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#hasProfile"), false, true);
	ResourceId hasAreaNameRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#hasAreaNameRsrcId"), false, true);
	ResourceId thisEmployeeRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#thisEmployeeRsrcId"), false, true);
	ResourceId thatEmployeeRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#thatEmployeeRsrcId"), false, true);
	ResourceId thisAddressRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#BBNArlingtonAddress"), false, true);
	ResourceId thatAddressRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#BBNColumbiaAddress"), false, true);
	ResourceId thisZipCodeRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#22209"), false, true);
	ResourceId thatZipCodeRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#21046"), false, true);
	ResourceId thisProfileRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#ArlingtonProfile"), false, true);
	ResourceId thatProfileRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#ColumbiaProfile"), false, true);
	ResourceId thisAreaNameRsrcId = kb.uriToRsrcId(k_arlingtonRsrc, false, true);
	ResourceId thatAreaNameRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#Columbia"), false, true);

	ResourceId aRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#A"), false, true);
	ResourceId bRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#B"), false, true);
	ResourceId cRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#C"), false, true);
	ResourceId dRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#D"), false, true);
	ResourceId eRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#E"), false, true);
	ResourceId fRsrcId = kb.uriToRsrcId(convertToRsrcChar("http://example.org/#F"), false, true);

	//a complex rule having only one var
	auto pNewRule1 = make_shared<StandardRule>(&kb, &(kb.ruleEngine()), k_nullRsrcId);
	pNewRule1->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(rdfTypeRsrcId),
			RuleAtomSlot::createForRsrc(mammalRsrcId)));
	pNewRule1->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(isAliveRsrcId),
			RuleAtomSlot::createForRsrc(trueRsrcId)));
	pNewRule1->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(stateOfHealthRsrcId),
			RuleAtomSlot::createForRsrc(healthyRsrcId)));
	pNewRule1->headPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(coveredWithRsrcId),
			RuleAtomSlot::createForRsrc(furRsrcId)));

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		kb.ruleEngine().addRule(pNewRule1);

		// Test entailments
		BOOST_CHECK_EQUAL(static_cast<size_t>(0), kb.stmtCount());
	}

	kb.addStmt(thisPersonRsrcId, rdfTypeRsrcId, mammalRsrcId, false);
	kb.addStmt(thisPersonRsrcId, isAliveRsrcId, trueRsrcId, false);
	kb.addStmt(thisPersonRsrcId, stateOfHealthRsrcId, healthyRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_beginning)
	{
		// Test entailments - rule added before statements
		BOOST_CHECK(isEntailed(kb, thisPersonRsrcId, coveredWithRsrcId, furRsrcId));
		BOOST_CHECK_EQUAL(static_cast<size_t>(4), kb.stmtCount());
	}

	if (rtp.m_initPoint == RuleInitPoint::k_middle)
	{
		kb.ruleEngine().addRule(pNewRule1);

		// Test entailments - statements added before rule
		BOOST_CHECK(isEntailed(kb, thisPersonRsrcId, coveredWithRsrcId, furRsrcId));
		BOOST_CHECK_EQUAL(static_cast<size_t>(4), kb.stmtCount());
	}

	kb.addStmt(fidoRsrcId, rdfTypeRsrcId, dogRsrcId, false);
	kb.addStmt(fidoRsrcId, isAliveRsrcId, trueRsrcId, false);
	kb.addStmt(fidoRsrcId, stateOfHealthRsrcId, healthyRsrcId, false);
	kb.addStmt(mammalRsrcId, rdfsSubClassOfRsrcId, animalRsrcId, false);
	kb.addStmt(dogRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_middle2)
	{
		kb.ruleEngine().addRule(pNewRule1);
		kb.ruleEngine().addRule(make_shared<SubclassRule>(&kb, &(kb.ruleEngine())));
		// Test entailments - the above complex rule paired with subclass rule - both must fire
		BOOST_CHECK_EQUAL(static_cast<size_t>(14), kb.stmtCount());
		BOOST_CHECK(isEntailed(kb, fidoRsrcId, coveredWithRsrcId, furRsrcId));
	}

	//this next rule has less real-world meaning than one would like, but does test important things
	//test that a rule that
	// 1) has body atoms with no common vars;
	// 2) in firing, will assert the same statement multiple times;
	//will succeed in firing, but not in adding the same statement twice
	auto pNewRule2 = make_shared<StandardRule>(&kb, &(kb.ruleEngine()), k_nullRsrcId);
	pNewRule2->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(rdfTypeRsrcId),
			RuleAtomSlot::createForRsrc(personRsrcId)));
	pNewRule2->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(1),
			RuleAtomSlot::createForRsrc(rdfTypeRsrcId),
			RuleAtomSlot::createForRsrc(personRsrcId)));
	pNewRule2->headPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(hasSSNRsrcId),
			RuleAtomSlot::createForRsrc(ssnRsrcId)));

	kb.addStmt(thisPersonRsrcId, rdfTypeRsrcId, personRsrcId, false);
	kb.addStmt(thatPersonRsrcId, rdfTypeRsrcId, personRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_middle3)
	{
		kb.ruleEngine().addRule(pNewRule2);
		// Test entailments
		BOOST_CHECK_EQUAL(static_cast<size_t>(12), kb.stmtCount());
		BOOST_CHECK(isEntailed(kb, thisPersonRsrcId, hasSSNRsrcId, ssnRsrcId));
		BOOST_CHECK(isEntailed(kb, thatPersonRsrcId, hasSSNRsrcId, ssnRsrcId));
	}

	kb.addStmt(thisEmployeeRsrcId, hasAddressRsrcId, thisAddressRsrcId, false);
	kb.addStmt(thatEmployeeRsrcId, hasAddressRsrcId, thatAddressRsrcId, false);
	kb.addStmt(thisAddressRsrcId, hasZipCodeRsrcId, thisZipCodeRsrcId, false);
	kb.addStmt(thatAddressRsrcId, hasZipCodeRsrcId, thatZipCodeRsrcId, false);
	kb.addStmt(thisZipCodeRsrcId, hasProfileRsrcId, thisProfileRsrcId, false);
	kb.addStmt(thatZipCodeRsrcId, hasProfileRsrcId, thatProfileRsrcId, false);
	kb.addStmt(thisZipCodeRsrcId, hasAreaNameRsrcId, thisAreaNameRsrcId, false);
	kb.addStmt(thatZipCodeRsrcId, hasAreaNameRsrcId, thatAreaNameRsrcId, false);

	//test that a rule having a series of linked vars executes properly
	auto pNewRule3 = make_shared<StandardRule>(&kb, &(kb.ruleEngine()), k_nullRsrcId);
	pNewRule3->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForRsrc(hasAddressRsrcId),
			RuleAtomSlot::createForVar(1)));
	pNewRule3->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(1),
			RuleAtomSlot::createForRsrc(hasZipCodeRsrcId),
			RuleAtomSlot::createForVar(2)));
	pNewRule3->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(2),
			RuleAtomSlot::createForRsrc(hasProfileRsrcId),
			RuleAtomSlot::createForVar(3)));
	pNewRule3->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(2),
			RuleAtomSlot::createForRsrc(hasAreaNameRsrcId),
			RuleAtomSlot::createForVar(4)));
	pNewRule3->headPushBack(RuleAtom(RuleAtomSlot::createForVar(3),
			RuleAtomSlot::createForRsrc(hasAreaNameRsrcId),
			RuleAtomSlot::createForVar(4)));

	if (rtp.m_initPoint == RuleInitPoint::k_middle4)
	{
		kb.ruleEngine().addRule(pNewRule3);
		// Test entailments
		BOOST_CHECK_EQUAL(static_cast<size_t>(20), kb.stmtCount());
		BOOST_CHECK(isEntailed(kb, thisProfileRsrcId, hasAreaNameRsrcId, thisAreaNameRsrcId));
		BOOST_CHECK(isEntailed(kb, thatProfileRsrcId, hasAreaNameRsrcId, thatAreaNameRsrcId));
	}

	//test that a hyper-connected rule (each pair of rule atoms shares at least one var) executes properly
	//this rule also tests that atoms may have vars as arguments
	auto pNewRule4 = make_shared<StandardRule>(&kb, &(kb.ruleEngine()), k_nullRsrcId);
	pNewRule4->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForVar(1),
			RuleAtomSlot::createForVar(2)));
	pNewRule4->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(1),
			RuleAtomSlot::createForVar(3),
			RuleAtomSlot::createForVar(4)));
	pNewRule4->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(2),
			RuleAtomSlot::createForVar(3),
			RuleAtomSlot::createForVar(5)));
	pNewRule4->bodyPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForVar(4),
			RuleAtomSlot::createForVar(5)));
	pNewRule4->headPushBack(RuleAtom(RuleAtomSlot::createForVar(0),
			RuleAtomSlot::createForVar(3),
			RuleAtomSlot::createForVar(5)));

	kb.addStmt(aRsrcId, bRsrcId, cRsrcId, false);
	kb.addStmt(bRsrcId, dRsrcId, eRsrcId, false);
	kb.addStmt(cRsrcId, dRsrcId, fRsrcId, false);
	kb.addStmt(aRsrcId, eRsrcId, fRsrcId, false);

	if (rtp.m_initPoint == RuleInitPoint::k_middle5)
	{
		kb.ruleEngine().addRule(pNewRule4);

		// Test entailments
		BOOST_CHECK_EQUAL(static_cast<size_t>(23), kb.stmtCount());
		BOOST_CHECK(isEntailed(kb, aRsrcId, dRsrcId, fRsrcId));
	}
}

BOOST_DATA_TEST_CASE(
	testRuleConfig,
	bdata::make(k_ruleConfigTestParams),
	rcti)
{
	auto config = createTestConfig(rcti.m_ruleEnablement);
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	ResourceId rdfsSubClassOfRsrcId	= kb.uriLib().m_rdfsSubClassOf.id();
	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId dogRsrcId = kb.uriToRsrcId(k_dogRsrc, false, true);
	ResourceId mammalRsrcId = kb.uriToRsrcId(k_mammalRsrc, false, true);
	ResourceId animalRsrcId = kb.uriToRsrcId(k_animalRsrc, false, true);
	ResourceId fidoRsrcId = kb.uriToRsrcId(k_fidoRsrc, false, true);

	kb.addStmt(mammalRsrcId, rdfsSubClassOfRsrcId, animalRsrcId, false);
	kb.addStmt(dogRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId, false);
	kb.addStmt(fidoRsrcId, rdfTypeRsrcId, dogRsrcId, false);

	BOOST_CHECK_EQUAL(rcti.m_expectedNumStmts, kb.stmtCount());
}

BOOST_AUTO_TEST_SUITE_END()
