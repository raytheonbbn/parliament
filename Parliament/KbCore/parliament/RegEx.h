// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_REGEX_H_INCLUDED)
#define PARLIAMENT_REGEX_H_INCLUDED

#include "parliament/Platform.h"

// GCC first supported std::regex in version 4.9.0:
#if defined(PARLIAMENT_GCC) && (__GNUC__ < 4 || (__GNUC__ == 4 && __GNUC_MINOR__ < 9))
#	define USE_XPRESSIVE
//#else
//#	define USE_BOOST_REGEX
#endif

#if defined(USE_XPRESSIVE)
//#	pragma message("Using Boost.Xpressive")
#	include <boost/xpressive/xpressive_dynamic.hpp>
#elif defined(USE_BOOST_REGEX)
//#	pragma message("Using Boost.Regex")
#	include <boost/regex.hpp>
#else
//#	pragma message("Using std regex")
#	include <regex>
#endif

#include <string>

PARLIAMENT_NAMESPACE_BEGIN

#if defined(USE_XPRESSIVE)
using RegEx = ::boost::xpressive::sregex;
using SMatch = ::boost::xpressive::smatch;
#elif defined(USE_BOOST_REGEX)
using RegEx = ::boost::regex;
using SMatch = ::boost::smatch;
#else
using RegEx = ::std::regex;
using SMatch = ::std::smatch;
#endif

inline RegEx compileRegEx(const char* pRegExStr)
{
#if defined(USE_XPRESSIVE)
	return RegEx::compile(pRegExStr);
#else
	return RegEx(pRegExStr);
#endif
}

inline RegEx compileRegEx(const ::std::string& regExStr)
{
#if defined(USE_XPRESSIVE)
	return RegEx::compile(regExStr);
#else
	return RegEx(regExStr);
#endif
}

inline bool regExMatch(const ::std::string& str, SMatch& m, const RegEx& rex)
{
	return regex_match(str, m, rex);
}

/*
	Example usage:

	auto rex = compileRegEx(k_optionRegExStr);
	SMatch captures;
	if (regExMatch(optionStr, captures, rex))
	{
	}
*/

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_REGEX_H_INCLUDED
