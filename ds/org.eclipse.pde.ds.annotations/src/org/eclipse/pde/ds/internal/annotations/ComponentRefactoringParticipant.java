/*******************************************************************************
 * Copyright (c) 2015 Ecliptical Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;

/**
 * Common protocol for DS {@link RefactoringParticipant refactoring participants}
 * since they need to extend different classes.
 */
public interface ComponentRefactoringParticipant {

	String getComponentNameRoot(IJavaElement renamedElement, RefactoringArguments args);
}
