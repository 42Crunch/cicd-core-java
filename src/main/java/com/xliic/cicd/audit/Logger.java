/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

public interface Logger {

    public interface Level {
        public static final int FATAL = 5;
        public static final int ERROR = 4;
        public static final int WARN = 3;
        public static final int INFO = 2;
        public static final int DEBUG = 1;
    }

    public void setLevel(int level);

    public void fatal(String message);

    public void error(String message);

    public void warn(String message);

    public void info(String message);

    public void debug(String message);

}
