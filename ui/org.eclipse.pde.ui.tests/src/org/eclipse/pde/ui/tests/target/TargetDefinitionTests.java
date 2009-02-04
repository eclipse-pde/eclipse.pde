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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.osgi.framework.ServiceReference;

/**
 * Tests for target definitions.  The tested targets will be created in the metadata.
 * @see WorkspaceTargetDefinitionTests
 * 
 * @since 3.5 
 */
public class TargetDefinitionTests extends TestCase {
	
	public static Test suite() {
		return new TestSuite(TargetDefinitionTests.class);
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
	 * Returns a new target definition from the target service.  This method is
	 * overridden by WorkspaceTargetDefinitionTests to run using the workspace model
	 * @return a new target definition
	 */
	protected ITargetDefinition getNewTarget() {
		return getTargetService().newTarget();
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
	 * Returns the given input stream as a byte array
	 * @param stream the stream to get as a byte array
	 * @param length the length to read from the stream or -1 for unknown
	 * @return the given input stream as a byte array
	 * @throws IOException
	 */
	protected byte[] getInputStreamAsByteArray(InputStream stream, int length) throws IOException {
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
	 * Returns a default target platform that takes target weaving into account
	 * if in a second instance of Eclipse. This allows the target platform to be 
	 * reset after changing it in a test.
	 * 
	 * @return default settings for target platform
	 */
	protected ITargetDefinition getDefaultTargetPlatorm() {
		ITargetDefinition definition = getNewTarget();
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
		ITargetDefinition definition = getNewTarget();
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
		ITargetDefinition definition = getNewTarget();
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
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
	 * Tests that a target definition based on the default target platform
	 * restricted to a subset of bundle versions contains the right set.
	 * 
	 * @throws Exception
	 */
	public void testVersionRestrictedDefaultTargetPlatform() throws Exception {
		ITargetDefinition definition = getNewTarget();
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		definition.setBundleContainers(new IBundleContainer[]{container});
		List infos = getAllBundleInfos(definition);
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
		infos = getAllBundleInfos(definition);
		
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
		ITargetDefinition definition = getNewTarget();
		IBundleContainer container = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		BundleInfo[] restrictions = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", "xyz", null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", "abc", null, BundleInfo.NO_LEVEL, false)
		};
		container.setIncludedBundles(restrictions);
		definition.setBundleContainers(new IBundleContainer[]{container});
		List infos = getAllBundleInfos(definition);
		
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
		ITargetDefinition definition = getNewTarget();
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
		ITargetDefinition definition = getNewTarget();
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
		ITargetDefinition definition = getNewTarget();
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
		ITargetDefinition definition = getNewTarget();
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
		ITargetDefinition definition = getNewTarget();
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
		ITargetDefinition definition = getNewTarget();
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
		ITargetDefinition definition = getNewTarget();
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
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
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
		ITargetDefinition definition = getNewTarget();
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
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
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
			ITargetDefinition target = getNewTarget();
			IBundleContainer container = getTargetService().newFeatureContainer("${eclipse_home}", "org.eclipse.jdt", version);
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
			if (Platform.getOS().equals(Platform.OS_MACOSX)) {
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
	 * Tests resolution of implicit dependencies in a default target platform
	 * 
	 * @throws Exception
	 */
	public void testImplicitDependencies() throws Exception {
		ITargetDefinition definition = getNewTarget();
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
