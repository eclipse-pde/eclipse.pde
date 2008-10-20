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
package org.eclipse.pde.api.tools.reference.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.provisional.search.ILocation;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.util.Util;

/**
 * This class tests discouraged access between components.
 * 
 * @since 1.0.0
 */
public class DiscouragedAccessTests extends TestCase {
	
	/**
	 * Extract all references in JDT debug UI
	 * 
	 * @throws CoreException
	 */
	public void testDiscouragedAccess() throws CoreException {
		// build baseline
		File jdt = TestSuiteHelper.getBundle("org.eclipse.jdt.debug.ui");
		assertNotNull("Missing jdt.debug.ui", jdt);
		IApiBaseline profile = TestSuiteHelper.createProfile("eclipse", jdt.getParentFile());
		
		// search
		IApiComponent jdtComponent = profile.getApiComponent("org.eclipse.jdt.debug.ui");
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchScope scope = Factory.newScope(new IApiComponent[]{jdtComponent});
		long start = System.currentTimeMillis();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setReferenceKinds(ReferenceModifiers.MASK_REF_ALL);
		criteria.setReferencedRestrictions(
				VisibilityModifiers.ALL_VISIBILITIES,
				RestrictionModifiers.ALL_RESTRICTIONS);
		IReference[] references = Util.getReferences(engine.search(scope, new IApiSearchCriteria[]{criteria}, null));
		long stop = System.currentTimeMillis();
		System.out.println("Search time: " + (stop - start) + "ms");
				
		// analyze
		int total = 0;
		int priv = 0;
		int unres = 0;
		int internal = 0;
		int external = 0;
		int virtual = 0;
		int pvirtual = 0;
		int pvirtualext = 0;
		int pvirtualint = 0;
		Map<String, List<IReference>> illegalRefsByType = new HashMap<String, List<IReference>>(); 
		Map<String, Set<String>> illegalSetRefsByType = new HashMap<String, Set<String>>();
		for (int i = 0; i < references.length; i++) {
			IReference ref = references[i];
			total++;
			IApiAnnotations resolved = ref.getResolvedAnnotations();
			if (resolved != null) {
				if(ref.getReferenceKind() == ReferenceModifiers.REF_VIRTUALMETHOD) {
					virtual++;
				}
				if (VisibilityModifiers.isPrivate(resolved.getVisibility())) {
					if(ref.getReferenceKind() == ReferenceModifiers.REF_VIRTUALMETHOD) {
						pvirtual++;
					}
					priv++;
					ILocation target = ref.getReferencedLocation();
					if (jdtComponent.equals(target.getApiComponent())) {
						if(ref.getReferenceKind() == ReferenceModifiers.REF_VIRTUALMETHOD) {
							pvirtualint++;
						}
						internal++;
					} else {
						if(ref.getReferenceKind() == ReferenceModifiers.REF_VIRTUALMETHOD) {
							pvirtualext++;
						}
						external++;
						String sourceName = ref.getSourceLocation().getType().getQualifiedName();
						List<IReference> list = illegalRefsByType.get(sourceName);
						Set<String> set = illegalSetRefsByType.get(sourceName);
						if (list == null) {
							list = new ArrayList<IReference>();
							set = new HashSet<String>();
							illegalRefsByType.put(sourceName, list);
							illegalSetRefsByType.put(sourceName, set);
						}
						list.add(ref);
						set.add(ref.getReferencedLocation().getType().getQualifiedName());
					}
				}				
			} else {
				unres++;
			}
		}
		
		// count effective import statements for illegal references
		int imports = 0;
		Iterator<Set<String>> iterator3 = illegalSetRefsByType.values().iterator();
		while (iterator3.hasNext()) {
			Set<String> set = (Set<String>) iterator3.next();
			imports += set.size();
		}
		
		System.out.println("** References **");
		System.out.println("Total:\t\t\t\t" + total);
		System.out.println("Private:\t\t\t" + priv);
		System.out.println("Unresolved:\t\t\t" + unres);
		System.out.println("Internal private:\t\t" + internal);
		System.out.println("External private:\t\t" + external);
		System.out.println("Inferred import private:\t" + imports);
		System.out.println("Virtual method invocations:\t"+virtual);
		System.out.println("** Private virtual method invocations **");
		System.out.println("Total:\t\t"+pvirtual);
		System.out.println("Internal:\t"+pvirtualint);
		System.out.println("External:\t"+pvirtualext);
		
		
		// dump the reference list
		Iterator<String> types = illegalRefsByType.keySet().iterator();
		while (types.hasNext()) {
			String name = (String) types.next();
			List<IReference> list = illegalRefsByType.get(name);
			Iterator<IReference> iterator2 = list.iterator();
			System.out.println();
			System.out.println("**" + name + "**");
			while (iterator2.hasNext()) {
				IReference reference = (IReference) iterator2.next();
				System.out.println(reference.toString());
			}
		}
		
		profile.dispose();
	}

}
