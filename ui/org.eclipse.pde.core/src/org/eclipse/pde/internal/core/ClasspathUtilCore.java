package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.HashSet;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;

public class ClasspathUtilCore {

	public static void setClasspath(
		IPluginModelBase model,
		boolean useClasspathContainer,
		IMissingPluginConfirmation confirmation,
		IProgressMonitor monitor) {

		Vector result = new Vector();
		try {
			int numUnits = 3;
			if (!useClasspathContainer)
				numUnits += model.getPluginBase().getImports().length;
			monitor.beginTask("", numUnits);
			
			// add own libraries/source
			addSourceAndLibraries(model, result);
			monitor.worked(1);
			
			if (useClasspathContainer) {
				result.add(createContainerEntry());
				monitor.worked(1);
			} else {
				computePluginEntries(model, true, result, confirmation, monitor);
			}
			
			// add JRE
			addJRE(result);
			monitor.worked(1);
			
			IClasspathEntry[] entries =
				(IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);

			IJavaProject javaProject =
				JavaCore.create(model.getUnderlyingResource().getProject());
			IJavaModelStatus validation =
				JavaConventions.validateClasspath(
					javaProject,
					entries,
					javaProject.getOutputLocation());
			if (!validation.isOK()) {
				PDECore.logErrorMessage(validation.getMessage());
				throw new CoreException(validation);
			}
			javaProject.setRawClasspath(entries, monitor);
		} catch (CoreException e) {
		}

	}
	
	private static void computePluginEntries(
		IPluginModelBase model,
		boolean relative,
		Vector result,
		IMissingPluginConfirmation confirmation,
		IProgressMonitor monitor) {
		try {
			HashSet alreadyAdded = new HashSet();
			if (model.isFragmentModel()) {
				addParentPlugin((IFragment) model.getPluginBase(), relative, result, alreadyAdded);
			} else {
				addFragmentLibraries(model.getPluginBase(), relative, result);
			}

			// add dependencies
			IPluginImport[] dependencies = model.getPluginBase().getImports();
			for (int i = 0; i < dependencies.length; i++) {
				IPluginImport dependency = dependencies[i];
				IPlugin plugin =
					PDECore.getDefault().findPlugin(
						dependency.getId(),
						dependency.getVersion(),
						dependency.getMatch());
				if (plugin != null) {
					addDependency(
						plugin,
						dependency.isReexported(),
						relative,
						true,
						result,
						alreadyAdded);
				}
				else if (confirmation!=null && confirmation.getUseProjectReference()) {
					addMissingDependencyAsProject(dependency.getId(), dependency.isReexported(), result);
				}
				if (monitor != null)
					monitor.worked(1);
			}

			// add implicit dependencies
			addImplicitDependencies(model.getPluginBase().getId(), relative, result, alreadyAdded);
			if (monitor != null)
				monitor.worked(1);
		} catch (CoreException e) {
		}

	}
	
	public static IClasspathEntry[] computePluginEntries(IPluginModelBase model, IMissingPluginConfirmation confirmation) {
		Vector result = new Vector();
		computePluginEntries(model, false, result, confirmation, null);
		return (IClasspathEntry[])result.toArray(new IClasspathEntry[result.size()]);
	}

	private static void addMissingDependencyAsProject(String name, boolean isExported, Vector result) {
		IProject project = PDECore.getWorkspace().getRoot().getProject(name);
		IClasspathEntry entry = JavaCore.newProjectEntry(project.getFullPath(), isExported);
		result.add(entry);
	}
	
	private static void addDependency(
		IPlugin plugin,
		boolean isExported,
		boolean relative,
		boolean doAddWorkspaceFragments,
		Vector result,
		HashSet alreadyAdded)
		throws CoreException {

		if (!alreadyAdded.add(plugin))
			return;
			
		IResource resource = plugin.getModel().getUnderlyingResource();
		if (resource != null) {
			IProject project = resource.getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IClasspathEntry entry =
					JavaCore.newProjectEntry(project.getFullPath(), isExported);
				result.add(entry);
			}
			if (doAddWorkspaceFragments)
				addFragmentsWithSource(plugin, isExported, result);
			return;
		}

		IPluginLibrary[] libraries = plugin.getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			IClasspathEntry entry =
				createLibraryEntry(libraries[i], isExported, relative);
			if (entry != null) {
				result.add(entry);
			}
		}

		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport dependency = imports[i];
			if (dependency.isReexported()) {
				IPlugin importedPlugin =
					PDECore.getDefault().findPlugin(
						dependency.getId(),
						dependency.getVersion(),
						dependency.getMatch());
				if (importedPlugin != null)
					addDependency(importedPlugin, isExported, relative, true, result, alreadyAdded);
			}
		}
	}

	private static void addFragmentLibraries(IPluginBase plugin, boolean relative, Vector result) {
		IFragment[] fragments = PDECore.getDefault().findFragmentsFor(plugin.getId(), plugin.getVersion());
		for (int i = 0; i < fragments.length; i++) {
			IPluginLibrary[] libraries = fragments[i].getLibraries();
			for (int j = 0; j < libraries.length; j++) {
				IClasspathEntry entry = createLibraryEntry(libraries[j], true, relative);
				if (entry != null && !result.contains(entry))
					result.add(entry);
			}
		}
		
	}
	
	private static void addFragmentsWithSource(IPlugin plugin, boolean isExported, Vector result)
		throws CoreException {
		IFragment[] fragments =
			PDECore.getDefault().getWorkspaceModelManager().getFragmentsFor(
				plugin.getId(),
				plugin.getVersion());

		for (int i = 0; i < fragments.length; i++) {
			IProject project =
				fragments[i].getModel().getUnderlyingResource().getProject();
			if (WorkspaceModelManager.isJavaPluginProjectWithSource(project)) {
				IClasspathEntry projectEntry =
					JavaCore.newProjectEntry(project.getFullPath(), isExported);
				if (!result.contains(projectEntry))
					result.add(projectEntry);
			}
		}
	}
	
	protected static void addImplicitDependencies(
		String id,
		boolean relative,
		Vector result,
		HashSet alreadyAdded)
		throws CoreException {
		if (!id.equals("org.eclipse.core.boot") && !id.equals("org.apache.xerces")) {
			IPlugin plugin = PDECore.getDefault().findPlugin("org.eclipse.core.boot");
			if (plugin != null)
				addDependency(plugin, false, relative, true, result, alreadyAdded);
			if (!id.equals("org.eclipse.core.runtime")) {
				//plugin = PDECore.getDefault().findPlugin("org.apache.xerces");
				//if (plugin != null)
					//addDependency(plugin, false, relative, true, result, alreadyAdded);
				plugin = PDECore.getDefault().findPlugin("org.eclipse.core.runtime");
				if (plugin != null)
					addDependency(plugin, false, relative, true, result, alreadyAdded);
			}
		}
	}
	
	protected static void addJRE(Vector result) {
		result.add(
			JavaCore.newContainerEntry(
				new Path("org.eclipse.jdt.launching.JRE_CONTAINER")));
	}
	
	public static void addLibraries(
		IPluginModelBase model,
		boolean unconditionallyExport,
		boolean relative,
		Vector result) {
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			IClasspathEntry entry =
				createLibraryEntry(libraries[i], unconditionallyExport, relative);
			if (entry != null)
				result.add(entry);
		}
	}

	private static void addParentPlugin(
		IFragment fragment,
		boolean relative,
		Vector result,
		HashSet alreadyAdded)
		throws CoreException {
		IPlugin parent =
			PDECore.getDefault().findPlugin(
				fragment.getPluginId(),
				fragment.getPluginVersion(),
				fragment.getRule());
		if (parent != null) {
			addDependency(parent, false, relative, false, result, alreadyAdded);
			IPluginImport[] imports = parent.getImports();
			for (int i = 0; i < imports.length; i++) {
				if (!imports[i].isReexported()) {
					IPlugin plugin = PDECore.getDefault().findPlugin(imports[i].getId(), imports[i].getVersion(), imports[i].getMatch());
					if (plugin != null) {
						addDependency(plugin, false, relative, true, result, alreadyAdded);
					}
				}
			}
		}
	}
	
	
	private static void addSourceAndLibraries(
		IPluginModelBase model,
		Vector result)
		throws CoreException {

		IProject project = model.getUnderlyingResource().getProject();
		IBuildEntry[] buildEntries = getBuildEntries(model, project);

		if (buildEntries.length == 0) {
			// keep existing source folders
			IClasspathEntry[] entries = JavaCore.create(project).getRawClasspath();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE
					&& entry.getContentKind() == IPackageFragmentRoot.K_SOURCE) {
					result.add(entry);
				}
			}
		}

		// add libraries			
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];
			boolean found = false;
			for (int j = 0; j < buildEntries.length; j++) {
				IBuildEntry buildEntry = buildEntries[j];
				// add corresponding source folder instead of library, if one exists
				if (buildEntry.getName().equals("source." + library.getName())) {
					String[] folders = buildEntry.getTokens();
					for (int k = 0; k < folders.length; k++) {
						IPath path = project.getFullPath().append(folders[k]);
						if (path.toFile().exists()) {
							result.add(JavaCore.newSourceEntry(path));
						} else {
							addSourceFolder(folders[k], project, result);
						}
					}
					found = true;
					break;
				}
			}
			// add library, since no source folder was found.
			if (!found) {
				IClasspathEntry entry = createLibraryEntry(library, library.isExported(), true);
				if (entry != null)
					result.add(entry);
				
			}
		}
	}
	
	protected static void addSourceFolder(
		String name,
		IProject project,
		Vector result)
		throws CoreException {
		IPath path = project.getFullPath().append(name);
		ensureFolderExists(project, path);
		IClasspathEntry entry = JavaCore.newSourceEntry(path);
		result.add(entry);
	}
	
	private static void ensureFolderExists(IProject project, IPath folderPath)
		throws CoreException {
		IWorkspace workspace = project.getWorkspace();

		for (int i = 1; i <= folderPath.segmentCount(); i++) {
			IPath partialPath = folderPath.uptoSegment(i);
			if (!workspace.getRoot().exists(partialPath)) {
				IFolder folder = workspace.getRoot().getFolder(partialPath);
				folder.create(true, true, null);
			}
		}
	}
	
	/**
	 * Creates a new instance of the classpath container entry for
	 * the given project.
	 * @param project
	 */

	public static IClasspathEntry createContainerEntry() {
		IPath path = new Path(PDECore.CLASSPATH_CONTAINER_ID);
		return JavaCore.newContainerEntry(path);
	}
	
	private static IClasspathEntry createLibraryEntry(
		IPluginLibrary library,
		boolean unconditionallyExport,
		boolean relative) {
		try {
			String expandedName = expandLibraryName(library.getName());
			boolean isExported = unconditionallyExport ? true : library.isFullyExported();

			IPluginModelBase model = library.getModel();
			IPath path = getPath(model, expandedName);
			if (path == null) {
				if (model.isFragmentModel())
					return null;
				model = resolveLibraryInFragments(library, expandedName);
				if (model == null)
					return null;
				path = getPath(model, expandedName);
			}
			if (relative && model.getUnderlyingResource() == null) {
				return JavaCore.newVariableEntry(
					EclipseHomeInitializer.createEclipseRelativeHome(path.toOSString()),
					getSourceAnnotation(model, expandedName, relative),
					null,
					isExported);
			}

			return JavaCore.newLibraryEntry(
				path,
				getSourceAnnotation(model, expandedName, relative),
				null,
				isExported);
		} catch (CoreException e) {
			return null;
		}
	}
		
	public static String expandLibraryName(String source) {
		if (source == null || source.length() == 0)
			return "";
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
			return variable + IPath.SEPARATOR + path.toString();
		}
		return source;
	}
	
	private static IBuildEntry[] getBuildEntries(
		IPluginModelBase model,
		IProject project)
		throws CoreException {
		IBuildModel buildModel = model.getBuildModel();
		if (buildModel == null) {
			IFile buildFile = project.getFile("build.properties");
			if (buildFile.exists()) {
				buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
			}
		}
		if (buildModel != null)
			return buildModel.getBuild().getBuildEntries();

		return new IBuildEntry[0];
	}
	
	private static IPath getSourceAnnotation(
		IPluginModelBase model,
		String libraryName,
		boolean relative)
		throws CoreException {
		IPath path = null;
		int dot = libraryName.lastIndexOf('.');
		if (dot != -1) {
			String zipName = libraryName.substring(0, dot) + "src.zip";
			path = getPath(model, zipName);
			if (path == null) {
				IResource resource = model.getUnderlyingResource();
				SourceLocationManager manager =
					PDECore.getDefault().getSourceLocationManager();
				path =
					manager.findVariableRelativePath(
						model.getPluginBase(),
						new Path(zipName));
				if (path != null) {
					if (!relative
						|| (resource != null
							&& !resource.getProject().hasNature(JavaCore.NATURE_ID))
						|| (resource != null && resource.isLinked()))
						path = JavaCore.getResolvedVariablePath(path);
				}
			}
		}
		return path;
	}
	
	private static IPluginModelBase resolveLibraryInFragments(
		IPluginLibrary library,
		String libraryName) {
		IFragment[] fragments =
			PDECore.getDefault().findFragmentsFor(
				library.getPluginBase().getId(),
				library.getPluginBase().getVersion());

		for (int i = 0; i < fragments.length; i++) {
			IPath path = getPath(fragments[i].getModel(), libraryName);
			if (path != null)
				return fragments[i].getModel();
		}
		return null;
	}
	
	private static IPath getPath(IPluginModelBase model, String libraryName) {
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			IResource jarFile = resource.getProject().findMember(libraryName);
			if (jarFile != null)
				return jarFile.getFullPath();
		} else {
			IPath path =
				new Path(model.getInstallLocation()).append(
					libraryName);
			if (path.toFile().exists()) {
				return path;
			}
		}
		return null;
		
	}
		
}
