package org.eclipse.pde.ui.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

/**
 * The base class of all the template options. Options have
 * unique name and a value that can be changed.
 * <p> 
 * <b>Note:</b> This class is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public abstract class TemplateOption extends TemplateField {
	private String name;
	private Object value;
	private boolean enabled = true;
	private boolean required;
	/**
	 * Creates a new option for the provided template section.
	 * @param section the parent template section
	 * @param name the unique name of this option
	 * @param the presentable label of this option
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public TemplateOption(
		BaseOptionTemplateSection section,
		String name,
		String label) {
		super(section, label);
		this.name = name;
	}
	/**
	 * Returns the unique name of this option
	 * @return option name
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getName() {
		return name;
	}
	/**
	 * Changes the unique name of this option
	 * @param name the new option name
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the value of this option.
	 * @return the current value
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public Object getValue() {
		return value;
	}
	/**
	 * Returns whether this option is currently empty. The actual
	 * semantics of the result depends on the implementing option.
	 * @return <samp>true</samp> if option is empty, </samp> false otherwise.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isEmpty() {
		return false;
	}
	/**
	 * Marks this option as required. Required options must be
	 * set by the user. An option that is empty and is marked
	 * required will be flagged as an error in the wizard.
	 * @param required the new value of the property
	 * @see #isEmpty
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setRequired(boolean required) {
		this.required = required;
	}
	/**
	 * Returns whether this option is required (cannot be empty)
	 * @return <samp>true</samp> if this option is required, 
	 * <samp>false</samp> otherwise.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * Sets the new value of this option.
	 * @param value the new value
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setValue(Object value) {
		this.value = value;
	}
	/**
	 * Returns whether this option is enabled. The actual presentation
	 * of enabled state depends on the implementing option.
	 * @return <samp>true</samp> if option is enabled and can
	 * be modified.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isEnabled() {
		return enabled;
	}
	/**
	 * Sets the enabled state of this option. The action presentation
	 * of the enabled state depends on the implementing option.
	 * @param enabled the new enabled state
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	/**
	 * Returns the label of this option that can be presented
	 * in the messages to the user. The default implementation
	 * trims the 'label' property from mnemonics and from the trailing
	 * column.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getMessageLabel() {
		String label = getLabel();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < label.length(); i++) {
			char c = label.charAt(i);
			if (c == '(' && i < label.length() -1) {
				char c2 = label.charAt(i+1);
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