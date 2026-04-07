package org.apache.ddlutils.alteration;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ddlutils.PlatformInfo;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.ForeignKey;
import org.apache.ddlutils.model.Index;
import org.apache.ddlutils.model.Table;

/**
 * Compares two database models and creates change objects that express how to
 * adapt the first model so that it becomes the second one. Neither of the models
 * are changed in the process, however, it is also assumed that the models do not
 * change in between.
 * 
 * TODO: Add support and tests for the change of the column order 
 * 
 * @version $Revision: $
 */
public class ModelComparator
{
    /** The log for this comparator. */
    private final Log _log = LogFactory.getLog(ModelComparator.class);

    /** The platform information. */
    private PlatformInfo _platformInfo;
    /** Whether comparison is case sensitive. */
    private boolean _caseSensitive;

    /**
     * Creates a new model comparator object.
     * 
     * @param platformInfo  The platform info
     * @param caseSensitive Whether comparison is case sensitive
     */
    public ModelComparator(PlatformInfo platformInfo, boolean caseSensitive)
    {
        _platformInfo  = platformInfo;
        _caseSensitive = caseSensitive;
    }

    @Deprecated
    public List compare(Database sourceModel, Database targetModel)
    {
        return this.compare(sourceModel, targetModel, false, Collections.emptySet());
    }
    /**
     * Compares the two models and returns the changes necessary to create the second
     * model from the first one.
     *  
     * @param sourceModel The source model
     * @param targetModel The target model
     * @return The changes
     */
    public List compare(Database sourceModel, Database targetModel, boolean borrarTablas, Set elementsPrefixIgnore)
    {
        ArrayList changes = new ArrayList();

        for (int tableIdx = 0; tableIdx < targetModel.getTableCount(); tableIdx++)
        {
            Table targetTable = targetModel.getTable(tableIdx);
            Table sourceTable = sourceModel.findTable(targetTable.getName(), _caseSensitive);

            if (sourceTable == null)
            {
                if (_log.isInfoEnabled())
                {
                    _log.info("Table " + targetTable.getName() + " needs to be added");
                }
                changes.add(new AddTableChange(targetTable));
                for (int fkIdx = 0; fkIdx < targetTable.getForeignKeyCount(); fkIdx++)
                {
                    // we have to use target table's definition here because the
                    // complete table is new
                    changes.add(new AddForeignKeyChange(targetTable, targetTable.getForeignKey(fkIdx)));
                }
            }
            else
            {
                changes.addAll(compareTables(sourceModel, sourceTable, targetModel, targetTable));
            }
        }
        
        //opcion de conservar tablas
        if(borrarTablas)
        {
            for (int tableIdx = 0; tableIdx < sourceModel.getTableCount(); tableIdx++)
            {
                Table sourceTable = sourceModel.getTable(tableIdx);
                Table targetTable = targetModel.findTable(sourceTable.getName(), _caseSensitive);
    
                if ((targetTable == null) && (sourceTable.getName() != null) && (sourceTable.getName().length() > 0))
                {
                    Iterator it = elementsPrefixIgnore.iterator();
                    boolean borrarTablaConcreta = true;
                    while(it.hasNext() && borrarTablaConcreta)
                    {
                        String prefijo = ObjectUtils.toString(it.next(),StringUtils.EMPTY);
                        String nombreTabla = sourceTable.getName();
                        if(!_caseSensitive)
                        {
                            prefijo = prefijo.toUpperCase();
                            nombreTabla = nombreTabla.toUpperCase();
                        }
                        if(nombreTabla.startsWith(prefijo))
                            borrarTablaConcreta = false;
                    }
                    //Si al final se decide borrar esta tabla concreta
                    //es decir, si la opcion de general está marcada y además no hay 
                    //ningún prefijo que lo impida, se borra
                    if(borrarTablaConcreta)
                    {
                        if (_log.isInfoEnabled())
                        {
                            _log.info("Table " + sourceTable.getName() + " needs to be removed");
                        }
                        changes.add(new RemoveTableChange(sourceTable));
                        // we assume that the target model is sound, ie. that there are no longer any foreign
                        // keys to this table in the target model; thus we already have removeFK changes for
                        // these from the compareTables method and we only need to create changes for the fks
                        // originating from this table
                        for (int fkIdx = 0; fkIdx < sourceTable.getForeignKeyCount(); fkIdx++)
                        {
                            changes.add(new RemoveForeignKeyChange(sourceTable, sourceTable.getForeignKey(fkIdx)));
                        }
                    }
                }
            }
        }
        return changes;
    }

    /**
     * Compares the two tables and returns the changes necessary to create the second
     * table from the first one.
     *  
     * @param sourceModel The source model which contains the source table
     * @param sourceTable The source table
     * @param targetModel The target model which contains the target table
     * @param targetTable The target table
     * @return The changes
     */
    public List compareTables(Database sourceModel,
                              Table    sourceTable,
                              Database targetModel,
                              Table    targetTable)
    {
        ArrayList changes = new ArrayList();

        for (int fkIdx = 0; fkIdx < sourceTable.getForeignKeyCount(); fkIdx++)
        {
            ForeignKey sourceFk = sourceTable.getForeignKey(fkIdx);
            ForeignKey targetFk = findCorrespondingForeignKey(targetTable, sourceFk);

            if (targetFk == null)
            {
                if (_log.isInfoEnabled())
                {
                    _log.info("Foreign key " + sourceFk + " needs to be removed from table " + sourceTable.getName());
                }
                changes.add(new RemoveForeignKeyChange(sourceTable, sourceFk));
            }
        }

        for (int fkIdx = 0; fkIdx < targetTable.getForeignKeyCount(); fkIdx++)
        {
            ForeignKey targetFk = targetTable.getForeignKey(fkIdx);
            ForeignKey sourceFk = findCorrespondingForeignKey(sourceTable, targetFk);

            if (sourceFk == null)
            {
                if (_log.isInfoEnabled())
                {
                    _log.info("Foreign key " + targetFk + " needs to be created for table " + sourceTable.getName());
                }
                // we have to use the target table here because the foreign key might
                // reference a new column
                changes.add(new AddForeignKeyChange(targetTable, targetFk));
            }
        }

        for (int indexIdx = 0; indexIdx < sourceTable.getIndexCount(); indexIdx++)
        {
            Index sourceIndex = sourceTable.getIndex(indexIdx);
            Index targetIndex = findCorrespondingIndex(targetTable, sourceIndex);

            if (targetIndex == null)
            {
                if (_log.isInfoEnabled())
                {
                    _log.info("Index " + sourceIndex.getName() + " needs to be removed from table " + sourceTable.getName());
                }
                changes.add(new RemoveIndexChange(sourceTable, sourceIndex));
            }
        }
        for (int indexIdx = 0; indexIdx < targetTable.getIndexCount(); indexIdx++)
        {
            Index targetIndex = targetTable.getIndex(indexIdx);
            Index sourceIndex = findCorrespondingIndex(sourceTable, targetIndex);

            if (sourceIndex == null)
            {
                if (_log.isInfoEnabled())
                {
                    _log.info("Index " + targetIndex.getName() + " needs to be created for table " + sourceTable.getName());
                }
                // we have to use the target table here because the index might
                // reference a new column
                changes.add(new AddIndexChange(targetTable, targetIndex));
            }
        }

        HashMap addColumnChanges = new HashMap();

        for (int columnIdx = 0; columnIdx < targetTable.getColumnCount(); columnIdx++)
        {
            Column targetColumn = targetTable.getColumn(columnIdx);
            Column sourceColumn = sourceTable.findColumn(targetColumn.getName(), _caseSensitive);

            if (sourceColumn == null)
            {
                if (_log.isInfoEnabled())
                {
                    _log.info("Column " + targetColumn.getName() + " needs to be created for table " + sourceTable.getName());
                }

                AddColumnChange change = new AddColumnChange(sourceTable,
                                                             targetColumn,
                                                             columnIdx > 0 ? targetTable.getColumn(columnIdx - 1) : null,
                                                             columnIdx < targetTable.getColumnCount() - 1 ? targetTable.getColumn(columnIdx + 1) : null);

                changes.add(change);
                addColumnChanges.put(targetColumn, change);
            }
            else
            {
                changes.addAll(compareColumns(sourceTable, sourceColumn, targetTable, targetColumn));
            }
        }
        // if the last columns in the target table are added, then we note this at the changes
        for (int columnIdx = targetTable.getColumnCount() - 1; columnIdx >= 0; columnIdx--)
        {
            Column          targetColumn = targetTable.getColumn(columnIdx);
            AddColumnChange change       = (AddColumnChange)addColumnChanges.get(targetColumn);

            if (change == null)
            {
                // column was not added, so we can ignore any columns before it that were added
                break;
            }
            else
            {
                change.setAtEnd(true);
            }
        }

        Column[] sourcePK = sourceTable.getPrimaryKeyColumns();
        Column[] targetPK = targetTable.getPrimaryKeyColumns();

        if ((sourcePK.length == 0) && (targetPK.length > 0))
        {
            if (_log.isInfoEnabled())
            {
                _log.info("A primary key needs to be added to the table " + sourceTable.getName());
            }
            // we have to use the target table here because the primary key might
            // reference a new column
            changes.add(new AddPrimaryKeyChange(targetTable, targetPK));
        }
        else if ((targetPK.length == 0) && (sourcePK.length > 0))
        {
            if (_log.isInfoEnabled())
            {
                _log.info("The primary key needs to be removed from the table " + sourceTable.getName());
            }
            changes.add(new RemovePrimaryKeyChange(sourceTable, sourcePK));
        }
        else if ((sourcePK.length > 0) && (targetPK.length > 0))
        {
            boolean changePK = false;

            if (sourcePK.length != targetPK.length)
            {
                changePK = true;
            }
            else
            {
                //Si son iguales hay que ver que sean iguales sin importar el orden
                changePK = false;
                int contador=0;
                for (int pkColumnIdx = 0; (pkColumnIdx < sourcePK.length); pkColumnIdx++)
                {
                    for (int pkColumn2Idx = 0; (pkColumn2Idx < targetPK.length); pkColumn2Idx++)
                    {
                        if ((_caseSensitive  && sourcePK[pkColumnIdx].getName().equals(targetPK[pkColumn2Idx].getName())) ||
                                (!_caseSensitive && sourcePK[pkColumnIdx].getName().equalsIgnoreCase(targetPK[pkColumn2Idx].getName())))
                            {
                                contador++;
                            }
                    }
                }
                if(contador!=targetPK.length)
                    changePK=true;
            }
            if (changePK)
            {
                if (_log.isInfoEnabled())
                {
                    _log.info("The primary key of table " + sourceTable.getName() + " needs to be changed");
                }
                changes.add(new PrimaryKeyChange(sourceTable, sourcePK, targetPK));
            }
        }
        
        HashMap columnPosChanges = new HashMap();

        for (int columnIdx = 0; columnIdx < sourceTable.getColumnCount(); columnIdx++)
        {
            Column sourceColumn = sourceTable.getColumn(columnIdx);
            Column targetColumn = targetTable.findColumn(sourceColumn.getName(), _caseSensitive);

            if (targetColumn == null)
            {
                if (_log.isInfoEnabled())
                {
                    _log.info("Column " + sourceColumn.getName() + " needs to be removed from table " + sourceTable.getName());
                }
                changes.add(new RemoveColumnChange(sourceTable, sourceColumn));
            }
            else
            {
                int targetColumnIdx = targetTable.getColumnIndex(targetColumn);

                if (targetColumnIdx != columnIdx)
                {
                    columnPosChanges.put(sourceColumn, new Integer(targetColumnIdx));
                }
            }
        }
        if (!columnPosChanges.isEmpty())
        {
            changes.add(new ColumnOrderChange(sourceTable, columnPosChanges));
        }

        return changes;
    }

    /**
     * Compares the two columns and returns the changes necessary to create the second
     * column from the first one.
     *  
     * @param sourceTable  The source table which contains the source column
     * @param sourceColumn The source column
     * @param targetTable  The target table which contains the target column
     * @param targetColumn The target column
     * @return The changes
     */
    public List compareColumns(Table  sourceTable,
                               Column sourceColumn,
                               Table  targetTable,
                               Column targetColumn)
    {
        ArrayList changes = new ArrayList();

        if (_platformInfo.getTargetJdbcType(targetColumn.getTypeCode()) != sourceColumn.getTypeCode())
        {
            changes.add(new ColumnDataTypeChange(sourceTable, sourceColumn, targetColumn.getTypeCode()));
        }

        boolean sizeMatters = _platformInfo.hasSize(sourceColumn.getTypeCode());

        if ((sizeMatters &&
            (!StringUtils.equals(sourceColumn.getSize(), targetColumn.getSize())) ||
             sourceColumn.getScale() != targetColumn.getScale()))
        {
            changes.add(new ColumnSizeChange(sourceTable, sourceColumn, targetColumn.getSizeAsInt(), targetColumn.getScale()));
        }

        Object sourceDefaultValue = sourceColumn.getParsedDefaultValue();
        Object targetDefaultValue = targetColumn.getParsedDefaultValue();

        if (((sourceDefaultValue == null) && (targetDefaultValue != null)) ||
            ((sourceDefaultValue != null) && !sourceDefaultValue.equals(targetDefaultValue)))
        {
            changes.add(new ColumnDefaultValueChange(sourceTable, sourceColumn, targetColumn.getDefaultValue()));
        }

        if (sourceColumn.isRequired() != targetColumn.isRequired())
        {
            changes.add(new ColumnRequiredChange(sourceTable, sourceColumn));
        }
        if (sourceColumn.isAutoIncrement() != targetColumn.isAutoIncrement())
        {
            changes.add(new ColumnAutoIncrementChange(sourceTable, sourceColumn));
        }

        return changes;
    }

    /**
     * Searches in the given table for a corresponding foreign key. If the given key
     * has no name, then a foreign key to the same table with the same columns (but not
     * necessarily in the same order) is searched. If the given key has a name, then the
     * corresponding key also needs to have the same name, or no name at all, but not a
     * different one. 
     * 
     * @param table The table to search in
     * @param fk    The original foreign key
     * @return The corresponding foreign key if found
     */
    private ForeignKey findCorrespondingForeignKey(Table table, ForeignKey fk)
    {
        for (int fkIdx = 0; fkIdx < table.getForeignKeyCount(); fkIdx++)
        {
            ForeignKey curFk = table.getForeignKey(fkIdx);

            if ((_caseSensitive  && fk.equals(curFk)) ||
                (!_caseSensitive && fk.equalsIgnoreCase(curFk)))
            {
                return curFk;
            }
        }
        return null;
    }

    
    /**
     * Returns the name to be used for the given foreign key. If the foreign key has no
     * specified name, this method determines a unique name for it. The name will also
     * be shortened to honor the maximum identifier length imposed by the platform.
     * 
     * @param table The table for whith the foreign key is defined
     * @param fk    The foreign key
     * @return The name
     */
    public String getForeignKeyName(Table table, ForeignKey fk)
    {
        String  fkName    = fk.getName();
        boolean needsName = (fkName == null) || (fkName.length() == 0);

        if (needsName)
        {
            StringBuffer name = new StringBuffer();
    
            for (int idx = 0; idx < fk.getReferenceCount(); idx++)
            {
                name.append(fk.getReference(idx).getLocalColumnName());
                name.append("_");
            }
            name.append(fk.getForeignTableName());
            fkName = getConstraintName(null, table, "FK", name.toString());
        }
        fkName = shortenName(fkName, -1);

        if (needsName)
        {
            _log.warn("Encountered a foreign key in table " + table.getName() + " that has no name. " +
                      "DdlUtils will use the auto-generated and shortened name " + fkName + " instead.");
        }

        return fkName;
    }
    
    /**
     * Generates a version of the name that has at most the specified
     * length.
     * 
     * @param name          The original name
     * @param desiredLength The desired maximum length
     * @return The shortened version
     */
    protected String shortenName(String name, int desiredLength)
    {
        // TODO: Find an algorithm that generates unique names
        int originalLength = name.length();

        if ((desiredLength <= 0) || (originalLength <= desiredLength))
        {
            return name;
        }

        int delta    = originalLength - desiredLength;
        int startCut = desiredLength / 2;

        StringBuffer result = new StringBuffer();

        result.append(name.substring(0, startCut));
        if (((startCut == 0) || (name.charAt(startCut - 1) != '_')) &&
            ((startCut + delta + 1 == originalLength) || (name.charAt(startCut + delta + 1) != '_')))
        {
            // just to make sure that there isn't already a '_' right before or right
            // after the cutting place (which would look odd with an aditional one)
            result.append("_");
        }
        result.append(name.substring(startCut + delta + 1, originalLength));
        return result.toString();
    }
    
    /**
     * Returns the constraint name. This method takes care of length limitations imposed by some databases.
     * 
     * @param prefix     The constraint prefix, can be <code>null</code>
     * @param table      The table that the constraint belongs to
     * @param secondPart The second name part, e.g. the name of the constraint column
     * @param suffix     The constraint suffix, e.g. a counter (can be <code>null</code>)
     * @return The constraint name
     */
    public String getConstraintName(String prefix, Table table, String secondPart, String suffix)
    {
        StringBuffer result = new StringBuffer();
        
        if (prefix != null)
        {
            result.append(prefix);
            result.append("_");
        }
        result.append(table.getName());
        result.append("_");
        result.append(secondPart);
        if (suffix != null)
        {
            result.append("_");
            result.append(suffix);
        }
        return shortenName(result.toString(), -1);
    }
    
    /**
     * Searches in the given table for a corresponding index. If the given index
     * has no name, then a index to the same table with the same columns in the
     * same order is searched. If the given index has a name, then the a corresponding
     * index also needs to have the same name, or no name at all, but not a different one. 
     * 
     * @param table The table to search in
     * @param index The original index
     * @return The corresponding index if found
     */
    private Index findCorrespondingIndex(Table table, Index index)
    {
        for (int indexIdx = 0; indexIdx < table.getIndexCount(); indexIdx++)
        {
            Index curIndex = table.getIndex(indexIdx);

            if ((_caseSensitive  && index.equals(curIndex)) ||
                (!_caseSensitive && index.equalsIgnoreCase(curIndex)))
            {
                return curIndex;
            }
        }
        return null;
    }
}
