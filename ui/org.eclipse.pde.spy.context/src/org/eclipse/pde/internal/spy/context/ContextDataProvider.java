/*******************************************************************************
 * Copyright (c) 2013 OPCoach.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     OPCoach - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.spy.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.e4.core.internal.contexts.Computation;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * The column Label and content Provider used to display information in context
 * data TreeViewer. Two instances for label provider are created : one for key,
 * one for values
 *
 * @see ContextDataPart
 */
@SuppressWarnings("restriction")
public class ContextDataProvider extends ColumnLabelProvider implements ITreeContentProvider {

	private static final String NO_VALUE_COULD_BE_COMPUTED = Messages.ContextDataProvider_0;
	private static final Color COLOR_IF_FOUND = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
	private static final Color COLOR_IF_NOT_COMPUTED = Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA);
	private static final Object[] EMPTY_RESULT = new Object[0];
	static final String LOCAL_VALUE_NODE = Messages.ContextDataProvider_1;
	static final String INHERITED_INJECTED_VALUE_NODE = Messages.ContextDataProvider_2;

	private static final String NO_VALUES_FOUND = Messages.ContextDataProvider_3;
	private static final String UPDATED_IN_CLASS = Messages.ContextDataProvider_4;
	private static final String INJECTED_IN_FIELD = Messages.ContextDataProvider_5;
	private static final String INJECTED_IN_METHOD = Messages.ContextDataProvider_6;

	// Image keys constants
	private static final String PUBLIC_METHOD_IMG_KEY = "icons/methpub_obj.png"; //$NON-NLS-1$
	private static final String PUBLIC_FIELD_IMG_KEY = "icons/field_public_obj.png"; //$NON-NLS-1$
	private static final String VALUE_IN_CONTEXT_IMG_KEY = "icons/valueincontext.png"; //$NON-NLS-1$
	private static final String INHERITED_VARIABLE_IMG_KEY = "icons/inher_co.png"; //$NON-NLS-1$
	private static final String LOCAL_VARIABLE_IMG_KEY = "icons/letter-l-icon.png"; //$NON-NLS-1$
	private static final String CONTEXT_FUNCTION_IMG_KEY = "icons/contextfunction.png"; //$NON-NLS-1$
	private static final String INJECT_IMG_KEY = "icons/annotation_obj.png"; //$NON-NLS-1$

	private ImageRegistry imgReg;

	@Inject
	private ContextDataFilter contextFilter;

	/** Store the selected context (initialized in inputChanged) */
	private static EclipseContext selectedContext;

	private Font boldFont;

	private boolean displayKey = false;

	@Inject
	public ContextDataProvider() {
		super();
		initFonts();
		initializeImageRegistry();
	}

	@Override
	public void dispose() {
		selectedContext = null;
		imgReg = null;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		selectedContext = (newInput instanceof EclipseContext) ? (EclipseContext) newInput : null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return new String[] { LOCAL_VALUE_NODE, INHERITED_INJECTED_VALUE_NODE };
	}

	@Override
	public Object[] getChildren(Object inputElement) {
		if (selectedContext == null)
			return EMPTY_RESULT;

		if (inputElement == LOCAL_VALUE_NODE) {
			Collection<Object> result = new ArrayList<>();

			result.addAll(selectedContext.localData().entrySet());

			// For context function, we have to compute the value (if possible),
			// and display it as a standard value
			Map<String, Object> cfValues = new HashMap<>();
			for (String key : selectedContext.localContextFunction().keySet())
				try {
					cfValues.put(key, selectedContext.get(key));
				} catch (Throwable e) {
					cfValues.put(key, NO_VALUE_COULD_BE_COMPUTED + " (Exception : " + e.getClass().getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			result.addAll(cfValues.entrySet());
			return result.toArray();

		} else if (inputElement == INHERITED_INJECTED_VALUE_NODE) {
			// Search for all values injected using this context but defined in
			// parent
			Collection<Object> result = new ArrayList<>();

			// Keep only the names that are not already displayed in local
			// values
			Collection<String> localKeys = selectedContext.localData().keySet();
			Collection<String> localContextFunctionsKeys = selectedContext.localContextFunction().keySet();

			if (selectedContext.getRawListenerNames() != null) {
				for (String name : selectedContext.getRawListenerNames()) {
					if (!localKeys.contains(name) && !localContextFunctionsKeys.contains(name))
						result.add(name);
				}
			}
			return result.isEmpty() ? new String[] { NO_VALUES_FOUND } : result.toArray();

		} else if (inputElement instanceof Map.Entry) {
			Set<Computation> listeners = getListeners(inputElement);
			return (listeners == null) ? null : listeners.toArray();
		} else if (inputElement instanceof String) {
			// This is the name of a raw listener in the inherited injected
			// value part
			return selectedContext.getListeners((String) inputElement).toArray();
		}

		return EMPTY_RESULT;
	}

	public void setDisplayKey(boolean k) {
		displayKey = k;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getText(Object element) {
		if (selectedContext == null)
			return null;

		if (element instanceof Map.Entry) {
			Map.Entry<String, Object> mapEntry = (Map.Entry<String, Object>) element;
			Object o = displayKey ? mapEntry.getKey() : mapEntry.getValue();
			return (o == null) ? Messages.ContextDataProvider_16 : o.toString();
		} else if (element instanceof Computation) {
			// For a computation : display field or method in key column and the
			// value in value
			String txt = super.getText(element);
			if (displayKey) {
				if (txt.contains("#")) //$NON-NLS-1$
					return INJECTED_IN_METHOD;
				else if (txt.contains("@")) //$NON-NLS-1$
					return UPDATED_IN_CLASS;
				else
					return INJECTED_IN_FIELD;
			} else
				return txt;
		}

		return displayKey ? super.getText(element) : null;
	}

	@Override
	public Color getForeground(Object element) {
		// Return magenta color if the value could not be yet computed (for
		// context functions)
		String s = getText(element);
		if ((s != null) && s.startsWith(NO_VALUE_COULD_BE_COMPUTED))
			return COLOR_IF_NOT_COMPUTED;

		// Return blue color if the string matches the search
		return (contextFilter.matchText(s)) ? COLOR_IF_FOUND : null;
	}

	/** Get the bold font for keys that are computed with ContextFunction */
	@Override
	public Font getFont(Object element) {
		return (element == LOCAL_VALUE_NODE || element == INHERITED_INJECTED_VALUE_NODE) ? boldFont : null;

	}

	@Override
	public Image getImage(Object element) {
		if (!displayKey) // No image in value column, only in key column
			return null;

		if (element == LOCAL_VALUE_NODE) {
			return selectedContext == null ? null : imgReg.get(LOCAL_VARIABLE_IMG_KEY);

		} else if (element == INHERITED_INJECTED_VALUE_NODE) {
			return selectedContext == null ? null : imgReg.get(INHERITED_VARIABLE_IMG_KEY);

		} else if (element instanceof Computation) {
			// For a computation : display field, method or class in key column
			// and
			// value in value column
			String txt = super.getText(element);

			if (txt.contains("#")) //$NON-NLS-1$
				return imgReg.get(PUBLIC_METHOD_IMG_KEY);
			else if (txt.contains("@")) //$NON-NLS-1$
				return imgReg.get(CONTEXT_FUNCTION_IMG_KEY);
			else
				return imgReg.get(PUBLIC_FIELD_IMG_KEY);

		} else if (element instanceof Map.Entry) {
			if (isAContextKeyFunction(element))
				return imgReg.get(CONTEXT_FUNCTION_IMG_KEY);
			else {
				// It is a value. If it is injected somewhere, display the
				// inject image
				return hasChildren(element) ? imgReg.get(INJECT_IMG_KEY) : imgReg.get(VALUE_IN_CONTEXT_IMG_KEY);
			}

		}

		return imgReg.get(INJECT_IMG_KEY);

	}

	@Override
	public String getToolTipText(Object element) {
		if (element == LOCAL_VALUE_NODE) {
			return Messages.ContextDataProvider_21
					+ Messages.ContextDataProvider_22
					+ Messages.ContextDataProvider_23;
		} else if (element == INHERITED_INJECTED_VALUE_NODE) {
			return Messages.ContextDataProvider_24
					+ Messages.ContextDataProvider_25;
		} else if (isAContextKeyFunction(element)) {
			String key = (String) ((Map.Entry<?, ?>) element).getKey();
			String fname = selectedContext.localContextFunction().get(key).getClass().getCanonicalName();

			return Messages.ContextDataProvider_26 + fname;
		} else {
			if (hasChildren(element))
				return Messages.ContextDataProvider_27;
			else {
				if (element instanceof Map.Entry)
					return Messages.ContextDataProvider_28;
			}

		}

		return super.getToolTipText(element);
	}

	@Override
	public Image getToolTipImage(Object object) {
		return getImage(object);
	}

	@Override
	public int getToolTipStyle(Object object) {
		return SWT.SHADOW_OUT;
	}

	/**
	 * Compute it the current entry in context is a context function
	 *
	 * @param element
	 * @return true if element is a context function
	 */
	@SuppressWarnings("unchecked")
	boolean isAContextKeyFunction(Object element) {
		if (selectedContext != null && element instanceof Map.Entry) {
			// Just check if key in element is a key in the map of context
			// functions.
			Map.Entry<String, Object> mapEntry = (Map.Entry<String, Object>) element;
			return (selectedContext.localContextFunction().containsKey(mapEntry.getKey()));
		}
		return false;

	}

	@Override
	public Object getParent(Object element) {
		if (element == LOCAL_VALUE_NODE || element == INHERITED_INJECTED_VALUE_NODE)
			return null;

		// Not computed
		return null;

	}

	@Override
	public boolean hasChildren(Object element) {
		if ((element == INHERITED_INJECTED_VALUE_NODE) || (element == LOCAL_VALUE_NODE)) {
			return true; // Intermediate nodes returns true
		}

		Set<Computation> listeners = getListeners(element);
		return (listeners != null) && !listeners.isEmpty();
	}

	@SuppressWarnings("unchecked")
	Set<Computation> getListeners(Object element) {
		if (selectedContext != null) {
			if (element instanceof Map.Entry) {
				// Ask the context to know if there are listeners for this value
				Map.Entry<String, Object> mapEntry = (Map.Entry<String, Object>) element;
				String key = mapEntry.getKey();
				return selectedContext.getListeners(key);

			} else if (element instanceof String) {
				// Ask the context to know if there are listeners for this raw
				// listener name
				return selectedContext.getListeners((String) element);
			}
		}
		return null;

	}

	private void initializeImageRegistry() {
		Bundle b = FrameworkUtil.getBundle(this.getClass());
		imgReg = new ImageRegistry();

		imgReg.put(CONTEXT_FUNCTION_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(CONTEXT_FUNCTION_IMG_KEY)));
		imgReg.put(INJECT_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(INJECT_IMG_KEY)));
		imgReg.put(PUBLIC_METHOD_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(PUBLIC_METHOD_IMG_KEY)));
		imgReg.put(PUBLIC_FIELD_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(PUBLIC_FIELD_IMG_KEY)));
		imgReg.put(PUBLIC_FIELD_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(PUBLIC_FIELD_IMG_KEY)));
		imgReg.put(LOCAL_VARIABLE_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(LOCAL_VARIABLE_IMG_KEY)));
		imgReg.put(VALUE_IN_CONTEXT_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(VALUE_IN_CONTEXT_IMG_KEY)));
		imgReg.put(INHERITED_VARIABLE_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(INHERITED_VARIABLE_IMG_KEY)));

	}

	private void initFonts() {
		FontData[] fontData = Display.getCurrent().getSystemFont().getFontData();
		String fontName = fontData[0].getName();
		FontRegistry registry = JFaceResources.getFontRegistry();
		boldFont = registry.getBold(fontName);
	}

}
