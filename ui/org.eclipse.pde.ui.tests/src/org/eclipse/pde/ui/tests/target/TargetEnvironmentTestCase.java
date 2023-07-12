/*******************************************************************************
 *  Copyright (c) 2006, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;

public class TargetEnvironmentTestCase {

	private static final String JAVA_SE_1_7 = "JavaSE-1.7";

	@ClassRule
	public static final TestRule RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;
	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@BeforeClass
	public static void setupTargetPlatform() throws Exception {
		TargetPlatformUtil.setRunningPlatformAsTarget();
	}

	private IExecutionEnvironment eeJava_1_7;
	private IVMInstall eeJava_1_7DefaultVM;

	@Before
	public void saveJava1_7EEDefault() {
		eeJava_1_7 = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(JAVA_SE_1_7);
		eeJava_1_7DefaultVM = eeJava_1_7.getDefaultVM();
	}

	@After
	public void restoreJava1_7EEDefault() {
		eeJava_1_7.setDefaultVM(eeJava_1_7DefaultVM);
	}

	@Test
	public void testOS() {
		assertEquals(Platform.getOS(), TargetPlatform.getOS());
	}

	@Test
	public void testWS() {
		assertEquals(Platform.getWS(), TargetPlatform.getWS());
	}

	@Test
	public void testArch() {
		assertEquals(Platform.getOSArch(), TargetPlatform.getOSArch());
	}

	@Test
	public void testNL() {
		assertEquals(Platform.getNL(), TargetPlatform.getNL());
	}

	@Test
	public void testEnvironmentDictionarySize() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(6, dictionary.size());
	}

	@Test
	public void testDictionaryOS() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getOS(), dictionary.get("osgi.os"));
	}

	@Test
	public void testDictionaryWS() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getWS(), dictionary.get("osgi.ws"));
	}

	@Test
	public void testDictionaryArch() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getOSArch(), dictionary.get("osgi.arch"));
	}

	@Test
	public void testDictionaryNL() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals(Platform.getNL(), dictionary.get("osgi.nl"));
	}

	@Test
	public void testResolveOptional() {
		Dictionary<String, String> dictionary = TargetPlatformHelper.getTargetEnvironment();
		assertEquals("true", dictionary.get("osgi.resolveOptional"));
	}

	/**
	 * Tests that the OSGi state for the PDE models has the correct properties set, based on known execution environments
	 */
	@Test
	public void testStateEEProperties() {
		Dictionary<?, ?>[] platformProps = TargetPlatformHelper.getState().getPlatformProperties();

		String[] profiles = TargetPlatformHelper.getKnownExecutionEnvironments();
		for (String profile : profiles) {
			IExecutionEnvironment environment = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(profile);
			if (environment != null) {
				Properties profileProps = environment.getProfileProperties();
				if (profileProps != null) {
					// If we have profile properties for an execution environment, ensure that they were added to the state
					Set<String> profileSystemPackages = getSystemPackages(profileProps)
							.filter(p -> p.startsWith("java.")).collect(Collectors.toSet());
					if (!profileSystemPackages.isEmpty()) {
						String profileEE = getExecutionenvironment(profileProps);
						boolean foundSystemPackage = Arrays.stream(platformProps)
								.filter(pp -> profileEE.equals(getExecutionenvironment(pp))).anyMatch(pp -> {
									Set<String> packages = getSystemPackages(pp).collect(Collectors.toSet());
									return packages.containsAll(profileSystemPackages);
								});
						if (!foundSystemPackage) {
							fail("The system packages property for EE " + profile + " was not found in the state's propeties");
						}
					}
					String ee = getExecutionenvironment(profileProps);
					if (ee != null) {
						if (Stream.of(platformProps).map(pp -> getExecutionenvironment(pp)).noneMatch(ee::equals)) {
							fail("The framework EE property for EE " + profile + " was not found in the state's propeties");
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private String getExecutionenvironment(Object profile) {
		return (String) ((Map<?, ?>) profile).get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
	}

	private static Stream<String> getSystemPackages(Object profile) {
		String platformPackages = (String) ((Map<?, ?>) profile).get(Constants.FRAMEWORK_SYSTEMPACKAGES);
		if (platformPackages == null) {
			return Stream.empty();
		}
		return Arrays.stream(platformPackages.split(","));
	}

	@Test
	public void testProjectWithJVMImports() throws CoreException {
		// A Java-1.7 VM does not provide the java.util.function package
		// introduced in 1.8. But if we say that a Java 1.8+ VM the default
		// (which the one running this test and thus the workspaces default-VM
		// should be), well then the system-packages of that Java 1.8+ VM should
		// be used in the PDEState and thus there should not be errors. This
		// might not be a good idea in production, but is a good test-case.
		IProject project = ProjectUtils.createPluginProject("foo.bar", "foo.bar", "1.0.0.qualifier", (d, s) -> {
			d.setExecutionEnvironments(new String[] { JAVA_SE_1_7 });
			d.setHeader(Constants.IMPORT_PACKAGE, "java.util.function");
			d.setHeader("Automatic-Module-Name", "foo.bar");
		});
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		List<IMarker> errorsWithoutEEDefault = findErrorMarkers(project);
		assertEquals(errorsWithoutEEDefault.toString(), 1, errorsWithoutEEDefault.size());

		eeJava_1_7.setDefaultVM(JavaRuntime.getDefaultVMInstall());

		// await at least VMInstall/EE change delay in
		// MinimalState.rereadSystemPackagesAndReloadTP()
		TestUtils.waitForJobs("testProjectWithEEImports", 300, 10_000);

		List<IMarker> errorsWithEEDefault = findErrorMarkers(project);
		assertEquals(List.of(), errorsWithEEDefault);
	}

	private List<IMarker> findErrorMarkers(IProject project) throws CoreException {
		IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
		List<IMarker> errorsWithoutEEDefault = Arrays.stream(markers)
				.filter(m -> m.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR).toList();
		return errorsWithoutEEDefault;
	}

}
