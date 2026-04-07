package org.apache.ddlutils.platform.h2;

/*
 * Copyright 1999-2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.apache.ddlutils.DdlUtilsException;
import org.apache.ddlutils.PlatformInfo;
import org.apache.ddlutils.platform.PlatformImplBase;

/**
 * The platform implementation for the H2 database.
 * 
 * @author Thomas Dudziak
 * @version $Revision: $
 */
public class H2Platform extends PlatformImplBase
{
    /** Database name of this platform. */
    public static final String DATABASENAME     = "H2";
    /** The standard H2 jdbc driver. */
    public static final String JDBC_DRIVER      = "org.h2.Driver";
    /** The subprotocol used by the standard H2 driver. */
    public static final String JDBC_SUBPROTOCOL = "h2";

    /**
     * Creates a new instance of the H2 platform.
     */
    public H2Platform()
    {
        PlatformInfo info = getPlatformInfo();

        info.setMaxIdentifierLength(256);
        info.setNullAsDefaultValueRequired(false);
        info.setPrimaryKeyEmbedded(true);
        info.setForeignKeysEmbedded(true);
        info.setIndicesEmbedded(false);
        info.setNonPKIdentityColumnsSupported(true);
        info.setLastIdentityValueReadable(true);

        info.addNativeTypeMapping(Types.ARRAY,       "BINARY",        Types.VARBINARY);
        info.addNativeTypeMapping(Types.BLOB,        "BINARY",        Types.VARBINARY);
        info.addNativeTypeMapping(Types.CLOB,        "CLOB",         Types.CLOB);
        info.addNativeTypeMapping(Types.DISTINCT,    "BINARY",        Types.VARBINARY);
        info.addNativeTypeMapping(Types.FLOAT,       "REAL",          Types.REAL);
        info.addNativeTypeMapping(Types.JAVA_OBJECT, "OTHER");
        info.addNativeTypeMapping(Types.LONGVARBINARY, "BINARY",      Types.VARBINARY);
        info.addNativeTypeMapping(Types.LONGVARCHAR, "VARCHAR",       Types.VARCHAR);
        info.addNativeTypeMapping(Types.NULL,        "BINARY",        Types.VARBINARY);
        info.addNativeTypeMapping(Types.REF,        "BINARY",        Types.VARBINARY);
        info.addNativeTypeMapping(Types.STRUCT,     "BINARY",        Types.VARBINARY);
        info.addNativeTypeMapping(Types.TINYINT,    "SMALLINT",      Types.SMALLINT);
        info.addNativeTypeMapping(Types.TIMESTAMP_WITH_TIMEZONE, "TIMESTAMP", Types.TIMESTAMP);
        info.addNativeTypeMapping(Types.TIME_WITH_TIMEZONE, "TIME", Types.TIME);

        info.addNativeTypeMapping("BIT",      "BOOLEAN",       "BOOLEAN");
        info.addNativeTypeMapping("DATALINK", "BINARY",        "VARBINARY");

        info.setDefaultSize(Types.CHAR,      255);
        info.setDefaultSize(Types.VARCHAR,   65535);
        info.setDefaultSize(Types.BINARY,     255);
        info.setDefaultSize(Types.VARBINARY, 65535);
        info.setDefaultSize(Types.DECIMAL,    65536);

        setSqlBuilder(new H2Builder(this));
        setModelReader(new H2ModelReader(this));
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return DATABASENAME;
    }

    /**
     * {@inheritDoc}
     */
    public void shutdownDatabase(Connection connection)
    {
        Statement stmt = null;

        try
        {
            stmt = connection.createStatement();
            stmt.executeUpdate("SHUTDOWN");
        }
        catch (SQLException ex)
        {
            throw new DdlUtilsException(ex);
        }
        finally
        {
            closeStatement(stmt);    
        }
    }
}
