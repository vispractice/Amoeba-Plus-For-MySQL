/*
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.amoeba.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
@SuppressWarnings("unchecked")
public class OrderRetainingMap extends HashMap {

	private static final long serialVersionUID = 1L;
	private Set keyOrder = new ArraySet();
    private List valueOrder = new ArrayList();
    
    public Object put(Object key, Object value) {
        keyOrder.add(key);
        valueOrder.add(value);
        return super.put(key, value);
    }

    public Collection values() {
        return Collections.unmodifiableList(valueOrder);
    }

    public Set keySet() {
        return Collections.unmodifiableSet(keyOrder);
    }

    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    private static class ArraySet extends ArrayList implements Set {
		private static final long serialVersionUID = 1L;
    }

}
