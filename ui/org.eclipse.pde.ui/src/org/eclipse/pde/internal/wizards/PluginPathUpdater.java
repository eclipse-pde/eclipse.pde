package org.eclipse.pde.internal.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.model.*;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.plugin.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.model.build.*;
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
import org.eclipse.pde.internal.util.*;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.pde.internal.base.model.*;

public class PluginPathUpdater {
	public static final String KEY_UPDATING = "PluginPathUpdater.updating";
	public static final String JRE_VAR = "JRE_LIB";
	public static final String JRE_SRCVAR = "JRE_SRC";
	public static final String JRE_SRCROOTVAR = "JRE_SRCROOT";
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
	private void addFoldersToClasspathEntries(
		IPluginModelBase model,
		Vector result) {
		IFile file = (IFile) model.getUnderlyingResource();
		IPath buildPath = file.getProject().getFullPath().append("build.properties");
		IFile buildFile = file.getWorkspace().getRoot().getFile(buildPath);
		if (!buildFile.exists())
			return;
		WorkspaceModelManager manager =
			PDEPlugin.getDefault().getWorkspaceModelManager();
		manager.connect(buildFile, null, false);
		IBuildModel buildModel = (IBuildModel) manager.getModel(buildFile, null);
		IBuild build = buildModel.getBuild();
		IBuildEntry[] entries = build.getBuildEntries();
		for (int i = 0; i < entries.length; i++) {
			IBuildEntry entry = entries[i];
			if (!entry.getName().startsWith("source."))
				continue;
			String[] tokens = entry.getTokens();
			for (int j = 0; j < tokens.length; j++) {
				String folderName = tokens[j];
				IPath folderPath = file.getProject().getFullPath().append(folderName);
				if (file.getWorkspace().getRoot().exists(folderPath)) {
					result.add(JavaCore.newSourceEntry(folderPath));
				}
			}
		}
		manager.disconnect(buildFile, null);
	}
	private static void addToClasspathEntries(
		CheckedPlugin element,
		Vector result) {
		IPlugin plugin = element.getPluginInfo();
		IPluginModelBase model = plugin.getModel();
		boolean internal = model.getUnderlyingResource() != null;

		if (internal) {
			IPath projectPath = model.getUnderlyingResource().getProject().getFullPath();
			if (!isEntryAdded(projectPath, IClasspathEntry.CPE_PROJECT, result)) {
				IClasspathEntry projectEntry = JavaCore.newProjectEntry(projectPath);
				result.addElement(projectEntry);
			}
			return;
		}
		IPath modelPath = new Path(PDEPlugin.ECLIPSE_HOME_VARIABLE);
		modelPath =
			modelPath.append(
				((ExternalPluginModelBase) model).getEclipseHomeRelativePath());

		IPluginLibrary[] libraries = plugin.getLibraries();

		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];

			String name = expandLibraryName(library.getName());
			IPath libraryPath = modelPath.append(name);
			IPath[] sourceAnnot = getSourceAnnotation(libraryPath, library);
			if (!isEntryAdded(libraryPath, IClasspathEntry.CPE_VARIABLE, result)) {
				IClasspathEntry libraryEntry =
					JavaCore.newVariableEntry(libraryPath, sourceAnnot[0], sourceAnnot[1]);
				IClasspathEntry resolved = JavaCore.getResolvedClasspathEntry(libraryEntry);
				if (resolved != null && resolved.getPath().toFile().exists())
					result.addElement(libraryEntry);
			}
		}
		// Recursively add reexported libraries
		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			// don't recurse if the library is not reexported
			if (iimport.isReexported() == false)
				continue;
			String id = iimport.getId();
			IPlugin reference = PDEPlugin.getDefault().findPlugin(id);
			if (reference != null) {
				CheckedPlugin ref = new CheckedPlugin(reference, true);
				addToClasspathEntries(ref, result);
			}
		}
	}

	public static IClasspathEntry createLibraryEntry(
		IPluginLibrary library,
		IPath rootPath) {
		String name = expandLibraryName(library.getName());
		IPath libraryPath = rootPath.append(name);
		IPath[] sourceAnnot = getSourceAnnotation(libraryPath, library);
		return JavaCore.newLibraryEntry(
			libraryPath,
			sourceAnnot[0],
			sourceAnnot[1],
			library.isFullyExported());
	}

	private static boolean isEntryAdded(IPath path, int kind, Vector entries) {
		for (int i = 0; i < entries.size(); i++) {
			IClasspathEntry entry = (IClasspathEntry) entries.elementAt(i);
			if (entry.getEntryKind() == kind) {
				if (entry.getPath().equals(path))
					return true;
			}
		}
		return false;
	}

	public void addClasspathEntries(Vector result) {
		for (Iterator iter = checkedPlugins; iter.hasNext();) {
			CheckedPlugin element = (CheckedPlugin) iter.next();
			if (element.isChecked()) {
				addToClasspathEntries(element, result);
			}
		}
	}

	public IClasspathEntry[] getClasspathEntries() {
		Vector result = new Vector();
		addClasspathEntries(result);
		IClasspathEntry[] finalEntries = new IClasspathEntry[result.size()];
		result.copyInto(finalEntries);
		return finalEntries;
	}
	public static IPath getJREPath() {
		return JavaCore.getClasspathVariable(JRE_VAR);
	}
	public static IPath[] getJRESourceAnnotation() {
		IPath source = JavaCore.getClasspathVariable(JRE_SRCVAR);
		IPath prefix = JavaCore.getClasspathVariable(JRE_SRCROOTVAR);
		return new IPath[] { source, prefix };
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
	private static IPath[] getSourceAnnotation(
		IPath libraryPath,
		IPluginLibrary library) {
		IPath[] annot = new IPath[2];
		annot[0] = new Path(libraryPath.removeFileExtension().toString() + "src.zip");
		return annot;
	}
	public IClasspathEntry[] getSourceClasspathEntries(IPluginModel model) {
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
	public void updateClasspath(IProgressMonitor monitor) {
		try {
			// create java nature
			if (!project.hasNature(JavaCore.NATURE_ID)) {
				CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
			}
			/*
			if (!project.hasNature(PDEPlugin.PLUGIN_NATURE)) {
				CoreUtility.addNatureToProject(project, PDEPlugin.PLUGIN_NATURE, monitor);
			}
			*/
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

	public static void addImplicitLibraries(Vector result, boolean addRuntime) {
		String bootId = "org.eclipse.core.boot";
		String runtimeId = "org.eclipse.core.runtime";
		IPlugin bootPlugin = PDEPlugin.getDefault().findPlugin(bootId);

		if (addRuntime) {
			IPlugin runtimePlugin = PDEPlugin.getDefault().findPlugin(runtimeId);
			if (runtimePlugin != null) {
				addToClasspathEntries(new CheckedPlugin(runtimePlugin, true), result);
			}
		}
		if (bootPlugin != null) {
			addToClasspathEntries(new CheckedPlugin(bootPlugin, true), result);
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

		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];
			if (internal) {
				updateLibrary(plugin, library.getName(), add, entries, result);
			} else {
				updateLibrary(
					((ExternalPluginModelBase) plugin.getModel()).getEclipseHomeRelativePath(),
					library.getName(),
					add,
					entries,
					result);
			}
		}
		// Recursively add
		IPluginImport[] imports = element.getPluginInfo().getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			if (iimport.isReexported() == false)
				continue;
			String id = iimport.getId();
			IPlugin reference = PDEPlugin.getDefault().findPlugin(id);
			if (reference != null) {
				CheckedPlugin ref = new CheckedPlugin(reference, true);
				updateLibrariesFor(ref, entries, result);
			}
		}
	}

	private void updateLibrary(
		IPath relativePath,
		String name,
		boolean add,
		IClasspathEntry[] entries,
		Vector result) {
		IPath basePath = new Path(PDEPlugin.ECLIPSE_HOME_VARIABLE).append(relativePath);
		name = expandLibraryName(name);
		IPath libraryPath = basePath.append(name);
		// Search for this entry
		IClasspathEntry libraryEntry = null;

		if (add && isEntryAdded(libraryPath, IClasspathEntry.CPE_VARIABLE, result))
			return;

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
		} else if (add) {
			IPath[] sourceAnnot = getSourceAnnotation(libraryPath, null);
			libraryEntry =
				JavaCore.newVariableEntry(libraryPath, sourceAnnot[0], sourceAnnot[1]);
			IClasspathEntry resolved = JavaCore.getResolvedClasspathEntry(libraryEntry);
			if (resolved != null && resolved.getPath().toFile().exists())
				result.addElement(libraryEntry);
		}
	}

	private static String expandLibraryName(String source) {
		if (source.charAt(0) != '$')
			return source;
		IPath path = new Path(source);
		String firstSegment = path.segment(0);
		if (firstSegment.charAt(firstSegment.length() - 1) != '$')
			return source;
		String variable = firstSegment.substring(1, firstSegment.length() - 1);
		variable = variable.toLowerCase();
		if (variable.equals("ws")) {
			variable = BootLoader.getWS();
			if (variable != null)
				variable = "ws" + File.separator + variable;
		} else if (variable.equals("os")) {
			variable = BootLoader.getOS();
			if (variable != null)
				variable = "os" + File.separator + variable;
		} else
			variable = null;
		if (variable != null) {
			path = path.removeFirstSegments(1);
			return variable + path.SEPARATOR + path.toString();
		}
		return source;
	}

	private void updateLibrary(
		IPlugin plugin,
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
		} else if (add) {
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
		} else if (add) {
			folderEntry = JavaCore.newSourceEntry(folderPath);
			result.addElement(folderEntry);
		}
		return folderEntry != null;
	}

}