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
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.exports.*;
import org.osgi.framework.*;

public class ProductExportJob extends FeatureExportJob {
	
	private IProduct fProduct;

	private String fFeatureLocation;

	private String fRoot;

	public ProductExportJob(IProductModel model, String productRoot, boolean toDirectory, boolean exportSource, String destination, String zipFileName) {
		super(PDEPlugin.getResourceString("ProductExportJob.jobName")); //$NON-NLS-1$
		fProduct = model.getProduct();
		fExportToDirectory = toDirectory;
		fExportSource = exportSource;
		fUseJarFormat = false;
		fDestinationDirectory = destination;
		fZipFilename = zipFileName;
		fRoot = productRoot;
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
		try {
			// create a feature to contain all plug-ins
			String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
			fFeatureLocation = fBuildTempLocation + File.separator + featureID;
			createFeature(featureID, fFeatureLocation);
			createBuildPropertiesFile(fFeatureLocation);
			createConfigIniFile();
			createEclipseProductFile();
			createLauncherIniFile();
			doExport(featureID, null, fFeatureLocation, TargetPlatform.getOS(),
					TargetPlatform.getWS(), TargetPlatform.getOSArch(), monitor);
		} catch (IOException e) {
		} finally {
			for (int i = 0; i < fItems.length; i++) {
				deleteBuildFiles((IModel)fItems[i]);
			}
			cleanup(new SubProgressMonitor(monitor, 1));
			monitor.done();
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
	
	private Dictionary getTargetEnvironment() {
		Dictionary result = new Hashtable(4);
		result.put ("osgi.os", TargetPlatform.getOS()); //$NON-NLS-1$
		result.put ("osgi.ws", TargetPlatform.getWS()); //$NON-NLS-1$
		result.put ("osgi.nl", TargetPlatform.getNL()); //$NON-NLS-1$
		result.put ("osgi.arch", TargetPlatform.getOSArch()); //$NON-NLS-1$
		return result;
	}

	private void createFeature(String featureID, String featureLocation)
			throws IOException {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory())
			file.mkdirs();
		File featureXML = new File(file, "feature.xml"); //$NON-NLS-1$
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(featureXML), "UTF-8"), true); //$NON-NLS-1$
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		writer.println("<feature id=\"" + featureID + "\" version=\"1.0\">"); //$NON-NLS-1$ //$NON-NLS-2$

		Dictionary environment = getTargetEnvironment();
		BundleContext context = PDEPlugin.getDefault().getBundleContext();
		for (int i = 0; i < fItems.length; i++) {
			if (fItems[i] instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase)fItems[i];
				try {
					String filterSpec = model.getBundleDescription().getPlatformFilter();
					if (filterSpec == null || context.createFilter(filterSpec).match(environment))
						writer.println("<plugin id=\"" + model.getPluginBase().getId() + "\" version=\"0.0.0\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (InvalidSyntaxException e) {
				}				
			} else if (fItems[i] instanceof IFeatureModel) {
				IFeature feature = ((IFeatureModel)fItems[i]).getFeature();
				writer.println("<includes id=\""+ feature.getId() + "\" version=\"" + feature.getVersion() + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		writer.println("</feature>"); //$NON-NLS-1$
		writer.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#getPaths()
	 */
	protected String[] getPaths() throws CoreException {
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

		Properties properties = new Properties();
		properties.put(IBuildPropertiesConstants.ROOT, getRootFileLocations()); //To copy a folder
		properties.put("root.permissions.755", getLauncherName()); //$NON-NLS-1$
		if (TargetPlatform.getOS().equals("linux")) { //$NON-NLS-1$
			properties.put("root.linux.motif.x86.link", "libXm.so.2.1,libXm.so.2,libXm.so.2.1,libXm.so"); //$NON-NLS-1$ //$NON-NLS-2$
			properties.put("root.linux.motif.x86.permissions.755", "*.so*"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		save(new File(file, "build.properties"), properties, "Build Configuration"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private String getRootFileLocations() {
		StringBuffer buffer = new StringBuffer();
		
		File homeDir = ExternalModelManager.getEclipseHome().toFile();
		if (homeDir.exists() && homeDir.isDirectory()) {
			File[] files = homeDir.listFiles();
			for (int i = 0; i < files.length; i++) {
				// TODO for now copy everything except .eclipseproduct
				// Once the branded executable is generated, we should not copy
				// eclipse.exe nor icon.xpm
				if (files[i].isFile() 
						&& !".eclipseproduct".equals(files[i].getName())  //$NON-NLS-1$
						&& !files[i].getName().endsWith(".html")) { //$NON-NLS-1$
					buffer.append("absolute:file:"); //$NON-NLS-1$
					buffer.append(files[i].getAbsolutePath());
					buffer.append(","); //$NON-NLS-1$
				}	
			}		
		}
		// add content of temp folder (.eclipseproduct, configuration/config.ini)
		buffer.append("/temp/"); //$NON-NLS-1$
		buffer.append(","); //$NON-NLS-1$

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

		Properties properties = new Properties();
		File custom = getCustomIniFile();
		if (custom != null) {
			String path = getExpandedPath(fProduct.getConfigurationFileInfo().getPath());
			InputStream stream = null;
			try {
				stream = new FileInputStream(new File(path));
				properties.load(stream);
			} catch (IOException e) {
			} finally {
				try {
					if (stream != null)
						stream.close();
				} catch (IOException e) {
				}
			}
		} else {
			//properties.put("osgi.framework", "platform:/base/plugins/org.eclipse.osgi");
			String location = getSplashLocation();
			if (location != null)
				properties.put("osgi.splashPath", location); //$NON-NLS-1$
			properties.put("eclipse.product", fProduct.getId()); //$NON-NLS-1$
			if (fProduct.useFeatures()) {
				properties.put("osgi.bundles", "org.eclipse.core.runtime@2:start,org.eclipse.update.configurator@3:start"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				properties.put("osgi.bundles", getPluginList()); //$NON-NLS-1$
			}
			properties.setProperty("osgi.bundles.defaultStartLevel", "4"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		save(new File(dir, "config.ini"), properties, "Eclipse Runtime Configuration File"); //$NON-NLS-1$ //$NON-NLS-2$
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
		}
		return buffer.toString();
	}
	
	protected boolean needBranding() {
		return true;
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
	
}
