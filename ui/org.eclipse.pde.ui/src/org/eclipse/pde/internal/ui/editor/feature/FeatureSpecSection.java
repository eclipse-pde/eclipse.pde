package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.eclipse.update.ui.forms.internal.*;

public class FeatureSpecSection extends PDEFormSection {
	public static final String SECTION_TITLE = "FeatureEditor.SpecSection.title";
	public static final String SECTION_DESC = "FeatureEditor.SpecSection.desc";
	public static final String SECTION_ID = "FeatureEditor.SpecSection.id";
	public static final String SECTION_NAME = "FeatureEditor.SpecSection.name";
	public static final String SECTION_VERSION =
		"FeatureEditor.SpecSection.version";
	public static final String SECTION_PROVIDER =
		"FeatureEditor.SpecSection.provider";
	public static final String SECTION_IMAGE = "FeatureEditor.SpecSection.image";
	public static final String SECTION_BROWSE = "FeatureEditor.SpecSection.browse";
	public static final String SECTION_PRIMARY =
		"FeatureEditor.SpecSection.primary";
	public static final String SECTION_CREATE_JAR =
		"FeatureEditor.SpecSection.createJar";
	public static final String SECTION_SYNCHRONIZE =
		"FeatureEditor.SpecSection.synchronize";
	public static final String KEY_BAD_VERSION_TITLE =
		"FeatureEditor.SpecSection.badVersionTitle";
	public static final String KEY_BAD_VERSION_MESSAGE =
		"FeatureEditor.SpecSection.badVersionMessage";

	private FormEntry idText;
	private FormEntry titleText;
	private FormEntry versionText;
	private FormEntry providerText;
	private FormEntry imageText;
	private Button browseImageButton;

	private Button primaryButton;
	private Button createJarButton;
	private Button synchronizeButton;

	private boolean updateNeeded;

	public FeatureSpecSection(FeatureFormPage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	public void commitChanges(boolean onSave) {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IFeature feature = model.getFeature();
		titleText.commit();
		providerText.commit();
		idText.commit();
		versionText.commit();
		imageText.commit();
		try {
			feature.setPrimary(primaryButton.getSelection());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		final IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		final IFeature feature = model.getFeature();

		idText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_ID), factory, 2));
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

		titleText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_NAME), factory, 2));
		titleText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setLabel(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
				getFormPage().getForm().setHeadingText(
					model.getResourceString(feature.getLabel()));
				((FeatureEditor) getFormPage().getEditor()).updateTitle();
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		versionText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_VERSION), factory, 2));
		versionText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				if (verifySetVersion(feature, text.getValue()) == false) {
					warnBadVersionFormat(text.getValue());
					text.setValue(feature.getVersion());
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});

		providerText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_PROVIDER), factory, 2));
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

		imageText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_IMAGE), factory));
		imageText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setImageName(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		GridData gd = (GridData)imageText.getControl().getLayoutData();
		gd.grabExcessHorizontalSpace = true;
		
		
		browseImageButton = factory.createButton(container, PDEPlugin.getResourceString(SECTION_BROWSE), SWT.PUSH);
		browseImageButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseImage();
			}
		});

		gd = (GridData) idText.getControl().getLayoutData();
		gd.widthHint = 150;

		primaryButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(SECTION_PRIMARY),
				SWT.CHECK);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
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
		gd.horizontalSpan = 3;
		buttonContainer.setLayoutData(gd);
		GridLayout blayout = new GridLayout();
		buttonContainer.setLayout(blayout);
		blayout.makeColumnsEqualWidth = true;
		blayout.numColumns = 2;
		blayout.marginWidth = 0;

		createJarButton =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(SECTION_CREATE_JAR),
				SWT.PUSH);
		createJarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleCreateJar();
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		createJarButton.setLayoutData(gd);

		synchronizeButton =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(SECTION_SYNCHRONIZE),
				SWT.PUSH);
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
		IModel model = (IModel) getFormPage().getModel();
		if (model instanceof IEditable) {
			IEditable editable = (IEditable) model;
			editable.setDirty(true);
			getFormPage().getEditor().fireSaveNeeded();
		}
	}

	private boolean verifySetVersion(IFeature feature, String value) {
		try {
			PluginVersionIdentifier pvi = new PluginVersionIdentifier(value);
			feature.setVersion(pvi.toString());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private void warnBadVersionFormat(String text) {
		MessageDialog.openError(
			PDEPlugin.getActiveWorkbenchShell(),
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
	private void handleBrowseImage() {
		final IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IResource resource = model.getUnderlyingResource();
		final IProject project = resource.getProject();

		BusyIndicator.showWhile(primaryButton.getDisplay(), new Runnable() {
			public void run() {
				ResourceSelectionDialog dialog =
					new ResourceSelectionDialog(primaryButton.getShell(), project, null);
				dialog.open();
				Object[] result = dialog.getResult();
				if (result==null || result.length==0) return;
				IResource resource = (IResource)result[0];
				acceptImage(resource);
			}
		});
	}
	
	private void acceptImage(IResource resource) {
		IPath path = resource.getProjectRelativePath();
		imageText.setValue(path.toString());
	}
	public void initialize(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		update(input);
		if (model.isEditable() == false) {
			idText.getControl().setEditable(false);
			titleText.getControl().setEditable(false);
			versionText.getControl().setEditable(false);
			providerText.getControl().setEditable(false);
			imageText.getControl().setEditable(false);
			primaryButton.setEnabled(false);
			createJarButton.setEnabled(false);
			synchronizeButton.setEnabled(false);
			browseImageButton.setEnabled(false);
		}
		model.addModelChangedListener(this);
	}
	public boolean isDirty() {
		return titleText.isDirty()
			|| idText.isDirty()
			|| providerText.isDirty()
			|| versionText.isDirty()
			|| imageText.isDirty();
	}
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
		}
		else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object objs[] = e.getChangedObjects();
			if (objs.length>0 && objs[0] instanceof IFeature) {
				updateNeeded=true;
				if (getFormPage().isVisible())
					update();
			}
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
		IFeatureModel model = (IFeatureModel) input;
		IFeature feature = model.getFeature();
		setIfDefined(idText, feature.getId());
		setIfDefined(titleText, feature.getLabel());
		getFormPage().getForm().setHeadingText(
			model.getResourceString(feature.getLabel()));
		setIfDefined(versionText, feature.getVersion());
		setIfDefined(providerText, feature.getProviderName());
		setIfDefined(imageText, feature.getImageName());
		primaryButton.setSelection(feature.isPrimary());
		updateNeeded = false;
	}
}