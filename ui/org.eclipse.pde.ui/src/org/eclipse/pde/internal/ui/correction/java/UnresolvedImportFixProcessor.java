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
package org.eclipse.pde.internal.ui.correction.java;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.correction.java.FindClassResolutionsOperation.AbstractClassResolutionCollector;

/**
 * Offers a classpath fix proposal if the broken import statement can be
 * fixed by adding a plugin dependency (required bundle or package import).
 * @since 3.4
 */
public class UnresolvedImportFixProcessor extends ClasspathFixProcessor {

	private class ClasspathFixCollector extends AbstractClassResolutionCollector {

		private ArrayList fList = new ArrayList();

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.correction.java.FindClassResolutionsOperation.AbstractClassResolutionCollector#addResolutionModification(org.eclipse.core.resources.IProject, org.eclipse.osgi.service.resolver.ExportPackageDescription)
		 */
		public void addResolutionModification(IProject project, ExportPackageDescription desc) {
			if (desc.getSupplier() == null)
				return;
			Object proposal = JavaResolutionFactory.createRequireBundleProposal(project, desc, JavaResolutionFactory.TYPE_CLASSPATH_FIX, 16);
			if (proposal != null)
				fList.add(proposal);
		}

		/*
		 * Returns all the ClasspathFixProposals which were found
		 */
		public ClasspathFixProposal[] getProposals() {
			return (ClasspathFixProposal[]) fList.toArray(new ClasspathFixProposal[fList.size()]);
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor#getFixImportProposals(org.eclipse.jdt.core.IJavaProject, java.lang.String)
	 */
	public ClasspathFixProposal[] getFixImportProposals(IJavaProject project, String name) throws CoreException {
		if (!WorkspaceModelManager.isPluginProject(project.getProject()))
			return new ClasspathFixProposal[0];
		ClasspathFixCollector collector = new ClasspathFixCollector();
		IRunnableWithProgress findOperation = new FindClassResolutionsOperation(project.getProject(), name, collector);
		try {
			findOperation.run(new NullProgressMonitor());
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
		return collector.getProposals();
	}

}
