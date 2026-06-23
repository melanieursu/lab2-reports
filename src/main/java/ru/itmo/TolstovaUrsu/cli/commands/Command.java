package ru.itmo.TolstovaUrsu.cli.commands;

import ru.itmo.TolstovaUrsu.validation.ValidationException;

public interface Command {

    String name();

    void execute(String[] tokens) throws ValidationException;

    String usage();
}
