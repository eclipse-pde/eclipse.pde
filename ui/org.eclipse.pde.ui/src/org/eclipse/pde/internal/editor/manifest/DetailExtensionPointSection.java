package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.extension.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.elements.*;
import java.util.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.window.*;
import org.eclipse.swt.custom.*;

public class DetailExtensionPointSection
	extends PDEFormSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE =
		"ManifestEditor.DetailExtensionPointSection.title";
	public static final String SECTION_NEW =
		"ManifestEditor.DetailExtensionPointSection.new";
	public static final String POPUP_NEW_EXTENSION_POINT =
		"ManifestEditor.DetailExtensionPointSection.newExtensionPoint";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private FormWidgetFactory factory;
	private TableViewer pointTable;
	private Button newButton;
	private SchemaRegistry schemaRegistry;
	private ExternalModelManager pluginInfoRegistry;
	private Image pointImage;

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginBase) {
				return ((IPluginBase) parent).getExtensionPoints();
			}
			return new Object[0];
		}
	}

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof IPluginExtensionPoint && index == 0) {
				IPluginExtensionPoint point = (IPluginExtensionPoint) obj;
				return point.getResourceString(point.getName());
			}
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 0)
				return pointImage;
			return null;
		}
	}

	public DetailExtensionPointSection(ManifestExtensionPointPage page) {
		super(page);
		this.setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		schemaRegistry = PDEPlugin.getDefault().getSchemaRegistry();
		pluginInfoRegistry = PDEPlugin.getDefault().getExternalModelManager();
	}
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		this.factory = factory;
		initializeImages();
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		//setWidthHint(300);

		container.setLayout(layout);
		Table table = factory.createTable(container, SWT.FULL_SELECTION);
		TableLayout tlayout = new TableLayout();

		TableColumn tableColumn = new TableColumn(table, SWT.NULL);
		tableColumn.setText("Point Name");
		ColumnLayoutData cLayout = new ColumnWeightData(100, true);
		tlayout.addColumnData(cLayout);

		//table.setLinesVisible(true);
		//table.setHeaderVisible(true);
		table.setLayout(tlayout);

		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillContextMenu(mng);
			}
		};
		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		Menu menu = popupMenuManager.createContextMenu(table);
		table.setMenu(menu);

		pointTable = new TableViewer(table);
		pointTable.setContentProvider(new TableContentProvider());
		pointTable.setLabelProvider(new TableLabelProvider());
		factory.paintBordersFor(container);

		pointTable.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object item = ((IStructuredSelection) event.getSelection()).getFirstElement();
				fireSelectionNotification(item);
				getFormPage().setSelection(event.getSelection());
			}
		});

		GridData gd = new GridData(GridData.FILL_VERTICAL);
		gd.widthHint = 200;
		table.setLayoutData(gd);

		Composite buttonContainer = factory.createComposite(container);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		layout = new GridLayout();
		layout.marginHeight = 0;
		buttonContainer.setLayout(layout);

		newButton =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(SECTION_NEW),
				SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalAlignment = GridData.BEGINNING;
		newButton.setLayoutData(gd);
		newButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleNew();
			}
		});
		return container;
	}
	public void dispose() {
		pointImage.dispose();
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			handleDelete();
			return true;
		}
		return false;
	}
	public void expandTo(Object object) {
		pointTable.setSelection(new StructuredSelection(object), true);
	}
	private void fillContextMenu(IMenuManager manager) {
		ISelection selection = pointTable.getSelection();

		manager
			.add(new Action(PDEPlugin.getResourceString(POPUP_NEW_EXTENSION_POINT)) {
			public void run() {
				handleNew();
			}
		});

		if (!selection.isEmpty()) {
			Object object = ((IStructuredSelection) selection).getFirstElement();
			final IPluginExtensionPoint point = (IPluginExtensionPoint) object;

			manager.add(new Separator());
			manager.add(new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					IPluginBase plugin = point.getPluginBase();
					try {
						plugin.remove(point);
					} catch (CoreException e) {
					}
				}
			});
		}
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());
		manager.add(new PropertiesAction(getFormPage().getEditor()));
	}
	private void handleDelete() {
		Object object =
			((IStructuredSelection) pointTable.getSelection()).getFirstElement();
		if (object != null && object instanceof IPluginExtensionPoint) {
			IPluginExtensionPoint ep = (IPluginExtensionPoint) object;
			IPluginBase plugin = ep.getPluginBase();
			try {
				plugin.remove(ep);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	private void handleNew() {
		IFile file =
			((IFileEditorInput) getFormPage().getEditor().getEditorInput()).getFile();
		final IProject project = file.getProject();
		BusyIndicator.showWhile(pointTable.getTable().getDisplay(), new Runnable() {
			public void run() {
				NewExtensionPointWizard wizard =
					new NewExtensionPointWizard(
						project,
						(IPluginModelBase) getFormPage().getModel());
				WizardDialog dialog =
					new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				dialog.getShell().setSize(400, 450);
				dialog.open();
			}
		});
	}
	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		pointTable.setInput(model.getPluginBase());
		setReadOnly(!model.isEditable());
		newButton.setEnabled(model.isEditable());
		model.addModelChangedListener(this);
	}
	private void initializeImages() {
		pointImage = PDEPluginImages.DESC_EXT_POINT_OBJ.createImage();
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			pointTable.refresh();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginExtensionPoint) {
			if (event.getChangeType() == event.INSERT) {
				pointTable.add(changeObject);
				pointTable.setSelection(new StructuredSelection(changeObject), true);
				pointTable.getTable().setFocus();
			} else if (event.getChangeType() == event.REMOVE) {
				pointTable.remove(changeObject);
			} else {
				pointTable.update(changeObject, null);
				if (pointTable.getTable().isFocusControl()) {
					ISelection sel = getFormPage().getSelection();
					if (sel != null && sel instanceof IStructuredSelection) {
						IStructuredSelection ssel = (IStructuredSelection) sel;
						if (!ssel.isEmpty() && ssel.getFirstElement().equals(changeObject)) {
							// update property sheet
							getFormPage().setSelection(sel);
						}
					}
				}
			}
		}
	}
	public void setFocus() {
		pointTable.getTable().setFocus();
	}
}