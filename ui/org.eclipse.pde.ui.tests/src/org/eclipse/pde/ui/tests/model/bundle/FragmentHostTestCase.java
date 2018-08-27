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
import org.eclipse.pde.internal.core.text.bundle.FragmentHostHeader;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class FragmentHostTestCase extends BundleModelTestCase {

	public FragmentHostTestCase() {
		super(Constants.FRAGMENT_HOST);
	}

	public void testAddFragmentHost() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);
		fModel.getBundle().setHeader(Constants.FRAGMENT_HOST, "org.eclipse.pde");

		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.FRAGMENT_HOST);
		assertNotNull(header);
		assertEquals("Fragment-Host: org.eclipse.pde\n", header.write());

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(buffer.toString() + header.write(), fDocument.get());
	}

	public void testRemoveFragmentHost() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Fragment-Host: org.eclipse.pde\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.FRAGMENT_HOST);
		assertNotNull(header);
		((FragmentHostHeader) header).setHostId("");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(4, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(3));
	}

	public void testChangeFragmentHost() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Fragment-Host: org.eclipse.pde\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.FRAGMENT_HOST);
		assertNotNull(header);
		((FragmentHostHeader) header).setHostId("org.eclipse.jdt");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);

		StringBuilder expected = new StringBuilder("Fragment-Host: org.eclipse.jdt\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

}
