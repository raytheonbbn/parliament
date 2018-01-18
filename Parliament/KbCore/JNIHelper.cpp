// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/JNIHelper.h"

#include <cstdarg>
#include <sstream>

namespace pmnt = ::bbn::parliament;

using ::std::basic_string;
using ::std::ostringstream;
using ::std::size_t;
using ::std::string;

jclass pmnt::JNIHelper::findClass(JNIEnv* pEnv, const char* pClassName)
{
	jclass cls = pEnv->FindClass(pClassName);
	if (cls == 0)
	{
		throw JavaException();
	}
	return cls;
}

jclass pmnt::JNIHelper::getClassId(JNIEnv* pEnv, jobject obj)
{
	jclass cls = pEnv->GetObjectClass(obj);
	if (cls == 0)
	{
		throw JavaException();
	}
	return cls;
}

jfieldID pmnt::JNIHelper::getStaticFieldId(JNIEnv* pEnv, jclass cls,
	const char* pFldName, const char* pSignature)
{
	jfieldID fid = pEnv->GetStaticFieldID(cls, pFldName, pSignature);
	if (fid == 0)
	{
		throw JavaException();
	}

	return fid;
}

jfieldID pmnt::JNIHelper::getFieldId(JNIEnv* pEnv, jclass cls,
	const char* pFldName, const char* pSignature)
{
	jfieldID fid = pEnv->GetFieldID(cls, pFldName, pSignature);
	if (fid == 0)
	{
		throw JavaException();
	}

	return fid;
}

jfieldID pmnt::JNIHelper::getFieldId(JNIEnv* pEnv, jobject obj,
	const char* pFldName, const char* pSignature)
{
	jclass cls = getClassId(pEnv, obj);
	return getFieldId(pEnv, cls, pFldName, pSignature);
}

jmethodID pmnt::JNIHelper::getMethodID(JNIEnv* pEnv, jobject obj,
	const char* pName, const char* pSigniture)
{
	jclass cls = getClassId(pEnv, obj);
	return getMethodID(pEnv, cls, pName, pSigniture);
}

jmethodID pmnt::JNIHelper::getMethodID(JNIEnv* pEnv, jclass cls,
	const char* pName, const char* pSigniture)
{
	jmethodID result = pEnv->GetMethodID(cls, pName, pSigniture);
	if (result == 0)
	{
		throw JavaException();
	}

	return result;
}

pmnt::int64 pmnt::JNIHelper::getLongFld(JNIEnv* pEnv, jobject obj, const char* pFldName)
{
	jfieldID fid = getFieldId(pEnv, obj, pFldName, "J");
	return pEnv->GetLongField(obj, fid);
}

void pmnt::JNIHelper::setLongFld(JNIEnv* pEnv, jobject obj, const char* pFldName, int64 value)
{
	jfieldID fid = getFieldId(pEnv, obj, pFldName, "J");
	pEnv->SetLongField(obj, fid, value);
}

string pmnt::JNIHelper::getStringFld(JNIEnv* pEnv, jobject obj, const char* pFldName)
{
	jfieldID fid = getFieldId(pEnv, obj, pFldName, "Ljava/lang/String;");
	jstring jstr = static_cast<jstring>(pEnv->GetObjectField(obj, fid));
	return jstringToCstring<char>(pEnv, jstr);
}

bool pmnt::JNIHelper::getBooleanFld(JNIEnv* pEnv, jobject obj, const char* pFldName)
{
	jfieldID fid = getFieldId(pEnv, obj, pFldName, "Z");
	return !!pEnv->GetBooleanField(obj, fid);
}

void pmnt::JNIHelper::setBooleanFld(JNIEnv* pEnv, jobject obj, const char* pFldName, bool value)
{
	jfieldID fid = getFieldId(pEnv, obj, pFldName, "Z");
	pEnv->SetBooleanField(obj, fid, value);
}

double pmnt::JNIHelper::getDoubleFld(JNIEnv* pEnv, jobject obj, const char* pFldName)
{
	jfieldID fid = getFieldId(pEnv, obj, pFldName, "D");
	return pEnv->GetDoubleField(obj, fid);
}

void pmnt::JNIHelper::setDoubleFld(JNIEnv* pEnv, jobject obj, const char* pFldName, double value)
{
	jfieldID fid = getFieldId(pEnv, obj, pFldName, "D");
	pEnv->SetDoubleField(obj, fid, value);
}

pmnt::int64 pmnt::JNIHelper::getStaticLongFld(JNIEnv* pEnv, jclass cls, const char* pFldName)
{
	jfieldID fid = getStaticFieldId(pEnv, cls, pFldName, "J");
	return pEnv->GetStaticLongField(cls, fid);
}

void pmnt::JNIHelper::setStaticLongFld(JNIEnv* pEnv, jclass cls,
	const char* pFldName, int64 value)
{
	jfieldID fid = getStaticFieldId(pEnv, cls, pFldName, "J");
	pEnv->SetStaticLongField(cls, fid, value);
}

pmnt::int32 pmnt::JNIHelper::getStaticIntFld(JNIEnv* pEnv, jclass cls, const char* pFldName)
{
	jfieldID fid = getStaticFieldId(pEnv, cls, pFldName, "I");
	return pEnv->GetStaticIntField(cls, fid);
}

void pmnt::JNIHelper::setStaticIntFld(JNIEnv* pEnv, jclass cls,
	const char* pFldName, int32 value)
{
	jfieldID fid = getStaticFieldId(pEnv, cls, pFldName, "I");
	pEnv->SetStaticIntField(cls, fid, value);
}

pmnt::int16 pmnt::JNIHelper::getStaticShortFld(JNIEnv* pEnv, jclass cls, const char* pFldName)
{
	jfieldID fid = getStaticFieldId(pEnv, cls, pFldName, "S");
	return pEnv->GetStaticShortField(cls, fid);
}

void pmnt::JNIHelper::setStaticShortFld(JNIEnv* pEnv, jclass cls,
	const char* pFldName, int16 value)
{
	jfieldID fid = getStaticFieldId(pEnv, cls, pFldName, "S");
	pEnv->SetStaticShortField(cls, fid, value);
}

void pmnt::JNIHelper::throwException(JNIEnv* pEnv, const char* pExType,
	const char* pMsg, const char* pSrcFile, uint32 srcLineNum)
{
	ostringstream s;
	s << pExType << " thrown from " << pSrcFile << " at line "
		<< srcLineNum << ":  " << pMsg;
	throwJavaException(pEnv, "com/bbn/parliament/jni/NativeCodeException",
		s.str().c_str());
}

void pmnt::JNIHelper::throwJavaException(JNIEnv* pEnv,
	const char* pClassName, const char* pMsg)
{
	if (pEnv->ThrowNew(findClass(pEnv, pClassName), pMsg) != 0)
	{
		throw JavaException();
	}
}

jobject pmnt::JNIHelper::newObject(JNIEnv* pEnv, jclass cls, const char* pCtorSignature, ...)
{
	jmethodID methodId = pEnv->GetMethodID(cls, "<init>", pCtorSignature);
	if (methodId == 0)
	{
		throw JavaException();
	}

	va_list argList;
	va_start(argList, pCtorSignature);
	jobject result = pEnv->NewObjectV(cls, methodId, argList);
	va_end(argList);

	if (result == 0)
	{
		throw JavaException();
	}

	return result;
}

jobject pmnt::JNIHelper::newObjectByDefaultCtor(JNIEnv* pEnv, jclass cls)
{
	return newObject(pEnv, cls, "()V");
}

jobject pmnt::JNIHelper::newObjectByDefaultCtor(JNIEnv* pEnv, const char* pClassName)
{
	return newObjectByDefaultCtor(pEnv, findClass(pEnv, pClassName));
}

// =============== JavaPrintStreamBuf ===============

pmnt::JavaPrintStreamBuf::JavaPrintStreamBuf(JNIEnv* pEnv, jobject printStream) :
	m_pEnv(pEnv),
	m_printStream(printStream),
	m_methodId(JNIHelper::getMethodID(m_pEnv, m_printStream, "print", "(Ljava/lang/String;)V")),
	m_buffer(k_bufSize)
{
	char_type* pBuffer = &m_buffer[0];
	setp(pBuffer, pBuffer + k_bufSize - 1);
}

pmnt::JavaPrintStreamBuf::~JavaPrintStreamBuf()
{
	sync();
}

pmnt::JavaPrintStreamBuf::int_type pmnt::JavaPrintStreamBuf::overflow(int_type ch)
{
	if (!traits_type::eq_int_type(ch, traits_type::eof()))
	{
		*pptr() = static_cast<char_type>(ch);
		pbump(1);
	}
	return (sync() == 0)
		? traits_type::not_eof(ch)
		: traits_type::eof();
}

int pmnt::JavaPrintStreamBuf::sync()
{
	int result = -1;

	ptrdiff_t numCharsToWrite = pptr() - pbase();

	basic_string<char_type> bufAsStr(&m_buffer[0], numCharsToWrite);
	jstring bufAsJStr = JNIHelper::cstringToJstring(m_pEnv, bufAsStr.c_str());

	// void java.io.PrintStream.print(String str)
	m_pEnv->CallVoidMethod(m_printStream, m_methodId, bufAsJStr);
	m_pEnv->DeleteLocalRef(bufAsJStr);

	if (m_pEnv->ExceptionOccurred() == nullptr)
	{
		pbump(static_cast<int>(-numCharsToWrite));
		result = 0;
	}
	else
	{
		m_pEnv->ExceptionClear();
	}
	return result;
}

// =============== JByteArray ===============

jbyteArray pmnt::JByteArray::createGlobalByteArrayRef(JNIEnv* pEnv, ::std::size_t length)
{
	jbyteArray array = pEnv->NewByteArray(static_cast<jsize>(length));
	if (array == nullptr)
	{
		throw JavaException();
	}
	jobject globalRef = pEnv->NewGlobalRef(array);
	if (globalRef == nullptr)
	{
		throw JavaException();
	}
	return reinterpret_cast<jbyteArray>(globalRef);
}

pmnt::JByteArray::JByteArray(JNIEnv* pEnv, ::std::size_t length) :
	m_pEnv(pEnv),
	m_array(createGlobalByteArrayRef(m_pEnv, length))
{
}

pmnt::JByteArray::~JByteArray()
{
	if (m_array != nullptr)
	{
		m_pEnv->DeleteGlobalRef(m_array);
	}
}

// =============== JavaOutputStreamBuf ===============

pmnt::JavaOutputStreamBuf::JavaOutputStreamBuf(JNIEnv* pEnv, jobject outputStream, size_t bufSize) :
	m_pEnv(pEnv),
	m_outputStream(outputStream),
	m_methodId(JNIHelper::getMethodID(m_pEnv, m_outputStream, "write", "([BII)V")),
	m_buffer(bufSize),
	m_javaBuffer(m_pEnv, bufSize)
{
	char_type* pBuffer = &m_buffer[0];
	setp(pBuffer, pBuffer + bufSize - 1);
}

pmnt::JavaOutputStreamBuf::~JavaOutputStreamBuf()
{
	sync();
}

pmnt::JavaOutputStreamBuf::int_type pmnt::JavaOutputStreamBuf::overflow(int_type ch)
{
	if (!traits_type::eq_int_type(ch, traits_type::eof()))
	{
		*pptr() = static_cast<char_type>(ch);
		pbump(1);
	}
	return (sync() == 0)
		? traits_type::not_eof(ch)
		: traits_type::eof();
}

int pmnt::JavaOutputStreamBuf::sync()
{
	static_assert(sizeof(jbyte) == sizeof(char_type),
		"JavaOutputStreamBuf assumes UTF-8 character encoding");

	int result = -1;

	ptrdiff_t numBytesToWrite = pptr() - pbase();
	jsize jnumBytesToWrite = static_cast<jsize>(numBytesToWrite);

	m_pEnv->SetByteArrayRegion(m_javaBuffer.get(), 0, jnumBytesToWrite,
		reinterpret_cast<const jbyte*>(&m_buffer[0]));

	// void java.io.OutputStream.write(byte[] b, int off, int len)
	m_pEnv->CallVoidMethod(m_outputStream, m_methodId, m_javaBuffer.get(),
		0, jnumBytesToWrite);

	if (m_pEnv->ExceptionOccurred() == nullptr)
	{
		pbump(static_cast<int>(-numBytesToWrite));
		result = 0;
	}
	else
	{
		throw JavaException();
	}
	return result;
}
