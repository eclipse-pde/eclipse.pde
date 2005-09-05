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
import org.eclipse.pde.internal.ui.model.IDocumentAttribute;
import org.eclipse.pde.internal.ui.model.plugin.FragmentModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginModelBase;
import org.eclipse.pde.internal.ui.model.plugin.PluginNode;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

public class PluginManifestChange {

	public static Change createChange(IFile file, IJavaElement element, String newName, IProgressMonitor monitor) 
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
				
				if (model instanceof PluginModel) {
					PluginNode plugin = (PluginNode)model.getPluginBase();
					IDocumentAttribute attr = plugin.getDocumentAttribute("class"); //$NON-NLS-1$
					TextEdit edit = null;
					if (element instanceof IType) 
						edit = createTextEdit(attr, (IType)element, newName);
					else if (element instanceof IPackageFragment)
						edit = createTextEdit(attr, (IPackageFragment)element, newName);
					if (edit != null)
						multiEdit.addChild(edit);					
				}
				
				SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
				IPluginExtension[] extensions = model.getPluginBase().getExtensions();
				for (int i = 0; i < extensions.length; i++) {
					ISchema schema = registry.getSchema(extensions[i].getPoint());
					if (schema != null)
						addExtensionAttributeEdit(schema, extensions[i], multiEdit, element, newName);
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
						TextEdit edit = null;
						if (element instanceof IType) 
							edit = createTextEdit(docAttr, (IType)element, newName);
						else if (element instanceof IPackageFragment)
							edit = createTextEdit(docAttr, (IPackageFragment)element, newName);
						if (edit != null)
							multi.addChild(edit);
					}
				}
			}
			addExtensionAttributeEdit(schema, child, multi, element, newName);
		}
	}
	
	private static TextEdit createTextEdit(IDocumentAttribute attr, IType type, String newName) {
		if (attr == null)
			return null;
		
		String value = attr.getAttributeValue();
		String shortName = type.getElementName();
		String oldName = type.getFullyQualifiedName('$');
		if (value != null && value.startsWith(oldName)) {
			int offset = attr.getValueOffset() + oldName.length() - shortName.length();
			return new ReplaceEdit(offset , shortName.length(), newName);
		}
		return null;
	}

	private static TextEdit createTextEdit(IDocumentAttribute attr, IPackageFragment packageFragment, String newName) {
		if (attr == null)
			return null;
		
		String value = attr.getAttributeValue();
		String oldName = packageFragment.getElementName();
		if (value != null && value.startsWith(oldName) && value.lastIndexOf('.') <= oldName.length()) {
			int offset = attr.getValueOffset();
			if (offset >= 0)
				return new ReplaceEdit(attr.getValueOffset(), oldName.length(), newName);
		}
		return null;
	}

}
