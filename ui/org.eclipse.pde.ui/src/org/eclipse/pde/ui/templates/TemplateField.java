package org.eclipse.pde.ui.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

/**
 * The base class for all the template options.
 * <p> 
 * <b>Note:</b> This class is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */

public abstract class TemplateField {
	private BaseOptionTemplateSection section;
	private String label;
	/**
	 * The constructor for the field.
	 * @param section the section that owns this field
	 * @param label the label of this field
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public TemplateField(BaseOptionTemplateSection section, String label) {
		this.section = section;
		this.label = label;
	}
	/**
	 * Returns the field label.
	 * @return field label
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getLabel() {
		return label;
	}
	/**
	 * Changes the label of this field.
	 * @param label the new label of this field.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	/**
	 * Returns the template section that owns this option field.
	 * @return parent template section
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public BaseOptionTemplateSection getSection() {
		return section;
	}
	/**
	 * Factory method that creates the label in the provided parent.
	 * @param parant the parent composite to create the label in
	 * @param span number of columns that the label should span
	 * @return the newly created Label widget.
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	protected Label createLabel(Composite parent, int span) {
		Label label = new Label(parent, SWT.NULL);
		label.setText(getLabel());
		return label;
	}
	/**
	 * Subclasses must implement this method to create the
	 * control of the template field.
	 * @param parent the parent composite the control should be
	 * created in
	 * @param span number of columns that the control should span
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public abstract void createControl(Composite parent, int span);
}