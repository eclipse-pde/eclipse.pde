/*
 * Created on Jun 26, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

/**
 * @author Wassim Melhem
 */
public class SessionDataSection extends FormSection {
	private LogEntry entry;

	public SessionDataSection(LogEntry entry) {
		super();
		this.entry = entry;
		setHeaderText("Session Data");
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		
		Text text = factory.createText(container, entry.getSession().getSessionData(), SWT.WRAP|SWT.V_SCROLL|SWT.H_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		text.setLayoutData(gd);
		return container;
	}

}
