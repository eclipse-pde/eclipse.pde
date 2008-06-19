/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests.util;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleFragmentModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.BundleModelFactory;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.PackageFriend;
import org.eclipse.pde.internal.ui.wizards.plugin.AbstractFieldData;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.IFragmentFieldData;
import org.eclipse.pde.ui.IPluginFieldData;
import org.osgi.framework.Constants;

/**
 * Util class for a variety of project related operations
 * 
 * @since 1.0.0
 */
public class ProjectUtils {
	
	/**
	 * Class for testing field data for a testing plugin project
	 */
	static class TestFieldData implements IFieldData {
		String pname = null;
		String srcfolder = null;
		String binfolder = null;
		
		/**
		 * Constructor
		 * @param pname
		 * @param src
		 * @param bin
		 */
		protected TestFieldData(String pname, String src, String bin) {
			this.pname = pname;
			this.srcfolder = src;
			this.binfolder = bin;
		}
		
		public String getId() {return pname;}
		public String getLibraryName() {return org.eclipse.pde.api.tools.internal.util.Util.getDefaultEEId();}
		public String getName() {return pname;}
		public String getOutputFolderName() {return binfolder;}
		public String getProvider() {return "ibm";}
		public String getSourceFolderName() {return srcfolder;}
		public String getVersion() {return "1.0.0";}
		public boolean hasBundleStructure() {return true;}
		public boolean isLegacy() {return false;}
		public boolean isSimple() {return true;}
		public String getTargetVersion() {return "3.4";}
	}
	
	/**
	 * Constant representing the name of the output directory for a project.
	 * Value is: <code>bin</code>
	 */
	public static final String BIN_FOLDER = "bin";
	
	/**
	 * Constant representing the name of the source directory for a project.
	 * Value is: <code>src</code>
	 */
	public static final String SRC_FOLDER = "src";
	
	/**
	 * Crate a plugin project with the given name
	 * @param projectName
	 * @param additionalNatures
	 * @return a new plugin project
	 * @throws CoreException
	 */
	public static IJavaProject createPluginProject(String projectName, String[] additionalNatures) throws CoreException {
		String[] resolvednatures = additionalNatures;
		if(additionalNatures != null) {
			ArrayList natures = new ArrayList(Arrays.asList(additionalNatures));
			if(!natures.contains(PDE.PLUGIN_NATURE)) {
				//need to always set this one first, in case others depend on it, like API tooling does
				natures.add(0, PDE.PLUGIN_NATURE);
			}
			resolvednatures = (String[]) natures.toArray(new String[natures.size()]);
		}
		IJavaProject project = createJavaProject(projectName, resolvednatures);
		IProject proj = project.getProject();
		//create a manifest file
		IFieldData data = new TestFieldData(projectName, SRC_FOLDER, BIN_FOLDER);
		createManifest(proj, data);
		//create a build.properties file for the project
		createBuildPropertiesFile(proj, data);
		return project;
	}

	/**
	 * creates a java project with the specified name and additional project natures
	 * @param projectName
	 * @param additionalNatures
	 * @return a new java project
	 * @throws CoreException
	 */
	public static IJavaProject createJavaProject(String projectName, String[] additionalNatures) throws CoreException {
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
	public static IPath getDefaultProjectOutputLocation(IProject project) throws CoreException {
		IFolder binFolder = project.getFolder(BIN_FOLDER);
		if (!binFolder.exists()) {
			binFolder.create(false, true, null);
		}
		return binFolder.getFullPath();
	}
	
	/**
	 * Adds a new source container specified by the container name to the source path of the specified project
	 * @param jproject
	 * @param containerName
	 * @return the package fragment root of the container name
	 * @throws CoreException
	 */
	public static IPackageFragmentRoot addSourceContainer(IJavaProject jproject, String containerName) throws CoreException {
		IProject project = jproject.getProject();
		IPackageFragmentRoot root = jproject.getPackageFragmentRoot(addFolderToProject(project, containerName));
		IClasspathEntry cpe = JavaCore.newSourceEntry(root.getPath());
		addToClasspath(jproject, cpe);
		return root;
	}  
	
	/**
	 * Adds a container entry to the specified java project
	 * @param project
	 * @param container
	 * @throws JavaModelException
	 */
	public static void addContainerEntry(IJavaProject project, IPath container) throws JavaModelException {
		IClasspathEntry cpe = JavaCore.newContainerEntry(container, false);
		addToClasspath(project, cpe);
	}
	
	/**
	 * Adds a folder with the given name to the specified project
	 * @param project
	 * @param name
	 * @return the new container added to the specified project
	 * @throws CoreException
	 */
	public static IContainer addFolderToProject(IProject project, String name) throws CoreException {
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
	 * Adds the specified classpath entry to the specified java project
	 * @param jproject
	 * @param cpe
	 * @throws JavaModelException
	 */
	public static void addToClasspath(IJavaProject jproject, IClasspathEntry cpe) throws JavaModelException {
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
	 * Removes the specified entry from the classpath of the specified project
	 * @param project
	 * @param entry
	 * @throws JavaModelException
	 */
	public static void removeFromClasspath(IJavaProject project, IClasspathEntry entry) throws JavaModelException {
		IClasspathEntry[] oldEntries = project.getRawClasspath();
		ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		for (int i= 0; i < oldEntries.length; i++) {
			if (!oldEntries[i].equals(entry)) {
				entries.add(oldEntries[i]);
			}
		}
		if(entries.size() != oldEntries.length) {
			project.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), new NullProgressMonitor());
		}
	}
	
	/**
	 * Delegate equals method to cover the test cases where we want to insert an updated element 
	 * and one with the same path/type/kind is already there. 
	 * @param e1
	 * @param e2
	 * @return
	 */
	private static boolean entriesEqual(IClasspathEntry e1, IClasspathEntry e2) {
		return e1.equals(e2) || (e1.getEntryKind() == e2.getEntryKind() && e1.getContentKind() == e2.getContentKind() && e1.getPath().equals(e2.getPath()));
	}
	
	/**
	 * Creates a project with the given name in the workspace and returns it.
	 * If a project with the given name exists, it is refreshed and opened (if closed) and returned
	 * @param projectName
	 * @param monitor
	 * @return a project with the given name
	 * @throws CoreException
	 */
	public static IProject createProject(String projectName, IProgressMonitor monitor) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if (!project.exists()) {
			project.create(monitor);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
		if (!project.isOpen()) {
			project.open(monitor);
		}
		return project;
	}
	
	/**
	 * Adds the specified nature to the specified project
	 * @param proj
	 * @param natureId
	 * @param monitor
	 * @throws CoreException
	 */
	public static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}
	
	/**
	 * Creates a build.properties file for the given project
	 * @param project
	 * @throws CoreException
	 */
	public static void createBuildPropertiesFile(IProject project, final IFieldData data) throws CoreException {
		IFile file = project.getFile("build.properties"); //$NON-NLS-1$
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildModelFactory factory = model.getFactory();
		
			// BIN.INCLUDES
			IBuildEntry binEntry = factory.createEntry(IBuildEntry.BIN_INCLUDES);
			fillBinIncludes(project, data, binEntry);
			createSourceOutputBuildEntries(model, data, factory);
			model.getBuild().add(binEntry);
			model.save();
		}
	}
	
	/**
	 * Creates the source output build entries for the given model / factory
	 * @param model
	 * @param data
	 * @param factory
	 * @throws CoreException
	 */
	public static void createSourceOutputBuildEntries(WorkspaceBuildModel model, final IFieldData data, IBuildModelFactory factory) throws CoreException {
		String srcFolder = data.getSourceFolderName();
		if (!data.isSimple() && srcFolder != null) {
			String libraryName = data.getLibraryName();
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
			String outputFolder = data.getOutputFolderName().trim();
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
	private static void fillBinIncludes(IProject project, final IFieldData data, IBuildEntry binEntry) throws CoreException {
		if (!data.hasBundleStructure()) {
			binEntry.addToken(data instanceof IFragmentFieldData ? "fragment.xml" : "plugin.xml");
		}
		if (data.hasBundleStructure()) {
			binEntry.addToken("META-INF/"); //$NON-NLS-1$
		}
		if (!data.isSimple()) {
			String libraryName = data.getLibraryName();
			binEntry.addToken(libraryName == null ? "." : libraryName); //$NON-NLS-1$
		}
	}
	
	/**
	 * Creates a new manifest.mf file for the given project
	 * @param project
	 * @throws CoreException
	 */
	public static void createManifest(IProject project, final IFieldData data) throws CoreException {
		WorkspacePluginModelBase base = null;
		if (data.hasBundleStructure()) {
			if (data instanceof IFragmentFieldData) {
				base = new WorkspaceBundleFragmentModel(project.getFile(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR), 
						project.getFile(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR));
			} else {

				base = new WorkspaceBundlePluginModel(project.getFile(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR), 
						project.getFile(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR));
			}
		} else {
			if (data instanceof IFragmentFieldData) {
				base = new WorkspaceFragmentModel(project.getFile(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR), false);
			} else {
				base = new WorkspacePluginModel(project.getFile(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR), false);
			}
		}
		IPluginBase pluginBase = base.getPluginBase();
		String targetVersion = ((TestFieldData) data).getTargetVersion();
		pluginBase.setSchemaVersion(Double.parseDouble(targetVersion) < 3.2 ? "3.0" : "3.2"); //$NON-NLS-1$ //$NON-NLS-2$
		pluginBase.setId(data.getId());
		pluginBase.setVersion(data.getVersion());
		pluginBase.setName(data.getName());
		pluginBase.setProviderName(data.getProvider());
		if (base instanceof IBundlePluginModelBase) {
			IBundlePluginModelBase bmodel = ((IBundlePluginModelBase)base);
			((IBundlePluginBase)bmodel.getPluginBase()).setTargetVersion(targetVersion);
			bmodel.getBundleModel().getBundle().setHeader(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		}
		if (pluginBase instanceof IFragment) {
			IFragment fragment = (IFragment) pluginBase;
			IFragmentFieldData fdata = (IFragmentFieldData) data;
			fragment.setPluginId(fdata.getPluginId());
			fragment.setPluginVersion(fdata.getPluginVersion());
			fragment.setRule(fdata.getMatch());
		} 
		if (!data.isSimple()) {
			setPluginLibraries(base, data);
		}
		// add Bundle Specific fields if applicable
		if (pluginBase instanceof BundlePluginBase) {
			IBundle bundle = ((BundlePluginBase)pluginBase).getBundle();
			if (data instanceof AbstractFieldData) {
				// Set required EE
				String exeEnvironment = ((AbstractFieldData)data).getExecutionEnvironment();
				if(exeEnvironment != null) {
					bundle.setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, exeEnvironment);
				}
			} 
			if (data instanceof IPluginFieldData && ((IPluginFieldData)data).doGenerateClass()) {
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
	 * Removes the given package from the exported packages header, if it exists.
	 * 
	 * This method is not safe to use in a headless manner.
	 * 
	 * @param project the project to remove the package from
	 * @param packagename the name of the package to remove from the export package header
	 */
	public static void removeExportedPackage(IProject project, String packagename) {
		IBundle bundle = getBundle(project);
		if(bundle == null) {
			return;
		}
		try {
			ExportPackageHeader header = getExportedPackageHeader(bundle);
			Object removed = header.removePackage(packagename);
			if(removed != null) {
				bundle.setHeader(Constants.EXPORT_PACKAGE, header.getValue());
			}
		}
		finally {
			WorkspaceBundleModel model = (WorkspaceBundleModel) bundle.getModel();
			if(model.isDirty()) {
				model.save();
				if(!model.isInSync()) {
					model.load();
				}
			}
		}
	}
	
	/**
	 * Returns the {@link IBundle} for the given {@link IProject} if one exists. Otherwise <code>null</code> is returned.
	 * 
	 * This method is not safe to use in a headless manner.
	 * 
	 * @param project the project to get the {@link IBundle} for
	 * @return the {@link IBundle} for the given project or <code>null</code> if one cannot be created
	 */
	public static IBundle getBundle(IProject project) {
		IFile manifest = project.getFile(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
		if(manifest == null || !manifest.exists()) {
			return null;
		}
		WorkspacePluginModelBase plugin = new WorkspaceBundlePluginModel(manifest, project.getFile(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR));
		IPluginBase pluginBase = plugin.getPluginBase();
		if (pluginBase instanceof BundlePluginBase) {
			return ((BundlePluginBase)pluginBase).getBundle();
		}
		return null;
	}
	
	/**
	 * Adds a new exported package to the manifest.
	 * 
	 * This method is not safe to use in a headless manner.
	 * 
	 * @param project the project to get the manifest information from
	 * @param packagename the fully qualified name of the package to add 
	 * @param internal if the added package should be internal or not
	 * @param friends a listing of friends for this exported package
	 * @throws CoreException if something bad happens
	 */
	public static void addExportedPackage(IProject project, String packagename, boolean internal, String[] friends) throws CoreException {
		if(!project.exists() || packagename == null) {
			//do not work
			return;
		}
		IFile manifest = project.getFile(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
		if(manifest == null || !manifest.exists()) {
			createManifest(project, new TestFieldData(project.getName(), SRC_FOLDER, BIN_FOLDER));
			manifest = project.getFile(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
		}
		IBundle bundle = getBundle(project);
		if (bundle != null) {
			ExportPackageObject pack = null;
			try {
				ExportPackageHeader header = getExportedPackageHeader(bundle);
				if(header != null) {
					pack = header.addPackage(packagename);
					if(internal != pack.isInternal()) {
						pack.setInternal(internal);
					}
					if(friends != null) {
						for(int i = 0; i < friends.length; i++) {
							if(friends[i] != null) {
								pack.addFriend(new PackageFriend(pack, friends[i]));
							}
						}
					}
					bundle.setHeader(Constants.EXPORT_PACKAGE, header.getValue());
				}
			}
			finally {
				WorkspaceBundleModel model = (WorkspaceBundleModel) bundle.getModel();
				if(model.isDirty()) {
					model.save();
					if(!model.isInSync()) {
						model.load();
					}
				}
			}
		}
	}
	
	/**
	 * Returns the {@link ExportPackageHeader} for the given {@link IBundle}.
	 * @param bundle the bundle to get the header from
	 * @return new {@link ExportPackageHeader} for the given {@link IBundle}
	 */
	public static ExportPackageHeader getExportedPackageHeader(IBundle bundle) {
		IManifestHeader header = bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		BundleModelFactory factory = new BundleModelFactory(bundle.getModel());
		String value = "";
		if(header != null && header.getValue() != null) {
			value = header.getValue();
		}
		return (ExportPackageHeader) factory.createHeader(Constants.EXPORT_PACKAGE, value);
	}
	
	/**
	 * Sets the library information from the field data into the model; creates a new library if needed
	 * @param model
	 * @throws CoreException
	 */
	public static void setPluginLibraries(WorkspacePluginModelBase model, final IFieldData data) throws CoreException {
		String libraryName = data.getLibraryName();
		if (libraryName == null && !data.hasBundleStructure()) {
			libraryName = "."; //$NON-NLS-1$
		}
		if (libraryName != null) {
			IPluginLibrary library = model.getPluginFactory().createLibrary();
			library.setName(libraryName);
			library.setExported(!data.hasBundleStructure());
			model.getPluginBase().add(library);
		}
	}
}
