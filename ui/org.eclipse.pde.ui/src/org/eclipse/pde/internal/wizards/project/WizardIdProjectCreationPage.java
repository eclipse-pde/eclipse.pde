package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.ui.dialogs.*;
import org.eclipse.pde.internal.*;
import java.util.StringTokenizer;

public class WizardIdProjectCreationPage extends WizardNewProjectCreationPage {
	public static final String KEY_INVALID_ID = "WizardIdProjectCreationPage.invalidId";
	/**
	 * Constructor for WizardIdProjectCreationPage
	 */
	public WizardIdProjectCreationPage(String name) {
		super(name);
	}
	
	public void setPageComplete(boolean complete) {
		if (complete) {
			// Check ID rules
			String problemText = verifyIdRules();
			if (problemText!=null)
			   complete = false;
		}
		super.setPageComplete(complete);
	}
	
	public void setErrorMessage(String message) {
		if (message==null) {
			message = verifyIdRules();
		}
		super.setErrorMessage(message);
	}
	
	private String verifyIdRules() {
		String problemText = PDEPlugin.getResourceString(KEY_INVALID_ID);
		String name = getProjectName();
		StringTokenizer stok = new StringTokenizer(name, ".");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i=0; i<token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i))==false)
				   return problemText;
			}
		}
		return null;
	}
}
