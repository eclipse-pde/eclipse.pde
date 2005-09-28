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
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPluginConverter;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
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


	private static String createInitialName(String id) {
		int loc = id.lastIndexOf('.');
		if (loc == -1)
			return id;
		StringBuffer buf = new StringBuffer(id.substring(loc + 1));
		buf.setCharAt(0, Character.toUpperCase(buf.charAt(0)));
		return buf.toString();
	}
	private static void createManifestFile(IFile file, IProgressMonitor monitor)
		throws CoreException {
		WorkspacePluginModel model = new WorkspacePluginModel(file, false);
		model.load();
		IPlugin plugin = model.getPlugin();
		if (PDECore.getDefault().getModelManager().isOSGiRuntime())
			plugin.setSchemaVersion("3.0"); //$NON-NLS-1$
		plugin.setId(file.getProject().getName());
		plugin.setName(createInitialName(plugin.getId()));
		plugin.setVersion("1.0.0"); //$NON-NLS-1$
		model.save();
		PDEPluginConverter.convertToOSGIFormat(file.getProject(), TargetPlatform.getTargetVersionString(), null, new SubProgressMonitor(monitor, 1));
		file.delete(true, null);
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
	
	public static void updateBuildPath(IProject project, IProgressMonitor monitor)
		throws CoreException {
		IPath manifestPath = project.getFullPath().append("plugin.xml"); //$NON-NLS-1$
		IFile file = project.getWorkspace().getRoot().getFile(manifestPath);
		if (!file.exists())
			return;
		WorkspacePluginModel model = new WorkspacePluginModel(file, true);
		model.load();
		if (!model.isLoaded())
			return;
			
		ClasspathComputer.setClasspath(project, model);
	}

	public static void convertProject(
		IProject project,
		IProgressMonitor monitor)
		throws CoreException {
		CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);
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
			if (entry.getTokens().length > 0)
				build.add(entry);
			model.save();
		}
	}
	
	private void convertProjects(
		Object[] selected,
		IProgressMonitor monitor)
		throws CoreException {
		int totalCount = 2 * selected.length;
		monitor.beginTask(
			PDEUIMessages.ConvertedProjectWizard_converting,
			totalCount);
		for (int i = 0; i < selected.length; i++) {
			convertProject((IProject) selected[i], monitor);
			monitor.worked(1);
		}
		
		// update build path
		monitor.subTask(PDEUIMessages.ConvertedProjectWizard_updating);
		for (int i = 0; i < selected.length; i++) {
			if (((IProject) selected[i]).hasNature(JavaCore.NATURE_ID)) {
				updateBuildPath((IProject) selected[i], new SubProgressMonitor(monitor,1));
			} else {
				monitor.worked(1);
			}
		}
		monitor.done();
	}

}
