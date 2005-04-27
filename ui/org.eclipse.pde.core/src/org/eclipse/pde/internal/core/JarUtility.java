/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.OpenableElementInfo;
import org.eclipse.jdt.internal.core.util.HashtableOfArrayToObject;
import org.eclipse.jdt.internal.core.util.Util;

public class JarUtility {
	protected boolean computeChildren(ZipFile jar, OpenableElementInfo info, Map newElements) throws JavaModelException {
		
		ArrayList vChildren= new ArrayList();
		final int JAVA = 0;
		final int NON_JAVA = 1;
		try {
			HashtableOfArrayToObject packageFragToTypes= new HashtableOfArrayToObject();
	
			for (Enumeration e= jar.entries(); e.hasMoreElements();) {
				ZipEntry member= (ZipEntry) e.nextElement();
				String entryName= member.getName();
	
				if (member.isDirectory()) {
					
					initPackageFragToTypes(packageFragToTypes, entryName, entryName.length()-1);
				} else {
					//store the class file / non-java rsc entry name to be cached in the appropriate package fragment
					//zip entries only use '/'
					int lastSeparator= entryName.lastIndexOf('/');
					String fileName= entryName.substring(lastSeparator + 1);
					String[] pkgName = initPackageFragToTypes(packageFragToTypes, entryName, lastSeparator);

					// add classfile info amongst children
					ArrayList[] children = (ArrayList[]) packageFragToTypes.get(pkgName);
					if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(entryName)) {
						//if (children[JAVA] == EMPTY_LIST) children[JAVA] = new ArrayList();
						children[JAVA].add(fileName);
					} else {
						//if (children[NON_JAVA] == EMPTY_LIST) children[NON_JAVA] = new ArrayList();
						children[NON_JAVA].add(fileName);
					}
				}
			}
			//loop through all of referenced packages, creating package fragments if necessary
			// and cache the entry names in the infos created for those package fragments
			/*for (int i = 0, length = packageFragToTypes.keyTable.length; i < length; i++) {
				String[] pkgName = (String[]) packageFragToTypes.keyTable[i];
				if (pkgName == null) continue;
				
				ArrayList[] entries= (ArrayList[]) packageFragToTypes.get(pkgName);
				//JarPackageFragment packFrag= (JarPackageFragment) getPackageFragment(pkgName);
				JarPackageFragmentInfo fragInfo= new JarPackageFragmentInfo();
				int resLength= entries[NON_JAVA].size();
				if (resLength == 0) {
					packFrag.computeNonJavaResources(CharOperation.NO_STRINGS, fragInfo, jar.getName());
				} else {
					String[] resNames= new String[resLength];
					entries[NON_JAVA].toArray(resNames);
					packFrag.computeNonJavaResources(resNames, fragInfo, jar.getName());
				}
				packFrag.computeChildren(fragInfo, entries[JAVA]);
				newElements.put(packFrag, fragInfo);
				vChildren.add(packFrag);
			}
		} catch (CoreException e) {
			if (e instanceof JavaModelException) throw (JavaModelException)e;
			throw new JavaModelException(e);*/
		} finally {
			JavaModelManager.getJavaModelManager().closeZipFile(jar);
		}


		IJavaElement[] children= new IJavaElement[vChildren.size()];
		vChildren.toArray(children);
		info.setChildren(children);
		return true;
	}
	
	private String[] initPackageFragToTypes(HashtableOfArrayToObject packageFragToTypes, String entryName, int lastSeparator) {
		String[] pkgName = Util.splitOn('/', entryName, 0, lastSeparator);
		String[] existing = null;
		int length = pkgName.length;
		int existingLength = length;
		while (existingLength >= 0) {
			existing = (String[]) packageFragToTypes.getKey(pkgName, existingLength);
			if (existing != null) break;
			existingLength--;
		}
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		for (int i = existingLength; i < length; i++) {
			System.arraycopy(existing, 0, existing = new String[i+1], 0, i);
			existing[i] = manager.intern(pkgName[i]);
			//packageFragToTypes.put(existing, new ArrayList[] { EMPTY_LIST, EMPTY_LIST });
		}
		
		return existing;
	}


}
