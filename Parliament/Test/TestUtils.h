// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_TESTUTILS_H_INCLUDED)
#define PARLIAMENT_TESTUTILS_H_INCLUDED

#include "parliament/KbConfig.h"
#include "parliament/Types.h"

#include <boost/filesystem/path.hpp>
#include <vector>

PARLIAMENT_NAMESPACE_BEGIN

class FileDeleter
{
public:
	FileDeleter(const ::boost::filesystem::path& filePath);
	FileDeleter(const FileDeleter&) = delete;
	FileDeleter& operator=(const FileDeleter&) = delete;
	FileDeleter(FileDeleter&&) = delete;
	FileDeleter& operator=(FileDeleter&&) = delete;
	~FileDeleter();

	void disableDeleteOnDestruct()
		{ m_deleteOnDestruct = false; }

private:
	::boost::filesystem::path m_filePath;
	bool m_deleteOnDestruct;
};

class KbDeleter
{
public:
	KbDeleter(const KbConfig& config, bool deleteContainingDir);

	KbDeleter(const KbDeleter&) = delete;
	KbDeleter& operator=(const KbDeleter&) = delete;
	KbDeleter(KbDeleter&&) = delete;
	KbDeleter& operator=(KbDeleter&&) = delete;

	~KbDeleter();

private:
	const KbConfig m_config;
	const bool m_deleteContainingDir;
};

class EnvVarReset
{
public:
	EnvVarReset(const TString& envVarName, const TString& newEnvVarValue);
	EnvVarReset(const EnvVarReset&) = delete;
	EnvVarReset& operator=(const EnvVarReset&) = delete;
	EnvVarReset(EnvVarReset&&) = delete;
	EnvVarReset& operator=(EnvVarReset&&) = delete;
	~EnvVarReset();

	void disableResetOnDestruct()
		{ m_resetOnDestruct = false; }

private:
	static void tSetEnvVar(const TString& envVarName, const TString& newEnvVarValue);

	TString m_envVarName;
	TString m_newEnvVarValue;
	TString m_oldEnvVarValue;
	bool m_resetOnDestruct;
};

void readFileContents(const ::boost::filesystem::path& fileName, ::std::vector<uint8>& content);
void writeBytesToFile(const ::boost::filesystem::path& fileName, const ::std::vector<uint8>& content);
void copyFile(const ::boost::filesystem::path& srcFile, const ::boost::filesystem::path& dstFile);

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_TESTUTILS_H_INCLUDED
