package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.util.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.base.model.*;

import java.util.*;
import org.eclipse.ui.part.*;

import java.lang.reflect.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;

import java.io.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.views.navigator.*;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.base.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.events.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.editor.manifest.*;
import org.eclipse.pde.internal.editor.*;

import java.util.List;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.pde.internal.preferences.*;


public class DefaultCodeGenerationPage extends WizardPage {
	private static final String RUNTIME_ID = "org.eclipse.core.runtime";
	private static final String RESOURCES_ID = "org.eclipse.core.resources";
	private static final String WORKBENCH_ID = "org.eclipse.ui";
	private static final String KEY_TITLE = "DefaultCodeGenerationPage.title";
	private static final String KEY_FTITLE = "DefaultCodeGenerationPage.ftitle";
	private static final String KEY_ID_NOT_SET = "DefaultCodeGenerationPage.idNotSet";
	private static final String KEY_VERSION_FORMAT = "DefaultCodeGenerationPage.versionFormat";
	private static final String KEY_INVALID_ID = "DefaultCodeGenerationPage.invalidId";
	private static final String KEY_DESC = "DefaultCodeGenerationPage.desc";
	private static final String KEY_FDESC = "DefaultCodeGenerationPage.fdesc";
	private static final String KEY_FNAME = "DefaultCodeGenerationPage.fname";
	private static final String KEY_NAME = "DefaultCodeGenerationPage.name";
	private static final String KEY_VERSION = "DefaultCodeGenerationPage.version";
	private static final String KEY_PROVIDER =
		"DefaultCodeGenerationPage.providerName";
	private static final String KEY_PLUGIN_ID =
		"DefaultCodeGenerationPage.pluginId";
	private static final String KEY_BROWSE =
		"DefaultCodeGenerationPage.pluginId.browse";
	private static final String KEY_PLUGIN_VERSION =
		"DefaultCodeGenerationPage.pluginVersion";
	private static final String KEY_CLASS = "DefaultCodeGenerationPage.class";
	private static final String KEY_GENERATE = "DefaultCodeGenerationPage.generate";
	private static final String KEY_INITIAL_NAME = "DefaultCodeGenerationPage.initialName";
	//private static final String KEY_INITIAL_NAME = "DefaultCodeGenerationPage.initialName.nl";
	private static final String KEY_INITIAL_FNAME = "DefaultCodeGenerationPage.initialFName";
	//private static final String KEY_INITIAL_FNAME = "DefaultCodeGenerationPage.initialFName.nl";
	private static final String KEY_CREATING = "DefaultCodeGenerationPage.creating";
	private static final String KEY_CREATING_PLUGIN = "DefaultCodeGenerationPage.creatingPlugin";
	private static final String KEY_CREATING_FRAGMENT = "DefaultCodeGenerationPage.creatingFragment";
	private static final String KEY_OPTIONS = "DefaultCodeGenerationPage.options";
	private static final String KEY_OPTIONS_THIS =
		"DefaultCodeGenerationPage.options.this";
	private static final String KEY_OPTIONS_BUNDLE =
		"DefaultCodeGenerationPage.options.bundle";
	private static final String KEY_OPTIONS_WORKSPACE =
		"DefaultCodeGenerationPage.options.workspace";

	private Text nameField;
	private Text requiresField;
	private Text pluginVersionField;
	private Text pluginIdField;
	private Button thisCheck;
	private Button bundleCheck;
	private Button workspaceCheck;
	private Button requiresButton;
	private Text versionField;
	private boolean fragment;
	//private Text libraryField;
	private Text providerField;
	private Text classField;
	private Button generateMainClass;
	private IProjectProvider projectProvider;
	private IPluginStructureData structureData;
	private static final String PLUGIN_REQUIRES_EXPORT = "export";
	private static final String MANIFEST_FILE_NAME = "plugin.xml";

	private class FieldData {
		String name;
		String[] requires;
		IPluginStructureData structureData;
		String version;
		String pluginId;
		String pluginVersion;
		String provider;
		String className;
		boolean thisCheck;
		boolean bundleCheck;
		boolean workspaceCheck;
	}

public DefaultCodeGenerationPage(
	IProjectProvider projectProvider,
	IPluginStructureData structureData,
	boolean fragment) {
	super("DefaultCodeGenerationPage");
	this.fragment = fragment;
	if (fragment) {
		setTitle(PDEPlugin.getResourceString(KEY_FTITLE));
		setDescription(PDEPlugin.getResourceString(KEY_FDESC));
	} else {
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
	}
	this.projectProvider = projectProvider;
	this.structureData = structureData;
}
private void appendAttribute(PrintWriter writer, String indent, String attName, String text) {
	appendAttribute(writer, indent, attName, text, false);
}
private void appendAttribute(PrintWriter writer, String indent, String attName, String text, boolean last) {
	if (text.length()>0) {
	   writer.print(indent + attName + "=\""+text+"\"");
	}
	if (last) writer.print(">");
	if (last || text.length()>0) writer.println();
}
	public void becomesVisible(int event){
		nameField.setFocus();
	}
private void browsePluginId() {
	BusyIndicator.showWhile(pluginIdField.getDisplay(), new Runnable() {
		public void run() {
			PluginSelectionDialog dialog =
				new PluginSelectionDialog(pluginIdField.getShell());
			dialog.create();
			dialog.getShell().setSize(350, 400);
			if (dialog.open() == PluginSelectionDialog.OK) {
				Object[] result = dialog.getResult();
				if (result != null && result.length == 1) {
					IPluginModel model = (IPluginModel) result[0];
					IPlugin plugin = model.getPlugin();
					pluginIdField.setText(plugin.getId());
					pluginVersionField.setText(plugin.getVersion());
				}
			}
		}
	});
}
private void copyTargetPluginImports(IPlugin targetPlugin, Vector missing) {
	missing.addElement(new PluginPathUpdater.CheckedPlugin(targetPlugin, true));
	IPluginImport[] imports = targetPlugin.getImports();
	for (int i = 0; i < imports.length; i++) {
		IPluginImport iimport = imports[i];
		IPlugin importPlugin = PDEPlugin.getDefault().findPlugin(iimport.getId());
		if (importPlugin != null)
			missing.addElement(new PluginPathUpdater.CheckedPlugin(importPlugin, true));
	}
}
private Button createCheck(Composite parent, String label, boolean state) {
	Button check = new Button(parent, SWT.CHECK);
	check.setText(label);
	GridData gd = new GridData();
	gd.horizontalAlignment = GridData.FILL;
	check.setLayoutData(gd);
	check.setSelection(state);
	return check;
}
public void createControl(Composite parent) {
	GridLayout layout = new GridLayout();
	Composite container = new Composite(parent, SWT.NONE);
	layout.numColumns = 3;
	layout.verticalSpacing = 9;
	container.setLayout(layout);

	String label = fragment ? PDEPlugin.getResourceString(KEY_FNAME) : PDEPlugin.getResourceString(KEY_NAME);
	nameField = createField(container, label);
	versionField = createField(container, PDEPlugin.getResourceString(KEY_VERSION));
	versionField.setText("1.0.0");
	versionField.addModifyListener(new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			if (isVersionValid()==false) {
				setPageComplete(false);
				setErrorMessage(PDEPlugin.getResourceString(KEY_VERSION_FORMAT));
			}
		    else if (fragment) verifyPluginFields();
		    else {
		    	setPageComplete(true);
		    	setErrorMessage(null);
		    }
		}
	});
	providerField = createField(container, PDEPlugin.getResourceString(KEY_PROVIDER));
	if (fragment) {
		pluginIdField = createField(container, PDEPlugin.getResourceString(KEY_PLUGIN_ID), false);
		pluginIdField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyPluginFields();
			}
		});
		Button browsePluginButton = new Button(container, SWT.PUSH);
		browsePluginButton.setText(PDEPlugin.getResourceString(KEY_BROWSE));
		browsePluginButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browsePluginId();
			}
		});
		pluginVersionField = createField(container, PDEPlugin.getResourceString(KEY_PLUGIN_VERSION));
		pluginVersionField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyPluginFields();
			}
		});
		verifyPluginFields();
	} else
		classField = createField(container, PDEPlugin.getResourceString(KEY_CLASS));

	if (!fragment) {
		new Label(container, SWT.NONE);
		generateMainClass = new Button(container, SWT.CHECK);
		generateMainClass.setText(PDEPlugin.getResourceString(KEY_GENERATE));
		generateMainClass.setSelection(true);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		generateMainClass.setLayoutData(gd);
		generateMainClass.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean value = generateMainClass.getSelection();
				thisCheck.setEnabled(value);
				bundleCheck.setEnabled(value);
				workspaceCheck.setEnabled(value);
			}
		});

		gd = new GridData();
		gd.horizontalSpan = 3;
		new Label(container, SWT.NONE).setLayoutData(gd);
		Group checkGroup = new Group(container, SWT.NONE);
		checkGroup.setText(PDEPlugin.getResourceString(KEY_OPTIONS));
		GridLayout cl = new GridLayout();
		checkGroup.setLayout(cl);
		gd = new GridData();
		gd.horizontalSpan = 3;
		gd.horizontalAlignment = GridData.FILL;
		checkGroup.setLayoutData(gd);
		thisCheck = createCheck(checkGroup, PDEPlugin.getResourceString(KEY_OPTIONS_THIS), true);
		bundleCheck = createCheck(checkGroup, PDEPlugin.getResourceString(KEY_OPTIONS_BUNDLE), true);
		workspaceCheck = createCheck(checkGroup, PDEPlugin.getResourceString(KEY_OPTIONS_WORKSPACE), true);
	}
	presetFields();
	setControl(container);
}
private Text createField(Composite parent, String label) {
	return createField(parent, label, true);
}
private Text createField(Composite parent, String label, boolean addFiller) {
	Label l = new Label(parent, SWT.NONE);
	l.setText(label);
	GridData gd = new GridData();
	gd.horizontalAlignment = GridData.BEGINNING;
	l.setLayoutData(gd);

	Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
	gd = new GridData();
	gd.horizontalAlignment = GridData.FILL;
	gd.grabExcessHorizontalSpace = true;
	text.setLayoutData(gd);

	if (addFiller) {
		Label filler = new Label(parent, SWT.NONE);
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		filler.setLayoutData(gd);
	}
	return text;
}

private boolean isVersionValid() {
	String version = versionField.getText();
	try {
	   PluginVersionIdentifier pid = new PluginVersionIdentifier(version);
	}
	catch (Exception e) {
		return false;
	}
	return true;
}

private IFile createFile(IContainer parent, String contents, IProgressMonitor monitor) throws CoreException {
	// create the new file resource
	IWorkspace workspace = parent.getWorkspace();
	String fileName = fragment ? "fragment.xml" : "plugin.xml";
	IPath filePath = parent.getFullPath().append(fileName);
	IFile file = workspace.getRoot().getFile(filePath);

	try{
		InputStream initialContents = new ByteArrayInputStream(contents.getBytes("UTF8"));
		file.create(initialContents, false, monitor);
	}catch(UnsupportedEncodingException uee){
		PDEPlugin.logException(uee);
	}
	IWorkbench workbench = PlatformUI.getWorkbench();
	workbench.getEditorRegistry().setDefaultEditor(file, PDEPlugin.MANIFEST_EDITOR_ID);
	return file;
}
private void createProject(IProject project, IProgressMonitor monitor) throws CoreException {
	if (project.exists() == false) {
		CoreUtility.createProject(project, projectProvider.getLocationPath(),
											monitor);		
		project.open(monitor);
	}
	if (!project.hasNature(JavaCore.NATURE_ID))
		CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
	if (!project.hasNature(PDEPlugin.PLUGIN_NATURE))
		CoreUtility.addNatureToProject(project, PDEPlugin.PLUGIN_NATURE, monitor);
	PDEPlugin.registerPlatformLaunchers(project);
}
public boolean finish() {
	final IProject project = (IProject) projectProvider.getProject();
	final boolean doMainClass = !fragment && generateMainClass.getSelection();
	final FieldData data = initializeFieldData();
	IRunnableWithProgress operation = new WorkspaceModifyOperation() {
		public void execute(IProgressMonitor monitor) {
			try {
				createProject(project, monitor);
				setJavaSettings(data, project, monitor);
				IFile file = generatePluginFile(project, data, monitor);
				if (doMainClass) {
					generatePluginClass(project, data, monitor);
				}
				openPluginFile(file);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			} finally {
				monitor.done();
			}
		}
	};
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
private void generatePluginClass(
	IProject project, 
	FieldData data, 
	IProgressMonitor monitor)
	throws CoreException {
	String fullyQualifiedClassName = data.className;
	if (fullyQualifiedClassName == null || fullyQualifiedClassName.length() == 0)
		return;
	int flags = 0;
	if (data.thisCheck) flags |= PluginClassCodeGenerator.F_THIS;
	if (data.workspaceCheck) flags |= PluginClassCodeGenerator.F_WORKSPACE;
	if (data.bundleCheck) flags |= PluginClassCodeGenerator.F_BUNDLES;
	String sourceFolder = data.structureData.getSourceFolderName();
	IPath folderPath = project.getFullPath().append(sourceFolder);
	IFolder folder = project.getWorkspace().getRoot().getFolder(folderPath);
	PluginClassCodeGenerator generator = 
		new PluginClassCodeGenerator(folder, fullyQualifiedClassName, flags); 

	monitor.subTask(PDEPlugin.getFormattedMessage(KEY_CREATING, fullyQualifiedClassName));
	generator.generate(monitor);
	monitor.done();
}
private IFile generatePluginFile(
	IProject project,
	FieldData data,
	IProgressMonitor monitor)
	throws CoreException {
	StringWriter sWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(sWriter);
	String indent = "   ";

	String message =
		fragment
			? PDEPlugin.getResourceString(KEY_CREATING_FRAGMENT)
			: PDEPlugin.getResourceString(KEY_CREATING_PLUGIN);
	monitor.subTask(message);
	writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	writer.println(fragment ? "<fragment" : "<plugin");
	appendAttribute(writer, indent, "name", data.name);
	appendAttribute(writer, indent, "id", data.structureData.getPluginId());
	appendAttribute(writer, indent, "version", data.version);
	appendAttribute(writer, indent, "provider-name", data.provider);
	if (!fragment)
		appendAttribute(writer, indent, "class", data.className, true);
	else {
		boolean last = data.pluginVersion.length() == 0;
		appendAttribute(writer, indent, "plugin-id", data.pluginId, last);
		if (!last)
			appendAttribute(writer, indent, "plugin-version", data.pluginVersion, true);
	}
	writer.println();

	if (!fragment && data.requires.length > 0) {
		writer.println(indent + "<requires>");
		String indent2 = indent + indent;
		for (int i = 0; i < data.requires.length; i++) {
			writer.println(indent2 + "<import plugin=\"" + data.requires[i] + "\"/>");
		}
		writer.println(indent + "</requires>");
		writer.println();
	}

	writer.println(indent + "<runtime>");
	String indent2 = indent + indent;
	writer.println(
		indent2
			+ "<library name=\""
			+ data.structureData.getRuntimeLibraryName()
			+ "\"/>");
	writer.println(indent + "</runtime>");
	writer.println();
	writer.println();
	writer.println(fragment ? "</fragment>" : "</plugin>");

	monitor.done();
	writer.close();

	IFile file = createFile(project, sWriter.toString(), monitor);
	monitor.done();
	return file;
}
private FieldData initializeFieldData() {
	FieldData data = new FieldData();
	data.name = nameField.getText();
	data.structureData = structureData;
	PluginVersionIdentifier pvi = new PluginVersionIdentifier(versionField.getText());
	data.version = pvi.toString();
	data.provider = providerField.getText();
	if (fragment) {
		data.pluginId = pluginIdField.getText();
		try {
		    PluginVersionIdentifier fvi = new PluginVersionIdentifier(pluginVersionField.getText());
		    data.pluginVersion = fvi.toString();
		}
		catch (NumberFormatException e) {
			data.pluginVersion = pluginVersionField.getText();
		}
	} else {
		data.className = classField.getText();
		data.thisCheck = thisCheck.getSelection();
		data.bundleCheck = bundleCheck.getSelection();
		data.workspaceCheck = workspaceCheck.getSelection();
	}
	return data;
}
private void openPluginFile(final IFile file) {
	final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();

	Display d = ww.getShell().getDisplay();
	d.asyncExec(new Runnable() {
		public void run() {
			try {
				String editorId =
					fragment ? PDEPlugin.FRAGMENT_EDITOR_ID : PDEPlugin.MANIFEST_EDITOR_ID;
				ww.getActivePage().openEditor(file, editorId);
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
			}
		}
	});
}
private void presetFields() {
	String name = projectProvider.getProjectName();
	int loc = name.lastIndexOf('.');
	String lastSegment = name;
	if (loc != -1) {
		StringBuffer buf = new StringBuffer(name.substring(loc + 1));
		buf.setCharAt(0, Character.toUpperCase(buf.charAt(0)));
		lastSegment = buf.toString();
	}

	if (!fragment) {
		classField.setText(name + "." + lastSegment.toString() + "Plugin");
	}
	versionField.setText("1.0.0");
	if (fragment)
		nameField.setText(PDEPlugin.getFormattedMessage(KEY_INITIAL_FNAME, lastSegment));
	else
		nameField.setText(PDEPlugin.getFormattedMessage(KEY_INITIAL_NAME, lastSegment));

	loc = name.indexOf('.');
	if (loc == -1)
		return;
	String firstSegment = name.substring(0, loc);
	if (firstSegment.toLowerCase().equals("com")) {
		// This is a reverse URL - use second segment
		// as the vendor name
		String provider = name.substring(loc + 1);
		loc = provider.indexOf('.');
		if (loc != -1) {
		   provider = provider.substring(0, loc);
		   providerField.setText(provider.toUpperCase());
		}
		else {
			provider = "";
		}
	}
}
private void setJavaSettings(
	FieldData data,
	IProject project,
	IProgressMonitor monitor)
	throws CoreException, JavaModelException {
	Vector result = new Vector();
	Vector missing = new Vector();
	IJavaProject javaProject = JavaCore.create(project);

	if (fragment) {
		if (data.pluginId != null) {
			IPlugin plugin = PDEPlugin.getDefault().findPlugin(data.pluginId);
			copyTargetPluginImports(plugin, missing);
		}
	} else {
		IPlugin plugin = PDEPlugin.getDefault().findPlugin(RUNTIME_ID);
		if (plugin != null) {
			missing.addElement(new PluginPathUpdater.CheckedPlugin(plugin, true));
			result.addElement(RUNTIME_ID);
		}
		plugin = PDEPlugin.getDefault().findPlugin(RESOURCES_ID);
		if (plugin != null) {
			missing.addElement(new PluginPathUpdater.CheckedPlugin(plugin, true));
			result.addElement(RESOURCES_ID);
		}
		plugin = PDEPlugin.getDefault().findPlugin(WORKBENCH_ID);
		if (plugin != null) {
			missing.addElement(new PluginPathUpdater.CheckedPlugin(plugin, true));
			result.addElement(WORKBENCH_ID);
		}
	}
	boolean updateBuildpath;
	
	if (fragment)
		updateBuildpath = BuildpathPreferencePage.isFragmentProjectUpdate();
	else 
		updateBuildpath = BuildpathPreferencePage.isPluginProjectUpdate();
	
	if (updateBuildpath) {
	   PluginPathUpdater updater = new PluginPathUpdater(project, missing.iterator());
	   IClasspathEntry[] libraries = updater.getClasspathEntries();
	   BuildPathUtil.setBuildPath(project, data.structureData, libraries, monitor);
	}
	if (!fragment) {
		String[] resultArray = new String[result.size()];
		result.copyInto(resultArray);
		data.requires = resultArray;
	}
}
private void verifyPluginFields() {
	if (pluginIdField.getText().length() == 0) {
		setErrorMessage(PDEPlugin.getResourceString(KEY_ID_NOT_SET));
		setPageComplete(false);
	} else {
		if (isPluginValid(pluginIdField.getText(),
		                          pluginVersionField.getText())) {
		   setPageComplete(true);
		   setErrorMessage(null);
		}
		else {
		   setPageComplete(false);
		   setErrorMessage(PDEPlugin.getResourceString(KEY_INVALID_ID));
		}
	}
}
private boolean isPluginValid(String pluginId, String pluginVersion) {
	IPlugin plugin = PDEPlugin.getDefault().findPlugin(pluginId, pluginVersion);
	return plugin != null;
}
}
