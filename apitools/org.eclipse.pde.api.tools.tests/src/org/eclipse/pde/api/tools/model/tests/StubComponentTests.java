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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;

/**
 * Tests class file stubs.
 * 
 * @since 1.0.0
 */
public class StubComponentTests extends TestCase {
	
	/**
	 * Exports components creating compressed stub class files.
	 * Ensures that references in the original and stub are the same.
	 * 
	 * @throws CoreException
	 * @throws IOException 
	 */
	public void testReferencesInStubs() throws CoreException, IOException {
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
			listFiles[i].delete();
		}
		options.put(IApiComponent.EXPORT_DIRECTORY, exportRoot.getAbsolutePath());
		
		IApiComponent[] components = profile.getApiComponents();
		for (int i = 0; i < components.length; i++) {
			IApiComponent component = components[i];
			if (!component.isSystemComponent()) {
				component.export(options, null);
			}
			component.close();
		}
		
		// check that delta is empty when compared		
		IApiProfile profile2 = TestSuiteHelper.createProfile("eclipse-2", exportRoot);
		
		// check that reference information is the same
		IApiSearchEngine engine = Factory.newSearchEngine();
		boolean ok = true; 
		for (int i = 0; i < components.length; i++) {
			IApiComponent component = components[i];
			if (!component.isSystemComponent()) {
				IApiComponent component2 = profile2.getApiComponent(component.getId());
				IApiSearchScope scope = Factory.newScope(new IApiComponent[]{component});
				IApiSearchScope scope2 = Factory.newScope(new IApiComponent[]{component2});
				IApiSearchCriteria criteria = Factory.newSearchCriteria();
				criteria.setConsiderComponentLocalReferences(true);
				criteria.setReferenceKinds(ReferenceModifiers.MASK_REF_ALL);
				criteria.setReferencedRestrictions(
						VisibilityModifiers.ALL_VISIBILITIES,
						RestrictionModifiers.ALL_RESTRICTIONS);
				IReference[] references = engine.search(scope, new IApiSearchCriteria[]{criteria}, null);
				IReference[] references2 = engine.search(scope2, new IApiSearchCriteria[]{criteria}, null);
				System.out.println(component.getId() + " origRefs: " + references.length + " stubRefs: " + references2.length);
				Set<IReference> leftOver = new HashSet<IReference>();
				for (int j = 0; j < references.length; j++) {
					leftOver.add(references[j]);
				}
				for (int j = 0; j < references2.length; j++) {
					leftOver.remove(references2[j]);
				}
				System.out.println("difference: " + leftOver.size());
				Iterator<IReference> iterator = leftOver.iterator();
				while (iterator.hasNext()) {
					IReference reference = (IReference) iterator.next();
					System.out.println(reference);
				}
				if (leftOver.size() > 0) {
					ok = false;
					break;
				}
				component2.close();
			}
			component.close();
		}

		// clean-up
		profile.dispose();
		profile2.dispose();
		listFiles = exportRoot.listFiles();
		for (int i = 0; i < listFiles.length; i++) {
			listFiles[i].delete();
		}
		tempFile.delete();
		exportRoot.delete();
		assertTrue("Different refs", ok);
	}	
}
