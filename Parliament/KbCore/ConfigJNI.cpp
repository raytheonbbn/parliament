// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

#include "parliament/generated/com_bbn_parliament_jni_Config.h"
#include "parliament/Platform.h"
#include "parliament/Config.h"
#include "parliament/JNIHelper.h"
#include "parliament/UnicodeIterator.h"

using namespace ::bbn::parliament;
using ::std::string;

// Note:  If you change this method, be sure to make parallel changes
// to the method assignJavaConfigToCppConfig in KbInstanceJNI.cpp.
static void assignCppConfigToJavaConfig(JNIEnv* pEnv, jobject obj, const Config& config)
{
	JNIHelper::setBooleanFld(pEnv, obj,	"m_logToConsole",						config.logToConsole());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_logConsoleAsynchronous",		config.logConsoleAsynchronous());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_logConsoleAutoFlush",			config.logConsoleAutoFlush());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_logToFile",							config.logToFile());
#if defined(PARLIAMENT_WINDOWS)
	JNIHelper::setStringFld(pEnv, obj,	"m_logFilePath",						pathAsUtf8(config.logFilePath()));
#else
	JNIHelper::setStringFld(pEnv, obj,	"m_logFilePath",						config.logFilePath().string());
#endif
	JNIHelper::setBooleanFld(pEnv, obj,	"m_logFileAsynchronous",			config.logFileAsynchronous());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_logFileAutoFlush",				config.logFileAutoFlush());
	JNIHelper::setLongFld(pEnv, obj,		"m_logFileRotationSize",			config.logFileRotationSize());
	JNIHelper::setLongFld(pEnv, obj,		"m_logFileMaxAccumSize",			config.logFileMaxAccumSize());
	JNIHelper::setLongFld(pEnv, obj,		"m_logFileMinFreeSpace",			config.logFileMinFreeSpace());
	JNIHelper::setStringFld(pEnv, obj,	"m_logFileRotationTimePoint",		config.logFileRotationTimePoint());
	JNIHelper::setStringFld(pEnv, obj,	"m_logLevel",							config.logLevel());

	jmethodID mId = JNIHelper::getMethodID(pEnv, obj, "clearLogChannelLevels", "()V");
	pEnv->CallVoidMethod(obj, mId);
	mId = JNIHelper::getMethodID(pEnv, obj, "addLogChannelLevel", "(Ljava/lang/String;Ljava/lang/String;)V");
	for (const auto& entry : config.logChannelLevels())
	{
		jstring channel = JNIHelper::cstringToJstring(pEnv, entry.first);
		jstring level = JNIHelper::cstringToJstring(pEnv, entry.second);
		pEnv->CallVoidMethod(obj, mId, channel, level);
	}

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
	JNIHelper::setDoubleFld(pEnv, obj,	"m_rsrcGrowthFactor",				config.rsrcGrowthFactor());
	JNIHelper::setLongFld(pEnv, obj,		"m_initialStmtCapacity",			config.initialStmtCapacity());
	JNIHelper::setDoubleFld(pEnv, obj,	"m_stmtGrowthFactor",				config.stmtGrowthFactor());
	JNIHelper::setStringFld(pEnv, obj,	"m_bdbCacheSize",						config.bdbCacheSize());
	JNIHelper::setBooleanFld(pEnv, obj,	"m_normalizeTypedStringLiterals",config.normalizeTypedStringLiterals());
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

JNIEXPORT void JNICALL Java_com_bbn_parliament_jni_Config_init(
	JNIEnv* pEnv, jobject obj)
{
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		Config config;
		assignCppConfigToJavaConfig(pEnv, obj, config);
	END_JNI_EXCEPTION_HANDLER(pEnv)
}

JNIEXPORT jobject JNICALL Java_com_bbn_parliament_jni_Config_readFromFile(
	JNIEnv* pEnv, jclass cls)
{
	jobject result = 0;
	BEGIN_JNI_EXCEPTION_HANDLER(pEnv)
		Config config = Config::readFromFile();
		result = JNIHelper::newObjectByDefaultCtor(pEnv, cls);
		assignCppConfigToJavaConfig(pEnv, result, config);
	END_JNI_EXCEPTION_HANDLER(pEnv)
	return result;
}
