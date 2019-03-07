// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/MMapMgr.h"
#include "parliament/ArrayLength.h"
#include "parliament/Exceptions.h"
#include "parliament/UnicodeIterator.h"
#include "parliament/Util.h"

#include <boost/format.hpp>

namespace bfs = ::boost::filesystem;
namespace pmnt = ::bbn::parliament;

using ::boost::format;

// Constants:
const char pmnt::MMapMgr::TblHeader::k_magicFileFormatId[] = "Parliament";
const char pmnt::MMapMgr::TblHeader::k_oldMagicFileFormatId[] = "@damldb@";

void pmnt::MMapMgr::TblHeader::initHeader(TblHeader* pHdr)
{
	if (strlen(k_magicFileFormatId) >= arrayLen(pHdr->m_magic))
	{
		throw Exception("Error:  The \"magic\" file format id is too long to fit in the file header.");
	}

	memset(pHdr, 0, sizeof(*pHdr));
	strcpy(pHdr->m_magic, k_magicFileFormatId);
	pHdr->m_majorVersion = k_majorVersion;
	pHdr->m_minorVersion = k_minorVersion;
	pHdr->m_byteOrderMark = k_byteOrderMark;
	pHdr->m_flags = k_nativeBitnessFlag | k_nativeCharWidthFlag;
	pHdr->m_recordCount = 0;
}

void pmnt::MMapMgr::TblHeader::checkCompatibility(const bfs::path& filePath) const
{
	if (strncmp(m_magic, k_magicFileFormatId, arrayLen(m_magic)) != 0)
	{
		const char* pErrMsg = (strncmp(m_magic, k_oldMagicFileFormatId, arrayLen(m_magic)) == 0)
			? "Error:  The file \"%1%\" was created by an earlier version of Parliament"
			: "Error:  \"%1%\" is not a Parliament-format file";
		throw Exception(format(pErrMsg) % pathAsUtf8(filePath));
	}

	if (m_majorVersion != k_majorVersion || m_minorVersion != k_minorVersion)
	{
		throw Exception(format("Error:  The format of the file \"%1%\" is not "
			"compatible with this version of Parliament") % pathAsUtf8(filePath));
	}

	if (m_byteOrderMark != k_byteOrderMark)
	{
		throw Exception(format("Error:  The byte ordering of the file \"%1%\" "
			"is not compatible with this machine's byte ordering") % pathAsUtf8(filePath));
	}

	FlagValues bitness = (m_flags & FlagValues::k_archBitsMask);
	if (bitness != k_nativeBitnessFlag)
	{
		const char* pBitnessLabel = "<<unknown>>";
		switch (bitness)
		{
		case FlagValues::k_32bitArch:
			pBitnessLabel = "32";
			break;
		case FlagValues::k_64bitArch:
			pBitnessLabel = "64";
			break;
		default:
			assert(false);
			break;
		}
		throw Exception(format("Error:  The file \"%1%\" was created by a "
			"%2%-bit version of Parliament") % pathAsUtf8(filePath) % pBitnessLabel);
	}

	FlagValues charWidth = (m_flags & FlagValues::k_charWidthMask);
	if (charWidth != k_nativeCharWidthFlag)
	{
		const char* pCharWidthLabel = (charWidth == FlagValues::k_16bitChar)
			? "wide (16-bit)" : "narrow (8-bit)";
		throw Exception(format("Error:  The file \"%1%\" was created by a "
			"version of Parliament that uses a %2% character encoding")
			% pathAsUtf8(filePath) % pCharWidthLabel);
	}
}

pmnt::MMapMgr::MMapMgr(const bfs::path& filePath, bool readOnly,
		FileHandle::FileSize initFileSize) :
	m_file(filePath, readOnly, initFileSize),
	m_fileMap(m_file),
	m_fileSize(m_file.getFileSize())
{
	if (m_file.isReadOnly() || !m_file.wasCreatedOnOpen())
	{
		// Validate the header:
		header().checkCompatibility(m_file.getFilePath());
	}
	else
	{
		// Initialize the header:
		TblHeader::initHeader(reinterpret_cast<TblHeader*>(m_fileMap.getBaseAddr()));
	}
}

pmnt::MMapMgr::~MMapMgr()
{
}

void pmnt::MMapMgr::reallocate(FileHandle::FileSize newFileSize)
{
	sync();
	m_fileMap.close();
	m_file.truncate(newFileSize);
	m_fileSize = m_file.getFileSize();
	m_fileMap.reopen(m_file);
}

void pmnt::MMapMgr::sync()
{
	m_fileMap.sync();
#if defined(PARLIAMENT_WINDOWS)
	m_file.sync();
#endif
}
