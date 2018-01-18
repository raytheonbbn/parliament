// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

// This little program generates the static initializers
// for the following constants:
//
//     ::bbn::parliament::Utf8Iterator::k_leadByteValidator
//     ::bbn::parliament::Utf8Iterator::k_byteValidator80toBF
//     ::bbn::parliament::Utf8Iterator::k_byteValidatorA0toBF
//     ::bbn::parliament::Utf8Iterator::k_byteValidator80to9F
//     ::bbn::parliament::Utf8Iterator::k_byteValidator90toBF
//     ::bbn::parliament::Utf8Iterator::k_byteValidator80to8F
//     ::bbn::parliament::Utf8Iterator::k_byteValidator80toC2
//
// The output of this program has been pasted into the
// source file UnicodeIterator.cpp near the top.

//#define USE_COMPACT_REPRESENTATION

#include <iostream>
#include <iomanip>

#include "parliament/Platform.h"

using namespace ::std;

class AbstractFunctor
{
public:
	virtual ~AbstractFunctor() = default;
	virtual bool operator()(size_t i) const = 0;
};

class LeadByteFunctor : public AbstractFunctor
{
public:
	bool operator()(size_t i) const override
		{ return i <= 0x7f || (0xc2 <= i && i <= 0xf4); }
};

class SingleRangeFunctor : public AbstractFunctor
{
public:
	SingleRangeFunctor(size_t loValid, size_t hiValid) :
		m_loValid(loValid), m_hiValid(hiValid) {}
	bool operator()(size_t i) const override
		{ return m_loValid <= i && i <= m_hiValid; }

private:
	size_t m_loValid;
	size_t m_hiValid;
};

static void generateLookupTable(const char* pArrayName, const AbstractFunctor& f)
{
	cout << hex << setfill('0');
	cout << "\nconst bool ::bbn::parliament::Utf8Iterator::" << pArrayName << "[] =\n{\n";
	for (size_t i = 0; i < 256; ++i)
	{
		if (i == 0)
		{
			cout << "\t/* 0x00 */ ";
		}
#if defined(USE_COMPACT_REPRESENTATION)
		else if (i % 16 == 0)
#else
		else if (i % 8 == 0)
#endif
		{
			cout << ",\n\t/* 0x" << setw(2) << i << " */ ";
		}
#if defined(USE_COMPACT_REPRESENTATION)
		else if (i % 8 == 0)
		{
			cout << ",  ";
		}
#endif
		else
		{
			cout << ", ";
		}
#if defined(USE_COMPACT_REPRESENTATION)
		cout << (f(i) ? "1" : "0");
#else
		cout << (f(i) ? " true" : "false");
#endif
	}
	cout << "\n};\n";
	cout << setfill(' ') << dec;
}

int main()
{
	generateLookupTable("k_leadByteValidator", LeadByteFunctor());

	generateLookupTable("k_byteValidator80toBF", SingleRangeFunctor(0x80, 0xbf));
	generateLookupTable("k_byteValidatorA0toBF", SingleRangeFunctor(0xa0, 0xbf));
	generateLookupTable("k_byteValidator80to9F", SingleRangeFunctor(0x80, 0x9f));
	generateLookupTable("k_byteValidator90toBF", SingleRangeFunctor(0x90, 0xbf));
	generateLookupTable("k_byteValidator80to8F", SingleRangeFunctor(0x80, 0x8f));

	return 0;
}
