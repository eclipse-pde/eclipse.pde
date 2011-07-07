/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

/**
 * The base class of all the template options. Options have unique name and a
 * value that can be changed. The value of the option is automatically available
 * to the template files - can be accessed by substitution (e.g. $value_name$)
 * or as part of conditional code generation (e.g. if value_name).
 * 
 * @since 2.0
 */
public abstract class TemplateOption extends TemplateField {
	private String name;
	private Object value;
	private boolean enabled = true;
	private boolean required;

	/**
	 * Creates a new option for the provided template section.
	 * 
	 * @param section
	 *            the parent template section
	 * @param name
	 *            the unique name of this option
	 * @param label
	 *            presentable label of this option
	 */
	public TemplateOption(BaseOptionTemplateSection section, String name, String label) {
		super(section, label);
		this.name = name;
	}

	/**
	 * Returns the unique name of this option
	 * 
	 * @return option name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Changes the unique name of this option
	 * 
	 * @param name
	 *            the new option name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the value of this option.
	 * 
	 * @return the current value
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Returns whether this option is currently empty. The actual semantics of
	 * the result depends on the implementing option.
	 * 
	 * @return <samp>true </samp> if option is empty, </samp> false otherwise.
	 */
	public boolean isEmpty() {
		return false;
	}

	/**
	 * Marks this option as required. Required options must be set by the user.
	 * An option that is empty and is marked required will be flagged as an
	 * error in the wizard.
	 * 
	 * @param required
	 *            the new value of the property
	 * @see #isEmpty
	 */
	public void setRequired(boolean required) {
		this.required = required;
	}

	/**
	 * Returns whether this option is required (cannot be empty)
	 * 
	 * @return <samp>true </samp> if this option is required, <samp>false
	 *         </samp> otherwise.
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * Sets the new value of this option.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Returns whether this option is enabled. The actual presentation of
	 * enabled state depends on the implementing option.
	 * 
	 * @return <samp>true </samp> if option is enabled and can be modified.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Sets the enabled state of this option. The action presentation of the
	 * enabled state depends on the implementing option.
	 * 
	 * @param enabled
	 *            the new enabled state
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns the label of this option that can be presented in the messages to
	 * the user. The default implementation trims the 'label' property from
	 * mnemonics and from the trailing column.
	 * 
	 * @return the label to show to the user 
	 */
	public String getMessageLabel() {
		String label = getLabel();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < label.length(); i++) {
			char c = label.charAt(i);
			if (c == '(' && i < label.length() - 1) {
				char c2 = label.charAt(i + 1);
				if (c2 == '&') {
					// DBCS mnemonic sequence "(&<char>)"
					// It is OK to truncate the label
					// at this point
					break;
				}
			}
			if (c != '&' && c != ':')
				buf.append(c);
		}
		return buf.toString();
	}
}
