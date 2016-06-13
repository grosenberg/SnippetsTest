/* Copyright © 2015 Gerald Rosenberg.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the License.md file.
 */
package net.certiv.remark.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Replaces commons.*
 */
public class TestUtils {

	private TestUtils() {}

	/**
	 * Returns file content as string, reading from a path. Throws runtime exception in case of FileNotFoundException or
	 * IOException.
	 * 
	 * @param filename
	 * @return the given file content
	 * @throws IOException
	 */
	public static String read(String filename) throws IOException {
		File file = new File(filename);
		return read(file);
	}

	public static String read(File file) throws IOException {
		BufferedReader reader = Files.newBufferedReader(file.toPath());
		StringBuilder sb = new StringBuilder();
		int c = reader.read();
		while (c != -1) {
			sb.append((char) c);
			c = reader.read();
		}
		return sb.toString();
	}

	/**
	 * Returns file content as string, reading from a url. Throws runtime exception in case of FileNotFoundException or
	 * IOException.
	 * 
	 * @param fileurl the url of the file to read.
	 * @return file content as string.
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static String read(URL url) throws URISyntaxException, IOException {
		File file = toFile(url);
		return read(file);
	}

	public static File toFile(URL url) throws URISyntaxException {
		return new File(url.toURI());
	}

	/**
	 * Writes a string to the specified file using the default encoding.
	 * 
	 * If the file path doesn't exist, it's created. If the file exists, it is overwritten.
	 * 
	 * @param pathname the path to the file.
	 * @param data the string to write.
	 * @throws IOException
	 */
	public static void writeFile(String pathname, String data) throws IOException {
		write(new File(pathname), data, false);
	}

	public static void write(File file, String data, boolean append) throws IOException {
		Set<OpenOption> options = new HashSet<OpenOption>();
		options.add(StandardOpenOption.CREATE);
		options.add(StandardOpenOption.WRITE);
		if (append) {
			options.add(StandardOpenOption.APPEND);
		} else {
			options.add(StandardOpenOption.TRUNCATE_EXISTING);
		}
		write(file, data, options.toArray(new OpenOption[options.size()]));
	}

	private static void write(File file, String data, OpenOption... options) throws IOException {
		Path path = file.toPath();
		Files.write(path, data.getBytes(), options);
	}

	public static Charset charsetForNameOrDefault(String encoding) {
		Charset charset = (encoding == null) ? Charset.defaultCharset() : Charset.forName(encoding);
		return charset;
	}

	/**
	 * Replaces all backslashes with slash char. Throws NPE if the original path is null.
	 * 
	 * @param original the path to normalize.
	 * @return the normalized path
	 */
	public static String normalizePathname(String pathname) {
		return pathname.replaceAll("\\\\", "/");
	}

	/**
	 * Returns the pathname of f relative to the directory d
	 * 
	 * @param d
	 * @param f
	 * @return
	 */
	public static String relative(File d, File f) {
		String dPath = d.getAbsolutePath();
		String fPath = f.getAbsolutePath();
		if (fPath.startsWith(dPath)) {
			fPath = fPath.substring(dPath.length());
			fPath = normalizePathname(fPath);
			if (fPath.startsWith("/")) {
				fPath = fPath.substring(1);
			}
		}
		return fPath;
	}

	/**
	 * Concats pathname parts - returns a normalized pathname
	 * 
	 * @param base
	 * @param parts
	 * @return
	 */
	public static String concat(String base, String... parts) {
		Path path = Paths.get(base, parts);
		return normalizePathname(path.toString());
	}

	public static String extent(String pathname) {
		if (pathname != null) {
			int dot = pathname.lastIndexOf(".");
			if (dot != -1 && dot + 1 < pathname.length() - 1) {
				return pathname.substring(dot + 1);
			}
		}
		return "";
	}

	/**
	 * Change or remove file extension.
	 * 
	 * @param pathname
	 * @param ext
	 */
	public static String changeExtent(String pathname, String ext) {
		if (ext == null) ext = "";
		if (ext.length() > 0 && !ext.startsWith(".")) ext = "." + ext;
		int dot = pathname.lastIndexOf(".");
		if (dot != -1) {
			return pathname.substring(0, dot) + ext;
		}
		return pathname + ext;
	}

	public static int pathDepth(File f) {
		String pathname = normalizePathname(f.getPath());
		if (pathname.endsWith("/")) {
			pathname = pathname.substring(0, pathname.length() - 2);
		}
		String[] p = pathname.split(":/");
		if (p.length > 1) {
			pathname = p[1];
		}
		p = pathname.split("/");
		return p.length;
	}

	/**
	 * Walks file tree returning list of files found with matching file extents that exist within the given depth offset
	 * from the starting directory and that are accessible.
	 * 
	 * @param f starting directory
	 * @param exts list (array) of acceptable extents
	 * @param depth allowed directory depth offset from the starting directory
	 * @return
	 */
	public static Collection<File> listFiles(File f, String[] exts, int depth) {
		List<File> files = new ArrayList<>();
		if (f.isDirectory()) {
			int d = pathDepth(f) + depth;
			FilterFileExts filter = new FilterFileExts(files, exts, d);
			try {
				Files.walkFileTree(f.toPath(), filter);
			} catch (IOException e) {}
		}
		return files;
	}

	public static class FilterFileExts extends SimpleFileVisitor<Path> {

		private List<File> files;
		private String[] exts;
		private int depth;

		public FilterFileExts(List<File> files, String[] exts, int depth) {
			super();
			this.files = files;
			this.exts = new String[exts.length];
			this.depth = depth;
			for (int idx = 0; idx < exts.length; idx++) {
				if (exts[idx].startsWith(".")) {
					this.exts[idx] = exts[idx].substring(1);
				} else {
					this.exts[idx] = exts[idx];
				}
			}
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			File f = dir.toFile();
			if (pathDepth(f) <= depth) {
				return FileVisitResult.CONTINUE;
			} else {
				return FileVisitResult.SKIP_SIBLINGS;
			}
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
			File f = path.toFile();
			if (f.isFile()) {
				String pExt = extent(path.toString());
				if (pExt.length() > 0) {
					for (String e : exts) {
						if (e.equals(pExt)) {
							files.add(f);
							return FileVisitResult.CONTINUE;
						}
					}
				}
			}
			return FileVisitResult.CONTINUE;
		}
	}
}
