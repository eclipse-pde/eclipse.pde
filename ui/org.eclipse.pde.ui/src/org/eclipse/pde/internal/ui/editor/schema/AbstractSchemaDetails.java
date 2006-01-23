/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public abstract class AbstractSchemaDetails extends PDEDetails {

	protected static final String STRING_TYPE = "string"; //$NON-NLS-1$
	protected static final String BOOLEAN_TYPE = "boolean"; //$NON-NLS-1$
	protected static final String[] BOOLS = 
		new String[] { Boolean.toString(true), Boolean.toString(false) };
	
	private Section fSection;
	private Text fDtdLabel;
	private ElementSection fElementSection;
	private boolean fShowDTD;
	private Spinner fMinOccurSpinner;
	private Spinner fMaxOccurSpinner;
	private Button fUnboundSelect;
	private Label fMinLabel;
	private Label fMaxLabel;
	
	public AbstractSchemaDetails(ElementSection section, boolean showDTD) {
		fElementSection = section;
		fShowDTD = showDTD;
	}
	
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.REMOVE)
			return;
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof ISchemaCompositor)
				updateDTDLabel(objects[i]);
		}
	}
	
	public final void createContents(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		parent.setLayout(layout);
		FormToolkit toolkit = getManagedForm().getToolkit();
		fSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fSection.marginHeight = 5;
		fSection.marginWidth = 5; 
		GridData gd = new GridData(GridData.FILL_BOTH);
		fSection.setLayoutData(gd);
		Composite client = toolkit.createComposite(fSection);
		GridLayout glayout = new GridLayout(3, false);
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		if (paintedBorder) glayout.verticalSpacing = 7;
		client.setLayout(glayout);
		
		createDetails(client);
		updateFields();
		
		if (fShowDTD) {
			Label label = toolkit.createLabel(client, PDEUIMessages.AbstractSchemaDetails_dtdLabel);
			label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 3;
			gd.verticalIndent = 15;
			label.setLayoutData(gd);
			
			fDtdLabel = toolkit.createText(client, "", SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);//$NON-NLS-1$
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 3;
			gd.heightHint = 60;
			fDtdLabel.setLayoutData(gd);
			fDtdLabel.setEditable(false);
			// remove pop-up menu
			fDtdLabel.setMenu(new Menu(client));
		}
		
		toolkit.paintBordersFor(client);
		fSection.setClient(client);
		markDetailsPart(fSection);
		
		hookListeners();
	}
	
	public abstract void createDetails(Composite parent);
	public abstract void updateFields();
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
		return (PDEFormPage)getManagedForm().getContainer();
	}
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}
	public void selectionChanged(IFormPart part, ISelection selection) {
		if (!(part instanceof ElementSection))
			return;
		updateDTDLabel(((IStructuredSelection)selection).getFirstElement());
	}
	
	private void updateDTDLabel(Object changeObject) {
		if (!fShowDTD || fDtdLabel.isDisposed()) return;
		if (changeObject instanceof ISchemaAttribute) {
			changeObject = ((ISchemaAttribute) changeObject).getParent();
		} else if (changeObject instanceof ISchemaCompositor) {
			while (changeObject != null) {
				if (changeObject instanceof ISchemaElement)
					break;
				changeObject = ((ISchemaCompositor)changeObject).getParent();
			}
		}
		if (changeObject instanceof ISchemaElement)
			fDtdLabel.setText(((ISchemaElement)changeObject).getDTDRepresentation(false));
	}
	
	protected void fireMasterSelection(ISelection selection) {
		fElementSection.fireSelection(selection);
	}
	
	protected ComboPart createComboPart(Composite parent, FormToolkit toolkit, String[] items, int colspan) {
		ComboPart cp = new ComboPart();
		cp.createControl(parent, toolkit, SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colspan;
		cp.getControl().setLayoutData(gd);
		cp.setItems(items);
		cp.getControl().setEnabled(isEditable());
		return cp;
	}
	
	protected Button[] createTrueFalseButtons(Composite parent, FormToolkit toolkit, int colSpan) {
		Composite comp = toolkit.createComposite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = gl.marginWidth = 0;
		comp.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
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
		fMinLabel.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
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
		fMinOccurSpinner.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				int minOccur = fMinOccurSpinner.getSelection();
				if (minOccur > getMaxOccur())
					fMinOccurSpinner.setSelection(minOccur - 1);
			}
		});
		return comp;
	}
	
	protected Composite createMaxOccurComp(Composite parent, FormToolkit toolkit) {
		fMaxLabel = toolkit.createLabel(parent, PDEUIMessages.AbstractSchemaDetails_maxOccurLabel);
		fMaxLabel.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
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
		fMaxOccurSpinner.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				int maxValue = fMaxOccurSpinner.getSelection();
				if (maxValue < getMinOccur())
					fMaxOccurSpinner.setSelection(maxValue + 1);
			}
		});
		
		fUnboundSelect = toolkit.createButton(comp, PDEUIMessages.AbstractSchemaDetails_unboundedButton, SWT.CHECK);
		gd = new GridData();
		gd.horizontalIndent = 10;
		fUnboundSelect.setLayoutData(gd);
		fUnboundSelect.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fMaxOccurSpinner.setEnabled(!fUnboundSelect.getSelection() 
						&& isEditableElement());
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
		if (fMaxOccurSpinner == null) return;
		boolean isMax = max == Integer.MAX_VALUE;
		fUnboundSelect.setSelection(isMax);
		fMaxOccurSpinner.setEnabled(!isMax);
		if (!isMax)
			fMaxOccurSpinner.setSelection(max);
	}
	
	protected void hookMinOccur(SelectionAdapter adapter) {
		fMinOccurSpinner.addSelectionListener(adapter);
	}
	
	protected void hookMaxOccur(SelectionAdapter adapter) {
		fUnboundSelect.addSelectionListener(adapter);
		fMaxOccurSpinner.addSelectionListener(adapter);
	}
	
	protected void enableMinMax(boolean enable) {
		fMinOccurSpinner.setEnabled(enable);
		fMaxOccurSpinner.setEnabled(!fUnboundSelect.getSelection() && enable);
		fUnboundSelect.setEnabled(enable);
		fMinLabel.setEnabled(enable);
		fMaxLabel.setEnabled(enable);
	}
}
