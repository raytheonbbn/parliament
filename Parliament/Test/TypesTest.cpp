// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/Types.h"
#include "parliament/ArrayLength.h"

using namespace ::bbn::parliament;

// Compile-time tests for arrayLen:

char testArray1[37];
double testArray2[73];

static_assert(
	sizeof(testArray1) / sizeof((testArray1)[0]) == arrayLen(testArray1),
	"Unexpected size for testArray1");
static_assert(
	sizeof(testArray2) / sizeof((testArray2)[0]) == arrayLen(testArray2),
	"Unexpected size for testArray2");
