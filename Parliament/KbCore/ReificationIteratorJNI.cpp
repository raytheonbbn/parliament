// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/generated/com_bbn_parliament_jni_ReificationIterator.h"
#include "parliament/Platform.h"
#include "parliament/KbInstance.h"
#include "parliament/JNIHelper.h"
#include "parliament/ReificationIterator.h"

using namespace ::bbn::parliament;

JNIEXPORT void JNICALL Java_com_bbn_parliament_jni_ReificationIterator_dispose(
	JNIEnv* pEnv, jobject /* obj */, jlong iterPtr)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		ReificationIterator* pIter = reinterpret_cast<ReificationIterator*>(
			static_cast<intPtr>(iterPtr));
		delete pIter;
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT jboolean JNICALL Java_com_bbn_parliament_jni_ReificationIterator_hasNext(
	JNIEnv* pEnv, jobject /* obj */, jlong iterPtr)
{
	jboolean result = false;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
	ReificationIterator* pIter = reinterpret_cast<ReificationIterator*>(
		static_cast<intPtr>(iterPtr));
	result = !pIter->isEnd();
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jobject JNICALL Java_com_bbn_parliament_jni_ReificationIterator_nextReification(
	JNIEnv* pEnv, jobject /* obj */, jlong iterPtr)
{
	jobject result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		ReificationIterator* pIter = reinterpret_cast<ReificationIterator*>(
			static_cast<intPtr>(iterPtr));
		if (pIter->isEnd())
		{
			JNIHelper::throwJavaException(pEnv,
				"java/util/NoSuchElementException",
				"Iteration has no more elements");
		}
		ResourceId statementName = (*pIter)->first;
		ResourceId subject = pIter->subjectId();
		ResourceId predicate = pIter->predicateId();
		ResourceId object = pIter->objectId();
		bool isLiteral = pIter->isLiteral();
		result = JNIHelper::newObject(pEnv,
					JNIHelper::findClass(pEnv, "com/bbn/parliament/jni/ReificationIterator$Reification"),
					"(JJJJZ)V", static_cast<uint64>(statementName), static_cast<uint64>(subject),
					static_cast<uint64>(predicate), static_cast<uint64>(object), isLiteral);
		++*pIter;
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}
