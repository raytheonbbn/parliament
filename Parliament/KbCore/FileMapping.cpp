// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/FileMapping.h"
#include "parliament/FileHandle.h"
#include "parliament/Exceptions.h"
#include "parliament/Log.h"
#include "parliament/Util.h"

#include <boost/format.hpp>
#if !defined(PARLIAMENT_WINDOWS)
#	include <sys/mman.h>
#endif

#if defined(FILE_MAP_DIAGNOSTICS)
#	include <algorithm>
#	include <iostream>
#	include <string>

using ::std::string;
#endif

namespace pmnt = ::bbn::parliament;

using ::boost::format;

static auto g_log(pmnt::Log::getSource("FileMapping"));

pmnt::FileMapping::FileMapping(const FileHandle& file) :
#if defined(PARLIAMENT_WINDOWS)
	m_hMap(0),
#endif
	m_fileSize(0),
	m_pBaseAddress(nullptr)
{
	reopen(file);
}

pmnt::FileMapping::~FileMapping()
{
#if defined(FILE_MAP_DIAGNOSTICS)
	try
	{
		dumpVirtualMemoryStatus();
	}
	catch (const Exception& ex)
	{
		PMNT_LOG(g_log, LogLevel::error) << format("Unable to dump virtual memory status:  %1%")
			% ex.what();
	}
#endif
	close();
}

void pmnt::FileMapping::reopen(const FileHandle& file)
{
	m_fileSize = file.getFileSize();
#if defined(PARLIAMENT_WINDOWS)
	DWORD desiredMapAccess = file.isReadOnly() ? PAGE_READONLY : PAGE_READWRITE;
	m_hMap = ::CreateFileMapping(file.getInternalFilehandle(), nullptr, desiredMapAccess, 0, 0, nullptr);
	if (m_hMap == 0)
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to create file mapping:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}

	DWORD desiredMapViewAccess = file.isReadOnly() ? FILE_MAP_READ : FILE_MAP_ALL_ACCESS;
	m_pBaseAddress = static_cast<uint8*>(::MapViewOfFile(m_hMap, desiredMapViewAccess, 0, 0, 0));
	if (m_pBaseAddress == nullptr)
	{
		SysErrCode errCode = Exception::getSysErrCode();
		CloseHandle(m_hMap);
		m_hMap = 0;
		throw Exception(format("Unable to map view of file:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}
#else
	int desiredMapAccess = file.isReadOnly() ? PROT_READ : (PROT_READ | PROT_WRITE);
	m_pBaseAddress = static_cast<uint8*>(::mmap(nullptr, m_fileSize, desiredMapAccess,
		MAP_SHARED, file.getInternalFilehandle(), 0));
	if (m_pBaseAddress == MAP_FAILED)
	{
		m_pBaseAddress = nullptr;
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to map memory:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}

#	if defined(PARLIAMENT_LINUX)
	::madvise(nullptr, m_fileSize, MADV_RANDOM);
#	endif
#endif
}

void pmnt::FileMapping::close()
{
#if defined(PARLIAMENT_WINDOWS)
	SysErrCode viewErrCode = NOERROR;
	SysErrCode mapErrCode = NOERROR;
	if (m_pBaseAddress != nullptr && !::UnmapViewOfFile(m_pBaseAddress))
	{
		viewErrCode = Exception::getSysErrCode();
	}
	m_pBaseAddress = nullptr;

	if (m_hMap != 0 && !::CloseHandle(m_hMap))
	{
		mapErrCode = Exception::getSysErrCode();
	}
	m_hMap = 0;

	if (viewErrCode != NOERROR)
	{
		PMNT_LOG(g_log, LogLevel::error) << format("Unable to unmap view of file:  %1% (%2%)")
			% Exception::getSysErrMsg(viewErrCode) % viewErrCode;
	}
	if (mapErrCode != NOERROR)
	{
		PMNT_LOG(g_log, LogLevel::error) << format("Unable to close file mapping:  %1% (%2%)")
			% Exception::getSysErrMsg(mapErrCode) % mapErrCode;
	}
#else
	bool anErrorOccurred = (m_pBaseAddress != nullptr
		&& ::munmap(m_pBaseAddress, m_fileSize) < 0);

	m_pBaseAddress = nullptr;

	if (anErrorOccurred)
	{
		SysErrCode errCode = Exception::getSysErrCode();
		PMNT_LOG(g_log, LogLevel::error) << format("Unable to unmap memory:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode;
	}
#endif
}

void pmnt::FileMapping::sync()
{
#if defined(PARLIAMENT_WINDOWS)
	if (!FlushViewOfFile(m_pBaseAddress, static_cast<SIZE_T>(m_fileSize)))
#else
	if (msync(m_pBaseAddress, m_fileSize, MS_SYNC) != 0)
#endif
	{
		SysErrCode errCode = Exception::getSysErrCode();
		PMNT_LOG(g_log, LogLevel::error) << format(
			"Unable to synchronously flush mapped memory:  %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode;
	}
}

#if defined(FILE_MAP_DIAGNOSTICS)
void pmnt::FileMapping::dumpVirtualMemoryStatus()
{
	SYSTEM_INFO si;
	::GetSystemInfo(&si);
	uint32 pageSize = si.dwPageSize;

	MEMORY_BASIC_INFORMATION mbi;
	BitFlagsHistogram stateMap;
	BitFlagsHistogram protectMap;

	for (uint8* pAddr = m_pBaseAddress;; pAddr += mbi.RegionSize)
	{
		if (::VirtualQuery(pAddr, &mbi, sizeof(mbi)) != sizeof(mbi))
		{
			SysErrCode errCode = Exception::getSysErrCode();
			throw Exception(format("Unable to dump virtual memory status:  %1% (%2%)")
				% Exception::getSysErrMsg(errCode) % errCode);
		}
		else if (mbi.AllocationBase != m_pBaseAddress)
		{
			break;
		}
		else
		{
			uint64 numPages = mbi.RegionSize / pageSize;
			incrementBitFlagsHistogram(stateMap, mbi.State, numPages);
			if (mbi.State == MEM_COMMIT)
			{
				incrementBitFlagsHistogram(protectMap, mbi.Protect, numPages);
			}
		}
	}

	printHistogram(stateMap, StateOrProtection::STATE, pageSize);
	//printHistogram(protectMap, StateOrProtection::PROTECTION, pageSize);
}

void pmnt::FileMapping::incrementBitFlagsHistogram(
	BitFlagsHistogram& hist, uint32 key, uint64 count)
{
	auto newCount = count;
	auto it = hist.find(key);
	if (it != end(hist))
	{
		newCount += it->second;
	}
	it->second = newCount;
}

void pmnt::FileMapping::printHistogram(BitFlagsHistogram& hist,
	StateOrProtection stateOrProtection, uint32 pageSize)
{
	using KeyLabelFxn = string (*)(uint32 key);

	string desc;
	string colHeading;
	KeyLabelFxn getKeyLabel;
	switch (stateOrProtection)
	{
	case StateOrProtection::STATE:
		desc = "states";
		colHeading = "Page State";
		getKeyLabel = &getStateLabel;
		break;
	case StateOrProtection::PROTECTION:
		desc = "protections";
		colHeading = "Protection";
		getKeyLabel = &getProtectLabel;
		break;
	default:
		throw Exception("Bad StateOrProtection enum value");
		break;
	}
	::std::cout << ::std::endl
		<< "Histogram of memory page " << desc << " for memory mapped file" << ::std::endl
		<< colHeading << ",Number of " << pageSize << "-Byte Pages" << ::std::endl;
	::std::for_each(cBegin(hist), cEnd(hist),
		[getKeyLabel](const BitFlagsHistogram::value_type& e)
		{
			::std::cout << getKeyLabel(e.first) << ',' << e.second << ::std::endl;
		});
}

string pmnt::FileMapping::getStateLabel(uint32 state)
{
	switch (state)
	{
	case MEM_COMMIT:	return "MEM_COMMIT";
	case MEM_FREE:		return "MEM_FREE";
	case MEM_RESERVE:	return "MEM_RESERVE";
	default:				return str(format("0x%1$08x") % state);
	}
}

string pmnt::FileMapping::getProtectLabel(uint32 protection)
{
	static const uint32 k_modifierMask = PAGE_GUARD | PAGE_NOCACHE | PAGE_WRITECOMBINE;

	uint32 modifiers = k_modifierMask & protection;
	uint32 unmodProtection = (~k_modifierMask) & protection;

	string result;
	bool ableToTranslate = true;
	switch (unmodProtection)
	{
	case PAGE_EXECUTE:				result = "PAGE_EXECUTE"; break;
	case PAGE_EXECUTE_READ:			result = "PAGE_EXECUTE_READ"; break;
	case PAGE_EXECUTE_READWRITE:	result = "PAGE_EXECUTE_READWRITE"; break;
	case PAGE_EXECUTE_WRITECOPY:	result = "PAGE_EXECUTE_WRITECOPY"; break;
	case PAGE_NOACCESS:				result = "PAGE_NOACCESS"; break;
	case PAGE_READONLY:				result = "PAGE_READONLY"; break;
	case PAGE_READWRITE:				result = "PAGE_READWRITE"; break;
	case PAGE_WRITECOPY:				result = "PAGE_WRITECOPY"; break;
	default:								ableToTranslate = false; break;
	}

	if (ableToTranslate)
	{
		if (modifiers & PAGE_GUARD)
		{
			result += " | PAGE_GUARD";
			modifiers &= ~PAGE_GUARD;
		}
		if (modifiers & PAGE_NOCACHE)
		{
			result += " | PAGE_NOCACHE";
			modifiers &= ~PAGE_NOCACHE;
		}
		if (modifiers & PAGE_WRITECOMBINE)
		{
			result += " | PAGE_WRITECOMBINE";
			modifiers &= ~PAGE_WRITECOMBINE;
		}
		if (modifiers != 0)
		{
			ableToTranslate = false;
		}
	}

	return ableToTranslate
		? result
		: str(format("0x%1$08x") % protection);
}
#endif
