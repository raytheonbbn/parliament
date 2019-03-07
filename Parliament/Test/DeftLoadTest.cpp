// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2018, BBN Technologies, Inc.
// All rights reserved.

#include <boost/filesystem/fstream.hpp>
#include <boost/filesystem/operations.hpp>
#include <boost/test/unit_test.hpp>
#include <fstream>
#include <string>
#include <unordered_map>

#include "parliament/CharacterLiteral.h"
#include "parliament/KbConfig.h"
#include "parliament/KbInstance.h"
#include "parliament/RegEx.h"
#include "parliament/UnicodeIterator.h"

namespace bfs = ::boost::filesystem;
using namespace ::bbn::parliament;
using ::std::getline;
using ::std::ifstream;
using ::std::runtime_error;
using ::std::string;

using BlankNodeMap = ::std::unordered_map<::std::string, ResourceId>;

BOOST_AUTO_TEST_SUITE(DeftTestSuite)

static const TChar k_pmntDepsEnvVar[] = _T("PARLIAMENT_DEPENDENCIES");
static const TChar k_dataFilePath[] = _T("data/deft-data-load.nt");
static const char k_blankOrCommentRegExStr[] = "^[ \t]*(?:#.*)?$";
static const char k_nTripleRegExStr[] = "^(?:(?:<([^ ]+)>)|(_:[^ ]+)) <([^ ]+)> (?:(?:<([^ ]+)>)|(_:[^ ]+)|(\".*\"(?:[^ \"]*)?)) \\.$";

static ResourceId getBNodeId(KbInstance& kb, BlankNodeMap& bnodeMap, const string& bnodeQName)
{
	ResourceId bNodeId;
	auto it = bnodeMap.find(bnodeQName);
	if (it != end(bnodeMap))
	{
		bNodeId = it->second;
	}
	else
	{
		bNodeId = kb.createAnonymousRsrc();
		bnodeMap.insert(make_pair(bnodeQName, bNodeId));
	}
	return bNodeId;
}

BOOST_AUTO_TEST_CASE(testDeftLoad)
{
	auto envVarValue = tGetEnvVar(k_pmntDepsEnvVar);
	if (envVarValue.empty())
	{
		BOOST_TEST_MESSAGE("Skipping test because environment variable "
			<< convertTCharToUtf8(k_pmntDepsEnvVar) << " is not defined.");
		return;
	}

	bfs::path dataFile{envVarValue};
	dataFile /= k_dataFilePath;
	if (!exists(dataFile))
	{
		BOOST_TEST_MESSAGE("Skipping test because data file " << convertTCharToUtf8(k_dataFilePath)
			<< " could not be found in dependencies directory " << convertTCharToUtf8(envVarValue));
		return;
	}

	KbConfig config;
	config.kbDirectoryPath("test-kb-data");
	KbInstance::deleteKb(config);

	{
		KbInstance kb(config);

		BlankNodeMap bnodeMap;
		RegEx blankOrCommentRex = compileRegEx(k_blankOrCommentRegExStr);
		RegEx tripleRex = compileRegEx(k_nTripleRegExStr);
		for (bfs::ifstream in(dataFile, ::std::ios::in); !in.eof();)
		{
			if (!in)
			{
				throw runtime_error("IO error reading from data file");
			}
			else
			{
				string line;
				getline(in, line);
				SMatch blankOrCommentCaptures;
				SMatch tripleCaptures;
				if (regExMatch(line, blankOrCommentCaptures, blankOrCommentRex))
				{
					// Do nothing
				}
				else if (regExMatch(line, tripleCaptures, tripleRex))
				{
					string uriSubj = tripleCaptures[1].str();
					string blankSubj = tripleCaptures[2].str();
					string pred = tripleCaptures[3].str();
					string uriObj = tripleCaptures[4].str();
					string blankObj = tripleCaptures[5].str();
					string litObj = tripleCaptures[6].str();

					//if (!uriSubj.empty()) { BOOST_TEST_MESSAGE("URI Subject:  '" << uriSubj << "'"); }
					//if (!blankSubj.empty()) { BOOST_TEST_MESSAGE("Blank Subject:  '" << blankSubj << "'"); }
					//BOOST_TEST_MESSAGE("Predicate:  '" << pred << "'");
					//if (!uriObj.empty()) { BOOST_TEST_MESSAGE("URI Object:  '" << uriObj << "'"); }
					//if (!blankObj.empty()) { BOOST_TEST_MESSAGE("Blank Object:  '" << blankObj << "'"); }
					//if (!litObj.empty()) { BOOST_TEST_MESSAGE("Literal Object:  '" << litObj << "'"); }

					ResourceId subjId;
					if (!uriSubj.empty())
					{
						subjId = kb.uriToRsrcId(convertToRsrcChar(uriSubj), false, true);
					}
					else
					{
						subjId = getBNodeId(kb, bnodeMap, blankSubj);
					}
					ResourceId predId = kb.uriToRsrcId(convertToRsrcChar(pred), false, true);
					ResourceId objId;
					if (!uriObj.empty())
					{
						objId = kb.uriToRsrcId(convertToRsrcChar(uriObj), false, true);
					}
					else if (!litObj.empty())
					{
						objId = kb.uriToRsrcId(convertToRsrcChar(litObj), true, true);
					}
					else
					{
						objId = getBNodeId(kb, bnodeMap, blankObj);
					}

					kb.addStmt(subjId, predId, objId, false);
				}
				else
				{
					BOOST_TEST_MESSAGE("Line doesn't match:  \"" << line << "\"");
				}
			}
		}

		size_t total = 0;
		size_t numDel = 0;
		size_t numInferred = 0;
		size_t numDelAndInferred = 0;
		size_t numHidden = 0;
		size_t numVirtual = 0;
		kb.countStmts(total, numDel, numInferred, numDelAndInferred, numHidden, numVirtual);

		// Note:  The total above does not count virtual statements.
		const size_t k_numStmtsInFile = 59837u;
		const size_t k_numReifications = 5994u;
		BOOST_CHECK_EQUAL(2u, numDel);
		BOOST_CHECK_EQUAL(0u, numDelAndInferred);
		BOOST_CHECK_EQUAL(k_numReifications, numHidden);
		BOOST_CHECK_EQUAL(4 * k_numReifications, numVirtual);
		BOOST_CHECK_EQUAL(k_numStmtsInFile, total + numVirtual - numInferred - numDel - numHidden);
		BOOST_CHECK_EQUAL(37049u, numInferred);
	}

	KbInstance::deleteKb(config);
}

BOOST_AUTO_TEST_SUITE_END()
