
package com.akiban.server.types3;

import com.akiban.util.BitSets;
import com.google.common.base.Predicate;

import java.util.Collections;
import java.util.List;

public abstract class TAggregatorBase implements TAggregator {

    @Override
    public String id() {
        return getClass().getName();
    }

    @Override
    public int[] getPriorities() {
        return new int[] { 1 };
    }

    @Override
    public String[] registeredNames() {
        return new String[] { displayName() };
    }

    @Override
    public final String displayName() {
        return name;
    }

    @Override
    public List<TInputSet> inputSets() {
        return Collections.singletonList(
                new TInputSet(inputClass, BitSets.of(0), false, inputClass == null, null));
    }

    @Override
    public InputSetFlags exactInputs() {
        return InputSetFlags.ALL_OFF;
    }

    @Override
    public final String toString() {
        return displayName();
    }

    @Override
    public Predicate<List<? extends TPreptimeValue>> isCandidate() {
        return null;
    }

    protected TClass inputClass() {
        return inputClass;
    }

    protected TAggregatorBase(String name, TClass inputClass) {
        this.name = name;
        this.inputClass = inputClass;
    }

    private final String name;
    private final TClass inputClass;
}
