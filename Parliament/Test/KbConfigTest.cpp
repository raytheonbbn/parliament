// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include <algorithm>
#include <boost/filesystem/path.hpp>
#include <boost/test/unit_test.hpp>
#include <boost/test/data/test_case.hpp>
#include <fstream>
#include <iterator>
#include <map>
#include <ostream>
#include <string>
#include <system_error>
#include "parliament/KbConfig.h"
#include "parliament/Exceptions.h"
#include "parliament/Windows.h"
#include "parliament/ArrayLength.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/UnicodeIterator.h"
#include "TestUtils.h"

namespace bdata = ::boost::unit_test::data;
namespace bfs = ::boost::filesystem;

using namespace ::bbn::parliament;
using ::std::endl;
using ::std::equal;
using ::std::ofstream;
using ::std::ostream;
using ::std::string;

static constexpr TChar k_envVar[] = _T("PARLIAMENT_KB_CONFIG_PATH");
static constexpr TChar k_fName[] = _T("tempFileForKbConfigTest.txt");

BOOST_AUTO_TEST_SUITE(KbConfigTestSuite)

BOOST_AUTO_TEST_CASE(testConfigDefaultCtor)
{
	KbConfig c;

	BOOST_CHECK_EQUAL(bfs::path(_T("kb-data")), c.kbDirectoryPath());
	BOOST_CHECK_EQUAL(string("statements.mem"), c.stmtFileName());
	BOOST_CHECK_EQUAL(string("resources.mem"), c.rsrcFileName());
	BOOST_CHECK_EQUAL(string("uris.mem"), c.uriTableFileName());
	BOOST_CHECK_EQUAL(string("u2i.db"), c.uriToIntFileName());

	BOOST_CHECK_EQUAL(false, c.readOnly());
	BOOST_CHECK_EQUAL(15000u, c.fileSyncTimerDelay());
	BOOST_CHECK_EQUAL(300000u, c.initialRsrcCapacity());
	BOOST_CHECK_EQUAL(100u, c.avgRsrcLen());
	BOOST_CHECK_EQUAL(600000u, c.rsrcGrowthIncrement());
	BOOST_CHECK_EQUAL(0.0, c.rsrcGrowthFactor());
	BOOST_CHECK_EQUAL(500000u, c.initialStmtCapacity());
	BOOST_CHECK_EQUAL(1000000u, c.stmtGrowthIncrement());
	BOOST_CHECK_EQUAL(0.0, c.stmtGrowthFactor());
	BOOST_CHECK_EQUAL(string("512m,1"), c.bdbCacheSize());

	BOOST_CHECK_EQUAL(true, c.normalizeTypedStringLiterals());

	BOOST_CHECK_EQUAL(5u, c.timeoutDuration());
	BOOST_CHECK(TimeUnit::k_min == c.timeoutUnit());

	BOOST_CHECK_EQUAL(false, c.runAllRulesAtStartup());
	BOOST_CHECK_EQUAL(false, c.enableSWRLRuleEngine());

	BOOST_CHECK_EQUAL(true, c.isSubclassRuleOn());
	BOOST_CHECK_EQUAL(true, c.isSubpropertyRuleOn());
	BOOST_CHECK_EQUAL(true, c.isDomainRuleOn());
	BOOST_CHECK_EQUAL(true, c.isRangeRuleOn());
	BOOST_CHECK_EQUAL(true, c.isEquivalentClassRuleOn());
	BOOST_CHECK_EQUAL(true, c.isEquivalentPropRuleOn());
	BOOST_CHECK_EQUAL(true, c.isInverseOfRuleOn());
	BOOST_CHECK_EQUAL(true, c.isSymmetricPropRuleOn());
	BOOST_CHECK_EQUAL(false, c.isFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(false, c.isInvFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(true, c.isTransitivePropRuleOn());

	BOOST_CHECK_EQUAL(false, c.inferRdfsClass());
	BOOST_CHECK_EQUAL(false, c.inferOwlClass());
	BOOST_CHECK_EQUAL(false, c.inferRdfsResource());
	BOOST_CHECK_EQUAL(false, c.inferOwlThing());
}

BOOST_AUTO_TEST_CASE(testConfigDefaultFileContainsDefaults)
{
	KbConfig defaults;
	KbConfig c;
	BOOST_REQUIRE_NO_THROW(c.readFromFile());

	BOOST_CHECK_EQUAL(defaults.kbDirectoryPath(), c.kbDirectoryPath());
	BOOST_CHECK_EQUAL(defaults.stmtFileName(), c.stmtFileName());
	BOOST_CHECK_EQUAL(defaults.rsrcFileName(), c.rsrcFileName());
	BOOST_CHECK_EQUAL(defaults.uriTableFileName(), c.uriTableFileName());
	BOOST_CHECK_EQUAL(defaults.uriToIntFileName(), c.uriToIntFileName());

	BOOST_CHECK_EQUAL(defaults.readOnly(), c.readOnly());
	BOOST_CHECK_EQUAL(defaults.fileSyncTimerDelay(), c.fileSyncTimerDelay());
	BOOST_CHECK_EQUAL(defaults.initialRsrcCapacity(), c.initialRsrcCapacity());
	BOOST_CHECK_EQUAL(defaults.avgRsrcLen(), c.avgRsrcLen());
	BOOST_CHECK_EQUAL(defaults.rsrcGrowthIncrement(), c.rsrcGrowthIncrement());
	BOOST_CHECK_EQUAL(defaults.rsrcGrowthFactor(), c.rsrcGrowthFactor());
	BOOST_CHECK_EQUAL(defaults.initialStmtCapacity(), c.initialStmtCapacity());
	BOOST_CHECK_EQUAL(defaults.stmtGrowthIncrement(), c.stmtGrowthIncrement());
	BOOST_CHECK_EQUAL(defaults.stmtGrowthFactor(), c.stmtGrowthFactor());
	BOOST_CHECK_EQUAL(defaults.bdbCacheSize(), c.bdbCacheSize());
	BOOST_CHECK_EQUAL(defaults.normalizeTypedStringLiterals(), c.normalizeTypedStringLiterals());

	BOOST_CHECK_EQUAL(defaults.timeoutDuration(), c.timeoutDuration());
	BOOST_CHECK(defaults.timeoutUnit() == c.timeoutUnit());

	BOOST_CHECK_EQUAL(defaults.runAllRulesAtStartup(), c.runAllRulesAtStartup());
	BOOST_CHECK_EQUAL(defaults.enableSWRLRuleEngine(), c.enableSWRLRuleEngine());

	BOOST_CHECK_EQUAL(defaults.isSubclassRuleOn(), c.isSubclassRuleOn());
	BOOST_CHECK_EQUAL(defaults.isSubpropertyRuleOn(), c.isSubpropertyRuleOn());
	BOOST_CHECK_EQUAL(defaults.isDomainRuleOn(), c.isDomainRuleOn());
	BOOST_CHECK_EQUAL(defaults.isRangeRuleOn(), c.isRangeRuleOn());
	BOOST_CHECK_EQUAL(defaults.isEquivalentClassRuleOn(), c.isEquivalentClassRuleOn());
	BOOST_CHECK_EQUAL(defaults.isEquivalentPropRuleOn(), c.isEquivalentPropRuleOn());
	BOOST_CHECK_EQUAL(defaults.isInverseOfRuleOn(), c.isInverseOfRuleOn());
	BOOST_CHECK_EQUAL(defaults.isSymmetricPropRuleOn(), c.isSymmetricPropRuleOn());
	BOOST_CHECK_EQUAL(defaults.isFunctionalPropRuleOn(), c.isFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(defaults.isInvFunctionalPropRuleOn(), c.isInvFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(defaults.isTransitivePropRuleOn(), c.isTransitivePropRuleOn());

	BOOST_CHECK_EQUAL(defaults.inferRdfsClass(), c.inferRdfsClass());
	BOOST_CHECK_EQUAL(defaults.inferOwlClass(), c.inferOwlClass());
	BOOST_CHECK_EQUAL(defaults.inferRdfsResource(), c.inferRdfsResource());
	BOOST_CHECK_EQUAL(defaults.inferOwlThing(), c.inferOwlThing());
}

BOOST_AUTO_TEST_CASE(testConfigReadFromFile)
{
	KbConfig c;

	EnvVarReset envVarReset(k_envVar, k_fName);
	FileDeleter filedeleter(k_fName);

	ofstream s(k_fName);
	s << "# Parameters file for the Parliament core DLL" << endl;
	s << " \t # Parameters file for the Parliament core DLL" << endl;
	s << endl;
	s << "kbDirectoryPath=./subdir" << endl;
	s << "stmtFileName         =statements" << endl;
	s << "rsrcFileName      \t = resources" << endl;
	s << "uriTableFileName     = foo" << endl;
	s << "uriToIntFileName     = bar" << endl;
	s << endl;
	s << "  readOnly           = yes" << endl;
	s << "  fileSyncTimerDelay = 10000" << endl;
	s << "initialRsrcCapacity  = 100" << endl;
	s << " \t avgRsrcLen       = 128" << endl;
	s << "rsrcGrowthIncrement  = 200 " << endl;
	s << "rsrcGrowthFactor     = 2 " << endl;
	s << "initialStmtCapacity  = 500 \t " << endl;
	s << "stmtGrowthIncrement  = 1000" << endl;
	s << "stmtGrowthFactor     = 2" << endl;
	s << "bdbCacheSize         = 512m,2" << endl;
	s << "normalizeTypedStringLiterals = no" << endl;
	s << endl;
	s << "TimeoutDuration = 200" << endl;
	s << "TimeoutUnit = milliseconds" << endl;
	s << endl;
	s << "runAllRulesAtStartup = yes" << endl;
	s << "enableSWRLRuleEngine = off" << endl;
	s << endl;
	s << "SubclassRule           = on" << endl;
	s << "SubpropertyRule        = on" << endl;
	s << "DomainRule             = on" << endl;
	s << "RangeRule              = on" << endl;
	s << "EquivalentClassRule    = on" << endl;
	s << "EquivalentPropRule     = on" << endl;
	s << "InverseOfRule          = on" << endl;
	s << "SymmetricPropRule      = on" << endl;
	s << "FunctionalPropRule     = off" << endl;
	s << "InvFunctionalPropRule  = on" << endl;
	s << "TransitivePropRule     = on" << endl;
	s << endl;
	s << "inferRdfsClass       = yes" << endl;
	s << "inferOwlClass        = no" << endl;
	s << "inferRdfsResource    = yes" << endl;
	s << "inferOwlThing        = no" << endl;
	s.close();

	BOOST_REQUIRE_NO_THROW(c.readFromFile());

	BOOST_CHECK_EQUAL(string("./subdir"), c.kbDirectoryPath());
	BOOST_CHECK_EQUAL(string("statements"), c.stmtFileName());
	BOOST_CHECK_EQUAL(string("resources"), c.rsrcFileName());
	BOOST_CHECK_EQUAL(string("foo"), c.uriTableFileName());
	BOOST_CHECK_EQUAL(string("bar"), c.uriToIntFileName());

	BOOST_CHECK_EQUAL(true, c.readOnly());
	BOOST_CHECK_EQUAL(10000u, c.fileSyncTimerDelay());
	BOOST_CHECK_EQUAL(100u, c.initialRsrcCapacity());
	BOOST_CHECK_EQUAL(128u, c.avgRsrcLen());
	BOOST_CHECK_EQUAL(200u, c.rsrcGrowthIncrement());
	BOOST_CHECK_EQUAL(2.0, c.rsrcGrowthFactor());
	BOOST_CHECK_EQUAL(500u, c.initialStmtCapacity());
	BOOST_CHECK_EQUAL(1000u, c.stmtGrowthIncrement());
	BOOST_CHECK_EQUAL(2.0, c.stmtGrowthFactor());
	BOOST_CHECK_EQUAL(string("512m,2"), c.bdbCacheSize());
	BOOST_CHECK_EQUAL(false, c.normalizeTypedStringLiterals());

	BOOST_CHECK_EQUAL(200u, c.timeoutDuration());
	BOOST_CHECK(TimeUnit::k_milliSec == c.timeoutUnit());

	BOOST_CHECK_EQUAL(true, c.runAllRulesAtStartup());
	BOOST_CHECK_EQUAL(false, c.enableSWRLRuleEngine());

	BOOST_CHECK_EQUAL(true, c.isSubclassRuleOn());
	BOOST_CHECK_EQUAL(true, c.isSubpropertyRuleOn());
	BOOST_CHECK_EQUAL(true, c.isDomainRuleOn());
	BOOST_CHECK_EQUAL(true, c.isRangeRuleOn());
	BOOST_CHECK_EQUAL(true, c.isEquivalentClassRuleOn());
	BOOST_CHECK_EQUAL(true, c.isEquivalentPropRuleOn());
	BOOST_CHECK_EQUAL(true, c.isInverseOfRuleOn());
	BOOST_CHECK_EQUAL(true, c.isSymmetricPropRuleOn());
	BOOST_CHECK_EQUAL(false, c.isFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(true, c.isInvFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(true, c.isTransitivePropRuleOn());

	BOOST_CHECK_EQUAL(true, c.inferRdfsClass());
	BOOST_CHECK_EQUAL(false, c.inferOwlClass());
	BOOST_CHECK_EQUAL(true, c.inferRdfsResource());
	BOOST_CHECK_EQUAL(false, c.inferOwlThing());
}

BOOST_AUTO_TEST_CASE(testConfigReadFromBadFile)
{
	KbConfig c;

	EnvVarReset envVarReset(k_envVar, k_fName);
	FileDeleter filedeleter(k_fName);

	ofstream s(k_fName);
	s << "# Parameters file for the Parliament core DLL" << endl;
	s << endl;
	s << "kbDirectoryPath      = ./subdir" << endl;
	s << "stmtFileNameMessedUp = statements" << endl;	// this key is bad
	s << "rsrcFileName         = resources" << endl;
	s << "uriTableFileName     = foo" << endl;
	s << "uriToIntFileName     = bar" << endl;
	s << endl;
	s << "readOnly             = yes" << endl;
	s << "fileSyncTimerDelay   = 10000" << endl;
	s << "initialRsrcCapacity  = 100" << endl;
	s << "avgRsrcLen           = 128" << endl;
	s << "rsrcGrowthIncrement  = 200" << endl;
	s << "rsrcGrowthFactor     = 2" << endl;
	s << "initialStmtCapacity  = 500" << endl;
	s << "stmtGrowthIncrement  = 1000" << endl;
	s << "stmtGrowthFactor     = 2" << endl;
	s << "bdbCacheSize         = 512m,2" << endl;
	s << "normalizeTypedStringLiterals = no" << endl;
	s << endl;
	s << "TimeoutDuration = 200" << endl;
	s << "TimeoutUnit = milliseconds" << endl;
	s << endl;
	s << "runAllRulesAtStartup = no" << endl;
	s << "enableSWRLRuleEngine = no" << endl;
	s << endl;
	s << "SubclassRule           = on" << endl;
	s << "SubpropertyRule        = on" << endl;
	s << "DomainRule             = on" << endl;
	s << "RangeRule              = on" << endl;
	s << "EquivalentClassRule    = on" << endl;
	s << "EquivalentPropRule     = on" << endl;
	s << "InverseOfRule          = on" << endl;
	s << "SymmetricPropRule      = on" << endl;
	s << "FunctionalPropRule     = on" << endl;
	s << "InvFunctionalPropRule  = on" << endl;
	s << "TransitivePropRule     = on" << endl;
	s << endl;
	s << "inferRdfsClass       = yes" << endl;
	s << "inferOwlClass        = yes" << endl;
	s << "inferRdfsResource    = yes" << endl;
	s << "inferOwlThing        = yes" << endl;
	s.close();

	BOOST_CHECK_THROW(c.readFromFile(), Exception);
}

BOOST_AUTO_TEST_SUITE_END()
