// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_KBADMIN_H_INCLUDED)
#define PARLIAMENT_KBADMIN_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <boost/filesystem/path.hpp>
#include <string>
#include <string_view>

namespace bbn::parliament
{

class KbInstance;

class KbAdmin
{
public:
	KbAdmin(uint32 numArgs, const char*const* argList);
	KbAdmin(const KbAdmin&) = delete;
	KbAdmin& operator=(const KbAdmin&) = delete;
	KbAdmin(KbAdmin&&) = delete;
	KbAdmin& operator=(KbAdmin&&) = delete;

	static void printVersion();
	static void printUsage(::std::string_view msg);
	void run();

private:
	enum class DumpFormat
	{
		k_noDump,
		k_terseDump,
		k_verboseDump,
		k_veryVerboseDump
	};

	static const char k_soi[];	// Short Option Introducer
	static const char k_loi[];	// Long Option Introducer

	bool								m_helpOpt;				// Print help
	bool								m_versionOpt;			// Print version information
	bool								m_timeOpt;				// Time the specified command
	bool								m_createKbOpt;			// Create and initialize a KB
	bool								m_countClassesOpt;	// Count the number of RDF classes
	bool								m_listClassesOpt;		// List the RDF classes
	bool								m_statisticsOpt;		// Print KB statistics
	bool								m_relExCapOpt;			// Release excess file capacity
	bool								m_grnteeEntlmntsOpt;	// guarantee entailments
	bool								m_validateOpt;			// check for file corruption
	bool								m_exportOpt;			// Export KB as n-triples
	InferredStmtsAction			m_infStmtAction;		// Include inferred statements in KB export?
	DeletedStmtsAction			m_delStmtAction;		// Include deleted statements in KB export?
	::boost::filesystem::path	m_exportFilePath;		// Path of file in which to create export
	DumpFormat						m_rsrcDumpOpt;			// Format in which to dump resources
	DumpFormat						m_stmtDumpOpt;			// Format in which to dump statements

	static ::std::string stripOptionIntroducer(::std::string_view fullArg,
		bool& isShortIntroducer, bool& isLongIntroducer);
	void listClasses(KbInstance& kb, bool printClassListings);
	size_t listClasses(KbInstance& kb, bool printClassListings, ResourceId rdfsClass,
		ResourceId rdfType);
	void showStatistics(KbInstance& kb);
	void exportKB(KbInstance& kb);
	static KbDisposition determineDisposition();
};

}	// namespace end

#endif // !PARLIAMENT_KBADMIN_H_INCLUDED
