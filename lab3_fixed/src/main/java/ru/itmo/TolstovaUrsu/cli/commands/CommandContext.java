package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.auth.UserService;
import ru.itmo.TolstovaUrsu.service.ReportService;
import ru.itmo.TolstovaUrsu.storage.AppStorage;

import java.util.Scanner;

public final class CommandContext {

    private final ReportService service;
    private final AppStorage    storage;
    private final Scanner       scanner;
    private final UserService   userService;

    public CommandContext(ReportService service, AppStorage storage,
                         Scanner scanner, UserService userService) {
        this.service     = service;
        this.storage     = storage;
        this.scanner     = scanner;
        this.userService = userService;
    }

    public ReportService getService()     { return service; }
    public AppStorage    getStorage()     { return storage; }
    public Scanner       getScanner()     { return scanner; }
    public UserService   getUserService() { return userService; }
}
