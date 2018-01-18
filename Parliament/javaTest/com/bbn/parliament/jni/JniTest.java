// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jni;

import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import com.bbn.parliament.jni.StmtIterator.Statement;

/** @author iemmons */
public class JniTest extends TestCase {
	private static final String UNICODE_LABEL = "\"\u0056\u004d\u0057\u0430\u0058\u4e8c\u0059\ud800\udf02\u005a\"";

	private static final String RDFS_SUB_CLASS = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
	private static final String RDF_TYPE       = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	private static final String HUMAN_URI      = "http://example.org/#Human";
	private static final String DOG_URI        = "http://example.org/#Dog";
	private static final String CAT_URI        = "http://example.org/#Cat";
	private static final String MAMMAL_URI     = "http://example.org/#Mammal";
	private static final String ANIMAL_URI     = "http://example.org/#Animal";
	private static final String DICK_URI       = "http://example.org/#Dick";
	private static final String JANE_URI       = "http://example.org/#Jane";
	private static final String SPOT_URI       = "http://example.org/#Spot";
	private static final String PUFF_URI       = "http://example.org/#Puff";

	@SuppressWarnings("static-method")
	public void testUnicodeTransfer() {
		Config cfg = buildConfig(false);

		// Clearing away old KB leftovers:
		KbInstance.deleteKb(cfg, null);

		try (KbInstance kb = new KbInstance(cfg)) {
			assertEquals(0, kb.rsrcCount());
			long unicodeLabelRsrcId = kb.uriToRsrcId(UNICODE_LABEL, true, true);
			assertEquals(1, kb.rsrcCount());
			String unicodeLabel = kb.rsrcIdToUri(unicodeLabelRsrcId);
			assertEquals("Unicode string literal", UNICODE_LABEL, unicodeLabel);
		} catch (Throwable ex) {
			ex.printStackTrace();
			assertTrue(ex.getMessage(), false);
		} finally {
			KbInstance.deleteKb(cfg, null);
		}
	}

	@SuppressWarnings("static-method")
	public void testByQuickOverview() {
		Config cfg = buildConfig(true);

		// Clearing away old KB leftovers:
		KbInstance.deleteKb(cfg, null);

		try (KbInstance kb = new KbInstance(cfg)) {
			long rdfsSubClassOfRsrcId = kb.uriToRsrcId(RDFS_SUB_CLASS, false, true);
			long rdfTypeRsrcId = kb.uriToRsrcId(RDF_TYPE, false, true);
			long humanRsrcId = kb.uriToRsrcId(HUMAN_URI, false, true);
			long dogRsrcId = kb.uriToRsrcId(DOG_URI, false, true);
			long catRsrcId = kb.uriToRsrcId(CAT_URI, false, true);
			long mammalRsrcId = kb.uriToRsrcId(MAMMAL_URI, false, true);
			long animalRsrcId = kb.uriToRsrcId(ANIMAL_URI, false, true);
			long dickRsrcId = kb.uriToRsrcId(DICK_URI, false, true);
			long janeRsrcId = kb.uriToRsrcId(JANE_URI, false, true);
			long spotRsrcId = kb.uriToRsrcId(SPOT_URI, false, true);
			long puffRsrcId = kb.uriToRsrcId(PUFF_URI, false, true);

			kb.addStmt(mammalRsrcId, rdfsSubClassOfRsrcId, animalRsrcId, false);
			kb.addStmt(humanRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId, false);
			kb.addStmt(dogRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId, false);
			kb.addStmt(catRsrcId, rdfsSubClassOfRsrcId, mammalRsrcId, false);

			kb.addStmt(dickRsrcId, rdfTypeRsrcId, humanRsrcId, false);
			kb.addStmt(janeRsrcId, rdfTypeRsrcId, humanRsrcId, false);
			kb.addStmt(spotRsrcId, rdfTypeRsrcId, dogRsrcId, false);
			kb.addStmt(puffRsrcId, rdfTypeRsrcId, catRsrcId, false);

			// Check that resources are inserted only once:
			long rdfTypeRsrcId2 = kb.uriToRsrcId(RDF_TYPE, false, true);
			assertEquals(String.format("rdf:type double inserted (rsrc ids %1$d and %2$d)",
				rdfTypeRsrcId, rdfTypeRsrcId2), rdfTypeRsrcId, rdfTypeRsrcId2);

			// Should be inferred:
			// Human subClassOf Animal
			// Dog subClassOf Animal
			// Cat subClassOf Animal
			// Dick a Mammal
			// Dick a Animal
			// Jane a Mammal
			// Jane a Animal
			// Spot a Mammal
			// Spot a Animal
			// Puff a Mammal
			// Puff a Animal

			// Check statement counts:
			KbInstance.CountStmtsResult counts = kb.countStmts();
			assertEquals("# total statements", 19, counts.getTotal());
			assertEquals("# deleted statements", 0, counts.getNumDel());
			assertEquals("# inferred statements", 11, counts.getNumInferred());
			assertEquals("# del & inf statements", 0, counts.getNumDelAndInferred());

			// Check that we can find instances of human:
			try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfTypeRsrcId,
				humanRsrcId, KbInstance.SKIP_DELETED_STMT_ITER_FLAG))
			{
				Set<Long> expectedResults = new TreeSet<>();
				expectedResults.add(dickRsrcId);
				expectedResults.add(janeRsrcId);

				Set<Long> results = new TreeSet<>();
				while (it.hasNext())
				{
					Statement result = it.next();
					results.add(result.getSubject());
					System.out.format("Human query result:  %1$s%n",
						kb.rsrcIdToUri(result.getSubject()));
				}
				assertEquals("Human query results", expectedResults, results);
			}

			// Check that we can find instances of animal (inferred):
			try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfTypeRsrcId,
				animalRsrcId, KbInstance.SKIP_DELETED_STMT_ITER_FLAG))
			{
				Set<Long> expectedResults = new TreeSet<>();
				expectedResults.add(dickRsrcId);
				expectedResults.add(janeRsrcId);
				expectedResults.add(spotRsrcId);
				expectedResults.add(puffRsrcId);

				Set<Long> results = new TreeSet<>();
				while (it.hasNext())
				{
					Statement result = it.next();
					results.add(result.getSubject());
					System.out.format("Animal query result:  %1$s%n",
						kb.rsrcIdToUri(result.getSubject()));
				}
				assertEquals("Animal query results", expectedResults, results);
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
			assertTrue(ex.getMessage(), false);
		} finally {
			KbInstance.deleteKb(cfg, null);
		}
	}

	private static Config buildConfig(boolean withInference) {
		Config cfg = new Config();
		cfg.m_kbDirectoryPath = ".";
		cfg.m_logToConsole = false;
		if (!withInference) {
			cfg.disableAllRules();
		}
		return cfg;
	}
}
