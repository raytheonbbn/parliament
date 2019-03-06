// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2016, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_LOG_H_INCLUDED)
#define PARLIAMENT_LOG_H_INCLUDED

#include "parliament/Platform.h"

#include <boost/log/sources/record_ostream.hpp>
#include <boost/log/sources/severity_channel_logger.hpp>
#include <boost/log/utility/setup/file.hpp>
#include <string>

PARLIAMENT_NAMESPACE_BEGIN

namespace log
{
	enum class Level { trace, debug, info, warn, error };

	using Source = ::boost::log::sources::severity_channel_logger_mt<Level, ::std::string>;

	Source getSource(const char* pChannelName);
	Source getSource(const ::std::string& channelName);
}

#define PMNT_LOG(logger, lvl) BOOST_LOG_STREAM_SEV(logger, lvl)

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_LOG_H_INCLUDED
