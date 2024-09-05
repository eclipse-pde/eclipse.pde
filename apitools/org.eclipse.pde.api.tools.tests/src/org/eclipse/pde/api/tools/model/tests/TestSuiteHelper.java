/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.model.tests;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;
import org.eclipse.jdt.launching.environments.ExecutionEnvironmentDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.model.ApiType;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.search.IReferenceCollection;
import org.eclipse.pde.api.tools.internal.search.UseScanReferences;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;
import org.junit.Assert;
import org.osgi.framework.Bundle;

/**
 * Helper methods to set up baselines, etc.
 *
 * @since 1.0.0
 */
public class TestSuiteHelper {

	/**
	 * Computes the compile options to use. Currently this only changes if we
	 * are running the tests on Java 8.
	 *
	 * @return the array of compiler options to use
	 * @since 1.0.400
	 */
	public static String[] getCompilerOptions() {
		ArrayList<String> args = new ArrayList<>();
		if (ProjectUtils.isJava8Compatible()) {
			args.add("-1.8"); //$NON-NLS-1$
		} else {
			args.add("-1.5"); //$NON-NLS-1$
		}
		args.add("-preserveAllLocals"); //$NON-NLS-1$
		args.add("-nowarn"); //$NON-NLS-1$
		return args.toArray(new String[] {});
	}

	/**
	 * Creates a baseline from all bundles in the specified directory.
	 *
	 * @param rootDirectory directory to collect bundles from
	 * @return API baseline
	 */
	public static IApiBaseline createBaseline(String name, File rootDirectory) throws CoreException {
		ExecutionEnvironmentDescription ee = getEEDescription();
		IApiBaseline baseline = ApiModelFactory.newApiBaseline(name, ee, null);
		// create a component for each jar/directory in the folder
		File[] files = rootDirectory.listFiles();
		List<IApiComponent> components = new ArrayList<>();
		Set<String> requiredComponents = new HashSet<>();
		for (File bundle : files) {
			IApiComponent component = ApiModelFactory.newApiComponent(baseline, bundle.getAbsolutePath());
			if (component != null) {
				components.add(component);
				requiredComponents.add(component.getSymbolicName());
			}
		}
		// collect required components
		IApiComponent[] base = components.toArray(new IApiComponent[components.size()]);
		for (IApiComponent component : base) {
			addAllRequired(baseline, requiredComponents, component, components);
		}

		baseline.addApiComponents(components.toArray(new IApiComponent[components.size()]));
		return baseline;
	}

	/**
	 * Creates a testing API baseline
	 */
	public static IApiBaseline createTestingBaseline(String testDirectory) throws CoreException {
		return createTestingBaseline(null, IPath.fromOSString(testDirectory));
	}

	/**
	 * Creates a testing {@link IApiComponent} that has a testing baseline
	 * created for it
	 *
	 * @param baselinename the name for the owning testing {@link IApiBaseline}
	 *            or <code>null</code>
	 * @param name the name for the component
	 * @param id the id for the component
	 * @param description an {@link IApiDescription} for the component or
	 *            <code>null</code>
	 *
	 * @return a new {@link IApiComponent} for testing purposes only
	 */
	public static IApiComponent createTestingApiComponent(final String baselinename, final String name, final String id, final IApiDescription description) {
		return new IApiComponent() {
			private IReferenceCollection fReferences;

			@Override
			public String[] getPackageNames() throws CoreException {
				return null;
			}

			@Override
			public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
				return null;
			}

			@Override
			public void close() throws CoreException {
			}

			@Override
			public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
			}

			@Override
			public String getVersion() {
				return null;
			}

			@Override
			public IRequiredComponentDescription[] getRequiredComponents() {
				return null;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getLocation() {
				return null;
			}

			@Override
			public String getSymbolicName() {
				return id;
			}

			@Override
			public List<String> getExecutionEnvironments() {
				return List.of();
			}

			@Override
			public IApiTypeContainer[] getApiTypeContainers() {
				return null;
			}

			@Override
			public IApiDescription getApiDescription() {
				return description;
			}

			@Override
			public boolean hasApiDescription() {
				return false;
			}

			@Override
			public boolean isSystemComponent() {
				return false;
			}

			@Override
			public void dispose() {
			}

			@Override
			public IApiBaseline getBaseline() {
				return ApiModelFactory.newApiBaseline(baselinename);
			}

			@Override
			public IApiFilterStore getFilterStore() {
				return null;
			}

			@Override
			public boolean isSourceComponent() {
				return false;
			}

			@Override
			public boolean isFragment() {
				return false;
			}

			@Override
			public boolean hasFragments() {
				return false;
			}

			@Override
			public IApiTypeContainer[] getApiTypeContainers(String id) {
				return null;
			}

			@Override
			public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
				return null;
			}

			@Override
			public IApiElement getAncestor(int ancestorType) {
				return null;
			}

			@Override
			public IApiElement getParent() {
				return null;
			}

			@Override
			public int getType() {
				return IApiElement.COMPONENT;
			}

			@Override
			public List<String> getLowestEEs() {
				return List.of();
			}

			@Override
			public ResolverError[] getErrors() throws CoreException {
				return null;
			}

			@Override
			public IApiComponent getApiComponent() {
				return this;
			}

			@Override
			public IElementDescriptor getHandle() {
				return null;
			}

			@Override
			public IApiComponent getHost() throws CoreException {
				return null;
			}

			@Override
			public int getContainerType() {
				return 0;
			}

			@Override
			public IReferenceCollection getExternalDependencies() {
				if (fReferences == null) {
					fReferences = new UseScanReferences();
				}
				return fReferences;
			}

			@Override
			public boolean isDisposed() {
				return false;
			}
		};
	}

	/**
	 * Creates a testing {@link IApiComponent}
	 *
	 * @return a new {@link IApiComponent} for testing
	 */
	public static IApiComponent createTestingApiComponent(final String name, final String id, final IApiDescription description) {
		return createTestingApiComponent(null, name, id, description);
	}

	/**
	 * Creates a simple baseline from bundles in the specified directory of the
	 * test plug-in project.
	 *
	 * @param baselineid the name for the testing baseline
	 * @param testDirectory the dir the test baseline resides in
	 *
	 * @return Testing API baseline. If for some reason the testing directory is
	 *         not available <code>null</code> is returned
	 */
	public static IApiBaseline createTestingBaseline(String baselineid, IPath testDirectory) throws CoreException {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append(testDirectory);
		File file = path.toFile();
		String name = baselineid == null ? "test" : baselineid; //$NON-NLS-1$
		if (file.exists()) {
			ExecutionEnvironmentDescription ee = getEEDescription();
			IApiBaseline baseline = ApiModelFactory.newApiBaseline(name, ee, null);
			// create a component for each jar/directory in the folder
			File[] files = file.listFiles();
			List<IApiComponent> components = new ArrayList<>();
			Set<String> requiredComponents = new HashSet<>();
			for (File bundle : files) {
					IApiComponent component = ApiModelFactory.newApiComponent(baseline, bundle.getAbsolutePath());
					if (component != null) {
						components.add(component);
						requiredComponents.add(component.getSymbolicName());
					}
			}
			// collect required components
			IApiComponent[] base = components.toArray(new IApiComponent[components.size()]);
			for (IApiComponent component : base) {
				addAllRequired(baseline, requiredComponents, component, components);
			}

			baseline.addApiComponents(components.toArray(new IApiComponent[components.size()]));
			return baseline;
		}
		return null;
	}

	/**
	 * Creates a new {@link IApiType} for testing
	 *
	 * @return a new testing {@link IApiType}
	 */
	public static IApiType createTestingApiType(String baselineid, String componentid, String name, String sig, String genericsig, int flags, String enclosingname) {
		return new ApiType(TestSuiteHelper.createTestingApiComponent(baselineid, componentid, componentid, null), name, sig, genericsig, flags, enclosingname, null);
	}

	/**
	 * Gets the .ee file supplied to run tests based on system property.
	 */
	public static ExecutionEnvironmentDescription getEEDescription() throws CoreException {
		String eePath = System.getProperty("ee.file"); //$NON-NLS-1$
		if (eePath == null) {
			// generate a fake 17 ee file
			return new ExecutionEnvironmentDescription(Map.of( //
					ExecutionEnvironmentDescription.JAVA_HOME, System.getProperty("java.home"), //$NON-NLS-1$
					ExecutionEnvironmentDescription.BOOT_CLASS_PATH,
					org.eclipse.pde.api.tools.internal.util.Util.getJavaClassLibsAsString(),
					ExecutionEnvironmentDescription.LANGUAGE_LEVEL, "17", //$NON-NLS-1$
					ExecutionEnvironmentDescription.CLASS_LIB_LEVEL, "JavaSE-17")); //$NON-NLS-1$
		}
		File eeFile = new File(eePath);
		Assert.assertTrue("EE file does not exist: " + eePath, eeFile.exists()); //$NON-NLS-1$
		return new ExecutionEnvironmentDescription(eeFile);
	}

	/**
	 * Returns a file to the root of the specified bundle or <code>null</code>
	 * if none. Searches for plug-ins based on the "requiredBundles" system
	 * property. If "requiredBundles" is not set, failback to resolution of
	 * plugin running under current Platform (using OSGi), if active and
	 * available.
	 *
	 * @param bundleName
	 *            symbolic name
	 * @return bundle root or <code>null</code>
	 */
	public static File getBundle(String bundleName) {
		String root = System.getProperty("requiredBundles"); //$NON-NLS-1$
		if (root != null) {
			File bundlesRoot = new File(root);
			if (bundlesRoot.exists() && bundlesRoot.isDirectory()) {
				File[] bundles = bundlesRoot.listFiles();
				String key = bundleName + "_"; //$NON-NLS-1$
				for (File file : bundles) {
					if (file.getName().startsWith(key)) {
						return file;
					}
				}
			}
		} else {
			Bundle bundle = Platform.getBundle(bundleName);
			if (bundle != null) {
				return FileLocator.getBundleFileLocation(bundle).orElse(null);
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
	 */
	public static void addAllRequired(IApiBaseline baseline, Set<String> done, IApiComponent component, List<IApiComponent> collection) throws CoreException {
		IRequiredComponentDescription[] descriptions = component.getRequiredComponents();
		boolean error = false;
		StringBuilder buffer = null;
		for (IRequiredComponentDescription description : descriptions) {
			if (!done.contains(description.getId())) {
				File bundle = getBundle(description.getId());
				if (bundle == null) {
					if (!description.isOptional()) {
						if (buffer == null) {
							buffer = new StringBuilder();
						}
						buffer.append(description.getId()).append(',');
						error = true;
					}
				} else {
					IApiComponent apiComponent = ApiModelFactory.newApiComponent(baseline, bundle.getAbsolutePath());
					collection.add(apiComponent);
					done.add(apiComponent.getSymbolicName());
					addAllRequired(baseline, done, apiComponent, collection);
				}
			}
		}
		if (error) {
			throw new CoreException(Status.error("Check the property : -DrequiredBundles=...\nMissing required bundle(s): " + String.valueOf(buffer))); //$NON-NLS-1$
		}
	}

	/**
	 * Compiles a single source file
	 *
	 * @return true if compilation succeeded false otherwise
	 */
	public static boolean compile(String sourcename, String destinationpath, String[] compileroptions) {
		StringWriter out = new StringWriter();
		PrintWriter outWriter = new PrintWriter(out);
		StringWriter err = new StringWriter();
		PrintWriter errWriter = new PrintWriter(err);
		List<String> cmd = new ArrayList<>();
		cmd.add("-noExit"); //$NON-NLS-1$
		for (String compileroption : compileroptions) {
			cmd.add(compileroption);
		}
		if (destinationpath != null) {
			cmd.add("-d"); //$NON-NLS-1$
			cmd.add(destinationpath);
		}
		cmd.add(sourcename);

		String[] args = new String[cmd.size()];
		cmd.toArray(args);
		boolean result = false;
		try {
			result = BatchCompiler.compile(args, outWriter, errWriter, null);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		if (!result) {
			System.err.println(err.getBuffer());
		}
		return result;
	}

	/**
	 * Compiles all source files in the specified source paths to the specified
	 * destination path, with the given compiler options
	 *
	 * @return true if the compilation succeeded false otherwise
	 */
	public static boolean compile(String[] sourceFilePaths, String destinationPath, String[] compilerOptions) {
		StringWriter out = new StringWriter();
		PrintWriter outWriter = new PrintWriter(out);
		StringWriter err = new StringWriter();
		PrintWriter errWriter = new PrintWriter(err);
		List<String> cmd = new ArrayList<>();
		cmd.add("-noExit"); //$NON-NLS-1$
		for (String compilerOption : compilerOptions) {
			cmd.add(compilerOption);
		}
		if (destinationPath != null) {
			cmd.add("-d"); //$NON-NLS-1$
			cmd.add(destinationPath);
		}
		Set<String> directories = new HashSet<>();
		for (String sourceFilePath : sourceFilePaths) {
			File file = new File(sourceFilePath);
			if (!file.exists()) {
				continue;
			}
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
			StringBuilder classpathEntry = new StringBuilder();
			int length = directories.size();
			int counter = 0;
			for (String path : directories) {
				classpathEntry.append(path);
				if (counter < length - 1) {
					classpathEntry.append(File.pathSeparatorChar);
				}
			}
			cmd.add("-classpath"); //$NON-NLS-1$
			cmd.add(String.valueOf(classpathEntry));
		}
		String[] args = new String[cmd.size()];
		cmd.toArray(args);
		boolean result = false;
		try {
			result = BatchCompiler.compile(args, outWriter, errWriter, null);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		if (!result) {
			System.err.println(err.getBuffer());
		}
		return result;
	}

	/**
	 * Delete the file f and all sub-directories if f is a directory
	 *
	 * @param f the given file to delete
	 * @return true if the file was successfully deleted, false otherwise
	 */
	public static boolean delete(File f) {
		if (!Util.delete(f)) {
			System.err.println("Could not delete " + f.getAbsolutePath()); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	/**
	 * Copy file into the destination folder. If file is not a directory, it is
	 * copied into the destination folder. If file is a directory, all its files
	 * and subfolders are copied to the destination folder.
	 *
	 * <code>dest</code> has to be a directory.
	 *
	 * @param file the given file to copy
	 * @param dest the given destination folder
	 * @throws IllegalArgumentException if dest is not a directory or it doesn't
	 *             exist or if the given file doesn't exist
	 */
	public static void copy(File file, File dest) {
		if (!dest.exists()) {
			if (!dest.mkdirs()) {
				throw new IllegalArgumentException("could not create destination"); //$NON-NLS-1$
			}
		} else if (!dest.isDirectory()) {
			throw new IllegalArgumentException("destination is not a directory"); //$NON-NLS-1$
		}
		if (!file.exists()) {
			throw new IllegalArgumentException("The given file to copy doesn't exist"); //$NON-NLS-1$
		}
		copy0(file, dest);
	}

	private static void copy0(File f, File dest) {
		dest.mkdirs();
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					String name = file.getName();
					copy0(new File(f, name), new File(dest, name));
				} else {
					copy0(file, dest);
				}
			}
		} else {
			try {
				Files.copy(f.toPath(), new File(dest, f.getName()).toPath(),
						java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the OS path to the directory that contains this plugin.
	 */
	public static IPath getPluginDirectoryPath() {
		if (Platform.isRunning()) {
			try {
				URL platformURL = Platform.getBundle("org.eclipse.pde.api.tools.tests").getEntry("/"); //$NON-NLS-1$ //$NON-NLS-2$
				return IPath.fromOSString(new File(FileLocator.toFileURL(platformURL).getFile()).getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return IPath.fromOSString(System.getProperty("user.dir")); //$NON-NLS-1$
	}

	/**
	 * @return the path for the system property <code>user.dir</code>
	 */
	public static IPath getUserDirectoryPath() {
		return IPath.fromOSString(System.getProperty("user.dir")); //$NON-NLS-1$
	}

}