package org.eclipse.pde.internal.editor.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.swt.custom.*;
import org.eclipse.core.runtime.IPath;

public class ComponentSpecSection extends PDEFormSection {
	public static final String SECTION_TITLE = "ComponentEditor.SpecSection.title";
	public static final String SECTION_DESC = "ComponentEditor.SpecSection.desc";
	public static final String SECTION_ID = "ComponentEditor.SpecSection.id";
	public static final String SECTION_NAME = "ComponentEditor.SpecSection.name";
	public static final String SECTION_VERSION = "ComponentEditor.SpecSection.version";
	public static final String SECTION_PROVIDER = "ComponentEditor.SpecSection.provider";
	public static final String SECTION_CREATE_JAR = "ComponentEditor.SpecSection.createJar";
	private FormText idText;
	private FormText titleText;
	private Button createJarButton;
	private boolean updateNeeded;
	private FormText providerText;
	private FormText versionText;

public ComponentSpecSection(ComponentFormPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public void commitChanges(boolean onSave) {
	IComponentModel model = (IComponentModel) getFormPage().getModel();
	IComponent component = model.getComponent();
	String oldId = component.getId();
	String oldVersion = component.getVersion();
	titleText.commit();
	providerText.commit();
	idText.commit();
	versionText.commit();
	String newId = component.getId();
	String newVersion = component.getVersion();
	if (!oldId.equals(newId) || !oldVersion.equals(newVersion)) {
		// component folder must be renamed
		String newName = newId+"_"+newVersion;
		IFolder folder = (IFolder)model.getUnderlyingResource().getParent();
		IPath newPath = folder.getFullPath().removeLastSegments(1).append(newName);
		try {
		   folder.move(newPath, false, null);
		}
		catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.verticalSpacing = 9;
	layout.horizontalSpacing = 6;
	container.setLayout(layout);

	IComponentModel model = (IComponentModel) getFormPage().getModel();
	final IComponent component = model.getComponent();

	idText = new FormText(createText(container, PDEPlugin.getResourceString(SECTION_ID), factory));
	idText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormText text) {
			try {
				component.setId(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	});

	titleText = new FormText(createText(container, PDEPlugin.getResourceString(SECTION_NAME), factory));
	titleText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormText text) {
			try {
				component.setLabel(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			getFormPage().getForm().setTitle(component.getLabel());
			((ComponentEditor) getFormPage().getEditor()).updateTitle();
		}
	});
	versionText = new FormText(createText(container, PDEPlugin.getResourceString(SECTION_VERSION), factory));
	versionText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormText text) {
			try {
				component.setVersion(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	});

	providerText = new FormText(createText(container, PDEPlugin.getResourceString(SECTION_PROVIDER), factory));
	providerText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormText text) {
			try {
				component.setProviderName(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	});

	GridData gd = (GridData) idText.getControl().getLayoutData();
	gd.widthHint = 150;

	createJarButton = factory.createButton(container, PDEPlugin.getResourceString(SECTION_CREATE_JAR), SWT.PUSH);
	createJarButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleCreateJar();
		}
	});
	gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
	gd.horizontalSpan = 2;
	createJarButton.setLayoutData(gd);

	factory.paintBordersFor(container);
	return container;
}
public void dispose() {
	IComponentModel model = (IComponentModel) getFormPage().getModel();
	model.removeModelChangedListener(this);
}
private void handleCreateJar() {
	final ComponentEditorContributor contributor =
		(ComponentEditorContributor) getFormPage().getEditor().getContributor();
	BusyIndicator.showWhile(createJarButton.getDisplay(), new Runnable() {
		public void run() {
			contributor.getBuildAction().run();
		}
	});
}
private void handleSynchronize() {
	final ComponentEditorContributor contributor =
		(ComponentEditorContributor) getFormPage().getEditor().getContributor();
	BusyIndicator.showWhile(createJarButton.getDisplay(), new Runnable() {
		public void run() {
			contributor.getSynchronizeAction().run();
		}
	});
}
public void initialize(Object input) {
	IComponentModel model = (IComponentModel)input;
	update(input);
	if (model.isEditable()==false) {
		idText.getControl().setEnabled(false);
		titleText.getControl().setEnabled(false);
		versionText.getControl().setEnabled(false);
		providerText.getControl().setEnabled(false);
	}
	model.addModelChangedListener(this);
}
public boolean isDirty() {
	return titleText.isDirty()
		|| idText.isDirty()
		|| providerText.isDirty()
		|| versionText.isDirty();
}
public void modelChanged(IModelChangedEvent e) {
	if (e.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		updateNeeded=true;
	}
}
public void setFocus() {
	if (idText != null)
		idText.getControl().setFocus();
}
private void setIfDefined(FormText formText, String value) {
	if (value != null) {
		formText.setValue(value);
		formText.setDirty(false);
	}
}
private void setIfDefined(Text text, String value) {
	if (value != null)
		text.setText(value);
}
public void update() {
	if (updateNeeded) {
		this.update(getFormPage().getModel());
	}
}
public void update(Object input) {
	IComponentModel model = (IComponentModel)input;
	IComponent component = model.getComponent();
	setIfDefined(idText, component.getId());
	setIfDefined(titleText, component.getLabel());
	getFormPage().getForm().setTitle(component.getLabel());
	setIfDefined(versionText, component.getVersion());
	setIfDefined(providerText, component.getProviderName());
	updateNeeded=false;
}
}
