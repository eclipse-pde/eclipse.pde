/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.util.ManifestUtils;
import org.osgi.framework.BundleException;

public class MinimalState {

	protected State fState;

	protected long fId;

	private boolean fEEListChanged = false; // indicates that the EE has changed
	// this could be due to the system bundle changing location
	// or initially when the ee list is first created.

	private String[] fExecutionEnvironments; // an ordered list of known/supported execution environments

	private boolean fNoProfile;

	protected static StateObjectFactory stateObjectFactory;

	protected static String DIR;

	protected String fSystemBundle = IPDEBuildConstants.BUNDLE_OSGI;

	static {
		DIR = PDECore.getDefault().getStateLocation().toOSString();
		stateObjectFactory = Platform.getPlatformAdmin().getFactory();
	}

	protected MinimalState(MinimalState state) {
		this.fState = stateObjectFactory.createState(state.fState);
		this.fState.setPlatformProperties(state.fState.getPlatformProperties());
		this.fState.setResolver(Platform.getPlatformAdmin().createResolver());
		this.fId = state.fId;
		this.fEEListChanged = state.fEEListChanged;
		this.fExecutionEnvironments = state.fExecutionEnvironments;
		this.fNoProfile = state.fNoProfile;
		this.fSystemBundle = state.fSystemBundle;
	}

	protected MinimalState() {
	}

	public void addBundle(IPluginModelBase model, boolean update) {
		if (model == null)
			return;

		BundleDescription desc = model.getBundleDescription();
		long bundleId = desc == null || !update ? -1 : desc.getBundleId();
		try {
			BundleDescription newDesc = addBundle(new File(model.getInstallLocation()), bundleId);
			model.setBundleDescription(newDesc);
			if (newDesc == null && update)
				fState.removeBundle(desc);
		} catch (CoreException e) {
			PDECore.log(e);
		}
	}

	public BundleDescription addBundle(Map<String, String> manifest, File bundleLocation, long bundleId) {
		try {
			// OSGi requires a dictionary over any map
			Hashtable<String, String> dictionaryManifest = new Hashtable<String, String>(manifest);
			BundleDescription descriptor = stateObjectFactory.createBundleDescription(fState, dictionaryManifest, bundleLocation.getAbsolutePath(), bundleId == -1 ? getNextId() : bundleId);
			// new bundle
			if (bundleId == -1) {
				fState.addBundle(descriptor);
			} else if (!fState.updateBundle(descriptor)) {
				fState.addBundle(descriptor);
			}
			return descriptor;
		} catch (BundleException e) {
		} catch (NumberFormatException e) {
		} catch (IllegalArgumentException e) {
		}
		return null;
	}

	public BundleDescription addBundle(File bundleLocation, long bundleId) throws CoreException {
		Map<String, String> manifest = ManifestUtils.loadManifest(bundleLocation);
		// update for development mode
		TargetWeaver.weaveManifest(manifest);

		BundleDescription desc = addBundle(manifest, bundleLocation, bundleId);
		if (desc != null && manifest != null && "true".equals(manifest.get(ICoreConstants.ECLIPSE_SYSTEM_BUNDLE))) { //$NON-NLS-1$
			// if this is the system bundle then 
			// indicate that the javaProfile has changed since the new system
			// bundle may not contain profiles for all EE's in the list
			fEEListChanged = true;
			fSystemBundle = desc.getSymbolicName();
		}
		if (desc != null) {
			addAuxiliaryData(desc, manifest, true);
		}
		return desc;
	}

	protected void addAuxiliaryData(BundleDescription desc, Map<String, String> manifest, boolean hasBundleStructure) {
	}

	protected void saveState(File dir) {
		saveState(fState, dir);
	}

	protected void saveState(State state, File dir) {
		try {
			if (!dir.exists())
				dir.mkdirs();
			stateObjectFactory.writeState(state, dir);
		} catch (FileNotFoundException e) {
			PDECore.log(e);
		} catch (IOException e) {
			PDECore.log(e);
		} finally {
		}
	}

	public StateDelta resolveState(boolean incremental) {
		return internalResolveState(incremental);
	}

	/**
	 * Resolves the state incrementally based on the given bundle names.
	 *  
	 * @param symbolicNames
	 * @return state delta
	 */
	public StateDelta resolveState(String[] symbolicNames) {
		if (initializePlatformProperties()) {
			return fState.resolve(false);
		}
		List<BundleDescription> bundles = new ArrayList<BundleDescription>();
		for (int i = 0; i < symbolicNames.length; i++) {
			BundleDescription[] descriptions = fState.getBundles(symbolicNames[i]);
			for (int j = 0; j < descriptions.length; j++) {
				bundles.add(descriptions[j]);
			}
		}
		return fState.resolve(bundles.toArray(new BundleDescription[bundles.size()]));
	}

	private synchronized StateDelta internalResolveState(boolean incremental) {
		boolean fullBuildRequired = initializePlatformProperties();
		return fState.resolve(incremental && !fullBuildRequired);
	}

	protected boolean initializePlatformProperties() {
		if (fExecutionEnvironments == null && !fNoProfile)
			setExecutionEnvironments();

		if (fEEListChanged) {
			fEEListChanged = false;
			return fState.setPlatformProperties(getProfilePlatformProperties());
		}
		return false;
	}

	private Dictionary<String, String>[] getProfilePlatformProperties() {
		return TargetPlatformHelper.getPlatformProperties(fExecutionEnvironments, this);
	}

	public void removeBundleDescription(BundleDescription description) {
		if (description != null)
			fState.removeBundle(description);
	}

	public State getState() {
		return fState;
	}

	private void setExecutionEnvironments() {
		String[] knownExecutionEnviroments = TargetPlatformHelper.getKnownExecutionEnvironments();
		if (knownExecutionEnviroments.length == 0) {
			String jreProfile = System.getProperty("pde.jreProfile"); //$NON-NLS-1$
			if (jreProfile != null && jreProfile.length() > 0)
				if ("none".equals(jreProfile)) //$NON-NLS-1$
					fNoProfile = true;
		}
		if (!fNoProfile) {
			fExecutionEnvironments = knownExecutionEnviroments;
		}
		fEEListChanged = true; // alway indicate the list has changed
	}

	public void addBundleDescription(BundleDescription toAdd) {
		if (toAdd != null)
			fState.addBundle(toAdd);
	}

	public long getNextId() {
		return ++fId;
	}

	private BundleDescription findActiveBundle(String symbolicName) {
		BundleDescription[] bundles = fState.getBundles(symbolicName);
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].isResolved())
				return bundles[i];
		}
		return null;
	}

	protected void logResolutionErrors() {
		MultiStatus errors = new MultiStatus(PDECore.PLUGIN_ID, 1, "Problems occurred during the resolution of the target platform", //$NON-NLS-1$
				null);

		StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
		BundleDescription[] all = fState.getBundles();
		for (int i = 0; i < all.length; i++) {
			if (!all[i].isResolved()) {
				VersionConstraint[] unsatisfiedConstraints = helper.getUnsatisfiedConstraints(all[i]);
				if (unsatisfiedConstraints.length == 0) {
					if (PDECore.DEBUG_CACHE) {
						BundleDescription activeBundle = findActiveBundle(all[i].getSymbolicName());
						String message = "Plug-in located at \"" + all[i].getLocation() + "\" was disabled because plug-in located at \"" + activeBundle.getLocation() + "\" was selected."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						System.out.print(message);
					}
				} else {
					for (int j = 0; j < unsatisfiedConstraints.length; j++) {
						String message = getResolutionFailureMessage(unsatisfiedConstraints[j]);
						if (message != null)
							errors.add(new Status(IStatus.WARNING, all[i].getSymbolicName(), IStatus.WARNING, message, null));
					}
				}
			}
		}
		if (errors.getChildren().length > 0)
			PDECore.log(errors);
	}

	private String getResolutionFailureMessage(VersionConstraint unsatisfied) {
		if (unsatisfied.isResolved())
			throw new IllegalArgumentException();
		if (unsatisfied instanceof ImportPackageSpecification)
			return "Missing imported package: " + toString(unsatisfied); //$NON-NLS-1$
		if (unsatisfied instanceof BundleSpecification && !((BundleSpecification) unsatisfied).isOptional())
			return "Missing required plug-in: " + toString(unsatisfied); //$NON-NLS-1$
		if (unsatisfied instanceof HostSpecification)
			return "Missing Fragment Host: " + toString(unsatisfied); //$NON-NLS-1$
		return null;
	}

	private String toString(VersionConstraint constraint) {
		VersionRange versionRange = constraint.getVersionRange();
		if (versionRange == null || versionRange.getMinimum() != null)
			return constraint.getName();
		return constraint.getName() + '_' + versionRange;
	}

	public String getSystemBundle() {
		return fSystemBundle;
	}

}
