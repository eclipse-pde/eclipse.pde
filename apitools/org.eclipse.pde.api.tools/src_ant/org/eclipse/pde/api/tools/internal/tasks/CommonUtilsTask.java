/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.search.SkippedComponent;
import org.eclipse.pde.api.tools.internal.util.TarException;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Common code for api tooling ant task.
 * 
 * @since 1.0.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class CommonUtilsTask extends Task {
	private static final String CVS_FOLDER_NAME = "CVS"; //$NON-NLS-1$
	private static final String ECLIPSE_FOLDER_NAME = "eclipse"; //$NON-NLS-1$
	protected static final String ISO_8859_1 = "ISO-8859-1"; //$NON-NLS-1$
	private static final String PLUGINS_FOLDER_NAME = "plugins"; //$NON-NLS-1$
	protected static final String REGULAR_EXPRESSION_START = "R:"; //$NON-NLS-1$

	protected static final String CURRENT = "currentBaseline"; //$NON-NLS-1$
	protected static final String CURRENT_PROFILE_NAME = "current_baseline"; //$NON-NLS-1$
	protected static final String REFERENCE = "referenceBaseline"; //$NON-NLS-1$
	protected static final String REFERENCE_PROFILE_NAME = "reference_baseline"; //$NON-NLS-1$

	protected boolean debug;
	protected String eeFileLocation;
	protected String currentBaselineLocation;
	protected String referenceBaselineLocation;
	protected String excludeListLocation;
	
	protected String reportLocation;
	
	/**
	 * Default comparator that orders {@link IApiComponent} by their ID 
	 */
	protected static final Comparator componentsorter = new Comparator(){
		public int compare(Object o1, Object o2) {
			if(o1 instanceof IApiComponent && o2 instanceof IApiComponent) {
				try {
					return ((IApiComponent)o1).getId().compareTo(((IApiComponent)o2).getId());
				}
				catch (CoreException ce) {}
			}
			if(o1 instanceof SkippedComponent && o2 instanceof SkippedComponent) {
				return ((SkippedComponent)o1).getComponentId().compareTo(((SkippedComponent)o2).getComponentId());
			}
			if(o1 instanceof String && o2 instanceof String) {
				return ((String)o1).compareTo((String)o2);
			}
			return -1;
		}
	};
	
	/**
	 * Default comparator that orders {@link File}s by their name
	 */
	protected static final Comparator filesorter = new Comparator(){
		public int compare(Object o1, Object o2) {
			if(o1 instanceof File && o2 instanceof File) {
				return ((File)o1).getName().compareTo(((File)o2).getName());
			}
			return 0;
		}
	};
	
	/**
	 * Creates a baseline with the given name and ee file location in the given directory
	 * @param baselineName
	 * @param dir
	 * @param eeFileLocation
	 * @return a new {@link IApiBaseline}
	 */
	protected IApiBaseline createBaseline(String baselineName, File dir, String eeFileLocation) {
		try {
			IApiBaseline baseline = null;
			if (ApiPlugin.isRunningInFramework()) {
				baseline = ApiModelFactory.newApiBaseline(baselineName);
			} else if (eeFileLocation != null) {
				baseline = ApiModelFactory.newApiBaseline(baselineName, new File(eeFileLocation));
			} else {
				baseline = ApiModelFactory.newApiBaseline(baselineName, Util.getEEDescriptionFile());
			}
			// create a component for each jar/directory in the folder
			File[] files = dir.listFiles();
			if(files == null) {
				throw new BuildException(
						Messages.bind(Messages.directoryIsEmpty,
						dir.getAbsolutePath()));
			}
			List components = new ArrayList();
			for (int i = 0; i < files.length; i++) {
				File bundle = files[i];
				if (!bundle.getName().equals(CVS_FOLDER_NAME)) {
					// ignore CVS folder
					IApiComponent component = ApiModelFactory.newApiComponent(baseline, bundle.getAbsolutePath());
					if(component != null) {
						components.add(component);
					}
				}
			}
			
			baseline.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
			return baseline;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Cleans the location if it exists
	 * @param file
	 */
	protected void scrubReportLocation(File file) {
		if(file.exists() && file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				if(files[i].isDirectory()) {
					scrubReportLocation(files[i]);
				}
				else {
					files[i].delete();
				}
			}
			file.delete();
		}
	}
	
	/**
	 * Deletes an {@link IApiBaseline} from the given folder
	 * @param referenceLocation
	 * @param folder
	 */
	protected void deleteBaseline(String referenceLocation, File folder) {
		if (isArchive(referenceLocation)) {
			Util.delete(folder.getParentFile());
		}
	}
	
	/**
	 * Extract extracts the SDK from the given location to the given directory name
	 * @param installDirName
	 * @param location
	 * @return the {@link File} handle to the extracted SDK
	 */
	protected File extractSDK(String installDirName, String location) {
		File file = new File(location);
		File locationFile = file;
		if (!locationFile.exists()) {
			throw new BuildException(Messages.bind(Messages.fileDoesnotExist, location));
		}
		if (isArchive(location)) {
			File tempDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
			File installDir = new File(tempDir, installDirName);
			if (installDir.exists()) {
				// delete existing folder
				if (!Util.delete(installDir)) {
					throw new BuildException(
						Messages.bind(
							Messages.couldNotDelete,
							installDir.getAbsolutePath()));
				}
			}
			if (!installDir.mkdirs()) {
				throw new BuildException(
						Messages.bind(
								Messages.couldNotCreate,
								installDir.getAbsolutePath()));
			}
			try {
				if (isZipJarFile(location)) {
					Util.unzip(location, installDir.getAbsolutePath());
				} else if (isTGZFile(location)) {
					Util.guntar(location, installDir.getAbsolutePath());
				}
			} catch (IOException e) {
				throw new BuildException(
					Messages.bind(
						Messages.couldNotUnzip,
						new String[] {
								location,
								installDir.getAbsolutePath()
						}));
			} catch (TarException e) {
				throw new BuildException(
						Messages.bind(
								Messages.couldNotUntar,
								new String[] {
										location,
										installDir.getAbsolutePath()
								}));
			}
			return new File(installDir, ECLIPSE_FOLDER_NAME);
		} else {
			return locationFile;
		}
	}
	
	/**
	 * Returns a file handle for the plug-ins directory within the given file
	 * @param dir
	 * @return the plug-ins directory file handle within the given file
	 */
	protected File getInstallDir(File dir) {
		return new File(dir, PLUGINS_FOLDER_NAME);
	}
	
	/**
	 * Returns if the given file name is the name of an archive file, 
	 * where an archive file is described as *.zip, *.jar, *.tar.gz or *.tgz
	 * 
	 * @param fileName
	 * @return true if the file name if that of an archive, false otherwise
	 */
	private boolean isArchive(String fileName) {
		return isZipJarFile(fileName) || isTGZFile(fileName);
	}
	
	/**
	 * Returns if the given file name represents a G-zip file name, where the name 
	 * has an extension of *.tar.gz or *.tgz
	 * 
	 * @param fileName
	 * @return true if the given file name is that of a G-zip archive, false otherwise
	 */
	private boolean isTGZFile(String fileName) {
		String normalizedFileName = fileName.toLowerCase();
		return normalizedFileName.endsWith(".tar.gz") //$NON-NLS-1$
			|| normalizedFileName.endsWith(".tgz"); //$NON-NLS-1$
	}
	
	/**
	 * Returns if the given file name represents a 'standard' archive, where the name
	 * has an extension of *.zip or *.jar
	 * 
	 * @param fileName
	 * @return true if the given file name is that of a 'standard' archive, false otherwise
	 */
	private boolean isZipJarFile(String fileName) {
		String normalizedFileName = fileName.toLowerCase();
		return normalizedFileName.endsWith(".zip") //$NON-NLS-1$
			|| normalizedFileName.endsWith(".jar"); //$NON-NLS-1$
	}
	
	/**
	 * Initializes the exclude list from the given file location, and returns
	 * a {@link Set} of project names that should be excluded.
	 * 
	 * @param excludeListLocation
	 * @return the set of project names to be excluded
	 */
	protected static Set initializeExcludedElement(String excludeListLocation) {
		return initializeRegexExcludeList(excludeListLocation, null);
	}
	
	/**
	 * Initializes the exclude set with regex support. The API baseline is used to determine which
	 * bundles should be added to the list when processing regex expressions.
	 * 
	 * @param location
	 * @param baseline
	 * @return the list of bundles to be excluded
	 */
	protected static Set initializeRegexExcludeList(String location, IApiBaseline baseline) {
		HashSet list = new HashSet();
		if (location != null) {
			File file = new File(location);
			if (file.exists()) {
				InputStream stream = null;
				char[] contents = null;
				try {
					stream = new BufferedInputStream(new FileInputStream(file));
					contents = Util.getInputStreamAsCharArray(stream, -1, CommonUtilsTask.ISO_8859_1);
				} 
				catch (FileNotFoundException e) {} 
				catch (IOException e) {} 
				finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException e) {}
					}
				}
				if (contents != null) {
					LineNumberReader reader = new LineNumberReader(new StringReader(new String(contents)));
					String line = null;
					try {
						while ((line = reader.readLine()) != null) {
							if (line.startsWith("#")) { //$NON-NLS-1$
								continue; 
							}
							if(baseline != null && line.startsWith(REGULAR_EXPRESSION_START)) {
								collectRegexIds(line, list, baseline.getApiComponents());
							}
							else {
								list.add(line);
							}
						}
					} 
					catch (IOException e) {} 
					finally {
						try {
							reader.close();
						} catch (IOException e) {}
					}
				}
			}
		}
		return list;
	}

	/**
	 * Collects the set of component ids that match a given regex in the exclude file
	 * @param line
	 * @param list
	 * @param components
	 */
	private static void collectRegexIds(String line, Set list, IApiComponent[] components) {
		if (line.startsWith(REGULAR_EXPRESSION_START)) {
			String componentname = line;
			// regular expression
			componentname = componentname.substring(2);
			Pattern pattern = null;
			try {
				pattern = Pattern.compile(componentname);
				String componentid = null;
				for (int j = 0, max2 = components.length; j < max2; j++) {
					componentid = components[j].getId();
					Matcher matcher = pattern.matcher(componentid);
					if (matcher.matches()) {
						list.add(componentid);
					}
				}
			} catch (PatternSyntaxException e) {
				throw new BuildException(Messages.bind(
						Messages.comparison_invalidRegularExpression,
						componentname));
			}
			catch(CoreException ce) {}
		}
	}
	
	/**
	 * Saves the report with the given name in the report location in a child directory with 
	 * the componentID name
	 * @param componentID
	 * @param contents
	 * @param reportname
	 */
	protected void saveReport(String componentID, String contents, String reportname) {
		File dir = new File(this.reportLocation);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new BuildException(Messages.bind(Messages.errorCreatingReportDirectory, this.reportLocation));
			}
		}
		File reportComponentIDDir = new File(dir, componentID);
		if (!reportComponentIDDir.exists()) {
			if (!reportComponentIDDir.mkdirs()) {
				throw new BuildException(Messages.bind(Messages.errorCreatingReportDirectory, reportComponentIDDir));
			}
		}
		File reportFile = new File(reportComponentIDDir, reportname);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(reportFile));
			writer.write(contents);
			writer.flush();
		} catch (IOException e) {
			ApiPlugin.log(e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
}
