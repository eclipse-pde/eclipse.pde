/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ExtensionsPage extends PDEFormPage {
	public static final String PAGE_ID = "extensions";
	private ExtensionsBlock block;
	public class ExtensionsBlock extends PDEMasterDetailsBlock {
		public ExtensionsBlock() {
			super(ExtensionsPage.this);
		}
		protected PDESection createMasterSection(ManagedForm managedForm,
				Composite parent) {
			return new ExtensionsSection(getPage(), parent);
		}
		protected void registerPages(DetailsPart detailsPart) {
			//detailsPart.registerPage(TypeOne.class, new
			// TypeOneDetailsPage());
			//detailsPart.registerPage(TypeTwo.class, new
			// TypeTwoDetailsPage());
		}
	}
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public ExtensionsPage(FormEditor editor) {
		super(editor, PAGE_ID, "Extensions");
		block = new ExtensionsBlock();
	}
	protected void createFormContent(ManagedForm managedForm) {
		super.createFormContent(managedForm);
		Form form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Extensions");
		block.createContent(managedForm);
	}
}