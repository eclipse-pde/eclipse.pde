/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeWizard;
import org.eclipse.pde.internal.ui.util.TextUtil;

public class NewClassCreationWizard extends JavaAttributeWizard {

	private boolean fIsInterface;

	private IPackageFragment packageName = null;
	private String className = null;
	private IPackageFragmentRoot packageRoot = null;

	public NewClassCreationWizard(IProject project, boolean isInterface, String value) {
		super(project, null, null, null);
		try {
			initializeValues(project, value);
		} catch (JavaModelException e) {
			// Ignore
		}
		fIsInterface = isInterface;
	}

	public void addPages() {
		if (fIsInterface)
			fMainPage = new NewInterfaceWizardPage();
		else
			fMainPage = new NewClassWizardPage();
		addPage(fMainPage);
		if (fIsInterface) {
			((NewInterfaceWizardPage) fMainPage).init(StructuredSelection.EMPTY);
			if (className != null)
				((NewInterfaceWizardPage) fMainPage).setTypeName(className, true);
			if (packageRoot != null)
				((NewInterfaceWizardPage) fMainPage).setPackageFragmentRoot(packageRoot, true);
			if (packageName != null)
				((NewInterfaceWizardPage) fMainPage).setPackageFragment(packageName, true);
		} else {
			((NewClassWizardPage) fMainPage).init(StructuredSelection.EMPTY);
			if (className != null)
				((NewClassWizardPage) fMainPage).setTypeName(className, true);
			if (packageRoot != null)
				((NewClassWizardPage) fMainPage).setPackageFragmentRoot(packageRoot, true);
			if (packageName != null)
				((NewClassWizardPage) fMainPage).setPackageFragment(packageName, true);
		}
	}

	private void initializeValues(IProject project, String value) throws JavaModelException {
		value = TextUtil.trimNonAlphaChars(value);
		IJavaProject javaProject = JavaCore.create(project);
		IPackageFragmentRoot srcEntryDft = null;
		IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
		for (int i = 0; i < roots.length; i++) {
			if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
				srcEntryDft = roots[i];
				break;
			}
		}
		if (srcEntryDft != null)
			packageRoot = srcEntryDft;
		else
			packageRoot = javaProject.getPackageFragmentRoot(javaProject.getResource());

		String packageNameString = null;
		int index = value.lastIndexOf("."); //$NON-NLS-1$
		if (index == -1) {
			className = value;
		} else {
			className = value.substring(index + 1);
			packageNameString = value.substring(0, index);
		}
		if (packageNameString != null && packageRoot != null) {
			IFolder packageFolder = project.getFolder(packageNameString);
			packageName = packageRoot.getPackageFragment(packageFolder.getProjectRelativePath().toOSString());
		}
	}
}
