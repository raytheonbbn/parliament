// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "com_bbn_parliament_jni_JniAssessments.h"
#include "CppTestClass.h"
#include <functional>
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

// =================================================================

class JObjectLess : public binary_function<jobject, jobject, bool>
{
public:
	JObjectLess() : m_pEnv(nullptr) {}

	void setEnv(JNIEnv* pEnv) { m_pEnv = pEnv; }

	// functor for operator<
	bool operator()(jobject lhs, jobject rhs) const
	{
		return (m_pEnv->IsSameObject(lhs, rhs))
			? false
			: lhs < rhs;
	}

private:
	JNIEnv* m_pEnv;
};

static JObjectLess g_jObjLess;
static map<jobject, CppTestClass*, JObjectLess> g_map(g_jObjLess);

static jclass findClass(JNIEnv* pEnv, const char* pClassName)
{
	jclass cls = pEnv->FindClass(pClassName);
	//if (cls == 0)
	//{
	//	throw JavaException();
	//}
	return cls;
}

jobject newObject(JNIEnv* pEnv, jclass cls, const char* pCtorSignature, ...)
{
	jmethodID methodId = pEnv->GetMethodID(cls, "<init>", pCtorSignature);
	//if (methodId == 0)
	//{
	//	throw JavaException();
	//}

	va_list argList;
	va_start(argList, pCtorSignature);
	jobject result = pEnv->NewObjectV(cls, methodId, argList);
	va_end(argList);

	//if (result == 0)
	//{
	//	throw JavaException();
	//}

	return result;
}

static jclass getClassId(JNIEnv* pEnv, jobject obj)
{
	jclass cls = pEnv->GetObjectClass(obj);
	//if (cls == 0)
	//{
	//	throw JavaException();
	//}
	return cls;
}

static jfieldID getFieldId(JNIEnv* pEnv, jobject obj,
	const char* pFldName, const char* pSignature)
{
	jclass cls = getClassId(pEnv, obj);
	jfieldID fid = pEnv->GetFieldID(cls, pFldName, pSignature);
	//if (fid == 0)
	//{
	//	throw JavaException();
	//}

	return fid;
}

static inline CppTestClass* testObjPtr(JNIEnv* pEnv, jobject obj)
{
	static jfieldID g_fid = 0;

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

JNIEXPORT void JNICALL Java_JniAssessments_printJniStringAsHex(JNIEnv* pEnv,
	jclass cls, jstring str)
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

JNIEXPORT jboolean JNICALL Java_JniAssessments_testJniStringEncoding(JNIEnv* pEnv,
	jclass cls, jstring str, jboolean testCriticalStrFunctions, jboolean testWideCharEncoding)
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

JNIEXPORT void JNICALL Java_JniAssessments_testJniStringCreation(JNIEnv* pEnv,
	jclass cls, jint numIters, jboolean useUtf16Chars)
{
	static const char			uriPrefixA[] = "http://example.org/item#";
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

JNIEXPORT jobject JNICALL Java_JniAssessments_create(JNIEnv* pEnv, jclass cls)
{
	CppTestClass* pCppTestObj = new CppTestClass();
	jclass clsToAlloc = findClass(pEnv, "JniAssessments");
	jobject pResult = newObject(pEnv, clsToAlloc, "(J)V",
		static_cast<uint64>(reinterpret_cast<uintPtr>(pCppTestObj)));
	g_jObjLess.setEnv(pEnv);
	g_map.insert(make_pair(pEnv->NewGlobalRef(pResult), pCppTestObj));
	return pResult;
}

JNIEXPORT void JNICALL Java_JniAssessments_dispose(JNIEnv* pEnv, jobject obj)
{
	g_jObjLess.setEnv(pEnv);
	auto it = g_map.find(obj);
	if (it != end(g_map))
	{
		auto [globalRef, pObj] = *it;
		g_map.erase(obj);
		pEnv->DeleteGlobalRef(globalRef);
		delete pObj;
	}
}

JNIEXPORT jdouble JNICALL Java_JniAssessments_testMethod1(JNIEnv* pEnv, jobject obj, jdouble d)
{
	CppTestClass* pObj = testObjPtr(pEnv, obj);
	return pObj->accumulate(d);
}

JNIEXPORT jdouble JNICALL Java_JniAssessments_internalTestMethod2(JNIEnv* pEnv, jobject obj, jlong objPtr, jdouble d)
{
	CppTestClass* pObj = reinterpret_cast<CppTestClass*>(static_cast<intPtr>(objPtr));
	return pObj->accumulate(d);
}

JNIEXPORT jdouble JNICALL Java_JniAssessments_testMethod3(JNIEnv* pEnv, jobject obj, jdouble d)
{
	g_jObjLess.setEnv(pEnv);
	auto it = g_map.find(obj);
	if (it == end(g_map))
	{
		throw invalid_argument("Unable to find object in map");
	}
	auto [ignore, pObj] = *it;
	return pObj->accumulate(d);
}
