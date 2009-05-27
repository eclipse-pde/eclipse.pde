/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.PDESourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.*;

public abstract class AbstractSchemaDetails extends PDEDetails {

	protected static final String[] BOOLS = new String[] {Boolean.toString(true), Boolean.toString(false)};

	protected int minLabelWeight;

	private Section fSection;
	private SchemaDtdDetailsSection fDtdSection = null;
	private ElementSection fElementSection;
	private boolean fShowDTD;
	private boolean fShowDescription;
	private Spinner fMinOccurSpinner;
	private Spinner fMaxOccurSpinner;
	private Button fUnboundSelect;
	private Label fMinLabel;
	private Label fMaxLabel;
	private PDESourceViewer fDescriptionViewer = null;
	private boolean fBlockListeners = false;
	private ISchemaObject fSchemaObject;

	public AbstractSchemaDetails(ElementSection section, boolean showDTD, boolean showDescription) {
		fElementSection = section;
		fShowDTD = showDTD;
		fShowDescription = showDescription;
	}

	public void modelChanged(IModelChangedEvent event) {
		if ((event.getChangeType() == IModelChangedEvent.REMOVE) || (fShowDTD == false) || (fDtdSection == null)) {
			return;
		}
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof ISchemaCompositor)
				fDtdSection.updateDTDLabel(objects[i]);
		}
	}

	public final void createContents(Composite parent) {
		// This is a hacked fix to ensure that the label columns on every details
		// page have the same width. SchemaDetails_translatable plus 11 pixels
		// represents the longest label on any field on any details page. This
		// occurs on SchemaStringAttributeDetails and 11 is the size of the
		// horizontal indent that contributes to the label's width.
		GC gc = new GC(parent);
		minLabelWeight = gc.textExtent(PDEUIMessages.SchemaDetails_translatable).x + 11;
		gc.dispose();
		gc = null;

		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));
		FormToolkit toolkit = getManagedForm().getToolkit();
		fSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));

		GridData gd;
		if (fShowDescription)
			gd = new GridData(GridData.FILL_BOTH);
		else
			gd = new GridData(GridData.FILL_HORIZONTAL);
		fSection.setLayoutData(gd);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(fElementSection.getSection(), fSection);

		Composite client = toolkit.createComposite(fSection);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));

		createDetails(client);

		if (fShowDescription)
			createDescription(client, toolkit);

		// If the DTD Approximation section was requested, instantiate it and create it's contents
		// on the same parent Composite
		if (fShowDTD) {
			fDtdSection = new SchemaDtdDetailsSection();
			fDtdSection.initialize(getManagedForm());
			fDtdSection.createContents(parent);
		}

		toolkit.paintBordersFor(client);
		fSection.setClient(client);
		markDetailsPart(fSection);

		if (fShowDescription)
			fDescriptionViewer.createUIListeners();
		hookListeners();
	}

	private void createDescription(Composite container, FormToolkit toolkit) {

		Label label = toolkit.createLabel(container, PDEUIMessages.AbstractSchemaDetails_descriptionLabel);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fDescriptionViewer = new PDESourceViewer(getPage());
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 75;
		gd.widthHint = 60;
		gd.horizontalSpan = 3;
		gd.horizontalIndent = 1;
		/* 
		 * Needed to align vertically with form entry field and allow space
		 * for a possible field decoration
		 * commented out for now since fields are already grossly misaligned (see bug 196879)
		 * commenting out temporarily makes the alignment better on Element details and Attribute details
		 * but worse on RootElement details
		 */
		//gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		fDescriptionViewer.createUI(container, gd);
		fDescriptionViewer.getDocument().addDocumentListener(new IDocumentListener() {
			public void documentChanged(DocumentEvent event) {
				if (blockListeners())
					return;
				if (fSchemaObject != null) {
					// Get the text from the event
					IDocument document = event.getDocument();
					if (document == null) {
						return;
					}
					// Get the text from the event
					String text = document.get().trim();
					updateObjectDescription(text);
				}
			}

			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		});
	}

	public abstract void createDetails(Composite parent);

	public abstract void updateFields(ISchemaObject obj);

	public abstract void hookListeners();

	public boolean isEditableElement() {
		return fElementSection.isEditable();
	}

	protected void setDecription(String desc) {
		fSection.setDescription(desc);
	}

	protected void setText(String title) {
		fSection.setText(title);
	}

	public String getContextId() {
		return SchemaInputContext.CONTEXT_ID;
	}

	public PDEFormPage getPage() {
		return (PDEFormPage) getManagedForm().getContainer();
	}

	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}

	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	public boolean canPaste(Clipboard clipboard) {
		if (fShowDescription && fDescriptionViewer != null && fDescriptionViewer.getViewer().getTextWidget().isFocusControl())
			return fDescriptionViewer.canPaste();
		return super.canPaste(clipboard);
	}

	public boolean doGlobalAction(String actionId) {
		if (fShowDescription && fDescriptionViewer != null && fDescriptionViewer.getViewer().getTextWidget().isFocusControl())
			return fDescriptionViewer.doGlobalAction(actionId);
		return super.doGlobalAction(actionId);
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (!(part instanceof ElementSection))
			return;
		Object obj = ((IStructuredSelection) selection).getFirstElement();
		if (obj instanceof ISchemaObject) {
			setBlockListeners(true);
			ISchemaObject sObj = (ISchemaObject) obj;
			fSchemaObject = sObj;
			if (fShowDTD && fDtdSection != null)
				fDtdSection.updateDTDLabel(obj);
			if (fShowDescription && fDescriptionViewer != null)
				updateDescriptionViewer(sObj);
			updateFields(sObj);
			setBlockListeners(false);
		}
	}

	public void updateDescriptionViewer(ISchemaObject obj) {
		if (obj != null) {
			String text = obj.getDescription();
			fDescriptionViewer.getDocument().set(text == null ? "" : text); //$NON-NLS-1$
			fDescriptionViewer.getViewer().setEditable(obj.getSchema().isEditable());
		}
	}

	private void updateObjectDescription(String text) {
		if (fSchemaObject instanceof SchemaObject) {
			((SchemaObject) fSchemaObject).setDescription(text);
		}
	}

	protected void fireSelectionChange() {
		fElementSection.fireSelection(fElementSection.getTreeViewer().getSelection());
	}

	protected void fireMasterSelection(ISelection selection) {
		fElementSection.fireSelection(selection);
	}

	protected ComboPart createComboPart(Composite parent, FormToolkit toolkit, String[] items, int colspan, int style) {
		ComboPart cp = new ComboPart();
		cp.createControl(parent, toolkit, SWT.READ_ONLY);
		GridData gd = new GridData(style);
		gd.horizontalSpan = colspan;
		gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		cp.getControl().setLayoutData(gd);
		cp.setItems(items);
		cp.getControl().setEnabled(isEditable());
		return cp;
	}

	protected ComboPart createComboPart(Composite parent, FormToolkit toolkit, String[] items, int colspan) {
		return createComboPart(parent, toolkit, items, colspan, GridData.FILL_HORIZONTAL);
	}

	protected Button[] createTrueFalseButtons(Composite parent, FormToolkit toolkit, int colSpan) {
		Composite comp = toolkit.createComposite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = gl.marginWidth = 0;
		comp.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		comp.setLayoutData(gd);
		Button tButton = toolkit.createButton(comp, BOOLS[0], SWT.RADIO);
		Button fButton = toolkit.createButton(comp, BOOLS[1], SWT.RADIO);
		gd = new GridData();
		gd.horizontalIndent = 20;
		fButton.setLayoutData(gd);
		return new Button[] {tButton, fButton};
	}

	protected Composite createMinOccurComp(Composite parent, FormToolkit toolkit) {
		fMinLabel = toolkit.createLabel(parent, PDEUIMessages.AbstractSchemaDetails_minOccurLabel);
		fMinLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		Composite comp = toolkit.createComposite(parent);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(gd);
		fMinOccurSpinner = new Spinner(comp, SWT.BORDER);
		fMinOccurSpinner.setMinimum(0);
		fMinOccurSpinner.setMaximum(999);
		return comp;
	}

	protected Composite createMaxOccurComp(Composite parent, FormToolkit toolkit) {
		fMaxLabel = toolkit.createLabel(parent, PDEUIMessages.AbstractSchemaDetails_maxOccurLabel);
		fMaxLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		Composite comp = toolkit.createComposite(parent);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(gd);

		fMaxOccurSpinner = new Spinner(comp, SWT.BORDER);
		fMaxOccurSpinner.setMinimum(1);
		fMaxOccurSpinner.setMaximum(999);
		fMaxOccurSpinner.setIncrement(1);

		fUnboundSelect = toolkit.createButton(comp, PDEUIMessages.AbstractSchemaDetails_unboundedButton, SWT.CHECK);
		gd = new GridData();
		gd.horizontalIndent = 10;
		fUnboundSelect.setLayoutData(gd);
		fUnboundSelect.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				fMaxOccurSpinner.setEnabled(!fUnboundSelect.getSelection() && isEditableElement());
			}
		});

		return comp;
	}

	protected int getMinOccur() {
		if (fMinOccurSpinner != null)
			return fMinOccurSpinner.getSelection();
		return 0;
	}

	protected int getMaxOccur() {
		if (fMaxOccurSpinner != null) {
			if (fMaxOccurSpinner.isEnabled())
				return fMaxOccurSpinner.getSelection();
			return Integer.MAX_VALUE;
		}
		return 1;
	}

	protected void updateMinOccur(int min) {
		if (fMinOccurSpinner != null)
			fMinOccurSpinner.setSelection(min);
	}

	protected void updateMaxOccur(int max) {
		if (fMaxOccurSpinner == null)
			return;
		boolean isMax = max == Integer.MAX_VALUE;
		fUnboundSelect.setSelection(isMax);
		fMaxOccurSpinner.setEnabled(!isMax);
		if (!isMax)
			fMaxOccurSpinner.setSelection(max);
	}

	protected void hookMinOccur(SelectionAdapter adapter) {
		fMinOccurSpinner.addSelectionListener(adapter);
		fMinOccurSpinner.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (blockListeners())
					return;
				int minOccur = fMinOccurSpinner.getSelection();
				if (minOccur > getMaxOccur())
					fMinOccurSpinner.setSelection(minOccur - 1);
			}
		});
	}

	protected void hookMaxOccur(SelectionAdapter adapter) {
		fUnboundSelect.addSelectionListener(adapter);
		fMaxOccurSpinner.addSelectionListener(adapter);
		fMaxOccurSpinner.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (blockListeners())
					return;
				int maxValue = fMaxOccurSpinner.getSelection();
				if (maxValue < getMinOccur())
					fMaxOccurSpinner.setSelection(maxValue + 1);
			}
		});
	}

	protected void enableMinMax(boolean enable) {
		fMinOccurSpinner.setEnabled(enable);
		fMaxOccurSpinner.setEnabled(!fUnboundSelect.getSelection() && enable);
		fUnboundSelect.setEnabled(enable);
		fMinLabel.setEnabled(enable);
		fMaxLabel.setEnabled(enable);
	}

	protected boolean blockListeners() {
		return fBlockListeners;
	}

	protected void setBlockListeners(boolean blockListeners) {
		fBlockListeners = blockListeners;
	}

	public void dispose() {
		// Set the context menu to null to prevent the editor context menu
		// from being disposed along with the source viewer
		if (fDescriptionViewer != null) {
			fDescriptionViewer.unsetMenu();
			fDescriptionViewer = null;
		}
		super.dispose();
	}
}
