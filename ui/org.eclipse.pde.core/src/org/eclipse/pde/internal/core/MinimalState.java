/*******************************************************************************
 * Copyright (c) 2005, 2024 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *     Hannes Wellmann - Enhance computation of system-package provided by a ExecutionEnvironment
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallChangedListener;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.PropertyChangeEvent;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.builders.PDEBuilderHelper;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.util.ManifestUtils;
import org.eclipse.pde.internal.core.util.UtilMessages;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;

public class MinimalState {

	protected State fState;

	protected long fId;

	private boolean fEEListChanged = false; // indicates that the EE has changed
	// this could be due to the system bundle changing location
	// or initially when the ee list is first created.

	/** ordered set of known/supported execution environments */
	private Set<String> fExecutionEnvironments;

	private boolean fNoProfile;

	protected static StateObjectFactory stateObjectFactory;

	protected String fSystemBundle = IPDEBuildConstants.BUNDLE_OSGI;

	static {
		stateObjectFactory = BundleHelper.getPlatformAdmin().getFactory();
	}

	protected MinimalState(MinimalState state) {
		this.fState = stateObjectFactory.createState(state.fState);
		this.fState.setPlatformProperties(state.fState.getPlatformProperties());
		this.fState.setResolver(BundleHelper.getPlatformAdmin().createResolver());
		this.fId = state.fId;
		this.fEEListChanged = state.fEEListChanged;
		this.fExecutionEnvironments = state.fExecutionEnvironments;
		this.fNoProfile = state.fNoProfile;
		this.fSystemBundle = state.fSystemBundle;
	}

	protected MinimalState() {
	}

	public void addBundle(IPluginModelBase model, boolean update) {
		if (model == null) {
			return;
		}

		BundleDescription desc = model.getBundleDescription();
		long bundleId = desc == null || !update ? -1 : desc.getBundleId();
		try {
			String installLocation = model.getInstallLocation();
			if (installLocation == null) {
				// This exception should help diagnosing the problem. If this
				// exception wasn't thrown then the code would throw a NPE in
				// the next line anyway.
				throw new IllegalArgumentException(
						"The plugin '" + model + "' was not created from a resource in the file system"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			File bundleLocation = new File(installLocation);
			BundleDescription newDesc = addBundle(bundleLocation, bundleId,
					loadWorkspaceBundleManifest(bundleLocation, model.getUnderlyingResource()));
			model.setBundleDescription(newDesc);
			if (newDesc == null && update) {
				fState.removeBundle(desc);
			}
		} catch (CoreException e) {
			PDECore.log(e);
			model.setBundleDescription(null);
		}
	}

	@SuppressWarnings("deprecation")
	private Map<String, String> loadWorkspaceBundleManifest(File bundleLocation, IResource resource)
			throws CoreException {
		Map<String, String> manifest = ManifestUtils.loadManifest(bundleLocation);
		if (resource == null || hasDeclaredRequiredEE(manifest)) {
			return manifest;
		}

		// inject BREE based on the project's JDK, otherwise packages from all
		// JREs are eligible for dependency resolution
		// e.g. a project compiled against Java 11 may get its java.xml
		// Import-Package resolved with a Java 8 profile
		IJavaProject javaProject = JavaCore.create(resource.getProject());
		if (!javaProject.exists()) {
			return manifest;
		}
		IVMInstall projectVmInstall = JavaRuntime.getVMInstall(javaProject);

		IExecutionEnvironment executionEnvironment = Arrays
				.stream(JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments())
				.filter(env -> env.isStrictlyCompatible(projectVmInstall)) //
				.findFirst().orElse(null);

		if (executionEnvironment != null) {
			manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, executionEnvironment.getId());
		}

		return manifest;
	}

	@SuppressWarnings("deprecation")
	private boolean hasDeclaredRequiredEE(Map<String, String> manifest) {
		if (manifest.containsKey(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT)) {
			return true;
		}
		try {
			String capability = manifest.get(Constants.REQUIRE_CAPABILITY);
			ManifestElement[] header = ManifestElement.parseHeader(Constants.REQUIRE_CAPABILITY, capability);
			return header != null && Arrays.stream(header).map(ManifestElement::getValue)
					.anyMatch(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE::equals);
		} catch (BundleException e) {
			return false; // ignore
		}
	}

	public BundleDescription addBundle(Map<String, String> manifest, File bundleLocation, long bundleId)
			throws CoreException {
		try {
			// OSGi requires a dictionary over any map
			Dictionary<String, String> dictionaryManifest = FrameworkUtil.asDictionary(manifest);
			BundleDescription descriptor = stateObjectFactory.createBundleDescription(fState, dictionaryManifest,
					bundleLocation.getAbsolutePath(), bundleId == -1 ? getNextId() : bundleId);
			// new bundle
			if (bundleId == -1 || !fState.updateBundle(descriptor)) {
				fState.addBundle(descriptor);
			}
			return descriptor;
		} catch (BundleException e) {
			// A stack trace isn't helpful here, but need to list the plug-in
			// location causing the issue
			MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0,
					NLS.bind(UtilMessages.ErrorReadingManifest, bundleLocation.toString()), null);
			status.add(Status.error(e.getMessage()));
			throw new CoreException(status);
		} catch (IllegalArgumentException e) {
		}
		return null;
	}

	public BundleDescription addBundle(File bundleLocation, long bundleId) throws CoreException {
		Map<String, String> manifest = ManifestUtils.loadManifest(bundleLocation);
		return addBundle(bundleLocation, bundleId, manifest);
	}

	private BundleDescription addBundle(File bundleLocation, long bundleId, Map<String, String> manifest)
			throws CoreException {
		// update for development mode
		TargetWeaver.weaveManifest(manifest, bundleLocation);

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

	public StateDelta resolveState(boolean incremental) {
		return internalResolveState(incremental);
	}

	/**
	 * Resolves the state incrementally based on the given bundle names.
	 *
	 * @return state delta
	 */
	public StateDelta resolveState(String[] symbolicNames) {
		if (initializePlatformProperties()) {
			return fState.resolve(false);
		}
		List<BundleDescription> bundles = new ArrayList<>();
		for (String symbolicName : symbolicNames) {
			BundleDescription[] descriptions = fState.getBundles(symbolicName);
			Collections.addAll(bundles, descriptions);
		}
		return fState.resolve(bundles.toArray(new BundleDescription[bundles.size()]));
	}

	private synchronized StateDelta internalResolveState(boolean incremental) {
		boolean fullBuildRequired = initializePlatformProperties();
		return fState.resolve(incremental && !fullBuildRequired);
	}

	protected boolean initializePlatformProperties() {
		if (fExecutionEnvironments == null && !fNoProfile) {
			setExecutionEnvironments();
		}

		if (fEEListChanged) {
			fEEListChanged = false;
			var properties = TargetPlatformHelper.getPlatformProperties(fExecutionEnvironments, this);
			return fState.setPlatformProperties(properties);
		}
		return false;
	}

	static {
		// Listen to changes in the available VMInstalls and
		// ExecutionEnvironment defaults
		@SuppressWarnings("restriction")
		String nodeQualifier = org.eclipse.jdt.internal.launching.LaunchingPlugin.ID_PLUGIN;
		IEclipsePreferences launchingNode = InstanceScope.INSTANCE.getNode(nodeQualifier);
		launchingNode.addPreferenceChangeListener(e -> {
			if (e.getKey().equals("org.eclipse.jdt.launching.PREF_DEFAULT_ENVIRONMENTS_XML")) { //$NON-NLS-1$
				Object oldValue = e.getOldValue() == null ? "" : e.getOldValue(); //$NON-NLS-1$
				Object newValue = e.getNewValue() == null ? "" : e.getNewValue(); //$NON-NLS-1$
				if (!oldValue.equals(newValue)) {
					triggerSystemPackagesReload();
				}
			}
		});
		JavaRuntime.addVMInstallChangedListener(new IVMInstallChangedListener() {
			@Override
			public void vmRemoved(IVMInstall vm) {
				triggerSystemPackagesReload();
			}

			@Override
			public void vmChanged(PropertyChangeEvent event) {
				triggerSystemPackagesReload();
			}

			@Override
			public void vmAdded(IVMInstall vm) {
				triggerSystemPackagesReload();
			}

			@Override
			public void defaultVMInstallChanged(IVMInstall previous, IVMInstall current) {
				triggerSystemPackagesReload();
			}
		});
	}

	public static void triggerSystemPackagesReload() {
		final String jobFamily = "pde.internal.ReresolveStateAfterVMorEEchanges"; //$NON-NLS-1$
		Job.getJobManager().cancel(jobFamily);
		WorkspaceJob job = new WorkspaceJob("Re-resolve Target state after VM-Install or EE change") { //$NON-NLS-1$
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				// The list of EEs has changed: re-read all available
				// VM-installs/EEs and re-resolve state with new properties
				return reloadSystemPackagesIntoState();
			}

			@Override
			public boolean belongsTo(Object family) {
				return jobFamily.equals(family);
			}
		};
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.schedule(200); // Small delay to bulk-handle multiple changes
	}

	// Visible for testing only
	public static IStatus reloadSystemPackagesIntoState() {
		MinimalState state = PDECore.getDefault().getModelManager().getState();
		if (state.fNoProfile) {
			return Status.OK_STATUS;
		}
		state.fEEListChanged = true;
		StateDelta delta = state.internalResolveState(true);
		if (delta.getChanges().length == 0) {
			return Status.OK_STATUS;
		}
		// Perform PDE-Manifest build, to re-validate all Manifests
		MultiStatus status = new MultiStatus(MinimalState.class, 0, "Reload of JRE system-packages encountered issues"); //$NON-NLS-1$
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (PDEBuilderHelper.hasManifestBuilder(project)) {
				try {
					project.build(IncrementalProjectBuilder.FULL_BUILD, PluginProject.MANIFEST_BUILDER_ID, null, null);
				} catch (CoreException e) { // ignore
					status.add(e.getStatus());
				}
			}
		}
		return status;
	}

	public void removeBundleDescription(BundleDescription description) {
		if (description != null) {
			fState.removeBundle(description);
		}
	}

	public void updateBundleDescription(BundleDescription description) {
		if (description != null) {
			fState.updateBundle(description);
		}
	}

	public State getState() {
		return fState;
	}

	private void setExecutionEnvironments() {
		List<String> knownExecutionEnviroments = TargetPlatformHelper.getKnownExecutionEnvironments();
		if (knownExecutionEnviroments.isEmpty()) {
			String jreProfile = System.getProperty("pde.jreProfile"); //$NON-NLS-1$
			if (jreProfile != null && !jreProfile.isEmpty() && "none".equals(jreProfile)) { //$NON-NLS-1$
				fNoProfile = true;
			}
		}
		if (!fNoProfile) {
			fExecutionEnvironments = Collections.unmodifiableSet(new LinkedHashSet<>(knownExecutionEnviroments));
		}
		fEEListChanged = true; // always indicate the list has changed
	}

	/** Returns an ordered Set of known/supported execution environments */
	public Set<String> getfProvidedExecutionEnvironments() {
		return fExecutionEnvironments; // TODO: use SequencedSet once available
	}

	public void addBundleDescription(BundleDescription toAdd) {
		if (toAdd != null) {
			fState.addBundle(toAdd);
		}
	}

	public long getNextId() {
		return ++fId;
	}

	public String getSystemBundle() {
		return fSystemBundle;
	}

}
