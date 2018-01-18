// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2016, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/Log.h"
#include "parliament/Config.h"
#include "parliament/ArrayLength.h"
#include "parliament/Exceptions.h"
#include "parliament/RegEx.h"
#include "parliament/Util.h"

#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/core/null_deleter.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/log/expressions.hpp>
#include <boost/log/sinks/async_frontend.hpp>
#include <boost/log/sinks/text_file_backend.hpp>
#include <boost/log/sinks/text_ostream_backend.hpp>
#include <boost/log/support/date_time.hpp>
#include <boost/log/utility/setup/common_attributes.hpp>
#include <boost/phoenix/bind.hpp>
#include <boost/range/numeric.hpp>
#include <boost/smart_ptr/shared_ptr.hpp>
#include <boost/thread/once.hpp>

#include <algorithm>
#include <cstddef>
#include <iostream>
#include <istream>
#include <iterator>
#include <map>
#include <ostream>

namespace ba = ::boost::algorithm;
namespace bl = ::boost::log;
namespace expr = ::boost::log::expressions;
namespace keywd = ::boost::log::keywords;
namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::boost::lexical_cast;
using ::boost::make_iterator_range;
using ::boost::make_shared;
using ::boost::posix_time::ptime;
using ::boost::shared_ptr;
using ::std::basic_istream;
using ::std::basic_ostream;
using ::std::basic_string;
using ::std::begin;
using ::std::end;
using ::std::size_t;
using ::std::string;

PARLIAMENT_NAMESPACE_BEGIN

BOOST_LOG_ATTRIBUTE_KEYWORD(logLevelKeywd, "Severity", LogLevel)
BOOST_LOG_ATTRIBUTE_KEYWORD(channelKeywd, "Channel", string)
BOOST_LOG_ATTRIBUTE_KEYWORD(threadIdKeywd, "ThreadID",
	bl::attributes::current_thread_id::value_type)

static ::boost::once_flag k_onceInitFlag = BOOST_ONCE_INIT;
static const char k_optionRegExStr[] = "^[ \t]*([0-9][0-9]?):([0-9][0-9]?):([0-9][0-9]?)[ \t]*$";
static const char*const k_levelStrings[] =
{
	"TRACE",
	"DEBUG",
	"INFO",
	"WARN",
	"ERROR"
};
static LogLevel g_level = LogLevel::info;
static ::std::map<string, LogLevel> g_channelToLevelMap;

#if 0
template<typename CharT, typename TraitsT>
static basic_istream<CharT, TraitsT>& operator>>(
	basic_istream<CharT, TraitsT>& strm, LogLevel& level)
{
	if (strm.good())
	{
		basic_string<CharT, TraitsT> levelStr;
		strm.setf(::std::ios_base::skipws);
		strm >> levelStr;
		level = Log::levelFromString(levelStr);
	}
	return strm;
}
#endif

template<typename CharT, typename TraitsT>
static basic_ostream<CharT, TraitsT>& operator<<(
	basic_ostream<CharT, TraitsT>& strm, const LogLevel level)
{
	if (strm.good())
	{
		size_t levelInt = static_cast<size_t>(level);
		if (levelInt < arrayLen(k_levelStrings))
		{
			strm << k_levelStrings[levelInt];
		}
		else
		{
			strm << levelInt;
		}
	}
	return strm;
}

// Wraps the given back end into a front end of the given type,
// sets the formatter, and registers the sink in the core.
template<template <typename...> class FEType, typename BEType>
static void setFrontEndSink(const shared_ptr<BEType>& pBackend)
{
	auto pSink = make_shared<FEType<BEType>>(pBackend);
	pSink->set_formatter(
		expr::format("%1% [%2%] %3% [%4%] %5%")
			% expr::format_date_time<ptime>("TimeStamp", "%Y-%m-%d %H:%M:%S.%f")
			% threadIdKeywd
			% logLevelKeywd
			% channelKeywd
			% expr::smessage
		);
	bl::core::get()->add_sink(pSink);
}

static bool logFilter(
	const bl::value_ref<LogLevel, tag::logLevelKeywd>& currentLevel,
	const bl::value_ref<string, tag::channelKeywd>& channel)
{
	bool result = false;
	if (currentLevel >= g_level)
	{
		result = true;
	}
	else if (!channel.empty())
	{
		auto it = g_channelToLevelMap.find(channel.get());
		if (it != g_channelToLevelMap.end() && currentLevel >= it->second)
		{
			result = true;
		}
	}
	return result;
}

PARLIAMENT_NAMESPACE_END

bool pmnt::Log::init(const Config& config)
{
	call_once(k_onceInitFlag, &unsynchronizedInit, config);
	return true;
}

void pmnt::Log::unsynchronizedInit(const Config& config)
{
	bl::add_common_attributes();

	if (config.logToConsole())
	{
		// Create the back end:
		auto pBackend = make_shared<bl::sinks::text_ostream_backend>();
		pBackend->add_stream(
			shared_ptr<::std::ostream>{&::std::clog, ::boost::null_deleter()});

		// Enable auto-flushing after each log record written:
		pBackend->auto_flush(config.logConsoleAutoFlush());

		if (config.logConsoleAsynchronous())
		{
			setFrontEndSink<bl::sinks::asynchronous_sink,
				bl::sinks::text_ostream_backend>(pBackend);
		}
		else
		{
			setFrontEndSink<bl::sinks::synchronous_sink,
				bl::sinks::text_ostream_backend>(pBackend);
		}
	}

	if (config.logToFile())
	{
		auto pBackend = make_shared<bl::sinks::text_file_backend>(
				keywd::file_name = config.logFilePath(),					// file name pattern
				keywd::target = config.logFilePath().parent_path(),	// log file directory
				keywd::auto_flush = config.logFileAutoFlush(),
				keywd::rotation_size = config.logFileRotationSize(),
				keywd::time_based_rotation = rotTimeFromString(config.logFileRotationTimePoint()),
				keywd::max_size = config.logFileMaxAccumSize(),
				keywd::min_free_space = config.logFileMinFreeSpace()
			);

		if (config.logFileAsynchronous())
		{
			setFrontEndSink<bl::sinks::asynchronous_sink,
				bl::sinks::text_file_backend>(pBackend);
		}
		else
		{
			setFrontEndSink<bl::sinks::synchronous_sink,
				bl::sinks::text_file_backend>(pBackend);
		}
	}

	g_level = levelFromString(config.logLevel());
	for (const auto& entry : config.logChannelLevels())
	{
		bool wasRecognized = false;
		LogLevel lvl = levelFromString(entry.second, wasRecognized);
		if (wasRecognized)
		{
			g_channelToLevelMap.insert(make_pair(entry.first, lvl));
		}
		else
		{
			format fmt("Unrecognized log level \"%1%\" for channel \"%2%\"");
			::std::cerr << (fmt % entry.second % entry.first) << ::std::endl;
		}
	}

	bl::core::get()->set_filter(::boost::phoenix::bind(&logFilter,
		logLevelKeywd.or_none(), channelKeywd.or_none()));
}

static bool isInBounds(pmnt::uint16 actualValue, pmnt::uint16 maxValue, const char* pName)
{
	bool result = true;
	if (actualValue > maxValue)
	{
		result = false;
		format fmt("In logFileRotationTimePoint option, %1% (%2%) must be between 0 and %3%, inclusive");
		::std::cerr << (fmt % pName % actualValue % maxValue) << ::std::endl;
	}
	return result;
}

pmnt::Log::RotationAtTimePoint pmnt::Log::rotTimeFromString(const string& rotTime)
{
	RegEx rex = compileRegEx(k_optionRegExStr);
	SMatch captures;
	if (regExMatch(rotTime, captures, rex))
	{
		try
		{
			uint16 hours = lexical_cast<uint16>(captures[1].str());
			uint16 minutes = lexical_cast<uint16>(captures[2].str());
			uint16 seconds = lexical_cast<uint16>(captures[3].str());
			if (isInBounds(hours, 23, "hours")
				&& isInBounds(minutes, 59, "minutes")
				&& isInBounds(seconds, 59, "seconds"))
			{
				return RotationAtTimePoint(
					static_cast<unsigned char>(hours),
					static_cast<unsigned char>(minutes),
					static_cast<unsigned char>(seconds));
			}
		}
		catch (const ::boost::bad_lexical_cast& ex)
		{
			format fmt("Numeric conversion error in logFileRotationTimePoint option string \"%1%\":  %2%");
			::std::cerr << (fmt % rotTime % ex.what()) << ::std::endl;
		}
	}
	else
	{
		format fmt("Syntax error in logFileRotationTimePoint option string \"%1%\"");
		::std::cerr << (fmt % rotTime) << ::std::endl;
	}
	return RotationAtTimePoint(2, 0, 0);
}

pmnt::LogLevel pmnt::Log::levelFromString(const string& level, bool& wasRecognized)
{
	LogLevel result = LogLevel::info;
	wasRecognized = false;
	string trimmedLevel = ba::trim_copy(level);
	auto it = ::std::find_if(begin(k_levelStrings), end(k_levelStrings),
		[&trimmedLevel](const string& str) { return ba::iequals(trimmedLevel, str); });
	if (it != end(k_levelStrings))
	{
		result = static_cast<LogLevel>(::std::distance(begin(k_levelStrings), it));
		wasRecognized = true;
	}
	return result;
}

pmnt::LogLevel pmnt::Log::levelFromString(const string& level)
{
	bool wasRecognized = false;
	LogLevel result = levelFromString(level, wasRecognized);
	if (!wasRecognized)
	{
		auto levelList = accumulate(make_iterator_range(k_levelStrings), string(), StringJoinOp("', '"));
		format fmt("'%1%' is not one of the recognized logging levels:  '%2%'");
		::std::cerr << (fmt % level % levelList) << ::std::endl;
	}
	return result;
}

pmnt::Log::Source pmnt::Log::getSource(const char* pChannelName)
{
	return Source(keywd::channel = pChannelName);
}

pmnt::Log::Source pmnt::Log::getSource(const string& channelName)
{
	return Source(keywd::channel = channelName);
}
