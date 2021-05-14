// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/generated/com_bbn_parliament_jni_StmtIterator.h"
#include "parliament/Platform.h"
#include "parliament/KbInstance.h"
#include "parliament/JNIHelper.h"
#include "parliament/Log.h"
#include "parliament/StmtIterator.h"

using namespace ::bbn::parliament;
namespace pmnt = ::bbn::parliament;

static auto g_log(pmnt::log::getSource("StmtIteratorJNI"));

JNIEXPORT void JNICALL Java_com_bbn_parliament_jni_StmtIterator_dispose(
	JNIEnv* pEnv, jobject /* obj */, jlong iterPtr)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		StmtIterator* pIter = reinterpret_cast<StmtIterator*>(
			static_cast<intPtr>(iterPtr));
		delete pIter;
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT jboolean JNICALL Java_com_bbn_parliament_jni_StmtIterator_hasNext(
	JNIEnv* pEnv, jobject /* obj */, jlong iterPtr)
{
	jboolean result = false;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		StmtIterator* pIter = reinterpret_cast<StmtIterator*>(
			static_cast<intPtr>(iterPtr));
		result = !pIter->isEnd();
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jobject JNICALL Java_com_bbn_parliament_jni_StmtIterator_nextStatement(
	JNIEnv* pEnv, jobject /* obj */, jlong iterPtr)
{
	jobject result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		StmtIterator* pIter = reinterpret_cast<StmtIterator*>(
			static_cast<intPtr>(iterPtr));
		if (pIter->isEnd())
		{
			JNIHelper::throwJavaException(pEnv,
				"java/util/NoSuchElementException",
				"Iteration has no more elements");
		}
		const Statement& stmt = pIter->statementRef();
		result = JNIHelper::newObject(pEnv,
			JNIHelper::findClass(pEnv, "com/bbn/parliament/jni/StmtIterator$Statement"),
			"(JJJZZZ)V",
			static_cast<uint64>(stmt.getSubjectId()),
			static_cast<uint64>(stmt.getPredicateId()),
			static_cast<uint64>(stmt.getObjectId()),
			stmt.isLiteral(),
			stmt.isDeleted(),
			stmt.isInferred());
		++*pIter;
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}
