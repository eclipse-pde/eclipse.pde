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
public class SessionDataSection extends BasePreviewSection {
	
	public SessionDataSection(ScrollableSectionForm form) {
		super(form, PDERuntimePlugin.getResourceString("LogView.preview.sessionData"));
		setCollapsed(true);
	}

	protected String getTextFromEntry() {
		String data = getEntry().getSession().getSessionData();
		if (data == null || data.length() == 0)
			data = PDERuntimePlugin.getResourceString("LogView.preview.noSessionData");
		return data;
	}
}
