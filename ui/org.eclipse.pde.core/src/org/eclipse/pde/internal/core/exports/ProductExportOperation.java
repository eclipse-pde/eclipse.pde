/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.*;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.packager.PackageScriptGenerator;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProductExportOperation extends FeatureExportOperation {

	private String fFeatureLocation;
	private String fRoot;
	private IProduct fProduct;

	public ProductExportOperation(FeatureExportInfo info, IProduct product, String root) {
		super(info);
		fProduct = product;
		fRoot = root;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		String[][] configurations = fInfo.targets;
		if (configurations == null)
			configurations = new String[][] {{TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), TargetPlatform.getNL()}};

		Properties versionAdvice = new Properties();
		monitor.beginTask("", 10 * configurations.length); //$NON-NLS-1$
		for (int i = 0; i < configurations.length; i++) {
			try {
				String[] config = configurations[i];
				if (config[0].equals("macosx") && fInfo.targets == null) //$NON-NLS-1$
					createMacScript(config, new SubProgressMonitor(monitor, 1));
				// create a feature to wrap all plug-ins and features
				String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
				fFeatureLocation = fBuildTempLocation + File.separator + featureID;

				createFeature(featureID, fFeatureLocation, config, true);
				createBuildPropertiesFile(fFeatureLocation, config);
				createConfigIniFile(config);
				createEclipseProductFile();
				createLauncherIniFile(config[0]);
				doExport(featureID, null, fFeatureLocation, config[0], config[1], config[2], new SubProgressMonitor(monitor, 8));
			} catch (IOException e) {
				PDECore.log(e);
			} catch (InvocationTargetException e) {
				throwCoreException(e);
			} finally {

				// Append platform specific version information so that it is available for the p2 post script
				String versionsPrefix = fProduct.useFeatures() ? IPDEBuildConstants.DEFAULT_FEATURE_VERSION_FILENAME_PREFIX : IPDEBuildConstants.DEFAULT_PLUGIN_VERSION_FILENAME_PREFIX;
				File versionFile = new File(fFeatureLocation, versionsPrefix + IPDEBuildConstants.PROPERTIES_FILE_SUFFIX);
				InputStream stream = null;
				try {
					stream = new BufferedInputStream(new FileInputStream(versionFile));
					versionAdvice.load(stream);
				} catch (IOException e) {
				} finally {
					try {
						if (stream != null)
							stream.close();
					} catch (IOException e) {
					}
				}

				// Clean up generated files
				for (int j = 0; j < fInfo.items.length; j++) {
					deleteBuildFiles(fInfo.items[j]);
				}
				cleanup(fInfo.targets == null ? null : configurations[i], new SubProgressMonitor(monitor, 1));
			}
		}

		// Run postscript to generate p2 metadata for product
		String postScript = PackageScriptGenerator.generateP2ProductScript(fFeatureLocation, fProduct.getModel().getInstallLocation(), versionAdvice);
		if (postScript != null) {
			try {
				Map properties = new HashMap();
				addP2MetadataProperties(properties);
				runScript(postScript, null, properties, monitor);
			} catch (InvocationTargetException e) {
				throwCoreException(e);
			}
		}

		cleanup(null, new SubProgressMonitor(monitor, 1));

		monitor.done();
	}

	private File getCustomIniFile(String os) {
		IConfigurationFileInfo info = fProduct.getConfigurationFileInfo();
		String path = info.getPath(os);
		if (path == null) // if we can't find an os path, let's try the normal one
			path = info.getPath(null);
		if (info != null && path != null) {
			String expandedPath = getExpandedPath(path);
			if (expandedPath != null) {
				File file = new File(expandedPath);
				if (file.exists() && file.isFile())
					return file;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#getPaths()
	 */
	protected String[] getPaths() {
		String[] paths = super.getPaths();
		String[] all = new String[paths.length + 1];
		all[0] = fFeatureLocation + File.separator + "feature.xml"; //$NON-NLS-1$
		System.arraycopy(paths, 0, all, 1, paths.length);
		return all;
	}

	private void createBuildPropertiesFile(String featureLocation, String[] config) {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory())
			file.mkdirs();

		boolean hasLaunchers = PDECore.getDefault().getFeatureModelManager().getDeltaPackFeature() != null;
		Properties properties = new Properties();
		properties.put(IBuildPropertiesConstants.ROOT, getRootFileLocations(hasLaunchers)); //To copy a folder
		if (!hasLaunchers) {
			properties.put("root.permissions.755", getLauncherName()); //$NON-NLS-1$
			if (TargetPlatform.getWS().equals("motif") && TargetPlatform.getOS().equals("linux")) { //$NON-NLS-1$ //$NON-NLS-2$
				properties.put("root.linux.motif.x86.permissions.755", "libXm.so.2"); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$
				properties.put("root.macosx.carbon.ppc.permissions.755", //$NON-NLS-1$
						"${launcherName}.app/Contents/MacOS/${launcherName}"); //$NON-NLS-1$
			}
		}

		IJREInfo jreInfo = fProduct.getJREInfo();
		File vm = jreInfo != null ? jreInfo.getJVMLocation(config[0]) : null;
		if (vm != null) {
			properties.put("root." + config[0] + //$NON-NLS-1$
					"." + config[1] + //$NON-NLS-1$
					"." + config[2] + //$NON-NLS-1$
					".folder.jre", //$NON-NLS-1$
					"absolute:" + vm.getAbsolutePath()); //$NON-NLS-1$
			String perms = (String) properties.get("root.permissions.755"); //$NON-NLS-1$
			if (perms != null) {
				StringBuffer buffer = new StringBuffer(perms);
				buffer.append(","); //$NON-NLS-1$
				buffer.append("jre/bin/java"); //$NON-NLS-1$
				properties.put("root.permissions.755", buffer.toString()); //$NON-NLS-1$
			}
		}

		save(new File(file, "build.properties"), properties, "Build Configuration"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String getRootFileLocations(boolean hasLaunchers) {
		StringBuffer buffer = new StringBuffer();

		File homeDir = new File(TargetPlatform.getLocation());
		if (!hasLaunchers) {
			if (homeDir.exists() && homeDir.isDirectory()) {
				appendAbsolutePath(buffer, new File(homeDir, "startup.jar")); //$NON-NLS-1$
				if (!TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$
					// try to retrieve the exact eclipse launcher path
					// see bug 205833
					File file = null;
					if (System.getProperties().get("eclipse.launcher") != null) { //$NON-NLS-1$
						String launcherPath = System.getProperties().get("eclipse.launcher").toString(); //$NON-NLS-1$
						file = new File(launcherPath);
						if (file.exists() && !file.isDirectory()) {
							appendAbsolutePath(buffer, file);
						} else { // just assume traditional eclipse paths
							appendEclipsePath(buffer, homeDir);
						}
					} else { // just assume traditional eclipse paths
						appendEclipsePath(buffer, homeDir);
					}
					file = new File(homeDir, "libXm.so.2"); //$NON-NLS-1$
					if (file.exists()) {
						appendAbsolutePath(buffer, file);
					}
				}
			}
		}
		// add content of temp folder (.eclipseproduct, configuration/config.ini)
		if (buffer.length() > 0)
			buffer.append(","); //$NON-NLS-1$
		buffer.append("/temp/"); //$NON-NLS-1$

		return buffer.toString();
	}

	private void appendEclipsePath(StringBuffer buffer, File homeDir) {
		File file = null;
		file = new File(homeDir, "eclipse"); //$NON-NLS-1$
		if (file.exists()) {
			appendAbsolutePath(buffer, file);
		}
		file = new File(homeDir, "eclipse.exe"); //$NON-NLS-1$
		if (file.exists()) {
			appendAbsolutePath(buffer, file);
		}
	}

	private void appendAbsolutePath(StringBuffer buffer, File file) {
		if (buffer.length() > 0)
			buffer.append(","); //$NON-NLS-1$

		buffer.append("absolute:file:"); //$NON-NLS-1$
		buffer.append(file.getAbsolutePath());
	}

	private void createEclipseProductFile() {
		File dir = new File(fFeatureLocation, "temp"); //$NON-NLS-1$
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();
		Properties properties = new Properties();
		IPluginModelBase model = PluginRegistry.findModel(getBrandingPlugin());
		if (model != null)
			properties.put("name", model.getResourceString(fProduct.getName())); //$NON-NLS-1$
		else
			properties.put("name", fProduct.getName()); //$NON-NLS-1$
		properties.put("id", fProduct.getId()); //$NON-NLS-1$
		if (model != null)
			properties.put("version", model.getPluginBase().getVersion()); //$NON-NLS-1$
		File product = new File(dir, ".eclipseproduct"); //$NON-NLS-1$
		save(product, properties, "Eclipse Product File"); //$NON-NLS-1$
	}

	private void createLauncherIniFile(String os) {
		String programArgs = getProgramArguments(os);
		String vmArgs = getVMArguments(os);

		if (programArgs.length() == 0 && vmArgs.length() == 0)
			return;

		File dir = new File(fFeatureLocation, "temp"); //$NON-NLS-1$
		// need to place launcher.ini file in special directory for MacOSX (bug 164762)
		if (Platform.OS_MACOSX.equals(os)) {
			dir = new File(dir, "Eclipse.app/Contents/MacOS"); //$NON-NLS-1$
		}
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();

		String lineDelimiter = Platform.OS_WIN32.equals(os) ? "\r\n" : "\n"; //$NON-NLS-1$ //$NON-NLS-2$

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(dir, getLauncherName() + ".ini"))); //$NON-NLS-1$
			ExecutionArguments args = new ExecutionArguments(vmArgs, programArgs);

			// add program arguments
			String[] array = args.getProgramArgumentsArray();
			for (int i = 0; i < array.length; i++) {
				writer.print(array[i]);
				writer.print(lineDelimiter);
			}

			// add VM arguments
			array = args.getVMArgumentsArray();
			if (array.length > 0) {
				writer.print("-vmargs"); //$NON-NLS-1$
				writer.print(lineDelimiter);
				for (int i = 0; i < array.length; i++) {
					writer.print(array[i]);
					writer.print(lineDelimiter);
				}
			}
		} catch (IOException e) {
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	private String getProgramArguments(String os) {
		IArgumentsInfo info = fProduct.getLauncherArguments();
		return info != null ? CoreUtility.normalize(info.getCompleteProgramArguments(os)) : "";//$NON-NLS-1$
	}

	private String getVMArguments(String os) {
		IArgumentsInfo info = fProduct.getLauncherArguments();
		return (info != null) ? CoreUtility.normalize(info.getCompleteVMArguments(os)) : ""; //$NON-NLS-1$
	}

	private void createConfigIniFile(String[] config) {
		File dir = new File(fFeatureLocation, "temp/configuration"); //$NON-NLS-1$
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();

		PrintWriter writer = null;

		File custom = getCustomIniFile(config[0]);
		if (custom != null) {
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(custom));
				writer = new PrintWriter(new FileWriter(new File(dir, "config.ini"))); //$NON-NLS-1$
				String line;
				while ((line = in.readLine()) != null) {
					writer.println(line);
				}
			} catch (IOException e) {
			} finally {
				try {
					if (in != null)
						in.close();
					if (writer != null)
						writer.close();
				} catch (IOException e) {
				}
			}
			return;
		}
		try {
			writer = new PrintWriter(new FileWriter(new File(dir, "config.ini"))); //$NON-NLS-1$
			String location = getSplashLocation(config[0], config[1], config[2]);
			writer.println("#Product Runtime Configuration File"); //$NON-NLS-1$
			writer.println();
			if (location != null)
				writer.println("osgi.splashPath=" + location); //$NON-NLS-1$
			writer.println("eclipse.product=" + fProduct.getId()); //$NON-NLS-1$
			writer.println("osgi.bundles.defaultStartLevel=4"); //$NON-NLS-1$

			String bundleList = getPluginList(config, TargetPlatform.getBundleList());
			writer.println("osgi.bundles=" + bundleList); //$NON-NLS-1$

		} catch (IOException e) {
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	private String getSplashLocation(String os, String ws, String arch) {
		ISplashInfo info = fProduct.getSplashInfo();
		String plugin = null;
		if (info != null) {
			plugin = info.getLocation();
		}
		if (plugin == null || plugin.trim().length() == 0)
			plugin = getBrandingPlugin();

		if (plugin == null)
			return null;

		StringBuffer buffer = new StringBuffer("platform:/base/plugins/"); //$NON-NLS-1$
		buffer.append(plugin.trim());

		State state = getState(os, ws, arch);
		BundleDescription bundle = state.getBundle(plugin, null);
		if (bundle != null) {
			BundleDescription[] fragments = bundle.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				String id = fragments[i].getSymbolicName();
				if (fProduct.containsPlugin(id)) {
					buffer.append(",platform:/base/plugins/"); //$NON-NLS-1$
					buffer.append(id);
				}
			}
		}
		return buffer.toString();
	}

	private String getBrandingPlugin() {
		int dot = fProduct.getId().lastIndexOf('.');
		return (dot != -1) ? fProduct.getId().substring(0, dot) : null;
	}

	private String getPluginList(String[] config, String bundleList) {
		if (fProduct.useFeatures()) {
			// if we're using features and find simple configurator in the list, let's just default and use update configurator 
			if (bundleList.indexOf("org.eclipse.equinox.simpleconfigurator") != -1) { //$NON-NLS-1$
				return "org.eclipse.equinox.common@2:start,org.eclipse.update.configurator@3:start,org.eclipse.core.runtime@start"; //$NON-NLS-1$
			}
			return bundleList;
		}

		StringBuffer buffer = new StringBuffer();

		// include only bundles that are actually in this product configuration
		Set initialBundleSet = new HashSet();
		StringTokenizer tokenizer = new StringTokenizer(bundleList, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			int index = token.indexOf('@');
			String id = index != -1 ? token.substring(0, index) : token;
			if (fProduct.containsPlugin(id)) {
				if (buffer.length() > 0)
					buffer.append(',');
				buffer.append(id);
				if (index != -1 && index < token.length() - 1)
					buffer.append(token.substring(index));
				initialBundleSet.add(id);
			}
		}

		if (!fProduct.containsPlugin("org.eclipse.update.configurator")) { //$NON-NLS-1$
			initialBundleSet.add("org.eclipse.osgi"); //$NON-NLS-1$

			Dictionary environment = new Hashtable(4);
			environment.put("osgi.os", config[0]); //$NON-NLS-1$
			environment.put("osgi.ws", config[1]); //$NON-NLS-1$
			environment.put("osgi.arch", config[2]); //$NON-NLS-1$
			environment.put("osgi.nl", config[3]); //$NON-NLS-1$

			BundleContext context = PDECore.getDefault().getBundleContext();
			for (int i = 0; i < fInfo.items.length; i++) {
				BundleDescription bundle = (BundleDescription) fInfo.items[i];
				String filterSpec = bundle.getPlatformFilter();
				try {
					if (filterSpec == null || context.createFilter(filterSpec).match(environment)) {
						String id = ((BundleDescription) fInfo.items[i]).getSymbolicName();
						if (!initialBundleSet.contains(id)) {
							if (buffer.length() > 0)
								buffer.append(","); //$NON-NLS-1$
							buffer.append(id);

							// ensure core.runtime is always started
							if ("org.eclipse.core.runtime".equals(id)) { //$NON-NLS-1$
								buffer.append("@start"); //$NON-NLS-1$
							}
						}
					}
				} catch (InvalidSyntaxException e) {
				}
			}
		}
		return buffer.toString();
	}

	protected HashMap createAntBuildProperties(String os, String ws, String arch) {
		HashMap properties = super.createAntBuildProperties(os, ws, arch);
		properties.put(IXMLConstants.PROPERTY_LAUNCHER_NAME, getLauncherName());

		ILauncherInfo info = fProduct.getLauncherInfo();
		if (info != null) {
			String images = null;
			if (os.equals("win32")) { //$NON-NLS-1$
				images = getWin32Images(info);
			} else if (os.equals("solaris")) { //$NON-NLS-1$
				images = getSolarisImages(info);
			} else if (os.equals("linux")) { //$NON-NLS-1$
				images = getExpandedPath(info.getIconPath(ILauncherInfo.LINUX_ICON));
			} else if (os.equals("macosx")) { //$NON-NLS-1$
				images = getExpandedPath(info.getIconPath(ILauncherInfo.MACOSX_ICON));
			}
			if (images != null && images.length() > 0)
				properties.put(IXMLConstants.PROPERTY_LAUNCHER_ICONS, images);
		}

		fAntBuildProperties.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, fRoot);
		fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_PREFIX, fRoot);

		addP2MetadataProperties(properties);

		return properties;
	}

	/**
	 * Adds the standard p2 metadata generator properties to the given map.
	 * @param map the map to add properties to
	 */
	private void addP2MetadataProperties(Map map) {
		if (fInfo.toDirectory && fInfo.exportMetadata) {
			map.put(IXMLConstants.TARGET_P2_METADATA, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_FLAVOR, P2Utils.P2_FLAVOR_DEFAULT);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_PUBLISH_ARTIFACTS, IBuildPropertiesConstants.TRUE);
			try {
				map.put(IBuildPropertiesConstants.PROPERTY_P2_METADATA_REPO, new File(fInfo.destinationDirectory + "/repository").toURL().toString()); //$NON-NLS-1$
				map.put(IBuildPropertiesConstants.PROPERTY_P2_ARTIFACT_REPO, new File(fInfo.destinationDirectory + "/repository").toURL().toString()); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				PDECore.log(e);
			}
		}
	}

	private String getLauncherName() {
		ILauncherInfo info = fProduct.getLauncherInfo();
		if (info != null) {
			String name = info.getLauncherName();
			if (name != null && name.length() > 0) {
				name = name.trim();
				if (name.endsWith(".exe")) //$NON-NLS-1$
					name = name.substring(0, name.length() - 4);
				return name;
			}
		}
		return "eclipse"; //$NON-NLS-1$
	}

	private String getWin32Images(ILauncherInfo info) {
		StringBuffer buffer = new StringBuffer();
		if (info.usesWinIcoFile()) {
			append(buffer, info.getIconPath(ILauncherInfo.P_ICO_PATH));
		} else {
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_16_LOW));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_16_HIGH));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_32_HIGH));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_32_LOW));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_48_HIGH));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_48_LOW));
		}
		return buffer.length() > 0 ? buffer.toString() : null;
	}

	private String getSolarisImages(ILauncherInfo info) {
		StringBuffer buffer = new StringBuffer();
		append(buffer, info.getIconPath(ILauncherInfo.SOLARIS_LARGE));
		append(buffer, info.getIconPath(ILauncherInfo.SOLARIS_MEDIUM));
		append(buffer, info.getIconPath(ILauncherInfo.SOLARIS_SMALL));
		append(buffer, info.getIconPath(ILauncherInfo.SOLARIS_TINY));
		return buffer.length() > 0 ? buffer.toString() : null;
	}

	private void append(StringBuffer buffer, String path) {
		path = getExpandedPath(path);
		if (path != null) {
			if (buffer.length() > 0)
				buffer.append(","); //$NON-NLS-1$
			buffer.append(path);
		}
	}

	private String getExpandedPath(String path) {
		if (path == null || path.length() == 0)
			return null;
		IResource resource = PDECore.getWorkspace().getRoot().findMember(new Path(path));
		if (resource != null) {
			IPath fullPath = resource.getLocation();
			return fullPath == null ? null : fullPath.toOSString();
		}
		return null;
	}

	private void save(File file, Properties properties, String header) {
		try {
			FileOutputStream stream = new FileOutputStream(file);
			properties.store(stream, header);
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	protected void setupGenerator(BuildScriptGenerator generator, String featureID, String versionId, String os, String ws, String arch, String featureLocation) throws CoreException {
		super.setupGenerator(generator, featureID, versionId, os, ws, arch, featureLocation);
		generator.setGenerateVersionsList(true);
		if (fProduct != null)
			generator.setProduct(fProduct.getModel().getInstallLocation());
	}

	private void createMacScript(String[] config, IProgressMonitor monitor) {
		String entryName = TargetPlatformHelper.getTargetVersion() >= 3.3 ? "macosx/Info.plist" //$NON-NLS-1$
				: "macosx/Info.plist.32"; //$NON-NLS-1$
		URL url = PDECore.getDefault().getBundle().getEntry(entryName);
		if (url == null)
			return;

		File scriptFile = null;
		File plist = null;
		InputStream in = null;
		String location = PDECore.getDefault().getStateLocation().toOSString();
		try {
			in = url.openStream();
			File dir = new File(location, "Eclipse.app/Contents"); //$NON-NLS-1$
			dir.mkdirs();
			plist = new File(dir, "Info.plist"); //$NON-NLS-1$
			CoreUtility.readFile(in, plist);
			scriptFile = createScriptFile("macbuild.xml"); //$NON-NLS-1$
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();

			Element root = doc.createElement("project"); //$NON-NLS-1$
			root.setAttribute("name", "project"); //$NON-NLS-1$ //$NON-NLS-2$
			root.setAttribute("default", "default"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(root);

			Element property = doc.createElement("property"); //$NON-NLS-1$
			property.setAttribute("name", "eclipse.base"); //$NON-NLS-1$ //$NON-NLS-2$
			property.setAttribute("value", "${assemblyTempDir}/${collectingFolder}"); //$NON-NLS-1$ //$NON-NLS-2$
			root.appendChild(property);

			Element target = doc.createElement("target"); //$NON-NLS-1$
			target.setAttribute("name", "default"); //$NON-NLS-1$ //$NON-NLS-2$
			root.appendChild(target);

			Element copy = doc.createElement("copy"); //$NON-NLS-1$
			StringBuffer toDir = new StringBuffer("${eclipse.base}/"); //$NON-NLS-1$
			toDir.append(config[0]);
			toDir.append("."); //$NON-NLS-1$
			toDir.append(config[1]);
			toDir.append("."); //$NON-NLS-1$
			toDir.append(config[2]);
			toDir.append("/${collectingFolder}"); //$NON-NLS-1$
			copy.setAttribute("todir", toDir.toString()); //$NON-NLS-1$ 
			copy.setAttribute("failonerror", "false"); //$NON-NLS-1$ //$NON-NLS-2$
			copy.setAttribute("overwrite", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			target.appendChild(copy);

			Element fileset = doc.createElement("fileset"); //$NON-NLS-1$
			fileset.setAttribute("dir", "${installFolder}"); //$NON-NLS-1$ //$NON-NLS-2$
			fileset.setAttribute("includes", "Eclipse.app/Contents/MacOS/eclipse"); //$NON-NLS-1$ //$NON-NLS-2$
			copy.appendChild(fileset);

			fileset = doc.createElement("fileset"); //$NON-NLS-1$
			fileset.setAttribute("dir", "${template}"); //$NON-NLS-1$ //$NON-NLS-2$
			fileset.setAttribute("includes", "Eclipse.app/Contents/Info.plist"); //$NON-NLS-1$ //$NON-NLS-2$
			copy.appendChild(fileset);

			XMLPrintHandler.writeFile(doc, scriptFile);

			AntRunner runner = new AntRunner();
			HashMap map = new HashMap();
			if (!fInfo.toDirectory) {
				String filename = fInfo.zipFileName;
				map.put(IXMLConstants.PROPERTY_ARCHIVE_FULLPATH, fInfo.destinationDirectory + File.separator + filename);
			} else {
				map.put(IXMLConstants.PROPERTY_ASSEMBLY_TMP, fInfo.destinationDirectory);
			}
			map.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, fRoot);
			map.put("installFolder", TargetPlatform.getLocation()); //$NON-NLS-1$
			map.put("template", location); //$NON-NLS-1$
			runner.addUserProperties(map);
			runner.setBuildFileLocation(scriptFile.getAbsolutePath());
			runner.setExecutionTargets(new String[] {"default"}); //$NON-NLS-1$
			runner.run(new SubProgressMonitor(monitor, 1));
		} catch (FactoryConfigurationError e) {
		} catch (ParserConfigurationException e) {
		} catch (CoreException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
			CoreUtility.deleteContent(new File(location, "Eclipse.app")); //$NON-NLS-1$
			if (scriptFile != null && scriptFile.exists())
				scriptFile.delete();
			monitor.done();
		}
	}

	protected void setAdditionalAttributes(Element plugin, BundleDescription bundle) {
		// always make sure launcher fragments are flat; or else you will have launching problems
		HostSpecification host = bundle.getHost();
		boolean unpack = (host != null && host.getName().equals("org.eclipse.equinox.launcher")) //$NON-NLS-1$
		? true
				: CoreUtility.guessUnpack(bundle);
		plugin.setAttribute("unpack", Boolean.toString(unpack)); //$NON-NLS-1$
	}

}
