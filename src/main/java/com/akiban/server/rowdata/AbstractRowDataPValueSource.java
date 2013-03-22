
package com.akiban.server.rowdata;

import com.akiban.server.AkServerUtil;
import com.akiban.server.types.*;
import com.akiban.server.types3.TClass;
import com.akiban.server.types3.TInstance;
import com.akiban.server.types3.common.types.TString;
import com.akiban.server.types3.mcompat.mtypes.MDatetimes;
import com.akiban.server.types3.mcompat.mtypes.MNumeric;
import com.akiban.server.types3.pvalue.PValueSource;


abstract class AbstractRowDataPValueSource implements PValueSource {

    // ValueSource interface

    @Override
    public TInstance tInstance() {
        return fieldDef().column().tInstance();
    }

    @Override
    public boolean hasAnyValue() {
        return true;
    }

    @Override
    public boolean hasRawValue() {
        return ! hasCacheValue();
    }

    @Override
    public boolean hasCacheValue() {
        return fieldDef().column().tInstance().typeClass() instanceof TString;
    }

    @Override
    public boolean canGetRawValue() {
        return true;
    }

    @Override
    public abstract boolean isNull();

    @Override
    public boolean getBoolean() {
        return extractLong(signage()) != 0;
    }

    @Override
    public boolean getBoolean(boolean defaultValue) {
        return isNull() ? defaultValue : getBoolean();
    }

    @Override
    public byte getInt8() {
        return (byte) extractLong(signage());
    }

    @Override
    public short getInt16() {
        return (short) extractLong(signage());
    }

    @Override
    public char getUInt16() {
        return (char) extractLong(signage());
    }

    @Override
    public int getInt32() {
        return (int) extractLong(signage());
    }

    @Override
    public long getInt64() {
        return extractLong(signage());
    }

    @Override
    public float getFloat() {
        return doGetFloat();
    }

    @Override
    public double getDouble() {
        return doGetDouble();
    }

    @Override
    public byte[] getBytes() {

        long offsetAndWidth = getRawOffsetAndWidth();
        if (offsetAndWidth == 0) {
            return null;
        }
        int offset = (int) offsetAndWidth + fieldDef().getPrefixSize();
        int size = (int) (offsetAndWidth >>> 32) - fieldDef().getPrefixSize();
        byte[] bytes = new byte[size];
        System.arraycopy(bytes(), offset, bytes, 0, size);
        return bytes;
    }

    @Override
    public String getString() {
        final long location = getRawOffsetAndWidth();
        return location == 0
                ? null
                : AkServerUtil.decodeMySQLString(bytes(), (int) location, (int) (location >>> 32), fieldDef());
    }

    @Override
    public Object getObject() {
        assert hasCacheValue() : "can't get cached object for " + fieldDef();
        final long location = getRawOffsetAndWidth();
        return location == 0
                ? null
                : AkServerUtil.byteSourceForMySQLString(bytes(), (int) location, (int) (location >>> 32), fieldDef());
    }


    // for subclasses
    protected abstract long getRawOffsetAndWidth();
    protected abstract byte[] bytes();
    protected abstract FieldDef fieldDef();

    // for use within this class
    private double doGetDouble() {
        long asLong = extractLong(Signage.SIGNED);
        return Double.longBitsToDouble(asLong);
    }

    private float doGetFloat() {
        long asLong = extractLong(Signage.SIGNED);
        int asInt = (int) asLong;
        return Float.intBitsToFloat(asInt);
    }

    private long extractLong(Signage signage) {
        long offsetAndWidth = getCheckedOffsetAndWidth();
        final int offset = (int)offsetAndWidth;
        final int width = (int)(offsetAndWidth >>> 32);
        if ((signage == Signage.SIGNED) || (width == 8)) {
            return AkServerUtil.getSignedIntegerByWidth(bytes(), offset, width);
        } else {
            assert signage == Signage.UNSIGNED;
            return AkServerUtil.getUnsignedIntegerByWidth(bytes(), offset, width);
        }
    }

    private Signage signage() {
        TClass tclass = fieldDef().column().tInstance().typeClass();
        if (tclass instanceof MNumeric)
            return ((MNumeric)tclass).isUnsigned() ? Signage.UNSIGNED : Signage.SIGNED;
        else if (tclass == MDatetimes.YEAR)
            return Signage.UNSIGNED;
        else
            return Signage.SIGNED;
    }

    private long getCheckedOffsetAndWidth() {
        long offsetAndWidth = getRawOffsetAndWidth();
        if (offsetAndWidth == 0) {
            throw new ValueSourceIsNullException();
        }
        return offsetAndWidth;
    }

    // object state

    private enum Signage {
        SIGNED, UNSIGNED
    }
}
