// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_FILEMAPPING_H_INCLUDED)
#define PARLIAMENT_FILEMAPPING_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"
#include "parliament/FileHandle.h"
#include "parliament/Windows.h"

#if (1 < 0) && defined(PARLIAMENT_WINDOWS)
#	define FILE_MAP_DIAGNOSTICS
#endif

#if defined(FILE_MAP_DIAGNOSTICS)
#	include <map>
#endif

PARLIAMENT_NAMESPACE_BEGIN

class FileMapping
{
public:
	FileMapping(const FileHandle& file);
	FileMapping(const FileMapping&) = delete;
	FileMapping& operator=(const FileMapping&) = delete;
	FileMapping(FileMapping&&) = delete;
	FileMapping& operator=(FileMapping&&) = delete;
	~FileMapping();

	void reopen(const FileHandle& file);
	void close();

	void sync();

	uint8* getBaseAddr() const
		{ return m_pBaseAddress; }

#if defined(FILE_MAP_DIAGNOSTICS)
	void dumpVirtualMemoryStatus();
#endif

private:
#if defined(FILE_MAP_DIAGNOSTICS)
	enum class StateOrProtection { STATE, PROTECTION };

	using BitFlagsHistogram = ::std::map<uint32, uint64>;

	static void incrementBitFlagsHistogram(BitFlagsHistogram& hist,
		uint32 key, uint64 count);
	static void printHistogram(BitFlagsHistogram& hist,
		StateOrProtection stateOrProtection, uint32 pageSize);
	static ::std::string getStateLabel(uint32 state);
	static ::std::string getProtectLabel(uint32 protection);
#endif

#if defined(PARLIAMENT_WINDOWS)
	HANDLE					m_hMap;
#endif
	FileHandle::FileSize	m_fileSize;
	uint8*					m_pBaseAddress;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_FILEMAPPING_H_INCLUDED
