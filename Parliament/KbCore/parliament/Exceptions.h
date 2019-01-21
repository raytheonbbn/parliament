// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_EXCEPTIONS_H_INCLUDED)
#define PARLIAMENT_EXCEPTIONS_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <exception>
#include <string>

#include <boost/format.hpp>

PARLIAMENT_NAMESPACE_BEGIN

#if defined(PARLIAMENT_WINDOWS)
using SysErrCode = uint32;
#else
using SysErrCode = int;
#endif

class PARLIAMENT_EXPORT Exception : public ::std::exception
{
public:
	Exception(const char* pMsg) noexcept;
	Exception(const ::std::string& msg) noexcept;
	Exception(const ::boost::format& fmt);
	Exception(const Exception& rhs) noexcept;
	Exception& operator=(const Exception& rhs) noexcept;
	~Exception() override;

	const char* what() const noexcept override { return m_msg; }

	static SysErrCode getSysErrCode() noexcept;
	static ::std::string getSysErrMsg(SysErrCode errCode);

private:
	inline void copyMsg(const char* pMsg) noexcept;

	char m_msg[384];
};

class PARLIAMENT_EXPORT UnicodeException : public Exception
{
public:
	UnicodeException(const char* pMsg) noexcept : Exception(pMsg) {}
	UnicodeException(const ::std::string& msg) noexcept : Exception(msg) {}
	UnicodeException(const ::boost::format& fmt) : Exception(fmt) {}
};

class PARLIAMENT_EXPORT UnimplementedException : public Exception
{
public:
	UnimplementedException(const char* pMsg) noexcept : Exception(pMsg) {}
	UnimplementedException(const ::std::string& msg) noexcept : Exception(msg) {}
	UnimplementedException(const ::boost::format& fmt) : Exception(fmt) {}
};

class PARLIAMENT_EXPORT UsageException : public Exception
{
public:
	UsageException(const char* pMsg) noexcept : Exception(pMsg) {}
	UsageException(const ::std::string& msg) noexcept : Exception(msg) {}
	UsageException(const ::boost::format& fmt) : Exception(fmt) {}
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_EXCEPTIONS_H_INCLUDED
