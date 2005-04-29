/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.exports.*;

public class ProductExportJob extends FeatureExportJob {
	
	private String fFeatureLocation;

	private String fRoot;
	
	private IProduct fProduct;

	public ProductExportJob(IProductModel model, String productRoot, boolean toDirectory, boolean exportSource, String destination, String zipFileName, String[][] targets) {
		super(PDEUIMessages.ProductExportJob_jobName); //$NON-NLS-1$
		fProduct = model.getProduct();
		fExportToDirectory = toDirectory;
		fExportSource = exportSource;
		fDestinationDirectory = destination;
		fZipFilename = zipFileName;
		fRoot = productRoot;
		fTargets = targets;
		// TODO remove when there is UI to set ftargets
		if (fTargets == null)
			fTargets = new String[][] { { "linux", "gtk", "x86", ""} , {"win32", "win32", "x86", ""} };
		if (fProduct.useFeatures()) {
			fItems = getFeatureModels();
		} else {
			fItems = getPluginModels();
		}
	}

	private IFeatureModel[] getFeatureModels() {
		ArrayList list = new ArrayList();
		FeatureModelManager manager = PDECore.getDefault()
				.getFeatureModelManager();
		IProductFeature[] features = fProduct.getFeatures();
		for (int i = 0; i < features.length; i++) {
			IFeatureModel model = manager.findFeatureModel(features[i].getId(),
					features[i].getVersion());
			if (model != null)
				list.add(model);
		}
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	private IPluginModelBase[] getPluginModels() {
		ArrayList list = new ArrayList();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IProductPlugin[] plugins = fProduct.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IPluginModelBase model = manager.findModel(plugins[i].getId());
			if (model != null) {
				list.add(model);
			}
		}
		return (IPluginModelBase[]) list.toArray(new IPluginModelBase[list
				.size()]);
	}

	protected void doExports(IProgressMonitor monitor)
			throws InvocationTargetException, CoreException {
		String[][] configurations = fTargets;
		if (configurations == null)
			configurations = new String[][] { {TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), TargetPlatform.getNL() } };
		for (int i = 0; i < configurations.length; i++) {
			try {
				String[] config = configurations[i];
	            monitor.beginTask("", 10);
				// create a feature to wrap all plug-ins and features
				String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
				fFeatureLocation = fBuildTempLocation + File.separator + featureID;
				createFeature(featureID, fFeatureLocation, config, true);
				createBuildPropertiesFile(fFeatureLocation);
				createConfigIniFile();
				createEclipseProductFile();
				createLauncherIniFile();
				doExport(featureID, 
	                        null, 
	                        fFeatureLocation, 
	                        config[0], 
	                        config[1], 
	                        config[2], 
	                        new SubProgressMonitor(monitor, 7));
			} catch (IOException e) {
			} finally {
				for (int j = 0; j < fItems.length; j++) {
					deleteBuildFiles((IModel)fItems[j]);
				}
				cleanup(fTargets == null ? null : configurations[i], new SubProgressMonitor(monitor, 3));
				monitor.done();
			}
		}
	}
	
	private File getCustomIniFile() {
		IConfigurationFileInfo info = fProduct.getConfigurationFileInfo();
		if (info != null  && info.getUse().equals("custom")) { //$NON-NLS-1$
			String path = getExpandedPath(info.getPath());
			if (path != null) {
				File file = new File(path);
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

	private void createBuildPropertiesFile(String featureLocation) {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory())
			file.mkdirs();
		boolean hasLaunchers = false;
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		for (int i = 0; i < models.length; i++) {
			if ("org.eclipse.platform.launchers".equals(models[i].getFeature().getId()))
				hasLaunchers = true;
		}

		Properties properties = new Properties();
		properties.put(IBuildPropertiesConstants.ROOT, getRootFileLocations(hasLaunchers)); //To copy a folder
		if (!hasLaunchers) {
			properties.put("root.permissions.755", getLauncherName()); //$NON-NLS-1$
			if (TargetPlatform.getOS().equals("linux")) { //$NON-NLS-1$
				properties.put("root.linux.motif.x86.link", "libXm.so.2.1,libXm.so.2,libXm.so.2.1,libXm.so"); //$NON-NLS-1$ //$NON-NLS-2$
				properties.put("root.linux.motif.x86.permissions.755", "*.so*"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		save(new File(file, "build.properties"), properties, "Build Configuration"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private String getRootFileLocations(boolean hasLaunchers) {
		StringBuffer buffer = new StringBuffer();

		if (!hasLaunchers) {
			File homeDir = ExternalModelManager.getEclipseHome().toFile();
			if (homeDir.exists() && homeDir.isDirectory()) {
				buffer.append("absolute:file:"); //$NON-NLS-1$
				buffer.append(new File(homeDir, "eclipse").getAbsolutePath());
				buffer.append(","); //$NON-NLS-1$
				buffer.append(new File(homeDir, "eclipse.exe").getAbsolutePath());
				buffer.append(","); //$NON-NLS-1$
				buffer.append(new File(homeDir, "startup.jar").getAbsolutePath());
				buffer.append(","); //$NON-NLS-1$
			}	
		}
		// add content of temp folder (.eclipseproduct, configuration/config.ini)
		buffer.append("/temp/"); //$NON-NLS-1$
		return buffer.toString();
	}
	
	private void createEclipseProductFile() {
		File dir = new File(fFeatureLocation, "temp"); //$NON-NLS-1$
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();
		Properties properties = new Properties();
		properties.put("name", fProduct.getName()); //$NON-NLS-1$
		properties.put("id", fProduct.getId());		 //$NON-NLS-1$
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(getBrandingPlugin());
		if (model != null)
			properties.put("version", model.getPluginBase().getVersion()); //$NON-NLS-1$
		save(new File(dir, ".eclipseproduct"), properties, "Eclipse Product File"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private void createLauncherIniFile() {
		String programArgs = getProgramArguments();
		String vmArgs = getVMArguments();
		
		if (programArgs.length() == 0 && vmArgs.length() == 0)
			return;
		
		File dir = new File(fFeatureLocation, "temp"); //$NON-NLS-1$
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();
		
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(dir, getLauncherName() + ".ini"))); //$NON-NLS-1$
			if (programArgs.length() > 0) {
				StringTokenizer tokenizer = new StringTokenizer(programArgs);
				while (tokenizer.hasMoreTokens())
					writer.println(tokenizer.nextToken());
			}
			if (vmArgs.length() > 0) {
				writer.println("-vmargs"); //$NON-NLS-1$
				StringTokenizer tokenizer = new StringTokenizer(vmArgs);
				while (tokenizer.hasMoreTokens())
					writer.println(tokenizer.nextToken());
			}	
		} catch (IOException e) {
		} finally {
			if (writer != null) {
				writer.close();
			}			
		}
	}

	private String getProgramArguments() {
		IArgumentsInfo info = fProduct.getLauncherArguments();
		return (info != null) ? CoreUtility.normalize(info.getProgramArguments()) : ""; //$NON-NLS-1$
	}
	
	private String getVMArguments() {
		IArgumentsInfo info = fProduct.getLauncherArguments();
		return (info != null) ? CoreUtility.normalize(info.getVMArguments()) : ""; //$NON-NLS-1$
	}	
	
	
	private void createConfigIniFile() {
		File dir = new File(fFeatureLocation, "temp/configuration"); //$NON-NLS-1$
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();

        PrintWriter writer = null;

        File custom = getCustomIniFile();       
		if (custom != null) {
			String path = getExpandedPath(fProduct.getConfigurationFileInfo().getPath());
			BufferedReader in = null;
			try {
                in = new BufferedReader(new FileReader(path));
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
            String location = getSplashLocation();
            writer.println("#Product Runtime Configuration File"); //$NON-NLS-1$
            writer.println();
            if (location != null)
            	writer.println("osgi.splashPath=" + location); //$NON-NLS-1$
            writer.println("eclipse.product=" + fProduct.getId()); //$NON-NLS-1$
            if (fProduct.useFeatures()) {
                writer.println("osgi.bundles=" +  "org.eclipse.core.runtime@2:start,org.eclipse.update.configurator@3:start"); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                writer.println("osgi.bundles=" + getPluginList()); //$NON-NLS-1$
            }
            writer.println("osgi.bundles.defaultStartLevel=4"); //$NON-NLS-1$ //$NON-NLS-2$		
        } catch (IOException e) {
        } finally {
            if (writer != null)
                writer.close();
        }
	}
	
	private String getSplashLocation() {
		ISplashInfo info = fProduct.getSplashInfo();
		String location = null;
		if (info != null) {
			location = info.getLocation();
		}
		if (location == null)
			location = getBrandingPlugin();
		
		return location == null ? null : "platform:/base/plugins/" + location; //$NON-NLS-1$
	}
	
	private String getBrandingPlugin() {
		int dot = fProduct.getId().lastIndexOf('.');
		return (dot != -1) ? fProduct.getId().substring(0, dot) : null;
	}
	
	private String getPluginList() {
		StringBuffer buffer = new StringBuffer();
		IProductPlugin[] plugins = fProduct.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			String id = plugins[i].getId();
			if ("org.eclipse.osgi".equals(id)) //$NON-NLS-1$
				continue;
			if (buffer.length() > 0)
				buffer.append(","); //$NON-NLS-1$
			buffer.append(id);
			if ("org.eclipse.core.runtime".equals(id)) //$NON-NLS-1$
				buffer.append("@2:start"); //$NON-NLS-1$
            if ("org.eclipse.update.configurator".equals(id)) //$NON-NLS-1$
                buffer.append("@3:start"); //$NON-NLS-1$
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
		return properties;
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
		return "eclipse";	 //$NON-NLS-1$
	}
	
	private String getWin32Images(ILauncherInfo info) {
		StringBuffer buffer = new StringBuffer();
		if (info.usesWinIcoFile()) {
			append(buffer, info.getIconPath(ILauncherInfo.P_ICO_PATH));
		} else {
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_16_HIGH));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_16_LOW));
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
		IResource resource = PDEPlugin.getWorkspace().getRoot().findMember(new Path(path));
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
		if (fProduct != null)
			generator.setProduct(fProduct.getModel().getInstallLocation());
	}
	
}
