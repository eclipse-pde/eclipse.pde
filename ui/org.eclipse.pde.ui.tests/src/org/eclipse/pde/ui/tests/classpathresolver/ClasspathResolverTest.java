/*******************************************************************************
 * Copyright (c) 2011, 2021 Sonatype, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      IBM Corporation - ongoing enhancements
 *      Hannes Wellmann - Bug 577629 - Unify project creation/deletion in tests
 *      Hannes Wellmann - Bug 577541 - Clean up ClasspathHelper and TargetWeaver and create tests
 *******************************************************************************/
package org.eclipse.pde.ui.tests.classpathresolver;

import static org.eclipse.pde.ui.tests.launcher.AbstractLaunchTest.findTargetModel;
import static org.eclipse.pde.ui.tests.launcher.AbstractLaunchTest.findWorkspaceModel;
import static org.junit.Assert.assertEquals;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.pde.core.IBundleClasspathResolver;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.TargetWeaver;
import org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupDirector;
import org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.osgi.framework.Bundle;

/**
 * Tests {@link IBundleClasspathResolver} API to extend how the classpath and
 * source lookup path is created.
 *
 */
public class ClasspathResolverTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;
	@ClassRule
	public static final TestRule RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;

	private static IProject project;

	/**
	 * The project name and bundle symbolic name of the test project
	 */
	public static final String bundleName = "classpathresolver";
	private static final String HOST_BUNDLE_ID = "org.eclipse.pde.core";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		project = ProjectUtils.importTestProject("tests/projects/" + bundleName);
		// create workspace plug-ins with same id like a running-platform bundle
		Bundle hostBundle = Platform.getBundle(HOST_BUNDLE_ID);
		createWorkspacePluginProjects(List.of( //
				bundle(hostBundle.getSymbolicName(), "2.0.0"), //
				bundle(hostBundle.getSymbolicName(), hostBundle.getVersion().toString())));
	}

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private Path mockedPlatformDevPropertiesFile;
	private String originalPlatformDevPropertiesURL;

	@Before
	public void setUp() throws Exception {
		mockedPlatformDevPropertiesFile = tempFolder.newFile("test-platform-dev.properties").toPath();
		String mockDevPropertiesURL = mockedPlatformDevPropertiesFile.toUri().toURL().toString();
		originalPlatformDevPropertiesURL = setPlatformDevPropertiesURL(mockDevPropertiesURL);
	}

	@After
	public void tearDown() throws ReflectiveOperationException {
		setPlatformDevPropertiesURL(originalPlatformDevPropertiesURL);
	}

	private class _PDESourceLookupQuery extends PDESourceLookupQuery {

		public _PDESourceLookupQuery(PDESourceLookupDirector director, Object object) {
			super(director, object);
		}

		@Override // Make super.getSourceContainers() visible
		public ISourceContainer[] getSourceContainers(String location, String id) throws CoreException {
			return super.getSourceContainers(location, id);
		}
	}

	/**
	 * Checks that a created dev properties file will recognise the modified
	 * classpath
	 */
	@Test
	public void testGetDevProperties() throws Exception {
		mockTPWithRunningPlatformAndBundles(List.of()); // running-platform only

		File devProperties = tempFolder.newFile("dev.properties").getCanonicalFile();
		String devPropertiesURL = ClasspathHelper.getDevEntriesProperties(devProperties.getPath(), false);

		Properties properties = loadProperties(devPropertiesURL);

		String expectedDevCP = project.getFolder("cpe").getLocation().toPortableString();
		assertEquals(expectedDevCP, properties.get(bundleName));
	}

	/**
	 * Checks that the source lookup path of a project is updated from the API
	 */
	@Test
	public void testSourceLookupPath() throws Exception {
		mockTPWithRunningPlatformAndBundles(List.of()); // running-platform only

		PDESourceLookupDirector d = new PDESourceLookupDirector();
		_PDESourceLookupQuery q = new _PDESourceLookupQuery(d, project);

		ISourceContainer[] containers = q.getSourceContainers(project.getLocation().toOSString(), bundleName);

		assertEquals(2, containers.length);
		assertEquals(JavaCore.create(project), ((JavaProjectSourceContainer) containers[0]).getJavaProject());
		assertEquals(project.getFolder("cpe").getLocation().toFile(),
				((DirectorySourceContainer) containers[1]).getDirectory());
	}

	@Test
	public void testGetDevProperties_workspacePlugin_devEntryWithAndWithoutVersion() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();

		mockTPWithBundles(List.of()); // empty TP

		IPluginModelBase wsModel = findWorkspaceModel(HOST_BUNDLE_ID, "2.0.0");

		Properties devProperties = createDevEntryProperties(List.of(wsModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath1,bin", devProperties.getProperty(HOST_BUNDLE_ID));
		assertUnrelatedEntriesArePresent(hostBundle, devProperties);
		assertEquals(5, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_workspacePluginWithSameVersionLikeHostBundle_devEntryWithAndWithoutVersion()
			throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		mockTPWithBundles(List.of()); // empty TP

		IPluginModelBase wsModel = findWorkspaceModel(HOST_BUNDLE_ID, hostBundleVersion);

		Properties devProperties = createDevEntryProperties(List.of(wsModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath1,bin", devProperties.getProperty(HOST_BUNDLE_ID));
		assertUnrelatedEntriesArePresent(hostBundle, devProperties);
		assertEquals(5, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_bundleFromRunningPlatform_wovenDevEntryWithAndWithoutVersion() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		mockTPWithRunningPlatformAndBundles(List.of()); // running-platform only

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, hostBundleVersion);

		Properties devProperties = createDevEntryProperties(List.of(hostModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath1", devProperties.getProperty(HOST_BUNDLE_ID));
		assertUnrelatedEntriesArePresent(hostBundle, devProperties);
		assertEquals(5, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_jarTPBundle_noDevEntries() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();

		// pretend there is only a jar-bundle in the TP that has the same
		// name and version like a woven plug-in from the host
		mockTPWithBundles(List.of( //
				bundle(HOST_BUNDLE_ID, "1.0.0")));

		IPluginModelBase tpModel = findTargetModel(HOST_BUNDLE_ID, "1.0.0");

		Properties devProperties = createDevEntryProperties(List.of(tpModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath1", devProperties.getProperty(HOST_BUNDLE_ID));
		assertUnrelatedEntriesArePresent(hostBundle, devProperties);
		assertEquals(5, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_jarTPBundleWithSameVersionLikeHostBundle_noDevEntries() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		// pretend there is only a jar-bundle in the TP that has the same
		// name and version like a woven plug-in from the host
		mockTPWithBundles(List.of( //
				bundle(HOST_BUNDLE_ID, hostBundleVersion)));

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, hostBundleVersion);

		Properties devProperties = createDevEntryProperties(List.of(hostModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath1", devProperties.getProperty(HOST_BUNDLE_ID));
		assertUnrelatedEntriesArePresent(hostBundle, devProperties);
		assertEquals(5, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_workspaceAndJarTPBundle_oneEmptyDevEntryAndOneWithAndWithoutVersion()
			throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();

		mockTPWithBundles(List.of( //
				bundle(HOST_BUNDLE_ID, "1.0.0")));

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, "1.0.0");
		IPluginModelBase wsModel = findWorkspaceModel(HOST_BUNDLE_ID, "2.0.0");

		Properties devProperties = createDevEntryProperties(List.of(hostModel, wsModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath1,bin", devProperties.getProperty(HOST_BUNDLE_ID));
		assertUnrelatedEntriesArePresent(hostBundle, devProperties);
		assertEquals(5, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_HostAndJarBundle_oneEmptyDevEntryAndOneWithAndWithoutVersion() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		mockTPWithRunningPlatformAndBundles(List.of( //
				bundle(HOST_BUNDLE_ID, "1.0.0")));

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, hostBundleVersion);
		IPluginModelBase tpModel = findTargetModel(HOST_BUNDLE_ID, "1.0.0");

		Properties devProperties = createDevEntryProperties(List.of(tpModel, hostModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath1", devProperties.getProperty(HOST_BUNDLE_ID));
		assertUnrelatedEntriesArePresent(hostBundle, devProperties);
		assertEquals(5, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_workspaceAndHostBundle_twoDevEntriesWithAndWithoutVersion() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		mockTPWithRunningPlatformAndBundles(List.of()); // running-platform only

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, hostBundleVersion);
		IPluginModelBase wsModel = findWorkspaceModel(HOST_BUNDLE_ID, "2.0.0");

		Properties devProperties = createDevEntryProperties(List.of(hostModel, wsModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath1,bin", devProperties.getProperty(HOST_BUNDLE_ID));
		assertUnrelatedEntriesArePresent(hostBundle, devProperties);
		assertEquals(5, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_mixedWorkspaceAndHostAndJarTPBundle_onlyUsedPlatformBundles() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		mockTPWithRunningPlatformAndBundles(List.of( //
				bundle(HOST_BUNDLE_ID, "1.0.0")));

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, hostBundleVersion);
		IPluginModelBase tpModel = findTargetModel(HOST_BUNDLE_ID, "1.0.0");
		IPluginModelBase wsModel = findWorkspaceModel(HOST_BUNDLE_ID, "2.0.0");

		Properties devProperties = createDevEntryProperties(List.of(hostModel, tpModel, wsModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath1,bin", devProperties.getProperty(HOST_BUNDLE_ID));
		assertUnrelatedEntriesArePresent(hostBundle, devProperties);
		assertEquals(5, devProperties.size()); // assert no more entries
	}

	// --- utility methods ---

	private static String setPlatformDevPropertiesURL(String string) throws ReflectiveOperationException {
		// trigger properties reload on next use
		setStaticField(TargetWeaver.class, "fgDevProperties", null);
		return setStaticField(TargetWeaver.class, "fgDevPropertiesURL", string);
	}

	private static <V> V setStaticField(Class<?> cl, String fieldName, V newValue) throws ReflectiveOperationException {
		Field field = cl.getDeclaredField(fieldName);
		field.trySetAccessible();
		@SuppressWarnings("unchecked")
		V oldValue = (V) field.get(null);
		field.set(null, newValue);
		return oldValue;
	}

	private static void createWorkspacePluginProjects(List<NameVersionDescriptor> workspacePlugins)
			throws CoreException {
		List<IProject> pluginProjects = ProjectUtils.createWorkspacePluginProjects(workspacePlugins);
		while (pluginProjects.stream().anyMatch(ClasspathResolverTest::isUpdatePending)) {
			Thread.yield(); // await async classpath update of projects
		}
	}

	private static boolean isUpdatePending(IProject project) {
		IJavaProject jProject = JavaCore.create(project);
		try {
			IPath outputLocation = jProject.getOutputLocation();
			return outputLocation == null || project.findMember(outputLocation.removeFirstSegments(1)) == null;
		} catch (JavaModelException e) {
			return false;
		}
	}

	private Bundle getHostBundleAndMockDevProperties() throws IOException {
		Bundle hostBundle = Platform.getBundle(HOST_BUNDLE_ID);
		Files.write(mockedPlatformDevPropertiesFile, List.of( //
				HOST_BUNDLE_ID + "=devPath1", //
				HOST_BUNDLE_ID + ";" + hostBundle.getVersion() + "=devPath2", //
				"some.other.plugin=devPath3", //
				"some.other.plugin;2.0.0=devPath4"));
		return hostBundle;
	}

	private void mockTPWithBundles(List<NameVersionDescriptor> targetBundles) throws IOException, InterruptedException {
		Path jarsDirectory = tempFolder.newFolder("TPJarsDirectory").toPath();
		TargetPlatformUtil.setDummyBundlesAsTarget(targetBundles, jarsDirectory);
	}

	private void mockTPWithRunningPlatformAndBundles(List<NameVersionDescriptor> targetBundles)
			throws IOException, InterruptedException {
		Path jarsDirectory = tempFolder.newFolder("TPJarsDirectory").toPath();
		TargetPlatformUtil.setRunningPlatformWithDummyBundlesAsTarget(targetBundles, jarsDirectory,
				b -> b.getSymbolicName().equals(HOST_BUNDLE_ID));
	}

	private static NameVersionDescriptor bundle(String id, String version) {
		return new NameVersionDescriptor(id, version);
	}

	private Properties createDevEntryProperties(List<IPluginModelBase> launchedBundles)
			throws IOException, CoreException {
		File devPropertiesFile = tempFolder.newFile("dev.properties").getCanonicalFile();
		Map<String, IPluginModelBase> bundlesMap = Map.of(HOST_BUNDLE_ID,
				launchedBundles.get(launchedBundles.size() - 1));
		String devPropertiesURL = ClasspathHelper.getDevEntriesProperties(devPropertiesFile.getPath(), bundlesMap);
		return loadProperties(devPropertiesURL);
	}

	private static Properties loadProperties(String devPropertiesURL) throws IOException {
		File propertiesFile = new File(new URL(devPropertiesURL).getPath());
		Properties devProperties = new Properties();
		try (InputStream stream = new FileInputStream(propertiesFile)) {
			devProperties.load(stream);
		}
		return devProperties;
	}

	private static void assertUnrelatedEntriesArePresent(Bundle hostBundle, Properties devProperties) {
		assertEquals("devPath2",
				devProperties.getProperty(hostBundle.getSymbolicName() + ";" + hostBundle.getVersion()));
		assertEquals("devPath3", devProperties.getProperty("some.other.plugin"));
		assertEquals("devPath4", devProperties.getProperty("some.other.plugin;2.0.0"));
	}
}
