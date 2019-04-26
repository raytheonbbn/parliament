// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2018, BBN Technologies, Inc.
// All rights reserved.

#include <boost/format.hpp>
#include <boost/test/unit_test.hpp>
#include "parliament/CharacterLiteral.h"
#include "parliament/KbConfig.h"
#include "parliament/KbInstance.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/UriLib.h"
#include "TestUtils.h"

using namespace ::bbn::parliament;

BOOST_AUTO_TEST_SUITE(InferenceStressTestSuite)

static const size_t k_numABoxStmts = 10000;

static const char k_aircraftInstFmt[]	= "http://example.org/abox#tailNo%|1$09|";

static const RsrcString	k_vehicleCls	= convertToRsrcChar("http://example.org/tbox#Vehicle");
static const RsrcString	k_aircraftCls	= convertToRsrcChar("http://example.org/tbox#Aircraft");

BOOST_AUTO_TEST_CASE(inferenceStressTest)
{
	KbConfig config;
	config.kbDirectoryPath(_T("test-kb-data"));
	KbDeleter deleter(config, true);
	KbInstance kb(config);

	ResourceId rdfTypeId = kb.uriLib().m_rdfType.id();
	ResourceId rdfsSubClassOfId = kb.uriLib().m_rdfsSubClassOf.id();
	ResourceId owlThingId = kb.uriLib().m_owlThing.id();
	ResourceId vehicleClsId = kb.uriToRsrcId(k_vehicleCls, false, true);
	ResourceId aircraftClsId = kb.uriToRsrcId(k_aircraftCls, false, true);

	kb.addStmt(vehicleClsId, rdfsSubClassOfId, owlThingId, false);

	auto fmt = ::boost::format{k_aircraftInstFmt};
	for (size_t i = 0; i < k_numABoxStmts; ++i)
	{
		RsrcString instUri = convertToRsrcChar(str(fmt % i));
		ResourceId instId = kb.uriToRsrcId(instUri, false, true);

		kb.addStmt(instId, rdfTypeId, aircraftClsId, false);
	}

	kb.addStmt(aircraftClsId, rdfsSubClassOfId, vehicleClsId, false);

	//BOOST_CHECK_EQUAL(2 * k_numABoxStmts + 1, kb.stmtCount());
	BOOST_CHECK_EQUAL(3 * k_numABoxStmts + 3, kb.stmtCount());
}

BOOST_AUTO_TEST_SUITE_END()
