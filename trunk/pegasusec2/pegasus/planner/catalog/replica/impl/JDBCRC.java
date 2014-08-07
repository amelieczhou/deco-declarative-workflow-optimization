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


package edu.isi.pegasus.planner.catalog.replica.impl;

import edu.isi.pegasus.common.logging.LogManagerFactory;
import java.util.*;
import java.sql.*;
import edu.isi.pegasus.planner.catalog.Catalog;
import edu.isi.pegasus.planner.catalog.ReplicaCatalog;
import edu.isi.pegasus.planner.catalog.replica.ReplicaCatalogEntry;
import edu.isi.pegasus.common.util.CommonProperties;
import edu.isi.pegasus.common.logging.LogManager;

/**
 * This class implements a replica catalog on top of a simple table in a
 * JDBC database. This enables a variety of replica catalog
 * implementations in a transactionally safe, concurrent environment.
 * The table must be defined using the statements appropriate for your
 * database - they are part of the setup in $PEGASUS_HOME/sql/.
 *
 * If you chose to use an unsupported database, please check, if your
 * database either supports sequence number, or if it supports auto
 * increment columns. If your database supports sequences (e.g.
 * PostGreSQL), you can use a setup similar to the following (for
 * Oracle, the autoinc can be implemented via a trigger).
 *
 * <pre>
 * create sequence rc_lfn_id;
 *
 * create table rc_lfn (
 *   id      bigint default nextval('rc_lfn_id'::text),
 *   lfn     varchar(255) not null,
 *   pfn     varchar(255) not null,
 *
 *   constraint pk_rc_lfn primary key(id),
 *   constraint sk_rc_lfn unique(lfn,pfn)
 * );
 *
 * create index idx_rc_lfn on rc_lfn(lfn);
 *
 * create table rc_attr (
 *   id      bigint,
 *   name    varchar(64) not null,
 *   value   varchar(255) not null,
 *
 *   constraint pk_rc_attr primary key(id,name),
 *   constraint fk_rc_attr foreign key(id) references rc_lfn(id) on delete cascade
 * );
 *
 * create index idx_rc_attr on rc_attr(name);
 * </pre>
 *
 * In case of databases that do not support sequences (e.g. MySQL), do
 * not specify the <code>create sequence</code>, and use an
 * auto-increment column for the primary key instead, e.g.:
 *
 * <pre>
 * create table rc_lfn (
 *   id      bigint default null auto_increment,
 *   lfn     varchar(255) not null,
 *   pfn     varchar(255) not null,
 *
 *   constraint pk_rc_lfn primary key(id),
 *   constraint sk_rc_lfn unique(lfn,pfn)
 * );
 *
 * create index idx_rc_lfn on rc_lfn(lfn);
 *
 * create table rc_attr (
 *   id      bigint,
 *   name    varchar(64) not null,
 *   value   varchar(255) not null,
 *
 *   constraint pk_rc_attr primary key(id,name),
 *   constraint fk_rc_attr foreign key id references rc_lfn(id) on delete cascade
 * );
 *
 * create index idx_rc_attr on rc_attr(name);
 * </pre>
 *
 * The site attribute should be specified whenever possible. For the
 * shell planner, it will always be of value "local".
 *
 * @author Jens-S. Vöckler
 * @author Yong Zhao
 * @version $Revision$
 */
public class JDBCRC implements ReplicaCatalog
{
  /**
   * This message is sent whenever one of the member function is executed
   * which relies on an established database context.
   */
  private static final String c_error =
    "The database connection is not established";

  /**
   * Maintains the connection to the database over the lifetime of
   * this instance.
   */
  protected Connection mConnection = null;

  /**
   * Maintains an essential set of prepared statement, ready to use.
   */
  protected PreparedStatement mStatements[] = null;

  /**
     * The handle to the logging object.
     */
    protected LogManager mLogger;


  /**
   * The statement to prepare to slurp attributes.
   */
  private static final String mCStatements[] =
  { // 0:
    "SELECT name,value FROM rc_attr WHERE id=?",
    // 1:
    "SELECT id,pfn FROM rc_lfn WHERE lfn=?",
    // 2:
    "SELECT r.id,r.pfn FROM rc_lfn r, rc_attr a WHERE r.id=a.id" +
    " AND r.lfn=? AND a.name=? AND a.value=?",
    // 3:
    "SELECT r.id,r.pfn FROM rc_lfn r, rc_attr a WHERE r.id=a.id" +
    " AND r.lfn=? AND a.name=? AND a.value IS NULL",
    // 4:
    "INSERT INTO rc_attr(id,name,value) VALUES(?,?,?)",
    // 5:
    "DELETE FROM rc_lfn WHERE lfn=?",
    // 6:
    "DELETE FROM rc_lfn WHERE id IN" +
    " ( SELECT id FROM rc_attr WHERE name=? AND value=? )",
    // 7:
    "DELETE FROM rc_lfn WHERE id IN" +
    " ( SELECT id FROM rc_attr WHERE name=? AND value IS NULL )",
    // 8:
    "DELETE FROM rc_lfn WHERE lfn=? AND id IN" +
    " ( SELECT id FROM rc_attr WHERE name=? AND value=? )",
    // 9:
    "DELETE FROM rc_lfn WHERE lfn=? AND id IN" +
    " ( SELECT id FROM rc_attr WHERE name=? AND value IS NULL )",
  };

  /**
   * Remembers if obtaining generated keys will work or not.
   */
  private boolean m_autoinc = false;

  /**
   * Convenience c'tor: Establishes the connection to the replica
   * catalog database. The usual suspects for the class name include:
   *
   * <pre>
   * org.postgresql.Driver
   * com.mysql.jdbc.Driver
   * com.microsoft.jdbc.sqlserver.SQLServerDriver
   * SQLite.JDBCDriver
   * sun.jdbc.odbc.JdbcOdbcDriver
   * </pre>
   *
   * @param jdbc is a string containing the full name of the java class
   * that must be dynamically loaded. This is usually an external jar
   * file which contains the Java database driver.
   * @param url is the database driving URL. This string is database
   * specific, and tell the JDBC driver, at which host and port the
   * database listens, permits additional arguments, and selects the
   * database inside the rDBMS to connect to. Please refer to your
   * JDBC driver documentation for the format and permitted values.
   * @param username is the database user account name to connect with.
   * @param password is the database account password to use.
   *
   * @throws LinkageError if linking the dynamically loaded driver fails.
   * This is a run-time error, and does not need to be caught.
   * @throws ExceptionInInitializerError if the initialization function
   * of the driver's instantiation threw an exception itself. This is a
   * run-time error, and does not need to be caught.
   * @throws ClassNotFoundException if the class in your jdbc parameter
   * cannot be found in your given CLASSPATH environment. Callers must
   * catch this exception.
   * @throws SQLException if something goes awry with the database.
   * Callers must catch this exception.
   */
  public JDBCRC( String jdbc, String url, String username, String password )
    throws LinkageError, ExceptionInInitializerError, ClassNotFoundException,
	   SQLException
  {
      this();
    // load database driver jar
    Class.forName( jdbc );
    // may throw LinkageError,
    // may throw ExceptionInInitializerError,
    // may throw ClassNotFoundException

    // establish connection to database generically
    connect( url, username, password );
    // may throws SQLException
  }

  /**
   * Default empty constructor creates an object that is not yet connected
   * to any database. You must use support methods to connect before this
   * instance becomes usable.
   *
   * @see #connect( String, String, String )
   */
  public JDBCRC()
  {
    // make connection defunc
    mConnection = null;
    mStatements = null;
    mLogger =  LogManagerFactory.loadSingletonInstance();
  }

  /**
   * Connects to the database. This is effectively an accessor to
   * initialize the internal connection instance variable. <b>Warning!
   * You must call {@link java.lang.Class#forName( String )} yourself
   * to load the database JDBC driver jar!</b>
   *
   * @param url is the database driving URL. This string is database
   * specific, and tell the JDBC driver, at which host and port the
   * database listens, permits additional arguments, and selects the
   * database inside the rDBMS to connect to. Please refer to your
   * JDBC driver documentation for the format and permitted values.
   * @param username is the database user account name to connect with.
   * @param password is the database account password to use.
   * @throws SQLException if something goes awry with the database.
   * Callers must catch this exception.
   * @see #JDBCRC( String, String, String, String )
   * @see java.sql.DriverManager#getConnection( String, String, String )
   */
  public void connect( String url, String username, String password )
    throws SQLException
  {
    // establish connection to database generically
    mConnection = DriverManager.getConnection( url, username, password );

    // may throws SQLException
    m_autoinc = mConnection.getMetaData().supportsGetGeneratedKeys();

    // prepared statements are Singletons -- prepared on demand
    mStatements = new PreparedStatement[ mCStatements.length ];
    for ( int i=0; i < mCStatements.length; ++i ) mStatements[i] = null;
  }

  /**
   * Establishes a connection to the database from the properties. You
   * can specify a <tt>driver</tt> property to contain the class name of
   * the JDBC driver for your database. This property will be removed
   * before attempting to connect. You must speficy a <tt>url</tt>
   * property to describe the connection. It will be removed before
   * attempting to connect.
   *
   * @param props is the property table with sufficient settings to
   * establish a link with the database. The minimum key required key is
   * "url", and possibly "driver". Any other keys depend on the database
   * driver.
   * @return true if connected, false if failed to connect.
   * @see java.sql.DriverManager#getConnection( String, Properties )
   *
   * @throws Error subclasses for runtime errors in the class loader.
   */
  public boolean connect( Properties props ){

      boolean result = false;
        // class loader: Will propagate any runtime errors!!!
        String driver = (String) props.remove("db.driver");

        Properties localProps = CommonProperties.matchingSubset( (Properties)props.clone(), "db", false );

        String url = (String) localProps.remove("url");
        if (url == null || url.length() == 0) {
            return result;
        }


        try {
            if (driver != null) {
                //only support mysql and postgres for time being
                if( driver.equalsIgnoreCase( "MySQL") ){
                    driver = "com.mysql.jdbc.Driver";
                }
                else if ( driver.equalsIgnoreCase( "Postgres" )){
                    driver = "org.postgresql.Driver";
                }
                Class.forName(driver);
            }
        }
        catch (Exception e) {
            mLogger.log( "While connecting to JDBCRC Replica Catalog", e, LogManager.DEBUG_MESSAGE_LEVEL );
            return result;
        }

        try {
            mConnection = DriverManager.getConnection( url, localProps );
            m_autoinc = mConnection.getMetaData().supportsGetGeneratedKeys();

            // prepared statements are Singletons -- prepared on demand
            mStatements = new PreparedStatement[mCStatements.length];
            for (int i = 0; i < mCStatements.length; ++i) {
                mStatements[i] = null;
            }

            result = true;
        }
        catch (SQLException e) {
            mLogger.log( "While connecting to JDBCRC Replica Catalog", e , LogManager.DEBUG_MESSAGE_LEVEL );
            result = false;
        }

        return result;
  }

  /**
   * Explicitely free resources before the garbage collection hits.
   */
  public void close()
  {

    if ( mConnection != null ) {
      try {
	if ( ! mConnection.getAutoCommit() ) mConnection.commit();
      } catch ( SQLException e ) {
	// ignore
      }
    }

    if ( mStatements != null ) {
      try {
	for ( int i=0; i < mCStatements.length; ++i ) {
	  if ( mStatements[i] != null ) {
	    mStatements[i].close();
	    mStatements[i] = null;
	  }
	}
      } catch ( SQLException e ) {
	// ignore
      } finally {
	mStatements = null;
      }
    }

    if ( mConnection != null ) {
      try {
	mConnection.close();
      } catch ( SQLException e ) {
	// ignore
      } finally {
	mConnection = null;
      }
    }
  }

  /**
   * Predicate to check, if the connection with the catalog's
   * implementation is still active. This helps determining, if it makes
   * sense to call <code>close()</code>.
   *
   * @return true, if the implementation is disassociated, false otherwise.
   * @see #close()
   */
  public boolean isClosed()
  {
    return ( mConnection == null );
  }

  /**
   * Quotes a string that may contain special SQL characters.
   * @param s is the raw string.
   * @return the quoted string, which may be just the input string.
   */
  protected String quote( String s )
  {
    if ( s.indexOf('\'') != -1 ) {
      StringBuffer result = new StringBuffer();
      for ( int i=0; i < s.length(); ++i ) {
        char ch = s.charAt(i);
        result.append(ch);
        if ( ch == '\'' ) result.append(ch);
      }
      return result.toString();
    } else {
      return s;
    }
  }

  /**
   * Singleton manager for prepared statements. This instruction
   * checks that a prepared statement is ready to use, and will
   * create an instance of the prepared statement, if it was unused
   * previously.
   *
   * @param i is the index which prepared statement to check.
   * @return a handle to the prepared statement.
   */
  protected PreparedStatement getStatement( int i )
    throws SQLException
  {
    if ( mStatements[i] == null )
      mStatements[i] = mConnection.prepareStatement( mCStatements[i] );
    else
      mStatements[i].clearParameters();

    return mStatements[i];
  }

  /**
   * Retrieves the entry for a given filename and site handle from the
   * replica catalog.
   *
   * @param lfn is the logical filename to obtain information for.
   * @param handle is the resource handle to obtain entries for.
   * @return the (first) matching physical filename, or
   * <code>null</code> if no match was found.
   */
  public String lookup( String lfn, String handle )
  {
    String result = null;
    int which = ( handle == null ? 3 : 2 );
    String query = mCStatements[which];

    // sanity check
    if ( lfn == null ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      PreparedStatement ps = getStatement(which);
      ps.setString( 1, quote(lfn) );
      ps.setString( 2, quote(ReplicaCatalogEntry.RESOURCE_HANDLE) );
      if ( handle != null ) ps.setString( 3, quote(handle) );

      // there should only be one result
      ResultSet rs = ps.executeQuery();
      if ( rs.next() ) result = rs.getString("pfn");

      rs.close();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to query database about " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Slurps all attributes from related to a mapping into a map.
   *
   * @param id is the reference id to slurp from as string. Especially
   * Postgres's indexing mechanism goes from tables scans to btrees, if
   * the numeric key is represented as a string. Strings should be safe
   * for other databases, too.
   * @return a Map with the attributes, which may be empty.
   */
  private Map attributes( String id )
    throws SQLException
  {
    Map result = new TreeMap();

    // sanity checks
    if ( id == null ) return result;

    // parametrize
    PreparedStatement ps = getStatement(0);
    ps.setString( 1, id );

    // slurp results
    ResultSet rs = ps.executeQuery();
    while ( rs.next() ) result.put( rs.getString(1), rs.getString(2) );

    // done
    rs.close();
    return result;
  }

  /**
   * Retrieves all entries for a given LFN from the replica catalog.
   * Each entry in the result set is a tuple of a PFN and all its
   * attributes.
   *
   * @param lfn is the logical filename to obtain information for.
   * @return a collection of replica catalog entries
   * @see ReplicaCatalogEntry
   */
  public Collection lookup( String lfn )
  {
    List result = new ArrayList();
    String query = mCStatements[1];

    // sanity check
    if ( lfn == null ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    // start to ask
    try {
      PreparedStatement ps = getStatement(1);
      ps.setString( 1, quote(lfn) );

      ResultSet rs = ps.executeQuery();
      while ( rs.next() ) {
	result.add( new ReplicaCatalogEntry( rs.getString("pfn"),
				attributes(rs.getString("id")) ) );
      }

      rs.close();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to query database about " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Retrieves all entries for a given LFN from the replica catalog.
   * Each entry in the result set is just a PFN string. Duplicates
   * are reduced through the set paradigm.
   *
   * @param lfn is the logical filename to obtain information for.
   * @return a set of PFN strings
   */
  public Set lookupNoAttributes( String lfn )
  {
    Set result = new TreeSet();
    String query = mCStatements[1];

    // sanity check
    if ( lfn == null ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    // start to ask
    try {
      PreparedStatement ps = getStatement(1);
      ps.setString( 1, quote(lfn) );

      ResultSet rs = ps.executeQuery(query);
      while ( rs.next() ) result.add( rs.getString("pfn") );

      rs.close();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to query database about " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Retrieves multiple entries for a given logical filename, up to the
   * complete catalog. Retrieving full catalogs should be harmful, but
   * may be helpful in an online display or portal.
   *
   * @param lfns is a set of logical filename strings to look up.
   * @return a map indexed by the LFN. Each value is a collection
   * of replica catalog entries for the LFN.
   * @see org.griphyn.common.catalog.ReplicaCatalogEntry
   */
  public Map lookup( Set lfns )
  {
    Map result = new HashMap();
    String query = mCStatements[1];

    // sanity check
    if ( lfns == null || lfns.size() == 0 ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      ResultSet rs = null;
      PreparedStatement ps = getStatement(1);
      for ( Iterator i = lfns.iterator(); i.hasNext(); ) {
	List value = new ArrayList();
	String lfn = (String) i.next();
	ps.setString( 1, quote(lfn) );
	rs = ps.executeQuery();
	while ( rs.next() ) {
	  value.add( new ReplicaCatalogEntry( rs.getString("pfn"),
				attributes(rs.getString("id")) ) );
	}
	rs.close();
	result.put( lfn, value );
      }
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to query database with " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Retrieves multiple entries for a given logical filename, up to the
   * complete catalog. Retrieving full catalogs should be harmful, but
   * may be helpful in an online display or portal.
   *
   * @param lfns is a set of logical filename strings to look up.
   * @return a map indexed by the LFN. Each value is a set
   * of PFN strings.
   */
  public Map lookupNoAttributes( Set lfns )
  {
    Map result = new HashMap();
    String query = mCStatements[1];

    // sanity check
    if ( lfns == null || lfns.size() == 0 ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      ResultSet rs = null;
      PreparedStatement ps = getStatement(1);
      for ( Iterator i = lfns.iterator(); i.hasNext(); ) {
	Set value = new TreeSet();
	String lfn = (String) i.next();
	ps.setString( 1, quote(lfn) );
	rs = ps.executeQuery();
	while ( rs.next() ) value.add(rs.getString("pfn"));
	rs.close();
	result.put( lfn, value );
      }
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to query database with " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Retrieves multiple entries for a given logical filename, up to the
   * complete catalog. Retrieving full catalogs should be harmful, but
   * may be helpful in online display or portal.<p>
   *
   * @param lfns is a set of logical filename strings to look up.
   * @param handle is the resource handle, restricting the LFNs.
   * @return a map indexed by the LFN. Each value is a collection
   * of replica catalog entries (all attributes).
   * @see ReplicaCatalogEntry
   */
  public Map lookup( Set lfns, String handle )
  {
    Map result = new HashMap();
    int which = ( handle == null ? 3 : 2 );
    String query = mCStatements[which];

    // sanity check
    if ( lfns == null || lfns.size() == 0 ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      ResultSet rs = null;
      PreparedStatement ps = getStatement(which);
      ps.setString( 2, quote(ReplicaCatalogEntry.RESOURCE_HANDLE) );
      if ( handle != null ) ps.setString( 3, quote(handle) );

      for ( Iterator i = lfns.iterator(); i.hasNext(); ) {
	List value = new ArrayList();
	String lfn = (String) i.next();
	ps.setString( 1, quote(lfn) );
	rs = ps.executeQuery();
	while ( rs.next() ) {
	  value.add( new ReplicaCatalogEntry( rs.getString("pfn"),
				attributes(rs.getString("id")) ) );
	}
	rs.close();
	result.put( lfn, value );
      }
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to query database with " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Retrieves multiple entries for a given logical filename, up to the
   * complete catalog. Retrieving full catalogs should be harmful, but
   * may be helpful in online display or portal.<p>
   *
   * @param lfns is a set of logical filename strings to look up.
   * @param handle is the resource handle, restricting the LFNs.
   * @return a map indexed by the LFN. Each value is a set of
   * physical filenames.
   */
  public Map lookupNoAttributes( Set lfns, String handle )
  {
    Map result = new HashMap();
    int which = ( handle == null ? 3 : 2 );
    String query = mCStatements[which];

    // sanity check
    if ( lfns == null || lfns.size() == 0 ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      ResultSet rs = null;
      PreparedStatement ps = getStatement(which);
      ps.setString( 2, quote(ReplicaCatalogEntry.RESOURCE_HANDLE) );
      if ( handle != null ) ps.setString( 3, quote(handle) );

      for ( Iterator i = lfns.iterator(); i.hasNext(); ) {
	Set value = new TreeSet();
	String lfn = (String) i.next();
	ps.setString( 1, quote(lfn) );
	rs = ps.executeQuery();
	while ( rs.next() ) value.add( rs.getString("pfn") );
	rs.close();
	result.put( lfn, value );
      }
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to query database with " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Helper function to assemble various pieces.
   *
   * @param value is the value of the object from the map.
   * @param obj is the name of the table column
   * @param where is the decision, if we had a previous WHERE clause or not.
   * @see #lookup( Map )
   */
  private String addItem( Object value, String obj, boolean where )
  {
    // sanity check, no column can be NULL
    if ( value == null ) return new String();

    String v = ( value instanceof String ) ? (String) value : value.toString();
    StringBuffer q = new StringBuffer(80);
    q.append( where ? " AND " : " WHERE " );
    q.append(obj).append(" LIKE '").append(quote(v)).append('\'');

    return q.toString();
  }

  /**
   * Retrieves multiple entries for a given logical filename, up to the
   * complete catalog. Retrieving full catalogs should be harmful, but
   * may be helpful in online display or portal.
   *
   * @param constraints is mapping of keys 'lfn', 'pfn', or any
   * attribute name, e.g. the resource handle 'site', to a string that
   * has some meaning to the implementing system. This can be a SQL
   * wildcard for queries, or a regular expression for Java-based memory
   * collections. Unknown keys are ignored. Using an empty map requests
   * the complete catalog.
   * @return a map indexed by the LFN. Each value is a collection
   * of replica catalog entries.
   * @see ReplicaCatalogEntry
   */
  public Map lookup( Map constraints )
  {
    Map result = new TreeMap();

    // more sanity
    if ( mConnection == null ) throw new RuntimeException( c_error );

    // prepare statement
    //boolean flag = false;
    boolean where = false;
    StringBuffer q = new StringBuffer(256);
    q.append("SELECT DISTINCT r.id,r.lfn,r.pfn FROM rc_lfn r, rc_attr a");

    for ( Iterator i=constraints.keySet().iterator(); i.hasNext(); ) {
      String s, key = (String) i.next();
      if ( key.equals("lfn") ) {
	s = addItem( constraints.get("lfn"), "r.lfn", where );
        where = true;
      } else if ( key.equals("pfn") ) {
	s = addItem( constraints.get("pfn"), "r.pfn", where );
        where = true;
      } else {
	if ( ! where ) {
	  q.append( where ? " AND " : " WHERE " ).append( "r.id=a.id" );
	  where = true;
	}
	//s = addItem( constraints.get(key), "a." + key, where );
        //add the clause to check on attribute name
        s = addItem( key, "a.name", where );
        where = true;
        s = addItem( constraints.get(key), "a.value", where );
      }
      if ( s.length() > 0 ) {
	where = true;
	q.append(s);
      }
    }

    // start to ask
    String lfn = null;
    ReplicaCatalogEntry pair = null;
    String query = q.toString();
    try {
      Statement st = mConnection.createStatement();
      ResultSet rs = st.executeQuery(query);
      while ( rs.next() ) {
	lfn = rs.getString("lfn");
	pair = new ReplicaCatalogEntry( rs.getString("pfn"),
					attributes(rs.getString("id")) );

	// add list, if the LFN does not already exist
	if ( ! result.containsKey(lfn) ) result.put( lfn, new ArrayList() );

	// now add to the list
	((List) result.get(lfn)).add( pair );
      }
      rs.close();
      st.close();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to query database about " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Lists all logical filenames in the catalog.
   *
   * @return A set of all logical filenames known to the catalog.
   */
  public Set list()
  {
    // short-cut
    return list( null );
  }

  /**
   * Lists a subset of all logical filenames in the catalog.
   *
   * @param constraint is a constraint for the logical filename only. It
   * is a string that has some meaning to the implementing system. This
   * can be a SQL wildcard for queries, or a regular expression for
   * Java-based memory collections.
   * @return A set of logical filenames that match. The set may be empty
   */
  public Set list( String constraint )
  {
    Set result = new TreeSet();

    // more sanity
    if ( mConnection == null ) throw new RuntimeException( c_error );

    // prepare statement
    // FIXME: work with pre-prepared statements
    String query = "SELECT lfn FROM rc_lfn";
    if ( constraint != null && constraint.length() > 0 )
      query += " WHERE lfn LIKE '" + quote(constraint) + "'";

    // start to ask
    try {
      Statement st = mConnection.createStatement();
      ResultSet rs = st.executeQuery(query);
      while ( rs.next() ) {
	result.add( rs.getString(0) );
      }
      rs.close();
      st.close();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to query database about " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }



  /**
   * Inserts a new mapping into the replica catalog.
   *
   * @param lfn is the logical filename under which to book the entry.
   * @param tuple is the physical filename and associated PFN attributes.
   *
   * @return number of insertions, should always be 1. On failure,
   * throw an exception, don't use zero.
   */
  public int insert( String lfn, ReplicaCatalogEntry tuple )
  {
    String query = "[no query]";
    int result = 0;
    boolean autoCommitWasOn = false;
    int state = 0;

    // sanity checks
    if ( lfn == null || tuple == null ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      // delete-before-insert as one transaction
      if ( (autoCommitWasOn = mConnection.getAutoCommit()) )
	mConnection.setAutoCommit(false);
      state++; // state == 1

      // // delete before insert...
      // delete( lfn, tuple.getPFN() );
      state++; // state == 2

      ResultSet rs = null;
      Statement st = null;
      StringBuffer m = new StringBuffer(256);
      String id = null;
      if ( ! m_autoinc ) {
	//
	// use sequences, no auto-generated keys possible
	//
	query = "SELECT nextval('rc_lfn_id')";
	st = mConnection.createStatement();
	rs = st.executeQuery(query);
        if ( rs.next() ) id = rs.getString(1);
	else throw new SQLException( "Unable to access sequence generator" );
	rs.close();
	st.close();
	state++; // state == 3

	m.append( "INSERT INTO rc_lfn(id,lfn,pfn) VALUES('" );
	m.append(id).append("','");
	m.append(quote(lfn)).append("','");
	m.append(quote(tuple.getPFN())).append("')");
	query = m.toString();
	st = mConnection.createStatement();
	result = st.executeUpdate(query); // ,Statement.RETURN_GENERATED_KEYS);
	st.close();
	state++; // state == 4
      } else {
	//
	// use autoinc columns, obtain autogenerated keys afterwards
	//
	m.append( "INSERT INTO rc_lfn(lfn,pfn) VALUES('" );
	m.append(quote(lfn)).append("','");
	m.append(quote(tuple.getPFN())).append("')");
	query = m.toString();
	st = mConnection.createStatement();
	result = st.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
	state++; // state == 3

	rs = st.getGeneratedKeys();
        if ( rs.next() ) id = rs.getString(1);
	else throw new SQLException( "Unable to access autogenerated key" );
	rs.close();
	st.close();
	state++; // state == 4
      }

      query = mCStatements[4];
      PreparedStatement ps = getStatement(4);
//      ps.setString( 1, id ); // GRRR, Pg8!!!
      ps.setLong( 1, Long.parseLong(id) );

      for ( Iterator i=tuple.getAttributeIterator(); i.hasNext(); ) {
	String name = (String) i.next();
	Object value = tuple.getAttribute(name);
	ps.setString( 2, name );
	if ( value == null ) ps.setNull( 3, Types.VARCHAR );
	else ps.setString( 3, value instanceof String ?
			   (String) value :
			   value.toString() );
	ps.executeUpdate();
      }
      state++; // state == 5

      mConnection.commit();
    } catch ( SQLException e ) {
      try {
	if ( state > 0 && state < 4 ) mConnection.rollback();
      } catch ( SQLException e2 ) {
	// ignore rollback problems
      }

      throw new RuntimeException( "Unable to tell database " +
				  query + " (state=" + state + "): " +
				  e.getMessage() );
    } finally {
      // restore original auto-commit state
      try {
	if ( autoCommitWasOn ) mConnection.setAutoCommit(true);
      } catch ( SQLException e ) {
	// ignore
      }
    }

    return result;
  }

  /**
   * Inserts a new mapping into the replica catalog. This is a
   * convenience function exposing the resource handle. Internally, the
   * <code>ReplicaCatalogEntry</code> element will be contructed, and
   * passed to the appropriate insert function.
   *
   * @param lfn is the logical filename under which to book the entry.
   * @param pfn is the physical filename associated with it.
   * @param handle is a resource handle where the PFN resides.
   * @return number of insertions, should always be 1. On failure,
   * throw an exception, don't use zero.
   * @see #insert( String, ReplicaCatalogEntry )
   * @see ReplicaCatalogEntry
   */
  public int insert( String lfn, String pfn, String handle )
  {
    return insert( lfn, new ReplicaCatalogEntry( pfn, handle ) );
  }

  /**
   * Inserts multiple mappings into the replica catalog. The input is a
   * map indexed by the LFN. The value for each LFN key is a collection
   * of replica catalog entries.
   *
   * @param x is a map from logical filename string to list of replica
   * catalog entries.
   * @return the number of insertions.
   * @see org.griphyn.common.catalog.ReplicaCatalogEntry
   */
  public int insert( Map x )
  {
    int result = 0;

    // sanity checks
    if ( x == null || x.size() == 0 ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    // FIXME: Create a true bulk mode. This is inefficient, but will
    // get the job done (for now).
    Set lfns = x.keySet();
    for ( Iterator i=lfns.iterator(); i.hasNext(); ) {
      String lfn = (String) i.next();
      List value = (List) x.get(lfn);
      if ( value != null && value.size() > 0 ) {
	for ( Iterator j=value.iterator(); j.hasNext(); ) {
	  result += insert( lfn, (ReplicaCatalogEntry) j.next() );
	}
      }
    }

    // done
    return result;
  }

  /**
   * Deletes multiple mappings into the replica catalog. The input is a
   * map indexed by the LFN. The value for each LFN key is a collection
   * of replica catalog entries. On setting matchAttributes to false, all entries
   * having matching lfn pfn mapping to an entry in the Map are deleted.
   * However, upon removal of an entry, all attributes associated with the pfn
   * also evaporate (cascaded deletion).
   *
   * @param x                is a map from logical filename string to list of
   *                         replica catalog entries.
   * @param matchAttributes  whether mapping should be deleted only if all
   *                         attributes match.
   *
   * @return the number of deletions.
   * @see ReplicaCatalogEntry
   */
  public int delete( Map x , boolean matchAttributes){
      
      int result = 0;
      
      //do a sequential delete for the time being
      for(Iterator it = x.entrySet().iterator();it.hasNext();){
          Map.Entry entry = (Map.Entry)it.next();
          String lfn = (String)entry.getKey();
          Collection c   = (Collection)entry.getValue();

          //iterate through all RCE's for this lfn and delete
          for(Iterator rceIt = c.iterator();rceIt.hasNext();){
              ReplicaCatalogEntry rce = (ReplicaCatalogEntry)rceIt.next();

                if( matchAttributes ){
                    //we are deleting a very specific mapping
                    result += delete(lfn,rce);
                }
                else{
                    //deleting the lfn and pfn mapping, and rely on
                    //cascaded deletes to delete the associated 
                    //attributes.
                    result += this.delete( lfn, rce.getPFN() );
                }
          }
      }
      return result;
  }



  /**
   * Deletes a specific mapping from the replica catalog. We don't care
   * about the resource handle. More than one entry could theoretically
   * be removed. Upon removal of an entry, all attributes associated
   * with the PFN also evaporate (cascading deletion).
   *
   * @param lfn is the logical filename in the tuple.
   * @param pfn is the physical filename in the tuple.
   * @return the number of removed entries.
   */
  public int delete( String lfn, String pfn )
  {
    int result = 0;

    // sanity checks
    if ( lfn == null || pfn == null ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    // prepare statement
    // FIXME: work with pre-prepared statements
    StringBuffer m = new StringBuffer(256);
    m.append( "DELETE FROM rc_lfn WHERE lfn='");
    m.append( quote(lfn) ).append('\'');
    m.append( " AND pfn='").append( quote(pfn) ).append('\'');
    String query = m.toString();

    try {
      Statement st = mConnection.createStatement();
      st.execute(query);
      result = st.getUpdateCount();
      st.close();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to tell database " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Deletes a very specific mapping from the replica catalog. The LFN
   * must be matches, the PFN, and all PFN attributes specified in the
   * replica catalog entry. More than one entry could theoretically be
   * removed. Upon removal of an entry, all attributes associated with
   * the PFN also evaporate (cascading deletion).
   *
   * @param lfn is the logical filename in the tuple.
   * @param tuple is a description of the PFN and its attributes.
   * @return the number of removed entries, either 0 or 1.
   */
  public int delete( String lfn, ReplicaCatalogEntry tuple )
  {
    int result = 0;
    String query = "[no query]";

    // sanity checks
    if ( lfn == null || tuple == null ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      StringBuffer m = new StringBuffer(256);
      for ( Iterator i=tuple.getAttributeIterator(); i.hasNext(); ) {
	String name = (String) i.next();
	Object value = tuple.getAttribute(name);
	m.append( "SELECT id FROM rc_attr WHERE name='");
	m.append(quote(name)).append( "' AND value" );
	if ( value == null ) m.append( " IS NULL" );
	else m.append("='").append(quote(value.toString())).append('\'');
	if ( i.hasNext() ) m.append( " INTERSECT " );
      }
      query = m.toString();

      m = new StringBuffer(256);
      m.append( "DELETE FROM rc_lfn WHERE lfn='" ).append(quote(lfn));
      m.append("' AND pfn='" ).append(quote(tuple.getPFN()));
      m.append("' AND id=?");

      Statement st = mConnection.createStatement();
      ResultSet rs = st.executeQuery(query);

      query = m.toString();
      PreparedStatement ps = mConnection.prepareStatement(query);
      while ( rs.next() ) {
	ps.setString( 1, rs.getString(1) );
	result += ps.executeUpdate();
      }
      ps.close();
      rs.close();
      st.close();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to tell database " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Deletes all PFN entries for a given LFN from the replica catalog
   * where the PFN attribute is found, and matches exactly the object
   * value. This method may be useful to remove all replica entries that
   * have a certain MD5 sum associated with them. It may also be harmful
   * overkill.
   *
   * @param lfn is the logical filename to look for.
   * @param name is the PFN attribute name to look for.
   * @param value is an exact match of the attribute value to match.
   * @return the number of removed entries.
   */
  public int delete( String lfn, String name, Object value )
  {
    int result = 0;
    int which = value == null ? 9 : 8;
    String query = mCStatements[which];

    // sanity checks
    if ( lfn == null || name == null ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      PreparedStatement ps = getStatement(which);
      ps.setString( 1, quote(lfn) );
      ps.setString( 2, quote(name) );
      if ( value != null ) ps.setString( 3, quote(value.toString()) );
      result = ps.executeUpdate();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to tell database " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Deletes all PFN entries for a given LFN from the replica catalog
   * where the resource handle is found. Karan requested this
   * convenience method, which can be coded like
   * <pre>
   *  delete( lfn, RESOURCE_HANDLE, handle )
   * </pre>
   *
   * @param lfn is the logical filename to look for.
   * @param handle is the resource handle
   * @return the number of entries removed.
   */
  public int deleteByResource( String lfn, String handle )
  {
    return delete( lfn, ReplicaCatalogEntry.RESOURCE_HANDLE, handle );
  }

  /**
   * Removes all mappings for an LFN from the replica catalog.
   *
   * @param lfn is the logical filename to remove all mappings for.
   * @return the number of removed entries.
   */
  public int remove( String lfn )
  {
    int result = 0;
    String query = mCStatements[5];

    // sanity checks
    if ( lfn == null ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      PreparedStatement ps = getStatement(5);
      ps.setString( 1, quote(lfn) );
      result = ps.executeUpdate();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to tell database " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Removes all mappings for a set of LFNs.
   *
   * @param lfns is a set of logical filename to remove all mappings for.
   * @return the number of removed entries.
   */
  public int remove( Set lfns )
  {
    int result = 0;
    String query = mCStatements[5];

    // sanity checks
    if ( lfns == null || lfns.size() == 0 ) return result;
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      PreparedStatement ps = getStatement(5);
      for ( Iterator i = lfns.iterator(); i.hasNext(); ) {
	ps.setString( 1, quote((String) i.next()) );
	result += ps.executeUpdate();
      }
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to tell database " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }

  /**
   * Removes all entries from the replica catalog where the PFN attribute
   * is found, and matches exactly the object value.
   *
   * @param name is the PFN attribute name to look for.
   * @param value is an exact match of the attribute value to match.
   * @return the number of removed entries.
   */
  public int removeByAttribute( String name, Object value )
  {
    int result = 0;
    int which = value == null ? 7 : 6;
    String query = mCStatements[which];

    // sanity checks
    if ( mConnection == null ) throw new RuntimeException( c_error );

    try {
      PreparedStatement ps = getStatement(which);
      ps.setString( 1, quote(name) );
      if ( value != null ) ps.setString( 2, value.toString() );
      result = ps.executeUpdate();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to tell database " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }


  /**
   * Removes all entries associated with a particular resource handle.
   * This is useful, if a site goes offline. It is a convenience method,
   * which calls the generic <code>removeByAttribute</code> method.
   *
   * @param handle is the site handle to remove all entries for.
   * @return the number of removed entries.
   * @see #removeByAttribute( String, Object )
   */
  public int removeByAttribute( String handle )
  {
    return removeByAttribute( ReplicaCatalogEntry.RESOURCE_HANDLE, handle );
  }


  /**
   * Removes everything. Use with caution!
   *
   * @return the number of removed entries.
   */
  public int clear()
  {
    int result = 0;

    // sanity checks
    if ( mConnection == null ) throw new RuntimeException( c_error );

    // prepare statement
    String query = "DELETE FROM lfn_rc";
    try {
      Statement st = mConnection.createStatement();
      st.execute(query);
      result = st.getUpdateCount();
      st.close();
    } catch ( SQLException e ) {
      throw new RuntimeException( "Unable to tell database " +
				  query + ": " + e.getMessage() );
    }

    // done
    return result;
  }
}

