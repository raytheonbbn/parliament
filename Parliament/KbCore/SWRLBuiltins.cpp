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
static const auto k_rsrcPlus = pmnt::convertToRsrcChar("+");
static const auto k_rsrcColon = pmnt::convertToRsrcChar(":");
static const auto k_rsrcHyphen = pmnt::convertToRsrcChar("-");
static const auto k_rsrcTee = pmnt::convertToRsrcChar("T");

static pmnt::Log::Source g_log(pmnt::Log::getSource("SWRLBuiltins"));

// TODO: complete this method
bool pmnt::AddBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto addVal = 0.0;

	auto current = begin(getAtomSlotList());
	auto result = *current;
	++current;

	for (; current != end(getAtomSlotList()); ++current)
	{
		auto val = getDoubleFromAtomSlot(*current, pKB, bindingList);
		addVal += val;
	}

	// TODO: is this correct?
	return checkResult(result, addVal, pKB, bindingList);
}

bool pmnt::SubtractBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultSlot = getAtomSlotList().at(0);
	auto firstSlot = getAtomSlotList().at(1);
	auto secondSlot = getAtomSlotList().at(2);

	auto firstVal = getDoubleFromAtomSlot(firstSlot, pKB, bindingList);
	auto secondVal = getDoubleFromAtomSlot(secondSlot, pKB, bindingList);

	auto resultVal = firstVal - secondVal;
	return checkResult(resultSlot, resultVal, pKB, bindingList);
}

// TODO: complete this method
bool pmnt::MultiplyBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto current = begin(getAtomSlotList());
	auto result = *current;
	++current;

	// TODO: handle ints v. double
	auto multVal = 1.0;

	for (; current != end(getAtomSlotList()); ++current)
	{
		multVal *= getDoubleFromAtomSlot(*current, pKB, bindingList);
	}

	// TODO: is this correct?
	return checkResult(result, multVal, pKB, bindingList);
}

bool pmnt::DivideBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultSlot = getAtomSlotList().at(0);
	auto firstSlot = getAtomSlotList().at(1);
	auto secondSlot = getAtomSlotList().at(2);

	auto firstVal = getDoubleFromAtomSlot(firstSlot, pKB, bindingList);
	auto secondVal = getDoubleFromAtomSlot(secondSlot, pKB, bindingList);

	auto resultVal = firstVal / secondVal; // todo: correct for all types?  what of check for DivByZero?
	return checkResult(resultSlot, resultVal, pKB, bindingList);
}

//TODO: Fix the iequals to handle Unicode properly.  How do we know the locale?
bool pmnt::StringEqualIngnoreCaseBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto firstSlot = getAtomSlotList().at(0);
	auto secondSlot = getAtomSlotList().at(1);

	auto firstStr = getLiteralStrFromAtomSlot(firstSlot, pKB, bindingList);
	auto secondStr = getLiteralStrFromAtomSlot(secondSlot, pKB, bindingList);
	auto firstStrUtf8 = convertFromRsrcChar(firstStr);
	auto secondStrUtf8 = convertFromRsrcChar(secondStr);
	return ::boost::iequals(firstStrUtf8, secondStrUtf8);
}

bool pmnt::StringConcatBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto result = getAtomSlotList().at(0);
	auto firstStr = getLiteralStrFromAtomSlot(result, pKB, bindingList);;
	RsrcString secondStr;
	auto isFirstIteration = true;
	for (auto current = begin(getAtomSlotList()); current != end(getAtomSlotList()); ++current)
	{
		if (isFirstIteration)
		{
			isFirstIteration = false;
		}
		else
		{
			secondStr += getLiteralStrFromAtomSlot(*current, pKB, bindingList);
		}
	}

	if (result.isVariable())
	{
		auto resultId = pKB->uriToRsrcId(k_rsrcQuote + secondStr + k_rsrcQuote, true, true);
		return result.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		return firstStr == secondStr;
	}
}

bool pmnt::YearMonthDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto yearMonthDurationSlot = getAtomSlotList().at(0);
	auto yearsSlot = getAtomSlotList().at(1);
	auto monthsSlot = getAtomSlotList().at(2);

	auto years = lexical_cast<int64>(convertFromRsrcChar(getLiteralStrFromAtomSlot(yearsSlot, pKB, bindingList)));
	auto months = lexical_cast<int64>(convertFromRsrcChar(getLiteralStrFromAtomSlot(monthsSlot, pKB, bindingList)));

	XSDYearMonthDuration resultDuration(years, months);

	if (yearMonthDurationSlot.isVariable())
	{
		auto resultId = pKB->uriToRsrcId(resultDuration.toXsdLiteral(), true, true);
		return yearMonthDurationSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		XSDYearMonthDuration foundDuration(getLiteralStrFromAtomSlot(yearMonthDurationSlot, pKB, bindingList));
		return foundDuration == resultDuration;
	}
}

bool pmnt::DayTimeDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto durationSlot = getAtomSlotList().at(0);
	auto daysSlot = getAtomSlotList().at(1);
	auto hourSlot = getAtomSlotList().at(2);
	auto minutesSlot = getAtomSlotList().at(3);
	auto secondsSlot = getAtomSlotList().at(4);

	auto days = lexical_cast<int64>(convertFromRsrcChar(getLiteralStrFromAtomSlot(daysSlot, pKB, bindingList)));
	auto hours = lexical_cast<int64>(convertFromRsrcChar(getLiteralStrFromAtomSlot(hourSlot, pKB, bindingList)));
	auto minutes = lexical_cast<int64>(convertFromRsrcChar(getLiteralStrFromAtomSlot(minutesSlot, pKB, bindingList)));
	auto seconds = lexical_cast<double>(convertFromRsrcChar(getLiteralStrFromAtomSlot(secondsSlot, pKB, bindingList)));

	XSDDayTimeDuration duration(days, hours, minutes, seconds);

	if (durationSlot.isVariable())
	{
		auto resultId = pKB->uriToRsrcId(duration.toXsdLiteral(), true, true);
		return durationSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		XSDDayTimeDuration resultDuration(getLiteralStrFromAtomSlot(durationSlot, pKB, bindingList));
		return resultDuration == duration;
	}
}

bool pmnt::TimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto timeSlot = getAtomSlotList().at(0);
	auto hourSlot = getAtomSlotList().at(1);
	auto minSlot = getAtomSlotList().at(2);
	auto secSlot = getAtomSlotList().at(3);
	auto tzSlot = getAtomSlotList().at(4);

	auto hours = getLiteralStrFromAtomSlot(hourSlot, pKB, bindingList);
	auto minutes = getLiteralStrFromAtomSlot(minSlot, pKB, bindingList);
	auto seconds = getLiteralStrFromAtomSlot(secSlot, pKB, bindingList);
	auto tzStr = getLiteralStrFromAtomSlot(tzSlot, pKB, bindingList);

	auto resultTimeStr = hours + k_rsrcColon + minutes + k_rsrcColon + seconds + tzStr;
	auto resultTime = DateTimeUtils::getPTimeFromXsdTime(resultTimeStr);

	if (timeSlot.isVariable())
	{
		auto timeStr = DateTimeUtils::getXsdTimeLiteral(resultTime);
		auto resultId = pKB->uriToRsrcId(timeStr, true, true);
		return timeSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundTimeStr = getLiteralStrFromAtomSlot(timeSlot, pKB, bindingList);
		auto foundTime = DateTimeUtils::getPTimeFromXsdTime(foundTimeStr);
		return foundTime == resultTime;
	}
}

bool pmnt::DateTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto dateTimeSlot = getAtomSlotList().at(0);
	auto yearSlot = getAtomSlotList().at(1);
	auto monthSlot = getAtomSlotList().at(2);
	auto daySlot = getAtomSlotList().at(3);
	auto hourSlot = getAtomSlotList().at(4);
	auto minSlot = getAtomSlotList().at(5);
	auto secSlot = getAtomSlotList().at(6);
	auto tzSlot = getAtomSlotList().at(7);

	auto years = getLiteralStrFromAtomSlot(yearSlot, pKB, bindingList);
	auto months = getLiteralStrFromAtomSlot(monthSlot, pKB, bindingList);
	auto days = getLiteralStrFromAtomSlot(daySlot, pKB, bindingList);
	auto hours = getLiteralStrFromAtomSlot(hourSlot, pKB, bindingList);
	auto minutes = getLiteralStrFromAtomSlot(minSlot, pKB, bindingList);
	auto seconds = getLiteralStrFromAtomSlot(secSlot, pKB, bindingList);
	auto tzStr = getLiteralStrFromAtomSlot(tzSlot, pKB, bindingList);

	auto resultDatetimeStr = years + k_rsrcHyphen + months + k_rsrcHyphen + days
		+ k_rsrcTee + hours + k_rsrcColon + minutes + k_rsrcColon + seconds + tzStr;
	auto resultDatetime = DateTimeUtils::getPTimeFromXsdDateTime(resultDatetimeStr);

	if (dateTimeSlot.isVariable())
	{
		auto datetimeStr = DateTimeUtils::getXsdDateTimeLiteral(resultDatetime);
		auto resultId = pKB->uriToRsrcId(datetimeStr, true, true);
		return dateTimeSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDatetimeStr = getLiteralStrFromAtomSlot(dateTimeSlot, pKB, bindingList);
		auto foundDatetime = DateTimeUtils::getPTimeFromXsdDateTime(foundDatetimeStr);
		return foundDatetime == resultDatetime;
	}
}

bool pmnt::DateBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto dateSlot = getAtomSlotList().at(0);
	auto yearSlot = getAtomSlotList().at(1);
	auto monthSlot = getAtomSlotList().at(2);
	auto daySlot = getAtomSlotList().at(3);
	auto timezoneSlot = getAtomSlotList().at(4);

	auto year = getLiteralStrFromAtomSlot(yearSlot, pKB, bindingList);
	auto month = getLiteralStrFromAtomSlot(monthSlot, pKB, bindingList);
	auto day = getLiteralStrFromAtomSlot(daySlot, pKB, bindingList);
	auto tzStr = getLiteralStrFromAtomSlot(timezoneSlot, pKB, bindingList);
	if (!tzStr.empty() && tzStr[0] != 'Z' && tzStr[0] != '-' && tzStr[0] != '+')
	{
		tzStr = k_rsrcPlus + tzStr;
	}

	auto resultDateStr = year + k_rsrcHyphen + month + k_rsrcHyphen + day + tzStr;
	auto resultDate = DateTimeUtils::getDateFromXsdDate(resultDateStr);

	if (dateSlot.isVariable())
	{
		auto dateStr = DateTimeUtils::getXsdDateLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(dateStr, true, true);
		return dateSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDateStr = getLiteralStrFromAtomSlot(dateSlot, pKB, bindingList);
		auto foundDate = DateTimeUtils::getDateFromXsdDate(foundDateStr);
		return foundDate == resultDate;
	}
}

bool pmnt::SubtractDatesBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDurationSlot = getAtomSlotList().at(0);
	auto firstDateSlot = getAtomSlotList().at(1);
	auto secondDateSlot = getAtomSlotList().at(2);

	auto firstDateStr = getLiteralStrFromAtomSlot(firstDateSlot, pKB, bindingList);
	auto secondDateStr = getLiteralStrFromAtomSlot(secondDateSlot, pKB, bindingList);

	// need ptimes for day-minute duration object, as specified in built-in definition.
	auto firstDate = DateTimeUtils::getPTimeFromXsdDate(firstDateStr);
	auto secondDate = DateTimeUtils::getPTimeFromXsdDate(secondDateStr);

	auto resultDuration = XSDDayTimeDuration{firstDate - secondDate};

	if (resultDurationSlot.isVariable())
	{
		auto resultId = pKB->uriToRsrcId(resultDuration.toXsdLiteral(), true, true);
		return resultDurationSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDuration = XSDDayTimeDuration{getLiteralStrFromAtomSlot(resultDurationSlot, pKB, bindingList)};
		return foundDuration == resultDuration;
	}
}

bool pmnt::SubtractTimesBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDurationSlot = getAtomSlotList().at(0);
	auto firstTimeSlot = getAtomSlotList().at(1);
	auto secondTimeSlot = getAtomSlotList().at(2);

	auto firstTimeStr = getLiteralStrFromAtomSlot(firstTimeSlot, pKB, bindingList);
	auto secondTimeStr = getLiteralStrFromAtomSlot(secondTimeSlot, pKB, bindingList);

	// need ptimes for day-minute duration object, as specified in built-in definition.
	auto firstTime = DateTimeUtils::getPTimeFromXsdTime(firstTimeStr);
	auto secondTime = DateTimeUtils::getPTimeFromXsdTime(secondTimeStr);

	auto resultDuration = XSDDayTimeDuration{firstTime - secondTime};

	if (resultDurationSlot.isVariable())
	{
		auto resultId = pKB->uriToRsrcId(resultDuration.toXsdLiteral(), true, true);
		return resultDurationSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDuration = XSDDayTimeDuration{getLiteralStrFromAtomSlot(resultDurationSlot, pKB, bindingList)};
		return foundDuration == resultDuration;
	}
}

bool pmnt::AddYearMonthDurationToDateTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatetimeSlot = getAtomSlotList().at(0);
	auto datetimeArgSlot = getAtomSlotList().at(1);
	auto durationArgSlot = getAtomSlotList().at(2);

	auto datetimeArgStr = getLiteralStrFromAtomSlot(datetimeArgSlot, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromAtomSlot(durationArgSlot, pKB, bindingList);

	auto durationArg = XSDYearMonthDuration{durationArgStr};

	auto datetimeArg = DateTimeUtils::getPTimeFromXsdDateTime(datetimeArgStr);
	auto resultDate = datetimeArg.date() + durationArg.totalMonthsAsDuration();

	auto resultDatetime = ::boost::posix_time::ptime{resultDate, datetimeArg.time_of_day()};

	if (resultDatetimeSlot.isVariable())
	{
		auto resultStr = DateTimeUtils::getXsdDateTimeLiteral(resultDatetime);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatetimeSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDatetimeStr = getLiteralStrFromAtomSlot(resultDatetimeSlot, pKB, bindingList);
		auto foundDatetime = DateTimeUtils::getPTimeFromXsdDateTime(foundDatetimeStr);
		return foundDatetime == resultDatetime;
	}
}

bool pmnt::AddDayTimeDurationToDateTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatetimeSlot = getAtomSlotList().at(0);
	auto datetimeArgSlot = getAtomSlotList().at(1);
	auto durationArgSlot = getAtomSlotList().at(2);

	auto datetimeArgStr = getLiteralStrFromAtomSlot(datetimeArgSlot, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromAtomSlot(durationArgSlot, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto datetimeArg = DateTimeUtils::getPTimeFromXsdDateTime(datetimeArgStr);
	auto resultDatetime = datetimeArg + durationArg.asTimeDuration();

	if (resultDatetimeSlot.isVariable())
	{
		auto resultStr = DateTimeUtils::getXsdDateTimeLiteral(resultDatetime);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatetimeSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDatetimeStr = getLiteralStrFromAtomSlot(resultDatetimeSlot, pKB, bindingList);
		auto foundDatetime = DateTimeUtils::getPTimeFromXsdDateTime(foundDatetimeStr);
		return foundDatetime == resultDatetime;
	}
}

bool pmnt::SubtractYearMonthDurationFromDateTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatetimeSlot = getAtomSlotList().at(0);
	auto datetimeArgSlot = getAtomSlotList().at(1);
	auto durationArgSlot = getAtomSlotList().at(2);

	auto datetimeArgStr = getLiteralStrFromAtomSlot(datetimeArgSlot, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromAtomSlot(durationArgSlot, pKB, bindingList);

	auto durationArg = XSDYearMonthDuration{durationArgStr};

	auto datetimeArg = DateTimeUtils::getPTimeFromXsdDateTime(datetimeArgStr);
	auto resultDate = datetimeArg.date() - durationArg.totalMonthsAsDuration();

	auto resultDatetime = ::boost::posix_time::ptime{resultDate, datetimeArg.time_of_day()};

	if (resultDatetimeSlot.isVariable())
	{
		auto resultStr = DateTimeUtils::getXsdDateTimeLiteral(resultDatetime);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatetimeSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDatetimeStr = getLiteralStrFromAtomSlot(resultDatetimeSlot, pKB, bindingList);
		auto foundDatetime = DateTimeUtils::getPTimeFromXsdDateTime(foundDatetimeStr);
		return foundDatetime == resultDatetime;
	}
}

bool pmnt::SubtractDayTimeDurationFromDateTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDatetimeSlot = getAtomSlotList().at(0);
	auto datetimeArgSlot = getAtomSlotList().at(1);
	auto durationArgSlot = getAtomSlotList().at(2);

	auto datetimeArgStr = getLiteralStrFromAtomSlot(datetimeArgSlot, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromAtomSlot(durationArgSlot, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto datetimeArg = DateTimeUtils::getPTimeFromXsdDateTime(datetimeArgStr);
	auto resultDatetime = datetimeArg - durationArg.asTimeDuration();

	if (resultDatetimeSlot.isVariable())
	{
		auto resultStr = DateTimeUtils::getXsdDateTimeLiteral(resultDatetime);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDatetimeSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDatetimeStr = getLiteralStrFromAtomSlot(resultDatetimeSlot, pKB, bindingList);
		auto foundDatetime = DateTimeUtils::getPTimeFromXsdDateTime(foundDatetimeStr);
		return foundDatetime == resultDatetime;
	}
}

bool pmnt::AddYearMonthDurationToDateBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDateSlot = getAtomSlotList().at(0);
	auto dateArgSlot = getAtomSlotList().at(1);
	auto durationArgSlot = getAtomSlotList().at(2);

	auto dateArgStr = getLiteralStrFromAtomSlot(dateArgSlot, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromAtomSlot(durationArgSlot, pKB, bindingList);

	auto durationArg = XSDYearMonthDuration{durationArgStr};

	auto dateArg = DateTimeUtils::getDateFromXsdDate(dateArgStr);
	auto resultDate = dateArg + durationArg.totalMonthsAsDuration();

	if (resultDateSlot.isVariable())
	{
		auto resultStr = DateTimeUtils::getXsdDateLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDateSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDateStr = getLiteralStrFromAtomSlot(resultDateSlot, pKB, bindingList);
		auto foundDate = DateTimeUtils::getDateFromXsdDate(foundDateStr);
		return foundDate == resultDate;
	}
}

bool pmnt::SubtractYearMonthDurationFromDateBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDateSlot = getAtomSlotList().at(0);
	auto dateArgSlot = getAtomSlotList().at(1);
	auto durationArgSlot = getAtomSlotList().at(2);

	auto dateArgStr = getLiteralStrFromAtomSlot(dateArgSlot, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromAtomSlot(durationArgSlot, pKB, bindingList);

	auto durationArg = XSDYearMonthDuration{durationArgStr};

	auto dateArg = DateTimeUtils::getDateFromXsdDate(dateArgStr);
	auto resultDate = dateArg - durationArg.totalMonthsAsDuration();

	if (resultDateSlot.isVariable())
	{
		auto resultStr = DateTimeUtils::getXsdDateLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDateSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDateStr = getLiteralStrFromAtomSlot(resultDateSlot, pKB, bindingList);
		auto foundDate = DateTimeUtils::getDateFromXsdDate(foundDateStr);
		return foundDate == resultDate;
	}
}

bool pmnt::AddDayTimeDurationToDateBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDateSlot = getAtomSlotList().at(0);
	auto dateArgSlot = getAtomSlotList().at(1);
	auto durationArgSlot = getAtomSlotList().at(2);

	auto dateArgStr = getLiteralStrFromAtomSlot(dateArgSlot, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromAtomSlot(durationArgSlot, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto dateArg = DateTimeUtils::getPTimeFromXsdDate(dateArgStr);
	auto resultDate = dateArg + durationArg.asTimeDuration();

	if (resultDateSlot.isVariable())
	{
		auto resultStr = DateTimeUtils::getXsdDateLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDateSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDateStr = getLiteralStrFromAtomSlot(resultDateSlot, pKB, bindingList);
		auto foundDate = DateTimeUtils::getPTimeFromXsdDate(foundDateStr);
		return foundDate == resultDate;
	}
}

bool pmnt::SubtractDayTimeDurationFromDateBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDateSlot = getAtomSlotList().at(0);
	auto dateArgSlot = getAtomSlotList().at(1);
	auto durationArgSlot = getAtomSlotList().at(2);

	auto dateArgStr = getLiteralStrFromAtomSlot(dateArgSlot, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromAtomSlot(durationArgSlot, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto dateArg = DateTimeUtils::getPTimeFromXsdDate(dateArgStr);
	auto resultDate = dateArg - durationArg.asTimeDuration();

	if (resultDateSlot.isVariable())
	{
		auto resultStr = DateTimeUtils::getXsdDateLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDateSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundDateStr = getLiteralStrFromAtomSlot(resultDateSlot, pKB, bindingList);
		auto foundDate = DateTimeUtils::getPTimeFromXsdDate(foundDateStr);
		return foundDate == resultDate;
	}
}

bool pmnt::AddDayTimeDurationToTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultTimeSlot = getAtomSlotList().at(0);
	auto timeArgSlot = getAtomSlotList().at(1);
	auto durationArgSlot = getAtomSlotList().at(2);

	auto timeArgStr = getLiteralStrFromAtomSlot(timeArgSlot, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromAtomSlot(durationArgSlot, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto timeArg = DateTimeUtils::getPTimeFromXsdTime(timeArgStr);
	auto resultDate = timeArg + durationArg.asTimeDuration();

	if (resultTimeSlot.isVariable())
	{
		auto resultStr = DateTimeUtils::getXsdTimeLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultTimeSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundTimeStr = getLiteralStrFromAtomSlot(resultTimeSlot, pKB, bindingList);
		auto foundDate = DateTimeUtils::getPTimeFromXsdTime(foundTimeStr);
		return foundDate == resultDate;
	}
}

bool pmnt::SubtractDayTimeDurationFromTimeBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultTimeSlot = getAtomSlotList().at(0);
	auto timeArgSlot = getAtomSlotList().at(1);
	auto durationArgSlot = getAtomSlotList().at(2);

	auto timeArgStr = getLiteralStrFromAtomSlot(timeArgSlot, pKB, bindingList);
	auto durationArgStr = getLiteralStrFromAtomSlot(durationArgSlot, pKB, bindingList);

	auto durationArg = XSDDayTimeDuration{durationArgStr};

	auto timeArg = DateTimeUtils::getPTimeFromXsdTime(timeArgStr);
	auto resultDate = timeArg - durationArg.asTimeDuration();

	if (resultTimeSlot.isVariable())
	{
		auto resultStr = DateTimeUtils::getXsdTimeLiteral(resultDate);
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultTimeSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto foundTimeStr = getLiteralStrFromAtomSlot(resultTimeSlot, pKB, bindingList);
		auto foundDate = DateTimeUtils::getPTimeFromXsdTime(foundTimeStr);
		return foundDate == resultDate;
	}
}

bool pmnt::AddYearMonthDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto current = begin(getAtomSlotList());
	auto resultSlot = *current;
	++current;

	auto totalDuration = XSDYearMonthDuration{};
	for (; current != end(getAtomSlotList()); ++current)
	{
		auto durationArgStr = getLiteralStrFromAtomSlot(*current, pKB, bindingList);
		totalDuration += XSDYearMonthDuration{durationArgStr};
	}

	if (resultSlot.isVariable())
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromAtomSlot(*current, pKB, bindingList);
		auto resultDuration = XSDYearMonthDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::SubtractYearMonthDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultSlot = getAtomSlotList().at(0);
	auto firstDurationSlot = getAtomSlotList().at(1);
	auto secondDurationSlot = getAtomSlotList().at(2);

	auto firstDurationStr = getLiteralStrFromAtomSlot(firstDurationSlot, pKB, bindingList);
	auto firstDuration = XSDYearMonthDuration{firstDurationStr};

	auto secondDurationStr = getLiteralStrFromAtomSlot(secondDurationSlot, pKB, bindingList);
	auto secondDuration = XSDYearMonthDuration{secondDurationStr};

	auto totalDuration = firstDuration - secondDuration;

	if (resultSlot.isVariable())
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromAtomSlot(resultSlot, pKB, bindingList);
		auto resultDuration = XSDYearMonthDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::MultiplyYearMonthDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultSlot = getAtomSlotList().at(0);
	auto durationSlot = getAtomSlotList().at(1);
	auto scaleSlot = getAtomSlotList().at(2);

	auto durationArgStr = getLiteralStrFromAtomSlot(durationSlot, pKB, bindingList);
	auto duration = XSDYearMonthDuration{durationArgStr};

	auto scale = getDoubleFromAtomSlot(scaleSlot, pKB, bindingList);

	auto total = duration.months() * scale;
	auto totalRounded = static_cast<int64>(floor(total + 0.5)); // must round

	auto totalDuration = XSDYearMonthDuration{totalRounded};

	if (resultSlot.isVariable())
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromAtomSlot(resultSlot, pKB, bindingList);
		auto resultDuration = XSDYearMonthDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::DivideYearMonthDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultSlot = getAtomSlotList().at(0);
	auto durationSlot = getAtomSlotList().at(1);
	auto divisorSlot = getAtomSlotList().at(2);

	auto durationArgStr = getLiteralStrFromAtomSlot(durationSlot, pKB, bindingList);
	auto duration = XSDYearMonthDuration{durationArgStr};

	auto divisor = getDoubleFromAtomSlot(divisorSlot, pKB, bindingList);

	auto total = duration.months() / divisor;
	auto totalRounded = static_cast<int64>(floor(total + 0.5)); // must round

	auto totalDuration = XSDYearMonthDuration{totalRounded};

	if (resultSlot.isVariable())
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromAtomSlot(resultSlot, pKB, bindingList);
		auto resultDuration = XSDYearMonthDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::AddDayTimeDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto current = begin(getAtomSlotList());
	auto result = *current;

	++current;

	auto totalDuration = XSDDayTimeDuration{};
	for (; current != end(getAtomSlotList()); ++current)
	{
		auto durationArgStr = getLiteralStrFromAtomSlot(*current, pKB, bindingList);
		totalDuration += XSDDayTimeDuration{durationArgStr};
	}

	if (result.isVariable())
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return result.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromAtomSlot(*current, pKB, bindingList);
		auto resultDuration = XSDDayTimeDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::SubtractDayTimeDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultSlot = getAtomSlotList().at(0);
	auto firstDurationSlot = getAtomSlotList().at(1);
	auto secondDurationSlot = getAtomSlotList().at(2);

	auto firstDurationStr = getLiteralStrFromAtomSlot(firstDurationSlot, pKB, bindingList);
	auto firstDuration = XSDDayTimeDuration{firstDurationStr};

	auto secondDurationStr = getLiteralStrFromAtomSlot(secondDurationSlot, pKB, bindingList);
	auto secondDuration = XSDDayTimeDuration{secondDurationStr};

	auto totalDuration = firstDuration - secondDuration;

	if (resultSlot.isVariable())
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromAtomSlot(resultSlot, pKB, bindingList);
		auto resultDuration = XSDDayTimeDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::MultiplyDayTimeDurationsBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultSlot = getAtomSlotList().at(0);
	auto durationSlot = getAtomSlotList().at(1);
	auto scaleSlot = getAtomSlotList().at(2);

	auto durationArgStr = getLiteralStrFromAtomSlot(durationSlot, pKB, bindingList);
	auto duration = XSDDayTimeDuration{durationArgStr};

	auto scale = getDoubleFromAtomSlot(scaleSlot, pKB, bindingList);

	//TODO:  Not sure why we're rounding, here
	auto total = floor(duration.totalSeconds() * scale + 0.5);
	auto totalDuration = XSDDayTimeDuration{0, 0, 0, total};

	if (resultSlot.isVariable())
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromAtomSlot(resultSlot, pKB, bindingList);
		auto resultDuration = XSDDayTimeDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::DivideDayTimeDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultSlot = getAtomSlotList().at(0);
	auto durationSlot = getAtomSlotList().at(1);
	auto divisorSlot = getAtomSlotList().at(2);

	auto durationArgStr = getLiteralStrFromAtomSlot(durationSlot, pKB, bindingList);
	auto duration = XSDDayTimeDuration{durationArgStr};

	auto divisor = getDoubleFromAtomSlot(divisorSlot, pKB, bindingList);

	//TODO:  Not sure why we're rounding, here
	auto total = floor(duration.totalSeconds() / divisor + 0.5);
	auto totalDuration = XSDDayTimeDuration{0, 0, 0, total};

	if (resultSlot.isVariable())
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromAtomSlot(resultSlot, pKB, bindingList);
		auto resultDuration = XSDDayTimeDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::SubtractDateTimesYieldingYearMonthDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDurationSlot = getAtomSlotList().at(0);
	auto firstDatetimeSlot = getAtomSlotList().at(1);
	auto secondDatetimeSlot = getAtomSlotList().at(2);

	auto firstDatetimeStr = getLiteralStrFromAtomSlot(firstDatetimeSlot, pKB, bindingList);
	auto firstDatetime = DateTimeUtils::getPTimeFromXsdDateTime(firstDatetimeStr);

	auto secondDatetimeStr = getLiteralStrFromAtomSlot(secondDatetimeSlot, pKB, bindingList);
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

	if (resultDurationSlot.isVariable())
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDurationSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromAtomSlot(resultDurationSlot, pKB, bindingList);
		auto resultDuration = XSDYearMonthDuration{resultStr};
		return resultDuration == totalDuration;
	}
}

bool pmnt::SubtractDateTimesYieldingDayTimeDurationBuiltinRuleAtom::evalImpl(KbInstance* pKB, BindingList& bindingList) const
{
	auto resultDurationSlot = getAtomSlotList().at(0);
	auto firstDatetimeSlot = getAtomSlotList().at(1);
	auto secondDatetimeSlot = getAtomSlotList().at(2);

	auto firstDatetimeStr = getLiteralStrFromAtomSlot(firstDatetimeSlot, pKB, bindingList);
	auto firstDatetime = DateTimeUtils::getPTimeFromXsdDateTime(firstDatetimeStr);

	auto secondDatetimeStr = getLiteralStrFromAtomSlot(secondDatetimeSlot, pKB, bindingList);
	auto secondDatetime = DateTimeUtils::getPTimeFromXsdDateTime(secondDatetimeStr);

	auto totalDuration = XSDDayTimeDuration{firstDatetime - secondDatetime};

	if (resultDurationSlot.isVariable())
	{
		auto resultStr = totalDuration.toXsdLiteral();
		auto resultId = pKB->uriToRsrcId(resultStr, true, true);
		return resultDurationSlot.checkSlotAddBinding(resultId, bindingList);
	}
	else
	{
		auto resultStr = getLiteralStrFromAtomSlot(resultDurationSlot, pKB, bindingList);
		auto resultDuration = XSDDayTimeDuration{resultStr};
		return resultDuration == totalDuration;
	}
}
