/*
 * Created on Jan 29, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
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
public class ExtensionPointDetails implements IDetailsPage {
	private IManagedForm mform;
	private DummyExtensionPoint input;
	private Text id;
	private Text name;
	private Text schema;
	private RichText rtext;
	
	private static final String RTEXT_DATA =
		"<form><p><img href=\"schema\"/> <a href=\"schema\">Open extension point schema file</a></p>"+
		"<p><img href=\"desc\"/> <a href=\"desc\">Open extension point description</a></p>"+
		"</form>";
	
	public ExtensionPointDetails() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm mform) {
		this.mform = mform;
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

		FormToolkit toolkit = mform.getToolkit();
		Section section = toolkit.createSection(parent, Section.DESCRIPTION);
		section.marginHeight = 5;		
		section.marginWidth = 10;
		section.setText("Extension Point Details");
		section.setDescription("Set the properties of the selected extension point.");
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
		
		toolkit.createLabel(client, "Id:");
		id = toolkit.createText(client, "", SWT.SINGLE);
		id.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (input!=null)
					input.setId(id.getText());
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		gd.widthHint = 10;
		id.setLayoutData(gd);
		
		toolkit.createLabel(client, "Name:");
		name = toolkit.createText(client, "", SWT.SINGLE);
		name.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (input!=null)
					input.setName(name.getText());
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		gd.widthHint = 10;
		name.setLayoutData(gd);
		
		toolkit.createLabel(client, "Schema:");
		schema = toolkit.createText(client, "", SWT.SINGLE);
		schema.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (input!=null) {
					input.setSchema(schema.getText());
					updateRichTextVisibility();
				}
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		gd.widthHint = 10;
		schema.setLayoutData(gd);
		
		createSpacer(toolkit, client, 2);
		
		rtext = toolkit.createRichText(parent, true);
		rtext.setText(RTEXT_DATA, true, false);
		rtext.setVisible(false);
		td = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP);
		td.grabHorizontal = true;
		rtext.setLayoutData(td);
		rtext.setImage("schema", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_SCHEMA_OBJ));
		rtext.setImage("desc", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_DOC_SECTION_OBJ));
		rtext.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				System.out.println("Link active: "+e.getHref());
			}
		});
		
		toolkit.paintBordersFor(section);
		section.setClient(client);
	}
	private void createSpacer(FormToolkit toolkit, Composite parent, int span) {
		Label spacer = toolkit.createLabel(parent, "");
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		spacer.setLayoutData(gd);
	}
	private void update() {
		id.setText(input!=null && input.getId()!=null?input.getId():"");
		name.setText(input!=null && input.getName()!=null?input.getName():"");
		schema.setText(input!=null && input.getSchema()!=null?input.getSchema():"");
		updateRichTextVisibility();
	}
	private void updateRichTextVisibility() {
		rtext.setVisible(schema.getText().length()>0);		
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void inputChanged(IStructuredSelection selection) {
		if (selection.size()==1) {
			input = (DummyExtensionPoint)selection.getFirstElement();
		}
		else
			input = null;
		update();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit() {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		id.setFocus();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#dispose()
	 */
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#isDirty()
	 */
	public boolean isDirty() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		update();
	}
}