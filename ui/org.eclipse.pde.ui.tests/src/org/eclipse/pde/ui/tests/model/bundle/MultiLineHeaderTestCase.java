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

public abstract class MultiLineHeaderTestCase extends BundleModelTestCase {

	public MultiLineHeaderTestCase(String headerName) {
		super(headerName);
	}

	public void testHeaderLengthMultiLines1() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc,\n");
		buffer.append(" com.example.abc.actions\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertEquals(fDocument.getLineLength(3) + fDocument.getLineLength(4), header.getLength());
	}

	public void testHeaderLengthMultiLines2() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc,\n");
		buffer.append(" com.example.abc.actions\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertEquals(fDocument.getLineLength(2) + fDocument.getLineLength(3), header.getLength());
	}
}
