/*
 * Created on Jun 26, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IMemento;
import org.eclipse.update.ui.forms.internal.*;

/**
 * @author Wassim Melhem
 */
public class DetailsForm extends ScrollableSectionForm {
	
	private SessionDataSection sessionSection;
	private StackSection stackSection;
	private Image headingImage;
	private Label date;
	private Text message;
	private Composite parent;
	private Label eventType;
	private IMemento memento;

	public DetailsForm(IMemento memento) {
		setVerticalFit(true);
		this.memento = memento;
		headingImage = PDERuntimePluginImages.DESC_FORM_BANNER_SHORT.createImage();
		if (isWhiteBackground())
			setHeadingImage(headingImage);
		setHeadingText(PDERuntimePlugin.getResourceString("logView.preview.header"));
	}
	
	private boolean isWhiteBackground() {
		Color color = factory.getBackgroundColor();
		return (
			color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255);
	}

	public void openTo(LogEntry entry) {
		if (entry == null) {
			parent.setVisible(false);
		} else {
			parent.setVisible(true);
			sessionSection.expandTo(entry);
			stackSection.expandTo(entry);
			updateTopSection(entry);
		}
		update();
	}
	
	protected void createFormClient(Composite parent) {
		this.parent = parent;
		GridLayout layout = new GridLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 0;
		layout.verticalSpacing = 15;
		parent.setLayout(layout);
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTopSection(parent);

		stackSection = new StackSection(this, memento.getString(LogView.P_COLLAPSE_STACK).equals("true"));
		Control control = stackSection.createControl(parent, factory);
		control.setLayoutData(
			new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		sessionSection = new SessionDataSection(this, memento.getString(LogView.P_COLLAPSE_SESSION).equals("true"));
		control = sessionSection.createControl(parent, factory);
		control.setLayoutData(
			new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		
		registerSection(sessionSection);
		registerSection(stackSection);
	}

	private void createTopSection(Composite parent) {
		Composite comp = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		comp.setLayout(layout);
		comp.setLayoutData(
			new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));

		eventType = factory.createLabel(comp, "", SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		eventType.setLayoutData(gd);
		
		date = factory.createLabel(comp, "", SWT.NONE);
		gd = new GridData();
		gd.horizontalSpan = 2;
		date.setLayoutData(gd);

		Label label =
			factory.createLabel(
				comp,
				PDERuntimePlugin.getResourceString("LogView.preview.message"),
				SWT.NONE);
		label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		message = new Text(comp, SWT.MULTI|SWT.WRAP);
		gd = new GridData();
		gd.widthHint = 380;
		message.setLayoutData(gd);
		message.setEditable(false);
		message.setBackground(factory.getBackgroundColor());
	}

	private void updateTopSection(LogEntry entry) {
		date.setText(
			PDERuntimePlugin.getResourceString("LogView.preview.date")
				+ " "
				+ entry.getDate());
		eventType.setText(
			PDERuntimePlugin.getResourceString("LogView.preview.type")
				+ " "
				+ entry.getSeverityText());
		message.setText(entry.getMessage());

		Composite control = date.getParent();
		control.setRedraw(false);
		control.getParent().setRedraw(false);
		control.layout(true);
		control.getParent().layout(true);
		control.setRedraw(true);
		control.getParent().setRedraw(true);

	}
	
	public void dispose() {
		super.dispose();
		headingImage.dispose();
		unregisterSection(sessionSection);
		unregisterSection(stackSection);
	}
	
	public void saveState() {
		memento.putString(
			LogView.P_COLLAPSE_SESSION,
			sessionSection.isCollapsed() ? "true" : "false");
		memento.putString(
			LogView.P_COLLAPSE_STACK,
			stackSection.isCollapsed() ? "true" : "false");
	}
}
