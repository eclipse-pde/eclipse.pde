/*
 * Created on Jun 26, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

/**
 * @author Wassim Melhem
 */
public class DetailsForm extends ScrollableSectionForm {
	
	private LogEntry logEntry;
	private SessionDataSection sessionSection;

	public DetailsForm(LogEntry entry) {
		super();
		this.logEntry = entry;
	}
	
	protected void createFormClient(Composite parent) {
		GridLayout layout = new GridLayout();
		parent.setLayout(layout);
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));
		FormWidgetFactory factory = getFactory();
		
		sessionSection = new SessionDataSection(logEntry);
		Control control = sessionSection.createControl(parent, factory);
		control.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		registerSection(sessionSection);

	}
	
	
	public void initialize() {
		setHeadingText(logEntry.getSeverityText());
		((Composite) getControl()).layout(true);
	}
	public void setHeadingText(String text) {
		super.setHeadingText(text);
		Composite control = (Composite)getControl();
		if (control!=null) {
			control.layout(true);
			control.redraw();
		}
	}
	
	

}
