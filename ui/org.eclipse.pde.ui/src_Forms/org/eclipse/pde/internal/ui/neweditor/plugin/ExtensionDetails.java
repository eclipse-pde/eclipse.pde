/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ExtensionDetails extends AbstractFormPart implements IDetailsPage, IContextPart {
	private IPluginExtension input;
	private FormEntry id;
	private FormEntry name;
	private FormEntry point;
	private FormText rtext;

	private static final String RTEXT_DATA =
		"<form>"+
		"<p><img href=\"search\"/> <a href=\"search\">Find declaring extension point</a></p>"+		
		"<p><img href=\"desc\"/> <a href=\"desc\">Open extension point description</a></p>"+
		"</form>";
	/**
	 * 
	 */
	public ExtensionDetails() {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		TableWrapLayout layout = new TableWrapLayout();
		FormToolkit toolkit = managedForm.getToolkit();
		boolean paintedBorder = toolkit.getBorderStyle()!=SWT.BORDER;
		layout.topMargin = 0;
		layout.leftMargin = 5;
		layout.rightMargin = 0;
		layout.bottomMargin = 0;
		parent.setLayout(layout);


		Section section = toolkit.createSection(parent, Section.TITLE_BAR|Section.DESCRIPTION);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.marginHeight = 5;		
		section.marginWidth = 5;
		section.setText("Extension Details");
		section.setDescription("Set the properties of the selected extension.");
		TableWrapData td = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP);
		td.grabHorizontal = true;
		section.setLayoutData(td);
		//toolkit.createCompositeSeparator(section);
		Composite client = toolkit.createComposite(section);
		GridLayout glayout = new GridLayout();
		glayout.marginWidth = glayout.marginHeight = 2;//paintedBorder?2:0;
		glayout.numColumns = 2;
		if (paintedBorder) glayout.verticalSpacing = 7;
		client.setLayout(glayout);
		
		GridData gd = new GridData();
		gd.horizontalSpan = 2;

		id = new FormEntry(client, toolkit, "Id:", null, false);
		id.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (input!=null)
					try {
						input.setId(id.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		
		name = new FormEntry(client, toolkit, "Name:", null, false);
		name.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (input!=null)
					try {
						input.setName(name.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		
		point = new FormEntry(client, toolkit, "Point:", null, false);
		point.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (input!=null)
					try {
						input.setPoint(point.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		
		createSpacer(toolkit, client, 2);
		
		rtext = toolkit.createFormText(parent, true);
		td = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP);
		td.grabHorizontal = true;
		td.indent = 10;
		rtext.setLayoutData(td);
		rtext.setImage("desc", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_DOC_SECTION_OBJ));
		rtext.setImage("search", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PSEARCH_OBJ));		
		rtext.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				System.out.println("Link active: "+e.getHref());
			}
		});
		rtext.setText(RTEXT_DATA, true, false);
		id.setEditable(isEditable());
		name.setEditable(isEditable());
		point.setEditable(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
	}
	
	private void createSpacer(FormToolkit toolkit, Composite parent, int span) {
		Label spacer = toolkit.createLabel(parent, "");
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		spacer.setLayoutData(gd);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection)selection;
		if (ssel.size()==1) {
			input = (IPluginExtension)ssel.getFirstElement();
		}
		else
			input = null;
		update();
	}
	
	private void update() {
		id.setValue(input!=null?input.getId():null, true);
		name.setValue(input!=null?input.getName():null, true);
		point.setValue(input!=null?input.getPoint():null, true);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit(boolean onSave) {
		id.commit();
		name.commit();
		point.commit();
		super.commit(onSave);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		id.getText().setFocus();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		update();
		super.refresh();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.IContextPart#fireSaveNeeded()
	 */
	public void fireSaveNeeded() {
		markDirty();
		PDEFormPage page = (PDEFormPage)managedForm.getContainer();
		page.getPDEEditor().fireSaveNeeded(getContextId(), false);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.IContextPart#getContextId()
	 */
	public String getContextId() {
		return PluginInputContext.CONTEXT_ID;
	}
	public PDEFormPage getPage() {
		return (PDEFormPage)managedForm.getContainer();
	}
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}
}