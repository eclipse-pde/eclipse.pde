/*
 * Created on Jun 26, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.pde.internal.runtime.*;
import org.eclipse.update.ui.forms.internal.ScrollableSectionForm;

/**
 * @author Wassim Melhem
 */
public class StackSection extends BasePreviewSection {
	
	public StackSection(ScrollableSectionForm form) {
		super(form, PDERuntimePlugin.getResourceString("LogView.preview.stackTrace"));
		setCollapsed(true);
	}
	
	protected String getTextFromEntry() {
		String text = getEntry().getStack();
		if (text == null)
			text = PDERuntimePlugin.getResourceString("LogView.preview.noStack");
		return text;
	}
}
