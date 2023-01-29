package com.brentcroft.tools.el;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static java.lang.String.format;

@AllArgsConstructor
@Getter
public class RetriesException extends RuntimeException
{
    private final int tries;
    private final String test;

    public String toString()
    {
        return format( "Ran out of tries (%s) but: %s", tries, test );
    }
}
