/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
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
	public static final String PAGE_ID = "ex-points";
	private ExtensionPointsSection extensionPointsSection;
	private ExtensionPointsBlock block;
	public class ExtensionPointsBlock extends PDEMasterDetailsBlock {
		public ExtensionPointsBlock() {
			super(ExtensionPointsPage.this);
		}
		protected PDESection createMasterSection(IManagedForm managedForm,
				Composite parent) {
			extensionPointsSection = new ExtensionPointsSection(getPage(), parent);
			return extensionPointsSection;
		}
		protected void registerPages(DetailsPart detailsPart) {
			detailsPart.setPageProvider(new IDetailsPageProvider() {
				public Object getPageKey(Object object) {
					if (object instanceof IPluginExtensionPoint)
						return IPluginExtensionPoint.class;
					return object.getClass();
				}
				public IDetailsPage getPage(Object key) {
					if (key.equals(IPluginExtensionPoint.class))
						return new ExtensionPointDetails();
					return null;
				}
			});
		}
	}
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public ExtensionPointsPage(FormEditor editor) {
		super(editor, PAGE_ID, "Extension Points");
		block = new ExtensionPointsBlock();
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Extension Points");
		block.createContent(managedForm);
		extensionPointsSection.fireSelection();
	}
}