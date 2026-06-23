package ru.itmo.TolstovaUrsu.ui;

import ru.itmo.TolstovaUrsu.auth.UserService;
import ru.itmo.TolstovaUrsu.service.ReportService;
import ru.itmo.TolstovaUrsu.storage.AppStorage;

public class ServiceLocator {

    private static ReportService service;
    private static AppStorage    storage;
    private static UserService   userService;

    public static void init(ReportService svc, AppStorage sto, UserService us) {
        service     = svc;
        storage     = sto;
        userService = us;
    }

    public static ReportService getService()     { return service; }
    public static AppStorage    getStorage()     { return storage; }
    public static UserService   getUserService() { return userService; }
}
