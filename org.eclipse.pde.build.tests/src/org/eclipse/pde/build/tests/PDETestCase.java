/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

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
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;
import org.eclipse.pde.internal.build.site.QualifierReplacer;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

public abstract class PDETestCase extends TestCase {
	public static final String PROJECT_NAME = "org.eclipse.pde.build.tests.builder";

	private IFolder buildFolder = null;
	
	protected void clearStatics() throws Exception {
		AbstractScriptGenerator.getConfigInfos().clear();
		BuildTimeSiteFactory.setInstalledBaseSite(null);
		AbstractScriptGenerator.setForceUpdateJar(false);
		//TODO: bug 192904, need to setGlobalQualifier to null
		QualifierReplacer.setGlobalQualifier("");
	}

	protected void runTest() throws Throwable {
		super.runTest();
		
		//clean up after success
		if (buildFolder != null && buildFolder.exists()) {
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

	protected IFolder newTest(String resources) throws Exception {
		clearStatics();

		IProject builderProject = newTest();

		// create build folder for this test
		buildFolder = builderProject.getFolder(resources);
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

			IOverwriteQuery query = new IOverwriteQuery() {
				public String queryOverwrite(String pathString) {
					return ALL;
				}
			};
			List files = new ArrayList(1);
			files.add(new File(path));
			ImportOperation op = new ImportOperation(new Path(PROJECT_NAME), FileSystemStructureProvider.INSTANCE, query, files);
			op.setCreateContainerStructure(false);
			op.run(null);
		}

		return buildFolder;
	}

	protected void runBuild(IFolder buildFolder) throws Exception {
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/build.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();

		runAntScript(buildXMLPath, new String[] {"main"}, buildFolder.getLocation().toOSString(), null);
	}

	protected void generateScripts(IFolder buildFolder, Properties generateProperties) throws Exception {
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();

		runAntScript(buildXMLPath, new String[] {"generateScript"}, buildFolder.getLocation().toOSString(), generateProperties);
	}

	protected void runAntScript(String script, String[] targets, String antHome, Properties additionalProperties) throws Exception {
		AntRunner runner = new AntRunner();
		String[] args = new String[5 + targets.length + (additionalProperties != null ? additionalProperties.size() : 0)];
		int idx = 0;
		args[idx++] = "-buildfile";
		args[idx++] = script;
		args[idx++] = "-logfile";
		args[idx++] = antHome + "/log.log";
		args[idx++] = "-Dbuilder=" + antHome;
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
		try {
			runner.run((Object) args);
		} catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			if (target instanceof Exception)
				throw (Exception)target;
			throw e;
		}
	}

	/**
	 * Assert that the zip file contains at least the given entries 
	 * @param buildFolder
	 * @param archive
	 * @param entries
	 * @throws Exception
	 */
	public static void assertZipContents(IFolder buildFolder, String archive, Set entries) throws Exception {
		File folder = new File(buildFolder.getLocation().toOSString());
		File archiveFile = new File(folder, archive);
		assertTrue(archiveFile.exists());

		ZipFile zip = new ZipFile(archiveFile);
		try {
			Enumeration e = zip.getEntries();
			while (e.hasMoreElements() && entries.size() > 0) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				String name = entry.getName();
				if (entries.contains(name)) {
					assertTrue(entry.getSize() > 0);
					entries.remove(name);
				}
			}
		} finally {
			zip.close();
		}
		assertTrue(entries.size() == 0);
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
	public static void assertLogContainsLines(IFile log, String [] lines) throws Exception {
		assertNotNull(log);
		assertTrue(log.exists());

		File logFile = log.getLocation().toFile();
		assertTrue(logFile.length() > 0);

		int idx = 0;
		CharSequence sequence = lines[idx].subSequence(0, lines[idx].length());
		BufferedReader reader = new BufferedReader(new FileReader(logFile));
		while (reader.ready()) {
			String line = reader.readLine();
			if (line.contains(sequence)) {
				if (++idx >= lines.length) {
					reader.close();
					return;
				}
				sequence = lines[idx].subSequence(0, lines[idx].length());
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
	public static void assertValidAntScript(IFile buildXML) throws Exception {
		assertResourceFile((IFolder) buildXML.getParent(), buildXML.getName());
		
		// Parse the build file using ant
		ProjectHelper2 helper = new ProjectHelper2();
		Project project = new Project();
		project.addReference("ant.projectHelper", helper); //$NON-NLS-1$
		
		AntXMLContext context = new AntXMLContext(project);
        project.addReference("ant.parsing.context", context);
        project.addReference("ant.targets", context.getTargets());
        context.setCurrentTargets(new HashMap());
        
        // this will throw an exception if it is not a valid ant script
        helper.parse(project, buildXML.getLocation().toFile(), new ProjectHelper2.RootHandler(context, new ProjectHelper2.MainHandler()));
	}
}
