package org.eclipse.pde.internal.ui.build;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.exports.*;

public class ProductExportJob extends FeatureExportJob {

	private IProduct fProduct;

	private String fZipExtension = Platform.getOS().equals("macosx") ? ".tar.gz" : ".zip"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$	

	private String fFeatureLocation;

	public ProductExportJob(String name, IProductModel model) {
		super(name);
		fProduct = model.getProduct();
		initializeValues();
	}

	private void initializeValues() {
		if (fProduct.useFeatures()) {
			fItems = getFeatureModels();
		} else {
			fItems = getPluginModels();
		}
		computeDestination();

		fExportType = EXPORT_AS_ZIP;
		fExportSource = fProduct.includeSource();
	}

	private void computeDestination() {
		String location = fProduct.getExportDestination().trim();
		if (!location.endsWith(fZipExtension))
			location += fZipExtension;
		IPath path = new Path(location);

		fZipFilename = path.lastSegment();
		fDestinationDirectory = path.removeLastSegments(1).toOSString();
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
			doExport(featureID, null, fFeatureLocation, TargetPlatform.getOS(),
					TargetPlatform.getWS(), TargetPlatform.getOSArch(), monitor);
		} catch (IOException e) {
		} finally {
			for (int i = 0; i < fItems.length; i++) {
				if (fItems[i] instanceof IPluginModelBase)
					deleteBuildFiles((IPluginModelBase) fItems[i]);
			}
			cleanup(new SubProgressMonitor(monitor, 1));
			monitor.done();
		}
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
		for (int i = 0; i < fItems.length; i++) {
			if (fItems[i] instanceof IPluginModelBase) {
				IPluginBase plugin = ((IPluginModelBase) fItems[i])
						.getPluginBase();
				writer.println("<plugin id=\"" + plugin.getId() + "\" version=\"0.0.0\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
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
		//TODO I am not sure what keys to use here
		// The idea is to include to list in this build.properties all the files that will be
		// included at the root such as the config.ini file, startup.jar, etc.
		// Note that since we are building against the os-ws-arch combination of the target platform,
		// no rcp delta pack should be required.  We should just copy JARs and other root files
		// from the target into the zip.
		save(new File(file, "build.properties"), properties, "Build Configuration");
	}
	
	private void createConfigIniFile() {
		Properties properties = new Properties();
		properties.put("osgi.framework", "platform:/base/plugins/org.eclipse.osgi");
		String location = getSplashLocation();
		if (location != null)
			properties.put("osgi.splashPath", location);
		properties.put("eclipse.product", fProduct.getId());
		if (fProduct.useFeatures()) {
			properties.put("osgi.bundles", "org.eclipse.core.runtime@2,org.eclipse.update.configurator@3");
		} else {
			properties.put("osgi.bundles", getPluginList());
		}
		properties.setProperty("osgi.bundles.defaultStartLevel", "4");
		
		File file = new File(fFeatureLocation, "configuration");
		if (!file.exists())
			file.mkdirs();
		save(new File(file, "config.ini"), properties, "Eclipse Runtime Configuration File");
	}
	
	private String getSplashLocation() {
		ISplashInfo info = fProduct.getSplashInfo();
		if (info != null) {
			String location = info.getLocation();
			if (location != null && location.length() > 0)
				return location;
		}
		
		int dot = fProduct.getId().lastIndexOf('.');
		return (dot != -1) ? fProduct.getId().substring(0, dot) : null;
	}
	
	private String getPluginList() {
		StringBuffer buffer = new StringBuffer();
		IProductPlugin[] plugins = fProduct.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			String id = plugins[i].getId();
			if ("org.eclipse.osgi".equals(id))
				continue;
			if (buffer.length() > 0)
				buffer.append(",");
			buffer.append(id);
			if ("org.eclipse.core.runtime".equals(id))
				buffer.append("@2");
		}
		return buffer.toString();
	}
	
	protected HashMap createAntBuildProperties(String os, String ws, String arch) {
		HashMap properties = super.createAntBuildProperties(os, ws, arch);
		ILauncherInfo info = fProduct.getLauncherInfo();
		
		//TODO Here I am loading the images and splash name as per Jeff's instructions
		// Not sure what to do with them otherwise or how to tell the build script generator
		// to generate the branding task.
		if (info != null) {
			String name = info.getLauncherName();
			if (name != null && name.length() > 0)
				properties.put("launcher.name", name);
			String images = null;
			if (os.equals("win32")) {
				images = getWin32Images(info);
			} else if (os.equals("solaris")) {
				images = getSolarisImages(info);
			} else if (os.equals("linux")) {
				images = getExpandedPath(info.getIconPath(ILauncherInfo.LINUX_ICON));
			} else if (os.equals("macosx")) {
				images = getExpandedPath(info.getIconPath(ILauncherInfo.MACOSX_ICON));
			}
			if (images != null && images.length() > 0)
				properties.put("launcher.icons", images);
		}
		
		//TODO  As opposed to the feature/plugin export, we want the path in the zip file
		// generated to start with eclipse/...
		// Pascal, make sure these are the correct properties to be overridden.
		fAntBuildProperties.put(IXMLConstants.PROPERTY_BUILD_LABEL, "eclipse"); //$NON-NLS-1$
		fAntBuildProperties.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, "eclipse"); //$NON-NLS-1$
		fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_PREFIX, "eclipse");	
		return properties;
	}
	
	private String getWin32Images(ILauncherInfo info) {
		StringBuffer buffer = new StringBuffer();
		append(buffer, info.getIconPath(ILauncherInfo.WIN32_16_HIGH));
		append(buffer, info.getIconPath(ILauncherInfo.WIN32_16_LOW));
		append(buffer, info.getIconPath(ILauncherInfo.WIN32_32_HIGH));
		append(buffer, info.getIconPath(ILauncherInfo.WIN32_32_LOW));
		append(buffer, info.getIconPath(ILauncherInfo.WIN32_48_HIGH));
		append(buffer, info.getIconPath(ILauncherInfo.WIN32_48_LOW));
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
				buffer.append(",");
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
