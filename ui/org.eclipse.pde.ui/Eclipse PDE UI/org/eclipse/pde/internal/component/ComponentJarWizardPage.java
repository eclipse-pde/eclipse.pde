package org.eclipse.pde.internal.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ant.internal.ui.*;
import org.eclipse.ant.core.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.util.*;
import org.eclipse.pde.internal.core.*;
import java.io.*;
import java.lang.reflect.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.swt.events.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.model.component.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.launcher.*;
import org.eclipse.pde.internal.preferences.*;
import org.apache.tools.ant.*;

public class ComponentJarWizardPage extends WizardPage {
	private IFile componentFile;
	private Button makeScriptsButton;
	private BuildConsoleViewer console;
	private Button makeJarsButton;
	private WorkspaceComponentModel model;
	private Vector logMessages=new Vector();
	private String buildFileName = "build.xml";

	public static final String WIZARD_DESC = "ComponentJarWizard.description";
	public static final String BUILDERS_UPDATING = "Builders.updating";
	public static final String WIZARD_TITLE = "ComponentJarWizard.title";
	public static final String WIZARD_GROUP = "ComponentJarWizard.group";
	public static final String WIZARD_RUNNING = "ComponentJarWizard.running";
	public static final String WIZARD_GENERATING = "ComponentJarWizard.generating";
	public static final String WIZARD_GENERATE_SCRIPTS = "ComponentJarWizard.generateScripts";
	public static final String WIZARD_GENERATE_JARS = "ComponentJarWizard.generateJars";

	private static final String PREFIX =
		PDEPlugin.getDefault().getPluginId() + ".componentJar.";
	private static final String F_MAKE_JARS = PREFIX + "makeJars";
	private static final String F_MAKE_SCRIPTS = PREFIX + "makeScripts";

	class JarsBuildListener extends UIBuildListener {
		IProgressMonitor monitor;
		public JarsBuildListener(IProgressMonitor monitor, IFile file) {
			super(monitor, file);
			this.monitor = monitor;
		}
		public void messageLogged(BuildEvent event) {
			if (monitor.isCanceled())
				throw new BuildCanceledException();
			if (event.getPriority() <= Project.MSG_INFO) {
				asyncLogMessage(event.getMessage());
			}
		}
	}

public ComponentJarWizardPage(IFile componentFile) {
	super("componentJar");
	setTitle(PDEPlugin.getResourceString(WIZARD_TITLE));
	setDescription(PDEPlugin.getResourceString(WIZARD_DESC));
	this.componentFile = componentFile;
	model = new WorkspaceComponentModel(componentFile);
	model.load();
}
private void asyncLogMessage(final String message) {
	/*
	getContainer().getShell().getDisplay().asyncExec(new Runnable() {
		public void run() {
			console.append(message);
		}
	});
	*/
	console.append(message);
	logMessages.add(message);
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	container.setLayout(layout);

	Group group = new Group(container, SWT.SHADOW_ETCHED_IN);
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	layout = new GridLayout();
	group.setLayout(layout);
	group.setLayoutData(gd);
	group.setText(PDEPlugin.getResourceString(WIZARD_GROUP));

	makeScriptsButton = new Button(group, SWT.CHECK);
	makeScriptsButton.setText(PDEPlugin.getResourceString(WIZARD_GENERATE_SCRIPTS));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	makeScriptsButton.setLayoutData(gd);
	makeScriptsButton.setSelection(true);

	makeJarsButton = new Button(group, SWT.CHECK);
	makeJarsButton.setText(PDEPlugin.getResourceString(WIZARD_GENERATE_JARS));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	makeJarsButton.setLayoutData(gd);
	makeJarsButton.setSelection(true);

	console = new BuildConsoleViewer(container);
	gd = new GridData(GridData.FILL_BOTH);
	console.getControl().setLayoutData(gd);
	console.getControl().setVisible(false);
	setControl(container);
	loadSettings();
}

private String createPluginPath(String eclipseDir) {
	IPath stateLocation = PDEPlugin.getDefault().getStateLocation();

	File file = stateLocation.append("component_plugin_path.properties").toFile();

	String fileName = file.getAbsolutePath();
	try {
		OutputStream stream = new FileOutputStream(file);
		PrintWriter writer = new PrintWriter(stream);
		String projectPath = Platform.getLocation().toOSString();
		WorkbenchLauncher.addExternalModels(writer, eclipseDir);
		WorkbenchLauncher.addWorkspaceModels(writer);
		writer.flush();
		writer.close();
	} catch (IOException e) {
		return null;
	}
	return fileName;
}

public boolean finish() {
	saveSettings();
	final boolean makeScripts = makeScriptsButton.getSelection();
	final boolean makeJars = makeJarsButton.getSelection();

	IRunnableWithProgress operation = new WorkspaceModifyOperation() {
		public void execute(IProgressMonitor monitor) {
			try {
				runOperation(makeScripts, makeJars, monitor);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} finally {
				monitor.done();
			}
		}
	};
	logMessages.clear();
	console.clearDocument();
	console.getControl().setVisible(true);
	try {
		getContainer().run(false, true, operation);
	} catch (InvocationTargetException e) {
		PDEPlugin.logException(e);
		return false;
	} catch (InterruptedException e) {
		PDEPlugin.logException(e);
		return false;
	}
	return true;
}
private void loadSettings() {
	IDialogSettings settings = getDialogSettings();
	if (settings.get(F_MAKE_SCRIPTS) != null)
		makeScriptsButton.setSelection(settings.getBoolean(F_MAKE_SCRIPTS));
	if (settings.get(F_MAKE_JARS) != null)
		makeJarsButton.setSelection(settings.getBoolean(F_MAKE_JARS));
}
private void logActivity() {
	//ResourcesPlugin.getPlugin().getLog().log(errors);
}
private void makeJars(IProgressMonitor monitor)
	throws CoreException, InvocationTargetException {
	IFile buildFile = null;

	IPath path =
		componentFile.getFullPath().removeLastSegments(1).append(buildFileName);
	IFile file = PDEPlugin.getWorkspace().getRoot().getFile(path);
	if (!file.exists())
		return;

	String[] args = { "-buildfile", file.getLocation().toOSString()};
	monitor.beginTask(
		PDEPlugin.getResourceString(WIZARD_RUNNING),
		IProgressMonitor.UNKNOWN);

	try {
		//TBD: should remove the build listener somehow
		new AntRunner().run(args, new JarsBuildListener(monitor, file));
	} catch (BuildCanceledException e) {
		// build was canceled don't propagate exception
		return;
	} catch (Exception e) {
		throw new InvocationTargetException(e);
	}
}
private void makeScripts(IProgressMonitor monitor) throws CoreException {
	ComponentBuildScriptGenerator generator = new ComponentBuildScriptGenerator();
	Vector args = new Vector();

	String eclipseDir =
		PDEPlugin.getDefault().getPreferenceStore().getString(
			PDEBasePreferencePage.PROP_PLATFORM_PATH);
	//eclipseDir += File.separator + "eclipse";
	String pluginPath = createPluginPath(eclipseDir);

	IPath platform =
		Platform.getLocation().append(
			model.getUnderlyingResource().getProject().getName());

	args.add("-install");
	args.add(platform.toOSString());

	args.add("-plugins");
	args.add(pluginPath);

	String name =
		model.getComponent().getId() + "_" + model.getComponent().getVersion();

	args.add("-component");
	//args.add(name);
	args.add(model.getComponent().getId());
	try {
		monitor.subTask(PDEPlugin.getResourceString(WIZARD_GENERATING));
		generator.run(args.toArray(new String[args.size()]));
		monitor.subTask(PDEPlugin.getResourceString(BUILDERS_UPDATING));
	} catch (Exception e) {
		PDEPlugin.logException(e);
	}
}

private void refreshLocal(IComponentReference[] references, IProgressMonitor monitor)
	throws CoreException {
	for (int i = 0; i < references.length; i++) {
		IComponentReference ref = references[i];
		IPluginModelBase refmodel = model.getComponent().getReferencedModel(ref);
		if (refmodel!=null) {
			refmodel.getUnderlyingResource().getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
	}
}
private void refreshLocal(IProgressMonitor monitor) throws CoreException {
	// refresh component
	componentFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
	// refresh references
	IComponent component = model.getComponent();
	refreshLocal(component.getPlugins(), monitor);
	refreshLocal(component.getFragments(), monitor);
	
}
private void runOperation(
	boolean makeScripts,
	boolean makeJars,
	IProgressMonitor monitor)
	throws CoreException, InvocationTargetException {
	if (makeScripts) {
		makeScripts(monitor);
		monitor.subTask(PDEPlugin.getResourceString(BUILDERS_UPDATING));
		refreshLocal(monitor);
	}
	if (makeJars) {
		makeJars(monitor);
		monitor.subTask(PDEPlugin.getResourceString(BUILDERS_UPDATING));
		refreshLocal(monitor);
	}
	if (logMessages.size()>0) {
		logActivity();
	}
}
private void saveSettings() {
	IDialogSettings settings = getDialogSettings();
	settings.put(F_MAKE_SCRIPTS, makeScriptsButton.getSelection());
	settings.put(F_MAKE_JARS, makeJarsButton.getSelection());
}
}
