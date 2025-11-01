package org.eclipse.pde.ds.internal.annotations.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.osgi.framework.Bundle;

/**
 * JUnit 5 Extension for setting up the test workspace.
 */
public class WorkspaceSetupExtension implements BeforeAllCallback, AfterAllCallback {

	private static final Map<String, Integer> PROJECTS = Map.of( //
			"test0", 0, //
			"test1", 5, //
			"test2", 14);

	private static boolean initialized = false;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		// Only initialize once for all test classes
		synchronized (WorkspaceSetupExtension.class) {
			if (initialized) {
				return;
			}
			initialized = true;
		}

		final IWorkspace ws = ResourcesPlugin.getWorkspace();
		final Bundle bundle = Activator.getContext().getBundle();
		List<Callable<Boolean>> conditions = new ArrayList<>();

		Job wsJob = new WorkspaceJob("Test Workspace Setup") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				// import test projects
				Path wsRoot = ws.getRoot().getLocation().toPath();
				for (Map.Entry<String, Integer> entry : PROJECTS.entrySet()) {
					String projectId = entry.getKey();
					String projectName = "ds.annotations." + projectId;
					String projectPath = "projects/" + projectId + "/";
					int expectedDSComponentFiles = entry.getValue();
					IProject project = ws.getRoot().getProject(projectName);
					try {
						Path projectLocation = Files.createDirectories(wsRoot.resolve(project.getName()));
						copyResources(bundle, projectPath, projectLocation);
						File projectFile = projectLocation.resolve("test.project").toFile();
						if (projectFile.isFile()) {
							projectFile.renameTo(projectLocation.resolve(".project").toFile());
						}
					} catch (IOException e) {
						throw new CoreException(Status.error("Error copying test project content.", e));
					}

					project.create(monitor);
					project.open(monitor);
					IFolder osgiInfFolder = project.getFolder("OSGI-INF");
					osgiInfFolder.create(true, true, null);
					project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					conditions.add(() -> {
						// The generated DS component XML files are not always immediately available on
						// Linux and Mac for some unknown reasons...
						Path projectRoot = project.getLocation().toPath();
						System.out.println("Walk project at: " + projectRoot);
						Files.walk(projectRoot).map(projectRoot::relativize).forEach(System.out::println);
						project.refreshLocal(IResource.DEPTH_INFINITE, null);
						return osgiInfFolder.members().length == expectedDSComponentFiles;
					});
				}
				return Status.OK_STATUS;
			}
		};

		wsJob.schedule();
		wsJob.join();
		for (int i = 0; i < 10; i++) {
			boolean anyFailure = false;
			for (Callable<Boolean> condition : conditions) {
				anyFailure |= !condition.call();
			}
			if (!anyFailure) {
				return;
			}
			System.out.println("OSGI-INF refresh: " + i);
			WorkspaceJob buildJob = new WorkspaceJob("Build test projects") {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					for (IProject project : ws.getRoot().getProjects()) {
						project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					}
					return Status.OK_STATUS;
				}
			};
			buildJob.schedule();
			buildJob.join();
		}
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		// Only cleanup if we're the root context
		if (context.getParent().isPresent()) {
			return;
		}
		final IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		Job wsJob = new WorkspaceJob("Test Workspace Cleanup") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				for (String projectId : PROJECTS.keySet()) {
					IProject project = wsRoot.getProject("ds.annotations." + projectId);
					if (project.exists()) {
						project.delete(true, true, monitor);
					}
				}
				return Status.OK_STATUS;
			}
		};
		wsJob.schedule();
		wsJob.join();
	}

	private static void copyResources(Bundle bundle, String srcPath, Path targetPath) throws IOException {
		Enumeration<String> projectPaths = bundle.getEntryPaths(srcPath);
		if (projectPaths == null) {
			return;
		}
		for (String entry : (Iterable<String>) projectPaths::asIterator) {
			Path target = targetPath.resolve(entry.substring(srcPath.length()));
			if (entry.endsWith("/")) {
				copyResources(bundle, entry, Files.createDirectories(target));
				continue;
			}
			try (InputStream src = bundle.getEntry(entry).openStream()) {
				Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
}
