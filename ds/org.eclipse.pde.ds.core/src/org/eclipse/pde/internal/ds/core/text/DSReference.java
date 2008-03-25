/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

public class DSReference extends DSObject {

	private static final long serialVersionUID = 1L;

	public DSReference(DSModel model) {
		super(model, ELEMENT_REFERENCE);
	}

	public boolean canAddChild(int objectType) {
		return false;
	}

	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_REFERENCE;
	}

	public boolean canBeParent() {
		return false;
	}

	public String getName() {
		return getReferenceName();
	}

	public int getType() {
		return TYPE_REFERENCE;
	}

	public void setReferenceName(String name){
		setXMLAttribute(ATTRIBUTE_REFERENCE_NAME, name);
	}
	
	public String getReferenceName(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_NAME);
	}
	
	public void setReferenceInterface(String interfaceName){
		setXMLAttribute(ATTRIBUTE_REFERENCE_INTERFACE, interfaceName);
	}
	
	public String getReferenceInterface(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_INTERFACE);
	}
	
	public void setReferenceCardinality(String cardinality){
		setXMLAttribute(ATTRIBUTE_REFERENCE_CARDINALITY, cardinality);
	}
	
	public String getReferenceCardinality(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_CARDINALITY);
	}
	
	
	public void setReferencePolicy(String policy){
		setXMLAttribute(ATTRIBUTE_REFERENCE_POLICY, policy);
	}
	
	public String getReferencePolicy(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_POLICY);
	}
	
	
	public void setReferenceTarget(String target){
		setXMLAttribute(ATTRIBUTE_REFERENCE_TARGET, target);
	}
	
	public String getReferenceTarget(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_TARGET);
	}
	
	
	public void setReferenceBind(String bind){
		setXMLAttribute(ATTRIBUTE_REFERENCE_BIND, bind);
	}
	
	public String getReferenceBind(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_BIND);
	}
	
	
	public void setReferenceUnbind(String unbind){
		setXMLAttribute(ATTRIBUTE_REFERENCE_UNBIND, unbind);
	}
	
	public String getReferenceUnbind(){
		return getXMLAttributeValue(ATTRIBUTE_REFERENCE_UNBIND);
	}
}
