package com.brinvex.ptfactivity.core.internal;

import com.brinvex.dms.api.DmsFactory;
import com.brinvex.java.validation.Validate;
import com.brinvex.ptfactivity.core.api.PtfActivityRuntimeConfig;
import com.brinvex.ptfactivity.core.api.Toolbox;
import com.brinvex.ptfactivity.core.api.facade.JsonMapperFacade;
import com.brinvex.ptfactivity.core.api.facade.PdfReaderFacade;
import com.brinvex.ptfactivity.core.api.facade.ValidatorFacade;
import com.brinvex.ptfactivity.core.internal.facade.JsonMapperFacadeImpl;
import com.brinvex.ptfactivity.core.internal.facade.PdfReaderFacadeImpl;
import com.brinvex.ptfactivity.core.internal.facade.ValidatorFacadeImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class ToolboxImpl implements Toolbox {

    private static final Logger LOG = LoggerFactory.getLogger(ToolboxImpl.class);

    private final PtfActivityRuntimeConfig config;

    private volatile ValidatorFacade validator;

    private volatile JsonMapperFacade jsonMapper;

    private volatile DmsFactory dmsFactory;

    private volatile PdfReaderFacade pdfReader;

    public ToolboxImpl(PtfActivityRuntimeConfig config) {
        Validate.notNull(config);
        this.config = config;
    }

    @Override
    public ValidatorFacade validator() {
        if (validator == null) {
            synchronized (this) {
                if (validator == null) {
                    Supplier<Validator> jakartaValidatorSupplier = config.validator();
                    jakarta.validation.Validator jakartaValidator;
                    if (jakartaValidatorSupplier != null) {
                        jakartaValidator = jakartaValidatorSupplier.get();
                    } else {
                        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
                            jakartaValidator = validatorFactory.getValidator();
                        }
                    }
                    validator = new ValidatorFacadeImpl(jakartaValidator);
                }
            }
        }
        return validator;
    }

    @Override
    public JsonMapperFacade jsonMapper() {
        if (jsonMapper == null) {
            synchronized (this) {
                if (jsonMapper == null) {
                    Supplier<JsonMapperFacade> jsonMapperSupplier = config.jsonMapper();
                    if (jsonMapperSupplier != null) {
                        jsonMapper = jsonMapperSupplier.get();
                    } else {
                        jsonMapper = new JsonMapperFacadeImpl();
                    }
                }
            }
        }
        return jsonMapper;
    }

    @Override
    public PdfReaderFacade pdfReader() {
        if (pdfReader == null) {
            synchronized (this) {
                if (pdfReader == null) {
                    Supplier<PdfReaderFacade> pdfReaderSupplier = config.pdfReader();
                    if (pdfReaderSupplier != null) {
                        pdfReader = pdfReaderSupplier.get();
                    } else {
                        pdfReader = new PdfReaderFacadeImpl();
                    }
                }
            }
        }
        return pdfReader;
    }

    public DmsFactory dmsFactory() {
        if (dmsFactory == null) {
            synchronized (this) {
                if (dmsFactory == null) {
                    Supplier<DmsFactory> dmsFactorySupplier = config.dmsFactory();
                    if (dmsFactorySupplier != null) {
                        dmsFactory = dmsFactorySupplier.get();
                    } else {
                        throw new IllegalStateException("dmsFactory supplier must not be null");
                    }
                }
            }
        }
        return dmsFactory;
    }
}
