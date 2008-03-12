/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.reference.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.util.Util;

/**
 * Tests for class file stubs.
 * 
 * @since 1.0.0
 */
public class ClassFileStubTests extends TestCase {

	/**
	 * Tests that the class files generated can be read by the JDT class
	 * file reader.
	 * @throws IOException 
	 * @throws CoreException 
	 */
	public void testClassFileReader() throws CoreException, IOException {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(IApiComponent.EXPORT_COMPRESS, Boolean.TRUE);
		options.put(IApiComponent.EXPORT_CLASS_FILE_STUBS, Boolean.TRUE);
		IApiProfile[] profiles = exportSDK(options);
		IApiProfile profile = profiles[1];
		IApiComponent[] components = profile.getApiComponents();
		
		try {
			for (int i = 0; i < components.length; i++) {
				IApiComponent apiComponent = components[i];
				ClassFileContainerVisitor visitor = new ClassFileContainerVisitor() {
					public void visit(String packageName, IClassFile classFile) {
						InputStream inputStream = null;
						try {
							inputStream = classFile.getInputStream();
							ToolFactory.createDefaultClassFileReader(inputStream, IClassFileReader.METHOD_INFOS);
						} catch (CoreException e) {
							e.printStackTrace();
						} finally {
							if (inputStream != null) {
								try {
									inputStream.close();
								} catch(IOException e) {
									// ignore
								}
							}
						}
					}
				};
				apiComponent.accept(visitor);
			}
		} finally {
			cleanupProfiles(profiles);
		}
	}
	
	/**
	 * Tests that reference info is the same in stubs and original file.
	 * 
	 * @throws IOException 
	 * @throws CoreException 
	 */
	public void testClassFileStubReferences() throws CoreException, IOException {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(IApiComponent.EXPORT_COMPRESS, Boolean.TRUE);
		options.put(IApiComponent.EXPORT_CLASS_FILE_STUBS, Boolean.TRUE);
		IApiProfile[] profiles = exportSDK(options);
		try {
			IApiProfile original = profiles[0];
			IApiProfile exported = profiles[1];
			
			IApiComponent component = original.getApiComponent("org.eclipse.pde");
			IApiComponent component2 = exported.getApiComponent("org.eclipse.pde");
			IApiSearchEngine engine = Factory.newSearchEngine();
			IApiSearchCriteria criteria = Factory.newSearchCriteria();
			criteria.setReferenceKinds(ReferenceModifiers.MASK_REF_ALL);
			criteria.setReferencedRestrictions(
					VisibilityModifiers.ALL_VISIBILITIES,
					RestrictionModifiers.ALL_RESTRICTIONS);
			IReference[] refs = Util.getReferences(engine.search(Factory.newScope(
					new IApiComponent[]{component}), new IApiSearchCriteria[]{criteria}, null));
			IReference[] refs2 = Util.getReferences(engine.search(Factory.newScope(
					new IApiComponent[]{component2}), new IApiSearchCriteria[]{criteria}, null));
			Set<IReference> set = new HashSet<IReference>();
			System.out.println("original refs: " + refs.length + " stub refs: " + refs2.length);
			for (int i = 0; i < refs.length; i++) {
				set.add(refs[i]);
			}
			for (int i = 0; i < refs2.length; i++) {
				set.remove(refs2[i]);
			}
			Iterator<IReference> iterator = set.iterator();
			while (iterator.hasNext()) {
				IReference reference = (IReference) iterator.next();
				System.out.println(reference);
			}
		} finally {
			cleanupProfiles(profiles);
		}
	}

	private void cleanupProfiles(IApiProfile[] profiles) {
		if (profiles == null) return;
		for (int i = 0, max = profiles.length; i < max; i++) {
			IApiProfile apiProfile = profiles[i];
			apiProfile.dispose();
		}
	}	
	
	/**
	 * Exports the SDK, builds a new profile out of it. Returns the original and exported profiles.
	 * 
	 * @param options export options
	 * @throws CoreException
	 * @throws IOException 
	 */
	protected IApiProfile[] exportSDK(Map<String, Object> options) throws CoreException, IOException {
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
		}
		
		// check that delta is empty when compared		
		IApiProfile profile2 = TestSuiteHelper.createProfile("eclipse-2", exportRoot);
		return new IApiProfile[]{profile, profile2};
	}	

}
