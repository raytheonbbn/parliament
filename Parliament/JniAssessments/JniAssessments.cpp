// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "com_bbn_parliament_core_jni_JniAssessments.h"
#include "CppTestClass.h"
#include <iostream>
#include <iomanip>
#include <limits>
#include <map>
#include <stdexcept>
#include <sstream>
#include <stdexcept>
#include "parliament/Types.h"

using namespace ::std;
using namespace ::bbn::parliament;

//static void checkpoint(const char* pFile, int lineNum, const char* pExtra)
//{
//	cout << "Checkpoint in " << pFile << " at line " << lineNum
//		<< " with extra info: " << pExtra << endl;
//}

#define CHECKPOINT(extra) checkpoint(__FILE__, __LINE__, extra)

static void assertTrue(const char* pFile, int lineNum, bool condition, const char* pExtra)
{
	if (!condition)
	{
		cout << "Assertion failure in " << pFile << " at line " << lineNum
			<< endl << "Extra info: " << pExtra << endl;
	}
}

#define ASSERT_TRUE(condition) assertTrue(__FILE__, __LINE__, condition, "none")
#define ASSERT_TRUE_2(condition, extra) assertTrue(__FILE__, __LINE__, condition, extra)

// =================================================================

static JNIEnv* g_pEnv;

class JObjectLess
{
public:
	bool operator()(jobject lhs, jobject rhs) const
	{
		ASSERT_TRUE(g_pEnv != 0);
		ASSERT_TRUE(lhs != 0);
		ASSERT_TRUE(rhs != 0);
		return (g_pEnv->IsSameObject(lhs, rhs))
			? false
			: lhs < rhs;
	}
};

static map<jobject, CppTestClass*, JObjectLess> g_map;

static jclass findClass(JNIEnv* pEnv, const char* pClassName)
{
	ASSERT_TRUE(pEnv != 0);

	jclass cls = pEnv->FindClass(pClassName);
	ASSERT_TRUE_2(cls != 0, pClassName);
	//if (cls == 0)
	//{
	//	throw JavaException();
	//}
	return cls;
}

jobject newObject(JNIEnv* pEnv, jclass cls, const char* pCtorSignature, ...)
{
	ASSERT_TRUE(pEnv != 0);
	ASSERT_TRUE(cls != 0);

	jmethodID methodId = pEnv->GetMethodID(cls, "<init>", pCtorSignature);
	ASSERT_TRUE(methodId != 0);
	//if (methodId == 0)
	//{
	//	throw JavaException();
	//}

	va_list argList;
	va_start(argList, pCtorSignature);
	jobject result = pEnv->NewObjectV(cls, methodId, argList);
	va_end(argList);

	ASSERT_TRUE(result != 0);
	//if (result == 0)
	//{
	//	throw JavaException();
	//}

	return result;
}

static jclass getClassId(JNIEnv* pEnv, jobject obj)
{
	ASSERT_TRUE(pEnv != 0);
	ASSERT_TRUE(obj != 0);

	jclass cls = pEnv->GetObjectClass(obj);
	ASSERT_TRUE(cls != 0);
	//if (cls == 0)
	//{
	//	throw JavaException();
	//}
	return cls;
}

static jfieldID getFieldId(JNIEnv* pEnv, jobject obj,
	const char* pFldName, const char* pSignature)
{
	ASSERT_TRUE(pEnv != 0);
	ASSERT_TRUE(obj != 0);

	jclass cls = getClassId(pEnv, obj);
	jfieldID fid = pEnv->GetFieldID(cls, pFldName, pSignature);
	ASSERT_TRUE(fid != 0);
	//if (fid == 0)
	//{
	//	throw JavaException();
	//}

	return fid;
}

static inline CppTestClass* testObjPtr(JNIEnv* pEnv, jobject obj)
{
	static jfieldID g_fid = 0;

	ASSERT_TRUE(pEnv != 0);
	ASSERT_TRUE(obj != 0);

	if (g_fid == 0)
	{
		g_fid = getFieldId(pEnv, obj, "_pObj", "J");
	}
	return reinterpret_cast<CppTestClass*>(static_cast<intPtr>(pEnv->GetLongField(obj, g_fid)));
}

// ==================================================

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* /* pVM */, void* /* pReserved */)
{
	if (numeric_limits<Utf16Char>::digits != 16)
	{
		throw runtime_error("The type 'Utf16Char' is not 16 bits!");
	}
	return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* /* pVM */, void* /* pReserved */)
{
}

template <typename t_char>
static string printOctets(const t_char* pStr, size_t strLen)
{
	const size_t charWidth = sizeof(t_char);
	ostringstream strm;
	strm << hex << setfill('0');
	for (size_t i = 0; i < strLen + 1; ++i, ++pStr)
	{
		strm << ' ' << setw(charWidth * 2) << char_traits<t_char>::to_int_type(*pStr);
	}
	return strm.str();
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_JniAssessments_printJniStringAsHex(
	JNIEnv* pEnv, jclass cls, jstring str)
{
	jboolean isCopy = JNI_FALSE;
	size_t strLen = static_cast<size_t>(pEnv->GetStringLength(str));

	{
		const char* pStr = pEnv->GetStringUTFChars(str, &isCopy);
		cout << "   GetStringUTFChars:" << printOctets(pStr, strLen) << endl;
		pEnv->ReleaseStringUTFChars(str, pStr);
	}

	{
		const Utf16Char* pWStr = reinterpret_cast<const Utf16Char*>(pEnv->GetStringChars(str, &isCopy));
		cout << "   GetStringChars:   " << printOctets(pWStr, strLen) << endl;
		pEnv->ReleaseStringChars(str, reinterpret_cast<const jchar*>(pWStr));
	}

	{
		const Utf16Char* pWStr = reinterpret_cast<const Utf16Char*>(pEnv->GetStringCritical(str, &isCopy));
		cout << "   GetStringCritical:" << printOctets(pWStr, strLen) << endl;
		pEnv->ReleaseStringCritical(str, reinterpret_cast<const jchar*>(pWStr));
	}
}

JNIEXPORT jboolean JNICALL Java_com_bbn_parliament_core_jni_JniAssessments_testJniStringEncoding(
	JNIEnv* pEnv, jclass cls, jstring str, jboolean testCriticalStrFunctions, jboolean testWideCharEncoding)
{
	jboolean isCopy = JNI_FALSE;
	if (testCriticalStrFunctions == JNI_TRUE)
	{
		const Utf16Char* pWStr = reinterpret_cast<const Utf16Char*>(pEnv->GetStringCritical(str, &isCopy));
		pEnv->ReleaseStringCritical(str, reinterpret_cast<const jchar*>(pWStr));
	}
	else if (testWideCharEncoding == JNI_TRUE)
	{
		const Utf16Char* pWStr = reinterpret_cast<const Utf16Char*>(pEnv->GetStringChars(str, &isCopy));
		pEnv->ReleaseStringChars(str, reinterpret_cast<const jchar*>(pWStr));
	}
	else
	{
		const char* pStr = pEnv->GetStringUTFChars(str, &isCopy);
		pEnv->ReleaseStringUTFChars(str, pStr);
	}
	return isCopy;
}

template<typename t_char>
void testJniStringCreation(JNIEnv* pEnv, int i, const t_char* pUriPrefix)
{
#if 0
	basic_ostringstream<t_char> s;
	s << pUriPrefix << i;
	basic_string<t_char> str = s.str();
	jstring jstr = (sizeof(t_char) == sizeof(jchar))
		? pEnv->NewString(reinterpret_cast<const jchar*>(str.c_str()),
			static_cast<jsize>(str.length()))
		: pEnv->NewStringUTF(reinterpret_cast<const char*>(str.c_str()));
#else
	jstring jstr = (sizeof(t_char) == sizeof(jchar))
		? pEnv->NewString(reinterpret_cast<const jchar*>(pUriPrefix),
		static_cast<jsize>(char_traits<t_char>::length(pUriPrefix)))
		: pEnv->NewStringUTF(reinterpret_cast<const char*>(pUriPrefix));
#endif
	pEnv->DeleteLocalRef(jstr);
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_JniAssessments_testJniStringCreation(
	JNIEnv* pEnv, jclass cls, jint numIters, jboolean useUtf16Chars)
{
	static const char		uriPrefixA[] = "http://example.org/item#";
	static const Utf16Char	uriPrefixW[] = { 'h', 't', 't', 'p', ':', '/', '/',
		'e', 'x', 'a', 'm', 'p', 'l', 'e', '.', 'o', 'r', 'g', '/', 'i', 't', 'e', 'm', '#' };

	if (useUtf16Chars == JNI_TRUE)
	{
		for (jint i = 0; i < numIters; ++i)
		{
			testJniStringCreation(pEnv, i, uriPrefixW);
		}
	}
	else
	{
		for (jint i = 0; i < numIters; ++i)
		{
			testJniStringCreation(pEnv, i, uriPrefixA);
		}
	}
}

JNIEXPORT jobject JNICALL Java_com_bbn_parliament_core_jni_JniAssessments_create(
	JNIEnv* pEnv, jclass cls)
{
	CppTestClass* pCppTestObj = new CppTestClass();
	jclass clsToAlloc = findClass(pEnv, "com/bbn/parliament/core/jni/JniAssessments");
	jobject pResult = newObject(pEnv, clsToAlloc, "(J)V",
		static_cast<uint64>(reinterpret_cast<uintPtr>(pCppTestObj)));
	g_pEnv = pEnv;
	g_map.insert(make_pair(pEnv->NewGlobalRef(pResult), pCppTestObj));
	return pResult;
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_JniAssessments_dispose(
	JNIEnv* pEnv, jclass cls, jlong nativeObj)
{
	ASSERT_TRUE(pEnv != 0);
	ASSERT_TRUE(cls != 0);
	ASSERT_TRUE(nativeObj != 0);

	jobject theGlobalRef = 0;
	CppTestClass* pTheObj = nullptr;
	g_pEnv = pEnv;
	for (auto it = begin(g_map); it != end(g_map); ++it)
	{
		auto [globalRef, pObj] = *it;
		if (static_cast<jlong>(reinterpret_cast<intPtr>(pObj)) == nativeObj)
		{
			theGlobalRef = globalRef;
			pTheObj = pObj;
			break;
		}
	}
	g_map.erase(theGlobalRef);
	pEnv->DeleteGlobalRef(theGlobalRef);
	delete pTheObj;
}

JNIEXPORT jdouble JNICALL Java_com_bbn_parliament_core_jni_JniAssessments_testMethod1(
	JNIEnv* pEnv, jobject obj, jdouble d)
{
	CppTestClass* pObj = testObjPtr(pEnv, obj);
	return pObj->accumulate(d);
}

JNIEXPORT jdouble JNICALL Java_com_bbn_parliament_core_jni_JniAssessments_internalTestMethod2(
	JNIEnv* pEnv, jobject obj, jlong objPtr, jdouble d)
{
	CppTestClass* pObj = reinterpret_cast<CppTestClass*>(static_cast<intPtr>(objPtr));
	return pObj->accumulate(d);
}

JNIEXPORT jdouble JNICALL Java_com_bbn_parliament_core_jni_JniAssessments_testMethod3(
	JNIEnv* pEnv, jobject obj, jdouble d)
{
	g_pEnv = pEnv;
	auto it = g_map.find(obj);
	if (it == end(g_map))
	{
		throw invalid_argument("Unable to find object in map");
	}
	auto [ignore, pObj] = *it;
	return pObj->accumulate(d);
}
