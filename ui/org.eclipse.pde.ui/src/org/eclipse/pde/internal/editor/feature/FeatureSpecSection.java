package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.model.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.swt.custom.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;

public class FeatureSpecSection extends PDEFormSection {
	public static final String SECTION_TITLE = "FeatureEditor.SpecSection.title";
	public static final String SECTION_DESC = "FeatureEditor.SpecSection.desc";
	public static final String SECTION_ID = "FeatureEditor.SpecSection.id";
	public static final String SECTION_NAME = "FeatureEditor.SpecSection.name";
	public static final String SECTION_VERSION = "FeatureEditor.SpecSection.version";
	public static final String SECTION_PROVIDER = "FeatureEditor.SpecSection.provider";
	public static final String SECTION_PRIMARY = "FeatureEditor.SpecSection.primary";
	public static final String SECTION_CREATE_JAR = "FeatureEditor.SpecSection.createJar";
	public static final String SECTION_SYNCHRONIZE = "FeatureEditor.SpecSection.synchronize";
	public static final String KEY_BAD_VERSION_TITLE = "FeatureEditor.SpecSection.badVersionTitle";
	public static final String KEY_BAD_VERSION_MESSAGE = "FeatureEditor.SpecSection.badVersionMessage";
	
	private FormEntry idText;
	private FormEntry titleText;
	private Button primaryButton;
	private Button createJarButton;
	private Button synchronizeButton;

	private boolean updateNeeded;
	private FormEntry providerText;
	private FormEntry versionText;

public FeatureSpecSection(FeatureFormPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public void commitChanges(boolean onSave) {
	IFeatureModel model = (IFeatureModel) getFormPage().getModel();
	IFeature feature = model.getFeature();
	String oldId = feature.getId();
	String oldVersion = feature.getVersion();
	titleText.commit();
	providerText.commit();
	idText.commit();
	versionText.commit();
	String newId = feature.getId();
	String newVersion = feature.getVersion();
	if (!oldId.equals(newId) || !oldVersion.equals(newVersion)) {
		// feature folder must be renamed
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
	try {
		feature.setPrimary(primaryButton.getSelection());
	}
	catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.verticalSpacing = 9;
	layout.horizontalSpacing = 6;
	container.setLayout(layout);

	final IFeatureModel model = (IFeatureModel) getFormPage().getModel();
	final IFeature feature = model.getFeature();

	idText = new FormEntry(createText(container, PDEPlugin.getResourceString(SECTION_ID), factory));
	idText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			try {
				feature.setId(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		public void textDirty(FormEntry text) {
			forceDirty();
		}
	});
	idText.getControl().setEditable(false);

	titleText = new FormEntry(createText(container, PDEPlugin.getResourceString(SECTION_NAME), factory));
	titleText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			try {
				feature.setLabel(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			getFormPage().getForm().setHeadingText(model.getResourceString(feature.getLabel()));
			((FeatureEditor) getFormPage().getEditor()).updateTitle();
		}
		public void textDirty(FormEntry text) {
			forceDirty();
		}
	});
	versionText = new FormEntry(createText(container, PDEPlugin.getResourceString(SECTION_VERSION), factory));
	versionText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			if (verifySetVersion(feature, text.getValue())==false) {
				warnBadVersionFormat(text.getValue());
				text.setValue(feature.getVersion());
			}
		}
		public void textDirty(FormEntry text) {
			forceDirty();
		}
	});
	versionText.getControl().setEditable(false);

	providerText = new FormEntry(createText(container, PDEPlugin.getResourceString(SECTION_PROVIDER), factory));
	providerText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			try {
				feature.setProviderName(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		public void textDirty(FormEntry text) {
			forceDirty();
		}
	});

	GridData gd = (GridData) idText.getControl().getLayoutData();
	gd.widthHint = 150;
	
	primaryButton = factory.createButton(container, PDEPlugin.getResourceString(SECTION_PRIMARY), SWT.CHECK);
	gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	gd.horizontalSpan = 2;
	primaryButton.setLayoutData(gd);
	primaryButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			try {
				feature.setPrimary(primaryButton.getSelection());
			} catch (CoreException ex) {
				PDEPlugin.logException(ex);
			}
		}
	});

	Composite buttonContainer = factory.createComposite(container);
	gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
	gd.horizontalSpan = 2;
	buttonContainer.setLayoutData(gd);
	GridLayout blayout = new GridLayout();
	buttonContainer.setLayout(blayout);
	blayout.makeColumnsEqualWidth=true;
	blayout.numColumns = 2;
	blayout.marginWidth = 0;
	
	
	createJarButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(SECTION_CREATE_JAR), SWT.PUSH);
	createJarButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleCreateJar();
		}
	});
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	createJarButton.setLayoutData(gd);
	
	synchronizeButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(SECTION_SYNCHRONIZE), SWT.PUSH);
	synchronizeButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleSynchronize();
		}
	});
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	synchronizeButton.setLayoutData(gd);

	factory.paintBordersFor(container);
	return container;
}

private void forceDirty() {
	setDirty(true);
	IModel model = (IModel)getFormPage().getModel();
	if (model instanceof IEditable) {
		IEditable editable = (IEditable)model;
		editable.setDirty(true);
		getFormPage().getEditor().fireSaveNeeded();
	}
}

private boolean verifySetVersion(IFeature feature, String value) {
	try {
		PluginVersionIdentifier pvi = new PluginVersionIdentifier(value);
		feature.setVersion(pvi.toString());
	}
	catch (Exception e) {
		return false;
	}
	return true;
}

private void warnBadVersionFormat(String text) {
	MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(),
		PDEPlugin.getResourceString(KEY_BAD_VERSION_TITLE),
		PDEPlugin.getResourceString(KEY_BAD_VERSION_MESSAGE));
}

public void dispose() {
	IFeatureModel model = (IFeatureModel) getFormPage().getModel();
	model.removeModelChangedListener(this);
	super.dispose();
}
private void handleCreateJar() {
	final FeatureEditorContributor contributor =
		(FeatureEditorContributor) getFormPage().getEditor().getContributor();
	BusyIndicator.showWhile(createJarButton.getDisplay(), new Runnable() {
		public void run() {
			contributor.getBuildAction().run();
		}
	});
}
private void handleSynchronize() {
	final FeatureEditorContributor contributor =
		(FeatureEditorContributor) getFormPage().getEditor().getContributor();
	BusyIndicator.showWhile(createJarButton.getDisplay(), new Runnable() {
		public void run() {
			contributor.getSynchronizeAction().run();
		}
	});
}
public void initialize(Object input) {
	IFeatureModel model = (IFeatureModel)input;
	update(input);
	if (model.isEditable()==false) {
		idText.getControl().setEnabled(false);
		titleText.getControl().setEnabled(false);
		versionText.getControl().setEnabled(false);
		providerText.getControl().setEnabled(false);
		primaryButton.setEnabled(false);
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
private void setIfDefined(FormEntry formText, String value) {
	if (value != null) {
		formText.setValue(value, true);
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
	IFeatureModel model = (IFeatureModel)input;
	IFeature feature = model.getFeature();
	setIfDefined(idText, feature.getId());
	setIfDefined(titleText, feature.getLabel());
	getFormPage().getForm().setHeadingText(model.getResourceString(feature.getLabel()));
	setIfDefined(versionText, feature.getVersion());
	setIfDefined(providerText, feature.getProviderName());
	primaryButton.setSelection(feature.isPrimary());
	updateNeeded=false;
}
}
