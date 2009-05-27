/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * This template option can be used to create blank space on the 
 * template section wizard page.
 * 
 * @since 3.2
 */
public class BlankField extends TemplateOption {

	private final static int DEFAULT_HEIGHT = 20;
	private final static String OPTION_NAME = "blankField"; //$NON-NLS-1$
	private static int NUM_CREATED = 0;

	private static String getUniqueName() {
		return OPTION_NAME + Integer.toString(NUM_CREATED++);
	}

	private Label fblankLabel;
	private int fheight;

	/**
	 * The default constructor. 
	 * 
	 * @param section
	 * 			the parent section
	 */
	public BlankField(BaseOptionTemplateSection section) {
		super(section, getUniqueName(), ""); //$NON-NLS-1$
		fheight = DEFAULT_HEIGHT;
	}

	/**
	 * Overloaded constructor to specify the height of the blank field. 
	 * 
	 * @param section
	 * 			the parent section
	 * @param height
	 * 			specifies the height of the blank field in pixels
	 */
	public BlankField(BaseOptionTemplateSection section, int height) {
		super(section, getUniqueName(), ""); //$NON-NLS-1$
		fheight = height;
	}

	/**
	 * Creates a blank field using a label widget.
	 * 
	 * @param parent
	 *            parent composite of the blank label
	 * @param span
	 *            the number of columns that the widget should span
	 */
	public void createControl(Composite parent, int span) {
		fblankLabel = createLabel(parent, span);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = fheight;
		gd.horizontalSpan = span;
		fblankLabel.setLayoutData(gd);
	}

}
