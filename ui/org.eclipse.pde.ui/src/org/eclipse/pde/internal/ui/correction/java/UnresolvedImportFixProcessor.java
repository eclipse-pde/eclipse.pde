/*******************************************************************************
 * Copyright (c) 2007, 2025 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor;
import org.eclipse.osgi.service.resolver.BundleDescription;
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

		private final List<ClasspathFixProposal> proposals = new ArrayList<>();

		@Override
		public void addResolutionModification(IProject project, ExportPackageDescription desc, CompilationUnit cu,
				String typeToImport) {
			BundleDescription exporter = desc.getExporter();
			if (exporter == null) {
				return;
			}
			var change = JavaResolutionFactory.createRequireBundleChange(project, exporter, cu, typeToImport);
			ClasspathFixProposal proposal = JavaResolutionFactory.createClasspathFixProposal(change, 16);
			proposals.add(proposal);
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
		var findOperation = new FindClassResolutionsOperation(project.getProject(), name, collector);
		findOperation.run(new NullProgressMonitor());
		return collector.proposals.toArray(ClasspathFixProposal[]::new);
	}

}
