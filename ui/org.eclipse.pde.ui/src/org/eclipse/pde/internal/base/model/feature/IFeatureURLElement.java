package org.eclipse.pde.internal.base.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.net.*;
/**
 * A URL element is a model object that represents
 * a single URL reference. The reference has a type
 * (UPDATE, DISCOVERY or INFO), and an optional
 * label that should be used to represent the URL
 * in the UI.
 */
public interface IFeatureURLElement extends IFeatureObject {
/**
 * Indicates that this is an update URL.
 */
public static final int UPDATE = 1;
/**
 * Indicates that this is a discovery URL.
 */
public static final int DISCOVERY = 2;
/**
 * This property name will be used to notify
 * about changes in the "URL" field.
 */
public static final String P_URL = "url";
/**
 * Returns the type of this URL element (UPDATE or DISCOVERY)
 */
public int getElementType();
/**
 * Returns the URL of this element.
 *
 * @return the URL
 */
public URL getURL();
/**
 * Sets the URL of this element.
 * This method will throw a CoreException
 * if the model is not editable.
 *
 * @param url the new URL 
 */
public void setURL(URL url) throws CoreException;
}
