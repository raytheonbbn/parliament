// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_TYPES_H_INCLUDED)
#define PARLIAMENT_TYPES_H_INCLUDED

#include "parliament/Platform.h"

#include <cstddef>
#include <cstdint>
#include <string>
#include <limits>
#include <type_traits>

PARLIAMENT_NAMESPACE_BEGIN

// ===========================================================================
// Platform-invariant integral types
// ===========================================================================

using int8		= ::std::int8_t;
using uint8		= ::std::uint8_t;
using int16		= ::std::int16_t;
using uint16	= ::std::uint16_t;
using int32		= ::std::int32_t;
using uint32	= ::std::uint32_t;
using int64		= ::std::int64_t;
using uint64	= ::std::uint64_t;
using intPtr	= ::std::intptr_t;
using uintPtr	= ::std::uintptr_t;



// ===========================================================================
// Unicode character and string types
//
// These were necessary because (a) C++ defined only two character types
// (char and wchar_t), and (b) the standard does not guarantee that the
// size of wchar_t matches with any particular UTF encoding.
// (Note that we use char and ::std::string for UTF-8.)
// ===========================================================================

//TODO: Once the std lib implements streams for char16_t and char32_t, change these to
// these types.  Also requires many casts for the unsigned-to-signed switch.
using Utf16Char = uint16;
using Utf32Char = uint32;

using Utf16String = ::std::basic_string<Utf16Char>;
using Utf32String = ::std::basic_string<Utf32Char>;



// ===========================================================================
// Types to encapsulate character encoding for OS routines
// ===========================================================================

#if defined(PARLIAMENT_WINDOWS) && defined(UNICODE)
using TChar		= wchar_t;
using TString	= ::std::wstring;
#else
using TChar		= char;
using TString	= ::std::string;
#endif



// ===========================================================================
// Types to encapsulate character encoding for resources
// ===========================================================================

#if defined(PARLIAMENT_RSRC_AS_UTF16)
using RsrcChar		= Utf16Char;
using RsrcString	= Utf16String;
#else
using RsrcChar		= char;
using RsrcString	= ::std::string;
#endif



// ===========================================================================
// Parliament-specific types
// ===========================================================================

using Flags			= uint32;
using StatementId	= ::std::size_t;
using ResourceId	= ::std::size_t;

// A sentinal representing a null statement identifier, used to
// initialize a StatementId or returned as value in the event
// of an error.
constexpr StatementId k_nullStmtId = ::std::numeric_limits<StatementId>::max();

// A sentinal representing a null resource identifier, used to
// initialize a ResourceId or returned as value in the event
// of an error.
constexpr ResourceId k_nullRsrcId = ::std::numeric_limits<ResourceId>::max();

enum class StatementFlags
{
	k_stmtFlagValid,
	k_stmtFlagDeleted,
	k_stmtFlagReserved0,	// Was the literal flag
	k_stmtFlagInferred,
	k_stmtFlagHidden
};

enum class ResourceFlags
{
	k_rsrcFlagValid,
	k_rsrcFlagReserved0,	// Was the deleted flag
	k_rsrcFlagLiteral,
	k_rsrcFlagAnonymous,
	k_rsrcFlagStatementTag
};

enum class KbDisposition
{
	k_indeterminateKbState,
	k_kbDoesNotExist,
	k_kbExistsWithoutUriToInt,
	k_kbExists
};

enum class StmtIteratorFlags
{
	k_skipNone				= 0x00,
	k_skipDeleted			= 0x01,
	k_skipInferred			= 0x02,
	k_skipLiteral			= 0x04,
	k_skipNonLiteral		= 0x08,
	k_skipVirtual			= 0x10,
	k_showHidden			= 0x20,
};

// Used to either include or exclude deleted statements from an operation:
enum class DeletedStmtsAction { include, exclude };

// Used to either include or exclude inferred statements from an operation:
enum class InferredStmtsAction { include, exclude };

// Selects the character set used in N-Triples exports:
enum class EncodingCharSet { ascii, utf8 };



// ===========================================================================
// Compile-time tests
// ===========================================================================

// Bit-ness macro:
#if defined(PARLIAMENT_64BITS)
static_assert(sizeof(void*) == 8u, "Unexpected size for void*");
#elif defined(PARLIAMENT_32BITS)
static_assert(sizeof(void*) == 4u, "Unexpected size for void*");
#else
static_assert(false, "No bit-ness macro is defined");
#endif

// Integer types:
static_assert(sizeof(int8) == 1u, "Unexpected size for type alias int8");
static_assert(sizeof(uint8) == 1u, "Unexpected size for type alias uint8");
static_assert(sizeof(int16) == 2u, "Unexpected size for type alias int16");
static_assert(sizeof(uint16) == 2u, "Unexpected size for type alias uint16");
static_assert(sizeof(int32) == 4u, "Unexpected size for type alias int32");
static_assert(sizeof(uint32) == 4u, "Unexpected size for type alias uint32");
static_assert(sizeof(int64) == 8u, "Unexpected size for type alias int64");
static_assert(sizeof(uint64) == 8u, "Unexpected size for type alias uint64");
static_assert(sizeof(intPtr) == sizeof(void*), "Unexpected size for type alias intPtr");
static_assert(sizeof(uintPtr) == sizeof(void*), "Unexpected size for type alias uintPtr");

// Character types:
#if defined(PARLIAMENT_WINDOWS) && defined(UNICODE)
static_assert(sizeof(TChar) == 2u, "Unexpected size for type alias TChar");
static_assert(sizeof(TString::value_type) == 2u, "Unexpected size for type alias TString::value_type");
#else
static_assert(sizeof(TChar) == 1u, "Unexpected size for type alias TChar");
static_assert(sizeof(TString::value_type) == 1u, "Unexpected size for type alias TString::value_type");
#endif

#if defined(PARLIAMENT_RSRC_AS_UTF16)
static_assert(sizeof(RsrcChar) == 2u, "Unexpected size for type alias RsrcChar");
static_assert(sizeof(RsrcString::value_type) == 2u, "Unexpected size for type alias RsrcString::value_type");
#else
static_assert(sizeof(RsrcChar) == 1u, "Unexpected size for type alias RsrcChar");
static_assert(sizeof(RsrcString::value_type) == 1u, "Unexpected size for type alias RsrcString::value_type");
#endif

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_TYPES_H_INCLUDED
