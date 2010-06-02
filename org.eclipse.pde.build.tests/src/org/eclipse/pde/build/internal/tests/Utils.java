/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import org.apache.tools.ant.filters.ReplaceTokens;
import org.apache.tools.ant.util.ReaderInputStream;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.pde.build.tests.Activator;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.FeatureGenerator;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;

public class Utils {
	private static final String ID = "id";
	private static final String VERSION = "version";

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

	static public String[] getVersionsNoQualifier(String[] bundles) {
		String[] results = new String[bundles.length];
		for (int i = 0; i < bundles.length; i++) {
			org.osgi.framework.Version version = Platform.getBundle(bundles[i]).getVersion();
			results[i] = String.valueOf(version.getMajor()) + "." + version.getMinor() + '.' + version.getMicro();
		}
		return results;
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
		mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mainAttributes.put(new Attributes.Name("Bundle-ManifestVersion"), "2");
		mainAttributes.put(new Attributes.Name("Bundle-Name"), "Test Bundle " + bundleId);
		mainAttributes.put(new Attributes.Name("Bundle-SymbolicName"), bundleId);
		mainAttributes.put(new Attributes.Name("Bundle-Version"), bundleVersion);
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
		writeBuffer(folder.getFile("src/foo.java"), new StringBuffer("public class foo { int i; }"));
		folder.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	static public void generateBundle(IFolder folder, String bundleId, String version) throws CoreException, IOException {
		generateBundleManifest(folder, bundleId, version, null);
		generatePluginBuildProperties(folder, null);
		writeBuffer(folder.getFile("src/foo.java"), new StringBuffer("public class foo { int i; }"));
		folder.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	static public void storeBuildProperties(IFolder buildFolder, Properties buildProperties) throws FileNotFoundException, IOException {
		storeProperties(buildFolder.getFile("build.properties"), buildProperties);
	}

	static public void storeProperties(IFile propertiesFile, Properties buildProperties) throws FileNotFoundException, IOException {
		File buildPropertiesFile = propertiesFile.getLocation().toFile();
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(buildPropertiesFile));
		buildProperties.store(outputStream, "");
		outputStream.close();
	}

	static public void generateFeature(IFolder workingDirectory, String id, String[] featureList, String[] pluginList) throws CoreException, IOException {
		generateFeature(workingDirectory, id, featureList, pluginList, null, false, false, null);
	}

	static public void generateFeature(IFolder workingDirectory, String id, String[] featureList, String[] pluginList, String version) throws CoreException, IOException {
		generateFeature(workingDirectory, id, featureList, pluginList, null, false, false, version);
	}

	static public void generateFeature(IFolder workingDirectory, String id, String[] featureList, String[] pluginList, String product, boolean includeLaunchers, boolean verify, String version) throws CoreException, IOException {
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
		generator.setVersion(version);
		generator.setProductFile(product);
		generator.setFeatureList(featureList);
		generator.setPluginList(pluginList);
		generator.setWorkingDirectory(workingDirectory.getLocation().toOSString());
		generator.generate();
	}

	static public void generateProduct(IFile productFile, String id, String version, String[] entryList, boolean features) throws CoreException, IOException {
		generateProduct(productFile, id, version, null, entryList, features, null);
	}

	static public void generateProduct(IFile productFile, String id, String version, String application, String[] entryList, boolean features) throws CoreException, IOException {
		generateProduct(productFile, id, version, application, entryList, features, null);
	}

	static public void generateProduct(IFile productFile, String id, String version, String application, String[] entryList, boolean features, StringBuffer extra) throws CoreException, IOException {
		generateProduct(productFile, null, id, version, application, null, entryList, features, extra);
	}

	static public void generateProduct(IFile productFile, String id, String version, String application, String launcher, String[] entryList, boolean features, StringBuffer extra) throws CoreException, IOException {
		generateProduct(productFile, null, id, version, application, launcher, entryList, features, extra);
	}

	static public void generateProduct(IFile productFile, String uid, String id, String version, String application, String launcher, String[] entryList, boolean features, StringBuffer extra) throws CoreException, IOException {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<product ");
		if (uid != null) {
			buffer.append(" uid=\"");
			buffer.append(uid);
			buffer.append("\"");
		}
		if (id != null) {
			buffer.append(" name=\"");
			buffer.append(id);
			buffer.append("\" id=\"");
			buffer.append(id);
			buffer.append("\"");
		}
		if (version != null) {
			buffer.append(" version=\"");
			buffer.append(version);
			buffer.append("\"");
		}
		if (application != null) {
			buffer.append(" application=\"");
			buffer.append(application);
			buffer.append("\"");
		}
		buffer.append(" useFeatures=\"");
		buffer.append(new Boolean(features).toString());
		buffer.append("\">\n");
		buffer.append("  <configIni use=\"default\"/>\n");
		buffer.append("  <launcher name=\"" + (launcher != null ? launcher : "eclipse") + "\"/>\n");
		if (features) {
			buffer.append("  <features>\n");
			for (int i = 0; i < entryList.length; i++) {
				Map items = org.eclipse.pde.internal.build.Utils.parseExtraBundlesString(entryList[i], false);
				buffer.append("    <feature id=\"");
				buffer.append(items.get(ID));
				buffer.append("\"");
				if (items.containsKey(VERSION)) {
					buffer.append(" version=\"");
					buffer.append(items.get(VERSION));
					buffer.append("\"");
				}
				buffer.append("/>\n");
			}
			buffer.append("  </features>\n");
		} else {
			buffer.append("  <plugins>\n");
			for (int i = 0; i < entryList.length; i++) {
				Map items = org.eclipse.pde.internal.build.Utils.parseExtraBundlesString(entryList[i], false);
				buffer.append("    <plugin id=\"");
				buffer.append(items.get(ID));
				buffer.append("\"");
				if (items.containsKey(VERSION)) {
					buffer.append(" version=\"");
					buffer.append(items.get(VERSION));
					buffer.append("\"");
				}
				buffer.append("/>\n");
			}
			buffer.append("  </plugins>\n");
		}

		if (extra != null)
			buffer.append(extra);

		buffer.append("</product>\n");

		Utils.writeBuffer(productFile, buffer);
		productFile.refreshLocal(IResource.DEPTH_INFINITE, null);
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

	private static File findExecutable(File baseLocation) {
		File plugins = new File(baseLocation, "features");
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("org.eclipse.equinox.executable");
			}
		};
		String[] files = plugins.list(filter);

		if (files != null && files.length > 0)
			return baseLocation;
		return null;
	}

	static private File deltaLocation = null;

	public static File findDeltaPack() throws IOException {
		if (deltaLocation != null)
			return deltaLocation;

		File baseLocation = new File(Platform.getInstallLocation().getURL().getPath());

		deltaLocation = findExecutable(baseLocation);
		if (deltaLocation != null)
			return deltaLocation;

		SimpleConfiguratorManipulator manipulator = (SimpleConfiguratorManipulator) BundleHelper.getDefault().acquireService(SimpleConfiguratorManipulator.class.getName());
		if (manipulator != null) {
			BundleInfo[] bundles = manipulator.loadConfiguration(BundleHelper.getDefault().getBundle().getBundleContext(), null);
			//find a fragment for a platform we aren't
			String id = "org.eclipse.equinox.launcher.win32.win32.x86" + (Platform.getOSArch().equals("x86") ? "_64" : "");
			for (int i = 0; i < bundles.length; i++) {
				if (bundles[i].getSymbolicName().equals(id)) {
					URI location = bundles[i].getLocation();
					deltaLocation = findExecutable(URIUtil.toFile(URIUtil.append(location, "../..")));
					if (deltaLocation != null)
						return deltaLocation;
					break;
				}
			}
		}

		deltaLocation = findExecutable(new File(baseLocation.getParent(), "deltapack/eclipse"));
		return deltaLocation;
	}

	public static void writeBuffer(IFile outputFile, StringBuffer buffer) throws IOException, CoreException {
		FileOutputStream stream = null;
		try {
			File output = outputFile.getLocation().toFile();
			output.getParentFile().mkdirs();
			stream = new FileOutputStream(output);
			stream.write(buffer.toString().getBytes());
		} finally {
			if (stream != null)
				stream.close();
		}
		outputFile.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	public static void transferStreams(InputStream source, OutputStream destination) throws IOException {
		transferStreams(source, true, destination, true);
	}

	public static void transferStreams(InputStream source, boolean closeIn, OutputStream destination, boolean closeOut) throws IOException {
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
				if (closeIn)
					source.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				destination.flush();
				if (closeOut)
					destination.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public static boolean extractFromZip(IFolder buildFolder, String zipFile, String zipEntry, IFile outputFile) throws CoreException {
		File folder = new File(buildFolder.getLocation().toOSString());
		File archiveFile = new File(folder, zipFile);
		if (!archiveFile.exists())
			return false;

		ZipFile zip = null;
		try {
			zip = new ZipFile(archiveFile);
			ZipEntry entry = zip.getEntry(zipEntry);
			if (entry == null)
				return false;
			InputStream stream = new BufferedInputStream(zip.getInputStream(entry));
			OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile.getLocation().toFile()));
			transferStreams(stream, out);
		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (zip != null)
					zip.close();
			} catch (IOException e) {
				return false;
			}
		}
		outputFile.refreshLocal(IResource.DEPTH_ONE, null);
		return true;
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

	public static void copy(File source, File target) throws IOException {
		if (!source.exists())
			return;
		if (source.isDirectory()) {
			if (target.exists() && target.isFile())
				target.delete();
			if (!target.exists())
				target.mkdirs();
			File[] children = source.listFiles();
			for (int i = 0; i < children.length; i++)
				copy(children[i], new File(target, children[i].getName()));
			return;
		}
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new BufferedInputStream(new FileInputStream(source));
			output = new BufferedOutputStream(new FileOutputStream(target));

			byte[] buffer = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = input.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					//ignore
				}
			}
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
	}

	public static Manifest loadManifest(IFile file) throws IOException {
		IPath location = file.getLocation();

		if (location.getFileExtension().equals(".jar")) {
			JarFile jar = null;
			try {
				jar = new JarFile(location.toFile());
				return jar.getManifest();
			} finally {
				org.eclipse.pde.internal.build.Utils.close(jar);
			}
		} else if (location.lastSegment().equalsIgnoreCase("MANIFEST.MF")) {
			InputStream stream = new FileInputStream(location.toFile());
			try {
				return new Manifest(stream);
			} finally {
				org.eclipse.pde.internal.build.Utils.close(stream);
			}
		}
		return null;
	}
}
