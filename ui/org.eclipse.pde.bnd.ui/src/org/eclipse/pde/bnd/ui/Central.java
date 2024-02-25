/*******************************************************************************
 * Copyright (c) 2013, 2024 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     PK Søreide <per.kristian.soreide@gmail.com> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
 *     Carter Smithhart <carter.smithhart@gmail.com> - ongoing enhancements
 *     Gregory Amerson <gregory.amerson@liferay.com> - ongoing enhancements
 *     Sean Bright <sean@malleable.com> - ongoing enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - ongoing enhancements
 *     Fr Jeremy Krieg <fr.jkrieg@greekwelfaresa.org.au> - ongoing enhancements
 *     Jürgen Albert <j.albert@data-in-motion.biz> - ongoing enhancements
 *     Christoph Läubrich - Adapt to PDE codebase
*******************************************************************************/
package org.eclipse.pde.bnd.ui;

import static aQute.bnd.exceptions.FunctionWithException.asFunction;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ModelListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.bnd.ui.plugins.RepositoriesViewRefresher;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.BiFunctionWithException;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.exceptions.FunctionWithException;
import aQute.bnd.exceptions.RunnableWithException;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.progress.ProgressPlugin.Task;
import aQute.bnd.service.progress.TaskManager;
import aQute.service.reporter.Reporter;

public class Central {

	public static final String BNDTOOLS_NATURE = "bndtools.core.bndnature";
	
	private final static List<ModelListener> listeners = new CopyOnWriteArrayList<>();

	public static IFile getWorkspaceBuildFile() throws Exception {
		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace()
			.getRoot();
		IProject cnf = wsroot.getProject(Workspace.CNFDIR);
		if (cnf == null || !cnf.isAccessible())
			return null;
		return cnf.getFile(Workspace.BUILDFILE);
	}

	public static PromiseFactory promiseFactory() {
		return Processor.getPromiseFactory();
	}

	/**
	 * Returns the Bnd Workspace directory <em>IF</em> there is a "cnf" project
	 * in the Eclipse workspace.
	 *
	 * @return The returned directory is the parent of the "cnf" project's
	 *         directory. Otherwise, {@code null}.
	 */
	private static File getWorkspaceDirectory() throws CoreException {
		IWorkspaceRoot eclipseWorkspace = ResourcesPlugin.getWorkspace()
			.getRoot();

		IProject cnfProject = eclipseWorkspace.getProject(Workspace.CNFDIR);
		if (cnfProject.exists()) {
			if (!cnfProject.isOpen())
				cnfProject.open(null);
			return cnfProject.getLocation()
				.toFile()
				.getParentFile();
		}

		return null;
	}

	/**
	 * Determine if the given directory is a workspace.
	 *
	 * @param directory the directory that must hold cnf/build.bnd
	 * @return true if a workspace directory
	 */
	public static boolean isWorkspace(File directory) {
		File build = new File(directory, "cnf/build.bnd");
		return build.isFile();
	}

	public static boolean hasWorkspaceDirectory() {
		try {
			return getWorkspaceDirectory() != null;
		} catch (CoreException e) {
			return false;
		}
	}

	public static boolean isChangeDelta(IResourceDelta delta) {
		if (IResourceDelta.MARKERS == delta.getFlags())
			return false;
		if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED)) == 0)
			return false;
		return true;
	}

	public void changed(Project model) {
		model.setChanged();
		for (ModelListener m : listeners) {
			try {
				m.modelChanged(model);
			} catch (Exception e) {
				ILog.get().error("While notifying ModelListener " + m + " of change to project " + model, e);
			}
		}
	}

	public void addModelListener(ModelListener m) {
		if (!listeners.contains(m)) {
			listeners.add(m);
		}
	}

	public void removeModelListener(ModelListener m) {
		listeners.remove(m);
	}

	public static IJavaProject getJavaProject(Project model) {
		return getProject(model).map(JavaCore::create)
			.filter(IJavaProject::exists)
			.orElse(null);
	}

	public static Optional<IProject> getProject(Project model) {
		String name = model.getName();
		return Arrays.stream(ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProjects())
			.filter(p -> p.getName()
				.equals(name))
			.findFirst();
	}

	public static IPath toPath(File file, Workspace workspace) throws Exception {
		File absolute = file.getCanonicalFile();
		return toFullPath(absolute).orElseGet(() -> {
			if (workspace != null) {
				try {
					String workspacePath = workspace.getBase().getAbsolutePath();
					String absolutePath = absolute.getPath();
					if (absolutePath.startsWith(workspacePath))
						return new Path(absolutePath.substring(workspacePath.length()));
					return null;
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}
			return null;
		});
	}

	public static IPath toPathMustBeInEclipseWorkspace(File file) throws Exception {
		File absolute = file.getCanonicalFile();
		return toFullPath(absolute).orElse(null);
	}

	private static Optional<IPath> toFullPath(File file) {
		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace()
			.getRoot();
		IFile[] candidates = wsroot.findFilesForLocationURI(file.toURI());
		return Stream.of(candidates)
			.map(IFile::getFullPath)
			.min((a, b) -> Integer.compare(a.segmentCount(), b.segmentCount()));
	}

	public static Optional<IPath> toBestPath(IResource resource) {
		return Optional.ofNullable(resource.getLocationURI())
			.map(File::new)
			.flatMap(Central::toFullPath);
	}

	public static void refresh(IPath path) {
		try {
			IResource r = ResourcesPlugin.getWorkspace()
				.getRoot()
				.findMember(path);
			if (r != null)
				return;

			IPath p = (IPath) path.clone();
			while (p.segmentCount() > 0) {
				p = p.removeLastSegments(1);
				IResource resource = ResourcesPlugin.getWorkspace()
					.getRoot()
					.findMember(p);
				if (resource != null) {
					resource.refreshLocal(IResource.DEPTH_INFINITE, null);
					return;
				}
			}
		} catch (Exception e) {
			ILog.get().error("While refreshing path " + path, e);
		}
	}

	public static void refreshPlugins(Workspace workspace) throws Exception {
		if (workspace == null) {
			return;
		}
		List<File> refreshedFiles = new ArrayList<>();
		List<Refreshable> rps = workspace.getPlugins(Refreshable.class);
		boolean changed = false;
		boolean repoChanged = false;
		for (Refreshable rp : rps) {
			if (rp.refresh()) {
				changed = true;
				File root = rp.getRoot();
				if (root != null)
					refreshedFiles.add(root);
				if (rp instanceof RepositoryPlugin) {
					repoChanged = true;
				}
			}
		}

		//
		// If repos were refreshed then
		// we should also update the classpath
		// containers. We can force this by setting the "bndtools.refresh"
		// property.
		//

		if (changed) {
			for (File file : refreshedFiles) {
				refreshFile(file);
			}

			if (repoChanged) {
				RepositoriesViewRefresher.refreshRepositories(null);
			}
			refreshProjects(workspace);
		}
	}

	public static void refreshPlugin(Workspace workspace, Refreshable plugin) throws Exception {
		refreshPlugin(workspace, plugin, false);
	}

	public static void refreshPlugin(Workspace workspace, Refreshable plugin, boolean force) throws Exception {
		boolean refresh = plugin.refresh();
		if (refresh || force) {
			refreshFile(plugin.getRoot());
			if (plugin instanceof RepositoryPlugin) {
				RepositoriesViewRefresher.refreshRepositories((RepositoryPlugin) plugin);
			}
			refreshProjects(workspace);
		}
	}

	public static void refreshProjects(Workspace workspace) throws Exception {
		if (workspace == null) {
			return;
		}
		Collection<Project> allProjects = workspace.getAllProjects();
		// Mark all projects changed before we notify model listeners
		// since the listeners can take actions on project's other than
		// the specified project.
		for (Project p : allProjects) {
			p.setChanged();
		}
		for (Project p : allProjects) {
			for (ModelListener m : listeners) {
				try {
					m.modelChanged(p);
				} catch (Exception e) {
					ILog.get().error("While notifying ModelListener " + m + " of change to project " + p, e);
				}
			}
		}
	}

	public static void refreshFile(File f) throws CoreException {
		refreshFile(f, null, false);
	}

	public static void refreshFile(File file, IProgressMonitor monitor, boolean derived) throws CoreException {
		IResource target = toResource(file);
		if (target == null) {
			return;
		}
		int depth = target.getType() == IResource.FILE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;
		if (!target.isSynchronized(depth)) {
			target.refreshLocal(depth, monitor);
			if (target.exists() && (target.isDerived() != derived)) {
				target.setDerived(derived, monitor);
			}
		}
	}

	public static void refresh(Project p) throws Exception {
		IJavaProject jp = getJavaProject(p);
		if (jp != null)
			jp.getProject()
				.refreshLocal(IResource.DEPTH_INFINITE, null);
	}


	public static Project getProject(Workspace workspace, File projectDir) throws Exception {
		return workspace.getProjectFromFile(projectDir);
	}

	@SuppressWarnings("resource")
	public static Project getProject(Workspace workspace, IProject p) throws Exception {
		return Optional.ofNullable(p.getLocation())
			.map(IPath::toFile)
				.map(asFunction(f -> getProject(workspace, f)))
			.orElse(null);
	}

	public static boolean isBndProject(IProject project) {
		return Optional.ofNullable(project)
				.map(asFunction(p -> p.getNature(BNDTOOLS_NATURE)))
			.isPresent();
	}

	/**
	 * Return the IResource associated with a file
	 *
	 * @param file
	 */

	public static IResource toResource(File file) {
		if (file == null)
			return null;

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
			.getRoot();
		return toFullPath(file).map(p -> file.isDirectory() ? root.getFolder(p) : root.getFile(p))
			.orElse(null);
	}

	/**
	 * Used to serialize access to the Bnd Workspace.
	 *
	 * @param lockMethod The Workspace lock method to use.
	 * @param callable The code to execute while holding the lock. The argument
	 *            can be used to register after lock release actions.
	 * @param monitorOrNull If the monitor is cancelled, a TimeoutException will
	 *            be thrown, can be null
	 * @return The result of the specified callable.
	 * @throws InterruptedException If the thread is interrupted while waiting
	 *             for the lock.
	 * @throws TimeoutException If the lock was not obtained within the timeout
	 *             period or the specified monitor is cancelled while waiting to
	 *             obtain the lock.
	 * @throws Exception If the callable throws an exception.
	 */
	public static <V> V bndCall(BiFunctionWithException<Callable<V>, BooleanSupplier, V> lockMethod,
		FunctionWithException<BiConsumer<String, RunnableWithException>, V> callable,
		IProgressMonitor monitorOrNull) throws Exception {
		IProgressMonitor monitor = monitorOrNull == null ? new NullProgressMonitor() : monitorOrNull;
		Task task = new Task() {
			@Override
			public void worked(int units) {
				monitor.worked(units);
			}

			@Override
			public void done(String message, Throwable e) {}

			@Override
			public boolean isCanceled() {
				return monitor.isCanceled();
			}

			@Override
			public void abort() {
				monitor.setCanceled(true);
			}
		};
		List<Runnable> after = new ArrayList<>();
		MultiStatus status = new MultiStatus(Central.class, 0,
			"Errors occurred while calling bndCall after actions");
		try {
			Callable<V> with = () -> TaskManager.with(task, () -> callable.apply((name, runnable) -> after.add(() -> {
				monitor.subTask(name);
					try {
					runnable.run();
				} catch (Exception e) {
					if (!(e instanceof OperationCanceledException)) {
						status.add(new Status(IStatus.ERROR, runnable.getClass(),
							"Unexpected exception in bndCall after action: " + name, e));
						}
					}
			})));
			return lockMethod.apply(with, monitor::isCanceled);
		} finally {
			for (Runnable runnable : after) {
				runnable.run();
			}
			if (!status.isOK()) {
				throw new CoreException(status);
			}
		}
	}

	/**
	 * Convert a processor to a status object
	 */

	public static IStatus toStatus(Processor processor, String message) {
		int severity = IStatus.INFO;
		List<IStatus> statuses = new ArrayList<>();
		for (String error : processor.getErrors()) {
			Status status = new Status(IStatus.ERROR, BndtoolsConstants.CORE_PLUGIN_ID, error);
			statuses.add(status);
			severity = IStatus.ERROR;
		}
		for (String warning : processor.getWarnings()) {
			Status status = new Status(IStatus.WARNING, BndtoolsConstants.CORE_PLUGIN_ID, warning);
			statuses.add(status);
			severity = IStatus.WARNING;
		}

		IStatus[] array = statuses.toArray(new IStatus[0]);
		return new MultiStatus(//
			BndtoolsConstants.CORE_PLUGIN_ID, //
			severity, //
			array, message, null);
	}

	public static boolean refreshFiles(Reporter reporter, Collection<File> files, IProgressMonitor monitor,
		boolean derived) {
		AtomicInteger errors = new AtomicInteger();

		files.forEach(t -> {
			try {
				Central.refreshFile(t, monitor, derived);
			} catch (CoreException e) {
				errors.incrementAndGet();
				if (reporter != null)
					reporter.error("failed to refresh %s : %s", t, Exceptions.causes(e));
				else
					throw Exceptions.duck(e);
			}
		});
		return errors.get() == 0;
	}

}
