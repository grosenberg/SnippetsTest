/* Copyright � 2015-2016 Gerald Rosenberg.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the License.md file.
 */
package net.certiv.remark.test;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import net.certiv.remark.Converter;
import net.certiv.remark.IOProcessor;
import net.certiv.remark.PhaseState;
import net.certiv.remark.RemarkContext;
import net.certiv.remark.RemarkLexer;
import net.certiv.remark.RemarkParser;
import net.certiv.remark.RemarkPhase01;
import net.certiv.remark.RemarkPhase02;
import net.certiv.remark.RemarkPhase03;
import net.certiv.remark.RemarkPhase04;
import net.certiv.remark.RemarkTokenFactory;

public class SnippetsTest extends TestBase {

	// ------------------------------------------------------------------------
	// Custom configuration area - change as needed ---------------------------

	/** Base directory for snippet data files and results */
	// TODO: customization required - see 'Use' instructions.
	public static final String BaseDir = "D:/DevFiles/WorkSpaces/net.certiv.remark";

	/** File extension for snippet data */
	// TODO: customization required - see 'Use' instructions.
	public static final String Ext = ".md";

	/** If true, enables echoing to console */
	public static final boolean Echo = true;

	/** If true, includes hidden tokens in the token dump */
	public static final boolean Hidden = false;

	/** If true, echoes a token dump */
	public static final boolean LexOut = true;
	/** If true, echoes a parse-tree listing */
	public static final boolean TreeOut = true;
	/** If true, echoes a system result listing */
	public static final boolean SysOut = true;

	/** If true, the parse-tree is pretty-printed */
	public static final boolean TreePretty = true;
	/** The literal indent char(s) used for pretty-printing */
	public static final String Indents = "  ";

	// ------------------------------------------------------------------------
	// Custom parsing methods - customization required ------------------------

	/**
	 * Create a token stream using the test target specific lexer.
	 * 
	 * @param is
	 *        a snippet derived input stream
	 * @return a lexer derived token stream
	 */
	@Override
	public CommonTokenStream createLexerStream(ANTLRInputStream is) {
		// TODO: customization required - see 'Use' instructions.
		RemarkLexer lexer = new RemarkLexer(is);
		lexer.setTokenFactory(new RemarkTokenFactory());
		return new CommonTokenStream(lexer);
	}

	/**
	 * Create a parse-tree using the test target specific parser.
	 * 
	 * @param tokens
	 *        a lexer derived token stream
	 * @return a parse-tree
	 */
	@Override
	public ParseTree createParseTree(CommonTokenStream tokens) {
		// TODO: customization required - see 'Use' instructions.
		RemarkParser parser = new RemarkParser(tokens);
		RemarkContext tree = parser.remark(); // invoke main rule

		// required - annotate the parse-tree with its recognizer - required
		annotations.put(tree, parser);
		return tree;
	}

	/**
	 * Create a result string using the test target specific parse-tree and any applicable
	 * tree-walkers.
	 * 
	 * @param tree
	 *        a test target specific parser derived parse-tree
	 * @return a result string
	 */
	@Override
	public String createResults(ParseTree tree) {
		// TODO: customization required - see 'Use' instructions.
		ParseTreeWalker walker = new ParseTreeWalker();
		Converter conv = new Converter(new IOProcessor(new String[] { "-s" }));
		RemarkPhase01 phase01 = conv.processPhase01(tree, walker, new PhaseState());
		RemarkPhase02 phase02 = conv.processPhase02(tree, walker, phase01);
		RemarkPhase03 phase03 = conv.processPhase03(tree, walker, phase02);
		RemarkPhase04 phase04 = conv.processPhase04(tree, walker, phase03);
		return phase04.toString();
	}

	// ------------------------------------------------------------------------
	// Standard per-method configuration - change as desired ------------------

	@BeforeMethod
	public void setUp() throws Exception {
		// Log.setTestMode(true); // stop logger noise
	}

	@AfterMethod
	public void teadDown() throws Exception {}

	// ------------------------------------------------------------------------
	// Standard Data Provider Method - do not change --------------------------

	@DataProvider(name = "srcFilenames")
	public Object[][] listFilenames() {
		Object[][] data = new Object[][] {};
		String dir = TestUtils.concat(getBaseDir(), getDataDir());
		File d = new File(dir);
		if (d.isDirectory()) {
			Collection<File> files = TestUtils.listFiles(d, new String[] { Ext }, 1);
			if (files != null) {
				data = new Object[files.size()][1];
				for (int idx = 0; idx < files.size(); idx++) {
					File f = ((List<File>) files).get(idx);
					String rel = TestUtils.relative(d, f);
					data[idx][0] = TestUtils.changeExtent(rel, "");
				}
			}
		}
		return data;
	}

	// ------------------------------------------------------------------------
	// Actual Snippet Test Methods - do not change ----------------------------

	@Test(dataProvider = "srcFilenames")
	public void testLex(String name) {
		String data = readSrcString(name);
		String found = lexSource(name, data, Echo, Hidden, LexOut);
		String expecting = readLexString(name, found);
		Assert.assertEquals(found, expecting);
	}

	@Test(dataProvider = "srcFilenames")
	public void testParse(String name) {
		String data = readSrcString(name);
		String found = parseSource(name, data, Echo, TreeOut);
		String expecting = readParseString(name, found);
		Assert.assertEquals(found, expecting);
	}

	@Test(dataProvider = "srcFilenames")
	public void testResult(String name) throws Exception {
		String data = readSrcString(name);
		String found = resultsSource(name, data, Echo, SysOut);
		String expecting = readResultsString(name, found);
		Assert.assertEquals(found, expecting);
	}

	// ------------------------------------------------------------------------
	// Required Utility Methods - do not change -------------------------------

	@Override
	public String getBaseDir() {
		return BaseDir;
	}

	@Override
	public String getSnippetExt() {
		return Ext;
	}

	@Override
	public boolean getTreePretty() {
		return TreePretty;
	}

	@Override
	public String getIndents() {
		return Indents;
	}
}
