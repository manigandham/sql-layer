
package com.akiban.server.test.it.keyupdate;

import com.akiban.ais.model.Group;
import com.akiban.server.rowdata.RowDef;

public class Schema
{
    // For all KeyUpdate*IT
    static Integer vendorId;
    static RowDef vendorRD;
    static Integer customerId;
    static RowDef customerRD;
    static Integer orderId;
    static RowDef orderRD;
    static Integer itemId;
    static RowDef itemRD;
    static Group group;
    // For KeyUpdateIT and KeyUpdateCascadingKeysIT
    static Integer v_vid;
    static Integer v_vx;
    static Integer c_cid;
    static Integer c_vid;
    static Integer c_cx;
    static Integer o_oid;
    static Integer o_cid;
    static Integer o_vid;
    static Integer o_ox;
    static Integer o_priority;
    static Integer o_when;
    static Integer i_vid;
    static Integer i_cid;
    static Integer i_oid;
    static Integer i_iid;
    static Integer i_ix;
    // For MultiColumnKeyUpdateIT and MultiColumnKeyUpdateCascadingKeysIT
    static Integer v_vid1;
    static Integer v_vid2;
    static Integer c_vid1;
    static Integer c_vid2;
    static Integer c_cid1;
    static Integer c_cid2;
    static Integer o_vid1;
    static Integer o_vid2;
    static Integer o_cid1;
    static Integer o_cid2;
    static Integer o_oid1;
    static Integer o_oid2;
    static Integer i_vid1;
    static Integer i_vid2;
    static Integer i_cid1;
    static Integer i_cid2;
    static Integer i_oid1;
    static Integer i_oid2;
    static Integer i_iid1;
    static Integer i_iid2;
}