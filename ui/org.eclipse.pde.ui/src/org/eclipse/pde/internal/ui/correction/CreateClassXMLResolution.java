package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeValue;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeWizard;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class CreateClassXMLResolution extends AbstractXMLMarkerResolution {

	public CreateClassXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	
	// create class code copied from org.eclipse.pde.internal.ui.editor.plugin.rows.ClassAttributeRow
	protected void createChange(IPluginModelBase model) {
		Object object = findNode(model);
		if (!(object instanceof PluginAttribute))
			return;
		
		PluginAttribute attr = (PluginAttribute)object;
		String name = attr.getValue();
		name = trimNonAlphaChars(name).replace('$', '.');
		IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement result = null;
				if (name.length() > 0)
					result = javaProject.findType(name);
				if (result != null) {
					JavaUI.openInEditor(result);
				} else {
					JavaAttributeValue value = new JavaAttributeValue(project, model, getAttribute(attr), name);
					JavaAttributeWizard wizard = new JavaAttributeWizard(value);
					WizardDialog dialog = new WizardDialog(PDEPlugin
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					int dResult = dialog.open();
					if (dResult == Window.OK) {
						name = wizard.getClassNameWithArgs();
						result = javaProject.findType(name);
						if (result != null)
							JavaUI.openInEditor(result);
					}
				}
			} else {
				IResource resource = project.findMember(new Path(name));
				if (resource != null && resource instanceof IFile) {
					IWorkbenchPage page = PDEPlugin.getActivePage();
					IDE.openEditor(page, (IFile) resource, true);
				} else {
					JavaAttributeValue value = new JavaAttributeValue(project, model, getAttribute(attr), name);
					JavaAttributeWizard wizard = new JavaAttributeWizard(value);
					WizardDialog dialog = new WizardDialog(PDEPlugin
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					int dResult = dialog.open();
					if (dResult == Window.OK) {
						String newValue = wizard.getClassName();
						name = newValue.replace('.', '/') + ".java"; //$NON-NLS-1$
						resource = project.findMember(new Path(name));
						if (resource != null && resource instanceof IFile) {
							IWorkbenchPage page = PDEPlugin.getActivePage();
							IDE.openEditor(page, (IFile) resource, true);
						}
					}
				}
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		} catch (JavaModelException e) {
			// nothing
			Display.getCurrent().beep();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
			if (!name.equals(attr.getValue())) {
				attr.getEnclosingElement().setXMLAttribute(attr.getName(), name);
			}
		}
	}

	private ISchemaAttribute getAttribute(PluginAttribute attr) {
		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
		IDocumentNode element = attr.getEnclosingElement();
		IPluginExtension extension = null;
		while (element.getParentNode() != null) {
			if (element instanceof IPluginExtension) {
				extension = (IPluginExtension)element;
				break;
			}
			element = element.getParentNode();
		}
		if (extension == null)
			return null;
		
		ISchema schema = registry.getSchema(extension.getPoint());
		ISchemaElement schemaElement = schema.findElement(attr.getEnclosingElement().getXMLTagName());
		if (schemaElement == null)
			return null;
		return schemaElement.getAttribute(attr.getName());
	}

	public String getDescription() {
		return getLabel();
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.CreateClassXMLResolution_label, getNameOfNode());
	}
	
	private String trimNonAlphaChars(String value) {
		value = value.trim();
		while (value.length() > 0 && !Character.isLetter(value.charAt(0)))
			value = value.substring(1, value.length());
		int loc = value.indexOf(":"); //$NON-NLS-1$
		if (loc != -1 && loc > 0)
			value = value.substring(0, loc);
		else if (loc == 0)
			value = ""; //$NON-NLS-1$
		return value;
	}
}
