/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.swt.*;
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
public class ExtensionDetails extends PDEDetails {
	private IPluginExtension input;
	private FormEntry id;
	private FormEntry name;
	private FormEntry point;
	private FormText rtext;

	private static final String RTEXT_DATA =
		PDEPlugin.getResourceString("ExtensionDetails.extensionPointLinks"); //$NON-NLS-1$
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
		FormToolkit toolkit = getManagedForm().getToolkit();
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
		//TODO translate strings in this file
		section.setText(PDEPlugin.getResourceString("ExtensionDetails.title")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString("ExtensionDetails.desc")); //$NON-NLS-1$
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

		id = new FormEntry(client, toolkit, PDEPlugin.getResourceString("ExtensionDetails.id"), null, false); //$NON-NLS-1$
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
		
		name = new FormEntry(client, toolkit, PDEPlugin.getResourceString("ExtensionDetails.name"), null, false); //$NON-NLS-1$
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
		
		point = new FormEntry(client, toolkit, PDEPlugin.getResourceString("ExtensionDetails.point"), null, false); //$NON-NLS-1$
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
		rtext.setImage("desc", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_DOC_SECTION_OBJ)); //$NON-NLS-1$
		rtext.setImage("search", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PSEARCH_OBJ));		 //$NON-NLS-1$
		rtext.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("search")){ //$NON-NLS-1$
					FindDeclarationsAction findDeclarationsAction = new FindDeclarationsAction(input);
					findDeclarationsAction.run();
				} else {
					if (input == null || input.getPoint() == null)
						return;
					IPluginExtensionPoint point = PDECore.getDefault().findExtensionPoint(input.getPoint());
					if (point != null){
						ShowDescriptionAction showDescAction = new ShowDescriptionAction(point);
						showDescAction.run();
					} else {
						showNoExtensionPointMessage();
					}
				}
			}
		});
		rtext.setText(RTEXT_DATA, true, false);
		id.setEditable(isEditable());
		name.setEditable(isEditable());
		point.setEditable(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		IPluginModelBase model = (IPluginModelBase)getPage().getModel();
		model.addModelChangedListener(this);
		markDetailsPart(section);
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
	public void cancelEdit() {
		id.cancelEdit();
		name.cancelEdit();
		point.cancelEdit();
		super.cancelEdit();
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
	
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase)getPage().getModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType()==IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj.equals(input))
				refresh();
		}
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
		PDEFormPage page = (PDEFormPage)getManagedForm().getContainer();
		page.getPDEEditor().fireSaveNeeded(getContextId(), false);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.IContextPart#getContextId()
	 */
	public String getContextId() {
		return PluginInputContext.CONTEXT_ID;
	}
	public PDEFormPage getPage() {
		return (PDEFormPage)getManagedForm().getContainer();
	}
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}
	private void showNoExtensionPointMessage() {
		String title = PDEPlugin.getResourceString("ExtensionDetails.noPoint.title"); //$NON-NLS-1$
		String message = PDEPlugin.getFormattedMessage("ShowDescriptionAction.noPoint.desc",input.getPoint()); //$NON-NLS-1$
		
		MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), title, message);
	}
}