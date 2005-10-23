/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.util.Iterator;
import java.util.TreeMap;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.IModelChangedEvent;

public class ExportPackageObject extends PackageObject {
    
    private static final String INTERNAL = "x-internal"; //$NON-NLS-1$
    private static final String FRIENDS = "x-friends"; //$NON-NLS-1$
    
    private static final long serialVersionUID = 1L;
    
    private TreeMap fFriends = new TreeMap();
     
    public ExportPackageObject(ManifestHeader header, ManifestElement element, String versionAttribute) {
        super(header, element, versionAttribute);
        processFriends();
    }
    
    public ExportPackageObject(ManifestHeader header, IPackageFragment fragment, String versionAttribute) {
        super(header, fragment.getElementName(), null, versionAttribute);
    }
    
    protected void processFriends() {
    	String[] friends = getDirectives(FRIENDS);
    	if (friends != null) {
	        for (int i = 0; i < friends.length; i++) {
	            fFriends.put(friends[i], new PackageFriend(this, friends[i]));
	        }
    	}
    }

    public boolean isInternal() {
        return "true".equals(getDirective(INTERNAL)) || getDirective(FRIENDS) != null; //$NON-NLS-1$
    }

    public void setInternal(boolean internal) {
    	boolean old = isInternal();
    	if (!internal) {
    		setDirective(INTERNAL, null);
    		setDirective(FRIENDS, null);
    	} else {
    		if (fFriends.size() == 0)
    			setDirective(INTERNAL, "true"); //$NON-NLS-1$
    		else {
    			Iterator iter = fFriends.keySet().iterator();
    			while (iter.hasNext())
    				addDirective(FRIENDS, iter.next().toString());
    		}
    	}
    	fHeader.update();
    	firePropertyChanged(this, INTERNAL, Boolean.toString(old), Boolean.toString(internal));
    }
    
    public PackageFriend[] getFriends() {
        return (PackageFriend[])fFriends.values().toArray(new PackageFriend[fFriends.size()]);
    }
    
    public void addFriend(PackageFriend friend) {
        fFriends.put(friend.getName(), friend);
        addDirective(FRIENDS, friend.getName());
        setDirective(INTERNAL, null);
        fHeader.update();
        fireStructureChanged(friend, IModelChangedEvent.INSERT);        
    }
    
    public void removeFriend(PackageFriend friend) {
        fFriends.remove(friend.getName());
        setDirective(FRIENDS, null);
        if (fFriends.size() == 0)
        	setDirective(INTERNAL, "true"); //$NON-NLS-1$
        else {
	        Iterator iter = fFriends.keySet().iterator();
	        while (iter.hasNext())
	        	addDirective(FRIENDS, iter.next().toString());
        }
        fHeader.update();
        fireStructureChanged(friend, IModelChangedEvent.REMOVE);       
    }
    
    public boolean hasFriend(String name) {
        return fFriends.containsKey(name);
    }
    
    public boolean hasSameVisibility(ExportPackageObject object) {
    	if (object.isInternal() != isInternal())
    		return false;
    	
    	if (fFriends.size() != object.fFriends.size())
    		return false;
    	
    	Iterator iter = fFriends.keySet().iterator();
    	while (iter.hasNext()) {
    		if (!object.fFriends.containsKey(iter.next()))
    			return false;
    	}
    	return true;
    }

}
