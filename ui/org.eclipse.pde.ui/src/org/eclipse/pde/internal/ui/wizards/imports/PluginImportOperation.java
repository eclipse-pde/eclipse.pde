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
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.team.core.*;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.*;

public class PluginImportOperation implements IWorkspaceRunnable {
	private static final String KEY_TITLE = "ImportWizard.messages.title";
	private static final String KEY_CREATING =
		"ImportWizard.operation.creating";
	private static final String KEY_MULTI_PROBLEM =
		"ImportWizard.operation.multiProblem";
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
	private ArrayList modelIds;
	private boolean doImport;
	private boolean extractSource;

	private IWorkspaceRoot root;
	private IReplaceQuery replaceQuery;

	public PluginImportOperation(
		IPluginModelBase[] models,
		ArrayList modelIds,
		boolean doImport,
		boolean doExtractSource,
		IReplaceQuery replaceQuery) {
		Assert.isNotNull(models);
		Assert.isNotNull(replaceQuery);
		this.models = models;
		this.modelIds = modelIds;
		this.extractSource = doExtractSource;
		this.doImport = doExtractSource ? true : doImport;

		root = ResourcesPlugin.getWorkspace().getRoot();
		this.replaceQuery = replaceQuery;
	}

	private IFragment[] findFragmentsFor(IPlugin plugin) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if (model.isFragmentModel()) {
				IFragment fragment = (IFragment) model.getPluginBase();
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
		return (IFragment[]) result.toArray(new IFragment[result.size()]);
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
			//long start = System.currentTimeMillis();
			for (int i = 0; i < models.length; i++) {
				try {
					createProject(
						models[i],
						new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					multiStatus.merge(e.getStatus());
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
			if (!multiStatus.isOK()) {
				throw new CoreException(multiStatus);
			}
			//long stop = System.currentTimeMillis();
			//System.out.println("Import time: "+(stop-start)+"ms");
		} finally {
			monitor.done();
		}
	}

	private void createProject(
		IPluginModelBase model,
		IProgressMonitor monitor)
		throws CoreException {
		String name = model.getPluginBase().getId();
		String task = PDEPlugin.getFormattedMessage(KEY_CREATING2, name);
		monitor.beginTask(task, 8);
		try {
			IProject project = root.getProject(name);

			if (project.exists()) {
				if (queryReplace(project)) {
					project.delete(
						true,
						true,
						new SubProgressMonitor(monitor, 1));
					try {
						RepositoryProvider.unmap(project);
					} catch (TeamException e) {
					}
				} else {
					return;
				}
			} else {
				monitor.worked(1);
			}

			project.create(new SubProgressMonitor(monitor, 1));
			if (!project.isOpen()) {
				project.open(null);
			}
			File pluginDir = new File(model.getInstallLocation());
			IFile buildFile = project.getFile("build.properties");
			WorkspaceBuildModel buildModel = new WorkspaceBuildModel(buildFile);

			if (doImport) {
				importContent(
					pluginDir,
					project.getFullPath(),
					FileSystemStructureProvider.INSTANCE,
					null,
					new SubProgressMonitor(monitor, 1));
				if (extractSource)
					configureBinIncludes(pluginDir, buildModel);

				importSource(
					project,
					model.getPluginBase(),
					new Path(pluginDir.getPath()),
					doImport,
					new SubProgressMonitor(monitor, 1));
			} else {
				linkContent(
					pluginDir,
					project,
					new SubProgressMonitor(monitor, 2));
			}

			boolean isJavaProject =
				model.getPluginBase().getLibraries().length > 0;

			setProjectDescription(project, isJavaProject, monitor);
			if (!doImport) {
				try {
					RepositoryProvider.map(
						project,
						PDECore.BINARY_REPOSITORY_PROVIDER);
				} catch (TeamException e) {
					// should report
				}
			}

			boolean sourceFound = false;
			if (isJavaProject & extractSource)
				sourceFound =
					doExtractSource(project, model, buildModel, monitor);

			//Mark this project so that we can show image overlay
			// using the label decorator
			if (doImport && (!isJavaProject || !sourceFound)) {
				project.setPersistentProperty(
					PDECore.EXTERNAL_PROJECT_PROPERTY,
					PDECore.BINARY_PROJECT_VALUE);
			}

			if (isJavaProject) {
				IJavaProject jProject = JavaCore.create(project);
				if (jProject.getRawClasspath() != null
					&& jProject.getRawClasspath().length > 0)
					jProject.setRawClasspath(new IClasspathEntry[0], monitor);
				modelIds.add(model.getPluginBase().getId());
			}
		} finally {
			monitor.done();
		}
	}

	private void configureBinIncludes(
		File pluginDir,
		WorkspaceBuildModel buildModel) {
		IBuild build = buildModel.getBuild(true);
		IBuildEntry entry = buildModel.getFactory().createEntry("bin.includes");
		File[] files = pluginDir.listFiles();

		try {
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				String token = file.getName();
				if (file.isDirectory())
					token = token + "/";
				entry.addToken(token);
			}
			build.add(entry);
			buildModel.save();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
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
		boolean doImport,
		IProgressMonitor monitor)
		throws CoreException {
		SourceLocationManager manager =
			PDECore.getDefault().getSourceLocationManager();
		IPluginLibrary[] libraries = plugin.getLibraries();
		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_COPYING_SOURCE),
			libraries.length);
		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];
			String libraryName = library.getName();
			IPath libPath = new Path(libraryName);
			boolean variableReference = libraryName.indexOf('$') != -1;
			IPath srcPath = UpdateClasspathOperation.getSourcePath(libPath);
			if (srcPath != null) {
				IFile sourceZip = project.getFile(srcPath);
				if (sourceZip.exists()) {
					// this library already has the zip as a library sibling -
					// no need to do anything here.
					continue;
				}
				// we must look up the source locations to
				// find this zip.

				File srcFile = manager.findSourceFile(plugin, srcPath);
				// cannot find it
				if (srcFile != null) {
					importSourceFile(project, srcFile, srcPath, doImport);
					continue;
				}
				// if we are here, either root source path is null
				// or full source file does not exist.
				if (variableReference && plugin instanceof IPlugin) {
					// contains '$' and cannot find in the plug-in
					// try fragments.
					IFragment[] fragments = findFragmentsFor((IPlugin) plugin);
					for (int j = 0; j < fragments.length; j++) {
						IFragment fragment = fragments[j];
						if (importCrossFragmentSource(project,
							manager,
							(IPlugin) plugin,
							srcPath,
							fragment,
							doImport))
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
		IPath srcPath,
		boolean doImport)
		throws CoreException {
		try {

			if (doImport) {
				IFile file = project.getFile(srcPath);
				FileInputStream fstream = new FileInputStream(srcFile);
				if (file.exists())
					file.setContents(fstream, true, false, null);
				else
					file.create(fstream, true, null);
				fstream.close();
			} else {
				IFile file = project.getFile(srcPath);
				if (!(file.getParent() instanceof IProject)) {
					// cannot link the file directly - must make
					// a flat path
					file = project.getFile(getFlatPath(srcPath));
				}
				file.createLink(
					new Path(srcFile.getPath()),
					IResource.NONE,
					null);
			}

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

	private String getFlatPath(IPath path) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < path.segmentCount(); i++) {
			if (i > 0)
				buf.append("_");
			buf.append(path.segment(i));
		}
		return buf.toString();
	}

	private boolean importCrossFragmentSource(
		IProject project,
		SourceLocationManager manager,
		IPlugin plugin,
		IPath srcPath,
		IFragment fragment,
		boolean doImport) {
		String id = fragment.getId();
		IProject fragmentProject =
			PDEPlugin.getWorkspace().getRoot().getProject(id);
		if (!fragmentProject.exists())
			return false;

		IFile fragmentFile = fragmentProject.getFile(srcPath);
		// No need to do anything if exists
		if (fragmentFile.exists())
			return true;

		File srcFile = manager.findSourceFile(fragment, srcPath);
		if (srcFile == null)
			return false;
		try {
			importSourceFile(fragmentProject, srcFile, srcPath, doImport);
			return true;
		} catch (CoreException e) {
			return false;
		}
	}

	private void linkContent(
		File sourceDir,
		IProject project,
		IProgressMonitor monitor)
		throws CoreException {
		File[] items = sourceDir.listFiles();
		monitor.beginTask("Linking content...", items.length);
		for (int i = 0; i < items.length; i++) {
			File sourceFile = items[i];
			if (sourceFile.isDirectory()) {
				IFolder folder = project.getFolder(sourceFile.getName());
				folder.createLink(
					new Path(sourceFile.getPath()),
					IResource.NONE,
					new SubProgressMonitor(monitor, 1));
			} else {
				String fileName = sourceFile.getName();
				// Ignore .classpath and .project in
				// the plug-in - these files will
				// be created, so ignore the imported ones.
				if (!fileName.equals(".classpath")
					&& !fileName.equals(".project")) {
					IFile file = project.getFile(fileName);
					file.createLink(
						new Path(sourceFile.getPath()),
						IResource.NONE,
						new SubProgressMonitor(monitor, 1));
				}
			}
			monitor.worked(1);
		}
	}

	/**
	 * Tries to find the source archives for library entries. If found, it is imported and the library classpath entry 
	 * is replaced by a source classpath entry.
	 */
	private boolean doExtractSource(
		IProject project,
		IPluginModelBase model,
		WorkspaceBuildModel buildModel,
		IProgressMonitor monitor)
		throws CoreException {

		boolean sourceFound = false;

		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();

		IClasspathEntry[] entries = new IClasspathEntry[libraries.length];
		for (int i = 0; i < libraries.length; i++) {
			entries[i] =
				UpdateClasspathOperation.getLibraryEntry(
					project,
					libraries[i],
					true);
		}

		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_EXTRACTING),
			entries.length * 2);
		try {
			IBuild build = buildModel.getBuild(true);
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				IPath curr = entry.getPath();
				String entryName = "source." + curr.lastSegment();
				IBuildEntry buildEntry = null;

				IPath sourceAttach = entry.getSourceAttachmentPath();
				if (sourceAttach != null) {
					IResource res = root.findMember(sourceAttach);
					if (res instanceof IFile) {
						String name = curr.removeFileExtension().lastSegment();
						IFolder dest = project.getFolder("src-" + name);
						if (buildEntry == null) {
							buildEntry =
								buildModel.getFactory().createEntry(entryName);
							build.add(buildEntry);
						}
						buildEntry.addToken(dest.getName() + "/");
						if (!dest.exists()) {
							dest.create(true, true, null);
						}
						extractZipFile(
							res,
							dest,
							new SubProgressMonitor(monitor, 1));
						sourceFound = true;

						// extract resources from the library JAR
						res = root.findMember(curr);
						if (res instanceof IFile) {
							extractResources(
								res,
								dest,
								new SubProgressMonitor(monitor, 1));
							createJarPackagerFiles(dest, (IFile) res);
							// defects 16137 and 17521
							res.delete(true, monitor);
							// defect 19351
							if (dest.getFolder("META-INF").exists())
								dest.getFolder("META-INF").delete(
									true,
									monitor);
						} else {
							monitor.worked(1);
						}

					} else {
						monitor.worked(2);
					}
				} else {
					monitor.worked(2);
				}
			}
			buildModel.save();
		} finally {
			monitor.done();
		}
		return sourceFound;
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

		String string =
			constructJarPackagerFileContent(
				sourceFolder,
				jarFile,
				descriptorFile.getFullPath());

		if (!descriptorFile.exists()) {
			descriptorFile.create(
				new ByteArrayInputStream(string.getBytes()),
				true,
				null);
		} else {
			descriptorFile.setContents(
				new ByteArrayInputStream(string.getBytes()),
				true,
				true,
				null);
		}
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

	private void setProjectDescription(
		IProject project,
		boolean needsJavaNature,
		IProgressMonitor monitor)
		throws CoreException {
		IProjectDescription desc = project.getDescription();
		if (needsJavaNature)
			desc.setNatureIds(
				new String[] { JavaCore.NATURE_ID, PDE.PLUGIN_NATURE });
		else
			desc.setNatureIds(new String[] { PDE.PLUGIN_NATURE });
		project.setDescription(desc, new SubProgressMonitor(monitor, 1));
	}

}