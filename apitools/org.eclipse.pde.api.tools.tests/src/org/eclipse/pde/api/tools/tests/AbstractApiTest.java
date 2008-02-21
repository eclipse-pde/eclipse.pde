/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleFragmentModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.ui.wizards.plugin.AbstractFieldData;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.IFragmentFieldData;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.pde.ui.IPluginFieldData;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.osgi.framework.Constants;

/**
 * Abstract class with commonly used methods for API Tooling tests
 * 
 * @since 1.0.0 
 */
public class AbstractApiTest extends TestCase {
	
	static class TestFieldData implements IFieldData {
		public String getId() {return TESTING_PLUGIN_PROJECT_NAME;}
		public String getLibraryName() {return Util.getDefaultEEId();}
		public String getName() {return TESTING_PLUGIN_PROJECT_NAME;}
		public String getOutputFolderName() {return BIN_FOLDER;}
		public String getProvider() {return "ibm";}
		public String getSourceFolderName() {return SRC_FOLDER;}
		public String getVersion() {return "1.0.0";}
		public boolean hasBundleStructure() {return true;}
		public boolean isLegacy() {return false;}
		public boolean isSimple() {return true;}
		public String getTargetVersion() {return "3.4";}
	}
	
	/**
	 * Static class for an <code>IOverwriteQuery</code> implementation
	 */
	private static class ImportOverwriteQuery implements IOverwriteQuery {
		/* (non-Javadoc)
		 * @see org.eclipse.ui.dialogs.IOverwriteQuery#queryOverwrite(java.lang.String)
		 */
		public String queryOverwrite(String file) {
			return ALL;
		}	
	}	
	
	/**
	 * Constant representing the name of the testing project created for plugin tests.
	 * Value is: <code>APITests</code>.
	 */
	protected static final String TESTING_PROJECT_NAME = "APITests";
	
	/**
	 * Constant representing the name of the testing plugin project created for plugin tests.
	 * Value is: <code>APIPluginTests</code>.
	 */
	protected static final String TESTING_PLUGIN_PROJECT_NAME = "APIPluginTests";
	/**
	 * Constant representing the name of the output directory for a project.
	 * Value is: <code>bin</code>
	 */
	protected static final String BIN_FOLDER = "bin";
	
	/**
	 * Constant representing the name of the source directory for a project.
	 * Value is: <code>src</code>
	 */
	protected static final String SRC_FOLDER = "src";
	
	/**
	 * The one instance of the testing plugin project field data
	 */
	private static final IFieldData fData = new TestFieldData();
	
	/**
	 * field used as compatibility artifact for PDE code
	 */
	private static IPluginContentWizard fContentWizard = null;
	
	/**
	 * Returns the {@link IJavaProject} with the given name. If this method
	 * is called from a non-plugin unit test, <code>null</code> is always returned.
	 * @return the {@link IJavaProject} with the given name or <code>null</code>
	 */
	protected IJavaProject getTestingJavaProject(String name) {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		if(ws != null) {
			IProject pro = ws.getRoot().getProject(name);
			if(pro.exists()) {
				return JavaCore.create(pro);
			}
		}
		return null;
	}
	
	/**
	 * Creates a build.properties file for the given project
	 * @param project
	 * @throws CoreException
	 */
	private void createBuildPropertiesFile(IProject project) throws CoreException {
		IFile file = project.getFile("build.properties"); //$NON-NLS-1$
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildModelFactory factory = model.getFactory();
		
			// BIN.INCLUDES
			IBuildEntry binEntry = factory.createEntry(IBuildEntry.BIN_INCLUDES);
			fillBinIncludes(project, binEntry);
			createSourceOutputBuildEntries(model, factory);
			model.getBuild().add(binEntry);
			model.save();
		}
	}
	
	/**
	 * Creates the source output build entries for the given model / factory
	 * @param model
	 * @param factory
	 * @throws CoreException
	 */
	private void createSourceOutputBuildEntries(WorkspaceBuildModel model, IBuildModelFactory factory) throws CoreException {
		String srcFolder = fData.getSourceFolderName();
		if (!fData.isSimple() && srcFolder != null) {
			String libraryName = fData.getLibraryName();
			if (libraryName == null) {
				libraryName = "."; //$NON-NLS-1$
			}
			// SOURCE.<LIBRARY_NAME>
			IBuildEntry entry = factory.createEntry(IBuildEntry.JAR_PREFIX+ libraryName);
			if (srcFolder.length() > 0) {
				entry.addToken(new Path(srcFolder).addTrailingSeparator().toString());
			}
			else {
				entry.addToken("."); //$NON-NLS-1$
			}
			model.getBuild().add(entry);

			// OUTPUT.<LIBRARY_NAME>
			entry = factory.createEntry(IBuildEntry.OUTPUT_PREFIX + libraryName);
			String outputFolder = fData.getOutputFolderName().trim();
			if (outputFolder.length() > 0) {
				entry.addToken(new Path(outputFolder).addTrailingSeparator().toString());
			}
			else {
				entry.addToken("."); //$NON-NLS-1$
			}
			model.getBuild().add(entry);
		}
	}
	
	/**
	 * Fills in the listing of bin-includes for the build.properties file of the given project
	 * @param project
	 * @param binEntry
	 * @throws CoreException
	 */
	private void fillBinIncludes(IProject project, IBuildEntry binEntry) throws CoreException {
		if (!fData.hasBundleStructure() || fContentWizard != null) {
			binEntry.addToken(fData instanceof IFragmentFieldData ? "fragment.xml" : "plugin.xml");
		}
		if (fData.hasBundleStructure()) {
			binEntry.addToken("META-INF/"); //$NON-NLS-1$
		}
		if (!fData.isSimple()) {
			String libraryName = fData.getLibraryName();
			binEntry.addToken(libraryName == null ? "." : libraryName); //$NON-NLS-1$
		}
	}
	
	/**
	 * Performs the given refactoring
	 * @param refactoring
	 * @throws Exception
	 */
	protected void performRefactoring(final Refactoring refactoring) throws CoreException {
		if(refactoring == null) {
			return;
		}
		NullProgressMonitor monitor = new NullProgressMonitor();
		CreateChangeOperation create= new CreateChangeOperation(refactoring);
		refactoring.checkFinalConditions(monitor);
		PerformChangeOperation perform = new PerformChangeOperation(create);
		ResourcesPlugin.getWorkspace().run(perform, monitor);
	}	
	
	/**
	 * Crate a plugin project with the given name
	 * @param projectName
	 * @param additionalNatures
	 * @return a new plugin project
	 * @throws CoreException
	 */
	protected IJavaProject createPluginProject(String projectName, String[] additionalNatures) throws CoreException {
		IJavaProject project = createJavaProject(projectName, new String[] {PDE.PLUGIN_NATURE, ApiPlugin.NATURE_ID});
		IProject proj = project.getProject();
		//create a manifest file
		createManifest(proj);
		//create a build.properties file for the project
		createBuildPropertiesFile(proj);
		return project;
	}
	
	/**
	 * Creates a new manifest.mf file for the given project
	 * @param project
	 * @throws CoreException
	 */
	private void createManifest(IProject project) throws CoreException {
		WorkspacePluginModelBase base = null;
		if (fData.hasBundleStructure()) {
			if (fData instanceof IFragmentFieldData) {
				base = new WorkspaceBundleFragmentModel(project.getFile(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR), 
						project.getFile(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR));
			} else {
				base = new WorkspaceBundlePluginModel(project.getFile(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR), 
						project.getFile(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR));
			}
		} else {
			if (fData instanceof IFragmentFieldData) {
				base = new WorkspaceFragmentModel(project.getFile(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR), false);
			} else {
				base = new WorkspacePluginModel(project.getFile(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), false);
			}
		}
		IPluginBase pluginBase = base.getPluginBase();
		String targetVersion = ((TestFieldData) fData).getTargetVersion();
		pluginBase.setSchemaVersion(Double.parseDouble(targetVersion) < 3.2 ? "3.0" : "3.2"); //$NON-NLS-1$ //$NON-NLS-2$
		pluginBase.setId(fData.getId());
		pluginBase.setVersion(fData.getVersion());
		pluginBase.setName(fData.getName());
		pluginBase.setProviderName(fData.getProvider());
		if (base instanceof IBundlePluginModelBase) {
			IBundlePluginModelBase bmodel = ((IBundlePluginModelBase)base);
			((IBundlePluginBase)bmodel.getPluginBase()).setTargetVersion(targetVersion);
			bmodel.getBundleModel().getBundle().setHeader(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		}
		if (pluginBase instanceof IFragment) {
			IFragment fragment = (IFragment) pluginBase;
			IFragmentFieldData data = (IFragmentFieldData) fData;
			fragment.setPluginId(data.getPluginId());
			fragment.setPluginVersion(data.getPluginVersion());
			fragment.setRule(data.getMatch());
		} 
		if (!fData.isSimple()) {
			setPluginLibraries(base);
		}
		// add Bundle Specific fields if applicable
		if (pluginBase instanceof BundlePluginBase) {
			IBundle bundle = ((BundlePluginBase)pluginBase).getBundle();
			if (fData instanceof AbstractFieldData) {
				// Set required EE
				String exeEnvironment = ((AbstractFieldData)fData).getExecutionEnvironment();
				if(exeEnvironment != null) {
					bundle.setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, exeEnvironment);
				}
			} 
			if (fData instanceof IPluginFieldData && ((IPluginFieldData)fData).doGenerateClass()) {
				if (targetVersion.equals("3.1")) //$NON-NLS-1$
					bundle.setHeader(ICoreConstants.ECLIPSE_AUTOSTART, "true"); //$NON-NLS-1$
				else 
					bundle.setHeader(ICoreConstants.ECLIPSE_LAZYSTART, "true"); //$NON-NLS-1$
			}
		} 
		if(base != null) {
			base.save();
		}
	}
	
	/**
	 * Sets the library information from the field data into the model; creates a new library if needed
	 * @param model
	 * @throws CoreException
	 */
	private void setPluginLibraries(WorkspacePluginModelBase model) throws CoreException {
		String libraryName = fData.getLibraryName();
		if (libraryName == null && !fData.hasBundleStructure()) {
			libraryName = "."; //$NON-NLS-1$
		}
		if (libraryName != null) {
			IPluginLibrary library = model.getPluginFactory().createLibrary();
			library.setName(libraryName);
			library.setExported(!fData.hasBundleStructure());
			model.getPluginBase().add(library);
		}
	}
	
	/**
	 * creates a java project with the specified name and additional project natures
	 * @param projectName
	 * @param additionalNatures
	 * @return a new java project
	 * @throws CoreException
	 */
	protected IJavaProject createJavaProject(String projectName, String[] additionalNatures) throws CoreException {
		IProgressMonitor monitor = new NullProgressMonitor();
		IProject project = createProject(projectName, monitor);
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			addNatureToProject(project, JavaCore.NATURE_ID, monitor);
		}
		if(additionalNatures != null) {
			for(int i = 0; i < additionalNatures.length; i++) {
				addNatureToProject(project, additionalNatures[i], monitor);
			}
		}
		IJavaProject jproject = JavaCore.create(project);
		jproject.setOutputLocation(getDefaultProjectOutputLocation(project), monitor);
		jproject.setRawClasspath(new IClasspathEntry[0], monitor);
		
		return jproject;	
	}
	
	/**
	 * Gets the output location for the given project, creates it if needed
	 * @param project
	 * @return the path of the output location for the given project
	 * @throws CoreException
	 */
	private IPath getDefaultProjectOutputLocation(IProject project) throws CoreException {
		IFolder binFolder = project.getFolder(BIN_FOLDER);
		if (!binFolder.exists()) {
			binFolder.create(false, true, null);
		}
		return binFolder.getFullPath();
	}
	
	/**
	 * Creates a project with the given name in the workspace and returns it.
	 * If a project with the given name exists, it is refreshed and opened (if closed) and returned
	 * @param projectName
	 * @param monitor
	 * @return a project with the given name
	 * @throws CoreException
	 */
	private IProject createProject(String projectName, IProgressMonitor monitor) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if (!project.exists()) {
			project.create(null);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
		
		if (!project.isOpen()) {
			project.open(monitor);
		}
		return project;
	}
	
	/**
	 * Adds a container entry to the specified java project
	 * @param project
	 * @param container
	 * @throws JavaModelException
	 */
	protected void addContainerEntry(IJavaProject project, IPath container) throws JavaModelException {
		IClasspathEntry cpe = JavaCore.newContainerEntry(container, false);
		addToClasspath(project, cpe);
	}
	
	/**
	 * Recursively adds files from the specified directory to the provided list
	 * @param dir
	 * @param collection
	 * @throws IOException
	 */
	protected void addJavaFiles(File dir, List collection) throws IOException {
		File[] files = dir.listFiles();
		List subDirs = new ArrayList(2);
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				collection.add(files[i]);
			} else if (files[i].isDirectory()) {
				subDirs.add(files[i]);
			}
		}
		Iterator iter = subDirs.iterator();
		while (iter.hasNext()) {
			File subDir = (File)iter.next();
			addJavaFiles(subDir, collection);
		}
	}
	
	/**
	 * Imports files from the specified root directory into the specified path
	 * @param rootDir
	 * @param destPath
	 * @param monitor
	 * @throws InvocationTargetException
	 * @throws IOException
	 */
	protected void importFilesFromDirectory(File rootDir, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException, IOException {		
		IImportStructureProvider structureProvider = FileSystemStructureProvider.INSTANCE;
		List files = new ArrayList(100);
		addJavaFiles(rootDir, files);
		try {
			ImportOperation op= new ImportOperation(destPath, rootDir, structureProvider, new ImportOverwriteQuery(), files);
			op.setCreateContainerStructure(false);
			op.run(monitor);
		} catch (InterruptedException e) {
			// should not happen
		}
	}
	
	/**
	 * Imports the specified file to the destination path 
	 * @param file
	 * @param destPath
	 * @param monitor
	 * @throws InvocationTargetException
	 * @throws IOException
	 */
	protected void importFileFromDirectory(File file, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException, IOException {		
		IImportStructureProvider structureProvider = FileSystemStructureProvider.INSTANCE;
		List files = new ArrayList(1);
		files.add(file);
		try {
			ImportOperation ioperation = new ImportOperation(destPath, file.getParentFile(), structureProvider, new ImportOverwriteQuery(), files);
			ioperation.setCreateContainerStructure(false);
			ioperation.run(monitor);
			IStatus status = ioperation.getStatus();
			if(!status.isOK()) {
				fail("the file: ["+file.getName()+"] could not be imported");
			}
		} catch (InterruptedException e) {
			// should not happen
		}
	}
	
	/**
	 * Adds the specified classpath entry to the specified java project
	 * @param jproject
	 * @param cpe
	 * @throws JavaModelException
	 */
	protected void addToClasspath(IJavaProject jproject, IClasspathEntry cpe) throws JavaModelException {
		boolean found = false;
		IClasspathEntry[] entries = jproject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			if (entriesEqual(entries[i], cpe)) {
				entries[i] = cpe;
				found = true;
			}
		}
		if(!found) {
			int nEntries = entries.length;
			IClasspathEntry[] newEntries = new IClasspathEntry[nEntries + 1];
			System.arraycopy(entries, 0, newEntries, 0, nEntries);
			newEntries[nEntries] = cpe;
			entries = newEntries;
		}
		jproject.setRawClasspath(entries, true, new NullProgressMonitor());
	}  
	
	/**
	 * Delegate equals method to cover the test cases where we want to insert an updated element 
	 * and one with the same path/type/kind is already there. 
	 * @param e1
	 * @param e2
	 * @return
	 */
	private boolean entriesEqual(IClasspathEntry e1, IClasspathEntry e2) {
		return e1.equals(e2) || (e1.getEntryKind() == e2.getEntryKind() && e1.getContentKind() == e2.getContentKind() && e1.getPath().equals(e2.getPath()));
	}
	
	/**
	 * Removes the specified entry from the classpath of the specified project
	 * @param project
	 * @param entry
	 * @throws JavaModelException
	 */
	protected void removeFromClasspath(IJavaProject project, IClasspathEntry entry) throws JavaModelException {
		IClasspathEntry[] oldEntries = project.getRawClasspath();
		ArrayList entries = new ArrayList();
		for (int i= 0; i < oldEntries.length; i++) {
			if (!oldEntries[i].equals(entry)) {
				entries.add(oldEntries[i]);
			}
		}
		if(entries.size() != oldEntries.length) {
			project.setRawClasspath((IClasspathEntry[])entries.toArray(new IClasspathEntry[entries.size()]), new NullProgressMonitor());
		}
	}
	
	/**
	 * Adds a new source container specified by the container name to the source path of the specified project
	 * @param jproject
	 * @param containerName
	 * @return the package fragment root of the container name
	 * @throws CoreException
	 */
	protected IPackageFragmentRoot addSourceContainer(IJavaProject jproject, String containerName) throws CoreException {
		IProject project = jproject.getProject();
		IPackageFragmentRoot root = jproject.getPackageFragmentRoot(addFolderToProject(project, containerName));
		IClasspathEntry cpe = JavaCore.newSourceEntry(root.getPath());
		addToClasspath(jproject, cpe);
		return root;
	}  
	
	/**
	 * Adds a folder with the given name to the specified project
	 * @param project
	 * @param name
	 * @return the new container added to the specified project
	 * @throws CoreException
	 */
	protected IContainer addFolderToProject(IProject project, String name) throws CoreException {
		IContainer container= null;
		if (name == null || name.length() == 0) {
			container = project;
		} else {
			IFolder folder = project.getFolder(name);
			if (!folder.exists()) {
				folder.create(false, true, null);
			}
			container = folder;
		}
		return container;
	}
	
	/**
	 * Adds the specified nature to the specified project
	 * @param proj
	 * @param natureId
	 * @param monitor
	 * @throws CoreException
	 */
	protected void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}
	/**
	 * Wait for autobuild notification to occur
	 */
	public static void waitForAutoBuild() {
		boolean wasInterrupted = false;
		do {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				wasInterrupted = false;
			} catch (OperationCanceledException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				wasInterrupted = true;
			}
		} while (wasInterrupted);
	}
}
