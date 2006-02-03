/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Constants;


public class UnusedDependenciesJob extends Job {
	
	class Requestor extends SearchRequestor {
		boolean found = false;
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			found = true;
		}
		public boolean foundMatches() {
			return found;
		}
	}

	private IPluginModelBase fModel;
	private boolean fReadOnly;

	public UnusedDependenciesJob(String name, IPluginModelBase model, boolean readOnly) {
		super(name);
		fModel = model;
		fReadOnly = readOnly;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.internal.jobs.InternalJob#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		ImportPackageObject[] packages = null;
		if (ClasspathUtilCore.hasBundleStructure(fModel)) {
			IBundle bundle = ((IBundlePluginModelBase)fModel).getBundleModel().getBundle();
			ImportPackageHeader header = (ImportPackageHeader)((Bundle)bundle).getManifestHeader(Constants.IMPORT_PACKAGE);
			if (header != null)
				packages = header.getPackages();
		}
		IPluginImport[] imports = fModel.getPluginBase().getImports();
		try {
			int totalWork = (packages != null) ? (packages.length + imports.length*3) : imports.length*3;
			monitor.beginTask("", totalWork); //$NON-NLS-1$
			ArrayList list = new ArrayList();
			for (int i = 0; i < imports.length; i++) {
				if (monitor.isCanceled())
					break;
				if (isUnused(imports[i], new SubProgressMonitor(monitor, 3))) {
					list.add(imports[i]);
				}
				updateMonitor(monitor, list.size());
			}
			
			if (packages != null && !monitor.isCanceled()) {
				for (int i = 0; i < packages.length; i++) {
					if (isUnused(packages[i], new SubProgressMonitor(monitor, 1))) {
						list.add(packages[i]);
						updateMonitor(monitor, list.size());
					}
				}
			}
			// List can contain IPluginImports or ImportPackageObjects
			showResults(list.toArray());
		} finally {
			monitor.done();
		}
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, PDEUIMessages.UnusedDependenciesJob_viewResults, null); 
	}
	
	private void updateMonitor(IProgressMonitor monitor, int size) {
		monitor.setTaskName(
				PDEUIMessages.UnusedDependencies_analyze
					+ size
					+ " " //$NON-NLS-1$
					+ PDEUIMessages.UnusedDependencies_unused
					+ " " //$NON-NLS-1$
					+ (size == 1
						? PDEUIMessages.DependencyExtent_singular
						: PDEUIMessages.DependencyExtent_plural) 
					+ " " //$NON-NLS-1$
					+ PDEUIMessages.DependencyExtent_found); 
	}
	
	private boolean isUnused(IPluginImport plugin, SubProgressMonitor monitor) {
		IPlugin[] plugins = PluginJavaSearchUtil.getPluginImports(plugin);
		return !provideJavaClasses(plugins, monitor);
	}
	
	private boolean isUnused(ImportPackageObject pkg, SubProgressMonitor monitor) {
		return !provideJavaClasses(pkg, monitor);
	}
	
	private boolean provideJavaClasses(IPlugin[] plugins, IProgressMonitor monitor) {
		try {
			IProject project = fModel.getUnderlyingResource().getProject();
			if (!project.hasNature(JavaCore.NATURE_ID))
				return false;
			
			IJavaProject jProject = JavaCore.create(project);
			IPackageFragment[] packageFragments = PluginJavaSearchUtil.collectPackageFragments(plugins, jProject, true);
			SearchEngine engine = new SearchEngine();
			IJavaSearchScope searchScope = PluginJavaSearchUtil.createSeachScope(jProject);
			monitor.beginTask("", packageFragments.length*2); //$NON-NLS-1$
			for (int i = 0; i < packageFragments.length; i++) {
				IPackageFragment pkgFragment = packageFragments[i];
				if (pkgFragment.hasChildren()) {
					Requestor requestor = new Requestor();
					engine.search(
							SearchPattern.createPattern(pkgFragment, IJavaSearchConstants.REFERENCES),
							new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
							searchScope,
							requestor, 
							new SubProgressMonitor(monitor, 1));
					if (requestor.foundMatches()) {
						if (provideJavaClasses(packageFragments[i], engine,
								searchScope, new SubProgressMonitor(monitor, 1))) {
							return true;
						}
					} else
						monitor.worked(1);
				} else {
					monitor.worked(2);
				}
			}	
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
		return false;
	}

	private boolean provideJavaClasses(IPackageFragment packageFragment,
			SearchEngine engine, IJavaSearchScope searchScope,
			IProgressMonitor monitor) throws JavaModelException, CoreException {
		Requestor requestor;
		IJavaElement[] children = packageFragment.getChildren();
		monitor.beginTask("", children.length); //$NON-NLS-1$

		try {
			for (int j = 0; j < children.length; j++) {
				IType[] types = null;
				if (children[j] instanceof ICompilationUnit) {
					types = ((ICompilationUnit) children[j]).getAllTypes();
				} else if (children[j] instanceof IClassFile) {
					types = new IType[] { ((IClassFile) children[j]).getType() };
				}
				if (types != null) {
					for (int t = 0; t < types.length; t++) {
						requestor = new Requestor();
						engine.search(SearchPattern.createPattern(types[t],
								IJavaSearchConstants.REFERENCES),
								new SearchParticipant[] { SearchEngine
										.getDefaultSearchParticipant() },
								searchScope, requestor, new SubProgressMonitor(
										monitor, 1));
						if (requestor.foundMatches()) {
							return true;
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
		return false;
	}
	
	private boolean provideJavaClasses(ImportPackageObject pkg, IProgressMonitor monitor) {
		try {
			IProject project = fModel.getUnderlyingResource().getProject();
			
			if (!project.hasNature(JavaCore.NATURE_ID))
				return false;
			
			monitor.beginTask("", 1); //$NON-NLS-1$
			IJavaProject jProject = JavaCore.create(project);
			SearchEngine engine = new SearchEngine();
			IJavaSearchScope searchScope = PluginJavaSearchUtil.createSeachScope(jProject);
			Requestor requestor = new Requestor();
			String packageName = pkg.getName();
			
			engine.search(
					SearchPattern.createPattern(packageName, IJavaSearchConstants.PACKAGE, 
							IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH),
							new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
							searchScope, 
							requestor, 
							new SubProgressMonitor(monitor, 1));
			
			if (requestor.foundMatches()) 
				return true;
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
		return false;
	}
	
	private Action getShowResultsAction(Object[] unused) {
		return new ShowResultsAction(fModel, unused, fReadOnly);
	}
	
    protected void showResults(final Object[] unused) {
        Display.getDefault().asyncExec(new Runnable() {
           public void run() {
              getShowResultsAction(unused).run();
           }
        });
     }
}
