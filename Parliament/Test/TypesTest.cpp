// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/Types.h"
#include "parliament/ArrayLength.h"

using namespace ::bbn::parliament;

// Compile-time tests for arrayLen:

char testArray1[7];
double testArray2[3];

static_assert(7u == arrayLen(testArray1), "Unexpected size for testArray1");
static_assert(3u == arrayLen(testArray2), "Unexpected size for testArray2");
