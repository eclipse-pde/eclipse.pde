/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import org.eclipse.pde.internal.core.importing.IBundleImporter;

import java.util.HashMap;
import java.util.Map;
import junit.framework.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.importing.BundleImportDescription;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.importing.CvsBundleImportDescription;
import org.eclipse.pde.internal.core.project.BundleProjectService;

/**
 * Tests for bundle importer extensions.
 * 
 * @since 3.6
 */
public class BundleImporterTests extends TestCase {
	
	public static Test suite() {
		return new TestSuite(BundleImporterTests.class);
	}
	
	/**
	 * Returns the CVS importer or <code>null</code>
	 * 
	 * @return CVS importer or <code>null</code>
	 */
	protected IBundleImporter getCVSImporter() {
		IBundleImporter[] importers = ((BundleProjectService)BundleProjectService.getDefault()).getBundleImporters();
		for (int i = 0; i < importers.length; i++) {
			if (importers[i].getId().equals("org.eclipse.pde.core.cvs.importer")) {
				return importers[i];
			}
		}
		return null;
	}
	
	/**
	 * Tests that a project can be created from a reference with no tag or project attributes.
	 * 
	 * @throws CoreException
	 */
	public void testProjectSCMURL() throws CoreException {
		IBundleImporter handler = getCVSImporter();
		assertNotNull("Missing CVS source reference handler", handler);
		
		String header = "scm:cvs:pserver:dev.eclipse.org:/cvsroot/rt:org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.publisher";
		Map manifest = new HashMap();
		manifest.put(ICoreConstants.ECLIPSE_SOURCE_REFERENCES, header);
		BundleImportDescription[] descriptions = handler.validateImport(new Map[]{manifest});
		
		assertEquals("Wrong number of descriptions", 1, descriptions.length);
		BundleImportDescription bid = descriptions[0];
		assertTrue("Wrong kind of description", bid instanceof CvsBundleImportDescription);
		CvsBundleImportDescription cvsDes = (CvsBundleImportDescription) bid;
		assertEquals("org.eclipse.equinox.p2.publisher", cvsDes.getProject());
		assertNull("Wrong tag", cvsDes.getTag());
		assertEquals("pserver", cvsDes.getProtocol());
		assertEquals("dev.eclipse.org", cvsDes.getServer());
		assertEquals("/cvsroot/rt", cvsDes.getPath());
		assertEquals("org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.publisher", cvsDes.getModule());
		
	}
	
	/**
	 * Tests that a project can be created from a reference with a tag attribute.
	 * 
	 * @throws CoreException
	 */
	public void testProjectSCMURLwithTagAndProject() throws CoreException {
		IBundleImporter handler = getCVSImporter();
		assertNotNull("Missing CVS source reference handler", handler);
		
		String header = "scm:cvs:pserver:dev.eclipse.org:/cvsroot/rt:org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.publisher;tag=v20100215;project=one.two.three";
		Map manifest = new HashMap();
		manifest.put(ICoreConstants.ECLIPSE_SOURCE_REFERENCES, header);
		BundleImportDescription[] descriptions = handler.validateImport(new Map[]{manifest});
		
		assertEquals("Wrong number of descriptions", 1, descriptions.length);
		BundleImportDescription bid = descriptions[0];
		assertTrue("Wrong kind of description", bid instanceof CvsBundleImportDescription);
		CvsBundleImportDescription cvsDes = (CvsBundleImportDescription) bid;
		assertEquals("one.two.three", cvsDes.getProject());
		assertEquals("v20100215", cvsDes.getTag());
		assertEquals("pserver", cvsDes.getProtocol());
		assertEquals("dev.eclipse.org", cvsDes.getServer());
		assertEquals("/cvsroot/rt", cvsDes.getPath());
		assertEquals("org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.publisher", cvsDes.getModule());
		
	}	
	
	/**
	 * Tests that a project can be created from a reference with a project attribute.
	 * 
	 * @throws CoreException
	 */
	public void testProjectSCMURLwithProject() throws CoreException {
		IBundleImporter handler = getCVSImporter();
		assertNotNull("Missing CVS source reference handler", handler);
		
		String header = "scm:cvs:pserver:dev.eclipse.org:/cvsroot/rt:org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.publisher;project=a.b.c";
		Map manifest = new HashMap();
		manifest.put(ICoreConstants.ECLIPSE_SOURCE_REFERENCES, header);
		BundleImportDescription[] descriptions = handler.validateImport(new Map[]{manifest});
		
		assertEquals("Wrong number of descriptions", 1, descriptions.length);
		BundleImportDescription bid = descriptions[0];
		assertTrue("Wrong kind of description", bid instanceof CvsBundleImportDescription);
		CvsBundleImportDescription cvsDes = (CvsBundleImportDescription) bid;
		assertEquals("a.b.c", cvsDes.getProject());
		assertNull(cvsDes.getTag());
		assertEquals("pserver", cvsDes.getProtocol());
		assertEquals("dev.eclipse.org", cvsDes.getServer());
		assertEquals("/cvsroot/rt", cvsDes.getPath());
		assertEquals("org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.publisher", cvsDes.getModule());
		
	}	

}
