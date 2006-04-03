/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;

public class RequiredPluginsClasspathContainer extends PDEClasspathContainer implements IClasspathContainer {

	private IPluginModelBase fModel;
	
	private static boolean DEBUG = false;
	
	private IClasspathEntry[] fEntries = null;
	
	static {
		DEBUG  = PDECore.getDefault().isDebugging() 
					&& "true".equals(Platform.getDebugOption("org.eclipse.pde.core/classpath")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Constructor for RequiredPluginsClasspathContainer.
	 */
	public RequiredPluginsClasspathContainer(IPluginModelBase model) {
		fModel = model;
	}

	
	public void reset() {
		fEntries = null;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
	 */
	public int getKind() {
		return K_APPLICATION;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
	 */
	public IPath getPath() {
		return new Path(PDECore.CLASSPATH_CONTAINER_ID);
	}
	
	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return PDECoreMessages.RequiredPluginsClasspathContainer_description; 
	}
	
	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	public IClasspathEntry[] getClasspathEntries() {
		if (fModel == null) {
			if (DEBUG) {
				System.out.println("********Returned an empty container"); //$NON-NLS-1$
				System.out.println();
			}
			return new IClasspathEntry[0];
		}
		if (fEntries == null) {
			fEntries = computePluginEntries();
		}
		if (DEBUG) {
			System.out.println("Dependencies for plugin '" + fModel.getPluginBase().getId() + "':"); //$NON-NLS-1$ //$NON-NLS-2$
			for (int i = 0; i < fEntries.length; i++) {
				System.out.println(fEntries[i]);
			}
			System.out.println();
		}
		return fEntries;
	}

	private IClasspathEntry[] computePluginEntries() {
		ArrayList entries = new ArrayList();
		try {			
			BundleDescription desc = fModel.getBundleDescription();
			if (desc == null)
				return new IClasspathEntry[0];
			
			Map map = retrieveVisiblePackagesFromState(desc);
			
			HashSet added = new HashSet();
			
			// to avoid cycles, e.g. when a bundle imports a package it exports
			added.add(desc.getSymbolicName());
			
			HostSpecification host = desc.getHost();
			if (host != null) {
				if (desc.isResolved())
					addHostPlugin(host, added, map, entries);
			} else if ("true".equals(System.getProperty("pde.allowCycles"))) { //$NON-NLS-1$ //$NON-NLS-2$
				BundleDescription[] fragments = desc.getFragments();
				for (int i = 0; i < fragments.length; i++) {
					if (fragments[i].isResolved()) {
						addPlugin(fragments[i], false, map, entries);
					}
				}
			}

			// add dependencies
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				addDependency(getSupplier(required[i]), added, map, entries);
			}
			
			IBuild build = ClasspathUtilCore.getBuild(fModel);
			if (build != null)
				addSecondaryDependencies(desc, added, entries, build);
			
			// add Import-Package
			Iterator iter = map.keySet().iterator();
			while (iter.hasNext()) {
				String symbolicName = iter.next().toString();
				IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(symbolicName);
				if (model != null && model.isEnabled())
					addDependencyViaImportPackage(model.getBundleDescription(), added, map, entries);
			}

			if (build != null)
				addExtraClasspathEntries(added, entries, build);

		} catch (CoreException e) {
		}
		return (IClasspathEntry[])entries.toArray(new IClasspathEntry[entries.size()]);
	}
	
	private BundleDescription getSupplier(BundleSpecification spec) {
		if (spec.isResolved())
			return (BundleDescription)spec.getSupplier();
		
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(spec.getName());
		return model != null && model.isEnabled() ? model.getBundleDescription() : null;	
	}
	
	private Map retrieveVisiblePackagesFromState(BundleDescription bundle) {
		Map visiblePackages = new TreeMap();
		if (bundle.isResolved()) {
			BundleDescription desc = bundle;
			StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
			addVisiblePackagesFromState(helper, desc, visiblePackages);
			if (desc.getHost() != null)
				addVisiblePackagesFromState(helper, (BundleDescription)desc.getHost().getSupplier(), visiblePackages);
		}
		return visiblePackages;
	}

	private void addVisiblePackagesFromState(StateHelper helper, BundleDescription desc, Map visiblePackages) {
		ExportPackageDescription[] exports = helper.getVisiblePackages(desc);
		for (int i = 0; i < exports.length; i++) {
			BundleDescription exporter = exports[i].getExporter();
			if (exporter == null)
				continue;
			ArrayList list = (ArrayList)visiblePackages.get(exporter.getSymbolicName());
			if (list == null) 
				list = new ArrayList();
			Rule rule = getRule(helper, desc, exports[i]);
			if (!list.contains(rule))
				list.add(rule);
			visiblePackages.put(exporter.getSymbolicName(), list);
		}
	}
	
	private Rule getRule(StateHelper helper, BundleDescription desc, ExportPackageDescription export) {
		Rule rule = new Rule();
		rule.discouraged = helper.getAccessCode(desc, export) == StateHelper.ACCESS_DISCOURAGED;
		rule.path = new Path(export.getName().replaceAll("\\.", "/") + "/*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return rule;
	}
	
	protected void addDependencyViaImportPackage(BundleDescription desc, HashSet added, Map map, ArrayList entries) throws CoreException {
		if (desc == null || !added.add(desc.getSymbolicName()))
			return;

		addPlugin(desc, true, map, entries);

		if (hasExtensibleAPI(desc) && desc.getContainingState() != null) {
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (fragments[i].isResolved())
					addDependencyViaImportPackage(fragments[i], added, map, entries);
			}
		}
	}

	private void addDependency(BundleDescription desc, HashSet added, Map map, ArrayList entries) throws CoreException {
		addDependency(desc, added, map, entries, true);
	}
	
	private void addDependency(BundleDescription desc, HashSet added, Map map, ArrayList entries, boolean useInclusion) throws CoreException {
		if (desc == null || !added.add(desc.getSymbolicName()))
			return;

		BundleDescription[] fragments = hasExtensibleAPI(desc) ? desc.getFragments() : new BundleDescription[0];

		// add fragment patches before host
		for (int i = 0; i < fragments.length; i++) {
			if (fragments[i].isResolved() && ClasspathUtilCore.isPatchFragment(fragments[i])) {			
				addDependency(fragments[i], added, map, entries, useInclusion);	
			}
		}
		
		addPlugin(desc, useInclusion, map, entries);
		
		// add fragments that are not patches after the host
		for (int i = 0; i < fragments.length; i++) {
			if (fragments[i].isResolved() && !ClasspathUtilCore.isPatchFragment(fragments[i])) {
				addDependency(fragments[i], added, map, entries, useInclusion);
			}
		}			

		BundleSpecification[] required = desc.getRequiredBundles();
		for (int i = 0; i < required.length; i++) {
			if (required[i].isExported()) {
				addDependency(getSupplier(required[i]), added, map, entries, useInclusion);
			}
		}
	}	
	
	private boolean addPlugin(BundleDescription desc, boolean useInclusions, Map map, ArrayList entries)
			throws CoreException {		
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(desc);
		if (model == null || !model.isEnabled())
			return false;
		IResource resource = model.getUnderlyingResource();
		Rule[] rules = useInclusions ? getInclusions(map, model) : null;
		if (resource != null) {
			addProjectEntry(resource.getProject(), rules, entries);
		} else {
			addExternalPlugin(model, rules, entries);
		}
		return true;
	}
	
	private Rule[] getInclusions(Map map, IPluginModelBase model) {
		if ("false".equals(System.getProperty("pde.restriction")) //$NON-NLS-1$ //$NON-NLS-2$
				||!fModel.getBundleDescription().isResolved()
				|| !(fModel instanceof IBundlePluginModelBase))
			return null;
		
		if (TargetPlatform.getTargetVersion() < 3.1)
			return null;
		
		BundleDescription desc = model.getBundleDescription();
		if (desc == null || !desc.isResolved())
			return null;
		
		Rule[] rules;

		if (desc.getHost() != null)
			rules = getInclusions(map, (BundleDescription)desc.getHost().getSupplier());
		else
			rules = getInclusions(map, desc);
		
		return (rules.length == 0 && !ClasspathUtilCore.hasBundleStructure(model)) ? null : rules;
	}
	
	private Rule[] getInclusions(Map map, BundleDescription desc) {
		ArrayList list = (ArrayList)map.get(desc.getSymbolicName());
		return list != null ? (Rule[])list.toArray(new Rule[list.size()]) : new Rule[0];		
	}

	private void addHostPlugin(HostSpecification hostSpec, HashSet added, Map map, ArrayList entries) throws CoreException {
		BaseDescription desc = hostSpec.getSupplier();
		
		if (desc instanceof BundleDescription && added.add(desc.getName())) {
			BundleDescription host = (BundleDescription)desc;
			// add host plug-in
			if (addPlugin(host, false, map, entries)) {			
				BundleSpecification[] required = host.getRequiredBundles();
				for (int i = 0; i < required.length; i++) {
					desc = getSupplier(required[i]);
					if (desc != null && desc instanceof BundleDescription) {
						addDependency((BundleDescription)desc, added, map, entries);
					}
				}
				
				// add Import-Package
				ImportPackageSpecification[] imports = host.getImportPackages();
				for (int i = 0; i < imports.length; i++) {
					BaseDescription supplier = imports[i].getSupplier();
					if (supplier instanceof ExportPackageDescription) {
						addDependencyViaImportPackage(((ExportPackageDescription)supplier).getExporter(), added, map, entries);				
					}
				}
			}
		}
	}
	
	private boolean hasExtensibleAPI(BundleDescription desc) {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(desc);
		return model != null ? ClasspathUtilCore.hasExtensibleAPI(model) : false;
	}
	
	protected void addExtraClasspathEntries(HashSet added, ArrayList entries, IBuild build) throws CoreException {
		IBuildEntry entry = build.getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
		if (entry == null)
			return;

		String[] tokens = entry.getTokens();
		for (int i = 0; i < tokens.length; i++) {
			IPath path = Path.fromPortableString(tokens[i]);
			if (!path.isAbsolute()) {
				File file = new File(fModel.getInstallLocation(), path.toString());
				if (file.exists()) {
					IFile resource = PDECore.getWorkspace().getRoot().getFileForLocation(new Path(file.getAbsolutePath()));
					if (resource != null && resource.getProject().equals(fModel.getUnderlyingResource().getProject())) {
						addExtraLibrary(resource.getFullPath(), null, entries);
						continue;
					}
				}
				if (path.segmentCount() >= 3 && "..".equals(path.segment(0))) { //$NON-NLS-1$
					path = path.removeFirstSegments(1);
					path = Path.fromPortableString("platform:/plugin/").append(path); //$NON-NLS-1$ 
				} else {
					continue;
				}
			}
			
			if (!path.toPortableString().startsWith("platform:")) { //$NON-NLS-1$
				File file = new File(path.toOSString());
				if (file.exists()) {
					addExtraLibrary(path, null, entries);			
				}
			} else {
				int count = path.getDevice() == null ? 4 : 3;
				if (path.segmentCount() >= count) {
					String pluginID = path.segment(count-2);
					if (added.contains(pluginID))
						continue;
					IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginID);
					if (model != null && model.isEnabled()) {
						path = path.setDevice(null);
						path = path.removeFirstSegments(count-1);
						if (model.getUnderlyingResource() == null) {
							File file = new File(model.getInstallLocation(), path.toOSString());
							if (file.exists()) {
								addExtraLibrary(new Path(file.getAbsolutePath()), model, entries);
							}
						} else {
							IProject project = model.getUnderlyingResource().getProject();
							IFile file = project.getFile(path);
							if (file.exists()) {
								addExtraLibrary(file.getFullPath(), model, entries);
							}
						}
					}
				}
			}						
		}	
	}
	
	private void addSecondaryDependencies(BundleDescription desc, HashSet added, ArrayList entries, IBuild build) {
		try {
		  IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
		  if (entry != null) {
			  String[] tokens = entry.getTokens();
			  for (int i = 0; i < tokens.length; i++) {
				  String pluginId = tokens[i];
				  if (added.contains(pluginId))
					  continue;
				  // Get PluginModelBase first to resolve system.bundle entry if it exists
				  IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginId);
				  if (model != null) {
					  TreeMap rules = new TreeMap();
					  findExportedPackages(model.getBundleDescription(), desc, rules);
					  if (model != null) {
						  addDependency(model.getBundleDescription(), added, rules, entries, true);
					  }
				  }
			  }
		  }
		} catch (CoreException e) {
			return;
		}
	}

	protected final void findExportedPackages(BundleDescription desc, BundleDescription projectDesc, Map map) {
		if (desc != null) {
			Stack stack = new Stack();
			stack.add(desc);
			while (!stack.isEmpty()) {
				BundleDescription bdesc = (BundleDescription) stack.pop();
				ExportPackageDescription[] expkgs = bdesc.getExportPackages();
				ArrayList rules = new ArrayList();
				for (int i = 0; i < expkgs.length; i++) {
					Rule rule = new Rule();
					rule.discouraged = restrictPackage(projectDesc, expkgs[i]);
					rule.path = new Path(expkgs[i].getName().replaceAll("\\.", "/") + "/*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					rules.add(rule);
				}
				map.put(bdesc.getSymbolicName(), rules);
			
				// Look at re-exported Require-Bundles for any other exported packages
				BundleSpecification[] requiredBundles = bdesc.getRequiredBundles();
				for (int i = 0; i < requiredBundles.length; i++) 
					if (requiredBundles[i].isExported()) {
						BaseDescription bd = requiredBundles[i].getSupplier();
						if (bd != null && bd instanceof BundleDescription)
							stack.add(bd);
					}
			}
		}
	}
	
	private boolean restrictPackage(BundleDescription desc, ExportPackageDescription pkg ) {
		String[] friends = (String[])pkg.getDirective(ICoreConstants.FRIENDS_DIRECTIVE);
		if (friends != null) {
			String symbolicName = desc.getSymbolicName();
			for (int i = 0; i < friends.length; i++) {
				if (symbolicName.equals(friends[i]))
					return false;
				
			}
			return true;
		} 
		return (((Boolean)pkg.getDirective(ICoreConstants.INTERNAL_DIRECTIVE)).booleanValue());
	}
	
	private void addExtraLibrary(IPath path, IPluginModelBase model, ArrayList entries) throws CoreException {
		IPath srcPath = null;
		if (model != null) {
			IPath shortPath = path.removeFirstSegments(path.matchingFirstSegments(new Path(model.getInstallLocation())));
			srcPath = ClasspathUtilCore.getSourceAnnotation(model, shortPath.toString());
		} else {
			String filename = ClasspathUtilCore.getSourceZipName(path.lastSegment());
			IPath candidate = path.removeLastSegments(1).append(filename);
			if (PDECore.getWorkspace().getRoot().getFile(candidate).exists()) 
				srcPath = candidate;
		}
		IClasspathEntry clsEntry = JavaCore.newLibraryEntry(
				path,
				srcPath,
				null);
		if (!entries.contains(clsEntry))
			entries.add(clsEntry);						
	}
}
