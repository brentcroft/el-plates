package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.ImportHandler;

import java.util.Map;

public interface ELContextFactory
{
    ELContext getELContext( Map< ?, ? > rootObjects );

    ELContext getELContext( Map< ?, ? > rootObjects, SimpleELContext parent );

    ELContext getELConfigContext();

    ImportHandler getImportHandler();
}
