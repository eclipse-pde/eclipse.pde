/*
 * Created on Jun 26, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

/**
 * @author Wassim Melhem
 */
public abstract class BasePreviewSection extends FormSection {
	private LogEntry entry;
	private Text text;
	private ScrollableSectionForm form;
	
	public BasePreviewSection(ScrollableSectionForm form, String title, boolean collapsed) {
		this.form = form;
		setHeaderText(title);
		setCollapsable(true);
		setCompactMode(true);
		setCollapsed(collapsed);
	}
	
	public LogEntry getEntry() {
		return entry;
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;

		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		text = new Text(container, SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = SWT.DEFAULT;
		text.setLayoutData(gd);
		return container;
	}
	
	public void expandTo(Object object) {
		entry = (LogEntry)object;
		refresh();
	}
	
	private void refresh() {
		if (entry==null) {
			text.setText("");
		}
		else {
			text.setText(getTextFromEntry());
		}
		if (!isCollapsed())
			reflow();
	}
	protected abstract String getTextFromEntry();

	protected void reflow() {
		super.reflow();
		form.update();
	}
}
