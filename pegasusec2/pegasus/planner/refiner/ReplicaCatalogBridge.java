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

package edu.isi.pegasus.planner.refiner;


import edu.isi.pegasus.common.credential.CredentialHandler;
import edu.isi.pegasus.common.credential.CredentialHandlerFactory;
import edu.isi.pegasus.common.logging.LoggingKeys;

import edu.isi.pegasus.planner.catalog.site.classes.GridGateway;
import edu.isi.pegasus.planner.catalog.site.classes.SiteCatalogEntry;

import edu.isi.pegasus.planner.classes.FileTransfer;
import edu.isi.pegasus.planner.classes.NameValue;
import edu.isi.pegasus.planner.classes.Profile;
import edu.isi.pegasus.planner.classes.ReplicaLocation;
import edu.isi.pegasus.planner.classes.ReplicaStore;
import edu.isi.pegasus.planner.classes.ADag;
import edu.isi.pegasus.planner.classes.Job;
import edu.isi.pegasus.planner.classes.PlannerOptions;

import edu.isi.pegasus.common.logging.LogManager;
import edu.isi.pegasus.planner.common.PegasusProperties;

import edu.isi.pegasus.planner.catalog.ReplicaCatalog;
import edu.isi.pegasus.planner.catalog.replica.ReplicaFactory;

import edu.isi.pegasus.planner.catalog.transformation.TransformationCatalogEntry;
import edu.isi.pegasus.planner.catalog.transformation.classes.TCType;

import java.io.File;
import java.io.FileWriter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import edu.isi.pegasus.planner.classes.PegasusBag;
import edu.isi.pegasus.planner.namespace.Dagman;

/**
 * This coordinates the look up to the Replica Location Service, to determine
 * the logical to physical mappings.
 *
 * @author Karan Vahi
 * @author Gaurang Mehta
 * @version $Revision$
 *
 */
public class ReplicaCatalogBridge
             extends Engine //for the time being.
             {
    
    /**
     * Default category for registration jobs
     */
    public static final String DEFAULT_REGISTRATION_CATEGORY_KEY = "registration";

    /**
     * The transformation namespace for the regostration jobs.
     */
    public static final String RC_TRANSFORMATION_NS = "pegasus";

    /**
     * The logical name of the transformation used.
     */
    public static final String RC_TRANSFORMATION_NAME = "rc-client";

    /**
     * The logical name of the transformation used.
     */
    public static final String RC_TRANSFORMATION_VERSION = null;

    /**
     * The derivation namespace for the transfer jobs.
     */
    public static final String RC_DERIVATION_NS = "pegasus";

    /**
     * The derivation name for the transfer jobs.
     */
    public static final String RC_DERIVATION_NAME = "rc-client";


    /**
     * The version number for the derivations for registration jobs.
     */
    public static final String RC_DERIVATION_VERSION = "1.0";

    /**
     * The name of the Replica Catalog Implementer that serves as the source for
     * cache files.
     */
    public static final String CACHE_REPLICA_CATALOG_IMPLEMENTER = "SimpleFile";

    /**
     * The name of the source key for Replica Catalog Implementer that serves as
     * cache
     */
    public static final String CACHE_REPLICA_CATALOG_KEY = "file";
    
    /**
     * The name of the key that disables writing back to the cache file.
     * Designates a static file. i.e. read only
     */
    public static final String CACHE_READ_ONLY_KEY = "read.only";

    
    /**
     * The name of the Replica Catalog Implementer that serves as the source for
     * cache files.
     */
    public static final String DIRECTORY_REPLICA_CATALOG_IMPLEMENTER = "Directory";

    /**
     * The name of the source key for Replica Catalog Implementer that serves as
     * cache
     */
    public static final String DIRECTORY_REPLICA_CATALOG_KEY = "directory";

    /**
     * The name of the URL key for the replica catalog impelementer to be picked
     * up.
     */
    public static final String REPLICA_CATALOG_URL_KEY = "url";

    /**
     * The handle to the main Replica Catalog.
     */
    private ReplicaCatalog mReplicaCatalog;



    /**
     * The Vector of <code>String</code> objects containing the logical
     * filenames of the files whose locations are to be searched in the
     * Replica Catalog.
     */
    protected Set mSearchFiles ;


    /**
     * A boolean variable to desingnate whether the RLI queried was down or not.
     * By default it is up, unless it is set to true explicitly.
     */
    private boolean mRCDown;

    /**
     * The replica store in which we store all the results that are queried from
     * the main replica catalog.
     */
    private ReplicaStore mReplicaStore;

    /**
     * The replica store in which we store all the results that are queried from
     * the cache replica catalogs.
     */
    private ReplicaStore mCacheStore;
    
    /**
     * The replica store where we store all the results that are queried from
     * the input directory specified at the command line.
     */
    private ReplicaStore mDirectoryReplicaStore;


    /**
     * The DAX Replica Store.
     */
    private ReplicaStore mDAXReplicaStore;


    /**
     * The inherited Replica Store
     */
    private ReplicaStore mInheritedReplicaStore;


    /**
     * A boolean indicating whether the cache file needs to be treated as a
     * replica catalog or not.
     */
    private boolean mTreatCacheAsRC;

    /**
     * The default tc entry.
     */
    private TransformationCatalogEntry mDefaultTCRCEntry;

    /**
     * A boolean indicating whether the attempt to create a default tc entry
     * has happened or not.
     */
    private boolean mDefaultTCRCCreated;

    /**
     * The DAG being worked upon.
     */
    private ADag mDag;


    /**
     * The overloaded constructor.
     *
     * @param dag         the workflow that is being worked on.
     * @param bag of initialization objects.
     *
     */
    public ReplicaCatalogBridge( ADag dag ,
                                 PegasusBag bag ) {
        super( bag );
        this.initialize( dag, bag );
    }

    /**
     * Intialises the refiner.
     *
     * @param dag         the workflow that is being worked on.
     * @param bag         the bag of Pegasus initialization objects
     *
     */
    public void initialize( ADag dag ,
                            PegasusBag bag ){
        
        this.mDAXReplicaStore = dag.getReplicaStore();
        this.initialize( dag, bag.getPegasusProperties(), bag.getPlannerOptions() );
    }
    
    /**
     * Intialises the refiner.
     *
     * @param dag         the workflow that is being worked on.
     * @param properties  the properties passed to the planner.
     * @param options     the options passed to the planner at runtime.
     *
     */
    @SuppressWarnings("static-access")
    public void initialize( ADag dag ,
                            PegasusProperties properties,
                            PlannerOptions options ){
        mDag   = dag;
        mProps = properties;
        mPOptions = options;
        mRCDown = false;
        mCacheStore = new ReplicaStore();
        mInheritedReplicaStore = new ReplicaStore();
        mDirectoryReplicaStore = new ReplicaStore();
        mTreatCacheAsRC = mProps.treatCacheAsRC();
        mDefaultTCRCCreated = false;

        //converting the Vector into vector of
        //strings just containing the logical
        //filenames
        mSearchFiles = dag.dagInfo.getLFNs( options.getForce() );

        //only for windward for time being
        properties.setProperty( "pegasus.catalog.replica.dax.id", dag.getAbstractWorkflowName() );
        properties.setProperty( "pegasus.catalog.replica.mrc.windward.dax.id", dag.getAbstractWorkflowName() );
        
        try {

            //make sure that RLS can be loaded from local environment
            //Karan May 1 2007
            mReplicaCatalog = null;
            if ( mSearchFiles != null && !mSearchFiles.isEmpty() ){

                //need to clone before setting any read only properites
                PegasusProperties props = (PegasusProperties) properties.clone();

                //set the read only property for the file based rc
                //we are connecting via PegasusProperties add the prefix
                String name =  ReplicaCatalog.c_prefix + "." + ReplicaCatalogBridge.CACHE_READ_ONLY_KEY;
                props.setProperty( name, "true" );
                
                String proxy = getPathToLocalProxy();
                if( proxy != null ){
                    mLogger.log( "Proxy used for Replica Catalog is " + proxy,
                                 LogManager.CONFIG_MESSAGE_LEVEL );
                    props.setProperty( ReplicaCatalog.c_prefix + "." + ReplicaCatalog.PROXY_KEY, 
                                       proxy );
                }
                
                mReplicaCatalog = ReplicaFactory.loadInstance( props );          

                //load all the mappings.
                mReplicaStore = new ReplicaStore( mReplicaCatalog.lookup( mSearchFiles ) );
            }

        } catch ( Exception ex ) {
            String msg = "Problem while connecting with the Replica Catalog: ";
            
            //set the flag to denote RLI is down
            mRCDown = true;
            //mReplicaStore = new ReplicaStore();

            
            //exit if there is no cache overloading specified.
            if ( options.getCacheFiles().isEmpty() &&       //no cache files specified
                 options.getInheritedRCFiles().isEmpty() && //no files locations inherited from outer level DAX
                 this.mDAXReplicaStore.isEmpty() &&         //no file locations in current DAX
                 options.getInputDirectory() == null  && //no input directory specified on the command line
                 dag.dagInfo.getLFNs( true ).size() > 0 //the number of raw input files is more than 1
                    ){
                mLogger.log( msg + ex.getMessage(),LogManager.ERROR_MESSAGE_LEVEL );
                throw new RuntimeException( msg , ex );
            }
            else{
                mLogger.log( msg + ex.getMessage(),LogManager.DEBUG_MESSAGE_LEVEL  );
            }
        }
        finally{
            //set replica store to an empty store if required
            mReplicaStore = ( mReplicaStore == null ) ?new ReplicaStore() : mReplicaStore;
        }

        
        if( mReplicaCatalog != null ){
            //specify maxjobs to 1 for File based replica catalog
            //JIRA PM-377
            if( mReplicaCatalog instanceof edu.isi.pegasus.planner.catalog.replica.impl.SimpleFile ){
                //we set the default category value to 1
                //in the properties
                String key = getDefaultRegistrationMaxJobsPropertyKey();
                mLogger.log( "Setting property " + key + " to 1 to set max jobs for registrations jobs category",
                              LogManager.DEBUG_MESSAGE_LEVEL );
                mProps.setProperty( key, "1" );
            }
        }
        
        //incorporate all mappings from input directory if specified
        String input = options.getInputDirectory();
        if( input != null ){
            mDirectoryReplicaStore = getReplicaStoreFromDirectory( input );
        }
            
        //incorporate the caching if any
        if ( !options.getCacheFiles().isEmpty() ) {
            loadCacheFiles( options.getCacheFiles() );
        }

        //load inherited replica store
        if ( !options.getInheritedRCFiles().isEmpty() ) {
            this.loadInheritedReplicaStore( options.getInheritedRCFiles() );
        }
    }


    /**
     * To close the connection to replica services. This must be defined in the
     * case where one has not done a singleton implementation. In other
     * cases just do an empty implementation of this method.
     */
    public void closeConnection() {
        if ( mReplicaCatalog != null ) {
            mReplicaCatalog.close();
        }
    }

    /**         
     * Closes the connection to the rli.
     */
    public void finalize() {
        this.closeConnection();
    }


    /**
     * This returns the files for which mappings exist in the Replica Catalog.
     * This should return a subset of the files which are
     * specified in the mSearchFiles, while getting an instance to this.
     *
     * @return  a <code>Set</code> of logical file names as String objects, for
     *          which logical to physical mapping exists.
     *
     * @see #mSearchFiles
     */
    public Set getFilesInReplica() {

        //check if any exist in the cache
        Set lfnsFound = mCacheStore.getLFNs( mSearchFiles );
        mLogger.log(lfnsFound.size()  + " entries found in cache of total " +
                    mSearchFiles.size(),
                    LogManager.DEBUG_MESSAGE_LEVEL);

        //check if any exist in input directory
        lfnsFound.addAll( this.mDirectoryReplicaStore.getLFNs( mSearchFiles ) );
        mLogger.log(lfnsFound.size()  + " entries found in cache of total " +
                    mSearchFiles.size(),
                    LogManager.DEBUG_MESSAGE_LEVEL);

        
        //check in the main replica catalog
        if ( this.mDAXReplicaStore.isEmpty() &&
                ( mRCDown || mReplicaCatalog == null )) {
            mLogger.log("Replica Catalog is either down or connection to it was never opened ",
                        LogManager.WARNING_MESSAGE_LEVEL);
            return lfnsFound;
        }

        //lookup from the DAX Replica Store
        lfnsFound.addAll( this.mDAXReplicaStore.getLFNs() );

        //lookup from the inherited Replica Store
        lfnsFound.addAll( this.mInheritedReplicaStore.getLFNs( mSearchFiles ) );

        //look up from the the main replica catalog
        lfnsFound.addAll( mReplicaStore.getLFNs() );


        return lfnsFound;

    }




    /**
     * Returns all the locations as returned from the Replica Lookup Mechanism.
     *
     * @param lfn   The name of the logical file whose PFN mappings are
     *                      required.
     *
     * @return ReplicaLocation containing all the locations for that LFN
     *
     * @see org.griphyn.cPlanner.classes.ReplicaLocation
     */
    public ReplicaLocation getFileLocs( String lfn ) {

        ReplicaLocation rl = retrieveFromCache( lfn );
        //first check from cache
        if(rl != null && !mTreatCacheAsRC){
            mLogger.log( "Location of file " + rl +
                         " retrieved from cache" , LogManager.DEBUG_MESSAGE_LEVEL);
            return rl;
        }
        
        //we prefer location in Directory over the DAX entries
        if( this.mDirectoryReplicaStore.containsLFN( lfn ) ){
            return this.mDirectoryReplicaStore.getReplicaLocation(lfn);
        }


        //we prefer location in DAX over the inherited replica store
        if( this.mDAXReplicaStore.containsLFN( lfn ) ){
            return this.mDAXReplicaStore.getReplicaLocation(lfn);
        }

        //we prefer location in inherited replica store over replica catalog
        if( this.mInheritedReplicaStore.containsLFN(lfn) ){
            return this.mInheritedReplicaStore.getReplicaLocation(lfn);
        }

        ReplicaLocation rcEntry = mReplicaStore.getReplicaLocation( lfn );
        if (rl == null) {
            rl = rcEntry;
        }
        else{
            //merge with the ones found in cache
            rl.merge(rcEntry);
        }


        return rl;
    }


    /**
     * Returns the property key that can be used to set the max jobs for the
     * default category associated with the registration jobs.
     * 
     * @return the property key
     */
    public String getDefaultRegistrationMaxJobsPropertyKey(){
        StringBuffer key = new StringBuffer();
        
        key.append( Dagman.NAMESPACE_NAME ).append( "." ).
            append( ReplicaCatalogBridge.DEFAULT_REGISTRATION_CATEGORY_KEY ).
            append( "." ).append( Dagman.MAXJOBS_KEY.toLowerCase() );
        
        return key.toString();
    }

    /**
     * It constructs the Job object for the registration node, which
     * registers the materialized files on the output pool in the RLS.
     * Note that the relations corresponding to this node should already have
     * been added to the concerned <code>DagInfo</code> object.
     *
     * @param regJobName  The name of the job which registers the files in the
     *                    Replica Location Service.
     * @param job         The job whose output files are to be registered in
     *                    the Replica Location Service.
     *
     * @param files       Collection of <code>FileTransfer</code> objects
     *                    containing the information about source and
     *                    destination URLs. The destination
     *                    URLs would be our PFNs.
     *
     * @return Job corresponding to the new registration node.
     */
    public  Job makeRCRegNode( String regJobName, Job job,
                                  Collection files ) {
        //making the files string

        Job newJob = new Job();

        newJob.setName( regJobName );
        newJob.setTransformation( ReplicaCatalogBridge.RC_TRANSFORMATION_NS,
                                  ReplicaCatalogBridge.RC_TRANSFORMATION_NAME,
                                  ReplicaCatalogBridge.RC_TRANSFORMATION_VERSION );
        newJob.setDerivation( ReplicaCatalogBridge.RC_DERIVATION_NS,
                              ReplicaCatalogBridge.RC_DERIVATION_NAME,
                              ReplicaCatalogBridge.RC_DERIVATION_VERSION );

//        SiteInfo site = mPoolHandle.getPoolEntry( mOutputPool, "vanilla" );
        SiteCatalogEntry site = mSiteStore.lookup( mOutputPool );

        //change this function
        List tcentries = null;
        try {
            tcentries = mTCHandle.lookup( newJob.getTXNamespace(),
                                                newJob.getTXName(),
                                                newJob.getTXVersion(),
                                                "local",
                                                TCType.INSTALLED );

        } catch ( Exception e ) {
            mLogger.log( "While retrieving entries from TC " + e.getMessage(),
                         LogManager.ERROR_MESSAGE_LEVEL);
        }

        TransformationCatalogEntry tc;

        if ( tcentries == null || tcentries.isEmpty() ) {

            mLogger.log( "Unable to find in entry for " + newJob.getCompleteTCName() +  " in transformation catalog on site local",
                         LogManager.DEBUG_MESSAGE_LEVEL );
            mLogger.log( "Constructing a default entry for it " , LogManager.DEBUG_MESSAGE_LEVEL );
            tc = defaultTCRCEntry(  );

            if( tc == null ){
                throw new RuntimeException( "Unable to create an entry for " +
                                            newJob.getCompleteTCName() +  " on site local");
            }
        }
        else{
            tc = (TransformationCatalogEntry) tcentries.get(0);
        }
        newJob.setRemoteExecutable( tc.getPhysicalTransformation() );
        newJob.setArguments( this.generateRepJobArgumentString( site, regJobName, files ) );
//        newJob.setUniverse( Engine.REGISTRATION_UNIVERSE );
        newJob.setUniverse( GridGateway.JOB_TYPE.register.toString() );
        newJob.setSiteHandle( tc.getResourceId() );
        newJob.setJobType( Job.REPLICA_REG_JOB );
        newJob.setVDSSuperNode( job.getName() );

        //the profile information from the pool catalog needs to be
        //assimilated into the job.
//        newJob.updateProfiles( mPoolHandle.getPoolProfile( newJob.getSiteHandle() ) );
        newJob.updateProfiles( mSiteStore.lookup( newJob.getSiteHandle() ).getProfiles() );

        //add any notifications specified in the transformation
        //catalog for the job. JIRA PM-391
        newJob.addNotifications( tc );


        //the profile information from the transformation
        //catalog needs to be assimilated into the job
        //overriding the one from pool catalog.
        newJob.updateProfiles( tc );

        //the profile information from the properties file
        //is assimilated overidding the one from transformation
        //catalog.
        newJob.updateProfiles( mProps );
        
        //if no category is associated with the job, add a default
       //category
       if( !newJob.dagmanVariables.containsKey( Dagman.CATEGORY_KEY ) ){
           newJob.dagmanVariables.construct( Dagman.CATEGORY_KEY, DEFAULT_REGISTRATION_CATEGORY_KEY );
       }

        //in order to make sure that COG picks the default proxy
        //correctly through condor
        newJob.condorVariables.construct( "getenv", "true" );

        return newJob;
    }

    /**
     * Returns a default TC entry to be used in case entry is not found in the
     * transformation catalog.
     *
     *
     *
     * @return  the default entry.
     */
    private  TransformationCatalogEntry defaultTCRCEntry( ){
        String site = "local";
        //generate only once.
        if( !mDefaultTCRCCreated ){

            //construct the path to it
            StringBuffer path = new StringBuffer();
            path.append( mProps.getBinDir() ).append( File.separator ).
                 append( "pegasus-rc-client" );

            mDefaultTCRCEntry = new TransformationCatalogEntry( this.RC_TRANSFORMATION_NS,
                                                                this.RC_TRANSFORMATION_NAME,
                                                                this.RC_TRANSFORMATION_VERSION );

            mDefaultTCRCEntry.setPhysicalTransformation( path.toString() );
            mDefaultTCRCEntry.setResourceId( site );

            //set the flag back to true
            mDefaultTCRCCreated = true;
        }
        return mDefaultTCRCEntry;
    }


    /**
     * Returns the classpath for the default rc-client entry.
     *
     * @param home   the home directory where we need to check for lib directory.
     *
     * @return the classpath in an environment profile.
     */
    private Profile getClassPath( String home ){
        Profile result = null ;

        //create the CLASSPATH from home
        String classpath = mProps.getProperty( "java.class.path" );
        if( classpath == null || classpath.trim().length() == 0 ){
            return result;
        }

        mLogger.log( "JAVA CLASSPATH SET IS " + classpath , LogManager.DEBUG_MESSAGE_LEVEL );

        StringBuffer cp = new StringBuffer();
        String prefix = home + File.separator + "lib";
        for( StringTokenizer st = new StringTokenizer( classpath, ":" ); st.hasMoreTokens(); ){
            String token = st.nextToken();
            if( token.startsWith( prefix ) ){
                //this is a valid lib jar to put in
                cp.append( token ).append( ":" );
            }
        }

        if ( cp.length() == 0 ){
            //unable to create a valid classpath
            mLogger.log( "Unable to create a sensible classpath from " + home,
                         LogManager.DEBUG_MESSAGE_LEVEL );
            return result;
        }

        //we have everything now
        result = new Profile( Profile.ENV, "CLASSPATH", cp.toString() );

        return result;
    }


    /**
     * Generates the argument string to be given to the replica registration job.
     * At present by default it would be picking up the file containing the
     * mappings.
     *
     * @param site     the <code>SiteCatalogEntry</code> object
     * @param regJob   The name of the registration job.
     *
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *                 information about source and destURLs. The destination
     *                 URLs would be our PFNs.
     *
     * @return the argument string.
     */
    private String generateRepJobArgumentString( SiteCatalogEntry site, String regJob, Collection files ) {
        StringBuffer arguments = new StringBuffer();

        //select a LRC. disconnect here. It should be select a RC.

        edu.isi.pegasus.planner.catalog.site.classes.ReplicaCatalog rc =
                                (site == null) ? null : site.selectReplicaCatalog();
        
        
        //we append the url property if a user has specified a
        //URL in the site catalog entry, else we rely on what
        //was specified in properties
        if (!( rc  == null || rc.getURL() == null || rc.getURL().length() == 0)) {
            //we have a lrc selected . construct vds.rc.url property
            arguments.append( "-D" ).append( ReplicaCatalog.c_prefix ).append( "." ).
                  append( ReplicaCatalogBridge.REPLICA_CATALOG_URL_KEY).append( "=" ).append( rc.getURL() ).
                  append( " " );
        }

        

        

        //get any command line properties that may need specifying
        arguments.append( "--conf" ).append( " " ).
                  append(  mProps.getPropertiesInSubmitDirectory( )  ).
                  append( " " );
        
        //append the insert option
        arguments.append( "--insert" ).append( " " ).
                  append( this.generateMappingsFile( regJob, files ) );

        return arguments.toString();

    }

    /**
     * Returns the properties that need to be passed to the the rc-client
     * invocation on the command line . It is of the form
     * "-Dprop1=value1 -Dprop2=value2 .."
     *
     * @param properties   the properties object
     *
     * @return the properties list, else empty string.
     */
    protected String getCommandLineProperties( PegasusProperties properties ){
        StringBuffer sb = new StringBuffer();
        appendProperty( sb,
                        "pegasus.user.properties",
                        properties.getPropertiesInSubmitDirectory( ));
        return sb.toString();
    }

    /**
     * Appends a property to the StringBuffer, in the java command line format.
     *
     * @param sb    the StringBuffer to append the property to.
     * @param key   the property.
     * @param value the property value.
     */
    protected void appendProperty( StringBuffer sb, String key, String value ){
        sb.append("-D").append( key ).append( "=" ).append( value ).append( " ");
    }


    /**
     * Generates the registration mappings in a text file that is generated in the
     * dax directory (the directory where all the condor submit files are
     * generated). The pool value for the mapping is the output pool specified
     * by the user when running Pegasus. The name of the file is regJob+.in
     *
     * @param regJob   The name of the registration job.
     * @param files    Collection of <code>FileTransfer</code>objects containing the
     *                 information about source and destURLs. The destination
     *                 URLs would be our PFNs.
     *
     * @return String corresponding to the path of the the file containig the
     *                mappings in the appropriate format.
     */
    private String generateMappingsFile( String regJob, Collection files ) {
        String fileName = regJob + ".in";
        File f = null;
        String submitFileDir = mPOptions.getSubmitDirectory();

        //writing the stdin file
        try {
            f = new File( submitFileDir, fileName );
            FileWriter stdIn = new FileWriter( f );

            for(Iterator it = files.iterator();it.hasNext();){
                FileTransfer ft = ( FileTransfer ) it.next();
                //checking for transient flag
                if ( !ft.getTransientRegFlag() ) {
                    stdIn.write( ftToRC( ft ) );
                    stdIn.flush();
                }
            }

            stdIn.close();

        } catch ( Exception e ) {
            throw new RuntimeException(
                "While writing out the registration file for job " + regJob, e );
        }

        return f.getAbsolutePath();
    }


    /**
     * Converts a <code>FileTransfer</code> to a RC compatible string representation.
     *
     * @param ft  the <code>FileTransfer</code> object
     *
     * @return the RC version.
     */
    private String ftToRC( FileTransfer ft ){
        StringBuffer sb = new StringBuffer();
        NameValue destURL = ft.getDestURL();
        sb.append( ft.getLFN() ).append( " " );
        sb.append( ft.getURLForRegistrationOnDestination()  ).append( " " );
        sb.append( "pool=\"" ).append( destURL.getKey() ).append( "\"" );
        sb.append( "\n" );
        return sb.toString();
    }

    
    /**
     * Retrieves a location from the cache table, that contains the contents
     * of the cache files specified at runtime.
     *
     * @param lfn  the logical name of the file.
     *
     * @return <code>ReplicaLocation</code> object corresponding to the entry
     *         if found, else null.
     */
    private ReplicaLocation retrieveFromCache( String lfn ){
        return mCacheStore.getReplicaLocation( lfn );
    }


    /**
     * Ends up loading the inherited replica files.
     *
     * @param files  set of paths to the inherited replica files.
     */
    private void loadInheritedReplicaStore( Set files ) {
        mLogger.log("Loading Inhertied ReplicaFiles files: " + files,  LogManager.DEBUG_MESSAGE_LEVEL);
        this.mInheritedReplicaStore = this.getReplicaStoreFromFiles( files );
    }
    /**
     * Ends up loading the cache files so as to enable the lookup for the transient
     * files created by the parent jobs.
     *
     * @param cacheFiles  set of paths to the cache files.
     */
    private void loadCacheFiles( Set cacheFiles ) {
        mLogger.log("Loading cache files: " + cacheFiles,  LogManager.DEBUG_MESSAGE_LEVEL);
        mCacheStore = this.getReplicaStoreFromFiles(cacheFiles);
    }

    /**
     * Ends up loading a Replica Store from replica catalog files
     *
     * @param files  set of paths to the cache files.
     */
    private ReplicaStore getReplicaStoreFromFiles( Set files ) {
        ReplicaStore store = new ReplicaStore();
        Properties cacheProps = mProps.getVDSProperties().matchingSubset(
                                                              ReplicaCatalog.c_prefix,
                                                              false );

        mLogger.logEventStart( LoggingKeys.EVENT_PEGASUS_LOAD_TRANSIENT_CACHE, 
                               LoggingKeys.DAX_ID,
                               mDag.getAbstractWorkflowName() );

        ReplicaCatalog simpleFile;
        Map wildcardConstraint = null;
        
        //all cache files are loaded in readonly mode
        cacheProps.setProperty( ReplicaCatalogBridge.CACHE_READ_ONLY_KEY, "true" );
        
        for ( Iterator it = files.iterator(); it.hasNext() ; ) {
            //read each of the cache file and load in memory
            String  file = ( String ) it.next();
            //set the appropriate property to designate path to file
            cacheProps.setProperty( ReplicaCatalogBridge.CACHE_REPLICA_CATALOG_KEY, file );

            mLogger.log("Loading  file: " + file,  LogManager.DEBUG_MESSAGE_LEVEL);
            try{
                simpleFile = ReplicaFactory.loadInstance( CACHE_REPLICA_CATALOG_IMPLEMENTER,
                                                          cacheProps );
            }
            catch( Exception e ){
                mLogger.log( "Unable to load cache file " + file,
                             e,
                             LogManager.ERROR_MESSAGE_LEVEL );
                continue;
            }
            //suck in all the entries into the cache replica store.
            //returns an unmodifiable collection. so merging an issue..
            store.add( simpleFile.lookup( mSearchFiles ) );

            //no wildcards as we only want to load mappings for files that
            //we require
            //mCacheStore.add( simpleFile.lookup( wildcardConstraint ) );

            //close connection
            simpleFile.close();
        }

        mLogger.logEventCompletion();
        return store;
    }
    
    /**
     * Loads the mappings from the input directory 
     * 
     * @param directory  the directory to load from
     */
    private ReplicaStore getReplicaStoreFromDirectory(String directory) {
        ReplicaStore store = new ReplicaStore();
        Properties properties = mProps.getVDSProperties().matchingSubset(
                                                              ReplicaCatalog.c_prefix,
                                                              false );

        mLogger.logEventStart( LoggingKeys.EVENT_PEGASUS_LOAD_DIRECTORY_CACHE, 
                               LoggingKeys.DAX_ID,
                               mDag.getAbstractWorkflowName() );

        ReplicaCatalog catalog = null;
        
        //set the appropriate property to designate path to file
        properties.setProperty( ReplicaCatalogBridge.DIRECTORY_REPLICA_CATALOG_KEY, directory );

            
        mLogger.log("Loading  from directory: " + directory,  LogManager.DEBUG_MESSAGE_LEVEL);
        try{
            catalog = ReplicaFactory.loadInstance( DIRECTORY_REPLICA_CATALOG_IMPLEMENTER,
                                                   properties );
            
            
            store.add( catalog.lookup( mSearchFiles ) );
        }
        catch( Exception e ){
            mLogger.log( "Unable to load from directory  " + directory,
                             e,
                             LogManager.ERROR_MESSAGE_LEVEL );
        }
        finally{
            if( catalog != null ){
                catalog.close();
            }
        }
        
        mLogger.logEventCompletion();
        return store;
    }


    /**
     * Returns path to the local proxy
     * 
     * @return path to the local proxy 
     */
    private String getPathToLocalProxy() {
        //load and intialize the CredentialHandler Factory
        CredentialHandlerFactory factory = new CredentialHandlerFactory();
        factory.initialize( mBag );
        CredentialHandler handler = factory.loadInstance(CredentialHandler.TYPE.x509);
        return handler.getPath( "local" );
    }



}
