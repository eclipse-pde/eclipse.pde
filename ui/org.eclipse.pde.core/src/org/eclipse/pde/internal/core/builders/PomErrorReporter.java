/*******************************************************************************
 *  Copyright (c) 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.Stack;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.*;
import org.osgi.framework.Version;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Validates the content of the pom.xml.  Currently the only check is that the
 * version specified in pom.xml matches the bundle version.
 *
 */
public class PomErrorReporter extends ErrorReporter {

	private static final String ELEMENT_PROJECT = "project"; //$NON-NLS-1$
	private static final String ELEMENT_VERSION = "version"; //$NON-NLS-1$
	private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT"; //$NON-NLS-1$

	private SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	private int pomVersionSeverity;

	public PomErrorReporter(IFile file) {
		super(file);
		pomVersionSeverity = CompilerFlags.getFlag(fProject, CompilerFlags.P_MATCHING_POM_VERSION);
	}

	@Override
	protected void validate(IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, 10);
		try {
			if (subMon.isCanceled()) {
				return;
			}
			if (pomVersionSeverity == CompilerFlags.IGNORE) {
				return;
			}

			PluginModelManager manager = PDECore.getDefault().getModelManager();
			IPluginModelBase bundle = manager.findModel(fProject);
			BundleDescription description = bundle.getBundleDescription();
			if (description != null) {
				Version bundleVersion = description.getVersion();
				try {
					SAXParser parser = parserFactory.newSAXParser();
					PomVersionHandler handler = new PomVersionHandler(fFile, bundleVersion);
					parser.parse(fFile.getContents(), handler);
				} catch (Exception e1) {
					PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.PomErrorReporter_problemParsingPom, e1));
				}

			}
		} finally {
			subMon.done();
			if (monitor != null) {
				monitor.done();
			}
		}

	}

	private void reportMarker(String message, int lineNumber, String correctedVersion) {
		IMarker marker = report(message, lineNumber, pomVersionSeverity, PDEMarkerFactory.POM_MISMATCH_VERSION, PDEMarkerFactory.CAT_OTHER);
		if (marker != null) {
			try {
				marker.setAttribute(PDEMarkerFactory.POM_CORRECT_VERSION, correctedVersion);
			} catch (CoreException e) {
				// Ignored
			}
		}
	}

	class PomVersionHandler extends DefaultHandler {
		private Version bundleVersion;
		private Stack<String> elements = new Stack<String>();
		private boolean checkVersion = false;
		private Locator locator;

		public PomVersionHandler(IFile file, Version bundleVersion) {
			this.bundleVersion = bundleVersion;
		}

		@Override
		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (ELEMENT_VERSION.equals(qName)) {
				if (!elements.isEmpty() && ELEMENT_PROJECT.equals(elements.peek())) {
					checkVersion = true;
				}
			}
			elements.push(qName);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			elements.pop();
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (checkVersion) {
				checkVersion = false;
				// Compare the versions
				String versionString = new String(ch, start, length);
				try {
					// Remove snapshot suffix
					int index = versionString.indexOf(SNAPSHOT_SUFFIX);
					if (index >= 0) {
						versionString = versionString.substring(0, index);
					}
					Version pomVersion = Version.parseVersion(versionString);
					// Remove qualifiers and snapshot
					Version bundleVersion2 = new Version(bundleVersion.getMajor(), bundleVersion.getMinor(), bundleVersion.getMicro());
					Version pomVersion2 = new Version(pomVersion.getMajor(), pomVersion.getMinor(), pomVersion.getMicro());

					if (!bundleVersion2.equals(pomVersion2)) {
						String correctedVersion = bundleVersion2.toString();
						if (index >= 0) {
							correctedVersion = correctedVersion.concat(SNAPSHOT_SUFFIX);
						}
						reportMarker(NLS.bind(PDECoreMessages.PomErrorReporter_pomVersionMismatch, pomVersion2.toString(), bundleVersion2.toString()), locator.getLineNumber(), correctedVersion);
					}
				} catch (IllegalArgumentException e) {
					// Do nothing, user has a bad version
				}
			}
		}
	}
}
