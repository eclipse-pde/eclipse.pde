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

import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleVendorHeader;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class BundleVendorTestCase extends BundleModelTestCase {

	public BundleVendorTestCase() {
		super(Constants.BUNDLE_VENDOR);
	}

	public void testAddBundleVendor() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);
		fModel.getBundle().setHeader(Constants.BUNDLE_VENDOR, "eclipse.org");

		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_VENDOR);
		assertNotNull(header);
		assertEquals("Bundle-Vendor: eclipse.org\n", header.write());

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(buffer.toString() + header.write(), fDocument.get());
	}

	public void testRemoveBundleVendor() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-Vendor: eclipse.org\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_VENDOR);
		assertNotNull(header);
		((BundleVendorHeader) header).setVendor("");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(4, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(3));
	}

	public void testChangeBundleVendor() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Bundle-Vendor: eclipse.org\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.BUNDLE_VENDOR);
		assertNotNull(header);
		((BundleVendorHeader) header).setVendor("Eclipse PDE");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);

		StringBuilder expected = new StringBuilder("Bundle-Vendor: Eclipse PDE\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}
}
