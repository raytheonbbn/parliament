// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2016, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/Log.h"
#include "parliament/LogConfig.h"
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

using ::boost::format;
using ::boost::lexical_cast;
using ::boost::make_iterator_range;
using ::boost::make_shared;
using ::boost::posix_time::ptime;
using ::boost::shared_ptr;
using ::std::basic_ostream;
using ::std::begin;
using ::std::cerr;
using ::std::end;
using ::std::endl;
using ::std::size_t;
using ::std::string;

using RotationAtTimePoint = bl::sinks::file::rotation_at_time_point;

PARLIAMENT_NAMESPACE_BEGIN namespace log {

static ::boost::once_flag g_onceInitFlag = BOOST_ONCE_INIT;
static const char k_optionRegExStr[] = "^[ \t]*([0-9][0-9]?):([0-9][0-9]?):([0-9][0-9]?)[ \t]*$";
static const char*const k_levelStrings[] =
{
	"TRACE",
	"DEBUG",
	"INFO",
	"WARN",
	"ERROR"
};
static Level g_level = Level::info;
static ::std::map<string, Level> g_channelToLevelMap;

BOOST_LOG_ATTRIBUTE_KEYWORD(logLevelKeywd, "Severity", Level)
BOOST_LOG_ATTRIBUTE_KEYWORD(channelKeywd, "Channel", string)
BOOST_LOG_ATTRIBUTE_KEYWORD(threadIdKeywd, "ThreadID",
	bl::attributes::current_thread_id::value_type)

static Level levelFromString(const string& level, bool& wasRecognized)
{
	auto result = Level::info;
	wasRecognized = false;
	auto trimmedLevel = ba::trim_copy(level);
	auto it = ::std::find_if(begin(k_levelStrings), end(k_levelStrings),
		[&trimmedLevel](const string& str) { return ba::iequals(trimmedLevel, str); });
	if (it != end(k_levelStrings))
	{
		result = static_cast<Level>(::std::distance(begin(k_levelStrings), it));
		wasRecognized = true;
	}
	return result;
}

static Level levelFromString(const string& level)
{
	auto wasRecognized = false;
	auto result = levelFromString(level, wasRecognized);
	if (!wasRecognized)
	{
		auto levelList = accumulate(make_iterator_range(k_levelStrings), string(), StringJoinOp("', '"));
		format fmt{"'%1%' is not one of the recognized logging levels:  '%2%'"};
		cerr << (fmt % level % levelList) << endl;
	}
	return result;
}

static bool isInBounds(uint16 actualValue, uint16 maxValue, const char* pName)
{
	auto result = true;
	if (actualValue > maxValue)
	{
		result = false;
		format fmt{"In logFileRotationTimePoint option, %1% (%2%) must be between 0 and %3%, inclusive"};
		cerr << (fmt % pName % actualValue % maxValue) << endl;
	}
	return result;
}

static RotationAtTimePoint rotTimeFromString(const string& rotTime)
{
	auto rex = compileRegEx(k_optionRegExStr);
	SMatch captures;
	if (regExMatch(rotTime, captures, rex))
	{
		try
		{
			auto hours = lexical_cast<uint16>(captures[1].str());
			auto minutes = lexical_cast<uint16>(captures[2].str());
			auto seconds = lexical_cast<uint16>(captures[3].str());
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
			format fmt{"Numeric conversion error in logFileRotationTimePoint option string \"%1%\":  %2%"};
			cerr << (fmt % rotTime % ex.what()) << endl;
		}
	}
	else
	{
		format fmt{"Syntax error in logFileRotationTimePoint option string \"%1%\""};
		cerr << (fmt % rotTime) << endl;
	}
	return RotationAtTimePoint(2, 0, 0);
}

template<typename CharT, typename TraitsT>
static basic_ostream<CharT, TraitsT>& operator<<(
	basic_ostream<CharT, TraitsT>& strm, const Level level)
{
	if (strm.good())
	{
		auto levelInt = static_cast<size_t>(level);
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
	const bl::value_ref<Level, tag::logLevelKeywd>& currentLevel,
	const bl::value_ref<string, tag::channelKeywd>& channel)
{
	auto result = false;
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

static void unsynchronizedInit()
{
	try
	{
		LogConfig config;
		config.readFromFile();

		bl::add_common_attributes();

		if (config.logToConsole())
		{
			// Create the back end:
			auto pBackend = make_shared<bl::sinks::text_ostream_backend>();
			pBackend->add_stream(
				shared_ptr<::std::ostream>(&::std::clog, ::boost::null_deleter()));

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
			auto wasRecognized = false;
			auto lvl = levelFromString(entry.second, wasRecognized);
			if (wasRecognized)
			{
				g_channelToLevelMap.insert(make_pair(entry.first, lvl));
			}
			else
			{
				format fmt{"Unrecognized log level \"%1%\" for channel \"%2%\""};
				cerr << (fmt % entry.second % entry.first) << endl;
			}
		}

		bl::core::get()->set_filter(::boost::phoenix::bind(&logFilter,
			logLevelKeywd.or_none(), channelKeywd.or_none()));
	}
	catch (const Exception& ex)
	{
		format fmt{"Unable to initialize logging (%1%):  %2%"};
		cerr << (fmt % typeid(ex).name() % ex.what()) << endl;
	}
	catch (const ::std::exception& ex)
	{
		format fmt{"Unable to initialize logging (%1%):  %2%"};
		cerr << (fmt % typeid(ex).name() % ex.what()) << endl;
	}
}

static void init()
{
	call_once(g_onceInitFlag, &unsynchronizedInit);
}

Source getSource(const char* pChannelName)
{
	init();
	return Source(keywd::channel = pChannelName);
}

Source getSource(const string& channelName)
{
	init();
	return Source(keywd::channel = channelName);
}

} PARLIAMENT_NAMESPACE_END
