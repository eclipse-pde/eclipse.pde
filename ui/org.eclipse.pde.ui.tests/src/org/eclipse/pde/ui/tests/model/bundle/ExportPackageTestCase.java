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

public class ExportPackageTestCase extends PackageHeaderTestCase {

	public ExportPackageTestCase() {
		super(Constants.EXPORT_PACKAGE);
	}

	@Override
	protected void addPackage(IManifestHeader header, String packageName) {
		PackageObject packageObject = new PackageObject((ExportPackageHeader) header, packageName, null, null);
		((ExportPackageHeader) header).addPackage(packageObject);
	}

	@Override
	protected PackageObject getPackage(IManifestHeader header, String packageName) {
		return ((ExportPackageHeader) header).getPackage(packageName);
	}

	public void testReadInternalPackage() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName + ": com.example.abc;x-internal:=true\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		ExportPackageObject object = (ExportPackageObject) getPackage(header, "com.example.abc");
		assertNotNull(object);
		assertEquals(object.getFriends().length, 0);
		assertTrue(object.isInternal());
	}

	public void testReadOneFriend() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName + ": com.example.abc;x-friends:=com.example.xyz\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		ExportPackageObject object = (ExportPackageObject) getPackage(header, "com.example.abc");
		assertNotNull(object);
		assertTrue(object.isInternal());
		PackageFriend[] friends = object.getFriends();
		assertEquals(friends.length, 1);
		assertTrue(friends[0].getName().equals("com.example.xyz"));
	}

	public void testReadMultipleFriend() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName + ": com.example.abc;x-friends:=\"com.example.xxx,com.example.yyy,com.example.zzz\"\n");
		fDocument.set(buffer.toString());
		load();

		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		ExportPackageObject object = (ExportPackageObject) getPackage(header, "com.example.abc");
		assertNotNull(object);
		assertTrue(object.isInternal());
		PackageFriend[] friends = object.getFriends();
		assertEquals(friends.length, 3);
		assertTrue(friends[0].getName().equals("com.example.xxx"));
		assertTrue(friends[1].getName().equals("com.example.yyy"));
		assertTrue(friends[2].getName().equals("com.example.zzz"));
	}

	public void testSetPackageInternal() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName + ": org.osgi.framework\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		ExportPackageObject object = (ExportPackageObject) getPackage(header, "org.osgi.framework");
		assertNotNull(object);
		object.setInternal(true);

		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": org.osgi.framework;x-internal:=true\n", fDocument.get(pos, length));
	}

	public void testAddPackageFriend() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName + ": org.osgi.framework\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		ExportPackageObject object = (ExportPackageObject) getPackage(header, "org.osgi.framework");
		assertNotNull(object);
		object.addFriend(new PackageFriend(object, "org.eclipse.pde.ui"));
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": org.osgi.framework;x-friends:=\"org.eclipse.pde.ui\"\n", fDocument.get(pos, length));
	}

	public void testAddPackageFriends() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName + ": org.osgi.framework\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);

		ExportPackageObject object = (ExportPackageObject) getPackage(header, "org.osgi.framework");
		assertNotNull(object);
		object.addFriend(new PackageFriend(object, "org.eclipse.pde.core"));
		object.addFriend(new PackageFriend(object, "org.eclipse.pde.ui"));
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);

		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));

		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		assertEquals(fHeaderName + ": org.osgi.framework;x-friends:=\"org.eclipse.pde.core,org.eclipse.pde.ui\"\n", fDocument.get(pos, length));
	}
}