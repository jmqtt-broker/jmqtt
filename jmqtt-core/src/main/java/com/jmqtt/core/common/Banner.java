package com.jmqtt.core.common;

import java.io.PrintStream;

public final class Banner {

    private Banner() {}

    public static void print(PrintStream out) {
        out.println("------------------------------------------------------");
        out.println("|   ####   ##   ##    ######    #########  ########## |");
        out.println("|    ##    # ### #   ##    ##       ##         ##     |");
        out.println("|    ##    ## # ##   ##    ##       ##         ##     |");
        out.println("|    ##    ##   ##   ##    ##       ##         ##     |");
        out.println("|    ##    ##   ##   ##  ## #       ##         ##     |");
        out.println("| #####    ##   ##     ####  ##     ##         ##     |");
        out.println("------------------------------------------------------");
    }
}
