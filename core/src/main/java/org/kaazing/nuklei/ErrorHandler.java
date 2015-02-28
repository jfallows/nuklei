package org.kaazing.nuklei;

@FunctionalInterface
public interface ErrorHandler
{

    void handleError(String message);

}
