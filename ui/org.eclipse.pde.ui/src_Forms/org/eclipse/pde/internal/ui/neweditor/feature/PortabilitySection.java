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
package org.eclipse.pde.internal.ui.neweditor.feature;

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.Choice;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.feature.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;

public class PortabilitySection extends PDESection implements IPartSelectionListener {
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
	private FormEntry wsText;
	private FormEntry nlText;
	private FormEntry archText;
	private boolean reactToSelections;
	private IStructuredSelection currentInput;

	public PortabilitySection(FeatureFormPage page, Composite parent) {
		this(page, parent,
			PDEPlugin.getResourceString(SECTION_TITLE),
			PDEPlugin.getResourceString(SECTION_DESC),
			Section.TWISTIE);
	}

	public PortabilitySection(
		PDEFormPage page,
		Composite parent,
		String title,
		String desc,
		int toggleStyle) {
		super(page, parent, Section.DESCRIPTION|toggleStyle);
		this.reactToSelections = toggleStyle==SWT.NULL;
		getSection().setText(title);
		getSection().setDescription(desc);
		if (!reactToSelections) {
			IFeatureModel model = (IFeatureModel) page.getModel();
			IFeature feature = model.getFeature();
			getSection().setExpanded(
				feature.getOS() != null
					|| feature.getWS() != null
					|| feature.getNL() != null
					|| feature.getArch() != null);
		}
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

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
	public void commit(boolean onSave) {
		osText.commit();
		wsText.commit();
		if (nlText != null)
			nlText.commit();
		archText.commit();
		super.commit(onSave);
	}

	public void createClient(
		Section section,
		FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);
		
		String editLabel = PDEPlugin.getResourceString(SECTION_EDIT);

		osText =
			new FormEntry(container,
					toolkit,
					PDEPlugin.getResourceString(SECTION_OS),
					editLabel, 
					false);
		osText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_OS, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator
				.showWhile(
					osText.getText().getDisplay(),
					new Runnable() {
				public void run() {
					Choice[] choices =
						ReferencePropertySource.getOSChoices();
					openPortabilityChoiceDialog(osText, choices);
				}
			});
			}
		});
		limitTextWidth(osText);

		wsText =
			new FormEntry(container, toolkit,
					PDEPlugin.getResourceString(SECTION_WS),
					editLabel, 
					false);
		wsText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_WS, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator
				.showWhile(
					wsText.getText().getDisplay(),
					new Runnable() {
						public void run() {
							Choice[] choices =
								ReferencePropertySource.getWSChoices();
							openPortabilityChoiceDialog(wsText, choices);
						}
					});
			}
		});
		limitTextWidth(wsText);

		if (!reactToSelections) {
			nlText =
				new FormEntry(container,
						toolkit,
						PDEPlugin.getResourceString(SECTION_NL),
						editLabel,
						false);

			nlText.setFormEntryListener(new FormEntryAdapter(this) {
				public void textValueChanged(FormEntry text) {
					try {
						applyValue(IFeature.P_NL, text.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
				public void browseButtonSelected(FormEntry entry) {
					BusyIndicator
					.showWhile(
						nlText.getText().getDisplay(),
						new Runnable() {
							public void run() {
								Choice[] choices =
									ReferencePropertySource.getNLChoices();
								openPortabilityChoiceDialog(nlText, choices);
							}
						});
				}
			});
			limitTextWidth(nlText);
		}

		archText =
			new FormEntry(container,
					toolkit,
					PDEPlugin.getResourceString(SECTION_ARCH),
					editLabel,
					false);
		archText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_ARCH, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator
				.showWhile(
					archText.getText().getDisplay(),
					new Runnable() {
						public void run() {
							Choice[] choices =
								ReferencePropertySource.getArchChoices();
							openPortabilityChoiceDialog(archText, choices);
						}
				});
			}

		});
		limitTextWidth(archText);
		toolkit.paintBordersFor(container);
		section.setClient(container);
	}

	private void limitTextWidth(FormEntry entry) {
		GridData gd = (GridData) entry.getText().getLayoutData();
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
		IFeatureModel model = (IFeatureModel) getPage().getModel();
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

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		enableForInput(model.isEditable());
		refresh();
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
	}
	public void setFocus() {
		if (osText != null)
			osText.getText().setFocus();
	}

	private void setIfDefined(FormEntry formText, String value) {
		formText.setValue(value, true);
	}

	private void enableForInput(boolean enable) {
		osText.getText().setEditable(enable);
		wsText.getText().setEditable(enable);
		if (nlText != null)
			nlText.getText().setEditable(enable);
		archText.getText().setEditable(enable);
		osText.getButton().setEnabled(enable);
		wsText.getButton().setEnabled(enable);
		if (nlText != null)
			nlText.getButton().setEnabled(enable);
		archText.getButton().setEnabled(enable);
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

	public void refresh() {
		if (reactToSelections && currentInput == null) {
			clearFields();
			enableForInput(false);
			super.refresh();
			return;
		}
		enableForInput(true);
		setValue(IEnvironment.P_OS);
		setValue(IEnvironment.P_WS);
		setValue(IEnvironment.P_ARCH);
		if (nlText != null)
			setValue(IFeature.P_NL);
		super.refresh();
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			currentInput = (IStructuredSelection) selection;
			if (currentInput.isEmpty())
				currentInput = null;
		} else
			currentInput = null;
		refresh();
	}
}