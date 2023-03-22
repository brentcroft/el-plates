package com.brentcroft.tools.el;

import jakarta.el.ELException;
import lombok.AllArgsConstructor;

import java.util.function.Supplier;

@AllArgsConstructor
public class ReturnException extends ELException implements Supplier< Object >
{
    private Object value;

    @Override
    public Object get()
    {
        return value;
    }
}
