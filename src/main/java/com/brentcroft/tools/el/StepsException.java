package com.brentcroft.tools.el;

import lombok.Getter;

import static java.lang.String.format;

@Getter
public class StepsException extends RuntimeException
{
    private final int line;
    private final String step;
    private final Object base;
    private final Object methodName;

    public StepsException( int line, String step, Object base, Object methodName, Throwable cause )
    {
        super( format( "Failed at step [%s] %s; base=%s, method=%s", line, step, base, methodName ), cause );
        this.line = line;
        this.step = step;
        this.base = base;
        this.methodName = methodName;
    }
}
