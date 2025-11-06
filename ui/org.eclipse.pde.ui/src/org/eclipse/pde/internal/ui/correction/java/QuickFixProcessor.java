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
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 428065
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.bnd.ui.autocomplete.AddBundleCompletionProposal;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.natures.BndProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.correction.java.FindClassResolutionsOperation.AbstractClassResolutionCollector;
import org.eclipse.swt.graphics.Image;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Constants;

public class QuickFixProcessor implements IQuickFixProcessor {

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		List<Object> results = new ArrayList<>();
		AbstractClassResolutionCollector collector = createCollector(results, context);
		for (IProblemLocation location : locations) {
			switch (location.getProblemId()) {
				case IProblem.ForbiddenReference:
					handleAccessRestrictionProblem(context, location, collector);
				case IProblem.ImportNotFound, IProblem.UndefinedName, IProblem.UndefinedType, IProblem.UnresolvedVariable, IProblem.MissingTypeInMethod, IProblem.MissingTypeInConstructor:
					handleImportNotFound(context, location, collector);

			}
		}
		return results.toArray(new IJavaCompletionProposal[results.size()]);
	}

	/*
	 * Adds IJavaCompletionProposals for a ForbiddenReference marker
	 */
	private void handleAccessRestrictionProblem(IInvocationContext context, IProblemLocation location, AbstractClassResolutionCollector collector) {
		IBinding referencedElement = null;
		ASTNode node = location.getCoveredNode(context.getASTRoot());
		if (node instanceof Type type) {
			referencedElement = type.resolveBinding();
		} else if (node instanceof Name name) {
			referencedElement = name.resolveBinding();
		} else if (node instanceof MethodInvocation methodInvocation) {
			IMethodBinding tempMethod = methodInvocation.resolveMethodBinding();
			if (tempMethod != null) {
				referencedElement = tempMethod.getDeclaringClass();
			}
		} else if (node instanceof FieldAccess fieldAccess) {
			IVariableBinding tempVariable = fieldAccess.resolveFieldBinding();
			if (tempVariable != null) {
				referencedElement = tempVariable.getDeclaringClass();
			}
		}
		if (referencedElement != null) {
			// get the project that contains the reference element
			// ensure it exists in the workspace and is a plug-in project
			IJavaProject referencedJavaProject = referencedElement.getJavaElement().getJavaProject();
			if (referencedJavaProject != null && WorkspaceModelManager.isPluginProject(referencedJavaProject.getProject())) {
				IPackageFragment referencedPackage = (IPackageFragment) referencedElement.getJavaElement().getAncestor(IJavaElement.PACKAGE_FRAGMENT);
				IJavaProject currentProject = context.getCompilationUnit().getJavaProject();
				// only find proposals for Plug-in projects
				if (!WorkspaceModelManager.isPluginProject(currentProject.getProject())) {
					return;
				}
				// get the packages exported by the referenced plug-in project
				if (!referencedJavaProject.equals(currentProject)) {
					IPluginModelBase referencedModel = PluginRegistry.findModel(referencedJavaProject.getProject());
					ExportPackageDescription[] exportPackages = referencedModel.getBundleDescription().getExportPackages();
					// check if the required package is exported already
					boolean packageExported = false;
					if (referencedPackage != null) {
						for (ExportPackageDescription exportPackage : exportPackages) {
							if (exportPackage.getName().equals(referencedPackage.getElementName())) {
								packageExported = true;
								// check to see if access restriction is caused by Import-Package
								handleAccessRestrictionByImportPackage(context.getCompilationUnit().getJavaProject().getProject(), exportPackage, collector);
								break;
							}
						}
						// if the package is not exported, add the quickfix
						if (!packageExported) {
							collector.addExportPackageResolutionModification(referencedPackage);
						}
					}
				} else {
					handleAccessRestrictionByImportPackage(referencedPackage, collector);
				}
			}
		}
	}

	/*
	 * Adds IJavaCompletionProposals for a Require-Bundle if user is using an Import-Package from the bundle
	 */
	private void handleAccessRestrictionByImportPackage(IPackageFragment fragment, AbstractClassResolutionCollector collector) {
		Set<String> set = new HashSet<>();
		IProject project = fragment.getJavaProject().getProject();
		String pkgName = fragment.getElementName();
		IPluginModelBase base = PluginRegistry.findModel(project);
		ExportPackageDescription[] descs = base.getBundleDescription().getResolvedImports();
		ExportPackageDescription foundExportPackage = null;
		for (ExportPackageDescription desc : descs) {
			BundleDescription exporter = desc.getExporter();
			if (set.add(exporter.getSymbolicName())) {
				ExportPackageDescription[] exportedPkgs = exporter.getExportPackages();
				for (ExportPackageDescription exportedPkg : exportedPkgs) {
					if (exportedPkg.getName().equals(pkgName)) {
						foundExportPackage = exportedPkg; // any one is fine, so simply remember the last one
						collector.addRequireBundleModification(project, exportedPkg, 16);
						break;
					}
				}
			}
		}
		if (foundExportPackage != null) {
			collector.addResolutionModification(project, foundExportPackage);
		}
	}

	/*
	 * Adds IJavaCompletionProposal for a Require-Bundle if user is using an Import-Package from the (workspace) bundle
	 */
	private void handleAccessRestrictionByImportPackage(IProject currentProject, ExportPackageDescription desc, AbstractClassResolutionCollector collector) {
		BundleDescription supplier = desc.getSupplier();
		if (supplier != null) {
			String supplierId = supplier.getSymbolicName();
			IPluginModelBase base = PluginRegistry.findModel(currentProject);
			BundleDescription bd = base.getBundleDescription();
			BundleSpecification[] imports = bd.getRequiredBundles();
			boolean supplierImported = false;
			for (BundleSpecification importstatement : imports) {
				BundleDescription importSupplier = (BundleDescription) importstatement.getSupplier();
				if (importSupplier != null && importSupplier.getSymbolicName().equals(supplierId)) {
					supplierImported = true;
					break;
				}
			}
			if (!supplierImported) {
				// add import-package, if possible
				boolean proposeImportPackage = true;

				ImportPackageSpecification[] importPackages = bd.getImportPackages();
				for (ImportPackageSpecification importPackage : importPackages) {
					if (desc.getName().equals(importPackage.getName())) {
						// already imported, try require-bundle
						proposeImportPackage = false;
						break;
					}
				}

				if (proposeImportPackage) {
					collector.addResolutionModification(currentProject, desc); // relevance should actually be 16...
				}
				collector.addRequireBundleModification(currentProject, desc, 16);
			}
		}
	}

	/*
	 * Adds IJavaCompletionProposals for a ImportNotFound problem
	 */
	private void handleImportNotFound(IInvocationContext context, IProblemLocation problemLocation, final AbstractClassResolutionCollector collector) {
		CompilationUnit cu = context.getASTRoot();
		ASTNode selectedNode = problemLocation.getCoveringNode(cu);
		if (selectedNode != null) {
			ASTNode node = getParent(selectedNode);
			String className = null;
			if (node == null) {
				if (selectedNode instanceof Name name) {
					ITypeBinding typeBinding = name.resolveTypeBinding();
					if (typeBinding != null) {
						className = typeBinding.getBinaryName();
					}
					if (className == null && selectedNode instanceof SimpleName simpleName) {
						// fallback if the type cannot be resolved
						className = simpleName.getIdentifier();
					}
				}
			} else if (node instanceof ImportDeclaration importDeclaration) {
				// Find the full package name, strip off the class name or on demand qualifier '.*';
				String packageName = importDeclaration.getName().getFullyQualifiedName();
				if (!importDeclaration.isOnDemand()) {
					int lastPeriod = packageName.lastIndexOf('.'); // if there is no period assume we are importing a single name package
					packageName = packageName.substring(0, lastPeriod >= 0 ? lastPeriod : packageName.length());
				}
				// always add the search repositories proposal
				collector.addSearchRepositoriesModification(packageName);
			}

			if (className != null) {
				IProject project = cu.getJavaElement().getJavaProject().getProject();
				// only try to find proposals on Plug-in Projects
				if (!WorkspaceModelManager.isPluginProject(project)) {
					return;
				}
				new FindClassResolutionsOperation(project, cu, className, collector).run(new NullProgressMonitor());
			}
		}
	}

	/*
	 * Custom AbstractClassResolutionCollector which will only add one IJavaCompletionProposal for adding an Import-Package or Export-Package entry
	 */
	private AbstractClassResolutionCollector createCollector(Collection<Object> result, IInvocationContext context) {
		return new AbstractClassResolutionCollector() {

			// the list of package names for which an import package resolution has been created
			private final Set<String> addedImportPackageResolutions = new HashSet<>();

			boolean isDone = false;

			@Override
			public void addResolutionModification(IProject project, ExportPackageDescription desc, CompilationUnit cu,
					String typeToImport) {
				if (BndProject.isBndProject(project)) {
					// for bnd projects the proposal to import a package is not
					// really useful as import package is computed automatically
					return;
				}
				var change = JavaResolutionFactory.createImportPackageChange(project, desc, cu, typeToImport);
				result.add(JavaResolutionFactory.createJavaCompletionProposal(change, 20));
				// guard against multiple import package resolutions for the
				// same package
				isDone |= addedImportPackageResolutions.add(desc.getName());
			}

			@Override
			public IJavaCompletionProposal addExportPackageResolutionModification(IPackageFragment aPackage) {
				IJavaCompletionProposal proposal = super.addExportPackageResolutionModification(aPackage);
				if (proposal != null) {
					result.add(proposal);
				}
				return proposal;
			}

			@Override
			public Object addRequireBundleModification(IProject project, ExportPackageDescription desc, int relevance) {
				return addRequireBundleModification(project, desc, relevance, null, ""); //$NON-NLS-1$
			}

			@Override
			public IJavaCompletionProposal addRequireBundleModification(IProject project, ExportPackageDescription desc,
					int relevance, CompilationUnit cu, String qualifiedTypeToImport) {
				IJavaCompletionProposal proposal;
				if (BndProject.isBndProject(project)) {
					proposal = createAddBundleCompletionProposal(project, desc, relevance, cu, qualifiedTypeToImport);
				} else {
					proposal = super.addRequireBundleModification(project, desc, relevance, cu, qualifiedTypeToImport);
				}
				if (proposal != null) {
					result.add(proposal);
				}
				return proposal;
			}

			private AddBundleCompletionProposal createAddBundleCompletionProposal(IProject project, ExportPackageDescription desc, int relevance,
					CompilationUnit cu, String qualifiedTypeToImport) {
				BundleDescription supplier = desc.getSupplier();
				if (supplier == null) {
					return null;
				}
				Project bndProject = Adapters.adapt(project, Project.class);
				if (bndProject == null) {
					return null;
				}
				String symbolicName = supplier.getSymbolicName();
				return new AddBundleCompletionProposal(new BundleId(symbolicName, (String) null),
							Map.of(qualifiedTypeToImport, true),
						relevance, context, bndProject, Constants.BUILDPATH) {
					@Override
					public Image getImage() {
						return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ);
					}
				};
			}

			@Override
			public Object addSearchRepositoriesModification(String packageName) {
				Object proposal = super.addSearchRepositoriesModification(packageName);
				if (proposal != null) {
					result.add(proposal);
				}
				return proposal;
			}

			// we want to finish after we add the first Import- or Export-Package Change
			@Override
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

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		switch (problemId) {
			case IProblem.ForbiddenReference :
			case IProblem.UndefinedName : // fall through
			case IProblem.ImportNotFound : // fall through
			case IProblem.UndefinedType : // fall through
			case IProblem.UnresolvedVariable : // fall through
			case IProblem.MissingTypeInMethod : // fall through
			case IProblem.MissingTypeInConstructor :
				IJavaElement parent = unit.getParent();
				if (parent != null) {
					IJavaProject project = parent.getJavaProject();
					if (project != null) {
						return WorkspaceModelManager.isPluginProject(project.getProject());
					}
				}
		}
		return false;
	}

}
