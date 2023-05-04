// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "KbAdmin.h"
#include "parliament/CharacterLiteral.h"
#include "parliament/KbConfig.h"
#include "parliament/Version.h"
#include "parliament/KbInstance.h"
#include "parliament/Exceptions.h"
#include "parliament/FileHandle.h"
#include "parliament/StmtIterator.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/UriLib.h"
#include "parliament/Util.h"

#include <boost/filesystem/fstream.hpp>
#include <boost/filesystem/operations.hpp>
#include <fstream>
#include <iostream>
#include <iomanip>

#include <boost/algorithm/string/case_conv.hpp>

namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::std::cout;
using ::std::endl;
using ::std::exception;
using ::std::ios_base;
using ::std::setw;
using ::std::size;
using ::std::string;
using ::std::string_view;
using ::boost::algorithm::to_lower_copy;
using ::boost::format;

#if defined(PARLIAMENT_WINDOWS)
const char pmnt::KbAdmin::k_soi[]	= "/";	// Short Option Introducer
const char pmnt::KbAdmin::k_loi[]	= "/";	// Long Option Introducer
#else
const char pmnt::KbAdmin::k_soi[]	= "-";	// Short Option Introducer
const char pmnt::KbAdmin::k_loi[]	= "--";	// Long Option Introducer
#endif

static const pmnt::TChar k_kbDir[] = _T(".");

int main(int argc, char** argv)
{
	int exitCode = EXIT_FAILURE;
	try
	{
		pmnt::KbAdmin app(argc, argv);
		app.run();
		exitCode = EXIT_SUCCESS;
	}
	catch (const pmnt::UsageException& ex)
	{
		pmnt::KbAdmin::printUsage(ex.what());
	}
	catch (const pmnt::Exception& ex)
	{
		cout << "::bbn::parliament::Exception:  " << ex.what() << endl;
	}
	catch (const exception& ex)
	{
		cout << "::std::exception:  " << ex.what() << endl;
	}

	return exitCode;
}

void pmnt::KbAdmin::printVersion()
{
	cout <<
		"Parliament Administration Tool version:  " << PARLIAMENT_VERSION_STRING << endl <<
		"Parliament Core Library version:         " << getKbVersion() << endl;
}

void pmnt::KbAdmin::printUsage(string_view msg)
{
	printVersion();

	if (!msg.empty())
	{
		cout << endl << msg << endl;
	}

	cout << endl <<
		"Usage 1:  ParliamentAdmin { " << k_soi << "? | " << k_soi << "h | " << k_loi << "help } \n"
		"Usage 2:  ParliamentAdmin { " << k_soi << "v | " << k_loi << "version } \n"
		"Usage 3:  ParliamentAdmin [ " << k_soi << "t | " << k_loi << "time ] { "
			<< k_soi << "e | " << k_loi << "export <fileName> } [ "
			<< k_soi << "ii | " << k_loi << "includeInferred ] [ "
			<< k_soi << "id | " << k_loi << "includeDeleted ]\n"
		"Usage 4:  ParliamentAdmin [ " << k_soi << "t | " << k_loi << "time ] <command-option> \n"
		"\n"
		"where\n"
		"\n"
		"   " << k_soi << "? or " << k_soi << "h or " << k_loi << "help\n"
		"      Prints this help text and exits.\n"
		"\n"
		"   " << k_soi << "v or " << k_loi << "version\n"
		"      Prints version information and exits.\n"
		"\n"
		"   " << k_soi << "t or " << k_loi << "time\n"
		"      Times the command options that follow.\n"
		"\n"
		"   " << k_soi << "e or " << k_loi << "export\n"
		"      Exports the KB content to stdout as an n-triples file.\n"
		"\n"
		"   " << k_soi << "is or " << k_loi << "includeSource\n"
		"      May only be used in conjunction with the export command.\n"
		"      Includes the source URI for each statement, resulting in an\n"
		"      n-quad file.  The default is to exclude the source URI.\n"
		"\n"
		"   " << k_soi << "ii or " << k_loi << "includeInferred\n"
		"      May only be used in conjunction with the export command.\n"
		"      Includes inferred statements in the export.  The default is to\n"
		"      exclude inferred statements.\n"
		"\n"
		"   " << k_soi << "id or " << k_loi << "includeDeleted\n"
		"      May only be used in conjunction with the export command.\n"
		"      Includes deleted statements in the export.  The default is to\n"
		"      exclude deleted statements.\n"
		"\n"
		"   <command-option>\n"
		"      One of the following options, each of which\n"
		"      directs ParliamentAdmin to execute a command.\n"
		"\n"
		"   " << k_soi << "c or " << k_loi << "create\n"
		"      Creates and initializes a new KB.\n"
		"\n"
		"   " << k_soi << "# or " << k_loi << "countClasses\n"
		"      Counts the number of RDF classes in the KB.\n"
		"\n"
		"   " << k_soi << "l or " << k_loi << "listClasses\n"
		"      Lists the RDF classes in the KB.\n"
		"\n"
		"   " << k_soi << "s or " << k_loi << "statistics\n"
		"      Prints KB statistics.\n"
		"\n"
		"   " << k_soi << "rel or " << k_loi << "releaseExcessCapacity\n"
		"      Releases excess capacity in the KB files to make them\n"
		"      as compact as possible (without loss of information).\n"
		"\n"
		"   " << k_soi << "ge or " << k_loi << "guaranteeEntailments\n"
		"      Runs all configured inference rules to guarantee that all\n"
		"      entailments have been asserted.  Useful when a new rule\n"
		"      has been enabled for an existing KB.\n"
		"\n"
		"   " << k_soi << "va or " << k_loi << "validate\n"
		"      Runs a variety of checks on the KB files to detect corruptions.\n"
		"\n"
		"   " << k_soi << "dr or " << k_loi << "dumpRsrc\n"
		"      Dumps the KB's resource records.\n"
		"\n"
		"   " << k_soi << "drv or " << k_loi << "dumpRsrcVerbose\n"
		"      Dumps the KB's resource records, in verbose format.\n"
		"\n"
		"   " << k_soi << "drvv or " << k_loi << "dumpRsrcVeryVerbose\n"
		"      Dumps the KB's resource records, in very verbose format.\n"
		"\n"
		"   " << k_soi << "ds or " << k_loi << "dumpStmt\n"
		"      Dumps the KB's statement records.\n"
		"\n"
		"   " << k_soi << "dsv or " << k_loi << "dumpStmtVerbose\n"
		"      Dumps the KB's statement records, in verbose format.\n"
		"\n"
		"   " << k_soi << "dsvv or " << k_loi << "dumpStmtVeryVerbose\n"
		"      Dumps the KB's statement records, in very verbose format.\n"
		<< endl;
}

pmnt::KbAdmin::KbAdmin(uint32 numArgs, const char*const* argList) :
	m_helpOpt(false),
	m_versionOpt(false),
	m_timeOpt(false),
	m_createKbOpt(false),
	m_countClassesOpt(false),
	m_listClassesOpt(false),
	m_statisticsOpt(false),
	m_relExCapOpt(false),
	m_grnteeEntlmntsOpt(false),
	m_validateOpt(false),
	m_exportOpt(false),
	m_infStmtAction(InferredStmtsAction::exclude),
	m_delStmtAction(DeletedStmtsAction::exclude),
	m_exportFilePath(),
	m_rsrcDumpOpt(DumpFormat::k_noDump),
	m_stmtDumpOpt(DumpFormat::k_noDump)
{
	for (uint32 i = 1; i < numArgs; ++i)
	{
		bool isShortIntroducer;
		bool isLongIntroducer;
		string arg = stripOptionIntroducer(argList[i], isShortIntroducer, isLongIntroducer);

		if ((isShortIntroducer && (arg == "?" || arg == "h")) || (isLongIntroducer && arg == "help"))
		{
			m_helpOpt = true;
		}
		else if ((isShortIntroducer && arg == "v") || (isLongIntroducer && arg == "version"))
		{
			m_versionOpt = true;
		}
		else if ((isShortIntroducer && arg == "t") || (isLongIntroducer && arg == "time"))
		{
			m_timeOpt = true;
		}
		else if ((isShortIntroducer && arg == "c") || (isLongIntroducer && arg == "create"))
		{
			m_createKbOpt = true;
		}
		else if ((isShortIntroducer && arg == "#") || (isLongIntroducer && arg == "countclasses"))
		{
			m_countClassesOpt = true;
		}
		else if ((isShortIntroducer && arg == "l") || (isLongIntroducer && arg == "listclasses"))
		{
			m_listClassesOpt = true;
		}
		else if ((isShortIntroducer && arg == "s") || (isLongIntroducer && arg == "statistics"))
		{
			m_statisticsOpt = true;
		}
		else if ((isShortIntroducer && arg == "rel") || (isLongIntroducer && arg == "releaseExcessCapacity"))
		{
			m_relExCapOpt = true;
		}
		else if ((isShortIntroducer && arg == "ge") || (isLongIntroducer && arg == "guaranteeEntailments"))
		{
			m_grnteeEntlmntsOpt = true;
		}
		else if ((isShortIntroducer && arg == "va") || (isLongIntroducer && arg == "validate"))
		{
			m_validateOpt = true;
		}
		else if ((isShortIntroducer && arg == "e") || (isLongIntroducer && arg == "export"))
		{
			m_exportOpt = true;
		}
		else if ((isShortIntroducer && arg == "ii") || (isLongIntroducer && arg == "includeInferred"))
		{
			m_infStmtAction = InferredStmtsAction::include;
		}
		else if ((isShortIntroducer && arg == "id") || (isLongIntroducer && arg == "includeDeleted"))
		{
			m_delStmtAction = DeletedStmtsAction::include;
		}
		else if (!isShortIntroducer && !isLongIntroducer)
		{
			if (size(m_exportFilePath) > 0)
			{
				throw UsageException(format{"Error:  Two file names specified:  %1% and %2%"}
					% argList[i] % m_exportFilePath);
			}
			m_exportFilePath = argList[i];
		}
		else if ((isShortIntroducer && arg == "dr") || (isLongIntroducer && arg == "dumpRsrc"))
		{
			m_rsrcDumpOpt = DumpFormat::k_terseDump;
		}
		else if ((isShortIntroducer && arg == "drv") || (isLongIntroducer && arg == "dumpRsrcVerbose"))
		{
			m_rsrcDumpOpt = DumpFormat::k_verboseDump;
		}
		else if ((isShortIntroducer && arg == "drvv") || (isLongIntroducer && arg == "dumpRsrcVeryVerbose"))
		{
			m_rsrcDumpOpt = DumpFormat::k_veryVerboseDump;
		}
		else if ((isShortIntroducer && arg == "ds") || (isLongIntroducer && arg == "dumpStmt"))
		{
			m_stmtDumpOpt = DumpFormat::k_terseDump;
		}
		else if ((isShortIntroducer && arg == "dsv") || (isLongIntroducer && arg == "dumpStmtVerbose"))
		{
			m_stmtDumpOpt = DumpFormat::k_verboseDump;
		}
		else if ((isShortIntroducer && arg == "dsvv") || (isLongIntroducer && arg == "dumpStmtVeryVerbose"))
		{
			m_stmtDumpOpt = DumpFormat::k_veryVerboseDump;
		}
		else
		{
			throw UsageException(format{"Error:  Unrecognized option:  %1%"} % argList[i]);
		}
	}

	int numCommandsGiven = (int) m_helpOpt + (int) m_versionOpt
		+ (int) m_createKbOpt + (int) m_countClassesOpt
		+ (int) m_listClassesOpt + (int) m_statisticsOpt
		+ (int) m_relExCapOpt + (int) m_grnteeEntlmntsOpt
		+ (int) m_validateOpt + (int) m_exportOpt;
	numCommandsGiven += (m_rsrcDumpOpt == DumpFormat::k_noDump) ? 0 : 1;
	numCommandsGiven += (m_stmtDumpOpt == DumpFormat::k_noDump) ? 0 : 1;
	if (numCommandsGiven < 1)
	{
		throw UsageException("Error:  No command specified");
	}
	else if (numCommandsGiven > 1)
	{
		throw UsageException("Error:  More than one command specified");
	}
	else if (m_exportOpt && size(m_exportFilePath) <= 0)
	{
		throw UsageException("Error:  Export option requires a file path");
	}
	else if (!m_exportOpt && size(m_exportFilePath) > 0)
	{
		throw UsageException("Error:  A file path is valid only in "
			"combination with the export option");
	}
	else if (!m_exportOpt && (m_infStmtAction == InferredStmtsAction::include
		|| m_delStmtAction == DeletedStmtsAction::include))
	{
		throw UsageException("Error:  The 'ii', and 'id' options "
			"are valid only in combination with the export command");
	}
}

string pmnt::KbAdmin::stripOptionIntroducer(string_view fullArg,
	bool& isShortIntroducer, bool& isLongIntroducer)
{
	string result;
	isShortIntroducer = false;
	isLongIntroducer = false;
	bool shortAndLongIntroducersAreDifferent = (strcmp(k_loi, k_soi) != 0);
	if (shortAndLongIntroducersAreDifferent && fullArg.find(k_loi) == 0)
	{
		isLongIntroducer = true;
		result = fullArg.substr(strlen(k_loi));
	}
	else if (fullArg.find(k_soi) == 0)
	{
		isShortIntroducer = true;
		isLongIntroducer = !shortAndLongIntroducersAreDifferent;
		result = fullArg.substr(strlen(k_soi));
	}
	return to_lower_copy(result);
}

void pmnt::KbAdmin::run()
{
	double timeInMicroSec = 0;

	if (m_helpOpt)
	{
		printUsage("");
	}
	else if (m_versionOpt)
	{
		printVersion();
	}
	else if (m_createKbOpt)
	{
		if (determineDisposition() != KbDisposition::k_kbDoesNotExist)
		{
			cout << "Unable to create a new KB:  Some or all of "
				"the KB files already exist." << endl;
		}
		else
		{
			HiResTimer timer;

			KbConfig config;
			config.readFromFile();
			config.kbDirectoryPath(k_kbDir);
			config.readOnly(false);
			KbInstance kb(config);

			timer.stop();
			timeInMicroSec = timer.getMicroSec();
		}
	}
	else if (m_relExCapOpt)
	{
		KbDisposition disp = determineDisposition();
		if (disp != KbDisposition::k_kbExists)
		{
			cout << "Unable to find the KB." << endl;
		}
		else
		{
			HiResTimer timer;

			KbConfig config;
			config.readFromFile();
			config.kbDirectoryPath(k_kbDir);
			config.readOnly(false);
			KbInstance kb(config);

			kb.releaseExcessCapacity();

			timer.stop();
			timeInMicroSec = timer.getMicroSec();
		}
	}
	else if (m_grnteeEntlmntsOpt)
	{
		KbDisposition disp = determineDisposition();
		if (disp != KbDisposition::k_kbExists)
		{
			cout << "Unable to find the KB." << endl;
		}
		else
		{
			HiResTimer timer;

			KbConfig config;
			config.readFromFile();
			config.kbDirectoryPath(k_kbDir);
			config.readOnly(false);
			config.runAllRulesAtStartup(true);

			KbInstance kb(config);

			timer.stop();
			timeInMicroSec = timer.getMicroSec();
		}
	}
	else
	{
		KbDisposition disp = determineDisposition();
		if (disp != KbDisposition::k_kbExists)
		{
			cout << "Unable to open the KB:  Either it does not "
				"exist, or some of the KB files are missing." << endl;
		}
		else
		{
			KbConfig config;
			config.readFromFile();
			config.kbDirectoryPath(k_kbDir);
			config.readOnly(true);
			KbInstance kb(config);

			HiResTimer timer;

			if (m_countClassesOpt)
			{
				listClasses(kb, false);
			}
			else if (m_listClassesOpt)
			{
				listClasses(kb, true);
			}
			else if (m_statisticsOpt)
			{
				showStatistics(kb);
			}
			else if (m_validateOpt)
			{
				kb.validate(cout);
			}
			else if (m_exportOpt)
			{
				exportKB(kb);
			}
			else if (m_rsrcDumpOpt == DumpFormat::k_terseDump)
			{
				kb.printResources(cout, false, false);
			}
			else if (m_rsrcDumpOpt == DumpFormat::k_verboseDump)
			{
				kb.printResources(cout, true, false);
			}
			else if (m_rsrcDumpOpt == DumpFormat::k_veryVerboseDump)
			{
				kb.printResources(cout, true, true);
			}
			else if (m_stmtDumpOpt == DumpFormat::k_terseDump)
			{
				kb.printStatements(cout, false, false);
			}
			else if (m_stmtDumpOpt == DumpFormat::k_verboseDump)
			{
				kb.printStatements(cout, true, false);
			}
			else if (m_stmtDumpOpt == DumpFormat::k_veryVerboseDump)
			{
				kb.printStatements(cout, true, true);
			}

			timer.stop();
			timeInMicroSec = timer.getMicroSec();
		}
	}

	if (m_timeOpt)
	{
		cout << "Elapsed time = " << timeInMicroSec << " micro-seconds." << endl;
	}
}

void pmnt::KbAdmin::listClasses(KbInstance& kb, bool printClassListings)
{
	if (printClassListings)
	{
		cout << "Classes:" << endl;
	}

	ResourceId rdfTypeRsrcId = kb.uriLib().m_rdfType.id();
	ResourceId owlClassRsrcId = kb.uriLib().m_owlClass.id();
	ResourceId rdfsClassRsrcId = kb.uriLib().m_rdfsClass.id();

	size_t count = listClasses(kb, printClassListings, owlClassRsrcId, rdfTypeRsrcId);
	count += listClasses(kb, printClassListings, rdfsClassRsrcId, rdfTypeRsrcId);

	if (printClassListings)
	{
		cout << "Done" << endl;
	}
	else
	{
		cout << count << " classes" << endl;
	}
}

size_t pmnt::KbAdmin::listClasses(KbInstance& kb, bool printClassListings,
	ResourceId classRsrcId, ResourceId rdfTypeRsrcId)
{
	size_t count = 0;
	if (rdfTypeRsrcId != k_nullRsrcId && classRsrcId != k_nullRsrcId)
	{
		StmtIterator end = kb.end();
		for (StmtIterator iter = kb.find(k_nullRsrcId, rdfTypeRsrcId, classRsrcId);
			iter != end; ++iter)
		{
			if (printClassListings)
			{
				const RsrcChar* pUri = kb.rsrcIdToUri(iter.statement().getSubjectId());
				if (pUri != nullptr)
				{
					cout << "  " << pUri << endl;
				}
			}

			++count;
		}
	}
	return count;
}

void pmnt::KbAdmin::showStatistics(KbInstance& kb)
{
	size_t totalCount, numDel, numInferred, numDelAndInferred, numHidden, numVirtual;
	kb.countStmts(totalCount, numDel, numInferred, numDelAndInferred, numHidden, numVirtual);
	size_t basePlusInferredCount = totalCount - numDel;
	size_t baseCount = basePlusInferredCount - numInferred + numDelAndInferred - numHidden;
	size_t basePlusHiddenCount = baseCount+ numHidden;

	double pctUnusedUriCapacity, pctUnusedRsrcCapacity, pctUnusedStmtCapacity;
	kb.getExcessCapacity(pctUnusedUriCapacity, pctUnusedRsrcCapacity, pctUnusedStmtCapacity);
	pctUnusedUriCapacity *= 100.0;
	pctUnusedRsrcCapacity *= 100.0;
	pctUnusedStmtCapacity *= 100.0;

	::std::streamsize oldPrecision = cout.precision(1);
	::std::ios_base::fmtflags oldFmtFlags = cout.flags();
	cout.setf(::std::ios_base::fixed);

	cout << "Resource Count:                   " << kb.rsrcCount() << endl;
	cout << "Average Resource Length:          " << kb.averageRsrcLength() << " chars" << endl;
	cout << "Statement Count:                  " << baseCount << endl;
	cout << "   Including Hidden:              " << basePlusHiddenCount << endl;
	cout << "   Including Inferred:            " << basePlusInferredCount << endl;
	cout << "   Including Inferred & Deleted:  " << totalCount << endl;
	cout << "   Virtual statements:            " << numVirtual << endl;
	cout << "Percent unused capacity:" << endl;
	cout << "   URI and literal store:         " << setw(4) << pctUnusedUriCapacity << "%" << endl;
	cout << "   Resource table:                " << setw(4) << pctUnusedRsrcCapacity << "%" << endl;
	cout << "   Statement table:               " << setw(4) << pctUnusedStmtCapacity << "%" << endl;

	cout.flags(oldFmtFlags);
	cout.precision(oldPrecision);
}

void pmnt::KbAdmin::exportKB(KbInstance& kb)
{
	if (exists(m_exportFilePath))
	{
		throw Exception(format{"Error:  File \"%1%\" already exists"} % m_exportFilePath);
	}

	bfs::ofstream s(m_exportFilePath, ios_base::out);
	if (!s)
	{
		throw Exception(format{"Error:  Unable to open file \"%1%\""} % m_exportFilePath);
	}

	kb.dumpKbAsNTriples(s, m_infStmtAction, m_delStmtAction);
}

pmnt::KbDisposition pmnt::KbAdmin::determineDisposition()
{
	KbConfig config;
	config.readFromFile();
	config.kbDirectoryPath(k_kbDir);
	return KbInstance::determineDisposition(config);
}
