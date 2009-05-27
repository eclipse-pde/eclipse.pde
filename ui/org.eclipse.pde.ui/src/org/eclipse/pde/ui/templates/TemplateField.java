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
package org.eclipse.pde.ui.templates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * The base class for all the template option fields. Template option is a
 * single editable option that is exposed to the users in the wizard pages
 * associated with templates. Although the field is associated with the template
 * section, there is no 1/1 mapping between the field and the substitution value
 * that can be used in the template files. In general, a subclass of this class
 * can generate any SWT control in the provided composite.
 * 
 * @since 2.0
 */
public abstract class TemplateField {
	private BaseOptionTemplateSection section;
	private String label;

	/**
	 * The constructor for the field.
	 * 
	 * @param section
	 *            the section that owns this field
	 * @param label
	 *            the label of this field
	 */
	public TemplateField(BaseOptionTemplateSection section, String label) {
		this.section = section;
		this.label = label;
	}

	/**
	 * Returns the field label.
	 * 
	 * @return field label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Changes the label of this field.
	 * 
	 * @param label
	 *            the new label of this field.
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Returns the template section that owns this option field.
	 * 
	 * @return parent template section
	 */
	public BaseOptionTemplateSection getSection() {
		return section;
	}

	/**
	 * Factory method that creates the label in the provided parent.
	 * 
	 * @param parent
	 *            the parent composite to create the label in
	 * @param span
	 *            number of columns that the label should span
	 * @return the newly created Label widget.
	 */
	protected Label createLabel(Composite parent, int span) {
		Label label = new Label(parent, SWT.NULL);
		label.setText(getLabel());
		return label;
	}

	/**
	 * Subclasses must implement this method to create the control of the
	 * template field.
	 * 
	 * @param parent
	 *            the parent composite the control should be created in
	 * @param span
	 *            number of columns that the control should span
	 */
	public abstract void createControl(Composite parent, int span);
}
