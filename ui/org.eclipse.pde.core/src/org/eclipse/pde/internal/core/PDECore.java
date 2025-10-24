/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IBundleClasspathResolver;
import org.eclipse.pde.core.IClasspathContributor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.bnd.BndResourceChangeListener;
import org.eclipse.pde.internal.core.bnd.BndWorkspaceServiceFactory;
import org.eclipse.pde.internal.core.builders.FeatureRebuilder;
import org.eclipse.pde.internal.core.builders.PluginRebuilder;
import org.eclipse.pde.internal.core.project.BundleProjectService;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.clipboard.Clipboard;

public class PDECore extends Plugin implements DebugOptionsListener {

	public static final String PLUGIN_ID = "org.eclipse.pde.core"; //$NON-NLS-1$

	public static final IPath REQUIRED_PLUGINS_CONTAINER_PATH = IPath.fromOSString(PLUGIN_ID + ".requiredPlugins"); //$NON-NLS-1$
	public static final IPath JAVA_SEARCH_CONTAINER_PATH = IPath.fromOSString(PLUGIN_ID + ".externalJavaSearch"); //$NON-NLS-1$
	public static final IPath JRE_CONTAINER_PATH = IPath.fromOSString(JavaRuntime.JRE_CONTAINER);

	public static final String BINARY_PROJECT_VALUE = "binary"; //$NON-NLS-1$
	public static final String BINARY_REPOSITORY_PROVIDER = PLUGIN_ID + "." + "BinaryRepositoryProvider"; //$NON-NLS-1$ //$NON-NLS-2$

	public static final QualifiedName EXTERNAL_PROJECT_PROPERTY = new QualifiedName(PLUGIN_ID, "imported"); //$NON-NLS-1$
	public static final QualifiedName TOUCH_PROJECT = new QualifiedName(PLUGIN_ID, "TOUCH_PROJECT"); //$NON-NLS-1$
	public static final QualifiedName BND_CLASSPATH_INSTRUCTION_FILE = new QualifiedName(PLUGIN_ID,
			"BND_CLASSPATH_PROJECT"); //$NON-NLS-1$

	public static final QualifiedName SCHEMA_PREVIEW_FILE = new QualifiedName(PLUGIN_ID, "SCHEMA_PREVIEW_FILE"); //$NON-NLS-1$

	public static boolean DEBUG_CLASSPATH = false;
	public static boolean DEBUG_MODEL = false;
	public static boolean DEBUG_TARGET_PROFILE = false;
	public static boolean DEBUG_VALIDATION = false;
	public static boolean DEBUG_STATE = false;
	public static DebugTrace TRACE;
	private static final String DEBUG = "/debug"; //$NON-NLS-1$

	public static final String KEY_DEBUG_STATE = DEBUG + "/state"; //$NON-NLS-1$
	public static final String KEY_DEBUG_VALIDATION = DEBUG + "/validation"; //$NON-NLS-1$
	private static final String DEBUG_FLAG = PLUGIN_ID + DEBUG;
	private static final String CLASSPATH_DEBUG = PLUGIN_ID + "/classpath"; //$NON-NLS-1$
	private static final String MODEL_DEBUG = PLUGIN_ID + "/model"; //$NON-NLS-1$
	private static final String TARGET_PROFILE_DEBUG = PLUGIN_ID + "/target/profile"; //$NON-NLS-1$
	private static final String VALIDATION_DEBUG = PLUGIN_ID + KEY_DEBUG_VALIDATION;
	private static final String STATE_DEBUG = PLUGIN_ID + KEY_DEBUG_STATE;

	// Shared instance
	private static PDECore inst;

	private static PDEExtensionRegistry fExtensionRegistry = null;

	/**
	 * The singleton preference manager instance
	 *
	 * @since 3.5
	 */
	private static PDEPreferencesManager fPreferenceManager;

	private Map<String, List<IPluginModelBase>> fHostPlugins;

	public static PDECore getDefault() {
		return inst;
	}

	/**
	 * Returns the singleton instance of if the {@link PDEPreferencesManager}
	 * for this bundle
	 *
	 * @return the preference manager for this bundle
	 *
	 * @since 3.5
	 */
	public synchronized PDEPreferencesManager getPreferencesManager() {
		if (fPreferenceManager == null) {
			fPreferenceManager = new PDEPreferencesManager(PLUGIN_ID);
		}
		return fPreferenceManager;
	}

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public static void log(IStatus status) {
		if (status != null) {
			ResourcesPlugin.getPlugin().getLog().log(status);
		}
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException || e.getMessage() != null) {
			status = Status.error(e.getMessage(), e);
		}
		log(status);
	}

	public static void logErrorMessage(String message) {
		log(Status.error(message));
	}

	public static void logException(Throwable e) {
		logException(e, null);
	}

	public static void logException(Throwable e, String message) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException) {
			status = Status.error(message, e);
		} else {
			if (message == null) {
				message = e.getMessage();
			}
			if (message == null) {
				message = e.toString();
			}
			status = Status.error(message, e);
		}
		log(status);
	}

	private FeatureModelManager fFeatureModelManager;

	private TargetDefinitionManager fTargetProfileManager;

	// Schema registry
	private SchemaRegistry fSchemaRegistry;

	private SourceLocationManager fSourceLocationManager;
	private JavadocLocationManager fJavadocLocationManager;
	private SearchablePluginsManager fSearchablePluginsManager;
	private ClasspathContainerResolverManager fClasspathContainerResolverManager;

	// Tracing options manager
	private TracingOptionsManager fTracingOptionsManager;
	private BundleContext fBundleContext;
	private JavaElementChangeListener fJavaElementChangeListener;

	private FeatureRebuilder fFeatureRebuilder;

	private PluginRebuilder fPluginRebuilder;

	/**
	 * Target platform service.
	 */
	private ServiceRegistration<ITargetPlatformService> fTargetPlatformService;

	/**
	 * Bundle project service.
	 */
	private ServiceRegistration<IBundleProjectService> fBundleProjectService;

	private BndResourceChangeListener bndResourceChangeListener;

	private ServiceTracker<IClasspathContributor, IClasspathContributor> classpathContributorServiceTracker;

	private ServiceTracker<Clipboard, Clipboard> clipBoardsServiceTracker;

	private ServiceTracker<RepositoryListenerPlugin, RepositoryListenerPlugin> repositoryListenerServiceTracker;

	public static final Comparator<IPluginModelBase> VERSION = Comparator.comparing(p -> getOSGiVersion(p));

	public PDECore() {
		inst = this;
	}

	/**
	 * Finds plugins from the host OSGi framework, in no particular order.
	 *
	 * @param id
	 *            the bundle symbolic name to search for
	 * @return a Stream of all bundles from the hosting OSGi framework in no
	 *         particular order, if ordering matter use for example
	 *         {@link #VERSION} comparator.
	 */
	public Stream<IPluginModelBase> findPluginsInHost(String id) {
		Map<String, List<IPluginModelBase>> hostPlugins = getHostPlugins();
		if (hostPlugins == null) {
			return null;
		}
		return hostPlugins.getOrDefault(id, List.of()).stream();
	}

	private synchronized Map<String, List<IPluginModelBase>> getHostPlugins() {
		if (fHostPlugins == null) {
			fHostPlugins = new HashMap<>();

			ITargetDefinition defaultTarget = TargetPlatformService.getDefault().newDefaultTarget();
			IStatus status = defaultTarget.resolve(new NullProgressMonitor());
			if (!status.isOK()) {
				log(status);
				return null;
			}

			URI[] pluginPaths = Arrays.stream(defaultTarget.getBundles()) //
					.filter(b -> !b.isSourceBundle()) //
					.map(b -> b.getBundleInfo().getLocation()) //
					.toArray(URI[]::new);
			PDEState state = new PDEState(pluginPaths, true, false, new NullProgressMonitor());
			state.resolveState(false);

			fHostPlugins = Arrays.stream(state.getTargetModels())
					.collect(Collectors.groupingBy(p -> p.getPluginBase().getId()));
		}
		return fHostPlugins;
	}

	public PluginModelManager getModelManager() {
		return PluginModelManager.getInstance();
	}

	public synchronized TargetDefinitionManager getTargetProfileManager() {
		if (fTargetProfileManager == null) {
			fTargetProfileManager = new TargetDefinitionManager();
		}
		return fTargetProfileManager;
	}

	public synchronized FeatureModelManager getFeatureModelManager() {
		if (fFeatureModelManager == null) {
			fFeatureModelManager = new FeatureModelManager();
		}
		return fFeatureModelManager;
	}

	public JavaElementChangeListener getJavaElementChangeListener() {
		return fJavaElementChangeListener;
	}

	public synchronized SchemaRegistry getSchemaRegistry() {
		if (fSchemaRegistry == null) {
			fSchemaRegistry = new SchemaRegistry();
		}
		return fSchemaRegistry;
	}

	public synchronized PDEExtensionRegistry getExtensionsRegistry() {
		if (fExtensionRegistry == null) {
			fExtensionRegistry = new PDEExtensionRegistry();
		}
		return fExtensionRegistry;
	}

	public synchronized SourceLocationManager getSourceLocationManager() {
		if (fSourceLocationManager == null) {
			fSourceLocationManager = new SourceLocationManager();
		}
		return fSourceLocationManager;
	}

	/**
	 * Returns the singleton instance of the classpath container resolver
	 * manager used to dynamically resolve a project's classpath. Clients may
	 * contribute a {@link IBundleClasspathResolver} to the manager through the
	 * <code>org.eclipse.pde.core.bundleClasspathResolvers</code> extension.
	 *
	 * @return singleton instance of the classpath container resolver manager
	 */
	public synchronized ClasspathContainerResolverManager getClasspathContainerResolverManager() {
		if (fClasspathContainerResolverManager == null) {
			fClasspathContainerResolverManager = new ClasspathContainerResolverManager();
		}
		return fClasspathContainerResolverManager;
	}

	public synchronized JavadocLocationManager getJavadocLocationManager() {
		if (fJavadocLocationManager == null) {
			fJavadocLocationManager = new JavadocLocationManager();
		}
		return fJavadocLocationManager;
	}

	public synchronized TracingOptionsManager getTracingOptionsManager() {
		if (fTracingOptionsManager == null) {
			fTracingOptionsManager = new TracingOptionsManager();
		}
		return fTracingOptionsManager;
	}

	public synchronized SearchablePluginsManager getSearchablePluginsManager() {
		if (fSearchablePluginsManager == null) {
			fSearchablePluginsManager = new SearchablePluginsManager();
		}
		return fSearchablePluginsManager;
	}

	public boolean areModelsInitialized() {
		return getModelManager().isInitialized();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext = context;

		fJavaElementChangeListener = new JavaElementChangeListener();
		fJavaElementChangeListener.start();
		fPluginRebuilder = new PluginRebuilder();
		fPluginRebuilder.start();
		fFeatureRebuilder = new FeatureRebuilder();
		fFeatureRebuilder.start();

		fTargetPlatformService = context.registerService(ITargetPlatformService.class,
				TargetPlatformService.getDefault(), new Hashtable<>());
		fBundleProjectService = context.registerService(IBundleProjectService.class, BundleProjectService.getDefault(),
				new Hashtable<>());

		// Register the debug options listener service (tracing)
		Hashtable<String, String> props = new Hashtable<>(2);
		props.put(org.eclipse.osgi.service.debug.DebugOptions.LISTENER_SYMBOLICNAME, PDECore.PLUGIN_ID);
		context.registerService(DebugOptionsListener.class.getName(), this, props);

		// use save participant to clean orphaned profiles.
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.addSaveParticipant(PLUGIN_ID, new ISaveParticipant() {
			@Override
			public void saving(ISaveContext saveContext) {
				try {
					P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
				} catch (CoreException e) {
					getLog().warn("Can't cleanup orphaned target definition profiles, will retry later.", e); //$NON-NLS-1$
				}
			}

			@Override
			public void rollback(ISaveContext saveContext) {
			}

			@Override
			public void prepareToSave(ISaveContext saveContext) throws CoreException {
			}

			@Override
			public void doneSaving(ISaveContext saveContext) {
			}
		});
		bndResourceChangeListener = new BndResourceChangeListener();
		workspace.addResourceChangeListener(bndResourceChangeListener);
		workspace.addResourceChangeListener(ClasspathContainerState.CHANGE_LISTENER, IResourceChangeEvent.PRE_DELETE);
		fBundleContext.registerService(Workspace.class, new BndWorkspaceServiceFactory(),
				FrameworkUtil.asDictionary(Map.of(Constants.SERVICE_RANKING, -10)));
	}

	public BundleContext getBundleContext() {
		return fBundleContext;
	}

	@Override
	public void stop(BundleContext context) throws CoreException {

		if (fPreferenceManager != null) {
			fPreferenceManager.savePluginPreferences();
		}

		fJavaElementChangeListener.shutdown();
		fPluginRebuilder.stop();
		fFeatureRebuilder.stop();

		if (fSchemaRegistry != null) {
			fSchemaRegistry.shutdown();
			fSchemaRegistry = null;
		}
		if (fTargetProfileManager != null) {
			fTargetProfileManager.shutdown();
			fTargetProfileManager = null;
		}
		if (fSearchablePluginsManager != null) {
			fSearchablePluginsManager.shutdown();
			fSearchablePluginsManager = null;
		}
		if (fFeatureModelManager != null) {
			fFeatureModelManager.shutdown();
			fFeatureModelManager = null;
		}
		// always shut down extension registry before model manager (since it
		// needs data from model manager)
		if (fExtensionRegistry != null) {
			fExtensionRegistry.stop();
			fExtensionRegistry = null;
		}

		PluginModelManager.shutdownInstance();

		if (fTargetPlatformService != null) {
			fTargetPlatformService.unregister();
			fTargetPlatformService = null;
		}
		if (fBundleProjectService != null) {
			fBundleProjectService.unregister();
			fBundleProjectService = null;
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.removeSaveParticipant(PLUGIN_ID);
		workspace.removeResourceChangeListener(bndResourceChangeListener);
		workspace.removeResourceChangeListener(ClasspathContainerState.CHANGE_LISTENER);

		MinimalState.shutdown();
	}

	/**
	 * Returns a service for the specified class or <code>null</code> if none.
	 *
	 * @param serviceClass
	 *            class of service
	 * @return service service or <code>null</code> if none
	 */
	public <T> T acquireService(Class<T> serviceClass) {
		ServiceReference<T> reference = fBundleContext.getServiceReference(serviceClass);
		if (reference == null) {
			return null;
		}
		T service = fBundleContext.getService(reference);
		if (service != null) {
			fBundleContext.ungetService(reference);
		}
		return service;
	}

	@Override
	public void optionsChanged(DebugOptions options) {
		boolean DEBUG = options.getBooleanOption(DEBUG_FLAG, false);
		if (DEBUG) {
			TRACE = options.newDebugTrace(PLUGIN_ID);
		}
		DEBUG_CLASSPATH = DEBUG && options.getBooleanOption(CLASSPATH_DEBUG, false);
		DEBUG_MODEL = DEBUG && options.getBooleanOption(MODEL_DEBUG, false);
		DEBUG_TARGET_PROFILE = DEBUG && options.getBooleanOption(TARGET_PROFILE_DEBUG, false);
		DEBUG_VALIDATION = DEBUG && options.getBooleanOption(VALIDATION_DEBUG, false);
		DEBUG_STATE = DEBUG & options.getBooleanOption(STATE_DEBUG, false);
	}

	/**
	 * @return a stream of currently registered {@link IClasspathContributor}s
	 *         from the OSGi service factory
	 */
	public synchronized Stream<IClasspathContributor> getClasspathContributors() {
		if (fBundleContext == null) {
			return Stream.empty();
		}
		if (classpathContributorServiceTracker == null) {
			classpathContributorServiceTracker = new ServiceTracker<>(fBundleContext, IClasspathContributor.class,
					null);
			classpathContributorServiceTracker.open();
		}
		return classpathContributorServiceTracker.getTracked().values().stream();
	}

	public synchronized Clipboard getClipboardPlugin() {
		if (fBundleContext == null) {
			return null;
		}
		if (clipBoardsServiceTracker == null) {
			clipBoardsServiceTracker = new ServiceTracker<>(fBundleContext, Clipboard.class, null);
			clipBoardsServiceTracker.open();
		}
		return clipBoardsServiceTracker.getService();
	}

	public synchronized Stream<RepositoryListenerPlugin> getRepositoryListenerPlugins() {
		if (fBundleContext == null) {
			return Stream.empty();
		}
		if (repositoryListenerServiceTracker == null) {
			repositoryListenerServiceTracker = new ServiceTracker<>(fBundleContext, RepositoryListenerPlugin.class,
					null);
			repositoryListenerServiceTracker.open();
		}
		return repositoryListenerServiceTracker.getTracked().values().stream();

	}

	public static Version getOSGiVersion(IPluginModelBase model) {
		if (model == null) {
			return Version.emptyVersion;
		}
		BundleDescription description = model.getBundleDescription();
		if (description == null) {
			return Version.emptyVersion;
		}
		return description.getVersion();
	}
}
