/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;

public class Extensions extends AbstractExtensions {

	private static final long serialVersionUID = 1L;

	private Boolean fValid;
	private boolean fIsFragment;

	public Extensions(boolean readOnly) {
		super(readOnly);
	}

	void load(Extensions srcPluginBase) {
		super.load(srcPluginBase);
	}

	void load(String schemaVersion) {
		fSchemaVersion = schemaVersion;
	}

	@Override
	public void reset() {
		super.reset();
		fValid = null;
	}

	@Override
	public boolean isValid() {
		if (fValid == null) {
			fValid = Boolean.valueOf(hasRequiredAttributes());
		}
		return fValid.booleanValue();
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		if (fSchemaVersion != null) {
			writer.println("<?eclipse version=\"" + fSchemaVersion + "\"?>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(fIsFragment ? "<fragment>" : "<plugin>"); //$NON-NLS-1$ //$NON-NLS-2$

		String firstIndent = "   "; //$NON-NLS-1$

		Object[] children = getExtensionPoints();
		if (children.length > 0) {
			writer.println();
		}
		for (Object element : children) {
			((IPluginExtensionPoint) element).write(firstIndent, writer);
		}

		// add extensions
		children = getExtensions();
		if (children.length > 0) {
			writer.println();
		}
		for (Object element : children) {
			((IPluginExtension) element).write(firstIndent, writer);
		}
		writer.println();
		writer.println(fIsFragment ? "</fragment>" : "</plugin>"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void setIsFragment(boolean isFragment) {
		fIsFragment = isFragment;
	}
}
