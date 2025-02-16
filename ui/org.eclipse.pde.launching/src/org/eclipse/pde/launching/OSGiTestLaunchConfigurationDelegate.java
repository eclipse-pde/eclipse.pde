/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.launching;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.pde.internal.core.bnd.PdeBuildJar;
import org.eclipse.pde.internal.core.osgitest.OSGiTestClasspathContributor;

import aQute.bnd.build.Container;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.service.reporter.Report;
import biz.aQute.resolve.Bndrun;

/**
 * @since 3.14
 */
public class OSGiTestLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	private static final String TESTER_CLASSIC = "biz.aQute.tester"; //$NON-NLS-1$
	private static final String TESTER_JUNIT_PLATFORM = "biz.aQute.tester.junit-platform"; //$NON-NLS-1$
	private static final String ATTR_ID = "id"; //$NON-NLS-1$
	private static final String BND_IDENTITY = "bnd.identity"; //$NON-NLS-1$

	private static final List<String> JUNIT5_BUNDLES = List.of("junit-jupiter-api", "junit-jupiter-engine", "junit-jupiter-migrationsupport", "junit-jupiter-params", "junit-platform-commons", "junit-platform-engine", "junit-platform-launcher", "junit-platform-runner", "junit-platform-suite-api", "junit-vintage-engine", "org.opentest4j", "org.apiguardian.api", "org.junit", "org.hamcrest");
	private static final List<String> JUNIT4_BUNDLES = List.of("org.junit", "org.hamcrest");

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		System.out.println("######## Launch ######");
		try {
			IJavaProject javaProject = verifyJavaProject(configuration);
			IProject project = javaProject.getProject();
			try (Bndrun bndrun = Adapters.adapt(project, Bndrun.class)) {
				if (bndrun == null) {
					throw new CoreException(Status.error("Can't get bndrun template from project " + project.getName()));
				}
				bndrun.set(Constants.RUNTRACE, "true");
				Path testJar = createTestJar(javaProject, bndrun);
				FileSetRepository repository = new FileSetRepository("extra", List.of(testJar.toFile()));
				bndrun.addBasicPlugin(repository);
				setupFramework(bndrun);
				setupRunRequires(configuration, bndrun);
				setupJava(configuration, bndrun);
				//TODO create a fragment to our project that imports all packages so we do not need any manual imports e.g.
				// for junit (should be providede by the junit classpath container already!)
				//also there might be other things used e.g. extra-classpath or additional bundles!
				Collection<Container> runbundles = doResolve(bndrun);
				for (Container container : runbundles) {
					System.out.println("runbundle: " + container.getFile());
				}
				ProjectTester tester = bndrun.getProjectTester();
				tester.getProjectLauncher().addRunBundle(testJar.toString());
//				tester.getProjectLauncher().addBasicPlugin(repository);
				tester.addTest("osgitest.HelloOSGiTest");
				//TODO add tests
				tester.prepare();
				int errors = tester.test();
				printErrors(bndrun);
				System.out.println("Errors: " + errors);
				//TODO connect to the running process and show results in the junit view, for this we should migrate the code from bndtools about launching so it can be shared with pde
			}
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Path createTestJar(IJavaProject javaProject, Bndrun bndrun) throws Exception {
		Path jarFile = Files.createTempFile("test-probe", ".jar");
		bndrun.addClose(() -> Files.delete(jarFile));
		try (PdeBuildJar jar = new PdeBuildJar(javaProject, true); Analyzer analyzer = new Analyzer(jar)) {
			analyzer.setBundleVersion("1.0.0");
			analyzer.setBundleSymbolicName("test-probe");
			analyzer.set("Test-Cases", "osgitest.HelloOSGiTest");
			Manifest manifest = jar.getManifest();
			jar.remove(JarFile.MANIFEST_NAME);
			jar.setManifest(new Manifest());
			//TODO find all testclasses!
			//TODO make fragment host if project is a fragment
			jar.setManifest(analyzer.calcManifest());
			jar.write(Files.newOutputStream(jarFile));
		}
		System.out.println("Testprobe written to " + jarFile);
		return jarFile;
	}

	protected void printErrors(Report bndrun) {
		//TODO throw on error?
		bndrun.getErrors().forEach(err -> System.out.println("ERROR:" + err));
		bndrun.getWarnings().forEach(err -> System.out.println("WARN:" + err));
	}

	protected void setupFramework(Bndrun bndrun) {
		bndrun.setRunfw("org.eclipse.osgi"); //$NON-NLS-1$ //TODO fetch from selection
	}

	protected Collection<Container> doResolve(Bndrun bndrun) throws Exception {
		String resolved = bndrun.resolve(false, false);
		bndrun.set(Constants.RUNBUNDLES, resolved);
		return bndrun.getRunbundles();
	}

	protected void setupJava(ILaunchConfiguration configuration, Bndrun bndrun) throws CoreException {
		IVMInstall vmInstall = verifyVMInstall(configuration);
		File executable = getJavaExecutable(vmInstall);
		if (executable != null) {
			bndrun.setProperty("java", executable.getAbsolutePath()); //$NON-NLS-1$
		}
	}

	protected void setupRunRequires(ILaunchConfiguration configuration, Bndrun bndrun) {
		Parameters runrequires = bndrun.getParameters(Constants.RUNREQUIRES);
		String tester = addTestFramework(runrequires, configuration);
		//TODO check if JUnit Classpath container is given
		OSGiTestClasspathContributor.bundles(true).forEach(b -> {
			runrequires.add(BND_IDENTITY, Attrs.create(ATTR_ID, b));
		});
		runrequires.add(BND_IDENTITY, Attrs.create(ATTR_ID, "test-probe"));
		//TODO add extra bundles selected by the user!
		StringBuilder builder = new StringBuilder();
		runrequires.append(builder);
		bndrun.setRunRequires(builder.toString());
		bndrun.set(Constants.TESTER, tester);
	}

	@SuppressWarnings("restriction")
	private String addTestFramework(Parameters runrequires, ILaunchConfiguration configuration) {
		ITestKind testKind = JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(testKind.getId())) {
			runrequires.add(BND_IDENTITY, Attrs.create(ATTR_ID, TESTER_JUNIT_PLATFORM));
			for (String bundle : JUNIT5_BUNDLES) {
				runrequires.add(BND_IDENTITY, Attrs.create(ATTR_ID, bundle));
			}
			return TESTER_JUNIT_PLATFORM;
		}
		runrequires.add(BND_IDENTITY, Attrs.create(ATTR_ID, TESTER_CLASSIC));
		for (String bundle : JUNIT4_BUNDLES) {
			runrequires.add(BND_IDENTITY, Attrs.create(ATTR_ID, bundle));
		}
		return TESTER_CLASSIC;
	}

	@SuppressWarnings("restriction")
	private static File getJavaExecutable(IVMInstall vmInstall) {
		File installLocation = vmInstall.getInstallLocation();
		if (installLocation != null) {
			return StandardVMType.findJavaExecutable(installLocation);
		}
		return null;
	}

}
