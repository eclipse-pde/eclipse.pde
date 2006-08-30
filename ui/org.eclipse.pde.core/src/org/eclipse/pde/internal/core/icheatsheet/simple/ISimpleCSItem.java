/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.icheatsheet.simple;

/**
 * ISimpleCSItem
 *
 */
public interface ISimpleCSItem extends ISimpleCSObject {

	/**
	 * Element:  description
	 * @return
	 */
	public ISimpleCSDescription getDescription();
	
	/**
	 * Element:  description
	 * @param description
	 */
	public void setDescription(ISimpleCSDescription description);	
	
	/**
	 * Attribute: title
	 * @return
	 */
	public String getTitle();
	
	/**
	 * Attribute: title
	 * @param title
	 */
	public void setTitle(String title);
	
	/**
	 * Attribute: dialog
	 * @return
	 */
	public boolean getDialog();
	
	/**
	 * Attribute: dialog
	 * @param dialog
	 */
	public void setDialog(boolean dialog);	
	
	/**
	 * Attribute: skip
	 * @return
	 */
	public boolean getSkip();
	
	/**
	 * Attribute: skip
	 * @param skip
	 */
	public void setSkip(boolean skip);		

	/**
	 * Attribute:  contextId
	 * @return
	 */
	public String getContextId();
	
	/**
	 * Attribute:  contextId
	 * @param contextId
	 */
	public void setContextId(String contextId);
	
	/**
	 * Attribute:  href
	 * @return
	 */
	public String getHref();
	
	/**
	 * Attribute:  href
	 * @param href
	 */
	public void setHref(String href);	
	
	/**
	 * Elements:  action, command, perform-when
	 * @return
	 */
	public ISimpleCSRunContainerObject getExecutable();	

	/**
	 * Elements:  action, command, perform-when
	 * @param executable
	 */
	public void setExecutable(ISimpleCSRunContainerObject executable);	

	/**
	 * Elements:  subitem, repeated-subitem, conditional-subitem
	 * @return
	 */
	public ISimpleCSSubItemObject[] getSubItems();
	
	/**
	 * Elements:  subitem, repeated-subitem, conditional-subitem
	 * @param subitems
	 */
	public void addSubItems(ISimpleCSSubItemObject[] subitems);
	
	/**
	 * Elements:  subitem, repeated-subitem, conditional-subitem
	 * @param subitems
	 */
	public void removeSubItems(ISimpleCSSubItemObject[] subitems);	
	
	/**
	 * Element:  subitem, repeated-subitem, conditional-subitem
	 * @param subitem
	 */
	public void addSubItem(ISimpleCSSubItemObject subitem);
	
	/**
	 * Element:  subitem, repeated-subitem, conditional-subitem
	 * @param subitem
	 */
	public void removeSubItem(ISimpleCSSubItemObject subitem);
	
	/**
	 * Element:  onCompletion
	 * @return
	 */
	public ISimpleCSOnCompletion getOnCompletion();
	
	/**
	 * Element:  onCompletion
	 * @param onCompletion
	 */
	public void setOnCompletion(ISimpleCSOnCompletion onCompletion);
	
}
