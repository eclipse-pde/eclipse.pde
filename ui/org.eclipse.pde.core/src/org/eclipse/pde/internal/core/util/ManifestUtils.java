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
package org.eclipse.pde.internal.core.util;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;

public class ManifestUtils {

	private ManifestUtils() {}

	public static IPackageFragmentRoot[] findPackageFragmentRoots(IManifestHeader header, IProject project) {
		IJavaProject javaProject = JavaCore.create(project);

		String[] libs;
		if (header == null || header.getValue() == null) 
			libs = new String[] {"."}; //$NON-NLS-1$
		else 
			libs = header.getValue().split(","); //$NON-NLS-1$

		List pkgFragRoots = new LinkedList();
		IBuild build = null;

		for (int j = 0; j < libs.length; j++) {
			String lib = libs[j];
			IPackageFragmentRoot root = null;
			if (!lib.equals(".")) //$NON-NLS-1$
				root = javaProject.getPackageFragmentRoot(project.getFile(lib));
			if (root != null && root.exists()) {
				pkgFragRoots.add(root);
			} else {
				// Parse build.properties only once
				if (build == null) 
					build = getBuild(project);
				// if valid build.properties exists.  Do NOT use else statement!  getBuild() could return null.
				if (build != null) {  
					IBuildEntry entry = build.getEntry("source." + lib); //$NON-NLS-1$
					if (entry == null)
						continue;
					String[] tokens = entry.getTokens();
					for (int i = 0; i < tokens.length; i++) {
						if(tokens[i].length() == 0) continue;
						root = javaProject.getPackageFragmentRoot(project.getFolder(tokens[i]));
						if (root != null && root.exists())
							pkgFragRoots.add(root);
					}
				}
			}
		}
		return (IPackageFragmentRoot[]) pkgFragRoots.toArray(new IPackageFragmentRoot[pkgFragRoots.size()]);
	}

	public final static IBuild getBuild(IProject project){
		IFile buildProps = project.getFile("build.properties"); //$NON-NLS-1$
		if (buildProps != null) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildProps);
			if (model != null) 
				return model.getBuild();
		}
		return null;
	}

	public static boolean isImmediateRoot(IPackageFragmentRoot root) throws JavaModelException {
		int kind = root.getKind();
		return kind == IPackageFragmentRoot.K_SOURCE
		|| (kind == IPackageFragmentRoot.K_BINARY && !root.isExternal());
	}

}
