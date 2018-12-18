// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_CONFIG_H_INCLUDED)
#define PARLIAMENT_CONFIG_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <boost/filesystem/path.hpp>
#include <map>
#include <string>

PARLIAMENT_NAMESPACE_BEGIN

class Config
{
public:
	// key = channel, value = log level
	using LogChannelMap = ::std::map< ::std::string, ::std::string >;

	static const TChar k_defaultConfigFileName[];

	PARLIAMENT_EXPORT static Config readFromFile();
	PARLIAMENT_EXPORT Config();
	PARLIAMENT_EXPORT Config(const Config&) = default;
	PARLIAMENT_EXPORT Config& operator=(const Config&) = default;
	PARLIAMENT_EXPORT Config(Config&&) = default;
	PARLIAMENT_EXPORT Config& operator=(Config&&) = default;
	PARLIAMENT_EXPORT ~Config() = default;

	// Enables logging to the console
	bool logToConsole() const
		{ return m_logToConsole; }
	void logToConsole(bool newValue)
		{ m_logToConsole = newValue; }

	// Sets whether console logging is asynchronous
	bool logConsoleAsynchronous() const
		{ return m_logConsoleAsynchronous; }
	void logConsoleAsynchronous(bool newValue)
		{ m_logConsoleAsynchronous = newValue; }

	// Enables auto-flushing of the console log
	bool logConsoleAutoFlush() const
		{ return m_logConsoleAutoFlush; }
	void logConsoleAutoFlush(bool newValue)
		{ m_logConsoleAutoFlush = newValue; }

	// Enables logging to a file
	bool logToFile() const
		{ return m_logToFile; }
	void logToFile(bool newValue)
		{ m_logToFile = newValue; }

	// Sets the log file name (and path).  %N = log file number, %Y-%m-%d = date, %H-%M-%S = time
	::boost::filesystem::path logFilePath() const
		{ return m_logFilePath; }
	void logFilePath(const ::std::string& newValue)
		{ m_logFilePath = newValue; }

	// Sets whether file logging is asynchronous
	bool logFileAsynchronous() const
		{ return m_logFileAsynchronous; }
	void logFileAsynchronous(bool newValue)
		{ m_logFileAsynchronous = newValue; }

	// Enables auto-flushing of the log file
	bool logFileAutoFlush() const
		{ return m_logFileAutoFlush; }
	void logFileAutoFlush(bool newValue)
		{ m_logFileAutoFlush = newValue; }

	// Sets the approximate size at which point log file rotation is performed
	size_t logFileRotationSize() const
		{ return m_logFileRotationSize; }
	void logFileRotationSize(size_t newValue)
		{ m_logFileRotationSize = newValue; }

	// Sets the maximum accumulated size of rotated log files, after which old files are deleted
	size_t logFileMaxAccumSize() const
		{ return m_logFileMaxAccumSize; }
	void logFileMaxAccumSize(size_t newValue)
		{ m_logFileMaxAccumSize = newValue; }

	// Sets the minimum free space on the log file volume, under which old files are deleted
	size_t logFileMinFreeSpace() const
		{ return m_logFileMinFreeSpace; }
	void logFileMinFreeSpace(size_t newValue)
		{ m_logFileMinFreeSpace = newValue; }

	// Sets the time of day at which log files are rotated, in HH:MM:SS format
	::std::string logFileRotationTimePoint() const
		{ return m_logFileRotationTimePoint; }
	void logFileRotationTimePoint(const ::std::string& newValue)
		{ m_logFileRotationTimePoint = newValue; }

	// Sets the global logging level
	::std::string logLevel() const
		{ return m_logLevel; }
	void logLevel(const ::std::string& newValue)
		{ m_logLevel = newValue; }

	// Per-channel log level settings
	const LogChannelMap& logChannelLevels() const
		{ return m_logChannelLevel; }
	void clearLogChannelLevel()
		{ m_logChannelLevel.clear(); }
	void addLogChannelLevel(const ::std::string& channel, const ::std::string& level)
		{ m_logChannelLevel[channel] = level; }

	// Directory containing the Parliament KB files
	::boost::filesystem::path kbDirectoryPath() const
		{ return m_kbDirectoryPath; }
	void kbDirectoryPath(const ::boost::filesystem::path& newValue)
		{ m_kbDirectoryPath = newValue; }

	// Name of the memory-mapped statement file
	::boost::filesystem::path stmtFilePath() const;
	::std::string stmtFileName() const
		{ return m_stmtFileName; }
	void stmtFileName(const ::std::string& newValue)
		{ m_stmtFileName = newValue; }

	// Name of the memory-mapped resource file
	::boost::filesystem::path rsrcFilePath() const;
	::std::string rsrcFileName() const
		{ return m_rsrcFileName; }
	void rsrcFileName(const ::std::string& newValue)
		{ m_rsrcFileName = newValue; }

	// Name of the memory-mapped URI string file
	::boost::filesystem::path uriTableFilePath() const;
	::std::string uriTableFileName() const
		{ return m_uriTableFileName; }
	void uriTableFileName(const ::std::string& newValue)
		{ m_uriTableFileName = newValue; }

	// Name of the Berkeley DB URI-to-rsrcId file
	::boost::filesystem::path uriToIntFilePath() const;
	::std::string uriToIntFileName() const
		{ return m_uriToIntFileName; }
	void uriToIntFileName(const ::std::string& newValue)
		{ m_uriToIntFileName = newValue; }

	// Whether to open the KB in read-only mode.
	bool readOnly() const
		{ return m_readOnly; }
	void readOnly(bool newValue)
		{ m_readOnly = newValue; }

	// The number of milliseconds between asynchronous syncs of the KB files
	// to disk.  Set to zero to disable flushing the files to disk.
	size_t fileSyncTimerDelay() const
		{ return m_fileSyncTimerDelay; }
	void fileSyncTimerDelay(size_t newValue)
		{ m_fileSyncTimerDelay = newValue; }

	// The initial # of resources for which space should be allocated
	size_t initialRsrcCapacity() const
		{ return m_initialRsrcCapacity; }
	void initialRsrcCapacity(size_t newValue)
		{ m_initialRsrcCapacity = newValue; }

	// The expected average URI length
	size_t avgRsrcLen() const
		{ return m_avgRsrcLen; }
	void avgRsrcLen(size_t newValue)
		{ m_avgRsrcLen = newValue; }

	// The factor by which the resource capacity should be grown whenever more space is required.
	double rsrcGrowthFactor() const
		{ return m_rsrcGrowthFactor; }
	void rsrcGrowthFactor(double newValue)
		{ m_rsrcGrowthFactor = newValue; }

	// The initial # of statements for which space should be allocated
	size_t initialStmtCapacity() const
		{ return m_initialStmtCapacity; }
	void initialStmtCapacity(size_t newValue)
		{ m_initialStmtCapacity = newValue; }

	// The factor by which the statement capacity should be grown whenever more space is required.
	double stmtGrowthFactor() const
		{ return m_stmtGrowthFactor; }
	void stmtGrowthFactor(double newValue)
		{ m_stmtGrowthFactor = newValue; }

	// Size of the Berkeley DB cache
	::std::string bdbCacheSize() const
		{ return m_bdbCacheSize; }
	void bdbCacheSize(const ::std::string& newValue)
		{ m_bdbCacheSize = newValue; }

	// Whether to enforce equivalence of plain and typed string literals.
	bool normalizeTypedStringLiterals() const
		{ return m_normalizeTypedStringLiterals; }
	void normalizeTypedStringLiterals(bool newValue)
		{ m_normalizeTypedStringLiterals = newValue; }

	// Whether to ensure all entailments at startup, or assume entailments are correct from previous runs.
	bool runAllRulesAtStartup() const
		{ return m_runAllRulesAtStartup; }
	void runAllRulesAtStartup(bool newValue)
		{ m_runAllRulesAtStartup = newValue; }

	// Whether to turn on the SWRL Trigger rule
	bool enableSWRLRuleEngine() const
		{ return m_enableSWRLRuleEngine; }
	void enableSWRLRuleEngine(bool newValue)
		{ m_enableSWRLRuleEngine = newValue; }

	// Whether to turn on the Subclass rule
	bool isSubclassRuleOn() const
		{ return m_isSubclassRuleOn; }
	void isSubclassRuleOn(bool newValue)
		{ m_isSubclassRuleOn = newValue; }

	// Whether to turn on the Subproperty rule
	bool isSubpropertyRuleOn() const
		{ return m_isSubpropertyRuleOn; }
	void isSubpropertyRuleOn(bool newValue)
		{ m_isSubpropertyRuleOn = newValue; }

	// Whether to turn on the Domain rule
	bool isDomainRuleOn() const
		{ return m_isDomainRuleOn; }
	void isDomainRuleOn(bool newValue)
		{ m_isDomainRuleOn = newValue; }

	// Whether to turn on the Range rule
	bool isRangeRuleOn() const
		{ return m_isRangeRuleOn; }
	void isRangeRuleOn(bool newValue)
		{ m_isRangeRuleOn = newValue; }

	// Whether to turn on the Equivalent Class rule
	bool isEquivalentClassRuleOn() const
		{ return m_isEquivalentClassRuleOn; }
	void isEquivalentClassRuleOn(bool newValue)
		{ m_isEquivalentClassRuleOn = newValue; }

	// Whether to turn on the Equivalent Property rule
	bool isEquivalentPropRuleOn() const
		{ return m_isEquivalentPropRuleOn; }
	void isEquivalentPropRuleOn(bool newValue)
		{ m_isEquivalentPropRuleOn = newValue; }

	// Whether to turn on the Inverse-Of rule
	bool isInverseOfRuleOn() const
		{ return m_isInverseOfRuleOn; }
	void isInverseOfRuleOn(bool newValue)
		{ m_isInverseOfRuleOn = newValue; }

	// Whether to turn on the Symmetric Property rule
	bool isSymmetricPropRuleOn() const
		{ return m_isSymmetricPropRuleOn; }
	void isSymmetricPropRuleOn(bool newValue)
		{ m_isSymmetricPropRuleOn = newValue; }

	// Whether to turn on the Functional Property rule
	bool isFunctionalPropRuleOn() const
		{ return m_isFunctionalPropRuleOn; }
	void isFunctionalPropRuleOn(bool newValue)
		{ m_isFunctionalPropRuleOn = newValue; }

	// Whether to turn on the Inverse Functional Property rule
	bool isInvFunctionalPropRuleOn() const
		{ return m_isInvFunctionalPropRuleOn; }
	void isInvFunctionalPropRuleOn(bool newValue)
		{ m_isInvFunctionalPropRuleOn = newValue; }

	// Whether to turn on the Transitive Property rule
	bool isTransitivePropRuleOn() const
		{ return m_isTransitivePropRuleOn; }
	void isTransitivePropRuleOn(bool newValue)
		{ m_isTransitivePropRuleOn = newValue; }

	// Whether to infer rdfs:Class based on subclass statements
	bool inferRdfsClass() const
		{ return m_inferRdfsClass; }
	void inferRdfsClass(bool newValue)
		{ m_inferRdfsClass = newValue; }

	// Whether to infer owl:Class based on subclass statements
	bool inferOwlClass() const
		{ return m_inferOwlClass; }
	void inferOwlClass(bool newValue)
		{ m_inferOwlClass = newValue; }

	// Whether to infer rdfs:Resource based on subclass statements
	bool inferRdfsResource() const
		{ return m_inferRdfsResource; }
	void inferRdfsResource(bool newValue)
		{ m_inferRdfsResource = newValue; }

	// Whether to infer owl:Thing based on subclass statements
	bool inferOwlThing() const
		{ return m_inferOwlThing; }
	void inferOwlThing(bool newValue)
		{ m_inferOwlThing = newValue; }

  // How long to allow a query to run before aborting it.
  size_t timeoutDuration() const
    { return m_timeoutDuration; }
  void timeoutDuration(size_t newValue)
    { m_timeoutDuration = newValue; }

  // Unit for timeout duration
  ::std::string timeoutUnit() const
    { return { m_timeoutUnit }; }
  void timeoutUnit(::std::string newValue)
    { m_timeoutUnit = { newValue }; }

	void disableAllRules();

	const Config& ensureKbDirExists() const;

#if defined(PARLIAMENT_UNIT_TEST)
	static ::boost::filesystem::path testGetConfigFilePath()
		{ return getConfigFilePath(); }
	static bool testIsBlankOrCommentLine(const ::std::string& line)
		{ return isBlankOrCommentLine(line); }
	static size_t testParseUnsigned(const ::std::string& s, uint32 lineNum)
		{ return parseUnsigned(s, lineNum); }
	static double testParseDouble(const ::std::string& s, uint32 lineNum)
		{ return parseDouble(s, lineNum); }
	static bool testParseBool(const ::std::string& s, uint32 lineNum)
		{ return parseBool(s, lineNum); }
#endif

private:
	static TString tGetEnvVar(const TChar* pVarName);
	static ::boost::filesystem::path getConfigFilePath();
	static bool isBlankOrCommentLine(const ::std::string& line);
	static void splitAtFirstEquals(/* in */ const ::std::string& line,
		/* in */ uint32 lineNum, /* out */ ::std::string& key,
		/* out */ ::std::string& value);
	static void parseKeyValuePair(const ::std::string& key,
		const ::std::string& value, uint32 lineNum, /* in-out */ Config& cp);
	static size_t parseUnsigned(const ::std::string& s, uint32 lineNum);
	static double parseDouble(const ::std::string& s, uint32 lineNum);
	static bool parseBool(const ::std::string& s, uint32 lineNum);

	bool								m_logToConsole;
	bool								m_logConsoleAsynchronous;
	bool								m_logConsoleAutoFlush;
	bool								m_logToFile;
	::boost::filesystem::path	m_logFilePath;
	bool								m_logFileAsynchronous;
	bool								m_logFileAutoFlush;
	size_t							m_logFileRotationSize;
	size_t							m_logFileMaxAccumSize;
	size_t							m_logFileMinFreeSpace;
	::std::string					m_logFileRotationTimePoint;
	::std::string					m_logLevel;
	LogChannelMap					m_logChannelLevel;

	::boost::filesystem::path	m_kbDirectoryPath;
	::std::string					m_stmtFileName;
	::std::string					m_rsrcFileName;
	::std::string					m_uriTableFileName;
	::std::string					m_uriToIntFileName;

	bool								m_readOnly;
	size_t							m_fileSyncTimerDelay;
	size_t							m_initialRsrcCapacity;
	size_t							m_avgRsrcLen;
	double							m_rsrcGrowthFactor;
	size_t							m_initialStmtCapacity;
	double							m_stmtGrowthFactor;
	::std::string					m_bdbCacheSize;

	bool								m_normalizeTypedStringLiterals;

	bool								m_runAllRulesAtStartup;
	bool								m_enableSWRLRuleEngine;

	bool								m_isSubclassRuleOn;
	bool								m_isSubpropertyRuleOn;
	bool								m_isDomainRuleOn;
	bool								m_isRangeRuleOn;
	bool								m_isEquivalentClassRuleOn;
	bool								m_isEquivalentPropRuleOn;
	bool								m_isInverseOfRuleOn;
	bool								m_isSymmetricPropRuleOn;
	bool								m_isFunctionalPropRuleOn;
	bool								m_isInvFunctionalPropRuleOn;
	bool								m_isTransitivePropRuleOn;

	bool								m_inferRdfsClass;
	bool								m_inferOwlClass;
	bool								m_inferRdfsResource;
	bool								m_inferOwlThing;

  size_t                m_timeoutDuration;
  ::std::string       m_timeoutUnit;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_CONFIG_H_INCLUDED
