package com.gist.guild.commons.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentRepositoryMethodParameter<T> {
    Class<T> type;
    T value;
}
