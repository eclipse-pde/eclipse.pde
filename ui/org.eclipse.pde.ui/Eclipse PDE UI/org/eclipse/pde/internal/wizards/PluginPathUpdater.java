package org.eclipse.pde.internal.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.model.*;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.plugin.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.*;
import java.util.*;
import java.io.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.preferences.*;


public class PluginPathUpdater {
	public static final String KEY_UPDATING = "PluginPathUpdater.updating";
	public static final String PROP_JDK= "org.eclipse.jdt.ui.build.jdk.library";
	public static final String JDK_VAR= "JRE_LIB";
	public static final String JDK_SRCVAR= "JRE_SRC";
	public static final String JDK_SRCROOTVAR= "JRE_SRCROOT";
	public static final String PROP_JDK_SOURCE= "org.eclipse.jdt.ui.build.jdk.source";
	public static final String PROP_JDK_PREFIX = "org.eclipse.jdt.ui.build.jdk.prefix";
	private IProject project;
	private Iterator checkedPlugins;
	private IJavaProject javaProject;

	public static class CheckedPlugin {
		private boolean checked;
		private IPlugin info;
		public CheckedPlugin(IPlugin info, boolean checked) {
			this.info = info;
			this.checked = checked;
		}
		public IPlugin getPluginInfo() {
			return info;
		}
		public boolean isChecked() {
			return checked;
		}
	}

public PluginPathUpdater(IProject project, Iterator checkedPlugins) {
	this.project = project;
	this.checkedPlugins = checkedPlugins;
}
private void addFoldersToClasspathEntries(IPluginModelBase model, Vector result) {
	IFile file = (IFile) model.getUnderlyingResource();
	IPath jarsPath = file.getProject().getFullPath().append("plugin.jars");
	IFile jarsFile = file.getWorkspace().getRoot().getFile(jarsPath);
	if (!jarsFile.exists())
		return;
	WorkspaceModelManager manager = PDEPlugin.getDefault().getWorkspaceModelManager();
	manager.connect(jarsFile, null, false);
	IJarsModel jarsModel =
		(IJarsModel) manager.getModel(jarsFile, null);
	IJars jars = jarsModel.getJars();
	IJarEntry[] entries = jars.getJarEntries();
	for (int i = 0; i < entries.length; i++) {
		IJarEntry entry = entries[i];
		String[] folderNames = entry.getFolderNames();
		for (int j = 0; j < folderNames.length; j++) {
			String folderName = folderNames[j];
			IPath folderPath = file.getProject().getFullPath().append(folderName);
			if (file.getWorkspace().getRoot().exists(folderPath)) {
				result.add(JavaCore.newSourceEntry(folderPath));
			}
		}
	}
	manager.disconnect(jarsFile, null);
}
private void addToClasspathEntries(CheckedPlugin element, Vector result) {
	IPlugin plugin = element.getPluginInfo();
	IPluginModelBase model = plugin.getModel();
	boolean internal = model.getUnderlyingResource() != null;

	if (internal) {
		//addFoldersToClasspathEntries(model, result);
		IPath projectPath = model.getUnderlyingResource().getProject().getFullPath();
		IClasspathEntry projectEntry = JavaCore.newProjectEntry(projectPath);
		result.addElement(projectEntry);
		return;
	}
	IPath modelPath = new Path(PDEPlugin.ECLIPSE_HOME_VARIABLE);
	modelPath = modelPath.append(((ExternalPluginModelBase)model).getEclipseHomeRelativePath());

	IPluginLibrary[] libraries = plugin.getLibraries();

	for (int i = 0; i < libraries.length; i++) {
		IPluginLibrary library = libraries[i];
		IPath libraryPath = modelPath.append(library.getName());
		IPath [] sourceAnnot = getSourceAnnotation(libraryPath, library);

		IClasspathEntry libraryEntry = JavaCore.newVariableEntry(libraryPath, sourceAnnot[0], sourceAnnot[1]);
		result.addElement(libraryEntry);
	}
}
public IClasspathEntry [] getClasspathEntries() {
	Vector result = new Vector();

	for (Iterator iter=checkedPlugins; iter.hasNext();) {
		CheckedPlugin element = (CheckedPlugin) iter.next();
		if (element.isChecked()) {
		   addToClasspathEntries(element, result);
		}
	}
	IClasspathEntry[] finalEntries = new IClasspathEntry[result.size()];
	result.copyInto(finalEntries);
	return finalEntries;
}
private IJarsModel getJarsModel(IPlugin plugin) {
	IFile file = (IFile)plugin.getModel().getUnderlyingResource();
	IPath jarsPath = file.getProject().getFullPath().append("plugin.jars");
	IFile jarsFile = file.getWorkspace().getRoot().getFile(jarsPath);
	if (!jarsFile.exists())
		return null;
	WorkspaceModelManager manager = PDEPlugin.getDefault().getWorkspaceModelManager();
	manager.connect(jarsFile, null, false);
	IJarsModel jarsModel =
		(IJarsModel) manager.getModel(jarsFile, null);
	return jarsModel;
}
public static IPath getJDKPath() {
	return JavaCore.getClasspathVariable(JDK_VAR);
}
public static IPath [] getJDKSourceAnnotation() {
	IPath source = JavaCore.getClasspathVariable(JDK_SRCVAR);
	IPath prefix = JavaCore.getClasspathVariable(JDK_SRCROOTVAR);
	return new IPath [] { source, prefix };
}
public IRunnableWithProgress getOperation() {
	return new WorkspaceModifyOperation() {
		public void execute(IProgressMonitor monitor) {
			monitor.beginTask(
				PDEPlugin.getResourceString(KEY_UPDATING),
				IProgressMonitor.UNKNOWN);
			updateClasspath(monitor);
		}
	};
}
private IPath[] getSourceAnnotation(IPath libraryPath, IPluginLibrary library) {
	IPath [] annot = new IPath[2];
	annot[0] = new Path(libraryPath.removeFileExtension().toString()+"src.zip");
	return annot;
}
public IClasspathEntry [] getSourceClasspathEntries(IPluginModel model) {
	Vector result = new Vector();

	addFoldersToClasspathEntries(model, result);
	IClasspathEntry[] finalEntries = new IClasspathEntry[result.size()];
	result.copyInto(finalEntries);
	return finalEntries;
}
public static boolean isAlreadyPresent(
	IClasspathEntry[] oldEntries,
	IClasspathEntry entry) {
	for (int i = 0; i < oldEntries.length; i++) {
		IClasspathEntry oldEntry = oldEntries[i];
		if (oldEntry.getContentKind() == entry.getContentKind()
			&& oldEntry.getEntryKind() == entry.getEntryKind()
			&& oldEntry.getPath().equals(entry.getPath())) {
			return true;
		}
	}
	return false;
}
private void releaseJarsModel(IJarsModel jarsModel) {
	WorkspaceModelManager manager = PDEPlugin.getDefault().getWorkspaceModelManager();
	IFile jarsFile = (IFile)jarsModel.getUnderlyingResource();
	manager.disconnect(jarsFile, null);
}
public void updateClasspath(IProgressMonitor monitor) {
	try {
		// create java nature
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
		}
		if (!project.hasNature(PDEPlugin.PLUGIN_NATURE)) {
			CoreUtility.addNatureToProject(project, PDEPlugin.PLUGIN_NATURE, monitor);
		}
		Vector result = new Vector();
		if (javaProject == null)
			javaProject = JavaCore.create(project);
		IClasspathEntry[] entries = javaProject.getRawClasspath();
		for (int i = 0; i < entries.length; i++)
			result.addElement(entries[i]);

		while (checkedPlugins.hasNext()) {
			CheckedPlugin element = (CheckedPlugin) checkedPlugins.next();
			updateLibrariesFor(element, entries, result);
		}
		IClasspathEntry[] finalEntries = new IClasspathEntry[result.size()];
		result.copyInto(finalEntries);
		javaProject.setRawClasspath(finalEntries, monitor);
	} catch (JavaModelException e) {
		PDEPlugin.logException(e);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
private void updateLibrariesFor(
	CheckedPlugin element,
	IClasspathEntry[] entries,
	Vector result) {
	IPlugin plugin = element.getPluginInfo();
	boolean internal = plugin.getModel().getUnderlyingResource() != null;
	boolean add = element.isChecked();
	IPluginLibrary[] libraries = plugin.getLibraries();
	IJarsModel jarsModel = null;
	if (internal) {
		jarsModel = getJarsModel(plugin);
	}
	for (int i = 0; i < libraries.length; i++) {
		IPluginLibrary library = libraries[i];
		if (internal) {
			updateLibrary(plugin, jarsModel, library.getName(), add, entries, result);
		} else {
			updateLibrary(
				((ExternalPluginModelBase)plugin.getModel()).getEclipseHomeRelativePath(),
				library.getName(),
				add,
				entries,
				result);
		}
	}
	if (jarsModel!=null) {
		releaseJarsModel(jarsModel);
	}
}
private void updateLibrary(
	IPath relativePath,
	String name,
	boolean add,
	IClasspathEntry[] entries,
	Vector result) {
	IPath basePath = new Path(PDEPlugin.ECLIPSE_HOME_VARIABLE).append(relativePath);
	IPath libraryPath = basePath.append(name);
	// Search for this entry
	IClasspathEntry libraryEntry = null;

	for (int i = 0; i < entries.length; i++) {
		IClasspathEntry entry = entries[i];
		if (entry.getEntryKind() != IClasspathEntry.CPE_VARIABLE)
			continue;
		IPath path = entry.getPath();
		if (path.equals(libraryPath)) {
			libraryEntry = entry;
			break;
		}
	}
	if (libraryEntry != null) {
		// already exists
		if (add) {
			// do nothing
		} else {
			// remove it
			result.remove(libraryEntry);
		}
	} else
		if (add) {
			IPath[] sourceAnnot = getSourceAnnotation(libraryPath, null);
			libraryEntry =
				JavaCore.newVariableEntry(libraryPath, sourceAnnot[0], sourceAnnot[1]);
			result.addElement(libraryEntry);
		}
}
private void updateLibrary(
	IPlugin plugin,
	IJarsModel jarsModel,
	String name,
	boolean add,
	IClasspathEntry[] entries,
	Vector result) {
	IPath projectPath =
		plugin.getModel().getUnderlyingResource().getProject().getFullPath();
	// Add or remove project reference
	IClasspathEntry projectEntry = null;
	for (int i = 0; i < entries.length; i++) {
		IClasspathEntry classpathEntry = entries[i];
		if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_PROJECT)
			continue;
		IPath path = classpathEntry.getPath();
		if (path.equals(projectPath)) {
			projectEntry = classpathEntry;
			break;
		}
	}
	if (projectEntry != null) {
		// already exists
		if (add) {
			// do nothing
		} else {
			// remove it
			result.remove(projectEntry);
		}
	} else
		if (add) {
			projectEntry = JavaCore.newProjectEntry(projectPath);
			result.addElement(projectEntry);
		}
}
private boolean updateSourceFolder(
	IPlugin plugin,
	String folderName,
	boolean add,
	IClasspathEntry[] entries,
	Vector result) {
	IProject project = plugin.getModel().getUnderlyingResource().getProject();
	IPath folderPath = project.getFullPath().append(folderName);
	IClasspathEntry folderEntry = null;

	for (int i = 0; i < entries.length; i++) {
		IClasspathEntry classpathEntry = entries[i];
		if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_SOURCE)
			continue;
		IPath path = classpathEntry.getPath();
		if (path.equals(folderPath)) {
			folderEntry = classpathEntry;
			break;
		}
	}
	if (folderEntry != null) {
		// already exists
		if (add) {
			// do nothing
		} else {
			// remove it
			result.remove(folderEntry);
		}
	} else
		if (add) {
			folderEntry = JavaCore.newSourceEntry(folderPath);
			result.addElement(folderEntry);
		}
	return folderEntry!=null;
}
}
