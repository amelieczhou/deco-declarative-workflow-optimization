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

package edu.isi.pegasus.planner.catalog.replica;

import edu.isi.pegasus.common.util.DynamicLoader;
import edu.isi.pegasus.common.util.CommonProperties;
import edu.isi.pegasus.planner.catalog.ReplicaCatalog;
import java.lang.reflect.*;
import java.io.IOException;
import java.util.Properties;
import java.util.MissingResourceException;



import edu.isi.pegasus.planner.common.PegasusProperties;
import java.util.Enumeration;

/**
 * This factory loads a replica catalog, as specified by the properties.
 * Each invocation of the factory will result in a new instance of a
 * connection to the replica catalog.
 *
 * @author Karan Vahi
 * @author Jens-S. Vöckler
 * @version $Revision$
 *
 * @see edu.isi.pegasus.planner.catalog.replica.ReplicaCatalog
 * @see edu.isi.pegasus.planner.catalog.replica.ReplicaCatalogEntry
 * @see edu.isi.pegasus.planner.catalog.replica.impl.JDBCRC
 */
public class ReplicaFactory{

    /**
     * Package to prefix "just" class names with.
     */
    public static final String DEFAULT_PACKAGE =   "edu.isi.pegasus.planner.catalog.replica.impl";


    /**
     * Connects the interface with the replica catalog implementation. The
     * choice of backend is configured through properties. This class is
     * useful for non-singleton instances that may require changing
     * properties.
     *
     * @param props is an instance of properties to use.
     *
     * @exception ClassNotFoundException if the schema for the database
     * cannot be loaded. You might want to check your CLASSPATH, too.
     * @exception NoSuchMethodException if the schema's constructor interface
     * does not comply with the database driver API.
     * @exception InstantiationException if the schema class is an abstract
     * class instead of a concrete implementation.
     * @exception IllegalAccessException if the constructor for the schema
     * class it not publicly accessible to this package.
     * @exception InvocationTargetException if the constructor of the schema
     * throws an exception while being dynamically loaded.
     *
     * @see org.griphyn.common.util.CommonProperties
     * @see #loadInstance()
     */
    static public ReplicaCatalog loadInstance( PegasusProperties props )
           throws ClassNotFoundException, IOException,
           NoSuchMethodException, InstantiationException,
           IllegalAccessException, InvocationTargetException {

        return loadInstance( props.getVDSProperties() );
    }


    /**
     * Connects the interface with the replica catalog implementation. The
     * choice of backend is configured through properties. This class is
     * useful for non-singleton instances that may require changing
     * properties.
     *
     * @param props is an instance of properties to use.
     *
     * @exception ClassNotFoundException if the schema for the database
     * cannot be loaded. You might want to check your CLASSPATH, too.
     * @exception NoSuchMethodException if the schema's constructor interface
     * does not comply with the database driver API.
     * @exception InstantiationException if the schema class is an abstract
     * class instead of a concrete implementation.
     * @exception IllegalAccessException if the constructor for the schema
     * class it not publicly accessible to this package.
     * @exception InvocationTargetException if the constructor of the schema
     * throws an exception while being dynamically loaded.
     *
     * @see org.griphyn.common.util.CommonProperties
     * @see #loadInstance()
     */
    static public ReplicaCatalog loadInstance( CommonProperties props )
      throws ClassNotFoundException, IOException,
             NoSuchMethodException, InstantiationException,
             IllegalAccessException, InvocationTargetException
    {
        // sanity check

        if ( props == null ) throw new NullPointerException("invalid properties");


        Properties connect = props.matchingSubset( ReplicaCatalog.c_prefix, false );

        //get the default db driver properties in first pegasus.catalog.*.db.driver.*
        Properties db = props.matchingSubset( ReplicaCatalog.DB_ALL_PREFIX, false );
        //now overload with the work catalog specific db properties.
        //pegasus.catalog.work.db.driver.*
        db.putAll( props.matchingSubset( ReplicaCatalog.DB_PREFIX , false ) );


        //to make sure that no confusion happens.
        //add the db prefix to all the db properties
        for( Enumeration e = db.propertyNames(); e.hasMoreElements(); ){
            String key = (String)e.nextElement();
            connect.put( "db." + key, db.getProperty( key ));
        }

        //put the driver property back into the DB property
 //       String driver = props.getProperty( ReplicaCatalog.DBDRIVER_PREFIX );
 //      driver = ( driver == null )? driver = props.getProperty( ReplicaCatalog.DBDRIVER_ALL_PREFIX ): driver;
 //      connect.put( "db.driver", driver );



        // determine the class that implements the work catalog
        return loadInstance( props.getProperty( ReplicaCatalog.c_prefix ),
                             connect );




    }


  /**
   * Connects the interface with the replica catalog implementation. The
   * choice of backend is configured through properties. This class is
   * useful for non-singleton instances that may require changing
   * properties.
   *
   * @param props is an instance of properties to use.
   *
   * @exception ClassNotFoundException if the schema for the database
   * cannot be loaded. You might want to check your CLASSPATH, too.
   * @exception NoSuchMethodException if the schema's constructor interface
   * does not comply with the database driver API.
   * @exception InstantiationException if the schema class is an abstract
   * class instead of a concrete implementation.
   * @exception IllegalAccessException if the constructor for the schema
   * class it not publicly accessible to this package.
   * @exception InvocationTargetException if the constructor of the schema
   * throws an exception while being dynamically loaded.
   *
   * @see org.griphyn.common.util.CommonProperties
   * @see #loadInstance()
   */
  static public ReplicaCatalog loadInstance( String catalogImplementor,
                                             Properties props )
    throws ClassNotFoundException, IOException,
	   NoSuchMethodException, InstantiationException,
	   IllegalAccessException, InvocationTargetException
  {
    ReplicaCatalog result = null;



    if ( catalogImplementor == null )
      throw new RuntimeException( "You need to specify the " +
				  ReplicaCatalog.c_prefix + " property" );
    // for Karan: 2005-10-27
    if ( catalogImplementor.equalsIgnoreCase("rls") ){
      catalogImplementor = "RLI";
    }

    //File also means SimpleFile
    if( catalogImplementor.equalsIgnoreCase( "File" ) ){
      catalogImplementor = "SimpleFile";
    }

    // syntactic sugar adds absolute class prefix
    if ( catalogImplementor.indexOf('.') == -1 )
      catalogImplementor = DEFAULT_PACKAGE + "." + catalogImplementor;
    // POSTCONDITION: we have now a fully-qualified classname

    DynamicLoader dl = new DynamicLoader( catalogImplementor );
    result = (ReplicaCatalog) dl.instantiate( new Object[0] );
    if ( result == null )
      throw new RuntimeException( "Unable to load " + catalogImplementor );


    if ( ! result.connect( props ) )
      throw new RuntimeException( "Unable to connect to replica catalog implementation" );

    // done
    return result;
  }

  /**
   * Connects the interface with the replica catalog implementation. The
   * choice of backend is configured through properties. This method uses
   * default properties from the property singleton.
   *
   * @exception ClassNotFoundException if the schema for the database
   * cannot be loaded. You might want to check your CLASSPATH, too.
   * @exception NoSuchMethodException if the schema's constructor interface
   * does not comply with the database driver API.
   * @exception InstantiationException if the schema class is an abstract
   * class instead of a concrete implementation.
   * @exception IllegalAccessException if the constructor for the schema
   * class it not publicly accessible to this package.
   * @exception InvocationTargetException if the constructor of the schema
   * throws an exception while being dynamically loaded.
   * @exception MissingResourceException if the properties could not
   * be loaded properly.
   *
   * @see org.griphyn.common.util.CommonProperties
   * @see #loadInstance( org.griphyn.common.util.CommonProperties )
   */
  static public ReplicaCatalog loadInstance()
    throws ClassNotFoundException, IOException,
	   NoSuchMethodException, InstantiationException,
	   IllegalAccessException, InvocationTargetException,
	   MissingResourceException
  {
    return loadInstance( CommonProperties.instance() );
  }
}
