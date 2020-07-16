/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit.model.api;

public class ErrorMessage {
    private String message;
    private Exception exception;

    public ErrorMessage(String message) {
        this.message = message;
    }

    public ErrorMessage(Exception exception) {
        this.message = exception.getMessage();
        this.exception = exception;
    }

    public String getMessage() {
        return this.message;
    }

    public Exception getException() {
        return this.exception;
    }
}
