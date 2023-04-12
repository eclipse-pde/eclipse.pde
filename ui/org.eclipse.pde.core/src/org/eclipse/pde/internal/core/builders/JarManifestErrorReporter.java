/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.builders;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.builders.IncrementalErrorReporter.VirtualMarker;

public class JarManifestErrorReporter extends ErrorReporter {
	/**
	 * Map of IHeader by name
	 */
	protected Map<String, JarManifestHeader> fHeaders;

	protected IDocument fTextDocument;

	public JarManifestErrorReporter(IFile file) {
		super(file);
		fTextDocument = createDocument(file);
	}

	private String getHeaderName(String line) {
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == ':') {
				return line.substring(0, i);
			}
			if ((c < 'A' || 'Z' < c) && (c < 'a' || 'z' < c) && (c < '0' || '9' < c)) {
				if (i == 0) {
					return null;
				}
				if (c != '-' && c != '_') {
					return null;
				}
			}
		}
		return null;
	}

	protected int getPackageLine(IHeader header, ManifestElement element) {
		String packageName = element.getValue();
		if (element.getDirectiveKeys() != null || element.getKeys() != null) {
			return getLine(header, packageName + ";"); //$NON-NLS-1$
		}

		// check for this exact package on the last line
		try {
			int lineNumberZeroBased = header.getLineNumber() - 1;
			IRegion lineRegion = fTextDocument.getLineInformation(lineNumberZeroBased + header.getLinesSpan() - 1);
			String lineStr = fTextDocument.get(lineRegion.getOffset(), lineRegion.getLength());
			if (lineStr.endsWith(packageName)) {
				return lineNumberZeroBased + header.getLinesSpan();
			}
		} catch (BadLocationException ble) {
			PDECore.logException(ble);
		}

		// search all except last line
		return getLine(header, packageName + ","); //$NON-NLS-1$
	}

	protected int getLine(IHeader header, String valueSubstring) {
		int lineNumberZeroBased = header.getLineNumber() - 1;
		for (int l = lineNumberZeroBased; l < lineNumberZeroBased + header.getLinesSpan(); l++) {
			try {
				IRegion lineRegion = fTextDocument.getLineInformation(l);
				String lineStr = fTextDocument.get(lineRegion.getOffset(), lineRegion.getLength());
				if (lineStr.contains(valueSubstring)) {
					return l + 1;
				}
			} catch (BadLocationException ble) {
				PDECore.logException(ble);
			}
		}
		// it might span mutliple lines, try a longer algorithm
		try {
			IRegion lineRegion = fTextDocument.getLineInformation(lineNumberZeroBased);
			String lineStr = fTextDocument.get(lineRegion.getOffset(), lineRegion.getLength());
			for (int l = lineNumberZeroBased + 1; l < lineNumberZeroBased + header.getLinesSpan(); l++) {
				lineRegion = fTextDocument.getLineInformation(l);
				lineStr += fTextDocument.get(lineRegion.getOffset() + 1/* the space */, lineRegion.getLength());
				if (lineStr.contains(valueSubstring)) {
					return l;
				}
			}
		} catch (BadLocationException ble) {
			PDECore.logException(ble);
		}
		return header.getLineNumber();
	}

	/**
	 * @param document
	 */
	protected void parseManifest(IDocument document, IProgressMonitor monitor) {
		try {
			fHeaders = new HashMap<>();
			JarManifestHeader header = null;
			int l = 0;
			for (; l < document.getNumberOfLines(); l++) {
				if (l % 100 == 0) {
					checkCanceled(monitor);
				}
				IRegion lineInfo = document.getLineInformation(l);
				String line = document.get(lineInfo.getOffset(), lineInfo.getLength());
				// test lines' length
				String lineDelimiter = document.getLineDelimiter(l);
				if (lineDelimiter == null) {
					lineDelimiter = ""; //$NON-NLS-1$
				}
				ByteBuffer byteBuf = StandardCharsets.UTF_8.encode(line);
				int lineNumber = l + 1;
				if (byteBuf.limit() + lineDelimiter.length() > 512) {
					report(PDECoreMessages.BundleErrorReporter_lineTooLong, lineNumber, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
					return;
				}
				// parse
				if (line.length() == 0) {
					// Empty Line
					if (l == 0) {
						report(PDECoreMessages.BundleErrorReporter_noMainSection, 1, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
						return;
					}
					/* flush last line */
					if (header != null) {
						fHeaders.put(header.getName().toLowerCase(), header);
						header = null;
					}
					break; /* done processing main attributes */
				}
				if (line.charAt(0) == ' ') {
					// Continuation Line
					if (l == 0) { /* if no previous line */
						report(PDECoreMessages.BundleErrorReporter_noMainSection, 1, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
						return;
					}
					if (header != null) {
						header.append(line.substring(1));
					}

					continue;
				}
				// Expecting New Header
				if (header != null) {
					fHeaders.put(header.getName().toLowerCase(), header);
					header = null;
				}

				int colon = line.indexOf(':');
				if (colon == -1) { /* no colon */
					report(PDECoreMessages.BundleErrorReporter_noColon, lineNumber, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
					return;
				}
				String headerName = getHeaderName(line);
				if (headerName == null) {
					report(PDECoreMessages.BundleErrorReporter_invalidHeaderName, lineNumber, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
					return;
				}
				if (line.length() < colon + 2 || line.charAt(colon + 1) != ' ') {
					report(PDECoreMessages.BundleErrorReporter_noSpaceValue, lineNumber, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
					return;
				}
				if ("Name".equals(headerName)) { //$NON-NLS-1$
					report(PDECoreMessages.BundleErrorReporter_nameHeaderInMain, lineNumber, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
					return;
				}
				header = new JarManifestHeader(headerName, line.substring(colon + 2), lineNumber, this);
				if (fHeaders.containsKey(header.getName().toLowerCase())) {
					report(PDECoreMessages.BundleErrorReporter_duplicateHeader, lineNumber, CompilerFlags.WARNING, PDEMarkerFactory.CAT_OTHER);
				}

			}
			if (header != null) {
				// lingering header, line not terminated
				VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_noLineTermination, l, CompilerFlags.ERROR, PDEMarkerFactory.M_NO_LINE_TERMINATION, PDEMarkerFactory.CAT_FATAL);
				if (marker != null) {
					// Check whether last line is purely whitespace, and add
					// this information to the marker.
					IRegion lineInfo = document.getLineInformation(document.getNumberOfLines() - 1);
					String line = document.get(lineInfo.getOffset(), lineInfo.getLength());
					marker.setAttribute(PDEMarkerFactory.ATTR_HAS_CONTENT, !line.matches("\\s+")); //$NON-NLS-1$
				}
				return;
			}
			// If there is any more headers, not starting with a Name header
			// the empty lines are a mistake, report it.
			for (; l < document.getNumberOfLines(); l++) {
				IRegion lineInfo = document.getLineInformation(l);
				String line = document.get(lineInfo.getOffset(), lineInfo.getLength());
				if (line.length() == 0) {
					continue;
				}
				if (!line.startsWith("Name:")) { //$NON-NLS-1$
					report(PDECoreMessages.BundleErrorReporter_noNameHeader, l, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
				}
				break;
			}

			return;
		} catch (BadLocationException ble) {
			PDECore.logException(ble);
		}
	}

	protected void reportIllegalAttributeValue(IHeader header, String key, String value) {
		String msg = NLS.bind(PDECoreMessages.BundleErrorReporter_att_value, (new String[] {value, key}));
		report(msg, getLine(header, key + "="), CompilerFlags.ERROR, //$NON-NLS-1$
				PDEMarkerFactory.CAT_FATAL);
	}

	protected void reportIllegalValue(IHeader header, String value) {
		String msg = NLS.bind(PDECoreMessages.BundleErrorReporter_illegal_value, value);
		report(msg, getLine(header, value), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
	}

	protected void reportIllegalDirectiveValue(IHeader header, String key, String value) {
		String msg = NLS.bind(PDECoreMessages.BundleErrorReporter_dir_value, (new String[] {value, key}));
		report(msg, getLine(header, key + ":="), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL); //$NON-NLS-1$
	}

	protected void validateAttributeValue(IHeader header, ManifestElement element, String key, String[] allowedValues) {
		String value = element.getAttribute(key);
		if (value == null) {
			return;
		}
		for (String allowedValue : allowedValues) {
			if (allowedValue.equals(value)) {
				return;
			}
		}
		reportIllegalAttributeValue(header, key, value);
	}

	protected void validateBooleanAttributeValue(IHeader header, ManifestElement element, String key) {
		validateAttributeValue(header, element, key, BOOLEAN_VALUES);
	}

	protected void validateBooleanDirectiveValue(IHeader header, ManifestElement element, String key) {
		validateDirectiveValue(header, element, key, BOOLEAN_VALUES);
	}

	protected void validateBooleanValue(IHeader header) {
		validateHeaderValue(header, BOOLEAN_VALUES);
	}

	@Override
	protected void validate(IProgressMonitor monitor) {
		if (fTextDocument != null) {
			parseManifest(fTextDocument, monitor);
		}
	}

	protected void validateDirectiveValue(IHeader header, ManifestElement element, String key, String[] allowedValues) {
		String value = element.getDirective(key);
		if (value == null) {
			return;
		}
		for (String allowedValue : allowedValues) {
			if (allowedValue.equals(value)) {
				return;
			}
		}
		reportIllegalDirectiveValue(header, key, value);
	}

	protected void validateHeaderValue(IHeader header, String[] allowedValues) {
		ManifestElement[] elements = header.getElements();
		if (elements.length > 0) {
			for (String allowedValue : allowedValues) {
				if (allowedValue.equals(elements[0].getValue())) {
					return;
				}
			}
			reportIllegalValue(header, elements[0].getValue());
		}
	}

	protected IHeader validateRequiredHeader(String name) {
		IHeader header = fHeaders.get(name.toLowerCase());
		if (header == null) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_headerMissing, name), 1, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
		}
		return header;
	}

	protected IHeader getHeader(String key) {
		return fHeaders.get(key.toLowerCase());
	}

	protected void checkCanceled(IProgressMonitor monitor) throws OperationCanceledException {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}
}
