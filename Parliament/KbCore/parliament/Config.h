// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_CONFIG_H_INCLUDED)
#define PARLIAMENT_CONFIG_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"

#include <boost/filesystem/path.hpp>
#include <functional>
#include <map>
#include <string>

PARLIAMENT_NAMESPACE_BEGIN

class Config
{
public:
	using Path = ::boost::filesystem::path;

	PARLIAMENT_EXPORT Config();
	PARLIAMENT_EXPORT Config(const Config&);
	PARLIAMENT_EXPORT Config& operator=(const Config&);
	PARLIAMENT_EXPORT Config(Config&&);
	PARLIAMENT_EXPORT Config& operator=(Config&&);
	PARLIAMENT_EXPORT virtual ~Config();

	PARLIAMENT_EXPORT void readFromFile();

#if defined(PARLIAMENT_UNIT_TEST)
	static Path testGetConfigFilePath(const TChar* pEnvVarName, const TChar* pDefaultConfigFileName)
		{ return getConfigFilePath(pEnvVarName, pDefaultConfigFileName); }
	static bool testIsBlankOrCommentLine(const ::std::string& line)
		{ return isBlankOrCommentLine(line); }
	static size_t testParseUnsigned(const ::std::string& s, uint32 lineNum)
		{ return parseUnsigned(s, lineNum); }
	static double testParseDouble(const ::std::string& s, uint32 lineNum)
		{ return parseDouble(s, lineNum); }
	static bool testParseBool(const ::std::string& s, uint32 lineNum)
		{ return parseBool(s, lineNum); }
#endif

protected:
	using EntryHandler = ::std::function<void(const ::std::string&, uint32, Config&)>;
	using ConfigEntryMap = ::std::map<::std::string, EntryHandler>;	// Maps key to handler

	static void splitAtFirstEquals(/* in */ const ::std::string& line,
		/* in */ uint32 lineNum, /* out */ ::std::string& key,
		/* out */ ::std::string& value);
	static size_t parseUnsigned(const ::std::string& s, uint32 lineNum);
	static double parseDouble(const ::std::string& s, uint32 lineNum);
	static bool parseBool(const ::std::string& s, uint32 lineNum);

private:
	static Path getConfigFilePath(const TChar* pEnvVarName, const TChar* pDefaultConfigFileName);
	static bool isBlankOrCommentLine(const ::std::string& line);

	virtual const TChar* getEnvVarName() const = 0;
	virtual const TChar* getDefaultConfigFileName() const = 0;
	virtual const ConfigEntryMap& getConfigEntryMap() const = 0;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_CONFIG_H_INCLUDED
