package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.update.ui.forms.internal.*;

public class PackagePrefixesSection extends TableSection {
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

	public PackagePrefixesSection(ManifestRuntimePage formPage) {
		super(
			formPage,
			new String[] {
				PDEPlugin.getResourceString(KEY_ADD),
				PDEPlugin.getResourceString(KEY_REMOVE)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
		handleDefaultButton = false;
	}

	public void commitChanges(boolean onSave) {
		if (isDirty() == false)
			return;
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
		setDirty(false);
		ignoreModelEvents = false;
	}
	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);

		createViewerPartControl(container, SWT.FULL_SELECTION, 2, factory);
		TablePart part = getTablePart();
		nameTableViewer = part.getTableViewer();
		nameTableViewer.setContentProvider(new TableContentProvider());
		nameTableViewer.setLabelProvider(new TableLabelProvider());
		factory.paintBordersFor(container);

		return container;
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
		getFormPage().setSelection(selection);
		getTablePart().setButtonEnabled(1, item != null);
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleAdd();
		else if (index == 1)
			handleDelete();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			handleDelete();
			return true;
		}
		return false;
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(addAction);
		deleteAction.setEnabled(
			!isReadOnly() && !nameTableViewer.getSelection().isEmpty());
		manager.add(deleteAction);
		manager.add(new Separator());
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
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
				(IPluginModelBase) getFormPage().getModel();
			IProject project = model.getUnderlyingResource().getProject();
			SelectionDialog dialog =
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
				setDirty(true);
				commitChanges(false);
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
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
		setDirty(true);
		commitChanges(false);
	}

	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		setReadOnly(!model.isEditable());
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

	public void sectionChanged(
		FormSection source,
		int changeType,
		Object changeObject) {
		update((IPluginLibrary) changeObject);
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