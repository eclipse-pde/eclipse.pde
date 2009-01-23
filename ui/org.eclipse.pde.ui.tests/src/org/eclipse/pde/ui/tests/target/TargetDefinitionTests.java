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

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import junit.framework.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.impl.*;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.osgi.framework.ServiceReference;

/**
 * Tests for target definitions.
 * 
 * @since 3.5 
 */
public class TargetDefinitionTests extends TestCase {
	
	public static Test suite() {
		return new TestSuite(TargetDefinitionTests.class);
	}
	
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
	 * Retrieves all bundles (source and code) in the given target definition
	 * returning them as a list of BundleInfos.
	 * 
	 * @param target target definition
	 * @return all BundleInfos
	 */
	protected List getAllBundleInfos(ITargetDefinition target) throws Exception {
		BundleInfo[] code = target.resolveBundles(null);
		BundleInfo[] source = target.resolveSourceBundles(null);
		List list = new ArrayList(code.length + source.length);
		for (int i = 0; i < code.length; i++) {
			list.add(code[i]);
		}
		for (int i = 0; i < source.length; i++) {
			list.add(source[i]);
		}
		return list;
	}	
	
	/**
	 * Retrieves all *code* bundles in the given target definition
	 * returning them as a list of BundleInfos.
	 * 
	 * @param target target definition
	 * @return all BundleInfos
	 */
	protected List getAllCodeBundleInfos(ITargetDefinition target) throws Exception {
		BundleInfo[] code = target.resolveBundles(null);
		List list = new ArrayList(code.length);
		for (int i = 0; i < code.length; i++) {
			list.add(code[i]);
		}
		return list;
	}	
	
	/**
	 * Collects all bundle symbolic names into a set.
	 * 
	 * @param infos bundles
	 * @return bundle symbolic names
	 */
	protected Set collectAllSymbolicNames(List infos) {
		Set set = new HashSet(infos.size());
		Iterator iterator = infos.iterator();
		while (iterator.hasNext()) {
			BundleInfo info = (BundleInfo) iterator.next();
			set.add(info.getSymbolicName());
		}
		return set;
	}
	
	/**
	 * Returns the resolved location of the specified bundle container.
	 * 
	 * @param container bundle container
	 * @return resolved location
	 * @throws CoreException 
	 */
	protected String getResolvedLocation(IBundleContainer container) throws CoreException {
		return ((AbstractBundleContainer)container).getLocation(true);
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
	 * if in a second instance of Eclipse. This allows the target platform to be 
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
	 * @throws CoreException 
	 */
	protected void resetTargetPlatform() throws CoreException {
		ITargetDefinition definition = getDefaultTargetPlatorm();
		setTargetPlatform(definition);
	}
	
	/**
	 * Sets the target platform based on the given definition.
	 * 
	 * @param target target definition or <code>null</code>
	 * @throws CoreException 
	 */
	protected void setTargetPlatform(ITargetDefinition target) throws CoreException {
		LoadTargetDefinitionJob job = new LoadTargetDefinitionJob(target);
		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			assertFalse("Target platform reset interrupted", true);
		}
		ITargetHandle handle = null;
		if (target != null) {
			handle = target.getHandle();
		}
		assertEquals("Wrong target platform handle preference setting", handle, getTargetService().getWorkspaceTargetHandle());		
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
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
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
	 * Tests that a target definition based on the default target platform
	 * restricted to a subset of bundles contains the right set.
	 * 
	 * @throws Exception
	 */
	public void testRestrictedDefaultTargetPlatform() throws Exception {
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		BundleInfo[] restrictions = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false)
		};
		container.setIncludedBundles(restrictions);
		definition.setBundleContainers(new IBundleContainer[]{container});
		List infos = getAllCodeBundleInfos(definition);
		
		assertEquals("Wrong number of bundles", 2, infos.size());
		Set set = collectAllSymbolicNames(infos);
		for (int i = 0; i < restrictions.length; i++) {
			BundleInfo info = restrictions[i];
			set.remove(info.getSymbolicName());
		}
		assertEquals("Wrong bundles", 0, set.size());
		
	}	
	
	/**
	 * Tests that a target definition based on the default target platform
	 * restricted to a subset of bundle versions contains the right set.
	 * 
	 * @throws Exception
	 */
	public void testVersionRestrictedDefaultTargetPlatform() throws Exception {
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		definition.setBundleContainers(new IBundleContainer[]{container});
		List infos = getAllCodeBundleInfos(definition);
		// find right versions
		String v1 = null;
		String v2 = null;
		Iterator iterator = infos.iterator();
		while (iterator.hasNext() && (v2 == null || v1 == null)) {
			BundleInfo info = (BundleInfo) iterator.next();
			if (info.getSymbolicName().equals("org.eclipse.jdt.launching")) {
				v1 = info.getVersion();
			} else if (info.getSymbolicName().equals("org.eclipse.jdt.debug")) {
				v2 = info.getVersion();
			}
		}
		assertNotNull(v1);
		assertNotNull(v2);
		
		BundleInfo[] restrictions = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", v1, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", v2, null, BundleInfo.NO_LEVEL, false)
		};
		container.setIncludedBundles(restrictions);
		infos = getAllCodeBundleInfos(definition);
		
		assertEquals("Wrong number of bundles", 2, infos.size());
		iterator = infos.iterator();
		while (iterator.hasNext()) {
			BundleInfo info = (BundleInfo) iterator.next();
			if (info.getSymbolicName().equals("org.eclipse.jdt.launching")) {
				assertEquals(v1, info.getVersion());
			} else if (info.getSymbolicName().equals("org.eclipse.jdt.debug")) {
				assertEquals(v2, info.getVersion());
			}
		}
	}	
	
	/**
	 * Tests that a target definition based on the default target platform
	 * restricted to a subset of bundles contains the right set. In this case
	 * empty, since the versions specified are bogus.
	 * 
	 * @throws Exception
	 */
	public void testMissingVersionRestrictedDefaultTargetPlatform() throws Exception {
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		BundleInfo[] restrictions = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", "xyz", null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", "abc", null, BundleInfo.NO_LEVEL, false)
		};
		container.setIncludedBundles(restrictions);
		definition.setBundleContainers(new IBundleContainer[]{container});
		List infos = getAllCodeBundleInfos(definition);
		
		assertEquals("Wrong number of bundles", 0, infos.size());		
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
		IBundleContainer container = getTargetService().newProfileContainer("${eclipse_home}", null);
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
	 * Tests that a target definition based on the JDT feature
	 * restricted to a subset of bundles contains the right set.
	 * 
	 * @throws Exception
	 */
	public void testRestrictedFeatureBundleContainer() throws Exception {
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newFeatureContainer("${eclipse_home}", "org.eclipse.jdt", null);
		BundleInfo[] restrictions = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false)
		};
		container.setIncludedBundles(restrictions);
		definition.setBundleContainers(new IBundleContainer[]{container});
		List infos = getAllBundleInfos(definition);
		
		assertEquals("Wrong number of bundles", 2, infos.size());
		Set set = collectAllSymbolicNames(infos);
		for (int i = 0; i < restrictions.length; i++) {
			BundleInfo info = restrictions[i];
			set.remove(info.getSymbolicName());
		}
		assertEquals("Wrong bundles", 0, set.size());
		
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
				assertTrue(models[i].isEnabled());
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
	 * @throws CoreException 
	 */
	public void testSetEmptyTargetPlatform() throws CoreException {
		try {
			setTargetPlatform(null);
						
			// current platform
			IPluginModelBase[] models = TargetPlatformHelper.getPDEState().getTargetModels();
			
			assertEquals("Wrong number of bundles in empty target", 0, models.length);

		} finally {
			resetTargetPlatform();
		}		
	}
	
	/**
	 * Tests that a complex target definition can be serialized to xml, then deserialized without
	 * any loss of data.
	 * 
	 * @throws Exception
	 */
	public void testPersistComplexDefinition() throws Exception {
		ITargetDefinition definitionA = getTargetService().newTarget();
		
		definitionA.setName("name");
		definitionA.setDescription("description");
		definitionA.setOS("os");
		definitionA.setWS("ws");
		definitionA.setArch("arch");
		definitionA.setNL("nl");
		definitionA.setProgramArguments("program\nargs");
		definitionA.setVMArguments("vm\nargs");
		definitionA.setJREContainer(JavaRuntime.newDefaultJREContainerPath());
		
		BundleInfo[] implicit = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false)
		};		
		definitionA.setImplicitDependencies(implicit);
		
		// Directory container
		IBundleContainer dirContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		// Profile container with specific config area
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), new File(Platform.getConfigurationLocation().getURL().getFile()).getAbsolutePath());
		// Feature container with specific version
		IPath location = getJdtFeatureLocation();
		String segment = location.lastSegment();
		int index = segment.indexOf('_');
		assertTrue("Missing version id", index > 0);
		String version = segment.substring(index + 1);
		IBundleContainer featureContainer = getTargetService().newFeatureContainer("${eclipse_home}", "org.eclipse.jdt", version);
		// Profile container restricted to just two bundles
		IBundleContainer restrictedProfileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		BundleInfo[] restrictions = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false)
		};
		restrictedProfileContainer.setIncludedBundles(restrictions);
		definitionA.setBundleContainers(new IBundleContainer[]{dirContainer, profileContainer, featureContainer, restrictedProfileContainer});
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(definitionA, outputStream);
		ITargetDefinition definitionB = getTargetService().newTarget();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		TargetDefinitionPersistenceHelper.initFromXML(definitionB, inputStream);
		
		assertTargetDefinitionsEqual(definitionA, definitionB);
	}
	
	/**
	 * Tests that an empty target definition can be serialized to xml, then deserialized without
	 * any loss of data.
	 * 
	 * @throws Exception
	 */
	public void testPersistEmptyDefinition() throws Exception {
		ITargetDefinition definitionA = getTargetService().newTarget();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(definitionA, outputStream);
		ITargetDefinition definitionB = getTargetService().newTarget();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		TargetDefinitionPersistenceHelper.initFromXML(definitionB, inputStream);
		assertTargetDefinitionsEqual(definitionA, definitionB);
	}
	
	protected void assertTargetDefinitionsEqual(ITargetDefinition targetA, ITargetDefinition targetB) throws CoreException{
		assertTrue("Target content not equal",((TargetDefinition)targetA).isContentEqual(targetB));
	}
	
	
	/**
	 * Reads a target definition file from the tests/targets/target-files location
	 * with the given name. Note that ".target" will be appended.
	 * 
	 * @param name
	 * @return target definition
	 * @throws Exception
	 */
	protected ITargetDefinition readOldTarget(String name) throws Exception {
		URL url = MacroPlugin.getBundleContext().getBundle().getEntry("/tests/targets/target-files/" + name + ".target");
		File file = new File(FileLocator.toFileURL(url).getFile());
		ITargetDefinition target = getTargetService().newTarget();
		FileInputStream stream = new FileInputStream(file);
		TargetDefinitionPersistenceHelper.initFromXML(target, stream);
		stream.close();
		return target;
	}
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldBasicTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("basic");
		
		assertEquals("Wrong name", "Basic", target.getName());
		assertNull(target.getDescription());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getImplicitDependencies());
		assertNull(target.getJREContainer());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a profile container", containers[0] instanceof ProfileBundleContainer);
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()),
				new Path(getResolvedLocation(containers[0])));
	}
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldBasicDirectoryTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("directory");
		
		assertEquals("Wrong name", "Directory", target.getName());
		assertNull(target.getDescription());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getImplicitDependencies());
		assertNull(target.getJREContainer());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a directory container", containers[0] instanceof DirectoryBundleContainer);
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()).append("plugins"),
				new Path(getResolvedLocation(containers[0])));
	}	
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldSpecificTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("specific");
		
		assertEquals("Wrong name", "Specific Settings", target.getName());
		assertNull(target.getDescription());
		assertEquals("x86", target.getArch());
		assertEquals("linux", target.getOS());
		assertEquals("en_US", target.getNL());
		assertEquals("gtk", target.getWS());
		assertEquals("pgm1 pgm2", target.getProgramArguments());
		assertEquals("-Dfoo=\"bar\"", target.getVMArguments());
		assertEquals(JavaRuntime.newJREContainerPath(JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.4")), target.getJREContainer());
		
		BundleInfo[] infos = target.getImplicitDependencies();
		assertEquals("Wrong number of implicit dependencies", 2, infos.length);
		Set set = new HashSet();
		for (int i = 0; i < infos.length; i++) {
			set.add(infos[i].getSymbolicName());
		}
		assertTrue("Missing ", set.remove("org.eclipse.jdt.debug"));
		assertTrue("Missing ", set.remove("org.eclipse.debug.core"));
		assertTrue(set.isEmpty());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a directory container", containers[0] instanceof DirectoryBundleContainer);
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()).append("plugins"),
				new Path(getResolvedLocation(containers[0])));
	}	
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldAdditionLocationsTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("additionalLocations");
		
		assertEquals("Wrong name", "Additional Locations", target.getName());
		assertNull(target.getDescription());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 3, containers.length);
		assertTrue(containers[0] instanceof ProfileBundleContainer);
		assertTrue(containers[1] instanceof DirectoryBundleContainer);
		assertTrue(containers[2] instanceof DirectoryBundleContainer);
		
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()),
				new Path(getResolvedLocation(containers[0])));
		
		String string = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution("${workspace_loc}");
		assertEquals("Wrong 1st additional location", new Path(string).append("stuff"),
				new Path(getResolvedLocation(containers[1])));
		
		assertEquals("Wrong 2nd additional location", new Path(TargetPlatform.getDefaultLocation()).append("dropins"),
				new Path(getResolvedLocation(containers[2])));
	}		
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldFeaturesTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("featureLocations");
		
		assertEquals("Wrong name", "Features", target.getName());
		assertNull(target.getDescription());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 2, containers.length);
		assertTrue(containers[0] instanceof FeatureBundleContainer);
		assertTrue(containers[1] instanceof FeatureBundleContainer);

		assertEquals("Wrong feature location", "org.eclipse.jdt", ((FeatureBundleContainer)containers[0]).getFeatureId());
		assertEquals("Wrong feature location", "org.eclipse.platform", ((FeatureBundleContainer)containers[1]).getFeatureId());
	}
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldRestrictionsTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("restrictions");
		
		assertEquals("Wrong name", "Restrictions", target.getName());
		assertNull(target.getDescription());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());
		
		BundleInfo[] restrictions = new BundleInfo[]{
			new BundleInfo("org.eclipse.debug.core", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.debug.ui", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.jdt.debug.ui", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false)
		};
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 3, containers.length);
		assertTrue(containers[0] instanceof ProfileBundleContainer);
		assertTrue(containers[1] instanceof FeatureBundleContainer);
		assertTrue(containers[2] instanceof DirectoryBundleContainer);
		
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()), new Path(getResolvedLocation(containers[0])));
		assertEquals("Wrong 1st additional location", "org.eclipse.jdt",((FeatureBundleContainer)containers[1]).getFeatureId());
		assertEquals("Wrong 2nd additional location", new Path(TargetPlatform.getDefaultLocation()).append("dropins"),
				new Path(getResolvedLocation(containers[2])));
		
		for (int i = 0; i < containers.length; i++) {
			IBundleContainer container = containers[i];
			BundleInfo[] actual = container.getIncludedBundles();
			assertNotNull(actual);
			assertEquals("Wrong number of restrictions", restrictions.length, actual.length);
			for (int j = 0; j < actual.length; j++) {
				assertEquals("Wrong restriction", restrictions[j], actual[j]);
			}
		}
	}		
	
	/**
	 * Tests resolution of implicit dependencies in a default target platform
	 * 
	 * @throws Exception
	 */
	public void testImplicitDependencies() throws Exception {
		ITargetDefinition definition = getTargetService().newTarget();
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		definition.setBundleContainers(new IBundleContainer[]{container});
		BundleInfo[] implicit = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false)
		};		
		definition.setImplicitDependencies(implicit);
		BundleInfo[] infos = definition.resolveImplicitDependencies(null);
		
		assertEquals("Wrong number of bundles", 2, infos.length);
		Set set = new HashSet();
		for (int i = 0; i < infos.length; i++) {
			set.add(infos[i].getSymbolicName());
		}
		for (int i = 0; i < implicit.length; i++) {
			BundleInfo info = implicit[i];
			set.remove(info.getSymbolicName());
		}
		assertEquals("Wrong bundles", 0, set.size());
		
	}	
	
}
