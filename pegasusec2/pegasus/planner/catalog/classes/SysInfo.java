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

package edu.isi.pegasus.planner.catalog.classes;

/**
 * A container class to keep system information associated with a Site entry in
 * the Site Catalog or a Transformation in the Transformation Catalog.
 *
 * The class follows the NMI conventions for specifying Architecture/ OS and OS release.
 *
 *
 * @author Karan Vahi
 * @version  $Revision$
 */
public class SysInfo implements Cloneable {

	/**
	 * Amelie: Enumerates the available instance types from Amazon EC2.
	 */
	public enum InstanceType {
		m1small, m1medium, m1large, m1xlarge
	}

    /**
     * Enumerates the new OS types supported in Pegasus.
     */
    public enum OS {
        LINUX, SUNOS, AIX, MACOSX, WINDOWS
    }

    /**
     * Enumerates the new architecture types supported in Pegasus.
     */
    public  enum Architecture {
        x86, x86_64, ppc, ppc_64, ia64,  sparcv7, sparcv9, amd64
    }

	/**
	* Amelie: The default instance type the entry is associated with if none is specified
	*/
	public static final InstanceType DEFAULT_INSTANCETYPE = InstanceType.m1small;

    
    /**
     * The default OS the entry is associated with if none is specified
     */
    public static final OS DEFAULT_OS = OS.LINUX;

    /**
     * The default Architecture the entry is associated with if none is specified
     */
    public static final Architecture DEFAULT_ARCHITECTURE = Architecture.x86;

	/**
     * Amelie: The instance type, only for cloud execution.
     */
    protected InstanceType mInstanceType;

    /**
     * The architecture.
     */
    protected Architecture mArchitecture;

    /**
     * The Operating System.
     */
    protected OS mOS;

    /**
     * The Operating System Release. Optional.
     */
    protected String mOSRelease;

    /**
     * The Operating System Version. Optional.
     */
    protected String mOSVersion;

    /**
     * The Glibc version. Optional.
     */
    protected String mGlibc;



    /**
     * The default constructor.
     */
    public SysInfo(){
    	mInstanceType = SysInfo.DEFAULT_INSTANCETYPE; //Amelie
        mArchitecture = SysInfo.DEFAULT_ARCHITECTURE;
        mOS           = SysInfo.DEFAULT_OS;
        mOSRelease    = "";
        mOSVersion    = "";
        mGlibc        = "";
    }
    /**
     * This constructor takes the system information in the format 
     * arch::os:instancetype:osversion:glibc//Amelie:where is it used?
     * @param system the system information string
     */
    public SysInfo(String system){
    	 if (system != null) {
             String s1[] = system.split("::", 2);
             if (s1.length == 2) {
            	 if(isValidArchitecture(s1[0].trim())){
            		 mArchitecture =Architecture.valueOf(s1[0].trim());
            	 }else {
	                 throw new IllegalStateException(
	                     "Error: Illegal Architecture defined. Please specify one of the predefined types \n [x86, x86_64, ppc, ppc_64, ia64,  sparcv7, sparcv9, amd64]");
            	 }
                 String s2[] = s1[1].split(":", 4);
                 if(isValidOS(s2[0].trim())){
                	 mOS = OS.valueOf(s2[0].trim());
                 }else {
                     throw new IllegalStateException(
                     "Error: Illegal Operating System defined. Please specify one of the predefined types \n [LINUX, SUNOS, AIX, MACOSX, WINDOWS]");
                 }
				 //Amelie
				 if(isValidInstanceType(s2[1].trim())){
					mInstanceType = InstanceType.valueOf(s2[1].trim());
				 }else{
					throw new IllegalStateException(
                     "Error: Illegal Instance Type defined. Please specify one of the predefined types \n [m1small, m1medium, m1large, m1xlarge]");
				 }
				 
                 for (int i = 2; i < s2.length; i++) {
                     if (i == 2) {
                         mOSVersion = s2[i];
                     }
                     if (i == 3) {
                         mGlibc = s2[i];
                     }
                 }
             } else {
                 throw new IllegalStateException(
                     "Error : Please check your system info string");
             }
         } else {
        	 mArchitecture = SysInfo.DEFAULT_ARCHITECTURE;
             mOS           = SysInfo.DEFAULT_OS;
			 mInstanceType = SysInfo.DEFAULT_INSTANCETYPE;//Amelie
             mOSRelease    = "";
             mOSVersion    = "";
             mGlibc        = "";
         }
    }
    /**
     * Checks if the architecture is a valid supported architecture
     * @param arch architecture
     * @return true if it is a valid supported architecture, false otherwise
     */
    private static boolean isValidArchitecture(String arch){
    	for(Architecture architecture : Architecture.values()){
    		if(architecture.toString().equals(arch))
    			return true;
    	}
    	return false;
    }
    /**
     * Checks if the operating system is a valid supported operating system
     * @param os operating system 
     * @return true if it is a valid supported operating system, false otherwise
     */
    private static boolean isValidOS(String os){
    	for(OS osystem : OS.values()){
    		if(osystem.toString().equals(os))
    			return true;
    	}
    	return false;
    }

	/**
     * Amelie: Checks if the instance type is a valid supported type
     * @param type instance type 
     * @return true if it is a valid supported type, false otherwise
     */
    private static boolean isValidInstanceType(String type){
    	for(InstanceType types : InstanceType.values()){
    		if(types.toString().equals(type))
    			return true;
    	}
    	return false;
    }
	
	/**
     * Amelie: Sets the instance type of the site.
     *
     * @param type  the instance type.
     */
    public void setInstanceType( InstanceType type ){
        mInstanceType= type;
    }


    /**
     *  Amelie: Returns the instance type of the site.
     *
     * @return  the instance type.
     */
    public InstanceType getInstanceType( ){
        return mInstanceType;
    }
	
    /**
     * Sets the architecture of the site.
     *
     * @param arch  the architecture.
     */
    public void setArchitecture( Architecture arch ){
        mArchitecture = arch;
    }


    /**
     * Returns the architecture of the site.
     *
     * @return  the architecture.
     */
    public Architecture getArchitecture( ){
        return mArchitecture;
    }


    /**
     * Sets the OS of the site.
     *
     * @param os the os of the site.
     */
    public void setOS( OS os ){
        mOS = os;
    }


    /**
     * Returns the OS of the site.
     *
     * @return  the OS
     */
    public OS getOS( ){
        return mOS;
    }

    /**
     * Sets the OS release of the site.
     *
     * @param release the os releaseof the site.
     */
    public void setOSRelease( String release ){
        mOSRelease = release;
    }


    /**
     * Returns the OS release of the site.
     *
     * @return  the OS
     */
    public String getOSRelease( ){
        return mOSRelease;
    }

    /**
     * Sets the OS version of the site.
     *
     * @param version  the os versionof the site.
     */
    public void setOSVersion( String version ){
        mOSVersion = version;
    }


    /**
     * Returns the OS version of the site.
     *
     * @return  the OS
     */
    public String getOSVersion( ){
        return mOSVersion;
    }

    /**
     * Sets the glibc version on the site.
     *
     * @param version  the glibc version of the site.
     */
    public void setGlibc( String version ){
        mGlibc = version;
    }


    /**
     * Returns the glibc version of the site.
     *
     * @return  the OS
     */
    public String getGlibc( ){
        return mGlibc;
    }

    /**
     * Check if the system information matches.
     * Amelie: add instance type check
     * @param obj to be compared.
     *
     * @return boolean
     */
    public boolean equals(Object obj) {
        boolean result = false;
        if( obj instanceof SysInfo ){
            SysInfo sysinfo = (SysInfo)obj;

            result = this.getArchitecture().equals( sysinfo.getArchitecture() ) &&
					 this.getInstanceType().equals( sysinfo.getInstanceType() ) && //Amelie
                     this.getOS().equals( sysinfo.getOS() ) &&
                     this.getOSRelease().equals( sysinfo.getOSRelease() ) &&
                     this.getOSVersion().equals( sysinfo.getOSVersion() ) &&
                     this.getGlibc().equals( sysinfo.getGlibc() );
        }
        return result;
    }

    /**
     * Returns the clone of the object.
     *
     * @return the clone
     */
    public Object clone(){
        SysInfo obj = null;
        try{
            obj = ( SysInfo ) super.clone();
            obj.setArchitecture( this.getArchitecture() );
            obj.setOS( this.getOS() );
			obj.setInstanceType( this.getInstanceType() );//Amelie

            obj.setOSRelease( this.getOSRelease() );
            obj.setOSVersion( this.getOSVersion() );
            obj.setGlibc( this.getGlibc() );
        }
        catch( CloneNotSupportedException e ){
            //somewhere in the hierarch chain clone is not implemented
            throw new RuntimeException("Clone not implemented in the base class of " + this.getClass().getName(),
                                       e );
        }
        return obj;

    }

    /**
     * Returns the output of the data class as string.
     * @return String
     */
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append( "{" );
        s.append( "arch=" + this.getArchitecture() );
        s.append( " os=" + this.getOS() );
		s.append( " instancetype=" + this.getInstanceType() );//Amelie

        String release = this.getOSRelease();
        if ( release  != null && release.length() > 0 ) {
            s.append( " osrelease=" + release );
        }

        String version = this.getOSVersion();
        if ( version  != null && version.length() > 0 ) {
            s.append( " osversion=" + version );
        }
        s.append( "}" );
        return s.toString();
    }

}
