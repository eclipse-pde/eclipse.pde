/*
 * Created on Dec 2, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Widget;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WizardCommandTarget extends WindowCommandTarget {
	/**
	 * @param widget
	 * @param window
	 */
	public WizardCommandTarget(Widget widget, Window window) {
		super(widget, window);
	}

	public WizardDialog getWizardDialog() {
		return (WizardDialog)getWindow();
	}
}