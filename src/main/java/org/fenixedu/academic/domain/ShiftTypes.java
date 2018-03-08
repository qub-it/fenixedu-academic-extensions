package org.fenixedu.academic.domain;

import java.util.Collection;

import org.fenixedu.academic.domain.ShiftType;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ShiftTypes {

    private final ImmutableSet<ShiftType> types;

    public ShiftTypes(JsonElement json) {
        ImmutableSet.Builder<ShiftType> builder = ImmutableSet.builder();
        for (JsonElement el : json.getAsJsonArray()) {
            builder.add(ShiftType.valueOf(el.getAsString()));
        }
        this.types = builder.build();
    }

    public ShiftTypes(Collection<ShiftType> types) {
        this.types = ImmutableSet.copyOf(types);
    }

    public ShiftTypes() {
        this.types = ImmutableSet.of();
    }

    public Collection<ShiftType> getTypes() {
        return types;
    }

    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        types.stream().map(ShiftType::name).map(JsonPrimitive::new).forEach(type -> array.add(type));
        return array;
    }

}
