/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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

import java.io.File;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class PDEState extends MinimalState {

	private PDEAuxiliaryState fAuxiliaryState;
	private ArrayList<IPluginModelBase> fTargetModels = new ArrayList<IPluginModelBase>();

	/**
	 * Creates a new PDE State containing bundles from the given URLs.
	 * 
	 * @param target urls of target bundles
	 * @param addResolver whether to add a resolver to the state
	 * @param removeDuplicates whether to remove duplicate conflicting bundles from the state
	 * @param monitor progress monitor
	 */
	public PDEState(URL[] target, boolean addResolver, boolean removeDuplicates, IProgressMonitor monitor) {
		long start = System.currentTimeMillis();
		fAuxiliaryState = new PDEAuxiliaryState();

		// We no longer try and restore from a cached state as it had no performance benefit
		createNewTargetState(addResolver, target, monitor);

		if (removeDuplicates) {
			removeDuplicatesFromState(fState);
		}

		initializePlatformProperties();
		createTargetModels(fState.getBundles());
		clearOldCache();

		if (PDECore.DEBUG_MODEL)
			System.out.println("Time to create state: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void createNewTargetState(boolean resolve, URL[] urls, IProgressMonitor monitor) {
		fState = stateObjectFactory.createState(resolve);
		monitor.beginTask(PDECoreMessages.PDEState_CreatingTargetModelState, urls.length);
		for (int i = 0; i < urls.length; i++) {
			File file = new File(urls[i].getFile());
			try {
				if (monitor.isCanceled())
					// if canceled, stop loading bundles
					return;
				monitor.subTask(file.getName());
				addBundle(file, -1);
			} catch (CoreException e) {
				PDECore.log(e);
			} finally {
				monitor.worked(1);
			}
		}
	}

	@Override
	protected void addAuxiliaryData(BundleDescription desc, Map<String, String> manifest, boolean hasBundleStructure) {
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
		// TODO This shouldn't be required if the target is removing duplicates, but test workspace shows some duplicates still
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
		HashMap<String, IPluginModelBase> models = new HashMap<String, IPluginModelBase>((4 / 3) * bundleDescriptions.length + 1);
		for (int i = 0; i < bundleDescriptions.length; i++) {
			BundleDescription desc = bundleDescriptions[i];
			IPluginModelBase base = createExternalModel(desc);
			fTargetModels.add(base);
			models.put(desc.getSymbolicName(), base);
		}
		if (models.isEmpty())
			return new IPluginModelBase[0];
		return models.values().toArray(new IPluginModelBase[models.size()]);
	}

	private IPluginModelBase createExternalModel(BundleDescription desc) {
		ExternalPluginModelBase model = null;
		if (desc.getHost() == null)
			model = new ExternalPluginModel();
		else
			model = new ExternalFragmentModel();
		model.load(desc, this);
		model.setBundleDescription(desc);
		model.setEnabled(true);
		return model;
	}

	public IPluginModelBase[] getTargetModels() {
		return fTargetModels.toArray(new IPluginModelBase[fTargetModels.size()]);
	}

	/**
	 * In previous releases the state was saved to the PDE .metadata directory.  If
	 * any of these states are still around we should delete them.
	 */
	private void clearOldCache() {
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString());
		File[] children = dir.listFiles();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				File child = children[i];
				if (child.isDirectory()) {
					String name = child.getName();
					if (name.endsWith(".target") && name.length() > ".target".length()) { //$NON-NLS-1$ //$NON-NLS-2$
						CoreUtility.deleteContent(child);
					} else if (name.endsWith(".workspace") && name.length() > ".workspace".length()) { //$NON-NLS-1$ //$NON-NLS-2$
						CoreUtility.deleteContent(child);
					} else if (name.endsWith(".cache") && name.length() > ".cache".length()) { //$NON-NLS-1$ //$NON-NLS-2$
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

}
