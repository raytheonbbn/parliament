// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

// Due to the likihood of name collisions, this file should only be included in .cpp files

#if !defined(PARLIAMENT_CHARACTERLITERAL_H_INCLUDED)
#define PARLIAMENT_CHARACTERLITERAL_H_INCLUDED

#include "parliament/Platform.h"

PARLIAMENT_NAMESPACE_BEGIN

// Windows wide character and wide string literals:
#if !defined(_T)
#	if defined(PARLIAMENT_WINDOWS) && defined(UNICODE)
#		define _T(x) L ## x
#	else
#		define _T(x) x
#	endif
#endif

// Resource character and string literals:
//#if defined(PARLIAMENT_RSRC_AS_UTF16)
//#	define RC(x) u ## x
//#else
//#	define RC(x) u8 ##x
//#endif

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_CHARACTERLITERAL_H_INCLUDED
