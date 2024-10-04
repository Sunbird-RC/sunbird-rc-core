package dev.sunbirdrc.registry.transform;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Data<T> {

    private final T data;

}
