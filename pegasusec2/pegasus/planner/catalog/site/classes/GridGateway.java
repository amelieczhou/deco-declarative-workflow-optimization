/*
 * 
 *   Copyright 2007-2008 University Of Southern California
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package edu.isi.pegasus.planner.catalog.site.classes;




import edu.isi.pegasus.planner.catalog.classes.SysInfo;
import java.io.Writer;
import java.io.IOException;

/**
 * This class describes the Grid Gateway into a site.
 * 
 * @version $Revision$
 * @author Karan Vahi
 */
public class GridGateway extends AbstractSiteData{

   



    /**
     * An enumeration of valid types of grid gateway.
     */
    public static enum TYPE { gt2, gt4, gt5, condor, cream, batch, pbs, lsf, sge, nordugrid, unicore, ec2, deltacloud };
    
    /**
     * An enumeration of types of jobs handled by an instance of a grid gateway.
     */
    public static enum JOB_TYPE{ compute, auxillary, transfer, register, cleanup };
    
    
    /**
     * An enumeration of valid schedulers on the grid gateway.
     */
    public static enum SCHEDULER_TYPE{ Fork, LSF, PBS, Condor, SGE, Unknown };
    
    
    /**
     * The grid type associated with this instance.
     */
    private TYPE mType;
    
    /**
     * The contact string for the grid gateway.
     */
    private String mContact;
    
    /**
     * The scheduler type with which it interfaces.
     */
    private SCHEDULER_TYPE mScheduler;
    
    /**
     * The type of jobs that this grid gateway can accept.
     */
    private JOB_TYPE mJobType;
    
    /**
     * An optional os type for the grid gateway.
     */
    private SysInfo.OS mOS;
    
    /**
     * An optional architecture type for the grid gateway.
     */
    private SysInfo.Architecture mArch;

	/**
     * Amelie: An optional instance type for the grid gateway.
     */
    private SysInfo.InstanceType mInstanceType;
    
    /**
     * Optional information about the os release.
     */
    private String mOSRelease;
    
    /**
     * Optional information about the version.
     */
    private String mOSVersion;
    
    /**
     * Optional information about the glibc.
     */
    private String mGlibc;
    
    /**
     * The number of idle nodes.
     */
    private int mIdleNodes;
    
    /**
     * The number of total nodes
     */
    private int mTotalNodes;
    
    /**
     * The default constructor.
     */
    public GridGateway( ){
        this( TYPE.gt2, "localhost/jobmanager-fork", SCHEDULER_TYPE.Fork );
    }
    
    /**
     * The overloaded constructor.
     * 
     * @param type          the type of grid gateway.
     * @param contact       the contact string for it.
     * @param scheduler     the underlying scheduler that it talks to.
     */
    public GridGateway( TYPE type, String contact, SCHEDULER_TYPE scheduler ){
        mType = type;
        mContact = contact;
        mScheduler = scheduler;
        mIdleNodes  = -1;
        mTotalNodes = -1;
    }
    
    
    /**
     * Sets the type of jobs that run via this grid gateway.
     * 
     * @param type  the job type.
     */
    public void setJobType( JOB_TYPE type ){
        mJobType = type;
    }
    
    
    /**
     * Returns the type of jobs that run via this grid gateway.
     * 
     * @return  the job type.
     */
    public JOB_TYPE getJobType( ){
        return mJobType;
    }
    
     /**
     * Sets the type of  grid gateway.
     * 
     * @param type  the gateway type.
     */
    public void setType( TYPE type ){
        mType = type;
    }
    
    
    /**
     * Returns the type of this grid gateway.
     * 
     * @return  the type of grid gateway
     */
    public TYPE getType( ){
        return mType;
    }

    /**
     * Sets the contact string for the Grid gateway
     * 
     * @param contact  the contact string
     */
    public void setContact( String contact ){
        mContact = contact;
    }
    
    
    /**
     * Returns the contact string for the Grid gateway
     * 
     * @return  the contact string
     */
    public String getContact( ){
        return mContact;
    }
    
    /**
     * Sets the number of total nodes that this grid gateway talks to.
     * 
     * @param num the number of nodes.
     */
    public void setTotalNodes( String num ){
        try{
            mTotalNodes = Integer.parseInt( num );
        }
        catch( Exception e ){
            //empty catch
        }
    }
    
    /**
     * Sets the total number of nodes that this grid gateway talks to.
     * 
     * @param num the number of nodes.
     */
    public void setTotalNodes( int num ){
        mTotalNodes = num;
    }
    
    
    /**
     * Returns the total number of nodes that this grid gateway talks to.
     * If they are not set then -1 is returned.
     * 
     * @return the number of nodes, or -1 if not set.
     */
    public int getTotalNodes(  ){
        return mTotalNodes;
    }
    
    /**
     * Sets the number of idle nodes that this grid gateway talks to.
     * 
     * @param num the number of nodes.
     */
    public void setIdleNodes( String num ){
        try{
        mIdleNodes = Integer.parseInt( num );
        }
        catch( Exception e ){
            //empty catch
        }
    }
    
    /**
     * Sets the number of idle nodes that this grid gateway talks to.
     * 
     * @param num the number of nodes.
     */
    public void setIdleNodes( int num ){
        mIdleNodes = num;
    }
    
    
    /**
     * Returns the total number of nodes that this grid gateway talks to.
     * If they are not set then -1 is returned.
     * 
     * @return the number of nodes, or -1 if not set.
     */
    public int getIdleNodes(  ){
        return mIdleNodes;
    }
    
    /**
     * Sets the the underlying scheduler that gateway talks to.
     * In case the value does not match any of the predefined enumeration,
     * the SCHEDULER_TYPE is set to Unknown.
     *
     * @param value  the string value
     *
     * @see SCHEDULER_TYPE
     */
    public void setScheduler(String value) {
        try{
           mScheduler = GridGateway.SCHEDULER_TYPE.valueOf( value );
        }
        catch( IllegalArgumentException iae ){
            //set the scheduler to unknown
            mScheduler = GridGateway.SCHEDULER_TYPE.Unknown;
        }
    }

    /**
     * Sets the the underlying scheduler that gateway talks to.
     * 
     * @param scheduler the scheduler.
     */
    public void setScheduler( SCHEDULER_TYPE scheduler ){
        mScheduler = scheduler;
    }
    
    /**
     * Returns the the underlying scheduler that gateway talks to.
     * 
     * @return  the scheduler.
     */
    public SCHEDULER_TYPE getScheduler( ){
        return mScheduler;
    }
    
    
    /**
     * Sets the Instance type of the nodes that this grid gateway talks to.
     * 
     * @param type   the instance type
     */
    public void setInstanceType( SysInfo.InstanceType type ){
        mInstanceType = type;
    }

    /**
     * Returns the Instance type of the nodes that this grid gateway talks to.
     * 
     * @return the Instance type if set else null
     */
    public SysInfo.InstanceType getInstanceType( ){
        return mInstanceType;
    }
    /**
     * Sets the OS of the nodes that this grid gateway talks to.
     * 
     * @param os   the os
     */
    public void setOS( SysInfo.OS os ){
        mOS = os;
    }

    /**
     * Returns the OS of the nodes that this grid gateway talks to.
     * 
     * @return the os if set else null
     */
    public SysInfo.OS getOS( ){
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
     * Sets the architecture of the nodes that this grid gateway talks to.
     * 
     * @param arch   the architecture of the nodes
     */
    public void setArchitecture( SysInfo.Architecture arch ){
        mArch = arch;
    }

    
    /**
     * Returns the architecture of the nodes that this grid gateway talks to.
     * 
     * @return the architecture if set else null
     */
    public SysInfo.Architecture getArchitecture( ){
        return mArch;
    }
    
    /**
     * Writes out the xml description of the object. 
     *
     * @param writer is a Writer opened and ready for writing. This can also
     *               be a StringWriter for efficient output.
     * @param indent the indent to be used.
     *
     * @exception IOException if something fishy happens to the stream.
     */
    public void toXML( Writer writer, String indent ) throws IOException {
        String newLine = System.getProperty( "line.separator", "\r\n" );
        String newIndent = indent + "\t";
        
        //write out the  xml element
        writer.write( indent );
        writer.write( "<grid " );        
        writeAttribute( writer, "type", getType().toString() );        
        writeAttribute( writer, "contact", getContact() );        
        writeAttribute( writer, "scheduler", getScheduler().toString() );
        writeAttribute( writer, "jobtype", getJobType().toString() );
        
        if( mOS != null ){            
            writeAttribute( writer, "os", getOS().toString() );
        }
        if( mArch != null ){            
            writeAttribute( writer, "arch", getArchitecture().toString() );
        }
        //Amelie
        if( mInstanceType != null ){            
            writeAttribute( writer, "instancetype", getInstanceType().toString() );
        }
        String val = null;
        if ( ( val = this.getOSRelease() ) != null ){
            writeAttribute( writer, "osrelease", val );
        }
        
        if ( ( val = this.getOSVersion() ) != null ){
            writeAttribute( writer, "osversion", val );
        }
         
        if ( ( val = this.getGlibc() ) != null ){
            writeAttribute( writer, "glibc", val );
        }
        
        if( this.getIdleNodes() != -1 ){
            writeAttribute( writer, "idle-nodes", Integer.toString( this.getIdleNodes() ));
        }
                
        if( this.getTotalNodes() != -1 ){
            writeAttribute( writer, "total-nodes", Integer.toString( this.getTotalNodes() ));
        }
        
        writer.write( "/>");
        writer.write( newLine );
        
    }

    /**
     * Returns the clone of the object.
     *
     * @return the clone
     */
    public Object clone(){
        GridGateway obj;
        try{
            obj = ( GridGateway ) super.clone();
            obj.setType( this.getType() );
            obj.setContact( this.getContact() );
            obj.setScheduler( this.getScheduler() );
            obj.setJobType( this.getJobType() );
            obj.setOS( this.getOS() );
            obj.setArchitecture( this.getArchitecture() );
            //Amelie
            obj.setInstanceType(this.getInstanceType());
            
            obj.setOSRelease( this.getOSRelease() );
            obj.setOSVersion( this.getOSVersion() );
            obj.setGlibc( this.getGlibc() );
            obj.setTotalNodes( this.getTotalNodes());
            obj.setIdleNodes( this.getIdleNodes() );
        }
        catch( CloneNotSupportedException e ){
            //somewhere in the hierarch chain clone is not implemented
            throw new RuntimeException("Clone not implemented in the base class of " + this.getClass().getName(),
                                       e );
        }
        return obj;
    }

    /**
     * Accepts a Site Data Visitor
     * @param visitor
     */
    public void accept(SiteDataVisitor visitor) throws IOException{
        visitor.visit( this );
        visitor.depart( this );
    }
}
