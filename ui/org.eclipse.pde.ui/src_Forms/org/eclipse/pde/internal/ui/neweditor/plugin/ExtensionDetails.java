/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.neweditor.EditorEntryAdapter;
import org.eclipse.pde.internal.ui.neweditor.plugin.dummy.DummyExtension;
import org.eclipse.pde.internal.ui.newparts.FormEntry;
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
public class ExtensionDetails implements IDetailsPage {
	private DummyExtension input;
	private IManagedForm managedForm;
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
	 * @see org.eclipse.ui.forms.IDetailsPage#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		this.managedForm = form;
	}
	/* (non-Javadoc)
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
		section.setDescription("Set the properties of the selected extension.");
		TableWrapData td = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP);
		td.grabHorizontal = true;
		section.setLayoutData(td);
		toolkit.createCompositeSeparator(section);
		Composite client = toolkit.createComposite(section);
		GridLayout glayout = new GridLayout();
		glayout.marginWidth = glayout.marginHeight = 0;
		glayout.numColumns = 2;
		client.setLayout(glayout);
		
		GridData gd = new GridData();
		gd.horizontalSpan = 2;

		id = new FormEntry(client, toolkit, "Id:", null, false);
		id.setFormEntryListener(new EditorEntryAdapter() {
			public void textDirty(FormEntry entry) {
				managedForm.markDirty();
			}
			public void textValueChanged(FormEntry entry) {
				if (input!=null)
					input.setProperty("id", id.getValue());
			}
		});
		
		name = new FormEntry(client, toolkit, "Name:", null, false);
		name.setFormEntryListener(new EditorEntryAdapter() {
			public void textDirty(FormEntry entry) {
				managedForm.markDirty();
			}
			public void textValueChanged(FormEntry entry) {
				if (input!=null)
					input.setProperty("name", name.getValue());
			}
		});
		
		point = new FormEntry(client, toolkit, "Point:", null, false);
		point.setFormEntryListener(new EditorEntryAdapter() {
			public void textDirty(FormEntry entry) {
				managedForm.markDirty();
			}
			public void textValueChanged(FormEntry entry) {
				if (input!=null)
					input.setProperty("point", point.getValue());
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
		
		toolkit.paintBordersFor(section);
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
	public void inputChanged(IStructuredSelection selection) {
		if (selection.size()==1) {
			input = (DummyExtension)selection.getFirstElement();
		}
		else
			input = null;
		update();
	}
	
	private void update() {
		id.setValue(input!=null && input.getProperty("id")!=null?input.getProperty("id"):"", true);
		name.setValue(input!=null && input.getProperty("name")!=null?input.getProperty("name"):"", true);
		point.setValue(input!=null && input.getProperty("point")!=null?input.getProperty("point"):"", true);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit() {
		id.commit();
		name.commit();
		point.commit();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		id.getText().setFocus();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#dispose()
	 */
	public void dispose() {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#isDirty()
	 */
	public boolean isDirty() {
		return id.isDirty() || name.isDirty() || point.isDirty();
	}
	public boolean isStale() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		update();
	}
}