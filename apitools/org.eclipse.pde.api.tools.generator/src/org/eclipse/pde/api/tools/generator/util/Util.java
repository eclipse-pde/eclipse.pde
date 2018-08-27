/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.generator.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.compiler.CharOperation;

public class Util {
	private static final int DEFAULT_READING_SIZE = 8192;
	private final static String UTF_8 = "UTF-8"; //$NON-NLS-1$

	private static final byte[] CHARSET = new byte[] {
			99, 104, 97, 114, 115, 101, 116, 61 };
	private static final byte[] CLOSING_DOUBLE_QUOTE = new byte[] { 34 };
	private static final byte[] CONTENT = new byte[] {
			99, 111, 110, 116, 101, 110, 116, 61, 34 };
	private static final byte[] CONTENT_TYPE = new byte[] {
			34, 67, 111, 110, 116, 101, 110, 116, 45, 84, 121, 112, 101, 34 };
	private static final CRC32 CRC32 = new CRC32();
	public static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

	public static boolean contains(char[] name, char[][] names) {
		for (char[] currentName : names) {
			if (CharOperation.equals(currentName, name)) {
				return true;
			}
		}
		return false;
	}

	private static int getIndexOf(byte[] array, byte[] toBeFound, int start) {
		if (array == null || toBeFound == null) {
			return -1;
		}
		final int toBeFoundLength = toBeFound.length;
		final int arrayLength = array.length;
		if (arrayLength < toBeFoundLength) {
			return -1;
		}
		loop: for (int i = start, max = arrayLength - toBeFoundLength + 1; i < max; i++) {
			if (array[i] == toBeFound[0]) {
				for (int j = 1; j < toBeFoundLength; j++) {
					if (array[i + j] != toBeFound[j]) {
						continue loop;
					}
				}
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the given input stream's contents as a byte array. If a length is
	 * specified (ie. if length != -1), only length bytes are returned.
	 * Otherwise all bytes in the stream are returned. Note this doesn't close
	 * the stream.
	 *
	 * @throws IOException if a problem occured reading the stream.
	 */
	public static byte[] getInputStreamAsByteArray(InputStream stream, int length) throws IOException {
		byte[] contents;
		if (length == -1) {
			contents = new byte[0];
			int contentsLength = 0;
			int amountRead = -1;
			do {
				int amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE); // read
																							// at
																							// least
																							// 8K

				// resize contents if needed
				if (contentsLength + amountRequested > contents.length) {
					System.arraycopy(contents, 0, contents = new byte[contentsLength + amountRequested], 0, contentsLength);
				}

				// read as many bytes as possible
				amountRead = stream.read(contents, contentsLength, amountRequested);

				if (amountRead > 0) {
					// remember length of contents
					contentsLength += amountRead;
				}
			} while (amountRead != -1);

			// resize contents if necessary
			if (contentsLength < contents.length) {
				System.arraycopy(contents, 0, contents = new byte[contentsLength], 0, contentsLength);
			}
		} else {
			contents = new byte[length];
			int len = 0;
			int readSize = 0;
			while ((readSize != -1) && (len != length)) {
				// See PR 1FMS89U
				// We record first the read size. In this case len is the actual
				// read size.
				len += readSize;
				readSize = stream.read(contents, len, length - len);
			}
		}

		return contents;
	}

	/**
	 * Returns the given input stream's contents as a character array. If a
	 * length is specified (i.e. if length != -1), this represents the number of
	 * bytes in the stream. Note this doesn't close the stream.
	 *
	 * @throws IOException if a problem occured reading the stream.
	 */
	public static char[] getInputStreamAsCharArray(InputStream stream, int length, String encoding) throws IOException {
		BufferedReader reader = null;
		try {
			reader = encoding == null ? new BufferedReader(new InputStreamReader(stream)) : new BufferedReader(new InputStreamReader(stream, encoding));
		} catch (UnsupportedEncodingException e) {
			// encoding is not supported
			reader = new BufferedReader(new InputStreamReader(stream));
		}
		char[] contents;
		int totalRead = 0;
		if (length == -1) {
			contents = CharOperation.NO_CHAR;
		} else {
			// length is a good guess when the encoding produces less or the
			// same amount of characters than the file length
			contents = new char[length]; // best guess
		}

		while (true) {
			int amountRequested;
			if (totalRead < length) {
				// until known length is met, reuse same array sized eagerly
				amountRequested = length - totalRead;
			} else {
				// reading beyond known length
				int current = reader.read();
				if (current < 0) {
					break;
				}

				amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE); // read
																						// at
																						// least
																						// 8K

				// resize contents if needed
				if (totalRead + 1 + amountRequested > contents.length) {
					System.arraycopy(contents, 0, contents = new char[totalRead + 1 + amountRequested], 0, totalRead);
				}

				// add current character
				contents[totalRead++] = (char) current; // coming from
														// totalRead==length
			}
			// read as many chars as possible
			int amountRead = reader.read(contents, totalRead, amountRequested);
			if (amountRead < 0) {
				break;
			}
			totalRead += amountRead;
		}

		// Do not keep first character for UTF-8 BOM encoding
		int start = 0;
		if (totalRead > 0 && UTF_8.equals(encoding)) {
			if (contents[0] == 0xFEFF) { // if BOM char then skip
				totalRead--;
				start = 1;
			}
		}

		// resize contents if necessary
		if (totalRead < contents.length) {
			System.arraycopy(contents, start, contents = new char[totalRead], 0, totalRead);
		}

		return contents;
	}

	public static String getInterfaces(char[][] superinterfacesNames) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0, max = superinterfacesNames.length; i < max; i++) {
			if (i > 0) {
				buffer.append(',');
			}
			buffer.append(superinterfacesNames[i]);
		}
		return String.valueOf(buffer);
	}

	public static ZipOutputStream getOutputStream(File file) throws FileNotFoundException {
		return new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
	}

	public static String getSimpleName(char[] name) {
		return new String(getSimpleNameAsCharArray(name));
	}

	public static char[] getSimpleNameAsCharArray(char[] name) {
		int index = CharOperation.lastIndexOf('.', name);
		char[] subarray = CharOperation.subarray(name, index + 1, name.length);
		subarray = CharOperation.replaceOnCopy(subarray, '$', '.');
		return subarray;
	}

	public static void write(String rootDirName, String subDirName, String fileName, String contents) {
		File rootDir = new File(rootDirName);
		rootDir.mkdirs();
		File subDir = new File(rootDir, subDirName);
		subDir.mkdirs();
		String fname = fileName;
		if (fname.indexOf('/') != -1) {
			fname = fname.replace('/', '_');
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(subDir, fname)))) {
			writer.write(contents);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the given entry information into the output stream
	 *
	 * @param outputStream the output stream to write out to
	 * @param entryName
	 * @param bytes
	 * @param crc32
	 * @param bits
	 * @throws IOException
	 */
	public static void writeZipFileEntry(ZipOutputStream outputStream, String entryName, byte[] bytes) throws IOException {
		CRC32.reset();
		int byteArraySize = bytes.length;
		CRC32.update(bytes, 0, byteArraySize);
		ZipEntry entry = new ZipEntry(entryName);
		entry.setMethod(ZipEntry.DEFLATED);
		entry.setSize(byteArraySize);
		entry.setCrc(CRC32.getValue());
		outputStream.putNextEntry(entry);
		outputStream.write(bytes, 0, byteArraySize);
		outputStream.closeEntry();
	}

	/*
	 * We don't use getContentEncoding() on the URL connection, because it might
	 * leave open streams behind. See
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=117890
	 */
	public static char[] getURLContents(String docUrlValue) {
		InputStream stream = null;
		JarURLConnection connection2 = null;
		try {
			URL docUrl = new URL(docUrlValue);
			URLConnection connection = docUrl.openConnection();
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);

			if (connection instanceof JarURLConnection) {
				connection2 = (JarURLConnection) connection;
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=156307
				connection.setUseCaches(false);
			}
			try {
				stream = new BufferedInputStream(connection.getInputStream());
			} catch (IllegalArgumentException e) {
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=304316
				return null;
			} catch (NullPointerException e) {
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=304316
				return null;
			}
			String encoding = connection.getContentEncoding();
			byte[] contents = Util.getInputStreamAsByteArray(stream, connection.getContentLength());
			if (encoding == null) {
				int index = getIndexOf(contents, CONTENT_TYPE, 0);
				if (index != -1) {
					index = getIndexOf(contents, CONTENT, index);
					if (index != -1) {
						int offset = index + CONTENT.length;
						int index2 = getIndexOf(contents, CLOSING_DOUBLE_QUOTE, offset);
						if (index2 != -1) {
							final int charsetIndex = getIndexOf(contents, CHARSET, offset);
							if (charsetIndex != -1) {
								int start = charsetIndex + CHARSET.length;
								encoding = new String(contents, start, index2 - start, StandardCharsets.UTF_8);
							}
						}
					}
				}
			}
			if (contents != null) {
				if (encoding != null) {
					return new String(contents, encoding).toCharArray();
				} else {
					// platform encoding is used
					return new String(contents).toCharArray();
				}
			}
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// ignore. see bug
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=120559
		} catch (SocketException e) {
			// ignore. see bug
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=247845
		} catch (UnknownHostException e) {
			// ignore. see bug
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=247845
		} catch (ProtocolException e) {
			// ignore. see bug
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=247845
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
			if (connection2 != null) {
				try {
					connection2.getJarFile().close();
				} catch (IOException e) {
					// ignore
				} catch (IllegalStateException e) {
					/*
					 * ignore. Can happen in case the stream.close() did close
					 * the jar file see
					 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=140750
					 */
				}
			}
		}
		return null;
	}

	private static void collectAllFiles(File root, ArrayList<File> collector, FileFilter fileFilter) {
		File[] files = root.listFiles(fileFilter);
		for (final File currentFile : files) {
			if (currentFile.isDirectory()) {
				collectAllFiles(currentFile, collector, fileFilter);
			} else {
				collector.add(currentFile);
			}
		}
	}

	public static File[] getAllFiles(File root, FileFilter fileFilter) {
		ArrayList<File> files = new ArrayList<>();
		if (root.isDirectory()) {
			collectAllFiles(root, files, fileFilter);
			// get the jmods
			// assumption all the jmods are in jmod folder
			// jmods are present post the jars
			String path = root.toString();
			IPath newPath = new Path(path);
			newPath = newPath.removeLastSegments(1).addTrailingSeparator();
			newPath = newPath.append("jmods"); //$NON-NLS-1$
			File jmod = newPath.toFile();
			if (jmod.exists()) {
				File[] listFiles = jmod.listFiles(fileFilter);
				for (File file : listFiles) {
					files.add(file);
				}
			}
			File[] result = new File[files.size()];
			files.toArray(result);
			return result;
		}
		return null;
	}

	public static String getProfileFileName(String profileName) {
		return profileName.replace('/', '_');
	}
}
