// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_UNICODEITERATOR_H_INCLUDED)
#define PARLIAMENT_UNICODEITERATOR_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"
#include "parliament/Exceptions.h"

#include <boost/filesystem/path.hpp>
#include <climits>
#include <iterator>
#include <memory>
#include <string>

PARLIAMENT_NAMESPACE_BEGIN

// Forward-declaration:
template<typename Char, typename FwdIter> class UnicodeIterator;



// Utility routine to tell whether a sequence of bytes is legal UTF-8.
// This routine assumes that the sequence starting at current contains
// at least charLen bytes.  If presented with a length > 4, this
// returns false, because the Unicode definition of UTF-8 only goes up
// to 4-byte sequences.
//
// FwdIter must be a forward iterator whose value type is char (usually either
// "const char*" or "::std::string::const_iterator").  This specialization of
// the policy template implements the UTF-8 iteration policy.
//
// IterClass must be the iterator class of FwdIter
template<typename FwdIter, typename IterClass>
struct Helper
{
	static bool isLegalUtf8(FwdIter current, size_t charLen)
	{
		using IterType = UnicodeIterator<char, FwdIter>;

		bool result = false;

		if (charLen == 1)
		{
			result = IterType::k_leadByteValidator[static_cast<uint8>(*current)];
		}
		else
		{
			const bool* p2ndByteValidator = IterType::get2ndByteValidator(*current);

			switch (charLen)
			{
			case 2:
				result = IterType::k_leadByteValidator[static_cast<uint8>(*current)]
					&& p2ndByteValidator[static_cast<uint8>(*++current)];
				break;

			case 3:
				result = IterType::k_leadByteValidator[static_cast<uint8>(*current)]
					&& p2ndByteValidator[static_cast<uint8>(*++current)]
					&& IterType::k_byteValidator80toBF[static_cast<uint8>(*++current)];
				break;

			case 4:
				result = IterType::k_leadByteValidator[static_cast<uint8>(*current)]
					&& p2ndByteValidator[static_cast<uint8>(*++current)]
					&& IterType::k_byteValidator80toBF[static_cast<uint8>(*++current)]
					&& IterType::k_byteValidator80toBF[static_cast<uint8>(*++current)];
				break;
			}
		}
		return result;
	}
};

// A specialization that takes advantage of the operator
// arithmetic available to random access iterators.
template<typename FwdIter>
struct Helper<FwdIter, ::std::random_access_iterator_tag>
{
	static bool isLegalUtf8(FwdIter current, size_t charLen)
	{
		using IterType = UnicodeIterator<char, FwdIter>;

		switch (charLen)
		{
			default: return false;

			// Everything else falls through if it's valid.
			case 4: if (!IterType::k_byteValidator80toBF[static_cast<uint8>(*(current + 3))]) { return false; }
			case 3: if (!IterType::k_byteValidator80toBF[static_cast<uint8>(*(current + 2))]) { return false; }
			case 2: if (!IterType::get2ndByteValidator(*current)[static_cast<uint8>(*(current + 1))]) { return false; }
			case 1: if (!IterType::k_leadByteValidator[static_cast<uint8>(*current)]) { return false; }
		}
		return true;
	}
};



template<typename Char, typename FwdIter>
class UnicodeIteratorPolicy
{
public:
	static void decodeBOM(UnicodeIterator<Char, FwdIter>& /* iter */)
		{ throw UnimplementedException("UnicodeIteratorPolicy::decodeBOM must be specialized"); }

	static void preIncrement(UnicodeIterator<Char, FwdIter>& /* iter */)
		{ throw UnimplementedException("UnicodeIteratorPolicy::preIncrement must be specialized"); }
};

// FwdIter must be a forward iterator whose value type is char (usually either
// "const char*" or "::std::string::const_iterator").  This specialization of
// the policy template implements the UTF-8 iteration policy.
template<typename FwdIter>
class UnicodeIteratorPolicy<char, FwdIter>
{
public:
	using IterType = UnicodeIterator<char, FwdIter>;

	static void decodeBOM(IterType& iter)
	{
		FwdIter beginOfInput = iter.m_current;

		// If 1st 3 bytes are the BOM, return 3, otherwise 0:
		iter.m_charLen = (iter.m_distanceToEnd >= 3
				&& static_cast<uint8>(*beginOfInput) == 0xefu
				&& static_cast<uint8>(*++beginOfInput) == 0xbbu
				&& static_cast<uint8>(*++beginOfInput) == 0xbfu)
			? 3 : 0;
		iter.m_isByteOrderSwitched = false;
	}

	static void preIncrement(IterType& iter)
	{
		::std::advance(iter.m_current, iter.m_charLen);
		iter.m_distanceToEnd -= iter.m_charLen;

		if (iter.m_current == iter.m_endOfInput)
		{
			iter.m_charLen = 0;
			iter.m_char = 0;
		}
		else
		{
			iter.m_char = IterType::k_replacementChar;
			iter.m_charLen = IterType::k_utf8CharLen[static_cast<uint8>(*iter.m_current)];
			if (iter.m_distanceToEnd < iter.m_charLen)
			{
				iter.postError("Premature end of UTF-8 stream");
			}
			else if (!Helper<FwdIter, typename ::std::iterator_traits<FwdIter>::iterator_category>
				::isLegalUtf8(iter.m_current, iter.m_charLen))	// Check this whether lenient or strict
			{
				iter.postError("Illegal character in UTF-8 stream");
			}
			else
			{
				Utf32Char ch = 0;
				FwdIter tmpCurrent = iter.m_current;
				switch (iter.m_charLen)	// The cases all fall through -- this is intentional
				{
					case 6: ch += static_cast<uint8>(*(tmpCurrent++)); ch <<= IterType::k_byteShift;
					case 5: ch += static_cast<uint8>(*(tmpCurrent++)); ch <<= IterType::k_byteShift;
					case 4: ch += static_cast<uint8>(*(tmpCurrent++)); ch <<= IterType::k_byteShift;
					case 3: ch += static_cast<uint8>(*(tmpCurrent++)); ch <<= IterType::k_byteShift;
					case 2: ch += static_cast<uint8>(*(tmpCurrent++)); ch <<= IterType::k_byteShift;
					case 1: ch += static_cast<uint8>(*tmpCurrent);
				}
				ch -= IterType::k_offsetsFromUtf8[iter.m_charLen - 1];

				if (ch > IterType::k_maxLegalUtf32)
				{
					iter.postError("Illegal character in UTF-8 stream (beyond plane 17, i.e., > 0x10ffff)");
					iter.m_char = IterType::k_replacementChar;
				}
				else if (ch >= IterType::k_srgtHiStart && ch <= IterType::k_srgtLoEnd)
				{
					iter.postError("Illegal character in UTF-8 stream (UTF-16 surrogate value)");
					iter.m_char = IterType::k_replacementChar;
				}
				else
				{
					iter.m_char = ch;
				}
			}
		}
	}
};

// FwdIter must be a forward iterator whose value type is Utf16Char (usually either
// "const Utf16Char*" or "Utf16String::const_iterator").  This specialization of
// the policy template implements the UTF-16 iteration policy.
template<typename FwdIter>
class UnicodeIteratorPolicy<Utf16Char, FwdIter>
{
public:
	using IterType = UnicodeIterator<Utf16Char, FwdIter>;

	static void decodeBOM(IterType& iter)
	{
		iter.m_charLen = 0;
		iter.m_isByteOrderSwitched = false;

		// If the first two bytes are the BOM, return 1, otherwise 0:
		if (iter.m_distanceToEnd >= 1)
		{
			Utf16Char bom = static_cast<Utf16Char>(*iter.m_current);
			iter.m_charLen = (bom == IterType::k_beBOM || bom == IterType::k_leBOM) ? 1 : 0;
			if (iter.m_charLen > 0)
			{
				bool isHostBigEndian = IterType::isHostBigEndian();
				iter.m_isByteOrderSwitched = (isHostBigEndian && (bom == IterType::k_beBOM))
					|| (!isHostBigEndian && (bom == IterType::k_leBOM));
			}

			// Note that in the default case (m_charLen == 0), the Unicode
			// standard says, "when there is no BOM, and in the absence of a
			// higher-level protocol, the byte order of the UTF-16 encoding
			// scheme is big-endian."  In our case, we will choose a
			// higher-level protocol such that no BOM means the byte order of
			// the current host.
		}
	}

	static void preIncrement(IterType& iter)
	{
		::std::advance(iter.m_current, iter.m_charLen);
		iter.m_distanceToEnd -= iter.m_charLen;

		if (iter.m_current == iter.m_endOfInput)
		{
			iter.m_charLen = 0;
			iter.m_char = 0;
		}
		else
		{
			iter.m_char = IterType::k_replacementChar;
			Utf32Char ch1 = iter.swapBytes(static_cast<Utf16Char>(*iter.m_current));
			iter.m_charLen = (IterType::k_srgtHiStart <= ch1 && ch1 <= IterType::k_srgtHiEnd) ? 2 : 1;
			if (iter.m_distanceToEnd < iter.m_charLen)
			{
				iter.postError("Premature end of UTF-16 stream");
			}
			else if (IterType::k_srgtLoStart <= ch1 && ch1 <= IterType::k_srgtLoEnd)
			{
				iter.postError("Illegal character in UTF-16 stream:  Low surrogate not preceded by a high surrogate");
			}
			else
			{
				bool isMalformedSurrogatePair = false;
				if (iter.m_charLen == 2)
				{
					FwdIter tmpCurrent = iter.m_current;
					Utf32Char ch2 = iter.swapBytes(static_cast<Utf16Char>(*(++tmpCurrent)));
					if (ch2 < IterType::k_srgtLoStart || ch2 > IterType::k_srgtLoEnd)
					{
						isMalformedSurrogatePair = true;
						iter.postError("Illegal character in UTF-16 stream:  High surrogate not followed by a low surrogate");
					}
					else
					{
						ch1 = ((ch1 - IterType::k_srgtHiStart) << IterType::k_halfShift)
							+ (ch2 - IterType::k_srgtLoStart) + IterType::k_halfBase;
					}
				}

				if (!isMalformedSurrogatePair)
				{
					if (ch1 > IterType::k_maxLegalUtf32)
					{
						iter.postError("Illegal character in UTF-16 stream (beyond plane 17, i.e., > 0x10ffff)");
					}
					else
					{
						iter.m_char = ch1;
					}
				}
			}
		}
	}
};

class UnicodeIteratorBase
{
public:
	using iterator_category = ::std::forward_iterator_tag;
	using value_type = Utf32Char;
	using difference_type = ptrdiff_t;
	using pointer = const value_type*;
	using const_reference = const value_type&;
	using reference = const_reference;

	static const Utf32Char k_replacementChar	= 0x0000fffd;
	static const Utf32Char k_maxLegalUtf32	= 0x10fffful;

	static const Utf32Char k_srgtHiStart	= 0xd800ul;	// low end of high-order surrogate range
	static const Utf32Char k_srgtHiEnd		= 0xdbfful;	// high end of high-order surrogate range
	static const Utf32Char k_srgtLoStart	= 0xdc00ul;	// low end of low-order surrogate range
	static const Utf32Char k_srgtLoEnd		= 0xdffful;	// high end of low-order surrogate range

	// === UTF-16 specific constants =====================================

	static const Utf16Char k_beBOM			= 0xfeffu;	// big-endian byte-order mark
	static const Utf16Char k_leBOM			= 0xfffeu;	// little-endian byte-order mark

	static const Utf32Char k_maxBmp			= 0xfffful;
	static const Utf32Char k_halfShift		= 10ul;		// used for shifting by 10 bits
	static const Utf32Char k_halfBase		= 0x10000ul;
	static const Utf32Char k_halfMask		= 0x3fful;

	// === UTF-8 specific constants =====================================

	static const Utf32Char k_byteMask		= 0xbful;
	static const Utf32Char k_byteMark		= 0x80ul;
	static const Utf32Char k_byteShift		= 6ul;		// used for shifting by 6 bits
	PARLIAMENT_EXPORT static const uint8 k_firstByteMark[];
	PARLIAMENT_EXPORT static const uint8 k_utf8CharLen[];
	PARLIAMENT_EXPORT static const Utf32Char k_offsetsFromUtf8[];

	// Note:  The static initializers for these validators were generated
	// by the auxilliary program Utf8StaticInitGen.cpp, which is included
	// with the Parliament code, but which is not built or executed by the
	// Parliament build infrastructure.
	PARLIAMENT_EXPORT static const bool k_leadByteValidator[];
	PARLIAMENT_EXPORT static const bool k_byteValidator80toBF[];
	PARLIAMENT_EXPORT static const bool k_byteValidatorA0toBF[];
	PARLIAMENT_EXPORT static const bool k_byteValidator80to9F[];
	PARLIAMENT_EXPORT static const bool k_byteValidator90toBF[];
	PARLIAMENT_EXPORT static const bool k_byteValidator80to8F[];
	PARLIAMENT_EXPORT static const bool k_byteValidator80toC2[];

	~UnicodeIteratorBase() = default;

	bool canThrow() const
		{ return m_conversionIsStrict; }

	bool errorOccurred() const
		{ return static_cast<bool>(m_pError); }

	const char* errorMsg() const
		{ return m_pError ? m_pError->what() : nullptr; }

	void clearError()
		{ m_pError.reset(); }

	const_reference operator*() const
		{ return m_char; }

	pointer operator->() const
		{ return &**this; }

	static const bool* get2ndByteValidator(uint8 byte1)
	{
		switch (byte1)
		{
			case 0xe0: return k_byteValidatorA0toBF;
			case 0xed: return k_byteValidator80to9F;
			case 0xf0: return k_byteValidator90toBF;
			case 0xf4: return k_byteValidator80to8F;
			default:   return k_byteValidator80toBF;
		}
	}

protected:
	static bool isHostBigEndian()
	{
		const Utf16Char k_testValue = 0x0102;
		return 0x01 == *static_cast<const uint8*>(
			static_cast<const void*>(&k_testValue));
	}

	Utf16Char swapBytes(Utf16Char x)
	{
		return m_isByteOrderSwitched
			? (x >> CHAR_BIT) | (x << CHAR_BIT)
			: x;
	}

	UnicodeIteratorBase(bool conversionIsStrict, size_t distanceToEnd) :
		m_pError(),
		m_distanceToEnd(distanceToEnd),
		m_char(0),
		m_charLen(0),
		m_conversionIsStrict(conversionIsStrict),
		m_isByteOrderSwitched(false)
	{}

	UnicodeIteratorBase(const UnicodeIteratorBase& rhs) :
		m_pError(rhs.m_pError
			? ::std::make_unique<UnicodeException>(*rhs.m_pError)
			: nullptr),
		m_distanceToEnd(rhs.m_distanceToEnd),
		m_char(rhs.m_char),
		m_charLen(rhs.m_charLen),
		m_conversionIsStrict(rhs.m_conversionIsStrict),
		m_isByteOrderSwitched(rhs.m_isByteOrderSwitched)
	{}

	UnicodeIteratorBase& operator=(const UnicodeIteratorBase& rhs)
	{
		m_pError = rhs.m_pError
			? ::std::make_unique<UnicodeException>(*rhs.m_pError)
			: nullptr;
		m_distanceToEnd = rhs.m_distanceToEnd;
		m_char = rhs.m_char;
		m_charLen = rhs.m_charLen;
		m_conversionIsStrict = rhs.m_conversionIsStrict;
		m_isByteOrderSwitched = rhs.m_isByteOrderSwitched;

		return *this;
	}

	UnicodeIteratorBase(UnicodeIteratorBase&& rhs) = default;

	UnicodeIteratorBase& operator=(UnicodeIteratorBase&& rhs) = default;

	PARLIAMENT_EXPORT void postError(const char* pMsg);

	::std::unique_ptr<UnicodeException>	m_pError;
	size_t											m_distanceToEnd;
	Utf32Char										m_char;
	uint8											m_charLen;
	bool											m_conversionIsStrict;
	bool											m_isByteOrderSwitched;
};

// Char is the input string's character type, which is intended
// to be either char or Utf16Char.  FwdIter must be a forward iterator
// whose value type is Char.  Usually, FwdIter is either
// "const Char*" or "::std::basic_string<Char>::const_iterator".
template<typename Char, typename FwdIter>
class UnicodeIterator : public UnicodeIteratorBase
{
public:
	static UnicodeIterator begin(const FwdIter& beginOfInput, const FwdIter& endOfInput)
		{ return UnicodeIterator(beginOfInput, endOfInput); }

	static UnicodeIterator begin(const FwdIter& beginOfInput, const FwdIter& endOfInput, const ::std::nothrow_t&)
		{ return UnicodeIterator(beginOfInput, endOfInput, ::std::nothrow); }

	static UnicodeIterator end(const FwdIter& endOfInput)
		{ return UnicodeIterator(endOfInput); }

	UnicodeIterator(const UnicodeIterator& rhs) :
		UnicodeIteratorBase(rhs),
		m_current(rhs.m_current),
		m_endOfInput(rhs.m_endOfInput)
	{}

	UnicodeIterator& operator=(const UnicodeIterator& rhs)
	{
		UnicodeIteratorBase::operator=(rhs);
		m_current = rhs.m_current;
		m_endOfInput = rhs.m_endOfInput;
		return *this;
	}

	UnicodeIterator& operator++()		// preincrement
	{
		UnicodeIteratorPolicy<Char, FwdIter>::preIncrement(*this);
		return *this;
	}

	UnicodeIterator operator++(int)	// postincrement
	{
		UnicodeIterator tmp = *this;
		++*this;
		return tmp;
	}

	bool operator==(const UnicodeIterator& rhs) const
		{ return m_current == rhs.m_current && m_endOfInput == rhs.m_endOfInput; }

	bool operator!=(const UnicodeIterator& rhs) const
		{ return !(*this == rhs); }

private:
	// Construct a begin iterator with strict Unicode correctness checking
	UnicodeIterator(const FwdIter& beginOfInput, const FwdIter& endOfInput) :
		UnicodeIteratorBase(true, ::std::distance(beginOfInput, endOfInput)),
		m_current(beginOfInput),
		m_endOfInput(endOfInput)
	{ decodeBOM(); ++*this; }

	// Construct a begin iterator with lenient Unicode correctness checking
	UnicodeIterator(const FwdIter& beginOfInput, const FwdIter& endOfInput, const ::std::nothrow_t&) :
		UnicodeIteratorBase(false, ::std::distance(beginOfInput, endOfInput)),
		m_current(beginOfInput),
		m_endOfInput(endOfInput)
	{ decodeBOM(); ++*this; }

	// Construct an end iterator
	UnicodeIterator(const FwdIter& endOfInput) :
		UnicodeIteratorBase(true, 0),
		m_current(endOfInput),
		m_endOfInput(endOfInput)
	{}


	void decodeBOM()
		{ UnicodeIteratorPolicy<Char, FwdIter>::decodeBOM(*this); }

	FwdIter	m_current;
	FwdIter	m_endOfInput;

	friend class UnicodeIteratorPolicy<Char, FwdIter>;
};

// Char is the input string's character type, which is intended
// to be either char or Utf16Char.
template<typename Char>
class UnicodeIteratorFactory
{
public:
	using TString = ::std::basic_string<Char>;
	using TStringIter = typename TString::const_iterator;

	static UnicodeIterator<Char, const Char*> begin(const Char* pStr)
	{
		size_t len = ::std::char_traits<Char>::length(pStr);
		return UnicodeIterator<Char, const Char*>::begin(pStr, pStr + len);
	}

	static UnicodeIterator<Char, const Char*> begin(const Char* pStr, const ::std::nothrow_t&)
	{
		size_t len = ::std::char_traits<Char>::length(pStr);
		return UnicodeIterator<Char, const Char*>::begin(pStr, pStr + len, ::std::nothrow);
	}

	static UnicodeIterator<Char, const Char*> end(const Char* pStr)
	{
		size_t len = ::std::char_traits<Char>::length(pStr);
		return UnicodeIterator<Char, const Char*>::end(pStr + len);
	}

	static UnicodeIterator<Char, TStringIter> begin(const TString& str)
		{ return UnicodeIterator<Char, TStringIter>::begin(::std::begin(str), ::std::end(str)); }

	static UnicodeIterator<Char, TStringIter> begin(const TString& str, const ::std::nothrow_t&)
		{ return UnicodeIterator<Char, TStringIter>::begin(::std::begin(str), ::std::end(str), ::std::nothrow); }

	static UnicodeIterator<Char, TStringIter> end(const TString& str)
		{ return UnicodeIterator<Char, TStringIter>::end(::std::end(str)); }
};



// ===========================================================================
// Conversion functions to go from UTF-32 to UTF-8 and UTF-16
// ===========================================================================

// FwdIter must be a forward iterator whose value type is Utf32Char, usually
// "const Utf32Char*".
template<typename FwdIter>
::std::string convertUtf32ToUtf8(FwdIter srcBegin, FwdIter srcEnd,
	bool conversionIsStrict = true)
{
	::std::string result;
	for (FwdIter src = srcBegin; src != srcEnd; ++src)
	{
		Utf32Char ch = *src;

		if ((UnicodeIteratorBase::k_srgtHiStart <= ch && ch <= UnicodeIteratorBase::k_srgtHiEnd)
			|| (UnicodeIteratorBase::k_srgtLoStart <= ch && ch <= UnicodeIteratorBase::k_srgtLoEnd))
		{
			if (conversionIsStrict)
			{
				throw UnicodeException("UTF-16 surrogate values are illegal in UTF-32");
			}
			else
			{
				ch = UnicodeIteratorBase::k_replacementChar;
			}
		}

		// Figure out how many bytes the result will require. Turn any
		// illegally large UTF-32 things (> Plane 17) into replacement chars.
		size_t bytesToWrite = 0;
		if (ch < 0x80u)
		{
			bytesToWrite = 1;
		}
		else if (ch < 0x800u)
		{
			bytesToWrite = 2;
		}
		else if (ch < 0x10000u)
		{
			bytesToWrite = 3;
		}
		else if (ch <= UnicodeIteratorBase::k_maxLegalUtf32)
		{
			bytesToWrite = 4;
		}
		else if (conversionIsStrict)
		{
			throw UnicodeException("UTF-32 values larger than 0x0010ffff are illegal");
		}
		else
		{
			bytesToWrite = 3;
			ch = UnicodeIteratorBase::k_replacementChar;
		}

		size_t i = result.length() + bytesToWrite;
		result.resize(i);
		switch (bytesToWrite) // The cases all fall through -- this is intentional
		{
			case 4:
				result[--i] = static_cast<uint8>((ch | UnicodeIteratorBase::k_byteMark) & UnicodeIteratorBase::k_byteMask);
				ch >>= UnicodeIteratorBase::k_byteShift;
			case 3:
				result[--i] = static_cast<uint8>((ch | UnicodeIteratorBase::k_byteMark) & UnicodeIteratorBase::k_byteMask);
				ch >>= UnicodeIteratorBase::k_byteShift;
			case 2:
				result[--i] = static_cast<uint8>((ch | UnicodeIteratorBase::k_byteMark) & UnicodeIteratorBase::k_byteMask);
				ch >>= UnicodeIteratorBase::k_byteShift;
			case 1:
				result[--i] = static_cast<uint8>(ch | UnicodeIteratorBase::k_firstByteMark[bytesToWrite]);
		}
	}
	return result;
}

// FwdIter must be a forward iterator whose value type is Utf32Char, usually
// "const Utf32Char*".
template<typename FwdIter>
Utf16String convertUtf32ToUtf16(FwdIter srcBegin, FwdIter srcEnd,
	bool conversionIsStrict = true)
{
	Utf16String result;
	for (FwdIter src = srcBegin; src != srcEnd; ++src)
	{
		Utf32Char ch = *src;
		if (ch <= UnicodeIteratorBase::k_maxBmp)
		{
			if ((UnicodeIteratorBase::k_srgtHiStart <= ch && ch <= UnicodeIteratorBase::k_srgtHiEnd)
				|| (UnicodeIteratorBase::k_srgtLoStart <= ch && ch <= UnicodeIteratorBase::k_srgtLoEnd))
			{
				if (conversionIsStrict)
				{
					throw UnicodeException("UTF-16 surrogate values are illegal in UTF-32");
				}
				else
				{
					result += static_cast<Utf16Char>(UnicodeIteratorBase::k_replacementChar);
				}
			}
			else
			{
				result += static_cast<Utf16Char>(ch); // the common case
			}
		}
		else if (ch > UnicodeIteratorBase::k_maxLegalUtf32)
		{
			if (conversionIsStrict)
			{
				throw UnicodeException("UTF-32 values larger than 0x0010ffff are illegal");
			}
			else
			{
				result += static_cast<Utf16Char>(UnicodeIteratorBase::k_replacementChar);
			}
		}
		else
		{
			ch -= UnicodeIteratorBase::k_halfBase;
			result += static_cast<Utf16Char>((ch >> UnicodeIteratorBase::k_halfShift) + UnicodeIteratorBase::k_srgtHiStart);
			result += static_cast<Utf16Char>((ch & UnicodeIteratorBase::k_halfMask) + UnicodeIteratorBase::k_srgtLoStart);
		}
	}
	return result;
}

inline ::std::string convertToUtf8(const Utf16Char* pSrc, bool conversionIsStrict = true)
{
	return convertUtf32ToUtf8(UnicodeIteratorFactory<Utf16Char>::begin(pSrc),
		UnicodeIteratorFactory<Utf16Char>::end(pSrc), conversionIsStrict);
}

inline ::std::string convertToUtf8(const Utf16String& src, bool conversionIsStrict = true)
{
	return convertUtf32ToUtf8(UnicodeIteratorFactory<Utf16Char>::begin(src),
		UnicodeIteratorFactory<Utf16Char>::end(src), conversionIsStrict);
}

inline ::std::string convertTCharToUtf8(const TChar* pSrc, bool conversionIsStrict = true)
{
#if defined(PARLIAMENT_WINDOWS) && defined(UNICODE)
	static_assert(sizeof(TChar) == sizeof(Utf16Char), "Unexpected size for type alias TChar");
	auto pTSrc = reinterpret_cast<const Utf16Char*>(pSrc);
	return convertUtf32ToUtf8(UnicodeIteratorFactory<Utf16Char>::begin(pTSrc),
		UnicodeIteratorFactory<Utf16Char>::end(pTSrc), conversionIsStrict);
#else
	static_assert(sizeof(TChar) == sizeof(::std::string::value_type), "Unexpected size for type alias TChar");
	return pSrc;
#endif
}

inline ::std::string convertTCharToUtf8(const TString& src, bool conversionIsStrict = true)
{
	return convertTCharToUtf8(src.c_str(), conversionIsStrict);
}

inline ::std::string convertFromRsrcChar(const RsrcChar* pSrc, bool conversionIsStrict = true)
{
#if defined(PARLIAMENT_RSRC_AS_UTF16)
	return convertUtf32ToUtf8(UnicodeIteratorFactory<Utf16Char>::begin(pSrc),
		UnicodeIteratorFactory<Utf16Char>::end(pSrc), conversionIsStrict);
#else
	return pSrc;
#endif
}

inline ::std::string convertFromRsrcChar(const RsrcString& src, bool conversionIsStrict = true)
{
#if defined(PARLIAMENT_RSRC_AS_UTF16)
	return convertUtf32ToUtf8(UnicodeIteratorFactory<Utf16Char>::begin(src),
		UnicodeIteratorFactory<Utf16Char>::end(src), conversionIsStrict);
#else
	return src;
#endif
}

inline Utf16String convertToUtf16(const char* pSrc, bool conversionIsStrict = true)
{
	return convertUtf32ToUtf16(UnicodeIteratorFactory<char>::begin(pSrc),
		UnicodeIteratorFactory<char>::end(pSrc), conversionIsStrict);
}

inline Utf16String convertToUtf16(const ::std::string& src, bool conversionIsStrict = true)
{
	return convertUtf32ToUtf16(UnicodeIteratorFactory<char>::begin(src),
		UnicodeIteratorFactory<char>::end(src), conversionIsStrict);
}

inline RsrcString convertToRsrcChar(const char* pSrc, bool conversionIsStrict = true)
{
#if defined(PARLIAMENT_RSRC_AS_UTF16)
	return convertUtf32ToUtf16(UnicodeIteratorFactory<char>::begin(pSrc),
		UnicodeIteratorFactory<char>::end(pSrc), conversionIsStrict);
#else
	return pSrc;
#endif
}

inline RsrcString convertToRsrcChar(const ::std::string& src, bool conversionIsStrict = true)
{
#if defined(PARLIAMENT_RSRC_AS_UTF16)
	return convertUtf32ToUtf16(UnicodeIteratorFactory<char>::begin(src),
		UnicodeIteratorFactory<char>::end(src), conversionIsStrict);
#else
	return src;
#endif
}

inline ::std::string pathAsUtf8(const ::boost::filesystem::path& p, bool conversionIsStrict = true)
{
#if defined(PARLIAMENT_WINDOWS)
	static_assert(sizeof(::boost::filesystem::path::value_type) == sizeof(Utf16Char),
		"Unexpected size for path characters");
	const auto pathStr = p.wstring();
	auto pPathStr = reinterpret_cast<const Utf16Char*>(pathStr.c_str());
	return convertUtf32ToUtf8(UnicodeIteratorFactory<Utf16Char>::begin(pPathStr),
		UnicodeIteratorFactory<Utf16Char>::end(pPathStr), conversionIsStrict);
#else
	return p.string();
#endif
}

inline ::boost::filesystem::path convertUtf8ToPath(const char* pSrc, bool conversionIsStrict = true)
{
#if defined(PARLIAMENT_WINDOWS)
	static_assert(sizeof(::boost::filesystem::path::value_type) == sizeof(Utf16Char),
		"Unexpected size for path characters");
	auto pathStr = convertToUtf16(pSrc, conversionIsStrict);
	return reinterpret_cast<const ::boost::filesystem::path::value_type*>(pathStr.c_str());
#else
	return pSrc;
#endif
}

inline ::boost::filesystem::path convertUtf8ToPath(const ::std::string& src, bool conversionIsStrict = true)
{
#if defined(PARLIAMENT_WINDOWS)
	static_assert(sizeof(::boost::filesystem::path::value_type) == sizeof(Utf16Char),
		"Unexpected size for path characters");
	auto pathStr = convertToUtf16(src, conversionIsStrict);
	return reinterpret_cast<const ::boost::filesystem::path::value_type*>(pathStr.c_str());
#else
	return src;
#endif
}

#if defined(PARLIAMENT_WINDOWS) && !defined(UNICODE)
extern Utf16String convertMultiByteToUtf16(const char* pMBStr);
#endif

inline ::std::ostream& operator<<(::std::ostream& s, const Utf16Char* pSrc)
{
	return s << convertToUtf8(pSrc);
}

inline ::std::ostream& operator<<(::std::ostream& s, const Utf16String& src)
{
	return s << convertToUtf8(src);
}

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_UNICODEITERATOR_H_INCLUDED
