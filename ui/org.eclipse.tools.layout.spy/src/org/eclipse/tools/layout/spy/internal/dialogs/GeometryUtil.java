/*******************************************************************************
 * Copyright (c) 2016 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stefan Xenos (Google) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.tools.layout.spy.internal.dialogs;

import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class GeometryUtil {
	public static Rectangle getDisplayBounds(Control boundsControl) {
		Control parent = boundsControl.getParent();
		if (parent == null || boundsControl instanceof Shell) {
			return boundsControl.getBounds();
		}

		return Geometry.toDisplay(parent, boundsControl.getBounds());
	}

	public static Rectangle extrudeEdge(Rectangle innerBoundsWrtOverlay, int distanceToTop, int side) {
		if (distanceToTop <= 0) {
			return new Rectangle(0, 0, 0, 0);
		}
		return Geometry.getExtrudedEdge(innerBoundsWrtOverlay, distanceToTop, side);
	}

	public static int getBottom(Rectangle rect) {
		return rect.y + rect.height;
	}

	public static int getRight(Rectangle rect) {
		return rect.x + rect.width;
	}
}
