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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.pde.internal.core.util.ManifestUtils;
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
			final String systemBSN = getSystemBundle();
			Comparator<BaseDescription> policy = systemBundlesFirst(systemBSN)
					.thenComparing(BaseDescription::getVersion, HIGHER_VERSION_FIRST)
					.thenComparing(BaseDescription::getSupplier, HIGHER_LOCAL_VERSION_FIRST);
			fState.getResolver().setSelectionPolicy(policy);
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
				if (e.getStatus().getCode() != ManifestUtils.STATUS_CODE_NOT_A_BUNDLE_MANIFEST) {
					PDECore.log(e);
				}
			}
			subMonitor.split(1);
		}
	}

	private Comparator<BaseDescription> systemBundlesFirst(String systemBSN) {
		Function<BaseDescription, Boolean> isSystemBundle = b -> systemBSN.equals(b.getSupplier().getSymbolicName());
		return Comparator.comparing(isSystemBundle).reversed(); // false<true
	}

	/**
	 * Compares the given versions and prefers ".qualifier" versions over
	 * versions with any concrete qualifier.
	 */
	private static final Comparator<Version> HIGHER_VERSION_FIRST = Comparator //
			.comparingInt(Version::getMajor) //
			.thenComparingInt(Version::getMinor) //
			.thenComparingInt(Version::getMicro) //
			.thenComparing(PDEState::hasGenericQualifier) // false<true
			.thenComparing(Version::getQualifier) //
			.reversed(); //

	private static final Comparator<BundleDescription> IN_WORKSPACE_FIRST = (s1, s2) -> {
		String n1 = s1.getName();
		String n2 = s2.getName();
		// From previous comparison we know that both versions are fully equal
		if (n1 != null && n1.equals(n2) && !hasGenericQualifier(s1.getVersion())) {
			String loc1 = s1.getLocation();
			String loc2 = s2.getLocation();
			if (loc1 != null && loc2 != null && !loc1.equals(loc2)) {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				if (root != null) {
					boolean s1InWorkspace = isInWorkspace(loc1, root);
					boolean s2InWorkspace = isInWorkspace(loc2, root);
					return -Boolean.compare(s1InWorkspace, s2InWorkspace); // false<true
				}
			}
		}
		return 0;
	};

	private static final Comparator<BundleDescription> HIGHER_LOCAL_VERSION_FIRST = Comparator
			// To have a stable ordering, the version MUST be compared first,
			// even if from different suppliers
			.comparing(BundleDescription::getVersion, HIGHER_VERSION_FIRST) //
			.thenComparing(IN_WORKSPACE_FIRST)
			// as a last resort, compare the bundle id...
			.thenComparing(BundleDescription::getBundleId);

	private static boolean hasGenericQualifier(Version version) {
		return "qualifier".equals(version.getQualifier()); //$NON-NLS-1$
	}

	private static boolean isInWorkspace(String location, IWorkspaceRoot root) {
		return root.findContainersForLocationURI(Path.of(location).toUri()).length > 0;
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

	public boolean exportsExternalAnnotations(long bundleID) {
		return fAuxiliaryState.exportsExternalAnnotations(bundleID);
	}

}
