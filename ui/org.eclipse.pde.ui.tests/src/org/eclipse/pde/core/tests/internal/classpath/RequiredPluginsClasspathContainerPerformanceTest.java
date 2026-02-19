package org.eclipse.pde.core.tests.internal.classpath;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.wizards.tools.UpdateClasspathJob;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RequiredPluginsClasspathContainerPerformanceTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		ProjectUtils.deleteAllWorkspaceProjects();
	}

	@AfterAll
	public static void afterAll() throws Exception {
		// ProjectUtils.deleteAllWorkspaceProjects();
	}

	private static final String CYCLE_BUNDLE_PREFIX = "Cycle_";
	private File targetDir;

	@BeforeEach
	public void setUp() throws Exception {
		// Disable auto-building
		IWorkspaceDescription desc = ResourcesPlugin.getWorkspace().getDescription();
		desc.setAutoBuilding(false);
		ResourcesPlugin.getWorkspace().setDescription(desc);

		targetDir = Files.createTempDirectory("pde_perf_target").toFile();
		System.out.println("Target Platform Location: " + targetDir.getAbsolutePath());
		createCyclicTargetPlatform();
	}

	@AfterEach
	public void tearDown() throws Exception {
		// Restore auto-building
		IWorkspaceDescription desc = ResourcesPlugin.getWorkspace().getDescription();
		desc.setAutoBuilding(true);
		ResourcesPlugin.getWorkspace().setDescription(desc);

//		if (targetDir != null && targetDir.exists()) {
//			deleteDir(targetDir);
//		}
		
		// Reset target platform
//		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);
//		ITargetDefinition defaultTarget = tps.newDefaultTarget();
//		TargetPlatformUtil.loadAndSetTarget(defaultTarget);
	}

	private void createCyclicTargetPlatform() throws Exception {
		// Cycle_A -> reexports Cycle_B
		// Cycle_B -> reexports Cycle_C
		// Cycle_C -> reexports Cycle_A
		createBundle(targetDir, CYCLE_BUNDLE_PREFIX + "A", null, CYCLE_BUNDLE_PREFIX + "B;visibility:=reexport");
		createBundle(targetDir, CYCLE_BUNDLE_PREFIX + "B", null, CYCLE_BUNDLE_PREFIX + "C;visibility:=reexport");
		createBundle(targetDir, CYCLE_BUNDLE_PREFIX + "C", null, CYCLE_BUNDLE_PREFIX + "A;visibility:=reexport");

		// Set Target Platform
		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition target = tps.newTarget();
		target.setTargetLocations(new ITargetLocation[] { tps.newDirectoryLocation(targetDir.getAbsolutePath()) });
		TargetPlatformUtil.loadAndSetTarget(target);
	}

	private void createBundle(File dir, String name, String exports, String requires) throws IOException {
		File jarFile = new File(dir, name + ".jar");
		try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
			Manifest manifest = new Manifest();
			Attributes main = manifest.getMainAttributes();
			main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			main.put(new Attributes.Name("Bundle-ManifestVersion"), "2");
			main.put(new Attributes.Name("Bundle-SymbolicName"), name);
			main.put(new Attributes.Name("Bundle-Version"), "1.0.0");
			if (exports != null) {
				main.put(new Attributes.Name("Export-Package"), exports);
			}
			if (requires != null) {
				main.put(new Attributes.Name("Require-Bundle"), requires);
			}

			ZipEntry entry = new ZipEntry("META-INF/MANIFEST.MF");
			jos.putNextEntry(entry);
			manifest.write(jos);
			jos.closeEntry();
		}
	}

	@Test
	public void testCyclicReexportInSecondaryDependencies() throws Exception {
		IBundleProjectService service = PDECore.getDefault().acquireService(IBundleProjectService.class);

		// Create Consumer project
		String consumerName = "ConsumerBundle";
		IProject consumerProj = ResourcesPlugin.getWorkspace().getRoot().getProject(consumerName);
		consumerProj.create(null);
		consumerProj.open(null);

		IBundleProjectDescription consumerDesc = service.getDescription(consumerProj);
		consumerDesc.setSymbolicName(consumerName);
		consumerDesc.setBundleVersion(new Version("1.0.0"));
		// No direct requirements, we use secondaryDependencies
		consumerDesc.setNatureIds(new String[] { JavaCore.NATURE_ID, IBundleProjectDescription.PLUGIN_NATURE });
		consumerDesc.apply(null);
		
		// Add Secondary Dependency to build.properties
		IFile buildProps = PDEProject.getBuildProperties(consumerProj);
		WorkspaceBuildModel buildModel = new WorkspaceBuildModel(buildProps);
		buildModel.load();
		IBuild build = buildModel.getBuild();
		IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
		if (entry == null) {
			entry = buildModel.getFactory().createEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			build.add(entry);
		}
		entry.addToken(CYCLE_BUNDLE_PREFIX + "A");
		buildModel.save();

		// Build to ensure models are ready
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		TestUtils.waitForJobs("Init", 500, 5000);

		IPluginModelBase consumerModel = PluginRegistry.findModel(consumerProj);
		if (consumerModel == null) {
			throw new IllegalStateException("Consumer model not found");
		}

		long start = System.currentTimeMillis();
		
		// This triggers the computation via Job
		Job job = UpdateClasspathJob.scheduleFor(List.of(consumerModel), false);
		job.join();
		
		long elapsed = System.currentTimeMillis() - start;
		System.out.println("Classpath computation took: " + elapsed + "ms for cyclic re-exported bundles.");
		
		if (elapsed > 5000) {
			throw new AssertionError("Performance regression or Infinite Loop: Classpath computation took too long (" + elapsed + "ms)");
		}
		
		IClasspathEntry[] resolvedClasspath = JavaCore.create(consumerProj).getRawClasspath();
		assertTrue(resolvedClasspath.length > 0, "Classpath should not be empty");
	}
}
