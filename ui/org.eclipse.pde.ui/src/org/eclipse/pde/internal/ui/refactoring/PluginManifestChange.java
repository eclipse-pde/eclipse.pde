/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.plugin.FragmentModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.core.text.plugin.PluginNode;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

public class PluginManifestChange {
	
	public static Change createRenameChange(IFile file, IJavaElement[] affectedElements, String[] newNames, IProgressMonitor monitor) 
			throws CoreException {
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
			
			IDocument document = buffer.getDocument();
			
			try {
				PluginModelBase model;
				if ("fragment.xml".equals(file.getName())) //$NON-NLS-1$
					model = new FragmentModel(document, false);
				else
					model = new PluginModel(document, false);

				model.load();
				if (!model.isLoaded())
					return null;
				
				MultiTextEdit multiEdit = new MultiTextEdit();
				
				for (int i = 0; i < affectedElements.length; i++) {
					if (model instanceof PluginModel) {
						PluginNode plugin = (PluginNode)model.getPluginBase();
						IDocumentAttribute attr = plugin.getDocumentAttribute("class"); //$NON-NLS-1$
						TextEdit edit = createTextEdit(attr, affectedElements[i], newNames[i]);
						if (edit != null)
							multiEdit.addChild(edit);					
					}
					
					SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
					IPluginExtension[] extensions = model.getPluginBase().getExtensions();
					for (int j = 0; j < extensions.length; j++) {
						ISchema schema = registry.getSchema(extensions[j].getPoint());
						if (schema != null)
							addExtensionAttributeEdit(schema, extensions[j], multiEdit, affectedElements[i], newNames[i]);
					}
				}
				
				if (multiEdit.hasChildren()) {
					TextFileChange change = new TextFileChange("", file); //$NON-NLS-1$
					change.setEdit(multiEdit);
					return change;
				}
			} catch (CoreException e) {
				return null;
			}	
			return null;
		} finally {
			manager.disconnect(file.getFullPath(), monitor);
		}	
	}
	
	private static void addExtensionAttributeEdit(ISchema schema, IPluginParent parent, MultiTextEdit multi, IJavaElement element, String newName) {
		IPluginObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				IPluginAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					IPluginAttribute attr = attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.getKind() == IMetaAttribute.JAVA) {
						IDocumentAttribute docAttr = (IDocumentAttribute)attr;
						TextEdit edit = createTextEdit(docAttr, element, newName);
						if (edit != null)
							multi.addChild(edit);
					}
				}
			}
			addExtensionAttributeEdit(schema, child, multi, element, newName);
		}
	}
	
	private static TextEdit createTextEdit(IDocumentAttribute attr, IJavaElement element, String newName) {
		if (attr == null)
			return null;
		
		String oldName = (element instanceof IType) 
							? ((IType)element).getFullyQualifiedName('$') 
							: element.getElementName();
		String value = attr.getAttributeValue();
		if (oldName.equals(value) || isGoodMatch(value, oldName, element instanceof IPackageFragment)) {
			int offset = attr.getValueOffset();
			if (offset >= 0)
				return new ReplaceEdit(offset, oldName.length(), newName);
		}
		return null;
	}
	
	private static boolean isGoodMatch(String value, String oldName, boolean isPackage) {
		if (value == null || value.length() <= oldName.length())
			return false;
		boolean goodLengthMatch = isPackage 
									? value.lastIndexOf('.') <= oldName.length() 
									: value.charAt(oldName.length()) == '$';
		return value.startsWith(oldName) && goodLengthMatch; 
	}
}
