/*
 * Created on Jan 29, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class PDEMasterDetailsBlock extends MasterDetailsBlock {
	private PDEFormPage page;
	
	public PDEMasterDetailsBlock(PDEFormPage page) {
		this.page = page;
	}
	
	public PDEFormPage getPage() {
		return page;
	}
	
	protected abstract PDESection createMasterSection(IManagedForm managedForm, Composite parent);

	protected void createMasterPart(final IManagedForm managedForm,
			Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();
		PDESection section = createMasterSection(managedForm, parent);
		Section sc = section.getSection();
		sc.marginWidth = 5;
		sc.marginHeight = 5;
	}
	protected void createToolBarActions(IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();
	
		Action haction = new Action("hor", Action.AS_RADIO_BUTTON) {
			public void run() {
				sashForm.setOrientation(SWT.HORIZONTAL);
				form.reflow(true);
			}
		};
		haction.setChecked(true);
		haction.setToolTipText("Horizontal orientation");
		haction.setImageDescriptor(PDEPluginImages.DESC_HORIZONTAL);
		haction.setHoverImageDescriptor(PDEPluginImages.DESC_HORIZONTAL_HOVER);
		haction.setDisabledImageDescriptor(PDEPluginImages.DESC_HORIZONTAL_DISABLED);

		Action vaction = new Action("ver", Action.AS_RADIO_BUTTON) {
			public void run() {
				sashForm.setOrientation(SWT.VERTICAL);
				form.reflow(true);
			}
		};
		vaction.setChecked(false);
		vaction.setToolTipText("Vertical orientation");
		vaction.setImageDescriptor(PDEPluginImages.DESC_VERTICAL);
		vaction.setHoverImageDescriptor(PDEPluginImages.DESC_VERTICAL_HOVER);
		vaction.setDisabledImageDescriptor(PDEPluginImages.DESC_VERTICAL_DISABLED);
		form.getToolBarManager().add(haction);
		form.getToolBarManager().add(vaction);
	}
}