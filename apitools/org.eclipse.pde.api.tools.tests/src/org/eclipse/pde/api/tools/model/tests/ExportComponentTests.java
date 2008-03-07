/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.model.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;

/**
 * Tests exporting API components.
 * 
 * @since 1.0.0
 */
public class ExportComponentTests extends TestCase {

	/**
	 * Exports the SDK compressing class files.
	 * 
	 * @throws CoreException
	 * @throws IOException 
	 */
	public void testExportSDKCompress() throws CoreException, IOException {
		HashMap<String, Object> options = new HashMap<String, Object>();
		options.put(IApiComponent.EXPORT_COMPRESS, Boolean.TRUE);
		exportSDKTest(options);
	}
	
	/**
	 * Exports the SDK creating compressed stub class files
	 * 
	 * @throws CoreException
	 * @throws IOException 
	 */
	public void testExportSDKCompressStubs() throws CoreException, IOException {
		HashMap<String, Object> options = new HashMap<String, Object>();
		options.put(IApiComponent.EXPORT_COMPRESS, Boolean.TRUE);
		options.put(IApiComponent.EXPORT_CLASS_FILE_STUBS, Boolean.TRUE);
		exportSDKTest(options);
	}	
	
	/**
	 * Exports the SDK, builds a new profile out of it and
	 * compares it to the original SDK. Should be no differences.
	 * 
	 * @param options export options
	 * @throws CoreException
	 * @throws IOException 
	 */
	protected void exportSDKTest(Map<String, Object> options) throws CoreException, IOException {
		File jdt = TestSuiteHelper.getBundle("org.eclipse.jdt.debug.ui");
		assertNotNull("Missing jdt.debug.ui", jdt);
		IApiProfile profile = TestSuiteHelper.createProfile("eclipse", jdt.getParentFile());
		assertNotNull("the testing baseline should exist", profile);
		File tempFile = File.createTempFile("api", "tmp");
		File exportRoot = new File(tempFile.getParentFile(), "exp-test");
		exportRoot.mkdirs();
		File[] listFiles = exportRoot.listFiles();
		for (int i = 0; i < listFiles.length; i++) {
			File file = listFiles[i];
			if (file.exists()) {
				assertTrue("File could not be deleted : " + file.getAbsolutePath(), file.delete());
			}
		}
		options.put(IApiComponent.EXPORT_DIRECTORY, exportRoot.getAbsolutePath());
		
		IApiComponent[] components = profile.getApiComponents();
		for (int i = 0; i < components.length; i++) {
			IApiComponent component = components[i];
			if (!component.isSystemComponent()) {
				component.export(options, null);
			}
		}
		// check that delta is empty when compared
		IApiProfile profile2 = TestSuiteHelper.createProfile("eclipse-2", exportRoot);
		IApiComponent[] all = profile.getApiComponents();
		try {
			for (int i = 0; i < all.length; i++) {
				IApiComponent apiComponent = all[i];
				IDelta delta = ApiComparator.compare(apiComponent, profile2, VisibilityModifiers.API, true);
				if (delta != ApiComparator.NO_DELTA) {
					System.out.println(delta);
				}
				assertTrue("Profiles should be identical", delta.isEmpty());
			}
			// check that reference information is the same
			IApiSearchEngine engine = Factory.newSearchEngine();
			for (int i = 0; i < components.length; i++) {
				IApiComponent component = components[i];
				if (!component.isSystemComponent()) {
					IApiComponent component2 = profile2.getApiComponent(component.getId());
					IApiSearchScope scope = Factory.newScope(new IApiComponent[]{component});
					IApiSearchScope scope2 = Factory.newScope(new IApiComponent[]{component2});
					IApiSearchCriteria criteria = Factory.newSearchCriteria();
					criteria.setConsiderComponentLocalReferences(false);
					criteria.setReferenceKinds(ReferenceModifiers.MASK_REF_ALL);
					criteria.setReferencedRestrictions(
							VisibilityModifiers.ALL_VISIBILITIES,
							RestrictionModifiers.ALL_RESTRICTIONS);
					IReference[] references = engine.search(scope, new IApiSearchCriteria[]{criteria}, null);
					IReference[] references2 = engine.search(scope2, new IApiSearchCriteria[]{criteria}, null);
					Set<IReference> referencesSet = new HashSet<IReference>();
					for (int j = 0; j < references.length; j++) {
						referencesSet.add(references[j]);
					}
					Set<IReference> referencesSet2 = new HashSet<IReference>();
					for (int j = 0; j < references2.length; j++) {
						referencesSet2.add(references2[j]);
					}
					if (referencesSet.size() != referencesSet2.size()) {
						assertEquals("Different number of references for " + component.getId(), referencesSet.size(), referencesSet2.size());
//						System.out.println("Different number of references for " + component.getId() + " was (before export) " + referencesSet.size() + " found (exported) " + referencesSet2.size());
//						System.out.println("======================================= FIRST SET ========================================");
//						for (Iterator iterator = referencesSet.iterator(); iterator.hasNext();) {
//							IReference reference = (IReference) iterator.next();
//							System.out.println(reference);
//						}
//						System.out.println("======================================= FIRST SET ========================================");
//						System.out.println("======================================= SECOND SET ========================================");
//						for (Iterator iterator = referencesSet2.iterator(); iterator.hasNext();) {
//							IReference reference = (IReference) iterator.next();
//							System.out.println(reference);
//						}
//						System.out.println("======================================= SECOND SET ========================================");
					}
				}
			}
		} finally {
			// clean-up
			profile.dispose();
			profile2.dispose();
			listFiles = exportRoot.listFiles();
			for (int i = 0; i < listFiles.length; i++) {
				File file = listFiles[i];
				if (file.exists()) {
					assertTrue("File could not be deleted : " + file.getAbsolutePath(), file.delete());
				}
			}
			tempFile.delete();
			exportRoot.delete();
		}
	}	
}
