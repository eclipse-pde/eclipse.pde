/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashMap;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ManifestPackageRenameParticipant extends PDERenameParticipant {

	protected boolean initialize(Object element) {
		try {
			if (element instanceof IPackageFragment) {
				IPackageFragment fragment = (IPackageFragment) element;
				if (!fragment.containsJavaResources())
					return false;
				IJavaProject javaProject = (IJavaProject) fragment.getAncestor(IJavaElement.JAVA_PROJECT);
				IProject project = javaProject.getProject();
				if (WorkspaceModelManager.isPluginProject(project)) {
					fProject = javaProject.getProject();
					fElements = new HashMap();
					fElements.put(fragment, getArguments().getNewName());
					return true;
				}
			}
		} catch (JavaModelException e) {
		}
		return false;
	}

	public String getName() {
		return PDEUIMessages.ManifestPackageRenameParticipant_packageRename;
	}

	protected void addBundleManifestChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		super.addBundleManifestChange(result, pm);
		IPluginModelBase model = PluginRegistry.findModel(fProject);
		if (model != null) {
			BundleDescription desc = model.getBundleDescription();
			if (desc != null) {
				BundleDescription[] dependents = desc.getDependents();
				for (int i = 0; i < dependents.length; i++) {
					if (isAffected(desc, dependents[i])) {
						IPluginModelBase candidate = PluginRegistry.findModel(dependents[i]);
						if (candidate instanceof IBundlePluginModelBase) {
							IFile file = (IFile) candidate.getUnderlyingResource();
							addBundleManifestChange(file, result, pm);
						}
					}
				}
			}
		}
	}

	private boolean isAffected(BundleDescription desc, BundleDescription dependent) {
		ImportPackageSpecification[] imports = dependent.getImportPackages();
		Iterator iter = fElements.keySet().iterator();
		while (iter.hasNext()) {
			String name = ((IJavaElement) iter.next()).getElementName();
			for (int i = 0; i < imports.length; i++) {
				if (name.equals(imports[i].getName())) {
					BaseDescription supplier = imports[i].getSupplier();
					if (supplier instanceof ExportPackageDescription) {
						if (desc.equals(((ExportPackageDescription) supplier).getExporter()))
							return true;
					}
				}
			}
		}
		return false;
	}

}
