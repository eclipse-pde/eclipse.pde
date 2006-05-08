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
package org.eclipse.pde.ui.tests.model;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class ImportPackageTestCase extends TestCase {

	public static Test suite() {
		return new TestSuite(ImportPackageTestCase.class);
	}

	private Document fDocument;
	private BundleModel fModel;
	private BundleTextChangeListener fListener;
	
	protected void setUp() throws Exception {
		fDocument = new Document();
	}
	
	private void load() {
		load(false);
	}
	
	private void load(boolean addListener) {
		try {
			fModel = new BundleModel(fDocument, false);
			fModel.load();
			if (addListener) {
				fListener = new BundleTextChangeListener(fModel.getDocument());
				fModel.addModelChangedListener(fListener);
			}
		} catch (CoreException e) {
			fail("model cannot be loaded");
		}
		
	}
	
	public void testAbsentHeader() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load();
		
		assertNull(fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE));		
	}
	
	public void testPresentHeader() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: com.example.abc\n");
		fDocument.set(buffer.toString());
		load();
		
		assertNotNull(fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE));				
	}
	
	public void testHeaderOffset1() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: com.example.abc\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertEquals(fDocument.getLineOffset(3), header.getOffset());		
	}
	
	public void testHeaderOffset2() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Import-Package: com.example.abc\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertEquals(fDocument.getLineOffset(2), header.getOffset());		
	}
	
	
	public void testHeaderLength() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: com.example.abc\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertEquals(fDocument.getLineLength(3), header.getLength());	
	}
	
	public void testHeaderLengthWithWindowsDelimiter() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\r\n");
		buffer.append("Bundle-ManifestVersion: 2\r\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\r\n");
		buffer.append("Import-Package: com.example.abc\r\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertEquals(fDocument.getLineLength(3), header.getLength());	
	}
	
	public void testHeaderLengthMultiLines1() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: com.example.abc\n");
		buffer.append(" com.example.abc.actions\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertEquals(fDocument.getLineLength(3) + fDocument.getLineLength(4), header.getLength());	
	}
	
	public void testHeaderLengthMultiLines2() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Import-Package: com.example.abc\n");
		buffer.append(" com.example.abc.actions\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertEquals(fDocument.getLineLength(2) + fDocument.getLineLength(3), header.getLength());	
	}

	public void testAddImportPackageHeader() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);
		fModel.getBundle().setHeader(Constants.IMPORT_PACKAGE, "com.example.abc");
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertNotNull(header);	
		assertEquals("Import-Package: com.example.abc\n", header.write());
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(buffer.toString() + header.write(), fDocument.get());
	}
	
	public void testRemoveExistingImportPackageHeader() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertNotNull(header);
		
		((ImportPackageHeader)header).removePackage("com.example.abc");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(4, fDocument.getNumberOfLines());	
	}
	
	public void testAddPackage() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		((ImportPackageHeader)header).addPackage("com.example.abc.actions");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3) + fDocument.getLineLength(4);
		
		StringBuffer expected = new StringBuffer("Import-Package: com.example.abc,\n");
		expected.append(" com.example.abc.actions\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}
	
	public void testAddMultiplePackages() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		((ImportPackageHeader)header).addPackage("com.example.abc.views");
		((ImportPackageHeader)header).addPackage("com.example.abc.imports");
		((ImportPackageHeader)header).addPackage("com.example.abc.exports");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(8, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(7));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(7) - fDocument.getLineOffset(3);
		StringBuffer expected = new StringBuffer("Import-Package: com.example.abc,\n");
		expected.append(" com.example.abc.exports,\n");
		expected.append(" com.example.abc.imports,\n");
		expected.append(" com.example.abc.views\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));		
	}
	
	public void testRemovePackage() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: com.example.abc,\n");
		buffer.append(" com.example.abc.actions,\n");
		buffer.append(" com.example.abc.refactoring\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		((ImportPackageHeader)header).removePackage("com.example.abc.actions");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(5) - fDocument.getLineOffset(3);
		StringBuffer expected = new StringBuffer("Import-Package: com.example.abc,\n");
		expected.append(" com.example.abc.refactoring\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));			
	}
	
	public void testRemoveMultiplePackages() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: com.example.abc,\n");
		buffer.append(" com.example.abc.actions,\n");
		buffer.append(" com.example.abc.refactoring\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		((ImportPackageHeader)header).removePackage("com.example.abc.actions");
		((ImportPackageHeader)header).removePackage("com.example.abc");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Import-Package: com.example.abc.refactoring\n", fDocument.get(pos, length));			
	}
	
	public void testReadOptionalPackage() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: com.example.abc;resolution:=optional\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertNotNull(header);
		
		ImportPackageObject object = ((ImportPackageHeader)header).getPackage("com.example.abc");
		assertNotNull(object);
		assertTrue(object.isOptional());	
	}
	
	public void testReadPackageWithVersion() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: org.osgi.framework;version=\"1.3.0\"\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertNotNull(header);
		
		ImportPackageObject object = ((ImportPackageHeader)header).getPackage("org.osgi.framework");
		assertNotNull(object);
		assertEquals("1.3.0", object.getVersion());		
	}
	
	public void testMakePackageOptional() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: org.osgi.framework\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertNotNull(header);

		ImportPackageObject object = ((ImportPackageHeader)header).getPackage("org.osgi.framework");
		assertNotNull(object);
		object.setOptional(true);
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Import-Package: org.osgi.framework;resolution:=optional\n", fDocument.get(pos, length));			
	}
	
	public void testRemoveOptionalDirective() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: org.osgi.framework;resolution:=optional\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertNotNull(header);

		ImportPackageObject object = ((ImportPackageHeader)header).getPackage("org.osgi.framework");
		assertNotNull(object);
		object.setOptional(false);
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Import-Package: org.osgi.framework\n", fDocument.get(pos, length));			
	}
	
	public void testAddVersionToPackage() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: org.osgi.framework\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertNotNull(header);

		ImportPackageObject object = ((ImportPackageHeader)header).getPackage("org.osgi.framework");
		assertNotNull(object);
		object.setVersion("1.3.0");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Import-Package: org.osgi.framework;version=\"1.3.0\"\n", fDocument.get(pos, length));					
	}
	
	public void testRemoveVersionFromPackage() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: org.osgi.framework\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		assertNotNull(header);

		ImportPackageObject object = ((ImportPackageHeader)header).getPackage("org.osgi.framework");
		assertNotNull(object);
		object.setVersion(null);
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);		
		assertEquals("Import-Package: org.osgi.framework\n", fDocument.get(pos, length));					
	}
	
	public void testAddPackageWithWindowsDelimiter() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\r\n");
		buffer.append("Bundle-ManifestVersion: 2\r\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\r\n");
		buffer.append("Import-Package: com.example.abc\r\n");
		fDocument.set(buffer.toString());
		load(true);
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		((ImportPackageHeader)header).addPackage("com.example.abc.actions");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3) + fDocument.getLineLength(4);
		
		StringBuffer expected = new StringBuffer("Import-Package: com.example.abc,\r\n");
		expected.append(" com.example.abc.actions\r\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));		
	}
	
	public void testRemovePackageWithWindowsDelimiter() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\r\n");
		buffer.append("Bundle-ManifestVersion: 2\r\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\r\n");
		buffer.append("Import-Package: com.example.abc,\r\n");
		buffer.append(" com.example.abc.actions,\r\n");
		buffer.append(" com.example.abc.refactoring\r\n");
		fDocument.set(buffer.toString());
		load(true);
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		((ImportPackageHeader)header).removePackage("com.example.abc.actions");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(6, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(5));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(5) - fDocument.getLineOffset(3);
		StringBuffer expected = new StringBuffer("Import-Package: com.example.abc,\r\n");
		expected.append(" com.example.abc.refactoring\r\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));					
	}
	
	public void testPreserveSpacing() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Import-Package: \n");
		buffer.append(" com.example.abc\n");
		fDocument.set(buffer.toString());
		load(true);
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
		((ImportPackageHeader)header).addPackage("com.example.abc.actions");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(7, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(6));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineOffset(6) - fDocument.getLineOffset(3);
		
		StringBuffer expected = new StringBuffer("Import-Package: \n");
		expected.append(" com.example.abc,\n");
		expected.append(" com.example.abc.actions\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
		
	}
}
