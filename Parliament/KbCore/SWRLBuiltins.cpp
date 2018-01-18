// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/SWRLBuiltins.h"
#include "parliament/DateTimeUtils.h"
#include "parliament/Log.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/XSDDurations.h"

#include <boost/algorithm/string.hpp>
#include <boost/format.hpp>
#include <cmath>

namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::boost::lexical_cast;

static const auto k_rsrcQuote = pmnt::convertToRsrcChar("\"");
static const auto k_dateRsrcEnd = pmnt::convertToRsrcChar("\"^^http://www.w3.org/2001/XMLSchema#date");
static const auto k_rsrcPlus = pmnt::convertToRsrcChar("+");
static const auto k_rsrcColon = pmnt::convertToRsrcChar(":");
static const auto k_rsrcHyphen = pmnt::convertToRsrcChar("-");
static const auto k_rsrcTee = pmnt::convertToRsrcChar("T");

static pmnt::Log::Source g_log(pmnt::Log::getSource("SWRLBuiltins"));

// TODO: complete this method
bool pmnt::AddBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto addVal = 0.0;

	auto current = begin(getRulePosList());
	auto result = *current;
	++current;

	for (; current != end(getRulePosList()); ++current)
	{
		auto val = getDoubleFromRulePos(*current, pKB, bindingList);
		addVal += val;
	}

	// TODO: is this correct?
	return checkResult(result, addVal, pKB, bindingList);
}

bool pmnt::SubtractBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultPos = getRulePosList().at(0);
	auto firstPos = getRulePosList().at(1);
	auto secondPos = getRulePosList().at(2);

	auto firstVal = getDoubleFromRulePos(firstPos, pKB, bindingList);
	auto secondVal = getDoubleFromRulePos(secondPos, pKB, bindingList);

	auto resultVal = firstVal - secondVal;
	return checkResult(resultPos, resultVal, pKB, bindingList);
}

// TODO: complete this method
bool pmnt::MultiplyBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto current = begin(getRulePosList());
	auto result = *current;
	++current;

	// TODO: handle ints v. double
	auto multVal = 1.0;

	for (; current != end(getRulePosList()); ++current)
	{
		multVal *= getDoubleFromRulePos(*current, pKB, bindingList);
	}

	// TODO: is this correct?
	return checkResult(result, multVal, pKB, bindingList);
}

bool pmnt::DivideBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultPos = getRulePosList().at(0);
	auto firstPos = getRulePosList().at(1);
	auto secondPos = getRulePosList().at(2);

	auto firstVal = getDoubleFromRulePos(firstPos, pKB, bindingList);
	auto secondVal = getDoubleFromRulePos(secondPos, pKB, bindingList);

	auto resultVal = firstVal / secondVal; // todo: correct for all types?  what of check for DivByZero?
	return checkResult(resultPos, resultVal, pKB, bindingList);
}

//TODO: Fix the iequals to handle Unicode properly.  How do we know the locale?
bool pmnt::StringEqualIngnoreCaseBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto firstPos = getRulePosList().at(0);
	auto secondPos = getRulePosList().at(1);

	auto firstStr = getLiteralStrFromRulePos(firstPos, pKB, bindingList);
	auto secondStr = getLiteralStrFromRulePos(secondPos, pKB, bindingList);
	auto firstStrUtf8 = convertFromRsrcChar(firstStr);
	auto secondStrUtf8 = convertFromRsrcChar(secondStr);
	return ::boost::iequals(firstStrUtf8, secondStrUtf8);
}

bool pmnt::StringConcatBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	RulePosition result;
	RsrcString firstStr;
	RsrcString secondStr;
	auto isFirstIteration = true;
	for (auto current = begin(getRulePosList()); current != end(getRulePosList()); ++current)
	{
		if (isFirstIteration)
		{
			result = *current;
			firstStr = getLiteralStrFromRulePos(result, pKB, bindingList);
			isFirstIteration = false;
		}
		else
		{
			secondStr += getLiteralStrFromRulePos(*current, pKB, bindingList);
		}
	}

	if (result.m_isVariable)
	{
		auto resultId = pKB->uriToRsrcId(k_rsrcQuote + secondStr + k_rsrcQuote, true, true);
		return result.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		return firstStr == secondStr;
	}
}

bool pmnt::YearMonthDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto yearMonthDurationPos = getRulePosList().at(0);
	auto yearsPos = getRulePosList().at(1);
	auto monthsPos = getRulePosList().at(2);

	auto years = lexical_cast<int64>(convertFromRsrcChar(getLiteralStrFromRulePos(yearsPos, pKB, bindingList)));
	auto months = lexical_cast<int64>(convertFromRsrcChar(getLiteralStrFromRulePos(monthsPos, pKB, bindingList)));

	XSDYearMonthDuration resultDuration(years, months);

	if (yearMonthDurationPos.m_isVariable)
	{
		auto resultId = pKB->uriToRsrcId(resultDuration.toXsdLiteral(), true, true);
		return yearMonthDurationPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		XSDYearMonthDuration foundDuration(getLiteralStrFromRulePos(yearMonthDurationPos, pKB, bindingList));
		return foundDuration == resultDuration;
	}
}

bool pmnt::DayTimeDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto durationPos = getRulePosList().at(0);
	auto daysPos = getRulePosList().at(1);
	auto hourPos = getRulePosList().at(2);
	auto minutesPos = getRulePosList().at(3);
	auto secondsPos = getRulePosList().at(4);

	auto days = lexical_cast<int64>(convertFromRsrcChar(getLiteralStrFromRulePos(daysPos, pKB, bindingList)));
	auto hours = lexical_cast<int64>(convertFromRsrcChar(getLiteralStrFromRulePos(hourPos, pKB, bindingList)));
	auto minutes = lexical_cast<int64>(convertFromRsrcChar(getLiteralStrFromRulePos(minutesPos, pKB, bindingList)));
	auto seconds = lexical_cast<double>(convertFromRsrcChar(getLiteralStrFromRulePos(secondsPos, pKB, bindingList)));

	XSDDayTimeDuration duration(days, hours, minutes, seconds);

	if (durationPos.m_isVariable)
	{
		auto resultId = pKB->uriToRsrcId(duration.toXsdLiteral(), true, true);
		return durationPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		XSDDayTimeDuration resultDuration(getLiteralStrFromRulePos(durationPos, pKB, bindingList));
		return resultDuration == duration;
	}
}

bool pmnt::TimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto timePos = getRulePosList().at(0);
	auto hourPos = getRulePosList().at(1);
	auto minPos = getRulePosList().at(2);
	auto secPos = getRulePosList().at(3);
	auto tzPos = getRulePosList().at(4);

	auto hours = getLiteralStrFromRulePos(hourPos, pKB, bindingList);
	auto minutes = getLiteralStrFromRulePos(minPos, pKB, bindingList);
	auto seconds = getLiteralStrFromRulePos(secPos, pKB, bindingList);
	auto tzStr = getLiteralStrFromRulePos(tzPos, pKB, bindingList);

	auto resultTimeStr = hours + k_rsrcColon + minutes + k_rsrcColon + seconds + tzStr;
	auto resultTime = DateTimeUtils::getPTimeFromXsdTime(resultTimeStr);

	if (timePos.m_isVariable)
	{
		auto timeStr = DateTimeUtils::getXsdTimeLiteral(resultTime);
		auto resultId = pKB->uriToRsrcId(timeStr, true, true);
		return timePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundTimeStr = getLiteralStrFromRulePos(timePos, pKB, bindingList);
		auto foundTime = DateTimeUtils::getPTimeFromXsdTime(foundTimeStr);
		return foundTime == resultTime;
	}
}

bool pmnt::DateTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto dateTimePos = getRulePosList().at(0);
	auto yearPos = getRulePosList().at(1);
	auto monthPos = getRulePosList().at(2);
	auto dayPos = getRulePosList().at(3);
	auto hourPos = getRulePosList().at(4);
	auto minPos = getRulePosList().at(5);
	auto secPos = getRulePosList().at(6);
	auto tzPos = getRulePosList().at(7);

	auto years = getLiteralStrFromRulePos(yearPos, pKB, bindingList);
	auto months = getLiteralStrFromRulePos(monthPos, pKB, bindingList);
	auto days = getLiteralStrFromRulePos(dayPos, pKB, bindingList);
	auto hours = getLiteralStrFromRulePos(hourPos, pKB, bindingList);
	auto minutes = getLiteralStrFromRulePos(minPos, pKB, bindingList);
	auto seconds = getLiteralStrFromRulePos(secPos, pKB, bindingList);
	auto tzStr = getLiteralStrFromRulePos(tzPos, pKB, bindingList);

	auto resultDatetimeStr = years + k_rsrcHyphen + months + k_rsrcHyphen + days
		+ k_rsrcTee + hours + k_rsrcColon + minutes + k_rsrcColon + seconds + tzStr;
	auto resultDatetime = DateTimeUtils::getPTimeFromXsdDateTime(resultDatetimeStr);

	if (dateTimePos.m_isVariable)
	{
		auto datetimeStr = DateTimeUtils::getXsdDateTimeLiteral(resultDatetime);
		auto resultId = pKB->uriToRsrcId(datetimeStr, true, true);
		return dateTimePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDatetimeStr = getLiteralStrFromRulePos(dateTimePos, pKB, bindingList);
		auto foundDatetime = DateTimeUtils::getPTimeFromXsdDateTime(foundDatetimeStr);
		return foundDatetime == resultDatetime;
	}
}

bool pmnt::DateBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto datePos = getRulePosList().at(0);
	auto yearPos = getRulePosList().at(1);
	auto monthPos = getRulePosList().at(2);
	auto dayPos = getRulePosList().at(3);
	auto timezonePos = getRulePosList().at(4);

	auto year = getLiteralStrFromRulePos(yearPos, pKB, bindingList);
	auto month = getLiteralStrFromRulePos(monthPos, pKB, bindingList);
	auto day = getLiteralStrFromRulePos(dayPos, pKB, bindingList);
	auto tzStr = getLiteralStrFromRulePos(timezonePos, pKB, bindingList);
	if (!tzStr.empty() && tzStr[0] != 'Z' && tzStr[0] != '-' && tzStr[0] != '+')
	{
		tzStr = k_rsrcPlus + tzStr;
	}

	auto resultDateStr = year + k_rsrcHyphen + month + k_rsrcHyphen + day + tzStr;
	auto resultDate = DateTimeUtils::getDateFromXsdDate(resultDateStr);

	if (datePos.m_isVariable)
	{
		auto dateStr = DateTimeUtils::getXsdDateLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(dateStr, true, true);
		return datePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDateStr = getLiteralStrFromRulePos(datePos, pKB, bindingList);
		auto foundDate = DateTimeUtils::getDateFromXsdDate(foundDateStr);
		return foundDate == resultDate;
	}
}

bool pmnt::SubtractDatesBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDurationPos = getRulePosList().at(0);
	auto firstDatePos = getRulePosList().at(1);
	auto secondDatePos = getRulePosList().at(2);

	auto firstDateStr = getLiteralStrFromRulePos(firstDatePos, pKB, bindingList);
	auto secondDateStr = getLiteralStrFromRulePos(secondDatePos, pKB, bindingList);

	// need ptimes for day-minute duration object, as specified in built-in definition.
	auto firstDate = DateTimeUtils::getPTimeFromXsdDate(firstDateStr);
	auto secondDate = DateTimeUtils::getPTimeFromXsdDate(secondDateStr);

	auto resultDuration = XSDDayTimeDuration{firstDate - secondDate};

	if (resultDurationPos.m_isVariable)
	{
		auto resultId = pKB->uriToRsrcId(resultDuration.toXsdLiteral(), true, true);
		return resultDurationPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDuration = XSDDayTimeDuration{getLiteralStrFromRulePos(resultDurationPos, pKB, bindingList)};
		return foundDuration == resultDuration;
	}
}

bool pmnt::SubtractTimesBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDurationPos = getRulePosList().at(0);
	auto firstTimePos = getRulePosList().at(1);
	auto secondTimePos = getRulePosList().at(2);

	auto firstTimeStr = getLiteralStrFromRulePos(firstTimePos, pKB, bindingList);
	auto secondTimeStr = getLiteralStrFromRulePos(secondTimePos, pKB, bindingList);

	// need ptimes for day-minute duration object, as specified in built-in definition.
	auto firstTime = DateTimeUtils::getPTimeFromXsdTime(firstTimeStr);
	auto secondTime = DateTimeUtils::getPTimeFromXsdTime(secondTimeStr);

	auto resultDuration = XSDDayTimeDuration{firstTime - secondTime};

	if (resultDurationPos.m_isVariable)
	{
		auto resultId = pKB->uriToRsrcId(resultDuration.toXsdLiteral(), true, true);
		return resultDurationPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDuration = XSDDayTimeDuration{getLiteralStrFromRulePos(resultDurationPos, pKB, bindingList)};
		return foundDuration == resultDuration;
	}
}

bool pmnt::AddYearMonthDurationToDateTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatetimePos = getRulePosList().at(0);
	auto datetimeArgPos = getRulePosList().at(1);
	auto durationArgPos = getRulePosList().at(2);

	auto datetimeArgStr = getLiteralStrFromRulePos(datetimeArgPos, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromRulePos(durationArgPos, pKB, bindingList);

	auto durationArg = XSDYearMonthDuration{durationArgStr};

	auto datetimeArg = DateTimeUtils::getPTimeFromXsdDateTime(datetimeArgStr);
	auto resultDate = datetimeArg.date() + durationArg.totalMonthsAsDuration();

	auto resultDatetime = ::boost::posix_time::ptime{resultDate, datetimeArg.time_of_day()};

	if (resultDatetimePos.m_isVariable)
	{
		auto resultStr = DateTimeUtils::getXsdDateTimeLiteral(resultDatetime);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatetimePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDatetimeStr = getLiteralStrFromRulePos(resultDatetimePos, pKB, bindingList);
		auto foundDatetime = DateTimeUtils::getPTimeFromXsdDateTime(foundDatetimeStr);
		return foundDatetime == resultDatetime;
	}
}

bool pmnt::AddDayTimeDurationToDateTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatetimePos = getRulePosList().at(0);
	auto datetimeArgPos = getRulePosList().at(1);
	auto durationArgPos = getRulePosList().at(2);

	auto datetimeArgStr = getLiteralStrFromRulePos(datetimeArgPos, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromRulePos(durationArgPos, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto datetimeArg = DateTimeUtils::getPTimeFromXsdDateTime(datetimeArgStr);
	auto resultDatetime = datetimeArg + durationArg.asTimeDuration();

	if (resultDatetimePos.m_isVariable)
	{
		auto resultStr = DateTimeUtils::getXsdDateTimeLiteral(resultDatetime);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatetimePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDatetimeStr = getLiteralStrFromRulePos(resultDatetimePos, pKB, bindingList);
		auto foundDatetime = DateTimeUtils::getPTimeFromXsdDateTime(foundDatetimeStr);
		return foundDatetime == resultDatetime;
	}
}

bool pmnt::SubtractYearMonthDurationFromDateTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatetimePos = getRulePosList().at(0);
	auto datetimeArgPos = getRulePosList().at(1);
	auto durationArgPos = getRulePosList().at(2);

	auto datetimeArgStr = getLiteralStrFromRulePos(datetimeArgPos, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromRulePos(durationArgPos, pKB, bindingList);

	auto durationArg = XSDYearMonthDuration{durationArgStr};

	auto datetimeArg = DateTimeUtils::getPTimeFromXsdDateTime(datetimeArgStr);
	auto resultDate = datetimeArg.date() - durationArg.totalMonthsAsDuration();

	auto resultDatetime = ::boost::posix_time::ptime{resultDate, datetimeArg.time_of_day()};

	if (resultDatetimePos.m_isVariable)
	{
		auto resultStr = DateTimeUtils::getXsdDateTimeLiteral(resultDatetime);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatetimePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDatetimeStr = getLiteralStrFromRulePos(resultDatetimePos, pKB, bindingList);
		auto foundDatetime = DateTimeUtils::getPTimeFromXsdDateTime(foundDatetimeStr);
		return foundDatetime == resultDatetime;
	}
}

bool pmnt::SubtractDayTimeDurationFromDateTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatetimePos = getRulePosList().at(0);
	auto datetimeArgPos = getRulePosList().at(1);
	auto durationArgPos = getRulePosList().at(2);

	auto datetimeArgStr = getLiteralStrFromRulePos(datetimeArgPos, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromRulePos(durationArgPos, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto datetimeArg = DateTimeUtils::getPTimeFromXsdDateTime(datetimeArgStr);
	auto resultDatetime = datetimeArg - durationArg.asTimeDuration();

	if (resultDatetimePos.m_isVariable)
	{
		auto resultStr = DateTimeUtils::getXsdDateTimeLiteral(resultDatetime);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatetimePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDatetimeStr = getLiteralStrFromRulePos(resultDatetimePos, pKB, bindingList);
		auto foundDatetime = DateTimeUtils::getPTimeFromXsdDateTime(foundDatetimeStr);
		return foundDatetime == resultDatetime;
	}
}

bool pmnt::AddYearMonthDurationToDateBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatePos = getRulePosList().at(0);
	auto dateArgPos = getRulePosList().at(1);
	auto durationArgPos = getRulePosList().at(2);

	auto dateArgStr = getLiteralStrFromRulePos(dateArgPos, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromRulePos(durationArgPos, pKB, bindingList);

	auto durationArg = XSDYearMonthDuration{durationArgStr};

	auto dateArg = DateTimeUtils::getDateFromXsdDate(dateArgStr);
	auto resultDate = dateArg + durationArg.totalMonthsAsDuration();

	if (resultDatePos.m_isVariable)
	{
		auto resultStr = DateTimeUtils::getXsdDateLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDateStr = getLiteralStrFromRulePos(resultDatePos, pKB, bindingList);
		auto foundDate = DateTimeUtils::getDateFromXsdDate(foundDateStr);
		return foundDate == resultDate;
	}
}

bool pmnt::SubtractYearMonthDurationFromDateBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatePos = getRulePosList().at(0);
	auto dateArgPos = getRulePosList().at(1);
	auto durationArgPos = getRulePosList().at(2);

	auto dateArgStr = getLiteralStrFromRulePos(dateArgPos, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromRulePos(durationArgPos, pKB, bindingList);

	auto durationArg = XSDYearMonthDuration{durationArgStr};

	auto dateArg = DateTimeUtils::getDateFromXsdDate(dateArgStr);
	auto resultDate = dateArg - durationArg.totalMonthsAsDuration();

	if (resultDatePos.m_isVariable)
	{
		auto resultStr = DateTimeUtils::getXsdDateLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDateStr = getLiteralStrFromRulePos(resultDatePos, pKB, bindingList);
		auto foundDate = DateTimeUtils::getDateFromXsdDate(foundDateStr);
		return foundDate == resultDate;
	}
}

bool pmnt::AddDayTimeDurationToDateBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatePos = getRulePosList().at(0);
	auto dateArgPos = getRulePosList().at(1);
	auto durationArgPos = getRulePosList().at(2);

	auto dateArgStr = getLiteralStrFromRulePos(dateArgPos, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromRulePos(durationArgPos, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto dateArg = DateTimeUtils::getPTimeFromXsdDate(dateArgStr);
	auto resultDate = dateArg + durationArg.asTimeDuration();

	if (resultDatePos.m_isVariable)
	{
		auto resultStr = DateTimeUtils::getXsdDateLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDateStr = getLiteralStrFromRulePos(resultDatePos, pKB, bindingList);
		auto foundDate = DateTimeUtils::getPTimeFromXsdDate(foundDateStr);
		return foundDate == resultDate;
	}
}

bool pmnt::SubtractDayTimeDurationFromDateBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatePos = getRulePosList().at(0);
	auto dateArgPos = getRulePosList().at(1);
	auto durationArgPos = getRulePosList().at(2);

	auto dateArgStr = getLiteralStrFromRulePos(dateArgPos, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromRulePos(durationArgPos, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto dateArg = DateTimeUtils::getPTimeFromXsdDate(dateArgStr);
	auto resultDate = dateArg - durationArg.asTimeDuration();

	if (resultDatePos.m_isVariable)
	{
		auto resultStr = DateTimeUtils::getXsdDateLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDateStr = getLiteralStrFromRulePos(resultDatePos, pKB, bindingList);
		auto foundDate = DateTimeUtils::getPTimeFromXsdDate(foundDateStr);
		return foundDate == resultDate;
	}
}

bool pmnt::AddDayTimeDurationToTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultTimePos = getRulePosList().at(0);
	auto timeArgPos = getRulePosList().at(1);
	auto durationArgPos = getRulePosList().at(2);

	auto timeArgStr = getLiteralStrFromRulePos(timeArgPos, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromRulePos(durationArgPos, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto timeArg = DateTimeUtils::getPTimeFromXsdTime(timeArgStr);
	auto resultDate = timeArg + durationArg.asTimeDuration();

	if (resultTimePos.m_isVariable)
	{
		auto resultStr = DateTimeUtils::getXsdTimeLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultTimePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundTimeStr = getLiteralStrFromRulePos(resultTimePos, pKB, bindingList);
		auto foundDate = DateTimeUtils::getPTimeFromXsdTime(foundTimeStr);
		return foundDate == resultDate;
	}
}

bool pmnt::SubtractDayTimeDurationFromTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultTimePos = getRulePosList().at(0);
	auto timeArgPos = getRulePosList().at(1);
	auto durationArgPos = getRulePosList().at(2);

	auto timeArgStr = getLiteralStrFromRulePos(timeArgPos, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromRulePos(durationArgPos, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto timeArg = DateTimeUtils::getPTimeFromXsdTime(timeArgStr);
	auto resultDate = timeArg - durationArg.asTimeDuration();

	if (resultTimePos.m_isVariable)
	{
		auto resultStr = DateTimeUtils::getXsdTimeLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultTimePos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundTimeStr = getLiteralStrFromRulePos(resultTimePos, pKB, bindingList);
		auto foundDate = DateTimeUtils::getPTimeFromXsdTime(foundTimeStr);
		return foundDate == resultDate;
	}
}

bool pmnt::AddYearMonthDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto current = begin(getRulePosList());
	auto resultPos = *current;
	++current;

	auto totalDuration = XSDYearMonthDuration{};
	for (; current != end(getRulePosList()); ++current)
	{
		auto durationArgStr = getLiteralStrFromRulePos(*current, pKB, bindingList);
		totalDuration += XSDYearMonthDuration{durationArgStr};
	}

	if (resultPos.m_isVariable)
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromRulePos(*current, pKB, bindingList);
		auto resultDuration = XSDYearMonthDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::SubtractYearMonthDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultPos = getRulePosList().at(0);
	auto firstDurationPos = getRulePosList().at(1);
	auto secondDurationPos = getRulePosList().at(2);

	auto firstDurationStr = getLiteralStrFromRulePos(firstDurationPos, pKB, bindingList);
	auto firstDuration = XSDYearMonthDuration{firstDurationStr};

	auto secondDurationStr = getLiteralStrFromRulePos(secondDurationPos, pKB, bindingList);
	auto secondDuration = XSDYearMonthDuration{secondDurationStr};

	auto totalDuration = firstDuration - secondDuration;

	if (resultPos.m_isVariable)
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromRulePos(resultPos, pKB, bindingList);
		auto resultDuration = XSDYearMonthDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::MultiplyYearMonthDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultPos = getRulePosList().at(0);
	auto durationPos = getRulePosList().at(1);
	auto scalePos = getRulePosList().at(2);

	auto durationArgStr = getLiteralStrFromRulePos(durationPos, pKB, bindingList);
	auto duration = XSDYearMonthDuration{durationArgStr};

	auto scale = getDoubleFromRulePos(scalePos, pKB, bindingList);

	auto total = duration.months() * scale;
	auto totalRounded = static_cast<int64>(floor(total + 0.5)); // must round

	auto totalDuration = XSDYearMonthDuration{totalRounded};

	if (resultPos.m_isVariable)
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromRulePos(resultPos, pKB, bindingList);
		auto resultDuration = XSDYearMonthDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::DivideYearMonthDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultPos = getRulePosList().at(0);
	auto durationPos = getRulePosList().at(1);
	auto divisorPos = getRulePosList().at(2);

	auto durationArgStr = getLiteralStrFromRulePos(durationPos, pKB, bindingList);
	auto duration = XSDYearMonthDuration{durationArgStr};

	auto divisor = getDoubleFromRulePos(divisorPos, pKB, bindingList);

	auto total = duration.months() / divisor;
	auto totalRounded = static_cast<int64>(floor(total + 0.5)); // must round

	auto totalDuration = XSDYearMonthDuration{totalRounded};

	if (resultPos.m_isVariable)
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromRulePos(resultPos, pKB, bindingList);
		auto resultDuration = XSDYearMonthDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::AddDayTimeDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto current = begin(getRulePosList());
	auto result = *current;

	++current;

	auto totalDuration = XSDDayTimeDuration{};
	for (; current != end(getRulePosList()); ++current)
	{
		auto durationArgStr = getLiteralStrFromRulePos(*current, pKB, bindingList);
		totalDuration += XSDDayTimeDuration{durationArgStr};
	}

	if (result.m_isVariable)
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return result.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromRulePos(*current, pKB, bindingList);
		auto resultDuration = XSDDayTimeDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::SubtractDayTimeDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultPos = getRulePosList().at(0);
	auto firstDurationPos = getRulePosList().at(1);
	auto secondDurationPos = getRulePosList().at(2);

	auto firstDurationStr = getLiteralStrFromRulePos(firstDurationPos, pKB, bindingList);
	auto firstDuration = XSDDayTimeDuration{firstDurationStr};

	auto secondDurationStr = getLiteralStrFromRulePos(secondDurationPos, pKB, bindingList);
	auto secondDuration = XSDDayTimeDuration{secondDurationStr};

	auto totalDuration = firstDuration - secondDuration;

	if (resultPos.m_isVariable)
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromRulePos(resultPos, pKB, bindingList);
		auto resultDuration = XSDDayTimeDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::MultiplyDayTimeDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultPos = getRulePosList().at(0);
	auto durationPos = getRulePosList().at(1);
	auto scalePos = getRulePosList().at(2);

	auto durationArgStr = getLiteralStrFromRulePos(durationPos, pKB, bindingList);
	auto duration = XSDDayTimeDuration{durationArgStr};

	auto scale = getDoubleFromRulePos(scalePos, pKB, bindingList);

	//TODO:  Not sure why we're rounding, here
	auto total = floor(duration.totalSeconds() * scale + 0.5);
	auto totalDuration = XSDDayTimeDuration{0, 0, 0, total};

	if (resultPos.m_isVariable)
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromRulePos(resultPos, pKB, bindingList);
		auto resultDuration = XSDDayTimeDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::DivideDayTimeDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultPos = getRulePosList().at(0);
	auto durationPos = getRulePosList().at(1);
	auto divisorPos = getRulePosList().at(2);

	auto durationArgStr = getLiteralStrFromRulePos(durationPos, pKB, bindingList);
	auto duration = XSDDayTimeDuration{durationArgStr};

	auto divisor = getDoubleFromRulePos(divisorPos, pKB, bindingList);

	//TODO:  Not sure why we're rounding, here
	auto total = floor(duration.totalSeconds() / divisor + 0.5);
	auto totalDuration = XSDDayTimeDuration{0, 0, 0, total};

	if (resultPos.m_isVariable)
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromRulePos(resultPos, pKB, bindingList);
		auto resultDuration = XSDDayTimeDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::SubtractDateTimesYieldingYearMonthDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDurationPos = getRulePosList().at(0);
	auto firstDatetimePos = getRulePosList().at(1);
	auto secondDatetimePos = getRulePosList().at(2);

	auto firstDatetimeStr = getLiteralStrFromRulePos(firstDatetimePos, pKB, bindingList);
	auto firstDatetime = DateTimeUtils::getPTimeFromXsdDateTime(firstDatetimeStr);

	auto secondDatetimeStr = getLiteralStrFromRulePos(secondDatetimePos, pKB, bindingList);
	auto secondDatetime = DateTimeUtils::getPTimeFromXsdDateTime(secondDatetimeStr);

	auto firstDate = firstDatetime.date();
	auto secondDate = secondDatetime.date();

	// use month iterator to avoid snap-to-end of month behavior
	auto isNegative = (firstDate < secondDate);
	auto startDate = isNegative ? firstDate : secondDate;
	auto targetDate = isNegative ? secondDate : firstDate;

	auto monthIter = ::boost::gregorian::month_iterator{startDate};
	++monthIter;  // advance once to check for less than one month start condition

	auto countOfMonths = 0;
	for (; *monthIter < targetDate; ++monthIter)
	{
		++countOfMonths;
	}

	auto totalDuration = XSDYearMonthDuration{isNegative ? -countOfMonths : countOfMonths};

	if (resultDurationPos.m_isVariable)
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDurationPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromRulePos(resultDurationPos, pKB, bindingList);
		auto resultDuration = XSDYearMonthDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::SubtractDateTimesYieldingDayTimeDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDurationPos = getRulePosList().at(0);
	auto firstDatetimePos = getRulePosList().at(1);
	auto secondDatetimePos = getRulePosList().at(2);

	auto firstDatetimeStr = getLiteralStrFromRulePos(firstDatetimePos, pKB, bindingList);
	auto firstDatetime = DateTimeUtils::getPTimeFromXsdDateTime(firstDatetimeStr);

	auto secondDatetimeStr = getLiteralStrFromRulePos(secondDatetimePos, pKB, bindingList);
	auto secondDatetime = DateTimeUtils::getPTimeFromXsdDateTime(secondDatetimeStr);

	auto totalDuration = XSDDayTimeDuration{firstDatetime - secondDatetime};

	if (resultDurationPos.m_isVariable)
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDurationPos.checkPositionAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromRulePos(resultDurationPos, pKB, bindingList);
		auto resultDuration = XSDDayTimeDuration{resultStr};
		return resultDuration == totalDuration;
	}
}
