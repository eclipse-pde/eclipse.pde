/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction.java;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.correction.java.FindClassResolutionsOperation.AbstractClassResolutionCollector;

import aQute.bnd.build.Project;

/**
 * Offers a classpath fix proposal if the broken import statement can be
 * fixed by adding a plugin dependency (required bundle or package import).
 * @since 3.4
 */
public class UnresolvedImportFixProcessor extends ClasspathFixProcessor {

	private static class ClasspathFixCollector extends AbstractClassResolutionCollector {

		private final ArrayList<Object> fList = new ArrayList<>();

		@Override
		public void addResolutionModification(IProject project, ExportPackageDescription desc) {
			addResolutionModification(project, desc, null, ""); //$NON-NLS-1$
		}

		@Override
		public void addResolutionModification(IProject project, ExportPackageDescription desc, CompilationUnit cu,
				String qualifiedTypeToImport) {
			if (desc.getSupplier() == null) {
				return;
			}
			Object proposal = JavaResolutionFactory.createRequireBundleProposal(project, desc,
					JavaResolutionFactory.TYPE_CLASSPATH_FIX, 16, cu, qualifiedTypeToImport);
			if (proposal != null) {
				fList.add(proposal);
			}
		}

		/*
		 * Returns all the ClasspathFixProposals which were found
		 */
		public ClasspathFixProposal[] getProposals() {
			return fList.toArray(new ClasspathFixProposal[fList.size()]);
		}


	}

	@Override
	public ClasspathFixProposal[] getFixImportProposals(IJavaProject project, String name) throws CoreException {
		if (!WorkspaceModelManager.isPluginProject(project.getProject())) {
			IFile bnd = project.getProject().getFile(Project.BNDFILE);
			if (bnd.exists()) {
				// it could be a bnd workspace project!
				return new ClasspathFixProposal[] { new AddPdeClasspathContainerClasspathFixProposal(project) };
			}
			return new ClasspathFixProposal[0];
		}
		ClasspathFixCollector collector = new ClasspathFixCollector();
		IRunnableWithProgress findOperation = new FindClassResolutionsOperation(project.getProject(), name, collector);
		try {
			findOperation.run(new NullProgressMonitor());
		} catch (InvocationTargetException | InterruptedException e) {
		}
		return collector.getProposals();
	}

}
