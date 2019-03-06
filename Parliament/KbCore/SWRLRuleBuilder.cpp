// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/SWRLRuleBuilder.h"
#include "parliament/KbInstance.h"
#include "parliament/Log.h"
#include "parliament/SWRLBuiltins.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"

#include <boost/format.hpp>
#include <iterator>

namespace pmnt = ::bbn::parliament;

using ::boost::format;

static auto g_log(::pmnt::log::getSource("SWRLRuleBuilder"));

void pmnt::SWRLRuleBuilder::addAtomToRule(ResourceId atomRsrcId, StandardRule* pRule,
	VarMap& varMap, bool isRuleHead)
{
	// get atom type
	ResourceId typeId = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_rdfType.id());
	if (typeId == k_nullRsrcId)
	{
		throw Exception("Atom type indeterminate in SWRL rule");
	}
	else if (typeId == uriLib().m_swrlBuiltinAtom.id())
	{
		pRule->addBuiltInAtom(buildBuiltinAtom(atomRsrcId, varMap, isRuleHead), isRuleHead);
	}
	else if (typeId == uriLib().m_swrlClassAtom.id())
	{
		pRule->addAtom(buildClassAtom(atomRsrcId, varMap, isRuleHead), isRuleHead);
	}
	else if (typeId == uriLib().m_swrlDataRangeAtom.id())
	{
		pRule->addAtom(buildDataRangeAtom(atomRsrcId, varMap, isRuleHead), isRuleHead);
	}
	else if (typeId == uriLib().m_swrlDatavaluedPropertyAtom.id())
	{
		pRule->addAtom(buildDatavaluedPropertyAtom(atomRsrcId, varMap, isRuleHead), isRuleHead);
	}
	else if (typeId == uriLib().m_swrlDifferentIndividualsAtom.id())
	{
		pRule->addAtom(buildDifferentIndividualsAtom(atomRsrcId, varMap, isRuleHead), isRuleHead);
	}
	else if (typeId == uriLib().m_swrlSameIndividualAtom.id())
	{
		pRule->addAtom(buildSameIndividualAtom(atomRsrcId, varMap, isRuleHead), isRuleHead);
	}
	else if (typeId == uriLib().m_swrlIndividualPropertyAtom.id())
	{
		pRule->addAtom(buildIndividualPropertyAtom(atomRsrcId, varMap, isRuleHead), isRuleHead);
	}
	else
	{
		throw Exception("Unknown SWRL rule atom type");
	}
}

// TODO: refactor validity checking of args in atoms / deal better with bindings of argIds.

pmnt::SWRLBuiltinAtomPtr pmnt::SWRLRuleBuilder::buildBuiltinAtom(
	ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead)
{
	SWRLBuiltinAtomPtr pResult;

	ResourceId builtinId = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlBuiltin.id());
	if (builtinId == k_nullRsrcId)
	{
		throw Exception{format("Ill-formed SWRL builtin atom '%1%':  Missing builtin ID")
			% convertFromRsrcChar(safeRsrcIdToUri(atomRsrcId))};
	}

	ResourceId argListId = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlArguments.id());
	if (argListId == k_nullRsrcId)
	{
		throw Exception{format("Ill-formed SWRL builtin atom '%1%':  Missing argument list")
			% convertFromRsrcChar(safeRsrcIdToUri(atomRsrcId))};
	}

	// Compare builtin ID via string rather than rsrc ID so that
	// UriLib doesn't insert resources for unused builtins:
	const auto builtinIdStr = safeRsrcIdToUri(builtinId);
	if (builtinIdStr == uriLib().m_swrlbMultiply.str())
	{
		pResult = makeUnique<MultiplyBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbAdd.str())
	{
		pResult = makeUnique<AddBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtract.str())
	{
		pResult = makeUnique<SubtractBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbDivide.str())
	{
		pResult = makeUnique<DivideBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbStringEqualIgnoreCase.str())
	{
		pResult = makeUnique<StringEqualIngnoreCaseBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbStringConcat.str())
	{
		pResult = makeUnique<StringConcatBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbYearMonthDuration.str())
	{
		pResult = makeUnique<YearMonthDurationBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbDayTimeDuration.str())
	{
		pResult = makeUnique<DayTimeDurationBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbDateTime.str())
	{
		pResult = makeUnique<DateTimeBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbDate.str())
	{
		pResult = makeUnique<DateBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbTime.str())
	{
		pResult = makeUnique<TimeBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbAddYearMonthDurations.str())
	{
		pResult = makeUnique<AddYearMonthDurationsBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractYearMonthDurations.str())
	{
		pResult = makeUnique<SubtractYearMonthDurationsBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbMultiplyYearMonthDuration.str())
	{
		pResult = makeUnique<MultiplyYearMonthDurationBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbDivideYearMonthDurations.str())
	{
		pResult = makeUnique<DivideYearMonthDurationsBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbAddDayTimeDurations.str())
	{
		pResult = makeUnique<AddDayTimeDurationsBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractDayTimeDurations.str())
	{
		pResult = makeUnique<SubtractDayTimeDurationsBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbMultiplyDayTimeDurations.str())
	{
		pResult = makeUnique<MultiplyDayTimeDurationsBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbDivideDayTimeDuration.str())
	{
		pResult = makeUnique<DivideDayTimeDurationBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractDates.str())
	{
		pResult = makeUnique<SubtractDatesBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractTimes.str())
	{
		pResult = makeUnique<SubtractTimesBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbAddYearMonthDurationToDateTime.str())
	{
		pResult = makeUnique<AddYearMonthDurationToDateTimeBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbAddDayTimeDurationToDateTime.str())
	{
		pResult = makeUnique<AddDayTimeDurationToDateTimeBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractYearMonthDurationFromDateTime.str())
	{
		pResult = makeUnique<SubtractYearMonthDurationFromDateTimeBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractDayTimeDurationFromDateTime.str())
	{
		pResult = makeUnique<SubtractDayTimeDurationFromDateTimeBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbAddYearMonthDurationToDate.str())
	{
		pResult = makeUnique<AddYearMonthDurationToDateBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbAddDayTimeDurationToDate.str())
	{
		pResult = makeUnique<AddDayTimeDurationToDateBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractYearMonthDurationFromDate.str())
	{
		pResult = makeUnique<SubtractYearMonthDurationFromDateBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractDayTimeDurationFromDate.str())
	{
		pResult = makeUnique<SubtractDayTimeDurationFromDateBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbAddDayTimeDurationToTime.str())
	{
		pResult = makeUnique<AddDayTimeDurationToTimeBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractDayTimeDurationFromTime.str())
	{
		pResult = makeUnique<SubtractDayTimeDurationFromTimeBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractDateTimesYieldingYearMonthDuration.str())
	{
		pResult = makeUnique<SubtractDateTimesYieldingYearMonthDurationBuiltinRuleAtom>(builtinIdStr);
	}
	else if (builtinIdStr == uriLib().m_swrlbSubtractDateTimesYieldingDayTimeDuration.str())
	{
		pResult = makeUnique<SubtractDateTimesYieldingDayTimeDurationBuiltinRuleAtom>(builtinIdStr);
	}
	else
	{
		throw Exception{format("Invalid SWRL rule builtin ID:  '%1%'")
			% convertFromRsrcChar(builtinIdStr)};
	}

	checkAndIndexAtomArg(isRuleHead, builtinId, varMap);

	// need to check all arguments from list and add them to the slots list
	checkAndIndexArgList(pResult.get(), isRuleHead, argListId, varMap);

	PMNT_LOG(g_log, log::Level::debug) << format{"Returning builtin type '%1%' for ID '%2%'"}
		% typeid(pResult.get()).name() % convertFromRsrcChar(builtinIdStr);

	return pResult;
}

// builds a swrl:ClassAtom from the existing KB.
pmnt::RuleAtom pmnt::SWRLRuleBuilder::buildClassAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead)
{
	// TODO: should this handle fuller descriptions than mere references to a class?
	// rdf ont doesn't mention anything other than a classPredicate. SWRL document @ w3 talks of fuller descriptions.
	// For now, ignoring "owlish" swrl

	ResourceId classId = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlClassPredicate.id());
	ResourceId arg1Id = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlArgument1.id());
	if (classId == k_nullRsrcId || arg1Id == k_nullRsrcId)
	{
		throw Exception("Error: Badly formatted class atom.");
	}

	bool arg1IsVar = checkAndIndexAtomArg(isRuleHead, arg1Id, varMap);
	return RuleAtom(arg1IsVar ? RuleAtomSlot::createForVar(varMap.find(arg1Id)->second) : RuleAtomSlot::createForRsrc(arg1Id),
			RuleAtomSlot::createForRsrc(uriLib().m_rdfType.id()),
			RuleAtomSlot::createForRsrc(classId));
}

// builds a swrl:DataRangeAtom from the existing KB.
// TODO: implement. for now, ignoring due to issues related to checking resource's datatypes
pmnt::RuleAtom pmnt::SWRLRuleBuilder::buildDataRangeAtom(ResourceId atomRsrcId, const VarMap& varMap, bool isRuleHead)
{
	throw Exception("Error: Do not currently support DataRange atoms!");

	//ResourceId dataRangeId = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlDataRange.id());
	//ResourceId arg1Id = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlArgument1.id());
	//if (dataRangeId == k_nullRsrcId || arg1Id == k_nullRsrcId)
	//{
	//	throw Exception("Error: Badly formatted data range atom.");
	//}
}

// builds a swrl:DatavaluedPropertyAtom from existing KB
// TODO: restrict to literal datavalues and enforce that the property is a datatype property
// requires first class support of datatypes at the resource of Parliament.
pmnt::RuleAtom pmnt::SWRLRuleBuilder::buildDatavaluedPropertyAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead)
{
	return buildIndividualPropertyAtom(atomRsrcId, varMap, isRuleHead);
}

// builds a swrl:DifferentIndividualsAtom from the existing KB.
pmnt::RuleAtom pmnt::SWRLRuleBuilder::buildDifferentIndividualsAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead)
{
	ResourceId arg1Id = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlArgument1.id());
	ResourceId arg2Id = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlArgument2.id());
	if (arg1Id == k_nullRsrcId || arg2Id == k_nullRsrcId)
	{
		throw Exception("Error: Badly formatted different individuals atom.");
	}

	//see if atom variables have already been encountered; if not, add them to varMap
	bool arg1IsVar = checkAndIndexAtomArg(isRuleHead, arg1Id, varMap);
	bool arg2IsVar = checkAndIndexAtomArg(isRuleHead, arg2Id, varMap);

	// TODO: is this correct?  Is there a different way that we should be checking this? open-world?
	// for now, we are operating with explicit statements that :x owl:differentFrom :y
	// TODO: does not deal with owl:AllDifferent statements
	return RuleAtom(arg1IsVar ? RuleAtomSlot::createForVar(varMap.find(arg1Id)->second) : RuleAtomSlot::createForRsrc(arg1Id),
			RuleAtomSlot::createForRsrc(uriLib().m_owlDifferentFrom.id()),
			arg2IsVar ? RuleAtomSlot::createForVar(varMap.find(arg2Id)->second) : RuleAtomSlot::createForRsrc(arg2Id));
}

// builds a swrl:IndividualPropertyAtom from the existing KB.
pmnt::RuleAtom pmnt::SWRLRuleBuilder::buildIndividualPropertyAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead)
{
	//get pieces of this atom
	ResourceId propertyId = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlpropertyPred.id());
	ResourceId arg1Id = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlArgument1.id());
	ResourceId arg2Id = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlArgument2.id());
	if (propertyId == k_nullRsrcId || arg1Id == k_nullRsrcId || arg2Id == k_nullRsrcId)
	{
		throw Exception("Error: Badly formatted individual property atom.");
	}

	//see if atom variables have already been encountered; if not, add them to varMap
	bool arg1IsVar = checkAndIndexAtomArg(isRuleHead, arg1Id, varMap);
	bool arg2IsVar = checkAndIndexAtomArg(isRuleHead, arg2Id, varMap);
	return RuleAtom(arg1IsVar ? RuleAtomSlot::createForVar(varMap.find(arg1Id)->second) : RuleAtomSlot::createForRsrc(arg1Id),
			RuleAtomSlot::createForRsrc(propertyId),
			arg2IsVar ? RuleAtomSlot::createForVar(varMap.find(arg2Id)->second) : RuleAtomSlot::createForRsrc(arg2Id));
}

pmnt::RuleAtom pmnt::SWRLRuleBuilder::buildSameIndividualAtom(ResourceId atomRsrcId, VarMap& varMap, bool isRuleHead)
{
	ResourceId arg1Id = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlArgument1.id());
	ResourceId arg2Id = m_pKB->findAndGetObjectId(atomRsrcId, uriLib().m_swrlArgument2.id());
	if (arg1Id == k_nullRsrcId || arg2Id == k_nullRsrcId)
	{
		throw Exception("Error: Badly formatted same individual atom.");
	}

	//see if atom variables have already been encountered; if not, add them to varMap
	bool arg1IsVar = checkAndIndexAtomArg(isRuleHead, arg1Id, varMap);
	bool arg2IsVar = checkAndIndexAtomArg(isRuleHead, arg2Id, varMap);

	// TODO: is this correct?  Is there a different way that we should be checking this? open-world?
	// for now, we are operating with explicit statements that :x owl:sameAs :y
	return RuleAtom(arg1IsVar ? RuleAtomSlot::createForVar(varMap.find(arg1Id)->second) : RuleAtomSlot::createForRsrc(arg1Id),
			RuleAtomSlot::createForRsrc(uriLib().m_owlSameAs.id()),
			arg2IsVar ? RuleAtomSlot::createForVar(varMap.find(arg2Id)->second) : RuleAtomSlot::createForRsrc(arg2Id));
}

// Checks the SWRL argument:
// * If it is a variable, assigns it a reference index
// * If variable atoms have not already been encountered, adds them to varMap
// * Enforces rule safety, i.e., ensures that head variables are present in the body
// Returns true if the argument is a variable, false otherwise
bool pmnt::SWRLRuleBuilder::checkAndIndexAtomArg(bool isRuleHead, ResourceId argumentId,
	VarMap& varMap)
{
	bool argIsVar = false;
	if (m_pKB->find(argumentId, uriLib().m_rdfType.id(), uriLib().m_swrlVariable.id()) != m_pKB->end())
	{
		argIsVar = true;
		if (varMap.count(argumentId) <= 0)
		{
			if (isRuleHead)
			{
				throw Exception("Error: IndividualPropertyAtom in SWRL rule head contains"
					" a variable not present in the body");
			}
			else
			{
				varMap[argumentId] = static_cast<uint32>(varMap.size());
			}
		}
	}
	return argIsVar;
}

void pmnt::SWRLRuleBuilder::checkAndIndexArgList(SWRLBuiltinRuleAtom* pBuiltinAtom, bool isRuleHead, ResourceId argumentId, VarMap& varMap)
{
	ResourceId firstId = m_pKB->findAndGetObjectId(argumentId, uriLib().m_rdfListFirst.id());
	ResourceId restId = m_pKB->findAndGetObjectId(argumentId, uriLib().m_rdfListRest.id());
	if (firstId == k_nullRsrcId || restId == k_nullRsrcId)
	{
		throw Exception ("invalid rdf list: need both a first and rest argument.");
	}

	if (checkAndIndexAtomArg(isRuleHead, firstId, varMap))
	{
		pBuiltinAtom->appendAtomSlot(RuleAtomSlot::createForVar(varMap.find(firstId)->second));
	}
	else
	{
		pBuiltinAtom->appendAtomSlot(RuleAtomSlot::createForRsrc(firstId));
	}

	// stop when the rest is Nil
	if (uriLib().m_rdfListNil.id() !=  restId)
	{
		checkAndIndexArgList(pBuiltinAtom, isRuleHead, restId, varMap);
	}
}

pmnt::RsrcString pmnt::SWRLRuleBuilder::safeRsrcIdToUri(ResourceId rsrcId) const
{
	const RsrcChar* pBuiltinIdStr = (rsrcId == k_nullRsrcId)
		? nullptr
		: m_pKB->rsrcIdToUri(rsrcId);
	if (pBuiltinIdStr == nullptr)
	{
		return RsrcString();
	}
	else
	{
		return pBuiltinIdStr;
	}
}

const pmnt::UriLib& pmnt::SWRLRuleBuilder::uriLib() const
{
	return m_pKB->uriLib();
}
