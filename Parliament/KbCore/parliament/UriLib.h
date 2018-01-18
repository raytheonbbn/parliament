// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_URILIB_H_INCLUDED)
#define PARLIAMENT_URILIB_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

PARLIAMENT_NAMESPACE_BEGIN

class KbInstance;

class LazyRsrc
{
public:
	LazyRsrc(KbInstance* pKB, const char*const pUri);
	LazyRsrc(const LazyRsrc&) = default;
	LazyRsrc& operator=(const LazyRsrc&) = default;
	LazyRsrc(LazyRsrc&&) = default;
	LazyRsrc& operator=(LazyRsrc&&) = default;
	~LazyRsrc() = default;

	PARLIAMENT_EXPORT ResourceId id() const;
	PARLIAMENT_EXPORT const RsrcString& str() const;
	PARLIAMENT_EXPORT RsrcString strNoCache() const;

private:
	KbInstance*				m_pKB;
	const char*const		m_pUri;
	mutable RsrcString	m_uri;
	mutable ResourceId	m_id;
};

class UriLib
{
public:
	explicit UriLib(KbInstance* pKB = nullptr);
	UriLib(const UriLib&) = default;
	UriLib& operator=(const UriLib&) = default;
	UriLib(UriLib&&) = default;
	UriLib& operator=(UriLib&&) = default;
	~UriLib() = default;

	const LazyRsrc m_rdfType;
	const LazyRsrc m_rdfsSubClassOf;
	const LazyRsrc m_rdfsSubPropertyOf;
	const LazyRsrc m_rdfsClass;
	const LazyRsrc m_rdfsResource;
	const LazyRsrc m_rdfsDomain;
	const LazyRsrc m_rdfsRange;
	const LazyRsrc m_rdfsDatatype;
	const LazyRsrc m_owlClass;
	const LazyRsrc m_owlThing;
	const LazyRsrc m_owlInverseOf;
	const LazyRsrc m_owlObjectProp;
	const LazyRsrc m_owlDatatypeProp;
	const LazyRsrc m_owlSymmetricProp;
	const LazyRsrc m_owlFuncProp;
	const LazyRsrc m_owlInvFuncProp;
	const LazyRsrc m_owlTransitiveProp;
	const LazyRsrc m_owlEquivalentClass;
	const LazyRsrc m_owlEquivalentProp;
	const LazyRsrc m_owlSameAs;
	const LazyRsrc m_owlDifferentFrom;
	const LazyRsrc m_owlOnProp;
	const LazyRsrc m_owlHasValue;
	const LazyRsrc m_owlRestriction;
	const LazyRsrc m_ruleSubclass;
	const LazyRsrc m_ruleSubproperty;
	const LazyRsrc m_ruleDomain;
	const LazyRsrc m_ruleRange;
	const LazyRsrc m_ruleEquivalentClass;
	const LazyRsrc m_ruleEquivalentProp;
	const LazyRsrc m_ruleInverseProp;
	const LazyRsrc m_ruleSymmetricProp;
	const LazyRsrc m_ruleFuncProp;
	const LazyRsrc m_ruleInvFuncProp;
	const LazyRsrc m_ruleTransitiveProp;
	const LazyRsrc m_ruleSWRLTrigger;
	const LazyRsrc m_statementHasName;
	const LazyRsrc m_rdfSubject;
	const LazyRsrc m_rdfPredicate;
	const LazyRsrc m_rdfObject;
	const LazyRsrc m_rdfStatement;

	// SWRL-related
	const LazyRsrc m_swrlImp;
	const LazyRsrc m_swrlBody;
	const LazyRsrc m_swrlHead;
	const LazyRsrc m_rdfListFirst;
	const LazyRsrc m_rdfListRest;
	const LazyRsrc m_rdfListNil;
	const LazyRsrc m_swrlpropertyPred;
	const LazyRsrc m_swrlArgument1;
	const LazyRsrc m_swrlArgument2;
	const LazyRsrc m_swrlVariable;

	// SWRL predicates
	const LazyRsrc m_swrlClassPredicate;
	const LazyRsrc m_swrlDataRange;
	const LazyRsrc m_swrlBuiltin;
	const LazyRsrc m_swrlArguments;

	// SWRL Atom Types
	const LazyRsrc m_swrlBuiltinAtom;
	const LazyRsrc m_swrlDifferentIndividualsAtom;
	const LazyRsrc m_swrlSameIndividualAtom;
	const LazyRsrc m_swrlDatavaluedPropertyAtom;
	const LazyRsrc m_swrlIndividualPropertyAtom;
	const LazyRsrc m_swrlClassAtom;
	const LazyRsrc m_swrlDataRangeAtom;

	// For SWRL Builtins
	const LazyRsrc m_swrlbEqual;
	const LazyRsrc m_swrlbNotEqual;
	const LazyRsrc m_swrlbLessThan;
	const LazyRsrc m_swrlbLessThanOrEqual;
	const LazyRsrc m_swrlbGreaterThan;
	const LazyRsrc m_swrlbGreaterThanOrEqual;
	const LazyRsrc m_swrlbAdd;
	const LazyRsrc m_swrlbSubtract;
	const LazyRsrc m_swrlbMultiply;
	const LazyRsrc m_swrlbDivide;
	const LazyRsrc m_swrlbIntegerDivide;
	const LazyRsrc m_swrlbMod;
	const LazyRsrc m_swrlbPow;
	const LazyRsrc m_swrlbUnaryPlus;
	const LazyRsrc m_swrlbUnaryMinus;
	const LazyRsrc m_swrlbAbs;
	const LazyRsrc m_swrlbCeiling;
	const LazyRsrc m_swrlbFloor;
	const LazyRsrc m_swrlbRound;
	const LazyRsrc m_swrlbRoundHalfToEven;
	const LazyRsrc m_swrlbSin;
	const LazyRsrc m_swrlbCos;
	const LazyRsrc m_swrlbTan;
	const LazyRsrc m_swrlbBooleanNot;
	const LazyRsrc m_swrlbStringEqualIgnoreCase;
	const LazyRsrc m_swrlbStringConcat;
	const LazyRsrc m_swrlbSubstring;
	const LazyRsrc m_swrlbStringLength;
	const LazyRsrc m_swrlbNormalizeSpace;
	const LazyRsrc m_swrlbUpperCase;
	const LazyRsrc m_swrlbLowerCase;
	const LazyRsrc m_swrlbTranslate;
	const LazyRsrc m_swrlbContains;
	const LazyRsrc m_swrlbContainsIgnoreCase;
	const LazyRsrc m_swrlbStartsWith;
	const LazyRsrc m_swrlbEndsWith;
	const LazyRsrc m_swrlbSubstringBefore;
	const LazyRsrc m_swrlbSubstringAfter;
	const LazyRsrc m_swrlbMatches;
	const LazyRsrc m_swrlbReplace;
	const LazyRsrc m_swrlbTokenize;
	const LazyRsrc m_swrlbYearMonthDuration;
	const LazyRsrc m_swrlbDayTimeDuration;
	const LazyRsrc m_swrlbDateTime;
	const LazyRsrc m_swrlbDate;
	const LazyRsrc m_swrlbTime;
	const LazyRsrc m_swrlbAddYearMonthDurations;
	const LazyRsrc m_swrlbSubtractYearMonthDurations;
	const LazyRsrc m_swrlbMultiplyYearMonthDuration;
	const LazyRsrc m_swrlbDivideYearMonthDurations;
	const LazyRsrc m_swrlbAddDayTimeDurations;
	const LazyRsrc m_swrlbSubtractDayTimeDurations;
	const LazyRsrc m_swrlbMultiplyDayTimeDurations;
	const LazyRsrc m_swrlbDivideDayTimeDuration;
	const LazyRsrc m_swrlbSubtractDates;
	const LazyRsrc m_swrlbSubtractTimes;
	const LazyRsrc m_swrlbAddYearMonthDurationToDateTime;
	const LazyRsrc m_swrlbAddDayTimeDurationToDateTime;
	const LazyRsrc m_swrlbSubtractYearMonthDurationFromDateTime;
	const LazyRsrc m_swrlbSubtractDayTimeDurationFromDateTime;
	const LazyRsrc m_swrlbAddYearMonthDurationToDate;
	const LazyRsrc m_swrlbAddDayTimeDurationToDate;
	const LazyRsrc m_swrlbSubtractYearMonthDurationFromDate;
	const LazyRsrc m_swrlbSubtractDayTimeDurationFromDate;
	const LazyRsrc m_swrlbAddDayTimeDurationToTime;
	const LazyRsrc m_swrlbSubtractDayTimeDurationFromTime;
	const LazyRsrc m_swrlbSubtractDateTimesYieldingYearMonthDuration;
	const LazyRsrc m_swrlbSubtractDateTimesYieldingDayTimeDuration;
	const LazyRsrc m_swrlbResolveURI;
	const LazyRsrc m_swrlbAnyURI;
	const LazyRsrc m_swrlbListConcat;
	const LazyRsrc m_swrlbListIntersection;
	const LazyRsrc m_swrlbListSubtraction;
	const LazyRsrc m_swrlbMember;
	const LazyRsrc m_swrlbLength;
	const LazyRsrc m_swrlbFirst;
	const LazyRsrc m_swrlbRest;
	const LazyRsrc m_swrlbSublist;
	const LazyRsrc m_swrlbEmpty;

	// Literal datatypes
	const LazyRsrc m_ptIntervalLiteral;
	const LazyRsrc m_gisGMLLiteral;
	const LazyRsrc m_gisWKTLiteral;
	const LazyRsrc m_rdfHTML;
	const LazyRsrc m_rdfLangString;
	const LazyRsrc m_rdfPlainLiteral;
	const LazyRsrc m_rdfXMLLiteral;
	const LazyRsrc m_xsdAnyURI;
	const LazyRsrc m_xsdBase64Binary;
	const LazyRsrc m_xsdBoolean;
	const LazyRsrc m_xsdByte;
	const LazyRsrc m_xsdDate;
	const LazyRsrc m_xsdDateTime;
	const LazyRsrc m_xsdDateTimeStamp;
	const LazyRsrc m_xsdDayTimeDuration;
	const LazyRsrc m_xsdDecimal;
	const LazyRsrc m_xsdDouble;
	const LazyRsrc m_xsdDuration;
	const LazyRsrc m_xsdFloat;
	const LazyRsrc m_xsdGDay;
	const LazyRsrc m_xsdGMonth;
	const LazyRsrc m_xsdGMonthDay;
	const LazyRsrc m_xsdGYear;
	const LazyRsrc m_xsdGYearMonth;
	const LazyRsrc m_xsdHexBinary;
	const LazyRsrc m_xsdInt;
	const LazyRsrc m_xsdInteger;
	const LazyRsrc m_xsdLanguage;
	const LazyRsrc m_xsdLong;
	const LazyRsrc m_xsdName;
	const LazyRsrc m_xsdNCName;
	const LazyRsrc m_xsdNegativeInteger;
	const LazyRsrc m_xsdNMTOKEN;
	const LazyRsrc m_xsdNonNegativeInteger;
	const LazyRsrc m_xsdNonPositiveInteger;
	const LazyRsrc m_xsdNormalizedString;
	const LazyRsrc m_xsdPositiveInteger;
	const LazyRsrc m_xsdShort;
	const LazyRsrc m_xsdString;
	const LazyRsrc m_xsdTime;
	const LazyRsrc m_xsdToken;
	const LazyRsrc m_xsdUnsignedByte;
	const LazyRsrc m_xsdUnsignedInt;
	const LazyRsrc m_xsdUnsignedLong;
	const LazyRsrc m_xsdUnsignedShort;
	const LazyRsrc m_xsdYearMonthDuration;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_URILIB_H_INCLUDED
