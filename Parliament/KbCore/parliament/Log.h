// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2016, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_LOG_H_INCLUDED)
#define PARLIAMENT_LOG_H_INCLUDED

#include "parliament/Platform.h"

#include <boost/log/sources/record_ostream.hpp>
#include <boost/log/sources/severity_channel_logger.hpp>
#include <boost/log/utility/setup/file.hpp>

PARLIAMENT_NAMESPACE_BEGIN

class Config;

enum class LogLevel { trace, debug, info, warn, error };

class Log
{
public:
	// Prevent instantiation:
	Log() = delete;
	Log(const Log&) = delete;
	Log& operator=(const Log&) = delete;
	Log(Log&&) = delete;
	Log& operator=(Log&&) = delete;
	~Log() = delete;

	using Source = ::boost::log::sources::severity_channel_logger_mt<LogLevel, ::std::string>;
	using RotationAtTimePoint = ::boost::log::sinks::file::rotation_at_time_point;

	// Always returns true to enable easy calling from initializer lists:
	static bool init(const Config& config);

	static Source getSource(const char* pChannelName);
	static Source getSource(const ::std::string& channelName);

	static RotationAtTimePoint rotTimeFromString(const ::std::string& rotTime);
	static LogLevel levelFromString(const ::std::string& level, bool& wasRecognized);
	static LogLevel levelFromString(const ::std::string& level);

private:
	static void unsynchronizedInit(const Config& config);
};

#define PMNT_LOG(logger, lvl) BOOST_LOG_STREAM_SEV(logger, lvl)

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_LOG_H_INCLUDED
