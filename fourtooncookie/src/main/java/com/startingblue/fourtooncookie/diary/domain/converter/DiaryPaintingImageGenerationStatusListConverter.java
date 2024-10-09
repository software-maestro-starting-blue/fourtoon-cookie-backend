package com.startingblue.fourtooncookie.diary.domain.converter;

import com.startingblue.fourtooncookie.diary.domain.DiaryPaintingImageGenerationStatus;
import com.startingblue.fourtooncookie.global.converter.exception.ConversionException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
@Component
public class DiaryPaintingImageGenerationStatusListConverter implements AttributeConverter<List<DiaryPaintingImageGenerationStatus>, String> {

    private static final String EMPTY_LIST_STRING = "";

    @Override
    public String convertToDatabaseColumn(List<DiaryPaintingImageGenerationStatus> attribute) {
        if (isAttributeEmpty(attribute)) {
            return EMPTY_LIST_STRING;
        }

        try {
            return attribute.stream()
                    .map(DiaryPaintingImageGenerationStatus::name)
                    .collect(Collectors.joining(","));
        } catch (Exception e) {
            throw new ConversionException("Error converting List<DiaryImageGenerationStatus> to String", e);
        }
    }

    @Override
    public List<DiaryPaintingImageGenerationStatus> convertToEntityAttribute(String dbData) {
        if (isDBColumnEmpty(dbData)) {
            return new ArrayList<>();
        }

        try {
            return Arrays.stream(dbData.split(","))
                    .map(DiaryPaintingImageGenerationStatus::valueOf)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new ConversionException("Error converting String to List<DiaryImageGenerationStatus>", e);
        }
    }

    private static boolean isAttributeEmpty(List<DiaryPaintingImageGenerationStatus> attribute) {
        return attribute == null || attribute.isEmpty();
    }

    private static boolean isDBColumnEmpty(String dbData) {
        return dbData == null || dbData.isEmpty();
    }
}
