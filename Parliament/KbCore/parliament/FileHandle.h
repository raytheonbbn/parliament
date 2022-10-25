// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_FILEHANDLE_H_INCLUDED)
#define PARLIAMENT_FILEHANDLE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"
#include "parliament/Exceptions.h"
#include "parliament/Windows.h"

#include <boost/filesystem/path.hpp>

namespace bbn::parliament
{

enum class SeekMethod { k_start, k_current, k_end };

class FileHandle
{
public:
#if defined(PARLIAMENT_WINDOWS)
	using HFile = HANDLE;
	using FileSize = uint64;
	using OSSeekMethod = DWORD;
	static const HFile k_nullFileHandle;
#else
	using HFile = int;
	using FileSize = size_t;
	using OSSeekMethod = int;
	static constexpr HFile k_nullFileHandle = -1;
#endif

	FileHandle(const ::boost::filesystem::path& filePath, bool readOnly, FileSize minSize = 0);	// creates or opens the file
	FileHandle(const FileHandle&) = delete;
	FileHandle& operator=(const FileHandle&) = delete;
	FileHandle(FileHandle&&) = delete;
	FileHandle& operator=(FileHandle&&) = delete;
	~FileHandle() noexcept;

	::boost::filesystem::path getFilePath() const noexcept
		{ return m_filePath; }
	bool isReadOnly() const noexcept
		{ return m_readOnly; }
	bool wasCreatedOnOpen() const noexcept
		{ return m_fileWasCreatedOnOpen; }
	HFile getInternalFilehandle() const noexcept
		{ return m_hFile; }

	FileSize getFileSize() const;
	FileSize seek(FileSize offset, SeekMethod method);			// returns absolute file position after seek
	FileSize write(const uint8* pBuffer, FileSize bytesToWrite);	// returns # bytes written
	void truncate(FileSize newFileSize);
	void sync();
	void close() noexcept;

private:
	void checkWritable() const;
	void setEndOfFile();	// is a no-op on some platforms
	int desiredFileAccess() const noexcept;
	static OSSeekMethod xlateSeekMethod(SeekMethod method) noexcept;
	static bool isErrorFileNotFound(SysErrCode errCode) noexcept;

	::boost::filesystem::path	m_filePath;
	bool								m_readOnly;
	bool								m_fileWasCreatedOnOpen;
	HFile								m_hFile;
};

}	// namespace end

#endif // !PARLIAMENT_FILEHANDLE_H_INCLUDED
