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
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.bundle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
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
		createWorkspacePluginProjects( //
				bundle(hostBundle.getSymbolicName(), "2.0.0"), //
				bundle(hostBundle.getSymbolicName(), hostBundle.getVersion().toString()));
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
		public List<ISourceContainer> getSourceContainers(String location, String id) throws CoreException {
			return super.getSourceContainers(location, id);
		}
	}

	/**
	 * Checks that a created dev properties file will recognise the modified
	 * classpath
	 */
	@Test
	public void testGetDevProperties() throws Exception {
		mockTPWithRunningPlatformAndBundles(); // running-platform only

		File devProperties = tempFolder.newFile("dev.properties").getCanonicalFile();
		String devPropertiesURL = ClasspathHelper.getDevEntriesProperties(devProperties.getPath(), false);

		Properties properties = loadProperties(devPropertiesURL);

		String expectedDevCP = project.getFolder("cpe").getLocation().toPortableString();
		assertEquals(expectedDevCP, properties.get(bundleName));
		assertEquals(expectedDevCP, properties.get(bundleName + ";1.0.0.qualifier"));
	}

	/**
	 * Checks that the source lookup path of a project is updated from the API
	 */
	@Test
	public void testSourceLookupPath() throws Exception {
		mockTPWithRunningPlatformAndBundles(); // running-platform only

		PDESourceLookupDirector d = new PDESourceLookupDirector();
		_PDESourceLookupQuery q = new _PDESourceLookupQuery(d, project);

		List<ISourceContainer> containers = q.getSourceContainers(project.getLocation().toOSString(), bundleName);

		assertEquals(2, containers.size());
		assertEquals(JavaCore.create(project), ((JavaProjectSourceContainer) containers.get(0)).getJavaProject());
		assertEquals(project.getFolder("cpe").getLocation().toFile(),
				((DirectorySourceContainer) containers.get(1)).getDirectory());
	}

	@Test
	public void testGetDevProperties_workspacePlugin_devEntryWithAndWithoutVersion() throws Exception {

		getHostBundleAndMockDevProperties();

		mockTPWithBundles(); // empty TP

		IPluginModelBase wsModel = findWorkspaceModel(HOST_BUNDLE_ID, "2.0.0");

		Properties devProperties = createDevEntryProperties(List.of(wsModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("bin", devProperties.getProperty(HOST_BUNDLE_ID));
		assertEquals("bin", devProperties.getProperty(HOST_BUNDLE_ID + ";2.0.0"));
		assertEquals(3, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_workspacePluginWithSameVersionLikeHostBundle_devEntryWithAndWithoutVersion()
			throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		mockTPWithBundles(); // empty TP

		IPluginModelBase wsModel = findWorkspaceModel(HOST_BUNDLE_ID, hostBundleVersion);

		Properties devProperties = createDevEntryProperties(List.of(wsModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("bin", devProperties.getProperty(HOST_BUNDLE_ID));
		assertEquals("bin", devProperties.getProperty(HOST_BUNDLE_ID + ";" + hostBundleVersion));
		assertEquals(3, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_bundleFromRunningPlatform_wovenDevEntryWithAndWithoutVersion() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		mockTPWithRunningPlatformAndBundles(); // running-platform only

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, hostBundleVersion);

		Properties devProperties = createDevEntryProperties(List.of(hostModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath2", devProperties.getProperty(HOST_BUNDLE_ID));
		assertEquals("devPath2", devProperties.getProperty(HOST_BUNDLE_ID + ";" + hostBundleVersion));
		assertEquals(3, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_jarTPBundle_noDevEntries() throws Exception {

		getHostBundleAndMockDevProperties();

		// pretend there is only a jar-bundle in the TP that has the same
		// name and version like a woven plug-in from the host
		mockTPWithBundles( //
				bundle(HOST_BUNDLE_ID, "1.0.0"));

		IPluginModelBase tpModel = findTargetModel(HOST_BUNDLE_ID, "1.0.0");

		Properties devProperties = createDevEntryProperties(List.of(tpModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertNull(devProperties.getProperty(HOST_BUNDLE_ID));
		assertEquals(1, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_jarTPBundleWithSameVersionLikeHostBundle_noDevEntries() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		// pretend there is only a jar-bundle in the TP that has the same
		// name and version like a woven plug-in from the host
		mockTPWithBundles(//
				bundle(HOST_BUNDLE_ID, hostBundleVersion));

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, hostBundleVersion);

		Properties devProperties = createDevEntryProperties(List.of(hostModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertNull(devProperties.getProperty(HOST_BUNDLE_ID));
		assertEquals(1, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_workspaceAndJarTPBundle_oneEmptyDevEntryAndOneWithAndWithoutVersion()
			throws Exception {

		getHostBundleAndMockDevProperties();

		mockTPWithBundles( //
				bundle(HOST_BUNDLE_ID, "1.0.0"));

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, "1.0.0");
		IPluginModelBase wsModel = findWorkspaceModel(HOST_BUNDLE_ID, "2.0.0");

		Properties devProperties = createDevEntryProperties(List.of(hostModel, wsModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("bin", devProperties.getProperty(HOST_BUNDLE_ID)); // last
		assertEquals("", devProperties.getProperty(HOST_BUNDLE_ID + ";1.0.0"));
		assertEquals("bin", devProperties.getProperty(HOST_BUNDLE_ID + ";2.0.0"));
		assertEquals(4, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_HostAndJarBundle_oneEmptyDevEntryAndOneWithAndWithoutVersion() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		mockTPWithRunningPlatformAndBundles( //
				bundle(HOST_BUNDLE_ID, "1.0.0"));

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, hostBundleVersion);
		IPluginModelBase tpModel = findTargetModel(HOST_BUNDLE_ID, "1.0.0");

		Properties devProperties = createDevEntryProperties(List.of(tpModel, hostModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("devPath2", devProperties.getProperty(HOST_BUNDLE_ID)); // last
		assertEquals("", devProperties.getProperty(HOST_BUNDLE_ID + ";1.0.0"));
		assertEquals("devPath2", devProperties.getProperty(HOST_BUNDLE_ID + ";" + hostBundleVersion));
		assertEquals(4, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_workspaceAndHostBundle_twoDevEntriesWithAndWithoutVersion() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		mockTPWithRunningPlatformAndBundles(); // running-platform only

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, hostBundleVersion);
		IPluginModelBase wsModel = findWorkspaceModel(HOST_BUNDLE_ID, "2.0.0");

		Properties devProperties = createDevEntryProperties(List.of(hostModel, wsModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		assertEquals("bin", devProperties.getProperty(HOST_BUNDLE_ID)); // last
		assertEquals("bin", devProperties.getProperty(HOST_BUNDLE_ID + ";2.0.0"));
		assertEquals("devPath2", devProperties.getProperty(HOST_BUNDLE_ID + ";" + hostBundleVersion));
		assertEquals(4, devProperties.size()); // assert no more entries
	}

	@Test
	public void testGetDevProperties_mixedWorkspaceAndHostAndJarTPBundle_onlyUsedPlatformBundles() throws Exception {

		Bundle hostBundle = getHostBundleAndMockDevProperties();
		String hostBundleVersion = hostBundle.getVersion().toString();

		mockTPWithRunningPlatformAndBundles( //
				bundle(HOST_BUNDLE_ID, "1.0.0"));

		IPluginModelBase hostModel = findTargetModel(HOST_BUNDLE_ID, hostBundleVersion);
		IPluginModelBase tpModel = findTargetModel(HOST_BUNDLE_ID, "1.0.0");
		IPluginModelBase wsModel = findWorkspaceModel(HOST_BUNDLE_ID, "2.0.0");

		Properties devProperties = createDevEntryProperties(List.of(hostModel, wsModel, tpModel));

		assertEquals("true", devProperties.getProperty("@ignoredot@"));
		// jar-bundle from tp should not be considered for non-version entry
		assertEquals("bin", devProperties.getProperty(HOST_BUNDLE_ID));
		assertEquals("", devProperties.getProperty(HOST_BUNDLE_ID + ";1.0.0"));
		assertEquals("bin", devProperties.getProperty(HOST_BUNDLE_ID + ";2.0.0"));
		assertEquals("devPath2", devProperties.getProperty(HOST_BUNDLE_ID + ";" + hostBundleVersion));
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

	@SafeVarargs
	private static void createWorkspacePluginProjects(
			Entry<NameVersionDescriptor, Map<String, String>>... workspacePlugins) throws CoreException {
		Set<NameVersionDescriptor> descriptions = Map.ofEntries(workspacePlugins).keySet();
		List<IProject> pluginProjects = ProjectUtils.createWorkspacePluginProjects(descriptions);
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

	@SafeVarargs
	private void mockTPWithBundles(Entry<NameVersionDescriptor, Map<String, String>>... bundles) throws Exception {
		Path jarsDirectory = tempFolder.newFolder("TPJarsDirectory").toPath();
		TargetPlatformUtil.setDummyBundlesAsTarget(Map.ofEntries(bundles), List.of(), jarsDirectory);
	}

	@SafeVarargs
	private void mockTPWithRunningPlatformAndBundles(
			Entry<NameVersionDescriptor, Map<String, String>>... additionalBundles) throws Exception {
		Path jarsDirectory = tempFolder.newFolder("TPJarsDirectory").toPath();
		TargetPlatformUtil.setRunningPlatformWithDummyBundlesAsTarget(b -> b.getSymbolicName().equals(HOST_BUNDLE_ID),
				Map.ofEntries(additionalBundles), Set.of(), jarsDirectory);
	}

	private Properties createDevEntryProperties(List<IPluginModelBase> launchedBundles)
			throws IOException, CoreException {
		File devPropertiesFile = tempFolder.newFile("dev.properties").getCanonicalFile();
		Map<String, List<IPluginModelBase>> bundlesMap = Map.of(HOST_BUNDLE_ID, launchedBundles);
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
}
