/*
 * Created on Jun 26, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.update.ui.forms.internal.ScrollableSectionForm;

/**
 * @author Wassim Melhem
 */
public class StackSection extends BasePreviewSection {
	
	public StackSection(ScrollableSectionForm form) {
		super(form, "Exception Stack Trace");
	}
	
	protected String getTextFromEntry() {
		return getEntry().getStack();
	}
}
