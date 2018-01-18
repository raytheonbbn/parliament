// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/FileHandle.h"
#include "parliament/Log.h"
#include "parliament/UnicodeIterator.h"

#include <boost/filesystem/operations.hpp>
#include <boost/format.hpp>
#include <vector>
#if !defined(PARLIAMENT_WINDOWS)
#	include <errno.h>
#	include <fcntl.h>
#	include <stdio.h>
#	include <sys/stat.h>
#endif

namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::boost::format;
using ::std::min;
using ::std::vector;

#if defined(PARLIAMENT_WINDOWS)
const pmnt::FileHandle::HFile pmnt::FileHandle::k_nullFileHandle = INVALID_HANDLE_VALUE;
#endif

static auto g_log(pmnt::Log::getSource("FileHandle"));

pmnt::FileHandle::FileHandle(const bfs::path& filePath, bool readOnly, FileSize minSize) :
	m_filePath(filePath),
	m_readOnly(readOnly),
	m_fileWasCreatedOnOpen(!exists(m_filePath) || !is_regular_file(m_filePath)),
#if !defined(PARLIAMENT_WINDOWS)
	m_hFile(::open(m_filePath.c_str(), desiredFileAccess(), 0777))
#else
	m_hFile(::CreateFileW(m_filePath.c_str(), desiredFileAccess(),
		0, nullptr, OPEN_ALWAYS, FILE_FLAG_RANDOM_ACCESS, 0))
#endif
{
	if (m_hFile == k_nullFileHandle)
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to open file \"%1%\":  %2% (%3%)")
			% pathAsUtf8(m_filePath) % Exception::getSysErrMsg(errCode) % errCode);
	}

	if (m_fileWasCreatedOnOpen && minSize > getFileSize())
	{
		try
		{
			truncate(minSize);
		}
		catch (const Exception&)
		{
			close();
			throw;
		}
	}
}

pmnt::FileHandle::~FileHandle() noexcept
{
	close();
}

void pmnt::FileHandle::close() noexcept
{
	bool anErrorOccurred = (m_hFile != k_nullFileHandle
#if defined(PARLIAMENT_WINDOWS)
		&& !::CloseHandle(m_hFile));
#else
		&& ::close(m_hFile) < 0);
#endif

	m_hFile= k_nullFileHandle;

	if (anErrorOccurred)
	{
		SysErrCode errCode = Exception::getSysErrCode();
		PMNT_LOG(g_log, LogLevel::error) << format("Unable to close file:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode;
	}
}

pmnt::FileHandle::FileSize pmnt::FileHandle::getFileSize() const
{
#if defined(PARLIAMENT_WINDOWS)
	LARGE_INTEGER fileSize;
	if (!::GetFileSizeEx(m_hFile, &fileSize))
#else
	struct stat s;
	if (::fstat(m_hFile, &s) < 0)
#endif
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to get file size:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}

#if defined(PARLIAMENT_WINDOWS)
	return fileSize.QuadPart;
#else
	return s.st_size;
#endif
}

pmnt::FileHandle::FileSize pmnt::FileHandle::seek(FileSize offset, SeekMethod method)
{
#if defined(PARLIAMENT_WINDOWS)
	LARGE_INTEGER tmpOffset;
	tmpOffset.QuadPart = offset;
	LARGE_INTEGER result;
	if (!::SetFilePointerEx(m_hFile, tmpOffset, &result, xlateSeekMethod(method)))
#else
	off_t result = ::lseek(m_hFile, offset, xlateSeekMethod(method));
	if (result < 0)
#endif
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to seek to file position:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}

#if defined(PARLIAMENT_WINDOWS)
	return result.QuadPart;
#else
	return result;
#endif
}

void pmnt::FileHandle::checkWritable() const
{
	if (m_readOnly)
	{
		throw Exception(
			"Write operations are prohibited on read-only FileHandle instances");
	}
}

pmnt::uint32 pmnt::FileHandle::write(const uint8* pBuffer, uint32 bytesToWrite)
{
	checkWritable();

#	if defined(PARLIAMENT_WINDOWS)
	DWORD bytesWritten;
	if (!::WriteFile(m_hFile, pBuffer, bytesToWrite, &bytesWritten, nullptr))
#	else
	ssize_t bytesWritten = ::write(m_hFile, pBuffer, bytesToWrite);
	if (bytesWritten <= 0)
#	endif
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to write to file:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}
	return bytesWritten;
}

void pmnt::FileHandle::truncate(FileSize newFileSize)
{
	checkWritable();

	FileSize oldFileSize = getFileSize();
	if (newFileSize != oldFileSize)
	{
#if defined(PARLIAMENT_WINDOWS)
		seek(newFileSize, SeekMethod::k_start);
		setEndOfFile();

		if (newFileSize > oldFileSize)
		{
			seek(oldFileSize, SeekMethod::k_start);

			const uint32 k_chunkSize = 16 * 1024;
			vector<uint8> buffer(k_chunkSize, 0);

			uint32 written;
			for (FileSize toWrite = newFileSize - oldFileSize; toWrite > 0; toWrite -= written)
			{
				uint32 bytesToWrite = static_cast<uint32>(min(toWrite, static_cast<FileSize>(k_chunkSize)));
				written = write(&(buffer[0]), bytesToWrite);
			}
		}
#else
		if (::ftruncate(m_hFile, newFileSize) < 0)
		{
			SysErrCode errCode = Exception::getSysErrCode();
			throw Exception(format("Unable to extend file:  %1% (%2%)")
				% Exception::getSysErrMsg(errCode) % errCode);
		}
#endif
	}
}

void pmnt::FileHandle::sync()
{
	if (!m_readOnly
#if defined(PARLIAMENT_WINDOWS)
		&& !::FlushFileBuffers(m_hFile))
#else
		&& ::fsync(m_hFile) < 0)
#endif
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to flush the file buffers to disk:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}
}

void pmnt::FileHandle::setEndOfFile()
{
	checkWritable();

#if defined(PARLIAMENT_WINDOWS)
	if (!::SetEndOfFile(m_hFile))
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to set the end-of-file position:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}
#endif
}

int pmnt::FileHandle::desiredFileAccess() const noexcept
{
	int result = m_readOnly
#if defined(PARLIAMENT_WINDOWS)
		? GENERIC_READ
		: GENERIC_READ | GENERIC_WRITE;
#elif defined(O_LARGEFILE)
		? O_RDONLY | O_CREAT | O_LARGEFILE
		: O_RDWR | O_CREAT | O_LARGEFILE;
#else
		? O_RDONLY | O_CREAT
		: O_RDWR | O_CREAT;
#endif

	return result;
}

pmnt::FileHandle::OSSeekMethod pmnt::FileHandle::xlateSeekMethod(SeekMethod method) noexcept
{
	switch (method)
	{
#if defined(PARLIAMENT_WINDOWS)
	case SeekMethod::k_start:		return FILE_BEGIN;
	case SeekMethod::k_current:	return FILE_CURRENT;
	case SeekMethod::k_end:			return FILE_END;
#else
	case SeekMethod::k_start:		return SEEK_SET;
	case SeekMethod::k_current:	return SEEK_CUR;
	case SeekMethod::k_end:			return SEEK_END;
#endif
	default:	// This should never happen
		PMNT_LOG(g_log, LogLevel::error) << "Encountered unrecognized SeekMethod enumeration value";
		assert(false);
#if defined(PARLIAMENT_WINDOWS)
		return FILE_BEGIN;
#else
		return SEEK_SET;
#endif
	}
}

bool pmnt::FileHandle::isErrorFileNotFound(pmnt::SysErrCode errCode) noexcept
{
#if defined(PARLIAMENT_WINDOWS)
	return errCode == ERROR_FILE_NOT_FOUND || errCode == ERROR_PATH_NOT_FOUND;
#else
	return errCode == ENOENT;
#endif
}
