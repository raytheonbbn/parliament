// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/LiteralUtils.h"
#include "parliament/Log.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/UriLib.h"
#include "parliament/Util.h"

#include <algorithm>
#include <boost/algorithm/string/case_conv.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/thread/once.hpp>
#include <iterator>

namespace pmnt = ::bbn::parliament;

using ::boost::bad_lexical_cast;
using ::boost::format;
using ::boost::lexical_cast;
using ::boost::numeric_cast;
using ::std::advance;
using ::std::any_of;
using ::std::begin;
using ::std::char_traits;
using ::std::end;
using ::std::find_end;
using ::std::make_tuple;
using ::std::map;
using ::std::search;
using ::std::transform;
using ::std::tuple;

static ::boost::once_flag g_onceInitFlag = BOOST_ONCE_INIT;

static const auto k_quote = pmnt::convertToRsrcChar("\"");
static const auto k_typeSep = pmnt::convertToRsrcChar("\"^^");
static const auto k_langSep = pmnt::convertToRsrcChar("\"@");

const pmnt::RsrcString pmnt::LiteralUtils::k_plainLiteralDatatype = pmnt::UriLib{}.m_xsdString.strNoCache();
const pmnt::RsrcString pmnt::LiteralUtils::k_langLiteralDatatype = pmnt::UriLib{}.m_rdfLangString.strNoCache();

pmnt::LiteralUtils::UriToEnumMap pmnt::LiteralUtils::g_uriToEnumMap;
const bool pmnt::LiteralUtils::k_ensureMapInit = pmnt::LiteralUtils::initUriToEnumMap();

static auto g_log = pmnt::log::getSource("LiteralUtils");

bool pmnt::LiteralUtils::initUriToEnumMap()
{
	call_once(g_onceInitFlag, &unsynchronizedInitUriToEnumMap);
	return true;
}

void pmnt::LiteralUtils::unsynchronizedInitUriToEnumMap()
{
	UriLib ul;

	auto notConvertible = [](const RsrcString& lexicalForm) -> double
		{ throw bad_lexical_cast(); };
	auto int64Converter = [](const RsrcString& lexicalForm)
		{ return numeric_cast<double>(lexical_cast<int64>(convertFromRsrcChar(lexicalForm))); };
	auto uint64Converter = [](const RsrcString& lexicalForm)
		{ return numeric_cast<double>(lexical_cast<uint64>(convertFromRsrcChar(lexicalForm))); };

	g_uriToEnumMap[ul.m_ptIntervalLiteral.strNoCache()] = notConvertible;

	g_uriToEnumMap[ul.m_gisGMLLiteral.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_gisWKTLiteral.strNoCache()] = notConvertible;

	g_uriToEnumMap[ul.m_rdfHTML.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_rdfLangString.strNoCache()] = defaultConversion;
	g_uriToEnumMap[ul.m_rdfPlainLiteral.strNoCache()] = defaultConversion;
	g_uriToEnumMap[ul.m_rdfXMLLiteral.strNoCache()] = notConvertible;

	g_uriToEnumMap[ul.m_xsdAnyURI.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdBase64Binary.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdBoolean.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdByte.strNoCache()] = [](const RsrcString& lexicalForm)
		{ return numeric_cast<double>(lexical_cast<int8>(convertFromRsrcChar(lexicalForm))); };
	g_uriToEnumMap[ul.m_xsdDate.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdDateTime.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdDateTimeStamp.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdDayTimeDuration.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdDecimal.strNoCache()] = defaultConversion;
	g_uriToEnumMap[ul.m_xsdDouble.strNoCache()] = defaultConversion;
	g_uriToEnumMap[ul.m_xsdDuration.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdFloat.strNoCache()] = [](const RsrcString& lexicalForm)
		{ return numeric_cast<double>(lexical_cast<float>(convertFromRsrcChar(lexicalForm))); };
	g_uriToEnumMap[ul.m_xsdGDay.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdGMonth.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdGMonthDay.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdGYear.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdGYearMonth.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdHexBinary.strNoCache()] = notConvertible;	// Questionable
	g_uriToEnumMap[ul.m_xsdInt.strNoCache()] = [](const RsrcString& lexicalForm)
		{ return numeric_cast<double>(lexical_cast<int32>(convertFromRsrcChar(lexicalForm))); };
	g_uriToEnumMap[ul.m_xsdInteger.strNoCache()] = int64Converter;
	g_uriToEnumMap[ul.m_xsdLanguage.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdLong.strNoCache()] = int64Converter;
	g_uriToEnumMap[ul.m_xsdName.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdNCName.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdNegativeInteger.strNoCache()] = int64Converter;
	g_uriToEnumMap[ul.m_xsdNMTOKEN.strNoCache()] = defaultConversion;
	g_uriToEnumMap[ul.m_xsdNonNegativeInteger.strNoCache()] = uint64Converter;
	g_uriToEnumMap[ul.m_xsdNonPositiveInteger.strNoCache()] = int64Converter;
	g_uriToEnumMap[ul.m_xsdNormalizedString.strNoCache()] = defaultConversion;
	g_uriToEnumMap[ul.m_xsdPositiveInteger.strNoCache()] = uint64Converter;
	g_uriToEnumMap[ul.m_xsdShort.strNoCache()] = [](const RsrcString& lexicalForm)
		{ return numeric_cast<double>(lexical_cast<int16>(convertFromRsrcChar(lexicalForm))); };
	g_uriToEnumMap[ul.m_xsdString.strNoCache()] = defaultConversion;
	g_uriToEnumMap[ul.m_xsdTime.strNoCache()] = notConvertible;
	g_uriToEnumMap[ul.m_xsdToken.strNoCache()] = defaultConversion;
	g_uriToEnumMap[ul.m_xsdUnsignedByte.strNoCache()] = [](const RsrcString& lexicalForm)
		{ return numeric_cast<double>(lexical_cast<uint8>(convertFromRsrcChar(lexicalForm))); };
	g_uriToEnumMap[ul.m_xsdUnsignedInt.strNoCache()] = [](const RsrcString& lexicalForm)
		{ return numeric_cast<double>(lexical_cast<uint32>(convertFromRsrcChar(lexicalForm))); };
	g_uriToEnumMap[ul.m_xsdUnsignedLong.strNoCache()] = uint64Converter;
	g_uriToEnumMap[ul.m_xsdUnsignedShort.strNoCache()] = [](const RsrcString& lexicalForm)
		{ return numeric_cast<double>(lexical_cast<uint16>(convertFromRsrcChar(lexicalForm))); };
	g_uriToEnumMap[ul.m_xsdYearMonthDuration.strNoCache()] = notConvertible;
}

double pmnt::LiteralUtils::convertToDouble(const RsrcString& lexicalForm,
	const RsrcString& datatypeUri)
{
	auto it = g_uriToEnumMap.find(datatypeUri);
	return (it == end(g_uriToEnumMap))
		? defaultConversion(lexicalForm)
		: (it->second)(lexicalForm);
}

double pmnt::LiteralUtils::defaultConversion(const RsrcString& lexicalForm)
{
	return lexical_cast<double>(convertFromRsrcChar(lexicalForm));
}

template <typename ConstBidiIter>
pmnt::LiteralComponents pmnt::LiteralUtils::parseLiteralImpl(ConstBidiIter first, ConstBidiIter last)
{
	if (search(first, last, cBegin(k_quote), cEnd(k_quote)) != first)
	{
		throw Exception(format("Literal does not begin with a double quote: %1%")
			% convertFromRsrcChar(RsrcString(first, last)));
	}

	// Try for a plain literal:
	auto trailingQuoteIt = find_end(first, last, cBegin(k_quote), cEnd(k_quote));
	if (unsignedDist(first, last) >= (2 * k_quote.size())
		&& unsignedDist(trailingQuoteIt, last) == k_quote.size())
	{
		advance(first, k_quote.size());
		return make_tuple(RsrcString{first, trailingQuoteIt}, RsrcString{}, RsrcString{});
	}

	// Try for a typed literal:
	auto typeSepIt = find_end(first, last, cBegin(k_typeSep), cEnd(k_typeSep));
	if (unsignedDist(first, last) > (k_quote.size() + k_typeSep.size())
		&& unsignedDist(typeSepIt, last) > k_typeSep.size())
	{
		advance(first, k_quote.size());
		auto typeIt = typeSepIt;
		advance(typeIt, k_typeSep.size());
		return make_tuple(RsrcString{first, typeSepIt}, RsrcString{typeIt, last}, RsrcString{});
	}

	// Try for a language-tagged literal:
	auto langSepIt = find_end(first, last, cBegin(k_langSep), cEnd(k_langSep));
	if (unsignedDist(first, last) > (k_quote.size() + k_langSep.size())
		&& unsignedDist(langSepIt, last) > k_langSep.size())
	{
		auto langIt = langSepIt;
		advance(langIt, k_langSep.size());
		if (any_of(langIt, last, [](RsrcChar ch){
			return ch < 'a' && ch > 'z' && ch < 'A' && ch > 'Z' && ch != '-'; }))
		{
			throw Exception(format("Language tag contains disallowed characters: %1%")
				% convertFromRsrcChar(RsrcString(first, last)));
		}

		advance(first, k_quote.size());
		return make_tuple(RsrcString{first, langSepIt}, RsrcString{}, RsrcString{langIt, last});
	}

	throw Exception(format("Literal does not have the expected format: %1%")
		% convertFromRsrcChar(RsrcString(first, last)));
}

pmnt::LiteralComponents pmnt::LiteralUtils::parseLiteral(const RsrcString& literal)
{
	return parseLiteralImpl(cBegin(literal), cEnd(literal));
}

pmnt::LiteralComponents pmnt::LiteralUtils::parseLiteral(const RsrcChar* pLiteral)
{
	return parseLiteralImpl(pLiteral, pLiteral + char_traits<RsrcChar>::length(pLiteral));
}

pmnt::LiteralComponents pmnt::LiteralUtils::parseLiteral(const RsrcChar* pBegin, const RsrcChar* pEnd)
{
	return parseLiteralImpl(pBegin, pEnd);
}

pmnt::RsrcString pmnt::LiteralUtils::composePlainLiteral(const RsrcString& lexicalForm)
{
	return k_quote + lexicalForm + k_quote;
}

pmnt::RsrcString pmnt::LiteralUtils::composeTypedLiteral(const RsrcString& lexicalForm, const RsrcString& typeIRI)
{
	return k_quote + lexicalForm + k_typeSep + typeIRI;
}

pmnt::RsrcString pmnt::LiteralUtils::composeLangLiteral(const RsrcString& lexicalForm, RsrcString langTag)
{
	// This is a bit of a hack, but it works for both UTF-8 and UTF-16
	// because the characters for language tags are restricted:
	PMNT_LOG(g_log, log::Level::debug) << format{"composeLangLiteral(): Lang tag before translation: ''%1%''"}
		% convertFromRsrcChar(langTag);
	transform(begin(langTag), end(langTag), begin(langTag),
		[](RsrcChar ch){ return (ch >= 'A' && ch <= 'Z') ? 'a' + (ch - 'A') : ch; });
	PMNT_LOG(g_log, log::Level::debug) << format{"composeLangLiteral(): Lang tag after translation:  ''%1%''"}
		% convertFromRsrcChar(langTag);
	return k_quote + lexicalForm + k_langSep + langTag;
}
