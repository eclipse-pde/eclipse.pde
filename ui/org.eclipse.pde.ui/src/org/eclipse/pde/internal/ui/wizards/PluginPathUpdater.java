package org.eclipse.pde.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.ui.*;

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
		private boolean exported;
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
		public void setExported(boolean exported) {
			this.exported = exported;
		}
		public boolean isExported() {
			return exported;
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
				IClasspathEntry projectEntry = JavaCore.newProjectEntry(projectPath, element.isExported());
				result.addElement(projectEntry);
			}
			return;
		}
		IPath modelPath = getExternalPath(model);

		IPluginLibrary[] libraries = plugin.getLibraries();

		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];

			String name = expandLibraryName(library.getName());
			IPath libraryPath = modelPath.append(name);
			IPath[] sourceAnnot = getSourceAnnotation(libraryPath);
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
	
	public static IPath getExternalPath(IPluginModelBase model) {
		IPath modelPath = new Path(PDEPlugin.ECLIPSE_HOME_VARIABLE);
		modelPath =
			modelPath.append(
				((ExternalPluginModelBase) model).getEclipseHomeRelativePath());
		return modelPath;
	}
	
	public static IClasspathEntry createLibraryEntry(
		IPluginLibrary library,
		IPath rootPath,
		boolean unconditionallyExport) {
		String name = expandLibraryName(library.getName());
		IPath libraryPath = rootPath.append(name);
		IPath[] sourceAnnot = getSourceAnnotation(libraryPath);
		return JavaCore.newLibraryEntry(
			libraryPath,
			sourceAnnot[0],
			sourceAnnot[1],
			unconditionallyExport? true : library.isFullyExported());
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

	private static IPath[] getSourceAnnotation(
		IPath libraryPath) {
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
			variable = TargetPlatform.getWS();
			if (variable != null)
				variable = "ws" + File.separator + variable;
		} else if (variable.equals("os")) {
			variable = TargetPlatform.getOS();
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


}