/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.AntXMLContext;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignedContentEntry;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.eclipse.pde.build.internal.tests.ant.AntUtils;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.builder.BuildDirector;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;
import org.eclipse.pde.internal.build.site.QualifierReplacer;
import org.junit.After;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class PDETestCase {
	public static final String PROJECT_NAME = "pde.build";
	public static final String EQUINOX_COMMON = "org.eclipse.equinox.common";
	public static final String SIMPLE_CONFIGURATOR = "org.eclipse.equinox.simpleconfigurator";
	public static final String EQUINOX_EXECUTABLE = "org.eclipse.equinox.executable";
	public static final String EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher";
	public static final String EQUINOX_APP = "org.eclipse.equinox.app";
	public static final String EQUINOX_REGISTRY = "org.eclipse.equinox.registry";
	public static final String EQUINOX_PREFERENCES = "org.eclipse.equinox.preferences";
	public static final String CORE_JOBS = "org.eclipse.core.jobs";
	public static final String CORE_RUNTIME = "org.eclipse.core.runtime";
	public static final String OSGI = "org.eclipse.osgi";

	private IFolder buildFolder = null;

	protected void clearStatics() throws Exception {
		AbstractScriptGenerator.getConfigInfos().clear();
		BuildTimeSiteFactory.setInstalledBaseSite(null);
		AbstractScriptGenerator.setForceUpdateJar(false);
		QualifierReplacer.setGlobalQualifier(null);
		BuildDirector.p2Gathering = false;
	}

	@After
	public void cleanup() {
		// clean up after success
		if (buildFolder != null && buildFolder.exists() && !Boolean.getBoolean("pde.build.noCleanup")) {
			try {
				buildFolder.delete(true, null);
			} catch (CoreException e) {
			}
		}

	}

	protected IProject newTest() throws Exception {
		IProject builderProject = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		if (!builderProject.exists()) {
			builderProject.create(null);
		}
		if (!builderProject.isOpen())
			builderProject.open(null);

		return builderProject;
	}

	protected IFolder newTest(String folderName, String resources) throws Exception {
		clearStatics();

		IProject builderProject = newTest();

		// create build folder for this test
		buildFolder = builderProject.getFolder(folderName);
		if (buildFolder.exists()) {
			try {
				buildFolder.delete(true, null);
				buildFolder.create(true, true, null);
			} catch (CoreException e) {
			}
		} else {
			buildFolder.create(true, true, null);
		}

		URL resource = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID),
				IPath.fromOSString("/resources/" + resources), null);
		if (resource != null) {
			String path = FileLocator.toFileURL(resource).getPath();

			Utils.copyFiles(path, buildFolder.getLocation().toOSString());
			buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
		}

		return buildFolder;
	}

	protected IFolder newTest(String resources) throws Exception {
		return newTest(resources, resources);
	}

	protected void runBuild(IFolder buildFolder) throws Exception {
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"),
				IPath.fromOSString("/scripts/build.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();

		runAntScript(buildXMLPath, new String[] { "main" }, buildFolder.getLocation().toOSString(), null);
	}

	protected void runProductBuild(IFolder buildFolder) throws Exception {
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"),
				IPath.fromOSString("/scripts/productBuild/productBuild.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();

		runAntScript(buildXMLPath, new String[] { "main" }, buildFolder.getLocation().toOSString(), null);
	}

	protected void generateScripts(IFolder buildFolder, Properties generateProperties) throws Exception {
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"),
				IPath.fromOSString("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();

		runAntScript(buildXMLPath, new String[] { "generateScript" }, buildFolder.getLocation().toOSString(),
				generateProperties);
	}

	protected void runAntScript(String script, String[] targets, String antHome, Properties additionalProperties)
			throws Exception {
		runAntScript(script, targets, antHome, additionalProperties, null, null);
	}

	protected void runAntScript(String script, String[] targets, String antHome, Properties additionalProperties,
			String listener, String logger) throws Exception {
		String[] args = createAntRunnerArgs(script, targets, antHome, additionalProperties, listener, logger);
		try {
			AntRunner runner = new AntRunner();
			runner.run(args);
		} catch (InvocationTargetException e) {
			Path logPath = Path.of(antHome, "log.log");
			String logContent = Files.readString(logPath);
			System.err.println("### Ant log file content from " + logPath + ":");
			System.err.println(logContent);
			System.err.println("### log file end");
			Throwable target = e.getTargetException();
			if (target instanceof Exception)
				throw (Exception) target;
			throw e;
		}
	}

	protected String[] createAntRunnerArgs(String script, String[] targets, String antHome,
			Properties additionalProperties, String listener, String logger) {
		int numArgs = 5 + targets.length + (additionalProperties != null ? additionalProperties.size() : 0);
		if (listener != null)
			numArgs += 2;
		if (logger != null)
			numArgs += 2;
		String[] args = new String[numArgs];
		int idx = 0;
		args[idx++] = "-buildfile";
		args[idx++] = script;
		args[idx++] = "-logfile";
		args[idx++] = antHome + "/log.log";
		args[idx++] = "-Dbuilder=" + antHome;
		if (listener != null) {
			args[idx++] = "-listener";
			args[idx++] = listener;
		}
		if (logger != null) {
			args[idx++] = "-logger";
			args[idx++] = logger;
		}
		if (additionalProperties != null && additionalProperties.size() > 0) {
			Enumeration<Object> e = additionalProperties.keys();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = additionalProperties.getProperty(key);
				if (value.length() > 0)
					args[idx++] = "-D" + key + "=" + additionalProperties.getProperty(key);
				else
					args[idx++] = "";
			}
		}

		for (String target : targets) {
			args[idx++] = target;
		}
		return args;
	}

	/**
	 * Assert that the zip file contains at least the given entries
	 */
	public static void assertZipContents(IFolder buildFolder, String archive, Set<String> entries) throws Exception {
		assertZipContents(buildFolder, archive, entries, true);
	}

	public static void assertZipContents(File archiveFile, Set<String> entries) throws Exception {
		assertZipContents(archiveFile, entries, true);
	}

	public static void assertZipContents(File archiveFile, Set<String> entries, boolean assertEmpty) throws Exception {
		assertTrue(archiveFile.exists());

		try (ZipFile zip = new ZipFile(archiveFile)) {
			Enumeration<ZipEntry> e = zip.getEntries();
			while (e.hasMoreElements() && entries.size() > 0) {
				ZipEntry entry = e.nextElement();
				String name = entry.getName();
				if (entries.contains(name)) {
					if (!entry.isDirectory())
						assertTrue(entry.getSize() > 0);
					entries.remove(name);
				}
			}
		}
		if (assertEmpty)
			assertTrue("Missing entry in archive: " + entries, entries.isEmpty());
	}

	public static void assertZipContents(IFolder buildFolder, String archive, Set<String> entries, boolean assertEmpty)
			throws Exception {
		File folder = new File(buildFolder.getLocation().toOSString());
		File archiveFile = new File(folder, archive);
		assertZipContents(archiveFile, entries, assertEmpty);
	}

	/**
	 * Assert that the given resource exists and has size > 0
	 */
	public static void assertResourceFile(IFolder buildFolder, String fileName) throws Exception {
		buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
		IFile file = buildFolder.getFile(fileName);
		assertTrue(file.exists());

		File ioFile = file.getLocation().toFile();
		assertTrue(ioFile.length() > 0);
	}

	public static void assertResourceFile(IFile file) throws Exception {
		file.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(file.exists());
		File ioFile = file.getLocation().toFile();
		assertTrue(ioFile.length() > 0);
	}

	/**
	 * Assert that the given log file contains the given message The message is
	 * expected to be contained on a single line
	 */
	public static void assertLogContainsLine(IFile log, String msg) throws Exception {
		assertLogContainsLines(log, new String[] { msg });
	}

	/**
	 * Assert that the given log file contains the given lines Lines are expected to
	 * appear in order
	 */
	public static void assertLogContainsLines(IFile log, String[] lines) throws Exception {
		log.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		assertNotNull(log);
		assertTrue(log.exists());

		File logFile = log.getLocation().toFile();
		assertTrue(logFile.length() > 0);

		int idx = 0;
		List<String> logContent = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
			while (reader.ready()) {
				String line = reader.readLine();
				logContent.add(line);
				if (line.indexOf(lines[idx]) >= 0) {
					if (++idx >= lines.length) {
						reader.close();
						return;
					}
				}
			}
		}

		// Will always fail here (expected)
		String expected = Stream.of(lines).map(x -> x.trim()).collect(Collectors.joining("\n"));
		String actual = logContent.stream().map(x -> x.trim()).collect(Collectors.joining("\n"));
		assertEquals("Not found given lines in given order", expected, actual);
	}

	/**
	 * assert that the given xml file exists, has size > 0 and is a valid ant script
	 */

	public static Project assertValidAntScript(IFile buildXML) throws Exception {
		return assertValidAntScript(buildXML, null);
	}

	public static Project assertValidAntScript(IFile buildXML, Map<String, String> alternateTasks) throws Exception {
		assertResourceFile((IFolder) buildXML.getParent(), buildXML.getName());

		// Parse the build file using ant
		ProjectHelper2 helper = new ProjectHelper2();
		Project project = new Project();
		project.addReference("ant.projectHelper", helper); //$NON-NLS-1$

		AntXMLContext context = new AntXMLContext(project);
		project.addReference("ant.parsing.context", context);
		project.addReference("ant.targets", context.getTargets());
		context.setCurrentTargets(new HashMap<>());

		AntUtils.setupProject(project, alternateTasks);
		project.init();

		// this will throw an exception if it is not a valid ant script
		helper.parse(project, buildXML.getLocation().toFile(),
				new ProjectHelper2.RootHandler(context, new ProjectHelper2.MainHandler()));
		return project;
	}

	public static void assertJarVerifies(File jarFile) throws Exception {
		assertJarVerifies(jarFile, false);
	}

	public static void assertJarVerifies(File jarFile, boolean throwIfNotSigned) throws Exception {
		BundleContext context = Activator.getDefault().getContext();
		ServiceReference<SignedContentFactory> certRef = context.getServiceReference(SignedContentFactory.class);
		if (certRef == null)
			throw new IllegalStateException("The SignedContentFactory service is not available");
		SignedContentFactory certFactory = context.getService(certRef);
		try {
			SignedContent content = certFactory.getSignedContent(jarFile);
			if (content.isSigned()) {
				SignedContentEntry[] entries = content.getSignedEntries();
				for (SignedContentEntry entrie : entries) {
					entrie.verify();
				}
			} else if (throwIfNotSigned)
				throw new AssertionFailedException(jarFile.toString() + " is not signed.");
		} finally {
			context.ungetService(certRef);
		}
	}

	public void assertZipPermissions(IFile zip, String file, String permissions) throws Exception {
		if (Platform.getOS().equals("linux")) {
			IFolder tempFolder = org.eclipse.pde.build.internal.tests.Utils.createFolder(buildFolder,
					"permissions" + String.valueOf(Math.random()).substring(2, 7));
			try {
				String[] command = new String[] { "unzip", "-qq", zip.getLocation().toOSString(), file, "-d",
						tempFolder.getLocation().toOSString() };
				Process proc = Runtime.getRuntime().exec(command);
				proc.waitFor();

				IFile extractedFile = tempFolder.getFile(file);
				assertResourceFile(extractedFile);

				command = new String[] { "ls", "-la", extractedFile.getLocation().toOSString() };
				proc = Runtime.getRuntime().exec(command);
				Path dest = tempFolder.getFile("ls.out").getLocation().toFile().toPath();
				try (InputStream inputStream = proc.getInputStream()) {
					Files.copy(inputStream, dest, StandardCopyOption.REPLACE_EXISTING);
				}
				proc.waitFor();

				assertLogContainsLine(tempFolder.getFile("ls.out"), permissions);
			} finally {
				FileUtils.deleteAll(tempFolder.getLocation().toFile());
			}
		}
	}
}
