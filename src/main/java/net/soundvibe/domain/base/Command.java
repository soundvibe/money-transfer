package net.soundvibe.domain.base;

public interface Command {

    default String getName() {
        return this.getClass().getSimpleName();
    }
}
