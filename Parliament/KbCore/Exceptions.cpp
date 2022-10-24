// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/Exceptions.h"
#if defined(PARLIAMENT_WINDOWS)
#	include "parliament/UnicodeIterator.h"
#endif
#include "parliament/Log.h"

#include <boost/algorithm/string/trim.hpp>
#include <cstring>

#if defined(PARLIAMENT_WINDOWS)
#	include "parliament/Windows.h"
#	include "parliament/CharacterLiteral.h"
#	include <vector>
#else
#	include <errno.h>
#endif

namespace pmnt = ::bbn::parliament;

using ::boost::algorithm::trim_copy;
using ::boost::format;
using ::std::exception;
using ::std::string;
using ::std::strncpy;

#if defined(PARLIAMENT_WINDOWS)
static auto g_log(pmnt::log::getSource("Exceptions"));
#endif



void pmnt::Exception::copyMsg(const char* pMsg) noexcept
{
	strncpy(m_msg.data(), pMsg, m_msg.size());
	m_msg.back() = '\0';
}

pmnt::Exception::Exception(const char* pMsg) noexcept :
	exception(),
	m_msg()
{
	copyMsg(pMsg);
}

pmnt::Exception::Exception(const string& msg) noexcept :
	exception(),
	m_msg()
{
	copyMsg(msg.c_str());
}

pmnt::Exception::Exception(const format& fmt) :
	exception(),
	m_msg()
{
	copyMsg(str(fmt).c_str());
}

pmnt::Exception::Exception(const Exception& rhs) noexcept :
	exception(),
	m_msg()
{
	copyMsg(rhs.m_msg.data());
}

pmnt::Exception& pmnt::Exception::operator=(const Exception& rhs) noexcept
{
	copyMsg(rhs.m_msg.data());
	return *this;
}

pmnt::Exception::~Exception()
{
}

pmnt::SysErrCode pmnt::Exception::getSysErrCode() noexcept
{
#if defined(PARLIAMENT_WINDOWS)
	return ::GetLastError();
#else
	return errno;
#endif
}

string pmnt::Exception::getSysErrMsg(SysErrCode errCode)
{
#if defined(PARLIAMENT_WINDOWS)
	constexpr DWORD k_flags = FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS;
#if defined(PARLIAMENT_UNIT_TEST)
	constexpr size_t k_bufferIncrement = 8;	// Force a retry with enlarged buffer at test time
#else
	constexpr size_t k_bufferIncrement = 256;
#endif
	for (size_t bufferSize = k_bufferIncrement;; bufferSize += k_bufferIncrement)
	{
		::std::vector<TChar> msgBuffer(bufferSize, _T('\0'));
		DWORD numChars = ::FormatMessage(k_flags, nullptr, errCode, 0, &(msgBuffer[0]),
			static_cast<DWORD>(msgBuffer.size()), nullptr);
		auto errCode = getSysErrCode();
		if ((numChars == 0 && errCode == ERROR_INSUFFICIENT_BUFFER)
			|| numChars >= msgBuffer.size())
		{
			// The buffer is too small -- loop around and try again with a bigger buffer
		}
		else if (numChars > 0)
		{
#if defined(UNICODE)
			return trim_copy(convertTCharToUtf8(&(msgBuffer[0])));
#else
			// Convert from Windows default multi-byte character encoding (which is never UTF-8) to UTF-8:
			return trim_copy(convertToUtf8(convertMultiByteToUtf16(&(msgBuffer[0]))));
#endif
		}
		else
		{
			// An error -- return an empty string
			PMNT_LOG(g_log, log::Level::error) << format{
				"FormatMessage error.  numChars = %1%, error code = %2%"}
				% numChars % errCode;
			return string();
		}
	}
#else
	return trim_copy(string(::strerror(errCode)));
#endif
}
