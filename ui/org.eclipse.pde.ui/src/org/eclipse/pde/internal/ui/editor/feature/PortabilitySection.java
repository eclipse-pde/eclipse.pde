package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.pde.internal.ui.util.Choice;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.update.ui.forms.internal.FormEntry;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.update.ui.forms.internal.IFormTextListener;

public class PortabilitySection extends PDEFormSection {
	public static final String KEY_DIALOG_TITLE =
		"FeatureEditor.PortabilityChoicesDialog.title";
	public static final String SECTION_TITLE =
		"FeatureEditor.PortabilitySection.title";
	public static final String SECTION_DESC =
		"FeatureEditor.PortabilitySection.desc";
	public static final String SECTION_OS = "FeatureEditor.PortabilitySection.os";
	public static final String SECTION_WS = "FeatureEditor.PortabilitySection.ws";
	public static final String SECTION_NL = "FeatureEditor.PortabilitySection.nl";
	public static final String SECTION_ARCH =
		"FeatureEditor.PortabilitySection.arch";
	public static final String SECTION_EDIT =
		"FeatureEditor.PortabilitySection.edit";

	private FormEntry osText;
	private Button osButton;
	private FormEntry wsText;
	private Button wsButton;
	private FormEntry nlText;
	private Button nlButton;
	private FormEntry archText;
	private Button archButton;
	private boolean updateNeeded;

	public PortabilitySection(FeatureFormPage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		setCollapsable(true);
		IFeatureModel model = (IFeatureModel) page.getModel();
		IFeature feature = model.getFeature();
		setCollapsed(
			feature.getOS() == null && feature.getWS() == null && feature.getNL() == null);
	}

	public boolean canPaste(Clipboard clipboard) {
		return (clipboard.getContents(TextTransfer.getInstance()) != null);
	}
	public void commitChanges(boolean onSave) {
		osText.commit();
		wsText.commit();
		nlText.commit();
		archText.commit();
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		final IFeature feature = model.getFeature();

		osText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_OS), factory));
		osText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setOS(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		limitTextWidth(osText);

		String editLabel = PDEPlugin.getResourceString(SECTION_EDIT);
		osButton = factory.createButton(container, editLabel, SWT.PUSH);
		osButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator.showWhile(osText.getControl().getDisplay(), new Runnable() {
					public void run() {
						Choice[] choices = ReferencePropertySource.getOSChoices();
						openPortabilityChoiceDialog(osText, choices);
					}
				});
			}
		});

		wsText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_WS), factory));
		wsText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setWS(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		limitTextWidth(wsText);
		wsButton = factory.createButton(container, editLabel, SWT.PUSH);
		wsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator.showWhile(wsText.getControl().getDisplay(), new Runnable() {
					public void run() {
						Choice[] choices = ReferencePropertySource.getWSChoices();
						openPortabilityChoiceDialog(wsText, choices);
					}
				});
			}
		});
		nlText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_NL), factory));
		nlText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setNL(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		limitTextWidth(nlText);
		nlButton = factory.createButton(container, editLabel, SWT.PUSH);
		nlButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator.showWhile(nlText.getControl().getDisplay(), new Runnable() {
					public void run() {
						Choice[] choices = ReferencePropertySource.getNLChoices();
						openPortabilityChoiceDialog(nlText, choices);
					}
				});
			}
		});

		archText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_ARCH), factory));
		archText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setArch(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		limitTextWidth(archText);
		archButton = factory.createButton(container, editLabel, SWT.PUSH);
		archButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator.showWhile(nlText.getControl().getDisplay(), new Runnable() {
					public void run() {
						Choice[] choices = ReferencePropertySource.getArchChoices();
						openPortabilityChoiceDialog(archText, choices);
					}
				});
			}
		});

		factory.paintBordersFor(container);
		return container;
	}
	
	private void limitTextWidth(FormEntry entry) {
		GridData gd = (GridData) entry.getControl().getLayoutData();
		gd.widthHint = 150;
	}

	private void openPortabilityChoiceDialog(FormEntry text, Choice[] choices) {
		String value = text.getValue();

		PortabilityChoicesDialog dialog =
			new PortabilityChoicesDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				choices,
				value);
		dialog.create();
		dialog.getShell().setText(PDEPlugin.getResourceString(KEY_DIALOG_TITLE));
		//dialog.getShell().setSize(300, 400);
		int result = dialog.open();
		if (result == PortabilityChoicesDialog.OK) {
			value = dialog.getValue();
			text.setValue(value);
		}
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

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		update(input);
		if (model.isEditable() == false) {
			osText.getControl().setEditable(false);
			wsText.getControl().setEditable(false);
			nlText.getControl().setEditable(false);
			archText.getControl().setEditable(false);
			osButton.setEnabled(false);
			wsButton.setEnabled(false);
			nlButton.setEnabled(false);
			archButton.setEnabled(false);
		}
		model.addModelChangedListener(this);
	}
	public boolean isDirty() {
		return osText.isDirty()
			|| wsText.isDirty()
			|| nlText.isDirty()
			|| archText.isDirty();
	}
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
		}
	}
	public void setFocus() {
		if (osText != null)
			osText.getControl().setFocus();
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
		setIfDefined(osText, feature.getOS());
		setIfDefined(wsText, feature.getWS());
		setIfDefined(nlText, feature.getNL());
		setIfDefined(archText, feature.getArch());
		updateNeeded = false;
	}
}