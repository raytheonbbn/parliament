// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/XSDDurations.h"
#include "parliament/Exceptions.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"

#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <cmath>
#include <sstream>

namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::boost::lexical_cast;
using ::boost::posix_time::time_duration;
using ::std::ostringstream;
using ::std::string;



static const auto k_numericChars = pmnt::convertToRsrcChar("0123456789.");
static const auto k_literalStart = pmnt::convertToRsrcChar("\"");
static const auto k_dayTimeDurationLiteralEnd = pmnt::convertToRsrcChar(
	"\"^^http://www.w3.org/2001/XMLSchema#dayTimeDuration");
static const auto k_yearMonthDurationLiteralEnd = pmnt::convertToRsrcChar(
	"\"^^http://www.w3.org/2001/XMLSchema#yearMonthDuration");

static double timeDurFracSecsOrderOfMagnitude()
{
	return ::std::pow(10.0, static_cast<double>(time_duration::num_fractional_digits()));
}

// T will be either int64 or double:
template<typename T>
T convertAndClearBuffer(pmnt::Utf32String& buffer)
{
	try
	{
		auto utf8Buffer = pmnt::convertUtf32ToUtf8(pmnt::cBegin(buffer), pmnt::cEnd(buffer));
		buffer.clear();
		return lexical_cast<T>(utf8Buffer);
	}
	catch (const ::boost::bad_lexical_cast& ex)
	{
		throw pmnt::Exception(format("Unable to parse duration string: %1%") % ex.what());
	}
}

static inline bool isDigit(pmnt::RsrcChar ch)
{
	return k_numericChars.find(ch) != pmnt::RsrcString::npos;
}

// ==============================================================
//
// XSDDayTimeDuration
//
// ==============================================================

double pmnt::XSDDayTimeDuration::seconds() const
{
	return m_duration.seconds() +
		(m_duration.fractional_seconds() / timeDurFracSecsOrderOfMagnitude());
}

pmnt::RsrcString pmnt::XSDDayTimeDuration::toXsdString() const
{
	if (m_duration.total_nanoseconds() == 0)
	{
		return convertToRsrcChar("PT0S");
	}
	else
	{
		ostringstream ss;
		if (isNegative())
		{
			ss << '-';
		}
		ss << 'P';
		if (days() != 0)
		{
			ss << abs(days()) << 'D';
		}
		ss << 'T';
		if (hours() != 0)
		{
			ss << abs(hours()) << 'H';
		}
		if (minutes() != 0)
		{
			ss << abs(minutes()) << 'M';
		}
		ss << fabs(seconds()) << 'S';

		return convertToRsrcChar(ss.str());
	}
}

pmnt::RsrcString pmnt::XSDDayTimeDuration::toXsdLiteral() const
{
	auto result = k_literalStart;
	result += toXsdString();
	result += k_dayTimeDurationLiteralEnd;
	return result;
}

time_duration pmnt::XSDDayTimeDuration::buildDuration(int64 days, int64 hrs, int64 mins, double secs)
{
	double fractionalSecs = 60 * (60 * (24 * days + hrs) + mins) + secs;

	//The value of the days component in the canonical form is then calculated by dividing the value by 86,400 (24*60*60)
	int64 calcDays = static_cast<int64>(fractionalSecs / 86400);

	//The remainder is in fractional seconds
	fractionalSecs = fmod(fractionalSecs, 86400);

	//The value of the hours component in the canonical form is calculated by dividing this remainder by 3,600 (60*60).
	int64 calcHours = static_cast<int64>((fractionalSecs / 3600) + (calcDays * 24));

	//The remainder is again in fractional seconds
	fractionalSecs = fmod(fractionalSecs, 3600);

	//The value of the minutes component in the canonical form is calculated by dividing this remainder by 60.
	int64 calcMins = static_cast<int64>(fractionalSecs / 60);

	//The remainder in fractional seconds is the value of the seconds component in the canonical form.
	fractionalSecs = fmod(fractionalSecs, 60);

	// to build duration, must separate out secs from fractional secs
	int64 calcSecs = static_cast<int64>(floor(fractionalSecs));
	fractionalSecs = fractionalSecs - calcSecs;

	// TODO: double imprecision is ok, i think, for our purposes, needs to be verified to consistently return equiv results
	int64 calcFracSecs = static_cast<int64>(fractionalSecs * timeDurFracSecsOrderOfMagnitude());

	return time_duration(calcHours, calcMins, calcSecs, calcFracSecs);
}

time_duration pmnt::XSDDayTimeDuration::buildDuration(const RsrcString& dayTimeDurStr)
{
	bool isNegative = false;
	int64 d = 0;
	int64 m = 0;
	int64 h = 0;
	double s = 0;

	auto strIt = UnicodeIteratorFactory<RsrcChar>::begin(dayTimeDurStr);
	auto strEnd = UnicodeIteratorFactory<RsrcChar>::end(dayTimeDurStr);

	if (strIt == strEnd)
	{
		throw Exception("XSDDayTimeDuration parse error:  Input is too short");
	}
	else if (*strIt == '-')
	{
		isNegative = true;
		++strIt;
	}

	if (strIt == strEnd)
	{
		throw Exception("XSDDayTimeDuration parse error:  Input is too short");
	}
	else if (*strIt == 'P')
	{
		++strIt;
	}
	else
	{
		throw Exception("XSDDayTimeDuration parse error:  Missing introductory P");
	}

	for (Utf32String buffer; strIt != strEnd; ++strIt)
	{
		if (*strIt == 'D')
		{
			d = convertAndClearBuffer<int64>(buffer);
		}
		else if (*strIt == 'H')
		{
			h = convertAndClearBuffer<int64>(buffer);
		}
		else if (*strIt == 'M')
		{
			m = convertAndClearBuffer<int64>(buffer);
		}
		else if (*strIt == 'S')
		{
			// seconds can have decimal values...
			s = convertAndClearBuffer<double>(buffer);
		}
		else if (!isDigit(*strIt))
		{
			throw Exception("XSDDayTimeDuration parse error");
		}
		else
		{
			buffer += *strIt;
		}
	}

	auto result = buildDuration(d, h, m, s);
	return isNegative ? result.invert_sign() : result;
}



// ==============================================================
//
// XSDYearMonthDuration
//
// ==============================================================

pmnt::RsrcString pmnt::XSDYearMonthDuration::toXsdString() const
{
	if (m_totalMonths == 0)
	{
		return convertToRsrcChar("P0M");
	}
	else
	{
		ostringstream ss;
		if (isNegative())
		{
			ss << '-';
		}
		ss << 'P';
		if (years() != 0)
		{
			ss << abs(years()) << 'Y';
		}
		ss << abs(months()) << 'M';

		return convertToRsrcChar(ss.str());
	}
}

pmnt::RsrcString pmnt::XSDYearMonthDuration::toXsdLiteral() const
{
	auto result = k_literalStart;
	result += toXsdString();
	result += k_yearMonthDurationLiteralEnd;
	return result;
}

pmnt::int64 pmnt::XSDYearMonthDuration::buildDuration(const RsrcString& yearMonDurStr)
{
	bool isNegative = false;
	int64 y = 0;
	int64 m = 0;

	auto strIt = UnicodeIteratorFactory<RsrcChar>::begin(yearMonDurStr);
	auto strEnd = UnicodeIteratorFactory<RsrcChar>::end(yearMonDurStr);

	if (strIt == strEnd)
	{
		throw Exception("XSDYearMonthDuration parse error:  Input is too short");
	}
	else if (*strIt == '-')
	{
		isNegative = true;
		++strIt;
	}

	if (strIt == strEnd)
	{
		throw Exception("XSDYearMonthDuration parse error:  Input is too short");
	}
	else if (*strIt == 'P')
	{
		++strIt;
	}
	else
	{
		throw Exception("XSDYearMonthDuration parse error:  Missing introductory P");
	}

	for (Utf32String buffer; strIt != strEnd; ++strIt)
	{
		if (*strIt == 'Y')
		{
			y = convertAndClearBuffer<int64>(buffer);
		}
		else if (*strIt == 'M')
		{
			m = convertAndClearBuffer<int64>(buffer);
		}
		else if (!isDigit(*strIt))
		{
			throw Exception("XSDYearMonthDuration parse error");
		}
		else
		{
			buffer += *strIt;
		}
	}

	int64 result = (y * 12) + m;
	return isNegative ? -result : result;
}
