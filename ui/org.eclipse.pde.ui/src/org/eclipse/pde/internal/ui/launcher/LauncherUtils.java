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
import org.eclipse.pde.internal.core.plugin.*;
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

	private static String bootPath = null;
	private static boolean bootInSource = false;
	
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
	
	public static String[] constructClasspath() throws CoreException {
		IPlugin plugin = PDECore.getDefault().findPlugin("org.eclipse.platform");
		if (plugin != null && plugin.getModel() instanceof WorkspacePluginModel) {
			IProject project = plugin.getModel().getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
						IPath path = jProject.getOutputLocation().removeFirstSegments(1);
						return new String[] {project.getLocation().append(path).toOSString()};
					}
				}
			}
		}
		File startupJar =
			ExternalModelManager.getEclipseHome(null).append("startup.jar").toFile();
		//TODO remove after memory profiling
		//return startupJar.exists() ? new String[] { startupJar.getAbsolutePath(), "C:\\ymp-1.0.2-build115\\lib\\ympagent.jar"} : null;
		
		return startupJar.exists() ? new String[] { startupJar.getAbsolutePath()} : null;
	}
	
	protected static IPluginModelBase[] getWorkspacePluginsToRun(
		ILaunchConfiguration config,
		boolean useDefault)
		throws CoreException {
		IPluginModelBase[] wsmodels =
			PDECore.getDefault().getWorkspaceModelManager().getAllModels();
		if (useDefault)
			return wsmodels;

		ArrayList result = new ArrayList();
		TreeSet deselectedWSPlugins = parseDeselectedWSIds(config);
		for (int i = 0; i < wsmodels.length; i++) {
			String id = wsmodels[i].getPluginBase().getId();
			if (id != null && !deselectedWSPlugins.contains(id))
				result.add(wsmodels[i]);
		}
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
	}
	
	public static String getBuildOutputFolders(IPluginModelBase[] wsmodels) {
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
	
	public static TreeMap validatePlugins(
		IPluginModelBase[] wsmodels,
		IPluginModelBase[] exmodels)
		throws CoreException {
		bootPath = null;
		bootInSource = false;
		TreeMap result = new TreeMap();
		ArrayList statusEntries = new ArrayList();

		for (int i = 0; i < wsmodels.length; i++) {
			IStatus status = validateModel(wsmodels[i]);
			if (status == null) {
				String id = wsmodels[i].getPluginBase().getId();
				if (id != null) {
					result.put(id, wsmodels[i]);
				}
			} else {
				statusEntries.add(status);
			}
		}
		
		for (int i = 0; i < exmodels.length; i++) {
			String id = exmodels[i].getPluginBase().getId();
			if (id != null && !result.containsKey(id)) {
				result.put(id, exmodels[i]);
			}
		}
		
		StringBuffer errorText = new StringBuffer();
		
		bootPath = getBootPath((IPluginModelBase) result.get("org.eclipse.core.boot"));
		boolean isOSGI = PDECore.getDefault().getModelManager().isOSGiRuntime();
		final String lineSeparator = System.getProperty("line.separator");
		if (bootPath == null && !isOSGI) {
			errorText.append("org.eclipse.core.boot" + lineSeparator);
		}
		
		if (isOSGI) {
			if (!result.containsKey("org.eclipse.osgi"))
				errorText.append("org.eclipse.osgi" + lineSeparator);
			if (!result.containsKey("org.eclipse.osgi.services"))
				errorText.append("org.eclipse.osgi.services" + lineSeparator);
			if (!result.containsKey("org.eclipse.osgi.util"))
				errorText.append("org.eclipse.osgi.util" + lineSeparator);
			if (!result.containsKey("org.eclipse.core.runtime"))
				errorText.append("org.eclipse.core.runtime" + lineSeparator);
			if (!result.containsKey("org.eclipse.update.configurator"))
				errorText.append("org.eclipse.update.configurator");
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
		return result;
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

	private static String getBootPath(IPluginModelBase bootModel) {
		if (bootModel == null)
			return null;
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
						bootInSource = true;
						IPath sourceBootPath =
							project.getParent().getLocation().append(path);
						return sourceBootPath.addTrailingSeparator().toOSString();
					}
				}
			} else {
				File binDir = new File(bootModel.getInstallLocation(), "bin/");
				if (binDir.exists())
					return binDir.getAbsolutePath();

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
	
	public static boolean isBootInSource() {
		return bootInSource;
	}
	
	public static String getBootPath() {
		return bootPath;
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
			mng.save(optionsFileName, options);
		} catch (CoreException e) {
			return "";
		}

		String tracingArg = "\"file:" + optionsFileName + "\"";
		if (SWT.getPlatform().equals("motif"))
			tracingArg = "file:" + optionsFileName;
		// defect 17661
		else if (SWT.getPlatform().equals("gtk"))
			tracingArg = "file://localhost" + optionsFileName;

		return tracingArg;
	}



}
