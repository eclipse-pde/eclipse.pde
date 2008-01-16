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
package org.eclipse.pde.internal.ui.correction.java;

import java.util.Hashtable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.ui.text.java.*;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.WorkspaceModelManager;

public class QuickFixProcessor implements IQuickFixProcessor {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickFixProcessor#getCorrections(org.eclipse.jdt.ui.text.java.IInvocationContext, org.eclipse.jdt.ui.text.java.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		Hashtable results = new Hashtable();
		for (int i = 0; i < locations.length; i++) {
			int id = locations[i].getProblemId();
			switch (id) {
				case IProblem.ForbiddenReference :
					handleAccessRestrictionProblem(context, locations[i], results);
			}
		}
		return (IJavaCompletionProposal[]) results.values().toArray(new IJavaCompletionProposal[results.size()]);
	}

	private void handleAccessRestrictionProblem(IInvocationContext context, IProblemLocation location, Hashtable results) {
		IBinding referencedElement = null;
		ASTNode node = location.getCoveredNode(context.getASTRoot());
		if (node instanceof Type) {
			referencedElement = ((Type) node).resolveBinding();
		} else if (node instanceof Name) {
			referencedElement = ((Name) node).resolveBinding();
		}
		if (referencedElement != null) {
			// get the project that contains the reference element
			// ensure it exists in the workspace and is a plug-in project
			IJavaProject referencedJavaProject = referencedElement.getJavaElement().getJavaProject();
			if (referencedJavaProject != null && WorkspaceModelManager.isPluginProject(referencedJavaProject.getProject())) {
				// get the packages exported by the referenced plug-in project
				if (referencedJavaProject.equals(context.getCompilationUnit().getJavaProject()))
					return;
				IPluginModelBase referencedModel = PluginRegistry.findModel(referencedJavaProject.getProject());
				ExportPackageDescription[] exportPackages = referencedModel.getBundleDescription().getExportPackages();
				// check if the required package is exported already
				boolean packageExported = false;
				IPackageFragment referencedPackage = (IPackageFragment) referencedElement.getJavaElement().getAncestor(IJavaElement.PACKAGE_FRAGMENT);
				if (referencedPackage != null) {
					for (int i = 0; i < exportPackages.length; i++) {
						if (exportPackages[i].getName().equals(referencedPackage.getElementName())) {
							packageExported = true;
							break;
						}
					}
					// if the package is not exported, add the quickfix
					if (!packageExported) {
						results.put(referencedPackage, new ForbiddenAccessProposal(referencedPackage, referencedJavaProject.getProject()));
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickFixProcessor#hasCorrections(org.eclipse.jdt.core.ICompilationUnit, int)
	 */
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		switch (problemId) {
			case IProblem.ForbiddenReference :
				IJavaElement parent = unit.getParent();
				if (parent != null) {
					IJavaProject project = parent.getJavaProject();
					if (project != null)
						return WorkspaceModelManager.isPluginProject(project.getProject());
				}
		}
		return false;
	}

}
