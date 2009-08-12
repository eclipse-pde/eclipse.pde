/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.ui.internal.actions.ActionMessages;
import org.eclipse.pde.api.tools.ui.internal.actions.DeltaSession;

/**
 * Default operation for comparing a given selection to the given API baseline
 * 
 * @since 1.0.1
 */
public class CompareOperation extends Job {

	private IApiBaseline baseline = null;
	private IStructuredSelection selection = null;
	
	/**
	 * Constructor
	 * 
	 * @param baseline
	 * @param selection
	 */
	public CompareOperation(IApiBaseline baseline, IStructuredSelection selection) {
		super(ActionMessages.CompareWithAction_comparing_apis);
		this.baseline = baseline;
		Assert.isNotNull(this.baseline);
		this.selection = selection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(ActionMessages.CompareDialogCollectingElementTaskName, IProgressMonitor.UNKNOWN);
		String baselineName = this.baseline.getName();
		final IApiScope scope = walkStructureSelection(this.selection, monitor);
		try {
			monitor.subTask(ActionMessages.CompareDialogComputeDeltasTaskName);
			try {
				IDelta delta = ApiComparator.compare(scope, baseline, VisibilityModifiers.API, false, monitor);
				if (delta == null) {
					// we don't want to continue. The .log file should already contain details about the failure
					return Status.CANCEL_STATUS;
				}
				int size = this.selection.size();
				String description = NLS.bind(ActionMessages.CompareWithAction_compared_with_against, new Object[] {
						new Integer(size), 
						baselineName, 
						new Integer(delta.getChildren().length)
					});
				if(size == 1) {
					description = NLS.bind(ActionMessages.CompareWithAction_compared_project_with, new Object[] {
							((IJavaElement)this.selection.getFirstElement()).getElementName(), 
							baselineName, 
							new Integer(delta.getChildren().length)
						});
				}
				ApiPlugin.getDefault().getSessionManager().addSession(new DeltaSession(description, delta, baselineName), true);
				return Status.OK_STATUS;
			} catch (CoreException e) {
				ApiPlugin.log(e);
			} catch(OperationCanceledException e) {
				// ignore
			}
		} finally {
			monitor.done();
		}
		return Status.CANCEL_STATUS;
	}

	public static ApiScope walkStructureSelection(
			IStructuredSelection structuredSelection,
			IProgressMonitor monitor) {
		Object[] selected=structuredSelection.toArray();
		ApiScope scope = new ApiScope();
		IApiBaseline workspaceBaseline = ApiBaselineManager.getManager().getWorkspaceBaseline();
		if (workspaceBaseline == null) {
			return scope;
		}
		Arrays.sort(selected, new Comparator() {
			public int compare(Object o1, Object o2) {
				if (o1 instanceof IJavaElement && o2 instanceof IJavaElement) {
					IJavaElement element = (IJavaElement) o1;
					IJavaElement element2 = (IJavaElement) o2;
					return element.getElementType() - element2.getElementType();
				}
				return 0;
			}
		});
		int length = selected.length;
		for (int i=0; i < length; i++) {
			Object currentSelection = selected[i];
			if (currentSelection instanceof IJavaElement) {
				IJavaElement element =(IJavaElement) currentSelection;
				IJavaProject javaProject = element.getJavaProject();
				try {
					switch (element.getElementType()) {
						case IJavaElement.COMPILATION_UNIT: {
							ICompilationUnit compilationUnit = (ICompilationUnit) element;
							IApiComponent apiComponent = workspaceBaseline.getApiComponent(javaProject.getElementName());
							if (apiComponent != null) {
								addElementFor(compilationUnit, apiComponent, scope);
							}
							break;
						}
						case IJavaElement.PACKAGE_FRAGMENT: {
							IPackageFragment fragment = (IPackageFragment) element;
							IApiComponent apiComponent = workspaceBaseline.getApiComponent(javaProject.getElementName());
							IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) fragment.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
							boolean isArchive = false;
							if (packageFragmentRoot != null) {
								isArchive = packageFragmentRoot.isArchive();
							}
							if (apiComponent != null) {
								addElementFor(fragment, isArchive, apiComponent, scope);
							}
							break;
						}
						case IJavaElement.PACKAGE_FRAGMENT_ROOT: {
							IPackageFragmentRoot fragmentRoot = (IPackageFragmentRoot) element;
							IApiComponent apiComponent = workspaceBaseline.getApiComponent(javaProject.getElementName());
							if (apiComponent != null) {
								addElementFor(fragmentRoot, apiComponent, scope);
							}
							break;
						}
						case IJavaElement.JAVA_PROJECT:
							IApiComponent apiComponent = workspaceBaseline.getApiComponent(javaProject.getElementName());
							if (apiComponent != null) {
								scope.addElement(apiComponent);
							}
							break;
					}
				} catch (JavaModelException e) {
					ApiPlugin.log(e);
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
		return scope;
	}

	private static void addElementFor(
			IPackageFragmentRoot fragmentRoot, IApiComponent apiComponent,
			ApiScope scope) throws JavaModelException, CoreException {
		boolean isArchive = fragmentRoot.isArchive();
		IJavaElement[] packageFragments = fragmentRoot.getChildren();
		for (int j = 0, max2 = packageFragments.length; j < max2; j++) {
			IPackageFragment packageFragment = (IPackageFragment) packageFragments[j];
			addElementFor(packageFragment, isArchive, apiComponent, scope);
		}
	}

	private static void addElementFor(
			IPackageFragment packageFragment,
			boolean isArchive,
			IApiComponent apiComponent,
			ApiScope scope)
		throws JavaModelException, CoreException {

		// add package fragment elements only if this is an API package
		IApiDescription apiDescription = apiComponent.getApiDescription();
		IApiAnnotations annotations = apiDescription.resolveAnnotations(Factory.packageDescriptor(packageFragment.getElementName()));
		if (annotations == null || !VisibilityModifiers.isAPI(annotations.getVisibility())) {
			return;
		}
		if (isArchive) {
			IClassFile[] classFiles = packageFragment.getClassFiles();
			for (int i = 0, max= classFiles.length; i < max; i++) {
				addElementFor(classFiles[i], apiComponent, scope);
			}
		} else {
			ICompilationUnit[] units = packageFragment.getCompilationUnits();
			for (int i = 0, max= units.length; i < max; i++) {
				addElementFor(units[i], apiComponent, scope);
			}
		}
	}

	private static void addElementFor(IClassFile classFile,
			IApiComponent apiComponent, ApiScope scope) {
		try {
			IApiTypeRoot typeRoot = apiComponent.findTypeRoot(classFile.getType().getFullyQualifiedName());
			if (typeRoot != null) {
				scope.addElement(typeRoot);
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}

	private static void addElementFor(ICompilationUnit compilationUnit, IApiComponent component, ApiScope scope) throws JavaModelException {
		IType[] types = compilationUnit.getTypes();
		for (int i = 0, max = types.length; i < max; i++) {
			try {
				IApiTypeRoot typeRoot = component.findTypeRoot(types[i].getFullyQualifiedName());
				if (typeRoot != null) {
					scope.addElement(typeRoot);
				}
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
	}
}
