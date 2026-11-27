/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.text.p2inf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.util.PropertiesUtil;

/**
 * A lightweight text model of a p2.inf file. Like the text
 * {@link org.eclipse.pde.internal.core.text.build.BuildModel} for
 * build.properties, the entries are read with {@link Properties} and then
 * located in the document so that problems can be reported on the right line.
 */
public class P2InfTextModel extends AbstractEditingModel {

	private final Map<String, P2InfEntry> fEntries = new HashMap<>();

	public P2InfTextModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}

	@Override
	public void load(InputStream source, boolean outOfSync) {
		fEntries.clear();
		Properties properties = new Properties();
		try (Reader reader = new InputStreamReader(source, getCharset())) {
			properties.load(reader);
		} catch (IOException | IllegalArgumentException e) {
			fLoaded = false;
			return;
		}
		for (String name : properties.stringPropertyNames()) {
			fEntries.put(name, new P2InfEntry(name));
		}
		fLoaded = true;
		adjustOffsets(getDocument());
	}

	@Override
	public void adjustOffsets(IDocument document) {
		try {
			boolean continuation = false;
			for (int i = 0, lines = document.getNumberOfLines(); i < lines; i++) {
				int lineOffset = document.getLineOffset(i);
				String line = document.get(lineOffset, document.getLineLength(i));
				String trimmed = line.stripLeading();
				String content = trimmed.stripTrailing();
				if (!continuation) {
					if (content.isEmpty() || content.startsWith("#") || content.startsWith("!")) { //$NON-NLS-1$ //$NON-NLS-2$
						continue;
					}
					P2InfEntry entry = fEntries.get(unescape(getKey(content)));
					if (entry != null) {
						entry.setOffset(lineOffset + (line.length() - trimmed.length()));
					}
				}
				continuation = endsWithContinuation(content);
			}
		} catch (BadLocationException e) {
			// offsets are best effort, entries without offset are reported on line 1
		}
	}

	/**
	 * @return all entries of the file ordered by their position in the document
	 */
	public List<P2InfEntry> getEntries() {
		List<P2InfEntry> entries = new ArrayList<>(fEntries.values());
		entries.sort(Comparator.comparingInt(P2InfEntry::getOffset));
		return entries;
	}

	/**
	 * The entries are keyed by the unescaped name (as returned by
	 * {@link Properties}), whereas the document contains the escaped key, e.g.
	 * <code>a\ b</code> or <code>\u0041</code>.
	 */
	private static String unescape(String key) {
		try {
			return PropertiesUtil.windEscapeChars(key);
		} catch (IllegalArgumentException e) {
			// malformed escape sequence, same fallback as the build.properties model
			return key;
		}
	}

	/**
	 * The key ends at the first unescaped '=', ':' or white space.
	 */
	private static String getKey(String line) {
		int end = 0;
		while (end < line.length()) {
			char c = line.charAt(end);
			if (c == '\\') {
				end += 2;
				continue;
			}
			if (c == '=' || c == ':' || Character.isWhitespace(c)) {
				break;
			}
			end++;
		}
		return line.substring(0, Math.min(end, line.length()));
	}

	/**
	 * A line is continued if it ends with an odd number of backslashes.
	 */
	private static boolean endsWithContinuation(String line) {
		int backslashes = 0;
		for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
			backslashes++;
		}
		return backslashes % 2 == 1;
	}
}
