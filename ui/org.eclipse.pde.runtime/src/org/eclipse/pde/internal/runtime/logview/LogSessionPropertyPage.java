package org.eclipse.pde.internal.runtime.logview;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;

public class LogSessionPropertyPage extends PropertyPage {
	public static final String KEY_SESSION = "LogView.propertyPage.session";

	public LogSessionPropertyPage() {
		noDefaultAndApplyButton();
	}
	protected Control createContents(Composite parent) {
		LogEntry entry = (LogEntry) getElement();
		LogSession session = entry.getSession();
		
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		//layout.numColumns = 2;
		container.setLayout(layout);
		Label label = new Label(container, SWT.NULL);
		label.setText(PDERuntimePlugin.getResourceString(KEY_SESSION));
		label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		Text text =
			new Text(
				container,
				SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		text.setEditable(false);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		gd.heightHint = 300;
		// defect 17008
		if (session != null && session.getSessionData() != null)
			text.setText(session.getSessionData());
		text.setLayoutData(gd);
		return container;
	}
}