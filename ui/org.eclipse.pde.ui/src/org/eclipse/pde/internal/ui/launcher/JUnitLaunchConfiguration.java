package org.eclipse.pde.internal.ui.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.model.Factory;
import org.eclipse.core.runtime.model.PluginRegistryModel;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;
import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * Launch configuration delegate for a plain JUnit test.
 */
public class JUnitLaunchConfiguration extends JUnitBaseLaunchConfiguration implements ILauncherSettings {
	public static final String ID_PLUGIN_JUNIT= "org.eclipse.pde.junit.launchconfig"; //$NON-NLS-1$

	public static final String[] fgApplicationNames= new String[] {
			"org.eclipse.pde.junit.uitestapplication",
			"org.eclipse.pde.junit.coretestapplication"
	};
	public static final String fgDefaultApp= fgApplicationNames[0];
	
	private Vector fDuplicates= new Vector();
	
	/*
	 * @see JUnitBaseLauncherDelegate#configureVM(IType[], int, String)
	 */
	protected VMRunnerConfiguration createVMRunner(ILaunchConfiguration configuration, IType[] testTypes, int port, String runMode) throws CoreException {
		String applicationName= configuration.getAttribute(APPLICATION, fgDefaultApp);
		String workspace = configuration.getAttribute(LOCATION, (String)null);
		if (workspace == null)
			workspace= getTempWorkSpaceLocation();

		//VMRunnerConfiguration vmConfig= WorkbenchLaunchConfigurationDelegate.createWorkspaceRunnerConfiguration(workspace, applicationName, new NullProgressMonitor());
		VMRunnerConfiguration vmConfig= createWorkspaceRunnerConfiguration(configuration, applicationName, workspace);
			
		String testPluginID= getTestPluginId(configuration);

		Vector postArgsVector= new Vector(20);
		postArgsVector.add("-consolelog");
		postArgsVector.add("-port");
		postArgsVector.add(Integer.toString(port));
		postArgsVector.add("-testpluginname");
		postArgsVector.add(testPluginID);
		//	"-debugging",
		if (keepAlive(configuration) && runMode.equals(ILaunchManager.DEBUG_MODE))
			postArgsVector.add(0, "-keepalive");
		
		// a testname was specified just run the single test
		String testName= configuration.getAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, "");
		if (testName.length() > 0) {
			postArgsVector.add("-test"); //$NON-NLS-1$
			postArgsVector.add(testTypes[0].getFullyQualifiedName()+":"+testName);			
		} else {
			postArgsVector.add("-classnames");
			for (int i= 0; i < testTypes.length; i++) 
				postArgsVector.add(testTypes[i].getFullyQualifiedName());
		}

		String[] standardArgs= vmConfig.getProgramArguments();
		String[] postArgs= new String[postArgsVector.size()];
		postArgsVector.copyInto(postArgs);
		String[] args= new String[standardArgs.length+postArgsVector.size()];
		
		System.arraycopy(standardArgs, 0, args, 0, standardArgs.length);
		System.arraycopy(postArgs, 0, args, standardArgs.length, postArgs.length);

		vmConfig.setProgramArguments(args);
		
		if(configuration.getAttribute(DOCLEAR,true)) {
			try {
				deleteTestWorkspace(workspace);
			} catch(IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDEPlugin.PLUGIN_ID, IStatus.ERROR, "Could not delete test workspace.", e));
			}
		}
		return vmConfig;
	}

	protected String getTestPluginId(ILaunchConfiguration configuration) throws CoreException {
		PluginRegistryModel testPluginModel= null;
		IJavaProject javaProject= getJavaProject(configuration);
		
		try {
			testPluginModel= getPluginRegistryModel(javaProject);
		} catch(IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDEPlugin.PLUGIN_ID, IStatus.ERROR, "Could not determine test plugin Id.", e));
		}
		if (testPluginModel == null)
			throw new CoreException(new Status(IStatus.ERROR, PDEPlugin.PLUGIN_ID, IStatus.ERROR, "Could not determine test plugin Id - project is not a plugin.", null));
		
		String testPluginID= testPluginModel.getPlugins()[0].getId();
		return testPluginID;
	}
	
	private VMRunnerConfiguration createWorkspaceRunnerConfiguration(ILaunchConfiguration configuration, String appName, String workspaceLocation) throws CoreException {
		final IPluginModelBase[] plugins = getPlugins();
		String workspace = workspaceLocation;
		Path tmpWorkspacePath= new Path(workspace);
		File propertiesFile = TargetPlatform.createPropertiesFile(plugins, tmpWorkspacePath);
		String[] classpath = constructClasspath(plugins);
		String progArgs= getProgramArguments(configuration);

		if (classpath == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDEPlugin.PLUGIN_ID, IStatus.ERROR, "Could not compute classpath.", null));
		}
		VMRunnerConfiguration vmConfig= new VMRunnerConfiguration("EclipseRuntimeLauncher", classpath);
					
		Vector argv= new Vector(20);
		argv.add(appName);
		argv.add(propertiesFile.getPath());

		// insert the program arguments
		ExecutionArguments execArgs = new ExecutionArguments("", progArgs); //$NON-NLS-1$
		String[] pa= execArgs.getProgramArgumentsArray();
		for (int i= 0; i < pa.length; i++) {
			argv.add(pa[i]);
		}

		argv.add("-data");
		argv.add(workspace);
		
		argv.add("-dev");
		argv.add(getBuildOutputFolders(plugins));
		
		String[] args= new String[argv.size()];
		argv.copyInto(args);
		vmConfig.setProgramArguments(args);
		return vmConfig;
	}

	private PluginRegistryModel getPluginRegistryModel(IJavaProject project) throws IOException {
		MultiStatus status= new MultiStatus("JUnit PDE Launcher", IStatus.ERROR, "Error scanning plugin.xml", null);
		Factory factory= new Factory(status);
		URL url= project.getProject().getLocation().toFile().toURL();
		url= Platform.asLocalURL(url);
		url= new URL(url, "plugin.xml");
		PluginRegistryModel pluginRegistryModel= Platform.parsePlugins(new URL[] {url}, factory);
		if (pluginRegistryModel != null && pluginRegistryModel.getPlugins().length == 1)
			return pluginRegistryModel;
		return null;
	}
	
	private String getTempLocation() {
		return System.getProperty("java.io.tmpdir") + File.separator;
	}

	private String getTempWorkSpaceLocation() {	
 		return getTempLocation() + "org.eclipse.pde.junit.workspace";		
	}
		
	private void deleteTestWorkspace(String workspace) throws IOException {
		deleteContent(new File(workspace));
	}
	
	private void deleteContent(File workspace) throws IOException {
		if (workspace.isDirectory()) {
			File[] children= workspace.listFiles();
			for (int i= 0; i < children.length; i++) {
				deleteContent(children[i]);
			}
		}
		workspace.delete();
	}

	IPluginModelBase[] getPlugins() {
		IPluginModelBase[] wsmodels= getWorkspacePlugins();
		ArrayList wsList = new ArrayList();
		for (int i = 0; i < wsmodels.length; i++) {
			IPluginModelBase model = wsmodels[i];
			wsList.add(model);
		}

		IPluginModelBase[] exmodels= getExternalPlugins();
		ArrayList exList = new ArrayList();
		for (int i = 0; i < exmodels.length; i++) {
			IPluginModelBase model = exmodels[i];
			//TODO: we should add at least the PDE JUnit and its pre-reqs
			//if (model.isEnabled())
				exList.add(model);	
		}
		
		ArrayList result = new ArrayList();
		mergeWithoutDuplicates(wsList, exList, result);
		IPluginModelBase[] plugins =
			(IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
		return plugins;
	}
	
	protected void setDefaultSourceLocator(ILaunch launch, ILaunchConfiguration configuration) throws CoreException {
		ISourceLocator sourceLocator = constructSourceLocator(getPlugins());
		launch.setSourceLocator(sourceLocator);					
	}
	
	// 
	// All methods below are copied from org.eclipse.pde.internal.ui.launcher.AdvancedLauncherTab 
	// TODO: request API from PDE
	static IPluginModelBase[] getWorkspacePlugins() {
		IPluginModelBase[] plugins =
			PDECore
				.getDefault()
				.getWorkspaceModelManager()
				.getWorkspacePluginModels();
		IPluginModelBase[] fragments =
			PDECore
				.getDefault()
				.getWorkspaceModelManager()
				.getWorkspaceFragmentModels();
		return getAllPlugins(plugins, fragments);
	}
	
	static IPluginModelBase[] getExternalPlugins() {
		IPluginModelBase[] plugins =
			PDECore.getDefault().getExternalModelManager().getModels();
		IPluginModelBase[] fragments =
			PDECore.getDefault().getExternalModelManager().getFragmentModels();
		return getAllPlugins(plugins, fragments);
	}

	static IPluginModelBase[] getAllPlugins(
		IPluginModelBase[] plugins,
		IPluginModelBase[] fragments) {
		IPluginModelBase[] all =
			new IPluginModelBase[plugins.length + fragments.length];
		System.arraycopy(plugins, 0, all, 0, plugins.length);
		System.arraycopy(fragments, 0, all, plugins.length, fragments.length);
		return all;
	}

	private void mergeWithoutDuplicates(ArrayList wsmodels, ArrayList exmodels, ArrayList result) {
		for (int i= 0; i < wsmodels.size(); i++) {
			result.add(wsmodels.get(i));
		}
		
		fDuplicates = new Vector();
		for (int i=0; i < exmodels.size(); i++) {
			IPluginModelBase exmodel = (IPluginModelBase)exmodels.get(i);
			boolean duplicate = false;
			for (int j= 0; j < wsmodels.size(); j++) {
				IPluginModelBase wsmodel = (IPluginModelBase)wsmodels.get(j);
				if (isDuplicate(wsmodel, exmodel)) {
					fDuplicates.add(exmodel.getPluginBase().getId() + " (" + exmodel.getPluginBase().getVersion() + ")");
					duplicate= true;
					break;
				} 
			}
			if (!duplicate)
				result.add(exmodel);
		}
	}

	private boolean isDuplicate(IPluginModelBase wsmodel, IPluginModelBase exmodel) {
		if (!wsmodel.isLoaded() || !exmodel.isLoaded()) 
			return false;
		String wsPluginId= wsmodel.getPluginBase().getId();
		String exPluginId= exmodel.getPluginBase().getId();
		if (wsPluginId == null)
			return false;
		return wsPluginId.equalsIgnoreCase(exPluginId);
	}
	
	/**
	 * Constructs a classpath with the slimlauncher and the boot plugin (org.eclipse.core.boot)
	 * If the boot project is in the workspace, the classpath used in the workspace is used.
	 */
	private String[] constructClasspath(IPluginModelBase[] plugins)
		throws CoreException {
		File slimLauncher =
			PDEPlugin.getFileInPlugin(new Path("launcher/slimlauncher.jar"));
		if (slimLauncher == null || !slimLauncher.exists()) {
			return null;
		}
		IPluginModelBase model= findModel("org.eclipse.core.boot", plugins);
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource != null) {
				// in workspace - use the java project
				IProject project = resource.getProject();
				IJavaProject jproject = JavaCore.create(project);
				String[] bootClassPath = JavaRuntime.computeDefaultRuntimeClassPath(jproject);
				if (bootClassPath != null) {
					String[] resClassPath = new String[bootClassPath.length + 1];
					resClassPath[0] = slimLauncher.getPath();
					System.arraycopy(bootClassPath, 0, resClassPath, 1, bootClassPath.length);
					return resClassPath;
				}
			} else {
				// outside - locate boot.jar
				String installLocation = model.getInstallLocation();
				if (installLocation.startsWith("file:"))
					installLocation = installLocation.substring(5);
				File bootJar = new File(installLocation, "boot.jar");
				if (bootJar.exists()) {
					return new String[] { slimLauncher.getPath(), bootJar.getPath()};
				}
				// Check PDE case (third instance) - it may be in the bin
				File binDir = new File(installLocation, "bin/");
				if (binDir.exists()) {
					return new String[] { slimLauncher.getPath(), binDir.getPath()};
				}
			}
		}
		// failed to construct the class path: boot plugin not existing or boot.jar not found
		return null;
	}
	
	private IPluginModelBase findModel(String id, IPluginModelBase[] models) {
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = (IPluginModelBase) models[i];
			if (model.getPluginBase().getId().equals(id))
				return model;
		}
		return null;
	}
	
	/**
	 * Constructs a source locator containg all projects selected as plugins.
	 */
	private ISourceLocator constructSourceLocator(IPluginModelBase[] plugins) throws CoreException {
		ArrayList javaProjects = new ArrayList(plugins.length);
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		for (int i = 0; i < plugins.length; i++) {
			try {
				File pluginDir =
					new File(new URL("file:" + plugins[i].getInstallLocation()).getFile());
				IContainer container =
					root.getContainerForLocation(new Path(pluginDir.getPath()));
				if (container instanceof IProject) {
					IProject project = (IProject) container;
					if (WorkspaceModelManager.isJavaPluginProject(project))
						javaProjects.add(JavaCore.create(project));
				}
			} catch (MalformedURLException e) {
				PDEPlugin.log(e);
			}
		}
		IJavaProject[] projs =
			(IJavaProject[]) javaProjects.toArray(new IJavaProject[javaProjects.size()]);
		return new JavaUISourceLocator(projs, false);
	}
	
	private String getBuildOutputFolders(IPluginModelBase[] plugins) {
		IPluginModelBase[] wsmodels = plugins;
		HashSet set = new HashSet();
		set.add("bin");
		for (int i = 0; i < wsmodels.length; i++) {
			IResource underlyingResource= wsmodels[i].getUnderlyingResource();
			if (underlyingResource != null) {
				IProject project= underlyingResource.getProject();
				try {
					if (project.hasNature(JavaCore.NATURE_ID)) {
						set.add(
							JavaCore
								.create(project)
								.getOutputLocation()
								.lastSegment());
					}
				} catch (JavaModelException e) {
				} catch (CoreException e) {
				}
			}
		}
		StringBuffer result = new StringBuffer();
		for (Iterator iter=set.iterator(); iter.hasNext();) {
			String folder = iter.next().toString();
			result.append(folder);
			if (iter.hasNext()) result.append(",");
		}
		return result.toString();
	}

	static String getDefaultWorkspace() {
		IPath ppath =
			new Path(
				PDECore.getDefault().getPluginPreferences().getString(
					ICoreConstants.PLATFORM_PATH));
		IPath runtimeWorkspace = ppath.append("runtime-test-workspace");
		return runtimeWorkspace.toOSString();



	}

}