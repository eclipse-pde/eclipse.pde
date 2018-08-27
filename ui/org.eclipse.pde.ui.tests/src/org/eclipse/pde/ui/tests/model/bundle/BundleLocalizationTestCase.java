/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.model.bundle;

import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleLocalizationHeader;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class BundleLocalizationTestCase extends BundleModelTestCase {

	public BundleLocalizationTestCase() {
		super(Constants.BUNDLE_LOCALIZATION);
	}

	public void testGetLocalizationDefault() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymbolicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load();

		assertEquals(fModel.getBundle().getLocalization(), Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME);
	}

	public void testGetLocalization() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymbolicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": plugin\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);
		assertEquals(((BundleLocalizationHeader) header).getLocalization(), "plugin");
	}

	public void testSetLocalization() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymbolicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNull(header);

		fModel.getBundle().setHeader(fHeaderName, "plugin");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(ops.length, 1);

		ops[0].apply(fDocument);

		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": plugin\n", fDocument.get(pos, length));
	}

	public void testChangeExistingLocalization() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymbolicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": oldLocalization\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		((BundleLocalizationHeader) header).setLocalization("plugin");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(ops.length, 1);

		ops[0].apply(fDocument);

		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": plugin\n", fDocument.get(pos, length));
	}
}
