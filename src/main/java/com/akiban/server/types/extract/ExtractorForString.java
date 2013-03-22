
package com.akiban.server.types.extract;

import com.akiban.server.types.AkType;
import com.akiban.server.types.ValueSource;
import com.akiban.server.types.conversion.Converters;
import com.akiban.util.ByteSource;
import java.io.UnsupportedEncodingException;

final class ExtractorForString extends ObjectExtractor<String> {
    @Override
    public String getObject(ValueSource source) {
        AkType type = source.getConversionType();
        switch (type) {
        case BOOL:      return Boolean.toString(source.getBool());
        case TEXT:      return source.getText();
        case NULL:      return "null";
        case VARCHAR:   return source.getString();
        case DOUBLE:    return Double.toString(source.getDouble());
        case FLOAT:     return Float.toString(source.getFloat());
        case INT:       return Long.toString(source.getInt());
        case LONG:      return Long.toString(source.getLong());
        case U_INT:     return Long.toString(source.getUInt());
        case U_DOUBLE:  return Double.toString(source.getUDouble());
        case U_FLOAT:   return Float.toString(source.getUFloat());
        case U_BIGINT:  return String.valueOf(source.getUBigInt());
        case TIME:      return longExtractor(AkType.TIME).asString(source.getTime());
        case TIMESTAMP: return longExtractor(AkType.TIMESTAMP).asString(source.getTimestamp());
        case YEAR:      return longExtractor(AkType.YEAR).asString(source.getYear());
        case DATE:      return longExtractor(AkType.DATE).asString(source.getDate());
        case DATETIME:  return longExtractor(AkType.DATETIME).asString(source.getDateTime());
        case DECIMAL:   return String.valueOf(source.getDecimal());
        case VARBINARY:
            try
            {
                ByteSource byteSource = source.getVarBinary();
                return new String(byteSource.byteArray(), 
                        byteSource.byteArrayOffset(), 
                        byteSource.byteArrayLength(), 
                        Converters.DEFAULT_CS);
            } 
            catch (UnsupportedEncodingException ex)
            {
                throw new UnsupportedOperationException(ex);
            }
        case INTERVAL_MILLIS:  return longExtractor(AkType.INTERVAL_MILLIS).asString(source.getInterval_Millis());
        case INTERVAL_MONTH:   return longExtractor(AkType.INTERVAL_MONTH).asString(source.getInterval_Month());
        default:
            throw unsupportedConversion(type);
        }
    }

    @Override
    public String getObject(String string) {
        return string;
    }

    private static LongExtractor longExtractor(AkType forType) {
        // we could also cache these, since they're static
        return Extractors.getLongExtractor(forType);
    }

    ExtractorForString() {
        super(AkType.VARCHAR);
    }
}
