package org.eclipse.pde.internal.preferences;

import java.util.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
/**
 * Insert the type's description here.
 * Creation date: (5/14/2001 8:45:10 PM)
 * @author: Dejan Glozic
 */
public class TextOption extends RuntimeOption {
	private Text text;
/**
 * CheckOption constructor comment.
 * @param key java.lang.String
 * @param label java.lang.String
 */
public TextOption(String key, String label) {
	super(key, label);
}
/**
 * createControl method comment.
 */
public Control createControl(Composite parent) {
	Label label = new Label(parent, SWT.NULL);
	label.setText(getLabel());
	text = new Text(parent, SWT.SINGLE | SWT.BORDER);
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	text.setLayoutData(gd);
	return text;
}
/**
 * Insert the method's description here.
 * Creation date: (5/14/2001 8:45:10 PM)
 * @param value java.lang.Object
 */
public String getValue() {
	return text.getText();
}
/**
 * Insert the method's description here.
 * Creation date: (5/14/2001 8:45:10 PM)
 * @param value java.lang.Object
 */
public void setValue(Object value) {
	if (value != null) {
		String svalue = value.toString();
		text.setText(svalue);
	}
}
}
