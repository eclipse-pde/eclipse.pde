/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;
import org.eclipse.pde.core.IModel;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.help.WorkbenchHelp;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public abstract class PDEFormPage extends FormPage {
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public PDEFormPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	protected void createFormContent(IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();
		form.setBackgroundImage(PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_FORM_BANNER));
		final String href = getHelpResource();
		if (href != null) {
			IToolBarManager manager = form.getToolBarManager();
			Action helpAction = new Action("help") {
				public void run() {
					BusyIndicator.showWhile(form.getDisplay(), new Runnable() {
						public void run() {
							WorkbenchHelp.displayHelpResource(href);
						}
					});
				}
			};
			helpAction.setToolTipText("Help");
			helpAction.setImageDescriptor(PlatformUI.getWorkbench()
					.getSharedImages().getImageDescriptor(
							ISharedImages.IMG_OBJS_INFO_TSK));
			manager.add(helpAction);
			form.updateToolBar();
		}
	}
	public PDEFormEditor getPDEEditor() {
		return (PDEFormEditor) getEditor();
	}
	protected String getHelpResource() {
		return null;
	}
	public IModel getModel() {
		return getPDEEditor().getAggregateModel();
	}
	public void contextMenuAboutToShow(IMenuManager menu) {
	}
}