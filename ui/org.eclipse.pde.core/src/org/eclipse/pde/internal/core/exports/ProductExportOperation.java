/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.w3c.dom.Element;

public class ProductExportOperation extends FeatureExportOperation {
	private static final String STATUS_MESSAGE = "!MESSAGE"; //$NON-NLS-1$
	private static final String STATUS_ENTRY = "!ENTRY"; //$NON-NLS-1$
	private static final String STATUS_SUBENTRY = "!SUBENTRY"; //$NON-NLS-1$
	private static final String ECLIPSE_APP_MACOS = "Eclipse.app/Contents/MacOS"; //$NON-NLS-1$
	private static final String ECLIPSE_APP_CONTENTS = "Eclipse.app/Contents"; //$NON-NLS-1$
	private static final String MAC_JAVA_FRAMEWORK = "/System/Library/Frameworks/JavaVM.framework"; //$NON-NLS-1$
	private String fFeatureLocation;
	private String fRoot;
	private IProduct fProduct;

	protected static String errorMessage;

	public static void setErrorMessage(String message) {
		errorMessage = message;
	}

	public static String getErrorMessage() {
		return errorMessage;
	}

	public static IStatus parseErrorMessage(CoreException e) {
		if (errorMessage == null)
			return null;

		MultiStatus status = null;
		StringTokenizer tokenizer = new StringTokenizer(errorMessage, "\n"); //$NON-NLS-1$
		for (; tokenizer.hasMoreTokens();) {
			String line = tokenizer.nextToken().trim();
			if (line.startsWith(STATUS_ENTRY) && tokenizer.hasMoreElements()) {
				String next = tokenizer.nextToken();
				if (next.startsWith(STATUS_MESSAGE)) {
					status = new MultiStatus(PDECore.PLUGIN_ID, 0, next.substring(8), null);
				}
			} else if (line.startsWith(STATUS_SUBENTRY) && tokenizer.hasMoreElements() && status != null) {
				String next = tokenizer.nextToken();
				if (next.startsWith(STATUS_MESSAGE)) {
					status.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, next.substring(8)));
				}
			}
		}
		if (status != null)
			return status;

		//parsing didn't work, just set the message
		return new MultiStatus(PDECore.PLUGIN_ID, 0, new IStatus[] {e.getStatus()}, errorMessage, null);
	}

	public ProductExportOperation(FeatureExportInfo info, String name, IProduct product, String root) {
		super(info, name);
		fProduct = product;
		fRoot = root;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.exports.FeatureExportOperation#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		String[][] configurations = fInfo.targets;
		if (configurations == null)
			configurations = new String[][] {{TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), TargetPlatform.getNL()}};

		cleanupBuildRepo();
		errorMessage = null;

		try {
			monitor.beginTask("", 10); //$NON-NLS-1$
			try {
				// create a feature to wrap all plug-ins and features
				String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
				fFeatureLocation = fBuildTempLocation + File.separator + featureID;

				createFeature(featureID, fFeatureLocation, configurations, fProduct.includeLaunchers());
				createBuildPropertiesFile(fFeatureLocation, configurations);
				doExport(featureID, null, fFeatureLocation, configurations, new SubProgressMonitor(monitor, 8));
			} catch (IOException e) {
				PDECore.log(e);
			} catch (InvocationTargetException e) {
				return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.FeatureBasedExportOperation_ProblemDuringExport, e.getTargetException());
			} catch (CoreException e) {
				if (errorMessage != null)
					return parseErrorMessage(e);
				return e.getStatus();
			} finally {
				// Clean up generated files
				for (int j = 0; j < fInfo.items.length; j++) {
					try {
						deleteBuildFiles(fInfo.items[j]);
					} catch (CoreException e) {
						PDECore.log(e);
					}
				}
				cleanup(null, new SubProgressMonitor(monitor, 1));
			}

			if (hasAntErrors()) {
				return new Status(IStatus.WARNING, PDECore.PLUGIN_ID, NLS.bind(PDECoreMessages.FeatureExportOperation_CompilationErrors, fInfo.destinationDirectory));
			}

		} finally {
			monitor.done();
			errorMessage = null;
		}
		return Status.OK_STATUS;
	}

	protected boolean groupedConfigurations() {
		// we never group product exports
		return false;
	}

	private void cleanupBuildRepo() {
		File metadataTemp = new File(fBuildTempMetadataLocation);
		if (metadataTemp.exists()) {
			//make sure our build metadata repo is clean
			deleteDir(metadataTemp);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#getPaths()
	 */
	protected String[] getPaths() {
		String[] paths = super.getPaths();
		String[] all = new String[paths.length + 1];
		all[0] = fFeatureLocation + File.separator + ICoreConstants.FEATURE_FILENAME_DESCRIPTOR;
		System.arraycopy(paths, 0, all, 1, paths.length);
		return all;
	}

	private void createBuildPropertiesFile(String featureLocation, String[][] configurations) {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory())
			file.mkdirs();

		boolean hasLaunchers = PDECore.getDefault().getFeatureModelManager().getDeltaPackFeature() != null;
		Properties properties = new Properties();
		if (fProduct.includeLaunchers() && !hasLaunchers && configurations.length > 0) {
			String rootPrefix = IBuildPropertiesConstants.ROOT_PREFIX + configurations[0][0] + "." + configurations[0][1] + "." + configurations[0][2]; //$NON-NLS-1$ //$NON-NLS-2$
			properties.put(rootPrefix, getRootFileLocations(hasLaunchers));
			if (TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$
				String plist = createMacInfoPList();
				if (plist != null)
					properties.put(rootPrefix + ".folder." + ECLIPSE_APP_CONTENTS, "absolute:file:" + plist); //$NON-NLS-1$ //$NON-NLS-2$
				properties.put(rootPrefix + ".folder." + ECLIPSE_APP_MACOS, getLauncherLocations(hasLaunchers)); //$NON-NLS-1$
				properties.put(rootPrefix + ".permissions.755", ECLIPSE_APP_MACOS + "/" + getLauncherName()); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				properties.put(rootPrefix, getLauncherLocations(hasLaunchers)); //To copy a folder
				properties.put(rootPrefix + ".permissions.755", getLauncherName()); //$NON-NLS-1$
				if (TargetPlatform.getWS().equals("motif") && TargetPlatform.getOS().equals("linux")) { //$NON-NLS-1$ //$NON-NLS-2$
					properties.put(rootPrefix + ".permissions.755", "libXm.so.2"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		IJREInfo jreInfo = fProduct.getJREInfo();
		for (int i = 0; i < configurations.length; i++) {
			String[] config = configurations[i];
			File vm = jreInfo != null ? jreInfo.getJVMLocation(config[0]) : null;

			if (vm != null) {

				if (config[0].equals("macosx") && vm.getPath().startsWith(MAC_JAVA_FRAMEWORK)) { //$NON-NLS-1$
					continue;
				}

				String rootPrefix = IBuildPropertiesConstants.ROOT_PREFIX + config[0] + "." + config[1] + //$NON-NLS-1$
						"." + config[2]; //$NON-NLS-1$
				properties.put(rootPrefix + ".folder.jre", "absolute:" + vm.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
				String perms = (String) properties.get(rootPrefix + ".permissions.755"); //$NON-NLS-1$
				if (perms != null) {
					StringBuffer buffer = new StringBuffer(perms);
					buffer.append(","); //$NON-NLS-1$
					buffer.append("jre/bin/java"); //$NON-NLS-1$
					properties.put(rootPrefix + ".permissions.755", buffer.toString()); //$NON-NLS-1$
				}
			}
		}

		if (fInfo.exportSource && fInfo.exportSourceBundle) {
			properties.put(IBuildPropertiesConstants.PROPERTY_INDIVIDUAL_SOURCE, "true"); //$NON-NLS-1$
			List workspacePlugins = Arrays.asList(PluginRegistry.getWorkspaceModels());
			for (int i = 0; i < fInfo.items.length; i++) {
				if (fInfo.items[i] instanceof IFeatureModel) {
					IFeature feature = ((IFeatureModel) fInfo.items[i]).getFeature();
					properties.put("generate.feature@" + feature.getId().trim() + ".source", feature.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					BundleDescription bundle = null;
					if (fInfo.items[i] instanceof IPluginModelBase) {
						bundle = ((IPluginModelBase) fInfo.items[i]).getBundleDescription();
					}
					if (bundle == null) {
						if (fInfo.items[i] instanceof BundleDescription)
							bundle = (BundleDescription) fInfo.items[i];
					}
					if (bundle == null)
						continue;

					//it doesn't matter if we generate extra properties for platforms we aren't exporting for
					if (workspacePlugins.contains(PluginRegistry.findModel(bundle))) {
						properties.put("generate.plugin@" + bundle.getSymbolicName().trim() + ".source", bundle.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
		save(new File(file, ICoreConstants.BUILD_FILENAME_DESCRIPTOR), properties, "Build Configuration"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.exports.FeatureExportOperation#publishingP2Metadata()
	 */
	protected boolean publishingP2Metadata() {
		return fInfo.exportMetadata;
	}

	private String getLauncherLocations(boolean hasLaunchers) {
		//get the launchers for the eclipse install
		StringBuffer buffer = new StringBuffer();
		if (!hasLaunchers) {
			File homeDir = new File(TargetPlatform.getLocation());
			if (homeDir.exists() && homeDir.isDirectory()) {
				// try to retrieve the exact eclipse launcher path
				// see bug 205833
				File file = null;
				if (System.getProperties().get("eclipse.launcher") != null) { //$NON-NLS-1$
					String launcherPath = System.getProperties().get("eclipse.launcher").toString(); //$NON-NLS-1$
					file = new File(launcherPath);
				}

				if (file != null && file.exists() && !file.isDirectory()) {
					appendAbsolutePath(buffer, file);
				} else if (TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$)
					appendEclipsePath(buffer, new File(homeDir, ECLIPSE_APP_MACOS));
				} else {
					appendEclipsePath(buffer, homeDir);
				}
			}
		}
		return buffer.toString();
	}

	private String getRootFileLocations(boolean hasLaunchers) {
		//Get the files that go in the root of the eclipse install, excluding the launcher
		StringBuffer buffer = new StringBuffer();
		if (!hasLaunchers) {
			File homeDir = new File(TargetPlatform.getLocation());
			if (homeDir.exists() && homeDir.isDirectory()) {
				File file = new File(homeDir, "startup.jar"); //$NON-NLS-1$
				if (file.exists())
					appendAbsolutePath(buffer, file);

				file = new File(homeDir, "libXm.so.2"); //$NON-NLS-1$
				if (file.exists()) {
					appendAbsolutePath(buffer, file);
				}
			}
		}

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

	protected HashMap createAntBuildProperties(String[][] configs) {
		HashMap properties = super.createAntBuildProperties(configs);
		properties.put(IXMLConstants.PROPERTY_LAUNCHER_NAME, getLauncherName());

		ILauncherInfo info = fProduct.getLauncherInfo();
		if (info != null) {
			String icons = ""; //$NON-NLS-1$
			for (int i = 0; i < configs.length; i++) {
				String images = null;
				if (configs[i][0].equals("win32")) { //$NON-NLS-1$
					images = getWin32Images(info);
				} else if (configs[i][0].equals("solaris")) { //$NON-NLS-1$
					images = getSolarisImages(info);
				} else if (configs[i][0].equals("linux")) { //$NON-NLS-1$
					images = getExpandedPath(info.getIconPath(ILauncherInfo.LINUX_ICON));
				} else if (configs[i][0].equals("macosx")) { //$NON-NLS-1$
					images = getExpandedPath(info.getIconPath(ILauncherInfo.MACOSX_ICON));
				}
				if (images != null) {
					if (icons.length() > 0)
						icons += ","; //$NON-NLS-1$
					icons += images;
				}

			}
			if (icons != null && icons.length() > 0)
				properties.put(IXMLConstants.PROPERTY_LAUNCHER_ICONS, icons);
		}

		fAntBuildProperties.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, fRoot);
		fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_PREFIX, fRoot);

		return properties;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.exports.FeatureExportOperation#setP2MetaDataProperties(java.util.Map)
	 */
	protected void setP2MetaDataProperties(Map map) {
		if (fInfo.exportMetadata) {
			if (PDECore.getDefault().getFeatureModelManager().getDeltaPackFeature() == null)
				map.put(IXMLConstants.PROPERTY_LAUNCHER_PROVIDER, "org.eclipse.pde.container.feature"); //$NON-NLS-1$
			map.put(IXMLConstants.TARGET_P2_METADATA, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_FLAVOR, P2Utils.P2_FLAVOR_DEFAULT);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_PUBLISH_ARTIFACTS, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_COMPRESS, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_GATHERING, Boolean.toString(publishingP2Metadata()));
			try {
				map.put(IBuildPropertiesConstants.PROPERTY_P2_BUILD_REPO, new File(fBuildTempMetadataLocation).toURL().toString());
				map.put(IBuildPropertiesConstants.PROPERTY_P2_METADATA_REPO, new File(fInfo.destinationDirectory + "/repository").toURL().toString()); //$NON-NLS-1$
				map.put(IBuildPropertiesConstants.PROPERTY_P2_ARTIFACT_REPO, new File(fInfo.destinationDirectory + "/repository").toURL().toString()); //$NON-NLS-1$
				map.put(IBuildPropertiesConstants.PROPERTY_P2_METADATA_REPO_NAME, NLS.bind(PDECoreMessages.ProductExportOperation_0, fProduct.getProductId()));
				map.put(IBuildPropertiesConstants.PROPERTY_P2_ARTIFACT_REPO_NAME, NLS.bind(PDECoreMessages.ProductExportOperation_0, fProduct.getProductId()));
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
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_256_HIGH));
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

	protected void setupGenerator(BuildScriptGenerator generator, String featureID, String versionId, String[][] configs, String featureLocation) throws CoreException {
		super.setupGenerator(generator, featureID, versionId, configs, featureLocation);
		generator.setGenerateVersionsList(true);
		if (fProduct != null)
			generator.setProduct(fProduct.getModel().getInstallLocation());
	}

	private String createMacInfoPList() {
		String entryName = TargetPlatformHelper.getTargetVersion() >= 3.3 ? "macosx/Info.plist" //$NON-NLS-1$
				: "macosx/Info.plist.32"; //$NON-NLS-1$
		URL url = PDECore.getDefault().getBundle().getEntry(entryName);
		if (url == null)
			return null;

		File plist = null;
		InputStream in = null;
		String location = fFeatureLocation;

		try {
			in = url.openStream();
			File dir = new File(location, ECLIPSE_APP_CONTENTS);
			dir.mkdirs();
			plist = new File(dir, "Info.plist"); //$NON-NLS-1$
			CoreUtility.readFile(in, plist);
			return plist.getAbsolutePath();
		} catch (IOException e) {
			// nothing to do
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
		}
		return null;
	}

	protected void setAdditionalAttributes(Element plugin, BundleDescription bundle) {
		plugin.setAttribute("unpack", Boolean.toString(CoreUtility.guessUnpack(bundle))); //$NON-NLS-1$
	}
}
