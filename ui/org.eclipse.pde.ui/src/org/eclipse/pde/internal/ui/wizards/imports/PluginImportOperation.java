/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.util.Assert;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.*;

public class PluginImportOperation implements IWorkspaceRunnable {
	private static final String KEY_TITLE = "ImportWizard.messages.title";
	private static final String KEY_CREATING =
		"ImportWizard.operation.creating";
	private static final String KEY_MULTI_PROBLEM = "ImportWizard.operation.multiProblem";
	private static final String KEY_PROBLEM = "ImportWizard.operation.problem";
	private static final String KEY_CREATING2 =
		"ImportWizard.operation.creating2";
	private static final String KEY_EXTRACTING =
		"ImportWizard.operation.extracting";
	private static final String KEY_COPYING_SOURCE =
		"ImportWizard.operation.copyingSource";

	public interface IReplaceQuery {

		// return codes
		public static final int CANCEL = 0;
		public static final int NO = 1;
		public static final int YES = 2;

		/**
		 * Do the callback. Returns YES, NO or CANCEL
		 */
		int doQuery(IProject project);
	}

	private IPluginModelBase[] models;
	private boolean doImport;
	private boolean extractSource;

	private IWorkspaceRoot root;
	private IReplaceQuery replaceQuery;

	public PluginImportOperation(
		IPluginModelBase[] models,
		boolean doImport,
		boolean doExtractSource,
		IReplaceQuery replaceQuery) {
		Assert.isNotNull(models);
		Assert.isNotNull(replaceQuery);
		this.models = placeFragmentsFirst(models);
		this.extractSource = doExtractSource;
		this.doImport = doExtractSource ? true : doImport;

		root = ResourcesPlugin.getWorkspace().getRoot();
		this.replaceQuery = replaceQuery;
	}

	private IPluginModelBase[] placeFragmentsFirst(IPluginModelBase[] models) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			if (models[i].isFragmentModel())
				result.add(models[i]);
		}
		if (result.size() > 0) {
			// Now add plug-ins
			for (int i = 0; i < models.length; i++) {
				if (models[i].isFragmentModel() == false)
					result.add(models[i]);
			}
			return (IPluginModelBase[]) result.toArray(
				new IPluginModelBase[result.size()]);
		} else
			return models;
	}
	
	private IFragment [] findFragmentsFor(IPlugin plugin) {
		ArrayList result = new ArrayList();
		for (int i=0; i<models.length; i++) {
			IPluginModelBase model = models[i];
			if (model.isFragmentModel()) {
				IFragment fragment = (IFragment)model.getPluginBase();
				if (PDECore
					.compare(
					fragment.getPluginId(),
					fragment.getPluginVersion(),
					plugin.getId(),
					plugin.getVersion(),
					fragment.getRule())) {
					result.add(fragment);
				}
			}
		}
		return (IFragment[])result.toArray(new IFragment[result.size()]);
	}

	/*
	 * @see IWorkspaceRunnable#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor)
		throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_CREATING),
			models.length);
		try {
			MultiStatus multiStatus =
				new MultiStatus(
					PDEPlugin.getPluginId(),
					IStatus.OK,
					PDEPlugin.getResourceString(KEY_MULTI_PROBLEM),
					null);
			for (int i = 0; i < models.length; i++) {
				try {
					createProject(
						models[i],
						new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					IStatus status = e.getStatus();
					/*
					String newMessage =
						PDEPlugin.getFormattedMessage(
							KEY_PROBLEM,
							new String[] {
								models[i].getPluginBase().getId(),
								status.getMessage()});
					MultiStatus newStatus =
						new Status(
							status.getSeverity(),
							PDEPlugin.getPluginId(),
							status.getCode(),
							newMessage,
							status.getException());
					*/
					multiStatus.merge(status);
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
			if (!multiStatus.isOK()) {
				throw new CoreException(multiStatus);
			}
		} finally {
			monitor.done();
		}
	}

	private void createProject(
		IPluginModelBase model,
		IProgressMonitor monitor)
		throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		String name = plugin.getId();
		String task = PDEPlugin.getFormattedMessage(KEY_CREATING2, name);
		monitor.beginTask(task, 8);
		try {
			File pluginDir = new File(model.getInstallLocation());
			IPath pluginPath = new Path(pluginDir.getPath());

			IProject project = root.getProject(name);
			if (project.exists()) {
				if (queryReplace(project)) {
					boolean deleteContent =
						doImport
							&& root.getLocation().isPrefixOf(
								project.getLocation());
					project.delete(
						deleteContent,
						true,
						new SubProgressMonitor(monitor, 1));
				} else {
					return;
				}
			} else {
				monitor.worked(1);
			}

			IProjectDescription desc =
				root.getWorkspace().newProjectDescription(project.getName());
			if (!doImport) {
				desc.setLocation(pluginPath);
			}

			project.create(desc, new SubProgressMonitor(monitor, 1));
			if (!project.isOpen()) {
				project.open(null);
			}

			if (doImport) {
				importContent(
					pluginDir,
					project.getFullPath(),
					FileSystemStructureProvider.INSTANCE,
					null,
					new SubProgressMonitor(monitor, 1));
				importSource(project, plugin, pluginPath, new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(2);
			}

			desc = project.getDescription();
			desc.setNatureIds(
				new String[] { JavaCore.NATURE_ID, PDE.PLUGIN_NATURE });
			project.setDescription(desc, new SubProgressMonitor(monitor, 1));
			//Mark this project so that we can show image overlay
			// using the label decorator
			if (!extractSource)
				project.setPersistentProperty(
					PDECore.EXTERNAL_PROJECT_PROPERTY,
					doImport
						? PDECore.EXTERNAL_PROJECT_VALUE
						: PDECore.BINARY_PROJECT_VALUE);

			IPath outputLocation = project.getFullPath();

			IPluginLibrary[] libraries = plugin.getLibraries();

			IClasspathEntry[] classpathEntries = null;
			if (libraries.length > 0) {
				classpathEntries = new IClasspathEntry[libraries.length];
				for (int i = 0; i < libraries.length; i++) {
					classpathEntries[i] =
						UpdateClasspathOperation.getLibraryEntry(
							project,
							libraries[i],
							true);
				}
				if (extractSource) {
					doExtractSource(
						project,
						classpathEntries,
						new SubProgressMonitor(monitor, 1));
				} else {
					monitor.worked(1);
				}
				outputLocation = project.getFullPath().append("bin");
			} else {
				classpathEntries = new IClasspathEntry[0];
				outputLocation = project.getFullPath();
				monitor.worked(1);
			}
			IJavaProject jproject = JavaCore.create(project);
			UpdateClasspathOperation op =
				new UpdateClasspathOperation(
					jproject,
					model,
					models,
					classpathEntries,
					outputLocation);
			op.run(new SubProgressMonitor(monitor, 2));
		} finally {
			monitor.done();
		}
	}

	private void importContent(
		Object source,
		IPath destPath,
		IImportStructureProvider provider,
		List filesToImport,
		IProgressMonitor monitor)
		throws CoreException {
		IOverwriteQuery query = new IOverwriteQuery() {
			public String queryOverwrite(String file) {
				return ALL;
			}
		};
		ImportOperation op =
			new ImportOperation(destPath, source, provider, query);
		op.setCreateContainerStructure(false);
		if (filesToImport != null) {
			op.setFilesToImport(filesToImport);
		}

		try {
			op.run(monitor);
		} catch (InvocationTargetException e) {
			Throwable th = e.getTargetException();
			if (th instanceof CoreException) {
				throw (CoreException) th;
			}
			IStatus status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					e.getMessage(),
					e);
			throw new CoreException(status);
		} catch (InterruptedException e) {
			throw new OperationCanceledException(e.getMessage());
		}
	}
	
	private void importSource(
		IProject project,
		IPluginBase plugin,
		IPath pluginPath,
		IProgressMonitor monitor)
		throws CoreException {
		SourceLocationManager manager = PDEPlugin.getDefault().getSourceLocationManager();
		IPluginLibrary[] libraries = plugin.getLibraries();
		monitor.beginTask(PDEPlugin.getResourceString(KEY_COPYING_SOURCE), libraries.length);
		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];
			String libraryName = library.getName();
			IPath libPath = new Path(libraryName);
			boolean variableReference = libraryName.indexOf('$') != -1;
			IPath srcPath = UpdateClasspathOperation.getSourcePath(libPath);
			if (srcPath!=null) {
				IFile sourceZip = project.getFile(srcPath);
				if (sourceZip.exists()) {
					// this library already has the zip as a library sibling -
					// no need to do anything here.
					continue;
				}
				// we must look up the source locations to
				// find this zip.

				File srcFile = manager.findSourceFile(srcPath);
				// cannot find it
				if (srcFile!=null) {;
					importSourceFile(project, srcFile, srcPath);
					continue;
				}
				// if we are here, either root source path is null
				// or full source file does not exist.
				if (variableReference && plugin instanceof IPlugin) {
					// contains '$' and cannot find in the plug-in
					// try fragments.
					IFragment [] fragments = findFragmentsFor((IPlugin)plugin);
					for (int j=0; j<fragments.length; j++) {
						IFragment fragment = fragments[j];
						if (importCrossFragmentSource(project, (IPlugin)plugin, srcPath, fragment))
							break;
					}
				}
			}
			monitor.worked(1);
		}
	}

	private void importSourceFile(
		IProject project,
		File srcFile,
		IPath srcPath)
		throws CoreException {
		try {
			FileInputStream fstream = new FileInputStream(srcFile);
			IFile file = project.getFile(srcPath);
			if (file.exists())
				file.setContents(fstream, true, false, null);
			else
				file.create(fstream, true, null);

		} catch (IOException e) {
			IStatus status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.OK,
					e.getMessage(),
					e);
			throw new CoreException(status);
		}
	}

	private boolean importCrossFragmentSource(IProject project, IPlugin plugin, IPath srcPath, IFragment fragment) {
		String fragmentDir = fragment.getModel().getInstallLocation();
		IPath fragmentPath = new Path(fragmentDir);
		IPath sourceRootPath = getSourceRootPath(fragmentPath);
		if (sourceRootPath==null) return false;
		String id = fragment.getId();
		IProject fragmentProject = PDEPlugin.getWorkspace().getRoot().getProject(id);
		if (!fragmentProject.exists()) return false;
		IPath fullSourcePath = sourceRootPath.append(srcPath);
		File srcFile = fullSourcePath.toFile();
		if (srcFile.exists()==false) return false;
		try {
			importSourceFile(fragmentProject, srcFile, srcPath);
			return true;
		}
		catch (CoreException e) {
			return false;
		}
	}

	/**
	 * Tries to find the source archives for library entries. If found, it is imported and the library classpath entry 
	 * is replaced by a source classpath entry.
	 */
	private void doExtractSource(
		IProject project,
		IClasspathEntry[] entries,
		IProgressMonitor monitor)
		throws CoreException {
		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_EXTRACTING),
			entries.length * 2);
		try {
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				IPath curr = entry.getPath();

				IPath sourceAttach = entry.getSourceAttachmentPath();
				if (sourceAttach != null) {
					IResource res = root.findMember(sourceAttach);
					if (res instanceof IFile) {
						String name = curr.removeFileExtension().lastSegment();
						IFolder dest = project.getFolder("src-" + name);
						if (!dest.exists()) {
							dest.create(true, true, null);
						}
						extractZipFile(
							res,
							dest,
							new SubProgressMonitor(monitor, 1));
						// extract resources from the library JAR
						res = root.findMember(curr);
						if (res instanceof IFile) {
							extractResources(
								res,
								dest,
								new SubProgressMonitor(monitor, 1));
							createJarPackagerFiles(dest, (IFile) res);
						} else {
							monitor.worked(1);
						}
						// replace the entry
						entries[i] =
							JavaCore.newSourceEntry(dest.getFullPath());
					} else {
						monitor.worked(2);
					}
				} else {
					monitor.worked(2);
				}
			}
		} finally {
			monitor.done();
		}
	}

	private void extractZipFile(
		IResource res,
		IFolder dest,
		IProgressMonitor monitor)
		throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(res.getLocation().toFile());
			ZipFileStructureProvider provider =
				new ZipFileStructureProvider(zipFile);
			importContent(
				provider.getRoot(),
				dest.getFullPath(),
				provider,
				null,
				monitor);
		} catch (IOException e) {
			IStatus status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					e.getMessage(),
					e);
			throw new CoreException(status);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void extractResources(
		IResource res,
		IFolder dest,
		IProgressMonitor monitor)
		throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(res.getLocation().toFile());
			ZipFileStructureProvider provider =
				new ZipFileStructureProvider(zipFile);
			ArrayList collected = new ArrayList();
			collectResources(provider, provider.getRoot(), collected);

			importContent(
				provider.getRoot(),
				dest.getFullPath(),
				provider,
				collected,
				monitor);
		} catch (IOException e) {
			IStatus status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					e.getMessage(),
					e);
			throw new CoreException(status);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void collectResources(
		ZipFileStructureProvider provider,
		Object element,
		ArrayList collected) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					collectResources(provider, curr, collected);
				} else if (!provider.getLabel(curr).endsWith(".class")) {
					collected.add(curr);
				}
			}
		}
	}

	private void createJarPackagerFiles(IFolder sourceFolder, IFile jarFile)
		throws CoreException {
		IProject project = sourceFolder.getProject();
		IFolder scriptsFolder = project.getFolder("scripts");
		if (!scriptsFolder.exists()) {
			scriptsFolder.create(true, true, null);
		}
		IFile descriptorFile =
			scriptsFolder.getFile(sourceFolder.getName() + ".jardesc");
		if (descriptorFile.exists()) {
			descriptorFile.delete(true, null);
		}

		String string =
			constructJarPackagerFileContent(
				sourceFolder,
				jarFile,
				descriptorFile.getFullPath());

		descriptorFile.create(
			new ByteArrayInputStream(string.getBytes()),
			true,
			null);
	}

	private String constructJarPackagerFileContent(
		IFolder folder,
		IFile jarFile,
		IPath descriptorPath) {
		IPath folderPath = folder.getFullPath();
		IPath manifestPath =
			folderPath.append("META-INF").append("MANIFEST.MF");

		IPackageFragmentRoot root =
			JavaCore.create(folder.getProject()).getPackageFragmentRoot(folder);

		StringBuffer buf = new StringBuffer();
		String lineDelim = System.getProperty("line.separator", "\n");

		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		buf.append(lineDelim);
		buf.append("<jardesc>");
		buf.append(lineDelim);
		buf.append("\t<jar path=\"");
		buf.append(jarFile.getLocation().toString());
		buf.append("\"/>");
		buf.append(lineDelim);
		buf.append("\t<options compress=\"true\"");
		buf.append(lineDelim);
		buf.append("\t\tdescriptionLocation=\"");
		buf.append(descriptorPath.toString());
		buf.append('"');
		buf.append(lineDelim);
		buf.append(
			"\t\texportErrors=\"true\" exportWarnings=\"true\" logErrors=\"true\"");
		buf.append(lineDelim);
		buf.append(
			"\t\tlogWarnings=\"true\" overwrite=\"false\" saveDescription=\"true\" useSourceFolders=\"false\"/>");
		buf.append(lineDelim);
		buf.append("\t<manifest generateManifest=\"false\"");
		buf.append(lineDelim);
		buf.append("\t\tmanifestLocation=\"");
		buf.append(manifestPath.toString());
		buf.append('"');
		buf.append(lineDelim);
		buf.append(
			"\t\tmanifestVersion=\"1.0\" reuseManifest=\"false\" saveManifest=\"false\" usesManifest=\"false\">");
		buf.append(lineDelim);
		buf.append(
			"\t\t<sealing sealJar=\"false\"><packagesToSeal/><packagesToUnSeal/></sealing>");
		buf.append(lineDelim);
		buf.append("\t</manifest>");
		buf.append(lineDelim);
		buf.append(
			"\t<selectedElements exportClassFiles=\"true\" exportJavaFiles=\"false\">");
		buf.append(lineDelim);
		buf.append("\t\t<javaElement handleIdentifier=\"");
		buf.append(root.getHandleIdentifier());
		buf.append("\"/>");
		buf.append(lineDelim);
		buf.append("\t</selectedElements>");
		buf.append(lineDelim);
		buf.append("</jardesc>");
		buf.append(lineDelim);

		return buf.toString();
	}

	private boolean queryReplace(IProject project)
		throws OperationCanceledException {
		switch (replaceQuery.doQuery(project)) {
			case IReplaceQuery.CANCEL :
				throw new OperationCanceledException();
			case IReplaceQuery.NO :
				return false;
		}
		return true;
	}

}