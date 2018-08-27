/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 109440
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.io.File;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.plugin.NewLibraryPluginProjectWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;

public class ConvertJarsAction implements IObjectActionDelegate {

	private IStructuredSelection selection;
	private IWorkbench workbench;

	public ConvertJarsAction() {
		super();
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		workbench = targetPart.getSite().getWorkbenchWindow().getWorkbench();
	}

	@Override
	public void run(IAction action) {
		Map<Manifest, Object> filesMap = new HashMap<>();
		Set<IProject> projectSelection = new HashSet<>();
		Iterator<?> i = selection.toList().iterator();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		while (i.hasNext()) {
			IPackageFragmentRoot pfr = (IPackageFragmentRoot) i.next();
			try {
				projectSelection.add(pfr.getJavaProject().getProject());
				IClasspathEntry rawClasspathEntry = pfr.getRawClasspathEntry();
				IPath path = rawClasspathEntry.getPath();
				IFile iFile = root.getFile(path);
				if (iFile.exists()) {
					try (JarFile file = new JarFile(iFile.getLocation().toString())) {
						if (!filesMap.containsKey(file.getManifest())) {
							filesMap.put(file.getManifest(), iFile);
						}
					}
				} else {
					String pathStr = path.toString();
					try (JarFile file = new JarFile(pathStr)) {
						if (!filesMap.containsKey(file.getManifest())) {
							filesMap.put(file.getManifest(), new File(file.getName()));
						}
					}
				}
			} catch (Exception e) {
				PDEPlugin.logException(e);
			}
		}
		NewLibraryPluginProjectWizard wizard = new NewLibraryPluginProjectWizard(filesMap.values(), projectSelection);
		wizard.init(workbench, selection);
		WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
		dialog.open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection s) {
		boolean enabled = true;
		if (s instanceof IStructuredSelection) {
			selection = (IStructuredSelection) s;
			if (selection.isEmpty())
				return;
			Iterator<?> i = selection.iterator();
			while (i.hasNext()) {
				Object obj = i.next();
				if (obj instanceof IPackageFragmentRoot) {
					try {
						IPackageFragmentRoot packageFragment = (IPackageFragmentRoot) obj;
						if (packageFragment.getKind() == IPackageFragmentRoot.K_BINARY) {
							if (PDE.hasPluginNature(packageFragment.getJavaProject().getProject())) {
								if (packageFragment.getRawClasspathEntry().getEntryKind() == IClasspathEntry.CPE_LIBRARY)
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
