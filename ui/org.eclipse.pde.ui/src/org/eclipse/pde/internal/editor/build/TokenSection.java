package org.eclipse.pde.internal.editor.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.dialogs.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.model.build.*;
import org.eclipse.pde.internal.parts.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.build.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.extension.*;
import org.eclipse.pde.model.*;
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

public class TokenSection
	extends TableSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE = "BuildEditor.TokenSection.title";
	public static final String POPUP_NEW_TOKEN = "BuildEditor.TokenSection.newToken";
	public static final String POPUP_DELETE = "BuildEditor.TokenSection.delete";
	public static final String ENTRY = "BuildEditor.TokenSection.entry";
	public static final String SECTION_NEW = "BuildEditor.TokenSection.new";
	public static final String SECTION_DESC = "BuildEditor.TokenSection.desc";
	private FormWidgetFactory factory;
	private TableViewer entryTable;
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
	super(page, new String [] { PDEPlugin.getResourceString(SECTION_NEW) });
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	initializeImages();
	Composite container = createClientContainer(parent, 2, factory);
	
	EditableTablePart tablePart = getTablePart();
	IModel model = (IModel)getFormPage().getModel();
	tablePart.setEditable(model.isEditable());
	
	createViewerPartControl(container, SWT.FULL_SELECTION, 2, factory);

	entryTable = tablePart.getTableViewer();
	entryTable.setContentProvider(new TableContentProvider());
	entryTable.setLabelProvider(new TableLabelProvider());
	factory.paintBordersFor(container);

	entryTable.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {

		}
	});
	return container;
}

protected void buttonSelected(int index) {
	if (index==0) handleNew();
}

protected void selectionChanged(IStructuredSelection selection) {
	Object item = selection.getFirstElement();
	fireSelectionNotification(item);
	getFormPage().setSelection(selection);
}

public void dispose() {
	IBuildModel model = (IBuildModel)getFormPage().getModel();
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
protected void fillContextMenu(IMenuManager manager) {
	IModel model = (IModel)getFormPage().getModel();
	if (!model.isEditable())
		return;
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
	if (object != null && object instanceof Token) {
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
	getTablePart().setButtonEnabled(0, model.isEditable());
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
