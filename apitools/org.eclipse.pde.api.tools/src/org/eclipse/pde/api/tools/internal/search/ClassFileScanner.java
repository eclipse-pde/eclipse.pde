/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.objectweb.asm.ClassReader;

/**
 * This class is the scanner that drives the ASM class visitor. Once scanned
 * the {@link IReference} objects can be retrieved from this scanner. All {@link IReference}
 * objects are unresolved.
 * 
 * @since 1.0.0
 */
public class ClassFileScanner {

	private List references= null;
	/**
	 * Singleton instance
	 */
	private static ClassFileScanner scanner = null;
	
	/**
	 * Constructor
	 * Cannot be instantiated
	 */
	private ClassFileScanner() {}
	
	/**
	 * returns the class file scanner singleton
	 * @return the class file scanner
	 */
	public static ClassFileScanner newScanner() {
		if(scanner == null) {
			scanner = new ClassFileScanner();
		}
		return scanner;
	}
	
	/**
	 * Returns the list of references built by scanning an {@link IClassFile}. The list of references is discarded
	 * once this method is called.
	 * If the scan method has never been called an empty list is returned.
	 * @return the listing of {@link IReference} objects or {@link Collections#EMPTY_LIST}, never <code>null</code>
	 */
	public List getReferenceListing() {
		if (this.references == null || this.references.size() == 0) {
			return Collections.EMPTY_LIST;
		}
		ArrayList result = new ArrayList(this.references.size());
		result.addAll(this.references);
		this.references.clear();
		return result;
	}

	/**
	 * Scans the specified {@link IClassFile} and build the set of references
	 * @param component the component we are scanning the class file from
	 * @param classfile the class file to scan
	 * @param referenceKinds kinds of references to extract as defined by {@link ReferenceModifiers}
	 * @throws CoreException 
	 */
	public void scan(IApiComponent component, IClassFile classfile, int referenceKinds) throws CoreException {
		if (this.references == null || this.references == Collections.EMPTY_LIST) {
			this.references = new ArrayList(100);
		} else {
			references.clear();
		}
		ClassReader reader = new ClassReader(classfile.getContents());
		reader.accept(new ClassFileVisitor(component, references, referenceKinds), ClassReader.SKIP_FRAMES);
	}
}
