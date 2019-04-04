/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 541067
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import junit.framework.TestCase;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.tests.PDETestsPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventHandler;

/**
 * Common utility methods for target definition tests
 */
public abstract class AbstractTargetTest extends TestCase {


	/**
	 * Returns the target platform service or <code>null</code> if none
	 *
	 * @return target platform service
	 */
	protected ITargetPlatformService getTargetService() {
		ServiceReference<ITargetPlatformService> reference = PDETestsPlugin.getBundleContext()
				.getServiceReference(ITargetPlatformService.class);
		assertNotNull("Missing target platform service", reference);
		if (reference == null)
			return null;
		return PDETestsPlugin.getBundleContext().getService(reference);
	}

	/**
	 * Extracts bundles a through e and returns a path to the root directory containing
	 * the plug-ins.
	 *
	 * @return path to the plug-ins directory
	 * @throws Exception
	 */
	protected IPath extractAbcdePlugins() throws Exception {
		IPath stateLocation = PDETestsPlugin.getDefault().getStateLocation();
		IPath location = stateLocation.append("abcde-plugins");
		if (location.toFile().exists()) {
			// recursively delete
			File dir = location.toFile();
			delete(dir);
		}
		return doUnZip(location, "/tests/targets/abcde-plugins.zip");
	}

	/**
	 * Extracts the same plugins as {@link #extractAbcdePlugins()}, but puts them
	 * in a linked folder setup (linked/eclipse/plugins).  Returns the location
	 * of the plugins directory.
	 *
	 * @return path to the plug-ins directory
	 * @throws Exception
	 */
	protected IPath extractLinkedPlugins() throws Exception {
		IPath stateLocation = PDETestsPlugin.getDefault().getStateLocation();
		IPath location = stateLocation.append("abcde/linked/eclipse/plugins");
		if (location.toFile().exists()) {
			// recursively delete
			File dir = location.toFile();
			delete(dir);
		}
		return doUnZip(location, "/tests/targets/abcde-plugins.zip");
	}

	/**
	 * Extracts the modified jdt features archive, if not already done, and returns a path to the
	 * root directory containing the features and plug-ins
	 *
	 * @return path to the root directory
	 * @throws Exception
	 */
	protected IPath extractModifiedFeatures() throws Exception {
		IPath stateLocation = PDETestsPlugin.getDefault().getStateLocation();
		IPath location = stateLocation.append("modified-jdt-features");
		if (location.toFile().exists()) {
			return location;
		}
		doUnZip(location,"/tests/targets/modified-jdt-features.zip");
		// If we are not on the mac, delete the mac launching bundle (in a standard non Mac build, the plug-in wouldn't exist)
		if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
			File macBundle = location.append("plugins").append("org.eclipse.jdt.launching.macosx_3.2.0.v20090527.jar").toFile();
			if (macBundle.exists()){
				assertTrue("Unable to delete test mac launching bundle",macBundle.delete());
			}
		}
		return location;
	}


	/**
	 * Extracts the multiple versions plug-ins archive, if not already done, and returns a path to the
	 * root directory containing the plug-ins.
	 *
	 * @return path to the directory containing the bundles
	 * @throws Exception
	 */
	protected IPath extractMultiVersionPlugins() throws Exception {
		IPath stateLocation = PDETestsPlugin.getDefault().getStateLocation();
		IPath location = stateLocation.append("multi-versions");
		if (location.toFile().exists()) {
			return location;
		}
		doUnZip(location,"/tests/targets/multi-versions.zip");
		return location;
	}

	/**
	 * Unzips the given archive to the specified location.
	 *
	 * @param location path in the local file system
	 * @param archivePath path to archive relative to the test plug-in
	 * @throws IOException
	 */
	private IPath doUnZip(IPath location, String archivePath) throws IOException {
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
	 * Recursively deletes the directory and files within.
	 *
	 * @param dir directory to delete
	 */
	protected void delete(File dir) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				delete(file);
			} else {
				file.delete();
			}
		}
		dir.delete();
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
	 * Returns a new target definition from the target service.  This method is
	 * overridden by {@link WorkspaceTargetDefinitionTests} to use a workspace
	 * target definition
	 *
	 * @return a new target definition
	 */
	protected ITargetDefinition getNewTarget() {
		return getTargetService().newTarget();
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
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(),
				new File(Platform.getConfigurationLocation().getURL().getFile()).getAbsolutePath());
		definition.setTargetLocations(new ITargetLocation[]{container});
		return definition;
	}

	/**
	 * Synchronously sets the target platform based on the given definition. This method should
	 * be called inside of a try/finally block that will always call {@link #resetTargetPlatform()}
	 *
	 * @param target target definition or <code>null</code>
	 * @throws CoreException
	 */
	protected void setTargetPlatform(ITargetDefinition target) throws CoreException {
		final Object[] payload = new Object[1];
		BundleContext bundleContext = PDECore.getDefault().getBundleContext();
		IEclipseContext context = EclipseContextFactory.getServiceContext(bundleContext);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		EventHandler handler = e -> payload[0] = e.getProperty(IEventBroker.DATA);
		eventBroker.subscribe(TargetEvents.TOPIC_WORKSPACE_TARGET_CHANGED, handler);

		// Create the job to load the target, but then join with the job's thread
		LoadTargetDefinitionJob job = new LoadTargetDefinitionJob(target);
		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			assertFalse("Target platform reset interrupted", true);
		}
		ITargetPlatformService service = getTargetService();
		ITargetDefinition definition = (target != null) ? target : service.getWorkspaceTargetDefinition();
		eventBroker.unsubscribe(handler);
		ITargetHandle handle = (target != null) ? target.getHandle() : null;
		assertEquals("Wrong target platform handle preference setting", handle, service.getWorkspaceTargetHandle());
		assertEquals("Wrong workspaceTargetChanged event payload", definition, payload[0]);

	}

	/**
	 * Collects all bundle symbolic names into a set.
	 *
	 * @param infos bundles
	 * @return bundle symbolic names
	 */
	protected Set<String> collectAllSymbolicNames(List<BundleInfo> infos) {
		Set<String> set = new HashSet<>(infos.size());
		for (BundleInfo info : infos) {
			set.add(info.getSymbolicName());
		}
		return set;
	}

	/**
	 * Retrieves all bundles (source and code) in the given target definition
	 * returning them as a list of BundleInfos.
	 *
	 * @param target target definition
	 * @return all BundleInfos
	 */
	protected List<BundleInfo> getAllBundleInfos(ITargetDefinition target) throws Exception {
		if (!target.isResolved()) {
			target.resolve(null);
		}
		TargetBundle[] bundles = target.getBundles();
		List<BundleInfo> list = new ArrayList<>(bundles.length);
		for (TargetBundle bundle : bundles) {
			list.add(bundle.getBundleInfo());
		}
		return list;
	}

	/**
	 * Returns a list of bundles included in the given container.
	 *
	 * @param container bundle container
	 * @return included bundles
	 * @throws Exception
	 */
	protected List<BundleInfo> getBundleInfos(ITargetLocation container) throws Exception {
		TargetBundle[] bundles = container.getBundles();
		List<BundleInfo> list = new ArrayList<>(bundles.length);
		for (TargetBundle bundle : bundles) {
			list.add(bundle.getBundleInfo());
		}
		return list;
	}

	public static ITextFileBuffer getTextFileBufferFromFile(File file) {
		try {
			IPath path = Path.fromOSString(file.getAbsolutePath());
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			manager.connect(path, LocationKind.LOCATION, null);
			return manager.getTextFileBuffer(path, LocationKind.LOCATION);
		} catch (CoreException e) {
			fail("Unable to retrive target definition file");
		}
		return null;
	}
}
