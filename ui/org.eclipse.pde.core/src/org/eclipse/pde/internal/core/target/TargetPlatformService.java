/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 541067
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetEvents;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.TargetDefinitionManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Target platform service implementation.
 *
 * @since 3.5
 */
public class TargetPlatformService implements ITargetPlatformService {

	/**
	 * Service instance
	 */
	private static ITargetPlatformService fgDefault;

	/**
	 * External File Targets
	 */
	private static Map<URI, ExternalFileTargetHandle> fExtTargetHandles;

	/**
	 * The target definition currently being used as the target platform for
	 * the workspace.
	 */
	private final AtomicReference<ITargetDefinition> fWorkspaceTarget;

	/**
	 * vm arguments for default target
	 */
	private StringBuilder fVMArguments;

	private final EventDispatcher eventSendingJob;

	/**
	 * Collects target files in the workspace
	 */
	static class ResourceProxyVisitor implements IResourceProxyVisitor {

		private List<IResource> fList;

		protected ResourceProxyVisitor(List<IResource> list) {
			fList = list;
		}

		/**
		 * @see org.eclipse.core.resources.IResourceProxyVisitor#visit(org.eclipse.core.resources.IResourceProxy)
		 */
		@Override
		public boolean visit(IResourceProxy proxy) {
			if (proxy.getType() == IResource.FILE) {
				if (ICoreConstants.TARGET_FILE_EXTENSION.equalsIgnoreCase(IPath.fromOSString(proxy.getName()).getFileExtension())) {
					fList.add(proxy.requestResource());
				}
				return false;
			}
			return true;
		}
	}

	private TargetPlatformService() {
		fWorkspaceTarget = new AtomicReference<>();
		eventSendingJob = new EventDispatcher("Sending 'workspace target changed' event", TargetPlatformService.class); //$NON-NLS-1$
	}

	/**
	 * The target service should be obtained by requesting the {@link ITargetPlatformService} from OSGi. This
	 * method should only be used internally be PDE.
	 *
	 * @return The singleton implementation of this service
	 */
	public synchronized static ITargetPlatformService getDefault() {
		if (fgDefault == null) {
			fgDefault = new TargetPlatformService();
		}
		return fgDefault;
	}

	@Override
	public void deleteTarget(ITargetHandle handle) throws CoreException {
		if (handle instanceof ExternalFileTargetHandle) {
			fExtTargetHandles.remove(((ExternalFileTargetHandle) handle).getLocation());
		}
		((AbstractTargetHandle) handle).delete();
	}

	@Override
	public ITargetHandle getTarget(IFile file) {
		return new WorkspaceFileTargetHandle(file);
	}

	@Override
	public ITargetHandle getTarget(String memento) throws CoreException {
		try {
			URI uri = new URI(memento);
			String scheme = uri.getScheme();
			if (scheme != null) {
				switch (scheme) {
				case WorkspaceFileTargetHandle.SCHEME:
					return WorkspaceFileTargetHandle.restoreHandle(uri);
				case LocalTargetHandle.SCHEME:
					return LocalTargetHandle.restoreHandle(uri);
				case ExternalFileTargetHandle.SCHEME:
					return ExternalFileTargetHandle.restoreHandle(uri);
				case RemoteTargetHandle.SCHEME:
					return RemoteTargetHandle.get(memento.substring(RemoteTargetHandle.SCHEME.length() + 1));
				default:
					break;
				}
			}
		} catch (URISyntaxException e) {
			throw new CoreException(Status.error(Messages.TargetPlatformService_0, e));
		}
		throw new CoreException(
				Status.error(NLS.bind(Messages.TargetPlatformService_1, memento,
						Stream.of(WorkspaceFileTargetHandle.SCHEME, LocalTargetHandle.SCHEME,
								ExternalFileTargetHandle.SCHEME, RemoteTargetHandle.SCHEME)
								.collect(Collectors.joining(", "))), //$NON-NLS-1$
						null));
	}

	@Override
	public ITargetHandle getTarget(URI uri) {
		if (fExtTargetHandles == null) {
			fExtTargetHandles = new LinkedHashMap<>(10);
		}
		if (fExtTargetHandles.containsKey(uri)) {
			return fExtTargetHandles.get(uri);
		}
		ExternalFileTargetHandle externalTarget = new ExternalFileTargetHandle(uri);
		fExtTargetHandles.put(uri, externalTarget);
		return externalTarget;
	}

	@Override
	public ITargetHandle[] getTargets(IProgressMonitor monitor) {
		List<ITargetHandle> local = findLocalTargetDefinitions();
		List<WorkspaceFileTargetHandle> ws = findWorkspaceTargetDefinitions();
		local.addAll(ws);
		if (fExtTargetHandles != null) {
			// If an external target is inaccessible then don't show it. But keep the reference in case it becomes accessible later
			Collection<ExternalFileTargetHandle> externalTargets = fExtTargetHandles.values();
			for (ExternalFileTargetHandle target : externalTargets) {
				if (target.exists()) {
					local.add(target);
				}
			}
		} else {
			PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
			String memento = preferences.getString(ICoreConstants.WORKSPACE_TARGET_HANDLE);
			if (memento != null && memento.length() != 0 && !memento.equals(ICoreConstants.NO_TARGET)) {
				try {
					URI uri = new URI(memento);
					String scheme = uri.getScheme();
					if (ExternalFileTargetHandle.SCHEME.equals(scheme)) {
						ITargetHandle target = getTarget(uri);
						local.add(target);
					}
				} catch (URISyntaxException e) {
					// ignore
				}
			}
		}
		return local.toArray(new ITargetHandle[local.size()]);
	}

	/**
	 * Finds and returns all local target definition handles
	 *
	 * @return all local target definition handles
	 */
	private List<ITargetHandle> findLocalTargetDefinitions() {
		IPath containerPath = LocalTargetHandle.LOCAL_TARGET_CONTAINER_PATH;
		List<ITargetHandle> handles = new ArrayList<>(10);
		final File directory = containerPath.toFile();
		if (directory.isDirectory()) {
			FilenameFilter filter = (dir, name) -> dir.equals(directory) && name.endsWith(ICoreConstants.TARGET_FILE_EXTENSION);
			File[] files = directory.listFiles(filter);
			for (File file : files) {
				try {
					handles.add(LocalTargetHandle.restoreHandle(file.toURI()));
				} catch (CoreException e) {
					PDECore.log(e);
				}
			}
		}
		return handles;
	}

	/**
	 * Finds and returns all target definition handles defined by workspace files
	 *
	 * @return all target definition handles in the workspace
	 */
	private List<WorkspaceFileTargetHandle> findWorkspaceTargetDefinitions() {
		List<IResource> files = new ArrayList<>(10);
		ResourceProxyVisitor visitor = new ResourceProxyVisitor(files);
		try {
			ResourcesPlugin.getWorkspace().getRoot().accept(visitor, IResource.NONE);
		} catch (CoreException e) {
			PDECore.log(e);
			return new ArrayList<>(0);
		}
		Iterator<IResource> iter = files.iterator();
		List<WorkspaceFileTargetHandle> handles = new ArrayList<>(files.size());
		while (iter.hasNext()) {
			IFile file = (IFile) iter.next();
			handles.add(new WorkspaceFileTargetHandle(file));
		}
		return handles;
	}

	@Override
	public ITargetLocation newDirectoryLocation(String path) {
		return new DirectoryBundleContainer(path);
	}

	@Override
	public ITargetLocation newProfileLocation(String home, String configurationLocation) {
		return new ProfileBundleContainer(home, configurationLocation);
	}

	@Override
	public ITargetDefinition newTarget() {
		return new TargetDefinition(new LocalTargetHandle());
	}

	@Override
	public void saveTargetDefinition(ITargetDefinition definition) throws CoreException {
		((AbstractTargetHandle) definition.getHandle()).save(definition);
	}

	@Override
	public ITargetLocation newFeatureLocation(String home, String id, String version) {
		return new FeatureBundleContainer(home, id, version);
	}

	@Override
	public ITargetHandle getWorkspaceTargetHandle() throws CoreException {
		PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
		String memento = preferences.getString(ICoreConstants.WORKSPACE_TARGET_HANDLE);
		if (memento != null && memento.length() != 0 && !memento.equals(ICoreConstants.NO_TARGET)) {
			return getTarget(memento);
		}
		return null;
	}

	@Override
	public synchronized ITargetDefinition getWorkspaceTargetDefinition() throws CoreException {
		ITargetDefinition workspaceTarget = fWorkspaceTarget.get();
		if (workspaceTarget != null && workspaceTarget.getHandle().equals(getWorkspaceTargetHandle())) {
			return workspaceTarget;
		}

		// If no target definition has been chosen before, try using preferences
		initDefaultTargetPlatformDefinition();

		// Load and resolve
		String memento = PDECore.getDefault().getPreferencesManager().getString(ICoreConstants.WORKSPACE_TARGET_HANDLE);
		ITargetDefinition target = null;
		if (memento == null || memento.equals("") || memento.equals(ICoreConstants.NO_TARGET)) { //$NON-NLS-1$
			target = newTarget();
		} else {
			ITargetHandle handle = getTarget(memento);
			target = handle.getTargetDefinition();
		}

		setWorkspaceTargetDefinition(target, true);
		return target;
	}

	/**
	 * Updates the current stored target. Provided to allow the LoadTargetDefinitionJob
	 * to pass along a possibly resolved target rather than force it to be resolved again.
	 * This method will not update the stored {@link ICoreConstants#WORKSPACE_TARGET_HANDLE},
	 * as it should only be called from LoadTargetDefinitionJob which does additional
	 * steps to reset the target.
	 *
	 * @param target
	 *            the new workspace target definition
	 * @param asyncEvents
	 *            to notify listener asynchronously
	 */
	public void setWorkspaceTargetDefinition(ITargetDefinition target, boolean asyncEvents) {
		ITargetDefinition oldTarget = fWorkspaceTarget.getAndSet(target);
		boolean changed = !Objects.equals(oldTarget, target);
		if (changed) {
			if (asyncEvents) {
				eventSendingJob.schedule(target);
			} else {
				notifyTargetChanged(target);
			}
		}
	}

	static void notifyTargetChanged(ITargetDefinition target) {
		IEclipseContext context = EclipseContextFactory.getServiceContext(PDECore.getDefault().getBundleContext());
		IEventBroker broker = context.get(IEventBroker.class);
		if (broker != null) {
			broker.send(TargetEvents.TOPIC_WORKSPACE_TARGET_CHANGED, target);
		}
	}

	static class EventDispatcher extends Job {

		private final ConcurrentLinkedQueue<ITargetDefinition> queue;
		private final Object myFamily;

		/**
		 * @param jobName
		 *            descriptive job name
		 * @param family
		 *            non null object to control this job execution
		 **/
		public EventDispatcher(String jobName, Object family) {
			super(jobName);
			Assert.isNotNull(family);
			this.myFamily = family;
			this.queue = new ConcurrentLinkedQueue<>();
			setSystem(true);
		}

		@Override
		public boolean belongsTo(Object family) {
			return myFamily == family;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ITargetDefinition target;
			while ((target = queue.poll()) != null && !monitor.isCanceled()) {
				notifyTargetChanged(target);
			}
			if (!queue.isEmpty() && !monitor.isCanceled()) {
				// in case actions got faster scheduled then processed
				schedule();
			}
			if (monitor.isCanceled()) {
				queue.clear();
				return Status.CANCEL_STATUS;
			}
			return Status.OK_STATUS;
		}

		/**
		 * Enqueue a task asynchronously.
		 **/
		public void schedule(ITargetDefinition target) {
			queue.offer(target);
			schedule(); // will reschedule if already running
		}
	}

	/**
	 * Sets active target definition handle if not yet set. If an existing target
	 * definition corresponds to workspace target settings, it is selected as the
	 * active target. If there are no targets that correspond to workspace settings
	 * a new definition is created.
	 */
	private void initDefaultTargetPlatformDefinition() {
		PDEPreferencesManager preferenceManager = PDECore.getDefault().getPreferencesManager();
		String memento = preferenceManager.getString(ICoreConstants.WORKSPACE_TARGET_HANDLE);
		if (removeInvalidTargetMementoInPreference(preferenceManager, memento)) {
			memento = preferenceManager.getString(ICoreConstants.WORKSPACE_TARGET_HANDLE);
		}
		if (memento == null || memento.equals("")) { //$NON-NLS-1$
			try {
				if (PDECore.DEBUG_MODEL) {
					System.out.println("No target platform memento, add default target."); //$NON-NLS-1$
				}

				// Add default target
				ITargetDefinition defaultTarget = newDefaultTarget();
				defaultTarget.setName(Messages.TargetPlatformService_7);
				saveTargetDefinition(defaultTarget);


				// Set active platform
				PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
				ITargetHandle active = defaultTarget.getHandle();
				preferences.setValue(ICoreConstants.WORKSPACE_TARGET_HANDLE, active.getMemento());
			} catch (CoreException e) {
				PDECore.log(e);
			}
		}
	}

	private boolean removeInvalidTargetMementoInPreference(PDEPreferencesManager preferenceManager, String memento) {
		// check if preference entry points to valid target
		if (memento != null && !memento.equals("")  && !memento.equals(ICoreConstants.NO_TARGET)) { //$NON-NLS-1$
			ITargetHandle handle;
			try {
				handle = getTarget(memento);
				if (!handle.exists()) {
					// preferences points to invalid target definition remove preference entry
					preferenceManager.setValueOrRemove(ICoreConstants.WORKSPACE_TARGET_HANDLE,
							preferenceManager.getDefaultString(ICoreConstants.WORKSPACE_TARGET_HANDLE));
					preferenceManager.flush();
					return true;
				}
			} catch (CoreException | BackingStoreException e) {
				PDECore.log(e);
			}

		}
		return false;
	}

	@Override
	public void copyTargetDefinition(ITargetDefinition from, ITargetDefinition to) throws CoreException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		((TargetDefinition) from).write(outputStream);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		((TargetDefinition) to).setContents(inputStream);
	}

	@Override
	public void loadTargetDefinition(ITargetDefinition definition, String targetExtensionId) throws CoreException {
		IConfigurationElement elem = PDECore.getDefault().getTargetProfileManager().getTarget(targetExtensionId);
		if (elem == null) {
			throw new CoreException(Status.error(NLS.bind(Messages.TargetPlatformService_2, targetExtensionId)));
		}
		String path = elem.getAttribute("definition"); //$NON-NLS-1$
		String symbolicName = elem.getDeclaringExtension().getContributor().getName();
		URL url = TargetDefinitionManager.getResourceURL(symbolicName, path);
		if (url != null) {
			try {
				((TargetDefinition) definition).setContents(new BufferedInputStream(url.openStream()));
			} catch (IOException e) {
				throw new CoreException(Status.error(NLS.bind(Messages.TargetPlatformService_3, path), e));
			}
		} else {
			throw new CoreException(Status.error(NLS.bind(Messages.TargetPlatformService_4, path)));
		}
	}

	@Override
	public ITargetDefinition newDefaultTarget() {
		ITargetDefinition target = newTarget();
		Location configArea = Platform.getConfigurationLocation();
		String configLocation = null;
		if (configArea != null) {
			configLocation = configArea.getURL().getFile();
		}
		if (configLocation != null) {
			Location location = Platform.getInstallLocation();
			if (location != null) {
				URL url = location.getURL();
				if (url != null) {
					IPath installPath = IPath.fromOSString(url.getFile());
					IPath configPath = IPath.fromOSString(configLocation);
					if (installPath.isPrefixOf(configPath)) {
						// if it is the default configuration area, do not specify explicitly
						configPath = configPath.removeFirstSegments(installPath.segmentCount());
						configPath = configPath.setDevice(null);
						if (configPath.segmentCount() == 1 && configPath.lastSegment().equals("configuration")) { //$NON-NLS-1$
							configLocation = null;
						}
					}
				}
			}
		}
		ITargetLocation container = newProfileLocation("${eclipse_home}", configLocation); //$NON-NLS-1$
		target.setTargetLocations(new ITargetLocation[] {container});
		target.setName(Messages.TargetPlatformService_7);

		// initialize environment with default settings
		target.setArch(Platform.getOSArch());
		target.setOS(Platform.getOS());
		target.setWS(Platform.getWS());
		target.setNL(Platform.getNL());

		// initialize vm arguments from the default container
		ITargetLocation[] containers = target.getTargetLocations();
		Job job = new Job(Messages.TargetPlatformService_6) {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				fVMArguments = getVMArguments(containers);
				return Status.OK_STATUS;
			}
		};
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(org.eclipse.core.runtime.jobs.IJobChangeEvent event) {
				if (fVMArguments != null) {
					target.setVMArguments(fVMArguments.toString().trim());
				}

			}
		});
		job.schedule();

		return target;
	}

	private StringBuilder getVMArguments(ITargetLocation[] containers) {
		StringBuilder arguments = new StringBuilder(""); //$NON-NLS-1$
		if (containers != null) {
			for (ITargetLocation container : containers) {
				String[] vmargs = container.getVMArguments();
				if (vmargs == null) {
					continue;
				}
				for (String vmarg : vmargs) {
					arguments.append(vmarg).append(' ');
				}
			}
		}
		return arguments;
	}
	@Override
	public IStatus compareWithTargetPlatform(ITargetDefinition target) throws CoreException {
		if (!target.isResolved()) {
			return null;
		}

		// Get the current models from the target platform
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getExternalModels();
		Set<File> allFilesAtLocations = new HashSet<>(models.length);
		Map<File, IPluginModelBase> stateLocations = new LinkedHashMap<>(models.length);
		for (IPluginModelBase base : models) {
			allFilesAtLocations.add(new File(base.getInstallLocation()));
			stateLocations.put(new File(base.getInstallLocation()), base);
		}

		// Compare the platform bundles against the definition ones and collect any missing bundles
		MultiStatus multi = new MultiStatus(PDECore.PLUGIN_ID, 0, "", null); //$NON-NLS-1$
		TargetBundle[] bundles = target.getAllBundles();
		Set<NameVersionDescriptor> alreadyConsidered = new HashSet<>(bundles.length);
		for (TargetBundle bundle : bundles) {
			BundleInfo info = bundle.getBundleInfo();
			File file = URIUtil.toFile(info.getLocation());
			String location = file.getAbsolutePath();
			File fileAtLocation = new File(location);
			stateLocations.remove(fileAtLocation);
			NameVersionDescriptor desc = new NameVersionDescriptor(info.getSymbolicName(), info.getVersion());
			if (!alreadyConsidered.contains(desc)) {
				alreadyConsidered.add(desc);
				// ignore duplicates (symbolic name & version)
				if (!allFilesAtLocations.contains(fileAtLocation)) {
					// it's not in the state... if it's not really in the target either (missing) this
					// is not an error
					IStatus status = bundle.getStatus();
					if (status.isOK() || (status.getCode() != TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST && status.getCode() != TargetBundle.STATUS_FEATURE_DOES_NOT_EXIST && status.getCode() != TargetBundle.STATUS_VERSION_DOES_NOT_EXIST)) {
						// its in the target, missing in the state
						IStatus s = new Status(IStatus.WARNING, PDECore.PLUGIN_ID, ITargetPlatformService.STATUS_MISSING_FROM_TARGET_PLATFORM, bundle.getBundleInfo().getSymbolicName(), null);
						multi.add(s);
					}
				}
			}
		}

		// Anything left over is in the state and not the target (have been removed from the target)
		Iterator<IPluginModelBase> iterator = stateLocations.values().iterator();
		while (iterator.hasNext()) {
			IPluginModelBase model = iterator.next();
			IStatus status = new Status(IStatus.WARNING, PDECore.PLUGIN_ID, ITargetPlatformService.STATUS_MISSING_FROM_TARGET_DEFINITION, model.getPluginBase().getId(), null);
			multi.add(status);
		}

		if (multi.isOK()) {
			return Status.OK_STATUS;
		}
		return multi;

	}

	@Override
	public ITargetLocation newIULocation(IInstallableUnit[] units, URI[] repositories, int resolutionFlags) {
		return new IUBundleContainer(units, repositories, resolutionFlags);
	}

	@Override
	public ITargetLocation newIULocation(String[] unitIds, String[] versions, URI[] repositories, int resolutionFlags) {
		return new IUBundleContainer(unitIds, versions, repositories, resolutionFlags);
	}

}
