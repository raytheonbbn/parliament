// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_UTIL_H_INCLUDED)
#define PARLIAMENT_UTIL_H_INCLUDED

#include "parliament/Types.h"

#include <boost/filesystem/path.hpp>
#include <iterator>
#include <memory>
#include <string>
#include <string_view>
#include <type_traits>

#if defined(PARLIAMENT_SOLARIS)
#	include <sys/time.h>
#endif

PARLIAMENT_NAMESPACE_BEGIN

PARLIAMENT_EXPORT ::std::string getKbVersion();	// Parliament version number
PARLIAMENT_EXPORT TString tGetEnvVar(const TChar* pVarName);
PARLIAMENT_EXPORT ::boost::filesystem::path getCurrentDllFilePath();



// ===========================================================================
// Boost-related conveniences:
// ===========================================================================

class StringJoinOp
{
public:
	StringJoinOp() : m_separator() {}
	StringJoinOp(::std::string_view sep) : m_separator(sep) {}

	::std::string operator()(::std::string lhs, ::std::string_view rhs) const
	{
		if (!lhs.empty())
		{
			lhs += m_separator;
		}
		return lhs += rhs;
	}

private:
	::std::string m_separator;
};



// ===========================================================================
// Unsigned iterator distance:  Use this to compute the distance between two
// iterators as an unsigned number.  Useful in cases where you are sure that
// first precedes last, so that the distance is positive, and where you need
// to compare the distance to an unsigned number, like the size of a container.
// ===========================================================================

template <typename InputIter>
inline ::std::size_t unsignedDist(InputIter first, InputIter last)
{
	using DiffType = typename ::std::iterator_traits<InputIter>::difference_type;
	using UnsignedDiffType = typename ::std::make_unsigned<DiffType>::type;
	return static_cast<UnsignedDiffType>(::std::distance(first, last));
}



// ===========================================================================
// Utilities for manipulating scoped enums as bit-flags
// ===========================================================================

template<typename EnumT>
inline EnumT& operator|=(EnumT& lhs, EnumT rhs) noexcept
{
	using ULT = typename ::std::underlying_type<EnumT>::type;
	lhs = static_cast<EnumT>(static_cast<ULT>(lhs) | static_cast<ULT>(rhs));
	return lhs;
}

template<typename EnumT>
inline EnumT& operator&=(EnumT& lhs, EnumT rhs) noexcept
{
	using ULT = typename ::std::underlying_type<EnumT>::type;
	lhs = static_cast<EnumT>(static_cast<ULT>(lhs) & static_cast<ULT>(rhs));
	return lhs;
}

template<typename EnumT>
constexpr inline EnumT operator|(EnumT lhs, EnumT rhs) noexcept
{
	using ULT = typename ::std::underlying_type<EnumT>::type;
	return static_cast<EnumT>(static_cast<ULT>(lhs) | static_cast<ULT>(rhs));
}

template<typename EnumT>
constexpr inline EnumT operator&(EnumT lhs, EnumT rhs) noexcept
{
	using ULT = typename ::std::underlying_type<EnumT>::type;
	return static_cast<EnumT>(static_cast<ULT>(lhs) & static_cast<ULT>(rhs));
}

template<typename EnumT>
constexpr inline int asBitMask(EnumT flag) noexcept
{
	using ULT = typename ::std::underlying_type<EnumT>::type;
	return 1 << static_cast<ULT>(flag);
}

template<typename EnumT>
constexpr inline bool isZero(EnumT flag) noexcept
{
	using ULT = typename ::std::underlying_type<EnumT>::type;
	return static_cast<ULT>(flag) == 0;
}



// ===========================================================================
// High-resolution timer class, for performance measurements
// ===========================================================================

//TODO: Replace this with Boost.Chrono
class HiResTimer
{
public:
	PARLIAMENT_EXPORT HiResTimer()
		: m_start(getHiResTime()), m_stop(0) {}

	PARLIAMENT_EXPORT void stop()
		{ m_stop = getHiResTime(); }

	PARLIAMENT_EXPORT double getMicroSec() const
		{ return getSec() * 1000.0 * 1000.0; }
	PARLIAMENT_EXPORT double getMilliSec() const
		{ return getSec() * 1000.0; }
	PARLIAMENT_EXPORT double getSec() const
		{ return static_cast<double>(m_stop - m_start) / static_cast<double>(getUnitsPerSec()); }

public:	// But conceptually private -- should be used only by HiResTimer JNI code
#if defined(PARLIAMENT_SOLARIS)
	using HiResTime = hrtime_t;
#else
	using HiResTime = uint64;
#endif

	PARLIAMENT_EXPORT static HiResTime getHiResTime();
	PARLIAMENT_EXPORT static uint64 getUnitsPerSec();

private:
	HiResTime	m_start;
	HiResTime	m_stop;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_UTIL_H_INCLUDED
