/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - bug 208531
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import java.util.Hashtable;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.ui.IFieldData;

/**
 * This class adds a notion of options to the default template section
 * implementation. Options have values and visual presence that allows users to
 * change them. When a section is configured with a number of options, they
 * become available to the code generator and can take part in conditional code
 * emitting.
 * <p>
 * This class is typically used in conjunction with
 * <samp>OptionTemplateWizardPage </samp>. The later is capable of creating UI
 * based on the list of options it was given, thus simplifying new template
 * section creation.
 * 
 * @since 2.0
 */

public abstract class BaseOptionTemplateSection extends AbstractTemplateSection {
	private Hashtable options = new Hashtable();

	/**
	 * Adds a boolean option with a provided name, label and initial value.
	 * 
	 * @param name
	 *            the unique name of the option (can be used as a variable in
	 *            conditional code emitting and variable substitution)
	 * @param label
	 *            presentable name of the option
	 * @param value
	 *            initial value of the option
	 * @param pageIndex
	 *            a zero-based index of a page where this option should appear
	 * @return the newly created option
	 */
	protected TemplateOption addOption(String name, String label, boolean value, int pageIndex) {
		BooleanOption option = new BooleanOption(this, name, label);
		registerOption(option, value ? Boolean.TRUE : Boolean.FALSE, pageIndex);
		return option;
	}

	/**
	 * Adds a string option with a provided name, label and initial value.
	 * 
	 * @param name
	 *            the unique name of the option (can be used as a variable in
	 *            conditional code emitting and variable substitution)
	 * @param label
	 *            presentable name of the option
	 * @param value
	 *            initial value of the option
	 * @param pageIndex
	 *            a zero-based index of a page where this option should appear
	 * @return the newly created option
	 */
	protected TemplateOption addOption(String name, String label, String value, int pageIndex) {
		StringOption option = new StringOption(this, name, label);
		registerOption(option, value, pageIndex);
		return option;
	}

	/**
	 * Adds a choice option with a provided name, label, list of choices and the
	 * initial value (choice).
	 * 
	 * @param name
	 *            the unique name of the option (can be used as a variable in
	 *            conditional code emitting and variable substitution)
	 * @param label
	 *            presentable name of the option
	 * @param choices
	 *            an array of choices that the user will have when setting the
	 *            value of the option. Each array position should accept an
	 *            array of String objects of size 2, the first being the unique
	 *            name and the second the presentable label of the choice.
	 * @param value
	 *            initial value (choice) of the option
	 * @param pageIndex
	 *            a zero-based index of a page where this option should appear
	 * @return the newly created option
	 */
	protected TemplateOption addOption(String name, String label, String[][] choices, String value, int pageIndex) {
		AbstractChoiceOption option;
		if (choices.length == 2)
			option = new RadioChoiceOption(this, name, label, choices);
		else
			option = new ComboChoiceOption(this, name, label, choices);
		registerOption(option, value, pageIndex);
		return option;
	}

	/**
	 * Force a combo choice representation.
	 * Radio buttons look bad - even if only two options specified.
	 * @param name
	 * @param label
	 * @param choices
	 * @param value
	 * @param pageIndex
	 * @return the newly created option
	 */
	protected ComboChoiceOption addComboChoiceOption(String name, String label, String[][] choices, String value, int pageIndex) {
		ComboChoiceOption option = new ComboChoiceOption(this, name, label, choices);
		registerOption(option, value, pageIndex);
		return option;
	}

	/**
	 * Adds a blank field with a default height to provide spacing.
	 * 
	 * @param pageIndex
	 *            a zero-based index of a page where this option should appear
	 * @return the newly created option
	 */
	protected TemplateOption addBlankField(int pageIndex) {
		BlankField field = new BlankField(this);
		registerOption(field, "", pageIndex); //$NON-NLS-1$
		return field;
	}

	/**
	 * Adds a blank field with a specific height to provide spacing.
	 * 
	 * @param height
	 *            specifies the height of the blank field in pixels            
	 * @param pageIndex
	 *            a zero-based index of a page where this option should appear
	 * @return the newly created option
	 */
	protected TemplateOption addBlankField(int height, int pageIndex) {
		BlankField field = new BlankField(this, height);
		registerOption(field, "", pageIndex); //$NON-NLS-1$
		return field;
	}

	/**
	 * Initializes the option with a given unique name with the provided value.
	 * The value will be set only if the option has not yet been initialized.
	 * 
	 * @param name
	 *            option unique name
	 * @param value
	 *            the initial value of the option
	 */
	protected void initializeOption(String name, Object value) {
		TemplateOption option = getOption(name);
		if (option != null) {
			// Only initialize options that have no value set
			if (option.getValue() == null)
				option.setValue(value);
		}
	}

	/**
	 * Returns a string value of the option with a given name. The option with
	 * that name must exist and must be registered as a string option to begin
	 * with.
	 * 
	 * @param name
	 *            the unique name of the option
	 * @return the string value of the option with a given name or <samp>null
	 *         </samp> if not found.
	 */
	public String getStringOption(String name) {
		TemplateOption option = (TemplateOption) options.get(name);
		if (option != null) {
			if (option instanceof StringOption) {
				return ((StringOption) option).getText();

			} else if (option instanceof AbstractChoiceOption) {
				// This situation covers both Combos and Radio buttons
				Object value = option.getValue();
				if (value instanceof String) {
					return (String) value;
				} else if (value != null) {
					return value.toString();
				}
			}
		}
		return null;
	}

	/**
	 * Returns a boolean value of the option with a given name. The option with
	 * that name must exist and must be registered as a boolean option to begin
	 * with.
	 * 
	 * @param key
	 *            the unique name of the option
	 * @return the boolean value of the option with a given name or <samp>null
	 *         </samp> if not found.
	 */
	public boolean getBooleanOption(String key) {
		TemplateOption option = (TemplateOption) options.get(key);
		if (option != null && option instanceof BooleanOption) {
			return ((BooleanOption) option).isSelected();
		}
		return false;
	}

	/**
	 * Enables the option with a given name. The exact effect of the method
	 * depends on the option type, but the end-result should always be the same -
	 * users should not be able to modify values of disabled options. This
	 * method has no effect if the option with a given name is not found.
	 * 
	 * @param name
	 *            the unique name of the option
	 * @param enabled
	 *            the enable state that the option should have
	 */
	public void setOptionEnabled(String name, boolean enabled) {
		TemplateOption option = (TemplateOption) options.get(name);
		if (option != null)
			option.setEnabled(enabled);
	}

	/**
	 * Returns the value of the option with a given name. The actual type of the
	 * returned object depends on the option type.
	 * 
	 * @param name
	 *            the name of the option
	 * @return the current value of the option with a specified name or
	 *         <samp>null </samp> if not found or not applicable.
	 */
	public Object getValue(String name) {
		TemplateOption option = (TemplateOption) options.get(name);
		if (option != null)
			return option.getValue();
		return super.getValue(name);
	}

	/**
	 * Returns true if this template depends on values set in the parent wizard.
	 * Values in the parent wizard include plug-in id, plug-in name, plug-in
	 * class name, plug-in provider etc. If the template does depend on these
	 * values, <samp>initializeFields </samp> will be called when the page is
	 * made visible in the forward direction (going from the first page to the
	 * pages owned by this template). If the page is never shown (Finish is
	 * pressed before the page is made visible at least once),
	 * <samp>initializeFields </samp> will be called with the model object
	 * instead during template execution. The same method will also be called
	 * when the template is created within the context of the plug-in manifest
	 * editor, because plug-in model already exists at that time.
	 * 
	 * @return <code>true</code> if this template depends on the data set in
	 *         the parent wizard, <code>false</code> otherwise.
	 */
	public boolean isDependentOnParentWizard() {
		return false;
	}

	/**
	 * Initializes options in the wizard page using the data provided by the
	 * method parameters. Some options may depend on the user selection in the
	 * common wizard pages before template page has been shown (for example,
	 * plug-in ID, plug-in name etc.). This method allows options to initialize
	 * in respect to these values.
	 * <p>
	 * The method is called before the actual plug-in has been built.
	 * </p>
	 * 
	 * @param data
	 *            plug-in data as defined in the common plug-in project wizard
	 *            pages
	 */
	protected void initializeFields(IFieldData data) {
	}

	/**
	 * Initializes options in the wizard page using the data provided by the
	 * method parameters. Some options may depend on the user selection in the
	 * common wizard pages before template page has been shown (for example,
	 * plug-in ID, plug-in name etc.). This method allows options to initialize
	 * in respect to these values.
	 * <p>
	 * This method is called after the plug-in has already been created or as
	 * part of new extension creation (inside the manifest editor). Either way,
	 * the plug-in properties in the model have been fully set and the model can
	 * be used to initialize options that cannot be initialized independently.
	 * 
	 * @param model
	 *            the model of the plug-in manifest file.
	 */
	public void initializeFields(IPluginModelBase model) {
	}

	/**
	 * Subclasses must implement this method in order to validate options whose
	 * value have been changed by the user. The subclass can elect to validate
	 * the option on its own, or to also check validity of other options in
	 * relation to the new value of this one.
	 * 
	 * @param changed
	 *            the option whose value has been changed by the user
	 */
	public abstract void validateOptions(TemplateOption changed);

	/**
	 * Expands variable substitution to include all string options defined in
	 * this template.
	 * 
	 * @see AbstractTemplateSection#getReplacementString(String, String)
	 */
	public String getReplacementString(String fileName, String key) {
		String value = getStringOption(key);
		if (value != null)
			return value;
		return super.getReplacementString(fileName, key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#execute(org.eclipse.core.resources.IProject, org.eclipse.pde.core.plugin.IPluginModelBase, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		/*
		 * Modifies the superclass implementation by adding the initialization step
		 * before commencing execution. This is important because some options may
		 * not be initialized and users may choose to press 'Finish' before the
		 * wizard page where the options are were shown for the first time.
		 */
		initializeFields(model);
		super.execute(project, model, monitor);
	}

	/**
	 * Registers the provided option and sets the initial value.
	 * 
	 * @param option
	 *            the option to register
	 * @param value
	 *            the initial value
	 * @param pageIndex
	 *            the page index to which this option belongs
	 */
	protected void registerOption(TemplateOption option, Object value, int pageIndex) {
		option.setValue(value);
		options.put(option.getName(), option);
	}

	private TemplateOption getOption(String key) {
		return (TemplateOption) options.get(key);
	}
}
