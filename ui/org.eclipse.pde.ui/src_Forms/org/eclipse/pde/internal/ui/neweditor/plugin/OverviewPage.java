/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormPage;
import org.eclipse.pde.internal.ui.newparts.ILinkLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class OverviewPage extends PDEFormPage {
	public static final String PAGE_ID = "overview";
	private Object[] objects = new Object[]{new DummyObject("Object 1"),
			new DummyObject("Object 2"), new DummyObject("Object 3")};
	private static class DummyObject {
		private String name;
		public DummyObject(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
	}
	class OverviewProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return objects;
		}
	}
	class OverviewLabelProvider extends LabelProvider
			implements
				ILinkLabelProvider {
		public String getText(Object obj) {
			return ((DummyObject) obj).getName();
		}
		public Image getImage(Object obj) {
			return null;
		}
		public String getToolTipText(Object obj) {
			return getText(obj);
		}
		public String getStatusText(Object obj) {
			return getText(obj);
		}
	}
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, "Overview");
	}
	protected void createFormContent(ManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Overview");
		fillBody(managedForm, toolkit);
	}
	private void fillBody(ManagedForm managedForm, FormToolkit toolkit) {
		ScrolledForm form = managedForm.getForm();
		Composite body = form.getBody();
		ColumnLayout layout = new ColumnLayout();
		body.setLayout(layout);
		OverviewProvider provider = new OverviewProvider();
		OverviewLabelProvider lprovider = new OverviewLabelProvider();
		LinkSection extensions = new LinkSection(this, body, Section.TWISTIE
				| Section.EXPANDED | Section.DESCRIPTION);
		// Extension section
		extensions.getSection().setText("Extensions");
		toolkit.createCompositeSeparator(extensions.getSection());
		extensions.getSection().setDescription("Extensions description");
		extensions.setContentProvider(provider);
		extensions.setLabelProvider(lprovider);
		extensions.setMorePageId(ExtensionsPage.PAGE_ID);
		extensions.setLinkNumberLimit(5);
		extensions.refresh();
		managedForm.addPart(extensions);
	}
}