package com.brentcroft.tools.el;

import jakarta.el.ELException;

public class UserException extends ELException
{
    public UserException(String msg) {
        super(msg);
    }
}
