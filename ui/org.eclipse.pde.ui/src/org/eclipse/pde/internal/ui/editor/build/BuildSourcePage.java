/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.swt.graphics.*;
/**
 * @author melhem
 *  
 */
public class BuildSourcePage extends KeyValueSourcePage {
	class BuildOutlineContentProvider extends DefaultContentProvider
			implements
				ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			return false;
		}
		public Object getParent(Object child) {
			if (child instanceof IBuildEntry)
				return ((IBuildEntry) child).getModel();
			return null;
		}
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				IBuildModel model = (IBuildModel) parent;
				IBuild build = model.getBuild();
				return build.getBuildEntries();
			}
			return new Object[0];
		}
	}
	class BuildLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof IBuildEntry) {
				return ((IBuildEntry) obj).getName();
			}
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			if (obj instanceof IBuildEntry)
				return PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_BUILD_VAR_OBJ);
			return null;
		}
	}
	public BuildSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	
	protected ILabelProvider createOutlineLabelProvider() {
		return new BuildLabelProvider();
	}
	
	protected ITreeContentProvider createOutlineContentProvider() {
		return new BuildOutlineContentProvider();
	}
}