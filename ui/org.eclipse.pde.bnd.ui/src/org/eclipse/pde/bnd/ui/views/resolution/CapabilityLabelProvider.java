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
package org.eclipse.pde.bnd.ui.views.resolution;

import java.util.Map.Entry;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.pde.bnd.ui.Resources;
import org.eclipse.pde.bnd.ui.model.resource.R5LabelFormatter;
import org.eclipse.swt.graphics.Image;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.service.resource.SupportingResource;

public class CapabilityLabelProvider extends StyledCellLabelProvider {

	private final boolean shortenNamespaces;

	public CapabilityLabelProvider() {
		this(false);
	}

	public CapabilityLabelProvider(boolean shortenNamespaces) {
		this.shortenNamespaces = shortenNamespaces;
	}

	@Override
	public void update(ViewerCell cell) {
		Capability cap = (Capability) cell.getElement();

		StyledString label = new StyledString();
		R5LabelFormatter.appendCapability(label, cap, shortenNamespaces);
		cell.setText(label.toString());
		cell.setStyleRanges(label.getStyleRanges());

		// Get the icon from the capability namespace
		Image icon = Resources.getImage(R5LabelFormatter.getNamespaceImagePath(cap.getNamespace()));
		cell.setImage(icon);
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof Capability cap) {
			return tooltipText(cap);
		}

		return null;
	}

	static String tooltipText(Capability cap) {
		// caps tooltips become quite large because of the bnd.hashes and uses
		StringBuilder buf = new StringBuilder(400);

		Resource r = cap.getResource();

		buf.append("FROM: ")
			.append(r)
			.append("\n");

		buf.append(cap.getNamespace());

		if (r instanceof SupportingResource sr) {
			int index = sr.getSupportingIndex();
			if (index >= 0) {
				buf.append("Capability from a supporting resource ")
					.append(index)
					.append(" part of ")
					.append(sr.getParent())
					.append("\n");
			}
		}

		for (Entry<String, Object> attribute : cap.getAttributes()
			.entrySet()) {
			buf.append(";\n\t")
				.append(attribute.getKey())
				.append(" = ")
				.append(attribute.getValue());
		}

		for (Entry<String, String> directive : cap.getDirectives()
			.entrySet()) {
			buf.append(";\n\t")
				.append(directive.getKey())
				.append(" := ")
				.append(directive.getValue());
		}

		return buf.toString();
	}

}
