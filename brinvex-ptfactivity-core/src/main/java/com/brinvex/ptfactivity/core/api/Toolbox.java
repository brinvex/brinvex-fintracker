package com.brinvex.ptfactivity.core.api;

import com.brinvex.ptfactivity.core.api.facade.JsonMapperFacade;
import com.brinvex.ptfactivity.core.api.facade.PdfReaderFacade;
import com.brinvex.ptfactivity.core.api.facade.ValidatorFacade;

public interface Toolbox {

    ValidatorFacade validator();

    PdfReaderFacade pdfReader();

    JsonMapperFacade jsonMapper();
}
