// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_DATETIMEUTILS_H_INCLUDED)
#define PARLIAMENT_DATETIMEUTILS_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <boost/date_time/posix_time/posix_time.hpp>

PARLIAMENT_NAMESPACE_BEGIN

class DateTimeUtils
{
public:
	using GregDate = ::boost::gregorian::date;
	using PTime = ::boost::posix_time::ptime;
	using TimeDuration = ::boost::posix_time::time_duration;

	DateTimeUtils() = delete;
	DateTimeUtils(const DateTimeUtils&) = delete;
	DateTimeUtils& operator=(const DateTimeUtils&) = delete;
	DateTimeUtils(DateTimeUtils&&) = delete;
	DateTimeUtils& operator=(DateTimeUtils&&) = delete;
	~DateTimeUtils() = delete;

	// As of 3/6/2012, boost 1_49_0 does not support ::boost::gregorian::time_from_string
	// on arch x86_64 and there apparently is a threading bug in get_month_map_ptr.
	// Plus, we want to extract timezone info, which isn't supported in the
	// time_from_string methods
	static PTime getPTimeFromXsdDate(const RsrcString& dateStr);
	static GregDate getDateFromXsdDate(const RsrcString& dateStr);

	static RsrcString getXsdDate(const PTime& time);
	static RsrcString getXsdDate(const GregDate& date);

	static RsrcString getXsdDateLiteral(const PTime& time);
	static RsrcString getXsdDateLiteral(const GregDate& date);

	// need to augment ptime construction to consider timezones without need for tz db
	static PTime getPTimeFromXsdTime(const RsrcString& timeStr);

	static RsrcString getXsdTime(const PTime& time);
	static RsrcString getXsdTimeLiteral(const PTime& time);

	// need to augment ptime construction to consider timezones without need for tz db
	static PTime getPTimeFromXsdDateTime(const RsrcString& datetimeStr);

	static RsrcString getXsdDateTime(const PTime& time);
	static RsrcString getXsdDateTimeLiteral(const PTime& time);

	static RsrcString getTZStrFromXsdDate(const RsrcString& dateStr);
	static RsrcString getTZStrFromXsdTime(const RsrcString& timeStr);
	static RsrcString getTZStrFromXsdDatetime(const RsrcString& timeStr);

	// We are avoiding loading timezone databases etc.  This is a simplified method for
	// basic operations.  timeZoneStr is expected to be either 'Z' or '[+/-]HH:MM'.
	// TODO: enforce format?
	static TimeDuration getTZOffsetFromTZStr(const RsrcString& timeZoneStr);
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_DATETIMEUTILS_H_INCLUDED
