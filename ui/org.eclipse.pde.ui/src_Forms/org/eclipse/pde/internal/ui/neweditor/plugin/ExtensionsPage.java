/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.swt.layout.GridData;
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
	private ExtensionsSection section;
	
	public class ExtensionsBlock extends PDEMasterDetailsBlock implements IDetailsPageProvider {
		public ExtensionsBlock() {
			super(ExtensionsPage.this);
		}
		protected PDESection createMasterSection(IManagedForm managedForm,
				Composite parent) {
			section = new ExtensionsSection(getPage(), parent);
			return section;
		}
		protected void registerPages(DetailsPart detailsPart) {
			// register static page for the extensions
			detailsPart.registerPage(IPluginExtension.class, new ExtensionDetails());
			// register a dynamic provider for elements
			detailsPart.setPageProvider(this);
		}
		public Object getPageKey(Object object) {
			if (object instanceof IPluginExtension)
				return IPluginExtension.class;
			if (object instanceof IPluginElement) {
				return ExtensionsSection.getSchemaElement((IPluginElement)object);
			}
			return object.getClass();
		}
		public IDetailsPage getPage(Object object) {
			if (object instanceof ISchemaElement)
				return new ExtensionElementDetails((ISchemaElement)object);
			return null;
		}
		protected void createToolBarActions(IManagedForm managedForm) {
			final ScrolledForm form = managedForm.getForm();
			Action collapseAction = new Action("col") {
				public void run() {
					section.handleCollapseAll();
				}
			};
			collapseAction.setToolTipText("Collapse All");
			collapseAction.setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
			form.getToolBarManager().add(collapseAction);
			super.createToolBarActions(managedForm);
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
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText("Extensions");
		block.createContent(managedForm);
		BodyTextSection bodyTextSection = new BodyTextSection(this, form.getBody());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		bodyTextSection.getSection().setLayoutData(gd);
		bodyTextSection.getSection().marginWidth = 5;
		managedForm.addPart(bodyTextSection);
		//refire selection
		section.fireSelection();
	}
}