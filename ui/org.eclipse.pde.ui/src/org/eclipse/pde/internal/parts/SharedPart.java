/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.parts;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

/**
 * @version 	1.0
 * @author
 */
public abstract class SharedPart {
	private boolean enabled = true;

	public void setEnabled(boolean enabled) {
		if (enabled != this.enabled) {
			this.enabled = enabled;
			updateEnabledState();
		}
	}

	public abstract void createControl(
		Composite parent,
		int style,
		int span,
		FormWidgetFactory factory);

	public boolean isEnabled() {
		return enabled;
	}

	protected void updateEnabledState() {
	}

	protected Composite createComposite(
		Composite parent,
		FormWidgetFactory factory) {
		if (factory == null)
			return new Composite(parent, SWT.NULL);
		else
			return factory.createComposite(parent);
	}
	protected Label createEmptySpace(
		Composite parent,
		int span,
		FormWidgetFactory factory) {
		Label label;
		if (factory != null) {
			label = factory.createLabel(parent, null);
		} else {
			label = new Label(parent, SWT.NULL);
		}
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalSpan= span;
		gd.widthHint= 0;
		gd.heightHint= 0;
		label.setLayoutData(gd);
		return label;
	}
}