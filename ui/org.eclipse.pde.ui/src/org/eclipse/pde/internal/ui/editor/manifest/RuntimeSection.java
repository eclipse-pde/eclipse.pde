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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class RuntimeSection
	extends PDEFormSection
	implements IHyperlinkListener, IModelChangedListener {
	public static final String SECTION_TITLE = "ManifestEditor.RuntimeSection.title";
	public static final String SECTION_MORE = "ManifestEditor.RuntimeSection.more";
	public static final String SECTION_DESC = "ManifestEditor.RuntimeSection.desc";
	public static final String SECTION_FDESC = "ManifestEditor.RuntimeSection.fdesc";
	private FormWidgetFactory factory;
	private Composite libraryParent;
	private boolean needsUpdate;
	private Image libraryImage;
	private Button moreButton;

public RuntimeSection(ManifestFormPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	boolean fragment = ((ManifestEditor) page.getEditor()).isFragmentEditor();
	if (fragment)
		setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
	else
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
private void addLibraryLink(IPluginLibrary library) {
	Label imageLabel = factory.createLabel(libraryParent, "");
	SelectableFormLabel hyperlink = factory.createSelectableLabel(libraryParent, library.getName());
	factory.turnIntoHyperlink(hyperlink, this);
	imageLabel.setImage(libraryImage);
	hyperlink.setData(library);
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	container.setLayout(layout);
	layout = new GridLayout();
	layout.numColumns = 2;
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	layout.verticalSpacing = 2;

	libraryParent = factory.createComposite(container);

	RowLayout rlayout = new RowLayout();
	rlayout.wrap = true;
	libraryParent.setLayout(layout);

	GridData gd = new GridData(GridData.FILL_BOTH);
	libraryParent.setLayoutData(gd);

	Composite buttonContainer = factory.createComposite(container);
	gd = new GridData(GridData.FILL_VERTICAL);
	buttonContainer.setLayoutData(gd);
	layout = new GridLayout();
	layout.marginWidth = 0;
	layout.marginHeight = 0;
	//layout.numColumns = 2;
	buttonContainer.setLayout(layout);

	moreButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(SECTION_MORE), SWT.PUSH);
	gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
	moreButton.setLayoutData(gd);
	final IPDEEditorPage targetPage = getFormPage().getEditor().getPage(ManifestEditor.RUNTIME_PAGE);
	moreButton.setToolTipText(((IFormPage)targetPage).getTitle());
	moreButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
		   getFormPage().getEditor().showPage(targetPage);
		}
	});
	return container;
}
public void dispose() {
	IPluginModelBase model = (IPluginModelBase)getFormPage().getModel();
	model.removeModelChangedListener(this);
	super.dispose();
}
public void initialize(Object input) {
	initializeImages();
	IPluginModelBase model = (IPluginModelBase)input;
	update(false);
	model.addModelChangedListener(this);
}
private void initializeImages() {
	PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
	libraryImage = provider.get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
}
public void linkActivated(Control linkLabel) {
	IPluginLibrary library = (IPluginLibrary)linkLabel.getData();
	getFormPage().getEditor().showPage(ManifestEditor.RUNTIME_PAGE, library);
}
public void linkEntered(Control linkLabel) {
	IPDEEditorPage page = getFormPage().getEditor().getPage(ManifestEditor.RUNTIME_PAGE);
	IPluginLibrary library = (IPluginLibrary)linkLabel.getData();
	String status = ((PDEFormPage)page).getStatusText() + "#"+library.getName();
	IStatusLineManager manager = getFormPage().getEditor().getStatusLineManager();
	if (manager!=null) manager.setMessage(status);
}
public void linkExited(org.eclipse.swt.widgets.Control linkLabel) {
	IStatusLineManager manager = getFormPage().getEditor().getStatusLineManager();
	if (manager != null)
		manager.setMessage("");
}
public void modelChanged(IModelChangedEvent event) {
	int type = event.getChangeType();
	if (type == IModelChangedEvent.WORLD_CHANGED)
		needsUpdate = true;
	else
		if (type == IModelChangedEvent.INSERT || type == IModelChangedEvent.REMOVE) {
			Object[] objects = event.getChangedObjects();
			if (objects[0] instanceof IPluginLibrary) {
				needsUpdate = true;
			}
		}
	if (getFormPage().isVisible())
		update();
}
public void update() {
	if (needsUpdate)
		update(true);
}
public void update(boolean removePrevious) {
	if (removePrevious) {
		Control[] children = libraryParent.getChildren();
		for (int i = 0; i < children.length; i++) {
			children[i].dispose();
		}
	}
	IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
	IPluginBase pluginBase = model.getPluginBase();
	IPluginLibrary [] libraries = pluginBase.getLibraries();
	for (int i=0; i<libraries.length; i++) {
		addLibraryLink(libraries[i]);
	}
	if (removePrevious) {
		libraryParent.layout(true);
		libraryParent.redraw();
	}
	needsUpdate = false;
}
}
