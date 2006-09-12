/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSElementSection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSSharedUIFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSIntroDetails
 *
 */
public class SimpleCSIntroDetails extends SimpleCSAbstractDetails implements ISimpleCSHelpDetails{

	private ISimpleCSIntro fIntro;
	
	private Text fContextId;
	
	private Text fHref;	
	
	private FormEntry fContent;
	
	private Section fMainSection;	
	
	private Section fHelpSection;	
	
	private Button fContextIdRadio;	

	private Button fHrefRadio;
	
	
	/**
	 * @param elementSection
	 */
	public SimpleCSIntroDetails(ISimpleCSIntro intro, SimpleCSElementSection elementSection) {
		super(elementSection);
		fIntro = intro;
		
		fContextId = null;
		fHref = null;
		fContent = null;
		fMainSection = null;
		fHelpSection = null;
		fContextIdRadio = null;
		fHrefRadio = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		// TODO: MP: Probably can refactor this back into super class as utility
		// Creation of section and composite
		FormToolkit toolkit = getManagedForm().getToolkit();
		//Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		GridData data = null;
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		//Label label = null;
		
		// Set parent layout
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		parent.setLayout(layout);
		
		// Create main section
		fMainSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fMainSection.marginHeight = 5;
		fMainSection.marginWidth = 5; 
		fMainSection.setText(PDEUIMessages.SimpleCSIntroDetails_2);
		fMainSection.setDescription(PDEUIMessages.SimpleCSIntroDetails_3);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);
		
		// Create container for main section
		Composite mainSectionClient = toolkit.createComposite(fMainSection);	
		layout = new GridLayout(2, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		mainSectionClient.setLayout(layout);				

	
		// description:  Content (Element)
		fContent = new FormEntry(mainSectionClient, toolkit, PDEUIMessages.SimpleCSDescriptionDetails_0, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 90;
		fContent.getText().setLayoutData(data);		
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fContent.getLabel().setLayoutData(data);				

		// Bind widgets
		toolkit.paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);
		
		// TODO: MP: Magic number
		// TODO: MP: Remember state of open close section
		fHelpSection = SimpleCSSharedUIFactory.createHelpSection(parent,
				toolkit, 1, this);
		if (fHelpSection == null) {}
		
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// description: Content (Element)
		fContent.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fIntro.getDescription() != null) {
					fIntro.getDescription().setContent(fContent.getValue());
				}
			}
		});
		
		// Attribute: contextId
		fContextId.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fIntro.setContextId(fContextId.getText());
			}
		});		
		// Attribute: href
		fHref.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fIntro.setHref(fHref.getText());
			}
		});	
		// Radio button for contextId
		fContextIdRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fContextIdRadio.getSelection();
				fContextId.setEnabled(selected);
				fHref.setEnabled(!selected);				
			}
		});
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		
		if (fIntro == null) {
			return;
		}			
		
		// Attribute: contextId
		// Attribute: href		
		// Radio button for contextId
		// Radio button for contextId		
		if (PDETextHelper.isDefined(fIntro.getContextId())) {
			fContextId.setText(fIntro.getContextId());
			fContextId.setEnabled(true && editable);
			fContextIdRadio.setSelection(true && editable);
			fHref.setEnabled(false);	
			fHrefRadio.setSelection(false);			
		} else if (PDETextHelper.isDefined(fIntro.getHref())) {
			fHref.setText(fIntro.getHref());
			fContextId.setEnabled(false);
			fContextIdRadio.setSelection(false);			
			fHref.setEnabled(true && editable);			
			fHrefRadio.setSelection(true && editable);
		} else {
			fContextId.setEnabled(true && editable);
			fContextIdRadio.setSelection(true && editable);
			fHref.setEnabled(false);	
			fHrefRadio.setSelection(false);					
		}
		
		
		if (fIntro.getDescription() == null) {
			return;
		}

		// description:  Content (Element)
		fContent.setValue(fIntro.getDescription().getContent());
		fContent.setEditable(editable);		

	

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSHelpDetails#setContextId(org.eclipse.pde.internal.ui.parts.FormEntry)
	 */
	public void setContextId(Text contextId) {
		fContextId = contextId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSHelpDetails#setContextIdRadio(org.eclipse.swt.widgets.Button)
	 */
	public void setContextIdRadio(Button contextIdRadio) {
		fContextIdRadio = contextIdRadio;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSHelpDetails#setHref(org.eclipse.pde.internal.ui.parts.FormEntry)
	 */
	public void setHref(Text href) {
		fHref = href;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSHelpDetails#setHrefRadio(org.eclipse.swt.widgets.Button)
	 */
	public void setHrefRadio(Button hrefRadio) {
		fHrefRadio = hrefRadio;
	}	
	
}
