/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.neweditor.plugin.dummy.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ExtensionElementDetails implements IDetailsPage {
	private DummyExtensionElement input;
	private ISchemaElement schemaElement;
	private IManagedForm managedForm;
	private ArrayList rows;
	/**
	 *  
	 */
	public ExtensionElementDetails(ISchemaElement schemaElement) {
		this.schemaElement = schemaElement;
		rows = new ArrayList();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		this.managedForm = form;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = 0;
		layout.leftMargin = 5;
		layout.rightMargin = 0;
		layout.bottomMargin = 0;
		parent.setLayout(layout);
		FormToolkit toolkit = managedForm.getToolkit();
		Section section = toolkit.createSection(parent, Section.DESCRIPTION);
		section.marginHeight = 5;
		section.marginWidth = 5;
		section.setText("Extension Details");
		section.setDescription("Set the properties of the selected element.");
		TableWrapData td = new TableWrapData(TableWrapData.FILL,
				TableWrapData.TOP);
		td.grabHorizontal = true;
		section.setLayoutData(td);
		toolkit.createCompositeSeparator(section);
		Composite client = toolkit.createComposite(section);
		GridLayout glayout = new GridLayout();
		glayout.marginWidth = glayout.marginHeight = 0;
		client.setLayout(glayout);
		ISchemaAttribute atts[] = schemaElement.getAttributes();
		int span = 2;
		// Compute horizontal span
		for (int i = 0; i < atts.length; i++) {
			if (atts[i].getKind() == ISchemaAttribute.JAVA
					|| atts[i].getKind() == ISchemaAttribute.RESOURCE) {
				span = 3;
				break;
			}
		}
		glayout.numColumns = span;
		// Add required attributes first
		for (int i = 0; i < atts.length; i++) {
			if (atts[i].getUse() == ISchemaAttribute.REQUIRED)
				rows.add(createAttributeRow(atts[i], client, toolkit, span));
		}
		// Add the rest
		for (int i = 0; i < atts.length; i++) {
			if (atts[i].getUse() != ISchemaAttribute.REQUIRED)
				rows.add(createAttributeRow(atts[i], client, toolkit, span));
		}
		createSpacer(toolkit, client, span);
		toolkit.paintBordersFor(section);
		section.setClient(client);
	}
	private ExtensionAttributeRow createAttributeRow(ISchemaAttribute att,
			Composite parent, FormToolkit toolkit, int span) {
		ExtensionAttributeRow row;
		if (att.getKind() == ISchemaAttribute.JAVA)
			row = new ClassAttributeRow(att);
		else
		if (att.getKind() == ISchemaAttribute.RESOURCE)
			row = new ResourceAttributeRow(att);
		else {
			ISchemaSimpleType type = att.getType();
			if (type.getName().equals("boolean"))
				row = new BooleanAttributeRow(att);
			else {
				ISchemaRestriction restriction = type.getRestriction();
				if (restriction != null)
					row = new ChoiceAttributeRow(att);
				else
					row = new TextAttributeRow(att);
			}
		}
		row.createContents(parent, toolkit, span);
		return row;
	}
	private void createSpacer(FormToolkit toolkit, Composite parent, int span) {
		Label spacer = toolkit.createLabel(parent, "");
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		spacer.setLayoutData(gd);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void inputChanged(IStructuredSelection selection) {
		if (selection.size() == 1) {
			input = (DummyExtensionElement) selection.getFirstElement();
		} else
			input = null;
		update();
	}
	private void update() {
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow) rows.get(i);
			row.setInput(input);
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit() {
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		if (rows.size() > 0)
			((ExtensionAttributeRow) rows.get(0)).setFocus();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#dispose()
	 */
	public void dispose() {
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow) rows.get(i);
			row.dispose();
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#isDirty()
	 */
	public boolean isDirty() {
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow) rows.get(i);
			if (row.isDirty())
				return true;
		}
		return false;
	}
	public boolean isStale() {
		// TODO need to implement this
		return false;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		update();
	}
}
