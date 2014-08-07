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


package edu.isi.pegasus.planner.common;

import edu.isi.pegasus.common.logging.LogManager;
import edu.isi.pegasus.planner.catalog.site.classes.Directory;
import edu.isi.pegasus.planner.catalog.site.classes.DirectoryLayout;
import edu.isi.pegasus.planner.catalog.site.classes.FileServer;
import edu.isi.pegasus.planner.catalog.site.classes.InternalMountPoint;
import edu.isi.pegasus.planner.catalog.site.classes.SiteCatalogEntry;
import edu.isi.pegasus.planner.catalog.site.classes.SiteStore;
import edu.isi.pegasus.planner.classes.PlannerOptions;
import edu.isi.pegasus.planner.transfer.sls.SLSFactory;
import java.io.File;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * A utility class that returns JAVA Properties that need to be set based on
 * a configuration value
 * 
 * @author Karan Vahi
 * @version $Revision$
 */
public class PegasusConfiguration {
    
    /**
     * The property key for pegasus configuration.
     */
    public static final String PEGASUS_CONFIGURATION_PROPERTY_KEY = "pegasus.data.configuration";

    /**
     * The value for the S3 configuration.
     */
    public static final String DEPRECATED_S3_CONFIGURATION_VALUE = "S3";


    /**
     * The value for the non shared filesystem configuration.
     */
    public static final String SHARED_FS_CONFIGURATION_VALUE = "sharedfs";

    /**
     * The default data configuration value
     */
    public static String DEFAULT_DATA_CONFIGURATION_VALUE = SHARED_FS_CONFIGURATION_VALUE;

    /**
     * The value for the non shared filesystem configuration.
     */
    public static final String NON_SHARED_FS_CONFIGURATION_VALUE = "nonsharedfs";

    /**
     * The value for the condor configuration.
     */
    public static final String CONDOR_CONFIGURATION_VALUE = "condorio";

    /**
     * The value for the condor configuration.
     */
    public static final String DEPRECATED_CONDOR_CONFIGURATION_VALUE = "Condor";
    
    /**
     * The logger to use.
     */
    private LogManager mLogger;
    
    /**
     * Overloaded Constructor
     * 
     * @param logger   the logger to use.
     */
    public PegasusConfiguration( LogManager logger ){
        mLogger = logger;
    }

    /**
     * Loads configuration specific properties into PegasusProperties,
     * and adjusts planner options accordingly.
     *
     * @param properties   the Pegasus Properties
     * @param options      the PlannerOptions .
     */
    public void loadConfigurationPropertiesAndOptions( PegasusProperties properties,
                                                       PlannerOptions options ){

        this.loadConfigurationProperties( properties );

        //sanitize on the planner options
        if( properties.executeOnWorkerNode() ){
            String slsImplementor = properties.getSLSTransferImplementation();
            if( slsImplementor == null ){
                slsImplementor = SLSFactory.DEFAULT_SLS_IMPL_CLASS;
            }
            
            //check for the sls implementation
            if( slsImplementor.equalsIgnoreCase( DEPRECATED_CONDOR_CONFIGURATION_VALUE ) ){

                for( String site : (Set<String>)options.getExecutionSites() ){
                    //sanity check to make sure staging outputSite is set to local
                    String stagingSite = options.getStagingSite( site );
                    if( stagingSite == null ){
                        stagingSite = "local";
                        //set it to local outputSite
                        mLogger.log( "Setting staging site for " + site + " to " + stagingSite ,
                                      LogManager.CONFIG_MESSAGE_LEVEL );
                        options.addToStagingSitesMappings( site , stagingSite );

                    }
                    else if (!( stagingSite.equalsIgnoreCase( "local" ) )){
                        StringBuffer sb = new StringBuffer();

                        sb.append( "Mismatch in the between execution site ").append( site ).
                           append( " and staging site " ).append( stagingSite ).
                           append( " . For Condor IO staging site should be set to local . " );

                        throw new RuntimeException( sb.toString() );
                    }
                }

            }
        }
    }

    

    /**
     * Updates Site Store and options based on the planner options set by the user
     * on the command line
     * 
     * @param store     the outputSite store
     * @param options   the planner options.
     */
    public void updateSiteStoreAndOptions( SiteStore store, PlannerOptions  options ) {
        //sanity check to make sure that output outputSite is loaded
        String outputSite = options.getOutputSite();
            
        if( options.getOutputSite() != null ){
            if( !store.list().contains( outputSite  ) ){
                StringBuffer error = new StringBuffer( );
                error.append( "The output site ["  ).append(  outputSite  ).
                      append( "] not loaded from the site catalog." );
                throw new  RuntimeException( error.toString() );
            }
        }
        
        //check if a user specified an output directory
        String directory = options.getOutputDirectory();
        String externalDirectory = options.getOutputDirectory();//the external view of the directory if relative
        if( directory != null ){
            outputSite = ( outputSite == null )?
                          "local": //user did not specify an output site, default to local
                          outputSite;//stick with what user specified
            
            options.setOutputSite( outputSite );
            
            SiteCatalogEntry entry = store.lookup( outputSite );
            
            
            //we first check for local directory
            DirectoryLayout storageDirectory = entry.getDirectory( Directory.TYPE.local_storage );
            if( storageDirectory == null || storageDirectory.isEmpty()){
                //default to shared directory
                storageDirectory = entry.getDirectory( Directory.TYPE.shared_storage );
            }
            if( storageDirectory == null || storageDirectory.isEmpty()){
                //now throw an error
                throw new RuntimeException( "No storage directory specified for output site " + outputSite );
            }

            boolean append = ( directory.startsWith( File.separator ) )?
                             false: //we use the path specified
                             true;  //we need to append to path in site catalog on basis of site handle


            //update the internal mount point and external URL's
            InternalMountPoint  imp = storageDirectory.getInternalMountPoint();
            if( imp == null || imp.getMountPoint() == null ){
                //now throw an error
                throw new RuntimeException( "No internal mount point specified  for HeadNode Storage Directory  for output site " + outputSite );
            
            }


            if( append ){
                //we update the output directory on basis of what is specified
                //in the site catalog for the site. For local site, we resolve
                //the relative path from the command line/environment
                if( outputSite.equals( "local" ) ){
                    directory = new File( directory ).getAbsolutePath();
                    externalDirectory = directory;
                    //we have the directory figured out for local site
                    //that should be the one to be used for external file servers
                    append = false;
                }
                else{
                    directory = new File( imp.getMountPoint(), directory).getAbsolutePath();
                }
            }


            //update all storage file server paths to refer to the directory
            StringBuffer message = new StringBuffer();
            message.append( "Updating internal storage file server paths for site " ).append( outputSite ).
                    append( " to directory " ).append( directory );
            mLogger.log( message.toString(), LogManager.CONFIG_MESSAGE_LEVEL );
            imp.setMountPoint( directory );

            for( FileServer.OPERATION op : FileServer.OPERATION.values() ){
                for( Iterator<FileServer> it = storageDirectory.getFileServersIterator(op); it.hasNext();){
                    FileServer server = it.next();
                    if( append ){
                        //get the mount point and append
                        StringBuffer mountPoint = new StringBuffer();
                        mountPoint.append( server.getMountPoint() ).append( File.separator ).append( externalDirectory );
                        server.setMountPoint( mountPoint.toString() );

                    }
                    else{
                        server.setMountPoint( directory );
                    }
                }
            }
            
            //log the updated output site entry
            mLogger.log( "Updated output site entry is " + entry,
                         LogManager.DEBUG_MESSAGE_LEVEL );
        }
        
    }
    
    /**
     * Loads configuration specific properties into PegasusProperties
     * 
     * @param properties   the Pegasus Properties.
     */
    private void loadConfigurationProperties( PegasusProperties properties ){
        String configuration  = properties.getProperty( PEGASUS_CONFIGURATION_PROPERTY_KEY ) ;
        
        Properties props = this.getConfigurationProperties(configuration);
        for( Iterator it = props.keySet().iterator(); it.hasNext(); ){
            String key = (String) it.next();
            String value = props.getProperty( key );
            this.checkAndSetProperty( properties, key, value );
        }
        
    }
    
    /**
     * Returns Properties corresponding to a particular configuration.
     * 
     * @param configuration  the configuration value.
     * 
     * @return Properties
     */
    public Properties getConfigurationProperties( String configuration ){
        //sanity check
        if( configuration == null ){
            //default is the sharedfs
            configuration = SHARED_FS_CONFIGURATION_VALUE;
        }        
        
        Properties p = new Properties( );
        if( configuration.equalsIgnoreCase( DEPRECATED_S3_CONFIGURATION_VALUE ) || configuration.equalsIgnoreCase( NON_SHARED_FS_CONFIGURATION_VALUE ) ){

            //throw warning for deprecated value
            if( configuration.equalsIgnoreCase( DEPRECATED_S3_CONFIGURATION_VALUE ) ){
                mLogger.log( deprecatedValueMessage( PEGASUS_CONFIGURATION_PROPERTY_KEY,DEPRECATED_S3_CONFIGURATION_VALUE ,NON_SHARED_FS_CONFIGURATION_VALUE ),
                             LogManager.WARNING_MESSAGE_LEVEL );
            }

            p.setProperty( "pegasus.execute.*.filesystem.local", "true" );
            p.setProperty( "pegasus.gridstart", "PegasusLite" );

            //we want the worker package to be staged, unless user sets it to false explicitly
            p.setProperty( PegasusProperties.PEGASUS_TRANSFER_WORKER_PACKAGE_PROPERTY, "true" );
        }
        else if ( configuration.equalsIgnoreCase( CONDOR_CONFIGURATION_VALUE ) || configuration.equalsIgnoreCase( DEPRECATED_CONDOR_CONFIGURATION_VALUE ) ){

            //throw warning for deprecated value
            if( configuration.equalsIgnoreCase( DEPRECATED_CONDOR_CONFIGURATION_VALUE ) ){
                mLogger.log( deprecatedValueMessage( PEGASUS_CONFIGURATION_PROPERTY_KEY,DEPRECATED_CONDOR_CONFIGURATION_VALUE ,CONDOR_CONFIGURATION_VALUE ),
                             LogManager.WARNING_MESSAGE_LEVEL );
            }

            p.setProperty( "pegasus.transfer.sls.*.impl", "Condor" );
            p.setProperty( "pegasus.execute.*.filesystem.local", "true" );
            p.setProperty( "pegasus.gridstart", "PegasusLite" );
            
            //we want the worker package to be staged, unless user sets it to false explicitly
            p.setProperty( PegasusProperties.PEGASUS_TRANSFER_WORKER_PACKAGE_PROPERTY, "true" );

        }
        else if( configuration.equalsIgnoreCase( SHARED_FS_CONFIGURATION_VALUE ) ){
            //PM-624
            //we should not explicitly set it to false. false is default value
            //in Pegasus Properties.
            //p.setProperty( "pegasus.execute.*.filesystem.local", "false" );
        }
        
        return p;
    }

    /**
     * Returns a boolean indicating if properties are setup for condor io
     *
     * @param properties
     *
     * @return boolean
     */
    public boolean setupForCondorIO( PegasusProperties properties ){
        String configuration  = properties.getProperty( PEGASUS_CONFIGURATION_PROPERTY_KEY ) ;

        return ( configuration == null )?
                false:
                configuration.equalsIgnoreCase( CONDOR_CONFIGURATION_VALUE ) ||
                    configuration.equalsIgnoreCase( DEPRECATED_CONDOR_CONFIGURATION_VALUE );
    }
    
    /**
     * Checks for a property, if it does not exist then sets the property to 
     * the value passed
     * 
     * @param key   the property key
     * @param value the value to set to 
     */
    protected void checkAndSetProperty( PegasusProperties properties, String key, String value ) {
        String propValue = properties.getProperty( key );
        if( propValue == null ){
            //set the value
            properties.setProperty( key, value );
        }
        else{
            //log a warning 
            StringBuffer sb = new StringBuffer();
            sb.append( "Property Key " ).append( key ).append( " already set to " ).
               append( propValue ).append( ". Will not be set to - ").append( value );
            mLogger.log( sb.toString(), LogManager.WARNING_MESSAGE_LEVEL );
        }
    }

    /**
     * Returns the deperecated value message
     *
     * @param property              the property
     * @param deprecatedValue       the deprecated value
     * @param updatedValue           the updated value
     *
     * @return message
     */
    protected String deprecatedValueMessage(String property, String deprecatedValue, String updatedValue) {
        StringBuffer sb = new StringBuffer();
        sb.append( " The property " ).append(  property ) .append( " = " ).append( deprecatedValue ).
           append( " is deprecated. Replace with ").append( property ) .append( " = " ).
           append( updatedValue );

        return sb.toString();
    }

    
}
