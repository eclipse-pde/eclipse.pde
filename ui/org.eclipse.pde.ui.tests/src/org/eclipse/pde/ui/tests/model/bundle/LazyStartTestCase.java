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

import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.LazyStartHeader;
import org.eclipse.text.edits.TextEdit;

public class LazyStartTestCase extends BundleModelTestCase {

	public LazyStartTestCase() {
		super(ICoreConstants.ECLIPSE_AUTOSTART);
	}

	public void testGetAutoStart() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": true\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);
		assertTrue(((LazyStartHeader) header).isLazyStart());
	}

	public void testGetAutoStart2() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": false\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);
		assertTrue(!((LazyStartHeader) header).isLazyStart());
	}

	public void testSetLazyStart() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
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

	public void testChangeExistingLazyStart() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": false\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		((LazyStartHeader) header).setLazyStart(true);
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(ops.length, 1);

		ops[0].apply(fDocument);

		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": true\n", fDocument.get(pos, length));
	}
}
