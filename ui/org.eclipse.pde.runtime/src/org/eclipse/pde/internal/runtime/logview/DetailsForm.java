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
	private StackSection stackSection;

	public DetailsForm() {
		setVerticalFit(true);
	}
	
	protected void createFormClient(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 3;
		layout.marginHeight = 0;
		layout.verticalSpacing = 10;
		parent.setLayout(layout);
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));
		FormWidgetFactory factory = getFactory();
		
		sessionSection = new SessionDataSection(this);
		Control control = sessionSection.createControl(parent, factory);
		control.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));

		stackSection = new StackSection(this);
		control = stackSection.createControl(parent, factory);
		control.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_FILL));
		
		registerSection(sessionSection);
		registerSection(stackSection);
	}
	
	public void initialize() {
		if (logEntry!=null) setHeadingText(logEntry.getSeverityText());
		update();
	}
	
	public void openTo(LogEntry entry) {
		sessionSection.expandTo(entry);
		stackSection.expandTo(entry);
		setHeadingText(entry.getSeverityText());
	}
}
