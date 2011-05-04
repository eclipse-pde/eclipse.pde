/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028, 249263
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 254971
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.sections;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SWTUtil;
import org.eclipse.pde.internal.ds.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ds.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ds.ui.parts.FormEntry;
import org.eclipse.pde.internal.ds.ui.wizards.DSNewClassCreationWizard;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.util.PDEJavaHelperUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;

public class DSComponentSection extends PDESection {

	private IDSComponent fComponent;
	private IDSImplementation fImplementation;
	private FormEntry fClassEntry;
	private FormEntry fNameEntry;
	private FormEntry fActivateEntry;
	private FormEntry fDeactivateEntry;
	private FormEntry fModifiedEntry;
	private IDSModel fModel;

	public DSComponentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {

		initializeAttributes();

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		section.setLayoutData(data);
		section.setText(Messages.DSSection_title);
		section.setDescription(Messages.DSSection_description);

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Attribute: name
		fNameEntry = new FormEntry(client, toolkit,
				Messages.DSComponentDetails_nameEntry, SWT.NONE);

		// Attribute: class
		fClassEntry = new FormEntry(client, toolkit,
				Messages.DSImplementationDetails_classEntry,
				Messages.DSImplementationDetails_browse, isEditable(), 0);

		// Attribute: activate
		fActivateEntry = new FormEntry(client, toolkit,
				Messages.DSComponentDetails_activateEntry, SWT.NONE);
		fActivateEntry.getLabel().setToolTipText(
				Messages.DSComponentDetails_activateTooltip);

		// Attribute: deactivate
		fDeactivateEntry = new FormEntry(client, toolkit,
				Messages.DSComponentDetails_deactivateEntry, SWT.NONE);
		fDeactivateEntry.getLabel().setToolTipText(
				Messages.DSComponentDetails_deactivateTooltip);
		
		// Attribute: modified
		fModifiedEntry = new FormEntry(client, toolkit,
				Messages.DSComponentDetails_modifiedEntry, SWT.NONE);
		fModifiedEntry.getLabel().setToolTipText(
				Messages.DSComponentDetails_modifiedTooltip);

		setListeners();
		updateUIFields();

		toolkit.paintBordersFor(client);
		section.setClient(client);

	}

	private void initializeAttributes() {
		fModel = (IDSModel) getPage().getModel();
		fModel.addModelChangedListener(this);

		fComponent = fModel.getDSComponent();
		if (fComponent != null) {
			fImplementation = fComponent.getImplementation();
		}
	}

	public void commit(boolean onSave) {
		fClassEntry.commit();
		fNameEntry.commit();
		fActivateEntry.commit();
		fDeactivateEntry.commit();
		fModifiedEntry.commit();
		super.commit(onSave);
	}

	public void modelChanged(IModelChangedEvent e) {
		fComponent = fModel.getDSComponent();
		if (fComponent != null)
			fImplementation = fComponent.getImplementation();

		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}

		if (fNameEntry != null) {
			Display display= fNameEntry.getText().getDisplay();
			if (display.getThread() == Thread.currentThread())
				updateUIFields();
			else
				display.asyncExec(new Runnable() {
					public void run() {
						if (!fNameEntry.getText().isDisposed())
							updateUIFields();
					}
				});
		}
	}

	public void updateUIFields() {

		if (fComponent != null) {
			if (fComponent.getAttributeName() == null) {
				// Attribute: name
				fNameEntry.setValue("", true); //$NON-NLS-1$
			} else {
				// Attribute: name
				fNameEntry.setValue(fComponent.getAttributeName(), true);
			}
			fNameEntry.setEditable(isEditable());

			if (fComponent.getActivateMethod() == null) {
				fActivateEntry.setValue("", true); //$NON-NLS-1$
			} else {
				fActivateEntry.setValue(fComponent.getActivateMethod(), true);
			}

			fActivateEntry.setEditable(isEditable());

			if (fComponent.getDeactivateMethod() == null) {
				fDeactivateEntry.setValue("", true); //$NON-NLS-1$
			} else {
				fDeactivateEntry.setValue(fComponent.getDeactivateMethod(),
						true);
			}
			fDeactivateEntry.setEditable(isEditable());

			if (fComponent.getModifiedMethod() == null) {
				fModifiedEntry.setValue("", true); //$NON-NLS-1$
			} else {
				fModifiedEntry.setValue(fComponent.getModifiedMethod(), true);
			}
			fModifiedEntry.setEditable(isEditable());
		}

		// Ensure data object is defined
		if (fImplementation != null) {
			if (fImplementation.getClassName() == null) {
				fClassEntry.setValue("", true); //$NON-NLS-1$
			} else {
				// Attribute: title
				fClassEntry.setValue(fImplementation.getClassName(), true);

			}
			fClassEntry.setEditable(isEditable());
		}

	}

	public void setListeners() {
		// Attribute: name
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setAttributeName(fNameEntry.getValue());
			}
		});
		fActivateEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setActivateMethod(fActivateEntry.getValue());
			}
		});
		fDeactivateEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setDeactivateMethod(fDeactivateEntry.getValue());
			}
		});
		fModifiedEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setModifiedeMethod(fModifiedEntry.getValue());
			}
		});

		IActionBars actionBars = this.getPage().getEditor().getEditorSite()
				.getActionBars();
		// Attribute: class
		fClassEntry
				.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
					public void textValueChanged(FormEntry entry) {
						if (fImplementation == null) {
							if (fComponent != null) {
								fImplementation = fComponent.getModel()
										.getFactory().createImplementation();
								fImplementation.setClassName(fClassEntry
										.getValue());
								fComponent.addChildNode(fImplementation, 0,
										true);
							}
						} else {
							fImplementation
									.setClassName(fClassEntry.getValue());
						}
					}

					public void linkActivated(HyperlinkEvent e) {
						String value = fClassEntry.getValue();
						value = handleLinkActivated(value, false);
						if (value != null)
							fClassEntry.setValue(value);
					}

					public void browseButtonSelected(FormEntry entry) {
						doOpenSelectionDialog(
								fClassEntry);
					}

				});
	}

	private String handleLinkActivated(String value, boolean isInter) {
		IProject project = getProject();
		try {
			if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement element = javaProject.findType(value.replace('$',
						'.'));
				if (element != null)
					JavaUI.openInEditor(element);
				else {
					// TODO create our own wizard for reuse here
					DSNewClassCreationWizard wizard = new DSNewClassCreationWizard(
							project, isInter, value);
					WizardDialog dialog = new WizardDialog(Activator
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					if (dialog.open() == Window.OK) {
						return wizard.getQualifiedName();
					}
				}
			}
		} catch (PartInitException e1) {
		} catch (CoreException e1) {
		}
		return null;
	}

	private void doOpenSelectionDialog(FormEntry entry) {
		String filter = entry.getValue();
		if (filter.length() == 0)
			filter = "**"; //$NON-NLS-1$
		else
			filter = filter.substring(filter.lastIndexOf(".") + 1); //$NON-NLS-1$
		String type = PDEJavaHelperUI.selectType(
				fModel.getUnderlyingResource(),
				IJavaElementSearchConstants.CONSIDER_CLASSES, filter, null);
		if (type != null) {
			entry.setValue(type);
			entry.commit();
		}
	}

}
