package org.eclipse.pde.internal.editor.build;

import org.eclipse.ui.dialogs.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.model.build.*;
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

public class TokenSection
	extends PDEFormSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE = "BuildEditor.TokenSection.title";
	public static final String POPUP_NEW_TOKEN = "BuildEditor.TokenSection.newToken";
	public static final String POPUP_DELETE = "BuildEditor.TokenSection.delete";
	public static final String ENTRY = "BuildEditor.TokenSection.entry";
	public static final String SECTION_NEW = "BuildEditor.TokenSection.new";
	public static final String SECTION_DESC = "BuildEditor.TokenSection.desc";
	private FormWidgetFactory factory;
	private TableViewer entryTable;
	private Button newButton;
	private IBuildEntry currentVariable;

	class Token {
		String name;
		public Token(String name) { this.name = name; }
		public String toString() { return name; }
	}

	class NameModifier implements ICellModifier {
		public boolean canModify(Object object, String property) {
			return true;
		}
		public void modify(Object object, String property, final Object value) {
			Item item = (Item) object;
			final Token token = (Token) item.getData();
			IBuildModel model = (IBuildModel) getFormPage().getModel();
			try {
				currentVariable.renameToken(token.name, value.toString());
				token.name = value.toString();
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}

			entryTable.getTable().getDisplay().asyncExec(new Runnable() {
				public void run() {
					entryTable.update(token, null);
				}
			});
		}
		public Object getValue(Object object, String property) {
			return object.toString();
		}
	}

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildEntry) {
				IBuildEntry entry = (IBuildEntry) parent;
				return createTokens(entry.getTokens());
			}
			return new Object[0];
		}
		Object [] createTokens(String [] tokens) {
			Token [] result = new Token[tokens.length];
			for (int i=0; i<tokens.length; i++) {
				result[i] = new Token(tokens[i]);
			}
			return result;
		}
	}

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}


public TokenSection(BuildPage page) {
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

	entryTable = new TableViewer(table);
	entryTable.setContentProvider(new TableContentProvider());
	entryTable.setLabelProvider(new TableLabelProvider());
	factory.paintBordersFor(container);

	if (getFormPage().getModel() instanceof IEditable) {
		CellEditor[] editors = new CellEditor[] { new ModifiedTextCellEditor(table)};
		String[] properties = { "name" };
		entryTable.setCellEditors(editors);
		entryTable.setCellModifier(new NameModifier());
		entryTable.setColumnProperties(properties);
	}

	entryTable.addSelectionChangedListener(new ISelectionChangedListener() {
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
	IBuildModel model = (IBuildModel)getFormPage().getModel();
	model.removeModelChangedListener(this);
}
public void doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		handleDelete();
	}
}
private void fillContextMenu(IMenuManager manager) {
	if (!(getFormPage().getModel() instanceof IEditable)) return;
	ISelection selection = entryTable.getSelection();

	manager.add(new Action(PDEPlugin.getResourceString(POPUP_NEW_TOKEN)) {
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
	IBuildModel buildModel = (IBuildModel) getFormPage().getModel();
	if (buildModel.isEditable() == false)
		return;
	Object object =
		((IStructuredSelection) entryTable.getSelection()).getFirstElement();
	if (object != null && object instanceof String) {
		IBuildEntry entry = currentVariable;
		if (entry != null) {
			try {
				entry.removeToken(object.toString());
				entryTable.remove(object);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
}
private void handleNew() {
	if (currentVariable == null)
		return;
	try {
		Token token = new Token(PDEPlugin.getResourceString(ENTRY));
		currentVariable.addToken(token.toString());
		entryTable.add(token);
		entryTable.editElement(token, 0);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
public void initialize(Object input) {
	IBuildModel model = (IBuildModel)input;
	setReadOnly(!model.isEditable());
	newButton.setEnabled(model.isEditable());
	model.addModelChangedListener(this);
}
private void initializeImages() {
}
public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		entryTable.refresh();
		return;
	}
}
public void sectionChanged(
	FormSection source,
	int changeType,
	Object changeObject) {
	IBuildEntry variable = (IBuildEntry) changeObject;
	update(variable);
}
public void setFocus() {
	entryTable.getTable().setFocus();
}
private void update(IBuildEntry variable) {
	currentVariable = variable;
	entryTable.setInput(currentVariable);
}
}
