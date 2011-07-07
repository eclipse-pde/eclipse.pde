/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
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
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;

public class ExtensionElementBodyTextDetails extends AbstractPluginElementDetails implements IControlHoverContentProvider {

	private IPluginElement fPluginElement;

	private ISchemaElement fSchemaElement;

	private FormEntry fTextBody;

	private Section fSectionElementDetails;

	private FormToolkit fToolkit;

	private Hyperlink fHyperlinkBody;

	private IInformationControl fInfoControlHover;

	public ExtensionElementBodyTextDetails(PDESection masterSection) {
		super(masterSection);
		fPluginElement = null;
		fSchemaElement = null;
		fTextBody = null;
		fSectionElementDetails = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		// Get the toolkit
		createUIToolkit();
		// Configure the parents layout
		configureParentLayout(parent);
		// Create the UI
		createUI(parent);
		// Create the listeners
		createListeners();
	}

	private void createListeners() {
		// Create the listeners for the body text field
		createListenersTextBody();
		// Create the listeners for the body text hyperlink
		createListenersHyperlinkBody();
		// Create the model listeners
		createListenersModel();
	}

	private void createListenersHyperlinkBody() {
		// Listen to hyperlink clicks
		fHyperlinkBody.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				handleHyperlinkBodyLinkActivated();
			}
		});
		// Listen to mouse hovers
		PDETextHover.addHoverListenerToControl(fInfoControlHover, fHyperlinkBody, this);
	}

	private void handleHyperlinkBodyLinkActivated() {
		boolean opened = false;
		// Open the reference if this is not a reference model
		if (isReferenceModel() == false) {
			opened = openReference();
		}
		// If the reference was not opened, notify the user with a beep
		if (opened == false) {
			Display.getCurrent().beep();
		}
	}

	private boolean openReference() {
		// Ensure a plugin element was specified
		if (fPluginElement == null) {
			return false;
		}
		// Create the link
		TranslationHyperlink link = new TranslationHyperlink(null, fTextBody.getValue(), fPluginElement.getModel());
		// Open the link
		link.open();

		return link.getOpened();
	}

	private void createListenersModel() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.addModelChangedListener(this);
	}

	private void createListenersTextBody() {
		// Listen for text input
		fTextBody.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				handleTextBodyValueChanged();
			}
		});
		// Listen to mouse hovers
		PDETextHover.addHoverListenerToControl(fInfoControlHover, fTextBody.getText(), this);
	}

	private void handleTextBodyValueChanged() {
		// Plugin element data not defined, nothing to update
		if (fPluginElement == null) {
			return;
		}
		// Update the body text field with the new value from plugin element
		// data
		try {
			fPluginElement.setText(fTextBody.getValue());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void configureParentLayout(Composite parent) {
		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));
	}

	private void createUIToolkit() {
		fToolkit = getManagedForm().getToolkit();
	}

	private void createUI(Composite parent) {
		// Create the element details section
		createUISectionElementDetails(parent);
		// Create the client container for the section
		Composite client = createUISectionContainer(fSectionElementDetails);
		// Create the info hover control for the body text field and hyperlink
		createUIInfoHoverControl(client);
		// Create the body text label
		createUIHyperlinkBody(client);
		// Create the body text field 
		createUITextBody(client);
		// Associate the client with the section
		fToolkit.paintBordersFor(client);
		fSectionElementDetails.setClient(client);
		// Needed for keyboard paste operation to work
		markDetailsPart(fSectionElementDetails);
	}

	private void createUIInfoHoverControl(Composite client) {
		// Shared between the body text field and body text hyperlink / label
		fInfoControlHover = PDETextHover.getInformationControlCreator().createInformationControl(client.getShell());
		fInfoControlHover.setSizeConstraints(300, 600);
	}

	private void createUIHyperlinkBody(Composite client) {
		fHyperlinkBody = fToolkit.createHyperlink(client, PDEUIMessages.ExtensionElementBodyTextDetails_labelBodyText, SWT.NULL);
	}

	private boolean isReferenceModel() {
		// If the model has no underlying resource, then it is a reference
		// model
		if ((fPluginElement == null) || (fPluginElement.getModel().getUnderlyingResource() == null)) {
			return true;
		}
		return false;
	}

	private Composite createUISectionContainer(Section section) {
		Composite client = fToolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		return client;
	}

	private void createUISectionElementDetails(Composite parent) {
		int section_style = Section.DESCRIPTION | ExpandableComposite.TITLE_BAR;
		fSectionElementDetails = fToolkit.createSection(parent, section_style);
		fSectionElementDetails.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fSectionElementDetails.setText(PDEUIMessages.ExtensionElementDetails_title);
		fSectionElementDetails.setDescription(PDEUIMessages.ExtensionElementBodyTextDetails_sectionDescElementGeneral);
		fSectionElementDetails.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		int layout_style = GridData.FILL_HORIZONTAL;
		GridData data = new GridData(layout_style);
		fSectionElementDetails.setLayoutData(data);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), fSectionElementDetails);
	}

	private void createUITextBody(Composite parent) {
		int widget_style = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL;
		fTextBody = new FormEntry(parent, fToolkit, null, widget_style);
		int layout_text_style = GridData.FILL_HORIZONTAL;
		GridData data = new GridData(layout_text_style);
		data.heightHint = 90;
		fTextBody.getText().setLayoutData(data);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IPartSelectionListener#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the structured selection
		IStructuredSelection structured_selection = (IStructuredSelection) selection;
		// The selection from the master tree viewer is our plugin element data
		if (structured_selection.size() == 1) {
			fPluginElement = (IPluginElement) structured_selection.getFirstElement();
		} else {
			fPluginElement = null;
		}
		// Update the UI given the new plugin element data
		updateUI();
	}

	private void updateUI() {
		// Update the section description
		updateUISectionElementDetails();
		// Update the body text field
		updateUITextBody();
	}

	private void updateUISectionElementDetails() {
		// Set the general or specifc section description depending if whether
		// the plugin element data is defined
		if (fPluginElement == null) {
			fSectionElementDetails.setDescription(PDEUIMessages.ExtensionElementBodyTextDetails_sectionDescElementGeneral);
		} else {
			fSectionElementDetails.setDescription(NLS.bind(PDEUIMessages.ExtensionElementBodyTextDetails_sectionDescElementSpecific, fPluginElement.getName()));
		}
		// Re-layout the section to properly wrap the new section description
		fSectionElementDetails.layout();
	}

	private void updateUITextBody() {
		// Set the new body text value from the new plugin element data if 
		// defined
		if (fPluginElement == null) {
			fTextBody.setEditable(false);
			fTextBody.setValue(null, true);
		} else {
			fTextBody.setEditable(isEditable());
			fTextBody.setValue(fPluginElement.getText(), true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#fireSaveNeeded()
	 */
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#getContextId()
	 */
	public String getContextId() {
		return PluginInputContext.CONTEXT_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#getPage()
	 */
	public PDEFormPage getPage() {
		return (PDEFormPage) getManagedForm().getContainer();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#isEditable()
	 */
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		// Refresh the UI if the plugin element data changed
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object object = event.getChangedObjects()[0];
			if (object.equals(fPluginElement)) {
				refresh();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		updateUI();
		super.refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEDetails#cancelEdit()
	 */
	public void cancelEdit() {
		fTextBody.cancelEdit();
		super.cancelEdit();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fTextBody.commit();
		super.commit(onSave);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		// Remove the model listener
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#setFocus()
	 */
	public void setFocus() {
		fTextBody.getText().setFocus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.text.TextHoverDescriptionProvider#getDescription(org.eclipse.swt.widgets.Control)
	 */
	public String getHoverContent(Control control) {
		// Retrieve either the hyperlink, label or text description as the 
		// hover content
		if ((control instanceof Hyperlink) || (control instanceof Label)) {
			return getHyperlinkDescription();
		} else if (control instanceof Text) {
			return getTextDescription((Text) control);
		}

		return null;
	}

	private String getHyperlinkDescription() {
		// Ensure there is an associated schema
		if (fSchemaElement == null) {
			return null;
		}
		// Return the associated element description
		return fSchemaElement.getDescription();
	}

	private String getTextDescription(Text text) {
		// Ensure there is an associated schema
		if (fSchemaElement == null) {
			return null;
		}
		String bodyText = text.getText();
		String translatedBodyText = null;
		// If the text represents a translated string key, retrieve its 
		// associated value
		if ((bodyText.startsWith("%")) && //$NON-NLS-1$
				fSchemaElement.hasTranslatableContent()) {
			translatedBodyText = fPluginElement.getResourceString(bodyText);
			// If the value does not equal the key, a value was found
			if (bodyText.equals(translatedBodyText) == false) {
				return translatedBodyText;
			}
		}

		return null;
	}

	public void setSchemaElement(ISchemaElement schemaElement) {
		fSchemaElement = schemaElement;
	}

}
