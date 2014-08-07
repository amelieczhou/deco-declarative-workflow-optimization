/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.isi.pegasus.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A common PegasusURL class to use by the planner and other components.
 *
 * The class parses the PegasusURL into 3 main components
 *  - protocol
 *  - hostname
 *  - path
 *
 * The class is consistent with the PegasusURL parsing scheme used in pegasus-transfer.
 *
 * @author Karan Vahi
 * @author  Mats Rynge
 */
public class PegasusURL {
    
    /**
     * The scheme name for file url.
     */
    public static final String FILE_URL_SCHEME = "file:";

    /**
     * The scheme name for file url.
     */
    public static final String SYMLINK_URL_SCHEME = "symlink:";


    /**
     * The default protocol if none is determined from the PegasusURL or path
     */
    public static final String DEFAULT_PROTOCOL = "file";

    /**
     * 
     * Stores the regular expressions necessary to parse a PegasusURL into 3 components
     * protocol, host and path
     */
    private static final String mRegexExpression = "([\\w]+)://([\\w\\.\\-:@]*)(/?[\\S]*)";
            
    /**
     * Stores compiled patterns at first use, quasi-Singleton.
     */
    private static Pattern mPattern = null;

    /**
     * The protocol referred to by the PegasusURL
     */
    private String mProtocol;

    /**
     * The hpstname referred to by the PegasusURL.
     * Can include the port also
     */
    private String mHost;

    /**
     * The path referred to by the PegasusURL
     */
    private String mPath;


    /**
     * The default constructor.
     */
    public PegasusURL(){
        if( mPattern == null ){
             mPattern = Pattern.compile( mRegexExpression );
         }
        reset();
    }

    /**
     * The overloaded constructor.
     *
     * @param url   the url to be parsed internally
     */
    public PegasusURL( String url ){
        this();
        this.parse( url );
    }


    /**
     * Parses the url and populates the internal member variables that can
     * be accessed via the appropriate accessor methods
     *
     * @param url
     */
    public void parse( String url ){
        //reset internal variables
        reset();

        //special case for file url's
        if( url.indexOf( ":" ) == -1 ){
            url = PegasusURL.DEFAULT_PROTOCOL + "://" + url;
        }

        Matcher m = mPattern.matcher( url );
        if( m.matches() ){
            mProtocol = m.group( 1 );
            mHost     = m.group( 2 );
            mPath     = m.group( 3 );
        }
        else{
            throw new RuntimeException( "Unable to pass URL " + url );
        }
    }

    /**
     * Returns the protocol associated with the PegasusURL
     *
     * @return  the protocol else empty
     */
    public String getProtocol(){
        return mProtocol;
    }

     /**
     * Returns the host asscoiated with the PegasusURL
     *
     * @return  the host else empty
     */
    public String getHost(){
        return mHost;
    }

    /**
     * Returns the path associated with the PegasusURL
     *
     * @return  the host else empty
     */
    public String getPath(){
        return mPath;
    }

    /**
     * Returns the url prefix associated with the PegasusURL. The PegasusURL prefix is the part
     * of the PegasusURL composed of protocol and the hostname
     *
     * For example PegasusURL prefix for
     * <pre>
     * gsiftp://dataserver.phys.uwm.edu/~/griphyn_test/ligodemo_output
     * </pre>
     *
     * is gsiftp://dataserver.phys.uwm.edu
     *
     * @return  the host else empty
     */
    public String getURLPrefix(){
        StringBuffer prefix = new StringBuffer();
        prefix.append( this.getProtocol() ).
               append( "://" ).
               append( this.getHost() );
        return prefix.toString();
    }

    /**
     * Resets the internal member variables
     */
    public void reset() {
        mProtocol  = "";
        mHost = "";
        mPath = "";
    }

    /**
     * The contents represented as a string
     * 
     * @return
     */
    public String toString(){
       StringBuffer sb = new StringBuffer();
       sb.append( "protocol -> " ).append( this.getProtocol() ).append( " , " ).
          append( "host -> " ).append( this.getHost() ).append( " , " ).
          append( "path -> " ).append( this.getPath() ).append( " , " ).
          append( "url-prefix -> ").append( this.getURLPrefix() );
       return sb.toString();
    }


    /**
     * Test program
     *
     * @param args
     */
    public static void main( String[] args ){
        //should print
        //protocol -> gsiftp , host -> sukhna.isi.edu , path -> /tmp/test.file , url-prefix -> gsiftp://sukhna.isi.edu
        String url = "gsiftp://sukhna.isi.edu/tmp/test.file";
        System.out.println( url );
        System.out.println( new PegasusURL(url) );


        //should print
        //protocol -> file , host ->  , path -> /tmp/test/k , url-prefix -> file://
        url = "file:///tmp/test/k";
        System.out.println( url );
        System.out.println( new PegasusURL(url) );

        //should print
        //protocol -> gsiftp , host -> dataserver.phys.uwm.edu , path -> /~/griphyn_test/ligodemo_output/ , url-prefix -> gsiftp://dataserver.phys.uwm.edu
        url = "gsiftp://dataserver.phys.uwm.edu/~/griphyn_test/ligodemo_output/" ;
        System.out.println( url );
        System.out.println( new PegasusURL(url) );

        //should print
        //protocol -> file , host ->  , path -> /tmp/path/to/input/file , url-prefix -> file://
        url =  "/tmp/path/to/input/file" ;
        System.out.println( url );
        System.out.println( new PegasusURL(url) );

        url =  "http://isis.isi.edu/" ;
        System.out.println( url );
        System.out.println( new PegasusURL(url) );

        url =  "http://isis.isi.edu/filename" ;
        System.out.println( url );
        System.out.println( new PegasusURL(url) );

        url =  "http://isis.isi.edu/directory/filename" ;
        System.out.println( url );
        System.out.println( new PegasusURL(url) );

    }
}
