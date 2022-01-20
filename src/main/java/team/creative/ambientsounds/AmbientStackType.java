package team.creative.ambientsounds;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

public enum AmbientStackType {
    
    overwrite {
        @Override
        public void apply(Object holder, Field field, Object newHolder) throws IllegalArgumentException, IllegalAccessException {
            throw new UnsupportedOperationException();
        }
    },
    add {
        @Override
        public void apply(Object holder, Field field, Object newHolder) throws IllegalArgumentException, IllegalAccessException {
            Object newValue = field.get(newHolder);
            if (newValue != null) {
                if (field.getType().isArray()) {
                    Object originalValue = field.get(holder);
                    if (originalValue != null) {
                        int originalSize = Array.getLength(originalValue);
                        int newSize = Array.getLength(newValue);
                        Object newArray = Array.newInstance(field.getType().getComponentType(), originalSize + newSize);
                        for (int i = 0; i < originalSize; i++)
                            Array.set(newArray, i, Array.get(originalValue, i));
                        for (int i = 0; i < newSize; i++)
                            Array.set(newArray, originalSize + i, Array.get(newValue, i));
                        field.set(holder, newArray);
                    } else
                        field.set(holder, newValue);
                } else
                    field.set(holder, newValue);
            }
        }
    },
    set {
        @Override
        public void apply(Object holder, Field field, Object newHolder) throws IllegalArgumentException, IllegalAccessException {
            Object newValue = field.get(newHolder);
            if (newValue != null)
                field.set(holder, newValue);
        }
    };
    
    public abstract void apply(Object holder, Field field, Object newHolder) throws IllegalArgumentException, IllegalAccessException;
    
}
