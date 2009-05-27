/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.actions.OpenSchemaAction;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.search.FindDeclarationsAction;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;

public class ExtensionDetails extends AbstractPluginElementDetails {
	private IPluginExtension input;
	private FormEntry id;
	private FormEntry name;
	private FormText rtext;

	private static final String RTEXT_DATA = PDEUIMessages.ExtensionDetails_extensionPointLinks;

	/**
	 * @param masterSection
	 */
	public ExtensionDetails(PDESection masterSection) {
		super(masterSection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));

		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
		section.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		section.setText(PDEUIMessages.ExtensionDetails_title);
		section.setDescription(PDEUIMessages.ExtensionDetails_desc);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), section);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createIDEntryField(toolkit, client);

		createNameEntryField(toolkit, client);

		createSpacer(toolkit, client, 2);

		Composite container = toolkit.createComposite(parent, SWT.NONE);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));

		rtext = toolkit.createFormText(container, true);
		rtext.setImage("desc", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_DOC_SECTION_OBJ)); //$NON-NLS-1$
		rtext.setImage("open", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_SCHEMA_OBJ)); //$NON-NLS-1$
		rtext.setImage("search", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PSEARCH_OBJ)); //$NON-NLS-1$
		rtext.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("search")) { //$NON-NLS-1$
					FindDeclarationsAction findDeclarationsAction = new FindDeclarationsAction(input);
					findDeclarationsAction.run();
				} else if (e.getHref().equals("open")) { //$NON-NLS-1$
					OpenSchemaAction action = new OpenSchemaAction();
					action.setInput(input);
					action.setEnabled(true);
					action.run();
				} else {
					if (input == null || input.getPoint() == null)
						return;
					IPluginExtensionPoint point = PDECore.getDefault().getExtensionsRegistry().findExtensionPoint(input.getPoint());
					if (point != null) {
						ShowDescriptionAction showDescAction = new ShowDescriptionAction(point);
						showDescAction.run();
					} else {
						showNoExtensionPointMessage();
					}
				}
			}
		});
		rtext.setText(RTEXT_DATA, true, false);
		id.setEditable(isEditable());
		name.setEditable(isEditable());

		toolkit.paintBordersFor(client);
		section.setClient(client);
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.addModelChangedListener(this);
		markDetailsPart(section);
	}

	/**
	 * @param toolkit
	 * @param client
	 */
	private void createNameEntryField(FormToolkit toolkit, Composite client) {
		name = new FormEntry(client, toolkit, PDEUIMessages.ExtensionDetails_name, null, false);
		name.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (input != null)
					try {
						input.setName(name.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
	}

	/**
	 * @param toolkit
	 * @param client
	 */
	private void createIDEntryField(FormToolkit toolkit, Composite client) {
		id = new FormEntry(client, toolkit, PDEUIMessages.ExtensionDetails_id, null, false);
		id.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (input != null)
					try {
						input.setId(id.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			input = (IPluginExtension) ssel.getFirstElement();
		} else
			input = null;
		update();
	}

	private void update() {
		id.setValue(input != null ? input.getId() : null, true);
		name.setValue(input != null ? input.getName() : null, true);

		// Update the ID label
		updateLabel(isFieldRequired(IIdentifiable.P_ID), id, PDEUIMessages.ExtensionDetails_id);
		// Update the Name label
		updateLabel(isFieldRequired(IPluginObject.P_NAME), name, PDEUIMessages.ExtensionDetails_name);
	}

	/**
	 * Denote a field as required by updating their label
	 * @param attributeName
	 * @param field
	 */
	private boolean isFieldRequired(String attributeName) {
		// Ensure we have input
		if (input == null) {
			return false;
		}
		// Get the associated schema
		Object object = input.getSchema();
		// Ensure we have a schema
		if ((object == null) || (object instanceof Schema) == false) {
			return false;
		}
		Schema schema = (Schema) object;
		// Find the extension element
		ISchemaElement element = schema.findElement(ICoreConstants.EXTENSION_NAME);
		// Ensure we found the element
		if (element == null) {
			return false;
		}
		// Get the attribute
		ISchemaAttribute attribute = element.getAttribute(attributeName);
		// Ensure we found the attribute
		if (attribute == null) {
			return false;
		}
		// Determine whether the attribute is required
		if (attribute.getUse() == ISchemaAttribute.REQUIRED) {
			return true;
		}
		return false;
	}

	/**
	 * @param field
	 * @param required
	 */
	private void updateLabel(boolean required, FormEntry field, String label) {
		// Get the label
		Control control = field.getLabel();
		// Ensure label is defined
		if ((control == null) || ((control instanceof Label) == false)) {
			return;
		}
		Label labelControl = ((Label) control);
		// If the label is required, add the '*' to indicate that
		if (required) {
			labelControl.setText(label + '*' + ':');
		} else {
			labelControl.setText(label + ':');
		}
		// Force the label's parent composite to relayout because 
		// clippage can occur when updating the text
		labelControl.getParent().layout();
	}

	public void cancelEdit() {
		id.cancelEdit();
		name.cancelEdit();
		super.cancelEdit();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit(boolean onSave) {
		id.commit();
		name.commit();
		super.commit(onSave);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		id.getText().setFocus();
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj.equals(input))
				refresh();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		update();
		super.refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.IContextPart#fireSaveNeeded()
	 */
	public void fireSaveNeeded() {
		markDirty();
		PDEFormPage page = (PDEFormPage) getManagedForm().getContainer();
		page.getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.IContextPart#getContextId()
	 */
	public String getContextId() {
		return PluginInputContext.CONTEXT_ID;
	}

	public PDEFormPage getPage() {
		return (PDEFormPage) getManagedForm().getContainer();
	}

	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}

	private void showNoExtensionPointMessage() {
		String title = PDEUIMessages.ExtensionDetails_noPoint_title;
		String message = NLS.bind(PDEUIMessages.ShowDescriptionAction_noPoint_desc, input.getPoint());

		MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), title, message);
	}
}
