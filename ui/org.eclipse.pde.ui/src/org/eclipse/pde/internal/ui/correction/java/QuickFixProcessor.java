/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.ui.text.java.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.correction.java.FindClassResolutionsOperation.AbstractClassResolutionCollector;

public class QuickFixProcessor implements IQuickFixProcessor {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickFixProcessor#getCorrections(org.eclipse.jdt.ui.text.java.IInvocationContext, org.eclipse.jdt.ui.text.java.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		ArrayList results = new ArrayList();
		for (int i = 0; i < locations.length; i++) {
			int id = locations[i].getProblemId();
			switch (id) {
				case IProblem.ForbiddenReference :
					handleAccessRestrictionProblem(context, locations[i], results);
				case IProblem.ImportNotFound :
					handleImportNotFound(context, locations[i], results);

			}
		}
		return (IJavaCompletionProposal[]) results.toArray(new IJavaCompletionProposal[results.size()]);
	}

	/*
	 * Adds IJavaCompletionProposals for a ForbiddenReference marker
	 */
	private void handleAccessRestrictionProblem(IInvocationContext context, IProblemLocation location, Collection results) {
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
				IPackageFragment referencedPackage = (IPackageFragment) referencedElement.getJavaElement().getAncestor(IJavaElement.PACKAGE_FRAGMENT);
				IJavaProject currentProject = context.getCompilationUnit().getJavaProject();
				// only find proposals for Plug-in projects
				if (!WorkspaceModelManager.isPluginProject(currentProject.getProject()))
					return;
				// get the packages exported by the referenced plug-in project
				if (!referencedJavaProject.equals(currentProject)) {
					IPluginModelBase referencedModel = PluginRegistry.findModel(referencedJavaProject.getProject());
					ExportPackageDescription[] exportPackages = referencedModel.getBundleDescription().getExportPackages();
					// check if the required package is exported already
					boolean packageExported = false;
					if (referencedPackage != null) {
						for (int i = 0; i < exportPackages.length; i++) {
							if (exportPackages[i].getName().equals(referencedPackage.getElementName())) {
								packageExported = true;
								// check to see if access restriction is caused by Import-Package
								handleAccessRestrictionByImportPackage(context.getCompilationUnit().getJavaProject().getProject(), exportPackages[i], results);
								break;
							}
						}
						// if the package is not exported, add the quickfix
						if (!packageExported) {
							Object proposal = JavaResolutionFactory.createExportPackageProposal(referencedJavaProject.getProject(), referencedPackage, JavaResolutionFactory.TYPE_JAVA_COMPLETION, 100);
							if (proposal != null)
								results.add(proposal);
						}
					}
				} else {
					handleAccessRestrictionByImportPackage(referencedPackage, results);
				}
			}
		}
	}

	/*
	 * Adds IJavaCompletionProposals for a Require-Bundle if user is using an Import-Package from the bundle
	 */
	private void handleAccessRestrictionByImportPackage(IPackageFragment fragment, Collection results) {
		HashSet set = new HashSet();
		IProject project = fragment.getJavaProject().getProject();
		String pkgName = fragment.getElementName();
		IPluginModelBase base = PluginRegistry.findModel(project);
		ExportPackageDescription[] descs = base.getBundleDescription().getResolvedImports();
		for (int i = 0; i < descs.length; i++) {
			BundleDescription exporter = descs[i].getExporter();
			if (set.add(exporter.getSymbolicName())) {
				ExportPackageDescription[] exportedPkgs = exporter.getExportPackages();
				for (int j = 0; j < exportedPkgs.length; j++) {
					if (exportedPkgs[j].getName().equals(pkgName)) {
						Object proposal = JavaResolutionFactory.createRequireBundleProposal(project, exportedPkgs[j], JavaResolutionFactory.TYPE_JAVA_COMPLETION, 16);
						if (proposal != null)
							results.add(proposal);
					}
				}
			}
		}
	}

	/*
	 * Adds IJavaCompletionProposal for a Require-Bundle if user is using an Import-Package from the (workspace) bundle
	 */
	private void handleAccessRestrictionByImportPackage(IProject currentProject, ExportPackageDescription desc, Collection results) {
		BundleDescription supplier = desc.getSupplier();
		String supplierId = supplier.getSymbolicName();
		if (supplier != null) {
			IPluginModelBase base = PluginRegistry.findModel(currentProject);
			BundleDescription bd = base.getBundleDescription();
			BundleSpecification[] imports = bd.getRequiredBundles();
			boolean supplierImported = false;
			for (int j = 0; j < imports.length; j++) {
				BundleDescription importSupplier = (BundleDescription) imports[j].getSupplier();
				if (importSupplier != null && importSupplier.getSymbolicName().equals(supplierId)) {
					supplierImported = true;
					break;
				}
			}
			if (!supplierImported) {
				Object proposal = JavaResolutionFactory.createRequireBundleProposal(currentProject, desc, JavaResolutionFactory.TYPE_JAVA_COMPLETION, 16);
				if (proposal != null)
					results.add(proposal);
			}
		}
	}

	/*
	 * Adds IJavaCompletionProposals for a ImportNotFound problem
	 */
	private void handleImportNotFound(IInvocationContext context, IProblemLocation problemLocation, final Collection result) {
		CompilationUnit cu = context.getASTRoot();
		ASTNode selectedNode = problemLocation.getCoveringNode(cu);
		if (selectedNode != null) {
			ASTNode node = getParent(selectedNode);
			String className = null;
			String packageName = null;
			if (node == null) {
				if (selectedNode instanceof SimpleName) {
					ITypeBinding typeBinding = ((SimpleName) selectedNode).resolveTypeBinding();
					className = typeBinding.getBinaryName();
					packageName = typeBinding.getPackage().getName();
				}
			} else if (node instanceof ImportDeclaration) {
				// Find import declaration which is the problem
				className = ((ImportDeclaration) node).getName().getFullyQualifiedName();

				// always add the search repositories proposal
				int lastPeriod = className.lastIndexOf('.'); // if there is no period assume we are importing a single name package
				packageName = className.substring(0, lastPeriod >= 0 ? lastPeriod : className.length());
				result.add(JavaResolutionFactory.createSearchRepositoriesProposal(packageName));
			}

			if (className != null && packageName != null) {
				IProject project = cu.getJavaElement().getJavaProject().getProject();
				// only try to find proposals on Plug-in Projects
				if (!WorkspaceModelManager.isPluginProject(project))
					return;

				// create a collector that will create IJavaCompletionProposals and load them into 'result'
				AbstractClassResolutionCollector collector = createCollector(result);
				IRunnableWithProgress findOperation = new FindClassResolutionsOperation(project, className, collector);
				try {
					findOperation.run(new NullProgressMonitor());
				} catch (InvocationTargetException e) {
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/*
	 * Custom AbstractClassResolutionCollector which will only add one IJavaCompletionProposal for adding an Import-Package entry
	 */
	private AbstractClassResolutionCollector createCollector(final Collection result) {
		return new AbstractClassResolutionCollector() {

			boolean isDone = false;

			public void addResolutionModification(IProject project, ExportPackageDescription desc) {
				Object proposal = JavaResolutionFactory.createImportPackageProposal(project, desc, JavaResolutionFactory.TYPE_JAVA_COMPLETION, 17);
				if (proposal != null) {
					result.add(proposal);
					isDone = true;
				}
			}

			// we want to finish after we add the first Import-Package Change
			public boolean isDone() {
				return isDone;
			}
		};
	}

	/*
	 * Copied from org.eclipse.jdt.internal.coreext.dom.ASTNoes.getParent.  Simplified for IMPORT_DECLARATION
	 */
	private static ASTNode getParent(ASTNode node) {
		do {
			node = node.getParent();
		} while (node != null && node.getNodeType() != ASTNode.IMPORT_DECLARATION);
		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IQuickFixProcessor#hasCorrections(org.eclipse.jdt.core.ICompilationUnit, int)
	 */
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		switch (problemId) {
			case IProblem.ForbiddenReference :
			case IProblem.ImportNotFound :
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
