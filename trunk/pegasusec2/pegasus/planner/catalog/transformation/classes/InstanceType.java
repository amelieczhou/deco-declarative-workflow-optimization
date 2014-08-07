/**@author Amelie
 * This is an enumerated data class for Instance type of the Amazon EC2 cloud.
 * */

package edu.isi.pegasus.planner.catalog.transformation.classes;

import java.io.Serializable;
import java.util.HashMap;

public class InstanceType 
	implements Serializable {
	private String _value_;
    private static HashMap _table_ = new HashMap(5);

    protected InstanceType(String value) {
        _value_ = value;
        _table_.put(_value_, this);
    }

    private static final String _SMALL = "m1small";
    private static final String _MEDIUM = "m1medium";
    private static final String _LARGE = "m1large";
    private static final String _XLARGE = "m1xlarge";

    public static final InstanceType SMALL = new InstanceType(_SMALL);
    public static final InstanceType MEDIUM = new InstanceType(_MEDIUM);
    public static final InstanceType LARGE = new InstanceType(_LARGE);
    public static final InstanceType XLARGE = new InstanceType(_XLARGE);

    public static final String err = "Error: Illegal Operating System defined. Please specify one of the predefined types \n [LINUX, SUNOS, AIX, WINDOWS]";

    /**
     * Returns the value of the operating system as string.
     * @return String
     */
    public String getValue() {
        return _value_;
    }

    /**
     * Creates a new InstanceType object given an instance type string.
     * @param value String
     * @throws IllegalStateException Throws Exception if the operating system is not defined in this class.
     * @return InstanceType
     */
    public static InstanceType fromValue(String value) throws IllegalStateException {
    	InstanceType m_enum = (InstanceType) _table_.get(value.toUpperCase());
        if (m_enum == null) {
            throw new IllegalStateException(err);
        }
        return m_enum;
    }

    /**
     * Creates a new Os object given an os string.
     * @param value String
     * @throws IllegalStateException Throws Exception if the operating system is not defined in this class.
     * @return Os
     */
    public static InstanceType fromString(String value) throws IllegalStateException {
        return fromValue(value);
    }

    /**
     * Compares if a given Os object is equal to this.
     * @param obj Object
     * @return boolean
     */
    public boolean equals(Object obj) {
        return (obj == this);
    }

    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Returns the string value of the operating system.
     * @return String
     */
    public String toString() {
        return _value_;
    }

}



