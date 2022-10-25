// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_EQUIVALENTCLASSRULE_H_INCLUDED)
#define PARLIAMENT_EQUIVALENTCLASSRULE_H_INCLUDED

#include "parliament/Platform.h"
#include "parliament/RuleEngine.h"

namespace bbn::parliament
{

class EquivalentClassRule : public StandardRule
{
public:
	EquivalentClassRule(KbInstance* pKB, RuleEngine* pRE);
};

}	// namespace end

#endif // !PARLIAMENT_EQUIVALENTCLASSRULE_H_INCLUDED
