package org.apache.ddlutils.platform.postgresql;

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

import java.io.IOException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.alteration.AddColumnChange;
import org.apache.ddlutils.alteration.AddPrimaryKeyChange;
import org.apache.ddlutils.alteration.ColumnAutoIncrementChange;
import org.apache.ddlutils.alteration.ColumnChange;
import org.apache.ddlutils.alteration.PrimaryKeyChange;
import org.apache.ddlutils.alteration.RemoveColumnChange;
import org.apache.ddlutils.alteration.RemovePrimaryKeyChange;
import org.apache.ddlutils.alteration.TableChange;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Index;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.platform.SqlBuilder;

/**
 * The SQL Builder for PostgresSql.
 * 
 * @author John Thorhauer
 * @author Thomas Dudziak
 * @version $Revision$
 */
public class PostgreSqlBuilder extends SqlBuilder
{
    /**
     * Creates a new builder instance.
     * 
     * @param platform The plaftform this builder belongs to
     */
    public PostgreSqlBuilder(Platform platform)
    {
        super(platform);
        // we need to handle the backslash first otherwise the other
        // already escaped sequences would be affected
        addEscapedCharSequence("\\", "\\\\");
        addEscapedCharSequence("'",  "\\'");
        addEscapedCharSequence("\b", "\\b");
        addEscapedCharSequence("\f", "\\f");
        addEscapedCharSequence("\n", "\\n");
        addEscapedCharSequence("\r", "\\r");
        addEscapedCharSequence("\t", "\\t");
    }

    /**
     * {@inheritDoc}
     */
    public void dropTable(Table table) throws IOException
    { 
        print("DROP TABLE ");
        printIdentifier(getTableName(table));
        print(" CASCADE");
        printEndOfStatement();

        Column[] columns = table.getAutoIncrementColumns();

        for (int idx = 0; idx < columns.length; idx++)
        {
            dropAutoIncrementSequence(table, columns[idx]);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternalIndexDropStmt(Table table, Index index) throws IOException
    {
        print("DROP INDEX ");
        printIdentifier(getIndexName(index));
        printEndOfStatement();
    }

    /**
     * {@inheritDoc}
     */
    public void createTable(Database database, Table table, Map parameters) throws IOException
    {
        createAutoIncrementSequencesBeforeCreateTable(table);
        super.createTable(database, table, parameters);
    }

    /**
     * Emits standalone {@code CREATE SEQUENCE} statements before {@code CREATE TABLE} for legacy
     * {@code nextval}-based auto-increment columns. Subclasses (e.g. PostgreSQL 14+ IDENTITY) may override
     * to skip this when the dialect uses {@code GENERATED ... AS IDENTITY} instead.
     *
     * @param table The table being created
     */
    protected void createAutoIncrementSequencesBeforeCreateTable(Table table) throws IOException
    {
        for (int idx = 0; idx < table.getColumnCount(); idx++)
        {
            Column column = table.getColumn(idx);

            if (column.isAutoIncrement())
            {
                createAutoIncrementSequence(table, column);
            }
        }
    }

    /**
     * Creates the auto-increment sequence that is then used in the column.
     *
     * @param table  The table
     * @param column The column
     */
    protected void createAutoIncrementSequence(Table table, Column column) throws IOException
    {
        print("CREATE SEQUENCE ");
        printIdentifier(getConstraintName(null, table, column.getName(), "seq"));
        printEndOfStatement();
    }

    /**
     * Drops the auto-increment sequence used by the legacy {@code nextval} default.
     *
     * @param table  The table
     * @param column The column
     */
    protected void dropAutoIncrementSequence(Table table, Column column) throws IOException
    {
        print("DROP SEQUENCE ");
        printIdentifier(getConstraintName(null, table, column.getName(), "seq"));
        printEndOfStatement();
    }

    /**
     * {@inheritDoc}
     * <p>
     * BIT/BOOLEAN map to {@code BOOLEAN} in PostgreSQL, but {@link org.apache.ddlutils.model.TypeMap} treats
     * these JDBC types as numeric, which would emit {@code DEFAULT 0}/{@code DEFAULT 1} (integer literals).
     * PostgreSQL requires boolean literals for {@code BOOLEAN} columns.
     */
    @Override
    protected void printDefaultValue(Object defaultValue, int typeCode) throws IOException
    {
        if (defaultValue != null && (typeCode == Types.BIT || typeCode == Types.BOOLEAN))
        {
            String s = defaultValue.toString().trim();
            if ("1".equals(s) || "true".equalsIgnoreCase(s))
            {
                print("TRUE");
            }
            else if ("0".equals(s) || "false".equalsIgnoreCase(s))
            {
                print("FALSE");
            }
            else
            {
                super.printDefaultValue(defaultValue, typeCode);
            }
        }
        else
        {
            super.printDefaultValue(defaultValue, typeCode);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void writeColumnAutoIncrementStmt(Table table, Column column) throws IOException
    {
        print("UNIQUE DEFAULT nextval('");
        print(getConstraintName(null, table, column.getName(), "seq"));
        print("')");
    }

    /**
     * {@inheritDoc}
     */
    public String getSelectLastIdentityValues(Table table)
    {
        Column[] columns = table.getAutoIncrementColumns();

        if (columns.length == 0)
        {
            return null;
        }
        else
        {
            StringBuffer result = new StringBuffer();
    
            result.append("SELECT ");
            for (int idx = 0; idx < columns.length; idx++)
            {
                if (idx > 0)
                {
                    result.append(", ");
                }
                result.append("currval('");
                result.append(getConstraintName(null, table, columns[idx].getName(), "seq"));
                result.append("') AS ");
                result.append(getDelimitedIdentifier(columns[idx].getName()));
            }
            return result.toString();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void processTableStructureChanges(Database currentModel,
                                                Database desiredModel,
                                                Table    sourceTable,
                                                Table    targetTable,
                                                Map      parameters,
                                                List     changes) throws IOException
    {
        // Drop primary keys first (PostgreSQL requires ALTER TABLE ... DROP CONSTRAINT).
        for (Iterator changeIt = changes.iterator(); changeIt.hasNext();)
        {
            TableChange change = (TableChange)changeIt.next();

            if (change instanceof RemovePrimaryKeyChange)
            {
                processChange(currentModel, desiredModel, (RemovePrimaryKeyChange)change);
                change.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
                changeIt.remove();
            }
            else if (change instanceof PrimaryKeyChange)
            {
                PrimaryKeyChange       pkChange       = (PrimaryKeyChange)change;
                RemovePrimaryKeyChange removePkChange = new RemovePrimaryKeyChange(pkChange.getChangedTable(),
                                                                                   pkChange.getOldPrimaryKeyColumns());

                processChange(currentModel, desiredModel, removePkChange);
                removePkChange.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
            }
        }

        // Add/remove columns and gather ColumnChange for ALTER COLUMN (same pattern as MSSqlBuilder).
        ArrayList columnChanges = new ArrayList();

        for (Iterator changeIt = changes.iterator(); changeIt.hasNext();)
        {
            TableChange change = (TableChange)changeIt.next();

            if (change instanceof AddColumnChange)
            {
                AddColumnChange addColumnChange = (AddColumnChange)change;

                // We can only use PostgreSQL-specific SQL if
                // * the column is not set to NOT NULL (the constraint would be applied immediately
                //   which will not work if there is already data in the table)
                // * the column has no default value (it would be applied after the change which
                //   means that PostgreSQL would behave differently from other databases where the
                //   default is applied to every column)
                // * the column is added at the end of the table (PostgreSQL does not support
                //   insertion of a column)
                if (!addColumnChange.getNewColumn().isRequired() &&
                    (addColumnChange.getNewColumn().getDefaultValue() == null) &&
                    (addColumnChange.getNextColumn() == null))
                {
                    processChange(currentModel, desiredModel, addColumnChange);
                    change.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
                    changeIt.remove();
                }
            }
            else if (change instanceof RemoveColumnChange)
            {
                processChange(currentModel, desiredModel, (RemoveColumnChange)change);
                change.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
                changeIt.remove();
            }
            else if (change instanceof ColumnAutoIncrementChange)
            {
                // Cannot add/remove legacy nextval identity with a simple ALTER; fall back to rebuild.
                columnChanges = null;
            }
            else if ((change instanceof ColumnChange) && (columnChanges != null))
            {
                columnChanges.add(change);
            }
        }

        if (columnChanges != null)
        {
            boolean caseSensitive = getPlatform().isDelimitedIdentifierModeOn();
            HashSet   seenColumns = new HashSet();

            for (Iterator chIt = columnChanges.iterator(); chIt.hasNext();)
            {
                ColumnChange change       = (ColumnChange)chIt.next();
                Column       refColumn    = change.getChangedColumn();
                String       colKey        = caseSensitive ? refColumn.getName() : refColumn.getName().toLowerCase();

                if (!seenColumns.contains(colKey))
                {
                    Column sourceCol =
                        sourceTable.findColumn(refColumn.getName(), caseSensitive);
                    Column targetCol =
                        targetTable.findColumn(refColumn.getName(), caseSensitive);
                    if (sourceCol != null && targetCol != null)
                    {
                        processColumnChange(sourceTable, targetTable, sourceCol, targetCol);
                    }
                    seenColumns.add(colKey);
                }
                changes.remove(change);
                change.apply(currentModel, caseSensitive);
            }
        }

        // Add primary keys last (PrimaryKeyChange from comparator becomes AddPrimaryKeyChange here).
        for (Iterator changeIt = changes.iterator(); changeIt.hasNext();)
        {
            TableChange change = (TableChange)changeIt.next();

            if (change instanceof AddPrimaryKeyChange)
            {
                processChange(currentModel, desiredModel, (AddPrimaryKeyChange)change);
                change.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
                changeIt.remove();
            }
            else if (change instanceof PrimaryKeyChange)
            {
                PrimaryKeyChange    pkChange    = (PrimaryKeyChange)change;
                AddPrimaryKeyChange addPkChange = new AddPrimaryKeyChange(pkChange.getChangedTable(),
                                                                          pkChange.getNewPrimaryKeyColumns());

                processChange(currentModel, desiredModel, addPkChange);
                addPkChange.apply(currentModel, getPlatform().isDelimitedIdentifierModeOn());
                changeIt.remove();
            }
        }
    }

    /**
     * Drops the primary key. Embedded {@code PRIMARY KEY} constraints created by PostgreSQL are named
     * {@code tablename_pkey}; DDL from {@link #writeExternalPrimaryKeysCreateStmt} uses
     * {@link #getConstraintName} with suffix {@code PK}. Both names are dropped with {@code IF EXISTS}.
     */
    protected void processChange(Database               currentModel,
                                 Database               desiredModel,
                                 RemovePrimaryKeyChange change) throws IOException
    {
        Table  table     = change.getChangedTable();
        String tableName = getTableName(table);
        int    maxLen    = getPlatformInfo().getMaxConstraintNameLength();
        String embeddedStylePk = shortenName(tableName + "_pkey", maxLen);
        String ddlUtilsStylePk = getConstraintName(null, table, "PK", null);

        print("ALTER TABLE ");
        printlnIdentifier(tableName);
        printIndent();
        print("DROP CONSTRAINT IF EXISTS ");
        printIdentifier(embeddedStylePk);
        printEndOfStatement();

        if (!embeddedStylePk.equals(ddlUtilsStylePk))
        {
            print("ALTER TABLE ");
            printlnIdentifier(tableName);
            printIndent();
            print("DROP CONSTRAINT IF EXISTS ");
            printIdentifier(ddlUtilsStylePk);
            printEndOfStatement();
        }
    }

    /**
     * Processes the addition of a column to a table.
     * 
     * @param currentModel The current database schema
     * @param desiredModel The desired database schema
     * @param change       The change object
     */
    protected void processChange(Database        currentModel,
                                 Database        desiredModel,
                                 AddColumnChange change) throws IOException
    {
        print("ALTER TABLE ");
        printlnIdentifier(getTableName(change.getChangedTable()));
        printIndent();
        print("ADD COLUMN ");
        writeColumn(change.getChangedTable(), change.getNewColumn());
        printEndOfStatement();
    }

    /**
     * Processes the removal of a column from a table.
     * 
     * @param currentModel The current database schema
     * @param desiredModel The desired database schema
     * @param change       The change object
     */
    protected void processChange(Database           currentModel,
                                 Database           desiredModel,
                                 RemoveColumnChange change) throws IOException
    {
        print("ALTER TABLE ");
        printlnIdentifier(getTableName(change.getChangedTable()));
        printIndent();
        print("DROP COLUMN ");
        printIdentifier(getColumnName(change.getColumn()));
        printEndOfStatement();
        if (change.getColumn().isAutoIncrement())
        {
            dropAutoIncrementSequence(change.getChangedTable(), change.getColumn());
        }
    }

    /**
     * Emits {@code ALTER COLUMN} for PostgreSQL to move from the current column definition to the target.
     */
    protected void processColumnChange(Table  sourceTable,
                                       Table  targetTable,
                                       Column sourceColumn,
                                       Column targetColumn) throws IOException
    {
        boolean caseSensitive = getPlatform().isDelimitedIdentifierModeOn();

        if (sourceColumn.getParsedDefaultValue() != null)
        {
            print("ALTER TABLE ");
            printlnIdentifier(getTableName(sourceTable));
            printIndent();
            print("ALTER COLUMN ");
            printIdentifier(getColumnName(sourceColumn));
            print(" DROP DEFAULT");
            printEndOfStatement();
        }

        boolean typeOrSizeDiffers =
            sourceColumn.getTypeCode() != targetColumn.getTypeCode() ||
            sourceColumn.getSizeAsInt() != targetColumn.getSizeAsInt() ||
            sourceColumn.getScale() != targetColumn.getScale();

        if (typeOrSizeDiffers)
        {
            print("ALTER TABLE ");
            printlnIdentifier(getTableName(sourceTable));
            printIndent();
            print("ALTER COLUMN ");
            printIdentifier(getColumnName(sourceColumn));
            print(" TYPE ");
            print(getSqlType(targetColumn));
            writePostgreSqlUsingCastIfNeeded(sourceColumn, targetColumn);
            printEndOfStatement();
        }

        if (sourceColumn.isRequired() != targetColumn.isRequired())
        {
            print("ALTER TABLE ");
            printlnIdentifier(getTableName(sourceTable));
            printIndent();
            print("ALTER COLUMN ");
            printIdentifier(getColumnName(sourceColumn));
            if (targetColumn.isRequired())
            {
                print(" SET NOT NULL");
            }
            else
            {
                print(" DROP NOT NULL");
            }
            printEndOfStatement();
        }

        if (targetColumn.getParsedDefaultValue() != null &&
            isValidDefaultValue(targetColumn.getDefaultValue(), targetColumn.getTypeCode()))
        {
            print("ALTER TABLE ");
            printlnIdentifier(getTableName(sourceTable));
            printIndent();
            print("ALTER COLUMN ");
            printIdentifier(getColumnName(sourceColumn));
            print(" SET DEFAULT ");
            writeColumnDefaultValue(sourceTable, targetColumn);
            printEndOfStatement();
        }
        else if (sourceColumn.getParsedDefaultValue() != null && targetColumn.getParsedDefaultValue() == null)
        {
            // default already dropped above if source had one; if only removal was needed, covered by DROP DEFAULT
        }
    }

    /**
     * Appends {@code USING (...)} when PostgreSQL cannot cast implicitly (e.g. timestamp to date).
     */
    protected void writePostgreSqlUsingCastIfNeeded(Column sourceColumn, Column targetColumn) throws IOException
    {
        int src = sourceColumn.getTypeCode();
        int tgt = targetColumn.getTypeCode();

        if (src == Types.TIMESTAMP && tgt == Types.DATE)
        {
            print(" USING (");
            printIdentifier(getColumnName(sourceColumn));
            print("::date)");
            return;
        }
        if (src == Types.TIMESTAMP && tgt == Types.TIME)
        {
            print(" USING (");
            printIdentifier(getColumnName(sourceColumn));
            print("::time)");
            return;
        }
        if (src == Types.DATE && (tgt == Types.TIMESTAMP || tgt == Types.TIME))
        {
            print(" USING (");
            printIdentifier(getColumnName(sourceColumn));
            print("::timestamp)");
        }
    }
}
