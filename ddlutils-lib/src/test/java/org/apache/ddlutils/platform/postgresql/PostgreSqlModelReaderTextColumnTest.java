package org.apache.ddlutils.platform.postgresql;

/*
 * Copyright 1999-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.apache.ddlutils.model.Column;
import org.junit.Test;

/**
 * Ensures JDBC metadata for PostgreSQL {@code text} maps to {@link Types#LONGVARCHAR} in the model.
 */
public class PostgreSqlModelReaderTextColumnTest
{
    private static Map<String, Object> baseColumnMeta()
    {
        HashMap<String, Object> values = new HashMap<String, Object>();
        values.put("COLUMN_DEF", null);
        values.put("TABLE_NAME", "t");
        values.put("COLUMN_NAME", "c");
        values.put("DATA_TYPE", Integer.valueOf(Types.VARCHAR));
        values.put("NUM_PREC_RADIX", Integer.valueOf(10));
        values.put("DECIMAL_DIGITS", Integer.valueOf(0));
        values.put("IS_NULLABLE", "YES");
        values.put("REMARKS", null);
        return values;
    }

    @Test
    public void typeNameTextWithLargeVarcharSizeBecomesLongVarchar() throws SQLException
    {
        Map<String, Object> values = baseColumnMeta();
        values.put("COLUMN_SIZE", "1073741824");
        values.put("TYPE_NAME", "text");

        Column column = new TestReader().readColumnForTest(values);

        assertEquals(Types.LONGVARCHAR, column.getTypeCode());
        assertNull(column.getSize());
    }

    @Test
    public void varcharWithMaxIntSizeWithoutTypeNameBecomesLongVarchar() throws SQLException
    {
        Map<String, Object> values = baseColumnMeta();
        values.put("COLUMN_SIZE", String.valueOf(Integer.MAX_VALUE));
        values.put("TYPE_NAME", null);

        Column column = new TestReader().readColumnForTest(values);

        assertEquals(Types.LONGVARCHAR, column.getTypeCode());
        assertNull(column.getSize());
    }

    /** Exposes {@link PostgreSqlModelReader#readColumn} for tests. */
    private static final class TestReader extends PostgreSqlModelReader
    {
        TestReader()
        {
            super(new PostgreSqlPlatform());
        }

        Column readColumnForTest(Map<String, Object> values) throws SQLException
        {
            return readColumn(null, values);
        }
    }
}
