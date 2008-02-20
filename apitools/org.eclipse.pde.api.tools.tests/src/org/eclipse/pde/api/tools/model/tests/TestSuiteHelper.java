/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.model.tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.launching.EEVMType;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Helper methods to set up baselines, etc.
 * 
 * @since 1.0.0
 */
public class TestSuiteHelper {

	public static final String[] COMPILER_OPTIONS = new String[] {
		"-1.5",
		"-preserveAllLocals",
		"-nowarn"
	};
	
	/**
	 * Creates a profile from all bundles in the specified directory.
	 *
	 * @param rootDirectory directory to collect bundles from
	 * @return API profile
	 * @throws CoreException
	 */
	public static IApiProfile createProfile(String name, File rootDirectory) throws CoreException {
		File eeFile = getEEDescriptionFile();
		IApiProfile baseline = newApiProfile(name, eeFile);
		// create a component for each jar/directory in the folder
		File[] files = rootDirectory.listFiles();
		List<IApiComponent> components = new ArrayList<IApiComponent>();
		Set<String> requiredComponents = new HashSet<String>();
		for (int i = 0; i < files.length; i++) {
			File bundle = files[i];
			IApiComponent component = baseline.newApiComponent(bundle.getAbsolutePath());
			if(component != null) {
				components.add(component);
				requiredComponents.add(component.getId());
			}
		}
		// collect required components
		IApiComponent[] base = (IApiComponent[]) components.toArray(new IApiComponent[components.size()]);
		for (int i = 0; i < base.length; i++) {
			IApiComponent component = base[i];
			addAllRequired(baseline, requiredComponents, component, components);
		} 
		
		baseline.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
		return baseline;
	}	
	
	/**
	 * Creates a simple baseline from bundles in the specified directory of 
	 * the test plug-in project.
	 * 
	 * @return Testing API baseline. If for some reason the testing dir is not available
	 * <code>null</code> is returned
	 * @throws CoreException
	 */
	public static IApiProfile createTestingProfile(String testDirectory) throws CoreException {
		String dir = System.getProperty("user.dir");
		IPath path = new Path(dir);
		path = path.append(testDirectory);
		File file = path.toFile();
		if(file.exists()) {
			File eeFile = getEEDescriptionFile();
			IApiProfile baseline = newApiProfile("test", eeFile);
			// create a component for each jar/directory in the folder
			File[] files = file.listFiles();
			List components = new ArrayList();
			Set requiredComponents = new HashSet();
			for (int i = 0; i < files.length; i++) {
				File bundle = files[i];
				if (!bundle.getName().equals("CVS")) {
					// ignore CVS folder
					IApiComponent component = baseline.newApiComponent(bundle.getAbsolutePath());
					if(component != null) {
						components.add(component);
						requiredComponents.add(component.getId());
					}
				}
			}
			// collect required components
			IApiComponent[] base = (IApiComponent[]) components.toArray(new IApiComponent[components.size()]);
			for (int i = 0; i < base.length; i++) {
				IApiComponent component = base[i];
				addAllRequired(baseline, requiredComponents, component, components);
			} 
			
			baseline.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
			return baseline;
		}
		return null;
	}

	/**
	 * Constructs a new {@link IApiProfile} with the given name, id, version, and environment.
	 * <p>
	 * Attempts to locate OSGi execution environment profile when not running in 
	 * an OSGi framework.
	 * </p>
	 * @param name
	 * @param ee execution environment description file
	 * @return API baseline
	 * @exception CoreException if unable to create a baseline
	 */
	public static IApiProfile newApiProfile(String name, File eeFile) throws CoreException {
		if (ApiPlugin.isRunningInFramework()) {
			return Factory.newApiProfile(name, eeFile);
		} else {
			String ee = EEVMType.getProperty(EEVMType.PROP_CLASS_LIB_LEVEL, eeFile);
			Properties properties = Util.getEEProfile(ee);
			if (properties == null) {
				throw new CoreException(new Status(IStatus.ERROR,
						"org.eclipse.pde.api.tools.tests",
						"Unable to locate OSGi profile: " + ee));
			} else {
				return Factory.newApiProfile(name,properties, eeFile);
			}
		}
	}

	/**
	 * Gets the .ee file supplied to run tests based on system
	 * property.
	 * 
	 * @return
	 */
	public static File getEEDescriptionFile() {
		String eePath = System.getProperty("ee.file");
		if (eePath == null) {
			// generate a fake 1.5 ee file
			File fakeEEFile = null;
			PrintWriter writer = null;
			try {
				fakeEEFile = File.createTempFile("eefile", ".ee");
				writer = new PrintWriter(new BufferedWriter(new FileWriter(fakeEEFile)));
				writer.print("-Djava.home=");
				writer.println(System.getProperty("java.home"));
				writer.print("-Dee.bootclasspath=");
				writer.println(getJavaClassLibsAsString());
				writer.println("-Dee.language.level=1.5");
				writer.println("-Dee.class.library.level=J2SE-1.5");
				writer.flush();
			} catch (IOException e) {
				// ignore
			} finally {
				if (writer != null) {
					writer.close();
				}
			}
			fakeEEFile.deleteOnExit();
			eePath = fakeEEFile.getAbsolutePath();
		}
		Assert.assertNotNull("-Dee.file not specified", eePath);
		File eeFile = new File(eePath);
		Assert.assertTrue("EE file does not exist: " + eePath, eeFile.exists());
		return eeFile;
	}

	private static String getJavaClassLibsAsString() {
		String[] libs = getJavaClassLibs();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0, max = libs.length; i < max; i++) {
			if (i > 0) {
				buffer.append(File.pathSeparatorChar);
			}
			buffer.append(libs[i]);
		}
		return String.valueOf(buffer);
	}

	/**
	 * Returns a file to the root of the specified bundle or <code>null</code>
	 * if none. Searches for plug-ins based on the "requiredBundles" system
	 * property.
	 * 
	 * @param bundleName symbolic name
	 * @return bundle root or <code>null</code>
	 */
	public static File getBundle(String bundleName) {
		String root = System.getProperty("requiredBundles");
		if (root != null) {
			File bundlesRoot = new File(root);
			if (bundlesRoot.exists() && bundlesRoot.isDirectory()) {
				File[] bundles = bundlesRoot.listFiles();
				String key = bundleName + "_";
				for (int i = 0; i < bundles.length; i++) {
					File file = bundles[i];
					if (file.getName().startsWith(key)) {
						return file;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Adds all required components to the collection of components.
	 * 
	 * @param done set of component id's that have already been collected
	 * @param component component to add all prerequisites for
	 * @param collection collection to add prerequisites to.
	 * @throws CoreException 
	 */
	public static void addAllRequired(IApiProfile baseline, Set done, IApiComponent component, List collection) throws CoreException {
		IRequiredComponentDescription[] descriptions = component.getRequiredComponents();
		boolean error = false;
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < descriptions.length; i++) {
			IRequiredComponentDescription description = descriptions[i];
			if (!done.contains(description.getId())) {
				File bundle = getBundle(description.getId());
				if (bundle == null) {
					if (!description.isOptional()) {
						buffer.append(description.getId()).append(',');
						error = true;
					}
				} else {
					IApiComponent apiComponent = baseline.newApiComponent(bundle.getAbsolutePath());
					collection.add(apiComponent);
					done.add(apiComponent.getId());
					addAllRequired(baseline, done, apiComponent, collection);
				}
			}
		}
		if (error) {
			throw new CoreException(new Status(IStatus.ERROR,
					"Missing required bundle: " + String.valueOf(buffer), null));
		}
	}

	/**
	 * Compiles a single source file 
	 * @param sourcename
	 * @param destinationpath
	 * @param compileroptions
	 * @return true if compilation succeeded false otherwise
	 */
	public static boolean compile(String sourcename, String destinationpath, String[] compileroptions) {
		StringWriter out = new StringWriter();
		PrintWriter outWriter = new PrintWriter(out);
		StringWriter err = new StringWriter();
		PrintWriter errWriter = new PrintWriter(err);
		List cmd = new ArrayList();
		cmd.add("-noExit");
		for (int i = 0, max = compileroptions.length; i < max; i++) {
			cmd.add(compileroptions[i]);
		}
		if (destinationpath != null) {
			cmd.add("-d");
			cmd.add(destinationpath);
		}
		cmd.add(sourcename);
		
		String[] args = new String[cmd.size()];
		cmd.toArray(args);
		boolean result = false;
		try {
			result = new Main(outWriter, errWriter, true).compile(args);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		if (!result) {
			System.err.println(err.getBuffer());
		}
		return result;
	}
	
	/**
	 * Compiles all source files in the specified source paths to the specified destination path, with the given 
	 * compiler options
	 * @param sourceFilePaths
	 * @param destinationPath
	 * @param compilerOptions
	 * @return true if the compilation succeeded false otherwise
	 */
	public static boolean compile(String[] sourceFilePaths, String destinationPath, String[] compilerOptions) {
		StringWriter out = new StringWriter();
		PrintWriter outWriter = new PrintWriter(out);
		StringWriter err = new StringWriter();
		PrintWriter errWriter = new PrintWriter(err);
		List cmd = new ArrayList();
		cmd.add("-noExit");
		for (int i = 0, max = compilerOptions.length; i < max; i++) {
			cmd.add(compilerOptions[i]);
		}
		if (destinationPath != null) {
			cmd.add("-d");
			cmd.add(destinationPath);
		}
		Set directories = new HashSet();
		for (int i = 0, max = sourceFilePaths.length; i < max; i++) {
			String sourceFilePath = sourceFilePaths[i];
			File file = new File(sourceFilePath);
			if (!file.exists()) continue;
			if (file.isDirectory()) {
				directories.add(file.getAbsolutePath());
			} else {
				File parent = file.getParentFile();
				directories.add(parent.getAbsolutePath());
			}
			cmd.add(sourceFilePath);
		}
		// add all directories as classpath entries
		if (!directories.isEmpty()) {
			StringBuffer classpathEntry = new StringBuffer();
			int length = directories.size();
			int counter = 0;
			for (Iterator iterator = directories.iterator(); iterator.hasNext();) {
				String path = (String) iterator.next();
				classpathEntry.append(path);
				if (counter < length - 1) {
					classpathEntry.append(File.pathSeparatorChar);
				}
			}
			cmd.add("-classpath");
			cmd.add(String.valueOf(classpathEntry));
		}
		String[] args = new String[cmd.size()];
		cmd.toArray(args);
		boolean result = false;
		try {
			result = new Main(outWriter, errWriter, true).compile(args);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		if (!result) {
			System.err.println(err.getBuffer());
		}
		return result;
	}
	
	/**
	 * Delete the file f and all subdirectories if f is a directory
	 * @param f the given file to delete
	 * @return true if the file was successfully deleted, false otherwise
	 */
	public static boolean delete(File f) {
		if (!delete0(f)) {
			System.err.println("Could not delete " + f.getAbsolutePath());
			return false;
		}
		return true;
	}
	
	private static boolean delete0(File f) {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (int i = 0, max = files.length; i < max; i++) {
				File file = files[i];
				if (!delete0(file)) {
					System.err.println("Could not delete " + file.getAbsolutePath());
					return false;
				}
			}
			return f.delete();
		} else {
			return f.delete();
		}
	}

	/**
	 * Copy file into the destination folder.
	 * If file is not a directory, it is copied into the destination folder.
	 * If file is a directory, all its files and subfolders are copied to the destination folder.
	 * 
	 * <code>dest</code> has to be a directory.
	 * 
	 * @param file the given file to copy
	 * @param dest the given destination folder
	 * @throws IllegalArgumentException if dest is not a directory
	 *        or it doesn't exist or if the given file doesn't exist
	 */
	public static void copy(File file, File dest) {
		if (!dest.exists() || !dest.isDirectory()) {
			throw new IllegalArgumentException("destination is not a directory");
		}
		if (!file.exists()) {
			throw new IllegalArgumentException("The given file to copy doesn't exist");
		}
		copy0(file, dest);
	}
	
	private static void copy0(File f, File dest) {
		dest.mkdirs();
		if (f.isDirectory()) {
			String dirName = f.getName();
			if ("CVS".equals(dirName)) return;
			File[] files = f.listFiles();
			for (int i = 0, max = files.length; i < max; i++) {
				File file = files[i];
				if (file.isDirectory()) {
					String name = file.getName();
					if ("CVS".equals(name)) continue;
					copy0(new File(f, name), new File(dest, name));
				} else {
					copy0(file, dest);
				}
			}
		} else {
			byte[] bytes = null;
			BufferedInputStream inputStream = null;
			try {
				inputStream = new BufferedInputStream(new FileInputStream(f));
				bytes = Util.getInputStreamAsByteArray(inputStream, -1);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						ApiPlugin.log(e);
					}
				}
			}
			if (bytes != null) {
				BufferedOutputStream outputStream = null;
				try {
					outputStream = new BufferedOutputStream(new FileOutputStream(new File(dest, f.getName())));
					outputStream.write(bytes);
					outputStream.flush();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (outputStream != null) {
						try {
							outputStream.close();
						} catch(IOException e) {
							ApiPlugin.log(e);
						}
					}
				}
			}
		}
	}
	/**
	 * Search the user hard-drive for a Java class library.
	 * Returns null if none could be found.
	 *
	 * Example of use: [org.eclipse.jdt.core.tests.util.Util.getJavaClassLib()]
	*/
	public static String[] getJavaClassLibs() {
		// check bootclasspath properties for Sun, JRockit and Harmony VMs
		String bootclasspathProperty = System.getProperty("sun.boot.class.path"); //$NON-NLS-1$
		if ((bootclasspathProperty == null) || (bootclasspathProperty.length() == 0)) {
			// IBM J9 VMs
			bootclasspathProperty = System.getProperty("vm.boot.class.path"); //$NON-NLS-1$
			if ((bootclasspathProperty == null) || (bootclasspathProperty.length() == 0)) {
				// Harmony using IBM VME
				bootclasspathProperty = System.getProperty("org.apache.harmony.boot.class.path"); //$NON-NLS-1$
			}
		}
		String[] jars = null;
		if ((bootclasspathProperty != null) && (bootclasspathProperty.length() != 0)) {
			StringTokenizer tokenizer = new StringTokenizer(bootclasspathProperty, File.pathSeparator);
			final int size = tokenizer.countTokens();
			jars = new String[size];
			int i = 0;
			while (tokenizer.hasMoreTokens()) {
				final String fileName = toNativePath(tokenizer.nextToken());
				if (new File(fileName).exists()) {
					jars[i] = fileName;
					i++;
				}
			}
			if (size != i) {
				// resize
				System.arraycopy(jars, 0, (jars = new String[i]), 0, i);
			}
		} else {
			String jreDir = getJREDirectory();
			final String osName = System.getProperty("os.name");
			if (jreDir == null) {
				return new String[] {};
			}
			if (osName.startsWith("Mac")) {
				return new String[] {
						toNativePath(jreDir + "/../Classes/classes.jar")
				};
			}
			final String vmName = System.getProperty("java.vm.name");
			if ("J9".equals(vmName)) {
				return new String[] {
						toNativePath(jreDir + "/lib/jclMax/classes.zip")
				};
			}
			String[] jarsNames = null;
			ArrayList paths = new ArrayList();
			if ("DRLVM".equals(vmName)) {
				FilenameFilter jarFilter = new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(".jar") & !name.endsWith("-src.jar");
					}
				};
				jarsNames = new File(jreDir + "/lib/boot/").list(jarFilter);
				addJarEntries(jreDir + "/lib/boot/", jarsNames, paths);
			} else {
				jarsNames = new String[] {
						"/lib/vm.jar",
						"/lib/rt.jar",
						"/lib/core.jar",
						"/lib/security.jar",
						"/lib/xml.jar",
						"/lib/graphics.jar"
				};
				addJarEntries(jreDir, jarsNames, paths);
			}
			jars = new String[paths.size()];
			paths.toArray(jars);
		}
		return jars;
	}
	private static void addJarEntries(String jreDir, String[] jarNames, ArrayList paths) {
		for (int i = 0, max = jarNames.length; i < max; i++) {
			final String currentName = jreDir + jarNames[i];
			File f = new File(currentName);
			if (f.exists()) {
				paths.add(toNativePath(currentName));
			}
		}
	}
	/**
	 * Makes the given path a path using native path separators as returned by File.getPath()
	 * and trimming any extra slash.
	 */
	private static String toNativePath(String path) {
	    String nativePath = path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
	    return
	        nativePath.endsWith("/") || nativePath.endsWith("\\") ?
	            nativePath.substring(0, nativePath.length() - 1) :
	            nativePath;
	}
	/**
	 * Returns the JRE directory this tests are running on.
	 * Returns null if none could be found.
	 *
	 * Example of use: [org.eclipse.jdt.core.tests.util.Util.getJREDirectory()]
	 */
	public static String getJREDirectory() {
	    return System.getProperty("java.home");
	}
}
