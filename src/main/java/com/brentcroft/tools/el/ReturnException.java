package com.brentcroft.tools.el;

import lombok.AllArgsConstructor;

import java.util.function.Supplier;

@AllArgsConstructor
public class ReturnException extends RuntimeException implements Supplier<Object>
{
    private Object value;

    @Override
    public Object get()
    {
        return value;
    }
}
