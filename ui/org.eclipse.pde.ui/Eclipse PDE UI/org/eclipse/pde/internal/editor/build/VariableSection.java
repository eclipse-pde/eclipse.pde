package org.eclipse.pde.internal.editor.build;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.build.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.extension.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
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


public class VariableSection
	extends PDEFormSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE = "BuildEditor.VariableSection.title";
	public static final String DIALOG_TITLE = "BuildEditor.VariableSection.dialogTitle";
	public static final String POPUP_NEW_VARIABLE = "BuildEditor.VariableSection.newVariable";
	public static final String POPUP_DELETE = "BuildEditor.VariableSection.delete";
	public static final String SECTION_NEW = "BuildEditor.VariableSection.new";
	public static final String SECTION_DESC = "BuildEditor.VariableSection.desc";
	private FormWidgetFactory factory;
	private TableViewer variableTable;
	private Button newButton;
	private Image variableImage;

	class NameModifier implements ICellModifier {
		public boolean canModify(Object object, String property) {
			return true;
		}
		public void modify(Object object, String property, Object value) {
			Item item = (Item) object;
			final IBuildEntry entry = (IBuildEntry) item.getData();
			try {
				String newValue = value.toString();
				if (newValue.equals(entry.getName()))
					return;
				entry.setName(value.toString());

				setDirty(true);
				commitChanges(false);
				variableTable.getTable().getDisplay().asyncExec(new Runnable() {
					public void run() {
						variableTable.update(entry, null);
					}
				});
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		public Object getValue(Object object, String property) {
			return object.toString();
		}
	}

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				return ((IBuildModel) parent).getBuild().getBuildEntries();
			}
			return new Object[0];
		}
	}

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof IBuildEntry && index == 0) {
				return ((IBuildEntry) obj).getName();
			}
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 0)
				return variableImage;
			return null;
		}
	}


public VariableSection(BuildPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	initializeImages();
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;

	container.setLayout(layout);
	final Table table = factory.createTable(container, SWT.FULL_SELECTION);
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

	variableTable = new TableViewer(table);
	variableTable.setContentProvider(new TableContentProvider());
	variableTable.setLabelProvider(new TableLabelProvider());
	factory.paintBordersFor(container);

	IBuildModel model = (IBuildModel)getFormPage().getModel();

	if (model.isEditable()) {
		CellEditor[] editors = new CellEditor[] { new ModifiedTextCellEditor(table)};
		String[] properties = { "name" };
		variableTable.setCellEditors(editors);
		variableTable.setCellModifier(new NameModifier());
		variableTable.setColumnProperties(properties);
	}

	variableTable.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			Object item = ((IStructuredSelection) event.getSelection()).getFirstElement();
			fireSelectionNotification(item);
			getFormPage().setSelection(event.getSelection());
		}
	});

	GridData gd = new GridData(GridData.FILL_BOTH);
	table.setLayoutData(gd);

	Composite buttonContainer = factory.createComposite(container);
	gd = new GridData(GridData.FILL_VERTICAL);
	buttonContainer.setLayoutData(gd);
	layout = new GridLayout();
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	buttonContainer.setLayout(layout);

	newButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(SECTION_NEW), SWT.PUSH);
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
	variableImage.dispose();
	IBuildModel model = (IBuildModel)getFormPage().getModel();
	model.removeModelChangedListener(this);
}
public void doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		handleDelete();
	}
}
public void expandTo(Object object) {
	variableTable.setSelection(new StructuredSelection(object), true);
}
private void fillContextMenu(IMenuManager manager) {
	if (!(getFormPage().getModel() instanceof IEditable))
		return;
	ISelection selection = variableTable.getSelection();

	manager.add(new Action(PDEPlugin.getResourceString(POPUP_NEW_VARIABLE)) {
		public void run() {
			handleNew();
		}
	});

	if (!selection.isEmpty()) {
		manager.add(new Separator());
		manager.add(new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
			public void run() {
				handleDelete();
			}
		});
	}
	manager.add(new Separator());
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
}
private void handleDelete() {
	Object object =
		((IStructuredSelection) variableTable.getSelection()).getFirstElement();
	if (object != null && object instanceof IBuildEntry) {
		IBuildEntry entry = (IBuildEntry) object;
		IBuild build = entry.getModel().getBuild();
		try {
			build.remove(entry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
private void handleNew() {
	final IBuildModel model = (IBuildModel) getFormPage().getModel();
	final IBuild build = model.getBuild();

	BusyIndicator.showWhile(variableTable.getTable().getDisplay(), new Runnable() {
		public void run() {
			VariableSelectionDialog dialog =
				new VariableSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), model);
			dialog.create();
			dialog.getShell().setText(PDEPlugin.getResourceString(DIALOG_TITLE));
			dialog.getShell().setSize(300, 350);
			if (dialog.open() == VariableSelectionDialog.OK) {
				IBuildEntry entry =
					model.getFactory().createEntry(dialog.getSelectedVariable());
				try {
					build.add(entry);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	});
}
public void initialize(Object input) {
	IBuildModel model = (IBuildModel)input;
	variableTable.setInput(model);
	setReadOnly(!model.isEditable());
	newButton.setEnabled(model.isEditable());
	model.addModelChangedListener(this);
}
private void initializeImages() {
	variableImage = PDEPluginImages.DESC_BUILD_VAR_OBJ.createImage();
}
public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		variableTable.refresh();
		return;
	}
	Object changeObject = event.getChangedObjects()[0];
	if (changeObject instanceof IBuildEntry) {
		if (event.getChangeType()==event.INSERT) {
			variableTable.add(changeObject);
		}
		else if (event.getChangeType()==event.REMOVE) {
			variableTable.remove(changeObject);
		}
		else {
			if (event.getChangedProperty()==null) {
				variableTable.update(changeObject, null);
			}
		}
	}
}
public void setFocus() {
	variableTable.getTable().setFocus();
}
}
