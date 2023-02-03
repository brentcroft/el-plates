package com.brentcroft.tools.el;

import jakarta.el.ELException;
import lombok.Getter;

import static java.lang.String.format;

@Getter
public class RetriesException extends ELException
{
    private final int tries;
    private final String test;

    public RetriesException(int tries, String test) {
        super(format( "Ran out of tries (%s) but: %s", tries, test ));
        this.tries = tries;
        this.test = test;
    }
}
