// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "TestUtils.h"

#include "parliament/CharacterLiteral.h"
#include "parliament/Config.h"
#include "parliament/Exceptions.h"
#include "parliament/KbInstance.h"
#include "parliament/Log.h"

#include <boost/filesystem/operations.hpp>
#include <boost/format.hpp>
#include <cstdio>
#include <stdexcept>

namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::exception;
using ::std::runtime_error;
using ::std::string;
using ::std::vector;

#if defined(PARLIAMENT_WINDOWS)
#	define FOPEN_MODE_SUFFIX _T("b")
#else
#	define FOPEN_MODE_SUFFIX
#endif

static const pmnt::TChar k_fopenReadMode[] = _T("r") FOPEN_MODE_SUFFIX;
static const pmnt::TChar k_fopenWriteMode[] = _T("w") FOPEN_MODE_SUFFIX;

static auto g_log(pmnt::Log::getSource("TestUtils"));

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

pmnt::KbDeleter::KbDeleter(const Config& config) :
	m_config(config),
	m_dataDir(),
	m_dataDirSupplied(false)
{
	KbInstance::deleteKb(m_config);
}

pmnt::KbDeleter::KbDeleter(const Config& config, const bfs::path& dataDir) :
	m_config(config),
	m_dataDir(dataDir),
	m_dataDirSupplied(true)
{
	KbInstance::deleteKb(m_config, m_dataDir);
}

pmnt::KbDeleter::~KbDeleter()
{
	if (m_dataDirSupplied)
	{
		KbInstance::deleteKb(m_config, m_dataDir);
	}
	else
	{
		KbInstance::deleteKb(m_config);
	}
}

void pmnt::setEnvVar(const TChar* pEnvStr)
{
#if !defined(PARLIAMENT_WINDOWS)
	if (putenv(const_cast<char*>(pEnvStr)) != 0)
#elif defined(UNICODE)
	if (_wputenv(pEnvStr) != 0)
#else
	if (_putenv(pEnvStr) != 0)
#endif
	{
		//Must throw something more specific than the base exception
		//class b/c the constructor that takes a char* is Windows-specific.
		throw runtime_error("putenv failed in unit test");
	}
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
		PMNT_LOG(g_log, pmnt::LogLevel::error) <<
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
		PMNT_LOG(g_log, pmnt::LogLevel::error) <<
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
