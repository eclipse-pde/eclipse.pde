/*******************************************************************************
 * Copyright (c) 2014, 2017 TwelveTone LLC and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Steven Spungin <steven@spungin.tv> - initial API and implementation, Ongoing Maintenance
 *******************************************************************************/

package org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.dialogs;

import java.net.URI;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs.BundleImageCache;
import org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Wizard page to select a project folder
 *
 * @author Steven Spungin
 */
public class PickProjectFolderPage extends WizardPage {

	private TreeViewer viewer;
	private String value;
	private final IEclipseContext context;
	private IPath path;

	private Label label2;
	private Label label3;
	private Label lblResourcePath;

	protected PickProjectFolderPage(IEclipseContext context) {
		super(Messages.PickProjectFolderPage_SelectProjectFolder, Messages.PickProjectFolderPage_SelectProjectFolder, null);
		this.context = context;

		setMessage(Messages.NonReferencedResourceDialog_selectProjectToReceiveCopy);
		Image image = context.get(BundleImageCache.class).create("/icons/full/wizban/plugin_wiz.gif"); //$NON-NLS-1$
		setImageDescriptor(ImageDescriptor.createFromImage(image));
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		// TODO Auto-generated method stub
		Composite ret = new Composite(parent, SWT.NONE);
		// ret.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ret.setLayout(new GridLayout(1, false));

		viewer = new TreeViewer(ret);
		viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(new ProjectContentProvider());
		viewer.setLabelProvider(new ProjectLabelProvider());
		viewer.expandToLevel(2);

		viewer.addDoubleClickListener(event -> onChanged());

		viewer.addSelectionChangedListener(event -> onChanged());

		Composite compPath = new Composite(ret, SWT.NONE);
		compPath.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		compPath.setLayout(new GridLayout(2, false));

		Label label = new Label(compPath, SWT.NONE);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		label.setText(Messages.ProjectFolderPickerDialog_sourceResourceName);

		label2 = new Label(compPath, SWT.NONE);
		label2.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		label3 = new Label(compPath, SWT.NONE);
		label3.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		lblResourcePath = new Label(compPath, SWT.NONE);
		lblResourcePath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		compPath.setVisible(false);

		String message = Messages.ProjectFolderPickerDialog_6;

		getShell().setText(message);
		setTitle(message);
		setMessage(message);
		setControl(ret);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			IProject project = (IProject) context.get("projectToCopyTo"); //$NON-NLS-1$
			viewer.setInput(project);

			Object object = context.get("folderToCopyTo.obj"); //$NON-NLS-1$
			if (object != null) {
				viewer.setSelection(new StructuredSelection(object));
			} else {
				viewer.setSelection(new StructuredSelection());
			}
			setPageComplete(viewer.getSelection().isEmpty() == false);

			path = IPath.fromOSString((String) context.get("srcPath")); //$NON-NLS-1$
			label2.setText(path.lastSegment());
			label3.setText(Messages.ProjectFolderPickerDialog_sourceResourceDirectory);
			lblResourcePath.setText(path.removeLastSegments(1).toOSString());
		}
		super.setVisible(visible);
	}

	protected void onChanged() {
		Object selected = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
		if (selected == null || selected instanceof String) {
			value = ""; //$NON-NLS-1$
		} else {
			IResource resource = (IResource) selected;
			value = resource.getFullPath().removeFirstSegments(1).toOSString();
		}
		context.set("folderToCopyTo", value); //$NON-NLS-1$
		context.set("folderToCopyTo.obj", selected); //$NON-NLS-1$
		setPageComplete(selected != null);
	}

	static class ProjectContentProvider implements ITreeContentProvider {

		private IProject project;

		public ProjectContentProvider() {
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.project = (IProject) newInput;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return new Object[] { project.getName() };
		}

		@Override
		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof String) {
				return getChildren(project);
			}
			final IResource resource = (IResource) parentElement;
			final ArrayList<Object> list = new ArrayList<>();
			IResourceProxyVisitor visitor = proxy -> {
				if (proxy.getType() == IResource.FOLDER && proxy.requestResource().getParent() == resource) {
					if (proxy.requestResource().equals(resource) == false) {
						list.add(proxy.requestResource());
					}
				}
				return true;
			};
			try {
				resource.accept(visitor, IResource.DEPTH_ONE);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			return list.toArray(new Object[0]);
		}

		@Override
		public Object getParent(Object element) {
			IResource resource = (IResource) element;
			return resource.getParent();
		}

		Boolean found = false;

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof String) {
				return true;
			}
			final IResource resource = (IResource) element;
			try {
				found = false;
				resource.accept(proxy -> {
					if (proxy.getType() == IResource.FOLDER && proxy.requestResource().equals(resource) == false) {
						found = true;
						return false;
					}
					return true;
				}, IResource.DEPTH_ONE);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			return found;
		}
	}

	static class ProjectLabelProvider extends ColumnLabelProvider {
		private final ResourceManager resources = new LocalResourceManager(JFaceResources.getResources());

		@Override
		public String getText(Object element) {
			if (element instanceof String) {
				return element.toString();
			}
			IResource resource = (IResource) element;
			return resource.getName();
		}

		@Override
		public Image getImage(Object element) {
			String uri = element instanceof String ? Messages.ProjectFolderPickerDialog_0
					: "platform:/plugin/org.eclipse.ui.ide/icons/full/obj16/folder.png"; //$NON-NLS-1$
			return resources.create(ImageDescriptor.createFromURI(URI.create(uri)));
		}

		@Override
		public void dispose() {
			resources.dispose();
			super.dispose();
		}
	}
}
