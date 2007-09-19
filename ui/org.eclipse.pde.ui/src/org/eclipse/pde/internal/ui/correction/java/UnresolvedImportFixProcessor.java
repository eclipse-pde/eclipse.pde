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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.converter.PluginConverter;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import com.ibm.icu.text.MessageFormat;

/**
 * Offers a classpath fix proposal if the broken import statement can be
 * fixed by adding a plugin dependency (required bundle or package import).
 * @since 3.4
 */
public class UnresolvedImportFixProcessor extends ClasspathFixProcessor {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor#getFixImportProposals(org.eclipse.jdt.core.IJavaProject, java.lang.String)
	 */
	public ClasspathFixProposal[] getFixImportProposals(IJavaProject project, String name) throws CoreException {
		int idx= name.lastIndexOf('.');
		String packageName= idx != -1 ? name.substring(0, idx) : null;
		String typeName= name.substring(idx + 1);
		if (typeName.length() == 1 && typeName.charAt(0) == '*') {
			typeName= null;
		}
		
		// if package is already referenced by Import-Package, exit since Import-Package overrides Require-Bundle during lookup
		if (packageName != null && !isImportedPackage(project, packageName)){
			// Get the packages exported by all bundles, see if any can provide the required classes
			Set validPackages = getValidPackages(project, packageName);			
			List proposals = new ArrayList();
			Iterator validPackagesIter = validPackages.iterator();
			Set visiblePkgs = null;
			while (validPackagesIter.hasNext()) {
				// since getting visible packages is not very efficient, only do it once and cache result
				if (visiblePkgs == null) {
					visiblePkgs = getVisiblePacakges(project);
				}
				ExportPackageDescription currentPackage = (ExportPackageDescription) validPackagesIter.next();
				// if package is already visible, skip over
				if (visiblePkgs.contains(currentPackage)) {
						continue;
				}
				addRequireBundleProposal(proposals, project.getProject(),currentPackage);
			}
				
			if (!proposals.isEmpty() && PluginRegistry.findModel(project.getProject()) instanceof IBundlePluginModelBase) {
				ExportPackageDescription pkgDesc = ((UnresolvedImportFixProposal)proposals.get(0)).getDependency();
				addImportPackageProposal(proposals, project.getProject(), pkgDesc);
			}

			return (ClasspathFixProposal[])proposals.toArray(new ClasspathFixProposal[proposals.size()]);
		}

		return new ClasspathFixProposal[0];
	}
	
	/**
	 * Helper method to create a proposal to add an require bundle dependency to the project
	 */
	private void addRequireBundleProposal(List proposalList, IProject project, ExportPackageDescription dependency){
		proposalList.add(new UnresolvedImportFixProposal(project, dependency) {

			/* (non-Javadoc)
			 * @see org.eclipse.pde.internal.ui.correction.java.UnresolvedImportFixProposal#handleDependencyChange(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase, org.eclipse.osgi.service.resolver.ExportPackageDescription, boolean)
			 */
			public void handleDependencyChange(IProgressMonitor pm,	IPluginModelBase model, ExportPackageDescription dependency, boolean isAdd) throws CoreException {
				IPluginImport pluginImport = model.getPluginFactory().createImport();
				pluginImport.setId(dependency.getExporter().getSymbolicName());
				if (isAdd) {
					model.getPluginBase().add(pluginImport);
				} else {
					model.getPluginBase().remove(pluginImport);
				}
			}

			/* (non-Javadoc)
			 * @see org.eclipse.pde.internal.ui.correction.java.UnresolvedImportFixProposal#getLabel(boolean)
			 */
			public String getLabel(boolean isAdd) {
				if (isAdd) {
					return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_0,new Object[]{getDependency().getExporter().getName()});
				}
				return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_1,new Object[]{getDependency().getExporter().getName()});
			}

			/* (non-Javadoc)
			 * @see org.eclipse.pde.internal.ui.correction.java.UnresolvedImportFixProposal#getDescription()
			 */
			public String getDescription() {
				return PDEUIMessages.UnresolvedImportFixProcessor_2;
			}
		});
	}
	
	/**
	 * Helper method to create a proposal to add an import package dependency to the project
	 */
	private void addImportPackageProposal(List proposalList, IProject project, ExportPackageDescription dependency){

		proposalList.add(new UnresolvedImportFixProposal(project, dependency) {

			/* (non-Javadoc)
			 * @see org.eclipse.pde.internal.ui.correction.java.UnresolvedImportFixProposal#handleDependencyChange(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase, org.eclipse.osgi.service.resolver.ExportPackageDescription, boolean)
			 */
			public void handleDependencyChange(IProgressMonitor pm,	IPluginModelBase model, ExportPackageDescription dependency, boolean isAdd) throws CoreException {
				IBundle bundle = ((IBundlePluginModelBase)model).getBundleModel().getBundle();
				if (!(bundle instanceof Bundle)){
					return;
				}
				IManifestHeader mheader = bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
				if (mheader == null) {
					bundle.setHeader(Constants.IMPORT_PACKAGE, new String());
					mheader = bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
				}
				if (mheader instanceof ImportPackageHeader) {
					ImportPackageHeader header = (ImportPackageHeader) mheader;
					if (isAdd) {
						String versionAttr = (BundlePluginBase.getBundleManifestVersion(bundle) < 2) ? ICoreConstants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;  
						ImportPackageObject obj = new ImportPackageObject(header, dependency, versionAttr);
						header.addPackage(obj);
					} else {
						header.removePackage(dependency.getName());
					}
				} else {
					StringBuffer buffer = new StringBuffer();
					String currentValue = (mheader != null) ? mheader.getValue() : null;
					if (currentValue != null){
						buffer.append(currentValue).append(PluginConverter.LIST_SEPARATOR);
					}
					if (dependency.getVersion().equals(Version.emptyVersion)){
						buffer.append(dependency.getName());
					} else {
						buffer.append(dependency.getName());
						buffer.append("; version=\""); //$NON-NLS-1$
						buffer.append(dependency.getVersion());
						buffer.append("\""); //$NON-NLS-1$
					}
					bundle.setHeader(Constants.IMPORT_PACKAGE, buffer.toString());
				}
			}

			/* (non-Javadoc)
			 * @see org.eclipse.pde.internal.ui.correction.java.UnresolvedImportFixProposal#getLabel(boolean)
			 */
			public String getLabel(boolean isAdd) {
				if (isAdd) {
					return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_3,new Object[]{getDependency().getName()});
				}
				return MessageFormat.format(PDEUIMessages.UnresolvedImportFixProcessor_4,new Object[]{getDependency().getName()});
			}

			/* (non-Javadoc)
			 * @see org.eclipse.pde.internal.ui.correction.java.UnresolvedImportFixProposal#getDescription()
			 */
			public String getDescription() {
				return PDEUIMessages.UnresolvedImportFixProcessor_5;
			}

		});
	}
	
	private Set getVisiblePacakges(IJavaProject project) {
		IPluginModelBase base = PluginRegistry.findModel(project.getProject());
		BundleDescription desc = base.getBundleDescription();
		
		StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
		ExportPackageDescription[] visiblePkgs = helper.getVisiblePackages(desc);
		
		HashSet set = new HashSet();
		for (int i =0; i < visiblePkgs.length; i++) {
			set.add(visiblePkgs[i]);
		}
		return set;
	}
	
	private boolean isImportedPackage(IJavaProject project, String pkgName) {
		BundleDescription desc = PluginRegistry.findModel(project.getProject()).getBundleDescription();
		ImportPackageSpecification[] importPkgs = desc.getImportPackages();
		for (int i = 0; i < importPkgs.length; i++) {
			if (importPkgs[i].getName().equals(pkgName)) {
				return true;
			}
		}
		return false;
	}
	
	private Set getValidPackages(IJavaProject project, String pkgName) {
		ExportPackageDescription[] knownPackages = PDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
		Set validPackages = new HashSet();
		for (int i = 0; i < knownPackages.length; i++) {
			if (knownPackages[i].getName().equals(pkgName)){
				validPackages.add(knownPackages[i]);
			}
		}
		// remove system packages if they happen to be included. Adding a system package won't resolve anything, since package package already comes from JRE
		if (!validPackages.isEmpty()) {
			knownPackages = PDECore.getDefault().getModelManager().getState().getState().getSystemPackages();
			for (int i = 0; i < knownPackages.length; i++) {
				validPackages.remove(knownPackages[i]);
			}
		}
		return validPackages;
	}

}
