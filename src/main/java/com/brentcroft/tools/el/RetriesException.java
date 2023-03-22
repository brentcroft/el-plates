package com.brentcroft.tools.el;

import jakarta.el.ELException;
import lombok.Getter;

import static java.lang.String.format;

@Getter
public class RetriesException extends ELException
{
    private final int tries;
    private final double seconds;
    private final String test;

    public RetriesException(int tries, double seconds, String test) {
        super(format( "Ran out of tries (%s) after %.2f seconds but test still true: %s", tries, seconds, test ));
        this.tries = tries;
        this.seconds = seconds;
        this.test = test;
    }
}
