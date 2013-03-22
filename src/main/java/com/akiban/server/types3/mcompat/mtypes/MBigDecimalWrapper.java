
package com.akiban.server.types3.mcompat.mtypes;

import com.akiban.server.types3.common.BigDecimalWrapper;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class MBigDecimalWrapper implements BigDecimalWrapper {

    public static final MBigDecimalWrapper ZERO = new MBigDecimalWrapper(BigDecimal.ZERO);

    private BigDecimal value;

    public MBigDecimalWrapper(BigDecimal value) {
        this.value = value;
    }

    public MBigDecimalWrapper(String num)
    {
        value = new BigDecimal(num);
    }

    public MBigDecimalWrapper(long val)
    {
        value = BigDecimal.valueOf(val);
    }

    public MBigDecimalWrapper()
    {
        value = BigDecimal.ZERO;
    }

    @Override
    public void reset() {
        value = BigDecimal.ZERO;
    }
            
    @Override
    public BigDecimalWrapper set(BigDecimalWrapper other) {
        value = other.asBigDecimal();
        return this;
    }
            
    @Override
    public BigDecimalWrapper add(BigDecimalWrapper other) {
        value = value.add(other.asBigDecimal());
        return this;
    }

    @Override
    public BigDecimalWrapper subtract(BigDecimalWrapper other) {
        value = value.subtract(other.asBigDecimal());
        return this;
    }

    @Override
    public BigDecimalWrapper multiply(BigDecimalWrapper other) {
        value = value.multiply(other.asBigDecimal());
        return this;
    }

    @Override
    public BigDecimalWrapper divide(BigDecimalWrapper other) {
        value = value.divide(other.asBigDecimal());
        return this;
    }

    @Override
    public BigDecimalWrapper ceil() {
        value = value.setScale(0, RoundingMode.CEILING);
        return this;
    }
    
    @Override
    public BigDecimalWrapper floor() {
        value = value.setScale(0, RoundingMode.FLOOR);
        return this;
    }
    
    @Override
    public BigDecimalWrapper truncate(int scale) {
        value = value.setScale(scale, RoundingMode.DOWN);
        return this;
    }
    
    @Override
    public BigDecimalWrapper round(int scale) {
        value = value.setScale(scale, RoundingMode.HALF_UP);
        return this;
    }
    
    @Override
    public int getSign() {
        return value.signum();
    }
    
    @Override
    public BigDecimalWrapper divide(BigDecimalWrapper divisor, int scale)
    {
        value = value.divide(divisor.asBigDecimal(),
                scale,
                RoundingMode.HALF_UP);
        return this;
    }

    @Override
    public BigDecimalWrapper divideToIntegralValue(BigDecimalWrapper divisor)
    {
        value = value.divideToIntegralValue(divisor.asBigDecimal());
        return this;
    }

    @Override
    public BigDecimalWrapper abs()
    {
        value = value.abs();
        return this;
    }
    
    @Override
    public int getScale()
    {
        return value.scale();
    }

    @Override
    public int getPrecision()
    {
        return value.precision();
    }

    @Override
    public BigDecimalWrapper parseString(String num)
    {
        value = new BigDecimal (num);
        return this;
    }

    @Override
    public int compareTo(BigDecimalWrapper o)
    {
        return value.compareTo(o.asBigDecimal());
    }

    @Override
    public BigDecimalWrapper round(int precision, int scale)
    {
        value = value.round(new MathContext(precision, RoundingMode.HALF_UP));
        return this;
    }

    @Override
    public BigDecimalWrapper negate()
    {
        value = value.negate();
        return this;
    }

    @Override
    public BigDecimal asBigDecimal() {
        return value;
    }

    @Override
    public boolean isZero()
    {
        return value.signum() == 0;
    }

    @Override
    public BigDecimalWrapper mod(BigDecimalWrapper num)
    {
        value = value.remainder(num.asBigDecimal());
        return this;
    }

    @Override
    public String toString() {
        return value == null ? "UNSET" : value.toString();
    }

    @Override
    public BigDecimalWrapper deepCopy()
    {
        return new MBigDecimalWrapper(value);
    }
}

