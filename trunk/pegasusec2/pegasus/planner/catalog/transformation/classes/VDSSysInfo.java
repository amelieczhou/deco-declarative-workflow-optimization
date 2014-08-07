/**
 *  Copyright 2007-2008 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package edu.isi.pegasus.planner.catalog.transformation.classes;

/**
 * This class keeps the system information associated with a
 * resource or transformation.
 *
 * @author Gaurang Mehta gmehta@isi.edu
 * @version $Revision$
 */

public class VDSSysInfo{

    /**
     * Architecture of the system.
     */
    private Arch arch;

    /**
     * Os of the system.
     */
    private Os os;
    
    /** Amelie: instance type of Amazon EC2 
     *  not sure what is VDS?
     * */
    private InstanceType instancetype;

    /**
     * Os version of the system.
     */
    private String osversion;

    /**
     * Glibc version of the system
     */
    private String glibc;

    /**
     * The secondary convenience constructor.
     * @param arch Arch  The architecture of the system.
     * @param os Os The os of the system.
     * @param osversion String  The os version of the system.
     * @param glibc String The glibc version of the system.
     * @see Arch
     * @see Os
     */
    public VDSSysInfo(Arch arch, Os os, InstanceType type ,String osversion, String glibc) {
        this.arch = (arch == null) ? Arch.INTEL32 : arch;
        this.instancetype = (type == null) ? InstanceType.SMALL : type;
        this.os = (os == null) ? Os.LINUX : os;

        this.osversion = (osversion == null || osversion.equals("") ) ?
                          null:
                          osversion;

        this.glibc = (glibc == null || glibc.equals(""))?
                      null:
                      glibc;
    }



    /**
     * Another convenience constructor that uses all entries as strings.
     * @param arch String
     * @param os String
     * @param glibc String
     * @param instancetype String
     */
    public VDSSysInfo(String arch, String os, String type, String glibc) {
        this( arch, os, type, null, glibc );
    }

    /**
     * Another convenience constructor that uses all entries as strings.
     * @param arch String
     * @param os String
     * @param osversion String
     * @param glibc String
     */
    public VDSSysInfo(String arch, String os, String type, String osversion, String glibc) {
        this.arch = (arch == null) ? Arch.INTEL32 : Arch.fromString(arch);
        this.os = (os == null) ? Os.LINUX : Os.fromString(os);
        this.instancetype = (type == null) ? InstanceType.SMALL : InstanceType.fromString(type);
        this.osversion = (osversion == null || osversion.equals("") ) ?
                          null:
                          osversion;

        this.glibc = (glibc == null || glibc.equals(""))?
                      null:
                      glibc;
    }


/**Amelie: revise the sysinfo
 * format: arch::os:instance:osversion:glibc
 * */

    public VDSSysInfo(String system) {
        if (system != null) {
            String s1[] = system.split("::", 2);
            if (s1.length == 2) {
                arch = Arch.fromString(s1[0]);
                String s2[] = s1[1].split(":", 4);
                os = Os.fromString(s2[0]);
                instancetype = InstanceType.fromValue(s2[1]);
                for (int i = 2; i < s2.length; i++) {
                    if (i == 2) {
                        osversion = s2[i];
                    }
                    if (i == 3) {
                        glibc = s2[i];
                    }
                }
            } else {
                throw new IllegalStateException(
                    "Error : Please check your system info string");
            }
        } else {
            this.arch = Arch.INTEL32;
            this.os = Os.LINUX;
            this.instancetype = InstanceType.SMALL;
        }
    }

    /**
     * The default constructor.
     * Sets the sysinfo to INTEL32::LINUX
     */
    public VDSSysInfo() {
        this.arch=Arch.INTEL32;
        this.os=Os.LINUX;
        this.instancetype = InstanceType.SMALL;//Amelie, default small?
    }

    /**
     * Sets the architecture of the system.
     * @param arch Arch
     * @see Arch
     */
    public void setArch(Arch arch) {
        this.arch = (arch == null) ? Arch.INTEL32 : arch;
    }

    /**
     * Sets the Os of the sytem.
     * @param os Os
     * @see Os
     */
    public void setOs(Os os) {
        this.os = (os == null) ? Os.LINUX : os;
    }
    /**
     * Sets the instance type of the sytem.
     * @param type InstanceType
     * @see InstanceType
     */
    public void setInstanceType(InstanceType type) {
        this.instancetype = (type == null) ? InstanceType.SMALL : type;
    }
    /**
     * Sets the Os version of the system.
     * @param osversion String
     */
    public void setOsversion(String osversion) {
        this.osversion = osversion;
    }

    /**
     * Sets the glibc version of the system
     * @param glibc String
     */
    public void setGlibc(String glibc) {
        this.glibc = glibc;
    }

    /**
     * Returns the architecture of the sytem.
     * @return Arch
     * @see Arch
     */
    public Arch getArch() {
        return arch;
    }

    /**
     * Returns the os type of the system.
     * @return Os
     * @see Os
     */
    public Os getOs() {
        return os;
    }
    
    /**
     * Returns the os type of the system.
     * @return Os
     * @see Os
     */
    public InstanceType getInstanceType() {
        return instancetype;
    }
    
    /**
     * Returns the os version of the system.
     * @return String
     */
    public String getOsversion() {
        return osversion;
    }

    /**
     * Retuns the glibc version of the system.
     * @return String
     */
    public String getGlibc() {
        return glibc;
    }

    /**
     * Return a copy of this Sysinfo object
     * @return Object
     */
    public Object clone() {
        return new VDSSysInfo(arch, os, instancetype, osversion, glibc);
    }

    /**
     * Check if the system information matches.
     * @param obj to be compared.
     * @return boolean
     */
    public boolean equals(Object obj) {
        boolean result = false;
        if(obj instanceof VDSSysInfo){
            VDSSysInfo sysinfo = (VDSSysInfo)obj;
            result = (arch.equals(sysinfo.getArch())
                      && os.equals(sysinfo.getOs()))
                      && instancetype.equals(sysinfo.getInstanceType());
        }
        return result;
    }
    /**
     * Returns the output of the data class as string.
     * @return String
     */
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append(arch + "::" + os);
        if(instancetype != null){
        	s.append(":" + instancetype);
        }
        if (osversion != null && !osversion.isEmpty()) {
            s.append(":" + osversion);
        }
        if (glibc != null && !glibc.isEmpty()) {
            s.append(":" + glibc);
        }
        return s.toString();
    }
}
