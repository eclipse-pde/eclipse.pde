package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.jface.viewers.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;

public class DetailChildrenSection
	extends PDEFormSection
	implements IModelChangedListener {
		public static final String SECTION_TITLE = "ManifestEditor.DetailChildrenSection.title";
		public static final String SECTION_BODY_TEXT = "ManifestEditor.DetailChildrenSection.bodyText";
		public static final String KEY_APPLY = "Actions.apply.label";
		public static final String KEY_RESET = "Actions.reset.label";
		public static final String KEY_DELETE = "Actions.delete.label";
	private FormWidgetFactory factory;
	private Button newButton;
	private Button applyButton;
	private Button deleteButton;
	private Button resetButton;
	private IPluginElement currentElement;
	private Text text;
	private TreeViewer treeViewer;
	private Image genericElementImage;

	class ContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			Object[] children = null;
			if (!(parent instanceof IPluginExtension) && parent instanceof IPluginParent)
				children = ((IPluginParent) parent).getChildren();
			if (children==null) children = new Object[0];
			return children;
		}
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length>0;
		}
		public Object getParent(Object child) {
			if (child instanceof IPluginObject)
				return ((IPluginObject) child).getParent();
			return null;
		}
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
	}
	class ChildrenLabelProvider extends LabelProvider {
		public String getText(Object o) {
			return resolveObjectName(o);
		}
		public Image getImage(Object o) {
			return resolveObjectImage(o);
		}
	}

public DetailChildrenSection(ManifestExtensionsPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	initializeImages();
	GridData gd;
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	container.setLayout(layout);

	// add tree
	Control tree = createTree(container);
	gd =
		new GridData(
			GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
	tree.setLayoutData(gd);

	Composite buttonContainer = factory.createComposite(container);
	layout = new GridLayout();
	layout.marginHeight = 0;
	buttonContainer.setLayout(layout);
	gd =
		new GridData(
			GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_FILL);
	buttonContainer.setLayoutData(gd);

	deleteButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(KEY_DELETE), SWT.PUSH);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	deleteButton.setLayoutData(gd);
	deleteButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleDelete();
		}
	});

	Label label = factory.createLabel(container, PDEPlugin.getResourceString(SECTION_BODY_TEXT));
	gd = new GridData();
	gd.horizontalSpan = 2;
	label.setLayoutData(gd);
	// text
	text =
		factory.createText(
			container,
			"",
			SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | factory.BORDER_STYLE);
	text.setEditable(true);
	gd = new GridData(GridData.FILL_BOTH);
	text.setLayoutData(gd);
	text.addModifyListener(new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			applyButton.setEnabled(true);
			resetButton.setEnabled(true);
		}
	});

	buttonContainer = factory.createComposite(container);
	layout = new GridLayout();
	layout.marginHeight = 0;
	buttonContainer.setLayout(layout);
	gd =
		new GridData(
			GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_FILL);
	buttonContainer.setLayoutData(gd);

	// add buttons
	applyButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(KEY_APPLY), SWT.PUSH);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	applyButton.setLayoutData(gd);
	applyButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleApply();
		}
	});

	resetButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(KEY_RESET), SWT.PUSH);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	resetButton.setLayoutData(gd);
	resetButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleReset();
		}
	});

	if (SWT.getPlatform().equals("motif")==false)
	   factory.paintBordersFor(container);
	return container;
}
private Control createTree(Composite parent) {
	Tree tree = factory.createTree(parent, SWT.SINGLE);

	treeViewer = new TreeViewer(tree);
	treeViewer.setLabelProvider(new ChildrenLabelProvider());
	treeViewer.setContentProvider(new ContentProvider());
	treeViewer.setAutoExpandLevel(999);
	treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent e) {
			Object item = ((IStructuredSelection) e.getSelection()).getFirstElement();
			fireSelectionNotification(item);
			getFormPage().setSelection(e.getSelection());
			IModel model = (IModel)getFormPage().getEditor().getModel();
			deleteButton.setEnabled(model.isEditable() && item!=null);
			if (item instanceof IPluginElement) {
				currentElement = (IPluginElement) item;
			} else
				currentElement = null;
			updateText(currentElement);
		}
	});
	MenuManager popupMenuManager = new MenuManager();
	IMenuListener listener = new IMenuListener () {
		public void menuAboutToShow(IMenuManager mng) {
			fillContextMenu(mng);
		}
	};
	popupMenuManager.setRemoveAllWhenShown(true);
	popupMenuManager.addMenuListener(listener);
	Menu menu=popupMenuManager.createContextMenu(tree);
	tree.setMenu(menu);
	return tree;
}
public void dispose() {
	genericElementImage.dispose();
	IPluginModelBase model = (IPluginModelBase)getFormPage().getModel();
	model.removeModelChangedListener(this);
	super.dispose();
}
public void doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		handleDelete();
	}
}
public void fillContextMenu(IMenuManager manager) {
	ISelection selection = treeViewer.getSelection();
	Object object = null;
	if (!selection.isEmpty()) {
		object = ((IStructuredSelection) selection).getFirstElement();
		if (object instanceof IPluginParent) {
			DetailExtensionSection.fillContextMenu(
				getFormPage(),
				(IPluginParent) object,
				manager,
				true);
			manager.add(new Separator());
		}
	} else {
		// just the input object
		object = treeViewer.getInput();
		if (object instanceof IPluginParent) {
			DetailExtensionSection.fillContextMenu(
				getFormPage(),
				(IPluginParent) object,
				manager,
				false, false);
		}
	}
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
}
private void handleApply() {
	try {
		currentElement.setText(text.getText().length() > 0 ? text.getText() : null);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
	applyButton.setEnabled(false);
}
private void handleDelete() {
	IPluginParent parent = (IPluginParent) currentElement.getParent();
	try {
		parent.remove(currentElement);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
	currentElement = null;
	updateInput();
}
private void handleReset() {
	updateText(currentElement);
	resetButton.setEnabled(false);
	applyButton.setEnabled(false);
}
public void initialize(Object input) {
	IPluginModelBase model = (IPluginModelBase)input;
	model.addModelChangedListener(this);
	setReadOnly(!model.isEditable());
	text.setEditable(model.isEditable());
	updateInput();
}
public void initializeImages() {
	genericElementImage = PDEPluginImages.DESC_GENERIC_XML_OBJ.createImage();
}
public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		treeViewer.refresh();
		return;
	}
	Object changeObject = event.getChangedObjects()[0];
	if (changeObject instanceof IPluginElement) {
		IPluginElement element = (IPluginElement) changeObject;
		treeViewer.refresh();
		if (event.getChangeType() == event.INSERT) {
			if (!(element.getParent() instanceof IPluginExtension)) {
			treeViewer.setSelection(new StructuredSelection(element), true);
			}
		}
		else if (event.getChangeType() == event.CHANGE) {
			treeViewer.update(changeObject, null);
		}
	}
}
private Image resolveObjectImage(Object obj) {
	if (obj instanceof IPluginElement) {
		IPluginElement element = (IPluginElement) obj;
		ISchemaElement elementInfo = element.getElementInfo();
		if (elementInfo != null) {
		}
		return genericElementImage;
	}
	return null;
}
private String resolveObjectName(Object obj) {
	String value = obj.toString();
	if (obj instanceof IPluginElement) {
		IPluginElement element = (IPluginElement) obj;
		ISchemaElement elementInfo = element.getElementInfo();
		if (elementInfo != null && elementInfo.getLabelProperty() != null) {
			IPluginAttribute att = element.getAttribute(elementInfo.getLabelProperty());
			if (att != null && att.getValue() != null)
				value = att.getValue();
		}
	}
	return DetailExtensionSection.stripShortcuts(value);
}
public void sectionChanged(
	FormSection source,
	int changeType,
	Object changeObject) {
	if (currentElement != null && currentElement == changeObject)
		return;
	if (changeObject instanceof IPluginElement)
		this.currentElement = (IPluginElement) changeObject;
	else
		currentElement = null;
	updateInput();
}
private void updateInput() {
  treeViewer.setInput(currentElement);
  deleteButton.setEnabled(false);
  applyButton.setEnabled(false);
  resetButton.setEnabled(false);
  updateText(null);
}
private void updateText(IPluginElement element) {
   text.setText(element!=null && element.getText()!=null ? element.getText() : "");
   applyButton.setEnabled(false);
   resetButton.setEnabled(false);
}
}
