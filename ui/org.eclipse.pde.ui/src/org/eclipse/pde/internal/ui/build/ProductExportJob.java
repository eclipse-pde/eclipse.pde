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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.BuildScriptGenerator;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.iproduct.IArgumentsInfo;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.ILauncherInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.ISplashInfo;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

public class ProductExportJob extends FeatureExportJob {
	
	private String fFeatureLocation;

	private String fRoot;
	
	private IProduct fProduct;

	public ProductExportJob(FeatureExportInfo info, IProductModel model, String productRoot) {
		super(info);
		fProduct = model.getProduct();
		fRoot = productRoot;
	}


	protected void doExports(IProgressMonitor monitor)
			throws InvocationTargetException, CoreException {
		String[][] configurations = fInfo.targets;
		if (configurations == null)
			configurations = new String[][] { {TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), TargetPlatform.getNL() } };
        monitor.beginTask("", 10 * configurations.length); //$NON-NLS-1$
		for (int i = 0; i < configurations.length; i++) {
			try {
				String[] config = configurations[i];
				// create a feature to wrap all plug-ins and features
				String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
				fFeatureLocation = fBuildTempLocation + File.separator + featureID;
				createFeature(featureID, fFeatureLocation, config, true);
				createBuildPropertiesFile(fFeatureLocation);
				createConfigIniFile(config);
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
				for (int j = 0; j < fInfo.items.length; j++) {
					deleteBuildFiles(fInfo.items[j]);
				}
				cleanup(fInfo.targets == null ? null : configurations[i], new SubProgressMonitor(monitor, 3));
			}
		}
		monitor.done();
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
		
		boolean hasLaunchers = PDECore.getDefault().getFeatureModelManager().findFeatureModel("org.eclipse.platform.launchers") != null; //$NON-NLS-1$
		Properties properties = new Properties();
		properties.put(IBuildPropertiesConstants.ROOT, getRootFileLocations(hasLaunchers)); //To copy a folder
		if (!hasLaunchers) {
			properties.put("root.permissions.755", getLauncherName()); //$NON-NLS-1$
			if (TargetPlatform.getWS().equals("motif") && TargetPlatform.getOS().equals("linux")) { //$NON-NLS-1$ //$NON-NLS-2$
				properties.put("root.linux.motif.x86.permissions.755", "libXm.so.2"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		save(new File(file, "build.properties"), properties, "Build Configuration"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private String getRootFileLocations(boolean hasLaunchers) {
		StringBuffer buffer = new StringBuffer();

		File homeDir = ExternalModelManager.getEclipseHome().toFile();
		if (!hasLaunchers) {
			if (homeDir.exists() && homeDir.isDirectory()) {
				buffer.append("absolute:file:"); //$NON-NLS-1$
				buffer.append(new File(homeDir, "startup.jar").getAbsolutePath()); //$NON-NLS-1$
				File file = new File(homeDir, "eclipse"); //$NON-NLS-1$
				if (file.exists()) {
					buffer.append(",absolute:file:"); //$NON-NLS-1$
					buffer.append(file.getAbsolutePath()); //$NON-NLS-1$
				}				
				file = new File(homeDir, "eclipse.exe"); //$NON-NLS-1$
				if (file.exists()) {
					buffer.append(",absolute:file:"); //$NON-NLS-1$
					buffer.append(file.getAbsolutePath()); //$NON-NLS-1$
				}
				file = new File(homeDir, "libXm.so.2"); //$NON-NLS-1$
				if (file.exists()) {
					buffer.append(",absolute:file:"); //$NON-NLS-1$
					buffer.append(file.getAbsolutePath()); //$NON-NLS-1$
				}
			}	
		}
		// add content of temp folder (.eclipseproduct, configuration/config.ini)
		buffer.append(",/temp/"); //$NON-NLS-1$
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
	
	
	private void createConfigIniFile(String[] config) {
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
            String location = getSplashLocation(config[0], config[1], config[2]);
            writer.println("#Product Runtime Configuration File"); //$NON-NLS-1$
            writer.println();
            if (location != null)
            	writer.println("osgi.splashPath=" + location); //$NON-NLS-1$
            writer.println("eclipse.product=" + fProduct.getId()); //$NON-NLS-1$
            if (fProduct.useFeatures() || fProduct.containsPlugin("org.eclipse.update.configurator")) { //$NON-NLS-1$
                writer.println("osgi.bundles=" +  "org.eclipse.core.runtime@2:start,org.eclipse.update.configurator@3:start"); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                writer.println("osgi.bundles=" + getPluginList(config)); //$NON-NLS-1$
            }
            writer.println("osgi.bundles.defaultStartLevel=4"); //$NON-NLS-1$ //$NON-NLS-2$		
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
		if (plugin == null)
			plugin = getBrandingPlugin();
		
		if (plugin == null)
			return null;
		
		StringBuffer buffer = new StringBuffer("platform:/base/plugins/");
		buffer.append(plugin);
		
		State state = getState(os, ws, arch);
		BundleDescription bundle = state.getBundle(plugin, null);
		if (bundle != null) {
			BundleDescription[] fragments = bundle.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				String id = fragments[i].getSymbolicName();
				if (fProduct.containsPlugin(id)) {
					buffer.append(",platform:/base/plugins/");
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
	
	private String getPluginList(String[] config) {
		StringBuffer buffer = new StringBuffer();
		
        Dictionary environment = new Hashtable(4);
        environment.put("osgi.os", config[0]); //$NON-NLS-1$
        environment.put("osgi.ws", config[1]); //$NON-NLS-1$
        environment.put("osgi.arch", config[2]); //$NON-NLS-1$
        environment.put("osgi.nl", config[3]); //$NON-NLS-1$

        BundleContext context = PDEPlugin.getDefault().getBundleContext();

		for (int i = 0; i < fInfo.items.length; i++) {
			BundleDescription bundle = (BundleDescription)fInfo.items[i];
            String filterSpec = bundle.getPlatformFilter();
            try {
				if (filterSpec == null|| context.createFilter(filterSpec).match(environment)) {			
					String id = ((BundleDescription)fInfo.items[i]).getSymbolicName();				
					if ("org.eclipse.osgi".equals(id)) //$NON-NLS-1$
						continue;
					if (buffer.length() > 0)
						buffer.append(","); //$NON-NLS-1$
					buffer.append(id);
					if ("org.eclipse.core.runtime".equals(id)) //$NON-NLS-1$
						buffer.append("@2:start"); //$NON-NLS-1$
				}
			} catch (InvalidSyntaxException e) {
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
