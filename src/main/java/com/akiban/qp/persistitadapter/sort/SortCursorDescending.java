/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.qp.persistitadapter.sort;

import com.akiban.qp.persistitadapter.PersistitAdapterException;
import com.akiban.qp.physicaloperator.Bindings;
import com.akiban.qp.row.Row;
import com.persistit.Key;
import com.persistit.exception.PersistitException;

class SortCursorDescending extends SortCursor
{
    // Cursor interface

    @Override
    public void open(Bindings bindings)
    {
        exchange.clear();
        exchange.append(Key.AFTER);
    }

    @Override
    public Row next()
    {
        Row next = null;
        try {
            if (exchange.previous(true)) {
                next = row();
            } else {
                close();
            }
        } catch (PersistitException e) {
            close();
            throw new PersistitAdapterException(e);
        }
        return next;
    }

    // SortCursorAscending interface

    public SortCursorDescending(Sorter sorter)
    {
        super(sorter);
    }
}
