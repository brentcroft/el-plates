package com.brentcroft.tools.el;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ReturnException extends RuntimeException
{
    private Object value;
}
