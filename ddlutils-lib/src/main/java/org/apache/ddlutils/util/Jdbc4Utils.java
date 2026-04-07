package org.apache.ddlutils.util;

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

import java.sql.Types;

import org.apache.ddlutils.model.TypeMap;

/**
 * Little helper class providing functions for dealing with the newer JDBC 4.0 functionality.
 *
 */
public abstract class Jdbc4Utils
{
    /**
     * Determines whether the system supports the JDBC 4.0 Types
     */
    public static boolean supportsJdbc40Types()
    {
        try {
            return (Types.class.getField(TypeMap.NCHAR) != null) &&
                (Types.class.getField(TypeMap.NVARCHAR) != null) &&
                (Types.class.getField(TypeMap.NCLOB) != null) &&
                (Types.class.getField(TypeMap.LONGNVARCHAR) != null);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Determines the type code for the NCHAR JDBC type.
     *
     * @return The type code
     * @throws UnsupportedOperationException If the NCHAR type is not supported
     */
    public static int determineNCharTypeCode()
        throws UnsupportedOperationException
    {
        try {
            return Types.class.getField(TypeMap.NCHAR).getInt(null);
        } catch (Exception ex) {
            throw new UnsupportedOperationException("The jdbc type NCHAR is not supported");
        }
    }

    /**
     * Determines the type code for the NVARCHAR JDBC type.
     *
     * @return The type code
     * @throws UnsupportedOperationException If the NVARCHAR type is not supported
     */
    public static int determineNVarcharTypeCode()
        throws UnsupportedOperationException
    {
        try {
            return Types.class.getField(TypeMap.NVARCHAR).getInt(null);
        } catch (Exception ex) {
            throw new UnsupportedOperationException("The jdbc type NVARCHAR is not supported");
        }
    }

    /**
     * Determines the type code for the LONGNVARCHAR JDBC type.
     *
     * @return The type code
     * @throws UnsupportedOperationException If the LONGNVARCHAR type is not supported
     */
    public static int determineLongNVarcharTypeCode()
        throws UnsupportedOperationException
    {
        try {
            return Types.class.getField(TypeMap.LONGNVARCHAR).getInt(null);
        } catch (Exception ex) {
            throw new UnsupportedOperationException("The jdbc type LONGNVARCHAR is not supported");
        }
    }

    /**
     * Determines the type code for the NCLOB JDBC type.
     *
     * @return The type code
     * @throws UnsupportedOperationException If the NCLOB type is not supported
     */
    public static int determineNClobTypeCode()
        throws UnsupportedOperationException
    {
        try {
            return Types.class.getField(TypeMap.NCLOB).getInt(null);
        } catch (Exception ex) {
            throw new UnsupportedOperationException("The jdbc type NCLOB is not supported");
        }
    }

    /**
     * Determines whether the system supports the JDBC 4.2 Types
     */
    public static boolean supportsJdbc42Types()
    {
        try {
            return (Types.class.getField(TypeMap.TIME_WITH_TIMEZONE) != null) &&
                (Types.class.getField(TypeMap.TIMESTAMP_WITH_TIMEZONE) != null);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Determines the type code for the TIME WITH TIMEZONE JDBC type.
     *
     * @return The type code
     * @throws UnsupportedOperationException If the TIME WITH TIMEZONE type is not supported
     */
    public static int determineTimeWithTimezoneTypeCode()
        throws UnsupportedOperationException
    {
        try {
            return Types.class.getField(TypeMap.TIME_WITH_TIMEZONE).getInt(null);
        } catch (Exception ex) {
            throw new UnsupportedOperationException("The jdbc type TIME WITH TIMEZONE is not supported");
        }
    }

    /**
     * Determines the type code for the TIMESTAMP WITH TIMEZONE JDBC type.
     *
     * @return The type code
     * @throws UnsupportedOperationException If the TIMESTAMP WITH TIMEZONE type is not supported
     */
    public static int determineTimestampWithTimezoneTypeCode()
        throws UnsupportedOperationException
    {
        try {
            return Types.class.getField(TypeMap.TIMESTAMP_WITH_TIMEZONE).getInt(null);
        } catch (Exception ex) {
            throw new UnsupportedOperationException("The jdbc type TIMESTAMP WITH TIMEZONE is not supported");
        }
    }
}
