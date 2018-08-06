package org.eclipse.pde.ds.internal.annotations.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.osgi.framework.Bundle;

@RunWith(Suite.class)
@SuiteClasses({
	ManagedProjectTest.class,
	UnmanagedProjectTest.class,
	ErrorProjectTest.class,
	DefaultComponentTest.class,
	FullComponentTestV1_2.class,
	FullComponentTest.class,
	ExtendedReferenceMethodComponentTest.class,
	ExtendedLifeCycleMethodComponentTest.class,
})
public class AllDSAnnotationsTests {

	private static final Map<String, String> projects;

	static {
		HashMap<String, String> map = new HashMap<>();
		map.put("ds.annotations.test0", "projects/test0/");
		map.put("ds.annotations.test1", "projects/test1/");
		map.put("ds.annotations.test2", "projects/test2/");
		projects = Collections.unmodifiableMap(map);
	}

	static Job wsJob;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		final IWorkspace ws = ResourcesPlugin.getWorkspace();
		final Bundle bundle = Activator.getContext().getBundle();

		wsJob = new WorkspaceJob("Test Workspace Setup") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				// import test projects
				Path wsRoot = Paths.get(ws.getRoot().getLocationURI());
				for (Map.Entry<String, String> entry : projects.entrySet()) {
					IProject project = ws.getRoot().getProject(entry.getKey());
					try {
						Path projectLocation = Files.createDirectories(wsRoot.resolve(project.getName()));
						copyResources(bundle, entry.getValue(), projectLocation);
						File projectFile = projectLocation.resolve("test.project").toFile();
						if (projectFile.isFile()) {
							projectFile.renameTo(projectLocation.resolve(".project").toFile());
						}
					} catch (IOException e) {
						throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error copying test project content.", e));
					}

					project.create(monitor);
					project.open(monitor);
				}

				// start the build
				ws.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				return Status.OK_STATUS;
			}
		};

		wsJob.schedule();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		wsJob.cancel();
		final IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		new WorkspaceJob("Test Workspace Cleanup") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				for (String projectName : projects.keySet()) {
					IProject project = wsRoot.getProject(projectName);
					project.delete(true, true, monitor);
				}

				return Status.OK_STATUS;
			}
		}.schedule();
	}

	private static void copyResources(Bundle bundle, String srcPath, Path targetPath) throws IOException {
		Enumeration<String> projectPaths = bundle.getEntryPaths(srcPath);
		if (projectPaths == null)
			return;

		while (projectPaths.hasMoreElements()) {
			String entry = projectPaths.nextElement();
			Path target = targetPath.resolve(entry.substring(srcPath.length()));
			if (entry.endsWith("/")) {
				copyResources(bundle, entry, Files.createDirectories(target));
				continue;
			}

			try (InputStream src = bundle.getEntry(entry).openStream()){
				Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
}
