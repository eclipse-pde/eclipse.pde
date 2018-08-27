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
import org.eclipse.pde.internal.core.text.bundle.BasePackageHeader;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.text.edits.TextEdit;

public abstract class PackageHeaderTestCase extends MultiLineHeaderTestCase {

	public PackageHeaderTestCase(String headerName) {
		super(headerName);
	}

	public void testAddExportPackageHeader() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);
		fModel.getBundle().setHeader(fHeaderName, "com.example.abc");

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);
		assertEquals(fHeaderName + ": com.example.abc\n", header.write());

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(buffer.toString() + header.write(), fDocument.get());
	}

	public void testRemoveExistingExportPackageHeader() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		((BasePackageHeader) header).removePackage("com.example.abc");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(4, fDocument.getNumberOfLines());
	}

	public void testAddPackage() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		addPackage(header, "com.example.abc.actions");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3) + fDocument.getLineLength(4);

		StringBuilder expected = new StringBuilder(fHeaderName + ": com.example.abc,\n");
		expected.append(" com.example.abc.actions\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

	public void testAddImportPackageHeader() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);
		fModel.getBundle().setHeader(fHeaderName, "com.example.abc");

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);
		assertEquals(fHeaderName + ": com.example.abc\n", header.write());

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(buffer.toString() + header.write(), fDocument.get());
	}

	public void testAddMultiplePackages() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		addPackage(header, "com.example.abc.views");
		addPackage(header, "com.example.abc.imports");
		addPackage(header, "com.example.abc.exports");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(8, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(7));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(7) - fDocument.getLineOffset(3);
		StringBuilder expected = new StringBuilder(fHeaderName + ": com.example.abc,\n");
		expected.append(" com.example.abc.exports,\n");
		expected.append(" com.example.abc.imports,\n");
		expected.append(" com.example.abc.views\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

	public void testRemovePackage() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc,\n");
		buffer.append(" com.example.abc.actions,\n");
		buffer.append(" com.example.abc.refactoring\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		((BasePackageHeader) header).removePackage("com.example.abc.actions");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(5) - fDocument.getLineOffset(3);
		StringBuilder expected = new StringBuilder(fHeaderName + ": com.example.abc,\n");
		expected.append(" com.example.abc.refactoring\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

	public void testRemoveMultiplePackages() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc,\n");
		buffer.append(" com.example.abc.actions,\n");
		buffer.append(" com.example.abc.refactoring\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		((BasePackageHeader) header).removePackage("com.example.abc.actions");
		((BasePackageHeader) header).removePackage("com.example.abc");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": com.example.abc.refactoring\n", fDocument.get(pos, length));
	}

	public void testReadPackageWithVersion() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": org.osgi.framework;version=\"1.3.0\"\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		PackageObject object = getPackage(header, "org.osgi.framework");
		assertNotNull(object);
		assertEquals("1.3.0", object.getVersion());
	}

	public void testAddVersionToPackage() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": org.osgi.framework\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		PackageObject object = getPackage(header, "org.osgi.framework");
		assertNotNull(object);
		object.setVersion("1.3.0");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": org.osgi.framework;version=\"1.3.0\"\n", fDocument.get(pos, length));
	}

	public void testRemoveVersionFromPackage() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": org.osgi.framework\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		PackageObject object = getPackage(header, "org.osgi.framework");
		assertNotNull(object);
		object.setVersion(null);

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": org.osgi.framework\n", fDocument.get(pos, length));
	}

	public void testAddPackageWithWindowsDelimiter() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\r\n");
		buffer.append("Bundle-ManifestVersion: 2\r\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\r\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc\r\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		addPackage(header, "com.example.abc.actions");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3) + fDocument.getLineLength(4);

		StringBuilder expected = new StringBuilder(fHeaderName + ": com.example.abc,\r\n");
		expected.append(" com.example.abc.actions\r\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

	public void testRemovePackageWithWindowsDelimiter() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\r\n");
		buffer.append("Bundle-ManifestVersion: 2\r\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\r\n");
		buffer.append(fHeaderName);
		buffer.append(": com.example.abc,\r\n");
		buffer.append(" com.example.abc.actions,\r\n");
		buffer.append(" com.example.abc.refactoring\r\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		((BasePackageHeader) header).removePackage("com.example.abc.actions");

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(5) - fDocument.getLineOffset(3);
		StringBuilder expected = new StringBuilder(fHeaderName + ": com.example.abc,\r\n");
		expected.append(" com.example.abc.refactoring\r\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

	public void testPreserveSpacing() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": \n");
		buffer.append(" com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		addPackage(header, "com.example.abc.actions");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(7, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(6));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(6) - fDocument.getLineOffset(3);

		StringBuilder expected = new StringBuilder(fHeaderName + ": \n");
		expected.append(" com.example.abc,\n");
		expected.append(" com.example.abc.actions\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));

	}

	protected abstract void addPackage(IManifestHeader header, String packageName);

	protected abstract PackageObject getPackage(IManifestHeader header, String packageName);
}
