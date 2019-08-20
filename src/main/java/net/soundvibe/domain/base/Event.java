package net.soundvibe.domain.base;

public interface Event {

    default String getName() {
        return this.getClass().getSimpleName();
    }

}
