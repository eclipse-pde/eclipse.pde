package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

public class LauncherUtils {
	private static final String KEY_DUPLICATES =
		"WorkbenchLauncherConfigurationDelegate.duplicates";
	private static final String KEY_DUPLICATE_PLUGINS =
		"WorkbenchLauncherConfigurationDelegate.duplicatePlugins";
	private static final String KEY_NO_BOOT =
		"WorkbenchLauncherConfigurationDelegate.noBoot";
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
		File startupJar =
			ExternalModelManager.getEclipseHome(null).append("startup.jar").toFile();

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
	
	public static IPluginModelBase[] validatePlugins(
		IPluginModelBase[] wsmodels,
		IPluginModelBase[] exmodels)
		throws CoreException {
		bootPath = null;
		bootInSource = false;
		IPluginModelBase bootModel = null;
		ArrayList result = new ArrayList();
		ArrayList statusEntries = new ArrayList();

		for (int i = 0; i < wsmodels.length; i++) {
			IStatus status = validateModel(wsmodels[i]);
			if (status == null) {
				String id = wsmodels[i].getPluginBase().getId();
				if (id != null) {
					result.add(wsmodels[i]);
					if (id.equals("org.eclipse.core.boot"))
						bootModel = wsmodels[i];
				}
			} else {
				statusEntries.add(status);
			}
		}

		Vector duplicates = new Vector();
		for (int i = 0; i < exmodels.length; i++) {
			IStatus status = validateModel(exmodels[i]);
			if (status == null) {
				boolean duplicate = false;
				String id = exmodels[i].getPluginBase().getId();
				for (int j = 0; j < wsmodels.length; j++) {
					if (wsmodels[j].getPluginBase().getId().equalsIgnoreCase(id)) {
						duplicates.add(id);
						duplicate = true;
						break;
					}
				}
				if (!duplicate) {
					result.add(exmodels[i]);
					if (id.equals("org.eclipse.core.boot"))
						bootModel = exmodels[i];
				}
			} else {
				statusEntries.add(status);
			}
		}

		// Look for boot path.  Cancel launch, if not found.
		bootPath = getBootPath(bootModel);
		if (bootPath == null) {
			MessageDialog.openError(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString(KEY_TITLE),
				PDEPlugin.getResourceString(KEY_NO_BOOT));
			return null;
		}

		// alert user if there are duplicate plug-ins.
		if (duplicates.size() > 0 && !continueRunning(duplicates)) {
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
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
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
	
	private static boolean continueRunning(final Vector duplicates) {
		final boolean[] result = new boolean[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				StringBuffer message =
					new StringBuffer(
						PDEPlugin.getFormattedMessage(
							KEY_DUPLICATES,
							new Integer(duplicates.size()).toString()));
				if (duplicates.size() <= 5) {
					String lineSeparator = System.getProperty("line.separator");
					message.append(
						lineSeparator
							+ lineSeparator
							+ PDEPlugin.getResourceString(KEY_DUPLICATE_PLUGINS)
							+ ":"
							+ lineSeparator);
					for (int i = 0; i < duplicates.size(); i++)
						message.append(duplicates.get(i) + lineSeparator);
				}
				result[0] =
					MessageDialog.openConfirm(
						PDEPlugin.getActiveWorkbenchShell(),
						PDEPlugin.getResourceString(KEY_TITLE),
						message.toString());
			}
		});
		return result[0];
	}

	private static boolean ignoreValidationErrors(final MultiStatus status) {
		final boolean[] result = new boolean[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0] =
					MessageDialog.openConfirm(
						PDEPlugin.getActiveWorkbenchShell(),
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
					PDEPlugin.getActiveWorkbenchShell(),
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
						PDEPlugin.getActiveWorkbenchShell(),
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
	
	public static String getTracingFileArgument(ILaunchConfiguration config) {
		TracingOptionsManager mng =
			PDECore.getDefault().getTracingOptionsManager();
		Map options;
		try {
			options =
				config.getAttribute(
					ILauncherSettings.TRACING_OPTIONS,
					mng.getTracingTemplateCopy());
		} catch (CoreException e) {
			return "";
		}
		mng.save(options);
		String optionsFileName = mng.getTracingFileName();
		String tracingArg;
		if (SWT.getPlatform().equals("motif"))
			tracingArg = "file:" + optionsFileName;
		// defect 17661
		else if (SWT.getPlatform().equals("gtk"))
			tracingArg = "file://localhost" + optionsFileName;
		else
			tracingArg = "\"file:" + optionsFileName + "\"";
		return tracingArg;
	}



}
