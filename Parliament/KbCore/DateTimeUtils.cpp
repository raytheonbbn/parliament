// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/DateTimeUtils.h"
#include "parliament/UnicodeIterator.h"

#include <boost/format.hpp>
#include <sstream>

namespace bg = ::boost::gregorian;
namespace bpt = ::boost::posix_time;
namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::string;
using ::std::istringstream;
using ::std::ostringstream;

static const auto k_rsrcQuote = pmnt::convertToRsrcChar("\"");
static const auto k_dateLiteralEnd = pmnt::convertToRsrcChar(
	"\"^^http://www.w3.org/2001/XMLSchema#date");
static const auto k_timeLiteralEnd = pmnt::convertToRsrcChar(
	"\"^^http://www.w3.org/2001/XMLSchema#time");
static const auto k_dateTimeLiteralEnd = pmnt::convertToRsrcChar(
	"\"^^http://www.w3.org/2001/XMLSchema#dateTime");
static const auto k_tzIntroducers = pmnt::convertToRsrcChar("Z+-");

// ==========================================================================

template <typename Facet, typename... Ts>
static inline void configureIStrStream(istringstream& strm, const pmnt::RsrcString& str, Ts&&... params)
{
	strm.exceptions(::std::ios_base::failbit);
	strm.imbue(::std::locale(::std::cout.getloc(), new Facet(::std::forward<Ts>(params)...)));
	strm.str(pmnt::convertFromRsrcChar(str));
}

template <typename Facet, typename... Ts>
static inline void configureOStrStream(ostringstream& strm, Ts&&... params)
{
	strm.exceptions(::std::ios_base::failbit);
	strm.imbue(::std::locale(::std::cout.getloc(), new Facet(::std::forward<Ts>(params)...)));
}

// ==========================================================================

pmnt::DateTimeUtils::PTime pmnt::DateTimeUtils::getPTimeFromXsdDate(const RsrcString& dateStr)
{
	try
	{
		PTime result;
		istringstream strm;
		configureIStrStream<bpt::time_input_facet>(strm, dateStr, "%Y-%m-%d");
		strm >> result;

		// now deal with timezones
		TimeDuration tz = getTZOffsetFromTZStr(getTZStrFromXsdDate(dateStr));
		return result - tz;
	}
	catch (const ::std::exception& ex)
	{
		throw Exception(format{"Unable to parse date string '%1%':  %2%"}
			% convertFromRsrcChar(dateStr) % ex.what());
	}
}

pmnt::DateTimeUtils::GregDate pmnt::DateTimeUtils::getDateFromXsdDate(const RsrcString& dateStr)
{
	return getPTimeFromXsdDate(dateStr).date();
}

pmnt::DateTimeUtils::TimeDuration pmnt::DateTimeUtils::getTZOffsetFromTZStr(const RsrcString& timeZoneStr)
{
	if (timeZoneStr.empty())
	{
		//TODO: is this correct?  Should we get local time?
		return bpt::hours(0);
	}
	else if (timeZoneStr[0] == 'Z')
	{
		// no offset for Zulu:
		return bpt::hours(0);
	}
	else
	{
		//TODO: Why the substr(1)?
		RsrcString offset = timeZoneStr.substr(1);
		TimeDuration td(bpt::duration_from_string(convertFromRsrcChar(offset)));
		return td;
	}
}

pmnt::RsrcString pmnt::DateTimeUtils::getXsdDate(const PTime& time)
{
	return getXsdDate(time.date());
}

pmnt::RsrcString pmnt::DateTimeUtils::getXsdDate(const GregDate& date)
{
	ostringstream strm;
	configureOStrStream<bg::date_facet>(strm, "%Y-%m-%d");
	strm << date << "Z";
	return convertToRsrcChar(strm.str());
}

pmnt::RsrcString pmnt::DateTimeUtils::getXsdDateLiteral(const PTime& time)
{
	return getXsdDateLiteral(time.date());
}

pmnt::RsrcString pmnt::DateTimeUtils::getXsdDateLiteral(const GregDate& date)
{
	auto result = k_rsrcQuote;
	result += getXsdDate(date);
	result += k_dateLiteralEnd;
	return result;
}

pmnt::DateTimeUtils::PTime pmnt::DateTimeUtils::getPTimeFromXsdTime(const RsrcString& timeStr)
{
	try
	{
		PTime result;
		istringstream strm;
		configureIStrStream<bpt::time_input_facet>(strm, timeStr, "%H:%M:%S%F");
		strm >> result;

		// now deal with timezones
		TimeDuration tz = getTZOffsetFromTZStr(getTZStrFromXsdTime(timeStr));
		return result - tz;
	}
	catch (const ::std::exception& ex)
	{
		throw Exception(format{"Unable to parse time string '%1%':  %2%"}
			% convertFromRsrcChar(timeStr) % ex.what());
	}
}

pmnt::RsrcString pmnt::DateTimeUtils::getXsdTime(const PTime& time)
{
	ostringstream strm;
	configureOStrStream<bpt::time_facet>(strm, "%H:%M:%S%F");
	strm << time << "Z";
	return convertToRsrcChar(strm.str());
}

pmnt::RsrcString pmnt::DateTimeUtils::getXsdTimeLiteral(const PTime& time)
{
	auto result = k_rsrcQuote;
	result += getXsdTime(time);
	result += k_timeLiteralEnd;
	return result;
}


pmnt::RsrcString pmnt::DateTimeUtils::getTZStrFromXsdDate(const RsrcString& dateStr)
{
	return (size(dateStr) > 9)
		? dateStr.substr(10)
		: RsrcString{};
}

pmnt::RsrcString pmnt::DateTimeUtils::getTZStrFromXsdTime(const RsrcString& timeStr)
{

	if (size(timeStr) < 8)
	{
		return RsrcString{};
	}
	else
	{
		size_t pos = timeStr.substr(8).find_last_of(k_tzIntroducers);
		return (pos != RsrcString::npos)
			? timeStr.substr(pos + 8)
			: RsrcString{};
	}
}

pmnt::DateTimeUtils::PTime pmnt::DateTimeUtils::getPTimeFromXsdDateTime(const RsrcString& datetimeStr)
{
	try
	{
		PTime result;
		istringstream strm;
		configureIStrStream<bpt::time_input_facet>(strm, datetimeStr, "%Y-%m-%dT%H:%M:%S%F");
		strm >> result;

		// now deal with timezones
		TimeDuration tz = getTZOffsetFromTZStr(getTZStrFromXsdDatetime(datetimeStr));
		return result - tz;
	}
	catch (const ::std::exception& ex)
	{
		throw Exception(format{"Unable to parse date-time string '%1%':  %2%"}
			% convertFromRsrcChar(datetimeStr) % ex.what());
	}
}

pmnt::RsrcString pmnt::DateTimeUtils::getTZStrFromXsdDatetime(const RsrcString& datetimeStr)
{
	if (size(datetimeStr) < 19)
	{
		return RsrcString{};
	}
	else
	{
		size_t pos = datetimeStr.substr(19).find_last_of(k_tzIntroducers);
		return (pos != RsrcString::npos)
			? datetimeStr.substr(pos + 8)
			: RsrcString{};
	}
}

pmnt::RsrcString pmnt::DateTimeUtils::getXsdDateTime(const PTime& time)
{
	ostringstream strm;
	configureOStrStream<bpt::time_facet>(strm, "%Y-%m-%dT%H:%M:%S%F");
	strm << time << "Z";
	return convertToRsrcChar(strm.str());
}

pmnt::RsrcString pmnt::DateTimeUtils::getXsdDateTimeLiteral(const PTime& time)
{
	auto result = k_rsrcQuote;
	result += getXsdDateTime(time);
	result += k_dateTimeLiteralEnd;
	return result;
}
