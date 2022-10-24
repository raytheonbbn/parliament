// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2019, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_LOG_CONFIG_H_INCLUDED)
#define PARLIAMENT_LOG_CONFIG_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <boost/filesystem/path.hpp>
#include <functional>
#include <map>
#include <string>

PARLIAMENT_NAMESPACE_BEGIN

class LogConfig
{
public:
	using Path = ::boost::filesystem::path;

	// key = channel, value = log level
	using LogChannelMap = ::std::map< ::std::string, ::std::string >;

	PARLIAMENT_EXPORT LogConfig();
	PARLIAMENT_EXPORT LogConfig(const LogConfig&);
	PARLIAMENT_EXPORT LogConfig& operator=(const LogConfig&);
	PARLIAMENT_EXPORT LogConfig(LogConfig&&);
	PARLIAMENT_EXPORT LogConfig& operator=(LogConfig&&);
	PARLIAMENT_EXPORT ~LogConfig();

	PARLIAMENT_EXPORT void readFromFile();

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
	Path logFilePath() const
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

private:
	using EntryHandler = ::std::function<void(const ::std::string&, uint32, LogConfig&)>;
	using ConfigEntryMap = ::std::map<::std::string, EntryHandler>;	// Maps key to handler

	static Path convertUtf8ToLogPath(const ::std::string& value);

	// Implementation note:  It may strike you as odd that this is an instance member
	// rather than a static member as in KbConfig.  The reason is that unlike KbConfig,
	// LogConfig is created at static initialization time, and if this is a static,
	// there are issues of static initialization order between LogConfig and the log
	// implementation.  Since this class is entirely internal to KbCore and will be
	// used exactly once at static initialization time, this seemed like the best
	// way to avoid those issues.
	ConfigEntryMap	m_ceMap;

	bool				m_logToConsole;
	bool				m_logConsoleAsynchronous;
	bool				m_logConsoleAutoFlush;
	bool				m_logToFile;
	Path				m_logFilePath;
	bool				m_logFileAsynchronous;
	bool				m_logFileAutoFlush;
	size_t			m_logFileRotationSize;
	size_t			m_logFileMaxAccumSize;
	size_t			m_logFileMinFreeSpace;
	::std::string	m_logFileRotationTimePoint;
	::std::string	m_logLevel;
	LogChannelMap	m_logChannelLevel;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_LOG_CONFIG_H_INCLUDED
