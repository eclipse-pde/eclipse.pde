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

import java.util.ArrayList;
import java.util.function.Consumer;

import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.databinding.swt.WidgetSideEffects;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Creates an overlay that allows the user to select any SWT control by moving
 * the mouse and clicking. Once the user selects the control or presses ESC, the
 * overlay is closed.
 */
public class ControlSelector {
	private static final int EDGE_SIZE = 4;

	private Shell overlay;
	private LocalResourceManager resources;
	private Color selectionRectangleColor;
	private Display display;
	private WritableValue<@Nullable Control> currentSelection = new WritableValue<>(null, null);
	private Region region;
	private Consumer<@Nullable Control> callback;
	private Listener moveFilter = this::mouseMove;
	private Listener selectFilter = this::select;

	/**
	 * Instantiates and opens the control selector.
	 *
	 * @param resultCallback
	 *            callback that will be invoked when the overlay is closed. If
	 *            the user selected a control, that control is passed to the
	 *            callback. If the user cancelled the overlay, null is passed.
	 */
	public ControlSelector(Consumer<@Nullable Control> resultCallback) {
		this.callback = resultCallback;
		display = Display.getCurrent();
		overlay = new Shell(SWT.ON_TOP | SWT.NO_TRIM);
		overlay.addPaintListener(this::paint);
		resources = new LocalResourceManager(JFaceResources.getResources(), overlay);
		selectionRectangleColor = resources.createColor(new RGB(255, 255, 0));
		display.addFilter(SWT.MouseMove, moveFilter);
		display.addFilter(SWT.MouseDown, selectFilter);
		overlay.addDisposeListener(this::disposed);
		region = new Region();
		WidgetSideEffects.createFactory(overlay).create(currentSelection::getValue, this::updateRegion);
	}

	private void updateRegion(@Nullable Control newControl) {
		if (newControl == null) {
			overlay.setVisible(false);
			return;
		}
		overlay.setBounds(newControl.getMonitor().getClientArea());
		Rectangle parentBoundsWrtDisplay = GeometryUtil.getDisplayBounds(newControl);
		Rectangle parentBoundsWrtOverlay = Geometry.toControl(overlay, parentBoundsWrtDisplay);
		Rectangle innerBoundsWrtOverlay = Geometry.copy(parentBoundsWrtOverlay);
		Geometry.expand(innerBoundsWrtOverlay, -EDGE_SIZE, -EDGE_SIZE, -EDGE_SIZE, -EDGE_SIZE);
		region.dispose();
		region = new Region();
		region.add(parentBoundsWrtOverlay);
		region.subtract(innerBoundsWrtOverlay);
		overlay.setRegion(region);
		overlay.setVisible(true);
	}

	private void disposed(DisposeEvent e) {
		currentSelection.dispose();
		region.dispose();
		display.removeFilter(SWT.MouseMove, moveFilter);
		display.removeFilter(SWT.MouseDown, selectFilter);
	}

	private void select(Event e) {
		closeWithResult(this.currentSelection.getValue());
		e.doit = false;
	}

	private void closeWithResult(@Nullable Control result) {
		display.removeFilter(SWT.MouseMove, moveFilter);
		display.removeFilter(SWT.MouseDown, selectFilter);
		overlay.dispose();
		callback.accept(result);
	}

	/**
	 * Finds and returns the most specific SWT control at the given location.
	 * (Note: this does a DFS on the SWT widget hierarchy, which is slow).
	 *
	 * @param displayToSearch
	 * @param locationToFind
	 * @return the most specific SWT control at the given location
	 */
	public static Control findControl(Display displayToSearch, Shell toIgnore, Point locationToFind) {
		Shell[] shells = displayToSearch.getShells();

		ArrayList<Shell> shellList = new ArrayList<>();
		for (Shell next : shells) {
			if (next == toIgnore) {
				continue;
			}
			shellList.add(next);
		}

		shells = shellList.toArray(new Shell[shellList.size()]);

		return findControl(shells, locationToFind);
	}

	/**
	 * Finds the control at the given location.
	 *
	 * @param toSearch
	 * @param locationToFind
	 *            location (in display coordinates)
	 * @return the control at the given location
	 */
	public static Control findControl(Composite toSearch, Point locationToFind) {
		Control[] children = toSearch.getChildren();

		return findControl(children, locationToFind);
	}

	/**
	 * Searches the given list of controls for a control containing the given
	 * point. If the array contains any composites, those composites will be
	 * recursively searched to find the most specific child that contains the
	 * point.
	 *
	 * @param toSearch
	 *            an array of composites
	 * @param locationToFind
	 *            a point (in display coordinates)
	 * @return the most specific Control that overlaps the given point, or null
	 *         if none
	 */
	public static Control findControl(Control[] toSearch, Point locationToFind) {
		for (int idx = toSearch.length - 1; idx >= 0; idx--) {
			Control next = toSearch[idx];

			if (!next.isDisposed() && next.isVisible()) {

				Rectangle bounds = GeometryUtil.getDisplayBounds(next);

				if (bounds.contains(locationToFind)) {
					if (next instanceof Composite) {
						Control result = findControl((Composite) next, locationToFind);

						if (result != null) {
							return result;
						}
					}

					return next;
				}
			}
		}

		return null;
	}

	private void mouseMove(Event e) {
		Point globalPoint = new Point(e.x, e.y);
		if (e.widget instanceof Control) {
			Control control = (Control) e.widget;

			globalPoint = control.toDisplay(globalPoint);
		}
		Control control = findControl(Display.getCurrent(), overlay, globalPoint);
		currentSelection.setValue(control);
	}

	protected void paint(PaintEvent e) {
		e.gc.setBackground(selectionRectangleColor);
		e.gc.fillRectangle(overlay.getClientArea());
	}

}
