/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
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
	private ILinkLabelProvider labelProvider;
	//Some dummy object to test the page until
	// a real model comes along
	private Object[] exobjects = new Object[]{
			new DummyObject(ExtensionsPage.PAGE_ID, "Extension 1"),
			new DummyObject(ExtensionsPage.PAGE_ID, "Extension 2"),
			new DummyObject(ExtensionsPage.PAGE_ID, "Longer extension 3"),
			new DummyObject(ExtensionsPage.PAGE_ID, "Extension 4"),
			new DummyObject(ExtensionsPage.PAGE_ID, "Extension 5"),
			new DummyObject(ExtensionsPage.PAGE_ID, "Extension 6"),
			new DummyObject(ExtensionsPage.PAGE_ID, "Extension 7"),
			new DummyObject(ExtensionsPage.PAGE_ID, "Extension 8"),};
	private Object[] rtobjects = new Object[]{
			new DummyObject(RuntimePage.PAGE_ID, "xyz.jar"),
			new DummyObject(RuntimePage.PAGE_ID, "foo.jar"),
			new DummyObject(RuntimePage.PAGE_ID, "bar.jar")};
	private Object[] epobjects = new Object[]{
			new DummyObject(ExtensionPointsPage.PAGE_ID, "Expoint 1"),
			new DummyObject(ExtensionPointsPage.PAGE_ID, "Expoint 2"),
			new DummyObject(ExtensionPointsPage.PAGE_ID, "Longer Expoint 3")};
	private Object[] depobjects = new Object[]{
			new DummyObject(DependenciesPage.PAGE_ID, "Import 1"),
			new DummyObject(DependenciesPage.PAGE_ID, "Import 2"),
			new DummyObject(DependenciesPage.PAGE_ID, "Import 3")};
	private static class DummyObject {
		private String name;
		private String pageId;
		public DummyObject(String pageId, String name) {
			this.name = name;
			this.pageId = pageId;
		}
		public String getName() {
			return name;
		}
		public String getPageId() {
			return pageId;
		}
	}
	// When dummy objects are gone, the provider
	// should hook to the real model and
	// return real objects
	class OverviewProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		private Object[] objects;
		public OverviewProvider(Object[] objects) {
			this.objects = objects;
		}
		public Object[] getElements(Object parent) {
			return objects;
		}
	}
	// When dummy objects are gone, the label provider
	// should pass the object directly to the
	// PDE label provider because it will know
	// how to decorate it
	class OverviewLabelProvider extends LabelProvider
			implements
				ILinkLabelProvider {
		public String getText(Object obj) {
			return ((DummyObject) obj).getName();
		}
		public Image getImage(Object obj) {
			String pageId = ((DummyObject) obj).getPageId();
			PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
			if (pageId.equals(ExtensionsPage.PAGE_ID))
				return lp.get(PDEPluginImages.DESC_EXTENSION_OBJ);
			if (pageId.equals(RuntimePage.PAGE_ID))
				return lp.get(PDEPluginImages.DESC_RUNTIME_OBJ);
			if (pageId.equals(DependenciesPage.PAGE_ID))
				return lp.get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ);
			if (pageId.equals(ExtensionPointsPage.PAGE_ID))
				return lp.get(PDEPluginImages.DESC_EXT_POINT_OBJ);
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
		labelProvider = new OverviewLabelProvider();
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
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 10;
		layout.minNumColumns = 1;
		layout.maxNumColumns = 2;
		body.setLayout(layout);
		LinkSection extensions = new LinkSection(this, body, Section.TWISTIE
				| Section.EXPANDED | Section.DESCRIPTION);
		// Extension section
		extensions
				.getSection()
				.setText(
						PDEPlugin
								.getResourceString("ManifestEditor.ExtensionSection.title"));
		extensions
				.getSection()
				.setDescription(
						PDEPlugin
								.getResourceString("ManifestEditor.ExtensionSection.desc"));
		extensions.setContentProvider(new OverviewProvider(exobjects));
		extensions.setMorePageId(ExtensionsPage.PAGE_ID);
		configureLinkSection(managedForm, extensions);
		LinkSection runtime = new LinkSection(this, body, Section.TWISTIE
				| Section.EXPANDED | Section.DESCRIPTION);
		// Runtime section
		runtime
				.getSection()
				.setText(
						PDEPlugin
								.getResourceString("ManifestEditor.RuntimeSection.title"));
		runtime
				.getSection()
				.setDescription(
						PDEPlugin
								.getResourceString("ManifestEditor.RuntimeSection.desc"));
		runtime.setContentProvider(new OverviewProvider(rtobjects));
		runtime.setMorePageId(RuntimePage.PAGE_ID);
		configureLinkSection(managedForm, runtime);
		LinkSection requires = new LinkSection(this, body, Section.TWISTIE
				| Section.EXPANDED | Section.DESCRIPTION);
		// Import section
		requires
				.getSection()
				.setText(
						PDEPlugin
								.getResourceString("ManifestEditor.RequiresSection.title"));
		requires
				.getSection()
				.setDescription(
						PDEPlugin
								.getResourceString("ManifestEditor.RequiresSection.desc"));
		requires.setContentProvider(new OverviewProvider(depobjects));
		requires.setMorePageId(DependenciesPage.PAGE_ID);
		configureLinkSection(managedForm, requires);
		LinkSection expoints = new LinkSection(this, body, Section.TWISTIE
				| Section.EXPANDED | Section.DESCRIPTION);
		// Extension points section
		expoints
				.getSection()
				.setText(
						PDEPlugin
								.getResourceString("ManifestEditor.ExtensionPointSection.title"));
		expoints
				.getSection()
				.setDescription(
						PDEPlugin
								.getResourceString("ManifestEditor.ExtensionPointSection.desc"));
		expoints.setContentProvider(new OverviewProvider(epobjects));
		expoints.setMorePageId(ExtensionPointsPage.PAGE_ID);
		configureLinkSection(managedForm, expoints);
	}
	private void configureLinkSection(ManagedForm managedForm, LinkSection part) {
		managedForm.getToolkit().createCompositeSeparator(part.getSection());
		part.setLabelProvider(labelProvider);
		part.setLinkNumberLimit(5);
		part.refresh();
		managedForm.addPart(part);
	}
}