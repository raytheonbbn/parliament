// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2022, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/generated/com_bbn_parliament_jni_LibraryLoader.h"
#include "parliament/Windows.h"

#include <boost/format.hpp>
#include <filesystem>
#include <string>
#include <system_error>
#include <vector>

#define BEGIN_JNI_EXCEPTION_HANDLER(pEnv)		\
	try													\
	{														\
		try												\
		{

#define END_JNI_EXCEPTION_HANDLER(pEnv)		\
		}													\
		catch (const exception& ex)				\
		{													\
			throwException(pEnv, ex);				\
		}													\
	}														\
	catch (const JavaException&)					\
	{														\
		/* Do nothing to return to the JVM. */	\
	}

using ::boost::format;
using ::std::exception;
using ::std::filesystem::path;
using ::std::string;
using ::std::system_category;
using ::std::system_error;
using ::std::vector;

static constexpr char k_nativeExClass[] = "com/bbn/parliament/jni/NativeCodeException";

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

static void throwException(JNIEnv* pEnv, const exception& ex)
{
	auto exClass = findClass(pEnv, k_nativeExClass);
	if (pEnv->ThrowNew(exClass, ex.what()) != 0)
	{
		throw JavaException();
	}
}

#if defined(PARLIAMENT_WINDOWS)
static void throwSystemException(int errCode, const char* pFile, int line,
	format& fmt)
{
	throw system_error{errCode, system_category(),
		str(fmt % errCode % pFile % line)};
}
#endif

static path getCurrentDllFilePath()
{
#if defined(PARLIAMENT_WINDOWS)
	HMODULE hModule = 0;
	if (!::GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS,
		reinterpret_cast<LPCWSTR>(getCurrentDllFilePath), &hModule))
	{
		throwSystemException(::GetLastError(), __FILE__, __LINE__, format{
			"Unable to retrieve the handle of the current module"
			" (error code %1%, file %2%, line %3%)"});
	}

	for (DWORD bufferLen = MAX_PATH;; bufferLen += MAX_PATH)
	{
		vector<wchar_t> buffer(bufferLen, '\0');
		DWORD retVal = ::GetModuleFileNameW(hModule, &buffer[0], bufferLen);
		if (retVal == 0)
		{
			throwSystemException(::GetLastError(), __FILE__, __LINE__, format{
				"Unable to retrieve the module file name"
				" (error code %1%, file %2%, line %3%)"});
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

static void addDirToDllPath(const path& dir)
{
#if defined(PARLIAMENT_WINDOWS)
	if (!SetDllDirectoryW(dir.c_str()))
	{
		throwSystemException(::GetLastError(), __FILE__, __LINE__, format{
			"Unable to set DLL search path %1%"
			" (error code %2%, file %3%, line %4%)"} % dir.generic_string());
	}
#endif
}

static void resetDllPath()
{
#if defined(PARLIAMENT_WINDOWS)
	if (!SetDllDirectoryW(nullptr))
	{
		throwSystemException(::GetLastError(), __FILE__, __LINE__, format{
			"Unable to reset DLL search path"
			" (error code %1%, file %2%, line %3%)"});
	}
#endif
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_jni_LibraryLoader_addDirToDllPath(
	JNIEnv* pEnv, jclass cls)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		auto directory = getCurrentDllFilePath().parent_path();
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
