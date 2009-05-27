/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.RequiredPluginsClasspathContainer;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class RequiredPluginsContainerPage extends WizardPage implements IClasspathContainerPage, IClasspathContainerPageExtension {
	private IClasspathEntry entry;
	private TableViewer viewer;
	private Image projectImage;
	private Image libraryImage;
	private Image slibraryImage;
	private IClasspathEntry[] realEntries;
	private IJavaProject javaProject;

	class EntryContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (realEntries != null)
				return realEntries;
			return new Object[0];
		}
	}

//	class EntrySorter extends ViewerSorter {
//		public int category(Object obj) {
//			IClasspathEntry entry = (IClasspathEntry) obj;
//			return entry.getEntryKind() == IClasspathEntry.CPE_PROJECT
//				? -10
//				: 0;
//		}
//	}

	class EntryLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getText(Object obj) {
			IClasspathEntry entry = (IClasspathEntry) obj;
			int kind = entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_PROJECT)
				return entry.getPath().segment(0);
			IPath path = entry.getPath();
			String name = path.lastSegment();
			return name + " - " //$NON-NLS-1$
					+ path.uptoSegment(path.segmentCount() - 1).toOSString();
		}

		public Image getImage(Object obj) {
			IClasspathEntry entry = (IClasspathEntry) obj;
			int kind = entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_PROJECT)
				return projectImage;
			else if (kind == IClasspathEntry.CPE_LIBRARY) {
				IPath sourceAtt = entry.getSourceAttachmentPath();
				return sourceAtt != null ? slibraryImage : libraryImage;
			}
			return null;
		}

		public String getColumnText(Object obj, int col) {
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int col) {
			return getImage(obj);
		}
	}

	/**
	 * The constructor.
	 */
	public RequiredPluginsContainerPage() {
		super("requiredPluginsContainerPage"); //$NON-NLS-1$
		setTitle(PDEUIMessages.RequiredPluginsContainerPage_title);
		setDescription(PDEUIMessages.RequiredPluginsContainerPage_desc);
		projectImage = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		//libraryImage = PDEPluginImages.DESC_BUILD_VAR_OBJ.createImage();
		libraryImage = JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE);
		slibraryImage = JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE_WITH_SOURCE);
		setImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#createControl
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout());
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEUIMessages.RequiredPluginsContainerPage_label);
		viewer = new TableViewer(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setContentProvider(new EntryContentProvider());
		viewer.setLabelProvider(new EntryLabelProvider());
		viewer.setComparator(new ViewerComparator());

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 400;
		gd.heightHint = 300;
		viewer.getTable().setLayoutData(gd);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PLUGINS_CONTAINER_PAGE);
		setControl(container);
		Dialog.applyDialogFont(container);
		if (realEntries != null)
			initializeView();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.REQUIRED_PLUINGS_CONTAINER);
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#finish
	 */
	public boolean finish() {
		return true;
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#getSelection
	 */
	public IClasspathEntry getSelection() {
		return entry;
	}

	public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
		javaProject = project;
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#setSelection
	 */
	public void setSelection(IClasspathEntry containerEntry) {
		this.entry = containerEntry;
		createRealEntries();
		if (viewer != null)
			initializeView();
	}

	private void createRealEntries() {
		IJavaProject javaProject = getJavaProject();
		if (javaProject == null) {
			realEntries = new IClasspathEntry[0];
			return;
		}

		if (entry == null) {
			entry = ClasspathComputer.createContainerEntry();
			IPluginModelBase model = PluginRegistry.findModel(javaProject.getProject());
			if (model != null) {
				IClasspathContainer container = new RequiredPluginsClasspathContainer(model);
				if (container != null)
					realEntries = container.getClasspathEntries();
			}
		} else {
			try {
				IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), javaProject);
				if (container != null)
					realEntries = container.getClasspathEntries();
			} catch (JavaModelException e) {
			}
		}
		if (realEntries == null)
			realEntries = new IClasspathEntry[0];
	}

	private IJavaProject getJavaProject() {
		return javaProject;
	}

	private void initializeView() {
		viewer.setInput(entry);
	}
}
