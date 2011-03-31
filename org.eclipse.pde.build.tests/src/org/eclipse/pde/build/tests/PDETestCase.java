/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.tests;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import junit.framework.TestCase;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.AntXMLContext;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.osgi.internal.provisional.verifier.CertificateVerifier;
import org.eclipse.osgi.internal.provisional.verifier.CertificateVerifierFactory;
import org.eclipse.pde.build.internal.tests.ant.AntUtils;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.builder.BuildDirector;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;
import org.eclipse.pde.internal.build.site.QualifierReplacer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class PDETestCase extends TestCase {
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

	protected void runTest() throws Throwable {
		super.runTest();

		//clean up after success
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

		URL resource = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/resources/" + resources), null);
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
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/build.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();

		runAntScript(buildXMLPath, new String[] {"main"}, buildFolder.getLocation().toOSString(), null);
	}

	protected void runProductBuild(IFolder buildFolder) throws Exception {
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/productBuild/productBuild.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();

		runAntScript(buildXMLPath, new String[] {"main"}, buildFolder.getLocation().toOSString(), null);
	}

	protected void generateScripts(IFolder buildFolder, Properties generateProperties) throws Exception {
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();

		runAntScript(buildXMLPath, new String[] {"generateScript"}, buildFolder.getLocation().toOSString(), generateProperties);
	}

	protected void runAntScript(String script, String[] targets, String antHome, Properties additionalProperties) throws Exception {
		runAntScript(script, targets, antHome, additionalProperties, null, null);
	}

	protected void runAntScript(String script, String[] targets, String antHome, Properties additionalProperties, String listener, String logger) throws Exception {
		String[] args = createAntRunnerArgs(script, targets, antHome, additionalProperties, listener, logger);
		try {
			AntRunner runner = new AntRunner();
			runner.run((Object) args);
		} catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			if (target instanceof Exception)
				throw (Exception) target;
			throw e;
		}
	}

	protected String[] createAntRunnerArgs(String script, String[] targets, String antHome, Properties additionalProperties, String listener, String logger) {
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
			Enumeration e = additionalProperties.keys();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = additionalProperties.getProperty(key);
				if (value.length() > 0)
					args[idx++] = "-D" + key + "=" + additionalProperties.getProperty(key);
				else
					args[idx++] = "";
			}
		}

		for (int i = 0; i < targets.length; i++) {
			args[idx++] = targets[i];
		}
		return args;
	}

	/**
	 * Assert that the zip file contains at least the given entries 
	 * @param buildFolder
	 * @param archive
	 * @param entries
	 * @throws Exception
	 */
	public static void assertZipContents(IFolder buildFolder, String archive, Set entries) throws Exception {
		assertZipContents(buildFolder, archive, entries, true);
	}

	public static void assertZipContents(File archiveFile, Set entries) throws Exception {
		assertZipContents(archiveFile, entries, true);
	}

	public static void assertZipContents(File archiveFile, Set entries, boolean assertEmpty) throws Exception {
		assertTrue(archiveFile.exists());

		ZipFile zip = new ZipFile(archiveFile);
		try {
			Enumeration e = zip.getEntries();
			while (e.hasMoreElements() && entries.size() > 0) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				String name = entry.getName();
				if (entries.contains(name)) {
					if (!entry.isDirectory())
						assertTrue(entry.getSize() > 0);
					entries.remove(name);
				}
			}
		} finally {
			zip.close();
		}
		if (assertEmpty)
			assertTrue("Missing entry in archive: " + entries, entries.size() == 0);
	}

	public static void assertZipContents(IFolder buildFolder, String archive, Set entries, boolean assertEmpty) throws Exception {
		File folder = new File(buildFolder.getLocation().toOSString());
		File archiveFile = new File(folder, archive);
		assertZipContents(archiveFile, entries, assertEmpty);
	}

	/**
	 * Assert that the given resource exists and has size > 0
	 * @param buildFolder
	 * @param fileName
	 * @throws Exception
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
	 * Assert that the given log file contains the given message
	 * The message is expected to be contained on a single line
	 * @param log
	 * @param msg
	 * @throws Exception
	 */
	public static void assertLogContainsLine(IFile log, String msg) throws Exception {
		assertLogContainsLines(log, new String[] {msg});
	}

	/**
	 * Assert that the given log file contains the given lines
	 * Lines are expected to appear in order
	 * @param log
	 * @param lines
	 * @throws Exception
	 */
	public static void assertLogContainsLines(IFile log, String[] lines) throws Exception {
		log.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		assertNotNull(log);
		assertTrue(log.exists());

		File logFile = log.getLocation().toFile();
		assertTrue(logFile.length() > 0);

		int idx = 0;
		BufferedReader reader = new BufferedReader(new FileReader(logFile));
		while (reader.ready()) {
			String line = reader.readLine();
			if (line.indexOf(lines[idx]) >= 0) {
				if (++idx >= lines.length) {
					reader.close();
					return;
				}
			}
		}
		reader.close();
		assertTrue(false);
	}

	/**
	 * assert that the given xml file exists, has size > 0 and is a valid ant script
	 * @param buildXML
	 * @throws Exception
	 */

	public static Project assertValidAntScript(IFile buildXML) throws Exception {
		return assertValidAntScript(buildXML, null);
	}

	public static Project assertValidAntScript(IFile buildXML, Map alternateTasks) throws Exception {
		assertResourceFile((IFolder) buildXML.getParent(), buildXML.getName());

		// Parse the build file using ant
		ProjectHelper2 helper = new ProjectHelper2();
		Project project = new Project();
		project.addReference("ant.projectHelper", helper); //$NON-NLS-1$

		AntXMLContext context = new AntXMLContext(project);
		project.addReference("ant.parsing.context", context);
		project.addReference("ant.targets", context.getTargets());
		context.setCurrentTargets(new HashMap());

		AntUtils.setupProject(project, alternateTasks);
		project.init();

		// this will throw an exception if it is not a valid ant script
		helper.parse(project, buildXML.getLocation().toFile(), new ProjectHelper2.RootHandler(context, new ProjectHelper2.MainHandler()));
		return project;
	}

	public static void assertJarVerifies(File jarFile) throws Exception {
		assertJarVerifies(jarFile, false);
	}

	public static void assertJarVerifies(File jarFile, boolean throwIfNotSigned) throws Exception {
		BundleContext context = Activator.getDefault().getContext();

		ServiceReference certRef = context.getServiceReference(CertificateVerifierFactory.class.getName());
		if (certRef == null)
			throw new IllegalStateException();
		CertificateVerifierFactory certFactory = (CertificateVerifierFactory) context.getService(certRef);
		try {
			CertificateVerifier verifier = certFactory.getVerifier(jarFile);
			if (verifier.isSigned())
				verifier.checkContent();
			else if (throwIfNotSigned)
				throw new AssertionFailedException(jarFile.toString() + " is not signed.");
		} finally {
			context.ungetService(certRef);
		}

	}

	public void assertZipPermissions(IFile zip, String file, String permissions) throws Exception {
		if (Platform.getOS().equals("linux")) {
			IFolder tempFolder = org.eclipse.pde.build.internal.tests.Utils.createFolder(buildFolder, "permissions" + String.valueOf(Math.random()).substring(2, 7));
			try {
				String[] command = new String[] {"unzip", "-qq", zip.getLocation().toOSString(), file, "-d", tempFolder.getLocation().toOSString()};
				Process proc = Runtime.getRuntime().exec(command);
				proc.waitFor();

				IFile extractedFile = tempFolder.getFile(file);
				assertResourceFile(extractedFile);

				command = new String[] {"ls", "-la", extractedFile.getLocation().toOSString()};
				proc = Runtime.getRuntime().exec(command);
				Utils.transferStreams(proc.getInputStream(), new FileOutputStream(tempFolder.getFile("ls.out").getLocation().toFile()));
				proc.waitFor();

				assertLogContainsLine(tempFolder.getFile("ls.out"), permissions);
			} finally {
				FileUtils.deleteAll(tempFolder.getLocation().toFile());
			}
		}
	}
}
