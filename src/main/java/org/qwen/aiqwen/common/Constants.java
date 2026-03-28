
package org.qwen.aiqwen.common;

public class Constants {

    public static final String SUCCESS_CODE = "200";
    public static final String ERROR_CODE = "500";
    public static final String UNAUTHORIZED_CODE = "401";

    public static final String DEFAULT_MODEL = "qwen-turbo";
    public static final String DEFAULT_CHARSET = "UTF-8";

    private Constants() {
        throw new IllegalStateException("Constant class");
    }
}