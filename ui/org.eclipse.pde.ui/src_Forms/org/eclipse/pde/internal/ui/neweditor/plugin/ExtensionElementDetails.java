/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.util.ArrayList;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.plugin.rows.*;
import org.eclipse.swt.SWT;
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
public class ExtensionElementDetails extends AbstractFormPart implements IDetailsPage, IContextPart {
	private IPluginElement input;
	private ISchemaElement schemaElement;
	private ArrayList rows;
	private Section section;
	/**
	 *  
	 */
	public ExtensionElementDetails(ISchemaElement schemaElement) {
		this.schemaElement = schemaElement;
		rows = new ArrayList();
	}
	public String getContextId() {
		return PluginInputContext.CONTEXT_ID;
	}
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}
	public PDEFormPage getPage() {
		return (PDEFormPage)managedForm.getContainer();
	}
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
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
		section = toolkit.createSection(parent, Section.TITLE_BAR|Section.DESCRIPTION);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.marginHeight = 5;
		section.marginWidth = 5;
		section.setText("Extension Element Details");
		section.setDescription("Set the properties of the selected element.");
		TableWrapData td = new TableWrapData(TableWrapData.FILL,
				TableWrapData.TOP);
		td.grabHorizontal = true;
		section.setLayoutData(td);
		//toolkit.createCompositeSeparator(section);
		Composite client = toolkit.createComposite(section);
		GridLayout glayout = new GridLayout();
		boolean paintedBorder = toolkit.getBorderStyle()!=SWT.BORDER;
		glayout.marginWidth = glayout.marginHeight = 2;//paintedBorder?2:0;
		if (paintedBorder)
			glayout.verticalSpacing = 7;
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
		toolkit.paintBordersFor(client);
		section.setClient(client);
	}
	private ExtensionAttributeRow createAttributeRow(ISchemaAttribute att,
			Composite parent, FormToolkit toolkit, int span) {
		ExtensionAttributeRow row;
		if (att.getKind() == ISchemaAttribute.JAVA)
			row = new ClassAttributeRow(this, att);
		else
		if (att.getKind() == ISchemaAttribute.RESOURCE)
			row = new ResourceAttributeRow(this, att);
		else {
			ISchemaSimpleType type = att.getType();
			if (type.getName().equals("boolean"))
				row = new BooleanAttributeRow(this, att);
			else {
				ISchemaRestriction restriction = type.getRestriction();
				if (restriction != null)
					row = new ChoiceAttributeRow(this, att);
				else
					row = new TextAttributeRow(this, att);
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
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection)selection;
		if (ssel.size() == 1) {
			input = (IPluginElement) ssel.getFirstElement();
		} else
			input = null;
		update();
	}
	private void update() {
		updateDescription();
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow) rows.get(i);
			row.setInput(input);
		}
	}
	private void updateDescription() {
		if (input!=null) {
			String iname = input.getName();
			section.setDescription("Set the properties of '"+iname+"'.");
		}
		else {
			section.setDescription("Set the properties of the selected element.");
		}
		section.layout();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit(boolean onSave) {
		for (int i=0; i<rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow)rows.get(i);
			row.commit();
		}
		super.commit(onSave);
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
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		update();
		super.refresh();
	}
}