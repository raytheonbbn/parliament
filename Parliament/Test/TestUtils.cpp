// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "TestUtils.h"

#include "parliament/CharacterLiteral.h"
#include "parliament/Exceptions.h"
#include "parliament/KbConfig.h"
#include "parliament/KbInstance.h"
#include "parliament/Log.h"
#include "parliament/Util.h"

#include <boost/filesystem/operations.hpp>
#include <boost/format.hpp>
#include <cstdio>
#include <cstdlib>
#include <stdexcept>

namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::exception;
using ::std::runtime_error;
using ::std::string;
using ::std::vector;

#if defined(PARLIAMENT_WINDOWS)
static constexpr pmnt::TChar k_fopenReadMode[] = _T("rb");
static constexpr pmnt::TChar k_fopenWriteMode[] = _T("wb");
#else
static constexpr pmnt::TChar k_fopenReadMode[] = _T("r");
static constexpr pmnt::TChar k_fopenWriteMode[] = _T("w");
#endif

static auto g_log(pmnt::log::getSource("TestUtils"));

pmnt::FileDeleter::FileDeleter(const bfs::path& filePath) :
	m_filePath(filePath), m_deleteOnDestruct(true)
{
	remove(m_filePath);
}

pmnt::FileDeleter::~FileDeleter()
{
	if (m_deleteOnDestruct)
	{
		remove(m_filePath);
	}
}

pmnt::KbDeleter::KbDeleter(const KbConfig& config, bool deleteContainingDir) :
	m_config(config),
	m_deleteContainingDir(deleteContainingDir)
{
	KbInstance::deleteKb(m_config, m_deleteContainingDir);
}

pmnt::KbDeleter::~KbDeleter()
{
	KbInstance::deleteKb(m_config, m_deleteContainingDir);
}

pmnt::EnvVarReset::EnvVarReset(const TString& envVarName, const TString& newEnvVarValue) :
	m_envVarName(envVarName),
	m_newEnvVarValue(newEnvVarValue),
	m_oldEnvVarValue(tGetEnvVar(envVarName.c_str())),
	m_resetOnDestruct(true)
{
	tSetEnvVar(m_envVarName, m_newEnvVarValue);
}

pmnt::EnvVarReset::~EnvVarReset()
{
	if (m_resetOnDestruct)
	{
		tSetEnvVar(m_envVarName, m_oldEnvVarValue);
	}
}

void pmnt::EnvVarReset::tSetEnvVar(const TString& envVarName, const TString& newEnvVarValue)
{
#if defined(PARLIAMENT_WINDOWS)
#	if defined(UNICODE)
	if (_wputenv_s(envVarName.c_str(), newEnvVarValue.c_str()) != 0)
#	else
	if (_putenv_s(envVarName.c_str(), newEnvVarValue.c_str()) != 0)
#	endif
	{
		throw runtime_error("_putenv_s failed in unit test");
	}
#else
	if (newEnvVarValue.empty())
	{
		if (unsetenv(envVarName.c_str()) != 0)
		{
			throw runtime_error("unsetenv failed in unit test");
		}
	}
	else if (setenv(envVarName.c_str(), newEnvVarValue.c_str(), true) != 0)
	{
		throw runtime_error("setenv failed in unit test");
	}
#endif
}

void pmnt::readFileContents(const bfs::path& fileName, vector<uint8>& content)
{
	try
	{
#if defined(PARLIAMENT_WINDOWS) && defined(UNICODE)
		FILE* pFile = _wfopen(fileName.c_str(), k_fopenReadMode);
#else
		FILE* pFile = fopen(fileName.string().c_str(), k_fopenReadMode);
#endif
		if (pFile == nullptr)
		{
			throw runtime_error("fopen failed in unit test");
		}

		fseek(pFile, 0, SEEK_END);
		size_t fileSize = ftell(pFile);
		size_t numItemsRead = 0;
		if (fileSize > 0)
		{
			fseek(pFile, 0, SEEK_SET);
			content.resize(fileSize);
			numItemsRead = fread(&(content[0]), sizeof(content[0]), content.size(), pFile);
		}
		fclose(pFile);
		if (numItemsRead != fileSize)
		{
			throw runtime_error("fread failed in unit test");
		}
	}
	catch (const exception& ex)
	{
		pmnt::SysErrCode errCode = pmnt::Exception::getSysErrCode();
		PMNT_LOG(g_log, pmnt::log::Level::error) <<
			format{"Exception in readFileContents():  %1% (exception type %2%, system error %3%, %4%)"}
			% ex.what() % typeid(ex).name() % errCode % pmnt::Exception::getSysErrMsg(errCode);
		throw;
	}
}

void pmnt::writeBytesToFile(const bfs::path& fileName, const vector<uint8>& content)
{
	try
	{
#if defined(PARLIAMENT_WINDOWS) && defined(UNICODE)
		FILE* pFile = _wfopen(fileName.c_str(), k_fopenWriteMode);
#else
		FILE* pFile = fopen(fileName.string().c_str(), k_fopenWriteMode);
#endif
		if (pFile == nullptr)
		{
			throw runtime_error("fopen failed in unit test");
		}

		size_t numItemsWritten = 0;
		if (content.size() > 0)
		{
			numItemsWritten = fwrite(&(content[0]), sizeof(content[0]), content.size(), pFile);
		}
		fclose(pFile);
		if (numItemsWritten != content.size())
		{
			throw runtime_error("fwrite failed in unit test");
		}
	}
	catch (const exception& ex)
	{
		pmnt::SysErrCode errCode = pmnt::Exception::getSysErrCode();
		PMNT_LOG(g_log, pmnt::log::Level::error) <<
			format{"Exception in writeBytesToFile():  %1% (exception type %2%, system error %3%, %4%)"}
			% ex.what() % typeid(ex).name() % errCode % pmnt::Exception::getSysErrMsg(errCode);
		throw;
	}
}

void pmnt::copyFile(const bfs::path& srcFile, const bfs::path& dstFile)
{
	vector<uint8> fileContent;
	readFileContents(srcFile, fileContent);
	writeBytesToFile(dstFile, fileContent);
}
