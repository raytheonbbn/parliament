// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#if !defined(PARLIAMENT_JNIHELPER_H_INCLUDED)
#define PARLIAMENT_JNIHELPER_H_INCLUDED

#include "parliament/Types.h"
#include "parliament/Exceptions.h"

#include <iterator>
#include <streambuf>
#include <string>
#include <vector>
#include <jni.h>

namespace bbn::parliament
{

// This exception indicates that a JNI API call (i.e., one of the JNIEnv methods) has
// failed.  This means that a Java exception is active in the JVM, so the C++ code
// should return to the JVM so that exception can propagate up the Java stack.
class JavaException {};

template<typename t_char>
class JStringAccessor
{
public:
	JStringAccessor(JNIEnv* pEnv, jstring str) :
	m_pEnv(pEnv),
	m_str(str),
	m_pStr(nullptr),
	m_size(0),
	m_isCopy(JNI_FALSE)
	{
		if (m_str != 0)
		{
			m_pStr = getStringChars(m_pEnv, m_str, m_isCopy);
			if (m_pStr == nullptr)
			{
				throw JavaException();
			}
			m_size = getStringLength();
		}
	}

	JStringAccessor(const JStringAccessor &) = delete;
	JStringAccessor &operator= (const JStringAccessor &) = delete;

	~JStringAccessor()
	{
		if (m_str != 0 && m_pStr != nullptr)
		{
			releaseStringChars(m_pEnv, m_str, m_pStr);
			m_pStr = nullptr;
		}
	}

	const t_char* begin() const
	{ return m_pStr; }

	const t_char* end() const
	{ return m_pStr + m_size; }

	size_t size() const
	{ return m_size; }

	bool isCopy() const
	{ return m_isCopy == JNI_TRUE; }

private:
	// These methods are defined via template specialization below:
	static const t_char* getStringChars(JNIEnv* pEnv, jstring str, jboolean& isCopy);
	static void releaseStringChars(JNIEnv* pEnv, jstring str, const t_char* pStr);
	jsize getStringLength() const;

	JNIEnv*			m_pEnv;
	jstring			m_str;
	const t_char*	m_pStr;
	jsize			m_size;
	jboolean		m_isCopy;
};

template<>
inline const char* JStringAccessor<char>::getStringChars(JNIEnv* pEnv, jstring str, jboolean& isCopy)
{
	return pEnv->GetStringUTFChars(str, &isCopy);
}

template<>
inline const Utf16Char* JStringAccessor<Utf16Char>::getStringChars(JNIEnv* pEnv, jstring str, jboolean& isCopy)
{
	return pEnv->GetStringCritical(str, &isCopy);
}

template<>
inline void JStringAccessor<char>::releaseStringChars(JNIEnv* pEnv, jstring str, const char* pStr)
{
	pEnv->ReleaseStringUTFChars(str, pStr);
}

template<>
inline void JStringAccessor<Utf16Char>::releaseStringChars(JNIEnv* pEnv, jstring str, const Utf16Char* pStr)
{
	pEnv->ReleaseStringCritical(str, pStr);
}

template<>
inline jsize JStringAccessor<char>::getStringLength() const
{
	return static_cast<jsize>(static_cast<ptrdiff_t>(
		::std::char_traits<char>::length(m_pStr)));
}

template<>
inline jsize JStringAccessor<Utf16Char>::getStringLength() const
{
	return m_pEnv->GetStringLength(m_str);
}

class JNIHelper
{
public:
	static jclass findClass(JNIEnv* pEnv, const char* pClassName);
	static jclass getClassId(JNIEnv* pEnv, jobject obj);
	static jfieldID getStaticFieldId(JNIEnv* pEnv, jclass cls,
		const char* pFldName, const char* pSignature);
	static jfieldID getFieldId(JNIEnv* pEnv, jclass cls,
		const char* pFldName, const char* pSignature);
	static jfieldID getFieldId(JNIEnv* pEnv, jobject obj,
		const char* pFldName, const char* pSignature);
	static jmethodID getMethodID(JNIEnv* pEnv, jobject obj,
		const char* pName, const char* pSigniture);
	static jmethodID getMethodID(JNIEnv* pEnv, jclass cls,
		const char* pName, const char* pSigniture);

	template<typename t_char>
	static ::std::basic_string<t_char> jstringToCstring(JNIEnv* pEnv, jstring str)
		{
			if (str == 0)
			{
				return ::std::basic_string<t_char>();
			}
			else
			{
				JStringAccessor<t_char> accessor(pEnv, str);
				return ::std::basic_string<t_char>(::std::begin(accessor), ::std::end(accessor));
			}
		}

	template<typename t_char>
	static jstring cstringToJstring(JNIEnv* pEnv, const t_char* pStr)
		{
			jstring result = 0;
			if (pStr != nullptr)
			{
				result = newString(pEnv, pStr);
				if (result == 0)
				{
					throw JavaException();
				}
			}
			return result;
		}
	template<typename t_char>
	static jstring cstringToJstring(JNIEnv* pEnv, const ::std::basic_string<t_char>& str)
		{ return cstringToJstring(pEnv, str.c_str()); }

	static void* getPtrFld(JNIEnv* pEnv, jobject obj, const char* pFldName)
		{ return reinterpret_cast<void*>(static_cast<intPtr>(getLongFld(pEnv, obj, pFldName))); }
	static void setPtrFld(JNIEnv* pEnv, jobject obj, const char* pFldName, void* pValue)
		{ setLongFld(pEnv, obj, pFldName, reinterpret_cast<intPtr>(pValue)); }
	static int64 getLongFld(JNIEnv* pEnv, jobject obj, const char* pFldName);
	static void setLongFld(JNIEnv* pEnv, jobject obj, const char* pFldName, int64 value);
	static ::std::string getStringFld(JNIEnv* pEnv, jobject obj, const char* pFldName);
	template<typename t_char>
	static void setStringFld(JNIEnv* pEnv, jobject obj, const char* pFldName, const t_char* pValue)
		{
			jfieldID fid = getFieldId(pEnv, obj, pFldName, "Ljava/lang/String;");
			jstring jstr = cstringToJstring(pEnv, pValue);
			pEnv->SetObjectField(obj, fid, jstr);
		}
	template<typename t_char>
	static void setStringFld(JNIEnv* pEnv, jobject obj, const char* pFldName, const ::std::basic_string<t_char>& value)
		{ setStringFld(pEnv, obj, pFldName, value.c_str()); }
	static ::std::string getTimeoutUnitFld(JNIEnv* pEnv, jobject obj);
	template<typename t_char>
	static void setTimeoutUnitFld(JNIEnv* pEnv, jobject obj, const t_char* pValue)
		{
			jmethodID mId = getMethodID(pEnv, obj, "setTimeoutUnit", "(Ljava/lang/String;)V");
			jstring jstr = cstringToJstring(pEnv, pValue);
			pEnv->CallVoidMethod(obj, mId, jstr);
		}
	static bool getBooleanFld(JNIEnv* pEnv, jobject obj, const char* pFldName);
	static void setBooleanFld(JNIEnv* pEnv, jobject obj, const char* pFldName, bool value);
	static double getDoubleFld(JNIEnv* pEnv, jobject obj, const char* pFldName);
	static void setDoubleFld(JNIEnv* pEnv, jobject obj, const char* pFldName, double value);
	static size_t getSizeTFld(JNIEnv* pEnv, jobject obj, const char* pFldName)
		{ return static_cast<size_t>(getLongFld(pEnv, obj, pFldName)); }

	static int64 getStaticLongFld(JNIEnv* pEnv, jclass cls, const char* pFldName);
	static void setStaticLongFld(JNIEnv* pEnv, jclass cls,
		const char* pFldName, int64 value);
	static int32 getStaticIntFld(JNIEnv* pEnv, jclass cls, const char* pFldName);
	static void setStaticIntFld(JNIEnv* pEnv, jclass cls,
		const char* pFldName, int32 value);
	static int16 getStaticShortFld(JNIEnv* pEnv, jclass cls, const char* pFldName);
	static void setStaticShortFld(JNIEnv* pEnv, jclass cls,
		const char* pFldName, int16 value);

	static void throwException(JNIEnv* pEnv, const ::std::exception& ex,
		const char* pSrcFile, uint32 srcLineNum);
	static void throwJavaException(JNIEnv* pEnv, const char* pClassName, const char* pMsg);

	static jobject newObject(JNIEnv* pEnv, jclass cls, const char* pCtorSignature, ...);
	static jobject newObjectByDefaultCtor(JNIEnv* pEnv, jclass cls);
	static jobject newObjectByDefaultCtor(JNIEnv* pEnv, const char* pClassName);

private:
	// This method is defined via template specialization below:
	template<typename t_char>
	static jstring newString(JNIEnv* pEnv, const t_char* pStr);
};

template<>
inline jstring JNIHelper::newString(JNIEnv* pEnv, const char* pStr)
{
	return pEnv->NewStringUTF(pStr);
}

template<>
inline jstring JNIHelper::newString(JNIEnv* pEnv, const Utf16Char* pStr)
{
	return pEnv->NewString(pStr,
		static_cast<jsize>(::std::char_traits<Utf16Char>::length(pStr)));
}

class JavaPrintStreamBuf : public ::std::streambuf
{
public:
	JavaPrintStreamBuf(JNIEnv* pEnv, jobject printStream);
	JavaPrintStreamBuf(const JavaPrintStreamBuf &) = delete;
	JavaPrintStreamBuf &operator= (const JavaPrintStreamBuf &) = delete;
	~JavaPrintStreamBuf() override;

protected:
	int_type overflow(int_type c) override;
	int sync() override;

private:
	static const size_t			k_bufSize	= 4 * 1024;

	JNIEnv*						m_pEnv;
	jobject						m_printStream;
	jmethodID					m_methodId;
	::std::vector<char_type>	m_buffer;
};

class JByteArray
{
public:
	JByteArray(JNIEnv* pEnv, ::std::size_t length);
	JByteArray(const JByteArray &) = delete;
	JByteArray &operator=(const JByteArray &) = delete;
	~JByteArray();

	jbyteArray get() const { return m_array; }

private:
	static jbyteArray createGlobalByteArrayRef(JNIEnv* pEnv, ::std::size_t length);

	JNIEnv*		m_pEnv;
	jbyteArray	m_array;
};

class JavaOutputStreamBuf : public ::std::streambuf
{
public:
	static const ::std::size_t k_bufSize = 16 * 1024;

	JavaOutputStreamBuf(JNIEnv* pEnv, jobject outputStream, ::std::size_t bufSize = k_bufSize);
	JavaOutputStreamBuf(const JavaOutputStreamBuf &) = delete;
	JavaOutputStreamBuf &operator=(const JavaOutputStreamBuf &) = delete;
	~JavaOutputStreamBuf() override;

private:
	int_type overflow(int_type ch) override;
	int sync() override;

	static jbyteArray createJavaBuffer(JNIEnv* pEnv);

	JNIEnv*						m_pEnv;
	jobject						m_outputStream;
	jmethodID					m_methodId;
	::std::vector<char_type>	m_buffer;
	JByteArray					m_javaBuffer;
};

}	// namespace end



#define BEGIN_JNI_EXCEPTION_HANDLER(pEnv)								\
	try																	\
	{																	\
		try																\
		{

#define END_JNI_EXCEPTION_HANDLER(pEnv)									\
		}																\
		catch (const ::std::exception& ex)								\
		{																\
			PMNT_LOG(g_log, log::Level::error) << typeid(ex).name()		\
				<< " caught in " << __FILE__ << " at line "				\
				<< __LINE__ << ":  " << ex.what();						\
			JNIHelper::throwException(pEnv, ex, __FILE__, __LINE__);	\
		}																\
	}																	\
	catch (const JavaException&)										\
	{																	\
		PMNT_LOG(g_log, log::Level::error)								\
			<< "JavaException caught in " << __FILE__ << " at line "	\
			<< __LINE__;												\
		/* Do nothing, so as to return to the JVM. */					\
	}

#endif // !PARLIAMENT_JNIHELPER_H_INCLUDED
