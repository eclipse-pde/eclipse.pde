/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 477527
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.build.BuildScriptGenerator;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IJREInfo;
import org.eclipse.pde.internal.core.iproduct.ILauncherInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.product.JREInfo;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class ProductExportOperation extends FeatureExportOperation {
	private static final String STATUS_MESSAGE = "!MESSAGE"; //$NON-NLS-1$
	private static final String STATUS_ENTRY = "!ENTRY"; //$NON-NLS-1$
	private static final String STATUS_SUBENTRY = "!SUBENTRY"; //$NON-NLS-1$
	private static final String ECLIPSE_APP_MACOS = "Eclipse.app/Contents/MacOS"; //$NON-NLS-1$
	private static final String ECLIPSE_APP_CONTENTS = "Eclipse.app/Contents"; //$NON-NLS-1$
	private static final String MAC_JAVA_FRAMEWORK = "/System/Library/Frameworks/JavaVM.framework"; //$NON-NLS-1$
	private String fFeatureLocation;
	private final String fRoot;
	private final IProduct fProduct;

	protected static String errorMessage;

	public static void setErrorMessage(String message) {
		errorMessage = message;
	}

	public static String getErrorMessage() {
		return errorMessage;
	}

	public static IStatus parseErrorMessage(CoreException e) {
		if (errorMessage == null) {
			return null;
		}

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
					status.add(Status.error(next.substring(8)));
				}
			}
		}
		if (status != null) {
			return status;
		}

		//parsing didn't work, just set the message
		return new MultiStatus(PDECore.PLUGIN_ID, 0, new IStatus[] {e.getStatus()}, errorMessage, null);
	}

	public ProductExportOperation(FeatureExportInfo info, String name, IProduct product, String root) {
		super(info, name);
		fProduct = product;
		fRoot = root;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		String[][] configurations = fInfo.targets;
		if (configurations == null) {
			configurations = new String[][] {{TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), TargetPlatform.getNL()}};
		}

		cleanupBuildRepo();
		errorMessage = null;
		SubMonitor subMonitor = SubMonitor.convert(monitor, 9);

		try {
			// create a feature to wrap all plug-ins and features
			String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
			fFeatureLocation = fBuildTempLocation + File.separator + featureID;

			createFeature(featureID, fFeatureLocation, configurations, fProduct.includeLaunchers());
			createBuildPropertiesFile(fFeatureLocation, configurations);
			doExport(featureID, null, fFeatureLocation, configurations, subMonitor.split(8));
		} catch (IOException e) {
			PDECore.log(e);
		} catch (CoreException e) {
			if (errorMessage != null) {
				return parseErrorMessage(e);
			}
			return e.getStatus();
		} finally {
			// Clean up generated files
			for (Object item : fInfo.items) {
				try {
					deleteBuildFiles(item);
				} catch (CoreException e) {
					PDECore.log(e);
				}
			}
			cleanup(subMonitor.split(1));
		}

		if (hasAntErrors()) {
			return Status.warning(NLS.bind(PDECoreMessages.FeatureExportOperation_CompilationErrors, fInfo.destinationDirectory));
		}

		errorMessage = null;
		return Status.OK_STATUS;
	}

	@Override
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

	@Override
	protected String[] getPaths() {
		String[] paths = super.getPaths();
		String[] all = new String[paths.length + 1];
		all[0] = fFeatureLocation + File.separator + ICoreConstants.FEATURE_FILENAME_DESCRIPTOR;
		System.arraycopy(paths, 0, all, 1, paths.length);
		return all;
	}

	private void createBuildPropertiesFile(String featureLocation, String[][] configurations) {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory()) {
			file.mkdirs();
		}

		boolean hasLaunchers = PDECore.getDefault().getFeatureModelManager().getDeltaPackFeature() != null;
		Properties properties = new Properties();
		if (fProduct.includeLaunchers() && !hasLaunchers && configurations.length > 0) {
			String rootPrefix = IBuildPropertiesConstants.ROOT_PREFIX + configurations[0][0] + "." + configurations[0][1] + "." + configurations[0][2]; //$NON-NLS-1$ //$NON-NLS-2$
			properties.put(rootPrefix, getRootFileLocations(hasLaunchers));
			if (TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$
				String plist = createMacInfoPList();
				if (plist != null) {
					properties.put(rootPrefix + ".folder." + ECLIPSE_APP_CONTENTS, "absolute:file:" + plist); //$NON-NLS-1$ //$NON-NLS-2$
				}
				properties.put(rootPrefix + ".folder." + ECLIPSE_APP_MACOS, getLauncherLocations(hasLaunchers)); //$NON-NLS-1$
				properties.put(rootPrefix + ".permissions.755", "Contents/MacOS/" + getLauncherName()); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				properties.put(rootPrefix, getLauncherLocations(hasLaunchers)); //To copy a folder
				properties.put(rootPrefix + ".permissions.755", getLauncherName()); //$NON-NLS-1$
				if (TargetPlatform.getWS().equals("motif") && TargetPlatform.getOS().equals("linux")) { //$NON-NLS-1$ //$NON-NLS-2$
					properties.put(rootPrefix + ".permissions.755", "libXm.so.2"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		IJREInfo jreInfo = getJreInfo();
		if (jreInfo != null) {
			for (String[] configuration : configurations) {
				String[] config = configuration;

				// Only include the JRE if product config says to do so
				if (!jreInfo.includeJREWithProduct(config[0])) {
					continue;
				}

				File vm = jreInfo.getJVMLocation(config[0]);
				if (vm != null) {

					if (config[0].equals("macosx") && vm.getPath().startsWith(MAC_JAVA_FRAMEWORK)) { //$NON-NLS-1$
						continue;
					}

					String rootPrefix = IBuildPropertiesConstants.ROOT_PREFIX + config[0] + "." + config[1] + //$NON-NLS-1$
							"." + config[2]; //$NON-NLS-1$
					properties.put(rootPrefix + ".folder.jre", "absolute:" + vm.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
					String perms = (String) properties.get(rootPrefix + ".permissions.755"); //$NON-NLS-1$
					if (perms != null) {
						StringBuilder buffer = new StringBuilder(perms);
						buffer.append(","); //$NON-NLS-1$
						buffer.append("jre/bin/java"); //$NON-NLS-1$
						properties.put(rootPrefix + ".permissions.755", buffer.toString()); //$NON-NLS-1$
					}
				}
			}
		}

		if (fInfo.exportSource && fInfo.exportSourceBundle) {
			properties.put(IBuildPropertiesConstants.PROPERTY_INDIVIDUAL_SOURCE, "true"); //$NON-NLS-1$
			for (Object item : fInfo.items) {
				if (item instanceof IFeatureModel) {
					IFeature feature = ((IFeatureModel) item).getFeature();
					properties.put("generate.feature@" + feature.getId().trim() + ".source", feature.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					BundleDescription bundle = null;
					if (item instanceof IPluginModelBase) {
						bundle = ((IPluginModelBase) item).getBundleDescription();
					}
					if (bundle == null && item instanceof BundleDescription) {
						bundle = (BundleDescription) item;
					}
					if (bundle == null) {
						continue;
					}

					//it doesn't matter if we generate extra properties for platforms we aren't exporting for
					if (FeatureExportOperation.isWorkspacePlugin(bundle)) {
						properties.put("generate.plugin@" + bundle.getSymbolicName().trim() + ".source", bundle.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
		save(new File(file, ICoreConstants.BUILD_FILENAME_DESCRIPTOR), properties, "Build Configuration"); //$NON-NLS-1$
	}

	private IJREInfo getJreInfo() {
		if (fProduct.includeJre()) {
			return new JREInfo(fProduct.getModel()) {

				private static final long serialVersionUID = 1L;

				@Override
				public boolean includeJREWithProduct(String os) {
					// always true for all os
					return true;
				}

				@Override
				public File getJVMLocation(String os) {
					// always the default vm install, that is the one from the
					// target definition
					return JavaRuntime.getDefaultVMInstall().getInstallLocation();
				}

			};
		}
		return fProduct.getJREInfo();
	}

	@Override
	protected boolean publishingP2Metadata() {
		return fInfo.exportMetadata;
	}

	private String getLauncherLocations(boolean hasLaunchers) {
		//get the launchers for the eclipse install
		StringBuilder buffer = new StringBuilder();
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
					appendEclipsePath(buffer, new File(homeDir, "../MacOS/")); //$NON-NLS-1$
				} else {
					appendEclipsePath(buffer, homeDir);
				}
			}
		}
		return buffer.toString();
	}

	private String getRootFileLocations(boolean hasLaunchers) {
		//Get the files that go in the root of the eclipse install, excluding the launcher
		StringBuilder buffer = new StringBuilder();
		if (!hasLaunchers) {
			File homeDir = new File(TargetPlatform.getLocation());
			if (homeDir.exists() && homeDir.isDirectory()) {
				File file = new File(homeDir, "startup.jar"); //$NON-NLS-1$
				if (file.exists()) {
					appendAbsolutePath(buffer, file);
				}

				file = new File(homeDir, "libXm.so.2"); //$NON-NLS-1$
				if (file.exists()) {
					appendAbsolutePath(buffer, file);
				}
			}
		}

		return buffer.toString();
	}

	private void appendEclipsePath(StringBuilder buffer, File homeDir) {
		File file = new File(homeDir, "eclipse"); //$NON-NLS-1$
		if (file.exists()) {
			appendAbsolutePath(buffer, file);
		}
		file = new File(homeDir, "eclipse.exe"); //$NON-NLS-1$
		if (file.exists()) {
			appendAbsolutePath(buffer, file);
		}
	}

	private void appendAbsolutePath(StringBuilder buffer, File file) {
		if (buffer.length() > 0) {
			buffer.append(","); //$NON-NLS-1$
		}

		buffer.append("absolute:file:"); //$NON-NLS-1$
		buffer.append(file.getAbsolutePath());
	}

	@Override
	protected HashMap<String, String> createAntBuildProperties(String[][] configs) {
		HashMap<String, String> properties = super.createAntBuildProperties(configs);
		properties.put(IXMLConstants.PROPERTY_LAUNCHER_NAME, getLauncherName());

		ILauncherInfo info = fProduct.getLauncherInfo();
		if (info != null) {
			String icons = ""; //$NON-NLS-1$
			for (String[] config : configs) {
				String images = null;
				switch (config[0]) {
					case "win32": //$NON-NLS-1$
					images = getWin32Images(info);
					break;
				case "freebsd": //$NON-NLS-1$
					images = getExpandedPath(info.getIconPath(ILauncherInfo.FREEBSD_ICON));
					break;
				case "linux": //$NON-NLS-1$
					images = getExpandedPath(info.getIconPath(ILauncherInfo.LINUX_ICON));
					break;
				case "macosx": //$NON-NLS-1$
					images = getExpandedPath(info.getIconPath(ILauncherInfo.MACOSX_ICON));
					break;
				default:
					break;
				}
				if (images != null) {
					if (icons.length() > 0) {
						icons += ","; //$NON-NLS-1$
					}
					icons += images;
				}

			}
			if (icons.length() > 0) {
				properties.put(IXMLConstants.PROPERTY_LAUNCHER_ICONS, icons);
			}
		}

		fAntBuildProperties.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, fRoot);
		fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_PREFIX, fRoot);

		return properties;
	}

	@Override
	protected void setP2MetaDataProperties(Map<String, String> map) {
		if (fInfo.exportMetadata) {
			if (PDECore.getDefault().getFeatureModelManager().getDeltaPackFeature() == null) {
				map.put(IXMLConstants.PROPERTY_LAUNCHER_PROVIDER, "org.eclipse.pde.container.feature"); //$NON-NLS-1$
			}
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
				if (name.endsWith(".exe")) { //$NON-NLS-1$
					name = name.substring(0, name.length() - 4);
				}
				return name;
			}
		}
		return "eclipse"; //$NON-NLS-1$
	}

	private String getWin32Images(ILauncherInfo info) {
		StringBuilder buffer = new StringBuilder();
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

	private void append(StringBuilder buffer, String path) {
		path = getExpandedPath(path);
		if (path != null) {
			if (buffer.length() > 0) {
				buffer.append(","); //$NON-NLS-1$
			}
			buffer.append(path);
		}
	}

	private String getExpandedPath(String path) {
		if (path == null || path.length() == 0) {
			return null;
		}
		IResource resource = PDECore.getWorkspace().getRoot().findMember(IPath.fromOSString(path));
		if (resource != null) {
			IPath fullPath = resource.getLocation();
			return fullPath == null ? null : fullPath.toOSString();
		}
		return null;
	}

	@Override
	protected void setupGenerator(BuildScriptGenerator generator, String featureID, String versionId, String[][] configs, String featureLocation) throws CoreException {
		super.setupGenerator(generator, featureID, versionId, configs, featureLocation);
		generator.setGenerateVersionsList(true);
		if (fProduct != null) {
			generator.setProduct(fProduct.getModel().getInstallLocation());
		}
	}

	private String createMacInfoPList() {
		String entryName = TargetPlatformHelper.getTargetVersion() >= 3.3 ? "macosx/Info.plist" //$NON-NLS-1$
				: "macosx/Info.plist.32"; //$NON-NLS-1$
		URL url = PDECore.getDefault().getBundle().getEntry(entryName);
		if (url == null) {
			return null;
		}

		File plist = null;
		String location = fFeatureLocation;

		try (InputStream in = url.openStream()) {
			File dir = new File(location, ECLIPSE_APP_CONTENTS);
			dir.mkdirs();
			plist = new File(dir, "Info.plist"); //$NON-NLS-1$
			CoreUtility.readFile(in, plist);
			return plist.getAbsolutePath();
		} catch (IOException e) {
			// nothing to do
		} finally {
		}
		return null;
	}
}
