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
public class ExtensionPointsPage extends PDEFormPage {
	private ExtensionPointsBlock block;
	public class ExtensionPointsBlock extends PDEMasterDetailsBlock {
		public ExtensionPointsBlock() {
			super(ExtensionPointsPage.this);
		}
		protected PDESection createMasterSection(ManagedForm managedForm,
				Composite parent) {
			return new ExtensionPointsSection(getPage(), parent);
		}
		protected void registerPages(DetailsPart detailsPart) {
			detailsPart.registerPage(DummyExtensionPoint.class, new
					ExtensionPointDetails());
		}
	}
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public ExtensionPointsPage(FormEditor editor) {
		super(editor, "ex-points", "Extension Points");
		block = new ExtensionPointsBlock();
	}
	protected void createFormContent(ManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Extension Points");
		block.createContent(managedForm);
	}
}