/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev (ProSyst) - bug 205777
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.target.LoadTargetDefinitionJob;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.bundle.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class PDEState extends MinimalState {

	private PDEAuxiliaryState fAuxiliaryState;

	private ArrayList fTargetModels = new ArrayList();
	private ArrayList fWorkspaceModels = new ArrayList();
	private boolean fCombined;
	private long fTargetTimestamp;
	private boolean fNewState;

	/**
	 * Creates a deep copy of the PDEState and its external models.  None of the workspace models are included in the copy.
	 * @param state
	 */
	public PDEState(PDEState state) {
		super(state);
		fCombined = false;
		fTargetTimestamp = state.fTargetTimestamp;
		// make sure to copy auxiliary state before trying to copy models, otherwise you will get NPEs.  Need auxiliary data to accurate create new models.
		copyAuxiliaryState();
		copyModels(state);
	}

	private void copyAuxiliaryState() {
		// always read the state instead of copying over contents of current state.  If current state has not been reloaded, it will
		// not contain any data from the target plug-ins.
		fAuxiliaryState = new PDEAuxiliaryState();
		fAuxiliaryState.readPluginInfoCache(new File(DIR, Long.toString(fTargetTimestamp) + ".target")); //$NON-NLS-1$
	}

	private void copyModels(PDEState state) {
		IPluginModelBase[] bases = state.getTargetModels();
		fTargetModels = new ArrayList(bases.length);
		for (int i = 0; i < bases.length; i++) {
			BundleDescription oldBD = bases[i].getBundleDescription();
			if (oldBD == null)
				continue;

			// do a deep copy of the model and accurately set the copied BundleDescription
			BundleDescription newBD = getState().getBundle(oldBD.getBundleId());
			// newDesc will be null if a workspace plug-in has the same Bundle-SymbolicName as the target plug-in.
			// This is because in PluginModelManager we add the workspace's BundleDescription and remove the corresponding targets.
			// This is done so that the resolver state will always resolve to the workspace's BundleDescription and not one in the target.
			if (newBD == null) {
				// If this happens, copy the target bundle's BundleDescription then add it back into the copied state.
				newBD = Platform.getPlatformAdmin().getFactory().createBundleDescription(oldBD);
				getState().addBundle(newBD);
			}
			IPluginModelBase model = createExternalModel(newBD);
			model.setEnabled(bases[i].isEnabled());
			fTargetModels.add(model);
		}
		// remove workspace models to accurately represent only the target platform
		bases = PluginRegistry.getWorkspaceModels();
		for (int i = 0; i < bases.length; i++) {
			removeBundleDescription(bases[i].getBundleDescription());
		}
	}

	public PDEState(URL[] urls, boolean resolve, IProgressMonitor monitor) {
		this(new URL[0], urls, resolve, false, monitor);
	}

	public PDEState(URL[] workspace, URL[] target, boolean resolve, boolean removeTargetDuplicates, IProgressMonitor monitor) {
		long start = System.currentTimeMillis();
		fAuxiliaryState = new PDEAuxiliaryState();

		if (resolve) {
			readTargetState(target, monitor);
		} else {
			createNewTargetState(resolve, target, monitor);
		}

		if (removeTargetDuplicates) {
			removeDuplicatesFromState(fState);
		}

		createTargetModels(fState.getBundles());

		if (resolve && workspace.length > 0 && !fNewState && !"true".equals(System.getProperty("pde.nocache"))) //$NON-NLS-1$ //$NON-NLS-2$
			readWorkspaceState(workspace);

		if (DEBUG)
			System.out.println("Time to create state: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void readTargetState(URL[] urls, IProgressMonitor monitor) {
		fTargetTimestamp = computeTimestamp(urls);
		if (DEBUG) {
			System.out.println("Timestamp of " + urls.length + " target URLS: " + fTargetTimestamp); //$NON-NLS-1$ //$NON-NLS-2$
		}
		File dir = new File(DIR, Long.toString(fTargetTimestamp) + ".target"); //$NON-NLS-1$
		if ((fState = readStateCache(dir)) == null || !fAuxiliaryState.readPluginInfoCache(dir)) {
			if (DEBUG) {
				System.out.println("Creating new state, persisted state did not exist"); //$NON-NLS-1$
			}
			createNewTargetState(true, urls, monitor);
			resolveState(false);
		} else {
			if (DEBUG) {
				System.out.println("Restored previously persisted state"); //$NON-NLS-1$
			}
			// get the system bundle from the State
			if (fState.getPlatformProperties() != null && fState.getPlatformProperties().length > 0) {
				String systemBundle = (String) fState.getPlatformProperties()[0].get(ICoreConstants.OSGI_SYSTEM_BUNDLE);
				if (systemBundle != null)
					fSystemBundle = systemBundle;
			}

			boolean propertiesChanged = initializePlatformProperties();
			fState.setResolver(Platform.getPlatformAdmin().createResolver());
			if (propertiesChanged)
				fState.resolve(false);
			fId = fState.getHighestBundleId();
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
				PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, "Invalid manifest format at " + file.getAbsolutePath(), //$NON-NLS-1$
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

	/**
	 * When creating a target state, having duplicates of certain bundles including core runtime cause problems when launching.  The
	 * {@link LoadTargetDefinitionJob} removes duplicates for us, but on restart the state is created from preferences.  This method
	 * search the state for bundles with the same ID/Version.  Where multiple bundles are found, all but one are removed from the state.
	 * 
	 * @param state state to search for duplicates in
	 */
	private void removeDuplicatesFromState(State state) {
		BundleDescription[] bundles = state.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription desc = bundles[i];
			String id = desc.getSymbolicName();
			BundleDescription[] conflicts = state.getBundles(id);
			if (conflicts.length > 1) {
				for (int j = 0; j < conflicts.length; j++) {
					if (desc.getVersion().equals(conflicts[j].getVersion()) && desc.getBundleId() != conflicts[j].getBundleId()) {
						fState.removeBundle(desc);
					}
				}
			}
		}
	}

	private IPluginModelBase[] createTargetModels(BundleDescription[] bundleDescriptions) {
		HashMap models = new HashMap((4 / 3) * bundleDescriptions.length + 1);
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
		fCombined = localState != null && fAuxiliaryState.readPluginInfoCache(dir);
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
		List sorted = new ArrayList(urls.length);
		for (int i = 0; i < urls.length; i++) {
			sorted.add(urls[i]);
		}
		Collections.sort(sorted, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((URL) o1).toExternalForm().compareTo(((URL) o2).toExternalForm());
			}
		});
		URL[] sortedURLs = (URL[]) sorted.toArray(new URL[sorted.size()]);
		for (int i = 0; i < sortedURLs.length; i++) {
			File file = new File(sortedURLs[i].getFile());
			if (file.exists()) {
				if (file.isFile()) {
					timestamp ^= file.lastModified();
				} else {
					File manifest = new File(file, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
				}
				timestamp ^= file.getAbsolutePath().toLowerCase().hashCode();
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
		IFile manifest = PDEProject.getManifest(project);
		IFile pluginXml = PDEProject.getPluginXml(project);
		IFile fragmentXml = PDEProject.getFragmentXml(project);
		if (manifest.exists()) {
			BundlePluginModelBase model = null;
			if (desc.getHost() == null)
				model = new BundlePluginModel();
			else
				model = new BundleFragmentModel();
			model.setEnabled(true);
			WorkspaceBundleModel bundle = new WorkspaceBundleModel(manifest);
			bundle.load(desc, this);
			model.setBundleDescription(desc);
			model.setBundleModel(bundle);
			bundle.setEditable(false);

			IFile file = (desc.getHost() == null) ? pluginXml : fragmentXml;
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
			model = new WorkspacePluginModel(pluginXml, true);
		else
			model = new WorkspaceFragmentModel(fragmentXml, true);
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
		return (IPluginModelBase[]) fTargetModels.toArray(new IPluginModelBase[fTargetModels.size()]);
	}

	public IPluginModelBase[] getWorkspaceModels() {
		return (IPluginModelBase[]) fWorkspaceModels.toArray(new IPluginModelBase[fWorkspaceModels.size()]);
	}

	/**
	 * Saves state associated with the external PDE target. 
	 */
	public void saveExternalState() {
		IPluginModelBase[] models = PluginRegistry.getExternalModels();
		URL[] urls = new URL[models.length];
		for (int i = 0; i < urls.length; i++) {
			try {
				urls[i] = new File(models[i].getInstallLocation()).toURL();
			} catch (MalformedURLException e) {
				if (DEBUG) {
					System.out.println("FAILED to save external state due to MalformedURLException"); //$NON-NLS-1$
				}
				return;
			}
		}
		fTargetTimestamp = computeTimestamp(urls);
		File dir = new File(DIR, Long.toString(fTargetTimestamp) + ".target"); //$NON-NLS-1$

		boolean osgiStateExists = dir.exists() && dir.isDirectory();
		boolean auxStateExists = fAuxiliaryState.exists(dir);
		if (!osgiStateExists || !auxStateExists) {
			if (!dir.exists())
				dir.mkdirs();
			if (DEBUG) {
				System.out.println("Saving external state of " + urls.length + " bundles to: " + dir.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			State state = stateObjectFactory.createState(false);
			for (int i = 0; i < models.length; i++) {
				BundleDescription desc = models[i].getBundleDescription();
				if (desc != null)
					state.addBundle(state.getFactory().createBundleDescription(desc));
			}
			fAuxiliaryState.savePluginInfo(dir);
			saveState(state, dir);
		} else if (DEBUG) {
			System.out.println("External state unchanged, save skipped."); //$NON-NLS-1$
		}
	}

	/**
	 * Save state associated with workspace models and deletes persisted
	 * files associated with other time stamps.
	 */
	public void saveWorkspaceState() {
		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
		long timestamp = 0;
		if (!"true".equals(System.getProperty("pde.nocache")) && shouldSaveState(models)) { //$NON-NLS-1$ //$NON-NLS-2$
			timestamp = computeTimestamp(models);
			File dir = new File(DIR, Long.toString(timestamp) + ".workspace"); //$NON-NLS-1$
			if (DEBUG) {
				System.out.println("Saving workspace state to: " + dir.getAbsolutePath()); //$NON-NLS-1$
			}
			State state = stateObjectFactory.createState(false);
			for (int i = 0; i < models.length; i++) {
				BundleDescription desc = models[i].getBundleDescription();
				if (desc != null)
					state.addBundle(state.getFactory().createBundleDescription(desc));
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
		int nonOSGiModels = 0;
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null) {
				// not an OSGi bundle
				++nonOSGiModels;
				continue;
			}
			if (id.trim().length() == 0 || !models[i].isLoaded() || !models[i].isInSync() || models[i].getBundleDescription() == null)
				return false;
		}
		return models.length - nonOSGiModels > 0;
	}

	private void clearStaleStates(String extension, long latest) {
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString());
		File[] children = dir.listFiles();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				File child = children[i];
				if (child.isDirectory()) {
					String name = child.getName();
					if (name.endsWith(extension) && name.length() > extension.length() && !name.equals(Long.toString(latest) + extension)) {
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

	public String getBundleSourceEntry(long bundleID) {
		return fAuxiliaryState.getBundleSourceEntry(bundleID);
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
				PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, "Invalid manifest format at " + file.getAbsolutePath(), //$NON-NLS-1$
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
