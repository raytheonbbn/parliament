// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: DumpDeletedStatements.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.utilities;

import com.bbn.parliament.jni.KbConfig;
import com.bbn.parliament.jni.KbInstance;
import com.bbn.parliament.jni.StmtIterator;
import com.bbn.parliament.jni.StmtIterator.Statement;

public class DumpDeletedStatements {
	static void usage() {
		System.out
				.println("Usage:  com.bbn.parliament.sesame.sail.utilities.DumpDeletedStatements <parliament-directory>");
		System.out
				.println("Dumps the deleted statements in the indicated Parliament instance");
		System.exit(1);
	}

	public static void main(String args[]) throws Throwable {
		if (args.length != 1) {
			usage();
		}
		String dir = args[0];

		KbConfig config = new KbConfig();
		config.readFromFile();
		config.m_kbDirectoryPath = dir;
		config.m_readOnly = true;
		try (
			KbInstance kb = new KbInstance(config);
			StmtIterator statements = kb.find(KbInstance.NULL_RSRC_ID,
				KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, 0);
		) {
			while (statements.hasNext()) {
				Statement statement = statements.next();
				if (statement.isDeleted()) {
					char infFlag = statement.isInferred() ? 'i' : ' ';
					char litFlag = statement.isLiteral() ? 'l' : ' ';

					long subId = statement.getSubject();
					long predId = statement.getPredicate();
					long objId = statement.getObject();

					String sub = kb.rsrcIdToUri(subId);
					String pred = kb.rsrcIdToUri(predId);
					String obj = kb.rsrcIdToUri(objId);

					System.out.format(
							"d%1$c%2$c %3$d %4$s %5$d %6$s %7$d %8$s",
							infFlag, litFlag, subId, sub, predId, pred,
							objId, obj);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
