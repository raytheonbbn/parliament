// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_EXCEPTIONS_H_INCLUDED)
#define PARLIAMENT_EXCEPTIONS_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <array>
#include <exception>
#include <string>

#include <boost/format.hpp>

// UNIX/Linux compilers require that the exception classes themselves be exported so
// that their RTTI is exported, but this upsets MSVC because the base class
// (std::exception) is not exported.
#if defined(PARLIAMENT_WINDOWS)
#define NON_WINDOWS_PARLIAMENT_EXPORT
#else
#define NON_WINDOWS_PARLIAMENT_EXPORT PARLIAMENT_EXPORT
#endif

namespace bbn::parliament
{

#if defined(PARLIAMENT_WINDOWS)
using SysErrCode = uint32;
#else
using SysErrCode = int;
#endif

class NON_WINDOWS_PARLIAMENT_EXPORT Exception : public ::std::exception
{
public:
	PARLIAMENT_EXPORT Exception(const char* pMsg) noexcept;
	PARLIAMENT_EXPORT Exception(const ::std::string& msg) noexcept;
	PARLIAMENT_EXPORT Exception(const ::boost::format& fmt);
	PARLIAMENT_EXPORT Exception(const Exception& rhs) noexcept;
	PARLIAMENT_EXPORT Exception& operator=(const Exception& rhs) noexcept;
	PARLIAMENT_EXPORT ~Exception() override;

	const char* what() const noexcept override { return m_msg.data(); }

	PARLIAMENT_EXPORT static SysErrCode getSysErrCode() noexcept;
	PARLIAMENT_EXPORT static ::std::string getSysErrMsg(SysErrCode errCode);

private:
	inline void copyMsg(const char* pMsg) noexcept;

	::std::array<char, 384> m_msg;
};

class NON_WINDOWS_PARLIAMENT_EXPORT UnicodeException : public Exception
{
public:
	UnicodeException(const char* pMsg) noexcept : Exception(pMsg) {}
	UnicodeException(const ::std::string& msg) noexcept : Exception(msg) {}
	UnicodeException(const ::boost::format& fmt) : Exception(fmt) {}
};

class NON_WINDOWS_PARLIAMENT_EXPORT UnimplementedException : public Exception
{
public:
	UnimplementedException(const char* pMsg) noexcept : Exception(pMsg) {}
	UnimplementedException(const ::std::string& msg) noexcept : Exception(msg) {}
	UnimplementedException(const ::boost::format& fmt) : Exception(fmt) {}
};

class NON_WINDOWS_PARLIAMENT_EXPORT UsageException : public Exception
{
public:
	UsageException(const char* pMsg) noexcept : Exception(pMsg) {}
	UsageException(const ::std::string& msg) noexcept : Exception(msg) {}
	UsageException(const ::boost::format& fmt) : Exception(fmt) {}
};

}	// namespace end

#endif // !PARLIAMENT_EXCEPTIONS_H_INCLUDED
