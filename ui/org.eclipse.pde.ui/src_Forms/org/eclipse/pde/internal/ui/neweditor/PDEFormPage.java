/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.views.properties.IPropertySheetPage;
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
		//form.setBackgroundImage(PDEPlugin.getDefault().getLabelProvider().get(
		//		PDEPluginImages.DESC_FORM_BANNER));
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
	public IBaseModel getModel() {
		return getPDEEditor().getAggregateModel();
	}
	public void contextMenuAboutToShow(IMenuManager menu) {
	}
	
	protected Control getFocusControl() {
		Control control = getManagedForm().getForm();
		if (control == null || control.isDisposed())
			return null;
		Display display = control.getDisplay();
		Control focusControl = display.getFocusControl();
		if (focusControl == null || focusControl.isDisposed())
			return null;
		return focusControl;
	}
	public boolean performGlobalAction(String actionId) {
		Control focusControl = getFocusControl();
		if (focusControl == null)
			return false;

		if (canPerformDirectly(actionId, focusControl))
			return true;
		PDESection targetSection = getFocusSection();
		if (targetSection!=null)
			return targetSection.doGlobalAction(actionId);
		return false;
	}

	public boolean canPaste(Clipboard clipboard) {
		PDESection targetSection = getFocusSection();
		if (targetSection != null) {
			return targetSection.canPaste(clipboard);
		}
		return false;
	}
	
	private PDESection getFocusSection() {
		Control focusControl = getFocusControl();
		if (focusControl == null)
			return null;
		Composite parent = focusControl.getParent();
		PDESection targetSection = null;
		while (parent != null) {
			Object data = parent.getData("part");
			if (data != null && data instanceof PDESection) {
				targetSection = (PDESection) data;
				break;
			}
			parent = parent.getParent();
		}
		return targetSection;
	}
	public IPropertySheetPage getPropertySheetPage() {
		return null;
	}
	protected boolean canPerformDirectly(String id, Control control) {
		if (control instanceof Text) {
			Text text = (Text) control;
			if (id.equals(ActionFactory.CUT.getId())) {
				text.cut();
				return true;
			}
			if (id.equals(ActionFactory.COPY.getId())) {
				text.copy();
				return true;
			}
			if (id.equals(ActionFactory.PASTE.getId())) {
				text.paste();
				return true;
			}
			if (id.equals(ActionFactory.SELECT_ALL.getId())) {
				text.selectAll();
				return true;
			}
			if (id.equals(ActionFactory.DELETE.getId())) {
				int count = text.getSelectionCount();
				if (count == 0) {
					int caretPos = text.getCaretPosition();
					text.setSelection(caretPos, caretPos + 1);
				}
				text.insert("");
				return true;
			}
		}
		return false;
	}	
}