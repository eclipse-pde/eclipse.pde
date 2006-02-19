/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Vector;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.internal.core.PDEPluginConverter;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.plugin.ClasspathComputer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
	
public class ConvertedProjectsPage extends WizardPage  {
	private CheckboxTableViewer projectViewer;
	private TablePart tablePart;
	private IProject[] fSelected;
	private IProject[] fUnconverted;
	
	private String fLibraryName;
	private String[] fSrcEntries;
	private String[] fLibEntries;
	
	public class ProjectContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {	
			if (fUnconverted!= null)
				return fUnconverted;
			return new Object[0];
		}		
	}

	public class ProjectLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (index == 0) 
				return ((IProject) obj).getName();
			return ""; //$NON-NLS-1$
		}
		public Image getColumnImage(Object obj, int index) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		}
	}

	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String mainLabel) {
			super(mainLabel);
		}
		public void updateCounter(int count) {
			super.updateCounter(count);
			setPageComplete(count > 0);
		}
	}

	public ConvertedProjectsPage(IProject[] projects, Vector initialSelection) {
		super("convertedProjects"); //$NON-NLS-1$
		setTitle(PDEUIMessages.ConvertedProjectWizard_title);
		setDescription(PDEUIMessages.ConvertedProjectWizard_desc);
		tablePart = new TablePart(PDEUIMessages.ConvertedProjectWizard_projectList);
		this.fSelected = (IProject[])initialSelection.toArray(new IProject[initialSelection.size()]);
		this.fUnconverted = projects;
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		container.setLayout(layout);

		tablePart.createControl(container);

		projectViewer = tablePart.getTableViewer();
		projectViewer.setContentProvider(new ProjectContentProvider());
		projectViewer.setLabelProvider(new ProjectLabelProvider());
		projectViewer.setInput(PDEPlugin.getWorkspace());
	
		tablePart.setSelection(fSelected);
		tablePart.updateCounter(fSelected.length);

		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.CONVERTED_PROJECTS);
	}


	private String createInitialName(String id) {
		int loc = id.lastIndexOf('.');
		if (loc == -1)
			return id;
		StringBuffer buf = new StringBuffer(id.substring(loc + 1));
		buf.setCharAt(0, Character.toUpperCase(buf.charAt(0)));
		return buf.toString();
	}
	private void createManifestFile(IFile file, IProgressMonitor monitor)
		throws CoreException {
		WorkspacePluginModel model = new WorkspacePluginModel(file, false);
		model.load();
		IPlugin plugin = model.getPlugin();
		plugin.setSchemaVersion("3.0"); //$NON-NLS-1$
		plugin.setId(getValidId(file.getProject().getName()));
		plugin.setName(createInitialName(plugin.getId()));
		plugin.setVersion("1.0.0"); //$NON-NLS-1$
		
		IPluginModelFactory factory = model.getPluginFactory();
		IPluginBase base = model.getPluginBase();
		if (fLibraryName != null && !fLibraryName.equals(".")) { //$NON-NLS-1$
			IPluginLibrary library = factory.createLibrary();
			library.setName(fLibraryName);
			library.setExported(true);
			base.add(library);
		}
		for (int i = 0; i < fLibEntries.length; i++) {
			IPluginLibrary library = factory.createLibrary();
			library.setName(fLibEntries[i]);
			library.setExported(true);
			base.add(library);
		}
		model.save();
		PDEPluginConverter.convertToOSGIFormat(file.getProject(), TargetPlatform.getTargetVersionString(), null, new SubProgressMonitor(monitor, 1));
		organizeExports(file.getProject());
		file.delete(true, null);
	}

	private String getValidId(String name) {
		return name.replaceAll("[^a-zA-Z0-9\\._]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private boolean isOldTarget() {
		return TargetPlatform.getTargetVersion() < 3.1;
	}
	
	public boolean finish() {
		final Object [] selected = tablePart.getSelection();
		
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					convertProjects(selected, monitor);
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

	public void convertProject(IProject project, IProgressMonitor monitor)
			throws CoreException {
		
		CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);
		
		loadClasspathEntries(project, monitor);
		loadLibraryName(project);
		
		if (!WorkspaceModelManager.isPluginProject(project))
			createManifestFile(project.getFile("plugin.xml"), monitor); //$NON-NLS-1$
		IFile buildFile = project.getFile("build.properties"); //$NON-NLS-1$
		if (!buildFile.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildFile);
			IBuild build = model.getBuild(true);
			IBuildEntry entry = model.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
			if (project.getFile("plugin.xml").exists()) //$NON-NLS-1$
				entry.addToken("plugin.xml"); //$NON-NLS-1$
			if (project.getFile("META-INF/MANIFEST.MF").exists()) //$NON-NLS-1$
				entry.addToken("META-INF/"); //$NON-NLS-1$
			for (int i = 0; i < fLibEntries.length; i++) {
				entry.addToken(fLibEntries[i]);
			}
			
			if (fSrcEntries.length > 0) {
				entry.addToken(fLibraryName);
				IBuildEntry source = model.getFactory().createEntry("source." + fLibraryName); //$NON-NLS-1$
				for (int i = 0; i < fSrcEntries.length; i++) {
					source.addToken(fSrcEntries[i]);
				}
				build.add(source);
			}
			if (entry.getTokens().length > 0)
				build.add(entry);
			
			model.save();
		}
	}
	
	private void convertProjects(Object[] selected, IProgressMonitor monitor)
			throws CoreException {
		monitor.beginTask(PDEUIMessages.ConvertedProjectWizard_converting, selected.length);
		for (int i = 0; i < selected.length; i++) {
			convertProject((IProject) selected[i], monitor);
			monitor.worked(1);
		}
		monitor.done();
	}
	
	private void loadClasspathEntries(IProject project, IProgressMonitor monitor) {
		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] currentClassPath = new IClasspathEntry[0];
		ArrayList sources = new ArrayList();
		ArrayList libraries = new ArrayList();
		try {
			currentClassPath = javaProject.getRawClasspath();
		} catch (JavaModelException e) {
		}
		for (int i = 0; i < currentClassPath.length; i++) {
			int contentType = currentClassPath[i].getEntryKind();
			if (contentType == IClasspathEntry.CPE_SOURCE)
				sources.add(getRelativePath(currentClassPath[i], project) + "/"); //$NON-NLS-1$
			else if (contentType == IClasspathEntry.CPE_LIBRARY) {
				String path = getRelativePath(currentClassPath[i], project);
				if (path.length() > 0)
					libraries.add(path);
				else
					libraries.add("."); //$NON-NLS-1$
			}
		}
		fSrcEntries = (String[])sources.toArray(new String[sources.size()]);
		fLibEntries = (String[])libraries.toArray(new String[libraries.size()]);
		
		IClasspathEntry[] classPath = new IClasspathEntry[currentClassPath.length + 1];
		System.arraycopy(currentClassPath, 0, classPath, 0, currentClassPath.length);
		classPath[classPath.length - 1] = ClasspathComputer.createContainerEntry();
		try {
			javaProject.setRawClasspath(classPath, monitor);
		} catch (JavaModelException e) {
		}
	}
	
	private String getRelativePath(IClasspathEntry cpe, IProject project) {
		IPath path = project.getFile(cpe.getPath()).getProjectRelativePath();
		return path.removeFirstSegments(1).toString();
	}
	
	private void loadLibraryName(IProject project) {
		if (isOldTarget() || 
				(fLibEntries.length > 0 && fSrcEntries.length > 0)) {
			String libName = project.getName();
			int i = libName.lastIndexOf("."); //$NON-NLS-1$
			if (i != -1)
				libName = libName.substring(i + 1);
			fLibraryName = libName + ".jar"; //$NON-NLS-1$
		} else {
			fLibraryName = "."; //$NON-NLS-1$
		}
	}
	
	private void organizeExports(IProject project) {
		IFile manifest = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		try {
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			manager.connect(manifest.getFullPath(), null);
			ITextFileBuffer buffer = manager.getTextFileBuffer(manifest.getFullPath());
			IDocument document = buffer.getDocument();		
			BundleModel model = new BundleModel(document, false);
			model.load();
			if (model.isLoaded())
				OrganizeManifest.organizeExportPackages(model.getBundle(), project, true, true);
		} catch (CoreException e) {} 
		finally {
			try {
				FileBuffers.getTextFileBufferManager().disconnect(manifest.getFullPath(), null);
			} catch (CoreException e) {}
		}
	}
}
