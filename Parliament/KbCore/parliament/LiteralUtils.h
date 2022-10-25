// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2017, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_LITERALUTILS_H_INCLUDED)
#define PARLIAMENT_LITERALUTILS_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <functional>
#include <map>
#include <tuple>

namespace bbn::parliament
{

// Contains lexical form, datatype IRI, and language.  For plain literals,
// both datatype and language will be empty.  For language strings, the
// datatype will be empty.  For typed literals, language will be empty.
using LiteralComponents = ::std::tuple<RsrcString, RsrcString, RsrcString>;

class LiteralUtils
{
public:
	LiteralUtils() = delete;
	LiteralUtils(const LiteralUtils&) = delete;
	LiteralUtils& operator=(const LiteralUtils&) = delete;
	LiteralUtils(LiteralUtils&&) = delete;
	LiteralUtils& operator=(LiteralUtils&&) = delete;
	~LiteralUtils() = delete;

	static bool isKnownRdfDatatype(const RsrcString& datatypeUri)
		{ return g_uriToEnumMap.find(datatypeUri) != end(g_uriToEnumMap); }

	// may throw ::boost::bad_lexical_cast
	static double convertToDouble(const RsrcString& lexicalForm, const RsrcString& datatypeUri);

	static LiteralComponents parseLiteral(const RsrcString& literal);
	static LiteralComponents parseLiteral(const RsrcChar* pLiteral);
	static LiteralComponents parseLiteral(const RsrcChar* pBegin, const RsrcChar* pEnd);

	static RsrcString composePlainLiteral(const RsrcString& lexicalForm);
	static RsrcString composeTypedLiteral(const RsrcString& lexicalForm, const RsrcString& typeIRI);
	static RsrcString composeLangLiteral(const RsrcString& lexicalForm, RsrcString langTag);

	static const RsrcString k_plainLiteralDatatype;
	static const RsrcString k_langLiteralDatatype;

private:
	using DoubleConverter = ::std::function<double(const RsrcString&)>;
	using UriToEnumMap = ::std::map<RsrcString, DoubleConverter>;

	static bool initUriToEnumMap();
	static void unsynchronizedInitUriToEnumMap();
	static double defaultConversion(const RsrcString& lexicalForm);

	// ConstBidiIter is assumed to dereference to RsrcChar:
	template <typename ConstBidiIter>
	static LiteralComponents parseLiteralImpl(ConstBidiIter first, ConstBidiIter last);

	static UriToEnumMap g_uriToEnumMap;
	static const bool k_ensureMapInit;
};

}	// namespace end

#endif
