package org.eclipse.pde.core.tests.internal.classpath;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.wizards.tools.UpdateClasspathJob;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;

public class ChainedReexportPerformanceTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		ProjectUtils.deleteAllWorkspaceProjects();
	}

	@AfterAll
	public static void afterAll() throws Exception {
		// ProjectUtils.deleteAllWorkspaceProjects();
	}

	private static final String CHAIN_PREFIX = "Chain_";
	private static final int PACKAGE_COUNT = 1000;
	private static final int BUNDLE_CHAIN_DEPTH = 5;
	private static final boolean DEBUG = false;
	private File targetDir;

	@BeforeEach
	public void setUp() throws Exception {
		// Disable auto-building
		IWorkspaceDescription desc = ResourcesPlugin.getWorkspace().getDescription();
		desc.setAutoBuilding(false);
		ResourcesPlugin.getWorkspace().setDescription(desc);

		targetDir = Files.createTempDirectory("pde_chain_perf_target").toFile();
		System.out.println("Target Platform Location: " + targetDir.getAbsolutePath());
		createChainedTargetPlatform();
	}

	@AfterEach
	public void tearDown() throws Exception {
		// Restore auto-building
		IWorkspaceDescription desc = ResourcesPlugin.getWorkspace().getDescription();
		desc.setAutoBuilding(true);
		ResourcesPlugin.getWorkspace().setDescription(desc);
	}

	private void createChainedTargetPlatform() throws Exception {
		// Create a chain of bundles: B_0 -> B_1 -> ... -> B_N (all re-exporting)
		for (int i = 0; i < BUNDLE_CHAIN_DEPTH; i++) {
			String name = CHAIN_PREFIX + i;
			String exports = createPackageExports(name);
			String requires = (i > 0) ? (CHAIN_PREFIX + (i - 1) + ";visibility:=reexport") : null;
			createBundle(targetDir, name, exports, requires);
		}

		// Set Target Platform
		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition target = tps.newTarget();
		target.setTargetLocations(new ITargetLocation[] { tps.newDirectoryLocation(targetDir.getAbsolutePath()) });
		TargetPlatformUtil.loadAndSetTarget(target);
	}

	private String createPackageExports(String bundleName) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < PACKAGE_COUNT; i++) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(bundleName).append(".pkg.").append(i);
		}
		return sb.toString();
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
	public void testChainedReexportPerformance() throws Exception {
		IBundleProjectService service = PDECore.getDefault().acquireService(IBundleProjectService.class);

		String consumerName = "ConsumerBundle";
		IProject consumerProj = ResourcesPlugin.getWorkspace().getRoot().getProject(consumerName);
		consumerProj.create(null);
		consumerProj.open(null);

		IBundleProjectDescription consumerDesc = service.getDescription(consumerProj);
		consumerDesc.setSymbolicName(consumerName);
		consumerDesc.setBundleVersion(new Version("1.0.0"));

		// Require the last bundle in the chain with re-export
		IRequiredBundleDescription mainReq = service.newRequiredBundle(CHAIN_PREFIX + (BUNDLE_CHAIN_DEPTH - 1), null, false, true);
		consumerDesc.setRequiredBundles(new IRequiredBundleDescription[] { mainReq });

		consumerDesc.setNatureIds(new String[] { JavaCore.NATURE_ID, IBundleProjectDescription.PLUGIN_NATURE });
		consumerDesc.apply(null);

		// Build to ensure models are ready
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		TestUtils.waitForJobs("Init", 500, 5000);

		IPluginModelBase consumerModel = PluginRegistry.findModel(consumerProj);
		if (consumerModel == null) {
			throw new IllegalStateException("Consumer model not found");
		}

		long start = System.currentTimeMillis();

		// This triggers the computation
		UpdateClasspathJob.scheduleFor(List.of(consumerModel), false);
		waitForJobsAndUI(60000);

		long elapsed = System.currentTimeMillis() - start;
		System.out.println("Classpath computation took: " + elapsed + "ms for chained re-exports.");

		if (elapsed > 10000) {
			throw new AssertionError("Performance regression: Classpath computation took too long (" + elapsed + "ms)");
		}

		IClasspathEntry[] resolvedClasspath = JavaCore.create(consumerProj).getRawClasspath();
		assertTrue(resolvedClasspath.length > 0, "Classpath should not be empty");

		if (DEBUG) {
			long endTime = System.currentTimeMillis() + 6000000;
			while (System.currentTimeMillis() < endTime) {
				if (!Display.getDefault().readAndDispatch()) {
					Display.getDefault().sleep();
				}
			}
		}
	}

	private void waitForJobsAndUI(long timeoutMillis) {
		long start = System.currentTimeMillis();

		while (System.currentTimeMillis() - start < timeoutMillis) {
			// Process UI events
			while (Display.getDefault().readAndDispatch()) {
				// Keep processing
			}

			// Check if all jobs are done
			if (Job.getJobManager().isIdle()) {
				// Process any final UI events
				while (Display.getDefault().readAndDispatch()) {
					// Keep processing
				}
				return;
			}

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}

		throw new AssertionError("Timeout waiting for jobs to complete");
	}
}