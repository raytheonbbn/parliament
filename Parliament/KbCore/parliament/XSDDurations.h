// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_XSDDURATIONS_H_INCLUDED)
#define PARLIAMENT_XSDDURATIONS_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <boost/date_time/posix_time/posix_time.hpp>

namespace bbn::parliament
{

class XSDDayTimeDuration
{
public:
	XSDDayTimeDuration()
		: m_duration(0, 0, 0, 0) {}

	XSDDayTimeDuration(int64 days, int64 hrs, int64 mins, double secs)
		: m_duration(buildDuration(days, hrs, mins, secs)) {}

	explicit XSDDayTimeDuration(const ::boost::posix_time::time_duration& duration)
		: m_duration(duration) {}

	explicit XSDDayTimeDuration(const RsrcString& dayTimeDurStr)
		: m_duration(buildDuration(dayTimeDurStr)) {}

	XSDDayTimeDuration(const XSDDayTimeDuration&) = default;
	XSDDayTimeDuration& operator=(const XSDDayTimeDuration&) = default;
	XSDDayTimeDuration(XSDDayTimeDuration&&) = default;
	XSDDayTimeDuration& operator=(XSDDayTimeDuration&&) = default;
	~XSDDayTimeDuration() = default;

	bool isNegative() const
		{ return m_duration.is_negative(); }

	int64 days() const
		{ return m_duration.hours() / 24; }

	int64 hours() const
		{ return m_duration.hours() % 24; }

	int64 minutes() const
		{ return m_duration.minutes(); }

	double seconds() const;

	double totalSeconds() const
		{ return 60 * (60 * hours() + minutes()) + seconds(); }

	::boost::posix_time::time_duration asTimeDuration() const
		{ return m_duration; }

	XSDDayTimeDuration& operator+=(const XSDDayTimeDuration& rhs)
		{ m_duration += rhs.m_duration; return *this; }

	XSDDayTimeDuration& operator-=(const XSDDayTimeDuration& rhs)
		{ m_duration -= rhs.m_duration; return *this; }

	RsrcString toXsdString() const;
	RsrcString toXsdLiteral() const;

private:
	friend bool operator==(const XSDDayTimeDuration&, const XSDDayTimeDuration&);
	friend bool operator<(const XSDDayTimeDuration&, const XSDDayTimeDuration&);

	static ::boost::posix_time::time_duration buildDuration(int64 days, int64 hrs, int64 mins, double secs);
	static ::boost::posix_time::time_duration buildDuration(const RsrcString& dayTimeDurStr);

	::boost::posix_time::time_duration m_duration;
};

inline bool operator==(const XSDDayTimeDuration& lhs, const XSDDayTimeDuration& rhs)
	{ return lhs.m_duration == rhs.m_duration; }

inline bool operator!=(const XSDDayTimeDuration& lhs, const XSDDayTimeDuration& rhs)
	{ return !operator==(lhs, rhs); }

inline bool operator<(const XSDDayTimeDuration& lhs, const XSDDayTimeDuration& rhs)
	{ return lhs.m_duration < rhs.m_duration; }

inline bool operator>(const XSDDayTimeDuration& lhs, const XSDDayTimeDuration& rhs)
	{ return !operator<(lhs, rhs) && !operator==(lhs, rhs); }

inline bool operator<=(const XSDDayTimeDuration& lhs, const XSDDayTimeDuration& rhs)
	{ return !operator>(lhs, rhs); }

inline bool operator>=(const XSDDayTimeDuration& lhs, const XSDDayTimeDuration& rhs)
	{ return !operator<(lhs, rhs); }

inline XSDDayTimeDuration operator+(XSDDayTimeDuration lhs, const XSDDayTimeDuration& rhs)
	{ return lhs += rhs; }

inline XSDDayTimeDuration operator-(XSDDayTimeDuration lhs, const XSDDayTimeDuration& rhs)
	{ return lhs -= rhs; }



class XSDYearMonthDuration
{
public:
	XSDYearMonthDuration()
		: m_totalMonths(0) {}

	XSDYearMonthDuration(int64 yrs, int64 months)
		: m_totalMonths(12 * yrs + months) {}

	explicit XSDYearMonthDuration(int64 totalMonths)
		: m_totalMonths(totalMonths) {}

	explicit XSDYearMonthDuration(const RsrcString& yearMonDurStr)
		: m_totalMonths(buildDuration(yearMonDurStr)) {}

	XSDYearMonthDuration(const XSDYearMonthDuration&) = default;
	XSDYearMonthDuration& operator=(const XSDYearMonthDuration&) = default;
	XSDYearMonthDuration(XSDYearMonthDuration&&) = default;
	XSDYearMonthDuration& operator=(XSDYearMonthDuration&&) = default;
	~XSDYearMonthDuration() = default;

	int64 years () const
		{ return m_totalMonths / 12; }

	int64 months () const
		{ return m_totalMonths % 12; }

	::boost::gregorian::months totalMonthsAsDuration() const
		{ return ::boost::gregorian::months(static_cast<int>(m_totalMonths)); }

	bool isNegative() const
		{ return m_totalMonths < 0; }

	XSDYearMonthDuration& operator+=(const XSDYearMonthDuration& rhs)
		{ m_totalMonths += rhs.m_totalMonths; return *this; }

	XSDYearMonthDuration& operator-=(const XSDYearMonthDuration& rhs)
		{ m_totalMonths -= rhs.m_totalMonths; return *this; }

	RsrcString toXsdString() const;
	RsrcString toXsdLiteral() const;

private:
	friend bool operator==(const XSDYearMonthDuration&, const XSDYearMonthDuration&);
	friend bool operator<(const XSDYearMonthDuration&, const XSDYearMonthDuration&);

	static int64 buildDuration(const RsrcString& yearMonDurStr);

	int64 m_totalMonths;
};

inline bool operator==(const XSDYearMonthDuration& lhs, const XSDYearMonthDuration& rhs)
	{ return lhs.m_totalMonths == rhs.m_totalMonths; }

inline bool operator!=(const XSDYearMonthDuration& lhs, const XSDYearMonthDuration& rhs)
	{ return !operator==(lhs, rhs); }

inline bool operator<(const XSDYearMonthDuration& lhs, const XSDYearMonthDuration& rhs)
	{ return lhs.m_totalMonths < rhs.m_totalMonths; }

inline bool operator>(const XSDYearMonthDuration& lhs, const XSDYearMonthDuration& rhs)
	{ return !operator<(lhs, rhs) && !operator==(lhs, rhs); }

inline bool operator<=(const XSDYearMonthDuration& lhs, const XSDYearMonthDuration& rhs)
	{ return !operator>(lhs, rhs); }

inline bool operator>=(const XSDYearMonthDuration& lhs, const XSDYearMonthDuration& rhs)
	{ return !operator<(lhs, rhs); }

inline XSDYearMonthDuration operator+(XSDYearMonthDuration lhs, const XSDYearMonthDuration& rhs)
	{ return lhs += rhs; }

inline XSDYearMonthDuration operator-(XSDYearMonthDuration lhs, const XSDYearMonthDuration& rhs)
	{ return lhs -= rhs; }

}	// namespace end

#endif /* XSDDURATIONS_H_ */
