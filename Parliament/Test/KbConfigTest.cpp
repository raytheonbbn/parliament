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
	KbConfig defaults;

	BOOST_CHECK_EQUAL(bfs::path(_T("kb-data")), defaults.kbDirectoryPath());
	BOOST_CHECK_EQUAL(string("statements.mem"), defaults.stmtFileName());
	BOOST_CHECK_EQUAL(string("resources.mem"), defaults.rsrcFileName());
	BOOST_CHECK_EQUAL(string("uris.mem"), defaults.uriTableFileName());
	BOOST_CHECK_EQUAL(string("u2i.db"), defaults.uriToIntFileName());

	BOOST_CHECK_EQUAL(false, defaults.readOnly());
	BOOST_CHECK_EQUAL(15000u, defaults.fileSyncTimerDelay());
	BOOST_CHECK_EQUAL(300000u, defaults.initialRsrcCapacity());
	BOOST_CHECK_EQUAL(100u, defaults.avgRsrcLen());
	BOOST_CHECK_EQUAL(600000u, defaults.rsrcGrowthIncrement());
	BOOST_CHECK_EQUAL(0.0, defaults.rsrcGrowthFactor());
	BOOST_CHECK_EQUAL(500000u, defaults.initialStmtCapacity());
	BOOST_CHECK_EQUAL(1000000u, defaults.stmtGrowthIncrement());
	BOOST_CHECK_EQUAL(0.0, defaults.stmtGrowthFactor());
	BOOST_CHECK_EQUAL(string("32m,1"), defaults.bdbCacheSize());

	BOOST_CHECK_EQUAL(true, defaults.normalizeTypedStringLiterals());

	BOOST_CHECK_EQUAL(5u, defaults.timeoutDuration());
	BOOST_CHECK(TimeUnit::k_min == defaults.timeoutUnit());

	BOOST_CHECK_EQUAL(false, defaults.runAllRulesAtStartup());
	BOOST_CHECK_EQUAL(false, defaults.enableSWRLRuleEngine());

	BOOST_CHECK_EQUAL(true, defaults.isSubclassRuleOn());
	BOOST_CHECK_EQUAL(true, defaults.isSubpropertyRuleOn());
	BOOST_CHECK_EQUAL(true, defaults.isDomainRuleOn());
	BOOST_CHECK_EQUAL(true, defaults.isRangeRuleOn());
	BOOST_CHECK_EQUAL(true, defaults.isEquivalentClassRuleOn());
	BOOST_CHECK_EQUAL(true, defaults.isEquivalentPropRuleOn());
	BOOST_CHECK_EQUAL(true, defaults.isInverseOfRuleOn());
	BOOST_CHECK_EQUAL(true, defaults.isSymmetricPropRuleOn());
	BOOST_CHECK_EQUAL(false, defaults.isFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(false, defaults.isInvFunctionalPropRuleOn());
	BOOST_CHECK_EQUAL(true, defaults.isTransitivePropRuleOn());

	BOOST_CHECK_EQUAL(false, defaults.inferRdfsClass());
	BOOST_CHECK_EQUAL(false, defaults.inferOwlClass());
	BOOST_CHECK_EQUAL(false, defaults.inferRdfsResource());
	BOOST_CHECK_EQUAL(false, defaults.inferOwlThing());
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

static auto k_validTestConfig = u8R"~~~(
# Parameters file for the Parliament core DLL
 	 # Parameters file for the Parliament core DLL

kbDirectoryPath=./subdir
stmtFileName         =statements
rsrcFileName      	 = resources
uriTableFileName     = foo
uriToIntFileName     = bar

  readOnly           = yes
  fileSyncTimerDelay = 10000
initialRsrcCapacity  = 100
 	 avgRsrcLen       = 128
rsrcGrowthIncrement  = 200 
rsrcGrowthFactor     = 2      
initialStmtCapacity  = 500 	 
stmtGrowthIncrement  = 1000
stmtGrowthFactor     = 2
bdbCacheSize         = 512m,2
normalizeTypedStringLiterals = no

TimeoutDuration = 200
TimeoutUnit = milliseconds

runAllRulesAtStartup = yes
enableSWRLRuleEngine = off

SubclassRule           = on
SubpropertyRule        = on
DomainRule             = on
RangeRule              = on
EquivalentClassRule    = on
EquivalentPropRule     = on
InverseOfRule          = on
SymmetricPropRule      = on
FunctionalPropRule     = off
InvFunctionalPropRule  = on
TransitivePropRule     = on

inferRdfsClass       = yes
inferOwlClass        = no
inferRdfsResource    = yes
inferOwlThing        = no
)~~~";

BOOST_AUTO_TEST_CASE(testConfigReadFromFile)
{
	KbConfig c;

	EnvVarReset envVarReset(k_envVar, k_fName);
	FileDeleter filedeleter(k_fName);

	{
		ofstream s(k_fName);
		s << k_validTestConfig;
	}

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

// Note that the key 'stmtFileNameMessedUp' is bad:
static auto k_invalidTestConfig = u8R"~~~(
# Parameters file for the Parliament core DLL

kbDirectoryPath      = ./subdir
stmtFileNameMessedUp = statements
rsrcFileName         = resources
uriTableFileName     = foo
uriToIntFileName     = bar

readOnly             = yes
fileSyncTimerDelay   = 10000
initialRsrcCapacity  = 100
avgRsrcLen           = 128
rsrcGrowthIncrement  = 200
rsrcGrowthFactor     = 2
initialStmtCapacity  = 500
stmtGrowthIncrement  = 1000
stmtGrowthFactor     = 2
bdbCacheSize         = 512m,2
normalizeTypedStringLiterals = no

TimeoutDuration = 200
TimeoutUnit = milliseconds

runAllRulesAtStartup = no
enableSWRLRuleEngine = no

SubclassRule           = on
SubpropertyRule        = on
DomainRule             = on
RangeRule              = on
EquivalentClassRule    = on
EquivalentPropRule     = on
InverseOfRule          = on
SymmetricPropRule      = on
FunctionalPropRule     = on
InvFunctionalPropRule  = on
TransitivePropRule     = on

inferRdfsClass       = yes
inferOwlClass        = yes
inferRdfsResource    = yes
inferOwlThing        = yes
)~~~";

BOOST_AUTO_TEST_CASE(testConfigReadFromBadFile)
{
	KbConfig c;

	EnvVarReset envVarReset(k_envVar, k_fName);
	FileDeleter filedeleter(k_fName);

	{
		ofstream s(k_fName);
		s << k_invalidTestConfig;
	}

	BOOST_CHECK_THROW(c.readFromFile(), Exception);
}

BOOST_AUTO_TEST_SUITE_END()
