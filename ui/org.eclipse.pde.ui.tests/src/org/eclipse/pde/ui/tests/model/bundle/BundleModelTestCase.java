/*******************************************************************************
 *  Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.bundle;

import junit.framework.TestCase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;

public abstract class BundleModelTestCase extends TestCase {

	protected Document fDocument;
	protected BundleModel fModel;
	protected BundleTextChangeListener fListener;
	protected String fHeaderName;

	public BundleModelTestCase(String headerName) {
		fHeaderName = headerName;
	}

	@Override
	protected void setUp() throws Exception {
		fDocument = new Document();
	}

	protected void load() {
		load(false);
	}

	protected void load(boolean addListener) {
		try {
			fModel = new BundleModel(fDocument, false);
			fModel.load();
			if (!fModel.isLoaded() || !fModel.isValid())
				fail("model cannot be loaded");
			if (addListener) {
				fListener = new BundleTextChangeListener(fModel.getDocument());
				fModel.addModelChangedListener(fListener);
			}
		} catch (CoreException e) {
			fail("model cannot be loaded");
		}
	}

	public void testAbsentHeader() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load();

		assertNull(fModel.getBundle().getManifestHeader(fHeaderName));
	}

	public void testPresentHeader() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\n");
		fDocument.set(buffer.toString());
		load();

		assertNotNull(fModel.getBundle().getManifestHeader(fHeaderName));
	}

	public void testHeaderOffset1() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertEquals(fDocument.getLineOffset(3), header.getOffset());
	}

	public void testHeaderOffset2() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertEquals(fDocument.getLineOffset(2), header.getOffset());
	}

	public void testHeaderLength() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertEquals(fDocument.getLineLength(3), header.getLength());
	}

	public void testHeaderLengthWithWindowsDelimiter() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\r\n");
		buffer.append("Bundle-ManifestVersion: 2\r\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\r\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\r\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertEquals(fDocument.getLineLength(3), header.getLength());
	}
}
