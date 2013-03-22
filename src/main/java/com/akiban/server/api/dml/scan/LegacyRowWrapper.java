
package com.akiban.server.api.dml.scan;

import com.akiban.server.rowdata.RowData;
import com.akiban.server.rowdata.RowDataExtractor;
import com.akiban.server.rowdata.RowDef;
import com.akiban.server.api.dml.ColumnSelector;
import com.akiban.server.api.dml.DMLError;
import com.akiban.server.service.session.Session;
import com.akiban.server.store.Store;

import java.util.Map;

public final class LegacyRowWrapper extends NewRow
{
    // Updates are handled by creating a NiceRow from the RowData and applying updates to the NiceRow.
    // When this happens, rowData is set to null, and is regenerated lazily. If newRow is null, then
    // rowData is current.
    private RowData rowData;
    private NiceRow niceRow;

    // For when a LegacyRowWrapper is being reused, e.g. WriteRowRequest.
    public LegacyRowWrapper()
    {
        this(null, null);
    }

    public LegacyRowWrapper(RowDef rowDef)
    {
        this(rowDef, null);
    }

    public LegacyRowWrapper(RowDef rowDef, RowData rowData)
    {
        super(rowDef);
        setRowData(rowData);
    }

    public void setRowData(RowData rowData)
    {
        assert
            rowData == null || rowDef == null || rowData.getRowDefId() == rowDef.getRowDefId()
            : String.format("rowData: %s, rowDef: %s", rowData, rowDef);
        this.rowData = rowData;
        this.niceRow = null;
        this.extractor = null;
    }

    @Override
    public Object put(int index, Object object)
    {
        return niceRow().put(index, object);
    }

    @Override
    public int getTableId()
    {
        assert rowData != null || niceRow != null;
        return niceRow != null ? niceRow.getTableId() : rowData.getRowDefId();
    }

    @Override
    public Object get(int columnId)
    {
        Object object;
        if (rowData == null && niceRow == null) {
            throw new DMLError("Row state has not been set");
        } else {
            object =
                niceRow != null
                ? niceRow.get(columnId)
                : extractor().get(rowDef.getFieldDef(columnId));
        }
        return object;
    }

    @Override
    public boolean hasValue(int columnId)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(int columnId)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Integer, Object> getFields()
    {
        return niceRow().getFields();
    }

    @Override
    public RowData toRowData()
    {
        if (rowData == null && niceRow != null) {
            rowData = niceRow.toRowData();
            extractor = null;
            niceRow = null;
        }
        return rowData;
    }

    @Override
    public ColumnSelector getActiveColumns() {
        return niceRow().getActiveColumns();
    }

    @Override
    public boolean isColumnNull(int columnId) {
        return toRowData().isNull(columnId);
    }

    @Override
    public boolean equals(Object o)
    {
        boolean eq;
        if (o == null) {
            eq = false;
        } else if (this == o) {
            eq = true;
        } else if (o instanceof LegacyRowWrapper) {
            LegacyRowWrapper that = (LegacyRowWrapper) o;
            eq = this.niceRow().equals(that.niceRow());
        } else if (o instanceof NiceRow) {
            NiceRow that = (NiceRow) o;
            eq = this.niceRow().equals(that);
        } else {
            eq = false;
        }
        return eq;
    }

    @Override
    public int hashCode()
    {
        return niceRow().hashCode();
    }

    @Override
    public String toString()
    {
        // newRow().toString() would be simpler, but there's a side-effect: If rowData != null, then
        // the rowData is converted to a NiceRow and rowData is set to null.
        return
            rowData == null && niceRow == null ? "null" :
            rowData == null ? niceRow.toString() :
            rowData.toString(rowDef);
    }

    public NiceRow niceRow()
    {
        if (niceRow == null) {
            niceRow = (NiceRow) NiceRow.fromRowData(rowData, rowDef);
            rowData = null;
            extractor = null;
        }
        return niceRow;
    }

    // Allows a LegacyRowWrapper to be used for a RowData acting as a container of rows of any type.
    public void setRowDef(RowDef rowDef)
    {
        this.rowDef = rowDef;
        niceRow = null;
    }

    private RowDataExtractor extractor()
    {
        if (extractor == null) {
            extractor = new RowDataExtractor(rowData, rowDef);
        }
        return extractor;
    }

    private RowDataExtractor extractor;
}
