package com.brentcroft.tools.el;

//import javax.el.ELContext;
import jakarta.el.*;
import java.util.Map;

public interface ELContextFactory
{
    ELContext getELContext( Map< ?, ? > rootObjects );

    ELContext getELConfigContext();
}
