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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

public class FeatureSpecSection extends PDESection {
	public static final String SECTION_TITLE = "FeatureEditor.SpecSection.title";
	public static final String SECTION_DESC = "FeatureEditor.SpecSection.desc";
	public static final String SECTION_ID = "FeatureEditor.SpecSection.id";
	public static final String SECTION_NAME = "FeatureEditor.SpecSection.name";
	public static final String SECTION_VERSION =
		"FeatureEditor.SpecSection.version";
	public static final String SECTION_PROVIDER =
		"FeatureEditor.SpecSection.provider";
	public static final String SECTION_PLUGIN =
		"FeatureEditor.SpecSection.plugin";
	public static final String SECTION_IMAGE = "FeatureEditor.SpecSection.image";
	public static final String SECTION_BROWSE = "FeatureEditor.SpecSection.browse";
	public static final String SECTION_PRIMARY =
		"FeatureEditor.SpecSection.primary";
	public static final String SECTION_EXCLUSIVE =
		"FeatureEditor.SpecSection.exclusive";
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
	private FormEntry pluginText;
	private FormEntry imageText;

	private Button primaryButton;
	private Button exclusiveButton;
	private Button createJarButton;
	private Button synchronizeButton;
	private boolean blockNotification;

	public FeatureSpecSection(FeatureFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		createClient(getSection(), page.getManagedForm().getToolkit());
	}
	
	public void commit(boolean onSave) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		titleText.commit();
		providerText.commit();
		pluginText.commit();
		idText.commit();
		versionText.commit();
		imageText.commit();
		/*
		 * Not needed - this is done directly in the
		 * button selection listener.
		try {
			feature.setPrimary(primaryButton.getSelection());
			feature.setExclusive(exclusiveButton.getSelection());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		*/
		super.commit(onSave);
	}
	
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		final IFeatureModel model = (IFeatureModel) getPage().getModel();
		final IFeature feature = model.getFeature();

		idText =
			new FormEntry(container, 
					toolkit,
					PDEPlugin.getResourceString(SECTION_ID),
					null,
					false);
		idText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setId(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		idText.getText().setEditable(false);

		titleText =
			new FormEntry(container,
					toolkit,
					PDEPlugin.getResourceString(SECTION_NAME),
					null, false);
		titleText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setLabel(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
				getPage().getManagedForm().getForm().setText(
					model.getResourceString(feature.getLabel()));
				((FeatureEditor) getPage().getEditor()).updateTitle();
			}
		});
		versionText =
			new FormEntry(container, toolkit,
					PDEPlugin.getResourceString(SECTION_VERSION),
					null,
					false);
		versionText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				if (verifySetVersion(feature, text.getValue()) == false) {
					warnBadVersionFormat(text.getValue());
					text.setValue(feature.getVersion());
				}
			}
		});

		providerText =
			new FormEntry(container, 
					toolkit,
					PDEPlugin.getResourceString(SECTION_PROVIDER), 
					null,
					false);
		providerText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setProviderName(getNonNullValue(text.getValue()));
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		
		pluginText =
			new FormEntry(container, 
					toolkit,
					PDEPlugin.getResourceString(SECTION_PLUGIN),
					null, 
					false);
		pluginText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setPlugin(getNonNullValue(text.getValue()));
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});

		imageText =
			new FormEntry(container, toolkit,
					PDEPlugin.getResourceString(SECTION_IMAGE),
					PDEPlugin.getResourceString(SECTION_BROWSE),
					false);
					
		imageText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setImageName(getNonNullValue(text.getValue()));
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowseImage();
			}
		});

		GridData gd = (GridData) idText.getText().getLayoutData();
		gd.widthHint = 150;
		
		Composite checkContainer = toolkit.createComposite(container);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		checkContainer.setLayoutData(gd);
		GridLayout blayout = new GridLayout();
		checkContainer.setLayout(blayout);
		blayout.numColumns = 2;
		blayout.marginWidth = 0;
		blayout.marginHeight = 0;

		primaryButton =
			toolkit.createButton(
				checkContainer,
				PDEPlugin.getResourceString(SECTION_PRIMARY),
				SWT.CHECK);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		//gd.horizontalSpan = 3;
		primaryButton.setLayoutData(gd);
		primaryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					if (!blockNotification)
						feature.setPrimary(primaryButton.getSelection());
				} catch (CoreException ex) {
					PDEPlugin.logException(ex);
				}
			}
		});
		
		exclusiveButton =
			toolkit.createButton(
				checkContainer,
				PDEPlugin.getResourceString(SECTION_EXCLUSIVE),
				SWT.CHECK);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		//gd.horizontalSpan = 3;
		exclusiveButton.setLayoutData(gd);
		exclusiveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					if (!blockNotification)
						feature.setExclusive(exclusiveButton.getSelection());
				} catch (CoreException ex) {
					PDEPlugin.logException(ex);
				}
			}
		});	

		Composite buttonContainer = toolkit.createComposite(container);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.horizontalSpan = 3;
		buttonContainer.setLayoutData(gd);
		blayout = new GridLayout();
		buttonContainer.setLayout(blayout);
		blayout.makeColumnsEqualWidth = true;
		blayout.numColumns = 2;
		blayout.marginWidth = 0;


		createJarButton =
			toolkit.createButton(
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
			toolkit.createButton(
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

		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}
	
	private String getNonNullValue(String value) {
		return value.length()>0?value:null;
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
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
	
	private void handleCreateJar() {
		final FeatureEditorContributor contributor =
			(FeatureEditorContributor) getPage().getPDEEditor().getContributor();
		BusyIndicator.showWhile(createJarButton.getDisplay(), new Runnable() {
			public void run() {
				contributor.getBuildAction().run();
			}
		});
	}
	private void handleSynchronize() {
		final FeatureEditorContributor contributor =
			(FeatureEditorContributor) getPage().getPDEEditor().getContributor();
		BusyIndicator.showWhile(synchronizeButton.getDisplay(), new Runnable() {
			public void run() {
				contributor.getSynchronizeAction().run();
			}
		});
	}
	private void handleBrowseImage() {
		final IFeatureModel model = (IFeatureModel) getPage().getModel();
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
	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		refresh();
		if (model.isEditable() == false) {
			idText.getText().setEditable(false);
			titleText.getText().setEditable(false);
			versionText.getText().setEditable(false);
			providerText.getText().setEditable(false);
			pluginText.getText().setEditable(false);
			imageText.getText().setEditable(false);
			primaryButton.setEnabled(false);
			exclusiveButton.setEnabled(false);
			createJarButton.setEnabled(false);
			synchronizeButton.setEnabled(false);
			imageText.getButton().setEnabled(false);
		}
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object objs[] = e.getChangedObjects();
			if (objs.length>0 && objs[0] instanceof IFeature) {
				markStale();
			}
		}
	}
	public void setFocus() {
		if (idText != null)
			idText.getText().setFocus();
	}
	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}

	public void refresh() {
		blockNotification=true;
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		setIfDefined(idText, feature.getId());
		setIfDefined(titleText, feature.getLabel());
		getPage().getManagedForm().getForm().setText(
			model.getResourceString(feature.getLabel()));
		setIfDefined(versionText, feature.getVersion());
		setIfDefined(providerText, feature.getProviderName());
		setIfDefined(pluginText, feature.getPlugin());
		setIfDefined(imageText, feature.getImageName());
		primaryButton.setSelection(feature.isPrimary());
		exclusiveButton.setSelection(feature.isExclusive());
		super.refresh();
		blockNotification=false;
	}
	/**
	 * @see org.eclipse.update.ui.forms.internal.FormSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers =
			new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}
}
