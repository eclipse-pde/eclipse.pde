/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.regex.Pattern;

import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;

/**
 * Visits an API description modifying package visibility based on pattern
 * matching.
 */
public class ApiDescriptionModifier extends ApiDescriptionVisitor {
	
	/**
	 * Internal package patterns or <code>null</code> if none.
	 */
	private Pattern[] fInternalPackages;
		
	/**
	 * API package patterns of <code>null</code> if none.
	 */
	private Pattern[] fApiPackages; 
	
	/**
	 * API description to modify.
	 */
	private IApiDescription fDescription;
	
	/**
	 * Constructs a visitor with the given patterns.
	 * 
	 * @param internal regular expressions to match as internal packages or <code>null</code>
	 * @param api regular expressions to match as API or <code>null</code>
	 */
	public ApiDescriptionModifier(String[] internal, String[] api) {
		setInternalPatterns(internal);
		setApiPatterns(api);
	}
	
	/**
	 * Sets the description to be modified.
	 * 
	 * @param description API description to modify
	 */
	public void setApiDescription(IApiDescription description) {
		fDescription = description;
	}

	/**
	 * Sets regular expressions to consider as internal packages. Used to override visibility settings
	 * in an API description.
	 * 
	 * @param patterns regular expressions, may be empty or <code>null</code>
	 */
	private void setInternalPatterns(String[] patterns) {
		if (patterns == null || patterns.length == 0) {
			fInternalPackages = null;
		} else {
			fInternalPackages = new Pattern[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				fInternalPackages[i] = Pattern.compile(patterns[i]);
			}
		}
	}
	
	/**
	 * Sets regular expressions to consider as API packages. Used to override visibility settings
	 * in an API description.
	 * 
	 * @param patterns regular expressions, may be empty or <code>null</code>
	 */
	private void setApiPatterns(String[] patterns) {
		if (patterns == null || patterns.length == 0) {
			fApiPackages = null;
		} else {
			fApiPackages = new Pattern[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				fApiPackages[i] = Pattern.compile(patterns[i]);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor#visitElement(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations)
	 */
	public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
		switch (element.getElementType()) {
			case IElementDescriptor.COMPONENT:
				return true;
			case IElementDescriptor.PACKAGE:
				IPackageDescriptor pkg = (IPackageDescriptor) element;
				if (fInternalPackages != null) {
					if (matchesPattern(pkg.getName(), fInternalPackages)) {
						fDescription.setVisibility(element, VisibilityModifiers.PRIVATE);
					}
				}
				if (fApiPackages != null) {
					if (matchesPattern(pkg.getName(), fApiPackages)) {
						fDescription.setVisibility(element, VisibilityModifiers.API);
					}
				}
				return false;
			default:
				return false;
		}
	}
	
	/**
	 * Returns whether the package matches any of the given patterns.
	 * 
	 * @param name name to match
	 * @param patterns patterns to match against
	 * @return whether there's a match
	 */
	private boolean matchesPattern(String name, Pattern[] patterns) {
		for (int i = 0; i < patterns.length; i++) {
			if (patterns[i].matcher(name).find()) {
				return true;
			}
		}
		return false;
	}
}
