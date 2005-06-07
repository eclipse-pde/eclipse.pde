/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;

public class LauncherUtils {
	public static IVMInstall[] getAllVMInstances() {
		ArrayList res = new ArrayList();
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (int i = 0; i < types.length; i++) {
			IVMInstall[] installs = types[i].getVMInstalls();
			for (int k = 0; k < installs.length; k++) {
				res.add(installs[k]);
			}
		}
		return (IVMInstall[]) res.toArray(new IVMInstall[res.size()]);
	}
	
	public static String[] getVMInstallNames() {
		IVMInstall[] installs = getAllVMInstances();
		String[] names = new String[installs.length];
		for (int i = 0; i < installs.length; i++) {
			names[i] = installs[i].getName();
		}
		return names;
	}
	
	public static String getDefaultVMInstallName() {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();
		if (install != null)
			return install.getName();
		return null;
	}
	
	public static IVMInstall getVMInstall(String name) {
		if (name != null) {
			IVMInstall[] installs = getAllVMInstances();
			for (int i = 0; i < installs.length; i++) {
				if (installs[i].getName().equals(name))
					return installs[i];
			}
		}
		return JavaRuntime.getDefaultVMInstall();
	}
	
	public static Map getVMSpecificAttributes(ILaunchConfiguration config) throws CoreException {
		Map map = new HashMap(2);
		String javaCommand = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, (String)null); 
		map.put(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, javaCommand);
		if (TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$
			ModelEntry entry = PDECore.getDefault().getModelManager().findEntry("org.eclipse.jdt.debug"); //$NON-NLS-1$
			if (entry != null) {
				IPluginModelBase model = entry.getExternalModel();
				if (model != null) {
                    File file = new File(model.getInstallLocation());
                    if (!file.isFile())
                        file = new File(file, "jdi.jar"); //$NON-NLS-1$
					if (file.exists())
						map.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND, new String[] {file.getAbsolutePath()});
				}
			}
		}
		return map;
	}

	public static String getDefaultWorkspace() {
		return getDefaultPath().append("runtime-workspace").toOSString(); //$NON-NLS-1$
	}
	
	public static IPath getDefaultPath() {
		return PDEPlugin.getWorkspace().getRoot().getLocation().removeLastSegments(1);
	}
	
	public static TreeSet parseWorkspacePluginIds(ILaunchConfiguration config)
		throws CoreException {
		TreeSet set = new TreeSet();
		String ids = config.getAttribute(ILauncherSettings.WSPROJECT, (String) null);
		if (ids != null) {
			StringTokenizer tok = new StringTokenizer(ids, File.pathSeparator);
			while (tok.hasMoreTokens())
				set.add(tok.nextToken());
		}
		return set;
	}
	
	public static TreeSet parseExternalPluginIds(ILaunchConfiguration config)
		throws CoreException {
		TreeSet selected = new TreeSet();
		String ids = config.getAttribute(ILauncherSettings.EXTPLUGINS, (String) null);
		if (ids != null) {
			StringTokenizer tok = new StringTokenizer(ids, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				int loc = token.lastIndexOf(',');
				if (loc == -1) {
					selected.add(token);
				} else if (token.charAt(loc + 1) == 't') {
					selected.add(token.substring(0, loc));
				}
			}
		}
		return selected;
	}
	
	
	public static String[] constructClasspath(ILaunchConfiguration configuration) throws CoreException {
		String jarPath = getStartupJarPath();
		if (jarPath == null)
			return null;
		
		ArrayList entries = new ArrayList();
		entries.add(jarPath);
		StringTokenizer tok = new StringTokenizer(configuration.getAttribute(ILauncherSettings.BOOTSTRAP_ENTRIES, ""), ","); //$NON-NLS-1$ //$NON-NLS-2$
		while (tok.hasMoreTokens())
			entries.add(tok.nextToken().trim());
		return (String[])entries.toArray(new String[entries.size()]);
	}
	
	private static String getStartupJarPath() throws CoreException {
		IPlugin plugin = PDECore.getDefault().findPlugin("org.eclipse.platform"); //$NON-NLS-1$
		if (plugin != null && plugin.getModel().getUnderlyingResource() != null) {
			IProject project = plugin.getModel().getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE &&
							roots[i].getPackageFragment("org.eclipse.core.launcher").exists()){ //$NON-NLS-1$
						IPath path = jProject.getOutputLocation().removeFirstSegments(1);
						return project.getLocation().append(path).toOSString();
					}
				}
			}
			if (project.getFile("startup.jar").exists()) //$NON-NLS-1$
				return project.getFile("startup.jar").getLocation().toOSString(); //$NON-NLS-1$
		}
		File startupJar =
			ExternalModelManager.getEclipseHome().append("startup.jar").toFile(); //$NON-NLS-1$
		
		// if something goes wrong with the preferences, fall back on the startup.jar 
		// in the running eclipse.  
		if (!startupJar.exists())
			startupJar = new Path(ExternalModelManager.computeDefaultPlatformPath()).append("startup.jar").toFile(); //$NON-NLS-1$
		
		return startupJar.exists() ? startupJar.getAbsolutePath() : null;
	}
			
	public static TreeMap getPluginsToRun(ILaunchConfiguration config)
			throws CoreException {
		TreeMap map = null;
		ArrayList statusEntries = new ArrayList();
		
		if (!config.getAttribute(ILauncherSettings.USE_DEFAULT, true)) {
			map = validatePlugins(getSelectedPlugins(config), statusEntries);
		}
		
		if (map == null)
			map = validatePlugins(PDECore.getDefault().getModelManager().getPlugins(), statusEntries);

		final String requiredPlugin;
		if (PDECore.getDefault().getModelManager().isOSGiRuntime())
			requiredPlugin = "org.eclipse.osgi"; //$NON-NLS-1$
		else
			requiredPlugin = "org.eclipse.core.boot"; //$NON-NLS-1$
		
		if (!map.containsKey(requiredPlugin)) {
			final Display display = getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(
							display.getActiveShell(),
							PDEUIMessages.WorkbenchLauncherConfigurationDelegate_title,
							NLS.bind(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_missingRequired, requiredPlugin));
				}
			});
			return null;
		}
		
		// alert user if any plug-ins are not loaded correctly.
		if (statusEntries.size() > 0) {
			final MultiStatus multiStatus =
				new MultiStatus(
						PDEPlugin.getPluginId(),
						IStatus.OK,
						(IStatus[])statusEntries.toArray(new IStatus[statusEntries.size()]),
						PDEUIMessages.WorkbenchLauncherConfigurationDelegate_brokenPlugins,
						null);
			if (!ignoreValidationErrors(multiStatus)) {
				return null;
			}
		}	
		
		return map;
	}
	
	public static IPluginModelBase[] getSelectedPlugins(ILaunchConfiguration config) throws CoreException {
		TreeMap map = new TreeMap();
		boolean automaticAdd = config.getAttribute(ILauncherSettings.AUTOMATIC_ADD, true);
		IPluginModelBase[] wsmodels = PDECore.getDefault().getModelManager().getWorkspaceModels();
		Set wsPlugins = parseWorkspacePluginIds(config);
		for (int i = 0; i < wsmodels.length; i++) {
			String id = wsmodels[i].getPluginBase().getId();		
			// see the documentation of AdvancedLauncherUtils.initWorkspacePluginsState
			if (id != null && automaticAdd != wsPlugins.contains(id))
				map.put(id, wsmodels[i]);
		}
		
		Set exModels = parseExternalPluginIds(config);
		IPluginModelBase[] exmodels =
			PDECore.getDefault().getModelManager().getExternalModels();
		for (int i = 0; i < exmodels.length; i++) {
			String id = exmodels[i].getPluginBase().getId();
			if (id != null && exModels.contains(id) && !map.containsKey(id))
				map.put(id, exmodels[i]);
		}

		return (IPluginModelBase[]) map.values().toArray(new IPluginModelBase[map.size()]);
	}
	
	public static IProject[] getAffectedProjects(ILaunchConfiguration config) throws CoreException {
		boolean doAdd = config.getAttribute(ILauncherSettings.AUTOMATIC_ADD, true);
		boolean useFeatures = config.getAttribute(ILauncherSettings.USEFEATURES, false);
		boolean useDefault = config.getAttribute(ILauncherSettings.USE_DEFAULT, true);

		ArrayList projects = new ArrayList();		
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getWorkspaceModels();
		Set wsPlugins = parseWorkspacePluginIds(config);
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null)
				continue;
			// see the documentation of AdvancedLauncherUtils.initWorkspacePluginsState
			if (useDefault || useFeatures || doAdd != wsPlugins.contains(id)) {
				IProject project = models[i].getUnderlyingResource().getProject();
				if (project.hasNature(JavaCore.NATURE_ID))
					projects.add(project);
			}
		}
		
		// add fake "Java Search" project
		SearchablePluginsManager manager = PDECore.getDefault().getModelManager().getSearchablePluginsManager();
		IJavaProject proxy = manager.getProxyProject();
		if (proxy != null) {
			IProject project = proxy.getProject();
			if (project.isOpen())
				projects.add(project);
		}
		return (IProject[])projects.toArray(new IProject[projects.size()]);
	}
	
	private static TreeMap validatePlugins(IPluginModelBase[] models, ArrayList statusEntries) {
		TreeMap map = new TreeMap();
		for (int i = 0; i < models.length; i++) {
			IStatus status = validateModel(models[i]);
			if (status == null) {
				String id = models[i].getPluginBase().getId();
				if (id != null) {
					map.put(id, models[i]);
				}
			} else {
				statusEntries.add(status);
			}
		}
		return map;
	}
	
	private static IStatus validateModel(IPluginModelBase model) {
		return model.isLoaded()
			? null
			: new Status(
				IStatus.WARNING,
				PDEPlugin.getPluginId(),
				IStatus.OK,
				model.getPluginBase().getId(),
				null);
	}

	public static String getBootPath(IPluginModelBase bootModel) {
		try {
			IResource resource = bootModel.getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (project.hasNature(JavaCore.NATURE_ID)) {
					resource = project.findMember("boot.jar"); //$NON-NLS-1$
					if (resource != null)
						return "file:" + resource.getLocation().toOSString(); //$NON-NLS-1$
					IPath path = JavaCore.create(project).getOutputLocation();
					if (path != null) {
						IPath sourceBootPath =
							project.getParent().getLocation().append(path);
						return sourceBootPath.addTrailingSeparator().toOSString();
					}
				}
			} else {
				File bootJar = new File(bootModel.getInstallLocation(), "boot.jar"); //$NON-NLS-1$
				if (bootJar.exists())
					return "file:" + bootJar.getAbsolutePath(); //$NON-NLS-1$
			}
		} catch (CoreException e) {
		}
		return null;
	}
	
	private static boolean ignoreValidationErrors(final MultiStatus status) {
		final boolean[] result = new boolean[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0] =
					MessageDialog.openConfirm(
						getDisplay().getActiveShell(),
						PDEUIMessages.WorkbenchLauncherConfigurationDelegate_title,
						status.getMessage());
			}
		});
		return result[0];
	}
	
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}
	
	public static IVMInstall createLauncher(
		ILaunchConfiguration configuration)
		throws CoreException {
		String vm = configuration.getAttribute(ILauncherSettings.VMINSTALL, (String) null);
		IVMInstall launcher = LauncherUtils.getVMInstall(vm);

		if (launcher == null) 
			throw new CoreException(
				createErrorStatus(NLS.bind(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_noJRE, vm)));
		
		if (!launcher.getInstallLocation().exists()) 
			throw new CoreException(
				createErrorStatus(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_jrePathNotFound));
		
		return launcher;
	}
	
	public static IStatus createErrorStatus(String message) {
		return new Status(
			IStatus.ERROR,
			PDEPlugin.getPluginId(),
			IStatus.OK,
			message,
			null);
	}

	public static void setDefaultSourceLocator(
			ILaunchConfiguration configuration, ILaunch launch)
			throws CoreException {
		ILaunchConfigurationWorkingCopy wc = null;
		if (configuration.isWorkingCopy()) {
			wc = (ILaunchConfigurationWorkingCopy) configuration;
		} else {
			wc = configuration.getWorkingCopy();
		}
		
		// set any old source locators to null.  Source locator is now declared in the plugin.xml
		String locator = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String) null);
		if (locator != null)
			wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID,(String) null);
		
		// set source path provider on pre-2.1 configurations
		String id = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, (String) null);
		if (id == null) 
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, "org.eclipse.pde.ui.workbenchClasspathProvider"); //$NON-NLS-1$
		
		if (locator != null || id == null)
			wc.doSave();
	}
	
	public static boolean clearWorkspace(ILaunchConfiguration configuration, String workspace, IProgressMonitor monitor) throws CoreException {
		File workspaceFile = new Path(workspace).toFile();
		if (configuration.getAttribute(ILauncherSettings.DOCLEAR, false) && workspaceFile.exists()) {
			boolean doClear = !configuration.getAttribute(ILauncherSettings.ASKCLEAR, true);
			if (!doClear) {
				int result = confirmDeleteWorkspace(workspaceFile);
				if (result == 2) {
					monitor.done();
					return false;
				}
				doClear = result == 0;
			}
			if (doClear) {
				try {
					deleteContent(workspaceFile, monitor);
				} catch (IOException e) {
					PDEPlugin.logErrorMessage("Error occurred while deleting the workspace data upon launching"); //$NON-NLS-1$
				}
			}
		}
		monitor.done();
		return true;
	}
	
	private static int confirmDeleteWorkspace(final File workspaceFile) {
		final int[] result = new int[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				String title = PDEUIMessages.WorkbenchLauncherConfigurationDelegate_title;
				String message =
					NLS.bind(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_confirmDeleteWorkspace, workspaceFile.getPath());
				MessageDialog dialog = new MessageDialog(getDisplay().getActiveShell(), title, null,
						message, MessageDialog.QUESTION, new String[]{IDialogConstants.YES_LABEL,
								IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
				result[0] = dialog.open();
			}
		});
		return result[0];
	}
	
	public static File createConfigArea(ILaunchConfiguration config) {
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString(), config.getName());
		try {
			if (!config.getAttribute(ILauncherSettings.CONFIG_USE_DEFAULT_AREA, true)) {
				String userPath = config.getAttribute(ILauncherSettings.CONFIG_LOCATION, (String)null);
				if (userPath != null)
					dir = new File(userPath);
			}
		} catch (CoreException e) {
		}		
		if (!dir.exists()) 
			dir.mkdirs();		
		return dir;		
	}

	public static void clearConfigArea(File configDir, IProgressMonitor monitor) {
		try {
			deleteContent(configDir, monitor);
		} catch (IOException e) {
			PDEPlugin.logErrorMessage("Error occurred while deleting the configuration area upon launching"); //$NON-NLS-1$
		}
	}
	
	private static void deleteContent(File curr, IProgressMonitor monitor) throws IOException {
		if (curr.isDirectory()) {
			File[] children = curr.listFiles();
			if (children != null) {
				monitor.beginTask("", children.length); //$NON-NLS-1$
				for (int i = 0; i < children.length; i++) {
					deleteContent(children[i], new SubProgressMonitor(monitor, 1));
				}
			}
		}
		curr.delete();
		monitor.done();
	}
	
	public static String getTracingFileArgument(
		ILaunchConfiguration config,
		String optionsFileName)
		throws CoreException {
		try {
			TracingOptionsManager mng = PDECore.getDefault().getTracingOptionsManager();
			Map options =
				config.getAttribute(ILauncherSettings.TRACING_OPTIONS, (Map) null);
			String selected = config.getAttribute(ILauncherSettings.TRACING_CHECKED, (String)null);
			if (selected == null) {
				mng.save(optionsFileName, options);
			} else if (!selected.equals(ILauncherSettings.TRACING_NONE)) {
				HashSet result = new HashSet();
				StringTokenizer tokenizer = new StringTokenizer(selected, ","); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens()) {
					result.add(tokenizer.nextToken());
				}
				mng.save(optionsFileName, options, result);
			}
		} catch (CoreException e) {
			return ""; //$NON-NLS-1$
		}
		return optionsFileName;
	}
	
	public static String getPrimaryPlugin() {
		Properties properties = TargetPlatform.getConfigIniProperties("install.ini");		 //$NON-NLS-1$
		return properties == null ? null : properties.getProperty("feature.default.id");		 //$NON-NLS-1$
	}
	
	public static String getContributingPlugin(String productID) {
		if (productID == null)
			return null;
		int index = productID.lastIndexOf('.');
		return index == -1 ? productID : productID.substring(0, index);
	}
	
	public static String getProductID(ILaunchConfiguration configuration) throws CoreException {
		String result = null;
		if (configuration.getAttribute(ILauncherSettings.USE_PRODUCT, false)) {
			result = configuration.getAttribute(ILauncherSettings.PRODUCT, (String)null);
		} else {
			// find the product associated with the application, and return its contributing plug-in
			String appID = configuration.getAttribute(ILauncherSettings.APPLICATION, getDefaultApplicationName());
			IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPlugins();
			for (int i = 0; i < plugins.length; i++) {
				String id = plugins[i].getPluginBase().getId();
				IPluginExtension[] extensions = plugins[i].getPluginBase().getExtensions();
				for (int j = 0; j < extensions.length; j++) {
					String point = extensions[j].getPoint();
					if (point != null && point.equals("org.eclipse.core.runtime.products")) {//$NON-NLS-1$
						IPluginObject[] children = extensions[j].getChildren();
						if (children.length != 1)
							continue;
						if (!"product".equals(children[0].getName())) //$NON-NLS-1$
							continue;
						if (appID.equals(((IPluginElement)children[0]).getAttribute("application").getValue())) { //$NON-NLS-1$
							result = id;
							break;
						}
					}
				}
			}
		}
		if (result != null)
			return result;
		
		Properties properties = TargetPlatform.getConfigIniProperties("configuration/config.ini");		 //$NON-NLS-1$
		return properties == null ? null : properties.getProperty("eclipse.product"); //$NON-NLS-1$
	}

	public static String getDefaultApplicationName() {
		if (!PDECore.getDefault().getModelManager().isOSGiRuntime())
			return "org.eclipse.ui.workbench"; //$NON-NLS-1$
		
		Properties properties = TargetPlatform.getConfigIniProperties("configuration/config.ini"); //$NON-NLS-1$
		String appName = (properties != null) ? properties.getProperty("eclipse.application") : null; //$NON-NLS-1$
		return (appName != null) ? appName : "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$
	}
	
	private static void addSplashLocation(Properties properties, String productID, Map map)  {
		Properties targetConfig = TargetPlatform.getConfigIniProperties("configuration/config.ini"); //$NON-NLS-1$
		String targetProduct = targetConfig == null ? null : targetConfig.getProperty("eclipse.product"); //$NON-NLS-1$
		String targetSplash = targetConfig == null ? null : targetConfig.getProperty("osgi.splashPath"); //$NON-NLS-1$
		ArrayList locations = new ArrayList();
		if (!productID.equals(targetProduct) || targetSplash == null) {
			String plugin = getContributingPlugin(productID);
			locations.add(plugin);
			IPluginModelBase model = (IPluginModelBase)map.get(plugin);
			if (model != null) {
				BundleDescription desc = model.getBundleDescription();
				if (desc != null) {
					BundleDescription[] fragments = desc.getFragments();
					for (int i = 0; i < fragments.length; i++)
						locations.add(fragments[i].getSymbolicName());
				}
			}
		} else {
			StringTokenizer tok = new StringTokenizer(targetSplash, ","); //$NON-NLS-1$
			while (tok.hasMoreTokens())
				locations.add(tok.nextToken());			
		}
		
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < locations.size(); i++) {
			String location = (String)locations.get(i);
			if (location.startsWith("platform:/base/plugins/")) { //$NON-NLS-1$
				location = location.replaceFirst("platform:/base/plugins/", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String url = TargetPlatform.getBundleURL(location, map);
			if (url == null)
				continue;
			if (buffer.length() > 0)
				buffer.append(","); //$NON-NLS-1$
			buffer.append(url);
		}
		if (buffer.length() > 0)
			properties.setProperty("osgi.splashPath", buffer.toString()); //$NON-NLS-1$
	}
	
	public static Properties createConfigIniFile(ILaunchConfiguration configuration, String productID, Map map, File directory) throws CoreException {
		Properties properties = new Properties();
		if (configuration.getAttribute(ILauncherSettings.CONFIG_GENERATE_DEFAULT, true)) {
			properties.setProperty("osgi.install.area", "file:" + ExternalModelManager.getEclipseHome().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
			properties.setProperty("osgi.configuration.cascaded", "false"); //$NON-NLS-1$ //$NON-NLS-2$
			properties.setProperty("osgi.framework", "org.eclipse.osgi"); //$NON-NLS-1$ //$NON-NLS-2$
			if (productID != null)
				addSplashLocation(properties, productID, map);
			if (map.containsKey("org.eclipse.update.configurator")) { //$NON-NLS-1$
				properties.setProperty("osgi.bundles", "org.eclipse.core.runtime@2:start,org.eclipse.update.configurator@3:start"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				StringBuffer buffer = new StringBuffer();
				Iterator iter = map.keySet().iterator();
				while (iter.hasNext()) {
					String id = iter.next().toString();
					if ("org.eclipse.osgi".equals(id)) //$NON-NLS-1$
						continue;
					buffer.append(id);
					if ("org.eclipse.core.runtime".equals(id)) { //$NON-NLS-1$
						buffer.append("@2:start"); //$NON-NLS-1$
					}
					if (iter.hasNext())
						buffer.append(","); //$NON-NLS-1$
				}
				properties.setProperty("osgi.bundles", buffer.toString()); //$NON-NLS-1$
			}
			properties.setProperty("osgi.bundles.defaultStartLevel", "4"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			String templateLoc = configuration.getAttribute(ILauncherSettings.CONFIG_TEMPLATE_LOCATION, (String)null);
			if (templateLoc != null) {
				File templateFile = new File(templateLoc);
				if (templateFile.exists() && templateFile.isFile()) {
					FileInputStream stream = null;
					try {
						stream = new FileInputStream(templateFile);
						properties.load(stream);
					} catch (Exception e) {
						String message = e.getMessage();
						if (message != null)
							throw new CoreException(
								new Status(
									IStatus.ERROR,
									PDEPlugin.getPluginId(),
									IStatus.ERROR,
									message,
									e));
					} finally {
						if (stream != null) {
							try {
								stream.close();
							} catch (IOException e) {
							}
						}
					}
				}
			}
		}
		setBundleLocations(map, properties);
		save(new File(directory, "config.ini"), properties); //$NON-NLS-1$
		return properties;
	}
	
	private static void setBundleLocations(Map map, Properties properties) {
		String framework = properties.getProperty("osgi.framework"); //$NON-NLS-1$
		if (framework != null) {
			if (framework.startsWith("platform:/base/plugins/")) { //$NON-NLS-1$
				framework.replaceFirst("platform:/base/plugins/", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String url = TargetPlatform.getBundleURL(framework, map);
			if (url != null)
				properties.setProperty("osgi.framework", url); //$NON-NLS-1$
		}
		
		String bundles = properties.getProperty("osgi.bundles"); //$NON-NLS-1$
		if (bundles != null) {
			StringBuffer buffer = new StringBuffer();
			StringTokenizer tokenizer = new StringTokenizer(bundles, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken().trim();
				String url = TargetPlatform.getBundleURL(token, map);
				int index = -1;
				if (url == null) {
					index = token.indexOf('@');
					if (index != -1) {
						url = TargetPlatform.getBundleURL(token.substring(0,index), map);
					}
					if (url == null) {
						index = token.indexOf(':');
						if (index != -1) {
							url = TargetPlatform.getBundleURL(token.substring(0,index), map);
						}
					}
				}
				if (url == null) {
					buffer.append(token);
				} else {
					buffer.append("reference:" + url); //$NON-NLS-1$
					if (index != -1)
						buffer.append(token.substring(index));
				}
				if (tokenizer.hasMoreTokens())
					buffer.append(","); //$NON-NLS-1$
			}
			properties.setProperty("osgi.bundles", buffer.toString()); //$NON-NLS-1$
		}
	}
	
	private static void save(File file, Properties properties) {
		try {
			FileOutputStream stream = new FileOutputStream(file);
			properties.store(stream, "Eclipse Runtime Configuration File"); //$NON-NLS-1$
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}
	
	public static void synchronizeManifests(ILaunchConfiguration config, File configDir) {
		if (!PDECore.getDefault().getModelManager().isOSGiRuntime())
			return;
		try {
			String programArgs = config.getAttribute(ILauncherSettings.PROGARGS, ""); //$NON-NLS-1$
			if (programArgs.indexOf("-clean") != -1) //$NON-NLS-1$
				return;
		} catch (CoreException e) {
		}
		File dir = new File(configDir, "org.eclipse.osgi/manifests"); //$NON-NLS-1$
		if (dir.exists() && dir.isDirectory()) {
			PDECore.getDefault().getJavaElementChangeListener().synchronizeManifests(dir);	
		}
	}

}
