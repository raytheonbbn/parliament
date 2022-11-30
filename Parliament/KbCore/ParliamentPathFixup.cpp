// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2022, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/generated/com_bbn_parliament_jni_LibraryLoader.h"
#include "parliament/Platform.h"
//#include "parliament/JNIHelper.h"
#include "parliament/Windows.h"

#include <boost/format.hpp>
#include <cstdint>
#include <string>

#define BEGIN_JNI_EXCEPTION_HANDLER(pEnv)					\
	try														\
	{														\
		try													\
		{

#define END_JNI_EXCEPTION_HANDLER(pEnv)						\
		}													\
		catch (const exception& ex)							\
		{													\
			throwException(pEnv, ex, __FILE__, __LINE__);	\
		}													\
	}														\
	catch (const JavaException&)							\
	{														\
		/* Do nothing, so as to return to the JVM. */		\
	}

//using namespace ::bbn::parliament;
//namespace pmnt = ::bbn::parliament;
using ::boost::format;
using ::std::exception;
using ::std::string;
using ::std::uint32_t;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* /* pVM */, void* /* pReserved */)
{
	return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* /* pVM */, void* /* pReserved */)
{
}

// This exception indicates that a JNI API call (i.e., one of the JNIEnv
// methods) has failed.  This means that a Java exception is active in the JVM,
// so the C++ code should return to the JVM so that exception can propagate up
// the Java stack.
class JavaException {};

static jclass findClass(JNIEnv* pEnv, const char* pClassName)
{
	jclass cls = pEnv->FindClass(pClassName);
	if (cls == nullptr)
	{
		throw JavaException();
	}
	return cls;
}

static void throwJavaException(JNIEnv* pEnv, const char* pClassName, const char* pMsg)
{
	if (pEnv->ThrowNew(findClass(pEnv, pClassName), pMsg) != 0)
	{
		throw JavaException();
	}
}

static void throwException(JNIEnv* pEnv, const exception& ex,
	const char* pSrcFile, uint32_t srcLineNum)
{
	auto errMsg = str(format{"%1% thrown from %2% at line %3%: %4%"}
		% typeid(ex).name() % pSrcFile % srcLineNum % ex.what());
	throwJavaException(pEnv, "com/bbn/parliament/jni/NativeCodeException",
		errMsg.c_str());
}

static string getCurrentDllFilePath()
{
#if defined(PARLIAMENT_WINDOWS)
	HMODULE hModule = 0;
	if (!::GetModuleHandleEx(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS,
		reinterpret_cast<LPCTSTR>(getCurrentDllFilePath), &hModule))
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to retrieve the module handle: %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}

	for (DWORD bufferLen = MAX_PATH;; bufferLen += MAX_PATH)
	{
		::std::vector<TChar> buffer(bufferLen, '\0');
		DWORD retVal = ::GetModuleFileName(hModule, &buffer[0], bufferLen);
		if (retVal == 0)
		{
			SysErrCode errCode = Exception::getSysErrCode();
			throw Exception(format("Unable to retrieve the module file name: %1% (%2%)")
				% Exception::getSysErrMsg(errCode) % errCode);
		}
		else if (retVal < bufferLen)
		{
			return &buffer[0];
			break;
		}
	}
#else
	return "";
#endif
}

static void addDirToDllPath(const string& dir)
{
#if defined(PARLIAMENT_WINDOWS)
	if (!SetDllDirectoryA(dir.c_str()))
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to set DLL search path %1%: %2% (%3%)")
			% dir % Exception::getSysErrMsg(errCode) % errCode);
	}
#endif
}

static void resetDllPath()
{
#if defined(PARLIAMENT_WINDOWS)
	if (!SetDllDirectoryA(nullptr))
	{
		SysErrCode errCode = Exception::getSysErrCode();
		throw Exception(format("Unable to reset DLL search path: %1% (%2%)")
			% Exception::getSysErrMsg(errCode) % errCode);
	}
#endif
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_jni_LibraryLoader_addDirToDllPath(
	JNIEnv* pEnv, jclass cls)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		auto directory = getCurrentDllFilePath();
		addDirToDllPath(directory);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_jni_LibraryLoader_resetDllPath(
	JNIEnv* pEnv, jclass cls)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		resetDllPath();
	END_JNI_EXCEPTION_HANDLER(pEnv)
}
