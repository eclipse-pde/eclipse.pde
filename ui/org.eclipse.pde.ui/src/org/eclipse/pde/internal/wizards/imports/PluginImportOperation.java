/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.model.LibraryModel;
import org.eclipse.core.runtime.model.PluginModel;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.pde.selfhosting.internal.SelfHostingPlugin;


public class PluginImportOperation implements IWorkspaceRunnable {
		
	public interface IReplaceQuery {
		
		// return codes
		public static final int CANCEL= 0;
		public static final int NO= 1;
		public static final int YES= 2;
		
		/**
		 * Do the callback. Returns YES, NO or CANCEL
		 */
		int doQuery(IProject project);
	}
	
	private PluginModel[] fPlugins;
	private boolean fDoImport;
	private boolean fDoExtractSource;
	
	private IWorkspaceRoot fRoot;
	private IReplaceQuery fReplaceQuery;
	
	public PluginImportOperation(PluginModel[] plugins, boolean doImport, boolean doExtractSource, IReplaceQuery replaceQuery) {
		Assert.isNotNull(plugins);
		Assert.isNotNull(replaceQuery);
		fPlugins=plugins;
		fDoExtractSource= doExtractSource;
		fDoImport= doExtractSource ? true : doImport;
		
		fRoot= ResourcesPlugin.getWorkspace().getRoot();
		fReplaceQuery= replaceQuery;
	}

	/*
	 * @see IWorkspaceRunnable#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask("Creating projects from plugins...", fPlugins.length);
		try {
			MultiStatus multiStatus= new MultiStatus(SelfHostingPlugin.PLUGIN_ID, IStatus.OK, "Import Plugins", null);
			for (int i= 0; i < fPlugins.length; i++) {
				try {
					createProject(fPlugins[i], new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					IStatus status= e.getStatus();
					String newMessage= "Problem while importing plugin '" + fPlugins[i].getId() + "': " + status.getMessage();
					IStatus newStatus= new Status(status.getSeverity(), SelfHostingPlugin.PLUGIN_ID, status.getCode(), newMessage, status.getException());
					multiStatus.add(newStatus);
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

	private void createProject(PluginModel plugin, IProgressMonitor monitor) throws CoreException {
		String name= plugin.getId();
		monitor.beginTask("Creating " + name + "...", 7);
		try {
			File pluginDir= new File(new URL(plugin.getLocation()).getFile());
			IPath pluginPath= new Path(pluginDir.getPath());
			
			IProject project= fRoot.getProject(name);
			if (project.exists()) {
				if (queryReplace(project)) {
					boolean deleteContent=  fDoImport && fRoot.getLocation().isPrefixOf(project.getLocation());
					project.delete(deleteContent, true, new SubProgressMonitor(monitor, 1));
				} else {
					return;
				}
			} else {
				monitor.worked(1);
			}

			IProjectDescription desc= fRoot.getWorkspace().newProjectDescription(project.getName());	
			if (!fDoImport) {
				desc.setLocation(pluginPath);
			}
		
			project.create(desc, new SubProgressMonitor(monitor, 1));
			if (!project.isOpen()) {
				project.open(null);
			}
			
			if (fDoImport) {
				importContent(pluginDir, project.getFullPath(), FileSystemStructureProvider.INSTANCE, null, new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}
			
			desc= project.getDescription();
			desc.setNatureIds(new String[] { JavaCore.NATURE_ID });
			project.setDescription(desc, new SubProgressMonitor(monitor, 1));
			
			IPath outputLocation= project.getFullPath();
					
			LibraryModel[] libraries= plugin.getRuntime();
			
			IClasspathEntry[] classpathEntries= null;
			if (libraries != null) {
				classpathEntries= new IClasspathEntry[libraries.length];
				for (int i= 0; i < libraries.length; i++) {
					classpathEntries[i]= UpdateClasspathOperation.getLibraryEntry(project, libraries[i], true);
				}
				if (fDoExtractSource) {
					doExtractSource(project, classpathEntries, new SubProgressMonitor(monitor, 1));
				} else {
					monitor.worked(1);
				}
				outputLocation= project.getFullPath().append("bin");
			} else {
				classpathEntries= new IClasspathEntry[0];
				outputLocation= project.getFullPath();
				monitor.worked(1);
			}
			IJavaProject jproject= JavaCore.create(project);
			UpdateClasspathOperation op= new UpdateClasspathOperation(jproject, plugin, classpathEntries, outputLocation);
			op.run(new SubProgressMonitor(monitor, 2));
		} catch (MalformedURLException e) {
			SelfHostingPlugin.log(e);
		} finally {
			monitor.done();
		}
	}
		
	private void importContent(Object source, IPath destPath, IImportStructureProvider provider, List filesToImport, IProgressMonitor monitor) throws CoreException {
		IOverwriteQuery query= new IOverwriteQuery() {
			public String queryOverwrite(String file) {
				return ALL;
			}
		};
		ImportOperation op= new ImportOperation(destPath, source, provider, query);
		op.setCreateContainerStructure(false);
		if (filesToImport != null) {
			op.setFilesToImport(filesToImport);
		}
		
		try {
			op.run(monitor);
		} catch (InvocationTargetException e) {
			Throwable th= e.getTargetException();
			if (th instanceof CoreException) {
				throw (CoreException) th;
			}
			IStatus status= new Status(IStatus.ERROR, SelfHostingPlugin.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} catch (InterruptedException e) {
			throw new OperationCanceledException(e.getMessage());
		} 
	}
	
	/**
	 * Tries to find the source archives for library entries. If found, it is imported and the library classpath entry 
	 * is replaced by a source classpath entry.
	 */
	private void doExtractSource(IProject project, IClasspathEntry[] entries, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Extracting...", entries.length * 2);
		try {
			for (int i= 0; i < entries.length; i++) {
				IClasspathEntry entry= entries[i];
				IPath curr= entry.getPath();
				
				IPath sourceAttach= entry.getSourceAttachmentPath();
				if (sourceAttach != null) {
					IResource res= fRoot.findMember(sourceAttach);
					if (res instanceof IFile) {
						String name= curr.removeFileExtension().lastSegment();
						IFolder dest= project.getFolder("src-" + name);
						if (!dest.exists()) {
							dest.create(true, true, null);
						}							
						extractZipFile(res, dest, new SubProgressMonitor(monitor, 1));
						// extract resources from the library JAR
						res= fRoot.findMember(curr);
						if (res instanceof IFile) {
							extractResources(res, dest, new SubProgressMonitor(monitor, 1));
							createJarPackagerFiles(dest, (IFile)res);
						} else {
							monitor.worked(1);
						}
						// replace the entry
						entries[i]= JavaCore.newSourceEntry(dest.getFullPath());
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

	private void extractZipFile(IResource res, IFolder dest, IProgressMonitor monitor) throws CoreException {
		ZipFile zipFile= null;
		try {
			zipFile= new ZipFile(res.getLocation().toFile());
			ZipFileStructureProvider provider= new ZipFileStructureProvider(zipFile);
			importContent(provider.getRoot(), dest.getFullPath(), provider, null, monitor);
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, SelfHostingPlugin.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} finally {
			if (zipFile != null) {
				try { zipFile.close(); } catch (IOException e) {}
			}
		}
	}
	
	private void extractResources(IResource res, IFolder dest, IProgressMonitor monitor) throws CoreException {
		ZipFile zipFile= null;
		try {
			zipFile= new ZipFile(res.getLocation().toFile());
			ZipFileStructureProvider provider= new ZipFileStructureProvider(zipFile);
			ArrayList collected= new ArrayList();
			collectResources(provider, provider.getRoot(), collected);
			
			importContent(provider.getRoot(), dest.getFullPath(), provider, collected, monitor);
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, SelfHostingPlugin.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} finally {
			if (zipFile != null) {
				try { zipFile.close(); } catch (IOException e) {}
			}
		}
	}
	
	private void collectResources(ZipFileStructureProvider provider, Object element, ArrayList collected) {
		List children= provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i= 0; i < children.size(); i++) {
				Object curr= children.get(i);
				if (provider.isFolder(curr)) {
					collectResources(provider, curr, collected);
				} else if (!provider.getLabel(curr).endsWith(".class")) {
					collected.add(curr);
				}
			}
		}
	}
	
	private void createJarPackagerFiles(IFolder sourceFolder, IFile jarFile) throws CoreException{
		IProject project= sourceFolder.getProject();
		IFolder scriptsFolder= project.getFolder("scripts");
		if (!scriptsFolder.exists()) {
			scriptsFolder.create(true, true, null);
		}
		IFile descriptorFile= scriptsFolder.getFile(sourceFolder.getName() + ".jardesc");
		if (descriptorFile.exists()) {
			descriptorFile.delete(true, null);
		}
		
		String string= constructJarPackagerFileContent(sourceFolder, jarFile, descriptorFile.getFullPath());
			
		descriptorFile.create(new ByteArrayInputStream(string.getBytes()), true, null);
	}
	
	private String constructJarPackagerFileContent(IFolder folder, IFile jarFile, IPath descriptorPath) {
		IPath folderPath= folder.getFullPath();
		IPath jarPath= jarFile.getFullPath();
		String name= folderPath.lastSegment();
		String jarName= jarPath.lastSegment();
		IPath manifestPath= folderPath.append("META-INF").append("MANIFEST.MF");
		
		IPackageFragmentRoot root= JavaCore.create(folder.getProject()).getPackageFragmentRoot(folder);
		
		StringBuffer buf= new StringBuffer();
		String lineDelim= System.getProperty("line.separator", "\n");
		
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); buf.append(lineDelim);
		buf.append("<jardesc>"); buf.append(lineDelim);
		buf.append("\t<jar path=\""); buf.append(jarFile.getLocation().toString()); buf.append("\"/>"); buf.append(lineDelim);
		buf.append("\t<options compress=\"true\""); buf.append(lineDelim);
		buf.append("\t\tdescriptionLocation=\"");  buf.append(descriptorPath.toString()); buf.append('"'); buf.append(lineDelim);
		buf.append("\t\texportErrors=\"true\" exportWarnings=\"true\" logErrors=\"true\""); buf.append(lineDelim);
		buf.append("\t\tlogWarnings=\"true\" overwrite=\"false\" saveDescription=\"true\" useSourceFolders=\"false\"/>"); buf.append(lineDelim);
		buf.append("\t<manifest generateManifest=\"false\""); buf.append(lineDelim);
    	buf.append("\t\tmanifestLocation=\""); buf.append(manifestPath.toString()); buf.append('"'); buf.append(lineDelim);
    	buf.append("\t\tmanifestVersion=\"1.0\" reuseManifest=\"false\" saveManifest=\"false\" usesManifest=\"false\">"); buf.append(lineDelim);
        buf.append("\t\t<sealing sealJar=\"false\"><packagesToSeal/><packagesToUnSeal/></sealing>"); buf.append(lineDelim);
        buf.append("\t</manifest>"); buf.append(lineDelim);
        buf.append("\t<selectedElements exportClassFiles=\"true\" exportJavaFiles=\"false\">"); buf.append(lineDelim);
        buf.append("\t\t<javaElement handleIdentifier=\""); buf.append(root.getHandleIdentifier()); buf.append("\"/>"); buf.append(lineDelim);
        buf.append("\t</selectedElements>"); buf.append(lineDelim);
        buf.append("</jardesc>"); buf.append(lineDelim);
        
        return buf.toString();
	}		

		
	private boolean queryReplace(IProject project) throws OperationCanceledException {
		switch (fReplaceQuery.doQuery(project)) {
			case IReplaceQuery.CANCEL:
				throw new OperationCanceledException();
			case IReplaceQuery.NO:
				return false;
		}
		return true;
	}

}

