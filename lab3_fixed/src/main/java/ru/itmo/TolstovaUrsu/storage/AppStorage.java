package ru.itmo.TolstovaUrsu.storage;

import ru.itmo.TolstovaUrsu.service.ReportService;
import ru.itmo.TolstovaUrsu.storage.XmlFileStorage.LoadResult;

import java.io.File;

public class AppStorage {

    private final ReportService  service;
    private final XmlFileStorage xmlStorage = new XmlFileStorage();
    private final FileValidator  validator  = new FileValidator();

    public AppStorage(ReportService service) {
        this.service = service;
    }

    public void save(String path) throws Exception {
        File file = new File(path);
        xmlStorage.save(file, service.getAllReports(), service.getAllLines());
    }

    public void load(String path) throws Exception {
        File file = new File(path);

        if (!file.exists())
            throw new IllegalArgumentException(
                    "Ошибка загрузки: файл не найден — " + path);
        if (!file.canRead())
            throw new IllegalArgumentException(
                    "Ошибка загрузки: файл недоступен для чтения — " + path);
        LoadResult result;
        try {
            result = xmlStorage.load(file);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Ошибка загрузки: не удалось разобрать XML — " + e.getMessage());
        }
        validator.validate(result);
        service.replaceAll(result.reports, result.lines);
    }
}
