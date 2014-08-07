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


package edu.isi.pegasus.planner.code.generator.condor.style;

import java.io.File;

import edu.isi.pegasus.common.credential.CredentialHandlerFactory;
import edu.isi.pegasus.planner.classes.AggregatedJob;
import edu.isi.pegasus.planner.classes.Job;
import edu.isi.pegasus.planner.classes.PegasusBag;
import edu.isi.pegasus.planner.classes.TransferJob;
import edu.isi.pegasus.planner.code.generator.condor.CondorStyleException;
import edu.isi.pegasus.planner.code.generator.condor.CondorStyleFactoryException;
import edu.isi.pegasus.planner.namespace.Pegasus;

/**
 * Enables a job to be directly submitted to the condor pool of which the
 * submit host is a part of.
 * This style is applied for jobs to be run
 *        - on the submit host in the scheduler universe (local pool execution)
 *        - on the local condor pool of which the submit host is a part of
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class Condor extends Abstract {

    //some constants imported from the Condor namespace.
    public static final String UNIVERSE_KEY =
                             edu.isi.pegasus.planner.namespace.Condor.UNIVERSE_KEY;

    public static final String VANILLA_UNIVERSE =
                         edu.isi.pegasus.planner.namespace.Condor.VANILLA_UNIVERSE;

    public static final String SCHEDULER_UNIVERSE =
                         edu.isi.pegasus.planner.namespace.Condor.SCHEDULER_UNIVERSE;

    public static final String STANDARD_UNIVERSE =
                         edu.isi.pegasus.planner.namespace.Condor.STANDARD_UNIVERSE;

    public static final String LOCAL_UNIVERSE =
                         edu.isi.pegasus.planner.namespace.Condor.LOCAL_UNIVERSE;

    public static final String PARALLEL_UNIVERSE =
                         edu.isi.pegasus.planner.namespace.Condor.PARALLEL_UNIVERSE;

    
    public static final String TRANSFER_EXECUTABLE_KEY =
                         edu.isi.pegasus.planner.namespace.Condor.TRANSFER_EXECUTABLE_KEY;

    //

    /**
     * The name of the style being implemented.
     */
    public static final String STYLE_NAME = "Condor";

    /**
     * The Pegasus Lite local wrapper basename.
     */
    public static final String PEGASUS_LITE_LOCAL_FILE_BASENAME = "pegasus-lite-local.sh";

    /**
     * The name of the environment variable for transferring input files
     */
    public static final String PEGASUS_TRANSFER_INPUT_FILES_KEY = "_PEGASUS_TRANSFER_INPUT_FILES";

    /**
     * The name of the environment variable for transferring output files
     */
    public static final String PEGASUS_TRANSFER_OUTPUT_FILES_KEY = "_PEGASUS_TRANSFER_OUTPUT_FILES";


    /**
     * The name of the environment variable for the initial dir for pegasus lite local
     */
    public static final String PEGASUS_INITIAL_DIR_KEY = "_PEGASUS_INITIAL_DIR";

    /**
     * The name of the environment variable that determines if job should be executed in initial dir or not
     */
    public static final String PEGASUS_EXECUTE_IN_INITIAL_DIR = "_PEGASUS_EXECUTE_IN_INITIAL_DIR";

    /**
     * Whether to connect stdin or not
     */
    public static final String PEGASUS_CONNECT_STDIN_KEY = "_PEGASUS_CONNECT_STDIN";

    /**
     * A boolean indicating whether pegasus lite mode is picked up or not.
     */
    private boolean mPegasusLiteEnabled;

    /**
     * Path to Pegasus Lite local wrapper script.
     */
    private String mPegasusLiteLocalWrapper;

    /**
     * The default constructor.
     */
    public Condor() {
        super();
    }

    /**
     * Initializes the Code Style implementation.
     *
     * @param bag  the bag of initialization objects
     * @param credentialFactory   the credential handler factory
     *
     *
     * @throws CondorStyleFactoryException that nests any error that
     *            might occur during the instantiation of the implementation.
     */
    public void initialize( PegasusBag bag , CredentialHandlerFactory credentialFactory )throws CondorStyleException{
        super.initialize( bag, credentialFactory );
        mPegasusLiteEnabled = mProps.getGridStart().equalsIgnoreCase( "PegasusLite" );
        mPegasusLiteLocalWrapper = this.getSubmitHostPathToPegasusLiteLocal();


    }


    /**
     * Applies the condor style to the job. Changes the job so that it results
     * in generation of a condor style submit file that can be directly
     * submitted to the underlying condor scheduler on the submit host, without
     * going through CondorG. This applies to the case of
     *      - local site execution
     *      - submitting directly to the condor pool of which the submit host
     *        is a part of.
     *
     * @param job  the job on which the style needs to be applied.
     *
     * @throws CondorStyleException in case of any error occuring code generation.
     */
    public void apply(Job job) throws CondorStyleException{

        //mLogger.log( "Credentials required for job " + job.getID() + " "  + job.getCredentialTypes() ,
         //            LogManager.DEBUG_MESSAGE_LEVEL );

        //           Removed for JIRA PM-543
//      String execSiteWorkDir = mSiteStore.getInternalWorkDirectory(job);
//        String workdir = (String) job.globusRSL.removeKey("directory"); // returns old value
//        workdir = (workdir == null)?execSiteWorkDir:workdir;
        String workdir = job.getDirectory();

        String defaultUniverse = job.getSiteHandle().equalsIgnoreCase("local")?
                                 Condor.LOCAL_UNIVERSE:
                                 Condor.VANILLA_UNIVERSE;
        String universe = job.condorVariables.containsKey( Condor.UNIVERSE_KEY )?
                              (String)job.condorVariables.get( Condor.UNIVERSE_KEY):
                              defaultUniverse;

        //boolean to indicate whether to use remote_initialdir or not
        //remote_initialdir does not work for standard universe
        boolean useRemoteInitialDir = !universe.equals( Condor.STANDARD_UNIVERSE );
        
        //extra check for standard universe
        if( universe.equals( Condor.STANDARD_UNIVERSE ) ){
            //standard universe should be only applied for compute jobs
            int type = job.getJobType();
            if ( !( type == Job.COMPUTE_JOB ) ) {
                //set universe to vanilla universe
                universe = Condor.VANILLA_UNIVERSE;
                //fix for JIRA PM-531
                //vanilla universe jobs need to have remote_initialdir key
                useRemoteInitialDir = true;
            }
            else{
                //job is a compute job.
                //check if it is clustered . 
                if( job instanceof AggregatedJob ){
                    //clustered jobs can never execute in standard universe
                    //update to vanilla universe. JIRA PM-530
                    universe = Condor.VANILLA_UNIVERSE;
                    //fix for JIRA PM-531
                    //vanilla universe jobs need to have remote_initialdir key
                    useRemoteInitialDir = true;
                }
            }

        }
        
        //set the universe for the job
        // Karan Jan 28, 2008
        job.condorVariables.construct( "universe", universe );

        if( universe.equalsIgnoreCase( Condor.VANILLA_UNIVERSE )  ||
            universe.equalsIgnoreCase( Condor.STANDARD_UNIVERSE ) ||
            universe.equalsIgnoreCase( Condor.PARALLEL_UNIVERSE ) ){
            //the glide in/ flocking case
            //submitting directly to condor
            //check if it is a glide in job.
            //vanilla jobs are glide in jobs?
            //No they are not.

            //set the vds change dir key to trigger -w
            //to kickstart invocation for all non transfer jobs
            if(!(job instanceof TransferJob)){
                job.vdsNS.checkKeyInNS(Pegasus.CHANGE_DIR_KEY, "true");
                //set remote_initialdir for the job only for non transfer jobs
                //this is removed later when kickstart is enabling.

                //added if loop for JIRA PM-543
                if( workdir != null ){
                    if( useRemoteInitialDir ){
                        job.condorVariables.construct("remote_initialdir", workdir);
                    }else{
                        job.condorVariables.construct("initialdir", workdir);
                    }
                }
            }
            else{
                //we need to set s_t_f and w_t_f_o to ensure
                //that condor transfers the proxy to the remote end
                //also the keys below are mutually exclusive to initialdir keys.
                job.condorVariables.construct("should_transfer_files", "YES");
                job.condorVariables.construct("when_to_transfer_output",
                                              "ON_EXIT");
            }
            //isGlobus = false;
            
            applyCredentialsForRemoteExec(job);
        }
        else if(universe.equalsIgnoreCase(Condor.SCHEDULER_UNIVERSE) || universe.equalsIgnoreCase( Condor.LOCAL_UNIVERSE )){

                String ipFiles = job.condorVariables.getIPFilesForTransfer();
                
                //check if the job can be run in the workdir or not
                //and whether intial dir is populated before hand or not.
                if(job.runInWorkDirectory() && !job.condorVariables.containsKey("initialdir")){
                    //for local jobs we need initialdir
                    //instead of remote_initialdir

                    //added if loop for JIRA PM-543
                    if( workdir != null ){
                        job.condorVariables.construct("initialdir", workdir);
                    }
                }



                wrapJobWithLocalPegasusLite( job );

                /*
                if( this.mPegasusLiteEnabled ){
                    //wrap the job with local pegasus lite wrapped job
                    //to work around the Condor IO bug for PegasusLite
                    //PM-542
                    wrapJobWithLocalPegasusLite( job );
                }
                else{
                    //do same as earlier for time being.
                
                    //check explicitly for any input files transferred via condor
                    //file transfer mechanism
                    if( ipFiles != null ){
                        //log a debug message before removing the files
                        StringBuffer sb = new StringBuffer();
                        sb.append( "Removing the following ip files from condor file tx for job " ).
                           append( job.getID() ).append( " " ).append( ipFiles );
                        mLogger.log(  sb.toString(), LogManager.DEBUG_MESSAGE_LEVEL );
                        job.condorVariables.removeIPFilesForTransfer();
                    }
                
                    //check for transfer_executable and remove if set
                    //transfer_executable does not work in local/scheduler universe
                    if( job.condorVariables.containsKey( Condor.TRANSFER_EXECUTABLE_KEY )){
                        job.condorVariables.removeKey( Condor.TRANSFER_EXECUTABLE_KEY );
                        job.condorVariables.removeKey( "should_transfer_files" );
                        job.condorVariables.removeKey( "when_to_transfer_output" );
                    }
                
                    //for local or scheduler universe we never should have
                    //should_transfer_file or w_t_f
                    //the keys can appear if a user in site catalog for local sites
                    //specifies these keys for the vanilla universe jobs
                    if( job.condorVariables.containsKey( "should_transfer_files" ) ||
                        job.condorVariables.containsKey( "when_to_transfer_output" )){
                        job.condorVariables.removeKey( "should_transfer_files" );
                        job.condorVariables.removeKey( "when_to_transfer_output" );
                    }
                }
                */
                applyCredentialsForLocalExec(job);
        }
        else{
            //Is invalid state
            throw new CondorStyleException( errorMessage( job, STYLE_NAME, universe ) );
        }


    }

    
    /**
     * Wraps the local universe jobs with a local Pegasus Lite wrapper to get
     * around the Condor file IO bug for local universe job
     *
     * @param job  the job that needs to be wrapped.
     */
    private void wrapJobWithLocalPegasusLite(Job job) throws CondorStyleException {
        //for the time being doing nothing for dax or dag jobs
        if( job.getJobType() == Job.DAG_JOB || job.getJobType() == Job.DAX_JOB ){
            //do nothing return
            return;
        }

        String ipFiles = job.condorVariables.getIPFilesForTransfer();
        String opFiles = job.condorVariables.getOutputFilesForTransfer();
        
        if( ipFiles == null && opFiles == null ){
            if( job.getRemoteExecutable().startsWith( File.separator ) ){
                //absoluate path specified
                //nothing to do other than check for transfer_executable
                
                //check for transfer_executable and remove if set
                //transfer_executable does not work in local/scheduler universe
                if( job.condorVariables.containsKey( Condor.TRANSFER_EXECUTABLE_KEY )){

                    job.condorVariables.removeKey( Condor.TRANSFER_EXECUTABLE_KEY );
                    job.condorVariables.removeKey( "should_transfer_files" );
                    job.condorVariables.removeKey( "when_to_transfer_output" );
                }

                return;
            }
            //for relative paths for local universe jobs it is better to wrap
            //with wrapper as condor else assumes the executable is in the 
            //directory where the job is launched.
        }
        
        String workdir = (String)job.condorVariables.get( "initialdir" );

        if( workdir != null ){
            job.envVariables.construct( Condor.PEGASUS_INITIAL_DIR_KEY, workdir );


            if ( !this.mPegasusLiteEnabled ){
                //for shared file system mode we want the wrapped job
                //to execute in workdir
                job.envVariables.construct( Condor.PEGASUS_EXECUTE_IN_INITIAL_DIR, "true" );
            }
        }



        //check if any transfer_input_files is transferred
        if( ipFiles != null ){
            String[] files = ipFiles.split( "," );
            StringBuffer value = new StringBuffer();
            for( String f: files ){
                if( f.startsWith( File.separator) ){
                    //absolute path to file specified
                    value.append( f );
                }
                else{
                    //make sure workdir is not null
                    if( workdir == null ){
                        throw new CondorStyleException( "Condor initialdir not set for job "  + job.getID() );
                    }
                    value.append( f );
                }
                value.append( "," );
            }
            job.envVariables.construct( Condor.PEGASUS_TRANSFER_INPUT_FILES_KEY, value.toString() );
            job.condorVariables.removeIPFilesForTransfer();
        }

        //check if any transfer_output_files is transferred
        if( opFiles != null ){

            //sanity check as wrapper requires initialdir to be set
            if( workdir == null ){
                throw new CondorStyleException( "Condor initialdir not set for job "  + job.getID() );
            }

            String[] files = opFiles.split( "," );
            StringBuffer value = new StringBuffer();
            for( String f: files ){
                value.append( f );
                value.append( "," );
            }
            job.envVariables.construct( Condor.PEGASUS_TRANSFER_OUTPUT_FILES_KEY, value.toString() );
            job.condorVariables.removeOutputFilesForTransfer();
        }



        //check for transfer_executable and remove if set
        //transfer_executable does not work in local/scheduler universe
        if( job.condorVariables.containsKey( Condor.TRANSFER_EXECUTABLE_KEY )){

            job.condorVariables.removeKey( Condor.TRANSFER_EXECUTABLE_KEY );
            job.condorVariables.removeKey( "should_transfer_files" );
            job.condorVariables.removeKey( "when_to_transfer_output" );
        }

        //the job executable is now an argument to pegasus-lite-local
        String executable = job.getRemoteExecutable();
        String arguments = job.getArguments();

        job.setRemoteExecutable( this.mPegasusLiteLocalWrapper );
        StringBuffer args = new StringBuffer();
        args.append( executable ).append( " " ).append( arguments );
        job.setArguments( args.toString() );

        String stdin = (String)job.condorVariables.get( "input" ) ;
        if( stdin != null ){
            //tell the wrapper to connect the stdin
            job.envVariables.construct( Condor.PEGASUS_CONNECT_STDIN_KEY, "true" );
        }

        //for local or scheduler universe we never should have
        //should_transfer_file or w_t_f
        //the keys can appear if a user in site catalog for local sites
        //specifies these keys for the vanilla universe jobs
        if( job.condorVariables.containsKey( "should_transfer_files" ) ){

                job.condorVariables.removeKey( "should_transfer_files" );
                job.condorVariables.removeKey( "when_to_transfer_output" );
        }
    }


    /**
     * Determines the path to PegasusLite local job
     *
     * @return the path on the submit host.
     */
    protected String getSubmitHostPathToPegasusLiteLocal() {
        StringBuffer path = new StringBuffer();

        //first get the path to the share directory
        File share = mProps.getSharedDir();
        if( share == null ){
            throw new RuntimeException( "Property for Pegasus share directory is not set" );
        }

        path.append( share.getAbsolutePath() ).append( File.separator ).
             append( "sh" ).append( File.separator ).append( Condor.PEGASUS_LITE_LOCAL_FILE_BASENAME );

        return path.toString();
    }

}
