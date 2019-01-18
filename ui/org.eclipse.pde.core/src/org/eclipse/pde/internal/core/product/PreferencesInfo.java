/*******************************************************************************
 * Copyright (c) 2014 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.iproduct.IPreferencesInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PreferencesInfo extends ProductObject implements IPreferencesInfo {

	private static final long serialVersionUID = 1L;
	private String fSourceFilePath;
	private String fPreferenceCustomizationPath;
	private String fOverwrite;

	public PreferencesInfo(IProductModel model) {
		super(model);
	}

	@Override
	public void setSourceFilePath(String text) {
		String old = fSourceFilePath;
		fSourceFilePath = text;
		if (isEditable()) {
			if (old != null && text != null) {
				if (!old.equals(text)) {
					firePropertyChanged(P_SOURCEFILEPATH, old, fSourceFilePath);
				}
			} else if (old != text) {
				firePropertyChanged(P_SOURCEFILEPATH, old, fSourceFilePath);
			}
		}
	}

	@Override
	public String getSourceFilePath() {
		return fSourceFilePath;
	}

	@Override
	public void setOverwrite(String text) {
		String old = fOverwrite;
		fOverwrite = text;
		if (isEditable()) {
			if (old != null && text != null) {
				if (!old.equals(text)) {
					firePropertyChanged(P_OVERWRITE, old, fOverwrite);
				}
			} else if (old != text) {
				firePropertyChanged(P_OVERWRITE, old, fOverwrite);
			}
		}
	}

	@Override
	public String getOverwrite() {
		return fOverwrite;
	}

	@Override
	public void setPreferenceCustomizationPath(String text) {
		String old = fPreferenceCustomizationPath;
		fPreferenceCustomizationPath = text;
		if (isEditable()) {
			if (old != null && text != null) {
				if (!old.equals(text)) {
					firePropertyChanged(P_TARGETFILEPATH, old, fPreferenceCustomizationPath);
				}
			} else if (old != text) {
				firePropertyChanged(P_TARGETFILEPATH, old, fPreferenceCustomizationPath);
			}
		}
	}

	@Override
	public String getPreferenceCustomizationPath() {
		return fPreferenceCustomizationPath;
	}


	@Override
	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<preferencesInfo>"); //$NON-NLS-1$
		if (fSourceFilePath != null && fSourceFilePath.length() > 0) {
			writer.println(indent + "   <sourcefile path=\"" + getWritableString(fSourceFilePath.trim()) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		boolean overwrite = fOverwrite != null && "true".equals(fOverwrite); //$NON-NLS-1$
		String targetFile = indent + "   <targetfile overwrite=\"" + Boolean.toString(overwrite) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		if (fPreferenceCustomizationPath != null && fPreferenceCustomizationPath.length() > 0) {
			targetFile += " path=\"" + getWritableString(fPreferenceCustomizationPath.trim()) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		}
		targetFile += "/>"; //$NON-NLS-1$
		writer.println(targetFile);
		writer.println(indent + "</preferencesInfo>"); //$NON-NLS-1$
	}

	@Override
	public void parse(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("sourcefile")) { //$NON-NLS-1$
					fSourceFilePath = ((Element) child).getAttribute("path"); //$NON-NLS-1$
					if (fSourceFilePath.length() == 0) {
						fSourceFilePath = null;
					}
				} else if (child.getNodeName().equals("targetfile")) { //$NON-NLS-1$
					fOverwrite = ((Element) child).getAttribute("overwrite"); //$NON-NLS-1$
					fPreferenceCustomizationPath = ((Element) child).getAttribute("path"); //$NON-NLS-1$
					if (fPreferenceCustomizationPath.length() == 0) {
						fPreferenceCustomizationPath = null;
					}
				}
			}
		}
	}

}
