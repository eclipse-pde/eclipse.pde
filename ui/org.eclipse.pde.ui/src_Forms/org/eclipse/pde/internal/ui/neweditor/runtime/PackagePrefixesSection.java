/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.runtime;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.TableSection;
import org.eclipse.pde.internal.ui.newparts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

public class PackagePrefixesSection extends TableSection implements IPartSelectionListener {
	private IPluginLibrary currentLibrary;
	private TableViewer nameTableViewer;
	public static final String SECTION_TITLE =
		"ManifestEditor.PackagePrefixesSection.title";
	public static final String SECTION_DESC =
		"ManifestEditor.PackagePrefixesSection.desc";
	public static final String KEY_ADD =
		"ManifestEditor.PackagePrefixesSection.add";
	public static final String KEY_REMOVE =
		"ManifestEditor.PackagePrefixesSection.remove";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private Vector packages;
	private Action addAction;
	private Action deleteAction;
	private boolean ignoreModelEvents;

	class PackagePrefix {
		private String name;
		public PackagePrefix(String name) {
			this.name = name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public String toString() {
			return name;
		}
	}

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginLibrary) {
				return createPrefixes(((IPluginLibrary) parent).getPackages());
			}
			return new Object[0];
		}
	}

	class TableLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return JavaUI.getSharedImages().getImage(
				ISharedImages.IMG_OBJS_PACKAGE);
		}
	}

	public PackagePrefixesSection(PDEFormPage formPage, Composite parent) {
		super(
			formPage,
			parent,
			Section.DESCRIPTION,
			new String[] {
				PDEPlugin.getResourceString(KEY_ADD),
				PDEPlugin.getResourceString(KEY_REMOVE)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
		handleDefaultButton = false;
	}

	public void commit(boolean onSave) {
		ignoreModelEvents = true;
		if (packages != null && currentLibrary != null) {
			try {
				if (packages.size() == 0) {
					currentLibrary.setPackages(null);
				} else {
					String[] result = new String[packages.size()];
					for (int i = 0; i < packages.size(); i++) {
						result[i] = packages.get(i).toString();
					}
					currentLibrary.setPackages(result);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		ignoreModelEvents = false;
		super.commit(onSave);
	}
	public void createClient(
		Section section,
		FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);

		createViewerPartControl(container, SWT.FULL_SELECTION, 2, toolkit);
		TablePart part = getTablePart();
		nameTableViewer = part.getTableViewer();
		nameTableViewer.setContentProvider(new TableContentProvider());
		nameTableViewer.setLabelProvider(new TableLabelProvider());
		toolkit.paintBordersFor(container);
		initialize();

		section.setClient(container);
	}

	private Object[] createPrefixes(String[] names) {
		if (packages == null) {
			packages = new Vector();
			if (names != null) {
				for (int i = 0; i < names.length; i++) {
					packages.add(new PackagePrefix(names[i]));
				}
			}
		}
		Object[] result = new Object[packages.size()];
		packages.copyInto(result);
		return result;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		//getFormPage().setSelection(selection);
		getTablePart().setButtonEnabled(1, item != null);
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleAdd();
		else if (index == 1)
			handleDelete();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		return false;
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}
	
	private boolean isReadOnly() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IEditable)
			return !((IEditable)model).isEditable();
		return true;
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(addAction);
		deleteAction.setEnabled(
			!isReadOnly() && !nameTableViewer.getSelection().isEmpty());
		manager.add(deleteAction);
		manager.add(new Separator());
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager);
	}

	private void makeActions() {
		addAction = new Action() {
			public void run() {
				handleAdd();
			}
		};
		addAction.setText(POPUP_NEW);
		addAction.setEnabled(isReadOnly());
		deleteAction = new Action() {
			public void run() {
				handleDelete();
			}
		};
		deleteAction.setText(POPUP_DELETE);
	}

	private void handleAdd() {
		try {
			IPluginModelBase model =
				(IPluginModelBase) getPage().getModel();
			IProject project = model.getUnderlyingResource().getProject();
			IJavaProject javaProject = JavaCore.create(project);
			IPackageFragmentRoot fragmentRoot =
				getPackageFragmentRoot(javaProject, currentLibrary);
			SelectionDialog dialog;
			if (fragmentRoot != null)
				dialog =
					JavaUI.createPackageDialog(
						nameTableViewer.getControl().getShell(),
						fragmentRoot);
			else
				dialog =
					JavaUI.createPackageDialog(
						nameTableViewer.getControl().getShell(),
						JavaCore.create(project),
						0);
			dialog.setTitle(PDEPlugin.getResourceString("Java Packages"));
			dialog.setMessage("");
			int status = dialog.open();
			if (status == SelectionDialog.OK) {
				Object[] result = dialog.getResult();
				for (int i = 0; i < result.length; i++) {
					IPackageFragment packageFragment =
						(IPackageFragment) result[i];
					PackagePrefix prefix =
						new PackagePrefix(packageFragment.getElementName());
					if (packages == null)
						packages = new Vector();
					packages.add(prefix);
					nameTableViewer.add(prefix);
				}
				markDirty();
				commit(false);
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
	}

	private IPackageFragmentRoot getPackageFragmentRoot(
		IJavaProject javaProject,
		IPluginLibrary library)
		throws JavaModelException {
		IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
		for (int i = 0; i < roots.length; i++) {
			IPackageFragmentRoot root = roots[i];
			String name = root.getElementName();
			if (name.equals(library.getName()))
				return root;
		}
		return null;
	}

	private void handleDelete() {
		ISelection selection = nameTableViewer.getSelection();
		if (selection.isEmpty())
			return;
		Iterator iter = ((IStructuredSelection) selection).iterator();

		while (iter.hasNext()) {
			PackagePrefix prefix = (PackagePrefix) iter.next();
			packages.remove(prefix);
			nameTableViewer.remove(prefix);
		}
		getTablePart().setButtonEnabled(1, false);
		markDirty();
		commit(false);
	}

	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.addModelChangedListener(this);
		if (isReadOnly()) {
			getTablePart().setButtonEnabled(0, false);
			getTablePart().setButtonEnabled(1, false);
		}
		makeActions();
		update(null);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (ignoreModelEvents)
			return;
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object object = e.getChangedObjects()[0];
			if (object.equals(currentLibrary)) {
				update(currentLibrary);
			}
		}
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			update(null);
		}
	}

	public void selectionChanged(
		IFormPart source,
		ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection)selection;
		update((IPluginLibrary) ssel.getFirstElement());
	}

	private void update(IPluginLibrary library) {
		if (library == null) {
			getTablePart().setButtonEnabled(0, false);
			getTablePart().setButtonEnabled(1, false);
			currentLibrary = null;
			return;
		} else if (currentLibrary == null && !isReadOnly()) {
			getTablePart().setButtonEnabled(0, true);
		}
		this.currentLibrary = library;
		packages = null;
		nameTableViewer.setInput(library);
	}
}
