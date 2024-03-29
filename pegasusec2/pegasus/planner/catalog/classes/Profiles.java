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


import edu.isi.pegasus.planner.classes.Profile;

import edu.isi.pegasus.planner.namespace.Namespace;
import edu.isi.pegasus.planner.namespace.Pegasus;
import edu.isi.pegasus.planner.namespace.Dagman;
import edu.isi.pegasus.planner.namespace.ENV;
import edu.isi.pegasus.planner.namespace.Globus;
import edu.isi.pegasus.planner.namespace.Hints;
import edu.isi.pegasus.planner.namespace.Condor;
import edu.isi.pegasus.planner.namespace.Stat;
import edu.isi.pegasus.planner.namespace.Selector;

import java.util.List;
import java.util.EnumMap;
import java.util.Iterator;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;


import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maintains profiles for different namespaces.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class Profiles {

    
	
    /**
     * The enumeration of valid namespaces. It should be 
     */
    public static enum NAMESPACES {

        env, globus, condor, dagman, pegasus, hints,selector,stat
    };
    
	private Logger logger;   

    /**
     * An enum map that associates the enum keys with the corresponding 
     * namespace objects.
     */
    private EnumMap<NAMESPACES,Namespace> mProfileMap;

    /**
     * The default constructor.
     */
    public Profiles() {
        mProfileMap = new EnumMap<NAMESPACES, Namespace>( NAMESPACES.class );
                
        mProfileMap.put( NAMESPACES.condor, new Condor() );
        mProfileMap.put( NAMESPACES.dagman, new Dagman() );
        mProfileMap.put( NAMESPACES.env, new ENV() );
        mProfileMap.put( NAMESPACES.globus, new Globus() );
        mProfileMap.put( NAMESPACES.hints, new Hints() );
        mProfileMap.put( NAMESPACES.pegasus, new Pegasus() );
        mProfileMap.put( NAMESPACES.selector, new Selector() );
        mProfileMap.put( NAMESPACES.stat, new Stat() );
    }


    /**
     * Adds multiple profiles.
     *
     * @param profiles the profiles object
     */
    public void addProfiles( Profiles profiles ) {

        //traverse through all the enum keys
        for ( NAMESPACES n : NAMESPACES.values() ){
            Namespace nm = profiles.get( n );
            for( Iterator it = nm.getProfileKeyIterator(); it.hasNext(); ){
                String key = (String) it.next();
                this.addProfile( new Profile( n.toString(), key, (String)nm.get( key ) ));
            }
        }

    }


    /**
     * Adds multiple profiles.
     * 
     * @param profiles  List of <code>Profile</code> objects.
     */
    public void addProfiles( List<Profile> profiles ) {
        for( Profile profile: profiles ){
            this.addProfile( profile );
        }
    }

    /**
     * Adds multiple profiles . to namespace bypassing any underlying namespace
     * specific checks. The catalog parsers should use this function
     *
     * @param profiles the profiles object
     */
    public void addProfilesDirectly( Profiles profiles ) {

        //traverse through all the enum keys
        for ( NAMESPACES n : NAMESPACES.values() ){
            Namespace nm = profiles.get( n );
            for( Iterator it = nm.getProfileKeyIterator(); it.hasNext(); ){
                String key = (String) it.next();
                this.addProfileDirectly( new Profile( n.toString(), key, (String)nm.get( key ) ));
            }
        }

    }

    /**
     * Adds multiple profiles to namespace bypassing any underlying namespace
     * specific checks. The catalog parsers should use this function.
     *
     *
     * @param profiles  List of <code>Profile</code> objects.
     */
    public void addProfilesDirectly( List<Profile> profiles ) {
        for ( Profile profile : profiles ){
            this.addProfileDirectly( profile);
        }
    }

    /**
     * Adds a profile directly to namespace bypassing any underlying namespace
     * specific checks. The catalog parsers should use this function.
     *
     * @param p  the profile to be added
     */
    public void addProfileDirectly( Profile p ){
        //retrieve the appropriate namespace and then add
        Namespace n = ( Namespace )mProfileMap.get( NAMESPACES.valueOf( p.getProfileNamespace() ) );
        n.construct( p.getProfileKey(), p.getProfileValue() );
    }

    /**
     * Adds a profile.
     * 
     * @param p  the profile to be added
     */
    public void addProfile( Profile p ){
        //retrieve the appropriate namespace and then add
        Namespace n = ( Namespace )mProfileMap.get( NAMESPACES.valueOf( p.getProfileNamespace() ) );
        n.checkKeyInNS( p.getProfileKey(), p.getProfileValue() );
    }

    /**
     * Add a profile. Convenience method
     * @param namespace
     * @param key
     * @param value
     */
    public void addProfileDirectly( NAMESPACES namespace, String key, String value ){
        //retrieve the appropriate namespace and then add
        Namespace n = ( Namespace )mProfileMap.get(namespace);
        n.construct(key,value);
    }
    
    /**
     * Add a profile. Convenience method
     * @param namespace
     * @param key
     * @param value
     */
     public void addProfileDirectly( String namespace, String key, String value ){
        //retrieve the appropriate namespace and then add
        Namespace n = ( Namespace )mProfileMap.get(namespace);
        n.construct(key,value);
    }
    
    

    /**
     * Add a profile. Convenience method
     * @param namespace
     * @param key
     * @param value
     */
     public void addProfile( NAMESPACES namespace, String key, String value ){
        //retrieve the appropriate namespace and then add
        Namespace n = ( Namespace )mProfileMap.get(namespace);
        n.checkKeyInNS(key,value);
    }
    
    /**
     * Add a profile. Convenience method
     * @param namespace
     * @param key
     * @param value
     */
     public void addProfile( String namespace, String key, String value ){
        //retrieve the appropriate namespace and then add
        Namespace n = ( Namespace )mProfileMap.get(namespace);
        n.checkKeyInNS(key,value);
    }
    

    /**
     * Returns the list of profiles for all namespaces.
     *
     * @return List of <code>Profiles</code>
     */
    public List<Profile> getProfiles( ){

        List<Profile> result = new LinkedList();

        //traverse through all the enum keys
        for ( NAMESPACES n : NAMESPACES.values() ){
            Namespace nm = this.get( n );
            for( Iterator it = nm.getProfileKeyIterator(); it.hasNext(); ){
                String key = ( String )it.next();
                result.add( new Profile( n.toString(), key, (String)nm.get( key ) ));
            }
        }
        return result;
    }
    
    /**
     * Returns the list of profiles corresponding to a single namespace
     * 
     * @param namespace   the namespace
     * 
     * @return List of <code>Profiles</code>
     */
    public List<Profile> getProfiles( String namespace ){

       return this.getProfiles( NAMESPACES.valueOf( namespace.toLowerCase() ));
    }

    /**
     * Returns the list of profiles corresponding to a single namespace
     *
     * @param namespace   the namespace
     *
     * @return List of <code>Profiles</code>
     */
    public List<Profile> getProfiles( NAMESPACES namespace ){
	if(this.get(namespace) == null)
		throw new RuntimeException(this+"get namespace "+namespace+" is null");
        return this.getProfiles( this.get(namespace) );
    }


    /**
     * Returns the list of profiles corresponding to a single namespace
     *
     * @param namespace   the namespace
     *
     * @return List of <code>Profiles</code>
     */
    public List<Profile> getProfiles( Namespace namespace ){

        List<Profile> result = new LinkedList();
	 //if(namespace.isEmpty())
                //throw new RuntimeException(namespace+" is empty");

        for( Iterator it = namespace.getProfileKeyIterator(); it.hasNext(); ){
            String key = ( String )it.next();
            result.add( new Profile( namespace.namespaceName(), key, (String)namespace.get( key ) ));
		//logger.log(Level.CONFIG,"getting result "+result);
            //throw new RuntimeException( "the first added name is " +  namespace.namespaceName()+" key is "+ key+" and result is "+ (String)namespace.get( key ) );
        }

        return result;
    }




    /**
     * Returns a  iterator over the profile keys corresponding to a particular namespace.
     * 
     * @param n   the namespace
     *
     * @return  iterator
     */
    public Iterator getProfileKeyIterator( NAMESPACES n ){
        return (( Namespace )mProfileMap.get( n )).getProfileKeyIterator();
    }
    
    /**
     * Returns the namespace object corresponding to a namespace
     *
     * @param n   the namespace
     *
     * @return Namespace
     */
    public Namespace get( NAMESPACES n ){
        return ( Namespace )mProfileMap.get( n );
    }

    /**
     * Returns a boolean indicating if the object is empty.
     *
     * The object is empty if all the underlying profile maps are empty.
     *
     * @return
     */
    public boolean isEmpty(){
         boolean result = true;
         for ( NAMESPACES n : NAMESPACES.values() ){
            if( !this.get( n ).isEmpty() ){
                result = false;
                break;
            }
         }
         return result;
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
        
        
        //traverse through all the enum keys
        for ( NAMESPACES n : NAMESPACES.values() ){
            Namespace nm = this.get( n );
            for( Iterator it = nm.getProfileKeyIterator(); it.hasNext(); ){                
                String key = ( String )it.next();
                //write out the  xml element
                writer.write( indent );
                writer.write( "<profile" );
                writeAttribute( writer, "namespace", n.toString() );
                writeAttribute( writer, "key", key  );
                writer.write( " >" );
                writer.write( (String)nm.get( key ) );
                writer.write( "</profile>" );
                writer.write( newLine );                
            }
        }
    }


    /**
     * Returns the string description of the object. 
     *
     * @return String containing the object in XML.
     *
     * @throws RuntimeException if something fishy happens to the stream.
     */
    public String toString()  {
        try {
            Writer writer = new StringWriter(32);
            toString(writer, "");
            return writer.toString();
        } catch (IOException ex) {
            throw new RuntimeException( "Exception while converting to String", ex );
        }
    }

    /**
     * Writes out the contents of the object as a String
     *
     * @param writer is a Writer opened and ready for writing. This can also
     *               be a StringWriter for efficient output.
     * @param indent the indent to be used.
     *
     * @exception IOException if something fishy happens to the stream.
     */
    public void toString( Writer writer, String indent ) throws IOException {
        String newLine = System.getProperty( "line.separator", "\r\n" );


        //traverse through all the enum keys
        for ( NAMESPACES n : NAMESPACES.values() ){
            Namespace nm = this.get( n );
            for( Iterator it = nm.getProfileKeyIterator(); it.hasNext(); ){
                String key = ( String )it.next();
                //write out the  xml element
                writer.write( indent );
                writer.write( "profile" );
                writer.write( " " );writer.write( n.toString() );
                writer.write( " " );writer.write ( key );
                writer.write( " " );writer.write(  (String)nm.get( key ) );
                writer.write( newLine );
            }
        }
    }
    
    
    
    /**
     * Returns the clone of the object.
     *
     * @return the clone
     */
    public Object clone(){
        Profiles obj;
        
        obj = new Profiles();
            
        //traverse through all the enum keys
        for ( NAMESPACES n : NAMESPACES.values() ){
            Namespace nm = this.get( n );
            nm =  ( Namespace )this.get( n ).clone();
            obj.mProfileMap.put( n, nm );
        }
        return obj;
    }
    
    /**
     * Returns the xml description of the object. This is used for generating
     * the partition graph. That is no longer done.
     *
     * @return String containing the object in XML.
     *
     * @exception IOException if something fishy happens to the stream.
     */
    public String toXML() throws IOException{
        Writer writer = new StringWriter(32);
        toXML( writer, "" );
        return writer.toString();
    }


    /**
     * Writes an attribute to the stream. Wraps the value in quotes as required
     * by XML.
     *
     * @param writer
     * @param key
     * @param value
     *
     * @exception IOException if something fishy happens to the stream.
     */
    public void writeAttribute( Writer writer, String key, String value ) throws IOException{
        writer.write( " " );
        writer.write( key );
        writer.write( "=\"");
        writer.write( value );
        writer.write( "\"" );
    }
    

    /**
     * Returns the index for the namespace.
     *
     * @param u  the unit
     * @return the index.
     */
    private int getIndex( NAMESPACES u ){
        return u.ordinal();
    }
   
    
    /**
     * 
     * @param args
     */
    public static void main ( String[] args ){
        try {
            Profiles p = new Profiles();
            p.addProfile(new Profile("pegasus", "gridstart", "none"));
            p.addProfile(new Profile("env", "PEGASUS_HOME", "/pegasus"));
            p.addProfile(new Profile("env", "GLOBUS_LOCATION", "GLOBUS_LOCATION"));

            System.out.println("Profiles are " + p.toXML());
        } catch (IOException ex) {
            Logger.getLogger(Profiles.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
