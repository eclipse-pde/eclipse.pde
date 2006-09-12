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

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple;

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSHelpDetails;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSSharedUIFactory
 *
 */
public class SimpleCSSharedUIFactory {

	// TODO: MP: Probably will delete this method after veritcal spacing adjusted
	/**
	 * @param parent
	 * @param toolkit
	 * @param columnSpan
	 */
	public static Label createSpacer(Composite parent, FormToolkit toolkit,
			int columnSpan) {
		Label label = toolkit.createLabel(parent, "", SWT.WRAP); //$NON-NLS-1$
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = columnSpan;
		label.setLayoutData(data);	
		return label;
	}
	
	/**
	 * @param parent
	 * @param toolkit
	 * @param columnSpan
	 * @param text
	 */
	public static Label createLabel(Composite parent, FormToolkit toolkit,
			int columnSpan, String text, Color foreground) {
		
		Label label = toolkit.createLabel(parent, text, SWT.WRAP);
		//label.setForeground(foreground);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		//data.widthHint = 150;
		data.horizontalSpan = columnSpan;
		label.setLayoutData(data);
		if (foreground != null) {
			label.setForeground(foreground);
		}
		return label;
	}

	// TODO: MP: Have to refactor this into own DETAILS class 
	// TODO: MP: Have to share listeners and update fields
	
	/**
	 * @param parent
	 * @param toolkit
	 * @param columnSpan
	 * @param helpDetails
	 * @return
	 */
	public static Section createHelpSection(Composite parent, FormToolkit toolkit,
			int columnSpan, ISimpleCSHelpDetails helpDetails) {

		Text contextId = null;
		Text href = null;
		Button contextIdRadio = null; 
		Button hrefRadio = null;
		Section helpSection = null;
		
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		GridData data = null;
		GridLayout layout = null;
		
		// Create help section
		helpSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE);
		helpSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		helpSection.marginHeight = 5;
		helpSection.marginWidth = 5;
		helpSection.setText(PDEUIMessages.SimpleCSSharedUIFactory_1);
		helpSection.setDescription(PDEUIMessages.SimpleCSSharedUIFactory_2);
		data = new GridData(GridData.FILL_HORIZONTAL);
		helpSection.setLayoutData(data);
		
		// Create container for help section		
		Composite helpSectionClient = toolkit.createComposite(helpSection);	
		layout = new GridLayout(columnSpan, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		helpSectionClient.setLayout(layout);		
		
		// Create the radio button for the contextId
		contextIdRadio = toolkit.createButton(helpSectionClient, PDEUIMessages.SimpleCSSharedUIFactory_3, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		contextIdRadio.setLayoutData(data);
		//data.horizontalSpan = 2;
		
		// Attribute: contextId
		//contextId = new FormEntry(helpSectionClient, toolkit, null, 20, 0);
		contextId = toolkit.createText(helpSectionClient, null);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalIndent = 20;
		contextId.setLayoutData(data);
		// TODO: MP: Delete
		//PDEUIMessages.SimpleCSIntroDetails_0

		// Create the radio button for the href
		hrefRadio = toolkit.createButton(helpSectionClient, PDEUIMessages.SimpleCSSharedUIFactory_4, SWT.RADIO);
		data = new GridData(GridData.FILL_HORIZONTAL);
		hrefRadio.setLayoutData(data);
		//data.horizontalSpan = 2;
		
		// Attribute: href
		//href = new FormEntry(helpSectionClient, toolkit, null, 20, 0);
		href = toolkit.createText(helpSectionClient, null);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalIndent = 20;
		href.setLayoutData(data);		
		// TODO: MP: Delete
		//PDEUIMessages.SimpleCSIntroDetails_1
		
		//createSpacer(helpSectionClient, toolkit, columnSpan);
		
		// Bind widgets
		toolkit.paintBordersFor(helpSectionClient);
		helpSection.setClient(helpSectionClient);
		// TODO: MP: What does this do?
		//markDetailsPart(fHelpSection);		
		
		// Set new widgets on caller
		helpDetails.setContextId(contextId);
		helpDetails.setContextIdRadio(contextIdRadio);
		helpDetails.setHref(href);
		helpDetails.setHrefRadio(hrefRadio);
		
		
		return helpSection;
	}
	
	
}
