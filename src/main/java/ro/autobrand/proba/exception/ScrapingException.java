package ro.autobrand.proba.exception;

public class ScrapingException extends RuntimeException {
    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}