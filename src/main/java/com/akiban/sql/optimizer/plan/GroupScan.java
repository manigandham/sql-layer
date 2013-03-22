
package com.akiban.sql.optimizer.plan;

import java.util.List;

public class GroupScan extends BaseScan implements TableLoader
{
    private TableGroup group;
    private List<TableSource> tables;

    public GroupScan(TableGroup group) {
        this.group = group;
    }

    public TableGroup getGroup() {
        return group;
    }

    /** The tables that this branch lookup introduces into the stream. */
    public List<TableSource> getTables() {
        return tables;
    }

    public void setTables(List<TableSource> tables) {
        this.tables = tables;
    }

    @Override
    public boolean accept(PlanVisitor v) {
        if (v.visitEnter(this)) {
            if (tables != null) {
                for (TableSource table : tables) {
                    if (!table.accept(v))
                        break;
                }
            }
        }
        return v.visitLeave(this);
    }

    @Override
    protected void deepCopy(DuplicateMap map) {
        super.deepCopy(map);
        group = (TableGroup)group.duplicate();
        tables = duplicateList(tables, map);
    }

    @Override
    public String summaryString() {
        StringBuilder str = new StringBuilder(super.summaryString());
        str.append('(');
        str.append(group.getGroup());
        if (getCostEstimate() != null) {
            str.append(", ");
            str.append(getCostEstimate());
        }
        str.append(")");
        return str.toString();
    }

}
