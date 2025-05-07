/*******************************************************************************
 * Copyright (c) 2014, 2023 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     Sean Bright <sean@malleable.com> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
 *     Christoph Rueger <chrisrueger@gmail.com> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.model.resolution;

import java.util.Map.Entry;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.pde.bnd.ui.Resources;
import org.eclipse.pde.bnd.ui.model.resource.R5LabelFormatter;
import org.eclipse.pde.bnd.ui.model.resource.RequirementLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.Clazz;
import aQute.bnd.service.resource.SupportingResource;

public class RequirementWrapperLabelProvider extends RequirementLabelProvider {

	private final Styler resolved = StyledString.QUALIFIER_STYLER;

	public RequirementWrapperLabelProvider(boolean shortenNamespaces) {
		super(shortenNamespaces);
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (element instanceof RequirementWrapper) {
			RequirementWrapper rw = (RequirementWrapper) element;

			Image icon = Resources.getImage(R5LabelFormatter.getNamespaceImagePath(rw.requirement.getNamespace()));
			cell.setImage(icon);

			StyledString label = getLabel(rw.requirement);
			if (rw.resolved || rw.java)
				label.setStyle(0, label.length(), resolved);

			cell.setText(label.getString());
			cell.setStyleRanges(label.getStyleRanges());
		} else if (element instanceof Clazz) {
			cell.setImage(Resources.getImage("class_obj"));

			String pkg;
			String className;

			String fqn = ((Clazz) element).getFQN();
			int dot = fqn.lastIndexOf('.');
			if (dot >= 0) {
				pkg = fqn.substring(0, dot);
				className = fqn.substring(dot + 1);
			} else {
				pkg = "<default package>";
				className = fqn;
			}

			StyledString label = new StyledString(className);
			label.append(" - " + pkg, StyledString.QUALIFIER_STYLER);

			cell.setText(label.getString());
			cell.setStyleRanges(label.getStyleRanges());
		}
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof RequirementWrapper rw) {
			return tooltipText(rw);
		}

		return null;
	}


	static String tooltipText(RequirementWrapper rw) {
		Requirement req = rw.requirement;

		StringBuilder buf = new StringBuilder(300);
		if (rw.resolved)
			buf.append("RESOLVED:\n");
		if (rw.java)
			buf.append("JAVA:\n");

		Resource r = req.getResource();

		buf.append("FROM: ")
			.append(r)
			.append("\n");

		if (r instanceof SupportingResource sr) {
			int index = sr.getSupportingIndex();
			if (index >= 0) {
				buf.append("Requirement from a supporting resource ")
					.append(index)
					.append(" part of ")
					.append(sr.getParent())
					.append("\n");
			}
		}
		buf.append(req.getNamespace());

		for (Entry<String, Object> attr : req.getAttributes()
			.entrySet())
			buf.append(";\n\t")
				.append(attr.getKey())
				.append(" = ")
				.append(attr.getValue());

		for (Entry<String, String> directive : req.getDirectives()
			.entrySet())
			buf.append(";\n\t")
				.append(directive.getKey())
				.append(" := ")
				.append(directive.getValue());

		return buf.toString();
	}

}
