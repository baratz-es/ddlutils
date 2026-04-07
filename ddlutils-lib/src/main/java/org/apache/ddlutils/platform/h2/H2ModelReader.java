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

import java.sql.SQLException;
import java.util.Map;

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.model.ForeignKey;
import org.apache.ddlutils.model.Index;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.platform.DatabaseMetaDataWrapper;
import org.apache.ddlutils.platform.JdbcModelReader;

/**
 * Reads a database model from an H2 database.
 *
 * @author Thomas Dudziak
 * @version $Revision: $
 */
public class H2ModelReader extends JdbcModelReader
{
    /**
     * Creates a new model reader for H2 databases.
     * 
     * @param platform The platform that this model reader belongs to
     */
    public H2ModelReader(Platform platform)
    {
        super(platform);
        setDefaultCatalogPattern(null);
        setDefaultSchemaPattern(null);
    }

    /**
     * {@inheritDoc}
     */
    protected Table readTable(DatabaseMetaDataWrapper metaData, Map values) throws SQLException
    {
        String schema = (String)values.get("TABLE_SCHEM");
        if (schema != null && (schema.equalsIgnoreCase("INFORMATION_SCHEMA")))
        {
            return null;
        }
        return super.readTable(metaData, values);
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isInternalForeignKeyIndex(DatabaseMetaDataWrapper metaData, Table table, ForeignKey fk, Index index)
    {
        String name = index.getName();

        if ((name != null) && name.startsWith("CONSTRAINT_"))
        {
            return true;
        }
        String fkName = getPlatform().getSqlBuilder().getForeignKeyName(table, fk);
        return (fkName != null) && fkName.equals(name);
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isInternalPrimaryKeyIndex(DatabaseMetaDataWrapper metaData, Table table, Index index)
    {
        String name = index.getName();

        return (name != null) && name.startsWith("PRIMARY_KEY_");
    }
}
