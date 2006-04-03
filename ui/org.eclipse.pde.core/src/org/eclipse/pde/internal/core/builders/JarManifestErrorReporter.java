/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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

public class JarManifestErrorReporter extends ErrorReporter {

	protected static final String[] BOOLEAN_VALUES = new String[] { "true", //$NON-NLS-1$
			"false" }; //$NON-NLS-1$

	/**
	 * Map of IHeader by name
	 */
	protected Map fHeaders;

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
			if ((c < 'A' || 'Z' < c) && (c < 'a' || 'z' < c)
					&& (c < '0' || '9' < c)) {
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

	protected int getLine(IHeader header, String valueSubstring) {
		for (int l = header.getLineNumber(); l < header.getLineNumber()
				+ header.getLinesSpan(); l++) {
			try {
				IRegion lineRegion = fTextDocument.getLineInformation(l);
				String lineStr = fTextDocument.get(lineRegion.getOffset(),
						lineRegion.getLength());
				if (lineStr.indexOf(valueSubstring) >= 0) {
					return l + 1;
				}
			} catch (BadLocationException ble) {
				PDECore.logException(ble);
			}
		}
		// it might span mutliple lines, try a longer algorithm
		try {
			IRegion lineRegion = fTextDocument.getLineInformation(header
					.getLineNumber());
			String lineStr = fTextDocument.get(lineRegion.getOffset(),
					lineRegion.getLength());
			for (int l = header.getLineNumber() + 1; l < header.getLineNumber()
					+ header.getLinesSpan(); l++) {
				lineRegion = fTextDocument.getLineInformation(l);
				lineStr += fTextDocument.get(
						lineRegion.getOffset() + 1/* the space */, lineRegion
								.getLength());
				if (lineStr.indexOf(valueSubstring) >= 0) {
					return l;
				}
			}
		} catch (BadLocationException ble) {
			PDECore.logException(ble);
		}
		return header.getLineNumber() + 1;
	}

	/**
	 * @param document
	 * @return Map of Header by header name
	 */
	protected void parseManifest(IDocument document, IProgressMonitor monitor) {
		try {
			fHeaders = new HashMap();
			JarManifestHeader header = null;
			int l = 0;
			for (; l < document.getNumberOfLines(); l++) {
				if(l % 100 ==0)
					checkCanceled(monitor);
				IRegion lineInfo = document.getLineInformation(l);
				String line = document.get(lineInfo.getOffset(), lineInfo
						.getLength());
				// test lines' length
				Charset charset = Charset.forName("UTF-8"); //$NON-NLS-1$
				String lineDelimiter = document.getLineDelimiter(l);
				if (lineDelimiter == null) {
					lineDelimiter = ""; //$NON-NLS-1$
				}
				ByteBuffer byteBuf = charset.encode(line);
				if (byteBuf.limit() + lineDelimiter.length() > 512) {
					report(
							PDECoreMessages.BundleErrorReporter_lineTooLong, 
							l + 1, CompilerFlags.ERROR);
					return;
				}
				// parse
				if (line.length() == 0) {
					// Empty Line
					if (l == 0) {
						report(
								PDECoreMessages.BundleErrorReporter_noMainSection, 
								1, CompilerFlags.ERROR);
						return;
					}
					/* flush last line */
					if (header != null) {
						fHeaders.put(header.getName(), header);
						header = null;
					}
					break; /* done processing main attributes */
				}
				if (line.charAt(0) == ' ') {
					// Continuation Line
					if (l == 0) { /* if no previous line */
						report(
								PDECoreMessages.BundleErrorReporter_noMainSection, 
								1, CompilerFlags.ERROR);
						return;
					}
					if (header != null) {
						header.append(line.substring(1));
					}

					continue;
				}
				// Expecting New Header
				if (header != null) {
					fHeaders.put(header.getName(), header);
					header = null;
				}

				int colon = line.indexOf(':');
				if (colon == -1) { /* no colon */
					report(
							PDECoreMessages.BundleErrorReporter_noColon, 
							l + 1, CompilerFlags.ERROR);
					return;
				}
				String headerName = getHeaderName(line);
				if (headerName == null) {
					report(
							PDECoreMessages.BundleErrorReporter_invalidHeaderName, 
							l + 1, CompilerFlags.ERROR);
					return;
				}
				if (line.length() < colon + 2 || line.charAt(colon + 1) != ' ') {
					report(
							PDECoreMessages.BundleErrorReporter_noSpaceValue, 
							l + 1, CompilerFlags.ERROR);
					return;
				}
				if ("Name".equals(headerName)) { //$NON-NLS-1$
					report(
							PDECoreMessages.BundleErrorReporter_nameHeaderInMain, 
							l + 1, CompilerFlags.ERROR);
					return;
				}
				header = new JarManifestHeader(headerName, line
						.substring(colon + 2), l, this);
				if (fHeaders.containsKey(header.getName())) {
					report(
							PDECoreMessages.BundleErrorReporter_duplicateHeader, 
							l + 1, CompilerFlags.WARNING);
				}

			}
			if (header != null) {
				// lingering header, line not terminated
				report(
						PDECoreMessages.BundleErrorReporter_noLineTermination, 
						l, CompilerFlags.ERROR);
				return;
			}
			// If there is any more headers, not starting with a Name header
			// the empty lines are a mistake, report it.
			for (; l < document.getNumberOfLines(); l++) {
				IRegion lineInfo = document.getLineInformation(l);
				String line = document.get(lineInfo.getOffset(), lineInfo
						.getLength());
				if (line.length() == 0) {
					continue;
				}
				if (!line.startsWith("Name:")) { //$NON-NLS-1$
					report(
							PDECoreMessages.BundleErrorReporter_noNameHeader, 
							l, CompilerFlags.ERROR);
					break;
				}

			}

			return;
		} catch (BadLocationException ble) {
			PDECore.logException(ble);
		}
	}

	protected void reportIllegalAttributeValue(IHeader header, String key,
			String value) {
		String msg = NLS.bind(PDECoreMessages.BundleErrorReporter_att_value, (new String[] { value, key })); 
		report(msg, getLine(header, key + "="), CompilerFlags.ERROR); //$NON-NLS-1$
	}

	protected void reportIllegalValue(IHeader header) {
		String msg = NLS.bind(PDECoreMessages.BundleErrorReporter_illegal_value, header.getValue()); 
		report(msg, getLine(header, header.getValue()), CompilerFlags.ERROR); 
	}

	protected void reportIllegalDirectiveValue(IHeader header, String key,
			String value) {
		String msg = NLS.bind(PDECoreMessages.BundleErrorReporter_dir_value, (new String[] { value, key })); 
		report(msg, getLine(header, key + ":="), CompilerFlags.ERROR); //$NON-NLS-1$
	}

	protected void validateAttributeValue(IHeader header,
			ManifestElement element, String key, String[] allowedValues) {
		String value = element.getAttribute(key);
		if (value == null) {
			return;
		}
		for (int i = 0; i < allowedValues.length; i++) {
			if (allowedValues[i].equals(value)) {
				return;
			}
		}
		reportIllegalAttributeValue(header, key, value);
	}

	protected void validateBooleanAttributeValue(IHeader header,
			ManifestElement element, String key) {
		validateAttributeValue(header, element, key, BOOLEAN_VALUES);
	}

	protected void validateBooleanDirectiveValue(IHeader header,
			ManifestElement element, String key) {
		validateDirectiveValue(header, element, key, BOOLEAN_VALUES);
	}
	
	protected void validateBooleanValue(IHeader header){
		validateHeaderValue(header, BOOLEAN_VALUES);
	}

	protected void validate(IProgressMonitor monitor) {
		if (fTextDocument == null) {
			return;
		}
		parseManifest(fTextDocument, monitor);
	}

	protected void validateDirectiveValue(IHeader header,
			ManifestElement element, String key, String[] allowedValues) {
		String value = element.getDirective(key);
		if (value == null) {
			return;
		}
		for (int i = 0; i < allowedValues.length; i++) {
			if (allowedValues[i].equals(value)) {
				return;
			}
		}
		reportIllegalDirectiveValue(header, key, value);
	}
	
	protected void validateHeaderValue(IHeader header, String[] allowedValues) {
		if (header.getValue() == null) {
			return;
		}
		for (int i = 0; i < allowedValues.length; i++) {
			if (allowedValues[i].equals(header.getValue())) {
				return;
			}
		}
		reportIllegalValue(header);
	}
	protected void checkCanceled(IProgressMonitor monitor)
			throws OperationCanceledException {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}
}
