// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/Platform.h"
#include "parliament/Windows.h"

#if defined(PARLIAMENT_WINDOWS)

BOOL APIENTRY DllMain(HANDLE /* hModule */, DWORD reasonForCall, void* /* pReserved */)
{
	switch (reasonForCall)
	{
	case DLL_PROCESS_ATTACH:
	case DLL_THREAD_ATTACH:
	case DLL_THREAD_DETACH:
	case DLL_PROCESS_DETACH:
		break;
	}

	return true;
}

#endif
