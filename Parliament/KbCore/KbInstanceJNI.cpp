// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

// Java Native Interface (JNI) to Parliament

#include "parliament/generated/com_bbn_parliament_core_jni_KbInstance.h"
#include "parliament/Platform.h"
#include "parliament/KbConfig.h"
#include "parliament/KbInstance.h"
#include "parliament/JNIHelper.h"
#include "parliament/Log.h"
#include "parliament/StmtIterator.h"
#include "parliament/ReificationIterator.h"
#include "parliament/Util.h"

#include <ostream>

using namespace ::bbn::parliament;
namespace pmnt = ::bbn::parliament;
using ::std::basic_ostream;
using ::std::size;
using ::std::string;

static auto g_log(pmnt::log::getSource("KbInstanceJNI"));

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* /* pVM */, void* /* pReserved */)
{
	return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* /* pVM */, void* /* pReserved */)
{
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_initStatic(
	JNIEnv* pEnv, jclass cls)
{
	JNIHelper::setStaticLongFld(pEnv, cls, "NULL_STMT_ID", k_nullStmtId);
	JNIHelper::setStaticLongFld(pEnv, cls, "NULL_RSRC_ID", k_nullRsrcId);

	JNIHelper::setStaticShortFld(pEnv, cls, "INDETERMINATE_KB_STATE", static_cast<int16>(KbDisposition::k_indeterminateKbState));
	JNIHelper::setStaticShortFld(pEnv, cls, "KB_DOES_NOT_EXIST", static_cast<int16>(KbDisposition::k_kbDoesNotExist));
	JNIHelper::setStaticShortFld(pEnv, cls, "KB_EXISTS_WITHOUT_URI_TO_INT", static_cast<int16>(KbDisposition::k_kbExistsWithoutUriToInt));
	JNIHelper::setStaticShortFld(pEnv, cls, "KB_EXISTS", static_cast<int16>(KbDisposition::k_kbExists));

	JNIHelper::setStaticIntFld(pEnv, cls, "SKIP_DELETED_STMT_ITER_FLAG", static_cast<int32>(StmtIteratorFlags::k_skipDeleted));
	JNIHelper::setStaticIntFld(pEnv, cls, "SKIP_INFERRED_STMT_ITER_FLAG", static_cast<int32>(StmtIteratorFlags::k_skipInferred));
	JNIHelper::setStaticIntFld(pEnv, cls, "SKIP_LITERAL_STMT_ITER_FLAG", static_cast<int32>(StmtIteratorFlags::k_skipLiteral));
	JNIHelper::setStaticIntFld(pEnv, cls, "SKIP_NON_LITERAL_STMT_ITER_FLAG", static_cast<int32>(StmtIteratorFlags::k_skipNonLiteral));
}

// The BAD_PERFORMANCE version of this function is intended as documentation of
// what the other version does.  The former makes nice use of the JNIHelper
// class to fetch the KB pointer with as little code as possible.  However,
// this implementation performs poorly because it fetches the JNI field ID anew
// on every call.  The second version caches the field ID.
static inline KbInstance* kbPtr(JNIEnv* pEnv, jobject obj)
{
#if defined(BAD_PERFORMANCE)
	return static_cast<KbInstance*>(JNIHelper::getPtrFld(pEnv, obj, "m_pKb"));
#else
	static jfieldID g_fid = 0;

	if (g_fid == 0)
	{
		// Note that two threads may execute this at the same time, but that
		// both will compute the same result, so the race condition is benign.
		g_fid = JNIHelper::getFieldId(pEnv, obj, "m_pKb", "J");
	}
	return reinterpret_cast<KbInstance*>(static_cast<intPtr>(pEnv->GetLongField(obj, g_fid)));
#endif
}

static void disposeInternal(JNIEnv* pEnv, jobject obj)
{
	KbInstance* pKb = kbPtr(pEnv, obj);
	JNIHelper::setPtrFld(pEnv, obj, "m_pKb", nullptr);
	delete pKb;
}

// Note:  If you change this method, be sure to make parallel changes
// to the method assignCppConfigToJavaConfig in ConfigJNI.cpp.
static void assignJavaConfigToCppConfig(KbConfig& config, JNIEnv* pEnv, jobject obj)
{
	config.kbDirectoryPath(						JNIHelper::getStringFld(pEnv, obj,	"m_kbDirectoryPath"));
	config.stmtFileName(							JNIHelper::getStringFld(pEnv, obj,	"m_stmtFileName"));
	config.rsrcFileName(							JNIHelper::getStringFld(pEnv, obj,	"m_rsrcFileName"));
	config.uriTableFileName(					JNIHelper::getStringFld(pEnv, obj,	"m_uriTableFileName"));
	config.uriToIntFileName(					JNIHelper::getStringFld(pEnv, obj,	"m_uriToIntFileName"));
	config.readOnly(								JNIHelper::getBooleanFld(pEnv, obj,	"m_readOnly"));
	config.fileSyncTimerDelay(					JNIHelper::getSizeTFld(pEnv, obj,	"m_fileSyncTimerDelay"));
	config.initialRsrcCapacity(				JNIHelper::getSizeTFld(pEnv, obj,	"m_initialRsrcCapacity"));
	config.avgRsrcLen(							JNIHelper::getSizeTFld(pEnv, obj,	"m_avgRsrcLen"));
	config.rsrcGrowthIncrement(				JNIHelper::getSizeTFld(pEnv, obj,	"m_rsrcGrowthIncrement"));
	config.rsrcGrowthFactor(					JNIHelper::getDoubleFld(pEnv, obj,	"m_rsrcGrowthFactor"));
	config.initialStmtCapacity(				JNIHelper::getSizeTFld(pEnv, obj,	"m_initialStmtCapacity"));
	config.stmtGrowthIncrement(				JNIHelper::getSizeTFld(pEnv, obj,	"m_stmtGrowthIncrement"));
	config.stmtGrowthFactor(					JNIHelper::getDoubleFld(pEnv, obj,	"m_stmtGrowthFactor"));
	config.bdbCacheSize(							JNIHelper::getStringFld(pEnv, obj,	"m_bdbCacheSize"));
	config.normalizeTypedStringLiterals(	JNIHelper::getBooleanFld(pEnv, obj,	"m_normalizeTypedStringLiterals"));
	config.timeoutDuration(						JNIHelper::getSizeTFld(pEnv, obj,	"m_timeoutDuration"));
	config.timeoutUnit(							JNIHelper::getTimeoutUnitFld(pEnv, obj));
	config.runAllRulesAtStartup(				JNIHelper::getBooleanFld(pEnv, obj,	"m_runAllRulesAtStartup"));
	config.enableSWRLRuleEngine(				JNIHelper::getBooleanFld(pEnv, obj,	"m_enableSWRLRuleEngine"));
	config.isSubclassRuleOn(					JNIHelper::getBooleanFld(pEnv, obj,	"m_isSubclassRuleOn"));
	config.isSubpropertyRuleOn(				JNIHelper::getBooleanFld(pEnv, obj,	"m_isSubpropertyRuleOn"));
	config.isDomainRuleOn(						JNIHelper::getBooleanFld(pEnv, obj,	"m_isDomainRuleOn"));
	config.isRangeRuleOn(						JNIHelper::getBooleanFld(pEnv, obj,	"m_isRangeRuleOn"));
	config.isEquivalentClassRuleOn(			JNIHelper::getBooleanFld(pEnv, obj,	"m_isEquivalentClassRuleOn"));
	config.isEquivalentPropRuleOn(			JNIHelper::getBooleanFld(pEnv, obj,	"m_isEquivalentPropRuleOn"));
	config.isInverseOfRuleOn(					JNIHelper::getBooleanFld(pEnv, obj,	"m_isInverseOfRuleOn"));
	config.isSymmetricPropRuleOn(				JNIHelper::getBooleanFld(pEnv, obj,	"m_isSymmetricPropRuleOn"));
	config.isFunctionalPropRuleOn(			JNIHelper::getBooleanFld(pEnv, obj,	"m_isFunctionalPropRuleOn"));
	config.isInvFunctionalPropRuleOn(		JNIHelper::getBooleanFld(pEnv, obj,	"m_isInvFunctionalPropRuleOn"));
	config.isTransitivePropRuleOn(			JNIHelper::getBooleanFld(pEnv, obj,	"m_isTransitivePropRuleOn"));
	config.inferRdfsClass(						JNIHelper::getBooleanFld(pEnv, obj,	"m_inferRdfsClass"));
	config.inferOwlClass(						JNIHelper::getBooleanFld(pEnv, obj,	"m_inferOwlClass"));
	config.inferRdfsResource(					JNIHelper::getBooleanFld(pEnv, obj,	"m_inferRdfsResource"));
	config.inferOwlThing(						JNIHelper::getBooleanFld(pEnv, obj,	"m_inferOwlThing"));
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_init(
	JNIEnv* pEnv, jobject obj, jobject jconfig)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		disposeInternal(pEnv, obj);

		KbConfig config;
		assignJavaConfigToCppConfig(config, pEnv, jconfig);
		KbInstance* pKb = new KbInstance(config);

		JNIHelper::setPtrFld(pEnv, obj, "m_pKb", pKb);
		END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_dispose(
	JNIEnv* pEnv, jobject obj)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		disposeInternal(pEnv, obj);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT jstring JNICALL Java_com_bbn_parliament_core_jni_KbInstance_getVersion(
	JNIEnv* pEnv, jclass /* cls */)
{
	jstring result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		string version = getKbVersion();
		result = JNIHelper::cstringToJstring(pEnv, version);
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_sync(
	JNIEnv* pEnv, jobject obj)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		pKb->sync();
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT jobject JNICALL Java_com_bbn_parliament_core_jni_KbInstance_getExcessCapacity(
	JNIEnv* pEnv, jobject obj)
{
	jobject result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);

		double pctUnusedUriCapacity = 0;
		double pctUnusedRsrcCapacity = 0;
		double pctUnusedStmtCapacity = 0;

		pKb->getExcessCapacity(pctUnusedUriCapacity, pctUnusedRsrcCapacity, pctUnusedStmtCapacity);

		result = JNIHelper::newObject(pEnv,
			JNIHelper::findClass(pEnv, "com/bbn/parliament/core/jni/KbInstance$GetExcessCapacityResult"),
			"(DDD)V", pctUnusedUriCapacity, pctUnusedRsrcCapacity, pctUnusedStmtCapacity);
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_releaseExcessCapacity(
	JNIEnv* pEnv, jobject obj)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		pKb->releaseExcessCapacity();
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT jshort JNICALL Java_com_bbn_parliament_core_jni_KbInstance_determineDisposition(
	JNIEnv* pEnv, jclass /* cls */, jobject javaConfig, jboolean throwIfIndeterminate)
{
	jshort result = static_cast<jshort>(KbDisposition::k_indeterminateKbState);
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbConfig cppConfig;
		assignJavaConfigToCppConfig(cppConfig, pEnv, javaConfig);
		result = static_cast<jshort>(KbInstance::determineDisposition(cppConfig,
			!!throwIfIndeterminate));
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_deleteKb(
	JNIEnv* pEnv, jclass /* cls */, jobject javaConfig, jstring directory, jboolean deleteContainingDir)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbConfig cppConfig;
		if (javaConfig == 0)
		{
			cppConfig.readFromFile();
		}
		else
		{
			assignJavaConfigToCppConfig(cppConfig, pEnv, javaConfig);
		}

		if (directory != 0)
		{
			cppConfig.kbDirectoryPath(
				JNIHelper::jstringToCstring<char>(pEnv, directory));
		}

		KbInstance::deleteKb(cppConfig, !!deleteContainingDir);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT jlong JNICALL Java_com_bbn_parliament_core_jni_KbInstance_stmtCount(
	JNIEnv* pEnv, jobject obj)
{
	jlong result = -1;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = static_cast<jlong>(pKb->stmtCount());
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jobject JNICALL Java_com_bbn_parliament_core_jni_KbInstance_countStmts(
	JNIEnv* pEnv, jobject obj)
{
	jobject result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);

		size_t total = 0;
		size_t numDel = 0;
		size_t numInferred = 0;
		size_t numDelAndInferred = 0;
		size_t numHidden = 0;
		size_t numVirtual = 0;

		pKb->countStmts(total, numDel, numInferred, numDelAndInferred, numHidden, numVirtual);

		result = JNIHelper::newObject(pEnv,
			JNIHelper::findClass(pEnv, "com/bbn/parliament/core/jni/KbInstance$CountStmtsResult"),
			"(JJJJJJ)V", static_cast<uint64>(total), static_cast<uint64>(numDel),
			static_cast<uint64>(numInferred), static_cast<uint64>(numDelAndInferred),
			static_cast<uint64>(numHidden), static_cast<uint64>(numVirtual));
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jlong JNICALL Java_com_bbn_parliament_core_jni_KbInstance_rsrcCount(
	JNIEnv* pEnv, jobject obj)
{
	jlong result = -1;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = static_cast<jlong>(pKb->rsrcCount());
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jdouble JNICALL Java_com_bbn_parliament_core_jni_KbInstance_averageRsrcLength(
	JNIEnv* pEnv, jobject obj)
{
	jdouble result = -1;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = static_cast<jdouble>(pKb->averageRsrcLength());
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jobject JNICALL Java_com_bbn_parliament_core_jni_KbInstance_find(
	JNIEnv* pEnv, jobject obj, jlong subjectId,
	jlong predicateId, jlong objectId, jint flags)
{
	jobject pResult = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		StmtIterator* pIter = new StmtIterator(pKb->find(
			static_cast<ResourceId>(subjectId), static_cast<ResourceId>(predicateId),
			static_cast<ResourceId>(objectId), static_cast<StmtIteratorFlags>(flags)));
		jclass iterCls = JNIHelper::findClass(pEnv,
			"com/bbn/parliament/core/jni/StmtIterator");
		pResult = JNIHelper::newObject(pEnv, iterCls, "(J)V",
			static_cast<uint64>(reinterpret_cast<uintPtr>(pIter)));
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return pResult;
}

JNIEXPORT jobject JNICALL Java_com_bbn_parliament_core_jni_KbInstance_findReifications(
	JNIEnv* pEnv, jobject obj, jlong statementName, jlong subjectId,
	jlong predicateId, jlong objectId)
{
	jobject pResult = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		ReificationIterator* pIter = new ReificationIterator(pKb->findReifications(
			static_cast<ResourceId>(statementName), static_cast<ResourceId>(subjectId),
			static_cast<ResourceId>(predicateId), static_cast<ResourceId>(objectId)
			));
		jclass iterCls = JNIHelper::findClass(pEnv,
			"com/bbn/parliament/core/jni/ReificationIterator");
		pResult = JNIHelper::newObject(pEnv, iterCls, "(J)V",
			static_cast<uint64>(reinterpret_cast<uintPtr>(pIter)));
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return pResult;
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_addReification(
	JNIEnv* pEnv, jobject obj, jlong statementName,
	jlong subjectId, jlong predicateId, jlong objectId)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)

		KbInstance* pKb = kbPtr(pEnv, obj);
		pKb->addReification(static_cast<ResourceId>(statementName),
				static_cast<ResourceId>(subjectId),
				static_cast<ResourceId>(predicateId),
				static_cast<ResourceId>(objectId));
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_deleteReification(
	JNIEnv* pEnv, jobject obj, jlong statementName, jlong subjectId,
	jlong predicateId, jlong objectId)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)

		KbInstance* pKb = kbPtr(pEnv, obj);
		pKb->deleteReification(static_cast<ResourceId>(statementName),
				static_cast<ResourceId>(subjectId),
				static_cast<ResourceId>(predicateId),
				static_cast<ResourceId>(objectId));
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT jlong JNICALL Java_com_bbn_parliament_core_jni_KbInstance_subjectCount(
	JNIEnv* pEnv, jobject obj, jlong rsrcId)
{
	jlong result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = static_cast<jlong>(pKb->subjectCount(static_cast<ResourceId>(rsrcId)));
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jlong JNICALL Java_com_bbn_parliament_core_jni_KbInstance_predicateCount(
	JNIEnv* pEnv, jobject obj, jlong rsrcId)
{
	jlong result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = static_cast<jlong>(pKb->predicateCount(static_cast<ResourceId>(rsrcId)));
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jlong JNICALL Java_com_bbn_parliament_core_jni_KbInstance_objectCount(
	JNIEnv* pEnv, jobject obj, jlong rsrcId)
{
	jlong result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = static_cast<jlong>(pKb->objectCount(static_cast<ResourceId>(rsrcId)));
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jboolean JNICALL Java_com_bbn_parliament_core_jni_KbInstance_isRsrcLiteral(
	JNIEnv* pEnv, jobject obj, jlong rsrcId)
{
	jboolean result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = pKb->isRsrcLiteral(static_cast<ResourceId>(rsrcId));
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jboolean JNICALL Java_com_bbn_parliament_core_jni_KbInstance_isRsrcAnonymous(
	JNIEnv* pEnv, jobject obj, jlong rsrcId)
{
	jboolean result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = pKb->isRsrcAnonymous(static_cast<ResourceId>(rsrcId));
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jlong JNICALL Java_com_bbn_parliament_core_jni_KbInstance_uriToRsrcId(
	JNIEnv* pEnv, jobject obj, jstring uri, jboolean isLiteral, jboolean createIfMissing)
{
	jlong result = k_nullRsrcId;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		if (uri != 0)
		{
			KbInstance* pKb = kbPtr(pEnv, obj);
			JStringAccessor<RsrcChar> accessor(pEnv, uri);
			result = static_cast<jlong>(pKb->uriToRsrcId(accessor.begin(), size(accessor), !!isLiteral, !!createIfMissing));
		}
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jstring JNICALL Java_com_bbn_parliament_core_jni_KbInstance_rsrcIdToUri(
	JNIEnv* pEnv, jobject obj, jlong rsrcId)
{
	jstring result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		const RsrcChar* pUri = pKb->rsrcIdToUri(static_cast<ResourceId>(rsrcId));
		result = JNIHelper::cstringToJstring(pEnv, pUri);
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jlong JNICALL Java_com_bbn_parliament_core_jni_KbInstance_createAnonymousRsrc(
	JNIEnv* pEnv, jobject obj)
{
	jlong result = k_nullRsrcId;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = static_cast<jlong>(pKb->createAnonymousRsrc());
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT jlong JNICALL Java_com_bbn_parliament_core_jni_KbInstance_addStmt(
	JNIEnv* pEnv, jobject obj, jlong subjectId,
	jlong predicateId, jlong objectId, jboolean isInferred)
{
	jlong result = k_nullStmtId;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = static_cast<jlong>(pKb->addStmt(static_cast<ResourceId>(subjectId),
			static_cast<ResourceId>(predicateId), static_cast<ResourceId>(objectId),
			!!isInferred));
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_deleteStmt(
	JNIEnv* pEnv, jobject obj, jlong subjectId, jlong predicateId, jlong objectId)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		pKb->deleteStmt(static_cast<ResourceId>(subjectId), static_cast<ResourceId>(predicateId), static_cast<ResourceId>(objectId));
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_dumpKbAsNTriples(
	JNIEnv* pEnv, jobject obj, jobject outputStream, jboolean includeInferredStmts,
	jboolean includeDeletedStmts, jboolean useAsciiOnlyEncoding)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		JavaOutputStreamBuf streamBuf(pEnv, outputStream);
		basic_ostream<JavaOutputStreamBuf::char_type> s(&streamBuf);
		KbInstance* pKb = kbPtr(pEnv, obj);
		pKb->dumpKbAsNTriples(s,
			includeInferredStmts ? InferredStmtsAction::include : InferredStmtsAction::exclude,
			includeDeletedStmts ? DeletedStmtsAction::include : DeletedStmtsAction::exclude,
			useAsciiOnlyEncoding ? EncodingCharSet::ascii : EncodingCharSet::utf8);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT jboolean JNICALL Java_com_bbn_parliament_core_jni_KbInstance_validate(
	JNIEnv* pEnv, jobject obj, jobject printStream)
{
	jboolean result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		JavaPrintStreamBuf streamBuf(pEnv, printStream);
		basic_ostream<JavaPrintStreamBuf::char_type> s(&streamBuf);
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = pKb->validate(s);
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_printStatements(
	JNIEnv* pEnv, jobject obj, jobject printStream, jboolean includeNextStmts,
	jboolean verboseNextStmts)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		JavaPrintStreamBuf streamBuf(pEnv, printStream);
		basic_ostream<JavaPrintStreamBuf::char_type> s(&streamBuf);
		KbInstance* pKb = kbPtr(pEnv, obj);
		pKb->printStatements(s, !!includeNextStmts, !!verboseNextStmts);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_printResources(
	JNIEnv* pEnv, jobject obj, jobject printStream, jboolean includeFirstStmts,
	jboolean verboseFirstStmts)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		JavaPrintStreamBuf streamBuf(pEnv, printStream);
		basic_ostream<JavaPrintStreamBuf::char_type> s(&streamBuf);
		KbInstance* pKb = kbPtr(pEnv, obj);
		pKb->printResources(s, !!includeFirstStmts, !!verboseFirstStmts);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT jlong JNICALL Java_com_bbn_parliament_core_jni_KbInstance_ruleCount(
	JNIEnv* pEnv, jobject obj)
{
	jlong result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbInstance* pKb = kbPtr(pEnv, obj);
		result = static_cast<jlong>(pKb->ruleCount());
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_printRules(
	JNIEnv* pEnv, jobject obj, jobject printStream)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		JavaPrintStreamBuf streamBuf(pEnv, printStream);
		basic_ostream<JavaPrintStreamBuf::char_type> s(&streamBuf);
		KbInstance* pKb = kbPtr(pEnv, obj);
		pKb->printRules(s);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_core_jni_KbInstance_printRuleTriggers(
	JNIEnv* pEnv, jobject obj, jobject printStream)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		JavaPrintStreamBuf streamBuf(pEnv, printStream);
		basic_ostream<JavaPrintStreamBuf::char_type> s(&streamBuf);
		KbInstance* pKb = kbPtr(pEnv, obj);
		pKb->printRuleTriggers(s);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}
