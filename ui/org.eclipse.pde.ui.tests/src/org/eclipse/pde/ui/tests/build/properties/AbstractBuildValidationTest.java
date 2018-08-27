/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.build.properties;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.PropertyResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import junit.framework.TestCase;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.correction.ResolutionGenerator;
import org.eclipse.pde.ui.tests.PDETestsPlugin;
import org.eclipse.pde.ui.tests.target.LocalTargetDefinitionTests;
import org.eclipse.ui.IMarkerResolution;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Abstract test case for tests that check the build.properties builder and its associated quick fixes.
 *
 * Extracts the necessary build.properties testing files and deletes them on tear-down.
 *
 * @since 3.6
 * @see BuildPropertiesValidationTest
 */
public abstract class AbstractBuildValidationTest extends TestCase {

	private static final String MARKER = "marker";
	private static final String MULTIPLE_MARKERS = "multipleMarkers";

	@Override
	protected void setUp() throws Exception {
		URL location = PDETestsPlugin.getBundleContext().getBundle().getEntry("/tests/build.properties/build.properties.tests.zip");
		File projectFile = new File(FileLocator.toFileURL(location).getFile());
		assertTrue("Could not find test zip file at " + projectFile, projectFile.isFile());
		doUnZip(PDETestsPlugin.getDefault().getStateLocation().removeLastSegments(2), "/tests/build.properties/build.properties.tests.zip");

		projectFile = PDETestsPlugin.getDefault().getStateLocation().removeLastSegments(3).toFile();
		File[] projects = projectFile.listFiles((FileFilter) pathname -> {
			int index = pathname.getName().lastIndexOf('.');
			if (index > 1 && pathname.isDirectory()) { // look out for "CVS"
														// files in the
														// workspace
				return true;
			}
			return false;
		});
		for (File projectFileName : projects) {
			IProject project = findProject(projectFileName.getName());
			if (project.exists()) {
				project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				project.delete(true, new NullProgressMonitor());
			}
			project.create(new NullProgressMonitor());
			project.open(new NullProgressMonitor());

		}
	}

	/**
	 * Runs the quick fix and verifies that the marker is now gone.
	 *
	 * @param buildProperty		build.properties file (on which markers will looked for)
	 * @param expectedValues	properties file from which expected values will be read
	 * @throws CoreException
	 */
	protected void verifyQuickFixes(IResource buildProperty, PropertyResourceBundle expectedValues) throws CoreException {
		IMarker[] markers = buildProperty.findMarkers(PDEMarkerFactory.MARKER_ID, true, IResource.DEPTH_INFINITE);
		ResolutionGenerator resGen = new ResolutionGenerator();
		for (IMarker marker : markers) {
			if (resGen.hasResolutions(marker)) {
				String markerEntry = (String) marker.getAttribute(PDEMarkerFactory.BK_BUILD_ENTRY);
				IMarkerResolution[] resolutions = resGen.getResolutions(marker);
				String quickFixindex = getProperty(expectedValues, markerEntry, "quickfix");
				resolutions[Integer.parseInt(quickFixindex.trim())].run(marker);
				buildProject(marker.getResource().getProject());
				assertFalse("Quick fix verification failed for the project " + buildProperty.getProject().getName() , marker.exists());
			}
		}
	}

	/**
	 * Verify the problem markers on the build.properties
	 * @param buildProperty		build.properties file (on which markers will looked for)
	 * @param expectedValues	properties file from which expected values will be read
	 * @param severity			expected severity of the problem markers
	 * @throws CoreException
	 */
	protected void verifyBuildPropertiesMarkers(IResource buildProperty, PropertyResourceBundle expectedValues, int severity) throws CoreException {
		IMarker[] markers = buildProperty.findMarkers(PDEMarkerFactory.MARKER_ID, true, IResource.DEPTH_INFINITE);

		String message;
		String markercount = getProperty(expectedValues, "count");

		String projectName = buildProperty.getProject().getName();
		message = "Marker count for the project " + projectName;
		assertEquals(message, markercount, String.valueOf(markers.length));

		int markerSeverity;
		switch (severity) {
		case CompilerFlags.ERROR :
			markerSeverity = IMarker.SEVERITY_ERROR;
			break;
		case CompilerFlags.WARNING :
			markerSeverity = IMarker.SEVERITY_WARNING;
			break;
		default :
			markerSeverity = IMarker.SEVERITY_INFO;
		}

		for (IMarker marker : markers) {
			message = "Marker severity for the project " + projectName;
			String markerEntry = (String) marker.getAttribute(PDEMarkerFactory.BK_BUILD_ENTRY);
			assertEquals(message, markerSeverity, getIntAttribute(marker, IMarker.SEVERITY));

			message = "Marker type for the project " + projectName;
			String markerType = getProperty(expectedValues, markerEntry, PDEMarkerFactory.CAT_ID);
			assertEquals(message, markerType, getStringAttribute(marker, PDEMarkerFactory.CAT_ID));

			message = "Marker line number for build.properties" + projectName;
			int lineNumber;
			try {
				lineNumber = Integer.parseInt(getProperty(expectedValues, markerEntry, IMarker.LINE_NUMBER));
			} catch (Exception e) {
				message = "Could not read expected line number for the project " + projectName;
				lineNumber = 0;
			}
			assertEquals(message, lineNumber, getIntAttribute(marker, IMarker.LINE_NUMBER));

			message = "Marker build entry token value for the project " + projectName;
			String multipleMarkers = getProperty(expectedValues, markerEntry, MULTIPLE_MARKERS);
			String tokenValue = getProperty(expectedValues, markerEntry, PDEMarkerFactory.BK_BUILD_TOKEN);
			if (multipleMarkers.equalsIgnoreCase(Boolean.TRUE.toString())) {
				boolean contains = tokenValue.indexOf(getStringAttribute(marker, PDEMarkerFactory.BK_BUILD_TOKEN)) >= 0;
				assertTrue(message, contains);
			} else {
				assertEquals(message, tokenValue, getStringAttribute(marker, PDEMarkerFactory.BK_BUILD_TOKEN));
			}
		}

	}

	private int getIntAttribute(IMarker marker, String property) {
		Integer value;
		try {
			value = (Integer) marker.getAttribute(property);
		} catch (CoreException e) {
			return 0;
		}

		if (value == null)
			return 0;
		return value.intValue();
	}

	private String getStringAttribute(IMarker marker, String property) {
		String value;
		try {
			value = (String) marker.getAttribute(property);
		} catch (CoreException e) {
			value = "";
		}

		if (value == null || value.equalsIgnoreCase("\"\""))
			value = "";
		return value.trim();
	}

	private String getProperty(PropertyResourceBundle propertyBundle, String property) {
		String value;
		try {
			value = propertyBundle.getString(MARKER + '.' + property);
		} catch (Exception e) {
			value = "";
		}
		if (value == null || value.equalsIgnoreCase("\"\""))
			value = "";
		return value.trim();
	}

	private String getProperty(PropertyResourceBundle propertyBundle, String entry, String property) {
		return getProperty(propertyBundle, entry + '.' + property);
	}

	/**
	 * Unzips the given archive to the specified location.
	 *
	 * @param location path in the local file system
	 * @param archivePath path to archive relative to the test plug-in
	 * @throws IOException
	 */
	protected IPath doUnZip(IPath location, String archivePath) throws IOException {
		URL zipURL = PDETestsPlugin.getBundleContext().getBundle().getEntry(archivePath);
		Path zipPath = new Path(new File(FileLocator.toFileURL(zipURL).getFile()).getAbsolutePath());
		try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			IPath parent = location.removeLastSegments(1);
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (!entry.isDirectory()) {
					IPath entryPath = parent.append(entry.getName());
					File dir = entryPath.removeLastSegments(1).toFile();
					dir.mkdirs();
					File file = entryPath.toFile();
					file.createNewFile();
					try (InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry));
							BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
						byte[] bytes = LocalTargetDefinitionTests.getInputStreamAsByteArray(inputStream, -1);
						outputStream.write(bytes);
					}
				}
			}
			return parent;
		}
	}

	/**
	 * Build the given project and wait till the build the complete
	 * @param project	project to be build
	 * @return			<code>true</code> if the project got build successfully. <code>false</code> otherwise.
	 * @throws CoreException
	 */
	protected boolean buildProject(IProject project) throws CoreException {
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		boolean wasInterrupted = false;
		do {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
				wasInterrupted = false;
			} catch (OperationCanceledException e) {
				return false;
			} catch (InterruptedException e) {
				wasInterrupted = true;
			}
		} while (wasInterrupted);
		return true;
	}

	/**
	 * Set the project specific preferences on build.properties
	 *
	 * @param project	project for which the preferences are to be set
	 * @param severity	severity level
	 * @throws BackingStoreException
	 */
	protected void setPreferences(IProject project, int severity) throws BackingStoreException {
		ProjectScope scope = new ProjectScope(project);
		IEclipsePreferences projectPrefs = scope.getNode(PDE.PLUGIN_ID);
		projectPrefs.putInt(CompilerFlags.P_BUILD, severity);
		projectPrefs.putInt(CompilerFlags.P_BUILD_MISSING_OUTPUT, severity);
		projectPrefs.putInt(CompilerFlags.P_BUILD_SOURCE_LIBRARY, severity);
		projectPrefs.putInt(CompilerFlags.P_BUILD_OUTPUT_LIBRARY, severity);
		projectPrefs.putInt(CompilerFlags.P_BUILD_SRC_INCLUDES, severity);
		projectPrefs.putInt(CompilerFlags.P_BUILD_BIN_INCLUDES, severity);
		projectPrefs.putInt(CompilerFlags.P_BUILD_JAVA_COMPLIANCE, severity);
		projectPrefs.flush();
		projectPrefs.sync();
	}

	/**
	 * Sets the given project specific preferences
	 *
	 * @param project	project for which the preference are to be set
	 * @param pref		the preference
	 * @param value		the value
	 * @throws BackingStoreException
	 */
	protected void setPreference(IProject project, String node, String pref, String value) throws BackingStoreException {
		ProjectScope scope = new ProjectScope(project);
		IEclipsePreferences projectPrefs = scope.getNode(node);
		projectPrefs.put(pref, value);
		projectPrefs.flush();
		projectPrefs.sync();
	}

	/**
	 * Find the project in workspace with the given id
	 * @param id	project id
	 * @return		project
	 */
	protected IProject findProject(String id) {
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource != null && resource.exists()) {
				return resource.getProject();
			}
		}
		return PDEPlugin.getWorkspace().getRoot().getProject(id);
	}

}
