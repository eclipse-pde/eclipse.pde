/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev (ProSyst) - bug 205777
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.framework.Version;

public class PDEState extends MinimalState {

	private final PDEAuxiliaryState fAuxiliaryState;
	private final ArrayList<IPluginModelBase> fTargetModels = new ArrayList<>();

	/**
	 * Creates a new PDE State containing bundles from the given URLs.
	 *
	 * @param target urls of target bundles
	 * @param addResolver whether to add a resolver to the state
	 * @param removeDuplicates whether to remove duplicate conflicting bundles from the state
	 * @param monitor progress monitor
	 */
	public PDEState(URI[] target, boolean addResolver, boolean removeDuplicates, IProgressMonitor monitor) {
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

		if (PDECore.DEBUG_MODEL) {
			System.out.println("Time to create state: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void createNewTargetState(boolean resolve, URI[] uris, IProgressMonitor monitor) {
		fState = stateObjectFactory.createState(resolve);
		if (resolve) {
			fState.getResolver().setSelectionPolicy(new Comparator<BaseDescription>() {
				@Override
				public int compare(BaseDescription bd1, BaseDescription bd2) {
					Version v1 = bd1.getVersion();
					Version v2 = bd2.getVersion();
					int versionCompare = versionCompare(v1, v2);
					if (versionCompare != 0) {
						return versionCompare;
					}
					BundleDescription s1 = bd1.getSupplier();
					BundleDescription s2 = bd2.getSupplier();
					String n1 = s1.getName();
					String n2 = s2.getName();
					if (n1 != null && n1.equals(n2)) {
						int retValue = versionCompare(s1.getVersion(), s2.getVersion());
						if(retValue == 0){
							boolean isQualifier = "qualifier".equals(v1.getQualifier()); //$NON-NLS-1$
							if (!isQualifier) {
								String loc1 = s1.getLocation();
								String loc2 = s2.getLocation();
								if (loc1 != null && loc2 != null  && !loc1.equals(loc2)) {
									IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
									if (root != null) {
										IPath p1 = new Path(loc1);
										if (root.findContainersForLocationURI(URIUtil.toURI(p1)).length != 0) {
											return -1;
										}
										IPath p2 = new Path(loc2);
										if (root.findContainersForLocationURI(URIUtil.toURI(p2)).length != 0) {
											return 1;
										}
									}
								}
							}
						}
						return retValue;
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
						if (v1.getQualifier().equals(v2.getQualifier())) {
							return 0;
						}
						boolean q1 = "qualifier".equals(v1.getQualifier()); //$NON-NLS-1$
						boolean q2 = "qualifier".equals(v2.getQualifier()); //$NON-NLS-1$
						if (q1 && !q2) {
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
		SubMonitor subMonitor = SubMonitor.convert(monitor, PDECoreMessages.PDEState_CreatingTargetModelState,
				uris.length);
		for (URI uri : uris) {
			File file = toFile(uri);
			if (file == null) {
				continue;
			}
			try {
				subMonitor.subTask(file.getName());
				addBundle(file, -1);
			} catch (CoreException e) {
				PDECore.log(e);
			}
			subMonitor.split(1);
		}
	}

	/**
	 * @param uri
	 * @return File object or {@code null} if URI can't be converted to file. In
	 *         the later case an error is logged.
	 */
	private static File toFile(URI uri) {
		IPath path = URIUtil.toPath(uri);
		if (path != null) {
			return path.toFile();
		}
		PDECore.log(new IllegalArgumentException("Failed to convert URI to file: " + uri)); //$NON-NLS-1$
		return null;
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
		for (BundleDescription desc : bundles) {
			String id = desc.getSymbolicName();
			BundleDescription[] conflicts = state.getBundles(id);
			if (conflicts.length > 1) {
				for (BundleDescription conflict : conflicts) {
					if (desc.getVersion().equals(conflict.getVersion()) && desc.getBundleId() != conflict.getBundleId()) {
						fState.removeBundle(desc);
					}
				}
			}
		}
	}

	private IPluginModelBase[] createTargetModels(BundleDescription[] bundleDescriptions) {
		HashMap<String, IPluginModelBase> models = new LinkedHashMap<>((4 / 3) * bundleDescriptions.length + 1);
		for (BundleDescription desc : bundleDescriptions) {
			IPluginModelBase base = createExternalModel(desc);
			fTargetModels.add(base);
			models.put(desc.getSymbolicName(), base);
		}
		if (models.isEmpty()) {
			return new IPluginModelBase[0];
		}
		return models.values().toArray(new IPluginModelBase[models.size()]);
	}

	private IPluginModelBase createExternalModel(BundleDescription desc) {
		ExternalPluginModelBase model = null;
		if (desc.getHost() == null) {
			model = new ExternalPluginModel();
		} else {
			model = new ExternalFragmentModel();
		}
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
			for (File child : children) {
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
