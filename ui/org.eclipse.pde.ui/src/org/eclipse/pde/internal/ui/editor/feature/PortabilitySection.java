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

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.Choice;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
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
import org.eclipse.update.ui.forms.internal.*;
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
	public static final String SECTION_OS =
		"FeatureEditor.PortabilitySection.os";
	public static final String SECTION_WS =
		"FeatureEditor.PortabilitySection.ws";
	public static final String SECTION_NL =
		"FeatureEditor.PortabilitySection.nl";
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
	private boolean reactToSelections;
	private IStructuredSelection currentInput;

	public PortabilitySection(FeatureFormPage page) {
		this(
			page,
			PDEPlugin.getResourceString(SECTION_TITLE),
			PDEPlugin.getResourceString(SECTION_DESC),
			false);
	}

	public PortabilitySection(
		PDEFormPage page,
		String title,
		String desc,
		boolean reactToSelections) {
		super(page);
		this.reactToSelections = reactToSelections;
		setHeaderText(title);
		setDescription(desc);
		setCollapsable(!reactToSelections);
		if (!reactToSelections) {
			IFeatureModel model = (IFeatureModel) page.getModel();
			IFeature feature = model.getFeature();
			setCollapsed(
				feature.getOS() == null
					&& feature.getWS() == null
					&& feature.getNL() == null
					&& feature.getArch() == null);
		}
	}

	public boolean canPaste(Clipboard clipboard) {
		return (clipboard.getContents(TextTransfer.getInstance()) != null);
	}
	public void commitChanges(boolean onSave) {
		osText.commit();
		wsText.commit();
		if (nlText != null)
			nlText.commit();
		archText.commit();
	}

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		osText =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(SECTION_OS),
					factory));
		osText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_OS, text.getValue());
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
				BusyIndicator
					.showWhile(
						osText.getControl().getDisplay(),
						new Runnable() {
					public void run() {
						Choice[] choices =
							ReferencePropertySource.getOSChoices();
						openPortabilityChoiceDialog(osText, choices);
					}
				});
			}
		});

		wsText =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(SECTION_WS),
					factory));
		wsText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_WS, text.getValue());
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
				BusyIndicator
					.showWhile(
						wsText.getControl().getDisplay(),
						new Runnable() {
					public void run() {
						Choice[] choices =
							ReferencePropertySource.getWSChoices();
						openPortabilityChoiceDialog(wsText, choices);
					}
				});
			}
		});

		if (!reactToSelections) {
			nlText =
				new FormEntry(
					createText(
						container,
						PDEPlugin.getResourceString(SECTION_NL),
						factory));

			nlText.addFormTextListener(new IFormTextListener() {
				public void textValueChanged(FormEntry text) {
					try {
						applyValue(IFeature.P_NL, text.getValue());
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
					BusyIndicator
						.showWhile(
							nlText.getControl().getDisplay(),
							new Runnable() {
						public void run() {
							Choice[] choices =
								ReferencePropertySource.getNLChoices();
							openPortabilityChoiceDialog(nlText, choices);
						}
					});
				}
			});
		}

		archText =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(SECTION_ARCH),
					factory));
		archText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_ARCH, text.getValue());
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
				BusyIndicator
					.showWhile(
						archText.getControl().getDisplay(),
						new Runnable() {
					public void run() {
						Choice[] choices =
							ReferencePropertySource.getArchChoices();
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
		if (reactToSelections) gd.widthHint = 30;
		else gd.widthHint = 150;
	}

	private void openPortabilityChoiceDialog(
		FormEntry text,
		Choice[] choices) {
		String value = text.getValue();

		PortabilityChoicesDialog dialog =
			new PortabilityChoicesDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				choices,
				value);
		dialog.create();
		dialog.getShell().setText(
			PDEPlugin.getResourceString(KEY_DIALOG_TITLE));
		//dialog.getShell().setSize(300, 400);
		int result = dialog.open();
		if (result == PortabilityChoicesDialog.OK) {
			value = dialog.getValue();
			text.setValue(value);
		}
	}

	private IFeature getFeature() {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		return model.getFeature();
	}

	private void applyValue(String property, String value)
		throws CoreException {
		if (reactToSelections) {
			if (currentInput == null)
				return;
			for (Iterator iter = currentInput.iterator(); iter.hasNext();) {
				IEnvironment env = (IEnvironment) iter.next();
				applyValue(env, property, value);
			}
		} else {
			applyValue(getFeature(), property, value);
		}
	}

	private void setValue(String property) {
		if (reactToSelections) {
			if (currentInput == null) {
				clearField(property);
			} else if (currentInput.size() == 1) {
				setValue(
					(IEnvironment) currentInput.getFirstElement(),
					property);
			} else {
				IEnvironment leader = null;
				String lvalue = null;
				for (Iterator iter = currentInput.iterator();
					iter.hasNext();
					) {
					IEnvironment next = (IEnvironment) iter.next();
					if (leader == null) {
						String nvalue = getValue(next, property);
						if (nvalue == null)
							break;
						leader = next;
						lvalue = nvalue;
					} else {
						String nvalue = getValue(next, property);
						if (nvalue == null || !lvalue.equals(nvalue)) {
							leader = null;
							break;
						}
					}
				}
				if (leader == null) {
					clearField(property);
				} else
					setValue(leader, property);
			}
		} else {
			setValue(getFeature(), property);
		}
	}

	private String getValue(IEnvironment obj, String property) {
		if (property.equals(IEnvironment.P_OS))
			return obj.getOS();
		if (property.equals(IEnvironment.P_WS))
			return obj.getWS();
		if (property.equals(IEnvironment.P_ARCH))
			return obj.getArch();
		return null;
	}

	private void applyValue(IEnvironment obj, String property, String value)
		throws CoreException {
		if (property.equals(IFeature.P_NL))
			 ((IFeature) obj).setNL(value);
		else if (property.equals(IFeature.P_OS))
			obj.setOS(value);
		else if (property.equals(IFeature.P_WS))
			obj.setWS(value);
		else if (property.equals(IFeature.P_ARCH))
			obj.setArch(value);
	}

	private void setValue(IEnvironment obj, String property) {
		if (property.equals(IFeature.P_NL))
			setIfDefined(nlText, ((IFeature) obj).getNL());
		else if (property.equals(IFeature.P_OS))
			setIfDefined(osText, obj.getOS());
		else if (property.equals(IFeature.P_WS))
			setIfDefined(wsText, obj.getWS());
		else if (property.equals(IFeature.P_ARCH))
			setIfDefined(archText, obj.getArch());
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
		enableForInput(model.isEditable());
		update(input);
		model.addModelChangedListener(this);
	}
	public boolean isDirty() {
		return osText.isDirty()
			|| wsText.isDirty()
			|| (nlText != null && nlText.isDirty())
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
		formText.setValue(value, true);
	}
	private void setIfDefined(Text text, String value) {
		if (value != null)
			text.setText(value);
		else
			text.setText("");
	}
	public void update() {
		if (updateNeeded) {
			this.update(getFormPage().getModel());
		}
	}

	private void enableForInput(boolean enable) {
		osText.getControl().setEditable(enable);
		wsText.getControl().setEditable(enable);
		if (nlText != null)
			nlText.getControl().setEditable(enable);
		archText.getControl().setEditable(enable);
		osButton.setEnabled(enable);
		wsButton.setEnabled(enable);
		if (nlButton != null)
			nlButton.setEnabled(enable);
		archButton.setEnabled(enable);
	}

	private void clearFields() {
		osText.setValue(null, true);
		wsText.setValue(null, true);
		if (nlText != null)
			nlText.setValue(null, true);
		archText.setValue(null, true);
	}
	
	private void clearField(String property) {
		if (property.equals(IEnvironment.P_OS))
			osText.setValue(null, true);
		else if (property.equals(IEnvironment.P_WS))
			wsText.setValue(null, true);
		else if (property.equals(IEnvironment.P_ARCH))
			archText.setValue(null, true);
	}

	public void update(Object input) {
		if (reactToSelections && currentInput == null) {
			clearFields();
			enableForInput(false);
			return;
		}
		enableForInput(true);
		setValue(IEnvironment.P_OS);
		setValue(IEnvironment.P_WS);
		setValue(IEnvironment.P_ARCH);
		if (nlText != null)
			setValue(IFeature.P_NL);
		updateNeeded = false;
	}

	public void sectionChanged(
		FormSection source,
		int changeType,
		Object changeObject) {
		if (changeObject instanceof IStructuredSelection) {
			currentInput = (IStructuredSelection) changeObject;
			if (currentInput.isEmpty())
				currentInput = null;
		} else
			currentInput = null;
		update(null);
	}
}
