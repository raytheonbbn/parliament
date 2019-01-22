// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_MMAPMGR_H_INCLUDED)
#define PARLIAMENT_MMAPMGR_H_INCLUDED

#include "parliament/Types.h"
#include "parliament/FileHandle.h"
#include "parliament/FileMapping.h"

#include <boost/filesystem/path.hpp>

PARLIAMENT_NAMESPACE_BEGIN

class MMapMgr
{
public:
	struct TblHeader
	{
		enum class FlagValues : uint32
		{
			k_32bitArch	= 0x0ul,
			k_64bitArch	= 0x1ul,
			k_archBitsMask	= k_32bitArch | k_64bitArch,

			k_8bitChar	= 0x0ul,
			k_16bitChar	= 0x4ul,
			k_charWidthMask	= k_8bitChar | k_16bitChar
		};

		static const char k_magicFileFormatId[];
		static const char k_oldMagicFileFormatId[];
		static const uint16 k_majorVersion = 4;
		static const uint16 k_minorVersion = 1;
		static const uint32 k_byteOrderMark = 0x01020304;

#if defined(PARLIAMENT_64BITS)
		static const FlagValues k_nativeBitnessFlag = FlagValues::k_64bitArch;
#elif defined(PARLIAMENT_32BITS)
		static const FlagValues k_nativeBitnessFlag = FlagValues::k_32bitArch;
#else
#	error Parliament currently supports only 32-bit and 64-bit hardware architectures
#endif

#if defined(PARLIAMENT_RSRC_AS_UTF16)
		static const FlagValues k_nativeCharWidthFlag = FlagValues::k_16bitChar;
#else
		static const FlagValues k_nativeCharWidthFlag = FlagValues::k_8bitChar;
#endif

		char			m_magic[12];
		uint16			m_majorVersion;
		uint16			m_minorVersion;
		uint32			m_byteOrderMark;
		FlagValues	m_flags;
		size_t			m_recordCount;

		static void initHeader(TblHeader* pHdr);
		void checkCompatibility(const ::boost::filesystem::path& filePath) const;
	};

	MMapMgr(const ::boost::filesystem::path& filePath, bool readOnly,
		FileHandle::FileSize initFileSize);
	MMapMgr(const MMapMgr&) = delete;
	MMapMgr& operator=(const MMapMgr&) = delete;
	MMapMgr(MMapMgr&&) = delete;
	MMapMgr& operator=(MMapMgr&&) = delete;
	~MMapMgr();

	uint8* baseAddr() const
		{ return m_fileMap.getBaseAddr(); }
	TblHeader& header() const
		{ return *static_cast<TblHeader*>(static_cast<void*>(baseAddr())); }
	FileHandle::FileSize fileSize() const
		{ return m_fileSize; }
	::boost::filesystem::path filePath() const
		{ return m_file.getFilePath(); }
	void reallocate(FileHandle::FileSize newFileSize);

	void sync();

	static size_t growRecordCount(size_t oldRecCount, double growthFactor);

private:
	FileHandle				m_file;
	FileMapping				m_fileMap;
	FileHandle::FileSize	m_fileSize;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_MMAPMGR_H_INCLUDED
