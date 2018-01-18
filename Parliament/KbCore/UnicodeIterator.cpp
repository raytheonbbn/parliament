// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/UnicodeIterator.h"

#if defined(PARLIAMENT_WINDOWS) && !defined(UNICODE)
#	include "parliament/Windows.h"

#	include <boost/format.hpp>
#	include <vector>
#endif

namespace pmnt = ::bbn::parliament;

// Once the bits are split out into bytes of UTF-8, this is a mask OR-ed
// into the first byte, depending on how many bytes follow.  There are
// as many entries in this table as there are UTF-8 sequence types.
// (I.e., one byte sequence, two byte... etc.). Remember that sequences
// for *legal* UTF-8 will be 4 or fewer bytes total.
const pmnt::uint8 pmnt::UnicodeIteratorBase::k_firstByteMark[] =
{ 0x00, 0x00, 0xc0, 0xe0, 0xf0, 0xf8, 0xfc };

// Index into the table below with the first byte of a UTF-8 sequence to
// get the number of trailing bytes that are supposed to follow it.
// Note that *legal* UTF-8 values cannot have 4 or 5-bytes. The table is
// left as-is for anyone who may want to do such conversion, which was
// allowed in earlier algorithms.
const pmnt::uint8 pmnt::UnicodeIteratorBase::k_utf8CharLen[] =
{
	1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
	1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
	1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
	1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
	1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
	1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
	2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2, 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
	3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3, 4,4,4,4,4,4,4,4,5,5,5,5,6,6,6,6
};

// Magic values subtracted from a buffer value during UTF-8 conversion.
// This table contains as many values as there might be trailing bytes
// in a UTF-8 sequence.
const pmnt::Utf32Char pmnt::UnicodeIteratorBase::k_offsetsFromUtf8[] =
{
	0x00000000ul,
	0x00003080ul,
	0x000e2080ul,
	0x03c82080ul,
	0xfa082080ul,
	0x82082080ul
};

const bool pmnt::UnicodeIteratorBase::k_leadByteValidator[] =
{
	/* 0x00 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x08 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x10 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x18 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x20 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x28 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x30 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x38 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x40 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x48 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x50 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x58 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x60 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x68 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x70 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x78 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x80 */ false, false, false, false, false, false, false, false,
	/* 0x88 */ false, false, false, false, false, false, false, false,
	/* 0x90 */ false, false, false, false, false, false, false, false,
	/* 0x98 */ false, false, false, false, false, false, false, false,
	/* 0xa0 */ false, false, false, false, false, false, false, false,
	/* 0xa8 */ false, false, false, false, false, false, false, false,
	/* 0xb0 */ false, false, false, false, false, false, false, false,
	/* 0xb8 */ false, false, false, false, false, false, false, false,
	/* 0xc0 */ false, false,  true,  true,  true,  true,  true,  true,
	/* 0xc8 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xd0 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xd8 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xe0 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xe8 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xf0 */  true,  true,  true,  true,  true, false, false, false,
	/* 0xf8 */ false, false, false, false, false, false, false, false
};

const bool pmnt::UnicodeIteratorBase::k_byteValidator80toBF[] =
{
	/* 0x00 */ false, false, false, false, false, false, false, false,
	/* 0x08 */ false, false, false, false, false, false, false, false,
	/* 0x10 */ false, false, false, false, false, false, false, false,
	/* 0x18 */ false, false, false, false, false, false, false, false,
	/* 0x20 */ false, false, false, false, false, false, false, false,
	/* 0x28 */ false, false, false, false, false, false, false, false,
	/* 0x30 */ false, false, false, false, false, false, false, false,
	/* 0x38 */ false, false, false, false, false, false, false, false,
	/* 0x40 */ false, false, false, false, false, false, false, false,
	/* 0x48 */ false, false, false, false, false, false, false, false,
	/* 0x50 */ false, false, false, false, false, false, false, false,
	/* 0x58 */ false, false, false, false, false, false, false, false,
	/* 0x60 */ false, false, false, false, false, false, false, false,
	/* 0x68 */ false, false, false, false, false, false, false, false,
	/* 0x70 */ false, false, false, false, false, false, false, false,
	/* 0x78 */ false, false, false, false, false, false, false, false,
	/* 0x80 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x88 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x90 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x98 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xa0 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xa8 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xb0 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xb8 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xc0 */ false, false, false, false, false, false, false, false,
	/* 0xc8 */ false, false, false, false, false, false, false, false,
	/* 0xd0 */ false, false, false, false, false, false, false, false,
	/* 0xd8 */ false, false, false, false, false, false, false, false,
	/* 0xe0 */ false, false, false, false, false, false, false, false,
	/* 0xe8 */ false, false, false, false, false, false, false, false,
	/* 0xf0 */ false, false, false, false, false, false, false, false,
	/* 0xf8 */ false, false, false, false, false, false, false, false
};

const bool pmnt::UnicodeIteratorBase::k_byteValidatorA0toBF[] =
{
	/* 0x00 */ false, false, false, false, false, false, false, false,
	/* 0x08 */ false, false, false, false, false, false, false, false,
	/* 0x10 */ false, false, false, false, false, false, false, false,
	/* 0x18 */ false, false, false, false, false, false, false, false,
	/* 0x20 */ false, false, false, false, false, false, false, false,
	/* 0x28 */ false, false, false, false, false, false, false, false,
	/* 0x30 */ false, false, false, false, false, false, false, false,
	/* 0x38 */ false, false, false, false, false, false, false, false,
	/* 0x40 */ false, false, false, false, false, false, false, false,
	/* 0x48 */ false, false, false, false, false, false, false, false,
	/* 0x50 */ false, false, false, false, false, false, false, false,
	/* 0x58 */ false, false, false, false, false, false, false, false,
	/* 0x60 */ false, false, false, false, false, false, false, false,
	/* 0x68 */ false, false, false, false, false, false, false, false,
	/* 0x70 */ false, false, false, false, false, false, false, false,
	/* 0x78 */ false, false, false, false, false, false, false, false,
	/* 0x80 */ false, false, false, false, false, false, false, false,
	/* 0x88 */ false, false, false, false, false, false, false, false,
	/* 0x90 */ false, false, false, false, false, false, false, false,
	/* 0x98 */ false, false, false, false, false, false, false, false,
	/* 0xa0 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xa8 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xb0 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xb8 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xc0 */ false, false, false, false, false, false, false, false,
	/* 0xc8 */ false, false, false, false, false, false, false, false,
	/* 0xd0 */ false, false, false, false, false, false, false, false,
	/* 0xd8 */ false, false, false, false, false, false, false, false,
	/* 0xe0 */ false, false, false, false, false, false, false, false,
	/* 0xe8 */ false, false, false, false, false, false, false, false,
	/* 0xf0 */ false, false, false, false, false, false, false, false,
	/* 0xf8 */ false, false, false, false, false, false, false, false
};

const bool pmnt::UnicodeIteratorBase::k_byteValidator80to9F[] =
{
	/* 0x00 */ false, false, false, false, false, false, false, false,
	/* 0x08 */ false, false, false, false, false, false, false, false,
	/* 0x10 */ false, false, false, false, false, false, false, false,
	/* 0x18 */ false, false, false, false, false, false, false, false,
	/* 0x20 */ false, false, false, false, false, false, false, false,
	/* 0x28 */ false, false, false, false, false, false, false, false,
	/* 0x30 */ false, false, false, false, false, false, false, false,
	/* 0x38 */ false, false, false, false, false, false, false, false,
	/* 0x40 */ false, false, false, false, false, false, false, false,
	/* 0x48 */ false, false, false, false, false, false, false, false,
	/* 0x50 */ false, false, false, false, false, false, false, false,
	/* 0x58 */ false, false, false, false, false, false, false, false,
	/* 0x60 */ false, false, false, false, false, false, false, false,
	/* 0x68 */ false, false, false, false, false, false, false, false,
	/* 0x70 */ false, false, false, false, false, false, false, false,
	/* 0x78 */ false, false, false, false, false, false, false, false,
	/* 0x80 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x88 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x90 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x98 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xa0 */ false, false, false, false, false, false, false, false,
	/* 0xa8 */ false, false, false, false, false, false, false, false,
	/* 0xb0 */ false, false, false, false, false, false, false, false,
	/* 0xb8 */ false, false, false, false, false, false, false, false,
	/* 0xc0 */ false, false, false, false, false, false, false, false,
	/* 0xc8 */ false, false, false, false, false, false, false, false,
	/* 0xd0 */ false, false, false, false, false, false, false, false,
	/* 0xd8 */ false, false, false, false, false, false, false, false,
	/* 0xe0 */ false, false, false, false, false, false, false, false,
	/* 0xe8 */ false, false, false, false, false, false, false, false,
	/* 0xf0 */ false, false, false, false, false, false, false, false,
	/* 0xf8 */ false, false, false, false, false, false, false, false
};

const bool pmnt::UnicodeIteratorBase::k_byteValidator90toBF[] =
{
	/* 0x00 */ false, false, false, false, false, false, false, false,
	/* 0x08 */ false, false, false, false, false, false, false, false,
	/* 0x10 */ false, false, false, false, false, false, false, false,
	/* 0x18 */ false, false, false, false, false, false, false, false,
	/* 0x20 */ false, false, false, false, false, false, false, false,
	/* 0x28 */ false, false, false, false, false, false, false, false,
	/* 0x30 */ false, false, false, false, false, false, false, false,
	/* 0x38 */ false, false, false, false, false, false, false, false,
	/* 0x40 */ false, false, false, false, false, false, false, false,
	/* 0x48 */ false, false, false, false, false, false, false, false,
	/* 0x50 */ false, false, false, false, false, false, false, false,
	/* 0x58 */ false, false, false, false, false, false, false, false,
	/* 0x60 */ false, false, false, false, false, false, false, false,
	/* 0x68 */ false, false, false, false, false, false, false, false,
	/* 0x70 */ false, false, false, false, false, false, false, false,
	/* 0x78 */ false, false, false, false, false, false, false, false,
	/* 0x80 */ false, false, false, false, false, false, false, false,
	/* 0x88 */ false, false, false, false, false, false, false, false,
	/* 0x90 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x98 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xa0 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xa8 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xb0 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xb8 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0xc0 */ false, false, false, false, false, false, false, false,
	/* 0xc8 */ false, false, false, false, false, false, false, false,
	/* 0xd0 */ false, false, false, false, false, false, false, false,
	/* 0xd8 */ false, false, false, false, false, false, false, false,
	/* 0xe0 */ false, false, false, false, false, false, false, false,
	/* 0xe8 */ false, false, false, false, false, false, false, false,
	/* 0xf0 */ false, false, false, false, false, false, false, false,
	/* 0xf8 */ false, false, false, false, false, false, false, false
};

const bool pmnt::UnicodeIteratorBase::k_byteValidator80to8F[] =
{
	/* 0x00 */ false, false, false, false, false, false, false, false,
	/* 0x08 */ false, false, false, false, false, false, false, false,
	/* 0x10 */ false, false, false, false, false, false, false, false,
	/* 0x18 */ false, false, false, false, false, false, false, false,
	/* 0x20 */ false, false, false, false, false, false, false, false,
	/* 0x28 */ false, false, false, false, false, false, false, false,
	/* 0x30 */ false, false, false, false, false, false, false, false,
	/* 0x38 */ false, false, false, false, false, false, false, false,
	/* 0x40 */ false, false, false, false, false, false, false, false,
	/* 0x48 */ false, false, false, false, false, false, false, false,
	/* 0x50 */ false, false, false, false, false, false, false, false,
	/* 0x58 */ false, false, false, false, false, false, false, false,
	/* 0x60 */ false, false, false, false, false, false, false, false,
	/* 0x68 */ false, false, false, false, false, false, false, false,
	/* 0x70 */ false, false, false, false, false, false, false, false,
	/* 0x78 */ false, false, false, false, false, false, false, false,
	/* 0x80 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x88 */  true,  true,  true,  true,  true,  true,  true,  true,
	/* 0x90 */ false, false, false, false, false, false, false, false,
	/* 0x98 */ false, false, false, false, false, false, false, false,
	/* 0xa0 */ false, false, false, false, false, false, false, false,
	/* 0xa8 */ false, false, false, false, false, false, false, false,
	/* 0xb0 */ false, false, false, false, false, false, false, false,
	/* 0xb8 */ false, false, false, false, false, false, false, false,
	/* 0xc0 */ false, false, false, false, false, false, false, false,
	/* 0xc8 */ false, false, false, false, false, false, false, false,
	/* 0xd0 */ false, false, false, false, false, false, false, false,
	/* 0xd8 */ false, false, false, false, false, false, false, false,
	/* 0xe0 */ false, false, false, false, false, false, false, false,
	/* 0xe8 */ false, false, false, false, false, false, false, false,
	/* 0xf0 */ false, false, false, false, false, false, false, false,
	/* 0xf8 */ false, false, false, false, false, false, false, false
};

void pmnt::UnicodeIteratorBase::postError(const char* pMsg)
{
	if (m_conversionIsStrict)
	{
		throw UnicodeException(pMsg);
	}
	else
	{
		m_pError = makeUnique<UnicodeException>(pMsg);
	}
}

#if defined(PARLIAMENT_WINDOWS) && !defined(UNICODE)
pmnt::Utf16String pmnt::convertMultiByteToUtf16(const char* pMBStr)
{
	using ::boost::format;

	int length = MultiByteToWideChar(CP_THREAD_ACP, MB_ERR_INVALID_CHARS, pMBStr, -1,
		nullptr, 0);
	if (length == 0)
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format{"MultiByteToWideChar #1 failure: %1% (%2%)"}
			% Exception::getSysErrMsg(errCode) % errCode);
	}

	::std::vector<Utf16Char> buffer(length, 0);
	if (MultiByteToWideChar(CP_THREAD_ACP, MB_ERR_INVALID_CHARS, pMBStr, -1,
		reinterpret_cast<LPWSTR>(&buffer[0]), length) == 0)
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format{"MultiByteToWideChar #2 failure: %1% (%2%)"}
			% Exception::getSysErrMsg(errCode) % errCode);
	}

	return &buffer[0];
}
#endif
