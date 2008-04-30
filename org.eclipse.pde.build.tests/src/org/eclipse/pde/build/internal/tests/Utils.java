/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import java.util.jar.Attributes.Name;

import org.apache.tools.ant.filters.ReplaceTokens;
import org.apache.tools.ant.util.ReaderInputStream;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.build.tests.Activator;
import org.eclipse.pde.internal.build.FeatureGenerator;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;

public class Utils {

	/**
	 * Transfer the contents of resource into the destination IFile.  During the transfer, replace all 
	 * instances of "@replaceTag@" with "replaceString"
	 * 
	 * @param resource		- input URL
	 * @param destination	- IFile destination
	 * @param replacements	- map of tokens and values to replaces, file should contain "@replaceTag@"
	 * @throws IOException 
	 * @throws CoreException 
	 */
	static public void transferAndReplace(URL resource, IFile destination, Map replacements) throws IOException, CoreException {
		Reader reader = new InputStreamReader(new BufferedInputStream(resource.openStream()));
		final ReplaceTokens replaces = new ReplaceTokens(reader);

		for (Iterator iterator = replacements.keySet().iterator(); iterator.hasNext();) {
			String replaceTag = (String) iterator.next();
			String replaceString = (String) replacements.get(replaceTag);
			ReplaceTokens.Token token = new ReplaceTokens.Token();
			token.setKey(replaceTag);
			token.setValue(replaceString);
			replaces.addConfiguredToken(token);
		}

		ReaderInputStream inputStream = new ReaderInputStream(replaces);
		destination.create(inputStream, true, null);
		inputStream.close();
	}

	static public void generateAllElements(IFolder buildFolder, String element) throws CoreException, IOException {
		if (element != null) {
			// get the productBuild/allElements.xml and replace @ELEMENT@ with element
			URL allElements = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/resources/allElements.xml"), null);
			Map replacements = new HashMap(1);
			replacements.put("ELEMENT", element);
			transferAndReplace(allElements, buildFolder.getFile("allElements.xml"), replacements);
			buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
	}

	static public void generatePluginBuildProperties(IFolder folder, Properties properties) throws FileNotFoundException, IOException {
		Properties buildProperties = properties != null ? properties : new Properties();

		//default contents:
		if (!buildProperties.containsKey("source.."))
			buildProperties.put("source..", "src/");
		if (!buildProperties.containsKey("output.."))
			buildProperties.put("output..", "bin/");
		if (!buildProperties.containsKey("bin.includes"))
			buildProperties.put("bin.includes", "META-INF/, .");

		storeBuildProperties(folder, buildProperties);
	}

	static public void generateBundleManifest(IFolder folder, String bundleId, String bundleVersion, Attributes additionalAttributes) throws CoreException, IOException {
		Manifest manifest = new Manifest();
		Attributes mainAttributes = manifest.getMainAttributes();
		mainAttributes.put(Name.MANIFEST_VERSION, "1.0");
		mainAttributes.put(new Name("Bundle-ManifestVersion"), "2");
		mainAttributes.put(new Name("Bundle-Name"), "Test Bundle " + bundleId);
		mainAttributes.put(new Name("Bundle-SymbolicName"), bundleId);
		mainAttributes.put(new Name("Bundle-Version"), bundleVersion);
		if (additionalAttributes != null)
			mainAttributes.putAll(additionalAttributes);

		IFile manifestFile = folder.getFile(JarFile.MANIFEST_NAME);
		IFolder metaInf = (IFolder) manifestFile.getParent();
		if (!metaInf.exists())
			metaInf.create(true, true, null);
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(manifestFile.getLocation().toFile()));
		manifest.write(outputStream);
		outputStream.close();
	}

	static public void generateBundle(IFolder folder, String bundleId) throws CoreException, IOException {
		generateBundleManifest(folder, bundleId, "1.0.0", null);
		generatePluginBuildProperties(folder, null);
		folder.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	static public void storeBuildProperties(IFolder buildFolder, Properties buildProperties) throws FileNotFoundException, IOException {
		File buildPropertiesFile = new File(buildFolder.getLocation().toFile(), "build.properties");
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(buildPropertiesFile));
		buildProperties.store(outputStream, "");
		outputStream.close();
	}

	static public void generateFeature(IFolder workingDirectory, String id, String[] featureList, String[] pluginList) throws CoreException {
		generateFeature(workingDirectory, id, featureList, pluginList, null, false, false);
	}

	static public void generateFeature(IFolder workingDirectory, String id, String[] featureList, String[] pluginList, String product, boolean includeLaunchers, boolean verify) throws CoreException {
		FeatureGenerator generator = new FeatureGenerator();
		if (verify) {
			FeatureGenerator.setConfigInfo("*,*,*");
			String baseLocation = Platform.getInstallLocation().getURL().getPath();
			BuildTimeSiteFactory.setInstalledBaseSite(baseLocation);
			File delta = findDeltaPack();
			if (delta != null && !delta.equals(new File(baseLocation)))
				generator.setPluginPath(new String[] {delta.getAbsolutePath()});
		}
		generator.setIncludeLaunchers(includeLaunchers);
		generator.setVerify(verify);
		generator.setFeatureId(id);
		generator.setProductFile(product);
		generator.setFeatureList(featureList);
		generator.setPluginList(pluginList);
		generator.setWorkingDirectory(workingDirectory.getLocation().toOSString());
		generator.generate();
	}

	/**
	 * Creates a IFolder resources.  Will create any folders necessary under parent
	 */
	static public IFolder createFolder(IFolder parent, String path) throws CoreException {
		parent.refreshLocal(IResource.DEPTH_INFINITE, null);
		IFolder folder = parent.getFolder(path);
		if (folder.exists())
			return folder;
		IFolder container = (IFolder) folder.getParent();
		if (!container.exists()) {
			LinkedList stack = new LinkedList();
			while (!container.equals(parent) && !container.exists()) {
				stack.add(0, container);
				container = (IFolder) container.getParent();
			}

			for (Iterator iterator = stack.iterator(); iterator.hasNext();) {
				container = (IFolder) iterator.next();
				container.create(true, true, null);
			}
		}
		folder.create(true, true, null);
		return folder;
	}

	public static File findDeltaPack() {
		File baseLocation = new File(Platform.getInstallLocation().getURL().getPath());

		File plugins = new File(baseLocation, "features");
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("org.eclipse.equinox.executable");
			}
		};
		String[] files = plugins.list(filter);

		if (files.length > 0)
			return baseLocation;

		File delta = new File(baseLocation.getParent(), "deltapack/eclipse");
		if (delta.exists()) {
			files = new File(delta, "features").list(filter);
			if (files.length > 0)
				return delta;
		}
		return null;
	}

	public static void writeBuffer(IFile outputFile, StringBuffer buffer) throws IOException {
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(outputFile.getLocation().toFile());
			stream.write(buffer.toString().getBytes());
		} finally {
			if (stream != null)
				stream.close();
		}
	}

	public static void transferStreams(InputStream source, OutputStream destination) throws IOException {
		source = new BufferedInputStream(source);
		destination = new BufferedOutputStream(destination);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				if ((bytesRead = source.read(buffer)) == -1)
					break;
				destination.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				destination.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public static void extractFromZip(IFolder buildFolder, String zipFile, String zipEntry, IFile outputFile) {
		File folder = new File(buildFolder.getLocation().toOSString());
		File archiveFile = new File(folder, zipFile);
		if (!archiveFile.exists())
			return;

		ZipFile zip = null;
		try {
			zip = new ZipFile(archiveFile);
			ZipEntry entry = zip.getEntry(zipEntry);
			if (entry == null)
				return;
			InputStream stream = new BufferedInputStream(zip.getInputStream(entry));
			OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile.getLocation().toFile()));
			transferStreams(stream, out);
		} catch (Exception e) {
			return;
		} finally {
			try {
				if (zip != null)
					zip.close();
			} catch (IOException e) {
				return;
			}
		}
	}

	public static Properties loadProperties(IFile propertiesFile) throws CoreException {
		propertiesFile.refreshLocal(IResource.DEPTH_INFINITE, null);
		if (!propertiesFile.exists())
			return null;

		InputStream stream = null;
		try {
			Properties props = new Properties();
			stream = propertiesFile.getContents(true);
			props.load(stream);
			return props;
		} catch (IOException e) {
			return null;
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
					return null;
				}
		}
	}
}
