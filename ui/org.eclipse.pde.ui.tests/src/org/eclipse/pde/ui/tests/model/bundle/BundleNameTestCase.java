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
import org.eclipse.pde.internal.core.text.bundle.BundleNameHeader;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class BundleNameTestCase extends BundleModelTestCase {

	public static Test suite() {
		return new TestSuite(BundleNameTestCase.class);
	}
	
	public BundleNameTestCase() {
		super(Constants.BUNDLE_NAME);
	}

	public void testGetName() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": Bundle Name\n");
		fDocument.set(buffer.toString());
		load();
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);
		assertEquals(((BundleNameHeader)header).getBundleName(), "Bundle Name");
	}
	
	public void testSetName() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNull(header);
		
		fModel.getBundle().setHeader(fHeaderName, "Bundle Name");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(ops.length, 1);
		
		ops[0].apply(fDocument);
		
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);	
		assertEquals(fHeaderName + ": Bundle Name\n", fDocument.get(pos, length));
	}
	
	public void testChangeExistingName() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append(fHeaderName);
		buffer.append(": Old Bundle Name\n");
		fDocument.set(buffer.toString());
		load(true);
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(fHeaderName);
		assertNotNull(header);
		
		fModel.getBundle().setHeader(fHeaderName, "Bundle Name");
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(ops.length, 1);
		
		ops[0].apply(fDocument);
		
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);	
		assertEquals(fHeaderName + ": Bundle Name\n", fDocument.get(pos, length));
	}
}
