/*******************************************************************************
 * Copyright (c) 2007, 2023 IBM Corporation and others.
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
 *     Manumitting Technologies Inc - bug 324310
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallChangedListener;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.PropertyChangeEvent;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jdt.launching.environments.ExecutionEnvironmentDescription;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.pde.api.tools.internal.AnyValue;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.CoreMessages;
import org.eclipse.pde.api.tools.internal.builder.ApiAnalysisBuilder.ApiAnalysisJob;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.internal.core.BuildDependencyCollector;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Implementation of an {@link IApiBaseline}
 *
 * @since 1.0
 */
public class ApiBaseline extends ApiElement implements IApiBaseline, IVMInstallChangedListener {

	/**
	 * Empty array of component
	 */
	private static final IApiComponent[] EMPTY_COMPONENTS = new IApiComponent[0];

	/**
	 * OSGi bundle state
	 */
	private volatile State fState;

	/**
	 * Execution environment identifier
	 */
	private String fExecutionEnvironment;

	/**
	 * Components representing the system library
	 */
	private final List<IApiComponent> fSystemLibraryComponentList;

	/**
	 * Whether an execution environment should be automatically resolved as API
	 * components are added.
	 */
	private boolean fAutoResolve = false;

	/**
	 * Contains the location of the baseline if the baseline was created with a
	 * location.
	 */
	private String fLocation;
	/**
	 * Execution environment status
	 */
	private IStatus fEEStatus = null;

	/**
	 * Constant to match any value for ws, os, arch.
	 */
	private final AnyValue ANY_VALUE = new AnyValue();

	/**
	 * Cache of resolved packages.
	 * <p>
	 * Map of <code>PackageName -> Map(componentName -> IApiComponent[])</code>
	 * </p>
	 * For each package the cache contains a map of API components that provide that
	 * package, by source component name (including the <code>null</code> component
	 * name). This map can be updated on the fly on changes in the workspave.
	 */
	private final Map<String, Map<IApiComponent, IApiComponent[]>> fComponentsProvidingPackageCache;

	/**
	 * Maps component id's to components.
	 * <p>
	 * Map of <code>componentId -> {@link IApiComponent}</code>
	 * </p>
	 * This map is not supposed to be modified except on creation / disposal.
	 */
	private final Map<String, IApiComponent> fComponentsById;
	/**
	 * Maps component id's to all components sorted from higher to lower version.
	 * This map is not supposed to be modified except on creation / disposal.
	 */
	private final Map<String, Set<IApiComponent>> fAllComponentsById;
	/**
	 * Maps project name's to components.
	 * <p>
	 * Map of <code>project name -> {@link IApiComponent}</code>
	 * </p>
	 * This map is not supposed to be modified except on creation / disposal.
	 */
	private final Map<String, IApiComponent> fComponentsByProjectNames;
	/**
	 * Cache of system package names
	 */
	private volatile Set<String> fSystemPackageNames;

	/**
	 * The VM install this baseline is bound to for system libraries or
	 * <code>null</code>. Only used in the IDE when OSGi is running.
	 */
	private IVMInstall fVMBinding;

	private volatile boolean disposed;

	private volatile boolean restored;

	/**
	 * Constructs a new API baseline with the given name.
	 *
	 * @param name baseline name
	 */
	public ApiBaseline(String name) {
		super(null, IApiElement.BASELINE, name);
		fComponentsProvidingPackageCache = new ConcurrentHashMap<>(8);
		fSystemLibraryComponentList = new CopyOnWriteArrayList<>();
		fComponentsById = new ConcurrentHashMap<>();
		fAllComponentsById = new ConcurrentHashMap<>();
		fComponentsByProjectNames = new ConcurrentHashMap<>();
		fAutoResolve = true;
		fEEStatus = Status.error(CoreMessages.ApiBaseline_0);
	}

	/**
	 * Constructs a new API baseline with the given attributes.
	 *
	 * @param name          baseline name
	 * @param eeDescription execution environment description file
	 * @throws CoreException if unable to create a baseline with the given
	 *                       attributes
	 */
	public ApiBaseline(String name, File eeDescription) throws CoreException {
		this(name, eeDescription, null);
	}

	/**
	 * Constructs a new API baseline with the given attributes.
	 *
	 * @param name          baseline name
	 * @param eeDescription execution environment description file
	 * @param location      the given baseline location
	 * @throws CoreException if unable to create a baseline with the given
	 *                       attributes
	 */
	public ApiBaseline(String name, File eeDescription, String location) throws CoreException {
		this(name);
		if (eeDescription != null) {
			fAutoResolve = false;
			ExecutionEnvironmentDescription ee = new ExecutionEnvironmentDescription(eeDescription);
			initialize(ee);
			fEEStatus = Status.OK_STATUS;
		}
		this.fLocation = location;
	}


	/**
	 * Initializes this baseline to resolve in the execution environment
	 * associated with the given description.
	 *
	 * @param ee execution environment description
	 * @throws CoreException if unable to initialize based on the given id
	 */
	private void initialize(ExecutionEnvironmentDescription ee) throws CoreException {
		Properties properties = null;
		String environmentId = ee.getProperty(ExecutionEnvironmentDescription.CLASS_LIB_LEVEL);
		if (ApiPlugin.isRunningInFramework()) {
			properties = getJavaProfileProperties(environmentId);
			if (properties == null) {
				// Java10 onwards, we take profile via this method
				@SuppressWarnings("restriction")
				IExecutionEnvironment ev = org.eclipse.jdt.internal.launching.environments.EnvironmentsManager
						.getDefault().getEnvironment(environmentId);
				properties = ev.getProfileProperties();

			}
		} else {
			properties = Util.getEEProfile(environmentId);
		}
		if (properties == null) {
			abort("Unknown execution environment: " + environmentId, null); //$NON-NLS-1$
		} else {
			initialize(properties, ee);
		}
	}

	private static Properties getPropertiesFromURL(URL profileURL) {
		try {
			URL resolvedURL = FileLocator.resolve(profileURL);
			URLConnection openConnection = resolvedURL.openConnection();
			openConnection.setUseCaches(false);
			try (InputStream is = openConnection.getInputStream()) {
				if (is != null) {
					Properties profile = new Properties();
					profile.load(is);
					return profile;
				}
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
		return null;
	}

	/**
	 * Returns the property file for the given environment or <code>null</code>.
	 *
	 * @param ee execution environment symbolic name
	 * @return properties file or <code>null</code> if none
	 */
	public static Properties getJavaProfileProperties(String ee) {
		Bundle osgiBundle = Platform.getBundle("org.eclipse.osgi"); //$NON-NLS-1$
		if (osgiBundle == null) {
			return null;
		}
		URL profileURL = osgiBundle.getEntry(ee.replace('/', '_') + ".profile"); //$NON-NLS-1$
		if (profileURL != null) {
			return getPropertiesFromURL(profileURL);
		}
		return null;
	}


	/**
	 * Initializes this baseline from the given properties.
	 *
	 * @param profile     OGSi profile properties
	 * @param description execution environment description
	 * @throws CoreException if unable to initialize
	 */
	@SuppressWarnings("deprecation")
	private synchronized void initialize(Properties profile, ExecutionEnvironmentDescription description)
			throws CoreException {
		String environmentId = description.getProperty(ExecutionEnvironmentDescription.CLASS_LIB_LEVEL);
		IExecutionEnvironment ee = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(environmentId);
		String value = TargetPlatformHelper.getSystemPackages(ee, profile);
		String[] systemPackages = null;
		if (value != null) {
			systemPackages = value.split(","); //$NON-NLS-1$
		}
		if (!(this instanceof WorkspaceBaseline)) {
			Dictionary<String, Object> dictionary = new Hashtable<>();
			if (value != null) {
				dictionary.put(Constants.FRAMEWORK_SYSTEMPACKAGES, value);
			}
			value = profile.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
			if (value != null) {
				dictionary.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, value);
			}
			fExecutionEnvironment = profile.getProperty("osgi.java.profile.name"); //$NON-NLS-1$
			if (fExecutionEnvironment == null) {
				// Java 10 onwards, profile id is same as class lib level.
				String id = description.getProperty(ExecutionEnvironmentDescription.CLASS_LIB_LEVEL);
				fExecutionEnvironment = id;
				if (fExecutionEnvironment == null) {
					abort("Profile file missing 'osgi.java.profile.name'", null); //$NON-NLS-1$
				}
			}
			dictionary.put("osgi.os", ANY_VALUE); //$NON-NLS-1$
			dictionary.put("osgi.arch", ANY_VALUE); //$NON-NLS-1$
			dictionary.put("osgi.ws", ANY_VALUE); //$NON-NLS-1$
			dictionary.put("osgi.nl", ANY_VALUE); //$NON-NLS-1$

			getState().setPlatformProperties(dictionary);
		}
		// clean up previous system library
		for (IApiComponent comp : fSystemLibraryComponentList) {
			fComponentsById.remove(comp.getSymbolicName());
		}
		if (fSystemPackageNames != null) {
			fSystemPackageNames.clear();
			fSystemPackageNames = null;
		}
		clearComponentsCache();
		// set new system library
		SystemLibraryApiComponent fSystemLibraryComponent = new SystemLibraryApiComponent(this, description, systemPackages);
		addComponent(fSystemLibraryComponent);
		fSystemLibraryComponentList.add(fSystemLibraryComponent);
	}



	/**
	 * Clears the package -> components cache
	 */
	private void clearComponentsCache() {
		fComponentsProvidingPackageCache.clear();
	}

	/**
	 * Adds an {@link IApiComponent} to the fComponentsById mapping
	 */
	protected void addComponent(IApiComponent component) {
		if (isDisposed() || component == null) {
			return;
		}

		IApiComponent comp = fComponentsById.get(component.getSymbolicName());

		// if more than 1 components, store all of them
		if (comp != null) {
			if (fAllComponentsById.containsKey(component.getSymbolicName())) {
				Set<IApiComponent> allComponents = fAllComponentsById.get(component.getSymbolicName());
				if (!allComponents.contains(component)) {
					allComponents.add(component);
				}
			} else {
				TreeSet<IApiComponent> allComponents = new TreeSet<>(
						(comp1, comp2) -> {
					if (comp2.getVersion().equals(comp1.getVersion())) {
						if (comp2.getVersion().contains("JavaSE")) { //$NON-NLS-1$
							ApiPlugin.logInfoMessage("Multiple locations for the same Java = " //$NON-NLS-1$
									+ comp1.getLocation() + comp2.getLocation());
						}
						return 0;
					}
					return new Version(comp2.getVersion()).compareTo(new Version(comp1.getVersion()));
				});
				allComponents.add(comp);
				allComponents.add(component);
				fAllComponentsById.put(component.getSymbolicName(), allComponents);
			}
		}

		fComponentsById.put(component.getSymbolicName(), component);
		if (component instanceof ProjectComponent projectApiComponent) {
			fComponentsByProjectNames.put(projectApiComponent.getJavaProject().getProject().getName(), component);
		}
	}

	@Override
	public void addApiComponents(IApiComponent[] components) throws CoreException {
		if (isDisposed()) {
			return;
		}
		HashSet<String> ees = new HashSet<>();
		for (IApiComponent apiComponent : components) {
			BundleComponent component = (BundleComponent) apiComponent;
			if (component.isSourceComponent()) {
				continue;
			}
			BundleDescription description = component.getBundleDescription();
			getState().addBundle(description);
			addComponent(component);
			ees.addAll(Arrays.asList(component.getExecutionEnvironments()));
		}
		resolveSystemLibrary(ees);
		getState().resolve();
	}

	/**
	 * Resolves and initializes the system library to use based on API component
	 * requirements. Only works when running in the framework. Has no effect if
	 * not running in the framework.
	 */
	protected void resolveSystemLibrary(HashSet<String> ees) {
		if (ApiPlugin.isRunningInFramework() && fAutoResolve) {
			IStatus error = null;
			IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
			Map<IVMInstall, Set<String>> vmToEEs = new HashMap<>();
			for (String ee : ees) {
				IExecutionEnvironment environment = manager.getEnvironment(ee);
				if (environment != null) {
					IVMInstall[] compatibleVMs = environment.getCompatibleVMs();
					for (IVMInstall vm : compatibleVMs) {
						vmToEEs.computeIfAbsent(vm, m -> new HashSet<>()).add(ee);
					}
				}
			}
			// select VM that is compatible with most required environments
			// keep list of all VMs
			List<IVMInstall> allVMInstalls = new ArrayList<>(vmToEEs.keySet());
			String systemEE = null;
			if (!allVMInstalls.isEmpty()) {
				for (IVMInstall iVMInstall : allVMInstalls) {
					// find the EE this VM is strictly compatible with
					IExecutionEnvironment[] environments = manager.getExecutionEnvironments();
					for (IExecutionEnvironment environment : environments) {
						if (environment.isStrictlyCompatible(iVMInstall)) {
							systemEE = environment.getId();
							break;
						}
					}
					if (systemEE == null) {
						// https://bugs.eclipse.org/bugs/show_bug.cgi?id=383261
						// we don't need to compute anything here, in all cases
						// if
						// we fail to find a compatible EE, fall back to highest
						// known.
						systemEE = "JavaSE-" + JavaCore.latestSupportedJavaVersion(); //$NON-NLS-1$
					}
					// only update if different from current or missing VM
					// binding
					if (!systemEE.equals(getExecutionEnvironment()) || fVMBinding == null) {
						try {
							File file = Util.createEEFile(iVMInstall, systemEE);
							JavaRuntime.addVMInstallChangedListener(this);
							fVMBinding = iVMInstall;
							ExecutionEnvironmentDescription ee = new ExecutionEnvironmentDescription(file);
							initialize(ee);
							file.delete();
						} catch (CoreException | IOException e) {
							error = Status.error(CoreMessages.ApiBaseline_2, e);
						}
					}
				}
			} else {
				// no VMs match any required EE
				error = Status.error(CoreMessages.ApiBaseline_6);
			}
			if (error == null) {
				// build status for unbound required EE's
				Set<String> missing = new HashSet<>(ees);
				Set<String> covered = new HashSet<>();
				for (IVMInstall fit : allVMInstalls) {
					covered.addAll(vmToEEs.get(fit));
				}
				missing.removeAll(covered);
				if (missing.isEmpty()) {
					fEEStatus = Status.OK_STATUS;
				} else {
					MultiStatus multi = new MultiStatus(ApiPlugin.PLUGIN_ID, 0, CoreMessages.ApiBaseline_4, null);
					for (String id : missing) {
						multi.add(Status.warning(MessageFormat.format(CoreMessages.ApiBaseline_5, id)));
					}
					fEEStatus = multi;
				}
			} else {
				fEEStatus = error;
			}
		}
	}

	/**
	 * Returns true if the {@link IApiBaseline} has its information loaded
	 * (components) false otherwise. This is a handle only method that will not
	 * load information from disk.
	 *
	 * @return true if the {@link IApiBaseline} has its information loaded
	 *         (components) false otherwise.
	 */
	public boolean peekInfos() {
		return !fComponentsById.isEmpty();
	}

	@Override
	public IApiComponent[] getApiComponents() {
		loadBaselineInfos();
		return getAlreadyLoadedApiComponents();
	}

	protected IApiComponent[] getAlreadyLoadedApiComponents() {
		if (disposed) {
			return EMPTY_COMPONENTS;
		}
		Collection<IApiComponent> values = fComponentsById.values();
		return values.toArray(new IApiComponent[values.size()]);
	}

	@Override
	public IApiComponent[] resolvePackage(IApiComponent sourceComponent, String packageName) throws CoreException {
		if (disposed) {
			IStatus error = Status.error("Trying to use disposed baseline " + getName()); //$NON-NLS-1$
			throw new CoreException(error);
		}
		Map<IApiComponent, IApiComponent[]> componentsForPackage = fComponentsProvidingPackageCache
				.computeIfAbsent(packageName, x -> new ConcurrentHashMap<>(8));
		IApiComponent[] cachedComponents = componentsForPackage.get(sourceComponent);
		if (cachedComponents != null && cachedComponents.length > 0) {
			return cachedComponents;
		}

		// check resolvePackage0 before the system packages to avoid wrong
		// add/remove API problems - see bug 430640
		if (sourceComponent != null) {
			ArrayList<IApiComponent> componentsList = new ArrayList<>();
			resolvePackage0(sourceComponent, packageName, componentsList);
			if (componentsList.size() != 0) {
				cachedComponents = new IApiComponent[componentsList.size()];
				componentsList.toArray(cachedComponents);
			}
		}
		if (isSystemPackage(packageName)) {
			if (!fSystemLibraryComponentList.isEmpty()) {
				if (cachedComponents == null) {
					cachedComponents = fSystemLibraryComponentList.toArray(new IApiComponent[] {});
				} else {
					List<IApiComponent> list = new ArrayList<>(Arrays.asList(cachedComponents));
					list.addAll(fSystemLibraryComponentList);
					cachedComponents = list.toArray(new IApiComponent[] {});
				}
			}
		}

		if (cachedComponents == null) {
			cachedComponents = EMPTY_COMPONENTS;
		}
		if (cachedComponents.length == 0) {
			return EMPTY_COMPONENTS;
		}
		componentsForPackage.put(sourceComponent, cachedComponents);
		return cachedComponents;
	}

	/**
	 * Resolves the listing of {@link IApiComponent}s that export the given
	 * package name. The collection of {@link IApiComponent}s is written into
	 * the specified list <code>componentList</code>
	 */
	private void resolvePackage0(IApiComponent component, String packageName, List<IApiComponent> componentsList)
			throws CoreException {
		if (component instanceof BundleComponent) {
			BundleDescription bundle = ((BundleComponent) component).getBundleDescription();
			if (bundle != null) {
				StateHelper helper = getState().getStateHelper();
				ExportPackageDescription[] visiblePackages = helper.getVisiblePackages(bundle);
				for (ExportPackageDescription pkg : visiblePackages) {
					String pkgName = pkg.getName();
					if (pkgName.equals(".")) { //$NON-NLS-1$
						// translate . to default package
						pkgName = Util.DEFAULT_PACKAGE_NAME;
					}
					if (packageName.equals(pkgName)) {
						BundleDescription bundleDescription = pkg.getExporter();
						IApiComponent exporter = getApiComponent(bundleDescription.getSymbolicName());
						if (exporter != null) {
							componentsList.add(exporter);
						}
					}
				}
				if (component.isFragment()) {
					// a fragment can see all the packages from the host
					HostSpecification host = bundle.getHost();
					BundleDescription[] hosts = host.getHosts();
					for (BundleDescription currentHost : hosts) {
						IApiComponent apiComponent = component.getBaseline().getApiComponent(currentHost.getName());
						if (apiComponent != null) {
							resolvePackage0(apiComponent, packageName, componentsList);
						}
					}
				}
				// check for package within the source component
				String[] packageNames = component.getPackageNames();
				int index = Arrays.binarySearch(packageNames, packageName, null);
				if (index >= 0) {
					componentsList.add(component);
				}
			}
		}
	}

	/**
	 * Returns all of the visible dependent components from the current state
	 *
	 * @return the listing of visible dependent components to the given ones
	 */
	public IApiComponent[] getVisibleDependentComponents(IApiComponent[] components) throws CoreException {
		ArrayList<BundleDescription> bundles = getBundleDescriptions(components);
		BundleDescription[] descs = getState().getStateHelper().getDependentBundles(bundles.toArray(new BundleDescription[bundles.size()]));
		HashSet<BundleDescription> visible = new HashSet<>();
		ExportPackageDescription[] packages = null;
		for (BundleDescription desc : descs) {
			packages = getState().getStateHelper().getVisiblePackages(desc);
			for (ExportPackageDescription package1 : packages) {
				if (bundles.contains(package1.getSupplier())) {
					visible.add(desc);
				}
			}
		}
		return getApiComponents(visible);
	}

	/**
	 * Returns whether the specified package is supplied by the system library.
	 *
	 * @param packageName package name
	 * @return whether the specified package is supplied by the system library
	 */
	private boolean isSystemPackage(String packageName) {
		if (packageName.startsWith("java.")) { //$NON-NLS-1$
			return true;
		}
		Set<String> systemPackageNames = fSystemPackageNames;
		if (systemPackageNames == null) {
			synchronized (this) {
				ExportPackageDescription[] systemPackages = getState().getSystemPackages();
				systemPackageNames = new HashSet<>(systemPackages.length);
				for (ExportPackageDescription systemPackage : systemPackages) {
					systemPackageNames.add(systemPackage.getName());
				}
				fSystemPackageNames = systemPackageNames;
			}
		}
		return systemPackageNames.contains(packageName);
	}

	/**
	 * @return the OSGi state for this {@link IApiBaseline}
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public State getState() {
		if (disposed) {
			return fState;
		}
		if (fState == null) {
			synchronized (this) {
				fState = StateObjectFactory.defaultFactory.createState(true);
			}
		}
		return fState;
	}

	@Override
	public IApiComponent getApiComponent(String id) {
		loadBaselineInfos();
		if (disposed) {
			return null;
		}
		return fComponentsById.get(id);
	}

	@Override
	public Set<IApiComponent> getAllApiComponents(String id) {
		loadBaselineInfos();
		if (disposed) {
			return Collections.emptySet();
		}
		Set<IApiComponent> set = fAllComponentsById.get(id);
		if (set == null) {
			return Collections.emptySet();
		}
		return set;
	}

	@Override
	public String getExecutionEnvironment() {
		return fExecutionEnvironment;
	}

	/**
	 * Loads the information from the *.profile file the first time the baseline
	 * is accessed
	 */
	private void loadBaselineInfos() {
		if (disposed || restored) {
			return;
		}
		ApiBaselineManager manager = ApiBaselineManager.getManager();
		if (!fComponentsById.isEmpty() && manager.isBaselineLoaded(this)) {
			return;
		}
		if (disposed || restored) {
			return;
		}
		try {
			manager.loadBaselineInfos(this);
		} catch (CoreException ce) {
			ApiPlugin.log(ce);
		}
	}

	/**
	 * Restore a baseline from the given input stream (persisted baseline).
	 *
	 * @param stream the given input stream, will be closed by caller
	 * @throws CoreException if unable to restore the baseline
	 */
	public void restoreFrom(InputStream stream) throws CoreException {
		if (disposed || restored) {
			return;
		}
		IApiComponent[] components = ApiBaselineManager.getManager().readBaselineComponents(this, stream);
		if (components == null) {
			restored = true;
			return;
		}
		synchronized (this) {
			if (disposed || restored) {
				for (IApiComponent component : components) {
					component.dispose();
				}
				return;
			}
			this.addApiComponents(components);
			restored = true;
		}
	}


	/**
	 * Returns all errors in the state.
	 *
	 * @return state errors
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public ResolverError[] getErrors() {
		List<ResolverError> errs = null;
		BundleDescription[] bundles = getState().getBundles();
		for (BundleDescription bundle : bundles) {
			ResolverError[] errors = getState().getResolverErrors(bundle);
			for (ResolverError error : errors) {
				if (errs == null) {
					errs = new ArrayList<>();
				}
				errs.add(error);
			}
		}
		if (errs != null) {
			return errs.toArray(new ResolverError[errs.size()]);
		}
		return null;
	}

	@Override
	public void setName(String name) {
		super.setName(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IApiBaseline baseline) {
			return this.getName().equals(baseline.getName());
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}

	@Override
	public void dispose() {
		if (fState == null) {
			// already disposed or nothing to dispose
			return;
		}
		doDispose();
		fState = null;
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	/**
	 * performs the actual dispose of mappings and cached elements
	 */
	protected void doDispose() {
		if (disposed) {
			return;
		}
		IApiComponent[] components;
		synchronized (this) {
			components = getAlreadyLoadedApiComponents();
			disposed = true;
		}
		clearCachedElements();
		if (ApiPlugin.isRunningInFramework()) {
			JavaRuntime.removeVMInstallChangedListener(this);
		}
		for (IApiComponent component2 : components) {
			component2.dispose();
		}
		clearComponentsCache();
		fComponentsById.clear();
		fAllComponentsById.clear();
		fComponentsByProjectNames.clear();
		if (fSystemPackageNames != null) {
			fSystemPackageNames.clear();
		}
		for (IApiComponent iApiComponent : fSystemLibraryComponentList) {
			iApiComponent.dispose();
		}
		fSystemLibraryComponentList.clear();
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline#close()
	 */
	@Override
	public void close() throws CoreException {
		clearCachedElements();
		IApiComponent[] components = getApiComponents();
		for (IApiComponent component2 : components) {
			component2.close();
		}
	}

	/**
	 * Clears all element information from the cache for this baseline
	 *
	 * @since 1.1
	 */
	void clearCachedElements() {
		ApiModelCache.getCache().removeElementInfo(this);
	}

	/**
	 * Returns an array of API components corresponding to the given bundle
	 * descriptions.
	 *
	 * @param bundles bundle descriptions
	 * @return corresponding API components
	 */
	private IApiComponent[] getApiComponents(Collection<BundleDescription> bundles) {
		ArrayList<IApiComponent> dependents = new ArrayList<>(bundles.size());
		for (BundleDescription bundle : bundles) {
			IApiComponent component = getApiComponent(bundle.getSymbolicName());
			if (component != null) {
				dependents.add(component);
			}
		}
		return dependents.toArray(new IApiComponent[dependents.size()]);
	}

	/**
	 * Returns an array of bundle descriptions corresponding to the given API
	 * components.
	 *
	 * @param components API components
	 * @return corresponding bundle descriptions
	 */
	private ArrayList<BundleDescription> getBundleDescriptions(IApiComponent[] components) throws CoreException {
		ArrayList<BundleDescription> bundles = new ArrayList<>(components.length);
		for (IApiComponent component : components) {
			if (component instanceof BundleComponent) {
				bundles.add(((BundleComponent) component).getBundleDescription());
			}
		}
		return bundles;
	}

	@Override
	public IApiComponent[] getPrerequisiteComponents(IApiComponent[] components) throws CoreException {
		ArrayList<BundleDescription> bundles = getBundleDescriptions(components);
		return getApiComponents(BuildDependencyCollector.collectBuildRelevantDependencies(bundles));
	}

	/**
	 * Clear cached settings for the given package.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	public void clearPackage(String packageName) {
		fComponentsProvidingPackageCache.remove(packageName);
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public IStatus getExecutionEnvironmentStatus() {
		return fEEStatus;
	}

	@Override
	public void defaultVMInstallChanged(IVMInstall previous, IVMInstall current) {
		//
	}

	@Override
	public void vmAdded(IVMInstall vm) {
		if (!(vm instanceof VMStandin)) {
			// there may be a better fit for VMs/EEs
			rebindVM();
		}
	}

	@Override
	public void vmChanged(PropertyChangeEvent event) {
		if (!(event.getSource() instanceof VMStandin)) {
			String property = event.getProperty();
			if (IVMInstallChangedListener.PROPERTY_INSTALL_LOCATION.equals(property) || IVMInstallChangedListener.PROPERTY_LIBRARY_LOCATIONS.equals(property)) {
				rebindVM();
			}
		}
	}

	/**
	 * Re-binds the VM this baseline is bound to.
	 */
	private void rebindVM() {
		Job.createSystem("Rebinding JVM", monitor -> { //$NON-NLS-1$
			try {
				// Let all the already running job finish first, to avoid errors
				Job.getJobManager().join(ApiAnalysisJob.class, monitor);
			} catch (OperationCanceledException | InterruptedException e) {
				ApiPlugin.log("Interrupted while rebinding JVM", e); //$NON-NLS-1$
				return;
			}
			fVMBinding = null;
			IApiComponent[] components = getApiComponents();
			HashSet<String> ees = new HashSet<>();
			for (IApiComponent component2 : components) {
				ees.addAll(Arrays.asList(component2.getExecutionEnvironments()));
			}
			resolveSystemLibrary(ees);
		}).schedule();
	}

	@Override
	public void vmRemoved(IVMInstall vm) {
		if (vm.equals(fVMBinding)) {
			rebindVM();
		}
	}

	@Override
	public String getLocation() {
		return this.fLocation;
	}

	@Override
	public void setLocation(String location) {
		this.fLocation = location;
	}

	@Override
	public IApiComponent getApiComponent(IProject project) {
		loadBaselineInfos();
		if (disposed) {
			return null;
		}
		return fComponentsByProjectNames.get(project.getName());
	}
}
