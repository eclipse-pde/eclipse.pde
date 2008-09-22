/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSReference;

public class DSReference extends DSObject implements IDSReference {

	private static final long serialVersionUID = 1L;

	public DSReference(DSModel model) {
		super(model, ELEMENT_REFERENCE);
		
		// set default values
		this
				.setReferenceCardinality(IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_ONE);
		this.setReferencePolicy(IDSConstants.VALUE_REFERENCE_POLICY_STATIC);
		
		// set generic values
		int ref_count = model.getDSComponent().getReferences().length + 1;
		this
				.setReferenceName(IDSConstants.ELEMENT_REFERENCE
						+ ref_count);
		this.setReferenceInterface(IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE
				+ ref_count);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#canAddChild(int)
	 */
	public boolean canAddChild(int objectType) {
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#canBeParent()
	 */
	public boolean canBeParent() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#getName()
	 */
	public String getName() {
		return getReferenceName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.text.DSObject#getType()
	 */
	public int getType() {
		return TYPE_REFERENCE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#setReferenceName(java.lang.String)
	 */
	public void setReferenceName(String name){
		setXMLAttribute(ATTRIBUTE_REFERENCE_NAME, name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#getReferenceName()
	 */
	public String getReferenceName(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_NAME);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#setReferenceInterface(java.lang.String)
	 */
	public void setReferenceInterface(String interfaceName){
		setXMLAttribute(ATTRIBUTE_REFERENCE_INTERFACE, interfaceName);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#getReferenceInterface()
	 */
	public String getReferenceInterface(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_INTERFACE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#setReferenceCardinality(java.lang.String)
	 */
	public void setReferenceCardinality(String cardinality){
		setXMLAttribute(ATTRIBUTE_REFERENCE_CARDINALITY, cardinality);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#getReferenceCardinality()
	 */
	public String getReferenceCardinality(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_CARDINALITY);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#setReferencePolicy(java.lang.String)
	 */
	public void setReferencePolicy(String policy){
		setXMLAttribute(ATTRIBUTE_REFERENCE_POLICY, policy);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#getReferencePolicy()
	 */
	public String getReferencePolicy(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_POLICY);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#setReferenceTarget(java.lang.String)
	 */
	public void setReferenceTarget(String target){
		setXMLAttribute(ATTRIBUTE_REFERENCE_TARGET, target);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#getReferenceTarget()
	 */
	public String getReferenceTarget(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_TARGET);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#setReferenceBind(java.lang.String)
	 */
	public void setReferenceBind(String bind){
		setXMLAttribute(ATTRIBUTE_REFERENCE_BIND, bind);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#getReferenceBind()
	 */
	public String getReferenceBind(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_BIND);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#setReferenceUnbind(java.lang.String)
	 */
	public void setReferenceUnbind(String unbind){
		setXMLAttribute(ATTRIBUTE_REFERENCE_UNBIND, unbind);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ds.core.text.IDSReference#getReferenceUnbind()
	 */
	public String getReferenceUnbind(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_UNBIND);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ds.core.IDSObject#getAttributesNames()
	 */
	public String[] getAttributesNames() {
		return new String[] { IDSConstants.ATTRIBUTE_REFERENCE_BIND,
				IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY,
				IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE,
				IDSConstants.ATTRIBUTE_REFERENCE_NAME,
				IDSConstants.ATTRIBUTE_REFERENCE_POLICY,
				IDSConstants.ATTRIBUTE_REFERENCE_TARGET,
				IDSConstants.ATTRIBUTE_REFERENCE_UNBIND };
	}

	public boolean isLeafNode() {
		return true;
	}

}
