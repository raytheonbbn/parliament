// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_SWRLBUILTINS_H_INCLUDED)
#define PARLIAMENT_SWRLBUILTINS_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"
#include "parliament/RuleEngine.h"

namespace bbn::parliament
{

struct AddBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	AddBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, ArgCountLimits::k_unbounded}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct MultiplyBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	MultiplyBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, ArgCountLimits::k_unbounded}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct DivideBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	DivideBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct StringEqualIngnoreCaseBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	StringEqualIngnoreCaseBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {2, 2}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct StringConcatBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	StringConcatBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, ArgCountLimits::k_unbounded}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct YearMonthDurationBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	YearMonthDurationBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct DayTimeDurationBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	DayTimeDurationBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {5, 5}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct TimeBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	TimeBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {5, 5}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct DateTimeBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	DateTimeBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {8, 8}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct DateBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	DateBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {5, 5}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractDatesBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractDatesBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractTimesBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractTimesBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct AddYearMonthDurationToDateTimeBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	AddYearMonthDurationToDateTimeBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct AddDayTimeDurationToDateTimeBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	AddDayTimeDurationToDateTimeBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractYearMonthDurationFromDateTimeBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractYearMonthDurationFromDateTimeBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractDayTimeDurationFromDateTimeBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractDayTimeDurationFromDateTimeBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct AddYearMonthDurationToDateBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	AddYearMonthDurationToDateBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractYearMonthDurationFromDateBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractYearMonthDurationFromDateBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct AddDayTimeDurationToDateBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	AddDayTimeDurationToDateBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractDayTimeDurationFromDateBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractDayTimeDurationFromDateBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct AddDayTimeDurationToTimeBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	AddDayTimeDurationToTimeBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractDayTimeDurationFromTimeBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractDayTimeDurationFromTimeBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct AddYearMonthDurationsBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	AddYearMonthDurationsBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractYearMonthDurationsBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractYearMonthDurationsBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct MultiplyYearMonthDurationBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	MultiplyYearMonthDurationBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct DivideYearMonthDurationsBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	DivideYearMonthDurationsBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct AddDayTimeDurationsBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	AddDayTimeDurationsBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, ArgCountLimits::k_unbounded}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractDayTimeDurationsBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractDayTimeDurationsBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct MultiplyDayTimeDurationsBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	MultiplyDayTimeDurationsBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct DivideDayTimeDurationBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	DivideDayTimeDurationBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractDateTimesYieldingYearMonthDurationBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractDateTimesYieldingYearMonthDurationBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

struct SubtractDateTimesYieldingDayTimeDurationBuiltinRuleAtom : public SWRLBuiltinRuleAtom
{
	SubtractDateTimesYieldingDayTimeDurationBuiltinRuleAtom(const RsrcString& id) : SWRLBuiltinRuleAtom(id) {}
	ArgCountLimits getArgCountLimits() const override { return {3, 3}; }
	bool evalImpl(KbInstance* pKB, BindingList& bindingList) const override;
};

}	// namespace end

#endif
