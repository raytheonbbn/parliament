// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2022, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_CONFIGFILEREADER_H_INCLUDED)
#define PARLIAMENT_CONFIGFILEREADER_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <boost/filesystem/path.hpp>
#include <fstream>
#include <functional>
#include <string>
#include <string_view>
#include <utility>

namespace bbn::parliament
{

enum class ConfigKind
{
	k_kb, k_log
};

class ConfigFileReader
{
public:
	using Path = ::boost::filesystem::path;
	using LineHandler = ::std::function<void(::std::string_view /* key */,
		::std::string_view /* value */, uint32 /* line # */)>;

	static void readFile(ConfigKind configKind, LineHandler lineHandler);

	static ::std::pair<::std::string_view, ::std::string_view> getKeyValueFromLine(
		::std::string_view line, uint32 lineNum);
	static size_t parseUnsigned(::std::string_view s, uint32 lineNum);
	static double parseDouble(::std::string_view s, uint32 lineNum);
	static bool parseBool(::std::string_view s, uint32 lineNum);

#if defined(PARLIAMENT_UNIT_TEST)
	static Path testGetConfigFilePath(TStringView envVarName, TStringView defaultConfigFileName)
		{ return getConfigFilePath(envVarName, defaultConfigFileName); }
	static bool testIsBlankOrCommentLine(const ::std::string& line)
		{ return isBlankOrCommentLine(line); }
#endif

private:
	static ::std::ifstream openFile(ConfigKind configKind);
	static ::std::pair<const TChar*, const TChar*> getEnvVarAndFileName(
		ConfigKind configKind);
	static Path getConfigFilePath(TStringView envVarName, TStringView defaultConfigFileName);
	static bool isBlankOrCommentLine(const ::std::string& line);
};

}	// namespace end

#endif // !PARLIAMENT_CONFIGFILEREADER_H_INCLUDED
