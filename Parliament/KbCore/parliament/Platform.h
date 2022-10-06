// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_PLATFORM_H_INCLUDED)
#define PARLIAMENT_PLATFORM_H_INCLUDED



//  ===========================================================================
//  Figure out which platform we are on:
//  ===========================================================================

#if defined(_WIN64)
#	define PARLIAMENT_WINDOWS
#	define PARLIAMENT_64BITS
#elif defined(_WIN32)
#	define PARLIAMENT_WINDOWS
#	define PARLIAMENT_32BITS
#elif defined(__clang__)
#	if defined(__APPLE__)
#		define PARLIAMENT_MACOS
#	else
#		error Clang C++ is not supported on this platform.
#	endif
#	if defined(__x86_64__) || defined(__arm64__) || defined(__amd64__) || defined(__ppc64__) || defined(__sparcv9)
#		define PARLIAMENT_64BITS
#	elif defined(__i386__) || defined(__arm__) || defined(__ppc__) || defined(__sparc__)
#		define PARLIAMENT_32BITS
#	else
#		error Clang C++ is not supported on this hardware architecture.
#	endif
#elif defined(__GNUC__)
#	if defined(__linux__)
#		define PARLIAMENT_LINUX
#	elif defined (__sun__)
#		define PARLIAMENT_SOLARIS
#	else
#		error GNU C++ (GCC) is not supported on this platform.
#	endif
#	if defined(__x86_64__) || defined(__amd64__) || defined(__ppc64__) || defined(__sparcv9)
#		define PARLIAMENT_64BITS
#	elif defined(__i386__) || defined(__ppc__) || defined(__sparc__)
#		define PARLIAMENT_32BITS
#	else
#		error GNU C++ (GCC) is not supported on this hardware architecture.
#	endif
#elif defined(__sun) || defined(__SUN__)
#	define PARLIAMENT_SOLARIS
#	define PARLIAMENT_32BITS
#	define __EXTENSIONS__
#	error Compilation for Solaris is not yet supported.
#elif defined(__hpux)
#	define PARLIAMENT_HPUX_11
#	define PARLIAMENT_32BITS
#	error Compilation for HP-UX is not yet supported.
#elif defined(_AIX)
#	define PARLIAMENT_AIX
#	define PARLIAMENT_32BITS
#	error Compilation for AIX is not yet supported.
#else
#	error Unknown platform.
#endif



//  ===========================================================================
//  Figure out which compiler we are using, and check its version:
//  ===========================================================================

#if defined(PARLIAMENT_WINDOWS) && defined(_MSC_VER)
#	define PARLIAMENT_MSVC
#	if _MSC_VER < 1310
#		error Microsoft Visual C++ version 7.1 (or later) is required.
#	endif
#elif defined(PARLIAMENT_SOLARIS) && defined(__SUNPRO_CC)
#	define PARLIAMENT_SPARCWORKS
#	if __SUNPRO_CC < 0x510
#		error SparcWorks version 6.0 (or later) is required.
#	endif
#elif defined(__clang__)
#	define PARLIAMENT_CLANG
#	if (__clang_major__ < 8 || (__clang_major__ == 8 && __clang_minor__ < 1) || (__clang_major__ == 8 && __clang_minor__ == 1 && __clang_patchlevel__ < 0))
#		error Clang C++ version 8.1.0 (or later) is required.
#	endif
#elif defined(__GNUC__)
#	define PARLIAMENT_GCC
#	if (__GNUC__ < 4 || (__GNUC__ == 4 && __GNUC_MINOR__ < 8) || (__GNUC__ == 4 && __GNUC_MINOR__ == 8 && __GNUC_PATCHLEVEL__ < 5))
#		error GNU C++ (GCC) version 4.8.5 (or later) is required.
#	endif
#else
#	error Unknown complier.
#endif



//  ===========================================================================
//  Set up for DLL exports
//  ===========================================================================

#if defined(PARLIAMENT_WINDOWS) && !defined(PARLIAMENT_SUPPRESS_EXPORTS)
#	if defined(BUILDING_KBCORE)
#		define PARLIAMENT_EXPORT __declspec(dllexport)
#		define PARLIAMENT_EXPORT_TEMPLATE_INST template class PARLIAMENT_EXPORT
#	else
#		define PARLIAMENT_EXPORT __declspec(dllimport)
#		define PARLIAMENT_EXPORT_TEMPLATE_INST extern template class PARLIAMENT_EXPORT
#	endif
#elif (defined(PARLIAMENT_MACOS) || defined(PARLIAMENT_LINUX)) && !defined(PARLIAMENT_SUPPRESS_EXPORTS)
#	define PARLIAMENT_EXPORT __attribute__((visibility("default")))
#	define PARLIAMENT_EXPORT_TEMPLATE_INST
#else
#	define PARLIAMENT_EXPORT
#	define PARLIAMENT_EXPORT_TEMPLATE_INST
#endif



//  ===========================================================================
//  Auto-specify libraries for the linker
//  ===========================================================================

#if defined(PARLIAMENT_MSVC) && !defined(BUILDING_KBCORE) && !defined(PARLIAMENT_SUPPRESS_EXPORTS) && !defined(PARLIAMENT_SUPPRESS_AUTOLINK)
#	pragma comment(lib, "Parliament.lib")
#endif



//  ===========================================================================
//  Disable spurious compiler warnings
//  ===========================================================================

#if defined(PARLIAMENT_MSVC)
#	pragma warning(disable: 4996)		// warns of deprecated functions
#endif



//  ===========================================================================
//  Define the namespace:
//  ===========================================================================

#define PARLIAMENT_NAMESPACE_BEGIN namespace bbn { namespace parliament {
#define PARLIAMENT_NAMESPACE_END } }



//  ===========================================================================
//  Enable the wide character version of the Win32 API:
//  ===========================================================================

#if defined(PARLIAMENT_WINDOWS) && !defined(UNICODE)
#	define UNICODE
#endif



//  ===========================================================================
//  Switch to UTF-16 encoding for resources (affects the file formats):
//  ===========================================================================

//#define PARLIAMENT_RSRC_AS_UTF16

#endif // !PARLIAMENT_PLATFORM_H_INCLUDED
