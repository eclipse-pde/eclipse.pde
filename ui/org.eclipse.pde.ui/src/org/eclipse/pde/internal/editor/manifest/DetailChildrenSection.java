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
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.jface.viewers.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.parts.TreePart;
import org.eclipse.pde.internal.preferences.MainPreferencePage;
import org.eclipse.pde.model.*;
import org.eclipse.pde.internal.model.plugin.*;

public class DetailChildrenSection
	extends TreeSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE =
		"ManifestEditor.DetailChildrenSection.title";
	public static final String SECTION_BODY_TEXT =
		"ManifestEditor.DetailChildrenSection.bodyText";
	public static final String KEY_APPLY = "Actions.apply.label";
	public static final String KEY_RESET = "Actions.reset.label";
	public static final String KEY_DELETE = "Actions.delete.label";
	private FormWidgetFactory factory;
	private Button applyButton;
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
			if (children == null)
				children = new Object[0];
			return children;
		}
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
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
		super(page, new String[] { PDEPlugin.getResourceString(KEY_DELETE)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	}
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		this.factory = factory;
		initializeImages();
		GridData gd;
		Composite container = createClientContainer(parent, 2, factory);

		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI, 2, factory);

		treeViewer = treePart.getTreeViewer();
		treeViewer.setLabelProvider(new ChildrenLabelProvider());
		treeViewer.setContentProvider(new ContentProvider());
		treeViewer.setAutoExpandLevel(999);

		Label label =
			factory.createLabel(container, PDEPlugin.getResourceString(SECTION_BODY_TEXT));
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		// text
		text =
			factory.createText(
				container,
				"",
				SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | factory.BORDER_STYLE);
		text.setEditable(false);
		gd = new GridData(GridData.FILL_BOTH);
		text.setLayoutData(gd);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				applyButton.setEnabled(true);
				resetButton.setEnabled(true);
			}
		});

		Composite buttonContainer = factory.createComposite(container);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_FILL);
		buttonContainer.setLayoutData(gd);

		// add buttons
		applyButton =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(KEY_APPLY),
				SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		applyButton.setLayoutData(gd);
		applyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleApply();
			}
		});

		resetButton =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(KEY_RESET),
				SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		resetButton.setLayoutData(gd);
		resetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleReset();
			}
		});

		if (SWT.getPlatform().equals("motif") == false)
			factory.paintBordersFor(container);
		return container;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		fireSelectionNotification(item);
		getFormPage().setSelection(selection);
		IModel model = (IModel) getFormPage().getEditor().getModel();
		getTreePart().setButtonEnabled(0, model.isEditable() && item != null);
		if (item instanceof IPluginElement) {
			currentElement = (IPluginElement) item;
		} else
			currentElement = null;
		updateText(currentElement);
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		PropertiesAction action = new PropertiesAction(getFormPage().getEditor());
		action.run();
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleDelete();
	}

	public void dispose() {
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
	protected void fillContextMenu(IMenuManager manager) {
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
					false,
					false);
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
		IPluginModelBase model = (IPluginModelBase) input;
		model.addModelChangedListener(this);
		setReadOnly(!model.isEditable());
		text.setEditable(model.isEditable());
		updateInput();
	}
	public void initializeImages() {
		genericElementImage =
			PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_GENERIC_XML_OBJ);
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
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
			} else if (event.getChangeType() == event.CHANGE) {
				treeViewer.update(changeObject, null);
				if (treeViewer.getTree().isFocusControl()) {
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
	private Image resolveObjectImage(Object obj) {
		if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			Image customImage = DetailExtensionSection.getCustomImage(element);
			if (customImage != null)
				return customImage;
			return genericElementImage;
		}
		return null;
	}
	private String resolveObjectName(Object obj) {
		String value = obj.toString();
		if (!MainPreferencePage.isFullNameModeEnabled()) return value;
		if (obj instanceof PluginElement) {
			PluginElement element = (PluginElement) obj;
			ISchemaElement elementInfo = element.getElementInfo();
			if (elementInfo != null && elementInfo.getLabelProperty() != null) {
				IPluginAttribute att = element.getAttribute(elementInfo.getLabelProperty());
				if (att != null && att.getValue() != null)
					value = att.getValue();
			}
		}
		value = DetailExtensionSection.stripShortcuts(value);
		return ((IModel) getFormPage().getModel()).getResourceString(value);
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
		getTreePart().setButtonEnabled(0, false);
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);
		updateText(currentElement);
		text.setEditable(!isReadOnly() && currentElement != null);
	}
	private void updateText(IPluginElement element) {
		text.setText(
			element != null && element.getText() != null ? element.getText() : "");
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);
	}
	protected void doPaste(Object target, Object[] objects) {
		if (target==null) target = currentElement;
		try {
			for (int i = 0; i < objects.length; i++) {
				Object obj = objects[i];
				if (obj instanceof IPluginElement && target instanceof IPluginParent) {
					PluginElement element = (PluginElement) obj;
					element.setModel((IPluginModelBase)getFormPage().getModel());
					element.setParent((IPluginParent)target);
					((IPluginParent) target).add(element);
					if (element instanceof PluginParent)
						((PluginParent)element).reconnect();
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}