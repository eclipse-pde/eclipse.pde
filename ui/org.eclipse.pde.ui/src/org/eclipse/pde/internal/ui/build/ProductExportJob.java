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
			createEclipseProductFile();
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
	
	private File getCustomIniFile() {
		IConfigurationFileInfo info = fProduct.getConfigurationFileInfo();
		if (info != null  && info.getUse().equals("custom")) {
			String path = getExpandedPath(info.getPath());
			if (path != null) {
				File file = new File(path);
				if (file.exists() && file.isFile())
					return file;
			}
		}
		return null;
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
		properties.put(IBuildPropertiesConstants.ROOT, getRootFileLocations()); //To copy a folder
		save(new File(file, "build.properties"), properties, "Build Configuration");
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
				if (files[i].isFile() && !".eclipseproduct".equals(files[i].getName())) {
					buffer.append("absolute:file:");
					buffer.append(files[i].getAbsolutePath());
					buffer.append(",");
				}	
			}		
		}
		// add config.ini
		buffer.append("absolute:file:");
		buffer.append(fFeatureLocation);
		buffer.append("/configuration/config.ini");
		buffer.append(",");

		// add .eclipseproduct
		buffer.append("absolute:file:");
		buffer.append(fFeatureLocation);
		buffer.append("/.eclipseproduct");

		return buffer.toString();
	}
	
	private void createEclipseProductFile() {
		Properties properties = new Properties();
		properties.put("name", fProduct.getName());
		properties.put("id", fProduct.getId());		
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(getBrandingPlugin());
		if (model != null)
			properties.put("version", model.getPluginBase().getVersion());
		save(new File(fFeatureLocation, ".eclipseproduct"), properties, "Eclipse Product File");
	}
	
	private void createConfigIniFile() {
		File file = new File(fFeatureLocation, "configuration");
		if (!file.exists())
			file.mkdirs();

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
		}
		save(new File(file, "config.ini"), properties, "Eclipse Runtime Configuration File");
	}
	
	private String getSplashLocation() {
		ISplashInfo info = fProduct.getSplashInfo();
		String location = null;
		if (info != null) {
			location = info.getLocation();
		}
		if (location == null)
			location = getBrandingPlugin();
		
		return location == null ? null : "platform:/base/plugins/" + location;
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
		
		//TODO branding is off.  If turned on, we get troubles while running the scripts.
		AbstractScriptGenerator.setBrandExecutable(false);
		
		//Just to make sure, Here the values that are put in properties must be passed to the script.
		if (info != null) {
			String name = info.getLauncherName();
			if (name != null && name.length() > 0)
				properties.put(IXMLConstants.PROPERTY_LAUNCHER_NAME, name);
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
				properties.put(IXMLConstants.PROPERTY_LAUNCHER_ICONS, images);
		}
		
		fAntBuildProperties.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, "eclipse"); //$NON-NLS-1$  This value and the next one can be set to the product name if the user desires it
		fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_PREFIX, "eclipse");	// or it can be more than one segment ( like bar/eclipse ) 
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
