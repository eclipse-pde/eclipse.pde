/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package i;

/**
 * Copy of the functional interface Supplier, but with a reference api restriction on the method
 */
@FunctionalInterface
public interface NoRefFunctionalInterface<T> {

    /**
     * Gets a result.
     *
     * @return a result
     * @noreference This method is not intended to be referenced by clients.
     */
    T get();
}