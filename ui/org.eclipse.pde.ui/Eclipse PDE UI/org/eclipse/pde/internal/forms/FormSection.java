package org.eclipse.pde.internal.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;


public abstract class FormSection {
	public static final int SELECTION = 1;
	private String headerColorKey = FormWidgetFactory.DEFAULT_HEADER_COLOR;
	private String headerText;
	protected Label header;
	protected Control separator;
	private SectionChangeManager sectionManager;
	private java.lang.String description;
	private boolean dirty;
	protected Label descriptionLabel;
	private boolean readOnly;
	private boolean titleAsHyperlink;
	private boolean addSeparator=true;
	private boolean descriptionPainted=true;
	private boolean headerPainted=true;
	private int widthHint = SWT.DEFAULT;
	private int heightHint=SWT.DEFAULT;

public FormSection() {
}
public void commitChanges(boolean onSave) {
}
public abstract Composite createClient(Composite parent, FormWidgetFactory factory);
public final Control createControl(
	Composite parent,
	FormWidgetFactory factory) {
	Composite section = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	section.setLayout(layout);
	section.setData(this);
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	layout.verticalSpacing = 3;
	layout.horizontalSpacing = 0;
	GridData gd;
	if (headerPainted) {
		Color headerColor = factory.getColor(getHeaderColorKey());
		header = factory.createHeadingLabel(section, getHeaderText(), headerColor);
		if (titleAsHyperlink) {
			factory.turnIntoHyperlink(header, new HyperlinkAdapter() {
				public void linkActivated(Control label) {
					titleActivated();
				}
			});
		}
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.BEGINNING;
		header.setLayoutData(gd);
	}

	if (addSeparator) {
        //separator = factory.createSeparator(section, SWT.HORIZONTAL);
		separator = factory.createCompositeSeparator(section);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 2;
		//gd.heightHint = 1;
		separator.setLayoutData(gd);
	}

	if (descriptionPainted && description != null) {
		descriptionLabel = factory.createLabel(section, description, SWT.WRAP);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.verticalAlignment = GridData.BEGINNING;
		//gd.widthHint = 20;
		//gd.widthHint = widthHint;
		descriptionLabel.setLayoutData(gd);
	}
	Control client = createClient(section, factory);
	gd =
		new GridData(
			GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
	client.setLayoutData(gd);

	section.setData(this);
	return section;
}
protected Text createText(Composite parent, String label, FormWidgetFactory factory) {
	return createText(parent, label, factory, 1);
}
protected Text createText(Composite parent, String label, FormWidgetFactory factory, int span) {
	factory.createLabel(parent, label);
	Text text = factory.createText(parent, "");
	GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
	gd.grabExcessHorizontalSpace = true;
	gd.horizontalSpan=span;
	text.setLayoutData(gd);
	return text;
}
protected Text createText(Composite parent, FormWidgetFactory factory, int span) {
	Text text = factory.createText(parent, "");
	GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
	gd.grabExcessHorizontalSpace = true;
	gd.horizontalSpan=span;
	text.setLayoutData(gd);
	return text;
}
public void dispose() {}
public void doGlobalAction(String actionId) {}
public void expandTo(Object object) {}
public final void fireChangeNotification(int changeType, Object changeObject) {
	if (sectionManager == null)
		return;
	sectionManager.dispatchNotification(this, changeType, changeObject);
}
public final void fireSelectionNotification(Object changeObject) {
	fireChangeNotification(SELECTION, changeObject);
}
public java.lang.String getDescription() {
	return description;
}
public java.lang.String getHeaderColorKey() {
	return headerColorKey;
}
public java.lang.String getHeaderText() {
	return headerText;
}
public int getHeightHint() {
	return heightHint;
}
public int getWidthHint() {
	return widthHint;
}
public void initialize(Object input) {}
public boolean isAddSeparator() {
	return addSeparator;
}
public boolean isDescriptionPainted() {
	return descriptionPainted;
}
public boolean isDirty() {
	return dirty;
}
public boolean isHeaderPainted() {
	return headerPainted;
}
public boolean isReadOnly() {
	return readOnly;
}
public boolean isTitleAsHyperlink() {
	return titleAsHyperlink;
}
public void sectionChanged(FormSection source, int changeType, Object changeObject) {}
public void setAddSeparator(boolean newAddSeparator) {
	addSeparator = newAddSeparator;
}
public void setDescription(java.lang.String newDescription) {
	description = newDescription;
	if (descriptionLabel!=null) descriptionLabel.setText(newDescription);
}
public void setDescriptionPainted(boolean newDescriptionPainted) {
	descriptionPainted = newDescriptionPainted;
}
public void setDirty(boolean newDirty) {
	dirty = newDirty;
}
public void setFocus() {
}
public void setHeaderColorKey(java.lang.String newHeaderColorKey) {
	headerColorKey = newHeaderColorKey;
}
public void setHeaderPainted(boolean newHeaderPainted) {
	headerPainted = newHeaderPainted;
}
public void setHeaderText(java.lang.String newHeaderText) {
	headerText = newHeaderText;
	if (header!=null) header.setText(headerText);
}
public void setHeightHint(int newHeightHint) {
	heightHint = newHeightHint;
}
void setManager(SectionChangeManager manager) {
	this.sectionManager = manager;
}
public void setReadOnly(boolean newReadOnly) {
	readOnly = newReadOnly;
}
public void setTitleAsHyperlink(boolean newTitleAsHyperlink) {
	//titleAsHyperlink = newTitleAsHyperlink;
}
public void setWidthHint(int newWidthHint) {
	widthHint = newWidthHint;
}
public void titleActivated() {
}
public void update() {}
}
