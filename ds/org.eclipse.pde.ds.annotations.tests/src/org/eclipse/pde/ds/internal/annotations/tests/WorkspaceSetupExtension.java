package org.eclipse.pde.ds.internal.annotations.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Map;

import org.eclipse.core.resources.IProject;
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
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.osgi.framework.Bundle;

/**
 * JUnit 5 Extension for setting up the test workspace.
 */
public class WorkspaceSetupExtension implements BeforeAllCallback, AfterAllCallback {

	private static final Map<String, String> PROJECTS = Map.of( //
			"ds.annotations.test0", "projects/test0/", //
			"ds.annotations.test1", "projects/test1/", //
			"ds.annotations.test2", "projects/test2/");

	private static Job wsJob;
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

		wsJob = new WorkspaceJob("Test Workspace Setup") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				// import test projects
				Path wsRoot = Paths.get(ws.getRoot().getLocationURI());
				for (Map.Entry<String, String> entry : PROJECTS.entrySet()) {
					IProject project = ws.getRoot().getProject(entry.getKey());
					try {
						Path projectLocation = Files.createDirectories(wsRoot.resolve(project.getName()));
						copyResources(bundle, entry.getValue(), projectLocation);
						File projectFile = projectLocation.resolve("test.project").toFile();
						if (projectFile.isFile()) {
							projectFile.renameTo(projectLocation.resolve(".project").toFile());
						}
					} catch (IOException e) {
						throw new CoreException(Status.error("Error copying test project content.", e));
					}

					project.create(monitor);
					project.open(monitor);
					project.refreshLocal(IProject.DEPTH_INFINITE, monitor);
				}

				// start the build
				ws.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
				return Status.OK_STATUS;
			}
		};

		wsJob.schedule();
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		// Only cleanup if we're the root context
		if (context.getParent().isPresent()) {
			return;
		}

		if (wsJob != null) {
			wsJob.cancel();
		}

		final IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		wsJob = new WorkspaceJob("Test Workspace Cleanup") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				for (String projectName : PROJECTS.keySet()) {
					IProject project = wsRoot.getProject(projectName);
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

	public static Job getWorkspaceJob() {
		return wsJob;
	}

	private static void copyResources(Bundle bundle, String srcPath, Path targetPath) throws IOException {
		Enumeration<String> projectPaths = bundle.getEntryPaths(srcPath);
		if (projectPaths == null) {
			return;
		}

		while (projectPaths.hasMoreElements()) {
			String entry = projectPaths.nextElement();
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
