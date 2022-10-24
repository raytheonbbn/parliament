// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2019, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_KB_CONFIG_H_INCLUDED)
#define PARLIAMENT_KB_CONFIG_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <boost/filesystem/path.hpp>
#include <functional>
#include <map>
#include <string>

PARLIAMENT_NAMESPACE_BEGIN

// Corresponds to Java's java.util.concurrent.TimeUnit
enum class TimeUnit
{
	k_nanoSec, k_microSec, k_milliSec, k_sec, k_min, k_hour, k_day
};

class KbConfig
{
public:
	using Path = ::boost::filesystem::path;

	PARLIAMENT_EXPORT KbConfig();
	PARLIAMENT_EXPORT KbConfig(const KbConfig&);
	PARLIAMENT_EXPORT KbConfig& operator=(const KbConfig&);
	PARLIAMENT_EXPORT KbConfig(KbConfig&&);
	PARLIAMENT_EXPORT KbConfig& operator=(KbConfig&&);
	PARLIAMENT_EXPORT ~KbConfig();

	PARLIAMENT_EXPORT void readFromFile();

	// Directory containing the Parliament KB files
	Path kbDirectoryPath() const
		{ return m_kbDirectoryPath; }
	PARLIAMENT_EXPORT void kbDirectoryPath(const Path& newValue);

	// Name of the memory-mapped statement file
	Path stmtFilePath() const;
	::std::string stmtFileName() const
		{ return m_stmtFileName; }
	void stmtFileName(const ::std::string& newValue)
		{ m_stmtFileName = newValue; }

	// Name of the memory-mapped resource file
	Path rsrcFilePath() const;
	::std::string rsrcFileName() const
		{ return m_rsrcFileName; }
	void rsrcFileName(const ::std::string& newValue)
		{ m_rsrcFileName = newValue; }

	// Name of the memory-mapped URI string file
	Path uriTableFilePath() const;
	::std::string uriTableFileName() const
		{ return m_uriTableFileName; }
	void uriTableFileName(const ::std::string& newValue)
		{ m_uriTableFileName = newValue; }

	// Name of the Berkeley DB URI-to-rsrcId file
	Path uriToIntFilePath() const;
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

	// The increment by which the resource capacity should be grown whenever more space is required.
	size_t rsrcGrowthIncrement() const
		{ return m_rsrcGrowthIncrement; }
	void rsrcGrowthIncrement(size_t newValue)
		{ m_rsrcGrowthIncrement = newValue; }

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

	// The increment by which the statement capacity should be grown whenever more space is required.
	size_t stmtGrowthIncrement() const
		{ return m_stmtGrowthIncrement; }
	void stmtGrowthIncrement(size_t newValue)
		{ m_stmtGrowthIncrement = newValue; }

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

	// How long to allow a query to run before aborting it.
	size_t timeoutDuration() const
		{ return m_timeoutDuration; }
	void timeoutDuration(size_t newValue)
		{ m_timeoutDuration = newValue; }

	// Unit for timeout duration
	TimeUnit timeoutUnit() const
		{ return m_timeoutUnit; }
	::std::string javaTimeoutUnit() const;
	void timeoutUnit(const ::std::string& newValue);

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

	void disableAllRules()
		{ setStatusOfAllRules(false); }
	void enableAllRules()
		{ setStatusOfAllRules(true); }
	void setStatusOfAllRules(bool enableAllRules);

	const KbConfig& ensureKbDirExists() const;

private:
	using EntryHandler = ::std::function<void(const ::std::string&, uint32, KbConfig&)>;
	using ConfigEntryMap = ::std::map<::std::string, EntryHandler>;	// Maps key to handler

	static bool initConfigEntryMap();
	static void unsynchronizedInitConfigEntryMap();

	static ConfigEntryMap g_ceMap;
	static bool g_isConfigEntryMapInitialized;

	Path				m_kbDirectoryPath;
	::std::string	m_stmtFileName;
	::std::string	m_rsrcFileName;
	::std::string	m_uriTableFileName;
	::std::string	m_uriToIntFileName;

	bool				m_readOnly;
	size_t			m_fileSyncTimerDelay;
	size_t			m_initialRsrcCapacity;
	size_t			m_avgRsrcLen;
	size_t			m_rsrcGrowthIncrement;
	double			m_rsrcGrowthFactor;
	size_t			m_initialStmtCapacity;
	size_t			m_stmtGrowthIncrement;
	double			m_stmtGrowthFactor;
	::std::string	m_bdbCacheSize;

	bool				m_normalizeTypedStringLiterals;

	size_t			m_timeoutDuration;
	TimeUnit			m_timeoutUnit;

	bool				m_runAllRulesAtStartup;
	bool				m_enableSWRLRuleEngine;

	bool				m_isSubclassRuleOn;
	bool				m_isSubpropertyRuleOn;
	bool				m_isDomainRuleOn;
	bool				m_isRangeRuleOn;
	bool				m_isEquivalentClassRuleOn;
	bool				m_isEquivalentPropRuleOn;
	bool				m_isInverseOfRuleOn;
	bool				m_isSymmetricPropRuleOn;
	bool				m_isFunctionalPropRuleOn;
	bool				m_isInvFunctionalPropRuleOn;
	bool				m_isTransitivePropRuleOn;

	bool				m_inferRdfsClass;
	bool				m_inferOwlClass;
	bool				m_inferRdfsResource;
	bool				m_inferOwlThing;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_KB_CONFIG_H_INCLUDED
