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

public class LogEntryPropertyPage extends PropertyPage {
	public static final String KEY_DATE = "LogView.propertyPage.date";
	public static final String KEY_SEVERITY = "LogView.propertyPage.severity";
	public static final String KEY_MESSAGE = "LogView.propertyPage.message";
	public static final String KEY_EXCEPTION = "LogView.propertyPage.exception";
	private LogViewLabelProvider labelProvider;

public LogEntryPropertyPage() {
	labelProvider = new LogViewLabelProvider();
	noDefaultAndApplyButton();
}
protected Control createContents(Composite parent) {
	LogEntry entry = (LogEntry) getElement();
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 3;
	container.setLayout(layout);
	
	Label label = new Label(container, SWT.NULL);
	label.setText(PDERuntimePlugin.getResourceString(KEY_DATE));
	label = new Label(container, SWT.NULL);
	label.setText(entry.getDate());
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.horizontalSpan = 2;
	label.setLayoutData(gd);

	label = new Label(container, SWT.NULL);
	label.setText(PDERuntimePlugin.getResourceString(KEY_SEVERITY));
	label = new Label(container, SWT.NULL);
	label.setImage(labelProvider.getColumnImage(entry, 1));

	label = new Label(container, SWT.NULL);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	label.setText(entry.getSeverityText());
	label.setLayoutData(gd);

	label = new Label(container, SWT.NULL);
	label.setText(PDERuntimePlugin.getResourceString(KEY_MESSAGE));
	label = new Label(container, SWT.NULL);
	label.setText(entry.getMessage());
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.horizontalSpan = 2;
	label.setLayoutData(gd);

	String stack = entry.getStack();
	if (stack != null) {
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
		text.setText(stack);
	}
	return container;
}
public void dispose() {
	labelProvider.dispose();
	super.dispose();
}
}
