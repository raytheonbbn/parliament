
package com.bbn.parliament.queryoptimization;

import java.util.Vector;

import com.bbn.parliament.jni.Config;
import com.bbn.parliament.jni.KbInstance;
import com.bbn.parliament.jni.KbInstance.CountStmtsResult;
import com.bbn.parliament.jni.ReificationIterator;
import com.bbn.parliament.jni.ReificationIterator.Reification;
import com.bbn.parliament.jni.StmtIterator;
import com.bbn.parliament.jni.StmtIterator.Statement;

import junit.framework.TestCase;

/** @author dkolas */
public class ReificationTest extends TestCase{
	private Config config;
	private KbInstance kb;

	private long a;
	private long b;
	private long c;
	private long cLiteral;
	private long d;
	private long e;
	private long f;
	private long g;
	private long h;
	private long rdfSubject;
	private long rdfPredicate;
	private long rdfObject;
	private long rdfStatement;
	private long rdfType;
	private long hasStatementName;

	@Override
	public void setUp(){
		config = new Config();
		config.m_kbDirectoryPath = ".";
		config.m_logToConsole = false;
		try {
			KbInstance.deleteKb(config, null);
			kb = new KbInstance(config);
		} catch (Throwable e1) {
			throw new RuntimeException(e1);
		}

		a = kb.uriToRsrcId("http://example.org/a", false, true);
		b = kb.uriToRsrcId("http://example.org/b", false, true);
		c = kb.uriToRsrcId("http://example.org/c", false, true);
		cLiteral = kb.uriToRsrcId("\"c\"", true, true);
		d = kb.uriToRsrcId("http://example.org/d", false, true);
		e = kb.uriToRsrcId("http://example.org/e", false, true);
		f = kb.uriToRsrcId("http://example.org/f", false, true);
		g = kb.uriToRsrcId("http://example.org/g", false, true);
		h = kb.uriToRsrcId("http://example.org/h", false, true);

		String rdfNs = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
		rdfSubject = kb.uriToRsrcId(rdfNs+"subject", false, true);
		rdfPredicate = kb.uriToRsrcId(rdfNs+"predicate", false, true);
		rdfObject = kb.uriToRsrcId(rdfNs+"object", false, true);
		rdfType = kb.uriToRsrcId(rdfNs+"type", false, true);
		rdfStatement = kb.uriToRsrcId(rdfNs+"Statement", false, true);
		hasStatementName = kb.uriToRsrcId("http://parliament.semwebcentral.org/parliament#hasStatementName", false, false);
	}

	@Override
	public void tearDown() {
		if (kb != null) {
			kb.finalize();
		}
		if (config != null) {
			KbInstance.deleteKb(config, null);
		}
	}

	public void testReificationAddOnly(){
		kb.addStmt(a, b, c, false);
		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, 0)) {
			@SuppressWarnings("unused")
			Statement s = it.next();
			kb.addReification(d, a, b, c);
		}

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, d, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			// Including this do-nothing call avoids a compiler warning about never using the auto-closable resource it:
			@SuppressWarnings("unused")
			boolean notUsed = it.hasNext();
		}
	}

	public void testReificationAddAndCheck(){
		kb.addStmt(a, b, c, false);
		kb.addReification(d, a, b, c);

		try (ReificationIterator it = kb.findReifications(d, a, b, c)) {
			checkReifications(it, new long[][]{{d,a,b,c}});
		}
	}

	public void testMultipleReifications(){
		kb.addStmt(a, b, c, false);
		kb.addReification(d, a, b, c);
		kb.addReification(e, a, b, c);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, a, b, c)) {
			checkReifications(it, new long[][]{{d,a,b,c},{e,a,b,c}});
		}
	}


	public void testFindReificationsByName(){
		kb.addStmt(a, b, c, false);
		kb.addReification(d, a, b, c);
		kb.addReification(e, a, b, c);
		kb.addReification(f, a, g, c);
		kb.addReification(h, a, g, b);
		kb.addReification(h, a, b, c);

		try (ReificationIterator it = kb.findReifications(d, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c}});
		}

		try (ReificationIterator it = kb.findReifications(a, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}

		try (ReificationIterator it = kb.findReifications(h, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{h,a,g,b},{h,a,b,c}});
		}
	}

	public void testFindReificationsBySubject(){
		kb.addStmt(a, b, c, false);
		kb.addReification(d, a, b, c);
		kb.addReification(e, a, b, c);
		kb.addReification(f, a, g, c);
		kb.addReification(h, a, g, b);

		//printTagStmts();
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, a, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{e,a,b,c},{f,a,g,c},{h,a,g,b}});
		}

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, a, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{e,a,b,c},{f,a,g,c},{h,a,g,b}});
		}
	}

	public void testFindReificationsByPredicate(){
		kb.addStmt(a, b, c, false);
		kb.addReification(d, a, b, c);
		kb.addReification(e, a, b, c);
		kb.addReification(f, a, g, c);
		kb.addReification(h, a, g, b);

		//printTagStmts();
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, g, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{f,a,g,c},{h,a,g,b}});
		}

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{e,a,b,c}});
		}
	}

	public void testFindReificationsByObject(){
		kb.addStmt(a, b, c, false);
		kb.addReification(d, a, b, c);
		kb.addReification(e, a, b, c);
		kb.addReification(f, a, g, c);
		kb.addReification(h, a, g, b);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, c)) {
			checkReifications(it, new long[][]{{d,a,b,c},{e,a,b,c},{f,a,g,c}});
		}

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b)) {
			checkReifications(it, new long[][]{{h,a,g,b}});
		}
	}

	public void testFindReificationsByNone(){
		kb.addStmt(a, b, c, false);
		kb.addReification(d, a, b, c);
		kb.addReification(e, a, b, c);
		kb.addReification(f, a, g, c);
		kb.addReification(h, a, g, b);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{e,a,b,c},{f,a,g,c},{h,a,g,b}});
		}
	}

	public void testFindReificationsBySubjectObject(){
		kb.addStmt(a, b, c, false);
		kb.addReification(d, a, b, c);
		kb.addReification(e, a, b, c);
		kb.addReification(f, a, g, c);
		kb.addReification(h, a, g, b);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, a, KbInstance.NULL_RSRC_ID, c)) {
			checkReifications(it, new long[][]{{d,a,b,c},{e,a,b,c},{f,a,g,c}});
		}
	}

	public void testReificationDelete(){
		kb.addStmt(a, b, c, false);
		kb.addReification(d, a, b, c);

		//Verify reification is present
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c}});
		}

		kb.deleteReification(d,a,b,c);
		//Verify reification is not present
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}
	}

	public void testReificationDeleteNonexistant(){
		kb.addStmt(a, b, c, false);
		kb.deleteReification(d,a,b,c);
	}

	public void testReificationAddByStatements(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c}});
		}
	}

	public void testReificationStatementLiteralness(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, cLiteral, false);
		kb.addStmt(d, rdfType, rdfStatement, false);

		try (StmtIterator it = kb.find(d, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfSubject, a, 0},{d,rdfPredicate,b,0},{d,rdfObject,cLiteral,1},{d,rdfType,rdfStatement,0}});
		}
	}

	public void testRegularStatementLiteralness(){
		kb.addStmt(a, b, cLiteral, false);
		kb.addStmt(d, e, f, false);

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{a, b, cLiteral, 1},{d,e,f,0}});
		}
	}

	public void testReificationsLiteralness(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, cLiteral, false);
		kb.addStmt(d, rdfType, rdfStatement, false);

		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,cLiteral,1},{h,e,f,g,0}});
		}
	}

	public void testReificationCheckNonExistentReification(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		try (ReificationIterator it = kb.findReifications(h, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}
	}

	public void testReificationCheckNonExistentReification2(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);
		try (ReificationIterator it = kb.findReifications(h, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}
	}

	public void testReificationListByStatementName(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);
		try (ReificationIterator it = kb.findReifications(h, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{h,e,f,g}});
		}
	}

	public void testReificationMultipleAdd(){
		kb.addReification(d, a, b, c);
		kb.addReification(d, a, b, c);
		//printKb();
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c}});
		}
	}

	public void testReificationMultipleAddByStatements(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c}});
		}
	}

	public void testReificationDeleteByStatements(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c}});
		}
		kb.deleteStmt(d, rdfPredicate, b);
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}
	}

	public void testReificationMultipleDeleteByStatements(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c}});
		}

		kb.deleteStmt(d, rdfPredicate, b);
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}

		kb.deleteStmt(d, rdfSubject, a);
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}

		kb.deleteStmt(d, rdfObject, c);
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{h,e,f,g}});
		}

		try (ReificationIterator it = kb.findReifications(h, KbInstance.NULL_RSRC_ID, a, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}

		kb.deleteStmt(h, rdfPredicate, a);
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{h,e,f,g}});
		}

		kb.deleteStmt(h, rdfPredicate, f);
		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}
	}

	public void testCountVirtualStatements(){
		CountStmtsResult result = kb.countStmts();
		checkCount(result, 0, 0, 0);
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
//		printKb();
		kb.addStmt(d, rdfType, rdfStatement, false);
//		printKb();
		System.out.println(kb.stmtCount());
		result = kb.countStmts();
		checkCount(result, 2, 1, 4);
	}

	public void testFindVirtualStatementsVarRdfTypeVar(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfType, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfType, rdfStatement}});
		}
	}

	public void testFindVirtualStatementsVarRdfTypeVar2(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{h,e,f,g}});
		}
		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfType, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfType, rdfStatement},{h, rdfType, rdfStatement}});
		}
	}

	public void testFindVirtualStatementsVarVarStatement(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, rdfStatement, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfType, rdfStatement}});
		}
	}

	public void testFindVirtualStatementsVarVarStatement2(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{h,e,f,g}});
		}
		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, rdfStatement, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfType, rdfStatement},{h, rdfType, rdfStatement}});
		}
	}

	public void testFindVirtualStatementsVarRdfTypeStatement(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfType, rdfStatement, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfType, rdfStatement}});
		}
	}

	public void testFindVirtualStatementsVarRdfTypeStatement2(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{h,e,f,g}});
		}
		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfType, rdfStatement, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfType, rdfStatement},{h, rdfType, rdfStatement}});
		}
	}

	public void testFindVirtualStatementsVarRdfSubjectVar(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfSubject, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfSubject, a}});
		}
	}

	public void testFindVirtualStatementsVarRdfSubjectVar2(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{h,e,f,g}});
		}
		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfSubject, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfSubject, a},{h, rdfSubject, e}});
		}
	}

	public void testFindVirtualStatementsNameVarVar(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);

		try (StmtIterator it = kb.find(d, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfType, rdfStatement},{d, rdfSubject, a},{d, rdfPredicate, b},{d, rdfObject, c}});
		}
	}

	public void testFindVirtualStatementsNameVarVar2(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{h,e,f,g}});
		}
		try (StmtIterator it = kb.find(d,  KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it,  new long[][]{{d, rdfType, rdfStatement},{d, rdfSubject, a},{d, rdfPredicate, b},{d, rdfObject, c}});
		}
	}

	public void testFindVirtualStatementsVarVarVar(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfType, rdfStatement},{d, rdfSubject, a},{d, rdfPredicate, b},{d, rdfObject, c}});
		}
	}

	public void testFindVirtualStatementsVarVarUri(){
		kb.addStmt(a, b, c, false);
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, c, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{h,c,f,g}});
		}

		try (StmtIterator it = kb.find( KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b,KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it,  new long[][]{{d, rdfPredicate, b}});
		}

		try (StmtIterator it = kb.find( KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, c,KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it,  new long[][]{{h, rdfSubject, c},{d, rdfObject, c},{a,b,c}});
		}
	}

	public void testFindVirtualStatementsVarRdfPredicateVar(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfPredicate, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfPredicate, b}});
		}
	}

	public void testFindVirtualStatementsVarRdfPredicateVar2(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{h,e,f,g}});
		}
		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfPredicate, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfPredicate, b},{h, rdfPredicate, f}});
		}
	}

	public void testFindVirtualStatementsVarRdfObjectVar(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfObject, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfObject, c}});
		}
	}

	public void testFindVirtualStatementsVarRdfObjectVar2(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(h, rdfSubject, e, false);
		kb.addStmt(h, rdfPredicate, f, false);
		kb.addStmt(h, rdfObject, g, false);
		kb.addStmt(h, rdfType, rdfStatement, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c},{h,e,f,g}});
		}
		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfObject, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, rdfObject, c},{h, rdfObject, g}});
		}
	}

	public void testFindRegularStatements(){
		kb.addStmt(d, rdfSubject, a, false);
		kb.addStmt(d, rdfPredicate, b, false);
		kb.addStmt(d, rdfObject, c, false);
		kb.addStmt(d, rdfType, rdfStatement, false);
		kb.addStmt(a, b, c, false);
		kb.addStmt(d, e, f, false);
		kb.addStmt(a, e, c, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{{d,a,b,c}});
		}

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{a, b, c}});
		}

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, f, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{d, e, f}});
		}

		try (StmtIterator it = kb.find(a, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{{a, b, c},{a,e,c}});
		}
	}

	public void testNoPartialReifications(){
		@SuppressWarnings("unused")
		long x = kb.addStmt(d, rdfSubject, a, false);

		try (ReificationIterator it = kb.findReifications(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, b, KbInstance.NULL_RSRC_ID)) {
			checkReifications(it, new long[][]{});
		}

		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, rdfSubject, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			checkStatements(it, new long[][]{});
		}
	}

	@SuppressWarnings("unused")
	private static void printReifications(ReificationIterator it) {
		while (it.hasNext()){
			System.out.print(it.next());
		}
		System.out.println();
		it.finalize();
	}

	@SuppressWarnings("unused")
	private void printTagStmts(){
		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, hasStatementName, KbInstance.NULL_RSRC_ID, KbInstance.SKIP_DELETED_STMT_ITER_FLAG)) {
			System.out.println("Expected ? hasStatementName d");
			while (it.hasNext()){
				Statement s = it.next();
				System.out.println("s: "+kb.rsrcIdToUri(s.getSubject()) + " p: "+kb.rsrcIdToUri(s.getPredicate())+ " o: "+kb.rsrcIdToUri(s.getObject()));
			}
		}
	}

	private static void checkReifications(ReificationIterator iterator, long[][] reifications){
		Vector<Reification> actuals = new Vector<>();
		while(iterator.hasNext()){
			actuals.add(iterator.next());
		}
		iterator.finalize();
		assertEquals("Wrong number of reifications returned ("+reifications.length+ " vs "+ actuals.size()+")",reifications.length, actuals.size());
		for (int i=0; i<reifications.length; i++){
			boolean found = false;
			for (Reification reification : actuals){
				if (sameReification(reification, reifications[i])){
					found = true;
					break;
				}
			}
			if (!found){
				fail("Reification "+reifications[i]+" not found in actuals");
			}
		}
	}

	private static boolean sameReification(Reification reification, long[] values){
		return reification.getStatementName() == values[0] &&
			reification.getSubject() == values[1] &&
			reification.getPredicate() == values[2] &&
			reification.getObject() == values[3] &&
			(values.length != 5 || (values[4] == 0 && !reification.isLiteral()) || (values[4] == 1 && reification.isLiteral()));
	}

	private void checkStatements(StmtIterator iterator, long[][] statements){
		Vector<Statement> actuals = new Vector<>();
		while(iterator.hasNext()){
			actuals.add(iterator.next());
		}
		iterator.finalize();
		assertEquals("Wrong number of statements returned ("+statements.length+ " vs "+ actuals+")",statements.length, actuals.size());
		for (int i=0; i<statements.length; i++){
			boolean found = false;
			for (Statement statement : actuals){
				if (sameStatement(statement, statements[i])){
					found = true;
					break;
				}
			}
			if (!found){
				fail("Statement "+arrayString(statements[i])+" not found in actuals");
			}
		}
	}

	private String arrayString(long[] ls) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (int i=0; i<ls.length; i++){
			builder.append("("+ls[i]+") "+kb.rsrcIdToUri(ls[i])+", ");
		}
		builder.append("]");
		return builder.toString();
	}

	private static boolean sameStatement(Statement statement, long[] values){
		return statement.getSubject() == values[0] &&
			statement.getPredicate() == values[1] &&
			statement.getObject() == values[2] &&
			(values.length != 4 || (values[3] == 0 && !statement.isLiteral()) || (values[3] == 1 && statement.isLiteral()));
	}

	@SuppressWarnings("unused")
	private static void checkReification(Reification reification, long statementName, long subject,
			long predicate, long object) {
		assertEquals("Incorrect statementName in returned reification",statementName, reification.getStatementName());
		assertEquals("Incorrect subject in returned reification",subject, reification.getSubject());
		assertEquals("Incorrect predicate in returned reification",predicate, reification.getPredicate());
		assertEquals("Incorrect object in returned reification",object, reification.getObject());
	}

	private static void checkCount(CountStmtsResult result, int numStatements, int numDeleted, int numVirtual) {
		assertEquals("Incorrect number of stored statements",numStatements, result.getTotal());
		assertEquals("Incorrect number of deleted statements",numDeleted, result.getNumDel());
		assertEquals("Incorrect number of virtual statements",numVirtual, result.getNumVirtual());
	}

	@SuppressWarnings("unused")
	private void printKb(){
		printKb(false);
	}

	private void printKb(boolean showVirtual){
		try (StmtIterator it = kb.find(KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, KbInstance.NULL_RSRC_ID, showVirtual ? 32 : 16)) {
			System.out.println("KB: "+kb.stmtCount());
			while (it.hasNext()) {
				Statement statement = it.next();
				System.out.println("["+kb.rsrcIdToUri(statement.getSubject()) + ", "+kb.rsrcIdToUri(statement.getPredicate())+", "+kb.rsrcIdToUri(statement.getObject())+"]");
			}
		}
	}
}
