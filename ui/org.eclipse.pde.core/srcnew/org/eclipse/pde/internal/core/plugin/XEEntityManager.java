/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import org.apache.xerces.impl.XMLEntityManager;


public class XEEntityManager extends XMLEntityManager {

	public class XEEntityScanner extends EntityScanner {
		private static final float BUFFER_SIZE_MULTIPLIER= 1.1f;

		public void insert(String s) {
			if (fCurrentEntity.position < s.length()) {
				int oldRequiredBufferSize= fCurrentEntity.count - fCurrentEntity.position;
				int requiredBufferSize= s.length() + oldRequiredBufferSize;
				if (fCurrentEntity.ch.length < requiredBufferSize) {
					char[] tmp= fCurrentEntity.ch;
					fCurrentEntity.ch= new char[(int)(BUFFER_SIZE_MULTIPLIER * requiredBufferSize)];
					int newPosition= fCurrentEntity.ch.length - oldRequiredBufferSize;
					System.arraycopy(tmp, fCurrentEntity.position, fCurrentEntity.ch, newPosition, oldRequiredBufferSize);
					
					fCurrentEntity.count= fCurrentEntity.ch.length;
					fCurrentEntity.position= newPosition;
				} else {
					int newPosition= fCurrentEntity.ch.length - oldRequiredBufferSize;
					System.arraycopy(fCurrentEntity.ch, fCurrentEntity.position, fCurrentEntity.ch, newPosition, oldRequiredBufferSize);

					fCurrentEntity.count= fCurrentEntity.ch.length;
					fCurrentEntity.position= newPosition;
				}
			}
			int newPosition= fCurrentEntity.position - s.length();
			System.arraycopy(s.toCharArray(), 0, fCurrentEntity.ch, newPosition, s.length());

			fCurrentEntity.position= newPosition;
			fCurrentEntity.columnNumber -= s.length();
		}

	}

	/**
	 * Constructor for XEEntityManager.
	 */
	public XEEntityManager() {
		super();
		fEntityScanner= new XEEntityScanner();
	}

	/**
	 * Constructor for XEEntityManager.
	 * @param entityManager
	 */
	public XEEntityManager(XMLEntityManager entityManager) {
		super(entityManager);
		fEntityScanner= new XEEntityScanner();
	}
}
