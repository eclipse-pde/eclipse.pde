/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.bundle.BundleFragmentModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginModelBase;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.plugin.WorkspaceExtensionsModel;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;


public class PDEState extends MinimalState {
	
	private PDEAuxiliaryState fAuxiliaryState;
	
	private ArrayList fTargetModels = new ArrayList();
	private ArrayList fWorkspaceModels = new ArrayList();
	private boolean fCombined;
	private long fTargetTimestamp;
	private boolean fNewState;
	
	public PDEState(PDEState state) {
		super(state);
		fTargetModels = new ArrayList(state.fTargetModels);
		fCombined = false;
		fTargetTimestamp = state.fTargetTimestamp;
		fAuxiliaryState = new PDEAuxiliaryState(state.fAuxiliaryState);
		if (fAuxiliaryState.fPluginInfos.isEmpty()) 
			fAuxiliaryState.readPluginInfoCache(new File(DIR, Long.toString(fTargetTimestamp) + ".target")); //$NON-NLS-1$
	}
	
	public PDEState(URL[] urls, boolean resolve, IProgressMonitor monitor) {
		this(new URL[0], urls, resolve, monitor);
	}
	
	public PDEState(URL[] workspace, URL[] target, boolean resolve, IProgressMonitor monitor) {
		long start = System.currentTimeMillis();	
		fAuxiliaryState = new PDEAuxiliaryState();
		
		if (resolve) {
			readTargetState(target, monitor);
		} else {
			createNewTargetState(resolve, target, monitor);	
		}
		createTargetModels(fState.getBundles());
		
		if (resolve && workspace.length > 0 && !fNewState && !"true".equals(System.getProperty("pde.nocache"))) //$NON-NLS-1$ //$NON-NLS-2$
			readWorkspaceState(workspace);
		
		fAuxiliaryState.clear();
		
		if (DEBUG)
			System.out.println("Time to create state: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private void readTargetState(URL[] urls, IProgressMonitor monitor) {
		fTargetTimestamp = computeTimestamp(urls);
		File dir = new File(DIR, Long.toString(fTargetTimestamp) + ".target"); //$NON-NLS-1$
		if ((fState = readStateCache(dir)) == null || !fAuxiliaryState.readPluginInfoCache(dir)) {
			createNewTargetState(true, urls, monitor);
			if (!dir.exists())
				dir.mkdirs();
			fAuxiliaryState.savePluginInfo(dir);
			resolveState(false);
			saveState(dir);
		} else {
			boolean propertiesChanged = initializePlatformProperties();
			fState.setResolver(Platform.getPlatformAdmin().getResolver());
			if (propertiesChanged)
				fState.resolve(false);
			fId = fState.getBundles().length;
		}
	}
	
	private void createNewTargetState(boolean resolve, URL[] urls, IProgressMonitor monitor) {
		fState = stateObjectFactory.createState(resolve);
		monitor.beginTask("", urls.length); //$NON-NLS-1$
		for (int i = 0; i < urls.length; i++) {
			File file = new File(urls[i].getFile());
			try {
				if (monitor.isCanceled())
					// if canceled, stop loading bundles
					return;
				monitor.subTask(file.getName());
				addBundle(file, -1);
			} catch (PluginConversionException e) {
			} catch (CoreException e) {
			} catch (IOException e) {
				PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR,
						"Invalid manifest format at " + file.getAbsolutePath(), //$NON-NLS-1$
						null)); 
			} finally {
				monitor.worked(1);
			}
		}
		fNewState = true;
	}
	
	protected void addAuxiliaryData(BundleDescription desc, Dictionary manifest, boolean hasBundleStructure) {
		fAuxiliaryState.addAuxiliaryData(desc, manifest, hasBundleStructure);
	}

	public IPluginModelBase[] createTargetModels(BundleDescription[] bundleDescriptions) {
		HashMap models = new HashMap((4/3) * bundleDescriptions.length + 1); 
		for (int i = 0; i < bundleDescriptions.length; i++) {
			BundleDescription desc = bundleDescriptions[i];
			IPluginModelBase base = createExternalModel(desc);
			fTargetModels.add(base);
			models.put(desc.getSymbolicName(), base);
		}
		if (models.isEmpty())
			return new IPluginModelBase[0];
		return (IPluginModelBase[]) models.values().toArray(new IPluginModelBase[models.size()]);
	}
 	
	private void readWorkspaceState(URL[] urls) {
		long workspace = computeTimestamp(urls);
		File dir = new File(DIR, Long.toString(workspace) + ".workspace"); //$NON-NLS-1$
		State localState = readStateCache(dir);
		fCombined = localState != null 
						&& fAuxiliaryState.readPluginInfoCache(dir);
		if (fCombined) {
			long targetCount = fId;
			BundleDescription[] bundles = localState.getBundles();
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription desc = bundles[i];
				String id = desc.getSymbolicName();
				BundleDescription[] conflicts = fState.getBundles(id);
				
				for (int j = 0; j < conflicts.length; j++) {
					// only remove bundles with a conflicting symblic name
					// if the conflicting bundles come from the target.
					// Workspace bundles with conflicting symbolic names are allowed
					if (conflicts[j].getBundleId() <= targetCount)
						fState.removeBundle(conflicts[j]);
				}
				
				BundleDescription newbundle = stateObjectFactory.createBundleDescription(desc);
				IPluginModelBase model = createWorkspaceModel(newbundle);
				if (model != null && fState.addBundle(newbundle)) {
					fId = Math.max(fId, newbundle.getBundleId());
					fWorkspaceModels.add(model);
				}
			}
			fId = Math.max(fId, fState.getBundles().length);
			fState.resolve(false);
		}
	}
	
	public boolean isCombined() {
		return fCombined;
	}	

	private State readStateCache(File dir) {
		if (dir.exists() && dir.isDirectory()) {
			try {
				return stateObjectFactory.readState(dir);	
			} catch (IllegalStateException e) {
				PDECore.log(e);
			} catch (FileNotFoundException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} finally {
			}
		} 
		return null;
	}
	
 	private long computeTimestamp(URL[] urls) {
		return computeTimestamp(urls, 0);
	}
 	
 	private long computeTimestamp(URL[] urls, long timestamp) {
 		for (int i = 0; i < urls.length; i++) {
			File file = new File(urls[i].getFile());
			if (file.exists()) {
				if (file.isFile()) {
					timestamp ^= file.lastModified();
				} else {
					File manifest = new File(file, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, "plugin.xml"); //$NON-NLS-1$
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, "fragment.xml"); //$NON-NLS-1$
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
				}
				timestamp ^= file.getAbsolutePath().hashCode();
			}
		}
		return timestamp;  
	}
 	
 	private IPluginModelBase createWorkspaceModel(BundleDescription desc) {
 		String projectName = fAuxiliaryState.getProject(desc.getBundleId());
 		if (projectName == null)
 			return null;
 		IProject project = PDECore.getWorkspace().getRoot().getProject(projectName);
 		if (!project.exists())
 			return null;
 		if (project.exists(ICoreConstants.MANIFEST_PATH)) {
 			BundlePluginModelBase model = null;
 			if (desc.getHost() == null)
 				model = new BundlePluginModel();
 			else
 				model = new BundleFragmentModel();
 			model.setEnabled(true);
 			WorkspaceBundleModel bundle = new WorkspaceBundleModel(project.getFile("META-INF/MANIFEST.MF")); //$NON-NLS-1$
 			bundle.load(desc, this);
  			model.setBundleDescription(desc);
 			model.setBundleModel(bundle);
 			bundle.setEditable(false);
 			
 			String filename = (desc.getHost() == null) ? "plugin.xml" : "fragment.xml"; //$NON-NLS-1$ //$NON-NLS-2$
 			IFile file = project.getFile(filename);
 			if (file.exists()) {
 				WorkspaceExtensionsModel extensions = new WorkspaceExtensionsModel(file);
 				extensions.setEditable(false);
 				extensions.setBundleModel(model);
 				extensions.load(desc, this);
 				model.setExtensionsModel(extensions);
 			}
 			return model;
 		}
 		
		WorkspacePluginModelBase model = null;
		if (desc.getHost() == null)
			model = new WorkspacePluginModel(project.getFile("plugin.xml"), true); //$NON-NLS-1$
		else
			model = new WorkspaceFragmentModel(project.getFile("fragment.xml"), true); //$NON-NLS-1$
		model.load(desc, this);
		model.setBundleDescription(desc);
		return model;
	}

	private IPluginModelBase createExternalModel(BundleDescription desc) {
 		ExternalPluginModelBase model = null;
 		if (desc.getHost() == null)
			model = new ExternalPluginModel();
		else
			model = new ExternalFragmentModel();
		model.load(desc, this);
		model.setBundleDescription(desc);
		return model;
 	}
 	
 	public IPluginModelBase[] getTargetModels() {
 		return (IPluginModelBase[])fTargetModels.toArray(new IPluginModelBase[fTargetModels.size()]);
 	}
 	
 	public IPluginModelBase[] getWorkspaceModels() {
 		return (IPluginModelBase[])fWorkspaceModels.toArray(new IPluginModelBase[fWorkspaceModels.size()]);		
 	}
 	
	public void shutdown() {
		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
		long timestamp = 0;
		if (!"true".equals(System.getProperty("pde.nocache")) && shouldSaveState(models)) { //$NON-NLS-1$ //$NON-NLS-2$
			timestamp = computeTimestamp(models);
			File dir = new File(DIR, Long.toString(timestamp) + ".workspace"); //$NON-NLS-1$
			State state = stateObjectFactory.createState(false);
			for (int i = 0; i < models.length; i++) {
				state.addBundle(models[i].getBundleDescription());
			}
			saveState(state, dir);
			PDEAuxiliaryState.writePluginInfo(models, dir);
		}
		clearStaleStates(".target", fTargetTimestamp); //$NON-NLS-1$
		clearStaleStates(".workspace", timestamp); //$NON-NLS-1$
		clearStaleStates(".cache", 0); //$NON-NLS-1$
	}
	
	private long computeTimestamp(IPluginModelBase[] models) {
		URL[] urls = new URL[models.length];
		for (int i = 0; i < models.length; i++) {
			try {
				IProject project = models[i].getUnderlyingResource().getProject();
				urls[i] = new File(project.getLocation().toString()).toURL();
			} catch (MalformedURLException e) {
			}
		}
		return computeTimestamp(urls);
	}
	
	private boolean shouldSaveState(IPluginModelBase[] models) {
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null
					|| id.trim().length() == 0
					|| !models[i].isLoaded()
					|| !models[i].isInSync() 
					|| models[i].getBundleDescription() == null)
				return false;
		}
		return models.length > 0;
	}
	
	private void clearStaleStates(String extension, long latest) {
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString());
		File[] children = dir.listFiles();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				File child = children[i];
				if (child.isDirectory()) {
					String name = child.getName();
					if (name.endsWith(extension)
							&& name.length() > extension.length()
							&& !name.equals(Long.toString(latest) + extension)) { 
						CoreUtility.deleteContent(child);
					}
				}
			}
		}
	}
	
	public String getClassName(long bundleID) {
		return fAuxiliaryState.getClassName(bundleID);
	}
	
	public boolean hasExtensibleAPI(long bundleID) {
		return fAuxiliaryState.hasExtensibleAPI(bundleID);		
	}

	public boolean isPatchFragment(long bundleID) {
		return fAuxiliaryState.isPatchFragment(bundleID);
	}
	
	public boolean hasBundleStructure(long bundleID) {
		return fAuxiliaryState.hasBundleStructure(bundleID);
	}

	public String getPluginName(long bundleID) {
		return fAuxiliaryState.getPluginName(bundleID);
	}
	
	public String getProviderName(long bundleID) {
		return fAuxiliaryState.getProviderName(bundleID);
	}
	
	public String[] getLibraryNames(long bundleID) {
		return fAuxiliaryState.getLibraryNames(bundleID);
	}
	
	public String getBundleLocalization(long bundleID) {
		return fAuxiliaryState.getBundleLocalization(bundleID);
	}
	
	public String getProject(long bundleID) {
		return fAuxiliaryState.getProject(bundleID);	
	}
	
	public BundleDescription[] addAdditionalBundles(URL[] newBundleURLs) {
		// add new Bundles to the State
		ArrayList descriptions = new ArrayList(newBundleURLs.length);
		for (int i = 0; i < newBundleURLs.length; i++) {
			File file = new File(newBundleURLs[i].getFile());
			try {
				BundleDescription desc = addBundle(file, -1);
				if (desc != null)
					descriptions.add(desc);
			} catch (PluginConversionException e) {
			} catch (CoreException e) {
			} catch (IOException e) {
				PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR,
						"Invalid manifest format at " + file.getAbsolutePath(), //$NON-NLS-1$
						null)); 
			}
		}
		// compute Timestamp and save all new information
		fTargetTimestamp = computeTimestamp(newBundleURLs, fTargetTimestamp);
		File dir = new File(DIR, Long.toString(fTargetTimestamp) + ".target"); //$NON-NLS-1$
		if (!dir.exists())
			dir.mkdirs();
		fAuxiliaryState.savePluginInfo(dir);
		saveState(dir);
		
		// resolve state - same steps as when populating a new State
		resolveState(false);
		
		return (BundleDescription[]) descriptions.toArray(new BundleDescription[descriptions.size()]);
	}
	
	public File getTargetDirectory() {
		return new File(DIR, Long.toString(fTargetTimestamp) + ".target"); //$NON-NLS-1$
	}

}
