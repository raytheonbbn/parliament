// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

// This little program generates the static initializer
// for the constant k_testData in the file UnicodeIteratorTest.cpp,
// along with the constants used in that initializer.  To use,
// compile and run with this command:
//
// cl /nologo /EHsc UnicodeTestCaseGen.cpp & UnicodeTestCaseGen < Utf8Input.txt > gen.cpp
//
// Then paste the contents of the file gen.cpp into the
// source file UnicodeIteratorTest.cpp near the top.
//
// Note:  This program is Windows-specific.

#if defined(_WIN32)

#include <cctype>
#include <cstdint>
#include <iomanip>
#include <iostream>
#include <iterator>
#include <limits>
#include <string>
#include <vector>

#define STRICT
#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#include <boost/format.hpp>

using namespace ::std;

using ::boost::format;
using ::std::setfill;
using ::std::setw;

using Utf16Char = ::std::uint16_t;
using Utf16String = ::std::basic_string<Utf16Char>;
using Utf32Char = ::std::uint32_t;
using Utf8InputList = vector<string>;

static const char*const k_pUtf8Data[] =
{
//	"\xc3" "\xa5",

//	// With a BOM:
//	"\xef" "\xbb" "\xbf" "\xc3" "\xa5",

//	// Should translate to:  V, M, W, U+0430, X, U+4e8c, Y, U+10302, Z:
//	"V\x4dW\xd0\xb0X\xe4\xba\x8cY\xf0\x90\x8c\x82Z",

	"\xef\xbb\xbf\x56\x4d\x57\xd0\xb0\x58\xe4\xba\x8c\x59\xf0\x90\x8c\x82\x5a",
	"VMW\xd0\xb0\x58\xe4\xba\x8c\x59\xf0\x90\x8c\x82\x5a",
	"English:  Leading the Web to its Full Potential\xe2\x80\xa6",
	"Catalan:  Duent la Web al seu ple potencial\xe2\x80\xa6",
	"Dutch:  Het Web tot zijn volle potentieel ontwikkelen\xe2\x80\xa6",
	"French:  Amener le Web vers son plein potentiel\xe2\x80\xa6",
	"German:  Alle M\xc3\xb6\x67\x6c\x69\x63\x68\x6b\x65\x69\x74\x65\x6e\x20\x64\x65\x73\x20\x57\x65\x62\x20\x65\x72\x73\x63\x68\x6c\x69\x65\xc3\x9f\x65\x6e\xe2\x80\xa6",
	"Greek:  \xce\x9f\xce\xb4\xce\xb7\xce\xb3\xcf\x8e\xce\xbd\xcf\x84\xce\xb1\xcf\x82\x20\xcf\x84\xce\xbf\xce\xbd\x20\xcf\x80\xce\xb1\xce\xb3\xce\xba\xcf\x8c\xce\xbc\xce\xb9\xce\xbf\x20\xce\xb9\xcf\x83\xcf\x84\xcf\x8c\x20\xcf\x83\xcf\x84\xce\xbf\x20\xce\xbc\xce\xad\xce\xb3\xce\xb9\xcf\x83\xcf\x84\xce\xbf\x20\xcf\x84\xcf\x89\xce\xbd\x20\xce\xb4\xcf\x85\xce\xbd\xce\xb1\xcf\x84\xce\xbf\xcf\x84\xce\xae\xcf\x84\xcf\x89\xce\xbd\x20\xcf\x84\xce\xbf\xcf\x85\xe2\x80\xa6",
	"Hungarian:  Hogy kihaszn\xc3\xa1\x6c\x68\x61\x73\x73\x75\x6b\x20\x61\x20\x57\x65\x62\x20\x6e\x79\xc3\xba\x6a\x74\x6f\x74\x74\x61\x20\xc3\xb6\x73\x73\x7a\x65\x73\x20\x6c\x65\x68\x65\x74\xc5\x91\x73\xc3\xa9\x67\x65\x74\xe2\x80\xa6",
	"Italian:  Sviluppare al massimo il potenziale del Web\xe2\x80\xa6",
	"Chinese:  \xe5\xbc\x95\xe5\x8f\x91\xe7\xbd\x91\xe7\xbb\x9c\xe7\x9a\x84\xe5\x85\xa8\xe9\x83\xa8\xe6\xbd\x9c\xe8\x83\xbd\xe2\x80\xa6",
	"Korean:  \xec\x9b\xb9\xec\x9d\x98\x20\xeb\xaa\xa8\xeb\x93\xa0\x20\xec\x9e\xa0\xec\x9e\xac\xeb\xa0\xa5\xec\x9d\x84\x20\xec\x9d\xb4\xeb\x81\x8c\xec\x96\xb4\x20\xeb\x82\xb4\xea\xb8\xb0\x20\xec\x9c\x84\xed\x95\x98\xec\x97\xac\xe2\x80\xa6",
	"Portuguese:  Levando a Web em direc\xc3\xa7\xc3\xa3\x6f\x20\x61\x6f\x20\x73\x65\x75\x20\x70\x6f\x74\x65\x6e\x63\x69\x61\x6c\x20\x6d\xc3\xa0\x78\x69\x6d\x6f\xe2\x80\xa6",
	"Russian:  P\xd0\xb0\xd1\x81\xd0\xba\xd1\x80\xd1\x8b\xd0\xb2\xd0\xb0\xd1\x8f\x20\xd0\xb2\xd0\xb5\xd1\x81\xd1\x8c\x20\xd0\xbf\xd0\xbe\xd1\x82\xd0\xb5\xd0\xbd\xd1\x86\xd0\xb8\xd0\xb0\xd0\xbb\x20\xd0\xa1\xd0\xb5\xd1\x82\xd0\xb8\xe2\x80\xa6",
	"Spanish:  Guiando el Web a su completo potencial\xe2\x80\xa6",
	"Swedish:  Se till att Webben n\xc3\xa5\x72\x20\x73\x69\x6e\x20\x66\x75\x6c\x6c\x61\x20\x70\x6f\x74\x65\x6e\x74\x69\x61\x6c\xe2\x80\xa6",
	"Finnish:  Ohjaamassa Webin kehittymist\xc3\xa4\x20\x74\xc3\xa4\x79\x74\x65\x65\x6e\x20\x6d\x69\x74\x74\x61\x61\x6e\x73\x61\xe2\x80\xa6",
	"Hebrew:  \xd7\x9c\xd7\x94\xd7\x95\xd7\x91\xd7\x99\xd7\x9c\x20\xd7\x90\xd7\xaa\x20\xd7\x94\xd7\xa8\xd7\xa9\xd7\xaa\x20\xd7\x9c\xd7\x9e\xd7\x99\xd7\xa6\xd7\x95\xd7\x99\x20\xd7\x94\xd7\xa4\xd7\x95\xd7\x98\xd7\xa0\xd7\xa6\xd7\x99\xd7\x90\xd7\x9c\x20\xd7\xa9\xd7\x9c\xd7\x94\xe2\x80\xa6",
	"Arabic:  \xd9\x84\xd8\xa5\xd9\x8a\xd8\xb5\xd8\xa7\xd9\x84\x20\xd8\xa7\xd9\x84\xd8\xb4\xd8\xa8\xd9\x83\xd8\xa9\x20\xd8\xa7\xd9\x84\xd9\x85\xd8\xb9\xd9\x84\xd9\x88\xd9\x85\xd8\xa7\xd8\xaa\xd9\x8a\xd8\xa9\x20\xd8\xa5\xd9\x84\xd9\x89\xd8\xa3\xd9\x82\xd8\xb5\xd9\x89\x20\xd8\xa5\xd9\x85\xd9\x83\xd8\xa7\xd9\x86\xd9\x8a\xd8\xa7\xd8\xaa\xd9\x87\xd8\xa7\xe2\x80\xa6",
	"Chinese:  \xe5\xbc\x95\xe7\x99\xbc\xe7\xb6\xb2\xe7\xb5\xa1\xe7\x9a\x84\xe5\x85\xa8\xe9\x83\xa8\xe6\xbd\x9b\xe8\x83\xbd\xe2\x8b\xae",
	"Japanese:  Web\xe3\x81\xae\xe5\x8f\xaf\xe8\x83\xbd\xe6\x80\xa7\xe3\x82\x92\xe6\x9c\x80\xe5\xa4\xa7\xe9\x99\x90\xe3\x81\xab\xe5\xb0\x8e\xe3\x81\x8d\xe5\x87\xba\xe3\x81\x99\xe3\x81\x9f\xe3\x82\x81\xe3\x81\xab\xe2\x8b\xae",
};

static void getUtf8InputStrings(Utf8InputList& list)
{
	while (!cin.eof())
	{
		string line;
		getline(cin, line);
		if (line.length() > 0)
		{
			list.push_back(line);
		}
	}
}

static Utf16String utf8ToUtf16(const string& utf8Str)
{
	const char* pUtf8Str = utf8Str.c_str();

	int length = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, pUtf8Str, -1, nullptr, 0);
	if (length == 0)
	{
		auto errMsg = str(format{"MultiByteToWideChar failure #1 0x%1$08x"} % ::GetLastError());
		throw exception(errMsg.c_str());
	}

	vector<Utf16Char> buffer(length);
	if (MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, pUtf8Str, -1,
		reinterpret_cast<LPWSTR>(&buffer[0]), length) == 0)
	{
		auto errMsg = str(format{"MultiByteToWideChar failure #2 0x%1$08x"} % ::GetLastError());
		throw exception(errMsg.c_str());
	}

	return &buffer[0];
}

static void genTestCaseFromUtf8String(size_t testCaseNum, const string& utf8Str)
{
	const Utf16String utf16Str = utf8ToUtf16(utf8Str);

	cout << dec << setfill('0');
	cout << "// " << utf8Str << "\nstatic const char k_testCase" << setw(2)
		<< testCaseNum << "Utf8Input[] =\n\t{ ";
	cout << hex << setfill('0');
	for (size_t i = 0; i < utf8Str.length(); ++i)
	{
		if (i > 0)
		{
			cout << (i % 10 == 0 ? ",\n\t\t" : ", ");
		}
		cout << "'\\x" << setw(2) << static_cast<unsigned>(static_cast<unsigned char>(utf8Str[i])) << "'";
	}
	cout << dec << setfill('0');
	cout << ", '\\x00' };\nstatic const Utf16Char k_testCase" << setw(2)
		<< testCaseNum << "Utf16Input[] =\n\t{ ";
	cout << hex << setfill('0');
	for (size_t i = 0; i < utf16Str.length(); ++i)
	{
		if (i > 0)
		{
			cout << (i % 10 == 0 ? ",\n\t\t" : ", ");
		}
		cout << "0x" << setw(4) << static_cast<unsigned short>(utf16Str[i]);
	}
	cout << dec << setfill('0');
	cout << ", 0x0000 };\nstatic const Utf32Char k_testCase" << setw(2) << testCaseNum << "ExpectedResult[] =\n\t{ ";
	cout << hex << setfill('0');
	int numChars = -1;
	for (auto i = cbegin(utf16Str); i != cend(utf16Str); ++i)
	{
		Utf32Char c = 0;

		if (i == cbegin(utf16Str) && (*i == 0xfeff || *i == 0xfffe))
		{
			continue;	// skip the BOM
		}
		else if (0xdc00 <= *i && *i <= 0xdfff)
		{
			throw exception("Encountered unmatched second half of surrogate pair");
		}
		else if (0xd800 <= *i && *i <= 0xdbff)
		{
			Utf32Char leadingSurrogate = *i;
			++i;
			if (i == cend(utf16Str))
			{
				throw exception("Encountered first half of surrogate pair at end of string");
			}
			else if (*i < 0xdc00 || 0xdfff < *i)
			{
				throw exception("Encountered unmatched first half of surrogate pair");
			}
			else
			{
				Utf32Char trailingSurrogate = *i;
				c = (0x3ff & trailingSurrogate)
					| ((0x3f & leadingSurrogate) << 10)
					| ((((leadingSurrogate >> 6) & 0xf) + 1) << 16);
				++numChars;
			}
		}
		else
		{
			c = *i;
			++numChars;
		}

		if (numChars > 0)
		{
			cout << (numChars % 8 == 0
				? ",\n\t\t"
				: ", ");
		}
		cout << "0x" << setw(6) << c;
	}

	cout << ", 0x000000 };\n";

	cout << setfill(' ') << dec;
}

int main()
{
	try
	{
		if (numeric_limits<Utf16Char>::digits != 16)
		{
			throw exception("The type 'Utf16Char' is not 16 bits!");
		}

		if (numeric_limits<Utf32Char>::digits != 32)
		{
			throw exception("The type 'Utf32Char' is not 32 bits!");
		}

		Utf8InputList utf8InputList;
		getUtf8InputStrings(utf8InputList);

		for (size_t i = 0; i < utf8InputList.size(); ++i)
		{
			if (i > 0)
			{
				cout << endl;
			}
			genTestCaseFromUtf8String(i, utf8InputList[i]);
		}

		cout << "\nstatic const TestCase k_testData[] =\n\t{\n";
		cout << setfill('0');
		for (size_t i = 0; i < utf8InputList.size(); ++i)
		{
			cout << "\t\t{ k_testCase" << setw(2) << i << "Utf8Input, k_testCase"
				<< setw(2) << i << "Utf16Input, k_testCase" << setw(2) << i << "ExpectedResult },\n";
		}
		cout << setfill(' ');
		cout << "\t};\n";
	}
	catch (const exception& e)
	{
		cout << "Exception:  " << e.what() << endl;
	}
	return 0;
}

#endif // _WIN32
