package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;

public class ExtensionsPropertySheet extends ManifestPropertySheet {
	private Composite attComposite;
	private Action cloneAction;
	private Composite composite;
	private Action addAttAction;
	private Action removeAttAction;
	private ActionContributionItem removeAttItem;
	private boolean newAttVisible;
	private Text newAttText;
	private SubActionBars customBars;
	public static final String KEY_NEW_ATTRIBUTE = "ManifestEditor.ExtensionsPropertySheet.newAttribute";
	public static final String KEY_NEW_ATTRIBUTE_ENTRY = "ManifestEditor.ExtensionsPropertySheet.newAttributeEntry";
	public static final String ADD_ATT_LABEL = "ManifestEditor.ExtensionsPropertySheet.addAttAction.label";
	public static final String ADD_ATT_TOOLTIP = "ManifestEditor.ExtensionsPropertySheet.addAttAction.tooltip";
	public static final String REMOVE_ATT_LABEL = "ManifestEditor.ExtensionsPropertySheet.removeAttAction.label";
	public static final String REMOVE_ATT_TOOLTIP = "ManifestEditor.ExtensionsPropertySheet.removeAttAction.tooltip";
	public static final String CLONE_LABEL = "ManifestEditor.ExtensionsPropertySheet.cloneAction.text";
	public static final String CLONE_TOOLTIP = "ManifestEditor.ExtensionsPropertySheet.cloneAction.tooltip";
	private SubActionBars unknownBars;


	class SubActionBars {
		SubMenuManager menuManager;
		SubToolBarManager toolBarManager;
		public void updateActionBars() {
			menuManager.update(true);
			toolBarManager.update(true);
		}
		public void setVisible(boolean visible) {
			menuManager.setVisible(visible);
			toolBarManager.setVisible(visible);
		}
	}

	class PropertyLayout extends Layout {
		public Point computeSize(
			Composite parent,
			int wHint,
			int hHint,
			boolean changed) {
			Control[] children = parent.getChildren();
			Control c1 = children[0];
			Control c2 = children[1];

			Point s1 = c1.computeSize(wHint, hHint, changed);
			Point s2 = c2.computeSize(wHint, hHint, changed);

			int width = Math.max(s1.x, s2.x);
			int height = s2.y;
			if (newAttVisible)
				height += s1.y;
			return new Point(width, height);
		}
		public void layout(Composite parent, boolean changed) {
			Rectangle bounds = parent.getClientArea();
			int y = 0;
			Control[] children = parent.getChildren();
			if (newAttVisible) {
				Control c1 = children[0];
				Point s1 = c1.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
				c1.setLocation(0, 0);
				c1.setSize(bounds.width, s1.y);
				y = s1.y;
			}
			Control c2 = children[1];
			c2.setSize(bounds.width, bounds.height - y);
			c2.setLocation(0, y);
		}
	}   

public ExtensionsPropertySheet(PDEMultiPageEditor editor) {
	super(editor);
}
public void createControl(Composite parent) {
	composite = new Composite(parent, SWT.NULL);
	PropertyLayout layout = new PropertyLayout();
	composite.setLayout(layout);
	createNewAttField(composite);
	super.createControl(composite);
}
protected void createNewAttField(Composite container) {
	attComposite = new Composite(container, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.marginWidth = 0;
	layout.marginHeight = 0;
	attComposite.setLayout(layout);

	Label label = new Label(attComposite, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(KEY_NEW_ATTRIBUTE));
	label.setLayoutData(new GridData());

	newAttText = new Text(attComposite, SWT.BORDER);
	newAttText.addKeyListener(new KeyAdapter() {
		public void keyReleased(KeyEvent e) {
			if (e.character == '\u001b') { // Escape character
				setNewAttVisible(false);
			} else
				if (e.character == '\r') { // Return key
					handleEnter();
				}
		}
	});
	newAttText.addListener(SWT.Traverse, new Listener() {
		public void handleEvent(Event e) {
			// do whatever it is you want to do on commit
			handleEnter();
			// this will prevent the return from 
			// traversing to the button
			e.doit = false;
		}
	});
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	newAttText.setLayoutData(gd);
}
public void fillContextMenu(IMenuManager manager) {
	if (isUnknown()) {
		manager.add(addAttAction);
		manager.add(removeAttAction);
	} else {
		super.fillContextMenu(manager);
	}
}
public void fillLocalMenuBar(IMenuManager menuManager) {
	doFillLocalMenuBar(menuManager);
	menuManager.add(cloneAction);
}
public void fillLocalToolBar(IToolBarManager toolBarManager) {
	//this.toolBarManager = toolBarManager;
	if (!(source instanceof ExtensionPropertySource)) {
		if (isUnknown()) {
			toolBarManager.add(addAttAction);
			toolBarManager.add(removeAttAction);
		} else {
			doFillLocalToolBar(toolBarManager);
		}
		toolBarManager.add(new Separator());
		toolBarManager.add(cloneAction);
	}
}
public Control getControl() {
	return composite;
}
private void handleAddAttribute() {
	setNewAttVisible(true);
}
private void handleClone() {
	IPluginElement element = (IPluginElement) currentInput;
	IPluginParent parent = (IPluginParent) element.getParent();
	IPluginElement newElement = element.createCopy();
	try {
		parent.add(newElement);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
private void handleEnter() {
	((UnknownElementPropertySource) source).addAttribute(newAttText.getText(), "");
	setNewAttVisible(false);
	refreshInput();
}
private void handleRemoveAttribute() {
	IPropertySheetEntry entry = getSelectedEntry();
	if (entry==null) return;
	PDEProblemFinder.fixMe("Should not be using display name as an id");
	String attName = entry.getDisplayName();
	((UnknownElementPropertySource)source).removeAttribute(attName);
	refreshInput();
	removeAttAction.setEnabled(false);
}
public boolean isNewAttVisible() {
	return newAttVisible;
}
protected boolean isUnknown() {
	return source!=null && source instanceof UnknownElementPropertySource;
}
protected void makeActions() {
	super.makeActions();
	addAttAction = new Action("addAtt") {
		public void run() {
			handleAddAttribute();
		}
	};
	addAttAction.setImageDescriptor(PDEPluginImages.DESC_ADD_ATT);
	addAttAction.setHoverImageDescriptor(PDEPluginImages.DESC_ADD_ATT_HOVER);
	addAttAction.setDisabledImageDescriptor(PDEPluginImages.DESC_ADD_ATT_DISABLED);
	addAttAction.setText(PDEPlugin.getResourceString(ADD_ATT_LABEL));
	addAttAction.setToolTipText(PDEPlugin.getResourceString(ADD_ATT_TOOLTIP));
	addAttAction.setEnabled(false);

	removeAttAction = new Action("removeAtt") {
		public void run() {
			handleRemoveAttribute();
		}
	};
	removeAttAction.setImageDescriptor(PDEPluginImages.DESC_REMOVE_ATT);
	removeAttAction.setHoverImageDescriptor(PDEPluginImages.DESC_REMOVE_ATT_HOVER);
	removeAttAction.setDisabledImageDescriptor(PDEPluginImages.DESC_REMOVE_ATT_DISABLED);
	removeAttAction.setText(PDEPlugin.getResourceString(REMOVE_ATT_LABEL));
	removeAttAction.setToolTipText(PDEPlugin.getResourceString(REMOVE_ATT_TOOLTIP));
	removeAttAction.setEnabled(false);

	cloneAction = new Action("clone") {
		public void run() {
			handleClone();
		}
	};
	cloneAction.setImageDescriptor(PDEPluginImages.DESC_CLONE_EL);
	cloneAction.setHoverImageDescriptor(PDEPluginImages.DESC_CLONE_EL_HOVER);
	cloneAction.setDisabledImageDescriptor(PDEPluginImages.DESC_CLONE_EL_DISABLED);
	cloneAction.setText(PDEPlugin.getResourceString(CLONE_LABEL));
	cloneAction.setToolTipText(PDEPlugin.getResourceString(CLONE_TOOLTIP));
	cloneAction.setEnabled(false);
}
public void makeContributions(
	IMenuManager menuManager,
	IToolBarManager toolBarManager,
	IStatusLineManager statusLineManager) {
	superMakeContributions(new NullMenuManager(), new NullToolBarManager(), statusLineManager);
	// Create and fill custom element bars
	customBars = new SubActionBars();
	customBars.menuManager = new SubMenuManager(menuManager);
	customBars.toolBarManager = new SubToolBarManager(toolBarManager);
	customBars.setVisible(true);

	doFillLocalToolBar(customBars.toolBarManager);
	customBars.toolBarManager.add(cloneAction);

	doFillLocalMenuBar(customBars.menuManager);
	customBars.menuManager.add(cloneAction);

	// Create and fill unknown element bars    
	unknownBars = new SubActionBars();

	unknownBars.toolBarManager = new SubToolBarManager(toolBarManager);
	unknownBars.toolBarManager.add(new Separator());
	unknownBars.toolBarManager.add(addAttAction);
	unknownBars.toolBarManager.add(removeAttAction);
	unknownBars.toolBarManager.add(new Separator());
	unknownBars.toolBarManager.add(cloneAction);

	unknownBars.menuManager = new SubMenuManager(menuManager);
	unknownBars.menuManager.add(new Separator());
	unknownBars.menuManager.add(addAttAction);
	unknownBars.menuManager.add(removeAttAction);
	unknownBars.menuManager.add(new Separator());
	unknownBars.menuManager.add(cloneAction);

	//switchBars();
}
public void selectionChanged(IWorkbenchPart part, ISelection sel) {
	super.selectionChanged(part, sel);
	updateActionVisibility();
}
public void setNewAttVisible(boolean value) {
	if (value != newAttVisible) {
		newAttVisible = value;
		attComposite.setVisible(value);
		composite.layout(true);
		if (value == true) {
			newAttText.setText(PDEPlugin.getResourceString(KEY_NEW_ATTRIBUTE_ENTRY));
			newAttText.selectAll();
			newAttText.setFocus();
		}
	}
}
protected void switchBars() {
	boolean unknown = isUnknown();
	unknownBars.setVisible(unknown);
	customBars.setVisible(!unknown);
	customBars.updateActionBars();
}
public void updateActions(IPropertySheetEntry entry) {
	super.updateActions(entry);
	if (isUnknown() && isEditable()) {
		removeAttAction.setEnabled(entry != null);
	} else {
	}
}
private void updateActionVisibility() {
	boolean unknown = isUnknown();
	addAttAction.setEnabled(unknown && isEditable());
	removeAttAction.setEnabled(false);
	setNewAttVisible(false);
	if (source instanceof ExtensionElementPropertySource
		|| source instanceof UnknownElementPropertySource) {
		cloneAction.setEnabled(isEditable());
	} else
		cloneAction.setEnabled(false);
}
}
