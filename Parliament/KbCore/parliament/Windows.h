// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_WINDOWS_H_INCLUDED)
#define PARLIAMENT_WINDOWS_H_INCLUDED

// Whenever you need to include <windows.h>, include "parliament/Windows.h"
// (this file) instead.  This takes care of the ifdef and the extra
// defines that control how <windows.h> is included.

#include "parliament/Platform.h"

#if defined(PARLIAMENT_WINDOWS)
#	if !defined(STRICT)
#		define STRICT					// Enables strict type checks on Win32 API
#	endif
#	if !defined(WIN32_LEAN_AND_MEAN)
#		define WIN32_LEAN_AND_MEAN	// Excludes rarely-used stuff from Windows headers
#	endif
#	if !defined(NOMINMAX)
#		define NOMINMAX				// Excludes min() and max() macros
#	endif
#	if !defined(_WIN32_WINNT) || _WIN32_WINNT < _WIN32_WINNT_WIN10
#		undef _WIN32_WINNT
#		define _WIN32_WINNT _WIN32_WINNT_WIN10
#	endif
#	if !defined(WINVER) || WINVER < _WIN32_WINNT_WIN10
#		undef WINVER
#		define WINVER _WIN32_WINNT_WIN10
#	endif
#	if defined(UNICODE) && !defined(_UNICODE)
#		define _UNICODE
#	endif
#	include <windows.h>
#endif

#endif // !PARLIAMENT_WINDOWS_H_INCLUDED
