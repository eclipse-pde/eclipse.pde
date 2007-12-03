/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 109440
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class ConvertJarsAction implements IObjectActionDelegate {

	private IStructuredSelection selection;

	/**
	 * Constructor for Action1.
	 */
	public ConvertJarsAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		Map filesMap = new HashMap();
		Set projectSelection = new HashSet();
		Iterator i = selection.toList().iterator();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		while (i.hasNext()) {
			IPackageFragmentRoot pfr = (IPackageFragmentRoot) i.next();
			try {
				projectSelection.add(pfr.getJavaProject().getProject());
				IClasspathEntry rawClasspathEntry = pfr.getRawClasspathEntry();
				IPath path = rawClasspathEntry.getPath();
				IFile iFile = root.getFile(path);
				if (iFile.exists()) {
					JarFile jFile = new JarFile(iFile.getLocation().toString());
					if (!filesMap.containsKey(jFile.getManifest())) {
						filesMap.put(jFile.getManifest(), iFile);
					}
				} else {
					String pathStr = path.toString();
					JarFile file = new JarFile(pathStr);
					if (!filesMap.containsKey(file.getManifest())) {
						filesMap.put(file.getManifest(), new File(file
								.getName()));
					}
				}
			} catch (Exception e) {
				PDEPlugin.logException(e);
			}
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection s) {
		boolean enabled = true;
		if (s instanceof IStructuredSelection) {
			selection = (IStructuredSelection) s;
			if (selection.size() == 0)
				return;
			Iterator i = selection.iterator();
			while (i.hasNext()) {
				Object obj = i.next();
				if (obj instanceof IPackageFragmentRoot) {
					try {
						IPackageFragmentRoot packageFragment = (IPackageFragmentRoot) obj;
						if (packageFragment.getKind() == IPackageFragmentRoot.K_BINARY) {
							if (PDE.hasPluginNature(packageFragment
									.getJavaProject().getProject())) {
								if (packageFragment.getRawClasspathEntry()
										.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
									continue;
							}
						}
					} catch (JavaModelException e) {
					}
				}
				enabled = false;
				break;
			}
		} else {
			enabled = false;
			this.selection = null;
		}
		action.setEnabled(enabled);
	}

}
