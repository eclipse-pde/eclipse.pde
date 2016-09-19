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
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.framework.Version;

public class PDEState extends MinimalState {

	private PDEAuxiliaryState fAuxiliaryState;
	private ArrayList<IPluginModelBase> fTargetModels = new ArrayList<>();

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
		if (resolve) {
			fState.getResolver().setSelectionPolicy(new Comparator<BaseDescription>() {
				@Override
				public int compare(BaseDescription bd1, BaseDescription bd2) {
					Version v1 = bd1.getVersion();
					Version v2 = bd2.getVersion();
					int versionCompare = versionCompare(v1, v2);
					if (versionCompare != 0)
						return versionCompare;
					BundleDescription s1 = bd1.getSupplier();
					BundleDescription s2 = bd2.getSupplier();
					String n1 = s1.getName();
					String n2 = s2.getName();
					if (n1 != null && n1.equals(n2)) {
						return versionCompare(s1.getVersion(), s2.getVersion());
					}
					long id1 = s1.getBundleId();
					long id2 = s2.getBundleId();
					return (id1 < id2) ? -1 : ((id1 == id2) ? 0 : 1);
				}

				/**
				 * Compares the given versions and prefers ".qualifier" versions over versions
				 * with any concrete qualifier.
				 * 
				 * @param v1 first version
				 * @param v2 second version
				 * @return a negative number, zero, or a positive number depending on
				 * if the first version is more desired, equal amount of desire, or less desired
				 * than the second version respectively
				 */
				private int versionCompare(Version v1, Version v2) {
					if (v1.getMajor() == v2.getMajor() && v1.getMinor() == v2.getMinor() && v1.getMicro() == v2.getMicro()) {
						boolean q1 = "qualifier".equals(v1.getQualifier()); //$NON-NLS-1$
						boolean q2 = "qualifier".equals(v2.getQualifier()); //$NON-NLS-1$
						if (q1 && q2) {
							return 0;
						} else if (q1 && !q2) {
							return -1;
						} else if (q2 && !q1) {
							return 1;
						}
					}
					int versionCompare = -(v1.compareTo(v2));
					return versionCompare;
				}
			});
		}
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
		HashMap<String, IPluginModelBase> models = new HashMap<>((4 / 3) * bundleDescriptions.length + 1);
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
