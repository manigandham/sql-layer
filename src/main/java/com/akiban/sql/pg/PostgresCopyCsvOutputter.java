
package com.akiban.sql.pg;

import com.akiban.sql.server.ServerValueEncoder;

import com.akiban.qp.row.Row;
import com.akiban.server.service.externaldata.CsvFormat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;

public class PostgresCopyCsvOutputter extends PostgresOutputter<Row>
{
    private CsvFormat format;

    public PostgresCopyCsvOutputter(PostgresQueryContext context,
                                    PostgresDMLStatement statement,
                                    CsvFormat format) {
        super(context, statement);
        this.format = format;
        encoder = new ServerValueEncoder(format.getEncoding(),
                                         new QuotingByteArrayOutputStream());
    }

    @Override
    public void output(Row row, boolean usePVals) throws IOException {
        messenger.beginMessage(PostgresMessages.COPY_DATA_TYPE.code());
        output(row, messenger.getRawOutput(), usePVals);
        messenger.sendMessage();
    }

    @Override
    public void beforeData() throws IOException {
        messenger.beginMessage(PostgresMessages.COPY_OUT_RESPONSE_TYPE.code());
        messenger.write(0);
        messenger.writeShort(ncols);
        for (int i = 0; i < ncols; i++) {
            assert !context.isColumnBinary(i);
            messenger.writeShort(0);
        }
        messenger.sendMessage();
        if (format.getHeadings() != null) {
            messenger.beginMessage(PostgresMessages.COPY_DATA_TYPE.code());
            outputHeadings(messenger.getRawOutput());
            messenger.sendMessage();
        }
    }

    @Override
    public void afterData() throws IOException {
        messenger.beginMessage(PostgresMessages.COPY_DONE_TYPE.code());
        messenger.sendMessage();
    }

    public void output(Row row, OutputStream outputStream, boolean usePVals) 
            throws IOException {
        for (int i = 0; i < ncols; i++) {
            if (i > 0) outputStream.write(format.getDelimiterByte());
            PostgresType type = columnTypes.get(i);
            boolean binary = context.isColumnBinary(i);
            QuotingByteArrayOutputStream bytes;
            if (usePVals) bytes = (QuotingByteArrayOutputStream)encoder.encodePValue(row.pvalue(i), type, binary);
            else bytes = (QuotingByteArrayOutputStream)encoder.encodeValue(row.eval(i), type, binary);
            if (bytes != null) {
                bytes.quote(format);
                bytes.writeTo(outputStream);
            }
            else {
                outputStream.write(format.getNullBytes());
            }
        }
        outputStream.write(format.getRecordEndBytes());
    }

    public void outputHeadings(OutputStream outputStream) throws IOException {
        for (int i = 0; i < ncols; i++) {
            if (i > 0) outputStream.write(format.getDelimiterByte());
            outputStream.write(format.getHeadingBytes(i));
        }
        outputStream.write(format.getRecordEndBytes());
    }

    static class QuotingByteArrayOutputStream extends ByteArrayOutputStream {
        public void quote(CsvFormat format) {
            if (needsQuoting(format.getRequiresQuoting())) {
                for (int i = 0; i < count; i++) {
                    int b = buf[i] & 0xFF;
                    if ((b == format.getQuoteByte()) ||
                        (b == format.getEscapeByte())) {
                        insert(i, format.getEscapeByte());
                        i++;
                    }
                }
                insert(0, format.getQuoteByte());
                insert(count, format.getQuoteByte());
            }
        }

        private boolean needsQuoting(byte[] quotable) {
            for (int i = 0; i < count; i++) {
                byte b = buf[i];
                for (int j = 0; j < quotable.length; j++) {
                    if (b == quotable[j]) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void insert(int pos, int b) {
            write(b);           // for private ensureCapacity.
            System.arraycopy(buf, pos, buf, pos+1, count-pos-1);
            buf[pos] = (byte)b;
        }
    }
 
}
