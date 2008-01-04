/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.bundle;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class ImportPackageTestCase extends PackageHeaderTestCase {

	public ImportPackageTestCase() {
		super(Constants.IMPORT_PACKAGE);
	}

	public static Test suite() {
		return new TestSuite(ImportPackageTestCase.class);
	}

	public void testReadOptionalPackage() {
		StringBuffer buffer = new StringBuffer();
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
		StringBuffer buffer = new StringBuffer();
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
		StringBuffer buffer = new StringBuffer();
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

	protected void addPackage(IManifestHeader header, String packageName) {
		((ImportPackageHeader) header).addPackage(packageName);

	}

	protected PackageObject getPackage(IManifestHeader header, String packageName) {
		return ((ImportPackageHeader) header).getPackage(packageName);
	}
}
