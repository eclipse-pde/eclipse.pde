package org.eclipse.pde.internal.ui.launcher;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.debug.ui.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;

public class LauncherUtils {
	private static final String KEY_MISSING_REQUIRED =
		"WorkbenchLauncherConfigurationDelegate.missingRequired";
	private static final String KEY_BROKEN_PLUGINS =
		"WorkbenchLauncherConfigurationDelegate.brokenPlugins";
	private static final String KEY_NO_JRE =
		"WorkbenchLauncherConfigurationDelegate.noJRE";
	private static final String KEY_JRE_PATH_NOT_FOUND =
		"WorkbenchLauncherConfigurationDelegate.jrePathNotFound";
	private static final String KEY_PROBLEMS_DELETING =
		"WorkbenchLauncherConfigurationDelegate.problemsDeleting";
	private static final String KEY_TITLE =
		"WorkbenchLauncherConfigurationDelegate.title";
	private static final String KEY_DELETE_WORKSPACE =
		"WorkbenchLauncherConfigurationDelegate.confirmDeleteWorkspace";

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

	public static String getDefaultProgramArguments() {
		String os = TargetPlatform.getOS();
		String ws = TargetPlatform.getWS();
		String arch = TargetPlatform.getOSArch();
		String nl = TargetPlatform.getNL();
		return "-os " + os + " -ws " + ws + " -arch " + arch + " -nl " + nl;
	}
	
	public static String getDefaultWorkspace() {
		return getDefaultPath().append("runtime-workspace").toOSString();
	}
	
	public static IPath getDefaultPath() {
		return PDEPlugin.getWorkspace().getRoot().getLocation().removeLastSegments(1);
	}
	
	public static TreeSet parseDeselectedWSIds(ILaunchConfiguration config)
		throws CoreException {
		TreeSet deselected = new TreeSet();
		String ids = config.getAttribute(ILauncherSettings.WSPROJECT, (String) null);
		if (ids != null) {
			StringTokenizer tok = new StringTokenizer(ids, File.pathSeparator);
			while (tok.hasMoreTokens())
				deselected.add(tok.nextToken());
		}
		return deselected;
	}
	
	public static TreeSet parseSelectedExtIds(ILaunchConfiguration config)
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
		StringTokenizer tok = new StringTokenizer(configuration.getAttribute(ILauncherSettings.BOOTSTRAP_ENTRIES, ""), ",");
		while (tok.hasMoreTokens())
			entries.add(tok.nextToken().trim());
		return (String[])entries.toArray(new String[entries.size()]);
	}
	
	private static String getStartupJarPath() throws CoreException {
		IPlugin plugin = PDECore.getDefault().findPlugin("org.eclipse.platform");
		if (plugin != null && plugin.getModel().getUnderlyingResource() != null) {
			IProject project = plugin.getModel().getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
						IPath path = jProject.getOutputLocation().removeFirstSegments(1);
						return project.getLocation().append(path).toOSString();
					}
				}
			}
			if (project.getFile("startup.jar").exists())
				return project.getFile("startup.jar").getLocation().toOSString();
		}
		File startupJar =
			ExternalModelManager.getEclipseHome().append("startup.jar").toFile();
		
		return startupJar.exists() ? startupJar.getAbsolutePath() : null;
	}
		
	public static String getBuildOutputFolders() {
		IPluginModelBase[] wsmodels = PDECore.getDefault().getWorkspaceModelManager().getAllModels();
		ArrayList result = new ArrayList();
		result.add(new Path("bin"));
		for (int i = 0; i < wsmodels.length; i++) {
			addOutputLocations(result, wsmodels[i]);
		}
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < result.size(); i++) {
			buffer.append(result.get(i).toString());
			if (i < result.size() -1)
				buffer.append(",");
		}
		return buffer.toString();
	}
	
	private static void addOutputLocations(ArrayList result, IPluginModelBase model) {
		IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				addPath(result, jProject.getOutputLocation());
				IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE)
						addPath(result, roots[i].getRawClasspathEntry().getOutputLocation());
				}
			} 
		} catch (Exception e) {
		}
	}
	
	private static void addPath(ArrayList result, IPath path) {
		if (path != null && path.segmentCount() > 1) {
			path = path.removeFirstSegments(1);
			if (!result.contains(path))
				result.add(path);
		}		
	}
	
	public static TreeMap getPluginsToRun(ILaunchConfiguration config)
			throws CoreException {
		TreeMap map = null;
		ArrayList statusEntries = new ArrayList();
		
		if (config.getAttribute(ILauncherSettings.USE_ONE_PLUGIN, false)) {
			String id = config.getAttribute(ILauncherSettings.ONE_PLUGIN_ID, "");
			if (id.length() > 0)
				map = validatePlugins(getPluginAndPrereqs(id), statusEntries);
		} else if (!config.getAttribute(ILauncherSettings.USECUSTOM, true)) {
			map = validatePlugins(getSelectedPlugins(config), statusEntries);
		}
		
		if (map == null)
			map = validatePlugins(PDECore.getDefault().getModelManager().getPlugins(), statusEntries);

		StringBuffer errorText = new StringBuffer();		
		final String lineSeparator = System.getProperty("line.separator");
		
		HashMap autoStart = getAutoStartPlugins(config);
		Iterator iter = autoStart.keySet().iterator();
		while (iter.hasNext()) {
			String id = iter.next().toString();
			if (!map.containsKey(id))
				errorText.append(id + lineSeparator);
		}
		
		if (errorText.length() > 0) {
			final String text = errorText.toString();
			final Display display = getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(
							display.getActiveShell(),
							PDEPlugin.getResourceString(KEY_TITLE),
							PDEPlugin.getResourceString(KEY_MISSING_REQUIRED)
							+ lineSeparator
							+ text);
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
						PDEPlugin.getResourceString(KEY_BROKEN_PLUGINS),
						null);
			if (!ignoreValidationErrors(multiStatus)) {
				return null;
			}
		}	
		
		return map;
	}
	
	public static HashMap getAutoStartPlugins(ILaunchConfiguration config) {
		HashMap list = new HashMap();
		if (!PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			list.put("org.eclipse.core.boot", new Integer(0));
		} else {
			try {
				list.put("org.eclipse.osgi", new Integer(0));
				if (config.getAttribute(ILauncherSettings.CONFIG_USE_DEFAULT, true)) {
					list.put("org.eclipse.osgi.services", new Integer(-1));
					list.put("org.eclipse.osgi.util", new Integer(-1));
					list.put("org.eclipse.core.runtime", new Integer(2));
					list.put("org.eclipse.update.configurator", new Integer(3));
				} else {
					String selected = config.getAttribute(ILauncherSettings.CONFIG_AUTO_START, "");
					StringTokenizer tokenizer = new StringTokenizer(selected, ",");
					while (tokenizer.hasMoreTokens()) {
						String token = tokenizer.nextToken().trim();
						Integer level = new Integer(token.substring(token.indexOf('@') + 1));
						list.put(token.substring(0,token.indexOf('@')), level);
					}		
				}
			} catch (CoreException e) {
			}
		}		
		return list;
	}
	
	public static IPluginModelBase[] getPluginAndPrereqs(String id) {
		TreeMap map = new TreeMap();
		addPluginAndPrereqs(id, map);
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			addPluginAndPrereqs("org.eclipse.osgi", map);
			addPluginAndPrereqs("org.eclipse.osgi.services", map);
			addPluginAndPrereqs("org.eclipse.osgi.util", map);
			addPluginAndPrereqs("org.eclipse.update.configurator", map);
		} else {
			addPluginAndPrereqs("org.eclipse.core.boot", map);
		}
		addPluginAndPrereqs("org.eclipse.core.runtime", map);
		
		return (IPluginModelBase[])map.values().toArray(new IPluginModelBase[map.size()]);
	}
	
	private static void addPluginAndPrereqs(String id, TreeMap map) {
		if (map.containsKey(id))
			return;
		
		ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(id);
		if (entry == null)
			return;
		
		IPluginModelBase model = entry.getActiveModel();
		
		map.put(id, model);
		
		IPluginImport[] imports = model.getPluginBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			addPluginAndPrereqs(imports[i].getId(), map);
		}
		
		if (!map.containsKey("org.apache.ant")) {
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				if (extensions[i].getPoint().startsWith("org.eclipse.ant.core")) {
					addPluginAndPrereqs("org.apache.ant", map);
					break;
				}
			}
		}
		
		if (model instanceof IFragmentModel) {
			addPluginAndPrereqs(((IFragmentModel) model).getFragment().getPluginId(), map);
		} else {
			IFragment[] fragments = PDECore.getDefault().findFragmentsFor(id, model.getPluginBase().getVersion());
			for (int i = 0; i < fragments.length; i++) {
				addPluginAndPrereqs(fragments[i].getId(), map);
			}
		}
	}
	
	private static IPluginModelBase[] getSelectedPlugins(ILaunchConfiguration config) throws CoreException {
		TreeMap map = new TreeMap();
		IPluginModelBase[] wsmodels = PDECore.getDefault().getWorkspaceModelManager().getAllModels();
		Set deselectedWSPlugins = parseDeselectedWSIds(config);
		for (int i = 0; i < wsmodels.length; i++) {
			String id = wsmodels[i].getPluginBase().getId();
			if (id != null && !deselectedWSPlugins.contains(id))
				map.put(id, wsmodels[i]);
		}
		
		Set selectedExModels = parseSelectedExtIds(config);
		IPluginModelBase[] exmodels =
			PDECore.getDefault().getExternalModelManager().getAllModels();
		for (int i = 0; i < exmodels.length; i++) {
			String id = exmodels[i].getPluginBase().getId();
			if (id != null && selectedExModels.contains(id) && !map.containsKey(id))
				map.put(id, exmodels[i]);
		}

		return (IPluginModelBase[]) map.values().toArray(new IPluginModelBase[map.size()]);
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
					resource = project.findMember("boot.jar");
					if (resource != null)
						return "file:" + resource.getLocation().toOSString();
					IPath path = JavaCore.create(project).getOutputLocation();
					if (path != null) {
						IPath sourceBootPath =
							project.getParent().getLocation().append(path);
						return sourceBootPath.addTrailingSeparator().toOSString();
					}
				}
			} else {
				File bootJar = new File(bootModel.getInstallLocation(), "boot.jar");
				if (bootJar.exists())
					return "file:" + bootJar.getAbsolutePath();
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
						PDEPlugin.getResourceString(KEY_TITLE),
						status.getMessage());
			}
		});
		return result[0];
	}
	
	private static Display getDisplay() {
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
				createErrorStatus(PDEPlugin.getFormattedMessage(KEY_NO_JRE, vm)));
		
		if (!launcher.getInstallLocation().exists()) 
			throw new CoreException(
				createErrorStatus(PDEPlugin.getResourceString(KEY_JRE_PATH_NOT_FOUND)));
		
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

	public static  void setDefaultSourceLocator(ILaunchConfiguration configuration, ILaunch launch) throws CoreException {
		String id = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, (String)null);
		if (id == null) {
			IPersistableSourceLocator locator = DebugPlugin.getDefault().getLaunchManager().newSourceLocator(JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
			ILaunchConfigurationWorkingCopy wc = null;
			if (configuration.isWorkingCopy()) {
				wc = (ILaunchConfigurationWorkingCopy)configuration;
			} else {
				wc = configuration.getWorkingCopy();
			}
			wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, "org.eclipse.pde.ui.workbenchClasspathProvider");
			locator.initializeDefaults(wc);
			wc.doSave();
			launch.setSourceLocator(locator);
		}		
	}
		
	public static void clearWorkspace(ILaunchConfiguration configuration, String workspace) throws CoreException {
		File workspaceFile = new Path(workspace).toFile();
		if (configuration.getAttribute(ILauncherSettings.DOCLEAR, false) && workspaceFile.exists()) {
			if (!configuration.getAttribute(ILauncherSettings.ASKCLEAR, true)
				|| confirmDeleteWorkspace(workspaceFile)) {
				try {
					deleteContent(workspaceFile);
				} catch (IOException e) {
					showWarningDialog(PDEPlugin.getResourceString(KEY_PROBLEMS_DELETING));
				}
			}
		}
	}
	
	private static void showWarningDialog(final String message) {
		getDisplay().syncExec(new Runnable() {
			public void run() {
				String title = PDEPlugin.getResourceString(KEY_TITLE);
				MessageDialog.openWarning(
					getDisplay().getActiveShell(),
					title,
					message);
			}
		});
	}
	
	private static boolean confirmDeleteWorkspace(final File workspaceFile) {
		final boolean[] result = new boolean[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				String title = PDEPlugin.getResourceString(KEY_TITLE);
				String message =
					PDEPlugin.getFormattedMessage(
						KEY_DELETE_WORKSPACE,
						workspaceFile.getPath());
				result[0] =
					MessageDialog.openQuestion(
						getDisplay().getActiveShell(),
						title,
						message);
			}
		});
		return result[0];
	}
	
	private static void deleteContent(File curr) throws IOException {
		if (curr.isDirectory()) {
			File[] children = curr.listFiles();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					deleteContent(children[i]);
				}
			}
		}
		curr.delete();
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
				StringTokenizer tokenizer = new StringTokenizer(selected, ",");
				while (tokenizer.hasMoreTokens()) {
					result.add(tokenizer.nextToken());
				}
				mng.save(optionsFileName, options, result);
			}
		} catch (CoreException e) {
			return "";
		}

		String tracingArg = "file:" + optionsFileName;
		if (SWT.getPlatform().equals("motif"))
			tracingArg = "file:" + optionsFileName;
		// defect 17661
		else if (SWT.getPlatform().equals("gtk"))
			tracingArg = "file://localhost" + optionsFileName;

		return tracingArg;
	}

	public static String getPrimaryFeatureId() {
		Properties properties = getInstallProperties();
		return (properties == null) ? null : properties.getProperty("feature.default.id");
	}

	public static String getDefaultApplicationName() {
		Properties properties = getInstallProperties();
		String appName = (properties != null) ? properties
				.getProperty("feature.default.application") : null;
		if (appName == null) {
			appName = PDECore.getDefault().getModelManager().isOSGiRuntime()
					? "org.eclipse.ui.ide.workbench"
					: "org.eclipse.ui.workbench";
		}
		return appName;
	}
	
	public static Properties getInstallProperties() {
		File iniFile = null;
		ModelEntry entry = PDECore.getDefault().getModelManager().findEntry("org.eclipse.platform");
		if (entry != null && entry.getActiveModel().getUnderlyingResource() != null) {
			IProject project = entry.getActiveModel().getUnderlyingResource().getProject();
			iniFile = new File(project.getFile("install.ini").getLocation().toOSString());
		}
		if (iniFile == null || !iniFile.exists()) {
			IPath eclipsePath = ExternalModelManager.getEclipseHome();
			iniFile = new File(eclipsePath.toFile(), "install.ini");
		}
		if (!iniFile.exists())
			return null;
		Properties pini = new Properties();
		try {
			FileInputStream fis = new FileInputStream(iniFile);
			pini.load(fis);
			fis.close();
			return pini;
		} catch (IOException e) {
		}		
		return null;
	}



}
