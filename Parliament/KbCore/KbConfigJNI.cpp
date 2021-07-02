// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/generated/com_bbn_parliament_jni_KbConfig.h"
#include "parliament/Platform.h"
#include "parliament/KbConfig.h"
#include "parliament/JNIHelper.h"
#include "parliament/Log.h"
#include "parliament/UnicodeIterator.h"

using namespace ::bbn::parliament;
namespace pmnt = ::bbn::parliament;
using ::std::string;

static auto g_log(pmnt::log::getSource("KbConfigJNI"));

// Note:  If you change this method, be sure to make parallel changes
// to the method assignJavaConfigToCppConfig in KbInstanceJNI.cpp.
static void assignCppConfigToJavaConfig(JNIEnv* pEnv, jobject obj, const KbConfig& config)
{
#if defined(PARLIAMENT_WINDOWS)
	JNIHelper::setStringFld(pEnv, obj,	"m_kbDirectoryPath",					pathAsUtf8(config.kbDirectoryPath()));
#else
	JNIHelper::setStringFld(pEnv, obj,	"m_kbDirectoryPath",					config.kbDirectoryPath().string());
#endif
	JNIHelper::setStringFld(pEnv, obj,	"m_stmtFileName",						config.stmtFileName());
	JNIHelper::setStringFld(pEnv, obj,	"m_rsrcFileName",						config.rsrcFileName());
	JNIHelper::setStringFld(pEnv, obj,	"m_uriTableFileName",				config.uriTableFileName());
	JNIHelper::setStringFld(pEnv, obj,	"m_uriToIntFileName",				config.uriToIntFileName());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_readOnly",							config.readOnly());
	JNIHelper::setLongFld(pEnv, obj,		"m_fileSyncTimerDelay",				config.fileSyncTimerDelay());
	JNIHelper::setLongFld(pEnv, obj,		"m_initialRsrcCapacity",			config.initialRsrcCapacity());
	JNIHelper::setLongFld(pEnv, obj,		"m_avgRsrcLen",						config.avgRsrcLen());
	JNIHelper::setLongFld(pEnv, obj,		"m_rsrcGrowthIncrement",			config.rsrcGrowthIncrement());
	JNIHelper::setDoubleFld(pEnv, obj,	"m_rsrcGrowthFactor",				config.rsrcGrowthFactor());
	JNIHelper::setLongFld(pEnv, obj,		"m_initialStmtCapacity",			config.initialStmtCapacity());
	JNIHelper::setLongFld(pEnv, obj,		"m_stmtGrowthIncrement",			config.stmtGrowthIncrement());
	JNIHelper::setDoubleFld(pEnv, obj,	"m_stmtGrowthFactor",				config.stmtGrowthFactor());
	JNIHelper::setStringFld(pEnv, obj,	"m_bdbCacheSize",						config.bdbCacheSize());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_normalizeTypedStringLiterals",config.normalizeTypedStringLiterals());
	JNIHelper::setLongFld(pEnv, obj,		"m_timeoutDuration",					config.timeoutDuration());
	JNIHelper::setTimeoutUnitFld(pEnv, obj,										config.javaTimeoutUnit().c_str());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_runAllRulesAtStartup",			config.runAllRulesAtStartup());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_enableSWRLRuleEngine",			config.enableSWRLRuleEngine());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isSubclassRuleOn",				config.isSubclassRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isSubpropertyRuleOn",			config.isSubpropertyRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isDomainRuleOn",					config.isDomainRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isRangeRuleOn",					config.isRangeRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isEquivalentClassRuleOn",		config.isEquivalentClassRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isEquivalentPropRuleOn",		config.isEquivalentPropRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isInverseOfRuleOn",				config.isInverseOfRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isSymmetricPropRuleOn",			config.isSymmetricPropRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isFunctionalPropRuleOn",		config.isFunctionalPropRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isInvFunctionalPropRuleOn",	config.isInvFunctionalPropRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_isTransitivePropRuleOn",		config.isTransitivePropRuleOn());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_inferRdfsClass",					config.inferRdfsClass());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_inferOwlClass",					config.inferOwlClass());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_inferRdfsResource",				config.inferRdfsResource());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_inferOwlThing",					config.inferOwlThing());
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_jni_KbConfig_init(
	JNIEnv* pEnv, jobject obj)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbConfig config;
		assignCppConfigToJavaConfig(pEnv, obj, config);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT void JNICALL Java_com_bbn_parliament_jni_KbConfig_readFromFile(
	JNIEnv* pEnv, jobject obj)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		KbConfig config;
		config.readFromFile();
		assignCppConfigToJavaConfig(pEnv, obj, config);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}
