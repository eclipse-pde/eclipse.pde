package org.eclipse.pde.internal.runtime.logview;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.swt.layout.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.runtime.*;

public class StatusPropertyPage extends PropertyPage {
	public static final String KEY_SEVERITY = "LogView.propertyPage.severity";
	public static final String KEY_MESSAGE = "LogView.propertyPage.message";
	public static final String KEY_EXCEPTION = "LogView.propertyPage.exception";
	private LogViewLabelProvider provider;
	private LogViewLabelProvider labelProvider;

public StatusPropertyPage() {
	labelProvider = new LogViewLabelProvider();
	noDefaultAndApplyButton();
}
protected Control createContents(Composite parent) {
	StatusAdapter adapter = (StatusAdapter) getElement();
	IStatus status = adapter.getStatus();
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 3;
	container.setLayout(layout);
	Label label = new Label(container, SWT.NULL);
	label.setText(PDERuntimePlugin.getResourceString(KEY_SEVERITY));
	label = new Label(container, SWT.NULL);
	label.setImage(labelProvider.getColumnImage(adapter, 1));

	label = new Label(container, SWT.NULL);
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	label.setText(adapter.getSeverityText());
	label.setLayoutData(gd);

	label = new Label(container, SWT.NULL);
	label.setText(PDERuntimePlugin.getResourceString(KEY_MESSAGE));
	label = new Label(container, SWT.NULL);
	label.setText(status.getMessage());
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.horizontalSpan = 2;
	label.setLayoutData(gd);

	Throwable exception = status.getException();
	if (exception != null) {
		label = new Label(container, SWT.NULL);
		label.setText(PDERuntimePlugin.getResourceString(KEY_EXCEPTION));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		Text text =
			new Text(container, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		gd.widthHint = 300;
		gd.heightHint = 300;
		text.setLayoutData(gd);

		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter, true);
		exception.printStackTrace(writer);
		try {
			swriter.close();
		}
		catch (IOException e) {
		}
		text.setText(swriter.toString());
	}
	return container;
}
public void dispose() {
	labelProvider.dispose();
	super.dispose();
}
}
