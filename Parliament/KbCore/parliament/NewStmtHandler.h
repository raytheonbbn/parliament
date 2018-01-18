// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_NEWSTMTHANDLER_H_INCLUDED)
#define PARLIAMENT_NEWSTMTHANDLER_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/Types.h"
#include "parliament/Statement.h"

PARLIAMENT_NAMESPACE_BEGIN

class KbInstance;

class NewStmtHandler
{
public:
	virtual ~NewStmtHandler() = default;
	virtual void onNewStmt(KbInstance* pKB, const Statement& stmt) = 0;
};

PARLIAMENT_NAMESPACE_END

#endif // !PARLIAMENT_NEWSTMTHANDLER_H_INCLUDED
