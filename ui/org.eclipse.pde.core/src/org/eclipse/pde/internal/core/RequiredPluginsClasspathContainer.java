/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;

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
		return PDECoreMessages.RequiredPluginsClasspathContainer_description; //$NON-NLS-1$
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

			HostSpecification host = desc.getHost();
			if (desc.isResolved() && host != null) {
				addHostPlugin(host, added, map, entries);
			}

			// add dependencies
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				addDependency(getSupplier(required[i]), added, map, entries);
			}
			
			// add Import-Package
			Iterator iter = map.keySet().iterator();
			while (iter.hasNext()) {
				String symbolicName = iter.next().toString();
				IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(symbolicName);
				if (model != null && model.isEnabled())
					addDependencyViaImportPackage(model.getBundleDescription(), added, map, entries);
			}

			addExtraClasspathEntries(added, entries);

			// add implicit dependencies
			addImplicitDependencies(added, map, entries);
			
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
			if (desc.getHost() != null)
				desc = (BundleDescription)desc.getHost().getSupplier();
			
			StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
			ExportPackageDescription[] exports = helper.getVisiblePackages(desc);
			for (int i = 0; i < exports.length; i++) {
				BundleDescription exporter = exports[i].getExporter();
				if (exporter == null)
					continue;
				ArrayList list = (ArrayList)visiblePackages.get(exporter.getName());
				if (list == null) 
					list = new ArrayList();
				list.add(getRule(helper, desc, exports[i]));
				visiblePackages.put(exporter.getName(), list);
			}
		}
		return visiblePackages;
	}
	
	private Rule getRule(StateHelper helper, BundleDescription desc, ExportPackageDescription export) {
		Rule rule = new Rule();
		rule.discouraged = helper.getAccessCode(desc, export) == StateHelper.ACCESS_DISCOURAGED;
		rule.path = new Path(export.getName().replaceAll("\\.", "/") + "/*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return rule;
	}
	
	private void addDependencyViaImportPackage(BundleDescription desc, HashSet added, Map map, ArrayList entries) throws CoreException {
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
		if (desc == null || !added.add(desc.getSymbolicName()))
			return;

		addPlugin(desc, true, map, entries);

		if (hasExtensibleAPI(desc) && desc.getContainingState() != null) {
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (fragments[i].isResolved())
					addDependency(fragments[i], added, map, entries);
			}
		}

		BundleSpecification[] required = desc.getRequiredBundles();
		for (int i = 0; i < required.length; i++) {
			if (required[i].isExported()) {
				addDependency(getSupplier(required[i]), added, map, entries);
			}
		}
	}
	
	private void addPlugin(BundleDescription desc, boolean useInclusions, Map map, ArrayList entries)
			throws CoreException {		
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(desc);
		if (model == null || !model.isEnabled())
			return;
		IResource resource = model.getUnderlyingResource();
		Rule[] rules = useInclusions ? getInclusions(map, model) : null;
		if (resource != null) {
			addProjectEntry(resource.getProject(), rules, entries);
		} else {
			addExternalPlugin(model, rules, entries);
		}
	}
	
	private Rule[] getInclusions(Map map, IPluginModelBase model) {
		if ("false".equals(System.getProperty("pde.restriction")) //$NON-NLS-1$ //$NON-NLS-2$
				||!fModel.getBundleDescription().isResolved())
			return null;
		
		BundleDescription desc = model.getBundleDescription();
		if (desc == null)
			return null;
		
		Rule[] rules;
		if (!desc.isResolved())
			return null;
		
		if (desc.getHost() != null)
			rules = getInclusions(map, (BundleDescription)desc.getHost().getSupplier());
		else
			rules = getInclusions(map, desc);
		
		return (rules.length == 0 && !ClasspathUtilCore.isBundle(model)) ? null : rules;
	}
	
	private Rule[] getInclusions(Map map, BundleDescription desc) {
		ArrayList list = (ArrayList)map.get(desc.getSymbolicName());
		return list != null ? (Rule[])list.toArray(new Rule[list.size()]) : new Rule[0];		
	}

	private void addImplicitDependencies(HashSet added, Map map, ArrayList entries) throws CoreException {
		String id = fModel.getPluginBase().getId();
		String schemaVersion = fModel.getPluginBase().getSchemaVersion();
		boolean isOSGi = TargetPlatform.isOSGi();
		
		if ((isOSGi && schemaVersion != null)
				|| id.equals("org.eclipse.core.boot") //$NON-NLS-1$
				|| id.equals("org.apache.xerces") //$NON-NLS-1$
				|| id.startsWith("org.eclipse.swt")) //$NON-NLS-1$
			return;

		PluginModelManager manager = PDECore.getDefault().getModelManager();

		if (schemaVersion == null && isOSGi) {
			if (!id.equals("org.eclipse.core.runtime")) { //$NON-NLS-1$
				IPluginModelBase plugin = manager.findModel(
						"org.eclipse.core.runtime.compatibility"); //$NON-NLS-1$
				if (plugin != null && plugin.isEnabled())
					addDependency(plugin.getBundleDescription(), added, map, entries);
			}
		} else {
			IPluginModelBase plugin = manager.findModel("org.eclipse.core.boot"); //$NON-NLS-1$
			if (plugin != null && plugin.isEnabled())
				addDependency(plugin.getBundleDescription(), added, map, entries);
			
			if (!id.equals("org.eclipse.core.runtime")) { //$NON-NLS-1$
				plugin = manager.findModel("org.eclipse.core.runtime"); //$NON-NLS-1$
				if (plugin != null && plugin.isEnabled())
					addDependency(plugin.getBundleDescription(), added, map, entries);
			}
		}
	}

	private void addHostPlugin(HostSpecification hostSpec, HashSet added, Map map, ArrayList entries) throws CoreException {
		BaseDescription desc = hostSpec.getSupplier();
		
		if (desc instanceof BundleDescription && added.add(desc.getName())) {
			BundleDescription host = (BundleDescription)desc;
			// add host plug-in
			addPlugin(host, false, map, entries);
			
			BundleSpecification[] required = host.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				desc = getSupplier(required[i]);
				if (desc != null && desc instanceof BundleDescription) {
					addDependency((BundleDescription)desc, added, map, entries);
				}
			}
		}
	}
	
	private boolean hasExtensibleAPI(BundleDescription desc) {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(desc);
		return (model instanceof IPluginModel) 
					? ClasspathUtilCore.hasExtensibleAPI(((IPluginModel)model).getPlugin()) 
					: false;
	}
	
	protected void addExtraClasspathEntries(HashSet added, ArrayList entries) throws CoreException {
		IBuild build = ClasspathUtilCore.getBuild(fModel);
		IBuildEntry entry = (build == null) ? null : build.getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
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
					path = Path.fromPortableString("platform:/plugin/").append(path); //$NON-NLS-1$ //$NON-NLS-2$
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
	
	private void addExtraLibrary(IPath path, IPluginModelBase model, ArrayList entries) throws CoreException {
		IPath srcPath = null;
		if (model != null) {
			IPath shortPath = path.removeFirstSegments(path.matchingFirstSegments(new Path(model.getInstallLocation())));
			srcPath = ClasspathUtilCore.getSourceAnnotation(model, shortPath.toString());
		}
		IClasspathEntry clsEntry = JavaCore.newLibraryEntry(
				path,
				srcPath,
				null);
		if (!entries.contains(clsEntry))
			entries.add(clsEntry);						
	}
}
