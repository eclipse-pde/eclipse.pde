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
import org.eclipse.pde.internal.core.text.bundle.BundleClasspathHeader;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class BundleClasspathTestCase extends MultiLineHeaderTestCase {

	public BundleClasspathTestCase() {
		super(Constants.BUNDLE_CLASSPATH);
	}

	public void testAddLibrary() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		((BundleClasspathHeader) header).addLibrary("com.example.xyz");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);

		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));

		int pos = fDocument.getLineOffset(4);
		int length = fDocument.getLineLength(4);
		assertEquals(" com.example.xyz\n", fDocument.get(pos, length));
	}

	public void testRemoveLibrary() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc,\n");
		buffer.append(" com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		((BundleClasspathHeader) header).removeLibrary("com.example.abc");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);

		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": com.example.xyz\n", fDocument.get(pos, length));
	}

	public void testRemoveOnlyLibrary() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		((BundleClasspathHeader) header).removeLibrary("com.example.abc");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);

		assertEquals(4, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(3));

		assertEquals(fDocument.get().indexOf(fHeaderName), -1);
	}
}
