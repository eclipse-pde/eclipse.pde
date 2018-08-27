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
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class ImportPackageTestCase extends PackageHeaderTestCase {

	public ImportPackageTestCase() {
		super(Constants.IMPORT_PACKAGE);
	}

	public void testReadOptionalPackage() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName + ": com.example.abc;resolution:=optional\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		ImportPackageObject object = (ImportPackageObject) getPackage(header, "com.example.abc");
		assertNotNull(object);
		assertTrue(object.isOptional());
	}

	public void testMakePackageOptional() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName + ": org.osgi.framework\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		ImportPackageObject object = (ImportPackageObject) getPackage(header, "org.osgi.framework");
		assertNotNull(object);
		object.setOptional(true);

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": org.osgi.framework;resolution:=optional\n", fDocument.get(pos, length));
	}

	public void testRemoveOptionalDirective() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName + ": org.osgi.framework;resolution:=optional\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		ImportPackageObject object = (ImportPackageObject) getPackage(header, "org.osgi.framework");
		assertNotNull(object);
		object.setOptional(false);

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": org.osgi.framework\n", fDocument.get(pos, length));
	}

	@Override
	protected void addPackage(IManifestHeader header, String packageName) {
		((ImportPackageHeader) header).addPackage(packageName);

	}

	@Override
	protected PackageObject getPackage(IManifestHeader header, String packageName) {
		return ((ImportPackageHeader) header).getPackage(packageName);
	}
}
