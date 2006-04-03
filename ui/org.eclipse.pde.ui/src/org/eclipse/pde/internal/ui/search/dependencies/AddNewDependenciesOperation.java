/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.plugin.PluginImport;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class AddNewDependenciesOperation extends WorkspaceModifyOperation {

	protected IProject fProject;
	protected IBundlePluginModelBase fBase;
	private boolean fNewDependencies = false;
	
	protected static class ReferenceFinder extends SearchRequestor {
		private boolean found = false;
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			found = true;
		}
		public boolean foundMatches() {
			return found;
		}
	}
	
	public AddNewDependenciesOperation(IProject project, IBundlePluginModelBase base) {
		fProject = project;
		fBase = base;
	}
	
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		monitor.beginTask(PDEUIMessages.AddNewDependenciesOperation_mainTask, 100);
		final IBundle bundle = fBase.getBundleModel().getBundle();
		final Set ignorePkgs = new HashSet();
		final String[] secDeps = findSecondaryBundles(bundle, ignorePkgs);
		if (secDeps == null || secDeps.length == 0) {
			monitor.done();
			return;
		}
		monitor.worked(4); 
		findImportPackages(bundle, ignorePkgs);
		monitor.worked(2);
		addProjectPackages(bundle, ignorePkgs);
		monitor.worked(4);
		
		final Map additionalDeps = new HashMap();
		monitor.subTask(PDEUIMessages.AddNewDependenciesOperation_searchProject);
		
		boolean useRequireBundle = new ProjectScope(fProject).getNode(PDECore.PLUGIN_ID).getBoolean(ICoreConstants.RESOLVE_WITH_REQUIRE_BUNDLE, true);
		findSecondaryDependencies(secDeps, ignorePkgs, additionalDeps, bundle, useRequireBundle, new SubProgressMonitor(monitor, 80));
		handleNewDependencies(additionalDeps, 
				useRequireBundle, new SubProgressMonitor(monitor, 10));
		monitor.done();
	}
	
	public boolean foundNewDependencies() {
		return fNewDependencies;
	}
	
	protected final String[] findSecondaryBundles(IBundle bundle, Set ignorePkgs) {
		String[] secDeps = getSecondaryDependencies();
		if (secDeps == null)
			return null;
		Set manifestPlugins = findManifestPlugins(bundle, ignorePkgs);
		
		List result = new LinkedList();
		for (int i = 0; i < secDeps.length; i++) 
			if (!manifestPlugins.contains(secDeps[i]))
				result.add(secDeps[i]);
	
		return (String[])result.toArray(new String[result.size()]);
	}

	private String[] getSecondaryDependencies() {
		IBuild build = getBuild();
		if (build != null) {
			IBuildEntry be = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			if (be != null) 
				return be.getTokens();
		}
		return null;
	}
	
	protected final IBuild getBuild() {
		IFile buildProps = fProject.getFile("build.properties"); //$NON-NLS-1$
		if (buildProps != null) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildProps);
			if (model != null) 
				return model.getBuild();
		}
		return null;
	}

	private Set findManifestPlugins(IBundle bundle, Set ignorePkgs) {
		IManifestHeader header = bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header == null)
			return new HashSet(0);
		Set plugins = (header instanceof RequireBundleHeader) ? findManifestPlugins((RequireBundleHeader)header, ignorePkgs):
			findManifestPlugins(ignorePkgs);
		if (plugins.contains("org.eclipse.core.runtime")) //$NON-NLS-1$
			plugins.add("system.bundle"); //$NON-NLS-1$
		return plugins;
	}
	
	private Set findManifestPlugins(RequireBundleHeader header, Set ignorePkgs) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		RequireBundleObject[] bundles = header.getRequiredBundles();
		Set result = new HashSet((4/3) * (bundles.length) + 2);
		ArrayList plugins = new ArrayList();
		for (int i = 0; i < bundles.length; i++) { 
			String id = bundles[i].getId();
			result.add(id);
			IPluginModelBase base = manager.findModel(id);
			if (base != null) {
				ExportPackageDescription[] exportedPkgs = findExportedPackages(base.getBundleDescription());
				for (int j = 0; j < exportedPkgs.length; j++)
					ignorePkgs.add(exportedPkgs[j].getName());
				plugins.add(base.getPluginBase());
			}
		}
		return result;
	}
	
	private Set findManifestPlugins(Set ignorePkgs) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		BundleSpecification[] bundles = fBase.getBundleDescription().getRequiredBundles();
		Set result = new HashSet((4/3) * (bundles.length) + 2);
		ArrayList plugins = new ArrayList();
		for (int i = 0; i < bundles.length; i++) {
			String id = bundles[i].getName();
			result.add(id);
			IPluginModelBase base = manager.findModel(id);
			if (base != null) {
				ExportPackageDescription[] exportedPkgs = findExportedPackages(base.getBundleDescription());
				for (int j = 0; j < exportedPkgs.length; j++)
					ignorePkgs.add(exportedPkgs[j].getName());
				plugins.add(base.getPluginBase());
			}
		}
		return result;
	}

	protected final ExportPackageDescription[] findExportedPackages(BundleDescription desc) {
		if (desc != null) {
			IBundle bundle = fBase.getBundleModel().getBundle();
			String value = bundle.getHeader(Constants.BUNDLE_SYMBOLICNAME);
			int index = (value != null) ? value.indexOf(';') : -1;
			String projectBundleId = (index > 0) ? value.substring(0, index) : value;
			List result = new LinkedList();
			Stack stack = new Stack();
			stack.add(desc);
			while (!stack.isEmpty()) {
				BundleDescription bdesc = (BundleDescription) stack.pop();
				ExportPackageDescription[] expkgs = bdesc.getExportPackages();
				for (int i = 0; i < expkgs.length; i++)
					if (addPackage(projectBundleId, expkgs[i]))
							result.add(expkgs[i]);
			
				// Look at re-exported Require-Bundles for any other exported packages
				BundleSpecification[] requiredBundles = bdesc.getRequiredBundles();
				for (int i = 0; i < requiredBundles.length; i++) 
					if (requiredBundles[i].isExported()) {
						BaseDescription bd = requiredBundles[i].getSupplier();
						if (bd != null && bd instanceof BundleDescription)
							stack.add(bd);
					}
			}
			return (ExportPackageDescription[]) result.toArray(new ExportPackageDescription[result.size()]);
		}
		return new ExportPackageDescription[0];
	}
	
	private boolean addPackage(String symbolicName, ExportPackageDescription pkg) {
		if (symbolicName == null)
			return true;
		String[] friends = (String[])pkg.getDirective(ICoreConstants.FRIENDS_DIRECTIVE);
		if (friends != null) {
			for (int i = 0; i < friends.length; i++) {
				if (symbolicName.equals(friends[i]))
					return true;
			}
			return false;
		} 
		return !(((Boolean)pkg.getDirective(ICoreConstants.INTERNAL_DIRECTIVE)).booleanValue());
	}

	protected final void findImportPackages(IBundle bundle, Set ignorePkgs) {
		IManifestHeader header = bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		if (header == null) 
			return;
		if (header instanceof ImportPackageHeader) {
			ImportPackageObject[] pkgs = ((ImportPackageHeader)header).getPackages();
			for (int i = 0; i < pkgs.length; i++) 
				ignorePkgs.add(pkgs[i].getName());
		} else {
			ImportPackageSpecification[] pkgs = fBase.getBundleDescription().getImportPackages();
			for (int i = 0; i < pkgs.length; i++)
				ignorePkgs.add(pkgs[i].getName());
		}
	}

	protected void findSecondaryDependencies(String[] secDeps, Set ignorePkgs, Map newDeps, IBundle bundle, boolean useRequireBundle, 
			IProgressMonitor monitor) {
		IJavaProject jProject = JavaCore.create(fProject);
		SearchEngine engine = new SearchEngine();
		if (ignorePkgs == null) 
			ignorePkgs = new HashSet(2);
		monitor.beginTask(PDEUIMessages.AddNewDependenciesOperation_searchProject, secDeps.length);
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		List dynamicImports = getDynamicImports(bundle);
		for (int j = 0; j < secDeps.length; j++) {
			try {
				if (monitor.isCanceled())
					return;
				IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
				String pluginId = secDeps[j];
				IPluginModelBase base = manager.findModel(secDeps[j]);
				if (base != null) {
					ExportPackageDescription[] exported = findExportedPackages(base.getBundleDescription());
					IJavaSearchScope searchScope = PluginJavaSearchUtil.createSeachScope(jProject);
					subMonitor.beginTask(NLS.bind(PDEUIMessages.AddNewDependenciesOperation_searchForDependency, pluginId), exported.length);
					for (int i = 0; i < exported.length; i++) {
						String pkgName = exported[i].getName();
						if (!ignorePkgs.contains(pkgName)) {
							ReferenceFinder requestor = new ReferenceFinder();
							engine.search(
									SearchPattern.createPattern(pkgName, IJavaSearchConstants.PACKAGE, IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH),
									new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
									searchScope,
									requestor, 
									null);
							if (requestor.foundMatches() && !isDynamicallyImported(pkgName, dynamicImports)) {
								fNewDependencies = true;
								ignorePkgs.add(pkgName);
								newDeps.put(pkgName, pluginId);
								if (useRequireBundle) {
									// since using require-bundle, rest of packages will be available when bundle is added.  
									for (; i < exported.length; i++) 
										ignorePkgs.add(exported[i].getName());
								}
							}
						} 
						subMonitor.worked(1);
					}
				}
				subMonitor.done();
			} catch (CoreException e) {
				monitor.done();
			}
		}
	}
	
	protected final void addProjectPackages(IBundle bundle, Set ignorePkgs) {
		IBuild build = getBuild();
		if (build == null)
			return;
		IBuildEntry binIncludes = build.getEntry(IBuildEntry.BIN_INCLUDES);
		if (binIncludes != null) {
			String value = bundle.getHeader(Constants.BUNDLE_CLASSPATH);
			if (value == null) 
				value = "."; //$NON-NLS-1$
			ManifestElement elems[];
			try {
				elems = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, value);
			} catch (BundleException e) {
				return;
			}
			IJavaProject jProject = JavaCore.create(fProject);
			for (int i = 0; i < elems.length; i++) {
				String library = elems[i].getValue();
				if (binIncludes.contains(library)) {
					IBuildEntry entry = build.getEntry(IBuildEntry.JAR_PREFIX + library);
					if (entry != null) {
						String [] resources = entry.getTokens();
						for (int j = 0; j < resources.length; j++) 
							addPackagesFromResource(jProject, fProject.findMember(resources[j]), ignorePkgs);
					}
				} else {
					StringTokenizer tokenizer = new StringTokenizer(library,"/"); //$NON-NLS-1$
					StringBuffer buffer = new StringBuffer();
					while (tokenizer.hasMoreTokens()) {
						buffer.append(tokenizer.nextToken()).append('/');
						if (binIncludes.contains(buffer.toString()))
							addPackagesFromResource(jProject, fProject.findMember(library), ignorePkgs);
					}
				}
			}
		}
	}
	
	private void addPackagesFromResource(IJavaProject jProject, IResource res, Set ignorePkgs) {
		if (res == null)
			return;
		try {
			IPackageFragmentRoot root = jProject.getPackageFragmentRoot(res);
			IJavaElement[] children = root.getChildren();
			for (int i = 0; i < children.length; i++) {
				String pkgName = children[i].getElementName();
				if (children[i] instanceof IParent)
					if (pkgName.length() > 0 && ((IParent)children[i]).hasChildren()) 
						ignorePkgs.add(children[i].getElementName());
			}
		} catch (JavaModelException e) {
		}
	}
	
	protected final List getDynamicImports(IBundle bundle) {
		String value = bundle.getHeader(Constants.DYNAMICIMPORT_PACKAGE);
		if (value == null) 
			return new ArrayList(0);
		ManifestElement elems[] = null;
		try {
			elems = ManifestElement.parseHeader(Constants.DYNAMICIMPORT_PACKAGE, value);
		} catch (BundleException e) {
		}
		if (elems != null) {
			List imports = new ArrayList();
			for (int i = 0 ; i < elems.length; i++) {
				String pkg = elems[i].getValue();
				if (pkg.endsWith("*")) //$NON-NLS-1$
					pkg = pkg.substring(0, pkg.length() - 1);
				imports.add(pkg);
			}
			return imports;
		}
		return new ArrayList(0);
	}
	
	protected final boolean isDynamicallyImported(String pkgName, List dynamicImports) {
		ListIterator li = dynamicImports.listIterator();
		while (li.hasNext()) {
			String pkgImport = (String)li.next();
			if (pkgName.startsWith(pkgImport))
				return true;
		}
		return false;
	}
	
	protected void handleNewDependencies(final Map additionalDeps, final boolean useRequireBundle, IProgressMonitor monitor) {
		if (!additionalDeps.isEmpty()) 
			addDependencies(additionalDeps, useRequireBundle);
		monitor.done();
	}
	
	protected void addDependencies(final Map depsToAdd, boolean useRequireBundle) {
		if (useRequireBundle) {
			Collection plugins = depsToAdd.values();
			IBuild build = getBuild();
			IPluginBase pbase = fBase.getPluginBase();
			if (pbase == null )  {
				addRequireBundles(plugins, fBase.getBundleModel().getBundle(), build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES));
			}
			else 
				addRequireBundles(plugins, pbase, build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES));
			try {
				build.write("", new PrintWriter(new FileOutputStream(fProject.getFile("build.properties").getFullPath().toFile()))); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (FileNotFoundException e) {
			}
		} else {
			Collection pkgs = depsToAdd.keySet();
			addImportPackages(pkgs, fBase.getBundleModel().getBundle());
		}
	}
	
	protected final void addImportPackages(final Collection depsToAdd, final IBundle bundle) {
		Iterator it = depsToAdd.iterator();
		IManifestHeader mheader = bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		if (mheader instanceof ImportPackageHeader) {
			ImportPackageHeader header = (ImportPackageHeader) mheader;
			while (it.hasNext()) 
				header.addPackage((String)it.next());
		} else {
			String currentValue = (mheader != null) ? mheader.getValue() : null;
			StringBuffer buffer = (currentValue == null) ? new StringBuffer() : new StringBuffer(currentValue).append(", "); //$NON-NLS-1$
			while(it.hasNext()) 
				buffer.append((String)it.next()).append(", "); //$NON-NLS-1$
			if (buffer.length() > 0) 
				buffer.setLength(buffer.length() - 2);
			bundle.setHeader(Constants.IMPORT_PACKAGE, buffer.toString());
		}
	}
	
	protected final void addRequireBundles(final Collection depsToAdd, final IBundle bundle, IBuildEntry entry) {
		if (bundle == null)
			return;
		HashSet added = new HashSet();
		Iterator it = depsToAdd.iterator();
		IManifestHeader mheader = bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (mheader instanceof RequireBundleHeader) {
			RequireBundleHeader header = (RequireBundleHeader) mheader;
			while (it.hasNext())  {
				String pluginId = (String)it.next();
				if (!added.contains(pluginId))
					try {
						header.addBundle(pluginId);
						added.add(pluginId);
						entry.removeToken(pluginId);
					} catch (CoreException e) {
					}
			}
		}
		else {
			String currentValue = (mheader != null) ? mheader.getValue() : null;
			StringBuffer buffer = (currentValue == null) ? new StringBuffer() : new StringBuffer(currentValue).append(", "); //$NON-NLS-1$
			while(it.hasNext()) { 
				String pluginId = (String) it.next();
				if (!added.contains(pluginId))
					try {
						buffer.append(pluginId).append(", "); //$NON-NLS-1$
						added.add(pluginId);
						entry.removeToken(pluginId);
					} catch (CoreException e) {
					}
			}
			if (buffer.length() > 0) 
				buffer.setLength(buffer.length() - 2);
			bundle.setHeader(Constants.REQUIRE_BUNDLE, buffer.toString());
		} 
	}
	
	protected final void addRequireBundles(final Collection depsToAdd, final IPluginBase base, IBuildEntry entry) {
		HashSet added = new HashSet();
		Iterator it = depsToAdd.iterator();
		// must call getImports to initialize IPluginBase.  Otherwise the .add(plugin) will not trigger a modification event.
		base.getImports();
		while (it.hasNext()) {
			String pluginId = (String)it.next();
			if (!added.contains(pluginId))
				try {
					PluginImport plugin = new PluginImport();
					ManifestElement element = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, pluginId)[0];
					plugin.load(element, 1);
					plugin.setModel(base.getModel());
					base.add(plugin);
					added.add(pluginId);
					entry.removeToken(pluginId);
				} catch (BundleException e){
				} catch (CoreException e) {
				}
		}
	}
}
