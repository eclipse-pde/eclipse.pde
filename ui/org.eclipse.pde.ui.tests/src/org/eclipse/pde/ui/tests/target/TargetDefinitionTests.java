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
package org.eclipse.pde.ui.tests.target;

import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;

import org.eclipse.pde.internal.core.target.provisional.LoadTargetDefinitionJob;

import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import junit.framework.TestCase;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.osgi.framework.ServiceReference;

/**
 * Tests for target definitions.
 * 
 * @since 3.5 
 */
public class TargetDefinitionTests extends TestCase {
	
	/**
	 * Retrieves all bundles (source and code) in the given target definition
	 * returning them as a set of URLs.
	 * 
	 * @param target target definition
	 * @return all bundle URLs
	 */
	protected Set getAllBundleURLs(ITargetDefinition target) throws Exception {
		BundleInfo[] code = target.resolveBundles(null);
		BundleInfo[] source = target.resolveSourceBundles(null);
		Set urls = new HashSet(code.length + source.length);
		for (int i = 0; i < code.length; i++) {
			urls.add(new File(code[i].getLocation()).toURL());
		}
		for (int i = 0; i < source.length; i++) {
			urls.add(new File(source[i].getLocation()).toURL());
		}
		return urls;
	}
	
	/**
	 * Extracts the classic plug-ins archive, if not already done, and returns a path to the
	 * root directory containing the plug-ins.
	 * 
	 * @return path to the plug-ins directory
	 * @throws Exception
	 */
	protected IPath extractClassicPlugins() throws Exception {
		// extract the 3.0.2 skeleton
		IPath stateLocation = MacroPlugin.getDefault().getStateLocation();
		IPath location = stateLocation.append("classic-plugins");
		if (location.toFile().exists()) {
			return location;
		}
		URL zipURL = MacroPlugin.getBundleContext().getBundle().getEntry("/tests/targets/classic-plugins.zip");
		Path zipPath = new Path(new File(FileLocator.toFileURL(zipURL).getFile()).getAbsolutePath());
		ZipFile zipFile = new ZipFile(zipPath.toFile());
		Enumeration entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			if (!entry.isDirectory()) {
				IPath entryPath = stateLocation.append(entry.getName());
				File dir = entryPath.removeLastSegments(1).toFile();
				dir.mkdirs();
				File file = entryPath.toFile();
				file.createNewFile();
				InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry));
				byte[] bytes = getInputStreamAsByteArray(inputStream, -1);
				inputStream.close();
				BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
				outputStream.write(bytes);
				outputStream.close();
			}
		}
		zipFile.close();
		return location;
	}
	
	/**
	 * Returns the target platform service or <code>null</code> if none
	 * 
	 * @return target platform service
	 */
	protected ITargetPlatformService getTargetService() {
		ServiceReference reference = MacroPlugin.getBundleContext().getServiceReference(ITargetPlatformService.class.getName());
		assertNotNull("Missing target platform service", reference);
		if (reference == null)
			return null;
		return (ITargetPlatformService) MacroPlugin.getBundleContext().getService(reference);
	}
	
	/**
	 * Returns a default target platform that takes target weaving into account
	 * if in a second instance of Eclipse. This allows the target platfrom to be 
	 * reset after changing it in a test.
	 * 
	 * @return default settings for target platform
	 */
	protected ITargetDefinition getDefaultTargetPlatorm() {
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(),
				new File(Platform.getConfigurationLocation().getURL().getFile()).getAbsolutePath());
		definition.setBundleContainers(new IBundleContainer[]{container});
		return definition;
	}
	
	/**
	 * Used to reset the target platform to original settings after a test that changes
	 * the target platform.
	 */
	protected void resetTargetPlatform() {
		ITargetDefinition definition = getDefaultTargetPlatorm();
		setTargetPlatform(definition);
	}
	
	/**
	 * Sets the target platform based on the given definition.
	 * 
	 * @param target target definition
	 */
	protected void setTargetPlatform(ITargetDefinition target) {
		LoadTargetDefinitionJob job = new LoadTargetDefinitionJob(target);
		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			assertFalse("Target platform reset interrupted", true);
		}
	}	
	
	/**
	 * Tests that resetting the target platform should work OK (i.e. is equivalent to the
	 * models in the default target platform).
	 * 
	 * @throws CoreException
	 */
	public void testResetTargetPlatform() throws Exception {
		ITargetDefinition definition = getDefaultTargetPlatorm();
		Set urls = getAllBundleURLs(definition);
		
		// current platform
		IPluginModelBase[] models = TargetPlatformHelper.getPDEState().getTargetModels();
		
		// should be equivalent
		assertEquals("Should have same number of bundles", urls.size(), models.length);
		for (int i = 0; i < models.length; i++) {
			String location = models[i].getInstallLocation();
			assertTrue("Missing plug-in " + location, urls.contains(new File(location).toURL()));
		}
	}	

	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform (this is an 
	 * explicit location with no target weaving).
	 * 
	 * @throws Exception
	 */
	public void testDefaultTargetPlatform() throws Exception {
		// the new way
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation());
		definition.setBundleContainers(new IBundleContainer[]{container});
		Set urls = getAllBundleURLs(definition);
		
		// the old way
		IPath location = new Path(TargetPlatform.getDefaultLocation());
		URL[] pluginPaths = P2Utils.readBundlesTxt(location.toOSString(), location.append("configuration").toFile().toURL());
		assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
		for (int i = 0; i < pluginPaths.length; i++) {
			URL url = pluginPaths[i];
			assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
		}
		
	}
	
	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform (this is an 
	 * explicit location with no target weaving), when created with a variable
	 * referencing ${eclipse_home}
	 * 
	 * @throws Exception
	 */
	public void testEclipseHomeTargetPlatform() throws Exception {
		// the new way
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newProfileContainer("${eclipse_home}");
		definition.setBundleContainers(new IBundleContainer[]{container});
		Set urls = getAllBundleURLs(definition);
		
		// the old way
		IPath location = new Path(TargetPlatform.getDefaultLocation());
		URL[] pluginPaths = P2Utils.readBundlesTxt(location.toOSString(), location.append("configuration").toFile().toURL());
		assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
		for (int i = 0; i < pluginPaths.length; i++) {
			URL url = pluginPaths[i];
			assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
		}
		
	}	
	
	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform (this is an 
	 * explicit location with no target weaving), when created with a variable
	 * referencing ${eclipse_home}.
	 * 
	 * @throws Exception
	 */
	public void testEclipseHomeTargetPlatformAndConfigurationArea() throws Exception {
		// the new way
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newProfileContainer("${eclipse_home}", "${eclipse_home}/configuration");
		definition.setBundleContainers(new IBundleContainer[]{container});
		Set urls = getAllBundleURLs(definition);
		
		// the old way
		IPath location = new Path(TargetPlatform.getDefaultLocation());
		URL[] pluginPaths = P2Utils.readBundlesTxt(location.toOSString(), location.append("configuration").toFile().toURL());
		assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
		for (int i = 0; i < pluginPaths.length; i++) {
			URL url = pluginPaths[i];
			assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
		}
		
	}		
	
	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform using the
	 * platform's configuration location (which will do target weaving). This
	 * is really only tested when run as a JUnit plug-in test suite from
	 * within Eclipse.
	 * 
	 * @throws Exception
	 */
	public void testWovenTargetPlatform() throws Exception {
		// the new way
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(),
				new File(Platform.getConfigurationLocation().getURL().getFile()).getAbsolutePath());
		definition.setBundleContainers(new IBundleContainer[]{container});
		Set urls = getAllBundleURLs(definition);
		
		// the old way
		URL[] pluginPaths = PluginPathFinder.getPluginPaths(TargetPlatform.getDefaultLocation());
		assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
		for (int i = 0; i < pluginPaths.length; i++) {
			URL url = pluginPaths[i];
			assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
		}
		
	}	

	/**
	 * Tests that a bundle directory container is equivalent to scanning locations.
	 * 
	 * @throws Exception
	 */
	public void testDirectoryBundleContainer() throws Exception {
		// the new way
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		definition.setBundleContainers(new IBundleContainer[]{container});
		Set urls = getAllBundleURLs(definition);
		
		Preferences store = PDECore.getDefault().getPluginPreferences();
		boolean restore = store.getBoolean(ICoreConstants.TARGET_PLATFORM_REALIZATION);
		try {
			store.setValue(ICoreConstants.TARGET_PLATFORM_REALIZATION, false);
			// the old way
			URL[] pluginPaths = PluginPathFinder.getPluginPaths(TargetPlatform.getDefaultLocation());
			assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
			for (int i = 0; i < pluginPaths.length; i++) {
				URL url = pluginPaths[i];
				assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
			}
		}
		finally {
			store.setValue(ICoreConstants.TARGET_PLATFORM_REALIZATION, restore);
		}
	}
	
	/**
	 * Tests that a bundle directory container is equivalent to scanning locations
	 * when it uses a variable to specify its location.
	 * 
	 * @throws Exception
	 */
	public void testVariableDirectoryBundleContainer() throws Exception {
		// the new way
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newDirectoryContainer("${eclipse_home}/plugins");
		definition.setBundleContainers(new IBundleContainer[]{container});
		Set urls = getAllBundleURLs(definition);
		
		Preferences store = PDECore.getDefault().getPluginPreferences();
		boolean restore = store.getBoolean(ICoreConstants.TARGET_PLATFORM_REALIZATION);
		try {
			store.setValue(ICoreConstants.TARGET_PLATFORM_REALIZATION, false);
			// the old way
			URL[] pluginPaths = PluginPathFinder.getPluginPaths(TargetPlatform.getDefaultLocation());
			assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
			for (int i = 0; i < pluginPaths.length; i++) {
				URL url = pluginPaths[i];
				assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
			}
		}
		finally {
			store.setValue(ICoreConstants.TARGET_PLATFORM_REALIZATION, restore);
		}
	}	
	
	/**
	 * Tests reading a 3.0.2 install with a mix of classic and OSGi plug-ins.
	 * 
	 * @throws Exception
	 */
	public void testClassicPlugins() throws Exception {
		// extract the 3.0.2 skeleton
		IPath location = extractClassicPlugins();
		
		// the new way
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newDirectoryContainer(location.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		Set urls = getAllBundleURLs(definition);
		
		Preferences store = PDECore.getDefault().getPluginPreferences();
		boolean restore = store.getBoolean(ICoreConstants.TARGET_PLATFORM_REALIZATION);
		try {
			store.setValue(ICoreConstants.TARGET_PLATFORM_REALIZATION, false);
			// the old way
			URL[] pluginPaths = PluginPathFinder.getPluginPaths(location.toOSString());
			for (int i = 0; i < pluginPaths.length; i++) {
				URL url = pluginPaths[i];
				if (!urls.contains(url)) {
					System.err.println(url.toString());
				}
			}
			assertEquals("Wrong number of bundles", pluginPaths.length, urls.size());
		}
		finally {
			store.setValue(ICoreConstants.TARGET_PLATFORM_REALIZATION, restore);
		}		
	}
	
	/**
	 * Tests identification of source bundles in a 3.0.2 install.
	 * 
	 * @throws Exception
	 */
	public void testClassicSourcePlugins() throws Exception {
		// extract the 3.0.2 skeleton
		IPath location = extractClassicPlugins();
		
		// the new way
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newDirectoryContainer(location.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		BundleInfo[] bundles = definition.resolveSourceBundles(null);
		assertEquals("Wrong number of source bundles", 3, bundles.length);
		Set names = new HashSet();
		for (int i = 0; i < bundles.length; i++) {
			names.add(bundles[i].getSymbolicName());
		}
		String[] expected = new String[]{"org.eclipse.platform.source", "org.eclipse.jdt.source", "org.eclipse.pde.source"};
		for (int i = 0; i < expected.length; i++) {
			assertTrue("Missing source for " + expected[i], names.contains(expected[i]));	
		}
	}

	/**
	 * Returns the given input stream as a byte array
	 * @param stream the stream to get as a byte array
	 * @param length the length to read from the stream or -1 for unknown
	 * @return the given input stream as a byte array
	 * @throws IOException
	 */
	public static byte[] getInputStreamAsByteArray(InputStream stream, int length) throws IOException {
		byte[] contents;
		if (length == -1) {
			contents = new byte[0];
			int contentsLength = 0;
			int amountRead = -1;
			do {
				// read at least 8K
				int amountRequested = Math.max(stream.available(), 8192);
				// resize contents if needed
				if (contentsLength + amountRequested > contents.length) {
					System.arraycopy(contents,
							0,
							contents = new byte[contentsLength + amountRequested],
							0,
							contentsLength);
				}
				// read as many bytes as possible
				amountRead = stream.read(contents, contentsLength, amountRequested);
				if (amountRead > 0) {
					// remember length of contents
					contentsLength += amountRead;
				}
			} while (amountRead != -1);
			// resize contents if necessary
			if (contentsLength < contents.length) {
				System.arraycopy(contents, 0, contents = new byte[contentsLength], 0, contentsLength);
			}
		} else {
			contents = new byte[length];
			int len = 0;
			int readSize = 0;
			while ((readSize != -1) && (len != length)) {
				// See PR 1FMS89U
				// We record first the read size. In this case length is the actual
				// read size.
				len += readSize;
				readSize = stream.read(contents, len, length - len);
			}
		}
		return contents;
	}	
	
	/**
	 * Tests restoration of a handle to target definition in an IFile 
	 * @throws CoreException 
	 */
	public void testWorkspaceTargetHandleMemento() throws CoreException {
		ITargetPlatformService service = getTargetService();
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("does/not/exist"));
		ITargetHandle handle = service.getTarget(file);
		assertFalse("Target should not exist", handle.exists());
		String memento = handle.getMemento();
		assertNotNull("Missing memento", memento);
		ITargetHandle handle2 = service.getTarget(memento);
		assertEquals("Restore failed", handle, handle2);
		IFile file2 = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("does/not/exist/either"));
		ITargetHandle handle3 = service.getTarget(file2);
		assertFalse("Should be different targets", handle.equals(handle3));
	}
	
	/**
	 * Tests restoration of a handle to target definition in local metadata 
	 * 
	 * @throws CoreException 
	 * @throws InterruptedException 
	 */
	public void testLocalTargetHandleMemento() throws CoreException, InterruptedException {
		ITargetPlatformService service = getTargetService();
		ITargetHandle handle = service.newTarget().getHandle();
		assertFalse("Target should not exist", handle.exists());
		String memento = handle.getMemento();
		assertNotNull("Missing memento", memento);
		ITargetHandle handle2 = service.getTarget(memento);
		assertEquals("Restore failed", handle, handle2);
		ITargetHandle handle3 = service.newTarget().getHandle();
		assertFalse("Should be different targets", handle.equals(handle3));
	}
	
	/**
	 * Returns the location of the JDT feature in the running host as
	 * a path in the local file system.
	 * 
	 * @return path to JDT feature
	 */
	protected IPath getJdtFeatureLocation() {
		IPath path = new Path(TargetPlatform.getDefaultLocation());
		path = path.append("features");
		File dir = path.toFile();
		assertTrue("Missing features directory", dir.exists() && !dir.isFile());
		String[] files = dir.list();
		String location = null;
		for (int i = 0; i < files.length; i++) {
			if (files[i].startsWith("org.eclipse.jdt_")) {
				location = path.append(files[i]).toOSString();
				break;
			}
		}
		assertNotNull("Missing JDT feature", location);
		return new Path(location);
	}
	
	/**
	 * Tests a JDT feature bundle container contains the appropriate bundles
	 * @throws Exception 
	 */
	public void testFeatureBundleContainer() throws Exception {
		IBundleContainer container = getTargetService().newFeatureContainer("${eclipse_home}", "org.eclipse.jdt", null);
		BundleInfo[] bundles = container.resolveBundles(null);
		
		Set expected = new HashSet();
		expected.add("org.eclipse.jdt");
		expected.add("org.eclipse.ant.ui");
		expected.add("org.eclipse.jdt.apt.core");
		expected.add("org.eclipse.jdt.apt.ui");
		expected.add("org.eclipse.jdt.apt.pluggable.core");
		expected.add("org.eclipse.jdt.compiler.apt");
		expected.add("org.eclipse.jdt.compiler.tool");
		expected.add("org.eclipse.jdt.core");
		expected.add("org.eclipse.jdt.core.manipulation");
		expected.add("org.eclipse.jdt.debug.ui");
		expected.add("org.eclipse.jdt.debug");
		expected.add("org.eclipse.jdt.junit");
		expected.add("org.eclipse.jdt.junit.runtime");
		expected.add("org.eclipse.jdt.junit4.runtime");
		expected.add("org.eclipse.jdt.launching");
		expected.add("org.eclipse.jdt.ui");
		expected.add("org.junit");
		expected.add("org.junit4");
		expected.add("org.eclipse.jdt.doc.user");
		if (Platform.getOS() == Platform.OS_MACOSX) {
			expected.add("org.eclipse.jdt.launching.macosx");
		}
		assertEquals("Wrong number of bundles in JDT feature", expected.size(), bundles.length);
		for (int i = 0; i < bundles.length; i++) {
			expected.remove(bundles[i].getSymbolicName());
		}
		Iterator iterator = expected.iterator();
		while (iterator.hasNext()) {
			String name = (String) iterator.next();
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());
		
		
		// should be no source bundles
		bundles = container.resolveSourceBundles(null);
		assertEquals("Wrong source bundle count", 0, bundles.length);
	}
	
	/**
	 * Tests a JDT source feature bundle container contains the appropriate bundles
	 * @throws Exception 
	 */
	public void testSourceFeatureBundleContainer() throws Exception {
		IBundleContainer container = getTargetService().newFeatureContainer("${eclipse_home}", "org.eclipse.jdt.source", null);
		BundleInfo[] bundles = container.resolveSourceBundles(null);
		
		Set expected = new HashSet();
		expected.add("org.eclipse.jdt.source");
		expected.add("org.eclipse.ant.ui.source");
		expected.add("org.eclipse.jdt.apt.core.source");
		expected.add("org.eclipse.jdt.apt.ui.source");
		expected.add("org.eclipse.jdt.apt.pluggable.core.source");
		expected.add("org.eclipse.jdt.compiler.apt.source");
		expected.add("org.eclipse.jdt.compiler.tool.source");
		expected.add("org.eclipse.jdt.core.source");
		expected.add("org.eclipse.jdt.core.manipulation.source");
		expected.add("org.eclipse.jdt.debug.ui.source");
		expected.add("org.eclipse.jdt.debug.source");
		expected.add("org.eclipse.jdt.junit.source");
		expected.add("org.eclipse.jdt.junit.runtime.source");
		expected.add("org.eclipse.jdt.junit4.runtime.source");
		expected.add("org.eclipse.jdt.launching.source");
		expected.add("org.eclipse.jdt.ui.source");
		expected.add("org.junit.source");
		expected.add("org.junit4.source");
		if (Platform.getOS() == Platform.OS_MACOSX) {
			expected.add("org.eclipse.jdt.launching.macosx.source");
		}
		for (int i = 0; i < bundles.length; i++) {
			expected.remove(bundles[i].getSymbolicName());
		}
		Iterator iterator = expected.iterator();
		while (iterator.hasNext()) {
			String name = (String) iterator.next();
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());
		
		
		// should be one doc bundle
		bundles = container.resolveBundles(null);
		assertEquals("Wrong bundle count", 1, bundles.length);
		assertEquals("Missing bundle", "org.eclipse.jdt.doc.isv", bundles[0].getSymbolicName());
	}
	
	
	/**
	 * Tests setting the target platform to the JDT feature with a specific version.
	 * 
	 * @throws Exception 
	 */
	public void testSetTargetPlatformToJdtFeature() throws Exception {
		try {
			IPath location = getJdtFeatureLocation();
			String segment = location.lastSegment();
			int index = segment.indexOf('_');
			assertTrue("Missing version id", index > 0);
			String version = segment.substring(index + 1);
			ITargetPlatformService targetService = getTargetService();
			IBundleContainer container = targetService.newFeatureContainer("${eclipse_home}", "org.eclipse.jdt", version);
			ITargetDefinition target = targetService.newTarget();
			target.setBundleContainers(new IBundleContainer[]{container});
			
			setTargetPlatform(target);
			
			Set expected = new HashSet();
			expected.add("org.eclipse.jdt");
			expected.add("org.eclipse.ant.ui");
			expected.add("org.eclipse.jdt.apt.core");
			expected.add("org.eclipse.jdt.apt.ui");
			expected.add("org.eclipse.jdt.apt.pluggable.core");
			expected.add("org.eclipse.jdt.compiler.apt");
			expected.add("org.eclipse.jdt.compiler.tool");
			expected.add("org.eclipse.jdt.core");
			expected.add("org.eclipse.jdt.core.manipulation");
			expected.add("org.eclipse.jdt.debug.ui");
			expected.add("org.eclipse.jdt.debug");
			expected.add("org.eclipse.jdt.junit");
			expected.add("org.eclipse.jdt.junit.runtime");
			expected.add("org.eclipse.jdt.junit4.runtime");
			expected.add("org.eclipse.jdt.launching");
			expected.add("org.eclipse.jdt.ui");
			expected.add("org.junit");
			expected.add("org.junit4");
			expected.add("org.eclipse.jdt.doc.user");
			if (Platform.getOS() == Platform.OS_MACOSX) {
				expected.add("org.eclipse.jdt.launching.macosx");
			}
			
			// current platform
			IPluginModelBase[] models = TargetPlatformHelper.getPDEState().getTargetModels();
			
			assertEquals("Wrong number of bundles in JDT feature", expected.size(), models.length);
			for (int i = 0; i < models.length; i++) {
				expected.remove(models[i].getPluginBase().getId());
			}
			Iterator iterator = expected.iterator();
			while (iterator.hasNext()) {
				String name = (String) iterator.next();
				System.err.println("Missing: " + name);
			}
			assertTrue("Wrong bundles in target platform", expected.isEmpty());
		} finally {
			resetTargetPlatform();
		}
	}	
	
	/**
	 * Tests setting the target platform to empty.
	 */
	public void testSetEmptyTargetPlatform() {
		try {
			ITargetPlatformService targetService = getTargetService();
			ITargetDefinition target = targetService.newTarget();
			
			setTargetPlatform(target);
						
			// current platform
			IPluginModelBase[] models = TargetPlatformHelper.getPDEState().getTargetModels();
			
			assertEquals("Wrong number of bundles in empty target", 0, models.length);

		} finally {
			resetTargetPlatform();
		}		
	}
}
