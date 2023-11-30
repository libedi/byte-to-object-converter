package io.github.libedi.converter.exception;

/**
 * ConvertFailException
 *
 * @author "Sangjun,Park"
 *
 */
public class ConvertFailException extends RuntimeException {

    private static final long serialVersionUID = -6517334134314384506L;

    public ConvertFailException(final String message) {
        super("The conversion/deconversion process failed. (" + message + ")");
    }

    public ConvertFailException(final Throwable cause) {
        super("The conversion/deconversion process failed. (" + cause.getMessage() + ")", cause);
    }

}
