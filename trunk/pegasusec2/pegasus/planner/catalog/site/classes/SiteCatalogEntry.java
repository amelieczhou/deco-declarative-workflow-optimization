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

package edu.isi.pegasus.planner.catalog.site.classes;

import edu.isi.pegasus.planner.catalog.classes.Profiles;
import edu.isi.pegasus.planner.catalog.classes.VDSSysInfo2NMI;

import edu.isi.pegasus.planner.catalog.classes.Profiles.NAMESPACES;
import edu.isi.pegasus.planner.catalog.classes.SysInfo;
import edu.isi.pegasus.planner.catalog.site.classes.GridGateway.JOB_TYPE;

import edu.isi.pegasus.planner.catalog.transformation.classes.VDSSysInfo;
import edu.isi.pegasus.planner.catalog.transformation.classes.NMI2VDSSysInfo;

import edu.isi.pegasus.planner.classes.Profile;

import edu.isi.pegasus.planner.common.PegRandom;


import edu.isi.pegasus.planner.namespace.Namespace;
import edu.isi.pegasus.planner.namespace.Pegasus;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import java.io.File;
import java.io.Writer;
import java.io.IOException;
        
/**
 * This data class describes a site in the site catalog.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class SiteCatalogEntry extends AbstractSiteData{

    /**
     * The name of the environment variable PEGASUS_BIN_DIR.
     */
    public static final String PEGASUS_BIN_DIR = "PEGASUS_BIN_DIR";
    
    /**
     * The name of the environment variable PEGASUS_HOME.
     */
    public static final String PEGASUS_HOME = "PEGASUS_HOME";

    /**
     * The name of the environment variable VDS_HOME.
     */
    public static final String VDS_HOME = "VDS_HOME";
  
   
    /**
     * The site identifier. 
     */
    private String mID;
    
    /**
     * The System Information for the Site.
     */
    private SysInfo mSysInfo;

    /**
     * The profiles asscociated with the site.
     */
    private Profiles mProfiles;
    
    /**
     * The handle to the head node filesystem.
     */
//    private HeadNodeFS mHeadFS;
    
    /**
     * The handle to the worker node filesystem.
     */
//    private WorkerNodeFS mWorkerFS;

    /**
     * A Map of different directories indexed by Directory.TYPE associated
     * with the site catalog entry
     */
    private Map<Directory.TYPE, Directory> mDirectories;
    
    /**
     * Map of grid gateways at the site for submitting different job types.
     */
    private Map<GridGateway.JOB_TYPE, GridGateway> mGridGateways;
    
    /**
     * The list of replica catalog associated with the site.
     */
    private List<ReplicaCatalog> mReplicaCatalogs;

    /**
     * The default constructor.
     */
    public SiteCatalogEntry() {
        this( "" );
    }
    
    /**
     * The overloaded constructor.
     * 
     * @param id   the site identifier.
     */
    public SiteCatalogEntry( String id ) {
        initialize( id );
    }

    /**
     * Not implmented as yet.
     *
     * @return UnsupportedOperationException
     */
    public Iterator getFileServerIterator() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Not implemented as yet.
     *
     * @return UnsupportedOperationException
     */
    public List getFileServers() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Not implemented as yet
     *
     * @return UnsupportedOperationException
     */
    public List getGridGateways() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    /**
     * Initializes the object.
     * 
     * @param id   the site identifier.
     */
    public void initialize( String id ){        
        mID       = id;
        mSysInfo  = new SysInfo();
        mDirectories = new HashMap<Directory.TYPE, Directory>();
        mProfiles        = new Profiles();
        mGridGateways    = new HashMap();
        mReplicaCatalogs = new LinkedList();
    }
    
    /**
     * Sets the site handle for the site
     * 
     * @param id  the site identifier.
     */
    public void setSiteHandle( String id ){
        mID = id;
    }
    
    
    /**
     * Returns the site handle for the site
     * 
     * @return  the site identifier.
     */
    public String getSiteHandle( ){
        return mID;
    }


    /**
     * Sets the System Information associated with the Site.
     *
     *
     * @param sysinfo  the system information of the site.
     */
    public void setSysInfo( SysInfo sysinfo  ) {
        mSysInfo = sysinfo;
    }

    /**
     * Returns the System Information associated with the Site.
     *
     *
     * @return SysInfo the system information.
     */
    public SysInfo getSysInfo(  ) {
        return mSysInfo;
    }

	 /**
     * Amelie: Sets the instance type of the site.
     * 
     * @param type  the instance type.
     */
    public void setInstanceType( SysInfo.InstanceType type ){
        mSysInfo.setInstanceType(type);
    }
    
    
    /**
     * Amelie: Returns the instance type of the site.
     * 
     * @return  the instance type.
     */
    public SysInfo.InstanceType getInstanceType( ){
        return mSysInfo.getInstanceType();
    }
	
    /**
     * Sets the architecture of the site.
     * 
     * @param arch  the architecture.
     */
    public void setArchitecture( SysInfo.Architecture arch ){
        mSysInfo.setArchitecture(arch);
    }
    
    
    /**
     * Returns the architecture of the site.
     * 
     * @return  the architecture.
     */
    public SysInfo.Architecture getArchitecture( ){
        return mSysInfo.getArchitecture();
    }
    
    
    /**
     * Sets the OS of the site.
     * 
     * @param os the os of the site.
     */
    public void setOS( SysInfo.OS os ){
        mSysInfo.setOS(os);
    }
    
    
    /**
     * Returns the OS of the site.
     * 
     * @return  the OS
     */
    public SysInfo.OS getOS( ){
        return mSysInfo.getOS();
    }
    
    /**
     * Sets the sysinfo for the site.
     * 
     * @param  sysinfo
     */
    public void setVDSSysInfo( VDSSysInfo sysinfo ){
        this.setSysInfo( VDSSysInfo2NMI.vdsSysInfo2NMI(sysinfo));
    }

    /**
     * Returns the sysinfo for the site.
     * 
     * @return getVDSSysInfo
     */
    public VDSSysInfo getVDSSysInfo(){
        return  NMI2VDSSysInfo.nmiToVDSSysInfo(mSysInfo);
                            
    }
    
    /**
     * Sets the OS release of the site.
     * 
     * @param release the os releaseof the site.
     */
    public void setOSRelease( String release ){
        mSysInfo.setOSRelease(release);
    }
    
    
    /**
     * Returns the OS release of the site.
     * 
     * @return  the OS
     */
    public String getOSRelease( ){
        return mSysInfo.getOSRelease();
    }
    
    /**
     * Sets the OS version of the site.
     * 
     * @param version  the os versionof the site.
     */
    public void setOSVersion( String version ){
        mSysInfo.setOSVersion(version);
    }
    
    
    /**
     * Returns the OS version of the site.
     * 
     * @return  the OS
     */
    public String getOSVersion( ){
        return mSysInfo.getOSVersion();
    }
    
    /**
     * Sets the glibc version on the site.
     * 
     * @param version  the glibc version of the site.
     */
    public void setGlibc( String version ){
        mSysInfo.setGlibc(version);
    }
    
    
    /**
     * Returns the glibc version of the site.
     * 
     * @return  the OS
     */
    public String getGlibc( ){
        return mSysInfo.getGlibc();
    }

    /**
     * Adds a directory to the site catalog entry object.
     * Adding a directory automatically will add a head node filesystem
     * and a worker node filesystem to the SiteCatalogEntry if it does not exist
     * already.
     * The mapping followed is
     * <pre>
     *  shared-scratch  ->  HeadNodeFS shared scratch
     *  shared-storage  ->  HeadNodeFS shared storage
     *  local-scratch   ->  WorkerNodeFS local scratch
     *  local-storage   ->  HeadNodeFS local storage
     * </pre>
     *
     * @param directory  the directory to be added
     *//*
    public void addDirectory( Directory directory ) {
        Directory.TYPE type = directory.getType();

        if( type == Directory.TYPE.shared_scratch ){
            HeadNodeFS headnode = this.getHeadNodeFS();
            if( headnode == null ){
                headnode = new HeadNodeFS();
                HeadNodeScratch scratch = new HeadNodeScratch();
                scratch.setSharedDirectory( directory );
                headnode.setScratch( scratch );
                this.setHeadNodeFS( headnode );
            }
            else{
                //retrive from existing
                HeadNodeScratch scratch = headnode.getScratch();
                if( scratch == null ){
                    scratch = new HeadNodeScratch();
                    headnode.setScratch(scratch);
                }
                //get the shared filesystem
               scratch.setSharedDirectory( directory );
            }
        }
        else if( type == Directory.TYPE.shared_storage ){
            HeadNodeFS headnode = this.getHeadNodeFS();
            if( headnode == null ){
                headnode = new HeadNodeFS();
                HeadNodeStorage storage = new HeadNodeStorage();
                storage.setSharedDirectory( directory );
                headnode.setStorage( storage );
                this.setHeadNodeFS( headnode );
            }
            else{
                //retrieve from existing
                HeadNodeStorage storage = headnode.getStorage();
                if( storage == null ){
                    storage = new HeadNodeStorage();
                    headnode.setStorage( storage );
                }
                //set the shared filesystem
               storage.setSharedDirectory( directory );
            }
        }
        else if( type == Directory.TYPE.local_scratch ){
            WorkerNodeFS workernode = this.getWorkerNodeFS();
            if( workernode == null ){
                workernode = new WorkerNodeFS();
                WorkerNodeScratch scratch = new WorkerNodeScratch();
                scratch.setLocalDirectory( directory );
                workernode.setScratch( scratch );
                this.setWorkerNodeFS( workernode );
            }
            else{
                //retrieve from existing
                WorkerNodeScratch scratch = workernode.getScratch();
                if( scratch == null ){
                    scratch = new WorkerNodeScratch();
                    workernode.setScratch(scratch);
                }
                //set the shared filesystem
               scratch.setLocalDirectory( directory );
            }
        }
        /// we now map HeadNode local storage
        else if( type == Directory.TYPE.local_storage ){
            WorkerNodeFS workernode = this.getWorkerNodeFS();
            if( workernode == null ){
                workernode = new WorkerNodeFS();
                WorkerNodeStorage storage = new WorkerNodeStorage();
                storage.setLocalDirectory( directory );
                workernode.setStorage( storage );
                this.setWorkerNodeFS( workernode );
            }
            else{
                //retrieve from existing
                WorkerNodeStorage storage = workernode.getStorage();
                if( storage == null ){
                    storage = new WorkerNodeStorage();
                    workernode.setStorage( storage );
                }
                //set the shared filesystem
               storage.setLocalDirectory( directory );
            }
        }
         
        else if( type == Directory.TYPE.local_storage ){
            HeadNodeFS headnode = this.getHeadNodeFS();
            if( headnode == null ){
                headnode = new HeadNodeFS();
                HeadNodeStorage storage = new HeadNodeStorage();
                storage.setLocalDirectory( directory );
                headnode.setStorage( storage );
                this.setHeadNodeFS( headnode );
            }
            else{
                //retrieve from existing
                HeadNodeStorage storage = headnode.getStorage();
                if( storage == null ){
                    storage = new HeadNodeStorage();
                    headnode.setStorage( storage );
                }
                //set the shared filesystem
               storage.setLocalDirectory( directory );
            }
        }

    }*/

    /**
     * Adds a directory internally. Complains if directory of same type already
     * exists
     *
     * @param directory   the directory to be added.
     */
    public void addDirectory(Directory directory) {
        //check for existence
        if( this.mDirectories.containsKey( directory.getType() ) ){
            StringBuffer error = new StringBuffer();
            error.append( "Unable to add Directory " ).append( directory.getInternalMountPoint() )
                 .append( " for site " ).append(  this.getSiteHandle() );
            throw new RuntimeException( error.toString() );

        }

        this.mDirectories.put( directory.getType(), directory);
    }

    /**
     * Sets a directory corresponding to a particular type
     *
     * @param directory the directory to be set
     */
    public void setDirectory( Directory directory ){
         this.mDirectories.put( directory.getType(), directory );
    }

    /**
     * Returns a directory corresponding to a particular type
     *
     * @return the iterator
     */
    public Iterator<Directory> getDirectoryIterator(   ){
        return this.mDirectories.values().iterator();
    }

    /**
     * Returns a directory corresponding to a particular type
     *
     * @param the type the directory type
     */
    public Directory getDirectory( Directory.TYPE type ){
        return this.mDirectories.get( type );
    }
    
    /**
     * Returns the local-storage directory.
     * If it is not specified, then returns shared-storage
     * If none is associated, then returns null
     * 
     * @return  the appropriate  directory
     */
    public Directory getHeadNodeStorageDirectory() {
        Directory result = this.getDirectory( Directory.TYPE.local_storage );
        if( result == null ){
            result = this.getDirectory(Directory.TYPE.shared_storage );
        }
        return result;
    }

   
    
    /**
     * Returns the work directory for the compute jobs on a site. 
     * 
     * Currently, the work directory is picked up from the head node shared filesystem.
     * 
     * @return the internal mount point, else null
     */
    public String getInternalMountPointOfWorkDirectory() {
        Directory dir = this.getDirectory( Directory.TYPE.shared_scratch );
        return ( dir == null ) ? null : dir.getInternalMountPoint().getMountPoint();

    }
    
    /**
     * Adds a profile.
     * 
     * @param p  the profile to be added
     */
    public void addProfile( Profile p ){
        //retrieve the appropriate namespace and then add
       mProfiles.addProfile(  p );
    }
    
    /**
     * Sets the profiles associated with the file server.
     * 
     * @param profiles   the profiles.
     */
    public void setProfiles( Profiles profiles ){
        mProfiles = profiles;
    }
    
    /**
     * Returns the profiles associated with the site.
     * 
     * @return profiles.
     */
    public Profiles getProfiles( ){
        return mProfiles;
    }
    
    /**
     * Returns the value of VDS_HOME for a site.
     *
     * 
     * @return value if set else null.
     */
    @Deprecated public String getVDSHome( ){
        
        String s = this.getEnvironmentVariable( VDS_HOME );
        if (s != null && s.length() > 0) {
            return s;
        }
        
        // fall back on bin dir - this is to ensure  a smooth transition to FHS
        s = this.getEnvironmentVariable( PEGASUS_BIN_DIR );
        if (s != null && s.length() > 0) {
            File f = new File(s + "/..");
            return f.getAbsolutePath();
        }

        return null;
    }


    /**
     * Returns the value of PEGASUS_HOME for a site.
     *
     * 
     * @return value if set else null.
     */
    @Deprecated public String getPegasusHome( ){
        
        String s = this.getEnvironmentVariable( PEGASUS_HOME );
        if (s == null || s.length() == 0) {
            // fall back on bin dir - this is to ensure  a smooth transition to FHS
            s = this.getEnvironmentVariable( PEGASUS_BIN_DIR );
            if (s != null && s.length() > 0) {
                s += "/..";
            }
            
        }
        
        // normalize the path
        if (s != null && s.length() > 0) {
            File f = new File(s);
            try {
                s = f.getAbsolutePath();
            }
            catch (Exception e) {
                // ignore - just leave s alone
            }
        }
        else {
            s = null;
        }
        
        return s;
    }
    
    
    
    /**
     * Returns an environment variable associated with the site.
     *
     * @param variable  the environment variable whose value is required.
     *
     * @return value of the environment variable if found, else null
     */
    public String getEnvironmentVariable( String variable ){
        Namespace n = this.mProfiles.get( Profiles.NAMESPACES.env );
        String value = ( n == null ) ? null : (String)n.get( variable );


        //change the preference order because of JIRA PM-471
        if( value == null ){
            //fall back only for local site the value in the env
            String handle = this.getSiteHandle();
            if( handle != null && handle.equals( "local" ) ){
                //try to retrieve value from environment
                //for local site.
                value = System.getenv( variable );
            }
        }

        return value;
    }

    
    /**
     * Returns a grid gateway object corresponding to a job type.
     * 
     * @param type the job type
     * 
     * @return GridGateway
     */
    public GridGateway getGridGateway( GridGateway.JOB_TYPE type ){
        return mGridGateways.get( type );
    }
    
    /**
     * Selects a grid gateway object corresponding to a job type.
     * It also defaults to other GridGateways if grid gateway not found for
     * that job type.
     *
     * @param type the job type
     * 
     * @return GridGateway
     */
    public GridGateway selectGridGateway( GridGateway.JOB_TYPE type ){
        GridGateway g = this.getGridGateway( type );
        if( g == null ){
            if( type == JOB_TYPE.transfer || type == JOB_TYPE.cleanup || type == JOB_TYPE.register ){
                return this.selectGridGateway( JOB_TYPE.auxillary );
            }
            else if ( type == JOB_TYPE.auxillary ){
                return this.selectGridGateway( JOB_TYPE.compute );
            }
        }
        return g;
    }
    
    
    /**
     * A convenience method to select the URL Prefix for the FileServer for 
     * the shared scratch space on the HeadNode matching a particular operation.
     *
     * For get and put operations, the results default back to searching for an
     * ALL operation server.
     *
     *
     * @param operation  the operation for which the file server is required
     * 
     * @return  URL Prefix for the FileServer for the shared scratch space , else null
     * @deprecated should be removed
     */
    public String selectHeadNodeScratchSharedFileServerURLPrefix( FileServer.OPERATION operation ){
        FileServer server = this.selectHeadNodeScratchSharedFileServer( operation );
        return ( server == null )?
               null:
               server.getURLPrefix();
    }
    
    /**
     * A convenience method to select the FileServer for the shared scratch
     * space on the HeadNode.
     *
     * For get and put operations, the results default back to searching for an
     * ALL operation server.
     *
     *
     * @param operation  the operation for which the file server is required
     * 
     * @return  FileServer for the shared scratch space , else null
     */
    public FileServer selectHeadNodeScratchSharedFileServer( FileServer.OPERATION operation ){
        Directory dir = this.getDirectory( Directory.TYPE.shared_scratch );

        //sanity check
        if( dir == null ){
            return null;
        }

        return dir.selectFileServer( operation );
    }
    
    
    
    /**
     * A convenience method that selects a file server for staging the data out to 
     * a site. It returns the file server to which the generated data is staged
     * out / published.
     * 
     * The <code>FileServer</code> selected is associated with the HeadNode Filesystem.
     * 
     * For get and put operations, the results default back to searching for an
     * ALL server.
     * 
     * 
     * @param operation  the operation for which the file server is required
     *
     * @return the <code>FileServer</code> else null.
     */
    public FileServer selectStorageFileServerForStageout( FileServer.OPERATION operation  ){
        Directory dir = this.getDirectory( Directory.TYPE.local_storage );
        if( dir == null ){
            dir = this.getDirectory( Directory.TYPE.shared_storage );
        }

        //sanity check
        if( dir == null ){
            return null;
        }

        return dir.selectFileServer( operation );
    }
    
    /**
     * Return an iterator to value set of the Map.
     * 
     * @return Iterator<GridGateway>
     */
    public Iterator<GridGateway> getGridGatewayIterator(){        
        return mGridGateways.values().iterator();
    }
    
    /**
     * Add a GridGateway to the site.
     * 
     * @param g   the grid gateway to be added.
     */
    public void addGridGateway( GridGateway g ){
        mGridGateways.put( g.getJobType(), g );
    }
    
    /**
     * This is a soft state remove, that removes a GridGateway from a particular
     * site. 
     * 
     * @param contact the contact string for the grid gateway.
     *
     * @return true if was able to remove the jobmanager from the cache
     *         false if unable to remove, or the matching entry is not found
     *         or if the implementing class does not maintain a soft state.
     */
    public boolean removeGridGateway( String contact ) {
        //iterate through the entry set
        for( Iterator it = this.mGridGateways.entrySet().iterator(); it.hasNext(); ){
            Map.Entry entry = (Entry) it.next();
            GridGateway g = ( GridGateway )entry.getValue();
            if( g.getContact().equals( contact ) ) {
                it.remove();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Return an iterator to the replica catalog associated with the site.
     * 
     * @return Iterator<ReplicaCatalog>
     */
    public Iterator<ReplicaCatalog> getReplicaCatalogIterator(){        
        return mReplicaCatalogs.iterator();
    }
    
    /**
     * Add a Replica Catalog to the site.
     * 
     * @param catalog   the replica catalog to be added.
     */
    public void addReplicaCatalog( ReplicaCatalog catalog ){
        mReplicaCatalogs.add( catalog );
    }
    
    /**
     * Selects a Random ReplicaCatalog.
     *
     * @return <code>ReplicaCatalog</object> if more than one associates else
     *         returns null.
     */
    public ReplicaCatalog selectReplicaCatalog( ) {
        
        return ( this.mReplicaCatalogs == null || this.mReplicaCatalogs.size() == 0 )?
                 null :
                 this.mReplicaCatalogs.get(  PegRandom.getInteger( this.mReplicaCatalogs.size() - 1) );
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
        writer.write( "<site " );        
        writeAttribute( writer, "handle", getSiteHandle() );
        writeAttribute( writer, "arch", getArchitecture().toString() );        
        writeAttribute( writer, "os", getOS().toString() );
		writeAttribute( writer, "instancetype", getInstanceType().toString() );//Amelie
       
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
        
        writer.write( ">");
        writer.write( newLine );
        
        //list all the gridgateways
        for( Iterator<GridGateway> it = this.getGridGatewayIterator(); it.hasNext(); ){
            it.next().toXML( writer, newIndent );
        }

        //list all the directories
        for( Directory directory : this.mDirectories.values() ){
            directory.toXML(writer, newIndent);
        }
        
        //list all the replica catalogs associate
        for( Iterator<ReplicaCatalog> it = this.getReplicaCatalogIterator(); it.hasNext(); ){
            it.next().toXML( writer, newIndent );
        }
        
        this.getProfiles().toXML( writer, newIndent );
        
        writer.write( indent );
        writer.write( "</site>" );
        writer.write( newLine );
    }

    /**
     * Returns the clone of the object.
     *
     * @return the clone
     */
    public Object clone(){
        SiteCatalogEntry obj;
        try{
            obj = ( SiteCatalogEntry ) super.clone();
            obj.initialize( this.getSiteHandle() );
            obj.setSysInfo( (SysInfo)this.getSysInfo().clone());
        
            //list all the gridgateways
            for( Iterator<GridGateway> it = this.getGridGatewayIterator(); it.hasNext(); ){
                obj.addGridGateway( (GridGateway)it.next().clone() );
            }   

            for( Directory directory : this.mDirectories.values() ){
                obj.setDirectory( (Directory)directory.clone() );
            }
            
        
            //list all the replica catalogs associate
            for( Iterator<ReplicaCatalog> it = this.getReplicaCatalogIterator(); it.hasNext(); ){
                obj.addReplicaCatalog( (ReplicaCatalog)it.next().clone( ) );
            }
        
            obj.setProfiles( (Profiles)this.mProfiles.clone() );
        
            
        }
        catch( CloneNotSupportedException e ){
            //somewhere in the hierarch chain clone is not implemented
            throw new RuntimeException("Clone not implemented in the base class of " + this.getClass().getName(),
                                       e );
        }
        return obj;
    }

    /**
     * Accept method for the visitor interface
     *
     * @param visitor   the visitor
     *
     * @throws  IOException  in case of error
     */
    public void accept(SiteDataVisitor visitor) throws IOException {

        visitor.visit( this );

       //list all the gridgateways
        for( Iterator<GridGateway> it = this.getGridGatewayIterator(); it.hasNext(); ){
            it.next().accept(visitor);
        }

        for( Directory directory: this.mDirectories.values() ){
            directory.accept( visitor );
        }
        
        //list all the replica catalogs associate
        for( Iterator<ReplicaCatalog> it = this.getReplicaCatalogIterator(); it.hasNext(); ){
            it.next().accept(visitor);
        }

        //profiles are handled in the depart method
        visitor.depart(this);
    }





   
    
    
    
}