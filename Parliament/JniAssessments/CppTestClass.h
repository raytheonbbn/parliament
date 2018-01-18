// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(CPPTESTCLASS_H_INCLUDED)
#define CPPTESTCLASS_H_INCLUDED

#include <cmath>

class CppTestClass
{
public:
	CppTestClass() : m_d(0) {}

	double accumulate(double d)
	{
		m_d = ::std::sin(d * m_d + d);
		return m_d;
	}

private:
	double m_d;
};

#endif // !CPPTESTCLASS_H_INCLUDED
