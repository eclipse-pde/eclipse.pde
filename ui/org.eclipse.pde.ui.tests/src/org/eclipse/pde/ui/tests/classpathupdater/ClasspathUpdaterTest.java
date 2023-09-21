/*******************************************************************************
 *  Copyright (c) 2023, 2023 Michael Haubenwallner and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Michael Haubenwallner - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.classpathupdater;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.wizards.tools.UpdateClasspathJob;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class ClasspathUpdaterTest {

	private static IProject project;
	private static IJavaProject jProject;

	private static IClasspathEntry[] originalClasspath;
	private static String originalTestPluginPattern;
	private static Boolean expectIsTest;
	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		project = ProjectUtils.importTestProject("tests/projects/classpathupdater");
		assertTrue("Project was not created", project.exists());
		jProject = JavaCore.create(project);
		originalClasspath = jProject.getRawClasspath();
		originalTestPluginPattern = PDECore.getDefault().getPreferencesManager()
				.getString(ICoreConstants.TEST_PLUGIN_PATTERN);
		expectIsTest = null;
	}

	@After
	public void restoreOriginalClasspath() throws Exception {
		jProject.setRawClasspath(originalClasspath, null);
	}

	@After
	public void restoreOriginalTestPluginPattern() {
		PDECore.getDefault().getPreferencesManager().setValue(ICoreConstants.TEST_PLUGIN_PATTERN,
				originalTestPluginPattern);
		expectIsTest = null;
	}

	private void setTestPluginPattern() {
		PDECore.getDefault().getPreferencesManager().setValue(ICoreConstants.TEST_PLUGIN_PATTERN, project.getName());
		expectIsTest = Boolean.TRUE;
	}

	@Test
	public void test_PreserveAttributes() throws Exception {
		runUpdateClasspathJob();
		assertClasspathAttribute("JavaSE-17", IClasspathAttribute.MODULE, true, Boolean::valueOf);
		assertClasspathAttribute("library.jar", IClasspathAttribute.TEST, true, Boolean::valueOf);
		assertClasspathProperty("library.jar", "exported=true", e -> e.isExported());
		assertClasspathProperty("library.jar", "no source", e -> e.getSourceAttachmentPath() == null);
		assertClasspathAttribute("A.jar", IClasspathAttribute.TEST, null, Boolean::valueOf);
		assertClasspathProperty("A.jar", "exported=false", e -> !e.isExported());
		assertClasspathProperty("A.jar", "default source",
				e -> "Asrc.zip".equals(nullOr(e.getSourceAttachmentPath(), IPath::lastSegment)));
		assertClasspathProperty("src", "exported=false", e -> !e.isExported());
		assertClasspathAttribute("src", IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, true, Boolean::valueOf);
		assertClasspathAttribute("src", IClasspathAttribute.TEST, true, Boolean::valueOf);
		assertClasspathProperty("tests", "exported=false", e -> !e.isExported());
		assertClasspathAttribute("tests", IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, null, Boolean::valueOf);
		assertClasspathAttribute("tests", IClasspathAttribute.TEST, expectIsTest, Boolean::valueOf);
		assertClasspathOrder("A.jar", "src", "org.eclipse.pde.core.requiredPlugins", "JavaSE-17", "SOMEVAR",
				"library.jar", "tests");
	}

	@Test
	public void test_PreserveAttributes_WithTestPluginName() throws Exception {
		setTestPluginPattern();
		test_PreserveAttributes();
	}

	@Test
	public void test_CreateFromScratch() throws Exception {
		jProject.setRawClasspath(new IClasspathEntry[0], null);

		runUpdateClasspathJob();
		assertClasspathAttribute("JavaSE-17", IClasspathAttribute.MODULE, null, Boolean::valueOf);
		assertClasspathAttribute("library.jar", IClasspathAttribute.TEST, null, Boolean::valueOf);
		assertClasspathProperty("library.jar", "exported=true", e -> e.isExported());
		assertClasspathProperty("library.jar", "no source", e -> e.getSourceAttachmentPath() == null);
		assertClasspathAttribute("A.jar", IClasspathAttribute.TEST, null, Boolean::valueOf);
		assertClasspathProperty("A.jar", "exported=true", e -> e.isExported());
		assertClasspathProperty("A.jar", "default source",
				e -> "Asrc.zip".equals(nullOr(e.getSourceAttachmentPath(), IPath::lastSegment)));
		assertClasspathProperty("src", "exported=false", e -> !e.isExported());
		assertClasspathAttribute("src", IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, null, Boolean::valueOf);
		assertClasspathAttribute("src", IClasspathAttribute.TEST, expectIsTest, Boolean::valueOf);
		assertClasspathProperty("tests", "exported=false", e -> !e.isExported());
		assertClasspathAttribute("tests", IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, null, Boolean::valueOf);
		assertClasspathAttribute("tests", IClasspathAttribute.TEST, expectIsTest, Boolean::valueOf);
		assertClasspathOrder("JavaSE-17", "org.eclipse.pde.core.requiredPlugins", "library.jar", "A.jar", "src",
				"tests");
	}

	@Test
	public void test_CreateFromScatch_WithTestPluginName() throws Exception {
		setTestPluginPattern();
		test_CreateFromScratch();
	}

	@Test
	public void test_ChangeSourceAttachment() throws Exception {
		Map<String, IPath> sourceMap = Map.of( //
				"A.jar", IPath.fromOSString("library.jar"), //
				"library.jar", IPath.fromOSString("A.jar"));

		IPluginModelBase model = PluginRegistry.findModel(project.getProject());

		IClasspathEntry[] cp = ClasspathComputer.getClasspath(project, model, sourceMap, false, false);
		jProject.setRawClasspath(cp, null);

		assertClasspathAttribute("JavaSE-17", IClasspathAttribute.MODULE, true, Boolean::valueOf);
		assertClasspathAttribute("library.jar", IClasspathAttribute.TEST, true, Boolean::valueOf);
		assertClasspathProperty("library.jar", "exported=true", e -> e.isExported());
		assertClasspathProperty("library.jar", "overridden source",
				e -> "A.jar".equals(nullOr(e.getSourceAttachmentPath(), IPath::lastSegment)));
		assertClasspathAttribute("A.jar", IClasspathAttribute.TEST, null, Boolean::valueOf);
		assertClasspathProperty("A.jar", "exported=false", e -> !e.isExported());
		assertClasspathProperty("A.jar", "overridden source",
				e -> "library.jar".equals(nullOr(e.getSourceAttachmentPath(), IPath::lastSegment)));
		assertClasspathProperty("src", "exported=false", e -> !e.isExported());
		assertClasspathAttribute("src", IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, true, Boolean::valueOf);
		assertClasspathAttribute("src", IClasspathAttribute.TEST, true, Boolean::valueOf);
		assertClasspathProperty("tests", "exported=false", e -> !e.isExported());
		assertClasspathAttribute("tests", IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, null, Boolean::valueOf);
		assertClasspathAttribute("tests", IClasspathAttribute.TEST, expectIsTest, Boolean::valueOf);
		assertClasspathOrder("A.jar", "src", "org.eclipse.pde.core.requiredPlugins", "JavaSE-17", "SOMEVAR",
				"library.jar", "tests");
	}

	@Test
	public void test_ChangeSourceAttachment_WithTestPluginName() throws Exception {
		setTestPluginPattern();
		test_ChangeSourceAttachment();
	}

	private void runUpdateClasspathJob() throws InterruptedException {
		IPluginModelBase model = PluginRegistry.findModel(project.getProject());
		Job job = UpdateClasspathJob.scheduleFor(List.of(model), false);
		job.join();
		assertTrue("Update Classpath Job failed", job.getResult().isOK());
	}

	private <T> void assertClasspathAttribute(String entryName, String attrName, T expectedValue,
			Function<String, T> parser) throws JavaModelException {
		IClasspathEntry entry = findClasspathEntry(entryName);
		assertNotNull("Classpath entry for " + entryName + " is missing", entry);
		String attrValue = findClasspathAttributeValue(entry, attrName);
		T current = attrValue != null ? parser.apply(attrValue) : null; // null: attribute not set
		assertEquals("Classpath entry for '" + entry.getPath().lastSegment() + "': attribute '" + attrName
				+ "' is not '" + expectedValue + "'", expectedValue, current);
	}

	private String findClasspathAttributeValue(IClasspathEntry entry, String name) {
		return Arrays.stream(entry.getExtraAttributes()) //
				.filter(a -> name.equals(a.getName())).map(IClasspathAttribute::getValue) //
				.findFirst().orElse(null);
	}

	private void assertClasspathProperty(String entryName, String expectedValue, Predicate<IClasspathEntry> checker)
			throws JavaModelException {
		IClasspathEntry entry = findClasspathEntry(entryName);
		assertTrue("Classpath entry for '" + entryName + "' has not set '" + expectedValue + "'", checker.test(entry));
	}

	private void assertClasspathOrder(String... names) throws Exception {
		var actualNames = Arrays.stream(jProject.getRawClasspath()).map(e -> e.getPath().lastSegment()).toList();
		assertEquals(Arrays.asList(names), actualNames);
	}

	private IClasspathEntry findClasspathEntry(String name) throws JavaModelException {
		Optional<IClasspathEntry> entry = Arrays.stream(jProject.getRawClasspath())
				.filter(e -> name.equals(e.getPath().lastSegment())).findFirst();
		assertTrue("Classpath entry for " + name + " is missing", entry.isPresent());
		return entry.get();
	}

	private static <T, R> R nullOr(T obj, Function<T, R> f) {
		return obj == null ? null : f.apply(obj);
	}
}
