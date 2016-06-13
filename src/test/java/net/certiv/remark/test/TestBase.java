/* Copyright � 2015 Gerald Rosenberg.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the License.md file.
 */
package net.certiv.remark.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;

public abstract class TestBase {

	/** Platform dependent end-of-line marker */
	public static final String Eol = System.lineSeparator();

	private static final String DataDir = "test.snippets";
	private static final String ResultDir = "test.expected";

	private static final String LexExt = "Tokens.txt";
	private static final String ParseExt = "Tree.txt";
	private static final String ResultExt = "Result.txt";

	/* Indent level counter for pretty printing */
	private int level = 0;

	/** Annotations map */
	public static final ParseTreeProperty<Parser> annotations = new ParseTreeProperty<>();

	public TestBase() {
		super();
	}

	public String getDataDir() {
		return DataDir;
	}

	public abstract String getBaseDir();

	public abstract String getSnippetExt();

	public abstract boolean getTreePretty();

	public abstract String getIndents();

	public String lexSource(String name, String source, boolean echo, boolean hidden, boolean lexout) {
		CommonTokenStream tokens = produceTokens(name, source);
		tokens.fill();
		StringBuilder sb = new StringBuilder();
		for (Token token : tokens.getTokens()) {
			if (token.getChannel() == 0 || hidden) {
				String tok = token.toString().trim() + Eol;
				sb.append(tok);
				if (echo && lexout) System.out.print(tok);
			}
		}
		if (echo && lexout) System.out.println();
		return sb.toString();
	}

	public CommonTokenStream produceTokens(String name, String data) {
		ANTLRInputStream is = new ANTLRInputStream(data);
		is.name = name;
		return createLexerStream(is);
	}

	public abstract CommonTokenStream createLexerStream(ANTLRInputStream is);

	public String parseSource(String name, String source, boolean echo, boolean treeout) {
		CommonTokenStream tokens = produceTokens(name, source);
		ParseTree tree = createParseTree(tokens);
		Parser parser = annotations.get(tree);
		List<String> ruleNamesList = Arrays.asList(parser.getRuleNames());
		String strTree = convertToStringTree(tree, ruleNamesList);
		annotations.removeFrom(tree);
		if (echo && treeout) System.out.println(strTree + Eol);
		return strTree;
	}

	private String convertToStringTree(ParseTree tree, List<String> ruleNamesList) {
		if (!getTreePretty()) { return Trees.toStringTree(tree, ruleNamesList); }
		return toPrettyTree(tree, ruleNamesList);
	}

	/**
	 * Pretty print out a whole tree. {@link #getNodeText} is used on the node payloads to get the
	 * text for the nodes. (Derived from Trees.toStringTree(....))
	 */
	public String toPrettyTree(final Tree t, final List<String> ruleNames) {
		level = 0;
		return process(t, ruleNames).replaceAll("(?m)^\\s+$", "").replaceAll("\\r?\\n\\r?\\n", Eol);
	}

	private String process(final Tree t, final List<String> ruleNames) {
		if (t.getChildCount() == 0) return Utils.escapeWhitespace(Trees.getNodeText(t, ruleNames), false);
		StringBuilder sb = new StringBuilder();
		sb.append(lead(level));
		level++;
		String s = Utils.escapeWhitespace(Trees.getNodeText(t, ruleNames), false);
		sb.append(s + ' ');
		for (int i = 0; i < t.getChildCount(); i++) {
			sb.append(process(t.getChild(i), ruleNames));
		}
		level--;
		sb.append(lead(level));
		return sb.toString();
	}

	private String lead(int level) {
		StringBuilder sb = new StringBuilder();
		if (level > 0) {
			sb.append(Eol);
			for (int cnt = 0; cnt < level; cnt++) {
				sb.append(getIndents());
			}
		}
		return sb.toString();
	}

	public abstract ParseTree createParseTree(CommonTokenStream tokens);

	public String resultsSource(String name, String source, boolean echo, boolean treeout) {
		CommonTokenStream tokens = produceTokens(name, source);
		ParseTree tree = createParseTree(tokens);
		String results = createResults(tree);
		annotations.removeFrom(tree);
		if (echo && treeout) System.out.println(results + Eol);
		return results;
	}

	public abstract String createResults(ParseTree tree);

	public String readSrcString(String name) {
		return readString(DataDir, name, getSnippetExt());
	}

	/**
	 * Reads the expected lexer dump results from disk. If no file is found, writes the given data
	 * to the lexer dump file.
	 * 
	 * @param name
	 *        the name of the snippet
	 * @param data
	 *        the data to write
	 * @return the data read from the snippet lexer dump file
	 */
	public String readLexString(String name, String data) {
		return readStringExt(name, data, LexExt);
	}

	/**
	 * Reads the expected parser results from disk. If no file is found, writes the given data to
	 * the parser results file.
	 * 
	 * @param name
	 *        the name of the snippet
	 * @param data
	 *        the data to write
	 * @return the data read from the snippet parser results file
	 */
	public String readParseString(String name, String data) {
		return readStringExt(name, data, ParseExt);
	}

	/**
	 * Reads the expected parser results from disk. If no file is found, writes the given data to
	 * the parser results file.
	 * 
	 * @param name
	 *        the name of the snippet
	 * @param data
	 *        the data to write
	 * @return the data read from the snippet parser results file
	 */
	public String readResultsString(String name, String data) {
		return readStringExt(name, data, ResultExt);
	}

	private String readStringExt(String name, String data, String ext) {
		String expecting = readString(ResultDir, name, ext);
		if (expecting.length() == 0) {
			writeString(ResultDir, name, data, ext);
		}
		return expecting;
	}

	private String readString(String dir, String name, String ext) {
		name = convertName(dir, name, ext);
		File f = new File(name);
		String data = "";
		if (f.isFile()) {
			try {
				data = TestUtils.read(f);
			} catch (IOException e) {
				System.err.println("Read failed: " + e.getMessage());
			}
		}
		return data;
	}

	private void writeString(String dir, String name, String data, String ext) {
		name = convertName(dir, name, ext);
		File f = new File(name);
		File p = f.getParentFile();
		if (!p.exists()) {
			if (!p.mkdirs()) {
				System.err.println("Failed to create directory: " + p.getAbsolutePath());
				return;
			}
		} else if (p.isFile()) {
			System.err.println("Cannot make directory: " + p.getAbsolutePath());
			return;
		}

		if (f.exists() && f.isFile()) {
			f.delete();
		}
		try {
			TestUtils.write(f, data, false);
		} catch (IOException e) {
			System.err.println("Write failed: " + e.getMessage());
		}
	}

	private String convertName(String dir, String name, String ext) {
		int idx = name.lastIndexOf('.');
		if (idx > 0) {
			name = name.substring(0, idx);
		}
		return TestUtils.concat(getBaseDir(), dir, name + ext);
	}
}
