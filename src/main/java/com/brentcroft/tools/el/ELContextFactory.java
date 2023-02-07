package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.ImportHandler;

import java.util.Map;

public interface ELContextFactory
{
    ELContext getELContext( Map< ?, ? > rootObjects );

    ELContext getELConfigContext();

    ImportHandler getImportHandler();
}
